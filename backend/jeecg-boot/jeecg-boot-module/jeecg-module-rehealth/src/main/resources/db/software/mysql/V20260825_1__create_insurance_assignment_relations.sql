-- 保险用户服务关系一期：项目、参与关系、负责人分配与变更日志。
-- 设计要点：
--   1. subject_ref 保持租户级假名（sha256(tenantId:userId)），不同保险公司之间不可关联；
--   2. 项目维度落在参与记录上，同一自然人在同一租户内可同时参与多个项目；
--   3. user_assignment 为区间化服务关系：换负责人=结束旧行+新建行，历史不覆盖；
--   4. 生成列 active_marker + 唯一索引保证“同一参与记录同一时刻至多一条 active PRIMARY”；
--   5. 不保存原始健康遥测，外部证件号仍只存哈希。
-- 存量数据迁移见 V20260825_2，执行前先跑 precheck-legacy-assignment-data.sql 体检。

CREATE TABLE IF NOT EXISTS rehealth_insurance_project (
    id VARCHAR(64) NOT NULL COMMENT '项目主键',
    tenant_id INT NOT NULL COMMENT '租户 ID',
    project_no VARCHAR(128) NOT NULL COMMENT '项目编号',
    name VARCHAR(255) NOT NULL COMMENT '项目名称',
    status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT '项目状态：active/disabled',
    start_date DATE NULL COMMENT '项目开始日期',
    end_date DATE NULL COMMENT '项目结束日期',
    created_at DATETIME(3) NOT NULL COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_insurance_project_tenant_no (tenant_id, project_no),
    KEY idx_insurance_project_tenant_status (tenant_id, status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='保险服务项目表';

CREATE TABLE IF NOT EXISTS rehealth_insurance_enrollment (
    id VARCHAR(64) NOT NULL COMMENT '参与记录主键',
    tenant_id INT NOT NULL COMMENT '租户 ID',
    project_id VARCHAR(64) NOT NULL COMMENT '所属项目 ID',
    subject_ref CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '被保人租户级假名引用',
    rehealth_user_id VARCHAR(64) NOT NULL COMMENT 'ReHealth 用户 ID',
    enrollment_status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '参与状态：pending/active/ended',
    consent_status VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '授权状态：pending/granted/revoked',
    consent_version VARCHAR(64) NULL COMMENT '授权版本',
    consented_at DATETIME(3) NULL COMMENT '授权时间',
    source_system VARCHAR(64) NOT NULL DEFAULT 'rehealth' COMMENT '来源系统',
    source_record_id VARCHAR(128) NULL COMMENT '来源系统记录 ID',
    metadata_json LONGTEXT NULL COMMENT '扩展元数据 JSON',
    created_at DATETIME(3) NOT NULL COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_insurance_enrollment_project_user (tenant_id, project_id, rehealth_user_id),
    UNIQUE KEY uk_insurance_enrollment_source_record (tenant_id, source_system, source_record_id),
    KEY idx_insurance_enrollment_subject_status (tenant_id, subject_ref, enrollment_status),
    KEY idx_insurance_enrollment_project_status (tenant_id, project_id, enrollment_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户保险项目参与关系表';

CREATE TABLE IF NOT EXISTS rehealth_insurance_user_assignment (
    id VARCHAR(64) NOT NULL COMMENT '分配记录主键',
    tenant_id INT NOT NULL COMMENT '租户 ID',
    enrollment_id VARCHAR(64) NOT NULL COMMENT '参与记录 ID',
    employee_id VARCHAR(64) NOT NULL COMMENT '服务员工用户 ID',
    role_type VARCHAR(32) NOT NULL DEFAULT 'PRIMARY' COMMENT '服务角色：PRIMARY/BACKUP/TEMPORARY/SUPERVISOR',
    start_time DATETIME(3) NOT NULL COMMENT '关系生效时间',
    end_time DATETIME(3) NULL COMMENT '关系失效时间，生效中为空',
    status VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '关系状态：active/ended/revoked',
    active_marker VARCHAR(65) GENERATED ALWAYS AS (CASE WHEN status = 'active' AND role_type = 'PRIMARY' THEN '1' ELSE id END) STORED COMMENT '主负责人活跃唯一标记（生成列，配合唯一索引保证同一参与记录同一时刻至多一条活跃主负责人）',
    start_time_source VARCHAR(16) NOT NULL DEFAULT 'system' COMMENT '生效时间来源：system/legacy',
    change_reason VARCHAR(64) NOT NULL DEFAULT 'assign' COMMENT '变更原因：assign/transfer/reassign/agent/legacy_migration/migration_conflict_resolved',
    operator_id VARCHAR(64) NULL COMMENT '操作人用户 ID',
    created_at DATETIME(3) NOT NULL COMMENT '创建时间',
    UNIQUE KEY uk_assignment_primary_active (enrollment_id, role_type, active_marker),
    KEY idx_assignment_employee_active (tenant_id, employee_id, status, role_type, start_time),
    KEY idx_assignment_enrollment_time (enrollment_id, start_time, end_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户服务负责人分配关系表（区间化、可追溯）';

CREATE TABLE IF NOT EXISTS rehealth_insurance_assignment_change_log (
    id VARCHAR(64) NOT NULL COMMENT '变更日志主键',
    tenant_id INT NOT NULL COMMENT '租户 ID',
    enrollment_id VARCHAR(64) NOT NULL COMMENT '参与记录 ID',
    assignment_id VARCHAR(64) NULL COMMENT '分配记录 ID',
    change_type VARCHAR(32) NOT NULL COMMENT '变更类型：assign/transfer/end/reassign/legacy_migration/migration_conflict_resolved',
    before_json LONGTEXT NULL COMMENT '变更前快照 JSON',
    after_json LONGTEXT NULL COMMENT '变更后快照 JSON',
    reason VARCHAR(64) NOT NULL COMMENT '变更原因',
    operator_id VARCHAR(64) NULL COMMENT '操作人用户 ID',
    created_at DATETIME(3) NOT NULL COMMENT '创建时间',
    KEY idx_assignment_log_enrollment (tenant_id, enrollment_id, created_at),
    KEY idx_assignment_log_operator (tenant_id, operator_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='服务关系变更日志表';

INSERT INTO sys_permission (
    id, parent_id, name, url, component, is_route, component_name, redirect,
    menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf,
    keep_alive, hidden, hide_tab, description, create_by, create_time,
    update_by, update_time, del_flag, rule_flag, status, internal_or_external
)
SELECT '9f0c4e2a1d3b47f6a9c5e712b084d65a', NULL, 'Insurance assignment view', NULL, NULL, 0, NULL, NULL,
       2, 'rehealth:insurance:assignment:view', '1', NULL, 0, NULL, 1,
       0, 1, 0, 'View own or team-scoped user service assignments', 'migration', CURRENT_TIMESTAMP,
       NULL, NULL, 0, 0, '1', 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE perms = 'rehealth:insurance:assignment:view'
);

INSERT INTO sys_permission (
    id, parent_id, name, url, component, is_route, component_name, redirect,
    menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf,
    keep_alive, hidden, hide_tab, description, create_by, create_time,
    update_by, update_time, del_flag, rule_flag, status, internal_or_external
)
SELECT '9f0c4e2a1d3b47f6a9c5e712b084d65b', NULL, 'Insurance assignment transfer', NULL, NULL, 0, NULL, NULL,
       2, 'rehealth:insurance:assignment:transfer', '1', NULL, 0, NULL, 1,
       0, 1, 0, 'Transfer user service assignments between employees', 'migration', CURRENT_TIMESTAMP,
       NULL, NULL, 0, 0, '1', 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission WHERE perms = 'rehealth:insurance:assignment:transfer'
);

INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT LOWER(REPLACE(UUID(), '-', '')), role.id, permission.id, NULL, CURRENT_TIMESTAMP, 'migration'
FROM sys_role role
JOIN sys_permission permission ON permission.perms = 'rehealth:insurance:assignment:view'
WHERE role.role_code IN (
    'insurance_org_admin', 'insurance_department_manager', 'insurance_operator',
    'insurer_viewer', 'insurer_analyst', 'insurer_auditor', 'admin'
)
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission existing
      WHERE existing.role_id = role.id AND existing.permission_id = permission.id
  );

INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT LOWER(REPLACE(UUID(), '-', '')), role.id, permission.id, NULL, CURRENT_TIMESTAMP, 'migration'
FROM sys_role role
JOIN sys_permission permission ON permission.perms = 'rehealth:insurance:assignment:manage'
WHERE role.role_code IN ('insurance_org_admin', 'insurance_department_manager', 'insurance_operator', 'admin')
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission existing
      WHERE existing.role_id = role.id AND existing.permission_id = permission.id
  );

INSERT INTO sys_role_permission (id, role_id, permission_id, data_rule_ids, operate_date, operate_ip)
SELECT LOWER(REPLACE(UUID(), '-', '')), role.id, permission.id, NULL, CURRENT_TIMESTAMP, 'migration'
FROM sys_role role
JOIN sys_permission permission ON permission.perms = 'rehealth:insurance:assignment:transfer'
WHERE role.role_code IN ('insurance_org_admin', 'insurance_department_manager', 'admin')
  AND NOT EXISTS (
      SELECT 1 FROM sys_role_permission existing
      WHERE existing.role_id = role.id AND existing.permission_id = permission.id
  );

INSERT IGNORE INTO rehealth_schema_migration(version)
VALUES ('software-V20260825.1');
