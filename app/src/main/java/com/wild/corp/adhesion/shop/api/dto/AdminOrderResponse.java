package com.wild.corp.adhesion.shop.api.dto;

import com.wild.corp.adhesion.shop.order.model.OrderStatus;

import java.time.Instant;
import java.util.List;

public record AdminOrderResponse(String orderNumber,
                                 OrderStatus status,
                                 MoneyResponse total,
                                 Instant createdAt,
                                 String customerEmail,
                                 Long customerTribeId,
                                 List<AdminOrderItemResponse> items) {
}
