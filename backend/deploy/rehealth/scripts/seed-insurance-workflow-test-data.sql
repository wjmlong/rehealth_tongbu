-- Local-only insurer workflow acceptance fixtures.
--
-- This script is intentionally repeatable. All business rows use the explicit
-- source marker LOCAL_INSURANCE_QA and contain synthetic, non-clinical data.
-- Never apply it to staging or production and never use the seeded risk values
-- for medical, underwriting, claim or settlement decisions.

SET NAMES utf8mb4;

SET @seed_actor_id = (
    SELECT id
    FROM sys_user
    WHERE username = @seed_actor
      AND del_flag = 0
    LIMIT 1
);

SET @manager_password_01 = '5b7e5dd2a7afdf0696c03f9b9ffed4d2c9ed320884af19e8f9474bd3a789f2e8';
SET @manager_password_02 = '5b7e5dd2a7afdf0696c03f9b9ffed4d2c9ed320884af19e8d97e8d651f5670e1';

DROP TEMPORARY TABLE IF EXISTS tmp_local_insurance_qa_cohort;
CREATE TEMPORARY TABLE tmp_local_insurance_qa_cohort (
    member_no INT NOT NULL PRIMARY KEY,
    cohort_group VARCHAR(16) NOT NULL,
    age SMALLINT NOT NULL,
    gender VARCHAR(16) NOT NULL,
    height_cm DECIMAL(6,2) NOT NULL,
    weight_kg DECIMAL(6,2) NOT NULL,
    bmi DECIMAL(5,2) NOT NULL,
    risk_score DECIMAL(8,6) NOT NULL,
    risk_level VARCHAR(16) NOT NULL,
    paid_amount DECIMAL(18,2) NOT NULL
);

INSERT INTO tmp_local_insurance_qa_cohort
    (member_no, cohort_group, age, gender, height_cm, weight_kg, bmi, risk_score, risk_level, paid_amount)
VALUES
    (1,  'treated', 44, 'female', 165.00, 63.50, 23.32, 0.310000, 'low',    1800.00),
    (2,  'treated', 49, 'male',   174.00, 77.00, 25.43, 0.460000, 'medium', 3200.00),
    (3,  'treated', 53, 'female', 160.00, 69.40, 27.11, 0.570000, 'medium', 4600.00),
    (4,  'treated', 57, 'male',   172.00, 83.75, 28.31, 0.690000, 'high',   7100.00),
    (5,  'treated', 61, 'female', 158.00, 73.10, 29.28, 0.760000, 'high',   8800.00),
    (6,  'treated', 64, 'male',   170.00, 88.40, 30.59, 0.830000, 'high',  11200.00),
    (7,  'control', 45, 'female', 165.00, 63.55, 23.34, 0.320000, 'low',    4100.00),
    (8,  'control', 48, 'male',   174.00, 76.95, 25.42, 0.450000, 'medium', 6500.00),
    (9,  'control', 54, 'female', 160.00, 69.32, 27.08, 0.580000, 'medium', 9200.00),
    (10, 'control', 56, 'male',   172.00, 83.50, 28.22, 0.680000, 'high',  12800.00),
    (11, 'control', 60, 'female', 158.00, 73.08, 29.27, 0.750000, 'high',  15600.00),
    (12, 'control', 65, 'male',   170.00, 88.45, 30.61, 0.840000, 'high',  19200.00);

START TRANSACTION;

