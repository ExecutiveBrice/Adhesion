package com.wild.corp.adhesion.shop.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SupplierOrderCompletionRequest(
        @NotBlank @Size(max = 100) String invoiceReference,
        @NotEmpty List<@Valid Line> lines) {

    public record Line(@NotNull Long variantId, @NotNull @PositiveOrZero Long unitCostAmountInCents) { }
}
