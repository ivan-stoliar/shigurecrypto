package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CoinbaseTradeDto {
    private String type;
    
    @JsonProperty("product_id")
    private String productId;
    
    private BigDecimal price;
    
    private BigDecimal size;
    
    private String time;
}
