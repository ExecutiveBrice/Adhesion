package com.wild.corp.adhesion.models.resources;

import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Contrat JSON stable pour les listes paginées exposées par l'API.
 */
public record PageResponse<T>(
        List<T> content,
        long totalElements,
        int totalPages,
        int number,
        int size) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.getNumber(),
                page.getSize());
    }
}
