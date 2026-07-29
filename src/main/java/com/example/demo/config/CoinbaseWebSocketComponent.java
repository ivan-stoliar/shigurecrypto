package com.example.demo.config;

import com.example.demo.entity.Trade;
import com.example.demo.normalizer.CoinbaseNormalizer;
import com.example.demo.service.TradeService;
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

@Slf4j
@Component
@RequiredArgsConstructor
public class CoinbaseWebSocketComponent implements WebSocket.Listener {

    private static final String COINBASE_WS_URL = "wss://ws-feed.exchange.coinbase.com";
    private static final String SUBSCRIBE_MESSAGE = "{\"type\": \"subscribe\", \"product_ids\": [\"BTC-USD\"], \"channels\": [\"matches\"]}";
    private static final long BASE_DELAY_MS = 1000;
    private static final long MAX_DELAY_MS = 60000;
    private static final double MULTIPLIER = 2.0;

    private final TradeService tradeService;
    private final CoinbaseNormalizer coinbaseNormalizer;
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
        
        log.info("Attempting to connect to Coinbase WebSocket...");
        httpClient.newWebSocketBuilder()
                .buildAsync(URI.create(COINBASE_WS_URL), this)
                .thenAccept(ws -> {
                    log.info("Successfully connected to Coinbase WebSocket.");
                    this.webSocket = ws;
                    this.currentDelayMs = BASE_DELAY_MS;
                    
                    // Send subscription message
                    ws.sendText(SUBSCRIBE_MESSAGE, true);
                    log.info("Sent subscription message to Coinbase.");
                })
                .exceptionally(ex -> {
                    log.error("Failed to connect to Coinbase WebSocket: {}", ex.getMessage());
                    scheduleReconnect();
                    return null;
                });
    }

    private void scheduleReconnect() {
        if (isShuttingDown) return;
        
        log.info("Scheduling reconnect to Coinbase in {} ms", currentDelayMs);
        scheduler.schedule(this::connect, currentDelayMs, TimeUnit.MILLISECONDS);
        
        currentDelayMs = (long) Math.min(MAX_DELAY_MS, currentDelayMs * MULTIPLIER);
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        WebSocket.Listener.super.onOpen(webSocket);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        messageBuffer.append(data);
        if (last) {
            String message = messageBuffer.toString();
            messageBuffer.setLength(0);
            try {
                Trade trade = coinbaseNormalizer.normalize(message);
                if (trade != null) {
                    log.info("New Trade -> Symbol: {}, Exchange: {}, Price: {}, Quantity: {}, Timestamp: {}",
                            trade.getSymbol(), trade.getExchange(), trade.getPrice(), trade.getQuantity(), trade.getTradeTimestamp());
                    tradeService.saveTrade(trade);
                }
            } catch (Exception e) {
                log.error("Failed to parse Coinbase trade JSON: {}", e.getMessage());
                log.debug("Payload was: {}", message);
            }
        }
        
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        log.warn("Coinbase WebSocket Connection Closed. Status: {}, Reason: {}", statusCode, reason);
        this.webSocket = null;
        scheduleReconnect();
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        log.error("Coinbase WebSocket Error: {}", error.getMessage());
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
