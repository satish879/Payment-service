-- Success rate window table for time-window based metrics
CREATE TABLE IF NOT EXISTS success_rate_window (
    id VARCHAR(64) PRIMARY KEY,
    profile_id VARCHAR(64) NOT NULL,
    connector VARCHAR(64) NOT NULL,
    payment_method VARCHAR(64),
    currency VARCHAR(3),
    window_start TIMESTAMP NOT NULL,
    window_end TIMESTAMP NOT NULL,
    total_attempts BIGINT DEFAULT 0,
    successful_attempts BIGINT DEFAULT 0,
    failed_attempts BIGINT DEFAULT 0,
    success_rate DECIMAL(5,2) DEFAULT 0.00,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes separately (PostgreSQL doesn't support inline INDEX definitions)
CREATE INDEX IF NOT EXISTS idx_profile_connector ON success_rate_window(profile_id, connector);
CREATE INDEX IF NOT EXISTS idx_window_time ON success_rate_window(window_start, window_end);

