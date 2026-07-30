package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.retrytopic.RetryTopicConfiguration;
import org.springframework.kafka.retrytopic.RetryTopicConfigurationBuilder;

@Configuration
public class KafkaRetryConfig {

    @Bean
    public RetryTopicConfiguration retryTopicConfiguration(KafkaTemplate<String, Object> template) {
        return RetryTopicConfigurationBuilder
                .newInstance()
                .fixedBackOff(2000)
                .maxAttempts(3)
                .useSingleTopicForSameIntervals()
                .create(template);
    }
}
