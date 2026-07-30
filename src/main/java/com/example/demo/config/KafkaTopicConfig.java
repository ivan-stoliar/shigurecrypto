package com.example.demo.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic tradesRaw() {
        return TopicBuilder.name("trades.raw")
                .partitions(2)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic anomaliesDetected() {
        return TopicBuilder.name("anomalies.detected")
                .partitions(2)
                .replicas(1)
                .build();
    }

    // Spring Kafka's useSingleTopicForSameIntervals() will default to these names
    @Bean
    public NewTopic retryTopic() {
        return TopicBuilder.name("retry-2000")
                .partitions(2)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic deadLettersTopic() {
        return TopicBuilder.name("retry-2000-dlt")
                .partitions(2)
                .replicas(1)
                .build();
    }
}
