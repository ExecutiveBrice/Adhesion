package com.wild.corp.adhesion.shop.payment.gateway;

import com.wild.corp.adhesion.shop.common.money.Money;
import com.wild.corp.adhesion.shop.payment.model.PaymentStatus;

public record PaymentVerification(
        String externalPaymentId,
        PaymentStatus status,
        Money amount) {
}
