-- Create api_keys table
CREATE TABLE IF NOT EXISTS api_keys (
    key_id VARCHAR(64) PRIMARY KEY,
    merchant_id VARCHAR(64) NOT NULL,
    name VARCHAR(64) NOT NULL,
    description VARCHAR(256),
    hash_key VARCHAR(64) NOT NULL,
    hashed_api_key VARCHAR(128) NOT NULL,
    prefix VARCHAR(16) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    last_used TIMESTAMP
    -- CONSTRAINT fk_api_keys_merchant FOREIGN KEY (merchant_id) REFERENCES merchant_account(merchant_id) ON DELETE CASCADE
);

CREATE INDEX idx_api_keys_merchant_id ON api_keys(merchant_id);
CREATE INDEX idx_api_keys_prefix ON api_keys(prefix);
CREATE INDEX idx_api_keys_created_at ON api_keys(created_at);

