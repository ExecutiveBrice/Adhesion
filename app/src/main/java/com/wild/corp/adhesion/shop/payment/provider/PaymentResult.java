package com.wild.corp.adhesion.shop.payment.provider;

import com.wild.corp.adhesion.shop.common.money.Money;
import com.wild.corp.adhesion.shop.payment.model.PaymentStatus;

import java.util.Objects;

public record PaymentResult(String externalPaymentId,
                            PaymentStatus status,
                            Money amount,
                            String failureCode,
                            String failureMessage) {

    public PaymentResult {
        if (externalPaymentId == null || externalPaymentId.isBlank()) {
            throw new IllegalArgumentException("L'identifiant externe du paiement est obligatoire");
        }
        externalPaymentId = externalPaymentId.trim();
        Objects.requireNonNull(status, "Le statut du paiement est obligatoire");
        Objects.requireNonNull(amount, "Le montant vérifié est obligatoire");
        failureCode = normalize(failureCode);
        failureMessage = normalize(failureMessage);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
