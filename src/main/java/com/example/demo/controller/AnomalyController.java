package com.example.demo.controller;

import com.example.demo.kafka.AnomalyEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/anomalies")
@RequiredArgsConstructor
public class AnomalyController {

    private final RedisTemplate<String, AnomalyEvent> anomalyRedisTemplate;

    @GetMapping
    public List<AnomalyEvent> getAnomalies() {

        Set<String> keys = anomalyRedisTemplate.keys("anomaly:*");
        if (keys == null || keys.isEmpty()) {
            return new ArrayList<>();
        }

        // Multi-get fetches all values in a single round-trip to Redis
        List<AnomalyEvent> anomalies = anomalyRedisTemplate.opsForValue().multiGet(keys);
        return anomalies == null ? new ArrayList<>() : anomalies;
    }
}
// #TODO: SCAN Instead of KEYS for production