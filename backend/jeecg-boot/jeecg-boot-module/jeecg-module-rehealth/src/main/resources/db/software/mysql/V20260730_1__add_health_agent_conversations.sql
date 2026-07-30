CREATE TABLE IF NOT EXISTS rehealth_ai_conversation (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    title VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    summary_text LONGTEXT,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    KEY idx_rehealth_ai_conversation_owner_updated (tenant_id, user_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS rehealth_ai_message (
    id VARCHAR(64) PRIMARY KEY,
    conversation_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    request_id VARCHAR(128) NOT NULL,
    role VARCHAR(16) NOT NULL,
    content TEXT NOT NULL,
    status VARCHAR(32) NOT NULL,
    provider VARCHAR(128),
    model_version VARCHAR(128),
    retryable TINYINT(1) NOT NULL DEFAULT 0,
    created_at DATETIME(3) NOT NULL,
    CONSTRAINT fk_rehealth_ai_message_conversation FOREIGN KEY (conversation_id)
        REFERENCES rehealth_ai_conversation (id) ON DELETE CASCADE,
    UNIQUE KEY uk_rehealth_ai_message_request_role (conversation_id, request_id, role),
    KEY idx_rehealth_ai_message_owner_created (tenant_id, user_id, created_at),
    KEY idx_rehealth_ai_message_conversation_created (conversation_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO rehealth_schema_migration(version) VALUES ('software-V20260730.1');
