CREATE TABLE IF NOT EXISTS rehealth_patient_profile (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    profile_json LONGTEXT NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_rehealth_profile_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rehealth_health_interview (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    answers_json LONGTEXT NOT NULL,
    baseline_json LONGTEXT,
    created_at DATETIME(3) NOT NULL,
    KEY idx_health_interview_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rehealth_attribution_result (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    status VARCHAR(64),
    model_version VARCHAR(128),
    request_json LONGTEXT NOT NULL,
    response_json LONGTEXT NOT NULL,
    created_at DATETIME(3) NOT NULL,
    KEY idx_attribution_result_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rehealth_model_request_log (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    request_id VARCHAR(128),
    operation VARCHAR(64) NOT NULL,
    model_version VARCHAR(128),
    outcome VARCHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    KEY idx_model_request_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP PROCEDURE IF EXISTS rehealth_add_column_if_missing;
DELIMITER $$
CREATE PROCEDURE rehealth_add_column_if_missing(
    IN target_table VARCHAR(64),
    IN target_column VARCHAR(64),
    IN column_definition VARCHAR(512)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = target_table
          AND COLUMN_NAME = target_column
    ) THEN
        SET @rehealth_alter = CONCAT('ALTER TABLE `', target_table, '` ADD COLUMN ', column_definition);
        PREPARE rehealth_statement FROM @rehealth_alter;
        EXECUTE rehealth_statement;
        DEALLOCATE PREPARE rehealth_statement;
    END IF;
END$$
DELIMITER ;

CALL rehealth_add_column_if_missing('rehealth_device_binding', 'model', '`model` VARCHAR(128) NULL AFTER `device_model`');
CALL rehealth_add_column_if_missing('rehealth_device_binding', 'status', '`status` VARCHAR(32) NULL AFTER `hardware_address_hash`');
CALL rehealth_add_column_if_missing('rehealth_cvd_feature_vector', 'payload_json', '`payload_json` LONGTEXT NULL AFTER `quality_json`');
CALL rehealth_add_column_if_missing('rehealth_cvd_risk_result', 'created_at', '`created_at` DATETIME(3) NULL AFTER `evaluated_at`');
CALL rehealth_add_column_if_missing('rehealth_intervention_feedback', 'intervention_id', '`intervention_id` VARCHAR(128) NULL AFTER `plan_id`');

UPDATE rehealth_device_binding
SET model = COALESCE(model, device_model),
    status = COALESCE(status, 'BOUND');
UPDATE rehealth_cvd_feature_vector
SET payload_json = COALESCE(payload_json, feature_json);
UPDATE rehealth_cvd_risk_result
SET created_at = COALESCE(created_at, evaluated_at);
UPDATE rehealth_intervention_feedback
SET intervention_id = COALESCE(intervention_id, plan_id);

ALTER TABLE rehealth_device_binding MODIFY COLUMN status VARCHAR(32) NOT NULL;
ALTER TABLE rehealth_cvd_feature_vector MODIFY COLUMN payload_json LONGTEXT NOT NULL;
ALTER TABLE rehealth_cvd_risk_result MODIFY COLUMN created_at DATETIME(3) NOT NULL;
ALTER TABLE rehealth_intervention_feedback MODIFY COLUMN intervention_id VARCHAR(128) NOT NULL;

DROP PROCEDURE rehealth_add_column_if_missing;

CREATE TABLE IF NOT EXISTS rehealth_schema_migration (
    version VARCHAR(64) NOT NULL PRIMARY KEY,
    applied_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT IGNORE INTO rehealth_schema_migration(version) VALUES ('software-V20260723.2');
