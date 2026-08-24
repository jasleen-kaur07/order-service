package com.ecommerce.orderservice.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    BAD_REQUEST(HttpStatus.BAD_REQUEST),

    ORDER_TOTAL_MISMATCH(HttpStatus.BAD_REQUEST),

    INVALID_ORDER_DETAILS(HttpStatus.BAD_REQUEST),


    ORDER_NOT_FOUND(HttpStatus.NOT_FOUND),

    DUPLICATE_ORDER(HttpStatus.CONFLICT),

    ORDER_NOT_CANCELLABLE(HttpStatus.CONFLICT),

    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR),

    UPSTREAM_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE);

    private final HttpStatus status;

    ErrorCode(HttpStatus status) {
        this.status = status;
    }

    public HttpStatus status() {
        return status;
    }
}
