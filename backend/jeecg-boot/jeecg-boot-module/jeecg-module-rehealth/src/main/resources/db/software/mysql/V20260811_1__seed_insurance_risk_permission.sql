-- Manual/idempotent permission seed for the insurer website service account.
-- This migration intentionally creates only the permission definition. It does not
-- grant the permission to admin or to any existing role. Assign it manually to a
-- dedicated website-service role through JEECG role authorization after review.
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

-- Deliberately no INSERT into sys_role_permission here. The deployment operator
-- must grant this permission only to the dedicated website-service role.
