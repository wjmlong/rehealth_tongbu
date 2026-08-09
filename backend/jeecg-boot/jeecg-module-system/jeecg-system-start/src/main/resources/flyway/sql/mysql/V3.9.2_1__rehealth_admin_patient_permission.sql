-- Adds an assignable, least-privilege permission for the ReHealth patient-health admin API.
-- Deliberately does not grant it to any role; an authorized administrator must assign it.
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
