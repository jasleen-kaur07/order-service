package com.ecommerce.orderservice.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@ConditionalOnProperty(
        name = "orders.events.enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class KafkaTopicsConfig {

    @Bean
    public NewTopic orderPlacedTopic(
            @Value("${orders.events.topics.order-placed}")
            String topic
    ) {
        return TopicBuilder
                .name(topic)
                .partitions(3)
                .replicas(1)
                .build();
    }
}