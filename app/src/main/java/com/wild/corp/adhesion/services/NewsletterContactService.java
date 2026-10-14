package com.wild.corp.adhesion.services;

import brevo.ApiException;
import brevo.ApiResponse;
import brevoApi.ContactsApi;
import brevoModel.CreateContact;
import brevoModel.UpdateContact;
import brevoModel.GetExtendedContactDetails;
import com.google.gson.JsonParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/** Brevo owns subscription state. No local account, role or subscription record is created. */
@Service
public class NewsletterContactService {
    public enum Status { NOT_FOUND, NOT_SUBSCRIBED, SUBSCRIBED, UNSUBSCRIBED, BLOCKED }
    public enum SubscriptionRequest { ALREADY_SUBSCRIBED, SUBSCRIBED, RESUBSCRIPTION_REQUIRED }
    public enum AdminSubscription { SUBSCRIBED, RESUBSCRIBED, ALREADY_SUBSCRIBED, STILL_UNSUBSCRIBED }

    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private final ContactsApi api;
    private final String apiKey;
    private final long listId;
    private Instant blockedUntil = Instant.EPOCH;
    private long nextRequestNanos;

    public NewsletterContactService(ContactsApi api,
            @Value("${brevo.newsletter.api-key:}") String apiKey,
            @Value("${brevo.newsletter.list-id:0}") long listId) {
        this.api = api;
        this.apiKey = apiKey;
        this.listId = listId;
    }

    public Status getStatus(String email) {
        requireConfiguration();
        String normalized = normalize(email);
        try {
            GetExtendedContactDetails contact = call(() ->
                    api.getContactInfoWithHttpInfo(normalized, "email_id", null, null));
            if (contact == null || contact.isEmailBlacklisted() == null || contact.getListIds() == null) {
                throw new NewsletterApiException(502, Duration.ZERO);
            }
            if (contact.isEmailBlacklisted()) return Status.BLOCKED;
            if (contact.getListUnsubscribed() != null && contact.getListUnsubscribed().contains(listId)) {
                return Status.UNSUBSCRIBED;
            }
            return contact.getListIds().contains(listId) ? Status.SUBSCRIBED : Status.NOT_SUBSCRIBED;
        } catch (NewsletterApiException exception) {
            if (exception.getStatusCode() == 404) return Status.NOT_FOUND;
            throw exception;
        }
    }

    /** Create only a missing contact, without enrolling it or changing any opt-out. */
    public Status createContact(String email) {
        String normalized = normalize(email);
        Status status = getStatus(normalized);
        if (status != Status.NOT_FOUND) return status;
        try {
            call(() -> api.createContactWithHttpInfo(new CreateContact().email(normalized).updateEnabled(false)));
        } catch (NewsletterApiException exception) {
            // Another request may have created this address after our GET. Read its actual state.
            if (exception.getStatusCode() != 409) throw exception;
        }
        return getStatus(normalized);
    }

    /** Invoke only for an explicit subscription request with an already verified email.
     * Existing opt-outs require a separate explicit resubscription flow; never unblock here. */
    public SubscriptionRequest addToNewsletter(String email) {
        String normalized = normalize(email);
        Status status = createContact(normalized);
        if (status == Status.SUBSCRIBED) return SubscriptionRequest.ALREADY_SUBSCRIBED;
        if (status == Status.BLOCKED || status == Status.UNSUBSCRIBED) return SubscriptionRequest.RESUBSCRIPTION_REQUIRED;
        if (status != Status.NOT_SUBSCRIBED) throw new NewsletterApiException(502, Duration.ZERO);
        // Append only this list. Do not alter blacklist flags, other lists or opt-out history.
        call(() -> api.updateContactWithHttpInfo(normalized,
                new UpdateContact().listIds(List.of(listId)), "email_id"));
        Status updated = getStatus(normalized);
        if (updated == Status.BLOCKED || updated == Status.UNSUBSCRIBED) return SubscriptionRequest.RESUBSCRIPTION_REQUIRED;
        if (updated != Status.SUBSCRIBED) throw new NewsletterApiException(502, Duration.ZERO);
        return SubscriptionRequest.SUBSCRIBED;
    }

    /** Synchronize contact existence only. Membership and opt-outs remain entirely in Brevo. */
    public Map<String, Status> synchronize(Collection<String> emails) {
        LinkedHashSet<String> unique = new LinkedHashSet<>();
        // Validate the whole batch before making any request.
        for (String email : emails) unique.add(normalize(email));
        Map<String, Status> result = new LinkedHashMap<>();
        for (String email : unique) result.put(email, createContact(email));
        return Collections.unmodifiableMap(result);
    }

    /** Explicit administrator operation: the email blacklist is global in Brevo. */
    public AdminSubscription subscribeFromAdministration(String email) {
        String normalized = normalize(email);
        Status before = createContact(normalized);
        if (before == Status.SUBSCRIBED) return AdminSubscription.ALREADY_SUBSCRIBED;
        if (before == Status.NOT_FOUND) throw new NewsletterApiException(502, Duration.ZERO);
        call(() -> api.updateContactWithHttpInfo(normalized,
                new UpdateContact().listIds(List.of(listId)).emailBlacklisted(false), "email_id"));
        Status after = getStatus(normalized);
        // A list-specific opt-out may remain: never report success without verifying Brevo.
        if (after == Status.UNSUBSCRIBED) return AdminSubscription.STILL_UNSUBSCRIBED;
        if (after != Status.SUBSCRIBED) throw new NewsletterApiException(502, Duration.ZERO);
        return before == Status.BLOCKED || before == Status.UNSUBSCRIBED
                ? AdminSubscription.RESUBSCRIBED : AdminSubscription.SUBSCRIBED;
    }

