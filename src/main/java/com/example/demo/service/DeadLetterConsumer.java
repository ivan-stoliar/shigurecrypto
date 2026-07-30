package com.example.demo.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class DeadLetterConsumer {

    @KafkaListener(topics = "retry-2000-dlt", groupId = "dlt-group")
    public void consumeDlt(String payload, 
                           @Header(name = KafkaHeaders.ORIGINAL_TOPIC, required = false) String originalTopic,
                           @Header(name = KafkaHeaders.EXCEPTION_MESSAGE, required = false) String exceptionMessage) {
        
        log.error("Received message in DLT! Original Topic: {}, Exception: {}, Payload: {}", 
                originalTopic, exceptionMessage, payload);
    }
}
