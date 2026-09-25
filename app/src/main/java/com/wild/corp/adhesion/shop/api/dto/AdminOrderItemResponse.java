package com.wild.corp.adhesion.shop.api.dto;

import com.wild.corp.adhesion.shop.order.model.OrderItemStatus;

public record AdminOrderItemResponse(Long id,
                                     String productName,
                                     String variantName,
                                     String sku,
                                     MoneyResponse unitPrice,
                                     int quantity,
                                     MoneyResponse lineTotal,
                                     OrderItemStatus status) {
}
