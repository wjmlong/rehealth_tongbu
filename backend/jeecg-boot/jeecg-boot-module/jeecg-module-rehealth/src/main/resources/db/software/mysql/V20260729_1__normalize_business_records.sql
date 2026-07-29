CREATE TABLE IF NOT EXISTS rehealth_schema_migration (
    version VARCHAR(64) NOT NULL PRIMARY KEY,
    applied_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

DROP PROCEDURE IF EXISTS rehealth_add_column_if_missing;
DELIMITER $$
CREATE PROCEDURE rehealth_add_column_if_missing(
    IN target_table VARCHAR(64),
    IN target_column VARCHAR(64),
    IN column_definition VARCHAR(1000)
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

CALL rehealth_add_column_if_missing('rehealth_patient_profile', 'name', '`name` VARCHAR(128) NULL AFTER `user_id`');
CALL rehealth_add_column_if_missing('rehealth_patient_profile', 'gender', '`gender` VARCHAR(32) NULL AFTER `name`');
CALL rehealth_add_column_if_missing('rehealth_patient_profile', 'age', '`age` SMALLINT NULL AFTER `gender`');
CALL rehealth_add_column_if_missing('rehealth_patient_profile', 'height_cm', '`height_cm` DECIMAL(6,2) NULL AFTER `age`');
CALL rehealth_add_column_if_missing('rehealth_patient_profile', 'weight_kg', '`weight_kg` DECIMAL(6,2) NULL AFTER `height_cm`');
CALL rehealth_add_column_if_missing('rehealth_patient_profile', 'bmi', '`bmi` DECIMAL(5,2) NULL AFTER `weight_kg`');
CALL rehealth_add_column_if_missing('rehealth_patient_profile', 'family_history', '`family_history` TINYINT(1) NULL AFTER `bmi`');
CALL rehealth_add_column_if_missing('rehealth_patient_profile', 'smoking', '`smoking` TINYINT(1) NULL AFTER `family_history`');
CALL rehealth_add_column_if_missing('rehealth_patient_profile', 'drinking', '`drinking` TINYINT(1) NULL AFTER `smoking`');
CALL rehealth_add_column_if_missing('rehealth_patient_profile', 'diabetes_history', '`diabetes_history` TINYINT(1) NULL AFTER `drinking`');
CALL rehealth_add_column_if_missing('rehealth_patient_profile', 'hypertension_history', '`hypertension_history` TINYINT(1) NULL AFTER `diabetes_history`');
CALL rehealth_add_column_if_missing('rehealth_patient_profile', 'profile_version', '`profile_version` BIGINT NOT NULL DEFAULT 1 AFTER `hypertension_history`');
CALL rehealth_add_column_if_missing('rehealth_health_interview', 'generated_at', '`generated_at` DATETIME(3) NULL AFTER `user_id`');
CALL rehealth_add_column_if_missing('rehealth_cvd_risk_result', 'fallback_reason', '`fallback_reason` VARCHAR(512) NULL AFTER `artifact_name`');
CALL rehealth_add_column_if_missing('rehealth_intervention_plan', 'priority_intervention', '`priority_intervention` VARCHAR(1000) NULL AFTER `artifact_name`');
CALL rehealth_add_column_if_missing('rehealth_intervention_plan', 'rationale', '`rationale` LONGTEXT NULL AFTER `priority_intervention`');
CALL rehealth_add_column_if_missing('rehealth_intervention_plan', 'expected_impact', '`expected_impact` VARCHAR(1000) NULL AFTER `rationale`');
CALL rehealth_add_column_if_missing('rehealth_intervention_plan', 'confidence', '`confidence` DOUBLE NULL AFTER `expected_impact`');
CALL rehealth_add_column_if_missing('rehealth_intervention_plan', 'medical_disclaimer', '`medical_disclaimer` VARCHAR(2000) NULL AFTER `confidence`');
CALL rehealth_add_column_if_missing('rehealth_attribution_result', 'request_id', '`request_id` VARCHAR(128) NULL AFTER `model_version`');
CALL rehealth_add_column_if_missing('rehealth_attribution_result', 'attribution_mode', '`attribution_mode` VARCHAR(64) NULL AFTER `request_id`');
CALL rehealth_add_column_if_missing('rehealth_attribution_result', 'is_mock', '`is_mock` TINYINT(1) NULL AFTER `attribution_mode`');
CALL rehealth_add_column_if_missing('rehealth_attribution_result', 'provider', '`provider` VARCHAR(128) NULL AFTER `is_mock`');
CALL rehealth_add_column_if_missing('rehealth_attribution_result', 'history_days', '`history_days` INT NULL AFTER `provider`');
CALL rehealth_add_column_if_missing('rehealth_attribution_result', 'min_history_days', '`min_history_days` INT NULL AFTER `history_days`');
CALL rehealth_add_column_if_missing('rehealth_attribution_result', 'intervention_days', '`intervention_days` INT NULL AFTER `min_history_days`');
CALL rehealth_add_column_if_missing('rehealth_attribution_result', 'intervention_data_sufficient', '`intervention_data_sufficient` TINYINT(1) NULL AFTER `intervention_days`');
CALL rehealth_add_column_if_missing('rehealth_attribution_result', 'current_risk_score', '`current_risk_score` DOUBLE NULL AFTER `intervention_data_sufficient`');
CALL rehealth_add_column_if_missing('rehealth_attribution_result', 'current_risk_level', '`current_risk_level` VARCHAR(64) NULL AFTER `current_risk_score`');
CALL rehealth_add_column_if_missing('rehealth_attribution_result', 'current_trend', '`current_trend` VARCHAR(64) NULL AFTER `current_risk_level`');
CALL rehealth_add_column_if_missing('rehealth_attribution_result', 'individual_att', '`individual_att` DOUBLE NULL AFTER `current_trend`');
CALL rehealth_add_column_if_missing('rehealth_attribution_result', 'trend_delta', '`trend_delta` DOUBLE NULL AFTER `individual_att`');
CALL rehealth_add_column_if_missing('rehealth_attribution_result', 'adherence_average', '`adherence_average` DOUBLE NULL AFTER `trend_delta`');
CALL rehealth_add_column_if_missing('rehealth_attribution_result', 'interpretation', '`interpretation` LONGTEXT NULL AFTER `adherence_average`');
CALL rehealth_add_column_if_missing('rehealth_attribution_result', 'error_code', '`error_code` VARCHAR(64) NULL AFTER `interpretation`');
CALL rehealth_add_column_if_missing('rehealth_attribution_result', 'retryable', '`retryable` TINYINT(1) NULL AFTER `error_code`');

DROP PROCEDURE rehealth_add_column_if_missing;

CREATE TABLE IF NOT EXISTS rehealth_patient_diagnosis (
    id VARCHAR(64) PRIMARY KEY,
    profile_id VARCHAR(64) NOT NULL,
    item_value VARCHAR(512) NOT NULL,
    sort_order INT NOT NULL,
    created_at DATETIME(3) NOT NULL,
    CONSTRAINT fk_rehealth_diagnosis_profile FOREIGN KEY (profile_id)
        REFERENCES rehealth_patient_profile (id) ON DELETE CASCADE,
    UNIQUE KEY uk_rehealth_diagnosis_order (profile_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rehealth_patient_medication (
    id VARCHAR(64) PRIMARY KEY,
    profile_id VARCHAR(64) NOT NULL,
    item_value VARCHAR(512) NOT NULL,
    sort_order INT NOT NULL,
    created_at DATETIME(3) NOT NULL,
    CONSTRAINT fk_rehealth_medication_profile FOREIGN KEY (profile_id)
        REFERENCES rehealth_patient_profile (id) ON DELETE CASCADE,
    UNIQUE KEY uk_rehealth_medication_order (profile_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rehealth_patient_allergy (
    id VARCHAR(64) PRIMARY KEY,
    profile_id VARCHAR(64) NOT NULL,
    item_value VARCHAR(512) NOT NULL,
    sort_order INT NOT NULL,
    created_at DATETIME(3) NOT NULL,
    CONSTRAINT fk_rehealth_allergy_profile FOREIGN KEY (profile_id)
        REFERENCES rehealth_patient_profile (id) ON DELETE CASCADE,
    UNIQUE KEY uk_rehealth_allergy_order (profile_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rehealth_health_interview_answer (
    id VARCHAR(64) PRIMARY KEY,
    interview_id VARCHAR(64) NOT NULL,
    question_id VARCHAR(128),
    topic VARCHAR(64),
    content TEXT NOT NULL,
    sort_order INT NOT NULL,
    CONSTRAINT fk_rehealth_interview_answer FOREIGN KEY (interview_id)
        REFERENCES rehealth_health_interview (id) ON DELETE CASCADE,
    UNIQUE KEY uk_rehealth_interview_answer_order (interview_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rehealth_health_interview_baseline (
    id VARCHAR(64) PRIMARY KEY,
    interview_id VARCHAR(64) NOT NULL,
    label VARCHAR(255),
    item_value VARCHAR(1000),
    sort_order INT NOT NULL,
    CONSTRAINT fk_rehealth_interview_baseline FOREIGN KEY (interview_id)
        REFERENCES rehealth_health_interview (id) ON DELETE CASCADE,
    UNIQUE KEY uk_rehealth_interview_baseline_order (interview_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rehealth_health_interview_focus (
    id VARCHAR(64) PRIMARY KEY,
    interview_id VARCHAR(64) NOT NULL,
    focus_area VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL,
    CONSTRAINT fk_rehealth_interview_focus FOREIGN KEY (interview_id)
        REFERENCES rehealth_health_interview (id) ON DELETE CASCADE,
    UNIQUE KEY uk_rehealth_interview_focus_order (interview_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rehealth_intervention_contraindication (
    id VARCHAR(64) PRIMARY KEY,
    plan_record_id VARCHAR(64) NOT NULL,
    item_value VARCHAR(1000) NOT NULL,
    sort_order INT NOT NULL,
    CONSTRAINT fk_rehealth_contraindication_plan FOREIGN KEY (plan_record_id)
        REFERENCES rehealth_intervention_plan (id) ON DELETE CASCADE,
    UNIQUE KEY uk_rehealth_contraindication_order (plan_record_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

UPDATE rehealth_patient_profile
SET name = COALESCE(name, LEFT(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(profile_json, '$.name')), 'null'), 128)),
    gender = COALESCE(gender, CASE
        WHEN JSON_UNQUOTE(JSON_EXTRACT(profile_json, '$.gender')) IN ('male', 'female')
        THEN JSON_UNQUOTE(JSON_EXTRACT(profile_json, '$.gender'))
    END),
    age = COALESCE(age, CASE
        WHEN CAST(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(profile_json, '$.age')), 'null') AS DECIMAL(10,2)) BETWEEN 1 AND 120
        THEN CAST(JSON_UNQUOTE(JSON_EXTRACT(profile_json, '$.age')) AS UNSIGNED)
    END),
    height_cm = COALESCE(height_cm, CASE
        WHEN CAST(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(profile_json, '$.heightCm')), 'null') AS DECIMAL(10,2)) BETWEEN 50 AND 250
        THEN CAST(JSON_UNQUOTE(JSON_EXTRACT(profile_json, '$.heightCm')) AS DECIMAL(6,2))
    END),
    weight_kg = COALESCE(weight_kg, CASE
        WHEN CAST(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(profile_json, '$.weightKg')), 'null') AS DECIMAL(10,2)) BETWEEN 2 AND 500
        THEN CAST(JSON_UNQUOTE(JSON_EXTRACT(profile_json, '$.weightKg')) AS DECIMAL(6,2))
    END),
    family_history = COALESCE(family_history, CASE JSON_UNQUOTE(JSON_EXTRACT(profile_json, '$.familyHistory')) WHEN 'true' THEN 1 WHEN 'false' THEN 0 END),
    smoking = COALESCE(smoking, CASE JSON_UNQUOTE(JSON_EXTRACT(profile_json, '$.smoking')) WHEN 'true' THEN 1 WHEN 'false' THEN 0 END),
    drinking = COALESCE(drinking, CASE JSON_UNQUOTE(JSON_EXTRACT(profile_json, '$.drinking')) WHEN 'true' THEN 1 WHEN 'false' THEN 0 END),
    diabetes_history = COALESCE(diabetes_history, CASE JSON_UNQUOTE(JSON_EXTRACT(profile_json, '$.diabetesHistory')) WHEN 'true' THEN 1 WHEN 'false' THEN 0 END),
    hypertension_history = COALESCE(hypertension_history, CASE JSON_UNQUOTE(JSON_EXTRACT(profile_json, '$.hypertensionHistory')) WHEN 'true' THEN 1 WHEN 'false' THEN 0 END)
WHERE profile_json IS NOT NULL AND JSON_VALID(profile_json);

UPDATE rehealth_patient_profile
SET bmi = ROUND(weight_kg / POWER(height_cm / 100.0, 2), 2)
WHERE height_cm BETWEEN 50 AND 250 AND weight_kg BETWEEN 2 AND 500;

UPDATE rehealth_health_interview
SET generated_at = COALESCE(
        generated_at,
        CASE WHEN JSON_VALID(baseline_json) THEN
            FROM_UNIXTIME(CAST(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(baseline_json, '$.generatedAt')), 'null') AS UNSIGNED) / 1000.0)
        END,
        created_at
    )
WHERE generated_at IS NULL;

UPDATE rehealth_cvd_risk_result
SET fallback_reason = COALESCE(
        fallback_reason,
        NULLIF(JSON_UNQUOTE(JSON_EXTRACT(response_json, '$.model_trace.fallback_reason')), 'null')
    )
WHERE response_json IS NOT NULL AND JSON_VALID(response_json);

UPDATE rehealth_intervention_plan
SET priority_intervention = COALESCE(priority_intervention, NULLIF(JSON_UNQUOTE(JSON_EXTRACT(response_json, '$.priority_intervention')), 'null')),
    rationale = COALESCE(rationale, NULLIF(JSON_UNQUOTE(JSON_EXTRACT(response_json, '$.rationale')), 'null')),
    expected_impact = COALESCE(expected_impact, NULLIF(JSON_UNQUOTE(JSON_EXTRACT(response_json, '$.expected_impact')), 'null')),
    confidence = COALESCE(confidence, CAST(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(response_json, '$.confidence')), 'null') AS DECIMAL(10,6))),
    medical_disclaimer = COALESCE(medical_disclaimer, NULLIF(JSON_UNQUOTE(JSON_EXTRACT(response_json, '$.medical_disclaimer')), 'null'))
WHERE response_json IS NOT NULL AND JSON_VALID(response_json);

UPDATE rehealth_attribution_result
SET request_id = COALESCE(request_id, NULLIF(JSON_UNQUOTE(JSON_EXTRACT(response_json, '$.request_id')), 'null')),
    attribution_mode = COALESCE(attribution_mode, NULLIF(JSON_UNQUOTE(JSON_EXTRACT(response_json, '$.attribution_mode')), 'null')),
    is_mock = COALESCE(is_mock, CASE JSON_UNQUOTE(JSON_EXTRACT(response_json, '$.is_mock')) WHEN 'true' THEN 1 WHEN 'false' THEN 0 END),
    provider = COALESCE(provider, NULLIF(JSON_UNQUOTE(JSON_EXTRACT(response_json, '$.provider')), 'null')),
    history_days = COALESCE(history_days, CAST(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(response_json, '$.history_days')), 'null') AS UNSIGNED)),
    min_history_days = COALESCE(min_history_days, CAST(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(response_json, '$.min_history_days')), 'null') AS UNSIGNED)),
    intervention_days = COALESCE(intervention_days, CAST(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(response_json, '$.intervention_days')), 'null') AS UNSIGNED)),
    intervention_data_sufficient = COALESCE(intervention_data_sufficient, CASE JSON_UNQUOTE(JSON_EXTRACT(response_json, '$.intervention_data_sufficient')) WHEN 'true' THEN 1 WHEN 'false' THEN 0 END),
    current_risk_score = COALESCE(current_risk_score, CAST(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(response_json, '$.current_state.risk_score')), 'null') AS DECIMAL(12,8))),
    current_risk_level = COALESCE(current_risk_level, NULLIF(JSON_UNQUOTE(JSON_EXTRACT(response_json, '$.current_state.risk_level')), 'null')),
    current_trend = COALESCE(current_trend, NULLIF(JSON_UNQUOTE(JSON_EXTRACT(response_json, '$.current_state.trend')), 'null')),
    individual_att = COALESCE(individual_att, CAST(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(response_json, '$.intervention_effect.individual_att')), 'null') AS DECIMAL(12,8))),
    trend_delta = COALESCE(trend_delta, CAST(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(response_json, '$.trend_delta')), 'null') AS DECIMAL(12,8))),
    adherence_average = COALESCE(adherence_average, CAST(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(response_json, '$.adherence_average')), 'null') AS DECIMAL(12,8))),
    interpretation = COALESCE(interpretation, NULLIF(JSON_UNQUOTE(JSON_EXTRACT(response_json, '$.interpretation')), 'null')),
    error_code = COALESCE(error_code, NULLIF(JSON_UNQUOTE(JSON_EXTRACT(response_json, '$.error_code')), 'null')),
    retryable = COALESCE(retryable, CASE JSON_UNQUOTE(JSON_EXTRACT(response_json, '$.retryable')) WHEN 'true' THEN 1 WHEN 'false' THEN 0 END)
