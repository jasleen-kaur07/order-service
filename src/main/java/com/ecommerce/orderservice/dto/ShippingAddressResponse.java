package com.ecommerce.orderservice.dto;

public record ShippingAddressResponse(
        String addressLine1,
        String addressLine2,
        String city,
        String state,
        String country,
        String pincode
) {
}
