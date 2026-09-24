package com.wild.corp.adhesion.shop.api.dto;

import com.wild.corp.adhesion.shop.order.model.OrderStatus;

import java.time.Instant;
import java.util.List;

public record OrderResponse(Long id,
                            String orderNumber,
                            OrderStatus status,
                            List<OrderItemResponse> items,
                            MoneyResponse subtotal,
                            MoneyResponse discountTotal,
                            MoneyResponse feesTotal,
                            MoneyResponse total,
                            Instant createdAt) {
}
