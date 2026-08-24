package com.ecommerce.orderservice.event;

import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        UUID cartId,
        UUID userId,
        String email,
        OrderStatus status,
        BigDecimal totalPrice,
        ShippingAddress shippingAddress,
        List<Item> items,
        Instant placedAt
) {

    public record ShippingAddress(
            String addressLine1,
            String addressLine2,
            String city,
            String state,
            String country,
            String pincode
    ) {
    }

    public record Item(
            UUID productId,
            UUID merchantId,
            String productName,
            int quantity,
            BigDecimal unitPrice
    ) {
    }

    public static OrderCreatedEvent from(Order order) {
        List<Item> items = order.getItems().stream()
                .map(i -> new Item(i.getProductId(), i.getMerchantId(),
                        i.getProductNameSnapshot(), i.getQuantity(), i.getUnitPricePaid()))
                .toList();

        return new OrderCreatedEvent(
                order.getId(),
                order.getCartId(),
                order.getUserId(),
                order.getEmailSnapshot(),
                order.getStatus(),
                order.getTotalPrice(),
                new ShippingAddress(
                        order.getShipLine1(),
                        order.getShipLine2(),
                        order.getShipCity(),
                        order.getShipState(),
                        order.getShipCountry(),
                        order.getShipPincode()),
                items,
                order.getCreatedAt());
    }
}
