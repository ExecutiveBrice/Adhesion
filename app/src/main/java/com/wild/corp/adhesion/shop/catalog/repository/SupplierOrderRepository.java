package com.wild.corp.adhesion.shop.catalog.repository;

import com.wild.corp.adhesion.shop.catalog.model.SupplierOrder;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SupplierOrderRepository extends JpaRepository<SupplierOrder, Long> {

    @EntityGraph(attributePaths = "lines")
    List<SupplierOrder> findAllByOrderByCreatedAtDescIdDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select supplierOrder from SupplierOrder supplierOrder where supplierOrder.id = :id")
    Optional<SupplierOrder> findByIdForUpdate(@Param("id") Long id);
}
