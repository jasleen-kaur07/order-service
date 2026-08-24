package com.ecommerce.orderservice.messaging;

import com.ecommerce.orderservice.entity.OrderStatus;
import com.ecommerce.orderservice.event.OrderCancelledEvent;
import com.ecommerce.orderservice.event.OrderCreatedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEventPublisherTest {

    private static final String CREATED_TOPIC = "order.created";
    private static final String CANCELLED_TOPIC = "order.cancelled";
    private static final UUID ORDER_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID USER_ID = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private OrderEventPublisher publisher(boolean enabled) {
        return new OrderEventPublisher(kafkaTemplate, enabled, CREATED_TOPIC, CANCELLED_TOPIC);
    }

    private static OrderCreatedEvent createdEvent() {
        return new OrderCreatedEvent(ORDER_ID, UUID.randomUUID(), USER_ID, "jasleen@gmail.com",
                OrderStatus.CREATED, new BigDecimal("100.00"),
                new OrderCreatedEvent.ShippingAddress("l1", null, "Noida", "UP", "India", "201301"),
                List.of(new OrderCreatedEvent.Item(UUID.randomUUID(), UUID.randomUUID(), "Widget", 2, new BigDecimal("50.00"))),
                Instant.parse("2026-08-24T10:00:00Z"));
    }

    private static OrderCancelledEvent cancelledEvent() {
        return new OrderCancelledEvent(ORDER_ID, USER_ID, "jasleen@gmail.com",
                Instant.parse("2026-08-24T11:00:00Z"));
    }

    @Test
    @DisplayName("sends OrderCreated to the created topic, keyed by orderId")
    void publishesOrderCreated() {
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

        OrderCreatedEvent event = createdEvent();
        publisher(true).onOrderCreated(event);

        verify(kafkaTemplate).send(CREATED_TOPIC, ORDER_ID.toString(), event);
    }

    @Test
    @DisplayName("sends OrderCancelled to the cancelled topic, keyed by orderId")
    void publishesOrderCancelled() {
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(null);
        when(kafkaTemplate.send(anyString(), anyString(), any())).thenReturn(future);

        OrderCancelledEvent event = cancelledEvent();
        publisher(true).onOrderCancelled(event);

        verify(kafkaTemplate).send(CANCELLED_TOPIC, ORDER_ID.toString(), event);
    }

    @Test
    @DisplayName("publishes nothing when events are disabled")
    void disabledPublishesNothing() {
        publisher(false).onOrderCreated(createdEvent());

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("swallows a synchronous send failure - the order is already committed")
    void swallowsSendFailure() {
        when(kafkaTemplate.send(anyString(), anyString(), any()))
                .thenThrow(new RuntimeException("no broker"));

        OrderEventPublisher publisher = publisher(true);
        OrderCreatedEvent event = createdEvent();

        assertThatCode(() -> publisher.onOrderCreated(event)).doesNotThrowAnyException();
    }
}
