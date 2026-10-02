package com.wild.corp.adhesion.shop.catalog.service;

import com.wild.corp.adhesion.shop.catalog.model.Product;
import com.wild.corp.adhesion.shop.catalog.model.ProductVariant;
import com.wild.corp.adhesion.shop.catalog.model.SupplierOrder;
import com.wild.corp.adhesion.shop.catalog.model.SupplierOrderStatus;
import com.wild.corp.adhesion.shop.catalog.repository.ProductVariantRepository;
import com.wild.corp.adhesion.shop.catalog.repository.SupplierOrderRepository;
import com.wild.corp.adhesion.shop.common.money.Money;
import com.wild.corp.adhesion.shop.order.model.OrderItem;
import com.wild.corp.adhesion.shop.order.model.OrderItemStatus;
import com.wild.corp.adhesion.shop.order.model.OrderStatus;
import com.wild.corp.adhesion.shop.order.model.ShopOrder;
import com.wild.corp.adhesion.shop.order.repository.ShopOrderRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SupplierOrderServiceTest {

    @Test
    void addsStockOnlyWhenOrderIsReceivedAndOnlyOnce() {
        Product product = new Product("Tee-shirt", "tee-shirt", null, true, 0);
        ProductVariant variant = new ProductVariant(product, "TS-M", "M", new Money(1_500, "EUR"), true, 0);
        ReflectionTestUtils.setField(variant, "id", 20L);
        variant.trackStock(2);
        SupplierOrderRepository orders = mock(SupplierOrderRepository.class);
        ProductVariantRepository variants = mock(ProductVariantRepository.class);
        when(variants.findByIdForUpdate(20L)).thenReturn(Optional.of(variant));
        when(orders.save(any(SupplierOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SupplierOrderService service = new SupplierOrderService(orders, variants, mock(ShopOrderRepository.class));

        SupplierOrder order = service.create("Fournisseur A", "BC-2026-01",
                List.of(new SupplierOrderService.Line(20L, 5)));
        ReflectionTestUtils.setField(order, "id", 30L);
        when(orders.findByIdForUpdate(30L)).thenReturn(Optional.of(order));

        assertThat(variant.getStockOnHand()).isEqualTo(2);
        assertThat(order.getLines()).hasSize(1);
        assertThat(order.getLines().getFirst().getQuantity()).isEqualTo(5);
        assertThatThrownBy(() -> service.receive(30L))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("facture");

        service.complete(30L, "FAC-1", List.of(new SupplierOrderService.PricedLine(20L, 800L)));
        assertThat(order.getInvoiceReference()).isEqualTo("FAC-1");
        assertThat(order.getLines().getFirst().getUnitCostAmountInCents()).isEqualTo(800L);

        service.receive(30L);

        assertThat(variant.getStockOnHand()).isEqualTo(7);
        assertThat(order.getStatus()).isEqualTo(SupplierOrderStatus.RECEIVED);
        assertThatThrownBy(() -> service.receive(30L))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("déjà été réceptionnée");
        assertThat(variant.getStockOnHand()).isEqualTo(7);
    }

    @Test
    void rejectsDuplicateVariants() {
        SupplierOrderService service = new SupplierOrderService(mock(SupplierOrderRepository.class),
                mock(ProductVariantRepository.class), mock(ShopOrderRepository.class));

        assertThatThrownBy(() -> service.create("Fournisseur A", null,
                List.of(new SupplierOrderService.Line(20L, 2), new SupplierOrderService.Line(20L, 3))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void draftDoesNotCountAsIncomingAndPlaceRequiresFreshNeed() {
        Product product = new Product("Tee-shirt", "tee-shirt", null, true, 0);
        ProductVariant variant = new ProductVariant(product, "TS-M", "M", new Money(1_500, "EUR"), true, 0);
        ReflectionTestUtils.setField(variant, "id", 20L);
        variant.trackStock(0);
        variant.updateStockOnHand(-5, 0);
        SupplierOrderRepository orders = mock(SupplierOrderRepository.class);
        ProductVariantRepository variants = mock(ProductVariantRepository.class);
        when(variants.findByIdForUpdate(20L)).thenReturn(Optional.of(variant));
        when(orders.findAllByOrderByCreatedAtDescIdDesc()).thenReturn(List.of());
        when(orders.save(any(SupplierOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ShopOrderRepository customerOrders = mock(ShopOrderRepository.class);
        ShopOrder customerOrder = mock(ShopOrder.class);
        OrderItem customerItem = mock(OrderItem.class);
        when(customerOrder.getStatus()).thenReturn(OrderStatus.PAID);
        when(customerOrder.getItems()).thenReturn(List.of(customerItem));
        when(customerItem.getProductVariantId()).thenReturn(20L);
        when(customerItem.getStatus()).thenReturn(OrderItemStatus.PENDING);
        when(customerItem.getQuantity()).thenReturn(5);
        when(customerOrders.findAllByOrderByCreatedAtDescIdDesc()).thenReturn(List.of(customerOrder));
        SupplierOrderService service = new SupplierOrderService(orders, variants, customerOrders);

        SupplierOrder draft = service.createDraft("Fournisseur A", null,
                List.of(new SupplierOrderService.Line(20L, 5)));
        ReflectionTestUtils.setField(draft, "id", 30L);
        when(orders.findByIdForUpdate(30L)).thenReturn(Optional.of(draft));
        assertThat(draft.getStatus()).isEqualTo(SupplierOrderStatus.DRAFT);
        assertThat(draft.getLines().getFirst().getExpectedNeed()).isEqualTo(5);
        assertThat(draft.getLines().getFirst().getLineTotalAmountInCents()).isNull();
        assertThat(variant.getStockOnHand()).isEqualTo(-5);

        SupplierOrder other = new SupplierOrder("Autre fournisseur", null);
        other.addLine(variant, 5);
        when(orders.findAllByOrderByCreatedAtDescIdDesc()).thenReturn(List.of(draft, other));
        assertThatThrownBy(() -> service.placeDraft(30L)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("besoins ont changé");
        assertThat(draft.getStatus()).isEqualTo(SupplierOrderStatus.DRAFT);

        service.updateDraft(30L, "Fournisseur A", null,
                List.of(new SupplierOrderService.Line(20L, 2)));
        assertThat(draft.getLines().getFirst().getExpectedNeed()).isZero();
        service.placeDraft(30L);
        assertThat(draft.getStatus()).isEqualTo(SupplierOrderStatus.ORDERED);
        assertThat(variant.getStockOnHand()).isEqualTo(-5);
    }
}
