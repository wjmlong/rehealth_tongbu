CREATE TABLE IF NOT EXISTS rehealth_care_plan_execution (
    id VARCHAR(64) NOT NULL COMMENT '计划任务执行事实主键',
    tenant_id INT NOT NULL COMMENT '从任务实例复制的所属 Jeecg 租户 ID',
    occurrence_id VARCHAR(64) NOT NULL COMMENT '被评分的计划任务实例 ID',
    plan_id VARCHAR(64) NOT NULL COMMENT '执行时所属关怀计划 ID',
    revision_id VARCHAR(64) NOT NULL COMMENT '执行时所属已发布版本 ID',
    plan_item_id VARCHAR(64) NOT NULL COMMENT '执行时所属版本内计划项目 ID',
    logical_item_id VARCHAR(64) NOT NULL COMMENT '跨版本保持稳定的逻辑项目 ID',
    subject_ref CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL COMMENT '租户范围内的服务对象引用',
    feedback_type VARCHAR(32) NOT NULL COMMENT '执行评分：completed、partially_completed、skipped 或 not_applicable',
    score_value DECIMAL(5,4) NULL COMMENT '依从性计分值：1、0.5、0；不适用为 NULL',
    verification_type VARCHAR(32) NOT NULL DEFAULT 'self_report' COMMENT '执行事实核验方式：用户自报、设备核验或人员确认',
    note VARCHAR(1000) NULL COMMENT '用户可选的有界执行备注，不保存原始健康遥测',
    occurred_at DATETIME(3) NOT NULL COMMENT '用户执行或提交评分的业务时间',
    source_system VARCHAR(64) NOT NULL DEFAULT 'rehealth_app' COMMENT '执行事实来源系统',
    source_record_id VARCHAR(128) NOT NULL COMMENT '来源系统内的幂等记录 ID',
    created_at DATETIME(3) NOT NULL COMMENT '执行事实写入服务端的时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_care_plan_execution_source (tenant_id, source_system, source_record_id),
    KEY idx_care_plan_execution_occurrence (tenant_id, occurrence_id, occurred_at),
    KEY idx_care_plan_execution_subject_time (tenant_id, subject_ref, occurred_at),
    KEY idx_care_plan_execution_plan_time (tenant_id, plan_id, occurred_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='独立于计划版本内容的任务执行评分事实表，用于滚动二十八日依从性';

INSERT IGNORE INTO rehealth_schema_migration(version)
VALUES ('software-V20260819.2');