WHERE response_json IS NOT NULL AND JSON_VALID(response_json);

DROP PROCEDURE IF EXISTS rehealth_backfill_json_arrays;
DELIMITER $$
CREATE PROCEDURE rehealth_backfill_json_arrays()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE record_id VARCHAR(64);
    DECLARE payload LONGTEXT;
    DECLARE item_count INT DEFAULT 0;
    DECLARE item_index INT DEFAULT 0;
    DECLARE item_value TEXT;
    DECLARE profile_cursor CURSOR FOR
        SELECT id, profile_json FROM rehealth_patient_profile WHERE profile_json IS NOT NULL AND JSON_VALID(profile_json);
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    OPEN profile_cursor;
    profile_loop: LOOP
        FETCH profile_cursor INTO record_id, payload;
        IF done = 1 THEN
            LEAVE profile_loop;
        END IF;

        SET item_index = 0;
        SET item_count = COALESCE(JSON_LENGTH(JSON_EXTRACT(payload, '$.diagnoses')), 0);
        WHILE item_index < item_count DO
            SET item_value = JSON_UNQUOTE(JSON_EXTRACT(payload, CONCAT('$.diagnoses[', item_index, ']')));
            IF item_value IS NOT NULL AND item_value <> 'null' AND item_value <> '' THEN
                INSERT IGNORE INTO rehealth_patient_diagnosis(id, profile_id, item_value, sort_order, created_at)
                VALUES (UUID(), record_id, LEFT(item_value, 512), item_index, CURRENT_TIMESTAMP(3));
            END IF;
            SET item_index = item_index + 1;
        END WHILE;

        SET item_index = 0;
        SET item_count = COALESCE(JSON_LENGTH(JSON_EXTRACT(payload, '$.medications')), 0);
        WHILE item_index < item_count DO
            SET item_value = JSON_UNQUOTE(JSON_EXTRACT(payload, CONCAT('$.medications[', item_index, ']')));
            IF item_value IS NOT NULL AND item_value <> 'null' AND item_value <> '' THEN
                INSERT IGNORE INTO rehealth_patient_medication(id, profile_id, item_value, sort_order, created_at)
                VALUES (UUID(), record_id, LEFT(item_value, 512), item_index, CURRENT_TIMESTAMP(3));
            END IF;
            SET item_index = item_index + 1;
        END WHILE;

        SET item_index = 0;
        SET item_count = COALESCE(JSON_LENGTH(JSON_EXTRACT(payload, '$.allergies')), 0);
        WHILE item_index < item_count DO
            SET item_value = JSON_UNQUOTE(JSON_EXTRACT(payload, CONCAT('$.allergies[', item_index, ']')));
            IF item_value IS NOT NULL AND item_value <> 'null' AND item_value <> '' THEN
                INSERT IGNORE INTO rehealth_patient_allergy(id, profile_id, item_value, sort_order, created_at)
                VALUES (UUID(), record_id, LEFT(item_value, 512), item_index, CURRENT_TIMESTAMP(3));
            END IF;
            SET item_index = item_index + 1;
        END WHILE;
    END LOOP;
    CLOSE profile_cursor;
