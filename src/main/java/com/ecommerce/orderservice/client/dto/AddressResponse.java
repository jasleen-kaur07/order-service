package com.ecommerce.orderservice.client.dto;

public record AddressResponse(
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String country,
        String pincode
) {
}
