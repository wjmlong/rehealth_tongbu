-- 存量归属数据迁移：旧 rehealth_insurance_subject_manager → 新表首条 PRIMARY 历史。
-- 迁移规则：
--   1. 每个有被保人数据的租户先建一个“默认服务项目”；
--   2. 每条 rehealth_insurance_subject 生成一条默认项目下的参与记录（enrollment）；
--   3. 旧归属行迁移为 user_assignment：
--      - 同一 (tenant_id, subject_ref) 下 updated_at 最新且 status='active' 的行
--        → 新的 active PRIMARY（end_time 为空，责任链延续）；
--      - 其余行（disabled 或已被更新的 active）→ status='ended'、
--        end_time=旧行 updated_at，作为历史保留；
--   4. 每行迁移写入 assignment_change_log（before_json 保留旧行快照）；
--   5. 全部使用确定性 ID + ON DUPLICATE KEY UPDATE，脚本可重复执行（幂等）；
--      唯一索引 uk_assignment_primary_active 兜底并发/重复风险。
-- 执行前必须先跑 backend/deploy/rehealth/scripts/precheck-legacy-assignment-data.sql
-- 体检并人工确认报告；迁移后执行脚本末尾的人工校验语句。

-- 1. 默认服务项目（确定性 ID，幂等）
INSERT INTO rehealth_insurance_project
    (id, tenant_id, project_no, name, status, start_date, end_date, created_at, updated_at)
SELECT CONCAT('default-project-', t.tenant_id),
       t.tenant_id,
       CONCAT('DEFAULT-', t.tenant_id),
       '默认服务项目',
       'active',
       NULL,
       NULL,
       NOW(3),
       NOW(3)
FROM (SELECT DISTINCT tenant_id FROM rehealth_insurance_subject) t
ON DUPLICATE KEY UPDATE name = VALUES(name);

-- 2. subject → enrollment（确定性 ID：enr-legacy-<subject.id>，幂等）
INSERT INTO rehealth_insurance_enrollment
    (id, tenant_id, project_id, subject_ref, rehealth_user_id, enrollment_status,
     consent_status, consent_version, consented_at, source_system, source_record_id,
     metadata_json, created_at, updated_at)
SELECT LEFT(CONCAT('enr-legacy-', s.id), 64),
       s.tenant_id,
       CONCAT('default-project-', s.tenant_id),
       s.subject_ref,
       s.rehealth_user_id,
       s.enrollment_status,
       s.consent_status,
       s.consent_version,
       s.consented_at,
       s.source_system,
       s.source_record_id,
       s.metadata_json,
       s.created_at,
       s.updated_at
FROM rehealth_insurance_subject s
ON DUPLICATE KEY UPDATE
    enrollment_status = VALUES(enrollment_status),
    consent_status = VALUES(consent_status),
    consent_version = VALUES(consent_version),
    consented_at = VALUES(consented_at),
    metadata_json = VALUES(metadata_json),
    updated_at = VALUES(updated_at);

-- 3a. 每个 (tenant_id, subject_ref) 下 updated_at 最新的 active 行 → active PRIMARY
INSERT INTO rehealth_insurance_user_assignment
    (id, tenant_id, enrollment_id, employee_id, role_type, start_time, end_time,
     status, start_time_source, change_reason, operator_id, created_at)
SELECT LEFT(CONCAT('asg-legacy-', sm.id), 64),
       sm.tenant_id,
       e.id,
       sm.manager_user_id,
       'PRIMARY',
       sm.created_at,
       NULL,
       'active',
       'legacy',
       'legacy_migration',
       NULL,
       sm.created_at
FROM rehealth_insurance_subject_manager sm
JOIN rehealth_insurance_enrollment e
  ON e.tenant_id = sm.tenant_id AND e.subject_ref = sm.subject_ref
WHERE sm.status = 'active'
  AND NOT EXISTS (
      SELECT 1
      FROM rehealth_insurance_subject_manager newer
      WHERE newer.tenant_id = sm.tenant_id
        AND newer.subject_ref = sm.subject_ref
        AND (newer.updated_at > sm.updated_at
             OR (newer.updated_at = sm.updated_at AND newer.id > sm.id))
  )
