package com.wild.corp.adhesion.shop.api;

import com.wild.corp.adhesion.shop.api.dto.AdminCategoryResponse;
import com.wild.corp.adhesion.shop.api.dto.AdminOrderItemResponse;
import com.wild.corp.adhesion.shop.api.dto.AdminOrderResponse;
import com.wild.corp.adhesion.shop.api.dto.AdminProductResponse;
import com.wild.corp.adhesion.shop.api.dto.AdminVariantResponse;
import com.wild.corp.adhesion.shop.api.dto.MoneyResponse;
import com.wild.corp.adhesion.models.User;
import com.wild.corp.adhesion.shop.catalog.model.Product;
import com.wild.corp.adhesion.shop.catalog.model.ProductCategory;
import com.wild.corp.adhesion.shop.catalog.model.ProductVariant;
import com.wild.corp.adhesion.shop.order.model.OrderItem;
import com.wild.corp.adhesion.shop.order.model.ShopOrder;


final class ShopAdminMapper {
    private ShopAdminMapper() { }

    static AdminOrderResponse order(ShopOrder order, User customer) {
        Long customerTribeId = customer != null && customer.getAdherent() != null && customer.getAdherent().getTribu() != null
                ? customer.getAdherent().getTribu().getId()
                : null;
        return new AdminOrderResponse(order.getOrderNumber(), order.getStatus(), MoneyResponse.from(order.getTotal()),
                order.getCreatedAt(), customer == null ? null : customer.getUsername(), customerTribeId,
                order.getItems().stream().map(ShopAdminMapper::orderItem).toList());
    }

    static AdminOrderItemResponse orderItem(OrderItem item) {
        return new AdminOrderItemResponse(item.getId(), item.getProductName(), item.getVariantName(), item.getSku(),
                MoneyResponse.from(item.getUnitPrice()), item.getQuantity(), MoneyResponse.from(item.getLineTotal()),
                item.getStatus());
    }

    static AdminProductResponse product(Product product) {
        return new AdminProductResponse(product.getId(), product.getName(), product.getSlug(), product.getDescription(),
                product.getImageUrl(), product.isActive(), product.getDisplayOrder(),
                product.getCategories().stream().map(ShopAdminMapper::category).toList(),
                product.getVariants().stream().map(ShopAdminMapper::variant).toList());
    }

    static AdminCategoryResponse category(ProductCategory category) {
        return new AdminCategoryResponse(category.getId(), category.getName(), category.getSlug(), category.getDescription(),
                category.isActive(), category.getDisplayOrder());
    }

    static AdminVariantResponse variant(ProductVariant variant) {
        return new AdminVariantResponse(variant.getId(), variant.getSku(), variant.getLabel(),
                variant.getPrice().getAmountInCents(), variant.getPrice().getCurrency(), variant.isActive(), variant.getDisplayOrder(),
                variant.isStockTracked(), variant.getStockOnHand(), variant.getStockReserved());
    }
}
