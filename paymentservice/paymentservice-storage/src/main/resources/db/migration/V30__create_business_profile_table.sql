-- V30: Create business_profile table
-- Note: Renamed from V28 to V30 to avoid version conflict with V28__create_users_table.sql
CREATE TABLE IF NOT EXISTS business_profile (
    profile_id VARCHAR(64) PRIMARY KEY,
    merchant_id VARCHAR(64) NOT NULL,
    profile_name VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    return_url TEXT,
    enable_payment_response_hash BOOLEAN NOT NULL DEFAULT TRUE,
    payment_response_hash_key VARCHAR(255),
    redirect_to_merchant_with_http_post BOOLEAN NOT NULL DEFAULT FALSE,
    webhook_details JSONB,
    metadata JSONB,
    routing_algorithm JSONB,
    intent_fulfillment_time BIGINT,
    frm_routing_algorithm JSONB,
    payout_routing_algorithm JSONB,
    is_recon_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    applepay_verified_domains TEXT[],
    payment_link_config JSONB,
    session_expiry BIGINT,
    authentication_connector_details JSONB,
    payout_link_config JSONB,
    is_extended_card_info_enabled BOOLEAN,
    extended_card_info_config JSONB,
    is_connector_agnostic_mit_enabled BOOLEAN,
    use_billing_as_payment_method_billing BOOLEAN,
    collect_shipping_details_from_wallet_connector BOOLEAN,
    collect_billing_details_from_wallet_connector BOOLEAN,
    outgoing_webhook_custom_http_headers BYTEA,
    always_collect_billing_details_from_wallet_connector BOOLEAN,
    always_collect_shipping_details_from_wallet_connector BOOLEAN,
    tax_connector_id VARCHAR(64),
    is_tax_connector_enabled BOOLEAN,
    version VARCHAR(16) DEFAULT 'v1',
    dynamic_routing_algorithm JSONB,
    routing_algorithm_id VARCHAR(64),
    order_fulfillment_time BIGINT,
    order_fulfillment_time_origin VARCHAR(32),
    frm_routing_algorithm_id VARCHAR(64),
    payout_routing_algorithm_id VARCHAR(64),
    default_fallback_routing JSONB,
    should_collect_cvv_during_payment BOOLEAN
    -- CONSTRAINT fk_business_profile_merchant FOREIGN KEY (merchant_id) REFERENCES merchant_account(merchant_id) ON DELETE CASCADE
);

CREATE INDEX idx_business_profile_merchant_id ON business_profile(merchant_id);
CREATE INDEX idx_business_profile_profile_name ON business_profile(profile_name);

