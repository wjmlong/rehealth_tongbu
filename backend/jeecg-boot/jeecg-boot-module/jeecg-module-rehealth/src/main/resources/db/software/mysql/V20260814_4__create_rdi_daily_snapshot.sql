-- Server projection of Android's locally calculated RDI daily aggregate.
-- Raw wearable samples and localized evidence text are intentionally excluded.
CREATE TABLE IF NOT EXISTS rehealth_rdi_daily_snapshot (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    scored_on DATE NOT NULL,
    raw_score DECIMAL(8,4) NOT NULL,
    display_score DECIMAL(8,4) NOT NULL,
    data_confidence DECIMAL(8,6) NOT NULL,
    status VARCHAR(32) NOT NULL,
    is_mock TINYINT(1) NOT NULL DEFAULT 0,
    algorithm_version VARCHAR(128) NOT NULL,
    calculation_source VARCHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_rdi_daily_user_date (user_id, scored_on),
    KEY idx_rdi_daily_user_updated (user_id, scored_on, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rehealth_rdi_contribution (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    snapshot_id VARCHAR(64) NOT NULL,
    factor_code VARCHAR(64) NOT NULL,
    domain_code VARCHAR(64) NOT NULL,
    source_code VARCHAR(64) NOT NULL,
    current_value DECIMAL(16,6) NOT NULL,
    baseline_value DECIMAL(16,6) NULL,
    unit VARCHAR(32) NOT NULL,
    raw_points DECIMAL(10,6) NOT NULL,
    confidence DECIMAL(8,6) NOT NULL,
    final_points DECIMAL(10,6) NOT NULL,
    source_factor_id VARCHAR(255) NOT NULL,
    algorithm_version VARCHAR(128) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_rdi_contribution_snapshot_factor (snapshot_id, factor_code),
    KEY idx_rdi_contribution_snapshot_points (snapshot_id, final_points),
    CONSTRAINT fk_rdi_contribution_snapshot
      FOREIGN KEY (snapshot_id) REFERENCES rehealth_rdi_daily_snapshot(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO rehealth_schema_migration(version)
VALUES ('software-V20260814.4');
