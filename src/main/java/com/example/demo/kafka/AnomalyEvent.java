package com.example.demo.kafka;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnomalyEvent {
    private String symbol;
    private String exchangeA;
    private String exchangeB;
    private BigDecimal avgPriceA;
    private BigDecimal avgPriceB;
    private BigDecimal divergencePercentage;
    private Instant timestamp;
}
