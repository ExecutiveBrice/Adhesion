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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ResponseStatusException;
import org.wild.corp.adhesion.client.helloasso.model.HelloAssoApiV5CommonModelsCartsCheckoutIntentResponse;
import org.wild.corp.adhesion.client.helloasso.model.HelloAssoApiV5CommonModelsCartsInitCheckoutBody;
import org.wild.corp.adhesion.client.helloasso.model.HelloAssoApiV5CommonModelsCartsInitCheckoutResponse;

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

        HelloAssoApiV5CommonModelsCartsInitCheckoutBody body = new HelloAssoApiV5CommonModelsCartsInitCheckoutBody();
        int amountInCents = adhesion.getTarif() * 100;
        body.setTotalAmount(amountInCents);
        body.setInitialAmount(amountInCents);
        body.setItemName("Adhesion " + adhesion.getActivite().getNom() + " #" + adhesion.getId());
        body.setBackUrl(request.getBackUrl());
        body.setErrorUrl(request.getErrorUrl());
        body.setReturnUrl(request.getReturnUrl());
        body.setContainsDonation( false);
        body.setMetadata((org.openapitools.jackson.nullable.JsonNullable<Object>) Map.of("adhesionId", adhesion.getId()));

        ResponseEntity<HelloAssoApiV5CommonModelsCartsInitCheckoutResponse> response = helloAssoCheckoutClient.organizationsOrganizationSlugCheckoutIntentsPost(
                properties.getOrganizationSlug(),
                body
        );

        Number id = response.getBody().getId();
        String redirectUrl = response.getBody().getRedirectUrl();

        return HelloAssoCheckoutResponse.builder()
                .checkoutIntentId(id == null ? null : id.intValue())
                .redirectUrl(redirectUrl)
                .build();
    }

    public HelloAssoApiV5CommonModelsCartsCheckoutIntentResponse getCheckoutIntent(Integer checkoutIntentId) {
        ResponseEntity<HelloAssoApiV5CommonModelsCartsCheckoutIntentResponse> response = helloAssoCheckoutClient.organizationsOrganizationSlugCheckoutIntentsCheckoutIntentIdGet(
                properties.getOrganizationSlug(),
                checkoutIntentId,
                false
        );
        return response.getBody();
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
