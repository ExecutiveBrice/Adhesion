package com.wild.corp.adhesion.shop.api.dto;

public record AdminCategoryResponse(Long id, String name, String slug, String description,
                                    boolean active, int displayOrder) {
}
