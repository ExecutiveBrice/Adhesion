package com.wild.corp.adhesion.shop.api;

import com.wild.corp.adhesion.models.UserDetails;
import com.wild.corp.adhesion.security.WebSecurityConfig;
import com.wild.corp.adhesion.security.jwt.AuthEntryPointJwt;
import com.wild.corp.adhesion.security.jwt.JwtUtils;
import com.wild.corp.adhesion.services.UserDetailsService;
import com.wild.corp.adhesion.shop.cart.service.CartPricingService;
import com.wild.corp.adhesion.shop.catalog.model.Product;
import com.wild.corp.adhesion.shop.catalog.model.ProductVariant;
import com.wild.corp.adhesion.shop.catalog.service.CatalogService;
import com.wild.corp.adhesion.shop.common.money.Money;
import com.wild.corp.adhesion.shop.order.model.OrderStatus;
import com.wild.corp.adhesion.shop.order.model.ShopOrder;
import com.wild.corp.adhesion.shop.order.service.OrderService;
import com.wild.corp.adhesion.shop.order.service.OrderConversationService;
import com.wild.corp.adhesion.shop.payment.provider.PaymentProviderException;
import com.wild.corp.adhesion.shop.payment.provider.PaymentProviderType;
import com.wild.corp.adhesion.shop.payment.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ShopController.class)
@ContextConfiguration(classes = {ShopController.class, WebSecurityConfig.class, AuthEntryPointJwt.class})
@Import(ShopApiExceptionHandler.class)
class ShopControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CatalogService catalogService;
    @MockitoBean
    private CartPricingService cartPricingService;
    @MockitoBean
    private OrderService orderService;
    @MockitoBean
    private OrderConversationService orderConversationService;
    @MockitoBean
    private PaymentService paymentService;
    @MockitoBean
    private UserDetailsService userDetailsService;
    @MockitoBean
    private JwtUtils jwtUtils;
    @MockitoBean
    private AuthenticationManager authenticationManager;

    @Test
    void exposesOnlyActiveProductsToAnonymousVisitors() throws Exception {
        Product product = new Product("Tee-shirt", "tee-shirt", "Coton", true, 0);
        ReflectionTestUtils.setField(product, "id", 10L);
        ProductVariant variant = new ProductVariant(product, "TS-M", "M", new Money(1_500, "EUR"), true, 0);
        ReflectionTestUtils.setField(variant, "id", 20L);
        variant.trackStock(0);
        product.addVariant(variant);
        given(catalogService.findActiveProducts()).willReturn(List.of(product));

        mockMvc.perform(get("/shop/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].variants[0].price.amountInCents").value(1500))
                .andExpect(jsonPath("$[0].variants[0].available").value(true))
                .andExpect(jsonPath("$[0].variants[0].availableQuantity").doesNotExist());
    }

    @Test
    void exposesActiveProductAndCartQuoteToAnonymousVisitors() throws Exception {
        Product product = new Product("Tee-shirt", "tee-shirt", "Coton", true, 0);
        ReflectionTestUtils.setField(product, "id", 10L);
        given(catalogService.findActiveProduct(10L)).willReturn(product);
        Money total = new Money(1_500, "EUR");
        var line = new CartPricingService.CartQuoteLine(10L, "Tee-shirt", 20L, "M", "TS-M",
                total, 1, total, true);
        given(cartPricingService.quote(any())).willReturn(new CartPricingService.CartQuote(
                List.of(line), total, Money.zero("EUR"), Money.zero("EUR"), total));

        mockMvc.perform(get("/shop/products/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(10));
        mockMvc.perform(post("/shop/cart/quote").contentType(APPLICATION_JSON)
                        .content("{\"items\":[{\"variantId\":20,\"quantity\":1}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total.amountInCents").value(1500));
    }

    @Test
    void requiresAuthenticationForOrdersAndPayment() throws Exception {
        mockMvc.perform(post("/shop/orders").header("Idempotency-Key", "anonymous-checkout")
                        .contentType(APPLICATION_JSON)
                        .content("{\"items\":[{\"variantId\":20,\"quantity\":1}]}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/shop/orders")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/shop/orders/CMD-2026-000123")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/shop/orders/CMD-2026-000123/payment-session")
                        .header("Idempotency-Key", "anonymous-payment").contentType(APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsNegativeQuantityBeforePricingCart() throws Exception {
        mockMvc.perform(post("/shop/cart/quote").with(authentication(customerAuthentication())).contentType(APPLICATION_JSON)
                        .content("{\"items\":[{\"variantId\":20,\"quantity\":-1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['items[0].quantity']").exists());
    }

    @Test
    void checkoutUsesOnlyReferencesAndQuantitiesAndIsIdempotent() throws Exception {
        ShopOrder order = new ShopOrder("CMD-2026-000123", 42L, "checkout-123", "EUR");
        ReflectionTestUtils.setField(order, "id", 123L);
        order.addItem(com.wild.corp.adhesion.shop.order.model.OrderItem.snapshot(10L, "Tee-shirt", 20L, "M", "TS-M",
                new Money(1_500, "EUR"), 2));
        order.submitForPayment();
        given(orderService.createOrder(eq(42L), eq("checkout-123"), any())).willReturn(order);

        mockMvc.perform(post("/shop/orders")
                        .with(authentication(customerAuthentication()))
                        .header("Idempotency-Key", "checkout-123")
                        .contentType(APPLICATION_JSON)
                        .content("{\"items\":[{\"variantId\":20,\"quantity\":2}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total.amountInCents").value(3000))
                .andExpect(jsonPath("$.discountTotal.amountInCents").value(0));

        verify(orderService).createOrder(eq(42L), eq("checkout-123"), any());
    }

    @Test
    void checkoutRequiresAnIdempotencyKey() throws Exception {
        mockMvc.perform(post("/shop/orders")
                        .with(authentication(customerAuthentication()))
                        .contentType(APPLICATION_JSON)
                        .content("{\"items\":[{\"variantId\":20,\"quantity\":1}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requête invalide"));
    }

    @Test
    void rejectsClientSuppliedTotals() throws Exception {
        mockMvc.perform(post("/shop/cart/quote").with(authentication(customerAuthentication())).contentType(APPLICATION_JSON)
                        .content("{\"items\":[{\"variantId\":20,\"quantity\":1}],\"total\":{\"amountInCents\":1}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requête invalide"));
    }

    @Test
    void listsOnlyOrdersForTheAuthenticatedCustomer() throws Exception {
        ShopOrder order = new ShopOrder("CMD-2026-000456", 42L, "EUR");
        order.addItem(com.wild.corp.adhesion.shop.order.model.OrderItem.snapshot(10L, "Tee-shirt", 20L, "M", "TS-M",
                new Money(1_500, "EUR"), 1));
        order.submitForPayment();
        given(orderService.findOrdersForCustomer(42L)).willReturn(List.of(order));

        mockMvc.perform(get("/shop/orders").with(authentication(customerAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderNumber").value("CMD-2026-000456"));

        verify(orderService).findOrdersForCustomer(42L);
    }

    @Test
    void cancelsAnUnpaidOrderForTheAuthenticatedCustomer() throws Exception {
        ShopOrder order = new ShopOrder("CMD-2026-000789", 42L, "EUR");
        order.addItem(com.wild.corp.adhesion.shop.order.model.OrderItem.snapshot(10L, "Tee-shirt", 20L, "M", "TS-M",
                new Money(1_500, "EUR"), 1));
        order.submitForPayment();
        order.transitionTo(OrderStatus.CANCELLED);
        given(orderService.cancelOrderForCustomer("CMD-2026-000789", 42L)).willReturn(order);

        mockMvc.perform(post("/shop/orders/CMD-2026-000789/cancel").with(authentication(customerAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));

        verify(orderService).cancelOrderForCustomer("CMD-2026-000789", 42L);
    }

    @Test
    void opensRefundRequestForAnAuthenticatedCustomer() throws Exception {
        ShopOrder order = new ShopOrder("CMD-2026-000790", 42L, "EUR");
        order.addItem(com.wild.corp.adhesion.shop.order.model.OrderItem.snapshot(10L, "Tee-shirt", 20L, "M", "TS-M",
                new Money(1_500, "EUR"), 1));
        order.submitForPayment();
        order.transitionTo(OrderStatus.PAID);
        order.requestRefund();
        given(orderService.requestRefundForCustomer("CMD-2026-000790", 42L)).willReturn(order);

        mockMvc.perform(post("/shop/orders/CMD-2026-000790/refund-request").with(authentication(customerAuthentication())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refundRequested").value(true));

        verify(orderService).requestRefundForCustomer("CMD-2026-000790", 42L);
    }

    @Test
    void allowsLocalHttpsCorsPreflightForPaymentSession() throws Exception {
        mockMvc.perform(options("/shop/orders/CMD-2026-000007/payment-session")
                        .header("Origin", "https://localhost:4200")
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "Authorization,Content-Type,Idempotency-Key"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "https://localhost:4200"));
    }

    @Test
    void paymentSessionExplainsThatHelloAssoRequiresHttps() throws Exception {
        ShopOrder order = new ShopOrder("CMD-2026-000007", 42L, "checkout-7", "EUR");
        ReflectionTestUtils.setField(order, "id", 7L);
        given(orderService.findOrderForCustomer("CMD-2026-000007", 42L)).willReturn(order);
        given(paymentService.createPaymentSession(eq(7L), eq("https-test"), any(URI.class), any(URI.class)))
                .willThrow(new PaymentProviderException(PaymentProviderType.HELLOASSO, "INSECURE_CHECKOUT_URL",
                        "HelloAsso exige une URL HTTPS absolue pour returnUrl. Ouvrez la boutique en HTTPS.", false));

        mockMvc.perform(post("/shop/orders/CMD-2026-000007/payment-session")
                        .with(authentication(customerAuthentication()))
                        .header("Idempotency-Key", "https-test")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"returnUrl":"http://localhost:4200/return","cancelUrl":"http://localhost:4200/cancel"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INSECURE_CHECKOUT_URL"))
                .andExpect(jsonPath("$.title").value("URL de retour non sécurisée"));
    }

    @Test
    void paymentVerificationExposesProviderErrorCodeWithoutProviderResponse() throws Exception {
        ShopOrder order = new ShopOrder("CMD-2026-000007", 42L, "checkout-7", "EUR");
        ReflectionTestUtils.setField(order, "id", 7L);
        given(orderService.findOrderForCustomer("CMD-2026-000007", 42L)).willReturn(order);
        given(paymentService.refreshPaymentStatusForOrder(7L)).willThrow(new PaymentProviderException(
                PaymentProviderType.HELLOASSO, "CHECKOUT_CALL_HTTP_404", "Détail privé HelloAsso", false));

        mockMvc.perform(post("/shop/orders/CMD-2026-000007/payment/verify")
                        .with(authentication(customerAuthentication())))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("CHECKOUT_CALL_HTTP_404"))
                .andExpect(jsonPath("$.detail").value("Le paiement ne peut pas être initialisé ou vérifié pour le moment"));
    }

    private UsernamePasswordAuthenticationToken customerAuthentication() {
        UserDetails user = new UserDetails(42L, "customer@example.test", "secret",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        return new UsernamePasswordAuthenticationToken(user, "secret", user.getAuthorities());
    }
}
