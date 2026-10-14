package com.wild.corp.adhesion.services;

import brevoApi.ContactsApi;
import brevoApi.EmailCampaignsApi;
import com.wild.corp.adhesion.config.BrevoNewsletterConfiguration;
import com.wild.corp.adhesion.models.Historique;
import com.wild.corp.adhesion.repository.HistoriqueRepository;
import okhttp3.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Simulated HTTP beneath the real SDK; never contacts Brevo. */
class NewsletterCampaignServiceTest {
    private final Queue<Object> replies = new ArrayDeque<>();
    private final List<Request> requests = new ArrayList<>();
    private final Map<Long, Historique> stored = new HashMap<>();
    private final HistoriqueRepository history = mock(HistoriqueRepository.class);
    private final EmailService mail = mock(EmailService.class);
    private NewsletterCampaignService service;
    private EmailCampaignsApi api;
    private ContactsApi contacts;

    @BeforeEach
    void setUp() {
        api = new BrevoNewsletterConfiguration().newsletterCampaignsApi("simulation-key");
        contacts = new ContactsApi(api.getApiClient());
        var client = contacts.getApiClient();
        client.setHttpClient(client.getHttpClient().newBuilder().addInterceptor(chain -> {
            Request request = chain.request();
            requests.add(request);
            // The durable history is committed before any send request leaves the application.
            if (request.method().equals("POST")) assertThat(stored.get(7L).getCampaignStatus()).isEqualTo("REQUESTED");
            if (replies.isEmpty()) throw new AssertionError("Unexpected HTTP: " + request.url());
            Object reply = replies.remove();
            if (reply instanceof IOException failure) throw failure;
            Reply response = (Reply) reply;
            return new Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(response.code())
                    .message("Simulated Brevo").header("Content-Type", "application/json")
                    .body(ResponseBody.create(MediaType.parse("application/json"), response.body())).build();
        }).build());

        service = new NewsletterCampaignService(api, contacts, history, mail, "simulation-key", 42);
        when(mail.isMailSendingEnabled()).thenReturn(true);
        when(history.findByCampaignId(any())).thenAnswer(call -> Optional.ofNullable(stored.get(call.getArgument(0))));
        when(history.saveAndFlush(any())).thenAnswer(call -> {
            Historique value = call.getArgument(0); stored.put(value.getCampaignId(), value); return value;
        });
    }

    record Reply(int code, String body) { }
    private void list() { replies.add(new Reply(200, "{\"id\":42,\"name\":\"Newsletter ALOD\",\"uniqueSubscribers\":120}")); }
    private String campaign(String status, String lists) {
        return "{\"id\":7,\"name\":\"Octobre\",\"subject\":\"Actualités ALOD\",\"previewText\":\"Cette semaine\","
                + "\"htmlContent\":\"<h1>Bonjour</h1>\",\"type\":\"classic\",\"status\":\"" + status
                + "\",\"recipients\":{\"lists\":" + lists + ",\"exclusionLists\":[]}}";
    }
    private void draft() { replies.add(new Reply(200, campaign("draft", "[42]"))); }
    private String previewVersion() { list(); draft(); return service.preview(7).confirmationVersion(); }
    private long sends() { return requests.stream().filter(r -> r.method().equals("POST")).count(); }

    @Test
    void filtersDraftsAndPaginatesBeforeFiltering() {
        list();
        replies.add(new Reply(200, "{\"count\":4,\"campaigns\":[" + campaign("draft", "[42]") + ","
                + campaign("draft", "[42,43]") + "," + campaign("sent", "[42]") + "," + campaign("draft", "[43]") + "]}"));
        var drafts = service.drafts();
        assertThat(drafts).hasSize(1);
        assertThat(drafts.getFirst().subject()).isEqualTo("Actualités ALOD");
        assertThat(drafts.getFirst().listName()).isEqualTo("Newsletter ALOD");
        assertThat(drafts.getFirst().htmlContent()).contains("Bonjour");
        assertThat(requests.getLast().url().queryParameter("status")).isEqualTo("draft");
        assertThat(requests.getLast().header("api-key")).isEqualTo("simulation-key");
    }

