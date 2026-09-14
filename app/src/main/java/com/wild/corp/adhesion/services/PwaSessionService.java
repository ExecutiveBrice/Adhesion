package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.PwaSessionToken;
import com.wild.corp.adhesion.models.User;
import com.wild.corp.adhesion.models.UserDetails;
import com.wild.corp.adhesion.repository.PwaSessionTokenRepository;
import com.wild.corp.adhesion.repository.UserRepository;
import com.wild.corp.adhesion.security.jwt.JwtUtils;
import com.wild.corp.adhesion.security.payload.response.JwtResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class PwaSessionService {
    private final PwaSessionTokenRepository tokenRepository;
    private final UserRepository users;
    private final JwtUtils jwtUtils;
    private final Duration lifetime;
    private final SecureRandom random = new SecureRandom();

    public PwaSessionService(PwaSessionTokenRepository tokenRepository,
                             UserRepository users, JwtUtils jwtUtils,
                             @Value("${adhesion.security.pwa.session-lifetime-days:90}") long lifetimeDays) {
        this.tokenRepository = tokenRepository;
        this.users = users;
        this.jwtUtils = jwtUtils;
        this.lifetime = Duration.ofDays(lifetimeDays);
    }

    @Transactional
    public String issue(UserDetails principal) {
        User user = users.findById(principal.getId()).orElseThrow(PwaSessionService::expiredSession);
        if (user.getSessionVersion() != principal.getSessionVersion()) {
            throw expiredSession();
        }
        return create(user, Instant.now());
    }

    @Transactional
    public JwtResponse refresh(String rawToken) {
        Instant now = Instant.now();
        String hash = ConfirmationTokenService.hash(rawToken);
        if (tokenRepository.consumeIfValid(hash, now) != 1) {
            throw expiredSession();
        }
        PwaSessionToken session = tokenRepository.findByTokenHash(hash).orElseThrow(PwaSessionService::expiredSession);
        User user = session.getUser();
        if (session.getSessionVersion() != user.getSessionVersion()) {
            throw expiredSession();
        }
        UserDetails principal = UserDetails.build(user);
        var authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        JwtResponse response = new JwtResponse(jwtUtils.generateJwtToken(authentication), principal.getId(),
                principal.getUsername(), principal.isEnabled(),
                principal.getAuthorities().stream().map(authority -> authority.getAuthority()).toList());
        // Chaque renouvellement remplace le jeton précédent, utilisable une seule fois.
        response.setRefreshToken(create(user, now));
        return response;
    }

    @Transactional
    public void revoke(String rawToken) {
        Instant now = Instant.now();
        tokenRepository.consumeIfValid(ConfirmationTokenService.hash(rawToken), now);
    }

    private String create(User user, Instant now) {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String secret = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        PwaSessionToken session = new PwaSessionToken();
        session.setUser(user);
        session.setTokenHash(ConfirmationTokenService.hash(secret));
        session.setSessionVersion(user.getSessionVersion());
        session.setExpiresAt(now.plus(lifetime));
        tokenRepository.saveAndFlush(session);
        return secret;
    }

    private static ResponseStatusException expiredSession() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "La session mémorisée est invalide ou expirée");
    }
}
