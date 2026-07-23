CREATE TABLE IF NOT EXISTS rehealth_telemetry_event_projection (
    event_id VARCHAR(128) NOT NULL PRIMARY KEY,
    event_type VARCHAR(128) NOT NULL,
    schema_id VARCHAR(128) NOT NULL,
    batch_id VARCHAR(128) NOT NULL,
    tenant_ref VARCHAR(160) NOT NULL,
    user_ref VARCHAR(160) NOT NULL,
    device_ref VARCHAR(160) NOT NULL,
    record_count INT NOT NULL,
    persistence_status VARCHAR(32) NOT NULL,
    quality_status VARCHAR(64),
    occurred_at DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    KEY idx_rehealth_telemetry_projection_tenant_time (tenant_ref, occurred_at),
    KEY idx_rehealth_telemetry_projection_device_time (tenant_ref, device_ref, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rehealth_telemetry_quality_case (
    event_id VARCHAR(128) NOT NULL PRIMARY KEY,
    batch_id VARCHAR(128) NOT NULL,
    tenant_ref VARCHAR(160) NOT NULL,
    device_ref VARCHAR(160) NOT NULL,
    accepted_count INT NOT NULL,
    rejected_count INT NOT NULL,
    quality_status VARCHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    CONSTRAINT fk_rehealth_quality_projection
        FOREIGN KEY (event_id) REFERENCES rehealth_telemetry_event_projection (event_id),
    KEY idx_rehealth_quality_case_tenant_time (tenant_ref, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO rehealth_schema_migration(version)
VALUES ('software-V20260723.3');
