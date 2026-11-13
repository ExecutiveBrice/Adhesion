package com.wild.corp.adhesion.shop.api.dto;

public record CartLineResponse(Long productId,
                               String productName,
                               Long variantId,
                               String variantName,
                               String sku,
                               MoneyResponse unitPrice,
                               int quantity,
                               MoneyResponse lineTotal,
                               boolean available) {
}
