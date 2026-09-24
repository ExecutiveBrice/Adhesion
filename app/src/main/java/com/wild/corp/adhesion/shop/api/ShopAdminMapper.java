package com.wild.corp.adhesion.shop.api;

import com.wild.corp.adhesion.shop.api.dto.AdminCategoryResponse;
import com.wild.corp.adhesion.shop.api.dto.AdminProductResponse;
import com.wild.corp.adhesion.shop.api.dto.AdminVariantResponse;
import com.wild.corp.adhesion.shop.catalog.model.Product;
import com.wild.corp.adhesion.shop.catalog.model.ProductCategory;
import com.wild.corp.adhesion.shop.catalog.model.ProductVariant;

final class ShopAdminMapper {
    private ShopAdminMapper() { }

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
