package com.ecommerce.orderservice.client.dto;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String email
) {
}
