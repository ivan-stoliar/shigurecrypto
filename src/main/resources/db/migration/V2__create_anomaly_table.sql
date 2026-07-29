CREATE TABLE anomaly (
    id BIGSERIAL PRIMARY KEY,
    symbol VARCHAR(20) NOT NULL,
    exchange_a VARCHAR(20) NOT NULL,
    exchange_b VARCHAR(20) NOT NULL,
    avg_price_a NUMERIC(20, 8) NOT NULL,
    avg_price_b NUMERIC(20, 8) NOT NULL,
    divergence_percentage NUMERIC(10, 4) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
