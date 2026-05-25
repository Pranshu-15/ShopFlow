package com.shopflow.order.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${kafka.topics.order-events}")
    private String orderEventsTopic;

    @Value("${kafka.topics.inventory-events}")
    private String inventoryEventsTopic;

    @Value("${kafka.topics.payment-events}")
    private String paymentEventsTopic;

    @Bean
    public NewTopic orderEventsNewTopic() {
        return TopicBuilder.name(orderEventsTopic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic inventoryEventsNewTopic() {
        return TopicBuilder.name(inventoryEventsTopic).partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic paymentEventsNewTopic() {
        return TopicBuilder.name(paymentEventsTopic).partitions(1).replicas(1).build();
    }
}
