package com.wild.corp.adhesion.shop.order.service;

import com.wild.corp.adhesion.shop.catalog.model.Product;
import com.wild.corp.adhesion.shop.catalog.model.ProductVariant;
import com.wild.corp.adhesion.shop.catalog.repository.ProductVariantRepository;
import com.wild.corp.adhesion.shop.common.exception.InvalidQuantityException;
import com.wild.corp.adhesion.shop.common.exception.ProductNotOrderableException;
import com.wild.corp.adhesion.shop.common.money.Money;
import com.wild.corp.adhesion.shop.order.model.ShopOrder;
import com.wild.corp.adhesion.shop.order.model.OrderStatus;
import com.wild.corp.adhesion.shop.order.repository.ShopOrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private ShopOrderRepository orderRepository;

    @Mock
    private ProductVariantRepository variantRepository;

    @Mock
    private OrderNumberGenerator orderNumberGenerator;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, variantRepository, orderNumberGenerator);
    }

    @Test
    void rejectsInactiveProduct() {
        Product product = product(10L, false);
        ProductVariant variant = variant(20L, product, new Money(1_500, "EUR"));
        when(variantRepository.findByIdForUpdate(20L)).thenReturn(java.util.Optional.of(variant));

        assertThatThrownBy(() -> orderService.createOrder(42L, List.of(new OrderItemRequest(20L, 1))))
                .isInstanceOf(ProductNotOrderableException.class);

        verify(orderRepository, never()).save(any());
    }

    @Test
    void keepsPriceSnapshotWhenCatalogPriceChanges() {
        Product product = product(10L, true);
        ProductVariant variant = variant(20L, product, new Money(1_500, "EUR"));
        when(variantRepository.findByIdForUpdate(20L)).thenReturn(java.util.Optional.of(variant));
        when(orderNumberGenerator.nextOrderNumber()).thenReturn("CMD-2026-000001");
        when(orderRepository.save(any(ShopOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShopOrder order = orderService.createOrder(42L, List.of(new OrderItemRequest(20L, 2)));
        variant.changePrice(new Money(2_000, "EUR"));

        assertThat(order.getItems().getFirst().getUnitPrice()).isEqualTo(new Money(1_500, "EUR"));
        assertThat(order.getItems().getFirst().getLineTotal()).isEqualTo(new Money(3_000, "EUR"));
        assertThat(order.getTotal()).isEqualTo(new Money(3_000, "EUR"));
    }

    @Test
    void rejectsInvalidQuantityBeforeReadingCatalog() {
        assertThatThrownBy(() -> orderService.createOrder(42L, List.of(new OrderItemRequest(20L, -1))))
                .isInstanceOf(InvalidQuantityException.class);

        verify(variantRepository, never()).findByIdForUpdate(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void reservesAndReleasesTrackedStock() {
        Product product = product(10L, true);
        ProductVariant variant = variant(20L, product, new Money(1_500, "EUR"));
        variant.trackStock(5);
        when(variantRepository.findByIdForUpdate(20L)).thenReturn(java.util.Optional.of(variant));
        when(orderNumberGenerator.nextOrderNumber()).thenReturn("CMD-2026-000001");
        when(orderRepository.save(any(ShopOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShopOrder order = orderService.createOrder(42L, List.of(new OrderItemRequest(20L, 2)));
        ReflectionTestUtils.setField(order, "id", 99L);

        assertThat(variant.getStockReserved()).isEqualTo(2);
        assertThat(variant.availableStock()).isEqualTo(3);

        when(orderRepository.findById(99L)).thenReturn(java.util.Optional.of(order));
        orderService.transition(99L, OrderStatus.CANCELLED);

        assertThat(variant.getStockReserved()).isZero();
        assertThat(variant.availableStock()).isEqualTo(5);
    }

    private Product product(Long id, boolean active) {
        Product product = new Product("Tee-shirt", "tee-shirt", null, active, 0);
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private ProductVariant variant(Long id, Product product, Money price) {
        ProductVariant variant = new ProductVariant(product, "TS-M", "M", price, true, 0);
        ReflectionTestUtils.setField(variant, "id", id);
        return variant;
    }
}
