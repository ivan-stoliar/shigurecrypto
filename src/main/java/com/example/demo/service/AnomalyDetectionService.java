package com.example.demo.service;

import com.example.demo.entity.Anomaly;
import com.example.demo.entity.Trade;
import com.example.demo.repository.AnomalyRepository;
import com.example.demo.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnomalyDetectionService {

    private final TradeRepository tradeRepository;
    private final AnomalyRepository anomalyRepository;

    @Value("${anomaly.threshold.percentage:0.05}")
    private double thresholdPercentage;

    @Value("${anomaly.window.minutes:1}")
    private int windowMinutes;

    // Scheduled polling removed in favor of real-time Kafka stream processing (AnomalyDetectionConsumer)
    @Transactional
    public void detectAnomalies() {
        Instant startTime = Instant.now().minus(windowMinutes, ChronoUnit.MINUTES);
        List<Trade> recentTrades = tradeRepository.findByTradeTimestampAfter(startTime);

        if (recentTrades.isEmpty()) {
            return;
        }

        // Group by symbol, then by exchange
        Map<String, Map<String, List<Trade>>> tradesBySymbolAndExchange = recentTrades.stream()
                .collect(Collectors.groupingBy(Trade::getSymbol,
                        Collectors.groupingBy(Trade::getExchange)));

        for (Map.Entry<String, Map<String, List<Trade>>> entry : tradesBySymbolAndExchange.entrySet()) {
            String symbol = entry.getKey();
            Map<String, List<Trade>> exchangeTrades = entry.getValue();

            if (exchangeTrades.containsKey("BINANCE") && exchangeTrades.containsKey("COINBASE")) {
                BigDecimal avgBinance = calculateAveragePrice(exchangeTrades.get("BINANCE"));
                BigDecimal avgCoinbase = calculateAveragePrice(exchangeTrades.get("COINBASE"));

                BigDecimal diff = avgBinance.subtract(avgCoinbase).abs();
                BigDecimal minPrice = avgBinance.min(avgCoinbase);
                
                if (minPrice.compareTo(BigDecimal.ZERO) == 0) continue;

                // divergence = (diff / minPrice) * 100
                BigDecimal divergence = diff.divide(minPrice, 8, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

                if (divergence.compareTo(BigDecimal.valueOf(thresholdPercentage)) > 0) {
                    log.warn("Anomaly detected for {}! Binance Avg: {}, Coinbase Avg: {}, Divergence: {}%",
                            symbol, avgBinance, avgCoinbase, divergence);

                    Anomaly anomaly = Anomaly.builder()
                            .symbol(symbol)
                            .exchangeA("BINANCE")
                            .exchangeB("COINBASE")
                            .avgPriceA(avgBinance)
                            .avgPriceB(avgCoinbase)
                            .divergencePercentage(divergence)
                            .build();
                    
                    anomalyRepository.save(anomaly);
                }
            }
        }
    }

    private BigDecimal calculateAveragePrice(List<Trade> trades) {
        if (trades == null || trades.isEmpty()) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal sum = trades.stream()
                .map(Trade::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        return sum.divide(BigDecimal.valueOf(trades.size()), 8, RoundingMode.HALF_UP);
    }
}
