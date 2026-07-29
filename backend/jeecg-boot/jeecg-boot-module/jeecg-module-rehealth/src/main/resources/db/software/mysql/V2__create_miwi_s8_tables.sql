-- S8 (云米) 4G watch pull-connector tables (8 月初 launch).
-- These live in the software (Jeecg) datasource, separate from rehealth_device_binding
-- (the app-side user binding). A watch only produces telemetry once it is BOTH
-- registered here AND bound to a user via the normal app bind flow.

CREATE TABLE rehealth_s8_device (
    device_id VARCHAR(128) PRIMARY KEY,
    imei VARCHAR(32) NOT NULL,
    model VARCHAR(128),
    role VARCHAR(32) NOT NULL DEFAULT 'SAFETY_4G',
    is_active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_rehealth_s8_imei (imei)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rehealth_s8_sync_cursor (
    device_id VARCHAR(128) NOT NULL,
    metric_type VARCHAR(64) NOT NULL,
    cursor_utc BIGINT NOT NULL,
    last_success_at BIGINT NOT NULL DEFAULT 0,
    failure_count INT NOT NULL DEFAULT 0,
    last_error_code VARCHAR(255),
    updated_at DATETIME(3) NOT NULL,
    PRIMARY KEY (device_id, metric_type),
    KEY idx_rehealth_s8_cursor_updated (updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
