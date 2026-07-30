package com.example.demo.service;

import com.example.demo.entity.Trade;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TradeConsumer {

    private final TradeService tradeService;

    @RetryableTopic
    @KafkaListener(topics = "trades.raw", groupId = "archiver-group")
    public void consumeTrade(Trade trade, Acknowledgment ack) {
        log.debug("Consumed trade from Kafka -> Symbol: {}, Exchange: {}, Price: {}", 
                trade.getSymbol(), trade.getExchange(), trade.getPrice());
        
        // Let exceptions propagate so Spring Kafka's retry mechanism can catch them
        tradeService.saveTrade(trade);
        ack.acknowledge();
    }
}
