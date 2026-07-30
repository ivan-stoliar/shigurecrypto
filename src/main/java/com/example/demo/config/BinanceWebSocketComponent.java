package com.example.demo.config;

import com.example.demo.entity.Trade;
import com.example.demo.normalizer.BinanceNormalizer;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.kafka.core.KafkaTemplate;
import lombok.RequiredArgsConstructor;

@Slf4j
@Component
@RequiredArgsConstructor
public class BinanceWebSocketComponent implements WebSocket.Listener {

    private static final String BINANCE_WS_URL = "wss://stream.binance.com:9443/ws/btcusdt@trade";
    private static final long BASE_DELAY_MS = 1000;
    private static final long MAX_DELAY_MS = 60000;
    private static final double MULTIPLIER = 2.0;

    private final KafkaTemplate kafkaTemplate;
    private final BinanceNormalizer binanceNormalizer;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private WebSocket webSocket;
    private long currentDelayMs = BASE_DELAY_MS;
    private boolean isShuttingDown = false;
    private final StringBuilder messageBuffer = new StringBuilder();

    @PostConstruct
    public void start() {
        connect();
    }

    private void connect() {
        if (isShuttingDown) return;
        
        log.info("Attempting to connect to Binance WebSocket...");
        httpClient.newWebSocketBuilder()
                .buildAsync(URI.create(BINANCE_WS_URL), this)
                .thenAccept(ws -> {
                    log.info("Successfully connected to Binance WebSocket.");
                    this.webSocket = ws;
                    this.currentDelayMs = BASE_DELAY_MS; // Reset delay on successful connection
                })
                .exceptionally(ex -> {
                    log.error("Failed to connect to Binance WebSocket: {}", ex.getMessage());
                    scheduleReconnect();
                    return null;
                });
    }

    private void scheduleReconnect() {
        if (isShuttingDown) return;
        
        log.info("Scheduling reconnect in {} ms", currentDelayMs);
        scheduler.schedule(this::connect, currentDelayMs, TimeUnit.MILLISECONDS);
        
        // Calculate next delay
        currentDelayMs = (long) Math.min(MAX_DELAY_MS, currentDelayMs * MULTIPLIER);
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        WebSocket.Listener.super.onOpen(webSocket); // Automatically requests the first message
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        messageBuffer.append(data);
        if (last) {
            String message = messageBuffer.toString();
            messageBuffer.setLength(0); // Clear buffer for next message
            try {
                Trade trade = binanceNormalizer.normalize(message);
                if (trade != null) {
                    log.info("New Trade -> Symbol: {}, Exchange: {}, Price: {}, Quantity: {}, Timestamp: {}",
                            trade.getSymbol(), trade.getExchange(), trade.getPrice(), trade.getQuantity(), trade.getTradeTimestamp());
                    
                    // Publish trade data to Kafka
                    kafkaTemplate.send("trades.raw", trade.getSymbol(), trade);
                }
            } catch (Exception e) {
                log.error("Failed to parse Binance trade JSON: {}", e.getMessage());
            }
        }
        
        webSocket.request(1); // Request next message
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        log.warn("WebSocket Connection Closed. Status: {}, Reason: {}", statusCode, reason);
        this.webSocket = null;
        scheduleReconnect();
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        log.error("WebSocket Error: {}", error.getMessage());
        WebSocket.Listener.super.onError(webSocket, error);
    }

    @PreDestroy
    public void stop() {
        isShuttingDown = true;
        if (webSocket != null) {
            webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Application shutting down");
        }
        scheduler.shutdownNow();
    }
}
