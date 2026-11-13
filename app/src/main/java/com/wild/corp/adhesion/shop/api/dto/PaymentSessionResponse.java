package com.wild.corp.adhesion.shop.api.dto;

import com.wild.corp.adhesion.shop.payment.model.PaymentStatus;
import com.wild.corp.adhesion.shop.payment.provider.PaymentSession;

public record PaymentSessionResponse(String redirectUrl, PaymentStatus status) {

    public static PaymentSessionResponse from(PaymentSession session) {
        return new PaymentSessionResponse(session.redirectUrl() == null ? null : session.redirectUrl().toASCIIString(), session.status());
    }
}
