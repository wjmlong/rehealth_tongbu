CREATE TABLE rehealth_patient_profile (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    profile_json LONGTEXT NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_rehealth_profile_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE rehealth_health_interview (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    answers_json LONGTEXT NOT NULL,
    baseline_json LONGTEXT,
    created_at DATETIME(3) NOT NULL,
    KEY idx_health_interview_user_created (user_id, created_at)
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
    generated_at DATETIME(3) NOT NULL,
    response_json LONGTEXT NOT NULL,
    created_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_rehealth_plan_user_plan (user_id, plan_id),
    KEY idx_plan_user_generated (user_id, generated_at)
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
    request_json LONGTEXT NOT NULL,
    response_json LONGTEXT NOT NULL,
    created_at DATETIME(3) NOT NULL,
    KEY idx_attribution_result_user_created (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

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
