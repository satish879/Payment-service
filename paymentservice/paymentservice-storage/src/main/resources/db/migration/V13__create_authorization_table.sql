-- Create authorization_status enum
CREATE TYPE authorization_status AS ENUM (
    'PROCESSING',
    'SUCCESS',
    'FAILED'
);

-- Create authorization table
-- Note: "authorization" is a reserved keyword in PostgreSQL, so we quote it
CREATE TABLE "authorization" (
    id VARCHAR(255) PRIMARY KEY,
    authorization_id VARCHAR(255) NOT NULL UNIQUE,
    merchant_id VARCHAR(255) NOT NULL,
    payment_id VARCHAR(255) NOT NULL,
    amount BIGINT NOT NULL,
    status authorization_status NOT NULL,
    error_code VARCHAR(255),
    error_message TEXT,
    connector_authorization_id VARCHAR(255),
    previously_authorized_amount BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes
CREATE INDEX idx_authorization_merchant_id ON "authorization"(merchant_id);
CREATE INDEX idx_authorization_payment_id ON "authorization"(payment_id);
CREATE INDEX idx_authorization_authorization_id ON "authorization"(authorization_id);
CREATE INDEX idx_authorization_status ON "authorization"(status);