ON DUPLICATE KEY UPDATE id = VALUES(id);

-- 3b. 其余行（disabled 或已被更新的 active）→ ended 历史
INSERT INTO rehealth_insurance_user_assignment
    (id, tenant_id, enrollment_id, employee_id, role_type, start_time, end_time,
     status, start_time_source, change_reason, operator_id, created_at)
SELECT LEFT(CONCAT('asg-legacy-', sm.id), 64),
       sm.tenant_id,
       e.id,
       sm.manager_user_id,
       'PRIMARY',
       sm.created_at,
       COALESCE(sm.updated_at, sm.created_at),
       'ended',
       'legacy',
       CASE
           WHEN sm.status <> 'active' THEN 'legacy_migration'
           ELSE 'migration_conflict_resolved'
       END,
       NULL,
       sm.created_at
FROM rehealth_insurance_subject_manager sm
JOIN rehealth_insurance_enrollment e
  ON e.tenant_id = sm.tenant_id AND e.subject_ref = sm.subject_ref
WHERE sm.status <> 'active'
   OR EXISTS (
      SELECT 1
      FROM rehealth_insurance_subject_manager newer
      WHERE newer.tenant_id = sm.tenant_id
        AND newer.subject_ref = sm.subject_ref
        AND (newer.updated_at > sm.updated_at
             OR (newer.updated_at = sm.updated_at AND newer.id > sm.id))
   )
ON DUPLICATE KEY UPDATE id = VALUES(id);

-- 4. 每行迁移写入变更日志（before_json 保留旧表行快照，含 department_id 等已下线的字段）
INSERT INTO rehealth_insurance_assignment_change_log
    (id, tenant_id, enrollment_id, assignment_id, change_type,
     before_json, after_json, reason, operator_id, created_at)
SELECT LEFT(CONCAT('asglog-legacy-', a.id), 64),
       a.tenant_id,
       a.enrollment_id,
       a.id,
       a.change_reason,
       JSON_OBJECT('legacy', JSON_OBJECT(
           'manager_user_id', sm.manager_user_id,
           'department_id', sm.department_id,
           'subject_ref', sm.subject_ref,
           'status', sm.status,
           'source_system', sm.source_system,
           'created_at', sm.created_at,
           'updated_at', sm.updated_at
       )),
       JSON_OBJECT('assignment', JSON_OBJECT(
           'employee_id', a.employee_id,
           'role_type', a.role_type,
           'start_time', a.start_time,
           'end_time', a.end_time,
           'status', a.status,
           'start_time_source', a.start_time_source
       )),
       a.change_reason,
       a.operator_id,
       a.created_at
FROM rehealth_insurance_user_assignment a
JOIN rehealth_insurance_subject_manager sm
  ON a.id = LEFT(CONCAT('asg-legacy-', sm.id), 64)
WHERE a.start_time_source = 'legacy'
  AND NOT EXISTS (
      SELECT 1
      FROM rehealth_insurance_assignment_change_log existing
      WHERE existing.assignment_id = a.id
        AND existing.change_type = a.change_reason
  );

-- 5. 迁移后人工校验（应全部返回 0 行）：
-- SELECT enrollment_id, COUNT(*) AS active_primary_count
-- FROM rehealth_insurance_user_assignment
-- WHERE status = 'active' AND role_type = 'PRIMARY'
-- GROUP BY enrollment_id
-- HAVING COUNT(*) > 1;
--
-- SELECT COUNT(*) AS unmigrated_legacy_rows
-- FROM rehealth_insurance_subject_manager sm
-- LEFT JOIN rehealth_insurance_user_assignment a ON a.id = CONCAT('asg-legacy-', sm.id)
-- WHERE a.id IS NULL;

INSERT IGNORE INTO rehealth_schema_migration(version)
VALUES ('software-V20260825.2');
