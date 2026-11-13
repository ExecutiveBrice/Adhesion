package com.wild.corp.adhesion.shop.order.service;

import com.wild.corp.adhesion.shop.catalog.model.Product;
import com.wild.corp.adhesion.shop.catalog.model.ProductVariant;
import com.wild.corp.adhesion.shop.catalog.repository.ProductVariantRepository;
import com.wild.corp.adhesion.shop.common.exception.InvalidQuantityException;
import com.wild.corp.adhesion.shop.common.exception.ProductNotOrderableException;
import com.wild.corp.adhesion.shop.common.money.Money;
import com.wild.corp.adhesion.shop.order.model.ShopOrder;
import com.wild.corp.adhesion.shop.order.model.OrderItem;
import com.wild.corp.adhesion.shop.order.model.OrderItemStatus;
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

import java.util.Optional;

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

    @Test
    void acceptsOrderAtZeroStockAndKeepsItsReservation() {
        Product product = product(10L, true);
        ProductVariant variant = variant(20L, product, new Money(1_500, "EUR"));
        variant.trackStock(0);
        when(variantRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(variant));
        when(orderNumberGenerator.nextOrderNumber()).thenReturn("CMD-2026-000001");
        when(orderRepository.save(any(ShopOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShopOrder order = orderService.createOrder(42L, List.of(new OrderItemRequest(20L, 2)));

        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(order.getItems().getFirst().isStockReserved()).isTrue();
        assertThat(variant.getStockReserved()).isEqualTo(2);
        assertThat(variant.availableStock()).isEqualTo(-2);
    }

    @Test
    void returnsExistingOrderForTheSameCheckoutKeyWithoutCreatingAnotherOne() {
        ShopOrder existingOrder = new ShopOrder("CMD-2026-000001", 42L, "checkout-1", "EUR");
        when(orderRepository.findByCustomerUserIdAndCheckoutKey(42L, "checkout-1"))
                .thenReturn(java.util.Optional.of(existingOrder));

        ShopOrder result = orderService.createOrder(42L, "checkout-1", List.of(new OrderItemRequest(20L, 1)));

        assertThat(result).isSameAs(existingOrder);
        verify(variantRepository, never()).findByIdForUpdate(any());
        verify(orderRepository, never()).save(any());
    }

    @Test
    void updatesAdminOrderStatusAndItsItems() {
        ShopOrder order = new ShopOrder("CMD-2026-000001", 42L, "EUR");
        OrderItem item = OrderItem.snapshot(10L, "Tee-shirt", 20L, "M", "TS-M", new Money(1_500, "EUR"), 1);
        order.addItem(item);
        order.submitForPayment();
        order.transitionTo(OrderStatus.PAID);
        item.setStatus(OrderItemStatus.PENDING);
        when(orderRepository.findByOrderNumberForUpdate(order.getOrderNumber())).thenReturn(Optional.of(order));

        orderService.updateAdminOrderStatus(order.getOrderNumber(), OrderStatus.PROCESSING);
        assertThat(item.getStatus()).isEqualTo(OrderItemStatus.PROCESSING);
        orderService.updateAdminOrderStatus(order.getOrderNumber(), OrderStatus.COMPLETED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(item.getStatus()).isEqualTo(OrderItemStatus.COMPLETED);
    }

    @Test
    void cancelsPendingPaymentOrderAndItsItemsAndReleasesStock() {
        Product product = product(10L, true);
        ProductVariant variant = variant(20L, product, new Money(1_500, "EUR"));
        variant.trackStock(5);
        when(variantRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(variant));
        when(orderNumberGenerator.nextOrderNumber()).thenReturn("CMD-2026-000001");
        when(orderRepository.save(any(ShopOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShopOrder order = orderService.createOrder(42L, List.of(new OrderItemRequest(20L, 2)));
        OrderItem item = order.getItems().getFirst();
        when(orderRepository.findByOrderNumberForUpdate(order.getOrderNumber())).thenReturn(Optional.of(order));

        orderService.updateAdminOrderStatus(order.getOrderNumber(), OrderStatus.CANCELLED);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(item.getStatus()).isEqualTo(OrderItemStatus.CANCELLED);
        assertThat(variant.getStockReserved()).isZero();
        assertThat(variant.availableStock()).isEqualTo(5);
    }

    @Test
    void customerCancelsOnlyTheirPendingPaymentOrderAndReleasesItsStock() {
        Product product = product(10L, true);
        ProductVariant variant = variant(20L, product, new Money(1_500, "EUR"));
        variant.trackStock(5);
        when(variantRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(variant));
        when(orderNumberGenerator.nextOrderNumber()).thenReturn("CMD-2026-000001");
        when(orderRepository.save(any(ShopOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ShopOrder order = orderService.createOrder(42L, List.of(new OrderItemRequest(20L, 2)));
        when(orderRepository.findByOrderNumber(order.getOrderNumber())).thenReturn(Optional.of(order));

        orderService.cancelOrderForCustomer(order.getOrderNumber(), 42L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(order.getItems().getFirst().getStatus()).isEqualTo(OrderItemStatus.CANCELLED);
        assertThat(variant.getStockReserved()).isZero();
        assertThat(variant.availableStock()).isEqualTo(5);
    }

    @Test
    void customerCannotCancelAnAlreadyPaidOrder() {
        ShopOrder order = new ShopOrder("CMD-2026-000001", 42L, "EUR");
        order.addItem(OrderItem.snapshot(10L, "Tee-shirt", 20L, "M", "TS-M", new Money(1_500, "EUR"), 1));
        order.submitForPayment();
        order.transitionTo(OrderStatus.PAID);
        when(orderRepository.findByOrderNumber(order.getOrderNumber())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrderForCustomer(order.getOrderNumber(), 42L))
                .isInstanceOf(IllegalStateException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
    }

    @Test
    void customerCanRequestRefundForTheirPaidOrderOnly() {
        ShopOrder order = new ShopOrder("CMD-2026-000001", 42L, "EUR");
        order.addItem(OrderItem.snapshot(10L, "Tee-shirt", 20L, "M", "TS-M", new Money(1_500, "EUR"), 1));
        order.submitForPayment();
        order.transitionTo(OrderStatus.PAID);
        when(orderRepository.findByOrderNumber(order.getOrderNumber())).thenReturn(Optional.of(order));

        orderService.requestRefundForCustomer(order.getOrderNumber(), 42L);

        assertThat(order.isRefundRequested()).isTrue();
    }

    @Test
    void customerCannotRequestRefundBeforePayment() {
        ShopOrder order = new ShopOrder("CMD-2026-000001", 42L, "EUR");
        order.addItem(OrderItem.snapshot(10L, "Tee-shirt", 20L, "M", "TS-M", new Money(1_500, "EUR"), 1));
        order.submitForPayment();
        when(orderRepository.findByOrderNumber(order.getOrderNumber())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.requestRefundForCustomer(order.getOrderNumber(), 42L))
                .isInstanceOf(IllegalStateException.class);
        assertThat(order.isRefundRequested()).isFalse();
    }

    @Test
    void rejectsPaymentStatusChangesFromAdmin() {
        ShopOrder order = new ShopOrder("CMD-2026-000001", 42L, "EUR");
        when(orderRepository.findByOrderNumberForUpdate(order.getOrderNumber())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateAdminOrderStatus(order.getOrderNumber(), OrderStatus.PAID))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.DRAFT);
    }

    @Test
    void updatesOnlyAnItemBelongingToTheRequestedOrder() {
        ShopOrder order = new ShopOrder("CMD-2026-000001", 42L, "EUR");
        OrderItem item = OrderItem.snapshot(10L, "Tee-shirt", 20L, "M", "TS-M", new Money(1_500, "EUR"), 1);
        order.addItem(item);
        ReflectionTestUtils.setField(item, "id", 7L);
        order.submitForPayment();
        order.transitionTo(OrderStatus.PAID);
        when(orderRepository.findByOrderNumberForUpdate(order.getOrderNumber())).thenReturn(Optional.of(order));

        assertThat(orderService.updateItemStatus(order.getOrderNumber(), 7L, OrderItemStatus.PROCESSING))
                .isSameAs(order);
        assertThat(item.getStatus()).isEqualTo(OrderItemStatus.PROCESSING);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        assertThatThrownBy(() -> orderService.updateItemStatus(order.getOrderNumber(), 8L, OrderItemStatus.COMPLETED))
                .isInstanceOf(java.util.NoSuchElementException.class);
        assertThat(item.getStatus()).isEqualTo(OrderItemStatus.PROCESSING);
    }

    @Test
    void completesOrderWhenItsLastItemIsCompleted() {
        ShopOrder order = new ShopOrder("CMD-2026-000002", 42L, "EUR");
        OrderItem first = OrderItem.snapshot(10L, "Tee-shirt", 20L, "M", "TS-M", new Money(1_500, "EUR"), 1);
        OrderItem second = OrderItem.snapshot(11L, "Casquette", 21L, "Unique", "CAP", new Money(1_000, "EUR"), 1);
        order.addItem(first);
        order.addItem(second);
        ReflectionTestUtils.setField(first, "id", 7L);
        ReflectionTestUtils.setField(second, "id", 8L);
        order.submitForPayment();
        order.transitionTo(OrderStatus.PAID);
        when(orderRepository.findByOrderNumberForUpdate(order.getOrderNumber())).thenReturn(Optional.of(order));

        orderService.updateItemStatus(order.getOrderNumber(), 7L, OrderItemStatus.COMPLETED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(first.getStatus()).isEqualTo(OrderItemStatus.COMPLETED);
        assertThat(second.getStatus()).isEqualTo(OrderItemStatus.PENDING);

        orderService.updateItemStatus(order.getOrderNumber(), 8L, OrderItemStatus.COMPLETED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(order.getItems()).allMatch(item -> item.getStatus() == OrderItemStatus.COMPLETED);
    }

    @Test
    void preparingOneItemDoesNotPrepareTheOtherItems() {
        ShopOrder order = new ShopOrder("CMD-2026-000003", 42L, "EUR");
        OrderItem first = OrderItem.snapshot(10L, "Tee-shirt", 20L, "M", "TS-M", new Money(1_500, "EUR"), 1);
        OrderItem second = OrderItem.snapshot(11L, "Casquette", 21L, "Unique", "CAP", new Money(1_000, "EUR"), 1);
        order.addItem(first);
        order.addItem(second);
        ReflectionTestUtils.setField(first, "id", 7L);
        ReflectionTestUtils.setField(second, "id", 8L);
        order.submitForPayment();
        order.transitionTo(OrderStatus.PAID);
        when(orderRepository.findByOrderNumberForUpdate(order.getOrderNumber())).thenReturn(Optional.of(order));

        orderService.updateItemStatus(order.getOrderNumber(), 7L, OrderItemStatus.PROCESSING);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(second.getStatus()).isEqualTo(OrderItemStatus.PENDING);

        orderService.updateItemStatus(order.getOrderNumber(), 8L, OrderItemStatus.PROCESSING);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PROCESSING);
        assertThat(order.getItems()).allMatch(item -> item.getStatus() == OrderItemStatus.PROCESSING);
    }

    @Test
    void refusesPaidItemCancellationWithoutChangingTotalOrStock() {
        ShopOrder order = new ShopOrder("CMD-2026-000002", 42L, "EUR");
        OrderItem item = OrderItem.snapshot(10L, "Tee-shirt", 20L, "M", "TS-M", new Money(1_500, "EUR"), 1);
        order.addItem(item);
        ReflectionTestUtils.setField(item, "id", 7L);
        order.submitForPayment();
        order.transitionTo(OrderStatus.PAID);
        when(orderRepository.findByOrderNumberForUpdate(order.getOrderNumber())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateItemStatus(order.getOrderNumber(), 7L, OrderItemStatus.CANCELLED))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("remboursement");
        assertThat(order.getTotal()).isEqualTo(new Money(1_500, "EUR"));
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PAID);
        assertThat(item.getStatus()).isEqualTo(OrderItemStatus.PENDING);
    }

    @Test
    void cancellingTheOnlyUnpaidItemCancelsTheOrderAndReleasesItsReservation() {
        Product product = product(10L, true);
        ProductVariant variant = variant(20L, product, new Money(1_500, "EUR"));
        variant.trackStock(5);
        when(variantRepository.findByIdForUpdate(20L)).thenReturn(Optional.of(variant));
        when(orderNumberGenerator.nextOrderNumber()).thenReturn("CMD-2026-000001");
        when(orderRepository.save(any(ShopOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ShopOrder order = orderService.createOrder(42L, List.of(new OrderItemRequest(20L, 2)));
        OrderItem item = order.getItems().getFirst();
        ReflectionTestUtils.setField(item, "id", 7L);
        when(orderRepository.findByOrderNumberForUpdate(order.getOrderNumber())).thenReturn(Optional.of(order));

        assertThat(orderService.updateItemStatus(order.getOrderNumber(), 7L, OrderItemStatus.CANCELLED)).isSameAs(order);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(item.getStatus()).isEqualTo(OrderItemStatus.CANCELLED);
        assertThat(variant.getStockReserved()).isZero();
        assertThat(variant.getStockOnHand()).isEqualTo(5);
        assertThat(order.getTotal()).isEqualTo(new Money(3_000, "EUR"));
    }

    @Test
    void refusesPartialCancellationOfAnUnpaidOrder() {
        ShopOrder order = new ShopOrder("CMD-2026-000002", 42L, "EUR");
        OrderItem first = OrderItem.snapshot(10L, "Tee-shirt", 20L, "M", "TS-M", new Money(1_500, "EUR"), 1);
        order.addItem(first);
        order.addItem(OrderItem.snapshot(11L, "Casquette", 21L, "Unique", "CAP", new Money(1_000, "EUR"), 1));
        ReflectionTestUtils.setField(first, "id", 7L);
        order.submitForPayment();
        when(orderRepository.findByOrderNumberForUpdate(order.getOrderNumber())).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateItemStatus(order.getOrderNumber(), 7L, OrderItemStatus.CANCELLED))
                .isInstanceOf(IllegalStateException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING_PAYMENT);
        assertThat(first.getStatus()).isEqualTo(OrderItemStatus.PENDING);
        assertThat(order.getTotal()).isEqualTo(new Money(2_500, "EUR"));
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
