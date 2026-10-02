package com.wild.corp.adhesion.shop.api.dto;

public record ProductVariantResponse(Long id,
                                     String sku,
                                     String label,
                                     MoneyResponse price,
                                     boolean available) {
}
