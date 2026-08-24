package com.ecommerce.orderservice.client;

import com.ecommerce.orderservice.client.dto.AddressResponse;
import com.ecommerce.orderservice.client.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(
        name = "user-service",
        url = "${integration.user-service.url}"
)
public interface UserServiceClient {

    @GetMapping("/api/users/{userId}")
    UserResponse getUser(@PathVariable("userId") UUID userId,
                         @RequestHeader("X-User-Id") UUID callerId,
                         @RequestHeader("X-User-Type") String callerType);

    @GetMapping("/api/users/{userId}/addresses/{addressId}")
    AddressResponse getAddress(@PathVariable("userId") UUID userId,
                               @PathVariable("addressId") UUID addressId,
                               @RequestHeader("X-User-Id") UUID callerId,
                               @RequestHeader("X-User-Type") String callerType);
}
