-- Insurance business foundation for software_db.
--
-- The website currently reads verified risk data through the existing
-- insurance bridge. These tables establish the tenant-scoped source of truth
-- for the first integration phase and the later PSM/RWE/settlement phases.
-- Do not store raw health telemetry or direct patient identifiers here.

CREATE TABLE IF NOT EXISTS rehealth_insurance_subject (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    tenant_id INT NOT NULL,
    subject_ref CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    rehealth_user_id VARCHAR(64) NOT NULL,
    external_subject_ref_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    enrollment_status VARCHAR(32) NOT NULL DEFAULT 'active',
    consent_status VARCHAR(32) NOT NULL DEFAULT 'pending',
    consent_version VARCHAR(64) NULL,
    consented_at DATETIME(3) NULL,
    source_system VARCHAR(64) NOT NULL DEFAULT 'rehealth',
    source_record_id VARCHAR(128) NULL,
    metadata_json LONGTEXT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_insurance_subject_tenant_ref (tenant_id, subject_ref),
    UNIQUE KEY uk_insurance_subject_tenant_user (tenant_id, rehealth_user_id),
    UNIQUE KEY uk_insurance_subject_source_record (tenant_id, source_system, source_record_id),
    KEY idx_insurance_subject_tenant_status (tenant_id, enrollment_status, consent_status),
    KEY idx_insurance_subject_tenant_updated (tenant_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rehealth_insurance_policy (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    tenant_id INT NOT NULL,
    policy_no VARCHAR(128) NOT NULL,
    product_code VARCHAR(64) NULL,
    product_name VARCHAR(255) NULL,
    policy_type VARCHAR(64) NOT NULL,
    policyholder_subject_ref CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    insured_subject_ref CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    coverage_amount DECIMAL(18,2) NULL,
    premium_amount DECIMAL(18,2) NULL,
    deductible_amount DECIMAL(18,2) NULL,
    waiting_period_days INT NULL,
    effective_on DATE NULL,
    expires_on DATE NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    source_system VARCHAR(64) NOT NULL,
    source_record_id VARCHAR(128) NULL,
    metadata_json LONGTEXT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_insurance_policy_tenant_no (tenant_id, policy_no),
    UNIQUE KEY uk_insurance_policy_source_record (tenant_id, source_system, source_record_id),
    KEY idx_insurance_policy_subject_status (tenant_id, insured_subject_ref, status),
    KEY idx_insurance_policy_effective (tenant_id, effective_on, expires_on)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rehealth_insurance_coverage (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    tenant_id INT NOT NULL,
    policy_id VARCHAR(64) NOT NULL,
    subject_ref CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    coverage_code VARCHAR(64) NOT NULL,
    coverage_name VARCHAR(255) NULL,
    limit_amount DECIMAL(18,2) NULL,
    deductible_amount DECIMAL(18,2) NULL,
    effective_on DATE NULL,
    expires_on DATE NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    source_system VARCHAR(64) NOT NULL,
    source_record_id VARCHAR(128) NULL,
    metadata_json LONGTEXT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_insurance_coverage_source_record (tenant_id, source_system, source_record_id),
    KEY idx_insurance_coverage_policy (tenant_id, policy_id, status),
    KEY idx_insurance_coverage_subject (tenant_id, subject_ref, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rehealth_insurance_consent (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    tenant_id INT NOT NULL,
    subject_ref CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    consent_type VARCHAR(64) NOT NULL,
    consent_version VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'granted',
    granted_at DATETIME(3) NULL,
    revoked_at DATETIME(3) NULL,
    evidence_ref VARCHAR(128) NULL,
    evidence_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    source_system VARCHAR(64) NOT NULL DEFAULT 'rehealth_app',
    source_record_id VARCHAR(128) NULL,
    metadata_json LONGTEXT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_insurance_consent_version (tenant_id, subject_ref, consent_type, consent_version),
    KEY idx_insurance_consent_current (tenant_id, subject_ref, consent_type, status),
    KEY idx_insurance_consent_updated (tenant_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rehealth_insurance_intervention (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    tenant_id INT NOT NULL,
    subject_ref CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    plan_id VARCHAR(128) NOT NULL,
    source_plan_id VARCHAR(64) NULL,
    consent_id VARCHAR(64) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'enrolled',
    enrolled_at DATETIME(3) NULL,
    ended_at DATETIME(3) NULL,
    last_feedback_at DATETIME(3) NULL,
    source_system VARCHAR(64) NOT NULL DEFAULT 'rehealth_app',
    source_record_id VARCHAR(128) NULL,
    metadata_json LONGTEXT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_insurance_intervention_plan (tenant_id, subject_ref, plan_id),
    UNIQUE KEY uk_insurance_intervention_source_record (tenant_id, source_system, source_record_id),
    KEY idx_insurance_intervention_status (tenant_id, status, enrolled_at),
    KEY idx_insurance_intervention_subject (tenant_id, subject_ref, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rehealth_insurance_claim (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    tenant_id INT NOT NULL,
    claim_no VARCHAR(128) NOT NULL,
    policy_id VARCHAR(64) NULL,
    subject_ref CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    claim_type VARCHAR(64) NOT NULL,
    event_on DATE NULL,
    submitted_at DATETIME(3) NULL,
    decided_at DATETIME(3) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'submitted',
    billed_amount DECIMAL(18,2) NULL,
    approved_amount DECIMAL(18,2) NULL,
    paid_amount DECIMAL(18,2) NULL,
    currency CHAR(3) NOT NULL DEFAULT 'CNY',
    coverage_code VARCHAR(64) NULL,
    outcome_code VARCHAR(64) NULL,
    source_system VARCHAR(64) NOT NULL,
    source_record_id VARCHAR(128) NULL,
    metadata_json LONGTEXT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_insurance_claim_tenant_no (tenant_id, claim_no),
    UNIQUE KEY uk_insurance_claim_source_record (tenant_id, source_system, source_record_id),
    KEY idx_insurance_claim_subject_status (tenant_id, subject_ref, status),
    KEY idx_insurance_claim_policy (tenant_id, policy_id, event_on),
    KEY idx_insurance_claim_period (tenant_id, event_on, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rehealth_insurance_study (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    tenant_id INT NOT NULL,
    study_no VARCHAR(128) NOT NULL,
    title VARCHAR(255) NOT NULL,
    period_start DATE NULL,
    period_end DATE NULL,
    population_rule_json LONGTEXT NOT NULL,
    intervention_rule_json LONGTEXT NOT NULL,
    outcome_rule_json LONGTEXT NOT NULL,
    methodology VARCHAR(64) NOT NULL DEFAULT 'psm',
    status VARCHAR(32) NOT NULL DEFAULT 'draft',
    model_version VARCHAR(128) NULL,
    created_by VARCHAR(64) NOT NULL,
    approved_by VARCHAR(64) NULL,
    approved_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_insurance_study_tenant_no (tenant_id, study_no),
    KEY idx_insurance_study_status (tenant_id, status, updated_at),
    KEY idx_insurance_study_period (tenant_id, period_start, period_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rehealth_insurance_study_snapshot (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    tenant_id INT NOT NULL,
    study_id VARCHAR(64) NOT NULL,
    snapshot_version INT NOT NULL,
    snapshot_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    source_watermark VARCHAR(128) NULL,
    cohort_total INT NOT NULL DEFAULT 0,
    treated_total INT NOT NULL DEFAULT 0,
    control_total INT NOT NULL DEFAULT 0,
    source_summary_json LONGTEXT NOT NULL,
    immutable TINYINT(1) NOT NULL DEFAULT 1,
    created_by VARCHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_insurance_snapshot_version (tenant_id, study_id, snapshot_version),
    UNIQUE KEY uk_insurance_snapshot_hash (tenant_id, study_id, snapshot_hash),
    KEY idx_insurance_snapshot_study (tenant_id, study_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rehealth_insurance_study_member (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    tenant_id INT NOT NULL,
    snapshot_id VARCHAR(64) NOT NULL,
    subject_ref CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    cohort_group VARCHAR(32) NOT NULL,
    baseline_risk DECIMAL(10,6) NULL,
    outcome_value DECIMAL(18,6) NULL,
    intervention_status VARCHAR(32) NULL,
    created_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_insurance_snapshot_member (tenant_id, snapshot_id, subject_ref),
    KEY idx_insurance_snapshot_member_group (tenant_id, snapshot_id, cohort_group)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rehealth_insurance_study_result (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    tenant_id INT NOT NULL,
    study_id VARCHAR(64) NOT NULL,
    snapshot_id VARCHAR(64) NOT NULL,
    result_version INT NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'calculated',
    att_estimate DECIMAL(18,8) NULL,
    ci_lower DECIMAL(18,8) NULL,
    ci_upper DECIMAL(18,8) NULL,
    matched_pairs INT NULL,
    balance_json LONGTEXT NULL,
    cost_basis_json LONGTEXT NULL,
    model_version VARCHAR(128) NULL,
    result_json LONGTEXT NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_insurance_study_result_version (tenant_id, study_id, result_version),
    KEY idx_insurance_study_result_status (tenant_id, study_id, status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rehealth_insurance_rwe_report (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    tenant_id INT NOT NULL,
    report_no VARCHAR(128) NOT NULL,
    study_id VARCHAR(64) NOT NULL,
    report_type VARCHAR(64) NOT NULL DEFAULT 'rwe',
    report_version INT NOT NULL DEFAULT 1,
    title VARCHAR(255) NOT NULL,
    period_start DATE NULL,
    period_end DATE NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'draft',
    evidence_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    report_json LONGTEXT NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    submitted_at DATETIME(3) NULL,
    approved_by VARCHAR(64) NULL,
    approved_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_insurance_rwe_report_no (tenant_id, report_no),
    UNIQUE KEY uk_insurance_rwe_report_version (tenant_id, study_id, report_version),
    KEY idx_insurance_rwe_report_status (tenant_id, status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rehealth_insurance_settlement_package (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    tenant_id INT NOT NULL,
    package_no VARCHAR(128) NOT NULL,
    study_id VARCHAR(64) NOT NULL,
    report_id VARCHAR(64) NULL,
    package_version INT NOT NULL DEFAULT 1,
    status VARCHAR(32) NOT NULL DEFAULT 'draft',
    currency CHAR(3) NOT NULL DEFAULT 'CNY',
    estimated_savings DECIMAL(18,2) NULL,
    approved_amount DECIMAL(18,2) NULL,
    snapshot_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    evidence_manifest_json LONGTEXT NOT NULL,
    package_json LONGTEXT NOT NULL,
    content_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    approved_by VARCHAR(64) NULL,
    approved_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_insurance_settlement_package_no (tenant_id, package_no),
    UNIQUE KEY uk_insurance_settlement_package_version (tenant_id, study_id, package_version),
    KEY idx_insurance_settlement_status (tenant_id, status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rehealth_insurance_settlement_approval (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    tenant_id INT NOT NULL,
    package_id VARCHAR(64) NOT NULL,
    action VARCHAR(32) NOT NULL,
    comment VARCHAR(2000) NULL,
    actor_user_id VARCHAR(64) NOT NULL,
    request_id VARCHAR(128) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_insurance_settlement_approval_request (tenant_id, package_id, request_id),
    KEY idx_insurance_settlement_approval_package (tenant_id, package_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rehealth_insurance_audit_event (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    tenant_id INT NOT NULL,
    actor_user_id VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    resource_type VARCHAR(64) NOT NULL,
    resource_id VARCHAR(64) NOT NULL,
    request_id VARCHAR(128) NULL,
    before_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    after_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL,
    metadata_json LONGTEXT NULL,
    created_at DATETIME(3) NOT NULL,
    KEY idx_insurance_audit_tenant_resource (tenant_id, resource_type, resource_id, created_at),
    KEY idx_insurance_audit_tenant_actor (tenant_id, actor_user_id, created_at),
    KEY idx_insurance_audit_request (tenant_id, request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rehealth_schema_migration (
    version VARCHAR(64) NOT NULL PRIMARY KEY,
    applied_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO rehealth_schema_migration(version)
VALUES ('software-V20260812.2');
