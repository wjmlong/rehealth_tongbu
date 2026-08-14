-- Tenant-scoped operational actions for the insurer intervention workbench.
-- Health evidence stays in its canonical tables; this table stores only staff workflow state.
CREATE TABLE IF NOT EXISTS rehealth_insurance_intervention_action (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    tenant_id INT NOT NULL,
    subject_ref CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    plan_id VARCHAR(128) NULL,
    action_type VARCHAR(32) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content VARCHAR(2000) NULL,
    assignee_user_id VARCHAR(32) NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'pending',
    due_at DATETIME(3) NULL,
    completed_at DATETIME(3) NULL,
    result_json LONGTEXT NULL,
    created_by VARCHAR(32) NOT NULL,
    request_id VARCHAR(128) NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    KEY idx_insurance_action_subject (tenant_id, subject_ref, updated_at),
    KEY idx_insurance_action_assignee (tenant_id, assignee_user_id, status, due_at),
    UNIQUE KEY uk_insurance_action_request (tenant_id, request_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

ALTER TABLE rehealth_insurance_intervention_feedback
    MODIFY intervention_id VARCHAR(128) NULL;

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

INSERT IGNORE INTO rehealth_schema_migration(version)
VALUES ('software-V20260814.2');
