package com.wild.corp.adhesion.shop.api.dto;

import java.util.List;

public record ProductResponse(Long id,
                              String name,
                              String slug,
                              String description,
                              String imageUrl,
                              List<ProductCategoryResponse> categories,
                              List<ProductVariantResponse> variants) {
}
