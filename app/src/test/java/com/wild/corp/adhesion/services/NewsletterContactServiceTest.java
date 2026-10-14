package com.wild.corp.adhesion.services;

import brevo.ApiClient;
import brevo.Configuration;
import brevo.auth.ApiKeyAuth;
import brevoApi.ContactsApi;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wild.corp.adhesion.config.BrevoNewsletterConfiguration;
import com.wild.corp.adhesion.models.EmailContent;
import okhttp3.MediaType;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Simulates HTTP below the real Brevo SDK, including its JSON and URL encoding. */
class NewsletterContactServiceTest {
    private final Queue<Reply> replies = new ArrayDeque<>();
    private final List<Request> requests = new ArrayList<>();
    private final List<JsonObject> bodies = new ArrayList<>();
    private NewsletterContactService service;
    private ContactsApi api;

    record Reply(int code, String body, Map<String, String> headers) { }

    @BeforeEach
    void setUp() {
        api = new BrevoNewsletterConfiguration().newsletterContactsApi("server-secret");
        ApiClient client = api.getApiClient();
        client.setHttpClient(client.getHttpClient().newBuilder().addInterceptor(chain -> {
            Request request = chain.request();
            requests.add(request);
            if (request.body() != null) {
                Buffer buffer = new Buffer();
                request.body().writeTo(buffer);
                bodies.add(JsonParser.parseString(buffer.readUtf8()).getAsJsonObject());
            }
            if (replies.isEmpty()) throw new AssertionError("Unexpected Brevo request: " + request.method());
            Reply reply = replies.remove();
            Response.Builder response = new Response.Builder().request(request).protocol(Protocol.HTTP_1_1)
                    .code(reply.code()).message("Simulated API").header("Content-Type", "application/json")
                    .body(ResponseBody.create(MediaType.parse("application/json"), reply.body()));
            reply.headers().forEach(response::header);
            return response.build();
        }).build());
        service = new NewsletterContactService(api, "server-secret", 42);
    }

    @ParameterizedTest
    @CsvSource({"false,[42],[],SUBSCRIBED", "true,[42],[],BLOCKED",
            "false,[42],[42],UNSUBSCRIBED", "false,[10],[42],UNSUBSCRIBED",
            "false,[10],[10],NOT_SUBSCRIBED"})
    void readsBrevoStateWithoutMutatingIt(boolean blocked, String lists, String unsubscribed,
                                         NewsletterContactService.Status expected) {
        reply(200, contact(blocked, lists, unsubscribed));
        assertThat(service.getStatus(" Parent+enfants@Example.org ")).isEqualTo(expected);
        assertThat(requests).hasSize(1);
        assertThat(requests.getFirst().method()).isEqualTo("GET");
        assertThat(requests.getFirst().url().encodedPath()).contains("parent%2Benfants%40example.org");
        assertThat(requests.getFirst().url().queryParameter("identifierType")).isEqualTo("email_id");
        assertThat(requests.getFirst().header("api-key")).isEqualTo("server-secret");
        assertThat(bodies).isEmpty();
    }

    @Test
    void createsMissingContactWithoutSubscribingOrUnblocking() {
        reply(404, "{\"code\":\"document_not_found\"}");
        reply(201, "{\"id\":123}");
        reply(200, contact(false, "[]", "[]"));
        assertThat(service.createContact("NEW@example.org")).isEqualTo(NewsletterContactService.Status.NOT_SUBSCRIBED);
        assertThat(requests.get(1).url().encodedPath()).isEqualTo("/v3/contacts");
        JsonObject body = bodies.getFirst();
        assertThat(body.get("email").getAsString()).isEqualTo("new@example.org");
        assertThat(body.get("updateEnabled").getAsBoolean()).isFalse();
        assertThat(body.has("emailBlacklisted")).isFalse();
        assertThat(body.has("listIds")).isFalse();
        assertThat(body.has("smtpBlacklistSender")).isFalse();
    }

    @Test
    void preservesUnsubscriptionsAndDeduplicatesSharedRepresentativeAddressesAcrossBatch() {
        reply(200, contact(true, "[42]", "[]"));
        reply(200, contact(false, "[42]", "[42]"));
        reply(200, contact(false, "[]", "[]"));
        Map<String, NewsletterContactService.Status> result = service.synchronize(List.of(
                "Parent@example.org", " parent@EXAMPLE.org ", "unsubscribed@example.org", "removed@example.org"));
        assertThat(result).containsEntry("parent@example.org", NewsletterContactService.Status.BLOCKED)
                .containsEntry("unsubscribed@example.org", NewsletterContactService.Status.UNSUBSCRIBED)
                .containsEntry("removed@example.org", NewsletterContactService.Status.NOT_SUBSCRIBED).hasSize(3);
        assertThat(requests).hasSize(3).allMatch(request -> request.method().equals("GET"));
    }