END$$
DELIMITER ;

CALL rehealth_backfill_json_arrays();
DROP PROCEDURE rehealth_backfill_json_arrays;

DROP PROCEDURE IF EXISTS rehealth_backfill_interviews;
DELIMITER $$
CREATE PROCEDURE rehealth_backfill_interviews()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE record_id VARCHAR(64);
    DECLARE answers_payload LONGTEXT;
    DECLARE baseline_payload LONGTEXT;
    DECLARE item_count INT DEFAULT 0;
    DECLARE item_index INT DEFAULT 0;
    DECLARE focus_value TEXT;
    DECLARE interview_cursor CURSOR FOR
        SELECT id, answers_json, baseline_json FROM rehealth_health_interview;
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    OPEN interview_cursor;
    interview_loop: LOOP
        FETCH interview_cursor INTO record_id, answers_payload, baseline_payload;
        IF done = 1 THEN
            LEAVE interview_loop;
        END IF;

        SET item_index = 0;
        SET item_count = IF(JSON_VALID(answers_payload), COALESCE(JSON_LENGTH(answers_payload), 0), 0);
        WHILE item_index < item_count DO
            INSERT IGNORE INTO rehealth_health_interview_answer(
                id, interview_id, question_id, topic, content, sort_order
            ) VALUES (
                UUID(), record_id,
                NULLIF(JSON_UNQUOTE(JSON_EXTRACT(answers_payload, CONCAT('$[', item_index, '].questionId'))), 'null'),
                NULLIF(JSON_UNQUOTE(JSON_EXTRACT(answers_payload, CONCAT('$[', item_index, '].topic'))), 'null'),
                COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(answers_payload, CONCAT('$[', item_index, '].content'))), 'null'), ''),
                item_index
            );
            SET item_index = item_index + 1;
        END WHILE;

        SET item_index = 0;
        SET item_count = IF(JSON_VALID(baseline_payload), COALESCE(JSON_LENGTH(JSON_EXTRACT(baseline_payload, '$.baselineItems')), 0), 0);
        WHILE item_index < item_count DO
            INSERT IGNORE INTO rehealth_health_interview_baseline(
                id, interview_id, label, item_value, sort_order
            ) VALUES (
                UUID(), record_id,
                NULLIF(JSON_UNQUOTE(JSON_EXTRACT(baseline_payload, CONCAT('$.baselineItems[', item_index, '].label'))), 'null'),
                NULLIF(JSON_UNQUOTE(JSON_EXTRACT(baseline_payload, CONCAT('$.baselineItems[', item_index, '].value'))), 'null'),
                item_index
            );
            SET item_index = item_index + 1;
        END WHILE;

        SET item_index = 0;
        SET item_count = IF(JSON_VALID(baseline_payload), COALESCE(JSON_LENGTH(JSON_EXTRACT(baseline_payload, '$.focusAreas')), 0), 0);
        WHILE item_index < item_count DO
            SET focus_value = NULLIF(JSON_UNQUOTE(JSON_EXTRACT(baseline_payload, CONCAT('$.focusAreas[', item_index, ']'))), 'null');
            IF focus_value IS NOT NULL AND focus_value <> '' THEN
                INSERT IGNORE INTO rehealth_health_interview_focus(id, interview_id, focus_area, sort_order)
                VALUES (UUID(), record_id, LEFT(focus_value, 255), item_index);
            END IF;
            SET item_index = item_index + 1;
        END WHILE;
    END LOOP;
    CLOSE interview_cursor;