INSERT INTO sys_depart (
    id, parent_id, depart_name, depart_order, org_category, org_type,
    org_code, status, del_flag, create_by, create_time, update_by,
    update_time, tenant_id, iz_leaf
)
VALUES
    ('iqdep000000000000000000000001', NULL, '本地保险测试公司', 1, '1', '1',
     'A01', '1', '0', @seed_actor_id, TIMESTAMP('2026-08-13 09:00:00'),
     @seed_actor_id, TIMESTAMP('2026-08-13 09:00:00'), @seed_tenant_id, 0),
    ('iqdep000000000000000000000002', 'iqdep000000000000000000000001', '健康险一部', 1, '2', '2',
     'A01A01', '1', '0', @seed_actor_id, TIMESTAMP('2026-08-13 09:00:00'),
     @seed_actor_id, TIMESTAMP('2026-08-13 09:00:00'), @seed_tenant_id, 1),
    ('iqdep000000000000000000000003', 'iqdep000000000000000000000001', '健康险二部', 2, '2', '2',
     'A01A02', '1', '0', @seed_actor_id, TIMESTAMP('2026-08-13 09:00:00'),
     @seed_actor_id, TIMESTAMP('2026-08-13 09:00:00'), @seed_tenant_id, 1)
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id),
    depart_name = VALUES(depart_name),
    org_code = VALUES(org_code),
    status = VALUES(status),
    del_flag = VALUES(del_flag),
    update_by = VALUES(update_by),
    update_time = VALUES(update_time),
    tenant_id = VALUES(tenant_id),
    iz_leaf = VALUES(iz_leaf);

INSERT INTO sys_user (
    id, username, realname, password, salt, sex, status, del_flag,
    create_by, create_time, update_by, update_time, user_identity,
    login_tenant_id, sort
)
VALUES
    ('iqmgr000000000000000000000001', 'local_insurance_manager_01', '保险测试经理一', @manager_password_01, 'QA260813', 1, 1, 0,
     @seed_actor_id, TIMESTAMP('2026-08-13 09:00:00'), @seed_actor_id, TIMESTAMP('2026-08-13 09:00:00'), 1, @seed_tenant_id, 8001),
    ('iqmgr000000000000000000000002', 'local_insurance_manager_02', '保险测试经理二', @manager_password_02, 'QA260813', 2, 1, 0,
     @seed_actor_id, TIMESTAMP('2026-08-13 09:00:00'), @seed_actor_id, TIMESTAMP('2026-08-13 09:00:00'), 1, @seed_tenant_id, 8002)
ON DUPLICATE KEY UPDATE
    realname = VALUES(realname),
    password = VALUES(password),
    salt = VALUES(salt),
    status = VALUES(status),
    del_flag = VALUES(del_flag),
    login_tenant_id = VALUES(login_tenant_id),
    update_by = VALUES(update_by),
    update_time = VALUES(update_time),
    login_tenant_id = VALUES(login_tenant_id),
    sort = VALUES(sort);

INSERT INTO sys_user_tenant (
    id, user_id, tenant_id, status, create_by, create_time, update_by, update_time
)
VALUES
    ('iqmt000000000000000000000001', 'iqmgr000000000000000000000001', @seed_tenant_id, '1', @seed_actor_id, TIMESTAMP('2026-08-13 09:00:00'), @seed_actor_id, TIMESTAMP('2026-08-13 09:00:00')),
    ('iqmt000000000000000000000002', 'iqmgr000000000000000000000002', @seed_tenant_id, '1', @seed_actor_id, TIMESTAMP('2026-08-13 09:00:00'), @seed_actor_id, TIMESTAMP('2026-08-13 09:00:00'))
ON DUPLICATE KEY UPDATE
    tenant_id = VALUES(tenant_id), status = VALUES(status), update_by = VALUES(update_by), update_time = VALUES(update_time);

INSERT INTO sys_user_depart (ID, user_id, dep_id)
VALUES
    ('iqmd000000000000000000000001', 'iqmgr000000000000000000000001', 'iqdep000000000000000000000002'),
    ('iqmd000000000000000000000002', 'iqmgr000000000000000000000002', 'iqdep000000000000000000000003')
ON DUPLICATE KEY UPDATE dep_id = VALUES(dep_id);

INSERT INTO sys_user_role (id, user_id, role_id, tenant_id)
SELECT 'iqmr000000000000000000000001', 'iqmgr000000000000000000000001', id, @seed_tenant_id
FROM sys_role WHERE role_code = 'insurer_analyst'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id), tenant_id = VALUES(tenant_id);

INSERT INTO sys_user_role (id, user_id, role_id, tenant_id)
SELECT 'iqmr000000000000000000000002', 'iqmgr000000000000000000000002', id, @seed_tenant_id
FROM sys_role WHERE role_code = 'insurer_analyst'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id), tenant_id = VALUES(tenant_id);

