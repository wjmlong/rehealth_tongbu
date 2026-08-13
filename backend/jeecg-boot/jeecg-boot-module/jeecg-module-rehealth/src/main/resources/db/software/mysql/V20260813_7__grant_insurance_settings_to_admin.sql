-- Keep the existing Jeecg admin account usable for local insurer settings QA.
-- This grant is intentionally limited to the local admin role. Production
-- accounts should receive these permissions through insurance_org_admin or a
-- dedicated least-privilege role instead.

INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT REPLACE(UUID(), '-', ''), role.id, permission.id, NULL, NOW(), 'migration'
FROM sys_role role
JOIN sys_permission permission
  ON permission.perms IN (
      'rehealth:insurance:organization:view',
      'rehealth:insurance:organization:edit',
      'rehealth:insurance:department:manage',
      'rehealth:insurance:member:view',
      'rehealth:insurance:member:manage',
      'rehealth:insurance:role:assign',
      'rehealth:insurance:assignment:manage'
  )
WHERE role.role_code = 'admin'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission existing
      WHERE existing.role_id = role.id
        AND existing.permission_id = permission.id
  );

INSERT IGNORE INTO rehealth_schema_migration(version)
VALUES ('software-V20260813.7');
