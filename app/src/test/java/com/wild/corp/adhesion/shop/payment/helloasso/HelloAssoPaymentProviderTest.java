package com.wild.corp.adhesion.shop.payment.helloasso;

import com.wild.corp.adhesion.shop.common.money.Money;
import com.wild.corp.adhesion.shop.payment.helloasso.client.HelloAssoCheckoutClient;
import com.wild.corp.adhesion.shop.payment.helloasso.client.HelloAssoOAuthClient;
import com.wild.corp.adhesion.shop.payment.helloasso.client.HelloAssoOAuthTokenResponse;
import com.wild.corp.adhesion.shop.payment.model.PaymentStatus;
import com.wild.corp.adhesion.shop.payment.provider.PaymentNotification;
import com.wild.corp.adhesion.shop.payment.provider.PaymentLine;
import com.wild.corp.adhesion.shop.payment.provider.PaymentPayer;
import com.wild.corp.adhesion.shop.payment.provider.PaymentProviderException;
import com.wild.corp.adhesion.shop.payment.provider.PaymentRequest;
import com.wild.corp.adhesion.shop.payment.provider.PaymentResult;
import com.wild.corp.adhesion.shop.payment.provider.PaymentSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.util.MultiValueMap;
import org.wild.corp.adhesion.client.helloasso.model.HelloAssoApiV5CommonModelsCartsInitCheckoutBody;
import org.wild.corp.adhesion.client.helloasso.model.HelloAssoApiV5CommonModelsCartsInitCheckoutResponse;
import org.wild.corp.adhesion.client.helloasso.model.HelloAssoApiV5CommonModelsEnumsPaymentState;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HelloAssoPaymentProviderTest {

    private HelloAssoCheckoutClient checkoutClient;
    private HelloAssoOAuthClient oauthClient;
    private HelloAssoPaymentProvider provider;

    @BeforeEach
    void setUp() {
        checkoutClient = mock(HelloAssoCheckoutClient.class);
        oauthClient = mock(HelloAssoOAuthClient.class);
        when(oauthClient.issueToken(any())).thenReturn(new HelloAssoOAuthTokenResponse("access-token", "refresh-token", 1800L));
        HelloAssoPaymentProperties properties = properties();
        provider = new HelloAssoPaymentProvider(checkoutClient,
                new HelloAssoAccessTokenService(oauthClient, properties), properties, new JsonMapper());
    }

    @Test
    void createsCheckoutIntentWithServerAmountAndReturnsRedirectUrl() {
        when(checkoutClient.createCheckoutIntent(anyString(), anyString(), any()))
                .thenReturn(new HelloAssoApiV5CommonModelsCartsInitCheckoutResponse()
                        .id(741).redirectUrl("https://checkout.helloasso.test/741"));

        PaymentSession session = provider.createPayment(new PaymentRequest(12L, "CMD-2026-00012",
                new Money(2_500, "EUR"), "idempotency-key", URI.create("https://site.test/return"),
                URI.create("https://site.test/cancel")));

        assertThat(session.externalPaymentId()).isEqualTo("741");
        assertThat(session.redirectUrl()).isEqualTo(URI.create("https://checkout.helloasso.test/741"));
        assertThat(session.status()).isEqualTo(PaymentStatus.PENDING);
        ArgumentCaptor<HelloAssoApiV5CommonModelsCartsInitCheckoutBody> body = ArgumentCaptor.forClass(
                HelloAssoApiV5CommonModelsCartsInitCheckoutBody.class);
        verify(checkoutClient).createCheckoutIntent(anyString(), anyString(), body.capture());
        assertThat(body.getValue().getTotalAmount()).isEqualTo(2_500);
        assertThat(body.getValue().getInitialAmount()).isEqualTo(2_500);
        assertThat(body.getValue().getMetadata().get()).isEqualTo(Map.of(
                "shopOrderId", 12L, "shopOrderNumber", "CMD-2026-00012", "idempotencyKey", "idempotency-key"));
        var json = JsonMapper.builder().findAndAddModules().build().valueToTree(body.getValue());
        assertThat(json.get("totalAmount").intValue()).isEqualTo(2_500);
        assertThat(json.get("initialAmount").intValue()).isEqualTo(2_500);
        assertThat(json.get("containsDonation").booleanValue()).isFalse();
        assertThat(json.get("metadata").get("shopOrderId").longValue()).isEqualTo(12L);
        assertThat(json.get("backUrl").asText()).isEqualTo("https://site.test/cancel");
        assertThat(json.get("errorUrl").asText()).isEqualTo("https://site.test/cancel");
        assertThat(json.get("returnUrl").asText()).isEqualTo("https://site.test/return");
        assertThat(json.has("terms")).isFalse();
        ArgumentCaptor<MultiValueMap<String, String>> tokenForm = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(oauthClient).issueToken(tokenForm.capture());
        assertThat(tokenForm.getValue().getFirst("grant_type")).isEqualTo("client_credentials");
    }

    @Test
    void rejectsHttpReturnUrlBeforeCallingHelloAsso() {
        var request = new PaymentRequest(12L, "CMD-2026-00012", new Money(2_500, "EUR"),
                "idempotency-key", URI.create("http://localhost:4200/return"),
                URI.create("http://localhost:4200/cancel"));

        assertThatThrownBy(() -> provider.createPayment(request))
                .isInstanceOf(PaymentProviderException.class)
                .extracting(exception -> ((PaymentProviderException) exception).getErrorCode())
                .isEqualTo("INSECURE_CHECKOUT_URL");
        verify(checkoutClient, never()).createCheckoutIntent(anyString(), anyString(), any());
    }

    @Test
    void describesEachProductAndQuantityInTheCheckoutLabel() {
        when(checkoutClient.createCheckoutIntent(anyString(), anyString(), any()))
                .thenReturn(new HelloAssoApiV5CommonModelsCartsInitCheckoutResponse()
                        .id(742).redirectUrl("https://checkout.helloasso.test/742"));

        provider.createPayment(new PaymentRequest(12L, "CMD-2026-00012", new Money(4_500, "EUR"),
                "idempotency-key", URI.create("https://site.test/return"),
                URI.create("https://site.test/cancel"), List.of(
                new PaymentLine("Tee-shirt", "M", 2),
                new PaymentLine("Tee-shirt", "M", 1),
                new PaymentLine("Mug", null, 3))));

        ArgumentCaptor<HelloAssoApiV5CommonModelsCartsInitCheckoutBody> body = ArgumentCaptor.forClass(
                HelloAssoApiV5CommonModelsCartsInitCheckoutBody.class);
        verify(checkoutClient).createCheckoutIntent(anyString(), anyString(), body.capture());
        assertThat(body.getValue().getItemName())
                .isEqualTo("Commande CMD-2026-00012 : Tee-shirt (M) x 3 ; Mug x 3");
        assertThat(body.getValue().getTotalAmount()).isEqualTo(4_500);
    }

    @Test
    void prefillsTheCheckoutPayerFromTheAccount() {
        when(checkoutClient.createCheckoutIntent(anyString(), anyString(), any()))
                .thenReturn(new HelloAssoApiV5CommonModelsCartsInitCheckoutResponse()
                        .id(743).redirectUrl("https://checkout.helloasso.test/743"));

        provider.createPayment(new PaymentRequest(12L, "CMD-2026-00012", new Money(2_500, "EUR"),
                "idempotency-key", URI.create("https://site.test/return"),
                URI.create("https://site.test/cancel"), List.of(),
                new PaymentPayer("Alice", "Martin", "alice@example.org")));

        ArgumentCaptor<HelloAssoApiV5CommonModelsCartsInitCheckoutBody> body = ArgumentCaptor.forClass(
                HelloAssoApiV5CommonModelsCartsInitCheckoutBody.class);
        verify(checkoutClient).createCheckoutIntent(anyString(), anyString(), body.capture());
        assertThat(body.getValue().getPayer().getFirstName().get()).isEqualTo("Alice");
        assertThat(body.getValue().getPayer().getLastName().get()).isEqualTo("Martin");
        assertThat(body.getValue().getPayer().getEmail().get()).isEqualTo("alice@example.org");
        var json = JsonMapper.builder().findAndAddModules().build().valueToTree(body.getValue());
        assertThat(json.get("payer").get("firstName").asText()).isEqualTo("Alice");
        assertThat(json.get("payer").get("lastName").asText()).isEqualTo("Martin");
        assertThat(json.get("payer").get("email").asText()).isEqualTo("alice@example.org");
    }

    @Test
    void omitsInvalidOptionalNamesWithoutBlockingCheckout() {
        when(checkoutClient.createCheckoutIntent(anyString(), anyString(), any()))
                .thenReturn(new HelloAssoApiV5CommonModelsCartsInitCheckoutResponse()
                        .id(744).redirectUrl("https://checkout.helloasso.test/744"));

        provider.createPayment(new PaymentRequest(12L, "CMD-2026-00012", new Money(2_500, "EUR"),
                "idempotency-key", URI.create("https://site.test/return"),
                URI.create("https://site.test/cancel"), List.of(),
                new PaymentPayer("A", "Test", "alice@example.org")));

        ArgumentCaptor<HelloAssoApiV5CommonModelsCartsInitCheckoutBody> body = ArgumentCaptor.forClass(
                HelloAssoApiV5CommonModelsCartsInitCheckoutBody.class);
        verify(checkoutClient).createCheckoutIntent(anyString(), anyString(), body.capture());
        assertThat(body.getValue().getPayer().getFirstName().isPresent()).isFalse();
        assertThat(body.getValue().getPayer().getLastName().isPresent()).isFalse();
        assertThat(body.getValue().getPayer().getEmail().get()).isEqualTo("alice@example.org");
    }

    @Test
    void acceptsPaymentOnlyWhenCheckoutReadShowsAuthorizedPayment() {
        when(checkoutClient.getCheckoutIntent(anyString(), anyString(), anyString()))
                .thenReturn(checkoutWith(HelloAssoApiV5CommonModelsEnumsPaymentState.AUTHORIZED, 2_500));

        PaymentResult result = provider.retrievePayment("741");

        assertThat(result.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        assertThat(result.amount()).isEqualTo(new Money(2_500, "EUR"));
    }

    @Test
    void keepsCheckoutPendingWhenHelloAssoHasNotPublishedTheOrderAmount() {
        when(checkoutClient.getCheckoutIntent(anyString(), anyString(), anyString()))
                .thenReturn("{\"id\":741,\"order\":{\"payments\":[{\"state\":\"Pending\"}]}}");

        assertThat(provider.retrievePayment("741").status()).isEqualTo(PaymentStatus.PENDING);
    }

    @Test
    void refusesToConfirmAuthorizedCheckoutWithoutVerifiedAmount() {
        when(checkoutClient.getCheckoutIntent(anyString(), anyString(), anyString()))
                .thenReturn("{\"id\":741,\"order\":{\"payments\":[{\"state\":\"Authorized\"}]}}");

        assertThatThrownBy(() -> provider.retrievePayment("741"))
                .isInstanceOf(PaymentProviderException.class)
                .extracting(exception -> ((PaymentProviderException) exception).getErrorCode())
                .isEqualTo("INVALID_PROVIDER_RESPONSE");
    }

    @Test
    void notificationOnlyProvidesAnIntentIdentifierThenForcesServerVerification() {
        when(checkoutClient.getCheckoutIntent(anyString(), anyString(), anyString()))
                .thenReturn(checkoutWith(HelloAssoApiV5CommonModelsEnumsPaymentState.AUTHORIZED, 2_500));

        PaymentResult result = provider.processNotification(new PaymentNotification(
                "{\"data\":{\"order\":{\"checkoutIntentId\":741},\"code\":\"success\"}}", Map.of()));

        assertThat(result.status()).isEqualTo(PaymentStatus.SUCCEEDED);
        verify(checkoutClient).getCheckoutIntent(anyString(), anyString(), org.mockito.Mockito.eq("741"));
    }

    @Test
    void notificationWithoutCheckoutIdentifierIsRejectedWithoutCallingHelloAsso() {
        assertThatThrownBy(() -> provider.processNotification(new PaymentNotification("{\"success\":true}", Map.of())))
                .isInstanceOf(PaymentProviderException.class)
                .extracting(exception -> ((PaymentProviderException) exception).getErrorCode())
                .isEqualTo("INVALID_NOTIFICATION");

        verify(checkoutClient, never()).getCheckoutIntent(anyString(), anyString(), anyString());
    }

    @Test
    void malformedNotificationIsRejectedWithoutCallingHelloAsso() {
        assertThatThrownBy(() -> provider.processNotification(new PaymentNotification("{", Map.of())))
                .isInstanceOf(PaymentProviderException.class)
                .extracting(exception -> ((PaymentProviderException) exception).getErrorCode())
                .isEqualTo("INVALID_NOTIFICATION");

        verify(checkoutClient, never()).getCheckoutIntent(anyString(), anyString(), anyString());
    }

    @Test
    void abandonedCheckoutIsNotMistakenForPaid() {
        when(checkoutClient.getCheckoutIntent(anyString(), anyString(), anyString()))
                .thenReturn(checkoutWith(HelloAssoApiV5CommonModelsEnumsPaymentState.ABANDONED, 2_500));

        assertThat(provider.retrievePayment("741").status()).isEqualTo(PaymentStatus.CANCELLED);
    }

    @Test
    void renewsAnExpiringOAuthTokenWithItsRefreshToken() {
        HelloAssoOAuthClient shortLivedOAuthClient = mock(HelloAssoOAuthClient.class);
        when(shortLivedOAuthClient.issueToken(any()))
                .thenReturn(new HelloAssoOAuthTokenResponse("first-token", "refresh-token", 1L))
                .thenReturn(new HelloAssoOAuthTokenResponse("second-token", "new-refresh-token", 1800L));
        HelloAssoAccessTokenService tokenService = new HelloAssoAccessTokenService(shortLivedOAuthClient, properties());

        tokenService.authorizationHeader();
        tokenService.authorizationHeader();

        ArgumentCaptor<MultiValueMap<String, String>> tokenForm = ArgumentCaptor.forClass(MultiValueMap.class);
        verify(shortLivedOAuthClient, org.mockito.Mockito.times(2)).issueToken(tokenForm.capture());
        assertThat(tokenForm.getAllValues().get(1).getFirst("grant_type")).isEqualTo("refresh_token");
        assertThat(tokenForm.getAllValues().get(1).getFirst("refresh_token")).isEqualTo("refresh-token");
    }

    private String checkoutWith(
            HelloAssoApiV5CommonModelsEnumsPaymentState state, int total) {
        return """
                {"id":741,"metadata":{"shopOrderNumber":"CMD-2026-000007"},
                 "order":{"date":"2026-09-25T13:45:15.123+00:00",
                          "payments":[{"state":"%s","date":"2026-09-25T13:45:15.123+00:00"}],
                          "amount":{"total":%d}}}
                """.formatted(state.getValue(), total);
    }

    private HelloAssoPaymentProperties properties() {
        HelloAssoPaymentProperties properties = new HelloAssoPaymentProperties();
        properties.setClientId("client-id");
        properties.setClientSecret("client-secret");
        properties.setOrganizationSlug("association-test");
        return properties;
    }
}
