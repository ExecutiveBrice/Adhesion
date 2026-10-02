package com.wild.corp.adhesion.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/** Journalise la configuration effective chargée depuis les fichiers Spring, sans exposer de secrets. */
@Component
public class ApplicationStartupConfigurationLogger {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationStartupConfigurationLogger.class);

    private final ConfigurableEnvironment environment;

    public ApplicationStartupConfigurationLogger(ConfigurableEnvironment environment) {
        this.environment = environment;
    }

    @EventListener(ApplicationStartedEvent.class)
    public void logEffectiveConfiguration() {
        String profiles = environment.getActiveProfiles().length == 0
                ? "(default)"
                : String.join(", ", environment.getActiveProfiles());
        LOGGER.info("Configuration active : profils=[{}]", profiles);

        Map<String, String> effectiveProperties = new TreeMap<>();
        for (PropertySource<?> source : environment.getPropertySources()) {
            if (!isConfigurationFile(source) || !(source instanceof EnumerablePropertySource<?> enumerableSource)) {
                continue;
            }
            for (String propertyName : enumerableSource.getPropertyNames()) {
                // Les sources sont parcourues dans l'ordre de priorité Spring : la première valeur est la valeur effective.
                effectiveProperties.putIfAbsent(propertyName, environment.getProperty(propertyName));
            }
        }

        effectiveProperties.forEach((name, value) -> LOGGER.info("Configuration effective : {}={}",
                name, safeValue(name, value)));
    }

    private boolean isConfigurationFile(PropertySource<?> source) {
        return source.getName().startsWith("Config resource") || source.getName().startsWith("applicationConfig:");
    }

    private String safeValue(String name, String value) {
        if (value == null) {
            return "<null>";
        }
        String normalizedName = name.toLowerCase(Locale.ROOT);
        if (Arrays.stream(new String[]{"password", "secret", "token", "key", "credential", "authorization"})
                .anyMatch(normalizedName::contains) || value.matches(".*://[^/]*@.*")) {
            return "<masked>";
        }
        return value;
    }
}
