CREATE TABLE IF NOT EXISTS rehealth_behavior_record (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    request_id VARCHAR(128) NOT NULL,
    category VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    summary VARCHAR(2000),
    items_json LONGTEXT,
    calories_kcal DECIMAL(10,2),
    protein_grams DECIMAL(10,2),
    carbohydrate_grams DECIMAL(10,2),
    fat_grams DECIMAL(10,2),
    ocr_text LONGTEXT,
    confidence DOUBLE,
    model_version VARCHAR(128) NOT NULL,
    occurred_at DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_rehealth_behavior_owner_request (tenant_id, user_id, request_id),
    KEY idx_rehealth_behavior_owner_occurred (tenant_id, user_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO rehealth_schema_migration(version) VALUES ('software-V20260731.1');
