package com.ecommerce.orderservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderLineItemRequest(

        @NotNull(message = "cartItemId is required")
        UUID cartItemId,

        @NotBlank(message = "productId is required")
        @Size(
                max = 255,
                message = "productId must not exceed 255 characters"
        )
        String productId,

        @NotBlank(message = "variantId is required")
        @Size(
                max = 255,
                message = "variantId must not exceed 255 characters"
        )
        String variantId,

        @NotBlank(message = "merchantId is required")
        @Size(
                max = 255,
                message = "merchantId must not exceed 255 characters"
        )
        String merchantId,

        @NotBlank(message = "productName is required")
        @Size(
                max = 255,
                message = "productName must not exceed 255 characters"
        )
        String productName,

        @Size(
                max = 1000,
                message = "imageUrl must not exceed 1000 characters"
        )
        String imageUrl,

        @NotNull(message = "quantity is required")
        @Min(
                value = 1,
                message = "quantity must be at least 1"
        )
        Integer quantity,

        @NotNull(message = "unitPrice is required")
        @DecimalMin(
                value = "0.00",
                inclusive = false,
                message = "unitPrice must be greater than zero"
        )
        @Digits(
                integer = 10,
                fraction = 2,
                message = "unitPrice must have at most 10 integer and 2 fraction digits"
        )
        BigDecimal unitPrice,

        @NotNull(message = "lineTotal is required")
        @DecimalMin(
                value = "0.00",
                inclusive = false,
                message = "lineTotal must be greater than zero"
        )
        @Digits(
                integer = 10,
                fraction = 2,
                message = "lineTotal must have at most 10 integer and 2 fraction digits"
        )
        BigDecimal lineTotal
) {
}