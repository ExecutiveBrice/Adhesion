package com.wild.corp.adhesion.shop.cart.service;

import com.wild.corp.adhesion.shop.catalog.model.Product;
import com.wild.corp.adhesion.shop.catalog.model.ProductVariant;
import com.wild.corp.adhesion.shop.catalog.repository.ProductVariantRepository;
import com.wild.corp.adhesion.shop.common.exception.InvalidQuantityException;
import com.wild.corp.adhesion.shop.common.money.Money;
import com.wild.corp.adhesion.shop.order.model.OrderTotalsCalculator;
import com.wild.corp.adhesion.shop.order.service.OrderItemRequest;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Builds an authoritative, non-reserving cart quotation from catalog data.
 * Promotions and fees are intentionally explicit and currently zero until their
 * respective domain rules are introduced.
 */
@Service
@Transactional(Transactional.TxType.SUPPORTS)
public class CartPricingService {

    private final ProductVariantRepository variantRepository;

    public CartPricingService(ProductVariantRepository variantRepository) {
        this.variantRepository = variantRepository;
    }

    public CartQuote quote(List<OrderItemRequest> requestedItems) {
        Map<Long, Integer> quantities = normalizeQuantities(requestedItems);
        List<ProductVariant> variants = variantRepository.findAllWithProductByIdIn(quantities.keySet());
        Map<Long, ProductVariant> variantsById = new LinkedHashMap<>();
        variants.forEach(variant -> variantsById.put(variant.getId(), variant));

        String currency = null;
        List<CartQuoteLine> lines = new ArrayList<>();
        List<Money> lineTotals = new ArrayList<>();
        for (Map.Entry<Long, Integer> requested : quantities.entrySet()) {
            ProductVariant variant = variantsById.get(requested.getKey());
            if (variant == null) {
                throw new NoSuchElementException("La variante " + requested.getKey() + " est introuvable");
            }
            variant.ensureOrderable(requested.getValue());
            if (currency == null) {
                currency = variant.getPrice().getCurrency();
            }
            Money lineTotal = variant.getPrice().multiply(requested.getValue());
            lineTotals.add(lineTotal);
            Product product = variant.getProduct();
            lines.add(new CartQuoteLine(product.getId(), product.getName(), variant.getId(), variant.getLabel(),
                    variant.getSku(), variant.getPrice(), requested.getValue(), lineTotal,
                    !variant.isStockTracked() || variant.availableStock() > 0));
        }

        Money subtotal = OrderTotalsCalculator.calculateLineTotals(lineTotals, currency);
        Money discountTotal = Money.zero(currency);
        Money feesTotal = Money.zero(currency);
        Money total = subtotal.subtract(discountTotal).add(feesTotal);
        return new CartQuote(lines, subtotal, discountTotal, feesTotal, total);
    }

    private Map<Long, Integer> normalizeQuantities(List<OrderItemRequest> requestedItems) {
        if (requestedItems == null || requestedItems.isEmpty()) {
            throw new IllegalArgumentException("Le panier doit contenir au moins un article");
        }
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        for (OrderItemRequest requestedItem : requestedItems) {
            if (requestedItem == null || requestedItem.variantId() == null) {
                throw new IllegalArgumentException("La variante est obligatoire");
            }
            if (requestedItem.quantity() <= 0) {
                throw new InvalidQuantityException(requestedItem.quantity());
            }
            quantities.merge(requestedItem.variantId(), requestedItem.quantity(), Math::addExact);
        }
        return quantities.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(Comparator.naturalOrder()))
                .collect(LinkedHashMap::new,
                        (result, entry) -> result.put(entry.getKey(), entry.getValue()),
                        LinkedHashMap::putAll);
    }

    public record CartQuote(List<CartQuoteLine> lines,
                            Money subtotal,
                            Money discountTotal,
                            Money feesTotal,
                            Money total) {
    }

    public record CartQuoteLine(Long productId,
                                String productName,
                                Long variantId,
                                String variantName,
                                String sku,
                                Money unitPrice,
                                int quantity,
                                Money lineTotal,
                                boolean available) {
    }
}