-- Activates department/subject scope enforcement while retaining analyst
-- permissions for the local workflow demo accounts.
INSERT INTO sys_user_role (id, user_id, role_id, tenant_id)
SELECT 'iqms000000000000000000000001', 'iqmgr000000000000000000000001', id, @seed_tenant_id
FROM sys_role WHERE role_code = 'insurance_department_manager'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id), tenant_id = VALUES(tenant_id);

INSERT INTO sys_user_role (id, user_id, role_id, tenant_id)
SELECT 'iqms000000000000000000000002', 'iqmgr000000000000000000000002', id, @seed_tenant_id
FROM sys_role WHERE role_code = 'insurance_department_manager'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id), tenant_id = VALUES(tenant_id);

INSERT INTO sys_user (
    id, username, realname, password, salt, sex, status, del_flag,
    create_by, create_time, update_by, update_time, user_identity,
    login_tenant_id, sort
)
SELECT
    CONCAT('iq', LPAD(member_no, 30, '0')),
    CONCAT('local_insurance_qa_', LPAD(member_no, 2, '0')),
    CONCAT('合成测试成员', LPAD(member_no, 2, '0')),
    NULL,
    NULL,
    CASE WHEN gender = 'male' THEN 1 ELSE 2 END,
    1,
    0,
    @seed_actor_id,
    TIMESTAMP('2026-08-13 09:00:00'),
    @seed_actor_id,
    TIMESTAMP('2026-08-13 09:00:00'),
    1,
    @seed_tenant_id,
    9000 + member_no
FROM tmp_local_insurance_qa_cohort
ON DUPLICATE KEY UPDATE
    realname = VALUES(realname),
    sex = VALUES(sex),
    status = VALUES(status),
    del_flag = VALUES(del_flag),
    login_tenant_id = VALUES(login_tenant_id),
    update_by = VALUES(update_by),
    update_time = VALUES(update_time),
    sort = VALUES(sort);

INSERT INTO sys_user_tenant (
    id, user_id, tenant_id, status, create_by, create_time, update_by, update_time
)
SELECT
    CONCAT('it', LPAD(member_no, 30, '0')),
    CONCAT('iq', LPAD(member_no, 30, '0')),
    @seed_tenant_id,
    '1',
    @seed_actor_id,
    TIMESTAMP('2026-08-13 09:00:00'),
    @seed_actor_id,
    TIMESTAMP('2026-08-13 09:00:00')
FROM tmp_local_insurance_qa_cohort
ON DUPLICATE KEY UPDATE
    tenant_id = VALUES(tenant_id),
    status = VALUES(status),
    update_by = VALUES(update_by),
    update_time = VALUES(update_time);

INSERT INTO rehealth_patient_profile (
    id, user_id, name, gender, age, height_cm, weight_kg, bmi,
    family_history, smoking, drinking, diabetes_history,
    hypertension_history, profile_version, created_at, updated_at
)
SELECT
    CONCAT('ip', LPAD(member_no, 30, '0')),
    CONCAT('iq', LPAD(member_no, 30, '0')),
    CONCAT('合成测试成员', LPAD(member_no, 2, '0')),
    gender,
    age,
    height_cm,
    weight_kg,
    bmi,
    CASE WHEN member_no IN (3, 4, 5, 6, 9, 10, 11, 12) THEN 1 ELSE 0 END,
    CASE WHEN member_no IN (4, 6, 10, 12) THEN 1 ELSE 0 END,
    0,
    CASE WHEN member_no IN (5, 6, 11, 12) THEN 1 ELSE 0 END,
    CASE WHEN member_no IN (3, 4, 5, 6, 9, 10, 11, 12) THEN 1 ELSE 0 END,
    1,
    TIMESTAMP('2025-12-01 09:00:00'),
    TIMESTAMP('2025-12-01 09:00:00')
