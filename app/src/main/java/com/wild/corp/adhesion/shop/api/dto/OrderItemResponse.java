package com.wild.corp.adhesion.shop.api.dto;

public record OrderItemResponse(Long productId,
                                String productName,
                                Long variantId,
                                String variantName,
                                String sku,
                                MoneyResponse unitPrice,
                                int quantity,
                                MoneyResponse lineTotal) {
}
