-- Complete the insurer workflow foundation without changing the immutable
-- V20260812.2 baseline.  JeecgBoot/MySQL remains the business source of truth;
-- FastAPI may parse files and run PSM, but it must persist through these APIs.

ALTER TABLE rehealth_insurance_study_member
    ADD COLUMN covariate_json LONGTEXT NULL AFTER intervention_status,
    ADD COLUMN source_row_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL AFTER covariate_json;

CREATE TABLE IF NOT EXISTS rehealth_insurance_import_batch (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    tenant_id INT NOT NULL,
    import_type VARCHAR(32) NOT NULL,
    source_system VARCHAR(64) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    content_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'processing',
    total_count INT NOT NULL DEFAULT 0,
    success_count INT NOT NULL DEFAULT 0,
    failure_count INT NOT NULL DEFAULT 0,
    error_json LONGTEXT NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    completed_at DATETIME(3) NULL,
    UNIQUE KEY uk_insurance_import_idempotency (tenant_id, import_type, idempotency_key),
    KEY idx_insurance_import_status (tenant_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rehealth_insurance_study_job (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    tenant_id INT NOT NULL,
    study_id VARCHAR(64) NOT NULL,
    snapshot_id VARCHAR(64) NOT NULL,
    job_type VARCHAR(32) NOT NULL DEFAULT 'psm',
    status VARCHAR(32) NOT NULL DEFAULT 'queued',
    request_id VARCHAR(128) NOT NULL,
    attempt INT NOT NULL DEFAULT 0,
    error_message VARCHAR(1000) NULL,
    result_id VARCHAR(64) NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    started_at DATETIME(3) NULL,
    finished_at DATETIME(3) NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_insurance_study_job_request (tenant_id, study_id, request_id),
    KEY idx_insurance_study_job_status (tenant_id, status, created_at),
    KEY idx_insurance_study_job_snapshot (tenant_id, snapshot_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rehealth_insurance_plan_binding (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    tenant_id INT NOT NULL,
    subject_ref CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    policy_id VARCHAR(64) NOT NULL,
    plan_id VARCHAR(128) NOT NULL,
    consent_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    bound_at DATETIME(3) NOT NULL,
    unbound_at DATETIME(3) NULL,
    source_system VARCHAR(64) NOT NULL DEFAULT 'rehealth_app',
    source_record_id VARCHAR(128) NULL,
    metadata_json LONGTEXT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_insurance_plan_binding (tenant_id, subject_ref, policy_id, plan_id),
    UNIQUE KEY uk_insurance_plan_binding_source (tenant_id, source_system, source_record_id),
    KEY idx_insurance_plan_binding_status (tenant_id, status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rehealth_insurance_intervention_feedback (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    tenant_id INT NOT NULL,
    binding_id VARCHAR(64) NOT NULL,
    subject_ref CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    intervention_id VARCHAR(64) NULL,
    feedback_type VARCHAR(64) NOT NULL,
    occurred_at DATETIME(3) NOT NULL,
    completion_rate DECIMAL(8,6) NULL,
    adherence_score DECIMAL(8,6) NULL,
    outcome_summary_json LONGTEXT NULL,
    source_system VARCHAR(64) NOT NULL DEFAULT 'rehealth_app',
    source_record_id VARCHAR(128) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_insurance_feedback_source (tenant_id, source_system, source_record_id),
    KEY idx_insurance_feedback_binding (tenant_id, binding_id, occurred_at),
    KEY idx_insurance_feedback_subject (tenant_id, subject_ref, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO rehealth_schema_migration(version)
VALUES ('software-V20260813.1');
