-- Miwi S8 push channel: raw envelope landing + device transport marker.
-- Additive migration; safe on both fresh and existing schemas (Flyway/compatible).

ALTER TABLE rehealth_s8_device
    ADD COLUMN transport VARCHAR(16) NOT NULL DEFAULT 'PULL' COMMENT 'PULL / PUSH / BOTH',
    ADD COLUMN last_push_at DATETIME(3) NULL COMMENT 'last vendor push received at',
    ADD COLUMN push_registered_at DATETIME(3) NULL COMMENT 'when push callback was registered';

CREATE TABLE rehealth_s8_push_raw (
    id BIGINT NOT NULL AUTO_INCREMENT,
    device_id VARCHAR(128) NULL,
    imei VARCHAR(32) NULL,
    data_type VARCHAR(32) NULL,
    raw_body MEDIUMTEXT NOT NULL,
    received_at DATETIME(3) NOT NULL,
    process_result VARCHAR(32) NULL,
    process_error VARCHAR(512) NULL,
    client_ip VARCHAR(64) NULL,
    PRIMARY KEY (id),
    KEY idx_push_raw_imei (imei),
    KEY idx_push_raw_received (received_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Miwi vendor push raw envelope (audit/replay)';
