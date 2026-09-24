package com.wild.corp.adhesion.shop.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminCategoryRequest(
        @NotBlank @Size(max = 160) String name,
        @NotBlank @Size(max = 180) String slug,
        @Size(max = 2_000) String description,
        boolean active,
        @Min(0) int displayOrder) {
}
