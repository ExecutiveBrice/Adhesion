package com.wild.corp.adhesion.shop.payment.provider;

import com.wild.corp.adhesion.shop.common.money.Money;
import com.wild.corp.adhesion.shop.payment.model.PaymentStatus;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public final class FakePaymentProvider implements PaymentProvider {

    private final AtomicInteger sequence = new AtomicInteger();
    private final Map<String, PaymentSession> sessionsByIdempotencyKey = new HashMap<>();
    private final Map<String, PaymentResult> resultsByExternalId = new HashMap<>();
    private PaymentProviderException nextCreationFailure;

    @Override
    public PaymentProviderType type() {
        return PaymentProviderType.FAKE;
    }

    @Override
    public PaymentSession createPayment(PaymentRequest request) {
        if (nextCreationFailure != null) {
            PaymentProviderException failure = nextCreationFailure;
            nextCreationFailure = null;
            throw failure;
        }
        return sessionsByIdempotencyKey.computeIfAbsent(request.idempotencyKey(), ignored -> {
            String externalId = "fake-" + sequence.incrementAndGet();
            PaymentSession session = new PaymentSession(externalId,
                    URI.create("https://payments.example.test/" + externalId), PaymentStatus.PENDING);
            resultsByExternalId.put(externalId,
                    new PaymentResult(externalId, PaymentStatus.PENDING, request.amount(), null, null));
            return session;
        });
    }

    @Override
    public PaymentResult retrievePayment(String externalPaymentId) {
        PaymentResult result = resultsByExternalId.get(externalPaymentId);
        if (result == null) {
            throw new PaymentProviderException(type(), "PAYMENT_NOT_FOUND", "Paiement fake introuvable", false);
        }
        return result;
    }

    @Override
    public PaymentResult processNotification(PaymentNotification notification) {
        return retrievePayment(notification.payload());
    }

    public void markSucceeded(String externalPaymentId, Money amount) {
        resultsByExternalId.put(externalPaymentId,
                new PaymentResult(externalPaymentId, PaymentStatus.SUCCEEDED, amount, null, null));
    }

    public void failNextCreation(String errorCode, boolean retryable) {
        nextCreationFailure = new PaymentProviderException(type(), errorCode, "Indisponibilité simulée", retryable);
    }

    public int createdSessionCount() {
        return sequence.get();
    }
}
