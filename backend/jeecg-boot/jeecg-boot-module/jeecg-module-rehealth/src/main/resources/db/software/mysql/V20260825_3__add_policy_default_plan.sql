-- 保单导入时指定默认健康计划：零输入绑定按保单自动关联计划。
-- 字段在导入接口（imports/policies）中由 PolicyRow.defaultPlanId 写入。

SET NAMES utf8mb4;

ALTER TABLE rehealth_insurance_policy
    ADD COLUMN default_plan_id VARCHAR(128) NULL COMMENT '默认健康计划 ID（保险侧导入保单时指定）' AFTER policy_type;

-- 王老五保单的默认健康计划（业务值，非测试数据）
UPDATE rehealth_insurance_policy
SET default_plan_id = 'PLAN-CHRONIC-2026'
WHERE tenant_id = 1000 AND policy_no = '8850882026080003712';

INSERT IGNORE INTO rehealth_schema_migration(version)
VALUES ('software-V20260825.3');
