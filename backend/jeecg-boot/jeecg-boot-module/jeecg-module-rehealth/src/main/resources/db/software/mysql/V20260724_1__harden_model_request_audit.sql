DROP PROCEDURE IF EXISTS rehealth_add_model_audit_column;
DELIMITER $$
CREATE PROCEDURE rehealth_add_model_audit_column(
    IN target_column VARCHAR(64),
    IN column_definition VARCHAR(512)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'rehealth_model_request_log'
          AND COLUMN_NAME = target_column
    ) THEN
        SET @rehealth_alter = CONCAT(
            'ALTER TABLE rehealth_model_request_log ADD COLUMN ',
            column_definition
        );
        PREPARE rehealth_statement FROM @rehealth_alter;
        EXECUTE rehealth_statement;
        DEALLOCATE PREPARE rehealth_statement;
    END IF;
END$$
DELIMITER ;

CALL rehealth_add_model_audit_column('error_code', 'error_code VARCHAR(64) NULL AFTER outcome');
CALL rehealth_add_model_audit_column(
    'latency_ms',
    'latency_ms BIGINT NOT NULL DEFAULT 0 AFTER error_code'
);

DROP PROCEDURE rehealth_add_model_audit_column;

INSERT IGNORE INTO rehealth_schema_migration(version)
VALUES ('software-V20260724.1');
