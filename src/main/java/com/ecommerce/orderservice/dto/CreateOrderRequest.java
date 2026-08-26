package com.ecommerce.orderservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Contract Cart Service sends to Order Service (POST /api/orders) at checkout. Order
 * Service no longer calls User Service itself, so Cart Service snapshots the customer's
 * identity and address here instead.
 */
public record CreateOrderRequest(

        @NotNull(message = "cartId is required")
        UUID cartId,

        @NotNull(message = "userId is required")
        UUID userId,

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid email address")
        @Size(max = 255, message = "email must not exceed 255 characters")
        String email,

        @Size(max = 100, message = "firstName must not exceed 100 characters")
        String firstName,

        @Size(max = 100, message = "lastName must not exceed 100 characters")
        String lastName,

        @NotNull(message = "shippingAddress is required")
        @Valid
        ShippingAddressRequest shippingAddress,

        @NotEmpty(message = "items must contain at least one line item")
        @Valid
        List<OrderLineItemRequest> items,

        @NotNull(message = "totalPrice is required")
        @DecimalMin(
                value = "0.00",
                inclusive = false,
                message = "totalPrice must be greater than zero"
        )
        @Digits(
                integer = 10,
                fraction = 2,
                message = "totalPrice must have at most 10 integer and 2 fraction digits"
        )
        BigDecimal totalPrice
) {
}