    @ParameterizedTest
    @CsvSource({"true,[42],[]", "false,[42],[42]"})
    void enrollmentPreservesExistingOptOut(boolean blocked, String lists, String unsubscribed) {
        reply(200, contact(blocked, lists, unsubscribed));
        assertThat(service.addToNewsletter("parent@example.org"))
                .isEqualTo(NewsletterContactService.SubscriptionRequest.RESUBSCRIPTION_REQUIRED);
        assertThat(requests).hasSize(1);
        assertThat(bodies).isEmpty();
    }

    @Test
    void addsVerifiedExistingContactDirectlyWithoutConfirmation() {
        reply(200, contact(false, "[10]", "[10]"));
        reply(204, "");
        reply(200, contact(false, "[10,42]", "[10]"));
        assertThat(service.addToNewsletter("parent@example.org"))
                .isEqualTo(NewsletterContactService.SubscriptionRequest.SUBSCRIBED);
        assertThat(requests).hasSize(3);
        assertThat(requests.get(1).method()).isEqualTo("PUT");
        assertThat(requests.get(1).url().encodedPath()).isEqualTo("/v3/contacts/parent%40example.org");
        assertThat(bodies.getFirst()).isEqualTo(JsonParser.parseString("{\"listIds\":[42]}").getAsJsonObject());
        assertThat(requests).noneMatch(request -> request.url().encodedPath().contains("doubleOptin"));
    }

    @Test
    void createsAndAddsVerifiedOutsideSubscriberWithoutConfirmation() {
        reply(404, "{\"code\":\"document_not_found\"}");
        reply(201, "{\"id\":123}");
        reply(200, contact(false, "[]", "[]"));
        reply(204, "");
        reply(200, contact(false, "[42]", "[]"));
        assertThat(service.addToNewsletter("outside@example.org"))
                .isEqualTo(NewsletterContactService.SubscriptionRequest.SUBSCRIBED);
        assertThat(requests).hasSize(5);
        assertThat(requests).noneMatch(request -> request.url().encodedPath().contains("doubleOptin"));
    }

    @Test
    void optOutDuringEnrollmentIsNeverReportedAsSubscribed() {
        reply(200, contact(false, "[]", "[]"));
        reply(204, "");
        reply(200, contact(true, "[42]", "[]"));
        assertThat(service.addToNewsletter("parent@example.org"))
                .isEqualTo(NewsletterContactService.SubscriptionRequest.RESUBSCRIPTION_REQUIRED);
        assertThat(bodies.getFirst().has("emailBlacklisted")).isFalse();
    }

    @Test
    void alreadySubscribedIsIdempotent() {
        reply(200, contact(false, "[42]", "[]"));
        assertThat(service.addToNewsletter("parent@example.org"))
                .isEqualTo(NewsletterContactService.SubscriptionRequest.ALREADY_SUBSCRIBED);
        assertThat(requests).hasSize(1);
    }

    @Test
    void concurrentCreationConflictReReadsBrevoWithoutOverwritingOptOut() {
        reply(404, "{}");
        reply(400, "{\"code\":\"duplicate_parameter\"}");
        reply(200, contact(true, "[42]", "[]"));
        assertThat(service.createContact("parent@example.org")).isEqualTo(NewsletterContactService.Status.BLOCKED);
        assertThat(bodies).hasSize(1);
        assertThat(bodies.getFirst().get("updateEnabled").getAsBoolean()).isFalse();
    }

    @Test
    void quotaFailureStopsBatchAndFurtherCallsHonorLongestServerDelay() {
        replies.add(new Reply(429, "{\"message\":\"secret parent@example.org\"}",
                Map.of("Retry-After", "90", "X-Sib-Ratelimit-Reset", "120")));
        NewsletterApiException failure = assertThrows(NewsletterApiException.class,
                () -> service.synchronize(List.of("parent@example.org", "other@example.org")));
        assertThat(failure.getStatusCode()).isEqualTo(429);
        assertThat(failure.getRetryAfter()).isEqualTo(Duration.ofSeconds(120));
        assertThat(failure.isRetryable()).isTrue();
        assertThat(failure.getCause()).isNull();
        assertThat(failure.getMessage()).doesNotContain("secret", "parent@example.org");
        assertThatThrownBy(() -> service.getStatus("other@example.org")).isInstanceOf(NewsletterApiException.class);
        assertThat(requests).hasSize(1);
    }