    /** Explicit request from the authenticated owner of a verified address. */
    public Status subscribeByOwner(String email) {
        return subscribeFromAdministration(email) == AdminSubscription.STILL_UNSUBSCRIBED
                ? Status.UNSUBSCRIBED : Status.SUBSCRIBED;
    }

    /** Remove only our newsletter list, keeping the contact and its other subscriptions. */
    public Status unsubscribeFromNewsletter(String email) {
        String normalized = normalize(email);
        Status before = getStatus(normalized);
        if (before == Status.NOT_FOUND || before == Status.NOT_SUBSCRIBED) return before;
        try {
            call(() -> api.updateContactWithHttpInfo(normalized,
                    new UpdateContact().unlinkListIds(List.of(listId)), "email_id"));
        } catch (NewsletterApiException exception) {
            if (exception.getStatusCode() == 404) return Status.NOT_FOUND;
            throw exception;
        }
        Status after = getStatus(normalized);
        if (after == Status.SUBSCRIBED) throw new NewsletterApiException(502, Duration.ZERO);
        return after;
    }

    private void requireConfiguration() {
        if (apiKey == null || apiKey.isBlank() || listId <= 0) {
            throw new IllegalStateException("Configure BREVO_APIKEY_PRIVATE and BREVO_NEWSLETTER_LIST_ID");
        }
    }

    static String normalize(String email) {
        if (email == null) throw new IllegalArgumentException("Newsletter email is required");
        String normalized = email.strip().toLowerCase(Locale.ROOT);
        if (normalized.length() > 254 || !EMAIL.matcher(normalized).matches()) {
            throw new IllegalArgumentException("Invalid newsletter email");
        }
        return normalized;
    }

    @FunctionalInterface
    private interface ApiCall<T> { ApiResponse<T> execute() throws ApiException; }

    // Serialize and space contacts calls below the general 10 requests/second limit.
    // Quota failures stop the caller; no replay of POSTs with an uncertain outcome.
    private synchronized <T> T call(ApiCall<T> operation) {
        Instant now = Instant.now();
        if (now.isBefore(blockedUntil)) {
            throw new NewsletterApiException(429, Duration.between(now, blockedUntil));
        }
        long waitNanos = nextRequestNanos - System.nanoTime();
        if (waitNanos > 0) {
            try { TimeUnit.NANOSECONDS.sleep(waitNanos); }
            catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new NewsletterApiException(0, Duration.ZERO);
            }
        }
        nextRequestNanos = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(110);
        try {
            ApiResponse<T> response = operation.execute();
            if ("0".equals(header(response.getHeaders(), "x-sib-ratelimit-remaining"))) {
                blockedUntil = Instant.now().plus(retryDelay(response.getHeaders()));
            }
            return response.getData();
        } catch (ApiException exception) {
            Duration delay = exception.getCode() == 429 ? retryDelay(exception.getResponseHeaders()) : Duration.ZERO;
            if (exception.getCode() == 429) blockedUntil = Instant.now().plus(delay);
            int code = exception.getCode();
            if (code == 400 && isDuplicate(exception.getResponseBody())) code = 409;
            throw new NewsletterApiException(code, delay);
        } catch (com.google.gson.JsonParseException exception) {
            throw new NewsletterApiException(502, Duration.ZERO);
        }
    }

    private static boolean isDuplicate(String body) {
        try { return "duplicate_parameter".equals(JsonParser.parseString(body).getAsJsonObject().get("code").getAsString()); }
        catch (RuntimeException exception) { return false; }
    }

    private static Duration retryDelay(Map<String, List<String>> headers) {
        long seconds = 0;
        for (String name : List.of("Retry-After", "x-sib-ratelimit-reset")) {
            String value = header(headers, name);
            if (value == null) continue;
            try { seconds = Math.max(seconds, Long.parseLong(value)); }
            catch (NumberFormatException exception) {
                if (name.equals("Retry-After")) {
                    try {
                        seconds = Math.max(seconds, Duration.between(Instant.now(),
                                ZonedDateTime.parse(value, DateTimeFormatter.RFC_1123_DATE_TIME).toInstant()).getSeconds());
                    } catch (RuntimeException ignored) { /* Use the conservative default below. */ }
                }
            }
        }
        return Duration.ofSeconds(seconds > 0 ? seconds : 60);
    }

    private static String header(Map<String, List<String>> headers, String name) {
        if (headers == null) return null;
        return headers.entrySet().stream().filter(entry -> entry.getKey().equalsIgnoreCase(name))
                .map(Map.Entry::getValue).filter(values -> values != null && !values.isEmpty())
                .map(values -> values.getFirst()).findFirst().orElse(null);
    }
}
