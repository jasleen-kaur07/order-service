package com.ecommerce.orderservice.controller;

import com.ecommerce.orderservice.dto.CreateOrderRequest;
import com.ecommerce.orderservice.dto.ErrorResponse;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.dto.OrderStatusResponse;
import com.ecommerce.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Order creation (called by Cart Service at checkout), retrieval, history, status and cancellation.")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(
            summary = "Place an order",
            description = """
                    Called by Cart Service at checkout. Cart Service resolves the customer's email,
                    name and delivery address itself and **snapshots** them onto this request; Order
                    Service no longer calls User Service. Order Service validates that `totalPrice`
                    equals the sum of the line items, persists the order and its items atomically, and
                    publishes an `OrderPlaced` event after commit.

                    One order per cart: a resubmitted checkout for the same `cartId` is a 409.""")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Order created; Location header points at it"),
            @ApiResponse(responseCode = "400", description = "VALIDATION_ERROR or ORDER_TOTAL_MISMATCH",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "DUPLICATE_ORDER - an order already exists for this cart",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse created = orderService.createOrder(request);
        return ResponseEntity
                .created(URI.create("/api/orders/" + created.id()))
                .body(created);
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get one order", description = "Returns the full order including its snapshotted address and line items.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order found"),
            @ApiResponse(responseCode = "404", description = "ORDER_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<OrderResponse> getOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.getOrder(orderId));
    }

    @GetMapping
    @Operation(
            summary = "List a customer's orders",
            description = "Order history for the given `userId`, newest first. This is the endpoint the frontend calls for \"my orders\".")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Orders returned (possibly an empty list)"),
            @ApiResponse(responseCode = "400", description = "BAD_REQUEST - userId is missing or not a valid UUID",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<List<OrderResponse>> getOrderHistory(@RequestParam UUID userId) {
        return ResponseEntity.ok(orderService.getOrderHistory(userId));
    }

    @GetMapping("/{orderId}/status")
    @Operation(summary = "Get an order's status", description = "A lightweight view of just the current status and when it last changed.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status returned"),
            @ApiResponse(responseCode = "404", description = "ORDER_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<OrderStatusResponse> getStatus(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.getStatus(orderId));
    }

    @PostMapping("/{orderId}/cancel")
    @Operation(
            summary = "Cancel an order",
            description = """
                    Moves a `CREATED` order to `CANCELLED`. No Kafka event is published for
                    cancellation - `OrderPlaced` is currently the only order event this service
                    emits. Only a `CREATED` order may be cancelled; cancelling one that is
                    already cancelled is a 409, not a silent success.""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Order cancelled"),
            @ApiResponse(responseCode = "404", description = "ORDER_NOT_FOUND",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "ORDER_NOT_CANCELLABLE - the order is not in a cancellable state",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<OrderResponse> cancelOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.cancelOrder(orderId));
    }
}