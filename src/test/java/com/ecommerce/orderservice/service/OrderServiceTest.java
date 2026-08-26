package com.ecommerce.orderservice.service;

import com.ecommerce.orderservice.dto.CreateOrderRequest;
import com.ecommerce.orderservice.dto.OrderLineItemRequest;
import com.ecommerce.orderservice.dto.OrderResponse;
import com.ecommerce.orderservice.dto.ShippingAddressRequest;
import com.ecommerce.orderservice.entity.Order;
import com.ecommerce.orderservice.entity.OrderItem;
import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.event.OrderPlacedEvent;
import com.ecommerce.orderservice.exception.ApiException;
import com.ecommerce.orderservice.exception.BadRequestException;
import com.ecommerce.orderservice.exception.ConflictException;
import com.ecommerce.orderservice.exception.ErrorCode;
import com.ecommerce.orderservice.exception.ResourceNotFoundException;
import com.ecommerce.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final UUID ORDER_ID =
            UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    private static final UUID CART_ID =
            UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    private static final UUID USER_ID =
            UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    private static final UUID CART_ITEM_ID =
            UUID.fromString("dddddddd-dddd-dddd-dddd-dddddddddddd");

    private static final String PRODUCT_ID = "product-101";
    private static final String VARIANT_ID = "variant-101";
    private static final String MERCHANT_ID = "merchant-201";

    private static final String EMAIL = "jasleen@gmail.com";

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    private static ShippingAddressRequest sampleShippingAddress() {
        return new ShippingAddressRequest(
                "123 Main Street",
                "Apt 4B",
                "Noida",
                "Uttar Pradesh",
                "India",
                "201301"
        );
    }

    private static CreateOrderRequest requestWithTotal(
            BigDecimal total) {

        return new CreateOrderRequest(
                CART_ID,
                USER_ID,
                EMAIL,
                "Jasleen",
                "Kaur",
                sampleShippingAddress(),
                List.of(
                        new OrderLineItemRequest(
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
                total
        );
    }

    private static CreateOrderRequest validRequest() {
        return requestWithTotal(
                new BigDecimal("100.00")
        );
    }

    private static Order sampleOrder() {

        Order order = new Order(
                CART_ID,
                USER_ID,
                EMAIL,
                "Jasleen",
                "Kaur",
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
        );

        return order;
    }

    private static ErrorCode errorCodeOf(Throwable ex) {
        return ((ApiException) ex).getErrorCode();
    }

    @Nested
    @DisplayName("placing an order")
    class Create {

        @Test
        @DisplayName(
                "snapshots the identity + address Cart Service sent, persists the order and publishes OrderPlaced"
        )
        void placesOrder() {

            when(orderRepository.existsByCartId(CART_ID))
                    .thenReturn(false);

            /*
             * In the real application Hibernate generates the Order UUID
             * because Order.id uses @GeneratedValue.
             *
             * This is a unit test, so there is no Hibernate/database.
             * We therefore simulate the generated ID here.
             */
            when(orderRepository.saveAndFlush(any(Order.class)))
                    .thenAnswer(call -> {
                        Order order = call.getArgument(0);

                        try {
                            var idField =
                                    Order.class.getDeclaredField("id");

                            idField.setAccessible(true);
                            idField.set(order, ORDER_ID);

                        } catch (ReflectiveOperationException e) {
                            throw new RuntimeException(e);
                        }

                        return order;
                    });

            OrderResponse response =
                    orderService.createOrder(validRequest());

            assertThat(response.id())
                    .isEqualTo(ORDER_ID);

            assertThat(response.status())
                    .isEqualTo(OrderStatus.CREATED);

            assertThat(response.email())
                    .isEqualTo(EMAIL);

            assertThat(response.firstName())
                    .isEqualTo("Jasleen");

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
                    .publishEvent(any(OrderPlacedEvent.class));
        }

        @Test
        @DisplayName(
                "rejects a totalPrice that does not match the line items, before touching the database"
        )
        void rejectsTotalMismatch() {

            assertThatThrownBy(
                    () -> orderService.createOrder(
                            requestWithTotal(
                                    new BigDecimal("999.00")
                            )
                    )
            )
                    .isInstanceOf(BadRequestException.class)
                    .extracting(OrderServiceTest::errorCodeOf)
                    .isEqualTo(ErrorCode.ORDER_TOTAL_MISMATCH);

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
        @DisplayName(
                "rejects a second order for the same cart"
        )
        void rejectsDuplicateCart() {

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
        @DisplayName(
                "throws ORDER_NOT_FOUND for an unknown order"
        )
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
                    orderRepository.findByUserIdOrderByCreatedAtDesc(
                            USER_ID
                    )
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
        @DisplayName(
                "returns just the status for the status endpoint"
        )
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
        @DisplayName(
                "throws ORDER_NOT_FOUND asking the status of an unknown order"
        )
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
        @DisplayName(
                "moves a CREATED order to CANCELLED without publishing a Kafka event"
        )
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

            verify(eventPublisher, never())
                    .publishEvent(any());
        }

        @Test
        @DisplayName(
                "throws ORDER_NOT_FOUND cancelling an unknown order"
        )
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
        @DisplayName(
                "rejects cancelling an order that is already cancelled"
        )
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