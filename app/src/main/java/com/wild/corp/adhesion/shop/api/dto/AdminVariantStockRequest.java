package com.wild.corp.adhesion.shop.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AdminVariantStockRequest(@NotNull @Min(0) Long stockOnHand,
                                       @NotNull @Min(0) Long expectedVersion) {
}
