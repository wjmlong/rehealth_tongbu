-- 存量归属数据体检（执行 V20260825_2 迁移前必须运行并人工确认）。
-- 用法：对 software 库执行本文件，逐段查看结果。
-- 除第 5 段（信息展示）外，其余各段应返回 0 行；非 0 行需先清理或确认后继续。
-- 冲突规则提醒：同一 (tenant_id, subject_ref) 存在多行 active 时，
-- 迁移将取 updated_at 最新的一行作为 active PRIMARY，其余记为 ended。

-- 1. 同一 (tenant_id, subject_ref) 存在多行 active（迁移冲突来源）
SELECT tenant_id, subject_ref, COUNT(*) AS active_rows
FROM rehealth_insurance_subject_manager
WHERE status = 'active'
GROUP BY tenant_id, subject_ref
HAVING COUNT(*) > 1;

-- 2. 归属行没有对应被保人（孤儿行，迁移时会因 JOIN 被跳过）
SELECT sm.id, sm.tenant_id, sm.subject_ref, sm.manager_user_id, sm.status
FROM rehealth_insurance_subject_manager sm
LEFT JOIN rehealth_insurance_subject s
  ON s.tenant_id = sm.tenant_id AND s.subject_ref = sm.subject_ref
WHERE s.id IS NULL;

-- 3. subject_ref 不是 64 位十六进制（非法假名）
SELECT id, tenant_id, subject_ref
FROM rehealth_insurance_subject
WHERE subject_ref NOT REGEXP '^[0-9a-f]{64}$';

-- 4. manager 不是该租户的活跃账号（离职/删除/跨租户）
SELECT sm.id, sm.tenant_id, sm.manager_user_id, sm.subject_ref
FROM rehealth_insurance_subject_manager sm
LEFT JOIN sys_user_tenant ut
  ON ut.user_id = sm.manager_user_id AND ut.tenant_id = sm.tenant_id AND ut.status = '1'
LEFT JOIN sys_user u
  ON u.id = sm.manager_user_id
WHERE ut.id IS NULL OR u.status <> 1 OR u.del_flag <> 0;

-- 5. 信息展示：将参与迁移的总行数（含 active/disabled 与冲突行）
SELECT COUNT(*) AS total_legacy_rows,
       SUM(CASE WHEN status = 'active' THEN 1 ELSE 0 END) AS active_rows,
       SUM(CASE WHEN status <> 'active' THEN 1 ELSE 0 END) AS disabled_rows
FROM rehealth_insurance_subject_manager;
