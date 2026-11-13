package com.wild.corp.adhesion.shop.api.dto;

public record ProductVariantResponse(Long id,
                                     String sku,
                                     String label,
                                     String imageUrl,
                                     MoneyResponse price,
                                     boolean available) {
}
