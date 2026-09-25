package com.wild.corp.adhesion.shop.payment.provider;

import com.wild.corp.adhesion.shop.common.money.Money;

import java.net.URI;
import java.util.List;
import java.util.Objects;

public record PaymentRequest(Long orderId,
                             String orderNumber,
                             Money amount,
                             String idempotencyKey,
                             URI returnUrl,
                             URI cancelUrl,
                             List<PaymentLine> lines,
                             PaymentPayer payer) {

    public PaymentRequest(Long orderId, String orderNumber, Money amount, String idempotencyKey,
                          URI returnUrl, URI cancelUrl) {
        this(orderId, orderNumber, amount, idempotencyKey, returnUrl, cancelUrl, List.of(), null);
    }

    public PaymentRequest(Long orderId, String orderNumber, Money amount, String idempotencyKey,
                          URI returnUrl, URI cancelUrl, List<PaymentLine> lines) {
        this(orderId, orderNumber, amount, idempotencyKey, returnUrl, cancelUrl, lines, null);
    }

    public PaymentRequest {
        Objects.requireNonNull(orderId, "La commande est obligatoire");
        orderNumber = requireText(orderNumber, "Le numéro de commande est obligatoire");
        Objects.requireNonNull(amount, "Le montant du paiement est obligatoire");
        idempotencyKey = requireText(idempotencyKey, "La clé d'idempotence est obligatoire");
        if (idempotencyKey.length() > 100) {
            throw new IllegalArgumentException("La clé d'idempotence ne peut pas dépasser 100 caractères");
        }
        Objects.requireNonNull(returnUrl, "L'URL de retour est obligatoire");
        Objects.requireNonNull(cancelUrl, "L'URL d'annulation est obligatoire");
        lines = List.copyOf(Objects.requireNonNull(lines, "Les lignes de commande sont obligatoires"));
    }

    private static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value.trim();
    }
}
