-- Insurance organization settings and least-privilege administration roles.
CREATE TABLE IF NOT EXISTS rehealth_insurance_tenant_profile (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    tenant_id INT NOT NULL,
    organization_name VARCHAR(200) NOT NULL,
    license_no VARCHAR(100) NULL,
    insurance_type VARCHAR(32) NULL,
    compliance_email VARCHAR(120) NULL,
    regulatory_email VARCHAR(120) NULL,
    data_retention_years INT NOT NULL DEFAULT 7,
    mask_sensitive_data TINYINT(1) NOT NULL DEFAULT 1,
    access_log_enabled TINYINT(1) NOT NULL DEFAULT 1,
    notification_config_json JSON NULL,
    version INT NOT NULL DEFAULT 1,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_insurance_tenant_profile_tenant (tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT INTO sys_permission (
    id, parent_id, name, url, component, is_route, component_name, redirect,
    menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf,
    keep_alive, hidden, hide_tab, description, create_by, create_time,
    update_by, update_time, del_flag, rule_flag, status, internal_or_external
)
SELECT permission_id, NULL, permission_name, NULL, NULL, 0, NULL, NULL,
       2, permission_code, '1', NULL, 0, NULL, 1, 0, 1, 0,
       permission_description, 'migration', CURRENT_TIMESTAMP,
       NULL, NULL, 0, 0, '1', 0
FROM (
    SELECT '9f0c4e2a1d3b47f6a9c5e712b084d650' permission_id, 'Insurance organization view' permission_name, 'rehealth:insurance:organization:view' permission_code, 'View organization settings' permission_description
    UNION ALL SELECT '9f0c4e2a1d3b47f6a9c5e712b084d651', 'Insurance organization edit', 'rehealth:insurance:organization:edit', 'Edit organization settings'
    UNION ALL SELECT '9f0c4e2a1d3b47f6a9c5e712b084d652', 'Insurance department manage', 'rehealth:insurance:department:manage', 'Manage departments'
    UNION ALL SELECT '9f0c4e2a1d3b47f6a9c5e712b084d653', 'Insurance member view', 'rehealth:insurance:member:view', 'View members and roles'
    UNION ALL SELECT '9f0c4e2a1d3b47f6a9c5e712b084d654', 'Insurance member manage', 'rehealth:insurance:member:manage', 'Manage member status and department'
    UNION ALL SELECT '9f0c4e2a1d3b47f6a9c5e712b084d655', 'Insurance role assign', 'rehealth:insurance:role:assign', 'Assign insurance roles'
    UNION ALL SELECT '9f0c4e2a1d3b47f6a9c5e712b084d656', 'Insurance subject assignment', 'rehealth:insurance:assignment:manage', 'Manage manager subject assignments'
) seed
WHERE NOT EXISTS (SELECT 1 FROM sys_permission existing WHERE existing.perms = seed.permission_code);

INSERT INTO sys_role (id, role_name, role_code, description, create_by, create_time, tenant_id)
SELECT '9f0c4e2a1d3b47f6a9c5e712b084d657', CONVERT(X'E4BF9DE999A9E69CBAE69E84E7AEA1E79086E59198' USING utf8mb4), 'insurance_org_admin', 'Insurance organization administrator', 'migration', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'insurance_org_admin');

INSERT INTO sys_role (id, role_name, role_code, description, create_by, create_time, tenant_id)
SELECT '9f0c4e2a1d3b47f6a9c5e712b084d658', CONVERT(X'E4BF9DE999A9E983A8E997A8E7BB8FE79086' USING utf8mb4), 'insurance_department_manager', 'Insurance department manager', 'migration', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'insurance_department_manager');

INSERT INTO sys_role_permission (id, role_id, permission_id, operate_date)
SELECT LOWER(REPLACE(UUID(), '-', '')), role.id, permission.id, CURRENT_TIMESTAMP
FROM sys_role role JOIN sys_permission permission ON permission.perms IN (
    'rehealth:insurance:risk:view', 'rehealth:insurance:organization:view', 'rehealth:insurance:organization:edit',
    'rehealth:insurance:department:manage', 'rehealth:insurance:member:view', 'rehealth:insurance:member:manage',
    'rehealth:insurance:role:assign', 'rehealth:insurance:assignment:manage')
WHERE role.role_code = 'insurance_org_admin'
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission x WHERE x.role_id = role.id AND x.permission_id = permission.id);

INSERT INTO sys_role_permission (id, role_id, permission_id, operate_date)
SELECT LOWER(REPLACE(UUID(), '-', '')), role.id, permission.id, CURRENT_TIMESTAMP
FROM sys_role role JOIN sys_permission permission ON permission.perms IN (
    'rehealth:insurance:risk:view', 'rehealth:insurance:organization:view', 'rehealth:insurance:member:view')
WHERE role.role_code = 'insurance_department_manager'
  AND NOT EXISTS (SELECT 1 FROM sys_role_permission x WHERE x.role_id = role.id AND x.permission_id = permission.id);

INSERT IGNORE INTO rehealth_schema_migration(version) VALUES ('software-V20260813.6');
