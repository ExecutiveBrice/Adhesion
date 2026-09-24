package com.wild.corp.adhesion.shop.payment.helloasso;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wild.corp.adhesion.shop.common.money.Money;
import com.wild.corp.adhesion.shop.payment.helloasso.client.HelloAssoCheckoutClient;
import com.wild.corp.adhesion.shop.payment.model.PaymentStatus;
import com.wild.corp.adhesion.shop.payment.provider.PaymentNotification;
import com.wild.corp.adhesion.shop.payment.provider.PaymentProvider;
import com.wild.corp.adhesion.shop.payment.provider.PaymentProviderException;
import com.wild.corp.adhesion.shop.payment.provider.PaymentProviderType;
import com.wild.corp.adhesion.shop.payment.provider.PaymentRequest;
import com.wild.corp.adhesion.shop.payment.provider.PaymentResult;
import com.wild.corp.adhesion.shop.payment.provider.PaymentSession;
import feign.FeignException;
import org.openapitools.jackson.nullable.JsonNullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.wild.corp.adhesion.client.helloasso.model.HelloAssoApiV5CommonModelsCartsCheckoutIntentResponse;
import org.wild.corp.adhesion.client.helloasso.model.HelloAssoApiV5CommonModelsCartsInitCheckoutBody;
import org.wild.corp.adhesion.client.helloasso.model.HelloAssoApiV5CommonModelsCartsInitCheckoutResponse;
import org.wild.corp.adhesion.client.helloasso.model.HelloAssoApiV5CommonModelsEnumsPaymentState;
import org.wild.corp.adhesion.client.helloasso.model.HelloAssoApiV5CommonModelsStatisticsOrderDetail;
import org.wild.corp.adhesion.client.helloasso.model.HelloAssoApiV5CommonModelsStatisticsOrderPayment;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/** Adaptateur HelloAsso : toutes les confirmations passent par une lecture serveur de l'intent. */
@Component
@ConditionalOnProperty(prefix = "shop.payment", name = "provider", havingValue = "helloasso")
public class HelloAssoPaymentProvider implements PaymentProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(HelloAssoPaymentProvider.class);
    private static final String CURRENCY = "EUR";

    private final HelloAssoCheckoutClient checkoutClient;
    private final HelloAssoAccessTokenService accessTokenService;
    private final HelloAssoPaymentProperties properties;
    private final ObjectMapper objectMapper;

    public HelloAssoPaymentProvider(HelloAssoCheckoutClient checkoutClient,
                                    HelloAssoAccessTokenService accessTokenService,
                                    HelloAssoPaymentProperties properties,
                                    ObjectMapper objectMapper) {
        this.checkoutClient = checkoutClient;
        this.accessTokenService = accessTokenService;
        this.properties = properties;
        this.objectMapper = objectMapper;
        properties.requireCredentials();
    }

    @Override
    public PaymentProviderType type() {
        return PaymentProviderType.HELLOASSO;
    }

    @Override
    public PaymentSession createPayment(PaymentRequest request) {
        requireEuro(request.amount());
        int amountInCents;
        try {
            amountInCents = Math.toIntExact(request.amount().getAmountInCents());
        } catch (ArithmeticException exception) {
            throw new PaymentProviderException(type(), "AMOUNT_OUT_OF_RANGE",
                    "Le montant dépasse la limite HelloAsso", false, exception);
        }

        HelloAssoApiV5CommonModelsCartsInitCheckoutBody body = new HelloAssoApiV5CommonModelsCartsInitCheckoutBody(
                amountInCents, amountInCents, itemName(request.orderNumber()), request.cancelUrl().toASCIIString(),
                request.cancelUrl().toASCIIString(), request.returnUrl().toASCIIString(), false);
        body.metadata(Map.of(
                "shopOrderId", request.orderId(),
                "shopOrderNumber", request.orderNumber(),
                "idempotencyKey", request.idempotencyKey()));

        HelloAssoApiV5CommonModelsCartsInitCheckoutResponse response = authenticated(authorization ->
                checkoutClient.createCheckoutIntent(authorization, properties.getOrganizationSlug(), body));
        if (response == null || response.getId() == null || response.getId() <= 0) {
            throw invalidResponse("HelloAsso n'a pas retourné d'identifiant de checkout");
        }
        URI redirectUrl = requiredRedirectUrl(response.getRedirectUrl());
        String checkoutIntentId = response.getId().toString();
        LOGGER.info("Checkout HelloAsso créé pour la commande {} (intent {})", request.orderNumber(), checkoutIntentId);
        return new PaymentSession(checkoutIntentId, redirectUrl, PaymentStatus.PENDING);
    }

    @Override
    public PaymentResult retrievePayment(String externalPaymentId) {
        String checkoutIntentId = validCheckoutIntentId(externalPaymentId);
        HelloAssoApiV5CommonModelsCartsCheckoutIntentResponse intent = authenticated(authorization ->
                checkoutClient.getCheckoutIntent(authorization, properties.getOrganizationSlug(), checkoutIntentId));
        return toPaymentResult(checkoutIntentId, intent);
    }

    @Override
    public PaymentResult processNotification(PaymentNotification notification) {
        String checkoutIntentId = checkoutIntentIdFrom(notification.payload());
        LOGGER.info("Notification HelloAsso reçue pour l'intent {} ; vérification serveur en cours", checkoutIntentId);
        // Le contenu et les paramètres de la notification ne sont jamais considérés comme une preuve de paiement.
        return retrievePayment(checkoutIntentId);
    }

    private PaymentResult toPaymentResult(String checkoutIntentId,
                                          HelloAssoApiV5CommonModelsCartsCheckoutIntentResponse intent) {
        if (intent == null || intent.getOrder() == null) {
            return pending(checkoutIntentId);
        }
        HelloAssoApiV5CommonModelsStatisticsOrderDetail order = intent.getOrder();
        Money amount = amountOf(order, checkoutIntentId);
        List<HelloAssoApiV5CommonModelsStatisticsOrderPayment> payments = values(order.getPayments());
        PaymentStatus status = statusOf(payments);
        if (status == PaymentStatus.PENDING) {
            return pending(checkoutIntentId);
        }
        if (status == PaymentStatus.FAILED) {
            return new PaymentResult(checkoutIntentId, status, Money.zero(CURRENCY), "REFUSED",
                    "Paiement refusé par HelloAsso");
        }
        if (status == PaymentStatus.CANCELLED) {
            return new PaymentResult(checkoutIntentId, status, Money.zero(CURRENCY), "CANCELLED",
                    "Paiement annulé ou abandonné sur HelloAsso");
        }
        return new PaymentResult(checkoutIntentId, status, amount, null, null);
    }

    private PaymentResult pending(String checkoutIntentId) {
        return new PaymentResult(checkoutIntentId, PaymentStatus.PENDING, Money.zero(CURRENCY), null, null);
    }

    private Money amountOf(HelloAssoApiV5CommonModelsStatisticsOrderDetail order, String checkoutIntentId) {
        if (order.getAmount() == null || order.getAmount().getTotal() == null || order.getAmount().getTotal() < 0) {
            throw invalidResponse("HelloAsso n'a pas retourné le montant vérifié de l'intent " + checkoutIntentId);
        }
        return new Money(order.getAmount().getTotal(), CURRENCY);
    }

    private PaymentStatus statusOf(List<HelloAssoApiV5CommonModelsStatisticsOrderPayment> payments) {
        if (payments.stream().map(HelloAssoApiV5CommonModelsStatisticsOrderPayment::getState)
                .anyMatch(state -> state == HelloAssoApiV5CommonModelsEnumsPaymentState.REFUNDED)) {
            return PaymentStatus.REFUNDED;
        }
        if (payments.stream().map(HelloAssoApiV5CommonModelsStatisticsOrderPayment::getState)
                .anyMatch(state -> state == HelloAssoApiV5CommonModelsEnumsPaymentState.AUTHORIZED
                        || state == HelloAssoApiV5CommonModelsEnumsPaymentState.AUTHORIZED_PREPROD)) {
            return PaymentStatus.SUCCEEDED;
        }
        if (payments.stream().map(HelloAssoApiV5CommonModelsStatisticsOrderPayment::getState)
                .anyMatch(state -> state == HelloAssoApiV5CommonModelsEnumsPaymentState.REFUSED
                        || state == HelloAssoApiV5CommonModelsEnumsPaymentState.ERROR)) {
            return PaymentStatus.FAILED;
        }
        if (payments.stream().map(HelloAssoApiV5CommonModelsStatisticsOrderPayment::getState)
                .anyMatch(state -> state == HelloAssoApiV5CommonModelsEnumsPaymentState.CANCELED
                        || state == HelloAssoApiV5CommonModelsEnumsPaymentState.ABANDONED)) {
            return PaymentStatus.CANCELLED;
        }
        return PaymentStatus.PENDING;
    }

    private <T> T authenticated(Function<String, T> call) {
        try {
            return call.apply(accessTokenService.authorizationHeader());
        } catch (FeignException.Unauthorized firstUnauthorized) {
            // Une seule répétition, après renouvellement du jeton. Aucun POST n'est rejoué sur timeout/5xx.
            accessTokenService.invalidate();
            try {
                return call.apply(accessTokenService.authorizationHeader());
            } catch (FeignException exception) {
                throw HelloAssoFailure.from(exception, "CHECKOUT_CALL");
            }
        } catch (FeignException exception) {
            throw HelloAssoFailure.from(exception, "CHECKOUT_CALL");
        }
    }

    private URI requiredRedirectUrl(JsonNullable<String> nullableUrl) {
        if (nullableUrl == null || !nullableUrl.isPresent() || nullableUrl.get() == null || nullableUrl.get().isBlank()) {
            throw invalidResponse("HelloAsso n'a pas retourné d'URL de redirection");
        }
        try {
            URI url = URI.create(nullableUrl.get());
            if (!"https".equalsIgnoreCase(url.getScheme())) {
                throw new IllegalArgumentException("schéma non HTTPS");
            }
            return url;
        } catch (IllegalArgumentException exception) {
            throw new PaymentProviderException(type(), "INVALID_REDIRECT_URL",
                    "HelloAsso a retourné une URL de redirection invalide", false, exception);
        }
    }

    private String checkoutIntentIdFrom(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode identifier = findCheckoutIntentId(root);
            if (identifier == null || !identifier.canConvertToInt() || identifier.intValue() <= 0) {
                throw invalidNotification();
            }
            return Integer.toString(identifier.intValue());
        } catch (JsonProcessingException exception) {
            throw new PaymentProviderException(type(), "INVALID_NOTIFICATION",
                    "La notification HelloAsso est illisible", false, exception);
        }
    }

    private JsonNode findCheckoutIntentId(JsonNode node) {
        if (node == null) {
            return null;
        }
        JsonNode direct = node.get("checkoutIntentId");
        if (direct != null) {
            return direct;
        }
        JsonNode data = node.get("data");
        if (data != null) {
            JsonNode inData = findCheckoutIntentId(data);
            if (inData != null) {
                return inData;
            }
        }
        JsonNode order = node.get("order");
        return order == null ? null : findCheckoutIntentId(order);
    }

    private String validCheckoutIntentId(String externalPaymentId) {
        try {
            int id = Integer.parseInt(Objects.requireNonNull(externalPaymentId).trim());
            if (id <= 0) {
                throw new NumberFormatException();
            }
            return Integer.toString(id);
        } catch (RuntimeException exception) {
            throw new PaymentProviderException(type(), "INVALID_EXTERNAL_PAYMENT_ID",
                    "L'identifiant de checkout HelloAsso est invalide", false, exception);
        }
    }

    private void requireEuro(Money amount) {
        if (!CURRENCY.equals(amount.getCurrency())) {
            throw new PaymentProviderException(type(), "UNSUPPORTED_CURRENCY",
                    "HelloAsso Checkout accepte uniquement les montants en EUR", false);
        }
    }

    private String itemName(String orderNumber) {
        String value = "Commande " + orderNumber;
        return value.length() <= 250 ? value : value.substring(0, 250);
    }

    private PaymentProviderException invalidResponse(String message) {
        return new PaymentProviderException(type(), "INVALID_PROVIDER_RESPONSE", message, false);
    }

    private PaymentProviderException invalidNotification() {
        return new PaymentProviderException(type(), "INVALID_NOTIFICATION",
                "La notification HelloAsso ne contient pas d'identifiant de checkout", false);
    }

    private static <T> List<T> values(JsonNullable<List<T>> value) {
        return value != null && value.isPresent() && value.get() != null ? value.get() : List.of();
    }
}
