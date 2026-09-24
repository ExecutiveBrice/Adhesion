package com.wild.corp.adhesion.shop.order.repository;

import com.wild.corp.adhesion.shop.order.model.ShopOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long> {

    Optional<ShopOrder> findByOrderNumber(String orderNumber);
}