    @Test
    void upstreamAuthenticationFailuresDoNotBecomeApplicationSessionFailures() {
        replies.add(new Reply(401, "{\"message\":\"private provider detail\"}"));
        assertThatThrownBy(service::drafts).isInstanceOfSatisfying(ResponseStatusException.class, error -> {
            assertThat(error.getStatusCode().value()).isEqualTo(502);
            assertThat(error.getReason()).contains("lecture de la liste", "HTTP 401", "clé API Brevo")
                    .doesNotContain("private provider detail", "simulation-key");
            assertThat(error.getCause()).isNull();
        });
        list(); replies.add(new Reply(403, "{}"));
        assertThatThrownBy(service::drafts).isInstanceOfSatisfying(ResponseStatusException.class, error -> {
            assertThat(error.getStatusCode().value()).isEqualTo(502);
            assertThat(error.getReason()).contains("lecture des brouillons", "HTTP 403");
        });
    }

    @Test
    void missingNewsletterConfigurationIsReportedBeforeAnyBrevoCall() {
        service = new NewsletterCampaignService(api, contacts, history, mail, "simulation-key", 0);
        assertThatThrownBy(service::drafts).isInstanceOfSatisfying(ResponseStatusException.class, error -> {
            assertThat(error.getStatusCode().value()).isEqualTo(503);
            assertThat(error.getReason()).contains("BREVO_NEWSLETTER_LIST_ID");
        });
        assertThat(requests).isEmpty();
    }

    @Test
    void malformedBrevoResponseHasARecoverableErrorInsteadOfAnUncaughtParsingException() {
        list(); replies.add(new Reply(200, "{\"count\":1,\"campaigns\":[{\"id\":{}}]}"));
        assertThatThrownBy(service::drafts).isInstanceOfSatisfying(ResponseStatusException.class, error -> {
            assertThat(error.getStatusCode().value()).isEqualTo(502);
            assertThat(error.getReason()).contains("Réponse Brevo illisible", "brouillons");
        });
    }

    @Test
    void campaignTwoIsReturnedWhenItsAudienceIsTheConfiguredNewsletterList() {
        list();
        replies.add(new Reply(200, "{\"count\":1,\"campaigns\":[" + campaign("draft", "[42]").replace("\"id\":7", "\"id\":2") + "]}"));
        assertThat(service.drafts()).extracting(campaign -> campaign.id()).containsExactly(2L);
    }

    @Test
    void paginationDoesNotStopWhenFirstPageHasNoEligibleCampaign() {
        list();
        String first = String.join(",", Collections.nCopies(100, campaign("draft", "[43]")));
        replies.add(new Reply(200, "{\"count\":101,\"campaigns\":[" + first + "]}"));
        replies.add(new Reply(200, "{\"count\":101,\"campaigns\":[" + campaign("draft", "[42]") + "]}"));
        assertThat(service.drafts()).hasSize(1);
        assertThat(requests.getLast().url().queryParameter("offset")).isEqualTo("100");
    }

    @Test
    void acceptsOnceThenReconcilesUntilCompletedEvenAfterRestart() {
        String version = previewVersion();
        list(); draft(); replies.add(new Reply(204, ""));
        Historique accepted = service.send(7, version);
        assertThat(accepted.getCampaignId()).isEqualTo(7L);
        assertThat(accepted.getCampaignStatus()).isEqualTo("ACCEPTED");
        assertThat(accepted.getNombre()).isNull();
        assertThat(accepted.getEchecs()).isNull();
        service = new NewsletterCampaignService(api, contacts, history, mail, "simulation-key", 42);
        list(); draft(); // Brevo may still show draft immediately after accepting
        assertThat(service.send(7, version).getCampaignStatus()).isEqualTo("ACCEPTED");
        list(); replies.add(new Reply(200, campaign("inProcess", "[42]")));
        assertThat(service.status(7).getCampaignStatus()).isEqualTo("ACCEPTED");
        list(); replies.add(new Reply(200, campaign("sent", "[42]")));
        assertThat(service.status(7).getCampaignStatus()).isEqualTo("COMPLETED");
        assertThat(sends()).isEqualTo(1);
    }

