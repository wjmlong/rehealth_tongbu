-- 保险侧两步式保单派发：保单先入库（被保人可空），后续按手机号分配给负责的用户。
-- 1) insured_subject_ref 允许为空：未分配保单进入保单库，App 端不可见；
-- 2) assigned_at 记录分配时间。
SET NAMES utf8mb4;

ALTER TABLE rehealth_insurance_policy
    MODIFY COLUMN insured_subject_ref VARCHAR(64) NULL COMMENT '被保人假名（两步式派发：可空，分配时写入；sha256(tenantId:userId)）';

ALTER TABLE rehealth_insurance_policy
    ADD COLUMN assigned_at DATETIME NULL COMMENT '保单分配时间（两步式派发）' AFTER default_plan_id;

INSERT IGNORE INTO rehealth_schema_migration(version)
VALUES ('software-V20260826.3');
