package com.wild.corp.adhesion.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "API Adhésion",
                version = "v1",
                description = "Documentation des endpoints exposés par l'application Adhésion."
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT"
)
public class OpenApiConfiguration {

        @Bean
        public GroupedOpenApi allOpenApi() {
                String paths[] = {"/**"};
                return GroupedOpenApi.builder().group("1-all").pathsToMatch(paths)
                        .build();
        }

        @Bean
        public GroupedOpenApi ficheAdherentApi() {
                String paths[] = {
                        "/auth/signin",
                        "/tribu/getTribuByUuid",
                        "/user/getUserByMail"
                };
                return GroupedOpenApi.builder().group("2-FicheAdhérent").pathsToMatch(paths)
                        .build();
        }
}
