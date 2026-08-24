package com.ecommerce.orderservice.exception;

import java.util.UUID;

public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message);
    }

    public static ResourceNotFoundException order(UUID orderId) {
        return new ResourceNotFoundException(ErrorCode.ORDER_NOT_FOUND, "Order not found: " + orderId);
    }
}
