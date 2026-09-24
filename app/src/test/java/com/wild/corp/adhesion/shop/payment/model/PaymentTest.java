package com.wild.corp.adhesion.shop.payment.model;

import com.wild.corp.adhesion.shop.common.exception.InvalidStatusTransitionException;
import com.wild.corp.adhesion.shop.common.money.Money;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentTest {

    @Test
    void recordsSuccessfulProviderAgnosticAttempt() {
        Payment payment = new Payment(12L, new Money(3_000, "EUR"));
        PaymentAttempt attempt = payment.startAttempt("provider-a", "attempt-1");
        attempt.markPending("external-42", "https://payment.example/42");

        payment.recordSucceeded(attempt);

        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(attempt.getStatus()).isEqualTo(PaymentStatus.SUCCEEDED);
    }

    @Test
    void forbidsNewAttemptAfterSuccess() {
        Payment payment = new Payment(12L, new Money(3_000, "EUR"));
        PaymentAttempt attempt = payment.startAttempt("provider-a", "attempt-1");
        attempt.markPending("external-42", null);
        payment.recordSucceeded(attempt);

        assertThatThrownBy(() -> payment.startAttempt("provider-b", "attempt-2"))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }
}
