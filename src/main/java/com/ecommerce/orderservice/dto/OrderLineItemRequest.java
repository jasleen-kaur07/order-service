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

        @NotNull(message = "productId is required")
        UUID productId,

        @NotNull(message = "merchantId is required")
        UUID merchantId,

        @NotBlank(message = "productName is required")
        @Size(max = 255, message = "productName must not exceed 255 characters")
        String productName,

        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        Integer quantity,

        @NotNull(message = "unitPrice is required")
        @DecimalMin(value = "0.00", inclusive = false, message = "unitPrice must be greater than zero")
        @Digits(integer = 10, fraction = 2, message = "unitPrice must have at most 10 integer and 2 fraction digits")
        BigDecimal unitPrice
) {
}
