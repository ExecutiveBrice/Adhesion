package com.wild.corp.adhesion.shop.payment.helloasso;

import com.wild.corp.adhesion.shop.common.money.Money;
import com.wild.corp.adhesion.shop.payment.helloasso.client.HelloAssoCheckoutClient;
import com.wild.corp.adhesion.shop.payment.model.PaymentStatus;
import com.wild.corp.adhesion.shop.payment.provider.PaymentNotification;
import com.wild.corp.adhesion.shop.payment.provider.PaymentLine;
import com.wild.corp.adhesion.shop.payment.provider.PaymentPayer;
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
import org.wild.corp.adhesion.client.helloasso.model.HelloAssoApiV5CommonModelsCartsCheckoutPayer;
import org.wild.corp.adhesion.client.helloasso.model.HelloAssoApiV5CommonModelsCartsInitCheckoutBody;
import org.wild.corp.adhesion.client.helloasso.model.HelloAssoApiV5CommonModelsCartsInitCheckoutResponse;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

/** Adaptateur HelloAsso : toutes les confirmations passent par une lecture serveur de l'intent. */
@Component
@ConditionalOnProperty(prefix = "shop.payment", name = "provider", havingValue = "helloasso")
public class HelloAssoPaymentProvider implements PaymentProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(HelloAssoPaymentProvider.class);
    private static final String CURRENCY = "EUR";
    private static final Pattern CHECKOUT_NAME = Pattern.compile("[\\p{IsLatin} '\\-’]+", Pattern.UNICODE_CASE);
    private static final Pattern CHECKOUT_EMAIL = Pattern.compile("[^\\s@]+@[^\\s@]+\\.[^\\s@]+");
    private static final Set<String> REJECTED_NAMES = Set.of("firstname", "lastname", "unknown",
            "first_name", "last_name", "anonyme", "user", "admin", "name", "nom", "prénom", "test");

    private final HelloAssoCheckoutClient checkoutClient;
    private final HelloAssoAccessTokenService accessTokenService;
    private final HelloAssoPaymentProperties properties;
    private final JsonMapper jsonMapper;

    public HelloAssoPaymentProvider(HelloAssoCheckoutClient checkoutClient,
                                    HelloAssoAccessTokenService accessTokenService,
                                    HelloAssoPaymentProperties properties,
                                    JsonMapper jsonMapper) {
        this.checkoutClient = checkoutClient;
        this.accessTokenService = accessTokenService;
        this.properties = properties;
        this.jsonMapper = jsonMapper;
        properties.requireCredentials();
    }

    @Override
    public PaymentProviderType type() {
        return PaymentProviderType.HELLOASSO;
    }

    @Override
    public PaymentSession createPayment(PaymentRequest request) {
        requireHttpsUrl(request.returnUrl());
        requireHttpsUrl(request.cancelUrl());
        requireEuro(request.amount());
        int amountInCents;
        try {
            amountInCents = Math.toIntExact(request.amount().getAmountInCents());
        } catch (ArithmeticException exception) {
            throw new PaymentProviderException(type(), "AMOUNT_OUT_OF_RANGE",
                    "Le montant dépasse la limite HelloAsso", false, exception);
        }

        HelloAssoApiV5CommonModelsCartsInitCheckoutBody body = new HelloAssoApiV5CommonModelsCartsInitCheckoutBody(
                amountInCents, amountInCents, itemName(request), request.cancelUrl().toASCIIString(),
                request.cancelUrl().toASCIIString(), request.returnUrl().toASCIIString(), false);
        HelloAssoApiV5CommonModelsCartsCheckoutPayer payer = checkoutPayer(request.payer());
        if (payer != null) {
            body.payer(payer);
        }
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
        String intent = authenticated(authorization ->
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

    private PaymentResult toPaymentResult(String checkoutIntentId, String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw invalidResponse("HelloAsso a retourné un checkout vide");
        }
        JsonNode intent;
        try {
            intent = jsonMapper.readTree(responseBody);
        } catch (JacksonException exception) {
            throw new PaymentProviderException(type(), "INVALID_PROVIDER_RESPONSE",
                    "La réponse de vérification HelloAsso est illisible", false, exception);
        }
        if (intent == null || !intent.isObject()) {
            throw invalidResponse("HelloAsso a retourné un checkout invalide");
        }
        JsonNode order = intent.get("order");
        if (order == null || order.isNull()) {
            return pending(checkoutIntentId);
        }
        if (!order.isObject()) {
            throw invalidResponse("HelloAsso a retourné une commande invalide");
        }
        PaymentStatus status = statusOf(order.get("payments"));
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
        return new PaymentResult(checkoutIntentId, status, amountOf(order, checkoutIntentId), null, null);
    }

    private PaymentResult pending(String checkoutIntentId) {
        return new PaymentResult(checkoutIntentId, PaymentStatus.PENDING, Money.zero(CURRENCY), null, null);
    }

    private Money amountOf(JsonNode order, String checkoutIntentId) {
        JsonNode amount = order.path("amount").path("total");
        if (!amount.isIntegralNumber() || !amount.canConvertToLong() || amount.longValue() < 0) {
            throw invalidResponse("HelloAsso n'a pas retourné le montant vérifié de l'intent " + checkoutIntentId);
        }
        return new Money(amount.longValue(), CURRENCY);
    }

    private PaymentStatus statusOf(JsonNode payments) {
        if (payments == null || payments.isNull()) return PaymentStatus.PENDING;
        if (!payments.isArray()) throw invalidResponse("HelloAsso a retourné une liste de paiements invalide");
        boolean authorized = false;
        boolean failed = false;
        boolean cancelled = false;
        for (JsonNode payment : payments) {
            String state = payment.path("state").asText().toLowerCase(Locale.ROOT);
            switch (state) {
                case "refunded" -> { return PaymentStatus.REFUNDED; }
                case "authorized", "authorizedpreprod" -> authorized = true;
                case "refused", "error" -> failed = true;
                case "canceled", "abandoned" -> cancelled = true;
                default -> { }
            }
        }
        if (authorized) return PaymentStatus.SUCCEEDED;
        if (failed) return PaymentStatus.FAILED;
        return cancelled ? PaymentStatus.CANCELLED : PaymentStatus.PENDING;
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
            JsonNode root = jsonMapper.readTree(payload);
            JsonNode identifier = findCheckoutIntentId(root);
            if (identifier == null || !identifier.canConvertToInt() || identifier.intValue() <= 0) {
                throw invalidNotification();
            }
            return Integer.toString(identifier.intValue());
        } catch (JacksonException exception) {
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

    private void requireHttpsUrl(URI url) {
        if (!"https".equalsIgnoreCase(url.getScheme()) || url.getHost() == null) {
            throw new PaymentProviderException(type(), "INSECURE_CHECKOUT_URL",
                    "HelloAsso exige des URL de retour HTTPS", false);
        }
    }

    private String itemName(PaymentRequest request) {
        String prefix = "Commande " + request.orderNumber();
        if (request.lines().isEmpty()) {
            return limitItemName(prefix);
        }
        Map<String, Integer> quantities = new LinkedHashMap<>();
        for (PaymentLine line : request.lines()) {
            String name = line.productName();
            if (line.variantName() != null && !line.variantName().isBlank()) {
                name += " (" + line.variantName() + ")";
            }
            quantities.merge(name, line.quantity(), Math::addExact);
        }
        String products = quantities.entrySet().stream()
                .map(entry -> entry.getKey() + " x " + entry.getValue())
                .collect(Collectors.joining(" ; "));
        return limitItemName(prefix + " : " + products);
    }

    private String limitItemName(String value) {
        return value.length() <= 250 ? value : value.substring(0, 249) + "…";
    }

    private HelloAssoApiV5CommonModelsCartsCheckoutPayer checkoutPayer(PaymentPayer source) {
        if (source == null) {
            return null;
        }
        String firstName = validName(source.firstName());
        String lastName = validName(source.lastName());
        if (firstName != null && lastName != null && firstName.equalsIgnoreCase(lastName)) {
            firstName = null;
            lastName = null;
        }
        String email = validEmail(source.email());
        if (firstName == null && lastName == null && email == null) {
            return null;
        }
        HelloAssoApiV5CommonModelsCartsCheckoutPayer payer = new HelloAssoApiV5CommonModelsCartsCheckoutPayer();
        if (firstName != null) {
            payer.firstName(firstName);
        }
        if (lastName != null) {
            payer.lastName(lastName);
        }
        if (email != null) {
            payer.email(email);
        }
        return payer;
    }

    private String validName(String value) {
        if (value == null) {
            return null;
        }
        String name = value.trim();
        String lower = name.toLowerCase(Locale.ROOT);
        if (name.length() < 2 || name.length() > 255 || !CHECKOUT_NAME.matcher(name).matches()
                || !lower.matches(".*[aeiouyàâäéèêëîïôöùûü].*")
                || lower.matches(".*(.)\\1\\1.*") || REJECTED_NAMES.contains(lower)) {
            return null;
        }
        return name;
    }

    private String validEmail(String value) {
        if (value == null) {
            return null;
        }
        String email = value.trim();
        return email.length() <= 255 && CHECKOUT_EMAIL.matcher(email).matches() ? email : null;
    }

    private PaymentProviderException invalidResponse(String message) {
        return new PaymentProviderException(type(), "INVALID_PROVIDER_RESPONSE", message, false);
    }

    private PaymentProviderException invalidNotification() {
        return new PaymentProviderException(type(), "INVALID_NOTIFICATION",
                "La notification HelloAsso ne contient pas d'identifiant de checkout", false);
    }

}
