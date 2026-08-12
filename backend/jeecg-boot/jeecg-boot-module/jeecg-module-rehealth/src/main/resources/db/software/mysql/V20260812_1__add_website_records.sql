CREATE TABLE IF NOT EXISTS rehealth_website_record (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    resource_type VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    payload_json LONGTEXT NOT NULL,
    created_by VARCHAR(64) NOT NULL,
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    KEY idx_website_record_tenant_resource_created (tenant_id, resource_type, created_at),
    KEY idx_website_record_tenant_status (tenant_id, resource_type, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO rehealth_schema_migration(version)
VALUES ('software-V20260812.1');
