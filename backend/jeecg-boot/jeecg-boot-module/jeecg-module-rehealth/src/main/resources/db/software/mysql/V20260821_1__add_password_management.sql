-- ReHealth password lifecycle state and tenant administrator reset permission.
-- The credential remains in the global Jeecg sys_user table; this table only
-- records whether that account must complete a password change.
CREATE TABLE IF NOT EXISTS rehealth_user_password_state (
    user_id VARCHAR(64) NOT NULL COMMENT 'Jeecg 用户 ID',
    must_change_password TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否必须在进入系统前修改密码',
    reason VARCHAR(32) NOT NULL DEFAULT 'new_member' COMMENT '强制改密原因：新建成员、管理员重置或自助修改',
    reset_tenant_id INT NULL COMMENT '最近一次管理员重置所属租户 ID',
    reset_by VARCHAR(64) NULL COMMENT '最近一次执行管理员重置的用户 ID',
    reset_at DATETIME(3) NULL COMMENT '最近一次管理员重置时间',
    changed_at DATETIME(3) NULL COMMENT '最近一次用户自助修改密码时间',
    created_at DATETIME(3) NOT NULL COMMENT '状态首次创建时间',
    updated_at DATETIME(3) NOT NULL COMMENT '状态最后更新时间',
    PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ReHealth 员工账号密码强制修改状态表';

INSERT INTO sys_permission (
    id, parent_id, name, url, component, is_route, component_name, redirect,
    menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf,
    keep_alive, hidden, hide_tab, description, create_by, create_time,
    update_by, update_time, del_flag, rule_flag, status, internal_or_external
)
SELECT '9f0c4e2a1d3b47f6a9c5e712b084d659', NULL, 'Insurance member password reset', NULL, NULL, 0, NULL, NULL,
       2, 'rehealth:insurance:member:password:reset', '1', NULL, 0, NULL, 1,
       0, 1, 0, 'Reset a member password within the current insurance tenant', 'migration', CURRENT_TIMESTAMP,
       NULL, NULL, 0, 0, '1', 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE perms = 'rehealth:insurance:member:password:reset'
);

INSERT INTO sys_role_permission (id, role_id, permission_id, operate_date)
SELECT LOWER(REPLACE(UUID(), '-', '')), role.id, permission.id, CURRENT_TIMESTAMP
FROM sys_role role
JOIN sys_permission permission ON permission.perms = 'rehealth:insurance:member:password:reset'
WHERE role.role_code IN ('insurance_org_admin', 'admin')
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission existing
      WHERE existing.role_id = role.id AND existing.permission_id = permission.id
  );

INSERT IGNORE INTO rehealth_schema_migration(version)
VALUES ('software-V20260821.1');
