-- Create least-privilege insurer role templates.
-- No user is assigned by this migration; administrators must explicitly assign
-- the role to an active tenant member after reviewing the tenant scope.

INSERT INTO sys_role (
    id, role_name, role_code, description, create_by, create_time, tenant_id
)
SELECT
    '8f0c4e2a1d3b47f6a9c5e712b084d632',
    'Insurance Analyst',
    'insurer_analyst',
    'Read-only ReHealth insurance risk workspace role',
    'migration',
    CURRENT_TIMESTAMP,
    0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role WHERE role_code = 'insurer_analyst'
);

INSERT INTO sys_role (
    id, role_name, role_code, description, create_by, create_time, tenant_id
)
SELECT
    '8f0c4e2a1d3b47f6a9c5e712b084d633',
    'Insurance Operator',
    'insurance_operator',
    'Read-only ReHealth insurance operations workspace role',
    'migration',
    CURRENT_TIMESTAMP,
    0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role WHERE role_code = 'insurance_operator'
);

INSERT INTO sys_role_permission (
    id, role_id, permission_id, operate_date
)
SELECT
    '8f0c4e2a1d3b47f6a9c5e712b084d634',
    role.id,
    permission.id,
    CURRENT_TIMESTAMP
FROM sys_role role
INNER JOIN sys_permission permission
    ON permission.perms = 'rehealth:insurance:risk:view'
WHERE role.role_code = 'insurer_analyst'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission existing
      WHERE existing.role_id = role.id
        AND existing.permission_id = permission.id
  );

INSERT INTO sys_role_permission (
    id, role_id, permission_id, operate_date
)
SELECT
    '8f0c4e2a1d3b47f6a9c5e712b084d635',
    role.id,
    permission.id,
    CURRENT_TIMESTAMP
FROM sys_role role
INNER JOIN sys_permission permission
    ON permission.perms = 'rehealth:insurance:risk:view'
WHERE role.role_code = 'insurance_operator'
  AND NOT EXISTS (
      SELECT 1
      FROM sys_role_permission existing
      WHERE existing.role_id = role.id
        AND existing.permission_id = permission.id
  );

INSERT IGNORE INTO rehealth_schema_migration(version)
VALUES ('software-V20260812.3');
