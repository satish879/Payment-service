-- Create apple_pay_verified_domains table
CREATE TABLE IF NOT EXISTS apple_pay_verified_domains (
    id BIGSERIAL PRIMARY KEY,
    merchant_id VARCHAR(255) NOT NULL,
    profile_id VARCHAR(255),
    merchant_connector_account_id VARCHAR(255) NOT NULL,
    domain_name VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- CONSTRAINT fk_apple_pay_merchant FOREIGN KEY (merchant_id) REFERENCES merchant_account(merchant_id) ON DELETE CASCADE,
    CONSTRAINT fk_apple_pay_mca FOREIGN KEY (merchant_connector_account_id) REFERENCES merchant_connector_account(id) ON DELETE CASCADE,
    UNIQUE(merchant_id, merchant_connector_account_id, domain_name)
);

CREATE INDEX idx_apple_pay_merchant_id ON apple_pay_verified_domains(merchant_id);
CREATE INDEX idx_apple_pay_mca_id ON apple_pay_verified_domains(merchant_connector_account_id);
CREATE INDEX idx_apple_pay_domain ON apple_pay_verified_domains(domain_name);

