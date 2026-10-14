package com.wild.corp.adhesion.services;

import java.time.Duration;

/** Sanitized failure: never retain the SDK exception, request URL or Brevo body. */
public class NewsletterApiException extends RuntimeException {
    private final int statusCode;
    private final Duration retryAfter;

    public NewsletterApiException(int statusCode, Duration retryAfter) {
        super("Brevo newsletter API unavailable (HTTP " + statusCode + ")");
        this.statusCode = statusCode;
        this.retryAfter = retryAfter;
    }

    public int getStatusCode() { return statusCode; }
    public Duration getRetryAfter() { return retryAfter; }
    public boolean isRetryable() { return statusCode == 0 || statusCode == 429 || statusCode >= 500; }
}
