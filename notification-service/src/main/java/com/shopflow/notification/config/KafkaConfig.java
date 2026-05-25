package com.shopflow.notification.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaConfig {

    @Value("${kafka.topics.notification-dlt}")
    private String notificationDlt;

    @Bean
    public NewTopic notificationDltTopic() {
        return TopicBuilder.name(notificationDlt).partitions(1).replicas(1).build();
    }
}
