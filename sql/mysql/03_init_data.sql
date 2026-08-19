-- ReHealth 生产可用的基础权限、角色与菜单初始化数据

-- 前置条件：已执行 JeecgBoot 官方基础库脚本和 mysql/01_schema.sql。

-- 本文件不创建管理员用户、不写业务测试样本，且不伪造 flyway/迁移历史。

SET NAMES utf8mb4;

SET time_zone = '+00:00';



-- 来源：backend/jeecg-boot/jeecg-server-cloud/jeecg-system-cloud-start/src/main/resources/flyway/sql/mysql/V3.9.2_1__rehealth_admin_patient_permission.sql

INSERT INTO sys_permission (
    id, parent_id, name, url, component, is_route, component_name, redirect,
    menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf,
    keep_alive, hidden, hide_tab, description, create_by, create_time,
    update_by, update_time, del_flag, rule_flag, status, internal_or_external
)
SELECT
    '2030500000000000001', NULL, 'ReHealth患者数据', NULL, NULL, 0, NULL, NULL,
    1, NULL, '0', 100, 0, NULL, 0, 0, 1, 0,
    'ReHealth后台患者健康数据权限组', 'system', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission
    WHERE id = '2030500000000000001' OR name = 'ReHealth患者数据'
);

INSERT INTO sys_permission (
    id, parent_id, name, url, component, is_route, component_name, redirect,
    menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf,
    keep_alive, hidden, hide_tab, description, create_by, create_time,
    update_by, update_time, del_flag, rule_flag, status, internal_or_external
)
SELECT
    '2030500000000000002',
    COALESCE((
        SELECT existing_parent.id
        FROM sys_permission existing_parent
        WHERE existing_parent.name = 'ReHealth患者数据'
        ORDER BY existing_parent.create_time
        LIMIT 1
    ), ''),
    '查看患者健康数据', NULL, NULL, 0, NULL, NULL,
    2, 'rehealth:admin:patient:view', '1', 1, 0, NULL, 1, 0, 1, 0,
    '读取租户隔离且最小化个人信息的患者健康数据', 'system', NOW(), NULL, NULL, 0, 0, '1', 0
FROM DUAL
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission
    WHERE id = '2030500000000000002' OR perms = 'rehealth:admin:patient:view'
);



-- 来源：backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260811_1__seed_insurance_risk_permission.sql

INSERT INTO sys_permission (
    id, parent_id, name, url, component, is_route, component_name, redirect,
    menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf,
    keep_alive, hidden, hide_tab, description, create_by, create_time,
    update_by, update_time, del_flag, rule_flag, status, internal_or_external
)
SELECT
    '8f0c4e2a1d3b47f6a9c5e712b084d631', NULL, '保险风险数据查看', NULL, NULL,
    0, NULL, NULL, 2, 'rehealth:insurance:risk:view', '1', NULL, 0, NULL,
    1, 0, 1, 0, '保险网站服务账号读取租户范围内脱敏风险数据',
    'migration', CURRENT_TIMESTAMP, NULL, NULL, 0, 0, '1', 0
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_permission
    WHERE perms = 'rehealth:insurance:risk:view'
);



-- 来源：backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260812_3__seed_insurer_roles.sql

INSERT INTO sys_role (
    id, role_name, role_code, description, create_by, create_time, tenant_id
)
SELECT
    '8f0c4e2a1d3b47f6a9c5e712b084d632',
    '保险分析员',
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
    '保险运营员',
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



-- 来源：backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260813_2__seed_insurer_workflow_permissions.sql

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



-- 来源：backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260813_5__rename_insurer_roles_cn.sql

UPDATE sys_role
SET role_name = CASE role_code
    WHEN 'insurer_viewer' THEN '保险查看员'
    WHEN 'insurer_analyst' THEN '保险分析员'
    WHEN 'insurance_operator' THEN '保险运营员'
    WHEN 'insurer_auditor' THEN '保险审计员'
END,
    update_by = 'migration',
    update_time = CURRENT_TIMESTAMP
WHERE role_code IN ('insurer_viewer', 'insurer_analyst', 'insurance_operator', 'insurer_auditor');



-- 来源：backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260813_6__create_insurance_settings.sql

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



-- 来源：backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260814_1__grant_insurance_settings_view.sql

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



-- 来源：backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260814_2__create_insurance_intervention_actions.sql

INSERT INTO sys_permission (
    id, parent_id, name, url, component, is_route, component_name, redirect,
    menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf,
    keep_alive, hidden, hide_tab, description, create_by, create_time,
    update_by, update_time, del_flag, rule_flag, status, internal_or_external
)
SELECT '8f0c4e2a1d3b47f6a9c5e712b084d670', NULL, '保险干预行动管理', NULL, NULL, 0, NULL, NULL,
       2, 'rehealth:insurance:intervention:manage', '1', NULL, 0, NULL, 1,
       0, 1, 0, '在负责对象范围内创建和更新保险干预行动', 'migration', CURRENT_TIMESTAMP,
       NULL, NULL, 0, 0, '1', 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE perms = 'rehealth:insurance:intervention:manage'
);

INSERT INTO sys_role_permission (id, role_id, permission_id, operate_date)
SELECT LOWER(REPLACE(UUID(), '-', '')), role.id, permission.id, CURRENT_TIMESTAMP
FROM sys_role role
JOIN sys_permission permission ON permission.perms = 'rehealth:insurance:intervention:manage'
WHERE role.role_code IN ('insurance_org_admin', 'insurance_department_manager', 'insurance_operator', 'admin')
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission existing
      WHERE existing.role_id = role.id AND existing.permission_id = permission.id
  );



-- 来源：backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/software/mysql/V20260819_1__create_versioned_care_plans.sql

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
    SELECT '7c6e2a1d3b474f6a9c5e712b084d6901' permission_id,
           '保险计划查看' permission_name,
           'rehealth:insurance:care-plan:view' permission_code,
           '查看当前负责对象的机构干预计划及版本历史' permission_description
    UNION ALL SELECT '7c6e2a1d3b474f6a9c5e712b084d6902', '保险计划编辑',
           'rehealth:insurance:care-plan:manage', '创建和编辑当前负责对象的计划草稿'
    UNION ALL SELECT '7c6e2a1d3b474f6a9c5e712b084d6903', '保险计划发布',
           'rehealth:insurance:care-plan:publish', '发布或撤回当前负责对象的计划版本'
) seed
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission existing WHERE existing.perms = seed.permission_code
);

INSERT INTO sys_role_permission (id, role_id, permission_id, operate_date)
SELECT LOWER(REPLACE(UUID(), '-', '')), role.id, permission.id, CURRENT_TIMESTAMP
FROM sys_role role
JOIN sys_permission permission ON (
       (permission.perms = 'rehealth:insurance:care-plan:view'
        AND role.role_code IN ('insurance_org_admin', 'insurance_department_manager',
                               'insurance_operator', 'insurer_analyst', 'insurer_viewer',
                               'insurer_auditor', 'admin'))
    OR (permission.perms = 'rehealth:insurance:care-plan:manage'
        AND role.role_code IN ('insurance_org_admin', 'insurance_department_manager',
                               'insurance_operator', 'admin'))
    OR (permission.perms = 'rehealth:insurance:care-plan:publish'
        AND role.role_code IN ('insurance_org_admin', 'insurance_department_manager', 'admin'))
)
WHERE NOT EXISTS (
    SELECT 1 FROM sys_role_permission existing
    WHERE existing.role_id = role.id AND existing.permission_id = permission.id
);
