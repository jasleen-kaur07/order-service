package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.client.UserServiceClient;
import com.ecommerce.orderservice.client.dto.AddressResponse;
import com.ecommerce.orderservice.client.dto.UserResponse;
import com.ecommerce.orderservice.dto.CreateOrderRequest;
import com.ecommerce.orderservice.dto.OrderLineItemRequest;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.event.OrderCancelledEvent;
import com.ecommerce.orderservice.event.OrderCreatedEvent;
import com.ecommerce.orderservice.exception.ApiException;
import com.ecommerce.orderservice.exception.BadRequestException;
import com.ecommerce.orderservice.exception.ConflictException;
import com.ecommerce.orderservice.exception.ErrorCode;
import com.ecommerce.orderservice.exception.ResourceNotFoundException;
import com.ecommerce.orderservice.repository.OrderRepository;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final UUID ORDER_ID =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static final UUID CART_ID =
            UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private static final UUID USER_ID =
            UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private static final UUID ADDRESS_ID =
            UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    private static final UUID PRODUCT_ID =
            UUID.fromString("eeeeeeee-eeee-eeee-eeee-eeeeeeeeeeee");

    private static final UUID MERCHANT_ID =
            UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");

    private static final String EMAIL = "jasleen@gmail.com";

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    private static CreateOrderRequest requestWithTotal(BigDecimal total) {
        return new CreateOrderRequest(
                CART_ID,
                USER_ID,
                ADDRESS_ID,
                List.of(
                        new OrderLineItemRequest(
                                PRODUCT_ID,
                                MERCHANT_ID,
                                "Widget",
                                2,
                                new BigDecimal("50.00")
                        )
                ),
                total
        );
    }

    private static CreateOrderRequest validRequest() {
        return requestWithTotal(new BigDecimal("100.00"));
    }

    private void stubUserServiceOk() {

        when(userServiceClient.getUser(USER_ID))
                .thenReturn(new UserResponse(USER_ID, EMAIL));

        when(userServiceClient.getAddress(USER_ID, ADDRESS_ID))
                .thenReturn(
                        new AddressResponse(
                                "123 Main Street",
                                "Apt 4B",
                                "Noida",
                                "Uttar Pradesh",
                                "India",
                                "201301"
                        )
                );
    }

    private static Order sampleOrder() {

        Order order = new Order(
                CART_ID,
                USER_ID,
                EMAIL,
                "123 Main Street",
                "Apt 4B",
                "Noida",
                "Uttar Pradesh",
                "India",
                "201301",
                new BigDecimal("100.00")
        );

        order.addItem(
                new OrderItem(
                        PRODUCT_ID,
                        MERCHANT_ID,
                        "Widget",
                        2,
                        new BigDecimal("50.00")
                )
        );

        return order;
    }

    private static FeignException.NotFound notFound() {

        Request request = Request.create(
                Request.HttpMethod.GET,
                "http://user-service/api/users",
                Collections.emptyMap(),
                null,
                StandardCharsets.UTF_8
        );

        return new FeignException.NotFound(
                "Not Found",
                request,
                null,
                Collections.emptyMap()
        );
    }

    private static ErrorCode errorCodeOf(Throwable ex) {
        return ((ApiException) ex).getErrorCode();
    }

    @Nested
    @DisplayName("placing an order")
    class Create {

        @Test
        @DisplayName("snapshots email + address, persists the order and publishes OrderCreated")
        void placesOrder() {

            stubUserServiceOk();

            when(orderRepository.existsByCartId(CART_ID))
                    .thenReturn(false);

            when(orderRepository.saveAndFlush(any(Order.class)))
                    .thenAnswer(call -> call.getArgument(0));

            OrderResponse response =
                    orderService.createOrder(validRequest());

            assertThat(response.status())
                    .isEqualTo(OrderStatus.CREATED);

            assertThat(response.email())
                    .isEqualTo(EMAIL);

            assertThat(response.totalPrice())
                    .isEqualByComparingTo("100.00");

            assertThat(response.shippingAddress().city())
                    .isEqualTo("Noida");

            assertThat(response.items())
                    .hasSize(1);

            assertThat(response.items().get(0).productName())
                    .isEqualTo("Widget");

            assertThat(response.items().get(0).lineTotal())
                    .isEqualByComparingTo("100.00");

            verify(eventPublisher)
                    .publishEvent(any(OrderCreatedEvent.class));
        }

        @Test
        @DisplayName("rejects a totalPrice that does not match the line items, before any network or DB work")
        void rejectsTotalMismatch() {

            assertThatThrownBy(
                    () -> orderService.createOrder(
                            requestWithTotal(new BigDecimal("999.00"))
                    )
            )
                    .isInstanceOf(BadRequestException.class)
                    .extracting(OrderServiceTest::errorCodeOf)
                    .isEqualTo(ErrorCode.ORDER_TOTAL_MISMATCH);

            verifyNoInteractions(userServiceClient);

            verify(
                    orderRepository,
                    never()
            ).saveAndFlush(any());

            verify(
                    eventPublisher,
                    never()
            ).publishEvent(any());
        }

        @Test
        @DisplayName("maps a User Service 404 to INVALID_ORDER_DETAILS and does not create an order")
        void rejectsUnknownUserOrAddress() {

            when(userServiceClient.getUser(USER_ID))
                    .thenThrow(notFound());

            assertThatThrownBy(
                    () -> orderService.createOrder(validRequest())
            )
                    .isInstanceOf(BadRequestException.class)
                    .extracting(OrderServiceTest::errorCodeOf)
                    .isEqualTo(ErrorCode.INVALID_ORDER_DETAILS);

            verify(
                    orderRepository,
                    never()
            ).saveAndFlush(any());

            verify(
                    eventPublisher,
                    never()
            ).publishEvent(any());
        }

        @Test
        @DisplayName("rejects a second order for the same cart (one order per checkout)")
        void rejectsDuplicateCart() {

            stubUserServiceOk();

            when(orderRepository.existsByCartId(CART_ID))
                    .thenReturn(true);

            assertThatThrownBy(
                    () -> orderService.createOrder(validRequest())
            )
                    .isInstanceOf(ConflictException.class)
                    .extracting(OrderServiceTest::errorCodeOf)
                    .isEqualTo(ErrorCode.DUPLICATE_ORDER);

            verify(
                    orderRepository,
                    never()
            ).saveAndFlush(any());

            verify(
                    eventPublisher,
                    never()
            ).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("reading orders")
    class Read {

        @Test
        @DisplayName("returns the order with its items")
        void getsOrder() {

            when(orderRepository.findWithItemsById(ORDER_ID))
                    .thenReturn(
                            Optional.of(sampleOrder())
                    );

            OrderResponse response =
                    orderService.getOrder(ORDER_ID);

            assertThat(response.status())
                    .isEqualTo(OrderStatus.CREATED);

            assertThat(response.items())
                    .hasSize(1);
        }

        @Test
        @DisplayName("throws ORDER_NOT_FOUND for an unknown order")
        void getOrderNotFound() {

            when(orderRepository.findWithItemsById(ORDER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> orderService.getOrder(ORDER_ID)
            )
                    .isInstanceOf(ResourceNotFoundException.class)
                    .extracting(OrderServiceTest::errorCodeOf)
                    .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
        }

        @Test
        @DisplayName("returns a customer's order history")
        void getsHistory() {

            when(
                    orderRepository.findByUserIdOrderByCreatedAtDesc(USER_ID)
            )
                    .thenReturn(
                            List.of(
                                    sampleOrder(),
                                    sampleOrder()
                            )
                    );

            assertThat(
                    orderService.getOrderHistory(USER_ID)
            )
                    .hasSize(2);
        }

        @Test
        @DisplayName("returns just the status for the status endpoint")
        void getsStatus() {

            when(orderRepository.findById(ORDER_ID))
                    .thenReturn(
                            Optional.of(sampleOrder())
                    );

            assertThat(
                    orderService.getStatus(ORDER_ID).status()
            )
                    .isEqualTo(OrderStatus.CREATED);
        }

        @Test
        @DisplayName("throws ORDER_NOT_FOUND asking the status of an unknown order")
        void getStatusNotFound() {

            when(orderRepository.findById(ORDER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> orderService.getStatus(ORDER_ID)
            )
                    .isInstanceOf(ResourceNotFoundException.class)
                    .extracting(OrderServiceTest::errorCodeOf)
                    .isEqualTo(ErrorCode.ORDER_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("cancelling an order")
    class Cancel {

        @Test
        @DisplayName("moves a CREATED order to CANCELLED and publishes OrderCancelled")
        void cancels() {

            Order order = sampleOrder();

            when(orderRepository.findWithItemsById(ORDER_ID))
                    .thenReturn(Optional.of(order));

            when(orderRepository.saveAndFlush(order))
                    .thenAnswer(call -> call.getArgument(0));

            OrderResponse response =
                    orderService.cancelOrder(ORDER_ID);

            assertThat(response.status())
                    .isEqualTo(OrderStatus.CANCELLED);

            verify(eventPublisher)
                    .publishEvent(any(OrderCancelledEvent.class));
        }

        @Test
        @DisplayName("throws ORDER_NOT_FOUND cancelling an unknown order")
        void cancelNotFound() {

            when(orderRepository.findWithItemsById(ORDER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> orderService.cancelOrder(ORDER_ID)
            )
                    .isInstanceOf(ResourceNotFoundException.class)
                    .extracting(OrderServiceTest::errorCodeOf)
                    .isEqualTo(ErrorCode.ORDER_NOT_FOUND);

            verify(
                    orderRepository,
                    never()
            ).saveAndFlush(any());

            verify(
                    eventPublisher,
                    never()
            ).publishEvent(any());
        }

        @Test
        @DisplayName("rejects cancelling an order that is already cancelled, and publishes nothing")
        void rejectsCancellingCancelled() {

            Order order = sampleOrder();

            order.cancel();

            when(orderRepository.findWithItemsById(ORDER_ID))
                    .thenReturn(Optional.of(order));

            assertThatThrownBy(
                    () -> orderService.cancelOrder(ORDER_ID)
            )
                    .isInstanceOf(ConflictException.class)
                    .extracting(OrderServiceTest::errorCodeOf)
                    .isEqualTo(ErrorCode.ORDER_NOT_CANCELLABLE);

            verify(
                    orderRepository,
                    never()
            ).saveAndFlush(any());

            verify(
                    eventPublisher,
                    never()
            ).publishEvent(any());
        }
    }
}