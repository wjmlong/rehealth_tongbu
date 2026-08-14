-- Department trees belong to one tenant. The legacy global org_code index
-- prevents different insurers from each starting their own A01 hierarchy.

SET @add_tenant_org_code_index = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_depart'
          AND index_name = 'uniq_depart_tenant_org_code'
    ),
    'SELECT 1',
    'ALTER TABLE sys_depart ADD UNIQUE INDEX uniq_depart_tenant_org_code (tenant_id, org_code)'
);
PREPARE add_tenant_org_code_index_stmt FROM @add_tenant_org_code_index;
EXECUTE add_tenant_org_code_index_stmt;
DEALLOCATE PREPARE add_tenant_org_code_index_stmt;

SET @drop_global_org_code_index = IF(
    EXISTS (
        SELECT 1
        FROM information_schema.statistics
        WHERE table_schema = DATABASE()
          AND table_name = 'sys_depart'
          AND index_name = 'uniq_depart_org_code'
    ),
    'ALTER TABLE sys_depart DROP INDEX uniq_depart_org_code',
    'SELECT 1'
);
PREPARE drop_global_org_code_index_stmt FROM @drop_global_org_code_index;
EXECUTE drop_global_org_code_index_stmt;
DEALLOCATE PREPARE drop_global_org_code_index_stmt;

-- Normalize the stable local insurer acceptance tree to Jeecg's three-character
-- YouBian code segments. Child additions can then advance A01, A02, A03, ...
UPDATE sys_depart
SET org_code = CASE id
    WHEN 'iqdep000000000000000000000001' THEN 'A01'
    WHEN 'iqdep000000000000000000000002' THEN 'A01A01'
    WHEN 'iqdep000000000000000000000003' THEN 'A01A02'
    ELSE org_code
END
WHERE tenant_id = 1000
  AND id IN (
      'iqdep000000000000000000000001',
      'iqdep000000000000000000000002',
      'iqdep000000000000000000000003'
  );

INSERT IGNORE INTO rehealth_schema_migration(version)
VALUES ('software-V20260813.8');
