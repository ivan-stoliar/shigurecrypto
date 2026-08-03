package com.example.demo.service;

import com.example.demo.entity.Trade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.support.Acknowledgment;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TradeConsumerTest {

    @Mock
    private TradeService tradeService;

    @Mock
    private Acknowledgment acknowledgment;

    @InjectMocks
    private TradeConsumer tradeConsumer;

    @Test
    void testConsumeTrade_Success() {
        // Arrange
        Trade trade = Trade.builder()
                .symbol("BTC-USD")
                .exchange("BINANCE")
                .price(new BigDecimal("60000"))
                .tradeTimestamp(Instant.now())
                .build();

        // Act
        tradeConsumer.consumeTrade(trade, acknowledgment);

        // Assert
        verify(tradeService, times(1)).saveTrade(trade);
        verify(acknowledgment, times(1)).acknowledge();
    }

    @Test
    void testConsumeTrade_Idempotency_IgnoresDuplicateKeyException() {
        // Arrange
        Trade trade = Trade.builder()
                .symbol("BTC-USD")
                .exchange("BINANCE")
                .price(new BigDecimal("60000"))
                .tradeTimestamp(Instant.now())
                .build();
                
        // Mock the tradeService to throw a Duplicate Key exception (DataIntegrityViolationException)
        doThrow(new DataIntegrityViolationException("Unique index violation"))
                .when(tradeService).saveTrade(any(Trade.class));

        // Act
        tradeConsumer.consumeTrade(trade, acknowledgment);

        // Assert
        // The exception should be caught, and the message should still be acknowledged!
        verify(tradeService, times(1)).saveTrade(trade);
        verify(acknowledgment, times(1)).acknowledge();
    }
}
