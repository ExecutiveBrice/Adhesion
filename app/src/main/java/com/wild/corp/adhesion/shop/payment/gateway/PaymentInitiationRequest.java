package com.wild.corp.adhesion.shop.payment.gateway;

import com.wild.corp.adhesion.shop.common.money.Money;

public record PaymentInitiationRequest(
        Long attemptId,
        String orderNumber,
        Money amount,
        String idempotencyKey) {
}
