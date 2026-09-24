package com.wild.corp.adhesion.shop.payment.provider;

import java.util.Map;

public record PaymentNotification(String payload, Map<String, String> headers) {

    public PaymentNotification {
        if (payload == null) {
            throw new IllegalArgumentException("Le contenu de la notification est obligatoire");
        }
        headers = headers == null ? Map.of() : Map.copyOf(headers);
    }
}
