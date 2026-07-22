CREATE TABLE rehealth_patient_profile (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    profile_json LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE (user_id)
);

CREATE TABLE rehealth_health_interview (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    answers_json LONGTEXT NOT NULL,
    baseline_json LONGTEXT,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_health_interview_user_created ON rehealth_health_interview(user_id, created_at);

CREATE TABLE rehealth_device_binding (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    device_id VARCHAR(128) NOT NULL,
    device_name VARCHAR(255),
    manufacturer VARCHAR(128),
    model VARCHAR(128),
    firmware_version VARCHAR(128),
    hardware_address_hash VARCHAR(255),
    status VARCHAR(32) NOT NULL,
    bound_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE (user_id, device_id)
);

CREATE TABLE rehealth_cvd_feature_vector (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    request_id VARCHAR(128) NOT NULL,
    payload_json LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_feature_user_created ON rehealth_cvd_feature_vector(user_id, created_at);

CREATE TABLE rehealth_cvd_risk_result (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    request_id VARCHAR(128) NOT NULL,
    risk_score DOUBLE,
    risk_level VARCHAR(64),
    model_version VARCHAR(128),
    response_json LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_risk_user_created ON rehealth_cvd_risk_result(user_id, created_at);

CREATE TABLE rehealth_intervention_plan (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    plan_id VARCHAR(128),
    model_version VARCHAR(128),
    response_json LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_plan_user_created ON rehealth_intervention_plan(user_id, created_at);

CREATE TABLE rehealth_intervention_feedback (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    intervention_id VARCHAR(128) NOT NULL,
    status VARCHAR(64),
    adherence DOUBLE,
    note VARCHAR(2000),
    checked_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_feedback_user_created ON rehealth_intervention_feedback(user_id, created_at);

CREATE TABLE rehealth_attribution_result (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    status VARCHAR(64),
    model_version VARCHAR(128),
    request_json LONGTEXT NOT NULL,
    response_json LONGTEXT NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_attribution_user_created ON rehealth_attribution_result(user_id, created_at);

CREATE TABLE rehealth_model_request_log (
    id VARCHAR(64) PRIMARY KEY,
    user_id VARCHAR(64) NOT NULL,
    request_id VARCHAR(128),
    operation VARCHAR(64) NOT NULL,
    model_version VARCHAR(128),
    outcome VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_model_request_user_created ON rehealth_model_request_log(user_id, created_at);
