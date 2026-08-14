-- Manager-to-insured assignment foundation for insurer data permissions.
-- The query layer must explicitly join this table for manager-scoped reads;
-- creating the table alone does not change the existing tenant-scoped API.

CREATE TABLE IF NOT EXISTS rehealth_insurance_subject_manager (
    id VARCHAR(64) NOT NULL PRIMARY KEY,
    tenant_id INT NOT NULL,
    manager_user_id VARCHAR(32) NOT NULL,
    department_id VARCHAR(32) NOT NULL,
    subject_ref CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'active',
    source_system VARCHAR(64) NOT NULL DEFAULT 'rehealth_admin',
    created_at DATETIME(3) NOT NULL,
    updated_at DATETIME(3) NOT NULL,
    UNIQUE KEY uk_insurance_subject_manager (tenant_id, manager_user_id, subject_ref),
    KEY idx_insurance_manager_subject (tenant_id, manager_user_id, status),
    KEY idx_insurance_subject_manager_department (tenant_id, department_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO rehealth_schema_migration(version)
VALUES ('software-V20260813.4');