    @Test
    void networkAmbiguityReadsStatusAndNeverResendsEvenIfStillDraft() {
        String version = previewVersion();
        list(); draft(); replies.add(new IOException("response lost")); draft();
        assertThat(service.send(7, version).getCampaignStatus()).isEqualTo("UNKNOWN");
        assertThat(requests.getLast().method()).isEqualTo("GET");
        list(); draft();
        assertThat(service.send(7, version).getCampaignStatus()).isEqualTo("UNKNOWN");
        assertThat(sends()).isEqualTo(1);
    }

    @Test
    void ambiguousServerResponseCanBeReconciledAsAccepted() {
        String version = previewVersion();
        list(); draft(); replies.add(new Reply(500, "{}"));
        replies.add(new Reply(200, campaign("queued", "[42]")));
        assertThat(service.send(7, version).getCampaignStatus()).isEqualTo("ACCEPTED");
        assertThat(sends()).isEqualTo(1);
    }

    @Test
    void unavailableStatusPreservesUncertaintyAndPreventsRetry() {
        String version = previewVersion();
        list(); draft(); replies.add(new IOException("network")); replies.add(new IOException("network"));
        assertThat(service.send(7, version).getCampaignStatus()).isEqualTo("UNKNOWN");
        list(); replies.add(new IOException("network"));
        assertThatThrownBy(() -> service.send(7, version)).isInstanceOf(ResponseStatusException.class);
        assertThat(stored.get(7L).getCampaignStatus()).isEqualTo("UNKNOWN");
        assertThat(sends()).isEqualTo(1);
    }

    @Test
    void definiteRefusalIsDistinctFromCompletion() {
        String version = previewVersion();
        list(); draft(); replies.add(new Reply(402, "{\"message\":\"credit\"}"));
        assertThat(service.send(7, version).getCampaignStatus()).isEqualTo("REJECTED");
        assertThat(stored.get(7L).getNombre()).isNull();
    }

    @Test
    void disabledMailCannotContactSendApi() {
        when(mail.isMailSendingEnabled()).thenReturn(false);
        assertThatThrownBy(() -> service.send(7, "anything")).isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(503));
        assertThat(requests).isEmpty(); assertThat(stored).isEmpty();
    }

    @Test
    void rejectsWrongListWrongAudienceAndChangedContentBeforeClaim() {
        list(); replies.add(new Reply(200, campaign("draft", "[42,43]")));
        assertThatThrownBy(() -> service.send(7, "anything")).isInstanceOf(ResponseStatusException.class);
        list(); draft();
        assertThatThrownBy(() -> service.send(7, "outdated confirmation")).isInstanceOf(ResponseStatusException.class);
        replies.add(new Reply(200, "{\"id\":42,\"name\":\"Other list\"}"));
        assertThatThrownBy(() -> service.preview(7)).isInstanceOf(ResponseStatusException.class);
        assertThat(sends()).isZero(); assertThat(stored).isEmpty();
    }

    @Test
    void refusesSegmentsAndExclusionsAndNonDrafts() {
        for (String audience : List.of("{\"lists\":[42],\"exclusionLists\":[9]}", "{\"lists\":[42],\"segments\":[9]}")) {
            list(); replies.add(new Reply(200, campaign("draft", "[42]").replace("{\"lists\":[42],\"exclusionLists\":[]}", audience)));
            assertThatThrownBy(() -> service.preview(7)).isInstanceOf(ResponseStatusException.class);
        }
        list(); replies.add(new Reply(200, campaign("queued", "[42]")));
        assertThatThrownBy(() -> service.send(7, "anything")).isInstanceOf(ResponseStatusException.class);
        assertThat(sends()).isZero();
    }
}
