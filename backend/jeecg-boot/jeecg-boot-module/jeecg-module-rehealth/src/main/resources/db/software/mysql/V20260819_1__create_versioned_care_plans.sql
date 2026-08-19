-- Canonical integrated DDL for the shared, tenant-scoped care-plan version foundation.
-- Keep these five tables together: plan aggregate, immutable revision, revision item,
-- scheduled occurrence and append-only audit evidence form one versioning boundary.
-- Every persisted table and column carries a database comment for schema discovery.
-- Published revisions are immutable. Institution adapters own authorization and subject scope.

CREATE TABLE IF NOT EXISTS rehealth_care_plan (
    id VARCHAR(64) NOT NULL COMMENT '关怀计划主键',
    tenant_id INT NOT NULL COMMENT '所属 Jeecg 租户 ID',
    owner_type VARCHAR(32) NOT NULL COMMENT '计划所属机构类型：保险、医疗或个人',
    owner_org_ref VARCHAR(64) NOT NULL COMMENT '所属机构引用；保险机构当前使用租户 ID',
    subject_ref CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '租户范围内的服务对象引用',
    rehealth_user_id VARCHAR(64) NOT NULL COMMENT '由可信服务关系解析的 ReHealth APP 用户 ID',
    source_plan_id VARCHAR(128) NULL COMMENT '可选的历史计划或外部计划标识',
    status VARCHAR(32) NOT NULL DEFAULT 'draft' COMMENT '计划生命周期状态：草稿、生效或已撤回',
    current_revision_id VARCHAR(64) NULL COMMENT '最新发布版本 ID；该版本可在未来时间生效',
    draft_revision_id VARCHAR(64) NULL COMMENT '当前唯一可编辑的草稿版本 ID',
    lock_version BIGINT NOT NULL DEFAULT 0 COMMENT '计划全部变更使用的乐观锁版本号',
    created_by VARCHAR(64) NOT NULL COMMENT '创建计划的认证用户 ID',
    created_at DATETIME(3) NOT NULL COMMENT '计划创建时间',
    updated_by VARCHAR(64) NOT NULL COMMENT '最后更新计划的认证用户 ID',
    updated_at DATETIME(3) NOT NULL COMMENT '计划最后更新时间',
    PRIMARY KEY (id),
    KEY idx_care_plan_subject (tenant_id, owner_type, subject_ref, status, updated_at),
    KEY idx_care_plan_user (tenant_id, rehealth_user_id, status, updated_at),
    KEY idx_care_plan_current_revision (tenant_id, current_revision_id),
    KEY idx_care_plan_draft_revision (tenant_id, draft_revision_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='按租户隔离并支持乐观锁的机构关怀计划主表';

CREATE TABLE IF NOT EXISTS rehealth_care_plan_revision (
    id VARCHAR(64) NOT NULL COMMENT '计划版本主键',
    tenant_id INT NOT NULL COMMENT '从计划主表复制的所属 Jeecg 租户 ID',
    plan_id VARCHAR(64) NOT NULL COMMENT '所属关怀计划 ID',
    revision_no INT NOT NULL COMMENT '计划内单调递增的版本序号',
    status VARCHAR(32) NOT NULL DEFAULT 'draft' COMMENT '版本状态：草稿、已发布或已撤回',
    title VARCHAR(255) NOT NULL COMMENT '用户可见的计划标题',
    summary VARCHAR(2000) NULL COMMENT '长度受限的用户可见计划摘要',
    change_reason VARCHAR(1000) NULL COMMENT '机构填写的本次版本变更原因',
    content_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '版本元数据及有序计划项目的 SHA-256 摘要',
    effective_from DATETIME(3) NULL COMMENT '发布时设置的版本生效时间，包含该时间点',
    effective_to DATETIME(3) NULL COMMENT '由新版本或撤回设置的失效时间，不包含该时间点',
    published_by VARCHAR(64) NULL COMMENT '发布版本的认证用户 ID',
    published_at DATETIME(3) NULL COMMENT '版本发布时间',
    withdrawn_by VARCHAR(64) NULL COMMENT '撤回版本的认证用户 ID',
    withdrawn_at DATETIME(3) NULL COMMENT '版本撤回时间',
    created_by VARCHAR(64) NOT NULL COMMENT '创建版本的认证用户 ID',
    created_at DATETIME(3) NOT NULL COMMENT '版本创建时间',
    updated_by VARCHAR(64) NOT NULL COMMENT '最后编辑草稿的认证用户 ID',
    updated_at DATETIME(3) NOT NULL COMMENT '版本最后更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_care_plan_revision_no (tenant_id, plan_id, revision_no),
    KEY idx_care_plan_revision_effective (tenant_id, plan_id, status, effective_from, effective_to),
    KEY idx_care_plan_revision_hash (tenant_id, content_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='发布后内容不可变的机构关怀计划版本表';

CREATE TABLE IF NOT EXISTS rehealth_care_plan_item (
    id VARCHAR(64) NOT NULL COMMENT '版本内计划项目主键',
    tenant_id INT NOT NULL COMMENT '从计划主表复制的所属 Jeecg 租户 ID',
    plan_id VARCHAR(64) NOT NULL COMMENT '所属关怀计划 ID',
    revision_id VARCHAR(64) NOT NULL COMMENT '包含该不可变项目快照的计划版本 ID',
    logical_item_id VARCHAR(64) NOT NULL COMMENT '克隆新版本时保持不变的逻辑项目 ID',
    category VARCHAR(32) NOT NULL COMMENT '保守干预分类，例如运动、营养、睡眠或随访',
    title VARCHAR(255) NOT NULL COMMENT '用户可见的计划项目标题',
    instructions VARCHAR(4000) NULL COMMENT '长度受限的用户可见执行说明',
    schedule_json LONGTEXT NULL COMMENT '结构化计划规则，由独立任务实例生成器展开',
    scoring_weight DECIMAL(10,3) NOT NULL DEFAULT 1.000 COMMENT '每个已生成任务实例的依从性计分权重',
    allow_not_applicable TINYINT(1) NOT NULL DEFAULT 1 COMMENT '用户是否可以将任务标记为不适用',
    display_order INT NOT NULL COMMENT '当前版本内稳定的展示顺序',
    created_at DATETIME(3) NOT NULL COMMENT '计划项目快照创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_care_plan_item_logical (tenant_id, revision_id, logical_item_id),
    UNIQUE KEY uk_care_plan_item_order (tenant_id, revision_id, display_order),
    KEY idx_care_plan_item_plan (tenant_id, plan_id, revision_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绑定具体版本的用户可见关怀计划项目快照表';

CREATE TABLE IF NOT EXISTS rehealth_care_plan_occurrence (
    id VARCHAR(64) NOT NULL COMMENT '用于反馈幂等的计划任务实例主键',
    tenant_id INT NOT NULL COMMENT '从计划主表复制的所属 Jeecg 租户 ID',
    plan_id VARCHAR(64) NOT NULL COMMENT '所属关怀计划 ID',
    revision_id VARCHAR(64) NOT NULL COMMENT '生成该任务实例的已发布版本 ID',
    plan_item_id VARCHAR(64) NOT NULL COMMENT '生成该任务实例的版本内计划项目 ID',
    logical_item_id VARCHAR(64) NOT NULL COMMENT '跨计划版本保持稳定的逻辑项目 ID',
    subject_ref CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '租户范围内的服务对象引用',
    scheduled_at DATETIME(3) NOT NULL COMMENT '按统一服务端时间记录的计划执行时间',
    due_at DATETIME(3) NOT NULL COMMENT '用于计算依从性时间窗口的截止时间',
    status VARCHAR(32) NOT NULL DEFAULT 'scheduled' COMMENT '任务实例状态：待执行或已取消；执行事实单独存储',
    exclusion_reason VARCHAR(128) NULL COMMENT '已取消任务不计入依从性的原因',
    created_at DATETIME(3) NOT NULL COMMENT '任务实例创建时间',
    updated_at DATETIME(3) NOT NULL COMMENT '任务实例最后更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_care_plan_occurrence_due (tenant_id, plan_item_id, scheduled_at),
    KEY idx_care_plan_occurrence_subject_due (tenant_id, subject_ref, status, due_at),
    KEY idx_care_plan_occurrence_revision (tenant_id, revision_id, status, scheduled_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='绑定计划版本并构成未来依从性分母的到期任务实例表';

CREATE TABLE IF NOT EXISTS rehealth_care_plan_audit_event (
    id VARCHAR(64) NOT NULL COMMENT '计划审计事件主键',
    tenant_id INT NOT NULL COMMENT '所属 Jeecg 租户 ID',
    owner_type VARCHAR(32) NOT NULL COMMENT '用于审计筛选的计划所属机构类型',
    actor_user_id VARCHAR(64) NOT NULL COMMENT '执行操作的认证用户 ID',
    action VARCHAR(64) NOT NULL COMMENT '版本操作，例如创建草稿、更新草稿、克隆版本、发布或撤回',
    plan_id VARCHAR(64) NOT NULL COMMENT '受影响的关怀计划 ID',
    revision_id VARCHAR(64) NULL COMMENT '受影响的计划版本 ID',
    before_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '操作前的计划内容摘要',
    after_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NULL COMMENT '操作后的计划内容摘要',
    reason VARCHAR(1000) NULL COMMENT '长度受限的机构变更或撤回原因',
    created_at DATETIME(3) NOT NULL COMMENT '审计事件创建时间',
    PRIMARY KEY (id),
    KEY idx_care_plan_audit_plan (tenant_id, plan_id, created_at),
    KEY idx_care_plan_audit_actor (tenant_id, actor_user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='仅追加写入的关怀计划版本生命周期审计表';

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

INSERT IGNORE INTO rehealth_schema_migration(version)
VALUES ('software-V20260819.1');
