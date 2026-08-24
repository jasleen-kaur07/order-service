package com.ecommerce.orderservice.event;

import com.ecommerce.orderservice.entity.Order;

import java.time.Instant;
import java.util.UUID;

public record OrderCancelledEvent(
        UUID orderId,
        UUID userId,
        String email,
        Instant cancelledAt
) {

    public static OrderCancelledEvent from(Order order) {
        return new OrderCancelledEvent(
                order.getId(),
                order.getUserId(),
                order.getEmailSnapshot(),
                order.getUpdatedAt());
    }
}
