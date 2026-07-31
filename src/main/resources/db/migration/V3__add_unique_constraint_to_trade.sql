ALTER TABLE trade ADD CONSTRAINT uk_trade_symbol_exchange_timestamp UNIQUE (symbol, exchange, trade_timestamp);
