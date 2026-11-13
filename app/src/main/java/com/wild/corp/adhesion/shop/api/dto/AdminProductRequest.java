package com.wild.corp.adhesion.shop.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.List;

public record AdminProductRequest(
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Size(max = 220) String slug,
        @Size(max = 5_000) String description,
        @Size(max = 2_048) String imageUrl,
        @Size(max = 2) List<@NotBlank @Size(max = 2_048) String> additionalImageUrls,
        boolean active,
        @Min(0) int displayOrder,
        Set<Long> categoryIds) {
}