FROM tmp_local_insurance_qa_cohort
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    gender = VALUES(gender),
    age = VALUES(age),
    height_cm = VALUES(height_cm),
    weight_kg = VALUES(weight_kg),
    bmi = VALUES(bmi),
    family_history = VALUES(family_history),
    smoking = VALUES(smoking),
    drinking = VALUES(drinking),
    diabetes_history = VALUES(diabetes_history),
    hypertension_history = VALUES(hypertension_history),
    updated_at = VALUES(updated_at);

INSERT INTO rehealth_cvd_feature_vector (
    id, user_id, request_id, feature_schema_version, feature_json,
    quality_json, payload_json, created_at
)
SELECT
    CONCAT('if', LPAD(member_no, 30, '0')),
    CONCAT('iq', LPAD(member_no, 30, '0')),
    CONCAT('local-insurance-qa-risk-', LPAD(member_no, 2, '0')),
    'cvd-16-v1',
    JSON_OBJECT(
        'age', age,
        'gender', gender,
        'bmi', bmi,
        'familyHistory', CASE WHEN member_no IN (3, 4, 5, 6, 9, 10, 11, 12) THEN 1 ELSE 0 END,
        'smoking', CASE WHEN member_no IN (4, 6, 10, 12) THEN 1 ELSE 0 END,
        'diabetesHistory', CASE WHEN member_no IN (5, 6, 11, 12) THEN 1 ELSE 0 END,
        'hypertensionHistory', CASE WHEN member_no IN (3, 4, 5, 6, 9, 10, 11, 12) THEN 1 ELSE 0 END
    ),
    JSON_OBJECT(
        'fixture', true,
        'source', 'LOCAL_INSURANCE_QA',
        'clinicalUseAllowed', false
    ),
    JSON_OBJECT(
        'fixture', true,
        'sourceSystem', 'LOCAL_INSURANCE_QA',
        'memberNo', member_no
    ),
    DATE_ADD(TIMESTAMP('2025-12-15 08:00:00'), INTERVAL member_no MINUTE)
FROM tmp_local_insurance_qa_cohort
ON DUPLICATE KEY UPDATE
    feature_json = VALUES(feature_json),
    quality_json = VALUES(quality_json),
    payload_json = VALUES(payload_json),
    created_at = VALUES(created_at);

INSERT INTO rehealth_cvd_risk_result (
    id, feature_vector_id, user_id, request_id, feature_schema_version,
    model_version, scorer_mode, is_mock, artifact_name, fallback_reason,
    contribution_method, risk_score, risk_level, contribution_json,
    missing_fields_json, quality_warnings_json, summary, response_json,
    evaluated_at, created_at
)
SELECT
    CONCAT('ir', LPAD(member_no, 30, '0')),
    CONCAT('if', LPAD(member_no, 30, '0')),
    CONCAT('iq', LPAD(member_no, 30, '0')),
    CONCAT('local-insurance-qa-risk-', LPAD(member_no, 2, '0')),
    'cvd-16-v1',
    'local-qa-seeded-nonclinical-v1',
    'local_qa_fixture',
    0,
    'LOCAL_INSURANCE_QA_NOT_A_MODEL',
    NULL,
    'fixture',
    risk_score,
    risk_level,
    JSON_OBJECT(
        'fixture', true,
        'factors', JSON_ARRAY('synthetic age', 'synthetic BMI')
    ),
    JSON_ARRAY(),
    JSON_ARRAY('LOCAL QA FIXTURE - NON-CLINICAL - DO NOT USE FOR DECISIONS'),
    '本地保险工作流合成测试结果，不可用于医疗、核保、理赔或结算决策。',
    JSON_OBJECT(
        'fixture', true,
        'sourceSystem', 'LOCAL_INSURANCE_QA',
        'clinicalUseAllowed', false,
        'riskScore', risk_score,
        'riskLevel', risk_level,
        'modelVersion', 'local-qa-seeded-nonclinical-v1'
    ),
    DATE_ADD(TIMESTAMP('2025-12-15 08:00:00'), INTERVAL member_no MINUTE),
    DATE_ADD(TIMESTAMP('2025-12-15 08:00:00'), INTERVAL member_no MINUTE)
