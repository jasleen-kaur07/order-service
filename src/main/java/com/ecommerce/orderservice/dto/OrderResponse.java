package com.ecommerce.orderservice.dto;

import com.ecommerce.orderservice.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        UUID cartId,
        UUID userId,
        String email,
        OrderStatus status,
        BigDecimal totalPrice,
        ShippingAddressResponse shippingAddress,
        List<OrderLineItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {
}
