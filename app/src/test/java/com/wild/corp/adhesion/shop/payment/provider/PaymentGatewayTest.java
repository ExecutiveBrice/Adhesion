package com.wild.corp.adhesion.shop.payment.provider;

import com.wild.corp.adhesion.shop.common.money.Money;
import com.wild.corp.adhesion.shop.payment.config.PaymentProperties;
import com.wild.corp.adhesion.shop.payment.model.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentGatewayTest {

    private FakePaymentProvider provider;
    private PaymentGateway gateway;

    @BeforeEach
    void setUp() {
        PaymentProperties properties = new PaymentProperties();
        properties.setProvider(PaymentProviderType.FAKE);
        provider = new FakePaymentProvider();
        gateway = new PaymentGateway(properties, List.of(provider));
    }

    @Test
    void selectsConfiguredProviderAndPreservesProviderIdempotency() {
        PaymentRequest request = request("payment-key-1");

        PaymentSession first = gateway.createPayment(request);
        PaymentSession replay = gateway.createPayment(request);

        assertThat(replay).isEqualTo(first);
        assertThat(first.redirectUrl()).isEqualTo(URI.create("https://payments.example.test/fake-1"));
        assertThat(provider.createdSessionCount()).isEqualTo(1);
    }

    @Test
    void retrievesStatusAndProcessesProviderNotification() {
        PaymentRequest request = request("payment-key-2");
        PaymentSession session = gateway.createPayment(request);
        provider.markSucceeded(session.externalPaymentId(), request.amount());

        PaymentResult verified = gateway.retrievePayment(PaymentProviderType.FAKE, session.externalPaymentId());
        PaymentResult notified = gateway.processNotification(PaymentProviderType.FAKE,
                new PaymentNotification(session.externalPaymentId(), Map.of("X-Signature", "fake-signature")));

        assertThat(verified.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(notified).isEqualTo(verified);
    }

    @Test
    void exposesNormalizedProviderFailure() {
        provider.failNextCreation("TEMPORARY_UNAVAILABLE", true);

        assertThatThrownBy(() -> gateway.createPayment(request("payment-key-3")))
                .isInstanceOfSatisfying(PaymentProviderException.class, exception -> {
                    assertThat(exception.getProviderType()).isEqualTo(PaymentProviderType.FAKE);
                    assertThat(exception.getErrorCode()).isEqualTo("TEMPORARY_UNAVAILABLE");
                    assertThat(exception.isRetryable()).isTrue();
                });
    }

    private PaymentRequest request(String idempotencyKey) {
        return new PaymentRequest(12L, "CMD-2026-000012", new Money(3_000, "EUR"), idempotencyKey,
                URI.create("https://shop.example.test/orders/12"),
                URI.create("https://shop.example.test/cart"));
    }
}
