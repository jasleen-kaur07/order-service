package com.ecommerce.orderservice.messaging;

import com.ecommerce.orderservice.event.OrderPlacedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.util.concurrent.CompletableFuture;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderEventPublisherTest {

    private static final String ORDER_PLACED_TOPIC = "order-placed";

    private static final String ORDER_ID =
            "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private OrderEventPublisher publisher(boolean enabled) {
        return new OrderEventPublisher(
                kafkaTemplate,
                enabled,
                ORDER_PLACED_TOPIC
        );
    }

    private static OrderPlacedEvent placedEvent() {
        return new OrderPlacedEvent(
                ORDER_ID,
                "jasleen@gmail.com",
                "Jasleen",
                "Kaur"
        );
    }

    @Test
    @DisplayName("sends OrderPlacedEvent to order-placed topic")
    void publishesOrderPlaced() {

        CompletableFuture<SendResult<String, Object>> future =
                CompletableFuture.completedFuture(null);

        when(kafkaTemplate.send(
                anyString(),
                anyString(),
                any()
        )).thenReturn(future);

        OrderPlacedEvent event = placedEvent();

        publisher(true).onOrderPlaced(event);

        verify(kafkaTemplate).send(
                ORDER_PLACED_TOPIC,
                ORDER_ID,
                event
        );
    }

    @Test
    @DisplayName("publishes nothing when events are disabled")
    void disabledPublishesNothing() {

        publisher(false).onOrderPlaced(placedEvent());

        verifyNoInteractions(kafkaTemplate);
    }

    @Test
    @DisplayName("swallows a synchronous Kafka send failure")
    void swallowsSendFailure() {

        when(kafkaTemplate.send(
                anyString(),
                anyString(),
                any()
        )).thenThrow(
                new RuntimeException("no broker")
        );

        OrderEventPublisher publisher =
                publisher(true);

        OrderPlacedEvent event =
                placedEvent();

        assertThatCode(
                () -> publisher.onOrderPlaced(event)
        ).doesNotThrowAnyException();
    }
}