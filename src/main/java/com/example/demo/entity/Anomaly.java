package com.example.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "anomaly")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Anomaly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String symbol;

    @Column(name = "exchange_a", nullable = false, length = 20)
    private String exchangeA;

    @Column(name = "exchange_b", nullable = false, length = 20)
    private String exchangeB;

    @Column(name = "avg_price_a", nullable = false, precision = 20, scale = 8)
    private BigDecimal avgPriceA;

    @Column(name = "avg_price_b", nullable = false, precision = 20, scale = 8)
    private BigDecimal avgPriceB;

    @Column(name = "divergence_percentage", nullable = false, precision = 10, scale = 4)
    private BigDecimal divergencePercentage;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;
}
