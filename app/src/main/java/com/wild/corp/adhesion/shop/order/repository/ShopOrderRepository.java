package com.wild.corp.adhesion.shop.order.repository;

import com.wild.corp.adhesion.shop.order.model.ShopOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;
import java.util.Optional;

public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long> {

    @EntityGraph(attributePaths = "items")
    List<ShopOrder> findAllByOrderByCreatedAtDescIdDesc();

    @EntityGraph(attributePaths = "items")
    List<ShopOrder> findAllByCustomerUserIdOrderByCreatedAtDescIdDesc(Long customerUserId);

    Optional<ShopOrder> findByOrderNumber(String orderNumber);

    Optional<ShopOrder> findByCustomerUserIdAndCheckoutKey(Long customerUserId, String checkoutKey);
}
