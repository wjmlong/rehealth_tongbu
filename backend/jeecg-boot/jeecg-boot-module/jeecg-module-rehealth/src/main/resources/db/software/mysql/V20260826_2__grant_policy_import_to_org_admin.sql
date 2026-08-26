-- 保险侧保单导入：机构管理员（insurance_org_admin）同样需要保单导入与派发能力。
-- 此前 business:import 仅授予 admin/保险运营员/保险经理，机构管理员登录官网会提示
-- "缺少保单导入权限"。仅授权限，不改任何用户或成员关系；幂等可重复执行。
SET NAMES utf8mb4;

INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT REPLACE(UUID(), '-', ''), role.id, permission.id, NULL, NOW(), 'migration'
FROM sys_role role
JOIN sys_permission permission ON permission.perms = 'rehealth:insurance:business:import'
WHERE role.role_code = 'insurance_org_admin'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission existing
      WHERE existing.role_id = role.id
        AND existing.permission_id = permission.id
  );

INSERT IGNORE INTO rehealth_schema_migration(version)
VALUES ('software-V20260826.2');
