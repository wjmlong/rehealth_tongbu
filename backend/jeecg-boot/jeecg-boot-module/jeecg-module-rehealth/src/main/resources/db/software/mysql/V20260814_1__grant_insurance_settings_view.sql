-- All insurer back-office roles can read the current tenant's organization
-- and staff directory. Mutation permissions remain limited to administrators.
INSERT INTO sys_role_permission (id, role_id, permission_id, operate_date)
SELECT LOWER(REPLACE(UUID(), '-', '')), role.id, permission.id, CURRENT_TIMESTAMP
FROM sys_role role
JOIN sys_permission permission ON permission.perms IN (
    'rehealth:insurance:organization:view',
    'rehealth:insurance:member:view'
)
WHERE role.role_code IN (
    'insurer_viewer',
    'insurer_analyst',
    'insurance_operator',
    'insurer_auditor'
)
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission existing
      WHERE existing.role_id = role.id
        AND existing.permission_id = permission.id
  );

INSERT IGNORE INTO rehealth_schema_migration(version)
VALUES ('software-V20260814.1');
