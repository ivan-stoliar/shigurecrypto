package com.example.demo.dto;

import com.example.demo.entity.Trade;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class TradeResponseDto {
    private Long id;
    private String symbol;
    private String exchange;
    private BigDecimal price;
    private BigDecimal quantity;
    private Instant tradeTimestamp;

    public static TradeResponseDto fromEntity(Trade trade) {
        return TradeResponseDto.builder()
                .id(trade.getId())
                .symbol(trade.getSymbol())
                .exchange(trade.getExchange())
                .price(trade.getPrice())
                .quantity(trade.getQuantity())
                .tradeTimestamp(trade.getTradeTimestamp())
                .build();
    }
}
