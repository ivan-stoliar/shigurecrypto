CREATE TABLE trade (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    exchange VARCHAR(20) NOT NULL,
    price NUMERIC(20, 8) NOT NULL,
    quantity NUMERIC(20, 8) NOT NULL,
    trade_timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_trade_symbol_timestamp ON trade (symbol, trade_timestamp DESC);
