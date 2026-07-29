package com.example.demo.normalizer;

import com.example.demo.entity.Trade;

public interface TradeNormalizer {
    Trade normalize(String message) throws Exception;
}
