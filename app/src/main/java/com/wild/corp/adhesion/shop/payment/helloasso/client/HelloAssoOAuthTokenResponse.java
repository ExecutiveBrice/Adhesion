package com.wild.corp.adhesion.shop.payment.helloasso.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record HelloAssoOAuthTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("expires_in") Long expiresIn) {
}
