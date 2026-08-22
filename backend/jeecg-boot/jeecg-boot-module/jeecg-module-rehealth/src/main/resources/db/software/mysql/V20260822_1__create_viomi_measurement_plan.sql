CREATE TABLE rehealth_viomi_measurement_plan (
    id VARCHAR(64) NOT NULL COMMENT '计划主键',
    user_id VARCHAR(64) NOT NULL COMMENT 'ReHealth 用户主键',
    device_id VARCHAR(128) NOT NULL COMMENT '脱敏后的云米设备编号',
    imei_ciphertext VARBINARY(512) NOT NULL COMMENT 'AES-GCM 加密后的云米 IMEI',
    enabled TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否启用主动测量计划',
    interval_minutes INT NOT NULL DEFAULT 5 COMMENT '主动测量间隔分钟数，允许 3 至 60',
    metrics_csv VARCHAR(255) NOT NULL COMMENT '允许主动测量的指标白名单',
    last_status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '最近一次调度状态',
    last_error VARCHAR(512) NULL COMMENT '最近一次脱敏错误信息',
    last_run_at DATETIME(3) NULL COMMENT '最近一次运行时间',
    next_run_at DATETIME(3) NULL COMMENT '下一次计划运行时间',
    created_at DATETIME(3) NOT NULL COMMENT '创建时间',
    updated_at DATETIME(3) NOT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_rehealth_viomi_plan_user_device (user_id, device_id),
    KEY idx_rehealth_viomi_plan_due (enabled, next_run_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='云米设备主动测量调度计划';
