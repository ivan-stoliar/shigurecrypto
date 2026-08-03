package com.example.demo.normalizer;

import com.example.demo.entity.Trade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class BinanceNormalizerTest {

    private BinanceNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new BinanceNormalizer();
    }

    @Test
    void testNormalizeValidMessage() throws Exception {
        // Arrange
        String json = "{\"e\":\"trade\",\"E\":1672531200000,\"s\":\"BTCUSDT\",\"t\":12345,\"p\":\"0.001\",\"q\":\"100\",\"b\":88,\"a\":50,\"T\":1672531200000,\"m\":true,\"M\":true}";

        // Act
        Trade trade = normalizer.normalize(json);

        // Assert
        assertNotNull(trade);
        assertEquals("BTC-USD", trade.getSymbol());
        assertEquals("BINANCE", trade.getExchange());
        assertEquals(new BigDecimal("0.001"), trade.getPrice());
        assertEquals(new BigDecimal("100"), trade.getQuantity());
        assertEquals(Instant.ofEpochMilli(1672531200000L), trade.getTradeTimestamp());
    }

    @Test
    void testNormalizeInvalidJsonThrowsException() {
        // Arrange
        String invalidJson = "{ invalid_json }";

        // Act & Assert
        assertThrows(Exception.class, () -> normalizer.normalize(invalidJson));
    }
}
