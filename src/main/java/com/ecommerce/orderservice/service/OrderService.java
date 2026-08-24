package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.client.UserServiceClient;
import com.ecommerce.orderservice.client.dto.AddressResponse;
import com.ecommerce.orderservice.client.dto.UserResponse;
import com.ecommerce.orderservice.dto.CreateOrderRequest;
import com.ecommerce.orderservice.dto.OrderLineItemRequest;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.dto.OrderStatusResponse;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.event.OrderCancelledEvent;
import com.ecommerce.orderservice.event.OrderCreatedEvent;
import com.ecommerce.orderservice.exception.BadRequestException;
import com.ecommerce.orderservice.exception.ConflictException;
import com.ecommerce.orderservice.exception.ErrorCode;
import com.ecommerce.orderservice.exception.ResourceNotFoundException;
import com.ecommerce.orderservice.mapper.OrderMapper;
import com.ecommerce.orderservice.repository.OrderRepository;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private static final String CUSTOMER_USER_TYPE = "CUSTOMER";

    private final OrderRepository orderRepository;
    private final UserServiceClient userServiceClient;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(OrderRepository orderRepository,
                        UserServiceClient userServiceClient,
                        ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.userServiceClient = userServiceClient;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {

        validateTotalMatchesItems(request);

        UserResponse user;
        AddressResponse address;
        try {
            user = userServiceClient.getUser(request.userId(), request.userId(), CUSTOMER_USER_TYPE);
            address = userServiceClient.getAddress(
                    request.userId(), request.addressId(), request.userId(), CUSTOMER_USER_TYPE);
        } catch (FeignException.NotFound ex) {
            throw new BadRequestException(ErrorCode.INVALID_ORDER_DETAILS,
                    "Cannot place order: user " + request.userId() + " or address "
                            + request.addressId() + " could not be found in User Service.");
        }

        if (orderRepository.existsByCartId(request.cartId())) {
            throw ConflictException.duplicateOrder(request.cartId());
        }

        Order order = new Order(
                request.cartId(), request.userId(), user.email(),
                address.addressLine1(), address.addressLine2(), address.city(),
                address.state(), address.country(), address.pincode(),
                request.totalPrice());
        for (OrderLineItemRequest line : request.items()) {
            order.addItem(new OrderItem(line.productId(), line.merchantId(),
                    line.productName(), line.quantity(), line.unitPrice()));
        }

        Order saved = orderRepository.saveAndFlush(order);
        log.info("Placed order {} for user {} from cart {} ({} items, total {})",
                saved.getId(), saved.getUserId(), saved.getCartId(),
                saved.getItems().size(), saved.getTotalPrice());

        eventPublisher.publishEvent(OrderCreatedEvent.from(saved));

        return OrderMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {
        Order order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> ResourceNotFoundException.order(orderId));
        return OrderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrderHistory(UUID userId) {
        return orderRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(OrderMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderStatusResponse getStatus(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ResourceNotFoundException.order(orderId));
        return new OrderStatusResponse(order.getId(), order.getStatus(), order.getUpdatedAt());
    }

    @Transactional
    public OrderResponse cancelOrder(UUID orderId) {
        Order order = orderRepository.findWithItemsById(orderId)
                .orElseThrow(() -> ResourceNotFoundException.order(orderId));

        order.cancel();
        Order saved = orderRepository.saveAndFlush(order);
        log.info("Cancelled order {} for user {}", saved.getId(), saved.getUserId());

        eventPublisher.publishEvent(OrderCancelledEvent.from(saved));

        return OrderMapper.toResponse(saved);
    }

    private void validateTotalMatchesItems(CreateOrderRequest request) {
        BigDecimal computed = request.items().stream()
                .map(line -> line.unitPrice().multiply(BigDecimal.valueOf(line.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (request.totalPrice().compareTo(computed) != 0) {
            throw new BadRequestException(ErrorCode.ORDER_TOTAL_MISMATCH,
                    "totalPrice " + request.totalPrice() + " does not match the sum of the line items "
                            + computed + " (unit price times quantity).");
        }
    }
}
