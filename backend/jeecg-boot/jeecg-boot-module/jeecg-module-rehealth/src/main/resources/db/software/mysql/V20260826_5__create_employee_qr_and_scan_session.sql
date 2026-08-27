-- 保险侧扫码关联（二期）：员工专属二维码 + 扫码确认会话。
-- 码 30 天有效期可刷新/停用；会话 5 分钟一次性消费；关系建立复用 createAssignment。
SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS rehealth_insurance_employee_qr (
    id            VARCHAR(64)  NOT NULL COMMENT '主键',
    tenant_id     INT          NOT NULL COMMENT '租户（保险机构）',
    employee_id   VARCHAR(64)  NOT NULL COMMENT '员工账号 ID（sys_user.id）',
    code          VARCHAR(16)  NOT NULL COMMENT '员工码：8 位 Base32，排除 0/O/1/I/L',
    status        VARCHAR(32)  NOT NULL DEFAULT 'active' COMMENT 'active/disabled/expired',
    expires_at    DATETIME     NULL COMMENT '码有效期（默认 30 天，刷新重置）',
    created_at    DATETIME     NOT NULL,
    updated_at    DATETIME     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tenant_code (tenant_id, code),
    UNIQUE KEY uk_tenant_employee (tenant_id, employee_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='保险员工专属二维码绑定（一人一码）';

CREATE TABLE IF NOT EXISTS rehealth_insurance_scan_session (
    id            VARCHAR(64)  NOT NULL COMMENT '主键',
    tenant_id     INT          NOT NULL COMMENT '租户',
    employee_id   VARCHAR(64)  NOT NULL COMMENT '目标员工账号 ID',
    qr_id         VARCHAR(64)  NOT NULL COMMENT '来源员工码（employee_qr.id）',
    user_id       VARCHAR(64)  NOT NULL COMMENT '扫码的 App 用户 ID（登录态）',
    status        VARCHAR(32)  NOT NULL DEFAULT 'pending' COMMENT 'pending/confirmed/cancelled/expired',
    expires_at    DATETIME     NOT NULL COMMENT '会话有效期（5 分钟）',
    confirmed_at  DATETIME     NULL,
    created_at    DATETIME     NOT NULL,
    updated_at    DATETIME     NULL,
    PRIMARY KEY (id),
    KEY idx_user (user_id),
    KEY idx_qr (qr_id),
    KEY idx_expires (status, expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='保险扫码关联会话（一次性消费）';

INSERT IGNORE INTO rehealth_schema_migration(version)
VALUES ('software-V20260826.5');
