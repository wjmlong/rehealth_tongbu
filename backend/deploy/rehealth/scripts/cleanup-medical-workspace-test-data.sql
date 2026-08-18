-- Narrow cleanup for LOCAL_MEDICAL_TEST_SEED MySQL rows.
-- Does not remove the canonical patient-view permission because it is application metadata.
SET NAMES utf8mb4;
SET @seed_actor = 'LOCAL_MEDICAL_TEST_SEED';

START TRANSACTION;

DELETE FROM rehealth_rdi_contribution WHERE source_code=@seed_actor;
DELETE FROM rehealth_rdi_daily_snapshot WHERE calculation_source=@seed_actor;
DELETE FROM rehealth_rhi_daily_snapshot WHERE calculation_source=@seed_actor;
DELETE FROM rehealth_intervention_feedback WHERE idempotency_key LIKE 'mhqa-feedback-%';
DELETE FROM rehealth_intervention_plan
WHERE artifact_name='LOCAL_MEDICAL_TEST_SEED_NOT_A_MODEL' AND is_mock=1;
DELETE FROM rehealth_cvd_risk_result
WHERE artifact_name='LOCAL_MEDICAL_TEST_SEED_NOT_A_MODEL' AND is_mock=1;
DELETE FROM rehealth_cvd_feature_vector
WHERE request_id LIKE 'mhqa-cvd16-%'
  AND JSON_UNQUOTE(JSON_EXTRACT(quality_json,'$.source'))=@seed_actor;
DELETE FROM rehealth_device_binding
WHERE device_id LIKE 'mhqa-device-%'
  AND user_id IN (SELECT id FROM sys_user WHERE create_by=@seed_actor);
DELETE FROM rehealth_patient_profile
WHERE JSON_UNQUOTE(JSON_EXTRACT(profile_json,'$.source'))=@seed_actor;

DELETE FROM sys_user_depart WHERE user_id IN (SELECT id FROM sys_user WHERE create_by=@seed_actor);
DELETE FROM sys_user_role WHERE user_id IN (SELECT id FROM sys_user WHERE create_by=@seed_actor);
DELETE FROM sys_user_tenant WHERE user_id IN (SELECT id FROM sys_user WHERE create_by=@seed_actor);
DELETE FROM sys_user WHERE create_by=@seed_actor;

DELETE FROM sys_role_permission
WHERE id IN (
    LOWER(MD5('LOCAL_MEDICAL_TEST_SEED:role-permission:hospital_admin')),
    LOWER(MD5('LOCAL_MEDICAL_TEST_SEED:role-permission:hospital_doctor'))
);
DELETE FROM sys_depart
WHERE description=@seed_actor
  AND parent_id IS NOT NULL
  AND org_code LIKE 'MHQA%';
DELETE FROM sys_depart
WHERE description=@seed_actor
  AND parent_id IS NULL
  AND org_code LIKE 'MHQA%';
DELETE FROM sys_role
WHERE description LIKE 'LOCAL_MEDICAL_TEST_SEED%'
  AND role_code IN ('hospital_admin','hospital_doctor')
  AND NOT EXISTS (SELECT 1 FROM sys_user_role WHERE sys_user_role.role_id=sys_role.id);
DELETE FROM sys_tenant
WHERE id IN (9261,9262)
  AND name IN ('睿禾滨江心血管健康管理中心（测试）','睿禾南山慢病管理中心（测试）')
  AND NOT EXISTS (SELECT 1 FROM sys_user_tenant WHERE sys_user_tenant.tenant_id=sys_tenant.id);

COMMIT;

SELECT 'remaining_seed_users' AS entity,COUNT(*) AS row_count
FROM sys_user WHERE create_by=@seed_actor
UNION ALL SELECT 'remaining_profiles',COUNT(*) FROM rehealth_patient_profile
WHERE JSON_UNQUOTE(JSON_EXTRACT(profile_json,'$.source'))=@seed_actor
UNION ALL SELECT 'remaining_mock_risks',COUNT(*) FROM rehealth_cvd_risk_result
WHERE artifact_name='LOCAL_MEDICAL_TEST_SEED_NOT_A_MODEL'
UNION ALL SELECT 'remaining_rhi',COUNT(*) FROM rehealth_rhi_daily_snapshot
WHERE calculation_source=@seed_actor;
