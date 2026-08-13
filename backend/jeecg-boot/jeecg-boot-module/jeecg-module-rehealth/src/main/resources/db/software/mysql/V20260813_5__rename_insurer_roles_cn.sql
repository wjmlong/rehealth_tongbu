-- Use Chinese display names in role management while keeping stable role codes.

UPDATE sys_role
SET role_name = CASE role_code
    WHEN 'insurer_viewer' THEN '保险查看员'
    WHEN 'insurer_analyst' THEN '保险分析员'
    WHEN 'insurance_operator' THEN '保险运营员'
    WHEN 'insurer_auditor' THEN '保险审计员'
END,
    update_by = 'migration',
    update_time = CURRENT_TIMESTAMP
WHERE role_code IN ('insurer_viewer', 'insurer_analyst', 'insurance_operator', 'insurer_auditor');

INSERT IGNORE INTO rehealth_schema_migration(version)
VALUES ('software-V20260813.5');
