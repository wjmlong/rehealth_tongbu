CREATE TABLE rehealth_patient_profile (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    name VARCHAR(128),
    gender VARCHAR(32),
    age SMALLINT,
    height_cm DECIMAL(6,2),
    weight_kg DECIMAL(6,2),
    bmi DECIMAL(5,2),
    family_history TINYINT(1),
    smoking TINYINT(1),
    drinking TINYINT(1),
    diabetes_history TINYINT(1),
    hypertension_history TINYINT(1),
    profile_version BIGINT NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_rehealth_profile_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rehealth_patient_diagnosis (
    id VARCHAR(64) PRIMARY KEY,
    profile_id VARCHAR(64) NOT NULL,
    item_value VARCHAR(512) NOT NULL,
    sort_order INT NOT NULL,
    created_at DATETIME(3) NOT NULL,
    CONSTRAINT fk_rehealth_diagnosis_profile FOREIGN KEY (profile_id)
        REFERENCES rehealth_patient_profile (id) ON DELETE CASCADE,
    UNIQUE KEY uk_rehealth_diagnosis_order (profile_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rehealth_patient_medication (
    id VARCHAR(64) PRIMARY KEY,
    profile_id VARCHAR(64) NOT NULL,
    item_value VARCHAR(512) NOT NULL,
    sort_order INT NOT NULL,
    created_at DATETIME(3) NOT NULL,
    CONSTRAINT fk_rehealth_medication_profile FOREIGN KEY (profile_id)
        REFERENCES rehealth_patient_profile (id) ON DELETE CASCADE,
    UNIQUE KEY uk_rehealth_medication_order (profile_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rehealth_patient_allergy (
    id VARCHAR(64) PRIMARY KEY,
    profile_id VARCHAR(64) NOT NULL,
    item_value VARCHAR(512) NOT NULL,
    sort_order INT NOT NULL,
    created_at DATETIME(3) NOT NULL,
    CONSTRAINT fk_rehealth_allergy_profile FOREIGN KEY (profile_id)
        REFERENCES rehealth_patient_profile (id) ON DELETE CASCADE,
    UNIQUE KEY uk_rehealth_allergy_order (profile_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rehealth_health_interview (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    generated_at DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    KEY idx_health_interview_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rehealth_health_interview_answer (
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

CREATE TABLE rehealth_health_interview_baseline (
    id VARCHAR(64) PRIMARY KEY,
    interview_id VARCHAR(64) NOT NULL,
    label VARCHAR(255),
    item_value VARCHAR(1000),
    sort_order INT NOT NULL,
    CONSTRAINT fk_rehealth_interview_baseline FOREIGN KEY (interview_id)
        REFERENCES rehealth_health_interview (id) ON DELETE CASCADE,
    UNIQUE KEY uk_rehealth_interview_baseline_order (interview_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rehealth_health_interview_focus (
    id VARCHAR(64) PRIMARY KEY,
    interview_id VARCHAR(64) NOT NULL,
    focus_area VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL,
    CONSTRAINT fk_rehealth_interview_focus FOREIGN KEY (interview_id)
        REFERENCES rehealth_health_interview (id) ON DELETE CASCADE,
    UNIQUE KEY uk_rehealth_interview_focus_order (interview_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rehealth_device_binding (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    device_id VARCHAR(128) NOT NULL,
    device_name VARCHAR(255),
    manufacturer VARCHAR(128),
    device_model VARCHAR(128),
    model VARCHAR(128),
    firmware_version VARCHAR(128),
    hardware_address_hash VARCHAR(255),
    status VARCHAR(32) NOT NULL,
    bound_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_rehealth_device_user_device (user_id, device_id),
    KEY idx_rehealth_device_user_updated (user_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rehealth_cvd_feature_vector (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    request_id VARCHAR(128) NOT NULL,
    feature_schema_version VARCHAR(64) NOT NULL,
    feature_json LONGTEXT NOT NULL,
    quality_json LONGTEXT,
    payload_json LONGTEXT NOT NULL,
    created_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_rehealth_feature_user_request (user_id, request_id),
    KEY idx_feature_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rehealth_cvd_risk_result (
    id VARCHAR(64) PRIMARY KEY,
    feature_vector_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    request_id VARCHAR(128) NOT NULL,
    feature_schema_version VARCHAR(64) NOT NULL,
    model_version VARCHAR(128) NOT NULL,
    scorer_mode VARCHAR(64),
    is_mock TINYINT(1),
    artifact_name VARCHAR(255),
    fallback_reason VARCHAR(512),
    contribution_method VARCHAR(64),
    risk_score DOUBLE NOT NULL,
    risk_level VARCHAR(64) NOT NULL,
    contribution_json LONGTEXT,
    missing_fields_json LONGTEXT,
    quality_warnings_json LONGTEXT,
    summary LONGTEXT,
    response_json LONGTEXT NOT NULL,
    evaluated_at DATETIME(3) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    CONSTRAINT fk_rehealth_risk_feature FOREIGN KEY (feature_vector_id) REFERENCES rehealth_cvd_feature_vector (id),
    UNIQUE KEY uk_rehealth_risk_user_request (user_id, request_id),
    KEY idx_risk_user_created (user_id, evaluated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rehealth_intervention_plan (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    plan_id VARCHAR(128) NOT NULL,
    source_request_id VARCHAR(128),
    feature_schema_version VARCHAR(64),
    model_version VARCHAR(128) NOT NULL,
    scorer_mode VARCHAR(64),
    is_mock TINYINT(1),
    artifact_name VARCHAR(255),
    priority_intervention VARCHAR(1000),
    rationale LONGTEXT,
    expected_impact VARCHAR(1000),
    confidence DOUBLE,
    medical_disclaimer VARCHAR(2000),
    generated_at DATETIME(3) NOT NULL,
    response_json LONGTEXT NOT NULL,
    created_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_rehealth_plan_user_plan (user_id, plan_id),
    KEY idx_plan_user_generated (user_id, generated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rehealth_intervention_contraindication (
    id VARCHAR(64) PRIMARY KEY,
    plan_record_id VARCHAR(64) NOT NULL,
    item_value VARCHAR(1000) NOT NULL,
    sort_order INT NOT NULL,
    CONSTRAINT fk_rehealth_contraindication_plan FOREIGN KEY (plan_record_id)
        REFERENCES rehealth_intervention_plan (id) ON DELETE CASCADE,
    UNIQUE KEY uk_rehealth_contraindication_order (plan_record_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rehealth_intervention_feedback (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    plan_record_id VARCHAR(64) NOT NULL,
    plan_id VARCHAR(128) NOT NULL,
    intervention_id VARCHAR(128) NOT NULL,
    idempotency_key VARCHAR(64) NOT NULL,
    status VARCHAR(64) NOT NULL,
    adherence DOUBLE,
    note VARCHAR(2000),
    checked_at DATETIME(3),
    created_at DATETIME(3) NOT NULL,
    CONSTRAINT fk_rehealth_feedback_plan FOREIGN KEY (plan_record_id) REFERENCES rehealth_intervention_plan (id),
    UNIQUE KEY uk_rehealth_feedback_user_key (user_id, idempotency_key),
    KEY idx_feedback_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rehealth_attribution_event (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    attribution_request_id VARCHAR(64) NOT NULL,
    event_date VARCHAR(32) NOT NULL,
    risk_score DOUBLE NOT NULL,
    intervention_id VARCHAR(128),
    adherence DOUBLE,
    baseline_risk_score DOUBLE,
    created_at DATETIME(3) NOT NULL,
    KEY idx_rehealth_attribution_user_date (user_id, event_date),
    KEY idx_rehealth_attribution_request (attribution_request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rehealth_attribution_result (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    status VARCHAR(64),
    model_version VARCHAR(128),
    request_id VARCHAR(128),
    attribution_mode VARCHAR(64),
    is_mock TINYINT(1),
    provider VARCHAR(128),
    history_days INT,
    min_history_days INT,
    intervention_days INT,
    intervention_data_sufficient TINYINT(1),
    current_risk_score DOUBLE,
    current_risk_level VARCHAR(64),
    current_trend VARCHAR(64),
    individual_att DOUBLE,
    trend_delta DOUBLE,
    adherence_average DOUBLE,
    interpretation LONGTEXT,
    error_code VARCHAR(64),
    retryable TINYINT(1),
    request_json LONGTEXT NOT NULL,
    response_json LONGTEXT NOT NULL,
    created_at DATETIME(3) NOT NULL,
    KEY idx_attribution_result_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rehealth_schema_migration (
    version VARCHAR(64) NOT NULL PRIMARY KEY,
    applied_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO rehealth_schema_migration(version) VALUES ('software-V20260729.1');

CREATE TABLE rehealth_model_request_log (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    request_id VARCHAR(128),
    operation VARCHAR(64) NOT NULL,
    model_version VARCHAR(128),
    outcome VARCHAR(64) NOT NULL,
    error_code VARCHAR(64),
    latency_ms BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL,
    KEY idx_model_request_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rehealth_ai_conversation (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    title VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    summary_text LONGTEXT,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    KEY idx_rehealth_ai_conversation_owner_updated (tenant_id, user_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rehealth_ai_message (
    id VARCHAR(64) PRIMARY KEY,
    conversation_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    request_id VARCHAR(128) NOT NULL,
    role VARCHAR(16) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    provider VARCHAR(128),
    model_version VARCHAR(128),
    retryable TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL,
    CONSTRAINT fk_rehealth_ai_message_conversation FOREIGN KEY (conversation_id)
        REFERENCES rehealth_ai_conversation (id) ON DELETE CASCADE,
    UNIQUE KEY uk_rehealth_ai_message_request_role (conversation_id, request_id, role),
    KEY idx_rehealth_ai_message_owner_created (tenant_id, user_id, created_at),
    KEY idx_rehealth_ai_message_conversation_created (conversation_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO rehealth_schema_migration(version) VALUES ('software-V20260730.1');
