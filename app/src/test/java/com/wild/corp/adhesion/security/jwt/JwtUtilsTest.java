package com.wild.corp.adhesion.security.jwt;

import com.wild.corp.adhesion.models.UserDetails;
import io.jsonwebtoken.security.WeakKeyException;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilsTest {

    private static final String SECRET = "a-secure-signing-secret-with-at-least-64-bytes-for-the-HS512-algorithm";
    private static final Instant NOW = Instant.parse("2026-08-03T12:00:00Z");

    @Test
    void generatesAndReadsAValidToken() {
        JwtUtils jwtUtils = jwtUtils(60_000);
        UserDetails principal = new UserDetails(42L, "member@example.org", "secret", List.of());

        String token = jwtUtils.generateJwtToken(
                new UsernamePasswordAuthenticationToken(principal, principal.getPassword(), principal.getAuthorities()));

        assertThat(jwtUtils.validateJwtToken(token)).isTrue();
        assertThat(jwtUtils.getUserNameFromJwtToken(token)).isEqualTo("member@example.org");
    }

    @Test
    void rejectsExpiredAndTamperedTokens() {
        JwtUtils jwtUtils = jwtUtils(-1);
        UserDetails principal = new UserDetails(42L, "member@example.org", "secret", List.of());
        String token = jwtUtils.generateJwtToken(new UsernamePasswordAuthenticationToken(principal, "secret"));

        assertThat(jwtUtils.validateJwtToken(token)).isFalse();
        assertThat(jwtUtils(60_000).validateJwtToken(token + "invalid")).isFalse();
    }

    @Test
    void refusesAWeakSigningSecret() {
        assertThatThrownBy(() -> new JwtUtils("too-short", 60_000, Clock.systemUTC()))
                .isInstanceOf(WeakKeyException.class);
    }

    private JwtUtils jwtUtils(long expirationMs) {
        return new JwtUtils(SECRET, expirationMs, Clock.fixed(NOW, ZoneOffset.UTC));
    }
}
