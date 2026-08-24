package com.ecommerce.orderservice.messaging;

import com.ecommerce.orderservice.event.OrderCancelledEvent;
import com.ecommerce.orderservice.event.OrderCreatedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class OrderEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(OrderEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final boolean enabled;
    private final String orderCreatedTopic;
    private final String orderCancelledTopic;

    public OrderEventPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                               @Value("${orders.events.enabled:true}") boolean enabled,
                               @Value("${orders.events.topics.order-created:order.created}") String orderCreatedTopic,
                               @Value("${orders.events.topics.order-cancelled:order.cancelled}") String orderCancelledTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.enabled = enabled;
        this.orderCreatedTopic = orderCreatedTopic;
        this.orderCancelledTopic = orderCancelledTopic;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        publish(orderCreatedTopic, event.orderId().toString(), event, "OrderCreated");
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCancelled(OrderCancelledEvent event) {
        publish(orderCancelledTopic, event.orderId().toString(), event, "OrderCancelled");
    }

    private void publish(String topic, String key, Object payload, String label) {
        if (!enabled) {
            log.debug("Order events disabled; not publishing {} for order {}", label, key);
            return;
        }
        try {
            kafkaTemplate.send(topic, key, payload).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish {} for order {} to topic {}. The order is committed; "
                            + "only the notification was not sent. Cause: {}", label, key, topic, ex.toString());
                } else {
                    log.info("Published {} for order {} to topic {}", label, key, topic);
                }
            });
        } catch (Exception ex) {
            log.error("Could not publish {} for order {} to topic {}. The order is committed; "
                    + "only the notification was not sent. Cause: {}", label, key, topic, ex.toString());
        }
    }
}
