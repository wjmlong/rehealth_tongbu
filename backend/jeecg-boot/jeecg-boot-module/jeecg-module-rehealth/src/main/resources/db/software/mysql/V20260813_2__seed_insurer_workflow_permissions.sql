-- Least-privilege insurer workflow permissions and role templates.
-- This migration never assigns a user or tenant membership.

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
    SELECT '8f0c4e2a1d3b47f6a9c5e712b084d640' permission_id,
           '保险业务数据导入' permission_name,
           'rehealth:insurance:business:import' permission_code,
           '导入租户范围内的投保人、保单和理赔数据' permission_description
    UNION ALL SELECT '8f0c4e2a1d3b47f6a9c5e712b084d641', '保险研究查看',
           'rehealth:insurance:study:view', '查看租户范围内的研究、快照和结果'
    UNION ALL SELECT '8f0c4e2a1d3b47f6a9c5e712b084d642', '保险研究管理',
           'rehealth:insurance:study:manage', '创建快照、运行研究和审核结果'
    UNION ALL SELECT '8f0c4e2a1d3b47f6a9c5e712b084d643', '保险报告查看',
           'rehealth:insurance:report:view', '查看和导出租户范围内的 RWE 报告'
    UNION ALL SELECT '8f0c4e2a1d3b47f6a9c5e712b084d644', '保险报告管理',
           'rehealth:insurance:report:manage', '生成、提交、审批和退回 RWE 报告'
    UNION ALL SELECT '8f0c4e2a1d3b47f6a9c5e712b084d645', '保险结算操作',
           'rehealth:insurance:settlement:operate', '生成、提交、审批、退回和重算结算包'
    UNION ALL SELECT '8f0c4e2a1d3b47f6a9c5e712b084d646', '保险审计查看',
           'rehealth:insurance:audit:view', '查看不可变研究、报告、结算和操作审计证据'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission existing WHERE existing.perms = seed.permission_code
);

INSERT INTO sys_role (id, role_name, role_code, description, create_by, create_time, tenant_id)
SELECT '8f0c4e2a1d3b47f6a9c5e712b084d647', '保险查看员', 'insurer_viewer',
       'Read-only insurer risk, study and report viewer', 'migration', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'insurer_viewer');

INSERT INTO sys_role (id, role_name, role_code, description, create_by, create_time, tenant_id)
SELECT '8f0c4e2a1d3b47f6a9c5e712b084d648', '保险审计员', 'insurer_auditor',
       'Read-only insurer evidence and audit reviewer', 'migration', CURRENT_TIMESTAMP, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'insurer_auditor');

-- Viewer: risk, studies and approved/report material. Analyst: research lifecycle.
-- Operator: source data, reports and settlement. Auditor: evidence and audit only.
INSERT INTO sys_role_permission (id, role_id, permission_id, operate_date)
SELECT LOWER(REPLACE(UUID(), '-', '')), role.id, permission.id, CURRENT_TIMESTAMP
FROM sys_role role
JOIN sys_permission permission ON (
       (role.role_code = 'insurer_viewer' AND permission.perms IN (
           'rehealth:insurance:risk:view', 'rehealth:insurance:study:view',
           'rehealth:insurance:report:view'))
    OR (role.role_code = 'insurer_analyst' AND permission.perms IN (
           'rehealth:insurance:risk:view', 'rehealth:insurance:study:view',
           'rehealth:insurance:study:manage', 'rehealth:insurance:report:view'))
    OR (role.role_code = 'insurance_operator' AND permission.perms IN (
           'rehealth:insurance:risk:view', 'rehealth:insurance:business:import',
           'rehealth:insurance:study:view', 'rehealth:insurance:report:view',
           'rehealth:insurance:report:manage', 'rehealth:insurance:settlement:operate'))
    OR (role.role_code = 'insurer_auditor' AND permission.perms IN (
           'rehealth:insurance:risk:view', 'rehealth:insurance:study:view',
           'rehealth:insurance:report:view', 'rehealth:insurance:audit:view'))
)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_permission existing
    WHERE existing.role_id = role.id AND existing.permission_id = permission.id
);

INSERT IGNORE INTO rehealth_schema_migration(version)
VALUES ('software-V20260813.2');