END$$
DELIMITER ;

CALL rehealth_backfill_interviews();
DROP PROCEDURE rehealth_backfill_interviews;

DROP PROCEDURE IF EXISTS rehealth_backfill_contraindications;
DELIMITER $$
CREATE PROCEDURE rehealth_backfill_contraindications()
BEGIN
    DECLARE done INT DEFAULT 0;
    DECLARE record_id VARCHAR(64);
    DECLARE payload LONGTEXT;
    DECLARE item_count INT DEFAULT 0;
    DECLARE item_index INT DEFAULT 0;
    DECLARE item_value TEXT;
    DECLARE plan_cursor CURSOR FOR
        SELECT id, response_json FROM rehealth_intervention_plan WHERE response_json IS NOT NULL AND JSON_VALID(response_json);
    DECLARE CONTINUE HANDLER FOR NOT FOUND SET done = 1;

    OPEN plan_cursor;
    plan_loop: LOOP
        FETCH plan_cursor INTO record_id, payload;
        IF done = 1 THEN
            LEAVE plan_loop;
        END IF;
        SET item_index = 0;
        SET item_count = COALESCE(JSON_LENGTH(JSON_EXTRACT(payload, '$.contraindications')), 0);
        WHILE item_index < item_count DO
            SET item_value = JSON_UNQUOTE(JSON_EXTRACT(payload, CONCAT('$.contraindications[', item_index, ']')));
            IF item_value IS NOT NULL AND item_value <> 'null' AND item_value <> '' THEN
                INSERT IGNORE INTO rehealth_intervention_contraindication(id, plan_record_id, item_value, sort_order)
                VALUES (UUID(), record_id, LEFT(item_value, 1000), item_index);
            END IF;
            SET item_index = item_index + 1;
        END WHILE;
    END LOOP;
    CLOSE plan_cursor;
END$$
DELIMITER ;

CALL rehealth_backfill_contraindications();
DROP PROCEDURE rehealth_backfill_contraindications;

ALTER TABLE rehealth_patient_profile MODIFY COLUMN profile_json LONGTEXT NULL;
ALTER TABLE rehealth_health_interview MODIFY COLUMN answers_json LONGTEXT NULL;
ALTER TABLE rehealth_health_interview MODIFY COLUMN generated_at DATETIME(3) NOT NULL;

INSERT IGNORE INTO rehealth_schema_migration(version) VALUES ('software-V20260729.1');
