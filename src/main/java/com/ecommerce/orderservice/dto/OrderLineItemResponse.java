package com.ecommerce.orderservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderLineItemResponse(
        UUID id,
        UUID cartItemId,
        String productId,
        String variantId,
        String merchantId,
        String productName,
        String imageUrl,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}