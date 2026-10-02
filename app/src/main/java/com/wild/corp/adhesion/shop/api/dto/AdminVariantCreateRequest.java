package com.wild.corp.adhesion.shop.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminVariantCreateRequest(
        @NotBlank @Size(max = 100) String sku,
        @Size(max = 160) String label,
        @Min(0) long priceAmountInCents,
        @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
        boolean active,
        @Min(0) int displayOrder) {
}