FROM tmp_local_insurance_qa_cohort
ON DUPLICATE KEY UPDATE
    model_version = VALUES(model_version),
    scorer_mode = VALUES(scorer_mode),
    is_mock = VALUES(is_mock),
    artifact_name = VALUES(artifact_name),
    risk_score = VALUES(risk_score),
    risk_level = VALUES(risk_level),
    contribution_json = VALUES(contribution_json),
    quality_warnings_json = VALUES(quality_warnings_json),
    summary = VALUES(summary),
    response_json = VALUES(response_json),
    evaluated_at = VALUES(evaluated_at),
    created_at = VALUES(created_at);

INSERT INTO rehealth_insurance_subject (
    id, tenant_id, subject_ref, rehealth_user_id,
    external_subject_ref_hash, enrollment_status, consent_status,
    consent_version, consented_at, source_system, source_record_id,
    metadata_json, created_at, updated_at
)
SELECT
    CONCAT('is', LPAD(member_no, 30, '0')),
    @seed_tenant_id,
    SHA2(CONCAT(@seed_tenant_id, ':', CONCAT('iq', LPAD(member_no, 30, '0'))), 256),
    CONCAT('iq', LPAD(member_no, 30, '0')),
    SHA2(CONCAT('LOCAL_INSURANCE_QA:', member_no), 256),
    'active',
    'granted',
    'local-qa-v1',
    TIMESTAMP('2025-01-01 09:00:00'),
    'LOCAL_INSURANCE_QA',
    CONCAT('subject-', LPAD(member_no, 2, '0')),
    JSON_OBJECT(
        'fixture', true,
        'cohortGroup', cohort_group,
        'clinicalUseAllowed', false
    ),
    TIMESTAMP('2025-01-01 09:00:00'),
    TIMESTAMP('2026-08-13 09:00:00')
FROM tmp_local_insurance_qa_cohort
ON DUPLICATE KEY UPDATE
    subject_ref = VALUES(subject_ref),
    rehealth_user_id = VALUES(rehealth_user_id),
    enrollment_status = VALUES(enrollment_status),
    consent_status = VALUES(consent_status),
    consent_version = VALUES(consent_version),
    consented_at = VALUES(consented_at),
    metadata_json = VALUES(metadata_json),
    updated_at = VALUES(updated_at);

INSERT INTO rehealth_insurance_subject_manager (
    id, tenant_id, manager_user_id, department_id, subject_ref,
    status, source_system, created_at, updated_at
)
SELECT
    CONCAT('iqassign', LPAD(member_no, 24, '0')),
    @seed_tenant_id,
    CASE WHEN member_no <= 6 THEN 'iqmgr000000000000000000000001' ELSE 'iqmgr000000000000000000000002' END,
    CASE WHEN member_no <= 6 THEN 'iqdep000000000000000000000002' ELSE 'iqdep000000000000000000000003' END,
    SHA2(CONCAT(@seed_tenant_id, ':', CONCAT('iq', LPAD(member_no, 30, '0'))), 256),
    'active',
    'LOCAL_INSURANCE_QA',
    TIMESTAMP('2026-08-13 09:00:00'),
    TIMESTAMP('2026-08-13 09:00:00')
FROM tmp_local_insurance_qa_cohort
ON DUPLICATE KEY UPDATE
    department_id = VALUES(department_id),
    status = VALUES(status),
    updated_at = VALUES(updated_at);

INSERT INTO rehealth_insurance_policy (
    id, tenant_id, policy_no, product_code, product_name, policy_type,
    policyholder_subject_ref, insured_subject_ref, coverage_amount,
    premium_amount, deductible_amount, waiting_period_days, effective_on,
    expires_on, status, source_system, source_record_id, metadata_json,
    created_at, updated_at
)
SELECT
    CONCAT('iy', LPAD(member_no, 30, '0')),
    @seed_tenant_id,
    CONCAT('LOCAL-QA-POLICY-', LPAD(member_no, 4, '0')),
    'LOCAL-QA-CVD',
    '本地合成心血管健康管理计划',
    'health_management',
    SHA2(CONCAT(@seed_tenant_id, ':', CONCAT('iq', LPAD(member_no, 30, '0'))), 256),
    SHA2(CONCAT(@seed_tenant_id, ':', CONCAT('iq', LPAD(member_no, 30, '0'))), 256),
    200000.00,
    1200.00 + member_no * 20,
    500.00,
    30,
    DATE('2025-01-01'),
    DATE('2026-12-31'),
    'active',
    'LOCAL_INSURANCE_QA',
    CONCAT('policy-', LPAD(member_no, 2, '0')),
    JSON_OBJECT('fixture', true, 'clinicalUseAllowed', false),
    TIMESTAMP('2025-01-01 09:00:00'),
    TIMESTAMP('2026-08-13 09:00:00')
