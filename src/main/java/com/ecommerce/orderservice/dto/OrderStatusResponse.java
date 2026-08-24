package com.ecommerce.orderservice.dto;

import com.ecommerce.orderservice.entity.OrderStatus;

import java.time.Instant;
import java.util.UUID;

public record OrderStatusResponse(
        UUID orderId,
        OrderStatus status,
        Instant updatedAt
) {
}
