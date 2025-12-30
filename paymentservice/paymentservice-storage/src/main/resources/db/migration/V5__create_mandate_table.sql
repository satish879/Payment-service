-- Mandate status and type enums
CREATE TYPE mandate_status AS ENUM (
    'ACTIVE',
    'INACTIVE',
    'PENDING',
    'REVOKED'
);

CREATE TYPE mandate_type AS ENUM (
    'SINGLE_USE',
    'MULTI_USE'
);

-- Mandate table
CREATE TABLE IF NOT EXISTS mandate (
    mandate_id VARCHAR(64) PRIMARY KEY,
    customer_id VARCHAR(64) NOT NULL,
    merchant_id VARCHAR(64) NOT NULL,
    payment_method_id VARCHAR(64) NOT NULL,
    mandate_status mandate_status NOT NULL DEFAULT 'PENDING',
    mandate_type mandate_type NOT NULL,
    customer_accepted_at TIMESTAMP,
    customer_ip_address VARCHAR(64),
    customer_user_agent VARCHAR(255),
    network_transaction_id VARCHAR(128),
    previous_attempt_id VARCHAR(64),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    mandate_amount BIGINT,
    mandate_currency VARCHAR(3),
    amount_captured BIGINT,
    connector VARCHAR(64) NOT NULL,
    connector_mandate_id VARCHAR(128),
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    metadata JSONB,
    connector_mandate_ids JSONB,
    original_payment_id VARCHAR(64),
    merchant_connector_id VARCHAR(32),
    updated_by VARCHAR(64),
    customer_user_agent_extended VARCHAR(2048)
);

-- Create indexes
CREATE INDEX IF NOT EXISTS idx_mandate_customer_id ON mandate(customer_id);
CREATE INDEX IF NOT EXISTS idx_mandate_merchant_id ON mandate(merchant_id);
CREATE INDEX IF NOT EXISTS idx_mandate_payment_method_id ON mandate(payment_method_id);
CREATE INDEX IF NOT EXISTS idx_mandate_status ON mandate(mandate_status);
CREATE INDEX IF NOT EXISTS idx_mandate_original_payment_id ON mandate(original_payment_id);
CREATE INDEX IF NOT EXISTS idx_mandate_created_at ON mandate(created_at);
CREATE INDEX IF NOT EXISTS idx_mandate_customer_status ON mandate(customer_id, mandate_status);

-- Foreign key constraints (if tables exist)
-- ALTER TABLE mandate ADD CONSTRAINT fk_mandate_customer FOREIGN KEY (customer_id) REFERENCES customer(customer_id);
-- ALTER TABLE mandate ADD CONSTRAINT fk_mandate_payment_method FOREIGN KEY (payment_method_id) REFERENCES payment_method(payment_method_id);