FROM tmp_local_insurance_qa_cohort
ON DUPLICATE KEY UPDATE
    product_code = VALUES(product_code),
    product_name = VALUES(product_name),
    policy_type = VALUES(policy_type),
    insured_subject_ref = VALUES(insured_subject_ref),
    coverage_amount = VALUES(coverage_amount),
    premium_amount = VALUES(premium_amount),
    effective_on = VALUES(effective_on),
    expires_on = VALUES(expires_on),
    status = VALUES(status),
    metadata_json = VALUES(metadata_json),
    updated_at = VALUES(updated_at);

INSERT INTO rehealth_insurance_coverage (
    id, tenant_id, policy_id, subject_ref, coverage_code, coverage_name,
    limit_amount, deductible_amount, effective_on, expires_on, status,
    source_system, source_record_id, metadata_json, created_at, updated_at
)
SELECT
    CONCAT('ic', LPAD(member_no, 30, '0')),
    @seed_tenant_id,
    CONCAT('iy', LPAD(member_no, 30, '0')),
    SHA2(CONCAT(@seed_tenant_id, ':', CONCAT('iq', LPAD(member_no, 30, '0'))), 256),
    'CVD-MGMT',
    '心血管健康管理测试保障',
    200000.00,
    500.00,
    DATE('2025-01-01'),
    DATE('2026-12-31'),
    'active',
    'LOCAL_INSURANCE_QA',
    CONCAT('coverage-', LPAD(member_no, 2, '0')),
    JSON_OBJECT('fixture', true),
    TIMESTAMP('2025-01-01 09:00:00'),
    TIMESTAMP('2026-08-13 09:00:00')
FROM tmp_local_insurance_qa_cohort
ON DUPLICATE KEY UPDATE
    policy_id = VALUES(policy_id),
    subject_ref = VALUES(subject_ref),
    limit_amount = VALUES(limit_amount),
    status = VALUES(status),
    metadata_json = VALUES(metadata_json),
    updated_at = VALUES(updated_at);

INSERT INTO rehealth_insurance_consent (
    id, tenant_id, subject_ref, consent_type, consent_version, status,
    granted_at, revoked_at, evidence_ref, evidence_hash, source_system,
    source_record_id, metadata_json, created_at, updated_at
)
SELECT
    CONCAT('in', LPAD(member_no, 30, '0')),
    @seed_tenant_id,
    SHA2(CONCAT(@seed_tenant_id, ':', CONCAT('iq', LPAD(member_no, 30, '0'))), 256),
    'insurance_analytics',
    'local-qa-v1',
    'granted',
    TIMESTAMP('2025-01-01 09:00:00'),
    NULL,
    CONCAT('LOCAL-QA-CONSENT-', LPAD(member_no, 2, '0')),
    SHA2(CONCAT('LOCAL_INSURANCE_QA_CONSENT:', member_no), 256),
    'LOCAL_INSURANCE_QA',
    CONCAT('consent-', LPAD(member_no, 2, '0')),
    JSON_OBJECT('fixture', true, 'rawHealthDataShared', false),
    TIMESTAMP('2025-01-01 09:00:00'),
    TIMESTAMP('2026-08-13 09:00:00')
FROM tmp_local_insurance_qa_cohort
ON DUPLICATE KEY UPDATE
    status = VALUES(status),
    granted_at = VALUES(granted_at),
    revoked_at = VALUES(revoked_at),
    evidence_ref = VALUES(evidence_ref),
    evidence_hash = VALUES(evidence_hash),
    metadata_json = VALUES(metadata_json),
    updated_at = VALUES(updated_at);

