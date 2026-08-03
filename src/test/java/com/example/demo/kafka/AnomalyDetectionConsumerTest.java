package com.example.demo.kafka;

import com.example.demo.entity.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AnomalyDetectionConsumerTest {

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    @InjectMocks
    private AnomalyDetectionConsumer consumer;

    @Captor
    private ArgumentCaptor<AnomalyEvent> eventCaptor;

    @BeforeEach
    void setUp() {
        // Inject values that are normally loaded from application.properties
        ReflectionTestUtils.setField(consumer, "thresholdPercentage", 0.05);
        ReflectionTestUtils.setField(consumer, "windowMinutes", 1);
        ReflectionTestUtils.setField(consumer, "cooldownSeconds", 60);
    }

    @Test
    void testConsumeTrade_NoAnomalyWhenDifferenceIsSmall() {
        // Arrange: Price difference is 0.01% (below 0.05% threshold)
        Trade binanceTrade = createTrade("BINANCE", "10000.00", Instant.now());
        Trade coinbaseTrade = createTrade("COINBASE", "10001.00", Instant.now());

        // Act
        consumer.consumeTrade(binanceTrade);
        consumer.consumeTrade(coinbaseTrade);

        // Assert
        verify(kafkaTemplate, never()).send(anyString(), anyString(), any());
    }

    @Test
    void testConsumeTrade_EmitsAnomalyWhenDifferenceIsLarge() {
        // Arrange: Price difference is 1% (above 0.05% threshold)
        Trade binanceTrade = createTrade("BINANCE", "10000.00", Instant.now());
        Trade coinbaseTrade = createTrade("COINBASE", "10100.00", Instant.now());

        // Act
        consumer.consumeTrade(binanceTrade);
        consumer.consumeTrade(coinbaseTrade);

        // Assert
        verify(kafkaTemplate, times(1)).send(eq("anomalies.detected"), eq("BTC-USD"), eventCaptor.capture());
        
        AnomalyEvent emittedEvent = eventCaptor.getValue();
        assertNotNull(emittedEvent);
        assertEquals("BTC-USD", emittedEvent.getSymbol());
        assertEquals(new BigDecimal("1.00000000"), emittedEvent.getDivergencePercentage());
    }

    @Test
    void testConsumeTrade_CooldownPreventsEventStorm() {
        // Arrange: Large difference
        Trade binanceTrade = createTrade("BINANCE", "10000.00", Instant.now());
        Trade coinbaseTrade = createTrade("COINBASE", "10100.00", Instant.now());
        
        // Followed immediately by another large difference
        Trade binanceTrade2 = createTrade("BINANCE", "10000.00", Instant.now());
        Trade coinbaseTrade2 = createTrade("COINBASE", "10100.00", Instant.now());

        // Act
        consumer.consumeTrade(binanceTrade);
        consumer.consumeTrade(coinbaseTrade);
        consumer.consumeTrade(binanceTrade2);
        consumer.consumeTrade(coinbaseTrade2);

        // Assert: Kafka send should only be called ONCE because of the 60 second cooldown
        verify(kafkaTemplate, times(1)).send(anyString(), anyString(), any());
    }

    private Trade createTrade(String exchange, String price, Instant timestamp) {
        return Trade.builder()
                .symbol("BTC-USD")
                .exchange(exchange)
                .price(new BigDecimal(price))
                .quantity(new BigDecimal("1.0"))
                .tradeTimestamp(timestamp)
                .build();
    }
}
