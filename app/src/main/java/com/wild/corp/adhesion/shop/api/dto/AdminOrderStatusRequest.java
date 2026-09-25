package com.wild.corp.adhesion.shop.api.dto;

import com.wild.corp.adhesion.shop.order.model.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record AdminOrderStatusRequest(@NotNull OrderStatus status) {
}
