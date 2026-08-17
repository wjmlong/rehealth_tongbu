-- Institution-plan execution facts used to calculate rolling adherence.
-- The APP reports bounded completion facts; the server derives adherence_score.
DROP PROCEDURE IF EXISTS rehealth_add_adherence_column;
DELIMITER $$
CREATE PROCEDURE rehealth_add_adherence_column(
    IN target_column VARCHAR(64),
    IN column_definition VARCHAR(1000)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'rehealth_insurance_intervention_feedback'
          AND COLUMN_NAME = target_column
    ) THEN
        SET @rehealth_adherence_alter = CONCAT(
            'ALTER TABLE rehealth_insurance_intervention_feedback ADD COLUMN ', column_definition
        );
        PREPARE rehealth_adherence_statement FROM @rehealth_adherence_alter;
        EXECUTE rehealth_adherence_statement;
        DEALLOCATE PREPARE rehealth_adherence_statement;
    END IF;
END$$
DELIMITER ;

CALL rehealth_add_adherence_column('plan_item_id', '`plan_item_id` VARCHAR(128) NULL AFTER `intervention_id`');
CALL rehealth_add_adherence_column('expected_count', '`expected_count` DECIMAL(10,3) NULL AFTER `adherence_score`');
CALL rehealth_add_adherence_column('completed_count', '`completed_count` DECIMAL(10,3) NULL AFTER `expected_count`');
CALL rehealth_add_adherence_column('verification_type', '`verification_type` VARCHAR(32) NOT NULL DEFAULT ''self_report'' AFTER `completed_count`');
CALL rehealth_add_adherence_column('calculation_version', '`calculation_version` VARCHAR(64) NOT NULL DEFAULT ''legacy-client-score'' AFTER `verification_type`');
DROP PROCEDURE rehealth_add_adherence_column;

SET @add_adherence_index = IF(
    EXISTS (
        SELECT 1 FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'rehealth_insurance_intervention_feedback'
          AND index_name = 'idx_insurance_feedback_item_period'
    ),
    'SELECT 1',
    'ALTER TABLE rehealth_insurance_intervention_feedback ADD INDEX idx_insurance_feedback_item_period (tenant_id, binding_id, plan_item_id, occurred_at)'
);
PREPARE add_adherence_index_statement FROM @add_adherence_index;
EXECUTE add_adherence_index_statement;
DEALLOCATE PREPARE add_adherence_index_statement;

INSERT IGNORE INTO rehealth_schema_migration(version)
VALUES ('software-V20260817.1');
