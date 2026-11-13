package com.wild.corp.adhesion.shop.api.dto;

import com.wild.corp.adhesion.shop.order.model.OrderMessageSender;

import java.time.Instant;

public record OrderMessageResponse(Long id, OrderMessageSender senderType, String content, Instant createdAt) {
}
