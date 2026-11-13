package com.wild.corp.adhesion.shop.payment.provider;

import com.wild.corp.adhesion.shop.payment.model.PaymentStatus;

import java.net.URI;

public record PaymentSession(String externalPaymentId,
                             URI redirectUrl,
                             PaymentStatus status) {

    public PaymentSession {
        if (externalPaymentId == null || externalPaymentId.isBlank()) {
            throw new IllegalArgumentException("L'identifiant externe du paiement est obligatoire");
        }
        externalPaymentId = externalPaymentId.trim();
        if (status != PaymentStatus.PENDING && status != PaymentStatus.SUCCEEDED) {
            throw new IllegalArgumentException("Une session doit être en attente ou déjà payée");
        }
    }
}
