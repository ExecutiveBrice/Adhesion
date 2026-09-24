package com.wild.corp.adhesion.shop.payment.gateway;

import com.wild.corp.adhesion.shop.payment.model.PaymentStatus;

public record PaymentInitiation(
        String externalPaymentId,
        String redirectUrl,
        PaymentStatus status) {
}
