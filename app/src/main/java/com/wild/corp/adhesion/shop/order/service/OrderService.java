package com.wild.corp.adhesion.shop.order.service;

import com.wild.corp.adhesion.shop.catalog.model.Product;
import com.wild.corp.adhesion.shop.catalog.model.ProductVariant;
import com.wild.corp.adhesion.shop.catalog.repository.ProductVariantRepository;
import com.wild.corp.adhesion.shop.common.exception.InvalidQuantityException;
import com.wild.corp.adhesion.shop.order.model.OrderItem;
import com.wild.corp.adhesion.shop.order.model.ShopOrder;
import com.wild.corp.adhesion.shop.order.repository.ShopOrderRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Comparator;

@Service
@Transactional
public class OrderService {

    private final ShopOrderRepository orderRepository;
    private final ProductVariantRepository variantRepository;
    private final OrderNumberGenerator orderNumberGenerator;

    public OrderService(ShopOrderRepository orderRepository,
                        ProductVariantRepository variantRepository,
                        OrderNumberGenerator orderNumberGenerator) {
        this.orderRepository = orderRepository;
        this.variantRepository = variantRepository;
        this.orderNumberGenerator = orderNumberGenerator;
    }

    public ShopOrder createOrder(Long customerUserId, List<OrderItemRequest> requestedItems) {
        Map<Long, Integer> quantities = normalizeQuantities(requestedItems);
        Map<Long, ProductVariant> variantsById = new LinkedHashMap<>();
        quantities.keySet().stream().sorted(Comparator.naturalOrder()).forEach(variantId -> {
            ProductVariant variant = variantRepository.findByIdForUpdate(variantId)
                    .orElseThrow(() -> new IllegalArgumentException("La variante " + variantId + " est introuvable"));
            variantsById.put(variantId, variant);
        });

        for (Map.Entry<Long, Integer> requested : quantities.entrySet()) {
            variantsById.get(requested.getKey()).ensureOrderable(requested.getValue());
        }

        ProductVariant firstVariant = variantsById.get(quantities.keySet().iterator().next());
        ShopOrder order = new ShopOrder(orderNumberGenerator.nextOrderNumber(), customerUserId,
                firstVariant.getPrice().getCurrency());

        for (Map.Entry<Long, Integer> requested : quantities.entrySet()) {
            ProductVariant variant = variantsById.get(requested.getKey());
            int quantity = requested.getValue();
            boolean stockReserved = variant.reserveStock(quantity);
            Product product = variant.getProduct();
            order.addItem(OrderItem.snapshot(
                    product.getId(),
                    product.getName(),
                    variant.getId(),
                    variant.getLabel(),
                    variant.getSku(),
                    variant.getPrice(),
                    quantity,
                    stockReserved));
        }

        order.submitForPayment();
        return orderRepository.save(order);
    }

    public ShopOrder transition(Long orderId, com.wild.corp.adhesion.shop.order.model.OrderStatus targetStatus) {
        ShopOrder order = orderRepository.findById(orderId).orElseThrow();
        order.transitionTo(targetStatus);
        if (targetStatus == com.wild.corp.adhesion.shop.order.model.OrderStatus.CANCELLED
                || targetStatus == com.wild.corp.adhesion.shop.order.model.OrderStatus.EXPIRED) {
            releaseReservedStock(order);
        }
        return order;
    }

    public void consumeReservedStock(ShopOrder order) {
        forEachReservedVariant(order, ProductVariant::consumeReservedStock);
    }

    private void releaseReservedStock(ShopOrder order) {
        forEachReservedVariant(order, ProductVariant::releaseStock);
    }

    private void forEachReservedVariant(ShopOrder order, StockOperation operation) {
        order.getItems().stream()
                .filter(OrderItem::isStockReserved)
                .sorted(Comparator.comparing(OrderItem::getProductVariantId))
                .forEach(item -> {
                    ProductVariant variant = variantRepository.findByIdForUpdate(item.getProductVariantId())
                            .orElseThrow(() -> new IllegalStateException("La variante réservée est introuvable"));
                    operation.apply(variant, item.getQuantity());
                });
    }

    private Map<Long, Integer> normalizeQuantities(List<OrderItemRequest> requestedItems) {
        if (requestedItems == null || requestedItems.isEmpty()) {
            throw new IllegalArgumentException("Une commande doit contenir au moins un article");
        }
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (OrderItemRequest requestedItem : new ArrayList<>(requestedItems)) {
            if (requestedItem == null || requestedItem.variantId() == null) {
                throw new IllegalArgumentException("La variante est obligatoire");
            }
            if (requestedItem.quantity() <= 0) {
                throw new InvalidQuantityException(requestedItem.quantity());
            }
            quantities.merge(requestedItem.variantId(), requestedItem.quantity(), Math::addExact);
        }
        return quantities;
    }

    @FunctionalInterface
    private interface StockOperation {
        void apply(ProductVariant variant, int quantity);
    }
}
