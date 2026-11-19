package com.wild.corp.adhesion.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(OutputCaptureExtension.class)
class StartupConfigurationLoggerTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(StartupConfigurationLogger.class);

    @Test
    void requiresExplicitActivation() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(StartupConfigurationLogger.class));
    }

    @Test
    void logsResolvedConfigurationAndOnlyHintsForSecrets(CapturedOutput output) {
        contextRunner.withPropertyValues("adhesion.logging.startup-parameters=true")
                .withInitializer(context -> {
                    var environment = context.getEnvironment();
                    environment.setActiveProfiles("helloasso-sandbox");
                    var sources = environment.getPropertySources();
                    sources.addLast(new MapPropertySource("application.yml", Map.of(
                            "server.port", "8080",
                            "server.name", "${DNS_NAME}",
                            "brevo.newsletter.api-key", "${BREVO_APIKEY_PRIVATE}",
                            "adhesion.calendrier.education-url", "${MISSING_TEST_VALUE}")));
                    sources.replace(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                            new SystemEnvironmentPropertySource(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME,
                                    Map.of("SERVER_PORT", "8000", "DNS_NAME", "example.test",
                                            "BREVO_APIKEY_PRIVATE", "A-demo-secret-z",
                                            "SPRING_DATASOURCE_PASSWORD", "xy",
                                            "SPRING_DATASOURCE_URL", "jdbc:postgresql://db:5432/adhesion?password=jdbc-secret",
                                            "SHOP_PAYMENT_HELLOASSO_BASE_URL", "https://user:http-secret@helloasso.test/api",
                                            "UNRELATED_TOKEN", "unrelated-secret")));
                })
                .run(context -> context.publishEvent(new ApplicationReadyEvent(new SpringApplication(), new String[0],
                        context.getSourceApplicationContext(), Duration.ZERO)));

        assertThat(output).contains("profils Spring = helloasso-sandbox", "server.port = 8000",
                        "server.name = example.test",
                        "spring.datasource.url = jdbc:postgresql://db:5432/adhesion [masqué, 55 caractères, premier « j », dernier « t »]",
                        "shop.payment.helloasso.base-url = https://helloasso.test [masqué, 43 caractères, premier « h », dernier « i »]",
                        "adhesion.calendrier.education-url = [non résolu]",
                        "brevo.newsletter.api-key = [masqué, 15 caractères, premier « A », dernier « z »]",
                        "spring.datasource.password = [masqué, 2 caractère(s)]")
                .doesNotContain("server.port = 8080", "A-demo-secret-z", "= xy", "jdbc-secret", "http-secret",
                        "UNRELATED_TOKEN", "unrelated-secret");
    }

    @Test
    void logsHintsForFullyMaskedUrls(CapturedOutput output) {
        contextRunner.withPropertyValues("adhesion.logging.startup-parameters=true",
                        "spring.datasource.url=jdbc:h2:mem:secret",
                        "shop.payment.helloasso.base-url=Z-invalid-url !",
                        "adhesion.calendrier.education-url=https://education.test")
                .run(context -> context.publishEvent(new ApplicationReadyEvent(new SpringApplication(), new String[0],
                        context.getSourceApplicationContext(), Duration.ZERO)));

        assertThat(output).contains(
                        "spring.datasource.url = [masqué, 18 caractères, premier « j », dernier « t »]",
                        "shop.payment.helloasso.base-url = [masqué, 15 caractères, premier « Z », dernier « ! »]",
                        "adhesion.calendrier.education-url = https://education.test")
                .doesNotContain("jdbc:h2:mem:secret", "Z-invalid-url !", "https://education.test [masqué");
    }
}
