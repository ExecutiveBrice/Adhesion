package com.wild.corp.adhesion.shop.api;

import com.wild.corp.adhesion.shop.api.dto.CartLineResponse;
import com.wild.corp.adhesion.shop.api.dto.CartQuoteResponse;
import com.wild.corp.adhesion.shop.api.dto.MoneyResponse;
import com.wild.corp.adhesion.shop.api.dto.OrderItemResponse;
import com.wild.corp.adhesion.shop.api.dto.OrderResponse;
import com.wild.corp.adhesion.shop.api.dto.ProductCategoryResponse;
import com.wild.corp.adhesion.shop.api.dto.ProductResponse;
import com.wild.corp.adhesion.shop.api.dto.ProductVariantResponse;
import com.wild.corp.adhesion.shop.cart.service.CartPricingService.CartQuote;
import com.wild.corp.adhesion.shop.cart.service.CartPricingService.CartQuoteLine;
import com.wild.corp.adhesion.shop.catalog.model.Product;
import com.wild.corp.adhesion.shop.catalog.model.ProductCategory;
import com.wild.corp.adhesion.shop.catalog.model.ProductVariant;
import com.wild.corp.adhesion.shop.order.model.OrderItem;
import com.wild.corp.adhesion.shop.order.model.ShopOrder;

import java.util.Comparator;

final class ShopApiMapper {

    private ShopApiMapper() {
    }

    static ProductResponse product(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getImageUrl(),
                product.getCategories().stream()
                        .filter(ProductCategory::isActive)
                        .sorted(Comparator.comparing(ProductCategory::getDisplayOrder)
                                .thenComparing(ProductCategory::getName))
                        .map(category -> new ProductCategoryResponse(category.getId(), category.getName(), category.getSlug()))
                        .toList(),
                product.getVariants().stream()
                        .filter(ProductVariant::isActive)
                        .map(variant -> new ProductVariantResponse(
                                variant.getId(), variant.getSku(), variant.getLabel(), MoneyResponse.from(variant.getPrice()),
                                !variant.isStockTracked() || variant.availableStock() > 0))
                        .toList());
    }

    static CartQuoteResponse quote(CartQuote quote) {
        return new CartQuoteResponse(
                quote.lines().stream().map(ShopApiMapper::quoteLine).toList(),
                MoneyResponse.from(quote.subtotal()),
                MoneyResponse.from(quote.discountTotal()),
                MoneyResponse.from(quote.feesTotal()),
                MoneyResponse.from(quote.total()));
    }

    static OrderResponse order(ShopOrder order) {
        MoneyResponse total = MoneyResponse.from(order.getTotal());
        return new OrderResponse(
                order.getId(), order.getOrderNumber(), order.getStatus(),
                order.getItems().stream().map(ShopApiMapper::orderItem).toList(),
                total, MoneyResponse.from(com.wild.corp.adhesion.shop.common.money.Money.zero(total.currency())),
                MoneyResponse.from(com.wild.corp.adhesion.shop.common.money.Money.zero(total.currency())), total,
                order.getCreatedAt());
    }

    private static CartLineResponse quoteLine(CartQuoteLine line) {
        return new CartLineResponse(line.productId(), line.productName(), line.variantId(), line.variantName(), line.sku(),
                MoneyResponse.from(line.unitPrice()), line.quantity(), MoneyResponse.from(line.lineTotal()), line.available());
    }

    private static OrderItemResponse orderItem(OrderItem item) {
        return new OrderItemResponse(item.getProductId(), item.getProductName(), item.getProductVariantId(),
                item.getVariantName(), item.getSku(), MoneyResponse.from(item.getUnitPrice()), item.getQuantity(),
                MoneyResponse.from(item.getLineTotal()));
    }
}
