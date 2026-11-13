package com.wild.corp.adhesion.shop.api.dto;

import jakarta.validation.constraints.NotNull;

import java.net.URI;

public record PaymentSessionRequest(@NotNull URI returnUrl, @NotNull URI cancelUrl) {

    public PaymentSessionRequest {
        requireHttpUrl(returnUrl, "returnUrl");
        requireHttpUrl(cancelUrl, "cancelUrl");
    }

    private static void requireHttpUrl(URI url, String field) {
        if (url == null || !url.isAbsolute()
                || (!"https".equalsIgnoreCase(url.getScheme()) && !"http".equalsIgnoreCase(url.getScheme()))) {
            throw new IllegalArgumentException(field + " doit être une URL HTTP(S) absolue");
        }
    }
}
