package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.dto.OrderLineItemResponse;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.dto.OrderStatusResponse;
import com.ecommerce.orderservice.dto.ShippingAddressResponse;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.exception.ConflictException;
import com.ecommerce.orderservice.exception.ResourceNotFoundException;
import com.ecommerce.orderservice.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    private static final UUID ORDER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID CART_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID USER_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID ADDRESS_ID = UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");
    private static final UUID PRODUCT_ID = UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");
    private static final UUID MERCHANT_ID = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
    private static final UUID ITEM_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    private static OrderResponse sampleResponse(OrderStatus status) {
        return new OrderResponse(ORDER_ID, CART_ID, USER_ID, "jasleen@gmail.com", status,
                new BigDecimal("100.00"),
                new ShippingAddressResponse("123 Main Street", "Apt 4B", "Noida",
                        "Uttar Pradesh", "India", "201301"),
                List.of(new OrderLineItemResponse(ITEM_ID, PRODUCT_ID, MERCHANT_ID, "Widget",
                        2, new BigDecimal("50.00"), new BigDecimal("100.00"))),
                Instant.parse("2026-08-24T10:00:00Z"),
                Instant.parse("2026-08-24T10:00:00Z"));
    }

    private static String validOrderJson() {
        return """
                {
                  "cartId": "%s",
                  "userId": "%s",
                  "addressId": "%s",
                  "items": [
                    { "productId": "%s", "merchantId": "%s", "productName": "Widget", "quantity": 2, "unitPrice": 50.00 }
                  ],
                  "totalPrice": 100.00
                }
                """.formatted(CART_ID, USER_ID, ADDRESS_ID, PRODUCT_ID, MERCHANT_ID);
    }

    @Test
    @DisplayName("POST creates the order, returns 201 and a Location header")
    void createsOrder() throws Exception {
        when(orderService.createOrder(any())).thenReturn(sampleResponse(OrderStatus.CREATED));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validOrderJson()))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/orders/" + ORDER_ID))
                .andExpect(jsonPath("$.id").value(ORDER_ID.toString()))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.email").value("jasleen@gmail.com"))
                .andExpect(jsonPath("$.shippingAddress.city").value("Noida"))
                .andExpect(jsonPath("$.items[0].productName").value("Widget"))
                .andExpect(jsonPath("$.items[0].lineTotal").value(100.00));
    }

    @Test
    @DisplayName("POST with no line items is 400 VALIDATION_ERROR and never reaches the service")
    void rejectsEmptyItems() throws Exception {
        String body = """
                {
                  "cartId": "%s", "userId": "%s", "addressId": "%s",
                  "items": [],
                  "totalPrice": 100.00
                }
                """.formatted(CART_ID, USER_ID, ADDRESS_ID);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("items"));

        verify(orderService, never()).createOrder(any());
    }

    @Test
    @DisplayName("POST with a missing required field is 400 with the offending field named")
    void rejectsMissingField() throws Exception {
        String body = """
                {
                  "userId": "%s", "addressId": "%s",
                  "items": [ { "productId": "%s", "merchantId": "%s", "productName": "Widget", "quantity": 2, "unitPrice": 50.00 } ],
                  "totalPrice": 100.00
                }
                """.formatted(USER_ID, ADDRESS_ID, PRODUCT_ID, MERCHANT_ID);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value("cartId"));

        verify(orderService, never()).createOrder(any());
    }

    @Test
    @DisplayName("POST with malformed JSON is 400 BAD_REQUEST, not 500")
    void rejectsMalformedJson() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not valid json "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));

        verify(orderService, never()).createOrder(any());
    }

    @Test
    @DisplayName("GET returns the order")
    void getsOrder() throws Exception {
        when(orderService.getOrder(ORDER_ID)).thenReturn(sampleResponse(OrderStatus.CREATED));

        mockMvc.perform(get("/api/orders/{orderId}", ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ORDER_ID.toString()))
                .andExpect(jsonPath("$.items[0].merchantId").value(MERCHANT_ID.toString()));
    }

    @Test
    @DisplayName("GET an unknown order is 404 ORDER_NOT_FOUND")
    void getOrderNotFound() throws Exception {
        when(orderService.getOrder(ORDER_ID)).thenThrow(ResourceNotFoundException.order(ORDER_ID));

        mockMvc.perform(get("/api/orders/{orderId}", ORDER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("ORDER_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value("/api/orders/" + ORDER_ID));
    }

    @Test
    @DisplayName("GET history returns the customer's orders")
    void getsHistory() throws Exception {
        when(orderService.getOrderHistory(USER_ID)).thenReturn(List.of(sampleResponse(OrderStatus.CREATED)));

        mockMvc.perform(get("/api/orders").param("userId", USER_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ORDER_ID.toString()));
    }

    @Test
    @DisplayName("GET history without a userId is 400, not 500")
    void historyRequiresUserId() throws Exception {
        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));

        verify(orderService, never()).getOrderHistory(any());
    }

    @Test
    @DisplayName("GET status returns just the status")
    void getsStatus() throws Exception {
        when(orderService.getStatus(ORDER_ID)).thenReturn(
                new OrderStatusResponse(ORDER_ID, OrderStatus.CREATED, Instant.parse("2026-08-24T10:00:00Z")));

        mockMvc.perform(get("/api/orders/{orderId}/status", ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(ORDER_ID.toString()))
                .andExpect(jsonPath("$.status").value("CREATED"));
    }

    @Test
    @DisplayName("POST cancel returns the cancelled order")
    void cancelsOrder() throws Exception {
        when(orderService.cancelOrder(ORDER_ID)).thenReturn(sampleResponse(OrderStatus.CANCELLED));

        mockMvc.perform(post("/api/orders/{orderId}/cancel", ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    @DisplayName("POST cancel on a non-cancellable order is 409 ORDER_NOT_CANCELLABLE")
    void cancelConflict() throws Exception {
        when(orderService.cancelOrder(ORDER_ID))
                .thenThrow(ConflictException.notCancellable(ORDER_ID, OrderStatus.CANCELLED));

        mockMvc.perform(post("/api/orders/{orderId}/cancel", ORDER_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("ORDER_NOT_CANCELLABLE"));
    }

    @Test
    @DisplayName("a malformed UUID in the path is 400, not 500")
    void rejectsMalformedUuid() throws Exception {
        mockMvc.perform(get("/api/orders/{orderId}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("BAD_REQUEST"));
    }
}
