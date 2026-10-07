package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.models.ConfirmationTokenType;
import com.wild.corp.adhesion.models.EmailContent;
import com.wild.corp.adhesion.models.User;
import com.wild.corp.adhesion.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class PasswordResetWorker {
    private final UserRepository userRepository;
    private final ConfirmationTokenService tokenService;
    private final EmailService emailService;
    private final String frontendBaseUrl;
    private final Duration tokenLifetime;

    public PasswordResetWorker(UserRepository userRepository,
                               ConfirmationTokenService tokenService,
                               EmailService emailService,
                               @Value("${adhesion.frontend.base-url:http://localhost:4200}") String frontendBaseUrl,
                               @Value("${adhesion.security.password-reset.token-lifetime-minutes:30}") long lifetimeMinutes) {
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.emailService = emailService;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
        this.tokenLifetime = Duration.ofMinutes(lifetimeMinutes);
    }

    @Async("passwordResetExecutor")
    @Transactional
    public void issue(String normalizedAddress) {
        userRepository.findByUsernameForUpdate(normalizedAddress).ifPresent(user -> {
            Instant now = Instant.now();
            tokenService.invalidateAll(user, ConfirmationTokenType.PASSWORD_RESET, now);
            String rawToken = tokenService.create(
                    user, ConfirmationTokenType.PASSWORD_RESET, tokenLifetime, now);
            sendResetEmail(user, rawToken);
        });
    }

    private void sendResetEmail(User user, String rawToken) {
        EmailContent message = new EmailContent();
        message.getDestinataires().add(user.getUsername());
        message.setSubject("Réinitialisation du mot de passe");
        message.setText("Bonjour,<br>" +
                "Ceci est le <a href=\"" + frontendBaseUrl + "/resetPassword/" + rawToken +
                "\">lien de renouvellement de votre mot de passe</a><br>" +
                "Cordialement,<br>" +
                "l'équipe de l'ALOD");
        emailService.sendMessage(message);
    }
}
