package com.wild.corp.adhesion.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "helloasso")
public class HelloAssoProperties {

    private String baseUrl = "https://api.helloasso.com/v5";
    private String oauthUrl = "https://api.helloasso.com/oauth2/token";
    private String organizationSlug;
    private String clientId;
    private String clientSecret;
}
