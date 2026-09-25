package com.wild.corp.adhesion.shop.api;

import com.wild.corp.adhesion.security.WebSecurityConfig;
import com.wild.corp.adhesion.security.jwt.AuthEntryPointJwt;
import com.wild.corp.adhesion.security.jwt.JwtUtils;
import com.wild.corp.adhesion.models.Adherent;
import com.wild.corp.adhesion.models.Tribu;
import com.wild.corp.adhesion.models.User;
import com.wild.corp.adhesion.repository.UserRepository;
import com.wild.corp.adhesion.services.UserDetailsService;
import com.wild.corp.adhesion.shop.catalog.service.CatalogService;
import com.wild.corp.adhesion.shop.common.money.Money;
import com.wild.corp.adhesion.shop.order.model.OrderItem;
import com.wild.corp.adhesion.shop.order.model.OrderItemStatus;
import com.wild.corp.adhesion.shop.order.model.OrderStatus;
import com.wild.corp.adhesion.shop.order.model.ShopOrder;
import com.wild.corp.adhesion.shop.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.http.MediaType;

import java.util.List;
import java.util.UUID;

import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ShopAdminController.class)
@ContextConfiguration(classes = {ShopAdminController.class, WebSecurityConfig.class, AuthEntryPointJwt.class})
@Import(ShopApiExceptionHandler.class)
class ShopAdminControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private CatalogService catalogService;
    @MockitoBean private OrderService orderService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private JwtUtils jwtUtils;
    @MockitoBean private AuthenticationManager authenticationManager;

    @Test
    void rejectsOrdinaryUser() throws Exception {
        mockMvc.perform(get("/shop/admin/products").with(user("member@example.test").roles("USER")))
                .andExpect(status().isForbidden());
        verifyNoInteractions(catalogService);
    }

    @Test
    void allowsOnlyShopManager() throws Exception {
        given(catalogService.findAllProducts()).willReturn(List.of());

        mockMvc.perform(get("/shop/admin/products").with(user("manager@example.test").roles("RESPONSABLE_BOUTIQUE")))
                .andExpect(status().isOk());
    }

    @Test
    void listsOrdersOnlyForShopManager() throws Exception {
        ShopOrder order = new ShopOrder("CMD-2026-000001", 42L, "EUR");
        OrderItem item = OrderItem.snapshot(10L, "Tee-shirt", 20L, "M", "TS-M", new Money(1_500, "EUR"), 2);
        order.addItem(item);
        ReflectionTestUtils.setField(item, "id", 7L);
        given(orderService.findAllOrders()).willReturn(List.of(order));
        User customer = new User("client@example.test", "secret");
        ReflectionTestUtils.setField(customer, "id", 42L);
        Tribu tribe = new Tribu(UUID.fromString("1f9e3778-1ca5-4559-a8ba-46f15bbbd10c"));
        ReflectionTestUtils.setField(tribe, "id", 17L);
        Adherent adherent = new Adherent();
        adherent.setTribu(tribe);
        customer.setAdherent(adherent);
        given(userRepository.findAllByIdIn(any())).willReturn(List.of(customer));

        mockMvc.perform(get("/shop/admin/orders").with(user("member@example.test").roles("USER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/shop/admin/orders").with(user("manager@example.test").roles("RESPONSABLE_BOUTIQUE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].orderNumber").value("CMD-2026-000001"))
                .andExpect(jsonPath("$[0].status").value("DRAFT"))
                .andExpect(jsonPath("$[0].customerEmail").value("client@example.test"))
                .andExpect(jsonPath("$[0].customerTribeId").value(17))
                .andExpect(jsonPath("$[0].total.amountInCents").value(3000))
                .andExpect(jsonPath("$[0].items[0].id").value(7))
                .andExpect(jsonPath("$[0].items[0].productName").value("Tee-shirt"))
                .andExpect(jsonPath("$[0].items[0].status").value("PENDING"));
    }

    @Test
    void changesItemStatusOnlyForShopManager() throws Exception {
        OrderItem item = OrderItem.snapshot(10L, "Tee-shirt", 20L, "M", "TS-M", new Money(1_500, "EUR"), 1);
        ReflectionTestUtils.setField(item, "id", 7L);
        item.setStatus(OrderItemStatus.PROCESSING);
        given(orderService.updateItemStatus("CMD-2026-000001", 7L, OrderItemStatus.PROCESSING)).willReturn(item);

        mockMvc.perform(put("/shop/admin/orders/CMD-2026-000001/items/7/status")
                        .with(user("member@example.test").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PROCESSING\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/shop/admin/orders/CMD-2026-000001/items/7/status")
                        .with(user("manager@example.test").roles("RESPONSABLE_BOUTIQUE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PROCESSING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.status").value("PROCESSING"));
    }

    @Test
    void changesOrderStatusOnlyForShopManager() throws Exception {
        ShopOrder order = new ShopOrder("CMD-2026-000001", 42L, "EUR");
        order.addItem(OrderItem.snapshot(10L, "Tee-shirt", 20L, "M", "TS-M", new Money(1_500, "EUR"), 1));
        order.submitForPayment();
        order.transitionTo(OrderStatus.PAID);
        order.transitionTo(OrderStatus.PROCESSING);
        given(orderService.updateAdminOrderStatus("CMD-2026-000001", OrderStatus.PROCESSING)).willReturn(order);

        mockMvc.perform(put("/shop/admin/orders/CMD-2026-000001/status")
                        .with(user("member@example.test").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PROCESSING\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/shop/admin/orders/CMD-2026-000001/status")
                        .with(user("manager@example.test").roles("RESPONSABLE_BOUTIQUE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PROCESSING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROCESSING"));
    }
}
