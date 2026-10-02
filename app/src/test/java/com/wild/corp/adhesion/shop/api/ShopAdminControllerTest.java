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
import com.wild.corp.adhesion.shop.catalog.service.ProductImageStorageService;
import com.wild.corp.adhesion.shop.catalog.service.SupplierOrderService;
import com.wild.corp.adhesion.shop.catalog.model.Product;
import com.wild.corp.adhesion.shop.catalog.model.ProductVariant;
import com.wild.corp.adhesion.shop.catalog.model.SupplierOrder;
import com.wild.corp.adhesion.shop.common.money.Money;
import com.wild.corp.adhesion.shop.order.model.OrderItem;
import com.wild.corp.adhesion.shop.order.model.OrderItemStatus;
import com.wild.corp.adhesion.shop.order.model.OrderStatus;
import com.wild.corp.adhesion.shop.order.model.ShopOrder;
import com.wild.corp.adhesion.shop.order.service.OrderService;
import com.wild.corp.adhesion.shop.order.service.OrderConversationService;
import com.wild.corp.adhesion.shop.payment.service.PaymentService;
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
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ShopAdminController.class)
@ContextConfiguration(classes = {ShopAdminController.class, WebSecurityConfig.class, AuthEntryPointJwt.class})
@Import(ShopApiExceptionHandler.class)
class ShopAdminControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private CatalogService catalogService;
    @MockitoBean private ProductImageStorageService productImageStorage;
    @MockitoBean private SupplierOrderService supplierOrderService;
    @MockitoBean private OrderService orderService;
    @MockitoBean private PaymentService paymentService;
    @MockitoBean private OrderConversationService orderConversationService;
    @MockitoBean private UserRepository userRepository;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private JwtUtils jwtUtils;
    @MockitoBean private AuthenticationManager authenticationManager;

    @Test
    void verifiesPaymentOnlyForShopManager() throws Exception {
        ShopOrder order = new ShopOrder("CMD-2026-000001", 42L, "EUR");
        order.addItem(OrderItem.snapshot(10L, "Tee-shirt", 20L, "M", "TS-M", new Money(1500, "EUR"), 1));
        order.submitForPayment();
        order.transitionTo(OrderStatus.PAID);
        given(paymentService.verifyPaymentForManager(order.getOrderNumber())).willReturn(order);

        String url = "/shop/admin/orders/" + order.getOrderNumber() + "/payment/verify";
        mockMvc.perform(post(url)).andExpect(status().isUnauthorized());
        mockMvc.perform(post(url).with(user("member").roles("USER"))).andExpect(status().isForbidden());
        verifyNoInteractions(paymentService);
        mockMvc.perform(post(url).with(user("manager").roles("RESPONSABLE_BOUTIQUE")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));
        verify(paymentService).verifyPaymentForManager(order.getOrderNumber());
    }

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
                .andExpect(jsonPath("$[0].items[0].productVariantId").value(20))
                .andExpect(jsonPath("$[0].items[0].productName").value("Tee-shirt"))
                .andExpect(jsonPath("$[0].items[0].status").value("PENDING"));
    }

    @Test
    void changesItemStatusOnlyForShopManager() throws Exception {
        OrderItem item = OrderItem.snapshot(10L, "Tee-shirt", 20L, "M", "TS-M", new Money(1_500, "EUR"), 1);
        ReflectionTestUtils.setField(item, "id", 7L);
        ShopOrder order = new ShopOrder("CMD-2026-000001", 42L, "EUR");
        order.addItem(item);
        order.submitForPayment();
        order.transitionTo(OrderStatus.PAID);
        order.transitionTo(OrderStatus.PROCESSING);
        given(orderService.updateItemStatus("CMD-2026-000001", 7L, OrderItemStatus.PROCESSING)).willReturn(order);

        mockMvc.perform(put("/shop/admin/orders/CMD-2026-000001/items/7/status")
                        .with(user("member@example.test").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PROCESSING\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/shop/admin/orders/CMD-2026-000001/items/7/status")
                        .with(user("manager@example.test").roles("RESPONSABLE_BOUTIQUE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PROCESSING\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderNumber").value("CMD-2026-000001"))
                .andExpect(jsonPath("$.status").value("PROCESSING"))
                .andExpect(jsonPath("$.items[0].id").value(7))
                .andExpect(jsonPath("$.items[0].status").value("PROCESSING"));
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

    @Test
    void updatesOnlyVariantStockForShopManager() throws Exception {
        Product product = new Product("Tee-shirt", "tee-shirt", null, true, 0);
        ProductVariant variant = new ProductVariant(product, "TS-M", "M", new Money(1_500, "EUR"), true, 0);
        ReflectionTestUtils.setField(variant, "id", 20L);
        variant.trackStock(5);
        given(catalogService.updateVariantStock(20L, 5L, 0L)).willReturn(variant);
        given(catalogService.updateVariantStock(20L, 5L, 1L))
                .willThrow(new IllegalStateException("Le stock a changé depuis son chargement"));

        mockMvc.perform(put("/shop/admin/variants/20/stock")
                        .with(user("member@example.test").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"stockOnHand\":5,\"expectedVersion\":0}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/shop/admin/variants/20/stock")
                        .with(user("manager@example.test").roles("RESPONSABLE_BOUTIQUE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"stockOnHand\":-1,\"expectedVersion\":0}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/shop/admin/variants/20/stock")
                        .with(user("manager@example.test").roles("RESPONSABLE_BOUTIQUE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"stockOnHand\":5}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/shop/admin/variants/20/stock")
                        .with(user("manager@example.test").roles("RESPONSABLE_BOUTIQUE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"stockOnHand\":5,\"expectedVersion\":1}"))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/shop/admin/variants/20/stock")
                        .with(user("manager@example.test").roles("RESPONSABLE_BOUTIQUE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"stockOnHand\":5,\"expectedVersion\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.stockOnHand").value(5))
                .andExpect(jsonPath("$.version").value(0));
        verify(catalogService).updateVariantStock(20L, 5L, 0L);
    }

    @Test
    void createsVariantWithoutStockChoice() throws Exception {
        Product product = new Product("Tee-shirt", "tee-shirt", null, true, 0);
        ProductVariant variant = new ProductVariant(product, "TS-M", "M", new Money(1_500, "EUR"), true, 0);
        variant.trackStock(0);
        given(catalogService.createVariant(10L, "TS-M", "M", new Money(1_500, "EUR"), true, 0))
                .willReturn(variant);

        mockMvc.perform(post("/shop/admin/products/10/variants")
                        .with(user("manager@example.test").roles("RESPONSABLE_BOUTIQUE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"TS-M\",\"label\":\"M\",\"priceAmountInCents\":1500,"
                                + "\"currency\":\"EUR\",\"active\":true,\"displayOrder\":0}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stockTracked").value(true))
                .andExpect(jsonPath("$.stockOnHand").value(0));
        verify(catalogService).createVariant(10L, "TS-M", "M", new Money(1_500, "EUR"), true, 0);
    }

    @Test
    void createsAndReceivesSupplierOrderForShopManager() throws Exception {
        Product product = new Product("Tee-shirt", "tee-shirt", null, true, 0);
        ProductVariant variant = new ProductVariant(product, "TS-M", "M", new Money(1_500, "EUR"), true, 0);
        ReflectionTestUtils.setField(variant, "id", 20L);
        SupplierOrder order = new SupplierOrder("Fournisseur A", "BC-1");
        order.addLine(variant, 5);
        ReflectionTestUtils.setField(order, "id", 30L);
        given(supplierOrderService.create("Fournisseur A", "BC-1",
                List.of(new SupplierOrderService.Line(20L, 5, null, false)))).willReturn(order);
        given(supplierOrderService.receive(30L)).willAnswer(invocation -> {
            order.markReceived();
            return order;
        });

        String body = "{\"supplierName\":\"Fournisseur A\",\"reference\":\"BC-1\","
                + "\"lines\":[{\"variantId\":20,\"quantity\":5}]}";
        mockMvc.perform(post("/shop/admin/supplier-orders")
                        .with(user("member@example.test").roles("USER"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/shop/admin/supplier-orders")
                        .with(user("manager@example.test").roles("RESPONSABLE_BOUTIQUE"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ORDERED"))
                .andExpect(jsonPath("$.lines[0].quantity").value(5));
        mockMvc.perform(put("/shop/admin/supplier-orders/30/receive")
                        .with(user("manager@example.test").roles("RESPONSABLE_BOUTIQUE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECEIVED"));
    }

    @Test
    void savesAndPlacesSupplierDraftWithLineAmount() throws Exception {
        Product product = new Product("Tee-shirt", "tee-shirt", null, true, 0);
        ProductVariant variant = new ProductVariant(product, "TS-M", "M", new Money(1_500, "EUR"), true, 0);
        ReflectionTestUtils.setField(variant, "id", 20L);
        SupplierOrder draft = new SupplierOrder("Fournisseur A", null, true);
        draft.addLine(variant, 5).setPurchaseDetails(800L, "EUR", 5, false);
        ReflectionTestUtils.setField(draft, "id", 30L);
        given(supplierOrderService.createDraft("Fournisseur A", null,
                List.of(new SupplierOrderService.Line(20L, 5, 800L, false)))).willReturn(draft);
        given(supplierOrderService.placeDraft(30L)).willAnswer(invocation -> {
            draft.markOrdered();
            return draft;
        });

        String body = "{\"supplierName\":\"Fournisseur A\",\"lines\":[{\"variantId\":20,"
                + "\"quantity\":5,\"unitCostAmountInCents\":800,\"extraApproved\":false}]}";
        mockMvc.perform(post("/shop/admin/supplier-orders/draft")
                        .with(user("manager@example.test").roles("RESPONSABLE_BOUTIQUE"))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.lines[0].lineTotalAmountInCents").value(4000));
        mockMvc.perform(put("/shop/admin/supplier-orders/30/place")
                        .with(user("manager@example.test").roles("RESPONSABLE_BOUTIQUE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ORDERED"));
    }

    @Test
    void requiresCurrentVersionWhenUpdatingVariantDetails() throws Exception {
        Product product = new Product("Tee-shirt", "tee-shirt", null, true, 0);
        ProductVariant variant = new ProductVariant(product, "TS-M", "M", new Money(1_500, "EUR"), true, 0);
        ReflectionTestUtils.setField(variant, "id", 20L);
        variant.trackStock(5);
        Money price = new Money(1_500, "EUR");
        given(catalogService.updateVariant(20L, "TS-M", "M", price, true, 0, true, null))
                .willThrow(new IllegalArgumentException("La version est obligatoire"));
        given(catalogService.updateVariant(20L, "TS-M", "M", price, true, 0, true, 1L))
                .willThrow(new IllegalStateException("La variante a changé depuis son chargement"));
        given(catalogService.updateVariant(20L, "TS-M", "M", price, true, 0, true, 0L))
                .willReturn(variant);

        String details = "\"sku\":\"TS-M\",\"label\":\"M\",\"priceAmountInCents\":1500,"
                + "\"currency\":\"EUR\",\"active\":true,\"displayOrder\":0,"
                + "\"stockTracked\":true";
        mockMvc.perform(put("/shop/admin/variants/20")
                        .with(user("manager@example.test").roles("RESPONSABLE_BOUTIQUE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{" + details + "}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/shop/admin/variants/20")
                        .with(user("manager@example.test").roles("RESPONSABLE_BOUTIQUE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{" + details + ",\"expectedVersion\":1}"))
                .andExpect(status().isConflict());
        mockMvc.perform(put("/shop/admin/variants/20")
                        .with(user("manager@example.test").roles("RESPONSABLE_BOUTIQUE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{" + details + ",\"expectedVersion\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(0));
        verify(catalogService).updateVariant(20L, "TS-M", "M", price, true, 0, true, 0L);
    }
}
