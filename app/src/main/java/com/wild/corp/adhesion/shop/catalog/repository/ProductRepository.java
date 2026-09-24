package com.wild.corp.adhesion.shop.catalog.repository;

import com.wild.corp.adhesion.shop.catalog.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySlug(String slug);
}
