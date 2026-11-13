package com.wild.corp.adhesion.shop.api;

import com.wild.corp.adhesion.shop.api.dto.PaymentSessionRequest;
import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentSessionRequestTest {

    @Test
    void acceptsOnlyAbsoluteHttpUrlsForPaymentReturns() {
        assertThatCode(() -> new PaymentSessionRequest(URI.create("https://app.example/return"), URI.create("http://localhost/cancel")))
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> new PaymentSessionRequest(URI.create("javascript:alert(1)"), URI.create("https://app.example/cancel")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new PaymentSessionRequest(URI.create("/return"), URI.create("https://app.example/cancel")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
