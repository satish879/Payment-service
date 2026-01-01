-- Create tokenization table for v2 API
-- Note: Foreign key constraints commented out as referenced tables may not exist yet
CREATE TABLE IF NOT EXISTS tokenization (
    id VARCHAR(255) PRIMARY KEY,
    merchant_id VARCHAR(255) NOT NULL,
    customer_id VARCHAR(255) NOT NULL,
    locker_id VARCHAR(255) NOT NULL,
    flag VARCHAR(50) NOT NULL DEFAULT 'enabled',
    version VARCHAR(50) NOT NULL DEFAULT 'v2',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
    -- CONSTRAINT fk_tokenization_merchant FOREIGN KEY (merchant_id) REFERENCES merchant_account(merchant_id) ON DELETE CASCADE,
    -- CONSTRAINT fk_tokenization_customer FOREIGN KEY (customer_id) REFERENCES customer(customer_id) ON DELETE CASCADE
);

CREATE INDEX idx_tokenization_merchant_id ON tokenization(merchant_id);
CREATE INDEX idx_tokenization_customer_id ON tokenization(customer_id);
CREATE INDEX idx_tokenization_locker_id ON tokenization(locker_id);

