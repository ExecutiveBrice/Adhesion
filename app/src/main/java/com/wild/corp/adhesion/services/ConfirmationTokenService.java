package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.ConfirmationToken;
import com.wild.corp.adhesion.models.ConfirmationTokenType;
import com.wild.corp.adhesion.models.User;
import com.wild.corp.adhesion.repository.ConfirmationTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

@Service
public class ConfirmationTokenService {

    private static final int TOKEN_BYTES = 32;
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    private ConfirmationTokenRepository confirmationTokenRepository;

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void purgeLegacyTokens() {
        confirmationTokenRepository.deleteLegacyTokensWithoutHash();
    }

    @Transactional
    public String create(User user, ConfirmationTokenType type, Duration lifetime, Instant now) {
        byte[] randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);
        String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        ConfirmationToken token = new ConfirmationToken();
        token.setUser(user);
        token.setType(type);
        token.setTokenHash(hash(rawToken));
        token.setCreatedAt(now);
        token.setExpiresAt(now.plus(lifetime));
        confirmationTokenRepository.saveAndFlush(token);
        return rawToken;
    }

    @Transactional
    public ConfirmationToken consume(String rawToken, ConfirmationTokenType expectedType, Instant now) {
        String tokenHash = hash(rawToken);
        if (confirmationTokenRepository.consumeIfValid(tokenHash, expectedType, now, now) != 1) {
            throw new InvalidConfirmationTokenException();
        }
        return confirmationTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(InvalidConfirmationTokenException::new);
    }

    @Transactional
    public void invalidateAll(User user, ConfirmationTokenType type, Instant now) {
        confirmationTokenRepository.invalidateActiveForUser(user.getId(), type, now);
    }

    public static String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 indisponible", impossible);
        }
    }
}
