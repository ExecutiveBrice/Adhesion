package com.wild.corp.adhesion.shop.payment.helloasso;

import com.wild.corp.adhesion.shop.payment.provider.PaymentNotification;
import com.wild.corp.adhesion.shop.payment.provider.PaymentProviderType;
import com.wild.corp.adhesion.shop.payment.repository.PaymentAttemptRepository;
import com.wild.corp.adhesion.shop.payment.service.PaymentService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

/** A webhook is only a signal to read an existing checkout through the authenticated API. */
@Service
public class HelloAssoWebhookService {
    private final JsonMapper mapper;
    private final PaymentAttemptRepository attempts;
    private final PaymentService payments;
    private final String signatureKey;

    public HelloAssoWebhookService(JsonMapper mapper, PaymentAttemptRepository attempts, PaymentService payments,
                                  @Value("${shop.payment.helloasso.webhook-signature-key:}") String signatureKey) {
        this.mapper = mapper;
        this.attempts = attempts;
        this.payments = payments;
        this.signatureKey = signatureKey;
    }

    public void receive(byte[] body, String signature) {
        if (body.length > 131_072) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE);
        }
        verifySignature(body, signature);
        JsonNode root;
        try {
            root = mapper.readTree(body);
        } catch (JacksonException exception) {
            throw new IllegalArgumentException("Notification HelloAsso illisible");
        }
        if (root == null || !root.isObject() || !root.path("eventType").isTextual()) {
            throw new IllegalArgumentException("Notification HelloAsso invalide");
        }
        String eventType = root.path("eventType").asText();
        if (!"Order".equals(eventType) && !"Payment".equals(eventType)) return;

        JsonNode checkoutId = field(root, "checkoutIntentId");
        if (checkoutId != null) {
            if (!checkoutId.isIntegralNumber() || !checkoutId.canConvertToInt() || checkoutId.intValue() <= 0) {
                throw new IllegalArgumentException("Identifiant de checkout invalide");
            }
            // Ignore other HelloAsso campaigns; never query arbitrary external IDs from an anonymous request.
            if (attempts.findIdByProviderAndExternalId("helloasso", checkoutId.asText()).isPresent()) {
                payments.processNotification(PaymentProviderType.HELLOASSO,
                        new PaymentNotification(new String(body, StandardCharsets.UTF_8), Map.of()));
                return;
            }
        }

        // Payment events may omit checkoutIntentId, but carry the metadata sent when creating the checkout.
        JsonNode metadata = field(root, "metadata");
        if (metadata == null || !metadata.isObject()) return;
        JsonNode orderId = metadata.path("shopOrderId");
        JsonNode key = metadata.path("idempotencyKey");
        if (!orderId.isIntegralNumber() || !orderId.canConvertToLong() || orderId.longValue() <= 0
                || !key.isTextual() || key.asText().isBlank() || key.asText().length() > 100) return;
        Optional<Long> attemptId = attempts.findIdByCheckoutMetadata("helloasso", orderId.longValue(), key.asText());
        if (attemptId.isEmpty()) {
            // The notification can arrive before the checkout creation transaction commits. Ask for a retry.
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Checkout pas encore disponible");
        }
        payments.refreshPaymentStatus(attemptId.get());
    }

    private JsonNode field(JsonNode root, String name) {
        // Explicit supported envelopes, never confuse data.id (a payment/order ID) with a checkout ID.
        for (JsonNode node : new JsonNode[]{root, root.path("data"), root.path("order"), root.path("data").path("order")}) {
            JsonNode value = node.get(name);
            if (value != null && !value.isNull()) return value;
        }
        return null;
    }

    private void verifySignature(byte[] body, String signature) {
        if (signatureKey.isBlank()) return; // Association accounts may not have a partner signature key.
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(signatureKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] received = signature == null ? new byte[0] : HexFormat.of().parseHex(signature);
            if (!MessageDigest.isEqual(mac.doFinal(body), received)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Signature HelloAsso invalide");
            }
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Signature HelloAsso invalide");
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Impossible de vérifier la signature HelloAsso", exception);
        }
    }
}