INSERT INTO rehealth_intervention_plan (
    id, user_id, plan_id, source_request_id, feature_schema_version,
    model_version, scorer_mode, is_mock, artifact_name,
    priority_intervention, rationale, expected_impact, confidence,
    medical_disclaimer, generated_at, response_json, created_at
)
SELECT
    CONCAT('ig', LPAD(member_no, 30, '0')),
    CONCAT('iq', LPAD(member_no, 30, '0')),
    CONCAT('local-insurance-qa-plan-', LPAD(member_no, 2, '0')),
    CONCAT('local-insurance-qa-risk-', LPAD(member_no, 2, '0')),
    'cvd-16-v1',
    'local-qa-seeded-nonclinical-v1',
    'local_qa_fixture',
    1,
    'LOCAL_INSURANCE_QA_NOT_A_MODEL',
    '合成本地测试：每周完成经授权的健康管理任务',
    '仅用于验证保险干预关联与 PSM 分组。',
    '仅验证流程，不代表健康改善或临床获益。',
    0.50,
    '合成测试计划，不构成医疗建议，不替代医生诊疗。',
    DATE_ADD(TIMESTAMP('2026-01-05 09:00:00'), INTERVAL member_no MINUTE),
    JSON_OBJECT(
        'fixture', true,
        'sourceSystem', 'LOCAL_INSURANCE_QA',
        'clinicalUseAllowed', false
    ),
    DATE_ADD(TIMESTAMP('2026-01-05 09:00:00'), INTERVAL member_no MINUTE)
FROM tmp_local_insurance_qa_cohort
WHERE cohort_group = 'treated'
ON DUPLICATE KEY UPDATE
    source_request_id = VALUES(source_request_id),
    model_version = VALUES(model_version),
    scorer_mode = VALUES(scorer_mode),
    is_mock = VALUES(is_mock),
    priority_intervention = VALUES(priority_intervention),
    rationale = VALUES(rationale),
    expected_impact = VALUES(expected_impact),
    medical_disclaimer = VALUES(medical_disclaimer),
    response_json = VALUES(response_json),
    generated_at = VALUES(generated_at);

INSERT INTO rehealth_insurance_intervention (
    id, tenant_id, subject_ref, plan_id, source_plan_id, consent_id,
    status, enrolled_at, ended_at, last_feedback_at, source_system,
    source_record_id, metadata_json, created_at, updated_at
)
SELECT
    CONCAT('ii', LPAD(member_no, 30, '0')),
    @seed_tenant_id,
    SHA2(CONCAT(@seed_tenant_id, ':', CONCAT('iq', LPAD(member_no, 30, '0'))), 256),
    CONCAT('local-insurance-qa-plan-', LPAD(member_no, 2, '0')),
    CONCAT('ig', LPAD(member_no, 30, '0')),
    CONCAT('in', LPAD(member_no, 30, '0')),
    'active',
    DATE_ADD(TIMESTAMP('2026-01-10 09:00:00'), INTERVAL member_no MINUTE),
    NULL,
    DATE_ADD(TIMESTAMP('2026-07-31 09:00:00'), INTERVAL member_no MINUTE),
    'LOCAL_INSURANCE_QA',
    CONCAT('intervention-', LPAD(member_no, 2, '0')),
    JSON_OBJECT(
        'fixture', true,
        'completionRate', ROUND(0.70 + member_no * 0.03, 2),
        'clinicalUseAllowed', false
    ),
    DATE_ADD(TIMESTAMP('2026-01-10 09:00:00'), INTERVAL member_no MINUTE),
    TIMESTAMP('2026-08-13 09:00:00')
FROM tmp_local_insurance_qa_cohort
WHERE cohort_group = 'treated'
ON DUPLICATE KEY UPDATE
    plan_id = VALUES(plan_id),
    source_plan_id = VALUES(source_plan_id),
    consent_id = VALUES(consent_id),
    status = VALUES(status),
    enrolled_at = VALUES(enrolled_at),
    ended_at = VALUES(ended_at),
    last_feedback_at = VALUES(last_feedback_at),
    metadata_json = VALUES(metadata_json),
    updated_at = VALUES(updated_at);

