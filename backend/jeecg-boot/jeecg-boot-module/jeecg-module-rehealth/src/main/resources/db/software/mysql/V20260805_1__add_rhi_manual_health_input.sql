CREATE TABLE IF NOT EXISTS rehealth_rhi_manual_health_input (
    user_id VARCHAR(64) NOT NULL,
    sedentary_hours_per_day DECIMAL(6,2) NULL,
    waist_circumference_cm DECIMAL(6,2) NULL,
    vo2_max_ml_kg_min DECIMAL(6,2) NULL,
    hba1c_percent DECIMAL(6,2) NULL,
    egfr_ml_min_1_73m2 DECIMAL(7,2) NULL,
    cuff_sbp_7d_mean DECIMAL(6,2) NULL,
    cuff_dbp_7d_mean DECIMAL(6,2) NULL,
    cuff_valid_days INT NULL,
    cuff_confirmed TINYINT(1) NOT NULL DEFAULT 0,
    fasting_glucose_mmol_l DECIMAL(7,3) NULL,
    total_cholesterol_mmol_l DECIMAL(7,3) NULL,
    ldl_mmol_l DECIMAL(7,3) NULL,
    hdl_mmol_l DECIMAL(7,3) NULL,
    triglycerides_mmol_l DECIMAL(7,3) NULL,
    lab_confirmed TINYINT(1) NOT NULL DEFAULT 0,
    lab_recorded_at BIGINT NULL,
    client_updated_at BIGINT NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (user_id),
    KEY idx_rhi_manual_updated_at (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO rehealth_schema_migration(version) VALUES ('software-V20260805.1');
