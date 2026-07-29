package com.example.demo.service;

import com.example.demo.entity.Trade;
import com.example.demo.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class TradeService {

    private final TradeRepository tradeRepository;

    @Transactional
    public void saveTrade(Trade trade) {
        if (trade == null) {
            return;
        }

        tradeRepository.save(trade);
        log.debug("Saved trade to database: {}", trade);
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<com.example.demo.dto.TradeResponseDto> getTrades(String symbol, Instant from, Instant to, org.springframework.data.domain.Pageable pageable) {
        return tradeRepository.findBySymbolAndTradeTimestampBetween(symbol, from, to, pageable)
                .map(com.example.demo.dto.TradeResponseDto::fromEntity);
    }

    @Transactional(readOnly = true)
    public java.util.Optional<com.example.demo.dto.TradeResponseDto> getLatestTrade(String symbol) {
        return tradeRepository.findFirstBySymbolOrderByTradeTimestampDesc(symbol)
                .map(com.example.demo.dto.TradeResponseDto::fromEntity);
    }
}
