package com.example.demo.normalizer;

import com.example.demo.dto.CoinbaseTradeDto;
import com.example.demo.entity.Trade;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
public class CoinbaseNormalizer implements TradeNormalizer {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Trade normalize(String message) throws Exception {
        CoinbaseTradeDto dto = objectMapper.readValue(message, CoinbaseTradeDto.class);
        
        // Skip messages that aren't "match" (e.g., subscriptions)
        if (!"match".equals(dto.getType()) || dto.getProductId() == null) {
            return null;
        }

        return Trade.builder()
                .symbol(dto.getProductId())
                .exchange("COINBASE")
                .price(dto.getPrice())
                .quantity(dto.getSize())
                .tradeTimestamp(Instant.parse(dto.getTime()))
                .build();
    }
}
