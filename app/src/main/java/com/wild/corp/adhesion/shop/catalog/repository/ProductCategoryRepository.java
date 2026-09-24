package com.wild.corp.adhesion.shop.catalog.repository;

import com.wild.corp.adhesion.shop.catalog.model.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {

    Optional<ProductCategory> findBySlug(String slug);
}
