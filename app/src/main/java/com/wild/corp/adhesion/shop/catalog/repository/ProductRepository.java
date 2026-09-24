package com.wild.corp.adhesion.shop.catalog.repository;

import com.wild.corp.adhesion.shop.catalog.model.Product;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySlug(String slug);

    @EntityGraph(attributePaths = {"categories", "variants"})
    List<Product> findByActiveTrueOrderByDisplayOrderAscNameAsc();

    @EntityGraph(attributePaths = {"categories", "variants"})
    Optional<Product> findByIdAndActiveTrue(Long id);

    @EntityGraph(attributePaths = {"categories", "variants"})
    List<Product> findAllByOrderByDisplayOrderAscNameAsc();

    @EntityGraph(attributePaths = {"categories", "variants"})
    Page<Product> findByNameContainingIgnoreCaseOrSlugContainingIgnoreCase(String name, String slug, Pageable pageable);
}
