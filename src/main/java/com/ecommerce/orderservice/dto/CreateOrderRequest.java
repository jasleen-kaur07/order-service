package com.ecommerce.orderservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CreateOrderRequest(

        @NotNull(message = "cartId is required")
        UUID cartId,

        @NotNull(message = "userId is required")
        UUID userId,

        @NotEmpty(message = "items must contain at least one line item")
        @Valid
        List<OrderLineItemRequest> items,

        @NotNull(message = "totalPrice is required")
        @DecimalMin(value = "0.00", inclusive = false)
        @Digits(integer = 10, fraction = 2)
        BigDecimal totalPrice
) {}