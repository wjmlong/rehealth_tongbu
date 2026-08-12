-- Keep the existing Jeecg admin account usable for local acceptance and
-- emergency administration. This grants permissions to the existing admin
-- role only; it never assigns insurer roles to a user.

INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT REPLACE(UUID(), '-', ''), role.id, permission.id, NULL, NOW(), 'migration'
FROM sys_role role
JOIN sys_permission permission
  ON permission.perms IN (
      'rehealth:insurance:business:import',
      'rehealth:insurance:study:view',
      'rehealth:insurance:study:manage',
      'rehealth:insurance:report:view',
      'rehealth:insurance:report:manage',
      'rehealth:insurance:settlement:operate',
      'rehealth:insurance:audit:view'
  )
WHERE role.role_code = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission existing
      WHERE existing.role_id = role.id
        AND existing.permission_id = permission.id
  );

INSERT IGNORE INTO rehealth_schema_migration(version)
VALUES ('software-V20260813.3');
