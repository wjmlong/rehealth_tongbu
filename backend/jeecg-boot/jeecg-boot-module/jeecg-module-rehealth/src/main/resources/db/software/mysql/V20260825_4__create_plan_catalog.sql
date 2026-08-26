-- 保险健康计划目录：保单 default_plan_id 引用计划标识，App 与官网展示计划名称。
-- 产品级计划模板；对具体用户的计划实例仍由关怀计划（care_plan）承载。

SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS rehealth_insurance_plan_catalog (
    id VARCHAR(64) NOT NULL COMMENT '目录主键',
    tenant_id INT NOT NULL COMMENT '租户 ID',
    plan_id VARCHAR(128) NOT NULL COMMENT '计划标识',
    name VARCHAR(255) NOT NULL COMMENT '计划名称',
    description VARCHAR(1000) NULL COMMENT '计划说明',
    status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT '状态：active/disabled',
    created_at DATETIME(3) NOT NULL COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL COMMENT '更新时间',
    UNIQUE KEY uk_plan_catalog_tenant_plan (tenant_id, plan_id),
    KEY idx_plan_catalog_tenant_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='保险健康计划目录表';

INSERT INTO rehealth_insurance_plan_catalog (id, tenant_id, plan_id, name, description, status, created_at, updated_at)
SELECT LOWER(REPLACE(UUID(), '-', '')), t.tenant_id, 'PLAN-CHRONIC-2026', '慢病管理关怀计划',
       '面向慢性病风险人群的健康管理计划，含定期监测、生活方式干预与复测随访。', 'active', NOW(3), NOW(3)
FROM (SELECT 1000 AS tenant_id UNION SELECT 9102) t
ON DUPLICATE KEY UPDATE name = VALUES(name), status = VALUES(status), updated_at = VALUES(updated_at);

INSERT INTO rehealth_insurance_plan_catalog (id, tenant_id, plan_id, name, description, status, created_at, updated_at)
SELECT LOWER(REPLACE(UUID(), '-', '')), 9101, 'PLAN-CVD-2025', '心血管风险改善计划',
       '面向心血管风险人群的改善计划，含血压监测、运动处方与血脂管理。', 'active', NOW(3), NOW(3)
ON DUPLICATE KEY UPDATE name = VALUES(name), status = VALUES(status), updated_at = VALUES(updated_at);

INSERT IGNORE INTO rehealth_schema_migration(version)
VALUES ('software-V20260825.4');
