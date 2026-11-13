package com.wild.corp.adhesion.shop.api;

import com.wild.corp.adhesion.security.WebSecurityConfig;
import com.wild.corp.adhesion.security.PublicApiEndpoints;
import com.wild.corp.adhesion.security.jwt.AuthEntryPointJwt;
import com.wild.corp.adhesion.security.jwt.JwtUtils;
import com.wild.corp.adhesion.services.UserDetailsService;
import com.wild.corp.adhesion.shop.payment.helloasso.HelloAssoWebhookService;
import com.wild.corp.adhesion.shop.payment.provider.PaymentProviderException;
import com.wild.corp.adhesion.shop.payment.provider.PaymentProviderType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ShopPaymentWebhookController.class, properties = "shop.payment.provider=helloasso")
@ContextConfiguration(classes = {ShopPaymentWebhookController.class, WebSecurityConfig.class, AuthEntryPointJwt.class})
@Import(ShopApiExceptionHandler.class)
class ShopPaymentWebhookControllerTest {
    private static final String URL = "/shop/payments/helloasso/notifications";
    @Autowired private MockMvc mvc;
    @MockitoBean private HelloAssoWebhookService notifications;
    @MockitoBean private UserDetailsService users;
    @MockitoBean private JwtUtils jwt;
    @MockitoBean private AuthenticationManager authenticationManager;

    @Test
    void acceptsAnonymousPostAndAcknowledgesWithoutExposingOrderData() throws Exception {
        String body = "{\"eventType\":\"Order\",\"data\":{\"checkoutIntentId\":123}}";
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content(body).header("x-ha-signature", "abc"))
                .andExpect(status().isOk()).andExpect(content().string(""));
        verify(notifications).receive(body.getBytes(StandardCharsets.UTF_8), "abc");
        assertThat(PublicApiEndpoints.isPublic(HttpMethod.POST, URL)).isTrue();
        mvc.perform(get(URL)).andExpect(status().isUnauthorized());
    }

    @Test
    void returnsFailureSoHelloAssoCanRetry() throws Exception {
        doThrow(new PaymentProviderException(PaymentProviderType.HELLOASSO, "TIMEOUT", "Indisponible", true))
                .when(notifications).receive(any(), isNull());
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void rejectsMalformedPayload() throws Exception {
        doThrow(new IllegalArgumentException("Notification invalide")).when(notifications).receive(any(), isNull());
        mvc.perform(post(URL).contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest());
    }
}
