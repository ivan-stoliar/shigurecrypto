package com.example.demo.normalizer;

import com.example.demo.dto.BinanceTradeDto;
import com.example.demo.entity.Trade;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
public class BinanceNormalizer implements TradeNormalizer {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Trade normalize(String message) throws Exception {
        BinanceTradeDto dto = objectMapper.readValue(message, BinanceTradeDto.class);
        if (dto == null || dto.getSymbol() == null) {
            return null;
        }

        String canonicalSymbol = "btcusdt".equalsIgnoreCase(dto.getSymbol()) ? "BTC-USD" : dto.getSymbol().toUpperCase();

        return Trade.builder()
                .symbol(canonicalSymbol)
                .exchange("BINANCE")
                .price(dto.getPrice())
                .quantity(dto.getQuantity())
                .tradeTimestamp(Instant.ofEpochMilli(dto.getTimestamp()))
                .build();
    }
}
