-- Server projection of Android's locally calculated daily RHI aggregate.
-- Raw wearable samples remain in Room/TimescaleDB and are never copied here.
CREATE TABLE IF NOT EXISTS rehealth_rhi_daily_snapshot (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    scored_on DATE NOT NULL,
    raw_score DECIMAL(8,4) NOT NULL,
    display_score DECIMAL(8,4) NOT NULL,
    data_confidence DECIMAL(8,6) NOT NULL,
    status VARCHAR(32) NOT NULL,
    product_tier VARCHAR(32) NOT NULL,
    available_days INT NOT NULL,
    available_feature_count INT NOT NULL,
    smoothing_alpha DECIMAL(8,6) NOT NULL,
    algorithm_version VARCHAR(128) NOT NULL,
    calculation_source VARCHAR(64) NOT NULL,
    domains_json LONGTEXT NOT NULL,
    features_json LONGTEXT NOT NULL,
    quality_json LONGTEXT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_rhi_daily_user_date (user_id, scored_on),
    KEY idx_rhi_daily_user_updated (user_id, scored_on, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO rehealth_schema_migration(version)
VALUES ('software-V20260814.3');
