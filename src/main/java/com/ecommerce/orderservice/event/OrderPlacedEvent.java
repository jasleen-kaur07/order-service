package com.ecommerce.orderservice.event;

import com.ecommerce.orderservice.entity.Order;

public record OrderPlacedEvent(
        String orderNumber,
        String email,
        String firstName,
        String lastName
) {

    public static OrderPlacedEvent from(
            Order order,
            String firstName,
            String lastName
    ) {
        return new OrderPlacedEvent(
                order.getId().toString(),
                order.getEmailSnapshot(),
                firstName,
                lastName
        );
    }
}