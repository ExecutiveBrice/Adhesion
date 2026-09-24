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
import com.wild.corp.adhesion.shop.order.model.ShopOrder;
import com.wild.corp.adhesion.shop.order.service.OrderService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
    private UserDetailsService userDetailsService;
    @MockitoBean
    private JwtUtils jwtUtils;
    @MockitoBean
    private AuthenticationManager authenticationManager;

    @Test
    void exposesOnlyActiveProductsWithoutAuthentication() throws Exception {
        Product product = new Product("Tee-shirt", "tee-shirt", "Coton", true, 0);
        ReflectionTestUtils.setField(product, "id", 10L);
        ProductVariant variant = new ProductVariant(product, "TS-M", "M", new Money(1_500, "EUR"), true, 0);
        ReflectionTestUtils.setField(variant, "id", 20L);
        product.addVariant(variant);
        given(catalogService.findActiveProducts()).willReturn(List.of(product));

        mockMvc.perform(get("/shop/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].variants[0].price.amountInCents").value(1500));
    }

    @Test
    void rejectsNegativeQuantityBeforePricingCart() throws Exception {
        mockMvc.perform(post("/shop/cart/quote").contentType(APPLICATION_JSON)
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
        mockMvc.perform(post("/shop/cart/quote").contentType(APPLICATION_JSON)
                        .content("{\"items\":[{\"variantId\":20,\"quantity\":1}],\"total\":{\"amountInCents\":1}}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Requête invalide"));
    }

    private UsernamePasswordAuthenticationToken customerAuthentication() {
        UserDetails user = new UserDetails(42L, "customer@example.test", "secret",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        return new UsernamePasswordAuthenticationToken(user, "secret", user.getAuthorities());
    }
}
