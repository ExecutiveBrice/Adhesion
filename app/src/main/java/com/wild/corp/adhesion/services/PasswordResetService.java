package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.ConfirmationToken;
import com.wild.corp.adhesion.models.ConfirmationTokenType;
import com.wild.corp.adhesion.models.User;
import com.wild.corp.adhesion.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;

@Service
public class PasswordResetService {
    private final UserRepository userRepository;
    private final ConfirmationTokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetRateLimiter rateLimiter;
    private final PasswordResetWorker worker;

    public PasswordResetService(UserRepository userRepository,
                                ConfirmationTokenService tokenService,
                                PasswordEncoder passwordEncoder,
                                PasswordResetRateLimiter rateLimiter,
                                PasswordResetWorker worker) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.passwordEncoder = passwordEncoder;
        this.rateLimiter = rateLimiter;
        this.worker = worker;
    }

    public void request(String submittedAddress, String ipAddress) {
        String address = normalize(submittedAddress);
        String addressHash = ConfirmationTokenService.hash(address);
        if (!rateLimiter.allow(ipAddress, addressHash) || address.isBlank()) {
            return;
        }

        worker.issue(address);
    }

    @Transactional
    public void confirm(String rawToken, String newPassword) {
        Instant now = Instant.now();
        ConfirmationToken token = tokenService.consume(rawToken, ConfirmationTokenType.PASSWORD_RESET, now);
        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setSessionVersion(user.getSessionVersion() + 1);
        userRepository.saveAndFlush(user);
        tokenService.invalidateAll(user, ConfirmationTokenType.PASSWORD_RESET, now);
    }

    private String normalize(String address) {
        return address == null ? "" : address.strip().toLowerCase(Locale.ROOT);
    }
}
