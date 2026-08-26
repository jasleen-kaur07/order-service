package com.ecommerce.orderservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShippingAddressRequest(

        @NotBlank(message = "shippingAddress.addressLine1 is required")
        @Size(max = 255, message = "shippingAddress.addressLine1 must not exceed 255 characters")
        String addressLine1,

        @Size(max = 255, message = "shippingAddress.addressLine2 must not exceed 255 characters")
        String addressLine2,

        @NotBlank(message = "shippingAddress.city is required")
        @Size(max = 100, message = "shippingAddress.city must not exceed 100 characters")
        String city,

        @NotBlank(message = "shippingAddress.state is required")
        @Size(max = 100, message = "shippingAddress.state must not exceed 100 characters")
        String state,

        @NotBlank(message = "shippingAddress.country is required")
        @Size(max = 100, message = "shippingAddress.country must not exceed 100 characters")
        String country,

        @NotBlank(message = "shippingAddress.pincode is required")
        @Size(max = 20, message = "shippingAddress.pincode must not exceed 20 characters")
        String pincode
) {
}