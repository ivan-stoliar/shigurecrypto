package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class BinanceTradeDto {

    @JsonProperty("s")
    private String symbol;

    @JsonProperty("p")
    private BigDecimal price;

    @JsonProperty("q")
    private BigDecimal quantity;

    @JsonProperty("T")
    private Long timestamp;
}