    @Test
    void successfulLastQuotaRequestPreventsFurtherNetworkCalls() {
        replies.add(new Reply(200, contact(false, "[42]", "[]"),
                Map.of("x-sib-ratelimit-remaining", "0", "x-sib-ratelimit-reset", "60")));
        assertThat(service.getStatus("parent@example.org")).isEqualTo(NewsletterContactService.Status.SUBSCRIBED);
        assertThatThrownBy(() -> service.getStatus("other@example.org")).isInstanceOf(NewsletterApiException.class);
        assertThat(requests).hasSize(1);
    }

    @ParameterizedTest
    @CsvSource({"400,false", "401,false", "403,false", "500,true", "503,true"})
    void apiErrorsAreNotMistakenForMissingContactsOrRetried(int code, boolean retryable) {
        reply(code, "{\"message\":\"server-secret\"}");
        NewsletterApiException failure = assertThrows(NewsletterApiException.class,
                () -> service.createContact("parent@example.org"));
        assertThat(failure.getStatusCode()).isEqualTo(code);
        assertThat(failure.isRetryable()).isEqualTo(retryable);
        assertThat(failure.getMessage()).doesNotContain("server-secret");
        assertThat(requests).hasSize(1);
    }

    @Test
    void failedEnrollmentIsNeverReplayedOrTreatedAsSubscribed() {
        reply(200, contact(false, "[]", "[]"));
        reply(503, "{}");
        assertThatThrownBy(() -> service.addToNewsletter("parent@example.org"))
                .isInstanceOf(NewsletterApiException.class);
        assertThat(requests).hasSize(2);
        assertThat(bodies).hasSize(1);
    }

    @Test
    void networkFailureIsSanitizedAndStopsProcessing() {
        ApiClient client = api.getApiClient();
        var builder = client.getHttpClient().newBuilder();
        builder.interceptors().clear();
        client.setHttpClient(builder.addInterceptor(chain -> {
            throw new IOException("request with server-secret");
        }).build());
        NewsletterApiException failure = assertThrows(NewsletterApiException.class,
                () -> service.getStatus("parent@example.org"));
        assertThat(failure.getStatusCode()).isZero();
        assertThat(failure.getCause()).isNull();
    }

    @Test
    void malformedOrIncompleteStatusDoesNotAllowEnrollment() {
        reply(200, "{\"id\":1,\"listIds\":[42]}");
        assertThatThrownBy(() -> service.addToNewsletter("parent@example.org"))
                .isInstanceOf(NewsletterApiException.class);
        assertThat(bodies).isEmpty();
    }

    @Test
    void validatesWholeBatchAndConfigurationBeforeAnyRequest() {
        assertThatThrownBy(() -> service.synchronize(List.of("parent@example.org", "invalid")))
                .isInstanceOf(IllegalArgumentException.class);
        NewsletterContactService missing = new NewsletterContactService(api, "", 0);
        assertThatThrownBy(() -> missing.getStatus("parent@example.org")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> missing.addToNewsletter("parent@example.org")).isInstanceOf(IllegalStateException.class);
        assertThat(requests).isEmpty();
    }

    @Test
    void newsletterConfigurationLeavesTransactionalGlobalClientUntouched() {
        ApiClient transactional = Configuration.getDefaultApiClient();
        String previousKey = ((ApiKeyAuth) transactional.getAuthentication("api-key")).getApiKey();
        ContactsApi newsletter = new BrevoNewsletterConfiguration().newsletterContactsApi("newsletter-only-key");
        assertThat(newsletter.getApiClient()).isNotSameAs(transactional);
        assertThat(Configuration.getDefaultApiClient()).isSameAs(transactional);
        assertThat(((ApiKeyAuth) transactional.getAuthentication("api-key")).getApiKey()).isEqualTo(previousKey);
        assertThat(newsletter.getApiClient().getHttpClient().retryOnConnectionFailure()).isFalse();
        assertThat(newsletter.getApiClient().getHttpClient().followRedirects()).isFalse();
    }

    @Test
    void transactionalEmailStillUsesSmtpEndpointAndExistingContent() {
        ApiClient previous = Configuration.getDefaultApiClient();
        // Route the unchanged transaction service through the same HTTP simulator.
        Configuration.setDefaultApiClient(api.getApiClient());
        try {
            new BrevoNewsletterConfiguration().newsletterContactsApi("separate-newsletter-key");
            ParamBooleanServices parameters = mock(ParamBooleanServices.class);
            when(parameters.findByParamValue("Envoi_Mails")).thenReturn(true);
            EmailService transactionService = new EmailService();
            ReflectionTestUtils.setField(transactionService, "paramBooleanServices", parameters);
            EmailContent mail = new EmailContent();
            mail.setSubject("Confirmation adhésion");
            mail.setText("<p>Votre adhésion est confirmée</p>");
            reply(201, "{\"messageId\":\"simulated-transaction-id\"}");
            transactionService.singleMessage(List.of("parent@example.org"), mail, null, false);
            assertThat(requests).hasSize(1);
            assertThat(requests.getFirst().url().encodedPath()).isEqualTo("/v3/smtp/email");
            JsonObject body = bodies.getFirst();
            assertThat(body.get("subject").getAsString()).isEqualTo(mail.getSubject());
            assertThat(body.get("htmlContent").getAsString()).isEqualTo(mail.getText());
            assertThat(body.getAsJsonObject("sender").get("email").getAsString()).isEqualTo("adhesion@alod.fr");
            assertThat(body.getAsJsonArray("to").get(0).getAsJsonObject().get("email").getAsString())
                    .isEqualTo("parent@example.org");
        } finally {
            Configuration.setDefaultApiClient(previous);
        }
    }

