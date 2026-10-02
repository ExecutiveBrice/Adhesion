package com.wild.corp.adhesion.shop.payment.helloasso;

import com.wild.corp.adhesion.shop.payment.provider.PaymentNotification;
import com.wild.corp.adhesion.shop.payment.provider.PaymentProviderType;
import com.wild.corp.adhesion.shop.payment.repository.PaymentAttemptRepository;
import com.wild.corp.adhesion.shop.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class HelloAssoWebhookServiceTest {
    private final PaymentAttemptRepository attempts = mock(PaymentAttemptRepository.class);
    private final PaymentService payments = mock(PaymentService.class);
    private final HelloAssoWebhookService service = new HelloAssoWebhookService(new JsonMapper(), attempts, payments, "");

    @Test
    void knownOrderEventUsesVerifiedProviderProcessing() {
        String payload = "{\"eventType\":\"Order\",\"data\":{\"checkoutIntentId\":123,\"state\":\"Authorized\"}}";
        when(attempts.findIdByProviderAndExternalId("helloasso", "123")).thenReturn(Optional.of(9L));
        receive(payload);
        verify(payments).processNotification(PaymentProviderType.HELLOASSO, new PaymentNotification(payload, Map.of()));
    }

    @Test
    void paymentWithoutCheckoutIdIsCorrelatedUsingStoredMetadata() {
        when(attempts.findIdByCheckoutMetadata("helloasso", 42L, "key")).thenReturn(Optional.of(9L));
        receive("""
                {"eventType":"Payment","data":{"id":999,"order":{"id":888}},
                 "metadata":{"shopOrderId":42,"idempotencyKey":"key"}}
                """);
        verify(payments).refreshPaymentStatus(9L);
        verify(attempts, never()).findIdByProviderAndExternalId(anyString(), anyString());
    }

    @Test
    void unrelatedCampaignAndNonPaymentEventsDoNotCallHelloAsso() {
        receive("{\"eventType\":\"Form\"}");
        receive("{\"eventType\":\"Order\",\"data\":{\"checkoutIntentId\":456}}");
        receive("{\"eventType\":\"Payment\",\"data\":{\"id\":123}}");
        verifyNoInteractions(payments);
    }

    @Test
    void notificationArrivingBeforeCheckoutCommitRequestsRetry() {
        assertThatThrownBy(() -> receive("""
                {"eventType":"Order","data":{"checkoutIntentId":123},
                 "metadata":{"shopOrderId":42,"idempotencyKey":"key"}}
                """))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(exception -> org.assertj.core.api.Assertions.assertThat(
                        ((ResponseStatusException) exception).getStatusCode().value()).isEqualTo(503));
        verifyNoInteractions(payments);
    }

    @Test
    void rejectsMalformedAndOversizedPayloads() {
        for (String payload : new String[]{"{", "[]", "null", "{}",
                "{\"eventType\":\"Order\",\"data\":{\"checkoutIntentId\":1.5}}"}) {
            assertThatThrownBy(() -> receive(payload)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> service.receive(new byte[131_073], null)).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(attempts, payments);
    }

    @Test
    void optionalPartnerSignatureIsVerifiedAgainstExactRequestBytes() throws Exception {
        var signed = new HelloAssoWebhookService(new JsonMapper(), attempts, payments, "test-signature-key");
        byte[] body = "{\"eventType\":\"Form\"}".getBytes(StandardCharsets.UTF_8);
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("test-signature-key".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signature = HexFormat.of().formatHex(mac.doFinal(body));
        signed.receive(body, signature);
        assertThatThrownBy(() -> signed.receive(body, null)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> signed.receive(body, "invalid")).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> signed.receive("{}".getBytes(StandardCharsets.UTF_8), signature))
                .isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(attempts, payments);
    }

    private void receive(String body) {
        service.receive(body.getBytes(StandardCharsets.UTF_8), null);
    }
}
