package com.wild.corp.adhesion.shop.order.repository;

import com.wild.corp.adhesion.shop.order.model.ShopOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.domain.Pageable;

import jakarta.persistence.LockModeType;
import java.time.Instant;

import java.util.List;
import java.util.Optional;

public interface ShopOrderRepository extends JpaRepository<ShopOrder, Long> {

    @EntityGraph(attributePaths = "items")
    List<ShopOrder> findAllByOrderByCreatedAtDescIdDesc();

    @EntityGraph(attributePaths = "items")
    List<ShopOrder> findAllByCustomerUserIdOrderByCreatedAtDescIdDesc(Long customerUserId);

    Optional<ShopOrder> findByOrderNumber(String orderNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from ShopOrder o where o.orderNumber = :orderNumber")
    Optional<ShopOrder> findByOrderNumberForUpdate(String orderNumber);

    @Query("select o.id from ShopOrder o where o.orderNumber = :orderNumber")
    Optional<Long> findIdByOrderNumber(String orderNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from ShopOrder o where o.id = :id")
    Optional<ShopOrder> findByIdForUpdate(Long id);

    @Query("""
            select o.id from ShopOrder o where o.status = 'PENDING_PAYMENT'
            and o.createdAt <= :createdBefore and o.id > :afterId order by o.id
            """)
    List<Long> findExpiredPendingOrderIds(Instant createdBefore, Long afterId, Pageable page);

    Optional<ShopOrder> findByCustomerUserIdAndCheckoutKey(Long customerUserId, String checkoutKey);
}
