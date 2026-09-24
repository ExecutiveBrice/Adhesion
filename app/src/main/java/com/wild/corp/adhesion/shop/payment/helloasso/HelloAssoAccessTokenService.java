package com.wild.corp.adhesion.shop.payment.helloasso;

import com.wild.corp.adhesion.shop.payment.helloasso.client.HelloAssoOAuthClient;
import com.wild.corp.adhesion.shop.payment.helloasso.client.HelloAssoOAuthTokenResponse;
import com.wild.corp.adhesion.shop.payment.provider.PaymentProviderException;
import com.wild.corp.adhesion.shop.payment.provider.PaymentProviderType;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.time.Clock;
import java.time.Instant;

/** Obtient et renouvelle les jetons OAuth sans jamais les écrire dans les journaux. */
@Service
public class HelloAssoAccessTokenService {

    private static final Logger LOGGER = LoggerFactory.getLogger(HelloAssoAccessTokenService.class);
    private static final long REFRESH_SKEW_SECONDS = 60;

    private final HelloAssoOAuthClient oauthClient;
    private final HelloAssoPaymentProperties properties;
    private final Clock clock;
    private volatile AccessToken currentToken;

    public HelloAssoAccessTokenService(HelloAssoOAuthClient oauthClient,
                                       HelloAssoPaymentProperties properties) {
        this(oauthClient, properties, Clock.systemUTC());
    }

    HelloAssoAccessTokenService(HelloAssoOAuthClient oauthClient,
                                HelloAssoPaymentProperties properties,
                                Clock clock) {
        this.oauthClient = oauthClient;
        this.properties = properties;
        this.clock = clock;
    }

    public String authorizationHeader() {
        AccessToken token = currentToken;
        if (token != null && token.isUsable(clock.instant())) {
            return "Bearer " + token.value();
        }
        synchronized (this) {
            token = currentToken;
            if (token == null || !token.isUsable(clock.instant())) {
                currentToken = refresh(token);
            }
            return "Bearer " + currentToken.value();
        }
    }

    public void invalidate() {
        currentToken = null;
    }

    private AccessToken refresh(AccessToken previous) {
        properties.requireCredentials();
        try {
            HelloAssoOAuthTokenResponse response = previous != null && previous.refreshToken() != null
                    ? oauthClient.issueToken(refreshForm(previous.refreshToken()))
                    : oauthClient.issueToken(clientCredentialsForm());
            return toAccessToken(response);
        } catch (FeignException exception) {
            if (previous != null && previous.refreshToken() != null
                    && (exception.status() == 400 || exception.status() == 401)) {
                // Un refresh token expiré ne met pas en jeu de paiement : une nouvelle authentification est sûre.
                return authenticateWithClientCredentials();
            }
            throw HelloAssoFailure.from(exception, "OAUTH_FAILED");
        }
    }

    private AccessToken authenticateWithClientCredentials() {
        try {
            return toAccessToken(oauthClient.issueToken(clientCredentialsForm()));
        } catch (FeignException exception) {
            throw HelloAssoFailure.from(exception, "OAUTH_FAILED");
        }
    }

    private AccessToken toAccessToken(HelloAssoOAuthTokenResponse response) {
        if (response == null || response.accessToken() == null || response.accessToken().isBlank()) {
            throw new PaymentProviderException(PaymentProviderType.HELLOASSO, "OAUTH_INVALID_RESPONSE",
                    "HelloAsso a retourné une réponse OAuth incomplète", false);
        }
        long lifetime = response.expiresIn() == null ? 1 : Math.max(1, response.expiresIn());
        Instant expiresAt = clock.instant().plusSeconds(lifetime);
        LOGGER.debug("Jeton OAuth HelloAsso obtenu (expiration dans {} secondes)", lifetime);
        return new AccessToken(response.accessToken(), response.refreshToken(), expiresAt);
    }

    private MultiValueMap<String, String> clientCredentialsForm() {
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        return form;
    }

    private MultiValueMap<String, String> refreshForm(String refreshToken) {
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("client_id", properties.getClientId());
        form.add("client_secret", properties.getClientSecret());
        form.add("refresh_token", refreshToken);
        return form;
    }

    private record AccessToken(String value, String refreshToken, Instant expiresAt) {
        boolean isUsable(Instant now) {
            return expiresAt.isAfter(now.plusSeconds(REFRESH_SKEW_SECONDS));
        }
    }
}
