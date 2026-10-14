package com.wild.corp.adhesion.services;

import brevo.ApiException;
import brevoApi.ContactsApi;
import brevoApi.EmailCampaignsApi;
import brevoModel.GetEmailCampaign;
import brevoModel.GetExtendedList;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.wild.corp.adhesion.models.Historique;
import com.wild.corp.adhesion.models.newsletter.NewsletterCampaign;
import com.wild.corp.adhesion.repository.HistoriqueRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class NewsletterCampaignService {
    private final EmailCampaignsApi api;
    private final ContactsApi contacts;
    private final HistoriqueRepository history;
    private final EmailService email;
    private final String apiKey;
    private final long listId;
    private final Gson gson;

    public NewsletterCampaignService(EmailCampaignsApi api, ContactsApi contacts, HistoriqueRepository history,
            EmailService email, @Value("${brevo.newsletter.api-key:}") String apiKey,
            @Value("${brevo.newsletter.list-id:0}") long listId) {
        this.api = api; this.contacts = contacts; this.history = history;
        this.email = email; this.apiKey = apiKey; this.listId = listId;
        this.gson = api.getApiClient().getJson().getGson();
    }

    public List<NewsletterCampaign> drafts() {
        log.info("Consultation des brouillons newsletter : liste={}", listId);
        GetExtendedList list = newsletterList();
        log.info("Liste newsletter validée, lecture des campagnes Brevo en brouillon");
        List<NewsletterCampaign> result = new ArrayList<>();
        try {
            for (long offset = 0; ; offset += 100) {
                var page = api.getEmailCampaigns("classic", "draft", null, null, null, 100L, offset, "desc", false);
                if (page == null || page.getCampaigns() == null) throw unavailable();
                log.debug("Page Brevo : offset={}, campagnes={}", offset, page.getCampaigns().size());
                for (Object item : page.getCampaigns()) {
                    GetEmailCampaign campaign = gson.fromJson(gson.toJson(item), GetEmailCampaign.class);
                    boolean eligible = allowed(campaign) && "draft".equals(remoteStatus(campaign));
                    if (eligible) result.add(view(campaign, list));
                    else log.info("Campagne Brevo exclue : id={}, raison={}",
                            campaign == null ? null : campaign.getId(),
                            allowed(campaign) ? "statut non brouillon" : exclusionReason(campaign));
                }
                if (page.getCampaigns().size() < 100) break;
            }
        } catch (ApiException ex) { throw unavailable("lecture des brouillons", ex); }
        catch (JsonParseException | IllegalArgumentException | IllegalStateException ex) {
            throw invalidResponse("lecture des brouillons", ex);
        }
        log.info("Brouillons newsletter autorisés : {}, liste={}", result.size(), listId);
        return result;
    }

    public NewsletterCampaign preview(long id) {
        GetExtendedList list = newsletterList();
        GetEmailCampaign campaign = campaign(id);
        requireAllowed(campaign);
        return view(campaign, list);
    }

    // Repository methods commit independently: the durable claim MUST exist before sendNow.
    // Do not wrap this method in a transaction spanning the remote API call.
    public synchronized Historique send(long id, String confirmationVersion) {
        if (!email.isMailSendingEnabled()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "L'envoi d'e-mails est désactivé.");
        }
        GetExtendedList list = newsletterList();
        GetEmailCampaign campaign = campaign(id); // always reconcile before considering another request
        requireAllowed(campaign);
        var existing = history.findByCampaignId(id);
        if (existing.isPresent()) return reconcile(existing.get(), campaign);
        if (!"draft".equals(remoteStatus(campaign))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette campagne n'est plus en brouillon.");
        }
        if (!Objects.equals(view(campaign, list).confirmationVersion(), confirmationVersion)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La campagne a changé. Rechargez son aperçu et confirmez à nouveau.");
        }
        Historique attempt = new Historique(list.getName(), campaign.getSubject(), null, null, LocalDateTime.now());
        attempt.setCampaignId(id);
        attempt.setCampaignStatus("REQUESTED");
        attempt.setBrevoStatus("draft");
        try {
            attempt = history.saveAndFlush(attempt);
        } catch (DataIntegrityViolationException concurrentClaim) {
            // Another application instance already owns the send; never POST again.
            return history.findByCampaignId(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT));
        }
        try {
            var response = api.sendEmailCampaignNowWithHttpInfo(id);
            attempt.setCampaignStatus(response.getStatusCode() == 204 ? "ACCEPTED" : "UNKNOWN");
        } catch (ApiException ex) {
            // Only documented, definite refusals are classified as rejected.
            attempt.setCampaignStatus(List.of(400, 402, 404).contains(ex.getCode()) ? "REJECTED" : "UNKNOWN");
        } catch (RuntimeException ex) {
            attempt.setCampaignStatus("UNKNOWN");
        }
        history.saveAndFlush(attempt);
        if ("UNKNOWN".equals(attempt.getCampaignStatus())) {
            try { return reconcile(attempt, campaign(id)); }
            catch (ResponseStatusException unavailable) { return attempt; }
        }
        return attempt;
    }

    public synchronized Historique status(long id) {
        newsletterList();
        GetEmailCampaign campaign = campaign(id);
        requireAllowed(campaign);
        Historique attempt = history.findByCampaignId(id).orElse(null);
        if (attempt == null) {
            attempt = new Historique("Newsletter ALOD", campaign.getSubject(), null, null, null);
            attempt.setCampaignId(id);
            attempt.setCampaignStatus("NOT_REQUESTED");
            attempt.setBrevoStatus(remoteStatus(campaign));
            return attempt;
        }
        return reconcile(attempt, campaign);
    }

    private Historique reconcile(Historique attempt, GetEmailCampaign campaign) {
        String status = remoteStatus(campaign);
        attempt.setBrevoStatus(status);
        if ("sent".equals(status)) attempt.setCampaignStatus("COMPLETED");
        else if (List.of("queued", "inProcess", "in_process", "in_review").contains(status)) attempt.setCampaignStatus("ACCEPTED");
        // draft after a timeout is not proof of refusal: retain the durable claim forever.
        return history.saveAndFlush(attempt);
    }

    private GetExtendedList newsletterList() {
        if (apiKey.isBlank() || listId <= 0) {
            log.warn("Configuration newsletter incomplète : clé présente={}, liste={}", !apiKey.isBlank(), listId);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Configuration newsletter manquante : vérifiez BREVO_APIKEY_PRIVATE et BREVO_NEWSLETTER_LIST_ID dans le processus backend.");
        }
        try {
            GetExtendedList list = contacts.getList(listId, null, null);
            if (list == null || !"Newsletter ALOD".equals(list.getName()) || !Objects.equals(list.getId(), listId)) {
                log.warn("Liste newsletter Brevo non conforme : id configuré={}", listId);
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "La liste Brevo configurée (n°" + listId + ") doit être nommée exactement Newsletter ALOD. Vérifiez BREVO_NEWSLETTER_LIST_ID.");
            }
            return list;
        } catch (ApiException ex) { throw unavailable("lecture de la liste", ex); }
        catch (JsonParseException | IllegalStateException ex) { throw invalidResponse("lecture de la liste", ex); }
    }

    private GetEmailCampaign campaign(long id) {
        if (id <= 0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        try {
            GetEmailCampaign result = api.getEmailCampaign(id, null);
            if (result == null || !Objects.equals(result.getId(), id) || result.getStatus() == null) throw unavailable();
            return result;
        } catch (ApiException ex) { throw unavailable("lecture de la campagne " + id, ex); }
        catch (JsonParseException | IllegalStateException ex) { throw invalidResponse("lecture de la campagne " + id, ex); }
    }

    private boolean allowed(GetEmailCampaign campaign) {
        return exclusionReason(campaign) == null;
    }

    private String exclusionReason(GetEmailCampaign campaign) {
        if (campaign == null || campaign.getId() == null) return "identifiant absent";
        if (campaign.getType() != GetEmailCampaign.TypeEnum.CLASSIC) return "type non classique";
        if (campaign.getStatus() == null) return "statut absent";
        if (campaign.getSubject() == null || campaign.getSubject().isBlank()) return "objet absent";
        if (campaign.getRecipients() == null) return "destinataires absents";
        var recipientsJson = gson.toJsonTree(campaign.getRecipients());
        if (!recipientsJson.isJsonObject()) return "destinataires invalides";
        JsonObject recipients = recipientsJson.getAsJsonObject();
        if (!recipients.has("lists") || !recipients.get("lists").isJsonArray()) return "liste destinataire absente";
        var lists = recipients.getAsJsonArray("lists");
        if (lists.size() != 1 || !lists.get(0).isJsonPrimitive() || lists.get(0).getAsLong() != listId) {
            return "liste destinataire différente de Newsletter ALOD ou listes multiples";
        }
        // Fail closed for segments, additional lists or exclusions: the displayed audience is exact.
        boolean exactAudience = recipients.entrySet().stream().allMatch(entry -> "lists".equals(entry.getKey())
                || entry.getValue().isJsonNull() || (entry.getValue().isJsonArray() && entry.getValue().getAsJsonArray().isEmpty()));
        return exactAudience ? null : "segments ou exclusions supplémentaires";
    }

    private void requireAllowed(GetEmailCampaign campaign) {
        if (!allowed(campaign)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Seules les campagnes destinées exclusivement à Newsletter ALOD sont autorisées.");
    }

    private String remoteStatus(GetEmailCampaign campaign) {
        return campaign.getStatus() == null ? "unknown" : campaign.getStatus().getValue();
    }

    private NewsletterCampaign view(GetEmailCampaign campaign, GetExtendedList list) {
        try {
            String version = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(gson.toJson(campaign).getBytes(StandardCharsets.UTF_8)));
            return new NewsletterCampaign(campaign.getId(), campaign.getName(), campaign.getSubject(), campaign.getPreviewText(),
                    campaign.getHtmlContent(), listId, list.getName(), list.getUniqueSubscribers(), remoteStatus(campaign), version);
        } catch (java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }

    private ResponseStatusException unavailable(String operation, ApiException exception) {
        int code = exception.getCode();
        // Never log SDK exception messages, URLs, response bodies or credentials.
        log.warn("Échec Brevo newsletter : opération={}, HTTP={}", operation, code);
        String advice = switch (code) {
            case 401, 403 -> "Vérifiez la clé API Brevo et ses autorisations.";
            case 404 -> "Vérifiez l'identifiant de la liste ou de la campagne dans Brevo.";
            case 429 -> "Quota Brevo atteint. Réessayez la consultation plus tard.";
            default -> "Vérifiez le statut avant toute nouvelle tentative d'envoi.";
        };
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Brevo : " + operation + " impossible (" + (code == 0 ? "erreur réseau" : "HTTP " + code) + "). " + advice);
    }

    private ResponseStatusException invalidResponse(String operation, RuntimeException exception) {
        log.warn("Réponse Brevo newsletter illisible : opération={}, type={}", operation, exception.getClass().getSimpleName());
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Réponse Brevo illisible pendant la " + operation + ".");
    }

    private ResponseStatusException unavailable() {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Brevo indisponible. Vérifiez le statut avant toute nouvelle tentative.");
    }
}
