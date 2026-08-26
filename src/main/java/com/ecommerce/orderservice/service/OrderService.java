package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.CreateOrderRequest;
import com.ecommerce.orderservice.dto.OrderLineItemRequest;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.dto.OrderStatusResponse;
import com.ecommerce.orderservice.dto.ShippingAddressRequest;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.event.OrderPlacedEvent;
import com.ecommerce.orderservice.exception.BadRequestException;
import com.ecommerce.orderservice.exception.ConflictException;
import com.ecommerce.orderservice.exception.ErrorCode;
import com.ecommerce.orderservice.exception.ResourceNotFoundException;
import com.ecommerce.orderservice.mapper.OrderMapper;
import com.ecommerce.orderservice.repository.OrderRepository;
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

    private static final Logger log =
            LoggerFactory.getLogger(OrderService.class);

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;

    public OrderService(
            OrderRepository orderRepository,
            ApplicationEventPublisher eventPublisher) {

        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Cart Service has already resolved the customer's identity and delivery address
     * (it is the caller that owns checkout) and snapshots both onto this request, so
     * Order Service no longer calls User Service itself here.
     */
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {

        validateTotalMatchesItems(request);

        if (orderRepository.existsByCartId(request.cartId())) {
            throw ConflictException.duplicateOrder(request.cartId());
        }

        ShippingAddressRequest address = request.shippingAddress();

        Order order = new Order(
                request.cartId(),
                request.userId(),
                request.email(),
                request.firstName(),
                request.lastName(),
                address.addressLine1(),
                address.addressLine2(),
                address.city(),
                address.state(),
                address.country(),
                address.pincode(),
                request.totalPrice()
        );

        for (OrderLineItemRequest line : request.items()) {

            order.addItem(
                    new OrderItem(
                            line.cartItemId(),
                            line.productId(),
                            line.variantId(),
                            line.merchantId(),
                            line.productName(),
                            line.imageUrl(),
                            line.quantity(),
                            line.unitPrice(),
                            line.lineTotal()
                    )
            );
        }

        Order saved =
                orderRepository.saveAndFlush(order);

        log.info(
                "Placed order {} for user {} from cart {} ({} items, total {})",
                saved.getId(),
                saved.getUserId(),
                saved.getCartId(),
                saved.getItems().size(),
                saved.getTotalPrice()
        );

        eventPublisher.publishEvent(
                OrderPlacedEvent.from(
                        saved,
                        request.firstName(),
                        request.lastName()
                )
        );

        return OrderMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(UUID orderId) {

        Order order = orderRepository
                .findWithItemsById(orderId)
                .orElseThrow(
                        () -> ResourceNotFoundException.order(orderId)
                );

        return OrderMapper.toResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrderHistory(UUID userId) {

        return orderRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(OrderMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderStatusResponse getStatus(UUID orderId) {

        Order order = orderRepository
                .findById(orderId)
                .orElseThrow(
                        () -> ResourceNotFoundException.order(orderId)
                );

        return new OrderStatusResponse(
                order.getId(),
                order.getStatus(),
                order.getUpdatedAt()
        );
    }

    @Transactional
    public OrderResponse cancelOrder(UUID orderId) {

        Order order = orderRepository
                .findWithItemsById(orderId)
                .orElseThrow(
                        () -> ResourceNotFoundException.order(orderId)
                );

        order.cancel();

        Order saved =
                orderRepository.saveAndFlush(order);

        log.info(
                "Cancelled order {} for user {}",
                saved.getId(),
                saved.getUserId()
        );

        /*
         * No Kafka event is published for cancellation.
         * The only Kafka event currently required is OrderPlacedEvent.
         */

        return OrderMapper.toResponse(saved);
    }

    private void validateTotalMatchesItems(
            CreateOrderRequest request) {

        BigDecimal computed = BigDecimal.ZERO;

        for (OrderLineItemRequest line : request.items()) {

            BigDecimal calculatedLineTotal =
                    line.unitPrice()
                            .multiply(
                                    BigDecimal.valueOf(
                                            line.quantity()
                                    )
                            );

            if (line.lineTotal().compareTo(
                    calculatedLineTotal
            ) != 0) {

                throw new BadRequestException(
                        ErrorCode.ORDER_TOTAL_MISMATCH,
                        "lineTotal "
                                + line.lineTotal()
                                + " does not match unitPrice "
                                + line.unitPrice()
                                + " multiplied by quantity "
                                + line.quantity()
                                + ". Expected "
                                + calculatedLineTotal
                );
            }

            computed = computed.add(
                    calculatedLineTotal
            );
        }

        if (request.totalPrice().compareTo(computed) != 0) {

            throw new BadRequestException(
                    ErrorCode.ORDER_TOTAL_MISMATCH,
                    "totalPrice "
                            + request.totalPrice()
                            + " does not match the sum of the line items "
                            + computed
                            + " (unit price times quantity)."
            );
        }
    }
}