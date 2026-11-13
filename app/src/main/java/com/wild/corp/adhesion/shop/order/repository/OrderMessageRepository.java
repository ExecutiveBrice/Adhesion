package com.wild.corp.adhesion.shop.order.repository;

import com.wild.corp.adhesion.shop.order.model.OrderMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderMessageRepository extends JpaRepository<OrderMessage, Long> {
    List<OrderMessage> findAllByOrderIdOrderByCreatedAtAscIdAsc(Long orderId);
}