INSERT INTO rehealth_insurance_claim (
    id, tenant_id, claim_no, policy_id, subject_ref, claim_type,
    event_on, submitted_at, decided_at, status, billed_amount,
    approved_amount, paid_amount, currency, coverage_code, outcome_code,
    source_system, source_record_id, metadata_json, created_at, updated_at
)
SELECT
    CONCAT('il', LPAD(member_no, 30, '0')),
    @seed_tenant_id,
    CONCAT('LOCAL-QA-CLAIM-', LPAD(member_no, 4, '0')),
    CONCAT('iy', LPAD(member_no, 30, '0')),
    SHA2(CONCAT(@seed_tenant_id, ':', CONCAT('iq', LPAD(member_no, 30, '0'))), 256),
    CASE WHEN member_no % 3 = 0 THEN 'outpatient' WHEN member_no % 3 = 1 THEN 'inpatient' ELSE 'chronic' END,
    ADDDATE(DATE('2026-02-01'), member_no * 10),
    ADDDATE(TIMESTAMP('2026-02-02 10:00:00'), member_no * 10),
    ADDDATE(TIMESTAMP('2026-02-08 16:00:00'), member_no * 10),
    'paid',
    paid_amount * 1.20,
    paid_amount,
    paid_amount,
    'CNY',
    'CVD-MGMT',
    'paid',
    'LOCAL_INSURANCE_QA',
    CONCAT('claim-', LPAD(member_no, 2, '0')),
    JSON_OBJECT(
        'fixture', true,
        'cohortGroup', cohort_group,
        'clinicalUseAllowed', false
    ),
    ADDDATE(TIMESTAMP('2026-02-02 10:00:00'), member_no * 10),
    TIMESTAMP('2026-08-13 09:00:00')
FROM tmp_local_insurance_qa_cohort
ON DUPLICATE KEY UPDATE
    policy_id = VALUES(policy_id),
    subject_ref = VALUES(subject_ref),
    claim_type = VALUES(claim_type),
    event_on = VALUES(event_on),
    submitted_at = VALUES(submitted_at),
    decided_at = VALUES(decided_at),
    status = VALUES(status),
    billed_amount = VALUES(billed_amount),
    approved_amount = VALUES(approved_amount),
    paid_amount = VALUES(paid_amount),
    metadata_json = VALUES(metadata_json),
    updated_at = VALUES(updated_at);

INSERT INTO rehealth_insurance_study (
    id, tenant_id, study_no, title, period_start, period_end,
    population_rule_json, intervention_rule_json, outcome_rule_json,
    methodology, status, model_version, created_by, approved_by,
    approved_at, created_at, updated_at
)
VALUES (
    'local-insurance-qa-study-2026',
    @seed_tenant_id,
    'LOCAL-QA-PSM-2026',
    '本地保险 PSM 工作流合成验收研究',
    DATE('2026-01-01'),
    DATE('2026-08-13'),
    JSON_OBJECT(
        'sourceSystem', 'LOCAL_INSURANCE_QA',
        'consentStatus', 'granted',
        'activePolicyRequired', true,
        'clinicalUseAllowed', false
    ),
    JSON_OBJECT(
        'treated', 'active intervention enrolled in study period',
        'control', 'no active intervention in study period'
    ),
    JSON_OBJECT(
        'metric', 'paid claim amount in CNY',
        'warning', 'synthetic local QA values only'
    ),
    'psm',
    'draft',
    'local-qa-psm-v1',
    @seed_actor_id,
    NULL,
    NULL,
    TIMESTAMP('2026-08-13 09:00:00'),
    TIMESTAMP('2026-08-13 09:00:00')
)
ON DUPLICATE KEY UPDATE
    id = VALUES(id);

COMMIT;

DROP TEMPORARY TABLE IF EXISTS tmp_local_insurance_qa_cohort;
