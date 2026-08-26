-- 本地联调测试数据（幂等）：把 APP 用户 local_app_9101_01 纳入租户 1000，
-- 使可登录的经理账号能完整体验"认领 → 我的客户 → 转移 → 责任链"闭环。
-- 仅限本地 QA 使用，不得用于生产。

SET NAMES utf8mb4;

INSERT INTO sys_user_tenant (id, user_id, tenant_id, status, create_by, create_time)
SELECT LOWER(REPLACE(UUID(), '-', '')), u.id, 1000, '1', 'local-qa-setup', NOW(3)
FROM sys_user u
WHERE u.username = 'local_app_9101_01'
  AND NOT EXISTS (
      SELECT 1 FROM sys_user_tenant x
      WHERE x.tenant_id = 1000
        AND x.user_id = CONVERT(u.id USING utf8mb3) COLLATE utf8mb3_general_ci
  );

INSERT INTO rehealth_insurance_subject
    (id, tenant_id, subject_ref, rehealth_user_id, enrollment_status, consent_status,
     consent_version, consented_at, source_system, source_record_id, metadata_json, created_at, updated_at)
SELECT LOWER(REPLACE(UUID(), '-', '')), 1000, LOWER(SHA2(CONCAT('1000:', u.id), 256)), u.id,
       'active', 'pending', NULL, NULL, 'local-qa-setup', NULL, NULL, NOW(3), NOW(3)
FROM sys_user u
WHERE u.username = 'local_app_9101_01'
  AND NOT EXISTS (
      SELECT 1 FROM rehealth_insurance_subject x
      WHERE x.tenant_id = 1000
        AND CONVERT(x.rehealth_user_id USING utf8mb3) COLLATE utf8mb3_general_ci = u.id
  );

INSERT INTO rehealth_insurance_project (id, tenant_id, project_no, name, status, start_date, end_date, created_at, updated_at)
SELECT 'default-project-1000', 1000, 'DEFAULT-1000', '默认服务项目', 'active', NULL, NULL, NOW(3), NOW(3)
WHERE NOT EXISTS (SELECT 1 FROM rehealth_insurance_project WHERE id = 'default-project-1000');

INSERT INTO rehealth_insurance_enrollment
    (id, tenant_id, project_id, subject_ref, rehealth_user_id, enrollment_status, consent_status,
     consent_version, consented_at, source_system, source_record_id, metadata_json, created_at, updated_at)
SELECT LEFT(CONCAT('enr-local-', s.id), 64), 1000, 'default-project-1000', s.subject_ref,
       s.rehealth_user_id, 'active', 'pending', NULL, NULL, 'local-qa-setup', NULL, NULL, NOW(3), NOW(3)
FROM rehealth_insurance_subject s
WHERE s.tenant_id = 1000 AND s.source_system = 'local-qa-setup'
  AND NOT EXISTS (
      SELECT 1 FROM rehealth_insurance_enrollment x
      WHERE x.tenant_id = 1000
        AND x.rehealth_user_id = s.rehealth_user_id COLLATE utf8mb4_0900_ai_ci
  );
