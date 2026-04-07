package com.wild.corp.adhesion.services;

import com.wild.corp.adhesion.config.HelloAssoProperties;
import com.wild.corp.adhesion.models.Adhesion;
import com.wild.corp.adhesion.models.helloasso.HelloAssoCheckoutRequest;
import com.wild.corp.adhesion.models.helloasso.HelloAssoCheckoutResponse;
import com.wild.corp.adhesion.models.helloasso.HelloAssoTokenResponse;
import com.wild.corp.adhesion.repository.AdhesionRepository;
import com.wild.corp.adhesion.services.helloasso.HelloAssoAuthClient;
import com.wild.corp.adhesion.services.helloasso.HelloAssoCheckoutClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class HelloAssoService {

    private final HelloAssoProperties properties;
    private final AdhesionRepository adhesionRepository;
    private final HelloAssoAuthClient helloAssoAuthClient;
    private final HelloAssoCheckoutClient helloAssoCheckoutClient;

    private String accessToken;
    private Instant tokenExpiresAt;

    public HelloAssoCheckoutResponse createCheckoutIntent(HelloAssoCheckoutRequest request) {
        Adhesion adhesion = adhesionRepository.findById(request.getAdhesionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Adhesion not found: " + request.getAdhesionId()));

        Map<String, Object> body = new HashMap<>();
        int amountInCents = adhesion.getTarif() * 100;
        body.put("totalAmount", amountInCents);
        body.put("initialAmount", amountInCents);
        body.put("itemName", "Adhesion " + adhesion.getActivite().getNom() + " #" + adhesion.getId());
        body.put("backUrl", request.getBackUrl());
        body.put("errorUrl", request.getErrorUrl());
        body.put("returnUrl", request.getReturnUrl());
        body.put("containsDonation", false);
        body.put("metadata", Map.of("adhesionId", adhesion.getId()));

        Map<String, Object> payload = helloAssoCheckoutClient.createCheckoutIntent(
                bearerToken(),
                properties.getOrganizationSlug(),
                body
        );

        Number id = (Number) payload.get("id");
        String redirectUrl = (String) payload.get("redirectUrl");

        return HelloAssoCheckoutResponse.builder()
                .checkoutIntentId(id == null ? null : id.intValue())
                .redirectUrl(redirectUrl)
                .build();
    }

    public Map<String, Object> getCheckoutIntent(Integer checkoutIntentId) {
        return helloAssoCheckoutClient.getCheckoutIntent(
                bearerToken(),
                properties.getOrganizationSlug(),
                checkoutIntentId
        );
    }

    private String bearerToken() {
        return "Bearer " + getAccessToken();
    }

    private synchronized String getAccessToken() {
        validateConfiguration();
        if (accessToken != null && tokenExpiresAt != null && Instant.now().isBefore(tokenExpiresAt)) {
            return accessToken;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());

        HelloAssoTokenResponse tokenResponse = helloAssoAuthClient.getAccessToken(form);
        if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
            throw new IllegalStateException("Unable to retrieve HelloAsso access token");
        }

        accessToken = tokenResponse.getAccessToken();
        int expiresIn = tokenResponse.getExpiresIn() == null ? 3600 : tokenResponse.getExpiresIn();
        tokenExpiresAt = Instant.now().plusSeconds(Math.max(60, expiresIn - 30L));

        log.info("HelloAsso access token refreshed, expires in {} seconds", expiresIn);
        return accessToken;
    }

    private void validateConfiguration() {
        if (properties.getOrganizationSlug() == null || properties.getOrganizationSlug().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "HELLOASSO_ORGANIZATION_SLUG is missing");
        }
        if (properties.getClientId() == null || properties.getClientId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "HELLOASSO_CLIENT_ID is missing");
        }
        if (properties.getClientSecret() == null || properties.getClientSecret().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "HELLOASSO_CLIENT_SECRET is missing");
        }
    }
}
