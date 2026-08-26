package com.ecommerce.orderservice.mapper;

import com.ecommerce.orderservice.dto.OrderLineItemResponse;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.dto.ShippingAddressResponse;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;

import java.util.List;

public final class OrderMapper {

    private OrderMapper() {
    }

    public static OrderResponse toResponse(Order order) {

        List<OrderLineItemResponse> items = order.getItems()
                .stream()
                .map(OrderMapper::toItemResponse)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getCartId(),
                order.getUserId(),
                order.getEmailSnapshot(),
                order.getFirstNameSnapshot(),
                order.getLastNameSnapshot(),
                order.getStatus(),
                order.getTotalPrice(),
                new ShippingAddressResponse(
                        order.getShipLine1(),
                        order.getShipLine2(),
                        order.getShipCity(),
                        order.getShipState(),
                        order.getShipCountry(),
                        order.getShipPincode()
                ),
                items,
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }

    private static OrderLineItemResponse toItemResponse(OrderItem item) {

        return new OrderLineItemResponse(
                item.getId(),
                item.getCartItemId(),
                item.getProductId(),
                item.getVariantId(),
                item.getMerchantId(),
                item.getProductNameSnapshot(),
                item.getProductImageSnapshot(),
                item.getQuantity(),
                item.getUnitPricePaid(),
                item.getLineTotalPaid()
        );
    }
}