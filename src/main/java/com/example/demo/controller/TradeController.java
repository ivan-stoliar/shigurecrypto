package com.example.demo.controller;

import com.example.demo.dto.TradeResponseDto;
import com.example.demo.service.TradeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
@Tag(name = "Trades API", description = "Endpoints for retrieving Binance trades")
public class TradeController {

    private final TradeService tradeService;

    @GetMapping
    @Operation(summary = "Get historical trades", description = "Retrieve paginated historical trades for a specific symbol.")
    public ResponseEntity<Page<TradeResponseDto>> getTrades(
            @RequestParam String symbol,
            @RequestParam Instant from,
            @RequestParam Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        
        PageRequest pageRequest = PageRequest.of(page, size);
        Page<TradeResponseDto> trades = tradeService.getTrades(symbol, from, to, pageRequest);
        return ResponseEntity.ok(trades);
    }

    @GetMapping("/latest")
    @Operation(summary = "Get latest trade", description = "Retrieve the single latest trade for a specific symbol.")
    public ResponseEntity<TradeResponseDto> getLatestTrade(
            @Parameter(description = "Trading symbol (e.g., BTCUSDT)", required = true)
            @RequestParam String symbol) {
        
        return tradeService.getLatestTrade(symbol)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
