package com.wild.corp.adhesion.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.regex.Pattern;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "adhesion.logging", name = "startup-parameters", havingValue = "true")
public class StartupConfigurationLogger {

    private static final List<String> PROPERTIES = List.of(
            "server.name", "server.port", "local.server.port", "server.app.jwtExpirationMs",
            "spring.main.allow-circular-references", "spring.h2.console.enabled", "spring.h2.console.path",
            "spring.datasource.driver-class-name", "spring.liquibase.enabled", "spring.liquibase.change-log",
            "spring.jpa.show-sql", "spring.jpa.generate-ddl", "spring.jpa.hibernate.ddl-auto",
            "spring.jpa.hibernate.show-sql", "spring.jpa.hibernate.format-sql",
            "spring.jpa.properties.hibernate.dialect", "springdoc.swagger-ui.path",
            "springdoc.swagger-ui.persist-authorization", "image-storage-dir",
            "logging.level.com.wild.corp", "logging.level.org.hibernate",
            "spring.cloud.openfeign.client.config.helloAssoCheckoutClient.connect-timeout",
            "spring.cloud.openfeign.client.config.helloAssoCheckoutClient.read-timeout",
            "spring.cloud.openfeign.client.config.helloAssoCheckoutClient.logger-level",
            "spring.cloud.openfeign.client.config.helloAssoOAuthClient.connect-timeout",
            "spring.cloud.openfeign.client.config.helloAssoOAuthClient.read-timeout",
            "spring.cloud.openfeign.client.config.helloAssoOAuthClient.logger-level",
            "adhesion.security.cors.allowed-origins", "adhesion.security.pwa.session-lifetime-days",
            "adhesion.security.password-reset.token-lifetime-minutes", "adhesion.security.password-reset.rate-limit.ip",
            "adhesion.security.password-reset.rate-limit.address", "adhesion.security.password-reset.rate-limit.window-seconds",
            "adhesion.security.password-reset.rate-limit.max-tracked-keys", "adhesion.calendrier.zone",
            "adhesion.calendrier.vacances-dataset", "adhesion.calendrier.jours-feries-dataset",
            "shop.order.expiry-scan-delay-ms", "shop.order.expiry-initial-delay-ms", "shop.payment.provider",
            "shop.payment.reconciliation.enabled", "shop.payment.reconciliation.fixed-delay-ms",
            "shop.payment.reconciliation.initial-delay-ms", "shop.payment.reconciliation.batch-size",
            "shop.payment.helloasso.organization-slug", "brevo.newsletter.list-id");

    private static final List<String> SECRETS = List.of(
            "spring.datasource.username", "spring.datasource.password", "server.app.jwtSecret",
            "shop.payment.helloasso.client-id", "shop.payment.helloasso.client-secret",
            "shop.payment.helloasso.webhook-signature-key", "brevo.newsletter.api-key", "BREVO_APIKEY_PRIVATE");

    private static final Pattern POSTGRESQL_ADDRESS =
            Pattern.compile("jdbc:postgresql://[\\w.:,\\[\\]-]+(?:/[\\w.-]+)?");

    private final Environment environment;

    public StartupConfigurationLogger(Environment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void logConfiguration() {
        if (!log.isInfoEnabled()) {
            return;
        }

        String[] profiles = environment.getActiveProfiles();
        if (profiles.length == 0) {
            profiles = environment.getDefaultProfiles();
        }
        log.info("Configuration effective au démarrage (profils Spring = {})", oneLine(String.join(", ", profiles)));
        PROPERTIES.forEach(key -> logProperty(key, UnaryOperator.identity()));
        logProperty("spring.datasource.url", StartupConfigurationLogger::databaseAddress);
        logProperty("shop.payment.helloasso.base-url", StartupConfigurationLogger::httpOrigin);
        logProperty("adhesion.calendrier.education-url", StartupConfigurationLogger::httpOrigin);
        logProperty("adhesion.calendrier.jours-feries-url", StartupConfigurationLogger::httpOrigin);
        SECRETS.forEach(key -> logProperty(key, StartupConfigurationLogger::maskedHint));
    }

    private void logProperty(String key, UnaryOperator<String> format) {
        String value;
        try {
            value = environment.getProperty(key);
        } catch (IllegalArgumentException exception) {
            log.info("{} = [non résolu]", key);
            return;
        }

        if (value != null) {
            log.info("{} = {}", key, oneLine(format.apply(value)));
        }
    }

    private static String databaseAddress(String value) {
        String address = value.split("\\?", 2)[0];
        return POSTGRESQL_ADDRESS.matcher(address).matches() ? withMaskedHint(value, address) : maskedHint(value);
    }

    private static String httpOrigin(String value) {
        try {
            URI uri = URI.create(value);
            if (uri.getHost() != null && ("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))) {
                String port = uri.getPort() == -1 ? "" : ":" + uri.getPort();
                return withMaskedHint(value, uri.getScheme() + "://" + uri.getHost() + port);
            }
        } catch (IllegalArgumentException exception) {
            return maskedHint(value);
        }
        return maskedHint(value);
    }

    private static String withMaskedHint(String value, String visiblePart) {
        return value.equals(visiblePart) ? visiblePart : visiblePart + " " + maskedHint(value);
    }

    private static String maskedHint(String value) {
        int length = value.codePointCount(0, value.length());
        if (length < 5) {
            return "[masqué, %d caractère(s)]".formatted(length);
        }

        String first = Character.toString(value.codePointAt(0));
        String last = Character.toString(value.codePointBefore(value.length()));
        return "[masqué, %d caractères, premier « %s », dernier « %s »]".formatted(length, first, last);
    }

    private static String oneLine(String value) {
        return value.replaceAll("\\p{C}|\\R", "?");
    }
}
