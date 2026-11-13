package com.wild.corp.adhesion.shop.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;

public record SupplierOrderCreateRequest(
        @NotBlank @Size(max = 200) String supplierName,
        @Size(max = 100) String reference,
        @NotEmpty List<@Valid Line> lines) {

    public record Line(@NotNull Long variantId, @Positive int quantity) { }
}
