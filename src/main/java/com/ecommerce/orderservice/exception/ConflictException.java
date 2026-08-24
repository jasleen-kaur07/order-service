package com.ecommerce.orderservice.exception;

import com.ecommerce.orderservice.entity.OrderStatus;

import java.util.UUID;

public class ConflictException extends ApiException {

    public ConflictException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public static ConflictException duplicateOrder(UUID cartId) {
        return new ConflictException(ErrorCode.DUPLICATE_ORDER,
                "An order already exists for cart " + cartId + ". A checkout may only be placed once.");
    }

    public static ConflictException notCancellable(UUID orderId, OrderStatus status) {
        return new ConflictException(ErrorCode.ORDER_NOT_CANCELLABLE,
                "Order " + orderId + " cannot be cancelled from status " + status
                        + "; only a CREATED order may be cancelled.");
    }
}
