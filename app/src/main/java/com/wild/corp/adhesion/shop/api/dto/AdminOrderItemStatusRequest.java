package com.wild.corp.adhesion.shop.api.dto;

import com.wild.corp.adhesion.shop.order.model.OrderItemStatus;
import jakarta.validation.constraints.NotNull;

public record AdminOrderItemStatusRequest(@NotNull OrderItemStatus status) {
}
