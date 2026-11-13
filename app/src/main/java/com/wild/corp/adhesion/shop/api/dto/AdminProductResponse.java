package com.wild.corp.adhesion.shop.api.dto;

import java.util.List;

public record AdminProductResponse(Long id, String name, String slug, String description, String imageUrl, List<String> additionalImageUrls,
                                   boolean active, int displayOrder, List<AdminCategoryResponse> categories,
                                   List<AdminVariantResponse> variants) {
}
