package com.example.demo.kafka;

import com.example.demo.entity.Trade;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnomalyDetectionConsumer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${anomaly.threshold.percentage:0.05}")
    private double thresholdPercentage;

    @Value("${anomaly.window.minutes:1}")
    private int windowMinutes;

    @Value("${anomaly.cooldown.seconds:60}")
    private int cooldownSeconds;

    // State: Symbol -> State (Holds trades for Binance/Coinbase and cooldown tracking)
    private final ConcurrentHashMap<String, SymbolWindowState> stateMap = new ConcurrentHashMap<>();

    @Data
    private static class SymbolWindowState {
        private final Deque<Trade> binanceTrades = new ConcurrentLinkedDeque<>();
        private final Deque<Trade> coinbaseTrades = new ConcurrentLinkedDeque<>();
        private Instant lastAnomalyEmittedAt = Instant.EPOCH;
    }

    @KafkaListener(topics = "trades.raw", groupId = "anomaly-group")
    public void consumeTrade(Trade trade) {
        String symbol = trade.getSymbol();
        SymbolWindowState state = stateMap.computeIfAbsent(symbol, k -> new SymbolWindowState());

        // 1. Add trade to the correct window
        Instant now = Instant.now();
        if ("BINANCE".equalsIgnoreCase(trade.getExchange())) {
            state.getBinanceTrades().addLast(trade);
        } else if ("COINBASE".equalsIgnoreCase(trade.getExchange())) {
            state.getCoinbaseTrades().addLast(trade);
        } else {
            return; 
        }

        // 2. Evict old trades
        Instant cutoff = now.minus(windowMinutes, ChronoUnit.MINUTES);
        evictOldTrades(state.getBinanceTrades(), cutoff);
        evictOldTrades(state.getCoinbaseTrades(), cutoff);

        // 3. Calculate moving averages
        if (state.getBinanceTrades().isEmpty() || state.getCoinbaseTrades().isEmpty()) {
            return; // Need data from both exchanges to compare
        }

        BigDecimal avgBinance = calculateAveragePrice(state.getBinanceTrades());
        BigDecimal avgCoinbase = calculateAveragePrice(state.getCoinbaseTrades());

        // 4. Calculate divergence
        BigDecimal diff = avgBinance.subtract(avgCoinbase).abs();
        BigDecimal minPrice = avgBinance.min(avgCoinbase);

        if (minPrice.compareTo(BigDecimal.ZERO) == 0) return;

        BigDecimal divergence = diff.divide(minPrice, 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

        // 5. Check threshold and cooldown
        if (divergence.compareTo(BigDecimal.valueOf(thresholdPercentage)) > 0) {
            if (now.isAfter(state.getLastAnomalyEmittedAt().plusSeconds(cooldownSeconds))) {
                log.warn("Real-time Anomaly detected for {}! Binance Avg: {}, Coinbase Avg: {}, Divergence: {}%",
                        symbol, avgBinance, avgCoinbase, divergence);

                AnomalyEvent event = AnomalyEvent.builder()
                        .symbol(symbol)
                        .exchangeA("BINANCE")
                        .exchangeB("COINBASE")
                        .avgPriceA(avgBinance)
                        .avgPriceB(avgCoinbase)
                        .divergencePercentage(divergence)
                        .timestamp(now)
                        .build();

                // Publish to Kafka 
                kafkaTemplate.send("anomalies.detected", symbol, event);
                
                // Update cooldown
                state.setLastAnomalyEmittedAt(now);
            }
        }
    }

    private void evictOldTrades(Deque<Trade> trades, Instant cutoff) {
        while (!trades.isEmpty() && trades.peekFirst().getTradeTimestamp().isBefore(cutoff)) {
            trades.pollFirst();
        }
    }

    private BigDecimal calculateAveragePrice(Deque<Trade> trades) {
        if (trades.isEmpty()) return BigDecimal.ZERO;
        
        BigDecimal sum = BigDecimal.ZERO;
        for (Trade t : trades) {
            sum = sum.add(t.getPrice());
        }
        return sum.divide(BigDecimal.valueOf(trades.size()), 8, RoundingMode.HALF_UP);
    }
}
