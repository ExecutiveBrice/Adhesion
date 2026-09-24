package com.wild.corp.adhesion.shop.api.dto;

public record AdminVariantResponse(Long id, String sku, String label, long priceAmountInCents, String currency,
                                   boolean active, int displayOrder, boolean stockTracked, Long stockOnHand,
                                   long stockReserved) {
}
