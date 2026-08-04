CREATE TABLE IF NOT EXISTS rehealth_schema_migration (
    version VARCHAR(64) NOT NULL PRIMARY KEY,
    applied_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP PROCEDURE IF EXISTS rehealth_add_factor16_column;
DELIMITER $$
CREATE PROCEDURE rehealth_add_factor16_column(
    IN target_column VARCHAR(64),
    IN column_definition VARCHAR(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'rehealth_cvd_risk_result'
          AND COLUMN_NAME = target_column
    ) THEN
        SET @rehealth_alter = CONCAT(
            'ALTER TABLE rehealth_cvd_risk_result ADD COLUMN ',
            column_definition
        );
        PREPARE rehealth_statement FROM @rehealth_alter;
        EXECUTE rehealth_statement;
        DEALLOCATE PREPARE rehealth_statement;
    END IF;
END$$
DELIMITER ;

CALL rehealth_add_factor16_column(
    'factor_contribution_version',
    'factor_contribution_version VARCHAR(64) NULL AFTER contribution_method'
);
CALL rehealth_add_factor16_column(
    'factor_contribution_json',
    'factor_contribution_json LONGTEXT NULL AFTER contribution_json'
);
CALL rehealth_add_factor16_column(
    'factor_measured_component_json',
    'factor_measured_component_json LONGTEXT NULL AFTER factor_contribution_json'
);
CALL rehealth_add_factor16_column(
    'factor_control_support_json',
    'factor_control_support_json LONGTEXT NULL AFTER factor_measured_component_json'
);

DROP PROCEDURE rehealth_add_factor16_column;

INSERT IGNORE INTO rehealth_schema_migration(version)
VALUES ('software-V20260731.2');
