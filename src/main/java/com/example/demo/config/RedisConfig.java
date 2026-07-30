package com.example.demo.config;

import com.example.demo.kafka.AnomalyEvent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, AnomalyEvent> anomalyRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, AnomalyEvent> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        template.setKeySerializer(new StringRedisSerializer());
        
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        Jackson2JsonRedisSerializer<AnomalyEvent> serializer = new Jackson2JsonRedisSerializer<>(objectMapper, AnomalyEvent.class);
        template.setValueSerializer(serializer);
        
        return template;
    }
}
