package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.dto.OrderLineItemResponse;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.dto.ShippingAddressResponse;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    private static final UUID ORDER_ID =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static final UUID CART_ID =
            UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private static final UUID USER_ID =
            UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private static final UUID CART_ITEM_ID =
            UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    private static final UUID ITEM_ID =
            UUID.fromString("11111111-2222-3333-4444-555555555555");

    private static final String PRODUCT_ID = "product-101";

    private static final String VARIANT_ID = "variant-101";

    private static final String MERCHANT_ID = "merchant-201";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private OrderService orderService;

    @Test
    void getOrderReturnsOrder() throws Exception {

        OrderStatus status = OrderStatus.CREATED;

        OrderResponse response = new OrderResponse(
                ORDER_ID,
                CART_ID,
                USER_ID,
                "jasleen@gmail.com",
                "Jasleen",
                "Kaur",
                status,
                new BigDecimal("100.00"),
                new ShippingAddressResponse(
                        "123 Main Street",
                        "Apt 4B",
                        "Noida",
                        "Uttar Pradesh",
                        "India",
                        "201301"
                ),
                List.of(
                        new OrderLineItemResponse(
                                ITEM_ID,
                                CART_ITEM_ID,
                                PRODUCT_ID,
                                VARIANT_ID,
                                MERCHANT_ID,
                                "Widget",
                                "https://example.com/widget.jpg",
                                2,
                                new BigDecimal("50.00"),
                                new BigDecimal("100.00")
                        )
                ),
                Instant.parse("2026-08-24T10:00:00Z"),
                Instant.parse("2026-08-24T10:00:00Z")
        );

        when(orderService.getOrder(ORDER_ID)).thenReturn(response);

        mockMvc.perform(
                        get("/api/orders/{orderId}", ORDER_ID)
                                .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ORDER_ID.toString()))
                .andExpect(jsonPath("$.cartId").value(CART_ID.toString()))
                .andExpect(jsonPath("$.userId").value(USER_ID.toString()))
                .andExpect(jsonPath("$.email").value("jasleen@gmail.com"))
                .andExpect(jsonPath("$.firstName").value("Jasleen"))
                .andExpect(jsonPath("$.lastName").value("Kaur"))
                .andExpect(jsonPath("$.status").value(status.toString()))
                .andExpect(jsonPath("$.totalPrice").value(100.00));
    }
}