    private void reply(int code, String body) { replies.add(new Reply(code, body, Map.of())); }

    @Test
    void unsubscribeRemovesOnlyNewsletterListAndVerifiesTheResult() {
        reply(200, contact(false, "[10,42]", "[]"));
        reply(204, "");
        reply(200, contact(false, "[10]", "[]"));
        assertThat(service.unsubscribeFromNewsletter("parent@example.org"))
                .isEqualTo(NewsletterContactService.Status.NOT_SUBSCRIBED);
        assertThat(bodies).hasSize(1);
        assertThat(bodies.getFirst().keySet()).containsExactly("unlinkListIds");
        assertThat(bodies.getFirst().get("unlinkListIds").toString()).isEqualTo("[42]");
    }

    @Test
    void unsubscribingMissingContactNeverCreatesOne() {
        reply(404, "{}");
        assertThat(service.unsubscribeFromNewsletter("parent@example.org"))
                .isEqualTo(NewsletterContactService.Status.NOT_FOUND);
        assertThat(bodies).isEmpty();
    }

    @Test
    void subscriptionByOwnerUnblocksAndReturnsVerifiedStatus() {
        reply(200, contact(true, "[42]", "[]"));
        reply(204, "");
        reply(200, contact(false, "[42]", "[]"));
        assertThat(service.subscribeByOwner("parent@example.org")).isEqualTo(NewsletterContactService.Status.SUBSCRIBED);
        assertThat(bodies.getFirst().get("emailBlacklisted").getAsBoolean()).isFalse();
    }

    @Test
    void adminSynchronizationUnblocksEmailAndAddsOnlyConfiguredList() {
        reply(200, contact(true, "[10]", "[]"));
        reply(204, "");
        reply(200, contact(false, "[10,42]", "[]"));
        assertThat(service.subscribeFromAdministration(" Parent@Example.org "))
                .isEqualTo(NewsletterContactService.AdminSubscription.RESUBSCRIBED);
        assertThat(bodies).hasSize(1);
        assertThat(bodies.getFirst().get("emailBlacklisted").getAsBoolean()).isFalse();
        assertThat(bodies.getFirst().get("listIds").toString()).isEqualTo("[42]");
        assertThat(bodies.getFirst().keySet()).containsExactlyInAnyOrder("emailBlacklisted", "listIds");
        assertThat(requests.get(1).method()).isEqualTo("PUT");
    }

    @Test
    void adminSynchronizationCreatesMissingContactAndVerifiesSubscription() {
        reply(404, "{}");
        reply(201, "{\"id\":123}");
        reply(200, contact(false, "[]", "[]"));
        reply(204, "");
        reply(200, contact(false, "[42]", "[]"));
        assertThat(service.subscribeFromAdministration("parent@example.org"))
                .isEqualTo(NewsletterContactService.AdminSubscription.SUBSCRIBED);
        assertThat(requests).hasSize(5);
    }

    @Test
    void adminSynchronizationReportsPersistingListOptOut() {
        reply(200, contact(true, "[42]", "[42]"));
        reply(204, "");
        reply(200, contact(false, "[42]", "[42]"));
        assertThat(service.subscribeFromAdministration("parent@example.org"))
                .isEqualTo(NewsletterContactService.AdminSubscription.STILL_UNSUBSCRIBED);
    }

    @Test
    void adminSynchronizationLeavesAlreadySubscribedContactUntouched() {
        reply(200, contact(false, "[42]", "[]"));
        assertThat(service.subscribeFromAdministration("parent@example.org"))
                .isEqualTo(NewsletterContactService.AdminSubscription.ALREADY_SUBSCRIBED);
        assertThat(bodies).isEmpty();
    }

    private static String contact(boolean blocked, String lists, String unsubscribed) {
        return "{\"id\":123,\"emailBlacklisted\":" + blocked + ",\"listIds\":" + lists
                + ",\"listUnsubscribed\":" + unsubscribed + "}";
    }
}
