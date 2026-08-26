-- 保险侧保单导入：部门经理（保险经理）同样需要保单导入能力。
-- 仅授权限，不改任何用户或成员关系；幂等可重复执行。
SET NAMES utf8mb4;

INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT REPLACE(UUID(), '-', ''), role.id, permission.id, NULL, NOW(), 'migration'
FROM sys_role role
JOIN sys_permission permission ON permission.perms = 'rehealth:insurance:business:import'
WHERE role.role_code = 'insurance_department_manager'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission existing
      WHERE existing.role_id = role.id
        AND existing.permission_id = permission.id
  );

INSERT IGNORE INTO rehealth_schema_migration(version)
VALUES ('software-V20260826.1');
