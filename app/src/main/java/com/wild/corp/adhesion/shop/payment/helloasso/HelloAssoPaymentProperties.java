package com.wild.corp.adhesion.shop.payment.helloasso;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.URI;
import java.util.Objects;

/** Configuration non secrète en dépôt ; les identifiants sont fournis par l'environnement. */
@ConfigurationProperties(prefix = "shop.payment.helloasso")
public class HelloAssoPaymentProperties {

    private URI baseUrl = URI.create("https://api.helloasso-sandbox.com");
    private String clientId;
    private String clientSecret;
    private String organizationSlug;

    public URI getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(URI baseUrl) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "L'URL de base HelloAsso est obligatoire");
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getOrganizationSlug() {
        return organizationSlug;
    }

    public void setOrganizationSlug(String organizationSlug) {
        this.organizationSlug = organizationSlug;
    }

    public void requireCredentials() {
        requireNonBlank(clientId, "HELLOASSO_CLIENT_ID");
        requireNonBlank(clientSecret, "HELLOASSO_CLIENT_SECRET");
        requireNonBlank(organizationSlug, "HELLOASSO_ORGANIZATION_SLUG");
    }

    private void requireNonBlank(String value, String variableName) {
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("La variable de configuration " + variableName + " est obligatoire avec HelloAsso");
        }
    }
}
