package com.ecommerce.orderservice.messaging;

import com.ecommerce.orderservice.event.OrderPlacedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderEventPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final boolean enabled;
    private final String orderPlacedTopic;

    public OrderEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            @Value("${orders.events.enabled:true}") boolean enabled,
            @Value("${orders.events.topics.order-placed:order-placed}")
            String orderPlacedTopic) {

        this.kafkaTemplate = kafkaTemplate;
        this.enabled = enabled;
        this.orderPlacedTopic = orderPlacedTopic;
    }

    /**
     * Publishes the OrderPlacedEvent after the order transaction
     * has successfully committed.
     */
    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT
    )
    public void onOrderPlaced(OrderPlacedEvent event) {

        publish(
                orderPlacedTopic,
                event.orderNumber(),
                event,
                "OrderPlaced"
        );
    }

    private void publish(
            String topic,
            String key,
            Object payload,
            String label) {

        if (!enabled) {
            log.debug(
                    "Order events disabled; not publishing {} for order {}",
                    label,
                    key
            );
            return;
        }

        try {

            kafkaTemplate
                    .send(topic, key, payload)
                    .whenComplete((result, ex) -> {

                        if (ex != null) {

                            log.error(
                                    "Failed to publish {} for order {} to topic {}. "
                                            + "The order is committed; the event was not sent. "
                                            + "Cause: {}",
                                    label,
                                    key,
                                    topic,
                                    ex.toString()
                            );

                        } else {

                            log.info(
                                    "Published {} for order {} to topic {}",
                                    label,
                                    key,
                                    topic
                            );
                        }
                    });

        } catch (Exception ex) {

            log.error(
                    "Could not publish {} for order {} to topic {}. "
                            + "The order is committed; the event was not sent. "
                            + "Cause: {}",
                    label,
                    key,
                    topic,
                    ex.toString()
            );
        }
    }
}