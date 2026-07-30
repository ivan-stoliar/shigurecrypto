package com.example.demo.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnomalyCacheConsumer {

    private final RedisTemplate<String, AnomalyEvent> anomalyRedisTemplate;

    @RetryableTopic
    @KafkaListener(topics = "anomalies.detected", groupId = "redis-cache-group")
    public void consumeAnomaly(AnomalyEvent event) {
        log.info("Caching anomaly for symbol: {}", event.getSymbol());

        String key = "anomaly:" + event.getSymbol();
        anomalyRedisTemplate.opsForValue().set(key, event, Duration.ofHours(24));
    }
}
