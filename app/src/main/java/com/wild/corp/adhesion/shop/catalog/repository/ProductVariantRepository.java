package com.wild.corp.adhesion.shop.catalog.repository;

import com.wild.corp.adhesion.shop.catalog.model.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Long> {

    Optional<ProductVariant> findBySku(String sku);

    @Query("""
            select variant from ProductVariant variant
            join fetch variant.product
            where variant.id in :ids
            """)
    List<ProductVariant> findAllWithProductByIdIn(@Param("ids") Collection<Long> ids);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select variant from ProductVariant variant
            join fetch variant.product
            where variant.id = :id
            """)
    Optional<ProductVariant> findByIdForUpdate(@Param("id") Long id);
}
