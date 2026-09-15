package com.wild.corp.adhesion.services;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class PasswordResetRateLimiterTest {
    @Test
    void limitsIndependentlyByIpAndAddressHash() {
        PasswordResetRateLimiter limiter = new PasswordResetRateLimiter(
                2, 1, 3600, 100,
                Clock.fixed(Instant.parse("2026-09-01T10:00:00Z"), ZoneOffset.UTC));

        assertThat(limiter.allow("192.0.2.1", "address-a")).isTrue();
        assertThat(limiter.allow("192.0.2.2", "address-a")).isFalse();
        assertThat(limiter.allow("192.0.2.1", "address-b")).isTrue();
        assertThat(limiter.allow("192.0.2.1", "address-c")).isFalse();
    }
}
