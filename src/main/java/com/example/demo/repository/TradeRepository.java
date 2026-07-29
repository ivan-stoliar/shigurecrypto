package com.example.demo.repository;

import com.example.demo.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Optional;
import java.util.List;

@Repository
public interface TradeRepository extends JpaRepository<Trade, Long> {

    Page<Trade> findBySymbolAndTradeTimestampBetween(String symbol, Instant from, Instant to, Pageable pageable);

    Optional<Trade> findFirstBySymbolOrderByTradeTimestampDesc(String symbol);

    List<Trade> findByTradeTimestampAfter(Instant timestamp);
}
