-- 保险侧基础保单库 + 保单-用户关联（2026-08-26）：
-- 保单是保险机构的独立资产（不再挂被保人），员工为 App 用户"添加保单"写入关联表；
-- 一张保单可关联多个用户；App 侧按关联表匹配可绑定保单。
-- 存量保单上的 insured_subject_ref 迁移为关联表行；保单表列保留（弃用，兼容旧种子脚本）。
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS rehealth_insurance_policy_link (
    id VARCHAR(64) NOT NULL COMMENT '主键',
    tenant_id INT NOT NULL COMMENT '租户（保险机构）',
    policy_no VARCHAR(128) NOT NULL COMMENT '保单号（关联 rehealth_insurance_policy.policy_no）',
    subject_ref VARCHAR(64) NOT NULL COMMENT '被保人假名（关联 rehealth_insurance_subject.subject_ref）',
    status VARCHAR(32) NOT NULL DEFAULT 'assigned' COMMENT '关联状态：assigned=已添加给用户',
    source_system VARCHAR(64) NULL COMMENT '来源系统',
    source_record_id VARCHAR(128) NULL COMMENT '来源记录 ID',
    created_by VARCHAR(64) NULL COMMENT '操作员工 ID',
    created_at DATETIME NOT NULL COMMENT '创建时间',
    updated_at DATETIME NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_policy_subject (tenant_id, policy_no, subject_ref),
    KEY idx_subject (tenant_id, subject_ref)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='保单-用户关联表（员工为 App 用户添加保单）';

-- 存量迁移：保单上的被保人假名转为首条关联（幂等）
INSERT IGNORE INTO rehealth_insurance_policy_link
    (id, tenant_id, policy_no, subject_ref, status, source_system, created_by, created_at, updated_at)
SELECT LEFT(CONCAT('plink-', p.id), 64), p.tenant_id, p.policy_no, p.insured_subject_ref,
       'assigned', p.source_system, NULL, p.created_at, p.updated_at
FROM rehealth_insurance_policy p
WHERE p.insured_subject_ref IS NOT NULL;

INSERT IGNORE INTO rehealth_schema_migration(version)
VALUES ('software-V20260826.4');
