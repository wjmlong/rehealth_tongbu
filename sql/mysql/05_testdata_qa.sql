-- ============================================================================
-- ReHealth QA 测试数据生成脚本（合并版）
-- ============================================================================
-- 适用环境：仅限本地开发 / 测试环境，严禁在生产数据库执行。
-- 目标数据库：MySQL 8.x，rehealth_software（JeecgBoot 软件库）。
-- 数据命名空间：REHEALTH_QA_TD_V1（与既有 LOCAL_MULTI_INSURER_QA、
--                LOCAL_MEDICAL_TEST_SEED、LOCAL_VERSIONED_CARE_PLAN_QA 隔离）。
-- 幂等策略：全部主键使用稳定 MD5/SHA2 派生 ID + ON DUPLICATE KEY UPDATE，
--           本脚本可重复执行，不会产生重复数据，也不会破坏系统内置数据。
--
-- 推荐执行顺序（全量基线）：
--   1. backend/jeecg-boot/db/jeecgboot-mysql-5.7.sql   （平台基线，部署时执行）
--   2. sql/mysql/01_schema.sql                         （当前结构快照）
--   3. sql/mysql/03_init_data.sql                      （保险角色/权限基线）
--   4. sql/mysql/04_test_data.sql                      （既有医疗/保险样本，第 3 部分依赖其 9102 机构）
--   5. 本脚本                                          （QA 扩展样本，按部分顺序执行）
--
-- 内容结构：
--   第 1 部分：平台用户体系（9201 医疗 / 9202 保险演示机构、部门、16 个员工账号）
--   第 2 部分：9202 保险域完整链路（受保人/保单/保障/授权/绑定/干预/反馈/
--             行动/理赔/负责人/审计/导入/PSM 研究），依赖第 1 部分
--   第 3 部分：9102 机构 30 名分类 APP 用户（pending_action / pending_review /
--             in_progress / improved / 数据不全演练 各 6 条），依赖 04 的 9102 机构
--   第 4 部分：9102 版本化关怀计划与依从性执行事实（care_plan 五表链路），
--             依赖第 3 部分
--
-- 密码说明：所有测试账号密码统一为 123456。
--   加密方式与 JeecgBoot 3.9.x 一致：PBEWithMD5AndDES，
--   PasswordUtil.encrypt(plaintext=username, password='123456', salt='LQA26081')，
--   已使用 JDK 原生实现与既有种子账号哈希逐字节交叉验证。
--   手机号/邮箱均为不可路由测试标识（.invalid 保留域）。
--
-- 时间说明：统一 UTC 会话、锚点日期 2026-08-19；依从性 28 天滚动窗口随
--           执行日期漂移时需同步调整锚点（见 sql/ISSUES.md）。
--
-- 清理方式：见文件末尾“清理”一节（按命名空间删除，只影响本脚本数据）。
-- ============================================================================


-- ============================================================================
-- 第 1 部分：平台用户体系（9201/9202 租户、部门、员工账号）
-- ============================================================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';
SET @seed_actor = 'REHEALTH_QA_TD_V1';
SET @seed_time  = TIMESTAMP('2026-08-19 09:00:00');

-- ============================================================================
-- 1. 测试租户（2 个：医疗健康管理演示机构 + 保险服务演示机构）
--    租户 ID 9201 / 9202 与既有种子（9101~9103、9261、9262）不冲突
-- ============================================================================
INSERT INTO sys_tenant (
    id, name, create_time, create_by, begin_date, end_date, status, trade,
    company_size, company_address, del_flag, update_by, update_time, apply_status
) VALUES
    (9201, '睿禾演示健康管理中心（测试）', @seed_time, @seed_actor,
     TIMESTAMP('2026-08-19 00:00:00'), TIMESTAMP('2029-08-19 23:59:59'), 1, 'medical', '100-499',
     '浙江省杭州市滨江区康健路 1 号（测试地址）', 0, @seed_actor, @seed_time, 1),
    (9202, '睿安演示保险服务（测试）', @seed_time, @seed_actor,
     TIMESTAMP('2026-08-19 00:00:00'), TIMESTAMP('2029-08-19 23:59:59'), 1, '6', '50-99',
     '上海市浦东新区安康路 9 号（测试地址）', 0, @seed_actor, @seed_time, 1)
ON DUPLICATE KEY UPDATE
    name = VALUES(name), status = 1, del_flag = 0,
    update_by = VALUES(update_by), update_time = VALUES(update_time);

-- ============================================================================
-- 2. 部门树（每租户 1 个根节点 + 2 个业务部门，org_code 按租户前缀隔离）
--    org_category: 1=公司 2=部门   org_type: 1=总公司 2=部门
-- ============================================================================
INSERT INTO sys_depart (
    id, parent_id, depart_name, depart_order, org_category, org_type,
    org_code, description, status, del_flag, create_by, create_time,
    update_by, update_time, tenant_id, iz_leaf
) VALUES
    -- 9201 医疗健康管理演示机构
    (LOWER(MD5('REHEALTH_QA_TD_V1:depart:9201:ROOT')),  NULL,
     '睿禾演示健康管理中心（测试）', 0, '1', '1', 'QA9201ROOT',
     'REHEALTH_QA_TD_V1 合成演示机构（医疗）', '1', '0',
     @seed_actor, @seed_time, @seed_actor, @seed_time, 9201, 0),
    (LOWER(MD5('REHEALTH_QA_TD_V1:depart:9201:CLINIC')), LOWER(MD5('REHEALTH_QA_TD_V1:depart:9201:ROOT')),
     '健康管理部', 1, '2', '2', 'QA9201CLINIC',
     'REHEALTH_QA_TD_V1 合成演示部门（医疗）', '1', '0',
     @seed_actor, @seed_time, @seed_actor, @seed_time, 9201, 1),
    (LOWER(MD5('REHEALTH_QA_TD_V1:depart:9201:MEDSVC')), LOWER(MD5('REHEALTH_QA_TD_V1:depart:9201:ROOT')),
     '医学服务部', 2, '2', '2', 'QA9201MEDSVC',
     'REHEALTH_QA_TD_V1 合成演示部门（医疗）', '1', '0',
     @seed_actor, @seed_time, @seed_actor, @seed_time, 9201, 1),
    -- 9202 保险服务演示机构
    (LOWER(MD5('REHEALTH_QA_TD_V1:depart:9202:ROOT')),  NULL,
     '睿安演示保险服务（测试）', 0, '1', '1', 'QA9202ROOT',
     'REHEALTH_QA_TD_V1 合成演示机构（保险）', '1', '0',
     @seed_actor, @seed_time, @seed_actor, @seed_time, 9202, 0),
    (LOWER(MD5('REHEALTH_QA_TD_V1:depart:9202:HEALTH')), LOWER(MD5('REHEALTH_QA_TD_V1:depart:9202:ROOT')),
     '健康险运营部', 1, '2', '2', 'QA9202HEALTH',
     'REHEALTH_QA_TD_V1 合成演示部门（保险）', '1', '0',
     @seed_actor, @seed_time, @seed_actor, @seed_time, 9202, 1),
    (LOWER(MD5('REHEALTH_QA_TD_V1:depart:9202:RISK')), LOWER(MD5('REHEALTH_QA_TD_V1:depart:9202:ROOT')),
     '精算风控部', 2, '2', '2', 'QA9202RISK',
     'REHEALTH_QA_TD_V1 合成演示部门（保险）', '1', '0',
     @seed_actor, @seed_time, @seed_actor, @seed_time, 9202, 1)
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id), depart_name = VALUES(depart_name),
    depart_order = VALUES(depart_order), org_code = VALUES(org_code),
    description = VALUES(description), status = '1', del_flag = '0',
    tenant_id = VALUES(tenant_id), iz_leaf = VALUES(iz_leaf),
    update_by = VALUES(update_by), update_time = VALUES(update_time);

-- ============================================================================
-- 3. 角色补齐（复用项目标准角色码；已存在时自动跳过，不产生重复角色）
--    hospital_admin / hospital_doctor / app_user 由 04_test_data.sql 医疗段创建；
--    此处幂等补齐，保证本脚本可脱离 04 脚本独立执行。
-- ============================================================================
INSERT INTO sys_role (
    id, role_name, role_code, description, create_by, create_time, update_by, update_time, tenant_id
)
SELECT LOWER(MD5('REHEALTH_QA_TD_V1:role:hospital_admin')), '医疗机构管理员', 'hospital_admin',
       'REHEALTH_QA_TD_V1 synthetic role; only created when the canonical role is absent',
       @seed_actor, @seed_time, @seed_actor, @seed_time, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'hospital_admin');

INSERT INTO sys_role (
    id, role_name, role_code, description, create_by, create_time, update_by, update_time, tenant_id
)
SELECT LOWER(MD5('REHEALTH_QA_TD_V1:role:hospital_doctor')), '医疗机构医生', 'hospital_doctor',
       'REHEALTH_QA_TD_V1 synthetic role; only created when the canonical role is absent',
       @seed_actor, @seed_time, @seed_actor, @seed_time, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'hospital_doctor');

INSERT INTO sys_role (
    id, role_name, role_code, description, create_by, create_time, update_by, update_time, tenant_id
)
SELECT LOWER(MD5('REHEALTH_QA_TD_V1:role:app_user')), 'APP 用户', 'app_user',
       'REHEALTH_QA_TD_V1 synthetic role; only created when the canonical role is absent',
       @seed_actor, @seed_time, @seed_actor, @seed_time, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'app_user');

-- 医疗角色绑定患者数据查看权限（权限由 04_test_data.sql 创建；不存在时自动跳过）
INSERT INTO sys_role_permission (id, role_id, permission_id, operate_date, operate_ip)
SELECT LOWER(MD5(CONCAT('REHEALTH_QA_TD_V1:role-permission:', role.role_code))),
       role.id, permission.id, @seed_time, '127.0.0.1'
FROM sys_role role
JOIN sys_permission permission ON permission.perms = 'rehealth:admin:patient:view'
WHERE role.role_code IN ('hospital_admin', 'hospital_doctor')
ON DUPLICATE KEY UPDATE
    role_id = VALUES(role_id), permission_id = VALUES(permission_id), operate_date = VALUES(operate_date);

-- ============================================================================
-- 4. 测试账号（16 个，统一密码 123456）
--    覆盖场景：
--      - 正常账号（管理员 / 部门经理 / 运营专员 / 数据分析员 / 只读查看员）
--      - 冻结账号  qa9201_frozen / qa9202_frozen   （status=2，登录应被拒绝）
--      - 逻辑删除  qa9201_deleted / qa9202_deleted （del_flag=1，列表默认不可见）
--      - 未绑手机号 qa9201_nophone                 （phone/email 为空，测试空值展示）
--      - 待接受邀请 qa9202_invitee                 （sys_user_tenant.status='5'）
--      - 长姓名边界 qa9202_viewer                  （realname 接近上限但未超长）
--    手机号 0009201xxxx / 0009202xxxx 为不可路由测试号码；
--    邮箱使用 .invalid 保留域，均不与真实用户冲突。
-- ============================================================================
INSERT INTO sys_user (
    id, username, realname, password, salt, birthday, sex, email, phone, org_code,
    status, del_flag, activiti_sync, work_no, create_by, create_time,
    update_by, update_time, user_identity, login_tenant_id, sort, iz_hide_contact
) VALUES
    -- 9201 医疗健康管理演示机构
    (LOWER(MD5('REHEALTH_QA_TD_V1:user:qa9201_admin')),    'qa9201_admin',
     '张伟', 'b563010c813d94e14b597f8eecede536', 'LQA26081', '1985-03-12', 1,
     'qa9201.admin@local.rehealth.invalid', '00092010001', NULL,
     1, 0, 0, 'QA-9201-001', @seed_actor, @seed_time, @seed_actor, @seed_time, 1, 9201, 920101, '0'),
    (LOWER(MD5('REHEALTH_QA_TD_V1:user:qa9201_manager')),  'qa9201_manager',
     '李娜', '78dce48a37faf76d6410f23c64e218fd', 'LQA26081', '1990-07-25', 2,
     'qa9201.manager@local.rehealth.invalid', '00092010002', NULL,
     1, 0, 0, 'QA-9201-002', @seed_actor, @seed_time, @seed_actor, @seed_time, 1, 9201, 920102, '0'),
    (LOWER(MD5('REHEALTH_QA_TD_V1:user:qa9201_operator')), 'qa9201_operator',
     '王磊', 'fb78b94ba8ae0c256e497dd418aa65ea', 'LQA26081', '1992-01-08', 1,
     'qa9201.operator@local.rehealth.invalid', '00092010003', NULL,
     1, 0, 0, 'QA-9201-003', @seed_actor, @seed_time, @seed_actor, @seed_time, 1, 9201, 920103, '0'),
    (LOWER(MD5('REHEALTH_QA_TD_V1:user:qa9201_analyst')),  'qa9201_analyst',
     '陈晨', 'b563010c813d94e1edeacf707d6b0ec8', 'LQA26081', '1995-11-19', 2,
     'qa9201.analyst@local.rehealth.invalid', '00092010004', NULL,
     1, 0, 0, 'QA-9201-004', @seed_actor, @seed_time, @seed_actor, @seed_time, 1, 9201, 920104, '0'),
    (LOWER(MD5('REHEALTH_QA_TD_V1:user:qa9201_viewer')),   'qa9201_viewer',
     '赵敏', '3b29c75dd4debb8bc7011bcb986a7d89', 'LQA26081', NULL, 2,
     'qa9201.viewer@local.rehealth.invalid', '00092010005', NULL,
     1, 0, 0, 'QA-9201-005', @seed_actor, @seed_time, @seed_actor, @seed_time, 1, 9201, 920105, '0'),
    (LOWER(MD5('REHEALTH_QA_TD_V1:user:qa9201_frozen')),   'qa9201_frozen',
     '孙立（已冻结）', 'bb5b231a8e045cc502d5a2b11602c199', 'LQA26081', '1988-05-30', 1,
     'qa9201.frozen@local.rehealth.invalid', '00092010006', NULL,
     2, 0, 0, 'QA-9201-006', @seed_actor, @seed_time, @seed_actor, @seed_time, 1, 9201, 920106, '0'),
    (LOWER(MD5('REHEALTH_QA_TD_V1:user:qa9201_deleted')),  'qa9201_deleted',
     '周芳（已删除）', 'd829787ac3779eef263bb678b0f81033', 'LQA26081', '1993-09-14', 2,
     'qa9201.deleted@local.rehealth.invalid', '00092010007', NULL,
     1, 1, 0, 'QA-9201-007', @seed_actor, @seed_time, @seed_actor, @seed_time, 1, 9201, 920107, '0'),
    (LOWER(MD5('REHEALTH_QA_TD_V1:user:qa9201_nophone')),  'qa9201_nophone',
     '吴强（未绑定联系方式）', 'c827d7ac3e190092d7aae24815019302', 'LQA26081', NULL, 1,
     NULL, NULL, NULL,
     1, 0, 0, 'QA-9201-008', @seed_actor, @seed_time, @seed_actor, @seed_time, 1, 9201, 920108, '0'),
    -- 9202 保险服务演示机构
    (LOWER(MD5('REHEALTH_QA_TD_V1:user:qa9202_admin')),    'qa9202_admin',
     '郑晓雯', '76e557f417efd259f6514c52f000e9b3', 'LQA26081', '1987-06-18', 2,
     'qa9202.admin@local.rehealth.invalid', '00092020001', NULL,
     1, 0, 0, 'QA-9202-001', @seed_actor, @seed_time, @seed_actor, @seed_time, 1, 9202, 920201, '0'),
    (LOWER(MD5('REHEALTH_QA_TD_V1:user:qa9202_manager')),  'qa9202_manager',
     '冯志远', 'fb7deda8e8e1a3a2564d82e3449cf621', 'LQA26081', '1984-10-02', 1,
     'qa9202.manager@local.rehealth.invalid', '00092020002', NULL,
     1, 0, 0, 'QA-9202-002', @seed_actor, @seed_time, @seed_actor, @seed_time, 1, 9202, 920202, '0'),
    (LOWER(MD5('REHEALTH_QA_TD_V1:user:qa9202_operator')), 'qa9202_operator',
     '蒋婷', 'b4717d73e14fa2cc63b61087be6dc935', 'LQA26081', '1996-04-27', 2,
     'qa9202.operator@local.rehealth.invalid', '00092020003', NULL,
     1, 0, 0, 'QA-9202-003', @seed_actor, @seed_time, @seed_actor, @seed_time, 1, 9202, 920203, '0'),
    (LOWER(MD5('REHEALTH_QA_TD_V1:user:qa9202_analyst')),  'qa9202_analyst',
     '沈思远', '76e557f417efd259c59511ad94f9dfa8', 'LQA26081', '1991-12-05', 1,
     'qa9202.analyst@local.rehealth.invalid', '00092020004', NULL,
     1, 0, 0, 'QA-9202-004', @seed_actor, @seed_time, @seed_actor, @seed_time, 1, 9202, 920204, '0'),
    (LOWER(MD5('REHEALTH_QA_TD_V1:user:qa9202_viewer')),   'qa9202_viewer',
     '韩雪（演示机构·数据合规与隐私保护委员会观察员·测试用长姓名边界）', 'bdd3e758d2dd85d8cc1c1cc78dc1941b', 'LQA26081', NULL, 2,
     'qa9202.viewer@local.rehealth.invalid', '00092020005', NULL,
     1, 0, 0, 'QA-9202-005', @seed_actor, @seed_time, @seed_actor, @seed_time, 1, 9202, 920205, '0'),
    (LOWER(MD5('REHEALTH_QA_TD_V1:user:qa9202_frozen')),   'qa9202_frozen',
     '曹斌（已冻结）', '4de4ba68c523a5a12b3057d4337e5059', 'LQA26081', '1989-08-21', 1,
     'qa9202.frozen@local.rehealth.invalid', '00092020006', NULL,
     2, 0, 0, 'QA-9202-006', @seed_actor, @seed_time, @seed_actor, @seed_time, 1, 9202, 920206, '0'),
    (LOWER(MD5('REHEALTH_QA_TD_V1:user:qa9202_deleted')),  'qa9202_deleted',
     '彭丽（已删除）', '8e8c69b90d9bd5f5d1f0b14658d418ee', 'LQA26081', '1994-02-16', 2,
     'qa9202.deleted@local.rehealth.invalid', '00092020007', NULL,
     1, 1, 0, 'QA-9202-007', @seed_actor, @seed_time, @seed_actor, @seed_time, 1, 9202, 920207, '0'),
    (LOWER(MD5('REHEALTH_QA_TD_V1:user:qa9202_invitee')),  'qa9202_invitee',
     '曾伟（待接受邀请）', 'ac60bdcc5eab3f3845108c21f91833b5', 'LQA26081', '1997-07-07', 1,
     'qa9202.invitee@local.rehealth.invalid', '00092020008', NULL,
     1, 0, 0, 'QA-9202-008', @seed_actor, @seed_time, @seed_actor, @seed_time, 1, 9202, 920208, '0')
ON DUPLICATE KEY UPDATE
    realname = VALUES(realname), password = VALUES(password), salt = VALUES(salt),
    birthday = VALUES(birthday), sex = VALUES(sex), email = VALUES(email),
    phone = VALUES(phone), status = VALUES(status), del_flag = VALUES(del_flag),
    work_no = VALUES(work_no), login_tenant_id = VALUES(login_tenant_id),
    sort = VALUES(sort), update_by = VALUES(update_by), update_time = VALUES(update_time);

-- ============================================================================
-- 5. 用户-租户成员关系（sys_user_tenant）
--    status: '1' 正常成员；'5' 待接受邀请（qa9202_invitee，用于测试邀请流程）
-- ============================================================================
INSERT INTO sys_user_tenant (
    id, user_id, tenant_id, status, create_by, create_time, update_by, update_time
)
SELECT LOWER(MD5(CONCAT('REHEALTH_QA_TD_V1:membership:', m.tenant_id, ':', m.username))),
       u.id, m.tenant_id, m.membership_status, @seed_actor, @seed_time, @seed_actor, @seed_time
FROM (
    SELECT 'qa9201_admin' username,    9201 tenant_id, '1' membership_status
    UNION ALL SELECT 'qa9201_manager',  9201, '1'
    UNION ALL SELECT 'qa9201_operator', 9201, '1'
    UNION ALL SELECT 'qa9201_analyst',  9201, '1'
    UNION ALL SELECT 'qa9201_viewer',   9201, '1'
    UNION ALL SELECT 'qa9201_frozen',   9201, '1'
    UNION ALL SELECT 'qa9201_deleted',  9201, '1'
    UNION ALL SELECT 'qa9201_nophone',  9201, '1'
    UNION ALL SELECT 'qa9202_admin',    9202, '1'
    UNION ALL SELECT 'qa9202_manager',  9202, '1'
    UNION ALL SELECT 'qa9202_operator', 9202, '1'
    UNION ALL SELECT 'qa9202_analyst',  9202, '1'
    UNION ALL SELECT 'qa9202_viewer',   9202, '1'
    UNION ALL SELECT 'qa9202_frozen',   9202, '1'
    UNION ALL SELECT 'qa9202_deleted',  9202, '1'
    UNION ALL SELECT 'qa9202_invitee',  9202, '5'
) m
JOIN sys_user u ON u.username = m.username
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id), tenant_id = VALUES(tenant_id), status = VALUES(status),
    update_by = VALUES(update_by), update_time = VALUES(update_time);

-- ============================================================================
-- 6. 用户-部门关系（sys_user_depart）
--    管理员挂根部门；经理/查看员挂部门一；运营/分析挂部门二
-- ============================================================================
INSERT INTO sys_user_depart (ID, user_id, dep_id)
SELECT LOWER(MD5(CONCAT('REHEALTH_QA_TD_V1:user-depart:', u.username))),
       u.id, d.id
FROM (
    SELECT 'qa9201_admin' username,    'QA9201ROOT'   org_code
    UNION ALL SELECT 'qa9201_manager', 'QA9201CLINIC'
    UNION ALL SELECT 'qa9201_operator','QA9201MEDSVC'
    UNION ALL SELECT 'qa9201_analyst', 'QA9201MEDSVC'
    UNION ALL SELECT 'qa9201_viewer',  'QA9201CLINIC'
    UNION ALL SELECT 'qa9201_frozen',  'QA9201MEDSVC'
    UNION ALL SELECT 'qa9201_deleted', 'QA9201CLINIC'
    UNION ALL SELECT 'qa9201_nophone', 'QA9201MEDSVC'
    UNION ALL SELECT 'qa9202_admin',   'QA9202ROOT'
    UNION ALL SELECT 'qa9202_manager', 'QA9202HEALTH'
    UNION ALL SELECT 'qa9202_operator','QA9202HEALTH'
    UNION ALL SELECT 'qa9202_analyst', 'QA9202RISK'
    UNION ALL SELECT 'qa9202_viewer',  'QA9202HEALTH'
    UNION ALL SELECT 'qa9202_frozen',  'QA9202RISK'
    UNION ALL SELECT 'qa9202_deleted', 'QA9202HEALTH'
    UNION ALL SELECT 'qa9202_invitee', 'QA9202HEALTH'
) rel
JOIN sys_user u   ON u.username = rel.username
JOIN sys_depart d ON d.org_code = rel.org_code AND d.del_flag = '0'
ON DUPLICATE KEY UPDATE dep_id = VALUES(dep_id);

-- ============================================================================
-- 7. 用户-角色关系（sys_user_role）
--    9201 医疗机构：hospital_admin / hospital_doctor
--    9202 保险机构：insurance_org_admin / insurance_department_manager /
--                  insurance_operator / insurer_analyst / insurer_viewer
--    绑定使用 INSERT..SELECT + JOIN 角色表：角色缺失时自动跳过该行，不产生孤儿数据
-- ============================================================================
INSERT INTO sys_user_role (id, user_id, role_id, tenant_id)
SELECT LOWER(MD5(CONCAT('REHEALTH_QA_TD_V1:user-role:', rel.tenant_id, ':', rel.username, ':', rel.role_code))),
       u.id, r.id, rel.tenant_id
FROM (
    SELECT 'qa9201_admin'    username, 9201 tenant_id, 'hospital_admin'                role_code
    UNION ALL SELECT 'qa9201_manager',  9201, 'hospital_doctor'
    UNION ALL SELECT 'qa9201_operator', 9201, 'hospital_doctor'
    UNION ALL SELECT 'qa9201_analyst',  9201, 'hospital_doctor'
    UNION ALL SELECT 'qa9201_viewer',   9201, 'hospital_doctor'
    UNION ALL SELECT 'qa9201_frozen',   9201, 'hospital_doctor'
    UNION ALL SELECT 'qa9201_deleted',  9201, 'hospital_doctor'
    UNION ALL SELECT 'qa9201_nophone',  9201, 'hospital_doctor'
    UNION ALL SELECT 'qa9202_admin',    9202, 'insurance_org_admin'
    UNION ALL SELECT 'qa9202_manager',  9202, 'insurance_department_manager'
    UNION ALL SELECT 'qa9202_operator', 9202, 'insurance_operator'
    UNION ALL SELECT 'qa9202_analyst',  9202, 'insurer_analyst'
    UNION ALL SELECT 'qa9202_viewer',   9202, 'insurer_viewer'
    UNION ALL SELECT 'qa9202_frozen',   9202, 'insurer_viewer'
    UNION ALL SELECT 'qa9202_deleted',  9202, 'insurer_viewer'
    UNION ALL SELECT 'qa9202_invitee',  9202, 'insurer_viewer'
) rel
JOIN sys_user u ON u.username = rel.username
JOIN sys_role r ON r.role_code = rel.role_code
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id), role_id = VALUES(role_id), tenant_id = VALUES(tenant_id);

-- ============================================================================
-- 8. 执行后核对（可选）：确认各表数量符合预期
--    预期：sys_tenant 2；sys_depart 6；sys_user 16；sys_user_tenant 16；
--          sys_user_depart 16；sys_user_role 16（角色缺失时相应行数会减少）
-- ============================================================================
SELECT 'sys_tenant' table_name, COUNT(*) cnt FROM sys_tenant WHERE id IN (9201, 9202)
UNION ALL SELECT 'sys_depart', COUNT(*) FROM sys_depart WHERE tenant_id IN (9201, 9202) AND del_flag = '0'
UNION ALL SELECT 'sys_user', COUNT(*) FROM sys_user WHERE username LIKE 'qa9201\_%' OR username LIKE 'qa9202\_%'
UNION ALL SELECT 'sys_user_tenant', COUNT(*) FROM sys_user_tenant WHERE tenant_id IN (9201, 9202)
UNION ALL SELECT 'sys_user_depart', COUNT(*) FROM sys_user_depart WHERE user_id IN (
    SELECT id FROM sys_user WHERE username LIKE 'qa9201\_%' OR username LIKE 'qa9202\_%')
UNION ALL SELECT 'sys_user_role', COUNT(*) FROM sys_user_role WHERE tenant_id IN (9201, 9202);

-- 角色绑定缺漏提示（若依赖的角色未创建，此处会显示未绑定的用户）
SELECT u.username, '未绑定任何角色' issue
FROM sys_user u
WHERE (u.username LIKE 'qa9201\_%' OR u.username LIKE 'qa9202\_%')
  AND NOT EXISTS (SELECT 1 FROM sys_user_role ur WHERE ur.user_id = u.id);

-- ============================================================================

-- ============================================================================
-- 第 2 部分：9202 保险域完整链路
-- ============================================================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';
SET @seed_actor = 'REHEALTH_QA_TD_V1';
SET @seed_time  = TIMESTAMP('2026-08-19 09:00:00');
SET @anchor_date = DATE('2026-08-19');
SET @tenant = 9202;

-- ============================================================================
-- 1. 角色补齐（app_user / insurance_service_user，已存在时自动跳过）
-- ============================================================================
INSERT INTO sys_role (
    id, role_name, role_code, description, create_by, create_time, update_by, update_time, tenant_id
)
SELECT LOWER(MD5('REHEALTH_QA_TD_V1:role:app_user')), 'APP 用户', 'app_user',
       'REHEALTH_QA_TD_V1 synthetic role; only created when the canonical role is absent',
       @seed_actor, @seed_time, @seed_actor, @seed_time, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'app_user');

INSERT INTO sys_role (
    id, role_name, role_code, description, create_by, create_time, update_by, update_time, tenant_id
)
SELECT LOWER(MD5('REHEALTH_QA_TD_V1:role:insurance_service_user')), '保险服务用户', 'insurance_service_user',
       'REHEALTH_QA_TD_V1 synthetic role; only created when the canonical role is absent',
       @seed_actor, @seed_time, @seed_actor, @seed_time, 0
WHERE NOT EXISTS (SELECT 1 FROM sys_role WHERE role_code = 'insurance_service_user');

-- ============================================================================
-- 2. APP 受保用户（12 个，统一密码 123456，salt LQA26081）
--    手机号 00092029xxx / 邮箱 .invalid 保留域，均为不可路由测试标识。
--    与既有 seed 一致：APP 账号是服务接受者，不加入 sys_user_tenant。
-- ============================================================================
INSERT INTO sys_user (
    id, username, realname, password, salt, birthday, sex, email, phone,
    status, del_flag, activiti_sync, work_no, create_by, create_time,
    update_by, update_time, user_identity, login_tenant_id, sort, iz_hide_contact
)
SELECT
    LOWER(MD5(CONCAT('REHEALTH_QA_TD_V1:user:', u.username))),
    u.username, u.realname, u.password_hash, 'LQA26081',
    DATE_SUB(@anchor_date, INTERVAL u.age YEAR), u.sex, u.email, u.phone,
    1, 0, 0, CONCAT('APP-9202-', LPAD(u.no, 3, '0')),
    @seed_actor, @seed_time, @seed_actor, @seed_time, 1, 0,
    920200 + u.no, '0'
FROM (
    SELECT 1 no, 'qa9202app01' username, '徐志远' realname, 44 age, 1 sex,
           '5d4c65f7783c11e0b42d29665fa55edb' password_hash,
           'qa9202app01@app.qa.invalid' email, '00092029001' phone
    UNION ALL SELECT 2,  'qa9202app02', '方雅兰', 49, 2,
           '5d4c65f7783c11e058843b30dda8b93c',
           'qa9202app02@app.qa.invalid', '00092029002'
    UNION ALL SELECT 3,  'qa9202app03', '马国栋', 54, 1,
           '5d4c65f7783c11e04d0759d5e19617c4',
           'qa9202app03@app.qa.invalid', '00092029003'
    UNION ALL SELECT 4,  'qa9202app04', '宋慧芳', 59, 2,
           '5d4c65f7783c11e08f0b62c06738fc4b',
           'qa9202app04@app.qa.invalid', '00092029004'
    UNION ALL SELECT 5,  'qa9202app05', '高建平', 42, 1,
           '5d4c65f7783c11e04357f26730900b3e',
           'qa9202app05@app.qa.invalid', '00092029005'
    UNION ALL SELECT 6,  'qa9202app06', '唐婉',   47, 2,
           '5d4c65f7783c11e016c232eb87177256',
           'qa9202app06@app.qa.invalid', '00092029006'
    UNION ALL SELECT 7,  'qa9202app07', '罗永康', 55, 1,
           '5d4c65f7783c11e057f7eb3582586eeb',
           'qa9202app07@app.qa.invalid', '00092029007'
    UNION ALL SELECT 8,  'qa9202app08', '潘淑华', 62, 2,
           '5d4c65f7783c11e0eac2efc0cf9f16ed',
           'qa9202app08@app.qa.invalid', '00092029008'
    UNION ALL SELECT 9,  'qa9202app09', '崔立新', 45, 1,
           '5d4c65f7783c11e0c752017fb9251532',
           'qa9202app09@app.qa.invalid', '00092029009'
    UNION ALL SELECT 10, 'qa9202app10', '邓丽娟', 50, 2,
           '5d4c65f7783c11e0913e84647751891c',
           'qa9202app10@app.qa.invalid', '00092029010'
    UNION ALL SELECT 11, 'qa9202app11', '陆振华', 57, 1,
           '5d4c65f7783c11e0e3c43dbcae2652ec',
           'qa9202app11@app.qa.invalid', '00092029011'
    UNION ALL SELECT 12, 'qa9202app12', '冯秀英', 64, 2,
           '5d4c65f7783c11e0ad0eadf9ff51f988',
           'qa9202app12@app.qa.invalid', '00092029012'
) u
ON DUPLICATE KEY UPDATE
    realname = VALUES(realname), password = VALUES(password), salt = VALUES(salt),
    birthday = VALUES(birthday), sex = VALUES(sex), email = VALUES(email),
    phone = VALUES(phone), status = 1, del_flag = 0, work_no = VALUES(work_no),
    update_by = VALUES(update_by), update_time = VALUES(update_time),
    login_tenant_id = 0, sort = VALUES(sort), iz_hide_contact = '0';

-- APP 账号归类：全局 app_user + 保险服务用户，且不复用租户级角色关系
DELETE user_role
FROM sys_user_role user_role
JOIN sys_user u ON u.id = user_role.user_id
WHERE u.username LIKE 'qa9202app%'
  AND user_role.tenant_id <> 0;

INSERT INTO sys_user_role (id, user_id, role_id, tenant_id)
SELECT LOWER(MD5(CONCAT('REHEALTH_QA_TD_V1:user-role:', u.username, ':', role.role_code))),
       u.id, role.id, 0
FROM sys_user u
JOIN sys_role role ON role.role_code IN ('app_user', 'insurance_service_user')
WHERE u.username LIKE 'qa9202app%'
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id), role_id = VALUES(role_id), tenant_id = 0;

-- ============================================================================
-- 3. 最小患者档案（保险工作台联调所需；完整健康数据由患者域脚本补充）
--    risk_base 仅用于派生 family_history/smoking 等字段，非真实模型输出。
-- ============================================================================
INSERT INTO rehealth_patient_profile (
    id, user_id, name, gender, age, height_cm, weight_kg, bmi,
    family_history, smoking, drinking, diabetes_history, hypertension_history,
    profile_version, profile_json, created_at, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:profile:', p.username), 256)),
    LOWER(MD5(CONCAT('REHEALTH_QA_TD_V1:user:', p.username))),
    p.realname, p.gender, p.age, p.height_cm, p.weight_kg, p.bmi,
    IF(p.risk_base >= 0.45, 1, 0), IF(p.risk_base >= 0.65, 1, 0),
    IF(p.no % 4 = 0, 1, 0), IF(p.risk_base >= 0.70, 1, 0),
    IF(p.risk_base >= 0.50, 1, 0), 1,
    JSON_OBJECT('source', 'REHEALTH_QA_TD_V1', 'scenario', 'insurance_workbench_basic_profile',
                'synthetic', TRUE, 'clinicalUseAllowed', FALSE),
    @seed_time, @seed_time
FROM (
    SELECT 1 no, 'qa9202app01' username, '徐志远' realname, 'male' gender, 44 age,
           175.00 height_cm, 70.10 weight_kg, 22.89 bmi, 0.26 risk_base
    UNION ALL SELECT 2,  'qa9202app02', '方雅兰', 'female', 49, 163.00, 61.20, 23.03, 0.35
    UNION ALL SELECT 3,  'qa9202app03', '马国栋', 'male',   54, 172.00, 78.40, 26.50, 0.48
    UNION ALL SELECT 4,  'qa9202app04', '宋慧芳', 'female', 59, 160.00, 71.00, 27.73, 0.70
    UNION ALL SELECT 5,  'qa9202app05', '高建平', 'male',   42, 176.00, 68.30, 22.05, 0.23
    UNION ALL SELECT 6,  'qa9202app06', '唐婉',   'female', 47, 164.00, 64.70, 24.06, 0.33
    UNION ALL SELECT 7,  'qa9202app07', '罗永康', 'male',   55, 171.00, 78.90, 26.98, 0.52
    UNION ALL SELECT 8,  'qa9202app08', '潘淑华', 'female', 62, 160.00, 74.60, 29.14, 0.72
    UNION ALL SELECT 9,  'qa9202app09', '崔立新', 'male',   45, 176.00, 71.60, 23.11, 0.28
    UNION ALL SELECT 10, 'qa9202app10', '邓丽娟', 'female', 50, 164.00, 63.50, 23.61, 0.39
    UNION ALL SELECT 11, 'qa9202app11', '陆振华', 'male',   57, 173.00, 81.20, 27.13, 0.58
    UNION ALL SELECT 12, 'qa9202app12', '冯秀英', 'female', 64, 159.00, 75.20, 29.75, 0.76
) p
ON DUPLICATE KEY UPDATE
    name = VALUES(name), gender = VALUES(gender), age = VALUES(age),
    height_cm = VALUES(height_cm), weight_kg = VALUES(weight_kg), bmi = VALUES(bmi),
    family_history = VALUES(family_history), smoking = VALUES(smoking),
    drinking = VALUES(drinking), diabetes_history = VALUES(diabetes_history),
    hypertension_history = VALUES(hypertension_history),
    profile_json = VALUES(profile_json), updated_at = VALUES(updated_at);

-- ============================================================================
-- 4. 保险机构档案（9202 租户一条）
-- ============================================================================
INSERT INTO rehealth_insurance_tenant_profile (
    id, tenant_id, organization_name, license_no, insurance_type,
    compliance_email, regulatory_email, data_retention_years,
    mask_sensitive_data, access_log_enabled, notification_config_json,
    version, created_at, updated_at
) VALUES
    (LOWER(SHA2('REHEALTH_QA_TD_V1:tenant-profile:9202', 256)), 9202,
     '睿安演示保险服务（测试）', 'RH-INS-QA-9202', 'mixed',
     'compliance.9202@local.rehealth.invalid', 'regulatory.9202@local.rehealth.invalid',
     7, 1, 1,
     JSON_OBJECT('fixture', TRUE, 'source', 'REHEALTH_QA_TD_V1'),
     1, @seed_time, @seed_time)
ON DUPLICATE KEY UPDATE
    organization_name = VALUES(organization_name), license_no = VALUES(license_no),
    insurance_type = VALUES(insurance_type), compliance_email = VALUES(compliance_email),
    regulatory_email = VALUES(regulatory_email), version = VALUES(version),
    updated_at = VALUES(updated_at);

-- ============================================================================
-- 5. 保险主体（12 个受保人）
--    subject_ref 使用可读业务编号 QA9202-S0001..0012（租户内唯一）。
--    S0007 停保+撤销授权；S0012 退出+授权待确认（异常/边界场景）。
-- ============================================================================
INSERT INTO rehealth_insurance_subject (
    id, tenant_id, subject_ref, rehealth_user_id, external_subject_ref_hash,
    enrollment_status, consent_status, consent_version, consented_at,
    source_system, source_record_id, metadata_json, created_at, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:subject:9202:', s.no), 256)),
    @tenant, CONCAT('QA9202-S', LPAD(s.no, 4, '0')),
    LOWER(MD5(CONCAT('REHEALTH_QA_TD_V1:user:', s.username))),
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:external:9202:', s.username), 256)),
    s.enrollment_status, s.consent_status, 'qa-td-v1',
    CASE WHEN s.consent_status = 'granted' THEN DATE_SUB(@seed_time, INTERVAL 180 DAY) ELSE NULL END,
    'REHEALTH_QA_TD_V1', CONCAT('subject-9202-', LPAD(s.no, 4, '0')),
    JSON_OBJECT('synthetic', TRUE, 'clinicalUseAllowed', FALSE),
    DATE_SUB(@seed_time, INTERVAL 180 DAY), @seed_time
FROM (
    SELECT 1 no, 'qa9202app01' username, 'active' enrollment_status, 'granted' consent_status
    UNION ALL SELECT 2,  'qa9202app02', 'active',  'granted'
    UNION ALL SELECT 3,  'qa9202app03', 'active',  'granted'
    UNION ALL SELECT 4,  'qa9202app04', 'active',  'granted'
    UNION ALL SELECT 5,  'qa9202app05', 'active',  'granted'
    UNION ALL SELECT 6,  'qa9202app06', 'active',  'granted'
    UNION ALL SELECT 7,  'qa9202app07', 'suspended', 'revoked'
    UNION ALL SELECT 8,  'qa9202app08', 'active',  'granted'
    UNION ALL SELECT 9,  'qa9202app09', 'active',  'granted'
    UNION ALL SELECT 10, 'qa9202app10', 'active',  'granted'
    UNION ALL SELECT 11, 'qa9202app11', 'active',  'granted'
    UNION ALL SELECT 12, 'qa9202app12', 'withdrawn', 'pending'
) s
ON DUPLICATE KEY UPDATE
    rehealth_user_id = VALUES(rehealth_user_id),
    enrollment_status = VALUES(enrollment_status), consent_status = VALUES(consent_status),
    consented_at = VALUES(consented_at), metadata_json = VALUES(metadata_json),
    updated_at = VALUES(updated_at);

-- ============================================================================
-- 6. 保单（14 张）
--    每人 1 张主保单；P0013 为 S0003 的已过期历史保单；P0014 为 S0005 的已退保保单；
--    P0012（S0012）为待生效保单，与主体退出状态呼应。
--    金额逻辑：免赔额只在住院/医疗类保单收取；起止日期满足 effective_on < expires_on。
-- ============================================================================
INSERT INTO rehealth_insurance_policy (
    id, tenant_id, policy_no, product_code, product_name, policy_type,
    policyholder_subject_ref, insured_subject_ref, coverage_amount,
    premium_amount, deductible_amount, waiting_period_days, effective_on,
    expires_on, status, source_system, source_record_id, metadata_json,
    created_at, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:policy:9202:', pol.no), 256)),
    @tenant, CONCAT('RH-9202-POL-', LPAD(pol.no, 4, '0')),
    pol.product_code, pol.product_name, pol.policy_type,
    CASE WHEN pol.policy_type = 'group_medical' THEN NULL
         ELSE CONCAT('QA9202-S', LPAD(pol.subject_no, 4, '0')) END,
    CONCAT('QA9202-S', LPAD(pol.subject_no, 4, '0')),
    pol.coverage_amount, pol.premium_amount, pol.deductible_amount,
    pol.waiting_period_days,
    CASE WHEN pol.eff_days_ago < 0 THEN DATE_ADD(@anchor_date, INTERVAL (-pol.eff_days_ago) DAY)
         ELSE DATE_SUB(@anchor_date, INTERVAL pol.eff_days_ago DAY) END,
    DATE_ADD(
        CASE WHEN pol.eff_days_ago < 0 THEN DATE_ADD(@anchor_date, INTERVAL (-pol.eff_days_ago) DAY)
             ELSE DATE_SUB(@anchor_date, INTERVAL pol.eff_days_ago DAY) END,
        INTERVAL pol.dur_days DAY),
    pol.status, 'REHEALTH_QA_TD_V1', CONCAT('policy-9202-', LPAD(pol.no, 4, '0')),
    JSON_OBJECT('synthetic', TRUE, 'clinicalUseAllowed', FALSE, 'channel', pol.channel),
    DATE_SUB(@seed_time, INTERVAL 180 DAY), @seed_time
FROM (
    SELECT 1 no, 1 subject_no, 'GROUP-MED' product_code, '悦享健康团体医疗保障计划' product_name,
           'group_medical' policy_type, 500000.00 coverage_amount, 980.00 premium_amount,
           500.00 deductible_amount, 0 waiting_period_days, 180 eff_days_ago, 365 dur_days,
           'active' status, '企业团险' channel
    UNION ALL SELECT 2,  2,  'LONG-MED', '安心守护长期医疗保障计划', 'long_term_medical',
           1000000.00, 1075.00, 500.00, 30, 180, 365, 'active', '银行保险'
    UNION ALL SELECT 3,  3,  'CI-PLUS', '康护无忧重大疾病保障计划', 'critical_illness',
           800000.00, 1170.00, 0.00, 90, 180, 365, 'active', '保险经纪'
    UNION ALL SELECT 4,  4,  'CVD-CARE', '臻享心脑血管专项保障计划', 'cvd_management',
           300000.00, 1265.00, 500.00, 30, 180, 365, 'active', '个人直销'
    UNION ALL SELECT 5,  5,  'GROUP-MED', '悦享健康团体医疗保障计划', 'group_medical',
           500000.00, 1360.00, 500.00, 0, 180, 365, 'active', '企业团险'
    UNION ALL SELECT 6,  6,  'LONG-MED', '安心守护长期医疗保障计划', 'long_term_medical',
           1000000.00, 1455.00, 500.00, 30, 180, 365, 'active', '银行保险'
    UNION ALL SELECT 7,  7,  'CI-PLUS', '康护无忧重大疾病保障计划', 'critical_illness',
           800000.00, 1550.00, 0.00, 90, 180, 365, 'active', '保险经纪'
    UNION ALL SELECT 8,  8,  'CVD-CARE', '臻享心脑血管专项保障计划', 'cvd_management',
           300000.00, 1645.00, 500.00, 30, 180, 365, 'active', '个人直销'
    UNION ALL SELECT 9,  9,  'GROUP-MED', '悦享健康团体医疗保障计划', 'group_medical',
           500000.00, 1740.00, 500.00, 0, 180, 365, 'active', '企业团险'
    UNION ALL SELECT 10, 10, 'LONG-MED', '安心守护长期医疗保障计划', 'long_term_medical',
           1000000.00, 1835.00, 500.00, 30, 180, 365, 'active', '银行保险'
    UNION ALL SELECT 11, 11, 'CI-PLUS', '康护无忧重大疾病保障计划', 'critical_illness',
           800000.00, 1930.00, 0.00, 90, 180, 365, 'active', '保险经纪'
    UNION ALL SELECT 12, 12, 'CVD-CARE', '臻享心脑血管专项保障计划', 'cvd_management',
           300000.00, 2025.00, 500.00, 30, -30, 365, 'pending', '个人直销'
    UNION ALL SELECT 13, 3,  'LONG-MED', '安心守护长期医疗保障计划', 'long_term_medical',
           800000.00, 995.00, 500.00, 30, 365, 320, 'expired', '银行保险'
    UNION ALL SELECT 14, 5,  'CVD-CARE', '臻享心脑血管专项保障计划', 'cvd_management',
           300000.00, 1180.00, 500.00, 30, 180, 365, 'cancelled', '个人直销'
) pol
ON DUPLICATE KEY UPDATE
    product_code = VALUES(product_code), product_name = VALUES(product_name),
    insured_subject_ref = VALUES(insured_subject_ref),
    coverage_amount = VALUES(coverage_amount), premium_amount = VALUES(premium_amount),
    deductible_amount = VALUES(deductible_amount), effective_on = VALUES(effective_on),
    expires_on = VALUES(expires_on), status = VALUES(status),
    metadata_json = VALUES(metadata_json), updated_at = VALUES(updated_at);

-- ============================================================================
-- 7. 保障责任（每保单 2 条：主医疗责任 + 门诊/健康管理责任，共 28 条）
--    保障状态随保单状态；主责任限额=保单保额，附加责任限额 20000~30000。
-- ============================================================================
INSERT INTO rehealth_insurance_coverage (
    id, tenant_id, policy_id, subject_ref, coverage_code, coverage_name,
    limit_amount, deductible_amount, effective_on, expires_on, status,
    source_system, source_record_id, metadata_json, created_at, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:coverage:9202:', pol.no, ':', kind.suffix), 256)),
    @tenant,
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:policy:9202:', pol.no), 256)),
    CONCAT('QA9202-S', LPAD(pol.subject_no, 4, '0')),
    kind.coverage_code,
    CASE WHEN kind.suffix = 'A' THEN pol.product_name
         WHEN pol.policy_type = 'cvd_management' THEN '心脑血管健康管理保障'
         ELSE '门诊医疗费用保障' END,
    CASE WHEN kind.suffix = 'A' THEN pol.coverage_amount
         WHEN pol.policy_type = 'cvd_management' THEN 30000.00
         ELSE 20000.00 END,
    CASE WHEN kind.suffix = 'A' THEN pol.deductible_amount ELSE 0.00 END,
    CASE WHEN pol.eff_days_ago < 0 THEN DATE_ADD(@anchor_date, INTERVAL (-pol.eff_days_ago) DAY)
         ELSE DATE_SUB(@anchor_date, INTERVAL pol.eff_days_ago DAY) END,
    DATE_ADD(
        CASE WHEN pol.eff_days_ago < 0 THEN DATE_ADD(@anchor_date, INTERVAL (-pol.eff_days_ago) DAY)
             ELSE DATE_SUB(@anchor_date, INTERVAL pol.eff_days_ago DAY) END,
        INTERVAL pol.dur_days DAY),
    pol.status, 'REHEALTH_QA_TD_V1',
    CONCAT('coverage-9202-', LPAD(pol.no, 4, '0'), '-', kind.suffix),
    JSON_OBJECT('synthetic', TRUE), @seed_time, @seed_time
FROM (
    SELECT 1 no, 1 subject_no, 'GROUP-MED' product_code, '悦享健康团体医疗保障计划' product_name,
           'group_medical' policy_type, 500000.00 coverage_amount, 500.00 deductible_amount,
           180 eff_days_ago, 365 dur_days, 'active' status
    UNION ALL SELECT 2,  2,  'LONG-MED', '安心守护长期医疗保障计划', 'long_term_medical',
           1000000.00, 500.00, 180, 365, 'active'
    UNION ALL SELECT 3,  3,  'CI-PLUS', '康护无忧重大疾病保障计划', 'critical_illness',
           800000.00, 0.00, 180, 365, 'active'
    UNION ALL SELECT 4,  4,  'CVD-CARE', '臻享心脑血管专项保障计划', 'cvd_management',
           300000.00, 500.00, 180, 365, 'active'
    UNION ALL SELECT 5,  5,  'GROUP-MED', '悦享健康团体医疗保障计划', 'group_medical',
           500000.00, 500.00, 180, 365, 'active'
    UNION ALL SELECT 6,  6,  'LONG-MED', '安心守护长期医疗保障计划', 'long_term_medical',
           1000000.00, 500.00, 180, 365, 'active'
    UNION ALL SELECT 7,  7,  'CI-PLUS', '康护无忧重大疾病保障计划', 'critical_illness',
           800000.00, 0.00, 180, 365, 'active'
    UNION ALL SELECT 8,  8,  'CVD-CARE', '臻享心脑血管专项保障计划', 'cvd_management',
           300000.00, 500.00, 180, 365, 'active'
    UNION ALL SELECT 9,  9,  'GROUP-MED', '悦享健康团体医疗保障计划', 'group_medical',
           500000.00, 500.00, 180, 365, 'active'
    UNION ALL SELECT 10, 10, 'LONG-MED', '安心守护长期医疗保障计划', 'long_term_medical',
           1000000.00, 500.00, 180, 365, 'active'
    UNION ALL SELECT 11, 11, 'CI-PLUS', '康护无忧重大疾病保障计划', 'critical_illness',
           800000.00, 0.00, 180, 365, 'active'
    UNION ALL SELECT 12, 12, 'CVD-CARE', '臻享心脑血管专项保障计划', 'cvd_management',
           300000.00, 500.00, -30, 365, 'pending'
    UNION ALL SELECT 13, 3,  'LONG-MED', '安心守护长期医疗保障计划', 'long_term_medical',
           800000.00, 500.00, 365, 320, 'expired'
    UNION ALL SELECT 14, 5,  'CVD-CARE', '臻享心脑血管专项保障计划', 'cvd_management',
           300000.00, 500.00, 180, 365, 'cancelled'
) pol
JOIN (
    SELECT 'A' suffix, 'INPATIENT' coverage_code
    UNION ALL SELECT 'B', 'OUTPATIENT'
) kind ON 1 = 1
ON DUPLICATE KEY UPDATE
    policy_id = VALUES(policy_id), subject_ref = VALUES(subject_ref),
    coverage_code = VALUES(coverage_code), coverage_name = VALUES(coverage_name),
    limit_amount = VALUES(limit_amount), deductible_amount = VALUES(deductible_amount),
    effective_on = VALUES(effective_on), expires_on = VALUES(expires_on),
    status = VALUES(status), updated_at = VALUES(updated_at);

-- 修正附加责任的覆盖代码：健康管理计划保单附加责任为 CVD-MGMT
UPDATE rehealth_insurance_coverage cov
JOIN rehealth_insurance_policy pol
  ON pol.id = cov.policy_id AND pol.tenant_id = cov.tenant_id
SET cov.coverage_code = 'CVD-MGMT'
WHERE cov.tenant_id = @tenant
  AND pol.policy_type = 'cvd_management'
  AND cov.coverage_code = 'OUTPATIENT'
  AND cov.source_system = 'REHEALTH_QA_TD_V1';

-- ============================================================================
-- 8. 授权记录（13 条）
--    S0007 已撤销；S0009 有 v1 过期 + v2 生效两条版本；S0012 待确认。
--    唯一键 (tenant, subject_ref, consent_type, consent_version) 已按版本区分。
-- ============================================================================
INSERT INTO rehealth_insurance_consent (
    id, tenant_id, subject_ref, consent_type, consent_version, status,
    granted_at, revoked_at, evidence_ref, evidence_hash, source_system,
    source_record_id, metadata_json, created_at, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:consent:9202:', c.no, ':', c.version_suffix), 256)),
    @tenant, CONCAT('QA9202-S', LPAD(c.no, 4, '0')),
    'insurance_health_management', c.consent_version, c.status,
    c.granted_at, c.revoked_at,
    CONCAT('RH-CONSENT-9202-', LPAD(c.no, 4, '0'), c.version_suffix),
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:consent-evidence:9202:', c.no, ':', c.version_suffix), 256)),
    'REHEALTH_QA_TD_V1', CONCAT('consent-9202-', LPAD(c.no, 4, '0'), c.version_suffix),
    JSON_OBJECT('synthetic', TRUE, 'scope', 'assigned-staff-full-business-data'),
    IFNULL(c.granted_at, TIMESTAMP('2026-08-19 09:00:00')), @seed_time
FROM (
    SELECT 1 no, '' version_suffix, 'qa-td-v1' consent_version, 'granted' status,
           DATE_SUB(@seed_time, INTERVAL 180 DAY) granted_at, NULL revoked_at
    UNION ALL SELECT 2,  '', 'qa-td-v1', 'granted', DATE_SUB(@seed_time, INTERVAL 180 DAY), NULL
    UNION ALL SELECT 3,  '', 'qa-td-v1', 'granted', DATE_SUB(@seed_time, INTERVAL 180 DAY), NULL
    UNION ALL SELECT 4,  '', 'qa-td-v1', 'granted', DATE_SUB(@seed_time, INTERVAL 180 DAY), NULL
    UNION ALL SELECT 5,  '', 'qa-td-v1', 'granted', DATE_SUB(@seed_time, INTERVAL 180 DAY), NULL
    UNION ALL SELECT 6,  '', 'qa-td-v1', 'granted', DATE_SUB(@seed_time, INTERVAL 180 DAY), NULL
    UNION ALL SELECT 7,  '', 'qa-td-v1', 'revoked', DATE_SUB(@seed_time, INTERVAL 180 DAY),
           DATE_SUB(@seed_time, INTERVAL 10 DAY)
    UNION ALL SELECT 8,  '', 'qa-td-v1', 'granted', DATE_SUB(@seed_time, INTERVAL 180 DAY), NULL
    UNION ALL SELECT 9,  '-v1', 'qa-td-v1', 'expired', DATE_SUB(@seed_time, INTERVAL 400 DAY),
           DATE_SUB(@seed_time, INTERVAL 190 DAY)
    UNION ALL SELECT 9,  '-v2', 'qa-td-v2', 'granted', DATE_SUB(@seed_time, INTERVAL 185 DAY), NULL
    UNION ALL SELECT 10, '', 'qa-td-v1', 'granted', DATE_SUB(@seed_time, INTERVAL 180 DAY), NULL
    UNION ALL SELECT 11, '', 'qa-td-v1', 'granted', DATE_SUB(@seed_time, INTERVAL 180 DAY), NULL
    UNION ALL SELECT 12, '', 'qa-td-v1', 'pending', NULL, NULL
) c
ON DUPLICATE KEY UPDATE
    status = VALUES(status), granted_at = VALUES(granted_at), revoked_at = VALUES(revoked_at),
    evidence_ref = VALUES(evidence_ref), evidence_hash = VALUES(evidence_hash),
    metadata_json = VALUES(metadata_json), updated_at = VALUES(updated_at);

-- ============================================================================
-- 9. 计划绑定（11 条：S0001~S0011；S0007 已解绑，S0012 从未绑定）
-- ============================================================================
INSERT INTO rehealth_insurance_plan_binding (
    id, tenant_id, subject_ref, policy_id, plan_id, consent_id, status,
    bound_at, unbound_at, source_system, source_record_id, metadata_json,
    created_at, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:binding:9202:', b.no), 256)),
    @tenant, CONCAT('QA9202-S', LPAD(b.no, 4, '0')),
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:policy:9202:', b.no), 256)),
    CONCAT('qa9202-plan-', LPAD(b.no, 4, '0')),
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:consent:9202:', b.no, IF(b.no = 9, ':-v2', ':')), 256)),
    b.status,
    DATE_SUB(@seed_time, INTERVAL 90 DAY),
    CASE WHEN b.status = 'unbound' THEN DATE_SUB(@seed_time, INTERVAL 10 DAY) ELSE NULL END,
    'REHEALTH_QA_TD_V1', CONCAT('binding-9202-', LPAD(b.no, 4, '0')),
    JSON_OBJECT('synthetic', TRUE),
    DATE_SUB(@seed_time, INTERVAL 90 DAY), @seed_time
FROM (
    SELECT 1 no, 'active' status
    UNION ALL SELECT 2,  'active'
    UNION ALL SELECT 3,  'active'
    UNION ALL SELECT 4,  'active'
    UNION ALL SELECT 5,  'active'
    UNION ALL SELECT 6,  'active'
    UNION ALL SELECT 7,  'unbound'
    UNION ALL SELECT 8,  'active'
    UNION ALL SELECT 9,  'active'
    UNION ALL SELECT 10, 'active'
    UNION ALL SELECT 11, 'active'
) b
ON DUPLICATE KEY UPDATE
    policy_id = VALUES(policy_id), plan_id = VALUES(plan_id), consent_id = VALUES(consent_id),
    status = VALUES(status), bound_at = VALUES(bound_at), unbound_at = VALUES(unbound_at),
    updated_at = VALUES(updated_at);

-- ============================================================================
-- 10. 干预服务关系（12 条：enrolled 3 / completed 7 / withdrawn 2）
-- ============================================================================
INSERT INTO rehealth_insurance_intervention (
    id, tenant_id, subject_ref, plan_id, source_plan_id, consent_id, status,
    enrolled_at, ended_at, last_feedback_at, source_system, source_record_id,
    metadata_json, created_at, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:insurance-intervention:9202:', i.no), 256)),
    @tenant, CONCAT('QA9202-S', LPAD(i.no, 4, '0')),
    CONCAT('qa9202-plan-', LPAD(i.no, 4, '0')),
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:plan:', i.username), 256)),
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:consent:9202:', i.no, IF(i.no = 9, ':-v2', ':')), 256)),
    i.status,
    DATE_SUB(@seed_time, INTERVAL 90 DAY),
    CASE WHEN i.status IN ('completed', 'withdrawn')
         THEN DATE_SUB(@seed_time, INTERVAL IF(i.status = 'completed', 2, 10) DAY)
         ELSE NULL END,
    CASE WHEN i.status = 'withdrawn' THEN DATE_SUB(@seed_time, INTERVAL 12 DAY)
         ELSE DATE_SUB(@seed_time, INTERVAL 1 DAY) END,
    'REHEALTH_QA_TD_V1', CONCAT('intervention-9202-', LPAD(i.no, 4, '0')),
    JSON_OBJECT('synthetic', TRUE, 'clinicalUseAllowed', FALSE),
    DATE_SUB(@seed_time, INTERVAL 90 DAY), @seed_time
FROM (
    SELECT 1 no, 'qa9202app01' username, 'enrolled' status
    UNION ALL SELECT 2,  'qa9202app02', 'completed'
    UNION ALL SELECT 3,  'qa9202app03', 'completed'
    UNION ALL SELECT 4,  'qa9202app04', 'enrolled'
    UNION ALL SELECT 5,  'qa9202app05', 'completed'
    UNION ALL SELECT 6,  'qa9202app06', 'completed'
    UNION ALL SELECT 7,  'qa9202app07', 'withdrawn'
    UNION ALL SELECT 8,  'qa9202app08', 'completed'
    UNION ALL SELECT 9,  'qa9202app09', 'completed'
    UNION ALL SELECT 10, 'qa9202app10', 'enrolled'
    UNION ALL SELECT 11, 'qa9202app11', 'completed'
    UNION ALL SELECT 12, 'qa9202app12', 'withdrawn'
) i
ON DUPLICATE KEY UPDATE
    source_plan_id = VALUES(source_plan_id), consent_id = VALUES(consent_id),
    status = VALUES(status), ended_at = VALUES(ended_at),
    last_feedback_at = VALUES(last_feedback_at), metadata_json = VALUES(metadata_json),
    updated_at = VALUES(updated_at);

-- ============================================================================
-- 11. 干预反馈（33 条）
--     S0001~S0011 每主体 3 条（7/4/1 天前）；S0012 已退出且从未绑定，无反馈；
--     S0011 第 3 条为 not_applicable + completion_rate=NULL 边界行。
--     completion_rate 由风险基线派生并限制在 0~1 之间。
-- ============================================================================
INSERT INTO rehealth_insurance_intervention_feedback (
    id, tenant_id, binding_id, subject_ref, intervention_id, feedback_type,
    occurred_at, completion_rate, adherence_score, plan_item_id,
    expected_count, completed_count, verification_type, calculation_version,
    outcome_summary_json, source_system, source_record_id, created_at
)
SELECT
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:feedback:9202:', f.no, ':', f.fb_no), 256)),
    @tenant,
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:binding:9202:', f.no), 256)),
    CONCAT('QA9202-S', LPAD(f.no, 4, '0')),
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:insurance-intervention:9202:', f.no), 256)),
    CASE
        WHEN f.no = 11 AND f.fb_no = 3 THEN 'not_applicable'
        WHEN f.rate >= 0.95 THEN 'completed'
        WHEN f.rate >= 0.20 THEN 'partially_completed'
        ELSE 'skipped'
    END,
    DATE_SUB(@seed_time, INTERVAL (CASE WHEN f.no = 12 THEN 30 ELSE 8 - f.fb_no * 3 END) DAY),
    CASE WHEN f.no = 11 AND f.fb_no = 3 THEN NULL ELSE f.rate END,
    CASE WHEN f.no = 11 AND f.fb_no = 3 THEN NULL ELSE f.rate END,
    CONCAT('qa9202-item-', LPAD(MOD(f.no - 1, 4) + 1, 2, '0'), '-', f.fb_no),
    1.000,
    CASE WHEN f.no = 11 AND f.fb_no = 3 THEN NULL ELSE ROUND(f.rate, 3) END,
    'self_report', 'insurance-adherence-event-v1',
    JSON_OBJECT('synthetic', TRUE, 'clinicalUseAllowed', FALSE,
                'note', CASE f.fb_no
                    WHEN 1 THEN '已确认计划并开始执行'
                    WHEN 2 THEN '已完成睡眠和运动记录'
                    ELSE '已回传本周执行结果' END),
    'REHEALTH_QA_TD_V1',
    CONCAT('feedback-9202-', LPAD(f.no, 4, '0'), '-', f.fb_no),
    @seed_time
FROM (
    SELECT p.no, fb.fb_no,
           ROUND(GREATEST(0, LEAST(1, 1.05 - p.risk_base + fb.score_offset)), 2) rate
    FROM (
        SELECT 1 no, 0.26 risk_base UNION ALL SELECT 2, 0.35 UNION ALL SELECT 3, 0.48
        UNION ALL SELECT 4, 0.70 UNION ALL SELECT 5, 0.23 UNION ALL SELECT 6, 0.33
        UNION ALL SELECT 7, 0.52 UNION ALL SELECT 8, 0.72 UNION ALL SELECT 9, 0.28
        UNION ALL SELECT 10, 0.39 UNION ALL SELECT 11, 0.58 UNION ALL SELECT 12, 0.76
    ) p
    CROSS JOIN (SELECT 1 fb_no, -0.25 score_offset
                UNION ALL SELECT 2, 0.00
                UNION ALL SELECT 3, 0.25) fb
) f
WHERE f.no <= 11
ON DUPLICATE KEY UPDATE
    binding_id = VALUES(binding_id), subject_ref = VALUES(subject_ref),
    intervention_id = VALUES(intervention_id), feedback_type = VALUES(feedback_type),
    occurred_at = VALUES(occurred_at), completion_rate = VALUES(completion_rate),
    adherence_score = VALUES(adherence_score), plan_item_id = VALUES(plan_item_id),
    completed_count = VALUES(completed_count), verification_type = VALUES(verification_type),
    outcome_summary_json = VALUES(outcome_summary_json), created_at = VALUES(created_at);

-- ============================================================================
-- 12. 人工行动（36 条：每主体 3 条 followup / reminder / review）
--     S0001~S0006 已完成人群 → 行动已闭环；
--     S0007~S0012 处理中人群 → 待办/进行中，S0007 的首次随访已逾期未处理。
-- ============================================================================
INSERT INTO rehealth_insurance_intervention_action (
    id, tenant_id, subject_ref, plan_id, action_type, title, content,
    assignee_user_id, status, due_at, completed_at, result_json,
    created_by, request_id, created_at, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:workbench-action:9202:', a.no, ':', a.action_no), 256)),
    @tenant, CONCAT('QA9202-S', LPAD(a.no, 4, '0')),
    CONCAT('qa9202-plan-', LPAD(a.no, 4, '0')),
    a.action_type,
    CASE a.action_no
        WHEN 1 THEN CASE MOD(a.no - 1, 4)
            WHEN 0 THEN '血压管理首次随访'
            WHEN 1 THEN '血脂资料首次复核'
            WHEN 2 THEN '睡眠与运动首次随访'
            ELSE '代谢指标首次复核' END
        WHEN 2 THEN CASE MOD(a.no - 1, 4)
            WHEN 0 THEN '血压记录完成提醒'
            WHEN 1 THEN '血脂复查预约提醒'
            WHEN 2 THEN '睡眠与运动计划提醒'
            ELSE '血糖与体重记录提醒' END
        ELSE CASE MOD(a.no - 1, 4)
            WHEN 0 THEN '血压趋势阶段复核'
            WHEN 1 THEN '血脂管理阶段复核'
            WHEN 2 THEN '生活方式执行复核'
            ELSE '代谢管理阶段复核' END
    END,
    CASE MOD(a.no - 1, 4)
        WHEN 0 THEN '核对早晚血压记录、低盐饮食执行和复测安排；不根据单次读数调整治疗。'
        WHEN 1 THEN '核对血脂复查、用药确认和膳食记录；具体用药由医生审核。'
        WHEN 2 THEN '核对睡眠记录、有氧活动次数和当前执行困难。'
        ELSE '核对空腹血糖、体重和腰围记录，确认是否需要人工复核。'
    END,
    assignee.id,
    CASE
        WHEN a.no <= 6 THEN 'completed'
        WHEN a.action_no = 2 THEN 'in_progress'
        ELSE 'pending'
    END,
    CASE WHEN a.no = 7 AND a.action_no = 1 THEN DATE_SUB(@seed_time, INTERVAL 2 DAY)
         ELSE DATE_ADD(@seed_time, INTERVAL a.due_days DAY) END,
    CASE WHEN a.no <= 6 THEN DATE_SUB(@seed_time, INTERVAL (4 - a.action_no) DAY)
         ELSE NULL END,
    JSON_OBJECT('synthetic', TRUE, 'clinicalUseAllowed', FALSE,
                'result', IF(a.no <= 6, '已完成随访记录', '待执行')),
    assignee.id,
    CONCAT('qa-td-action-9202-', LPAD(a.no, 4, '0'), '-', a.action_no),
    DATE_SUB(@seed_time, INTERVAL (10 - a.action_no) DAY),
    DATE_SUB(@seed_time, INTERVAL (4 - a.action_no) DAY)
FROM (
    SELECT 1 no, 'qa9202_manager' assignee_username, 1 due_days, 'followup' action_type, 1 action_no
    UNION ALL SELECT 1,  'qa9202_manager', 3, 'reminder', 2
    UNION ALL SELECT 1,  'qa9202_manager', 7, 'review', 3
    UNION ALL SELECT 2,  'qa9202_manager', 1, 'followup', 1
    UNION ALL SELECT 2,  'qa9202_manager', 3, 'reminder', 2
    UNION ALL SELECT 2,  'qa9202_manager', 7, 'review', 3
    UNION ALL SELECT 3,  'qa9202_manager', 1, 'followup', 1
    UNION ALL SELECT 3,  'qa9202_manager', 3, 'reminder', 2
    UNION ALL SELECT 3,  'qa9202_manager', 7, 'review', 3
    UNION ALL SELECT 4,  'qa9202_manager', 1, 'followup', 1
    UNION ALL SELECT 4,  'qa9202_manager', 3, 'reminder', 2
    UNION ALL SELECT 4,  'qa9202_manager', 7, 'review', 3
    UNION ALL SELECT 5,  'qa9202_manager', 1, 'followup', 1
    UNION ALL SELECT 5,  'qa9202_manager', 3, 'reminder', 2
    UNION ALL SELECT 5,  'qa9202_manager', 7, 'review', 3
    UNION ALL SELECT 6,  'qa9202_manager', 1, 'followup', 1
    UNION ALL SELECT 6,  'qa9202_manager', 3, 'reminder', 2
    UNION ALL SELECT 6,  'qa9202_manager', 7, 'review', 3
    UNION ALL SELECT 7,  'qa9202_operator', 1, 'followup', 1
    UNION ALL SELECT 7,  'qa9202_operator', 3, 'reminder', 2
    UNION ALL SELECT 7,  'qa9202_operator', 7, 'review', 3
    UNION ALL SELECT 8,  'qa9202_operator', 1, 'followup', 1
    UNION ALL SELECT 8,  'qa9202_operator', 3, 'reminder', 2
    UNION ALL SELECT 8,  'qa9202_operator', 7, 'review', 3
    UNION ALL SELECT 9,  'qa9202_operator', 1, 'followup', 1
    UNION ALL SELECT 9,  'qa9202_operator', 3, 'reminder', 2
    UNION ALL SELECT 9,  'qa9202_operator', 7, 'review', 3
    UNION ALL SELECT 10, 'qa9202_operator', 1, 'followup', 1
    UNION ALL SELECT 10, 'qa9202_operator', 3, 'reminder', 2
    UNION ALL SELECT 10, 'qa9202_operator', 7, 'review', 3
    UNION ALL SELECT 11, 'qa9202_operator', 1, 'followup', 1
    UNION ALL SELECT 11, 'qa9202_operator', 3, 'reminder', 2
    UNION ALL SELECT 11, 'qa9202_operator', 7, 'review', 3
    UNION ALL SELECT 12, 'qa9202_operator', 1, 'followup', 1
    UNION ALL SELECT 12, 'qa9202_operator', 3, 'reminder', 2
    UNION ALL SELECT 12, 'qa9202_operator', 7, 'review', 3
) a
JOIN sys_user assignee
  ON assignee.username = a.assignee_username
 AND assignee.status = 1 AND assignee.del_flag = 0
ON DUPLICATE KEY UPDATE
    subject_ref = VALUES(subject_ref), plan_id = VALUES(plan_id),
    action_type = VALUES(action_type), title = VALUES(title), content = VALUES(content),
    assignee_user_id = VALUES(assignee_user_id), status = VALUES(status),
    due_at = VALUES(due_at), completed_at = VALUES(completed_at),
    result_json = VALUES(result_json), created_by = VALUES(created_by),
    updated_at = VALUES(updated_at);

-- ============================================================================
-- 13. 理赔（16 条，覆盖全部状态；金额满足 billed >= approved >= paid）
--     C0014 为无保单理赔（policy_id=NULL）边界行；
--     C0012 关联已过期保单 P0013（历史保单理赔）。
-- ============================================================================
INSERT INTO rehealth_insurance_claim (
    id, tenant_id, claim_no, policy_id, subject_ref, claim_type,
    event_on, submitted_at, decided_at, status, billed_amount,
    approved_amount, paid_amount, currency, coverage_code, outcome_code,
    source_system, source_record_id, metadata_json, created_at, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:claim:9202:', c.claim_no), 256)),
    @tenant, c.claim_no,
    CASE WHEN c.policy_no IS NULL THEN NULL
         ELSE LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:policy:9202:', c.policy_no), 256)) END,
    CONCAT('QA9202-S', LPAD(c.subject_no, 4, '0')),
    c.claim_type,
    DATE_SUB(@anchor_date, INTERVAL c.event_days_ago DAY),
    DATE_SUB(@seed_time, INTERVAL c.submitted_days_ago DAY),
    CASE WHEN c.status IN ('submitted', 'under_review') THEN NULL
         ELSE DATE_SUB(@seed_time, INTERVAL c.decided_days_ago DAY) END,
    c.status, c.billed_amount, c.approved_amount, c.paid_amount,
    'CNY', c.coverage_code, c.outcome_code,
    'REHEALTH_QA_TD_V1', CONCAT('claim-9202-', c.claim_no),
    JSON_OBJECT('synthetic', TRUE, 'clinicalUseAllowed', FALSE),
    DATE_SUB(@seed_time, INTERVAL c.submitted_days_ago DAY), @seed_time
FROM (
    SELECT 'RH-9202-CLAIM-0001' claim_no, 1 subject_no, 1 policy_no, 'inpatient' claim_type,
           60 event_days_ago, 58 submitted_days_ago, 55 decided_days_ago, 'paid' status,
           1200.00 billed_amount, 960.00 approved_amount, 960.00 paid_amount,
           'INPATIENT' coverage_code, 'paid' outcome_code
    UNION ALL SELECT 'RH-9202-CLAIM-0002', 2,  2,  'inpatient', 40, 38, 36, 'approved',
           2400.00, 1980.00, NULL, 'INPATIENT', 'approved'
    UNION ALL SELECT 'RH-9202-CLAIM-0003', 3,  3,  'health_management', 5, 2, NULL, 'submitted',
           860.00, NULL, NULL, 'CVD-MGMT', NULL
    UNION ALL SELECT 'RH-9202-CLAIM-0004', 4,  4,  'inpatient', 12, 8, NULL, 'under_review',
           1520.00, NULL, NULL, 'INPATIENT', NULL
    UNION ALL SELECT 'RH-9202-CLAIM-0005', 5,  5,  'inpatient', 30, 26, 24, 'rejected',
           3200.00, 0.00, NULL, 'INPATIENT', 'rejected'
    UNION ALL SELECT 'RH-9202-CLAIM-0006', 6,  6,  'outpatient', 20, 15, 13, 'paid',
           980.00, 700.00, 700.00, 'OUTPATIENT', 'paid'
    UNION ALL SELECT 'RH-9202-CLAIM-0007', 7,  7,  'inpatient', 25, 22, 20, 'rejected',
           2100.00, 0.00, NULL, 'INPATIENT', 'rejected'
    UNION ALL SELECT 'RH-9202-CLAIM-0008', 8,  8,  'inpatient', 35, 31, 28, 'paid',
           1450.00, 1160.00, 1160.00, 'INPATIENT', 'paid'
    UNION ALL SELECT 'RH-9202-CLAIM-0009', 9,  9,  'outpatient', 10, 6, 4, 'paid',
           760.00, 608.00, 608.00, 'OUTPATIENT', 'paid'
    UNION ALL SELECT 'RH-9202-CLAIM-0010', 10, 10, 'inpatient', 18, 14, 12, 'approved',
           1890.00, 1512.00, NULL, 'INPATIENT', 'approved'
    UNION ALL SELECT 'RH-9202-CLAIM-0011', 11, 11, 'outpatient', 4, 1, NULL, 'submitted',
           540.00, NULL, NULL, 'OUTPATIENT', NULL
    UNION ALL SELECT 'RH-9202-CLAIM-0012', 3,  13, 'inpatient', 300, 290, 287, 'paid',
           2750.00, 2200.00, 2200.00, 'INPATIENT', 'paid'
    UNION ALL SELECT 'RH-9202-CLAIM-0013', 1,  1,  'outpatient', 45, 42, 40, 'paid',
           320.00, 256.00, 256.00, 'OUTPATIENT', 'paid'
    UNION ALL SELECT 'RH-9202-CLAIM-0014', 6,  NULL, 'health_management', 15, 9, NULL, 'submitted',
           680.00, NULL, NULL, 'CVD-MGMT', NULL
    UNION ALL SELECT 'RH-9202-CLAIM-0015', 9,  9,  'inpatient', 90, 85, 82, 'rejected',
           1500.00, 0.00, NULL, 'INPATIENT', 'rejected'
    UNION ALL SELECT 'RH-9202-CLAIM-0016', 8,  8,  'outpatient', 8, 5, 3, 'paid',
           420.00, 336.00, 336.00, 'OUTPATIENT', 'paid'
) c
ON DUPLICATE KEY UPDATE
    policy_id = VALUES(policy_id), subject_ref = VALUES(subject_ref),
    claim_type = VALUES(claim_type), event_on = VALUES(event_on),
    submitted_at = VALUES(submitted_at), decided_at = VALUES(decided_at),
    status = VALUES(status), billed_amount = VALUES(billed_amount),
    approved_amount = VALUES(approved_amount), paid_amount = VALUES(paid_amount),
    coverage_code = VALUES(coverage_code), outcome_code = VALUES(outcome_code),
    updated_at = VALUES(updated_at);

-- ============================================================================
-- 14. 负责人关系（17 条）
--     健康险运营部经理负责 S0001~S0006；运营专员负责 S0007~S0012；
--     精算风控部分析员抽样负责 4 人；含 1 条 inactive 历史负责关系（S0007）。
-- ============================================================================
INSERT INTO rehealth_insurance_subject_manager (
    id, tenant_id, manager_user_id, department_id, subject_ref,
    status, source_system, created_at, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:assignment:9202:', m.staff_username, ':', m.subject_no), 256)),
    @tenant, staff.id, depart.id,
    CONCAT('QA9202-S', LPAD(m.subject_no, 4, '0')),
    m.status, 'REHEALTH_QA_TD_V1', @seed_time, @seed_time
FROM (
    SELECT 'qa9202_manager' staff_username, 1 subject_no, 'active' status, 'QA9202HEALTH' org_code
    UNION ALL SELECT 'qa9202_manager',  2, 'active',  'QA9202HEALTH'
    UNION ALL SELECT 'qa9202_manager',  3, 'active',  'QA9202HEALTH'
    UNION ALL SELECT 'qa9202_manager',  4, 'active',  'QA9202HEALTH'
    UNION ALL SELECT 'qa9202_manager',  5, 'active',  'QA9202HEALTH'
    UNION ALL SELECT 'qa9202_manager',  6, 'active',  'QA9202HEALTH'
    UNION ALL SELECT 'qa9202_manager',  7, 'inactive', 'QA9202HEALTH'
    UNION ALL SELECT 'qa9202_operator', 7, 'active',  'QA9202HEALTH'
    UNION ALL SELECT 'qa9202_operator', 8, 'active',  'QA9202HEALTH'
    UNION ALL SELECT 'qa9202_operator', 9, 'active',  'QA9202HEALTH'
    UNION ALL SELECT 'qa9202_operator', 10, 'active', 'QA9202HEALTH'
    UNION ALL SELECT 'qa9202_operator', 11, 'active', 'QA9202HEALTH'
    UNION ALL SELECT 'qa9202_operator', 12, 'active', 'QA9202HEALTH'
    UNION ALL SELECT 'qa9202_analyst',  1, 'active',  'QA9202RISK'
    UNION ALL SELECT 'qa9202_analyst',  4, 'active',  'QA9202RISK'
    UNION ALL SELECT 'qa9202_analyst',  7, 'active',  'QA9202RISK'
    UNION ALL SELECT 'qa9202_analyst',  10, 'active', 'QA9202RISK'
) m
JOIN sys_user staff
  ON staff.username = m.staff_username AND staff.status = 1 AND staff.del_flag = 0
JOIN sys_depart depart
  ON depart.org_code = m.org_code AND depart.tenant_id = @tenant
 AND depart.status = '1' AND depart.del_flag = '0'
ON DUPLICATE KEY UPDATE
    manager_user_id = VALUES(manager_user_id), department_id = VALUES(department_id),
    subject_ref = VALUES(subject_ref), status = VALUES(status), updated_at = VALUES(updated_at);

-- ============================================================================
-- 15. 审计事件（12 条，覆盖保单/理赔/授权/绑定/负责人关键动作）
-- ============================================================================
INSERT INTO rehealth_insurance_audit_event (
    id, tenant_id, actor_user_id, action, resource_type, resource_id,
    request_id, before_hash, after_hash, metadata_json, created_at
)
SELECT
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:audit:9202:', ev.seq), 256)),
    @tenant, actor.id, ev.action, ev.resource_type,
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:', ev.resource_key), 256)),
    CONCAT('qa-td-audit-9202-', LPAD(ev.seq, 2, '0')),
    NULL,
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:after:', ev.resource_key, ':', ev.action), 256)),
    JSON_OBJECT('sourceSystem', 'REHEALTH_QA_TD_V1', 'synthetic', TRUE),
    DATE_SUB(@seed_time, INTERVAL ev.days_ago DAY)
FROM (
    SELECT 1 seq, 'qa9202_admin' actor_username, 'IMPORT_SUBJECTS' action,
           'insurance_import_batch' resource_type, 'import:9202:batch:qa-td-policy-import-v1' resource_key, 180 days_ago
    UNION ALL SELECT 2,  'qa9202_admin',   'CREATE_POLICY', 'insurance_policy',
           'policy:9202:1', 180
    UNION ALL SELECT 3,  'qa9202_admin',   'CREATE_POLICY', 'insurance_policy',
           'policy:9202:13', 400
    UNION ALL SELECT 4,  'qa9202_admin',   'REVOKE_CONSENT', 'insurance_consent',
           'consent:9202:7:', 10
    UNION ALL SELECT 5,  'qa9202_manager', 'UNBIND_PLAN', 'insurance_plan_binding',
           'binding:9202:7', 10
    UNION ALL SELECT 6,  'qa9202_manager', 'APPROVE_CLAIM', 'insurance_claim',
           'claim:9202:RH-9202-CLAIM-0001', 55
    UNION ALL SELECT 7,  'qa9202_manager', 'APPROVE_CLAIM', 'insurance_claim',
           'claim:9202:RH-9202-CLAIM-0006', 13
    UNION ALL SELECT 8,  'qa9202_analyst',  'REJECT_CLAIM', 'insurance_claim',
           'claim:9202:RH-9202-CLAIM-0005', 24
    UNION ALL SELECT 9,  'qa9202_analyst',  'REJECT_CLAIM', 'insurance_claim',
           'claim:9202:RH-9202-CLAIM-0007', 20
    UNION ALL SELECT 10, 'qa9202_admin',   'ASSIGN_RESPONSIBLE_STAFF', 'insurance_subject_manager',
           'assignment:9202:qa9202_manager:1', 90
    UNION ALL SELECT 11, 'qa9202_admin',   'ASSIGN_RESPONSIBLE_STAFF', 'insurance_subject_manager',
           'assignment:9202:qa9202_operator:7', 90
    UNION ALL SELECT 12, 'qa9202_admin',   'ASSIGN_RESPONSIBLE_STAFF', 'insurance_subject_manager',
           'assignment:9202:qa9202_analyst:10', 90
) ev
JOIN sys_user actor
  ON actor.username = ev.actor_username AND actor.status = 1 AND actor.del_flag = 0
ON DUPLICATE KEY UPDATE
    actor_user_id = VALUES(actor_user_id), action = VALUES(action),
    resource_id = VALUES(resource_id), request_id = VALUES(request_id),
    after_hash = VALUES(after_hash), metadata_json = VALUES(metadata_json),
    created_at = VALUES(created_at);

-- ============================================================================
-- 16. 导入批次（2 条：成功批次 + 含失败明细的批次）
-- ============================================================================
INSERT INTO rehealth_insurance_import_batch (
    id, tenant_id, import_type, source_system, idempotency_key, content_hash,
    status, total_count, success_count, failure_count, error_json,
    created_by, created_at, completed_at
) VALUES
    (LOWER(SHA2('REHEALTH_QA_TD_V1:import:9202:policy', 256)), 9202, 'policy_import',
     'REHEALTH_QA_TD_V1', 'qa-td-policy-import-v1',
     LOWER(SHA2('qa-td-policy-import-v1:content', 256)),
     'completed', 12, 12, 0, NULL,
     'qa9202_admin', DATE_SUB(@seed_time, INTERVAL 180 DAY), DATE_SUB(@seed_time, INTERVAL 179 DAY)),
    (LOWER(SHA2('REHEALTH_QA_TD_V1:import:9202:claim', 256)), 9202, 'claim_import',
     'REHEALTH_QA_TD_V1', 'qa-td-claim-import-v1',
     LOWER(SHA2('qa-td-claim-import-v1:content', 256)),
     'failed', 3, 2, 1,
     JSON_OBJECT('failedRow', 3, 'reason', '未知保单号 RH-9202-POL-9999，已跳过该行'),
     'qa9202_admin', DATE_SUB(@seed_time, INTERVAL 15 DAY), DATE_SUB(@seed_time, INTERVAL 14 DAY))
ON DUPLICATE KEY UPDATE
    content_hash = VALUES(content_hash), status = VALUES(status),
    total_count = VALUES(total_count), success_count = VALUES(success_count),
    failure_count = VALUES(failure_count), error_json = VALUES(error_json),
    completed_at = VALUES(completed_at);

-- ============================================================================
-- 17. PSM 研究链路（1 个已批准研究：快照 / 成员 / 结果 / 报告 / 结算包 / 审批）
-- ============================================================================
INSERT INTO rehealth_insurance_study (
    id, tenant_id, study_no, title, period_start, period_end,
    population_rule_json, intervention_rule_json, outcome_rule_json,
    methodology, status, model_version, created_by, approved_by, approved_at,
    created_at, updated_at
) VALUES
    (LOWER(SHA2('REHEALTH_QA_TD_V1:study:9202:1', 256)), 9202,
     'RH-9202-STUDY-0001', '心脑血管健康管理干预效果回顾性分析（测试）',
     DATE('2026-02-19'), DATE('2026-08-18'),
     JSON_OBJECT('rule', 'age >= 40 AND has_policy', 'source', 'REHEALTH_QA_TD_V1'),
     JSON_OBJECT('rule', 'intervention_enrolled_90d', 'source', 'REHEALTH_QA_TD_V1'),
     JSON_OBJECT('rule', 'cvd_risk_delta_90d', 'source', 'REHEALTH_QA_TD_V1'),
     'psm', 'approved', 'rehealth-cvd-v3.1.0',
     'qa9202_analyst', 'qa9202_admin', DATE_SUB(@seed_time, INTERVAL 10 DAY),
     DATE_SUB(@seed_time, INTERVAL 60 DAY), @seed_time)
ON DUPLICATE KEY UPDATE
    title = VALUES(title), status = VALUES(status),
    approved_by = VALUES(approved_by), approved_at = VALUES(approved_at),
    updated_at = VALUES(updated_at);

INSERT INTO rehealth_insurance_study_snapshot (
    id, tenant_id, study_id, snapshot_version, snapshot_hash, source_watermark,
    cohort_total, treated_total, control_total, source_summary_json,
    immutable, created_by, created_at
) VALUES
    (LOWER(SHA2('REHEALTH_QA_TD_V1:snapshot:9202:1:1', 256)), 9202,
     LOWER(SHA2('REHEALTH_QA_TD_V1:study:9202:1', 256)), 1,
     LOWER(SHA2('qa-td-study-9202-1:snapshot:v1', 256)),
     '2026-08-18T23:59:59Z', 12, 9, 3,
     JSON_OBJECT('source', 'REHEALTH_QA_TD_V1', 'synthetic', TRUE),
     1, 'qa9202_analyst', DATE_SUB(@seed_time, INTERVAL 12 DAY))
ON DUPLICATE KEY UPDATE
    snapshot_hash = VALUES(snapshot_hash), cohort_total = VALUES(cohort_total),
    treated_total = VALUES(treated_total), control_total = VALUES(control_total),
    source_summary_json = VALUES(source_summary_json), created_by = VALUES(created_by),
    created_at = VALUES(created_at);

INSERT INTO rehealth_insurance_study_member (
    id, tenant_id, snapshot_id, subject_ref, cohort_group,
    baseline_risk, outcome_value, intervention_status, covariate_json,
    source_row_hash, created_at
)
SELECT
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:study-member:9202:', m.no), 256)),
    @tenant, LOWER(SHA2('REHEALTH_QA_TD_V1:snapshot:9202:1:1', 256)),
    CONCAT('QA9202-S', LPAD(m.no, 4, '0')),
    m.cohort_group, m.baseline_risk, m.outcome_value, m.intervention_status,
    JSON_OBJECT('age', m.age, 'bmi', m.bmi, 'synthetic', TRUE),
    LOWER(SHA2(CONCAT('qa-td-member:9202:', m.no), 256)),
    DATE_SUB(@seed_time, INTERVAL 12 DAY)
FROM (
    SELECT 1 no, 'treated' cohort_group, 0.260000 baseline_risk, 0.190000 outcome_value, 'enrolled' intervention_status, 44 age, 22.89 bmi
    UNION ALL SELECT 2,  'treated', 0.350000, 0.270000, 'completed', 49, 23.03
    UNION ALL SELECT 3,  'treated', 0.480000, 0.360000, 'completed', 54, 26.50
    UNION ALL SELECT 4,  'treated', 0.700000, 0.520000, 'enrolled', 59, 27.73
    UNION ALL SELECT 5,  'treated', 0.230000, 0.170000, 'completed', 42, 22.05
    UNION ALL SELECT 6,  'treated', 0.330000, 0.240000, 'completed', 47, 24.06
    UNION ALL SELECT 7,  'control', 0.520000, 0.500000, 'none', 55, 26.98
    UNION ALL SELECT 8,  'treated', 0.720000, 0.550000, 'completed', 62, 29.14
    UNION ALL SELECT 9,  'treated', 0.280000, 0.210000, 'completed', 45, 23.11
    UNION ALL SELECT 10, 'treated', 0.390000, 0.290000, 'enrolled', 50, 23.61
    UNION ALL SELECT 11, 'control', 0.580000, 0.560000, 'none', 57, 27.13
    UNION ALL SELECT 12, 'control', 0.760000, 0.750000, 'none', 64, 29.75
) m
ON DUPLICATE KEY UPDATE
    snapshot_id = VALUES(snapshot_id), subject_ref = VALUES(subject_ref),
    cohort_group = VALUES(cohort_group), baseline_risk = VALUES(baseline_risk),
    outcome_value = VALUES(outcome_value), intervention_status = VALUES(intervention_status),
    covariate_json = VALUES(covariate_json), source_row_hash = VALUES(source_row_hash),
    created_at = VALUES(created_at);

INSERT INTO rehealth_insurance_study_result (
    id, tenant_id, study_id, snapshot_id, result_version, status,
    att_estimate, ci_lower, ci_upper, matched_pairs, balance_json,
    cost_basis_json, model_version, result_json, created_by, created_at
) VALUES
    (LOWER(SHA2('REHEALTH_QA_TD_V1:study-result:9202:1:1', 256)), 9202,
     LOWER(SHA2('REHEALTH_QA_TD_V1:study:9202:1', 256)),
     LOWER(SHA2('REHEALTH_QA_TD_V1:snapshot:9202:1:1', 256)),
     1, 'calculated', -0.08240000, -0.12100000, -0.04360000, 12,
     JSON_OBJECT('smd', JSON_OBJECT('age', 0.08, 'bmi', 0.05), 'synthetic', TRUE),
     JSON_OBJECT('basis', 'REHEALTH_QA_TD_V1', 'currency', 'CNY'),
     'rehealth-cvd-v3.1.0',
     JSON_OBJECT('synthetic', TRUE, 'method', 'psm_nearest_1:1'),
     'qa9202_analyst', DATE_SUB(@seed_time, INTERVAL 11 DAY))
ON DUPLICATE KEY UPDATE
    status = VALUES(status), att_estimate = VALUES(att_estimate),
    ci_lower = VALUES(ci_lower), ci_upper = VALUES(ci_upper),
    matched_pairs = VALUES(matched_pairs), result_json = VALUES(result_json),
    created_by = VALUES(created_by), created_at = VALUES(created_at);

INSERT INTO rehealth_insurance_rwe_report (
    id, tenant_id, report_no, study_id, report_type, report_version,
    title, period_start, period_end, status, evidence_hash, report_json,
    created_by, submitted_at, approved_by, approved_at, created_at, updated_at
) VALUES
    (LOWER(SHA2('REHEALTH_QA_TD_V1:rwe-report:9202:1', 256)), 9202,
     'RH-9202-RWE-0001', LOWER(SHA2('REHEALTH_QA_TD_V1:study:9202:1', 256)),
     'rwe', 1, '心脑血管健康管理干预效果回顾性分析报告（测试）',
     DATE('2026-02-19'), DATE('2026-08-18'), 'approved',
     LOWER(SHA2('qa-td-rwe-9202-1:evidence', 256)),
     JSON_OBJECT('synthetic', TRUE, 'att', -0.0824),
     'qa9202_analyst', DATE_SUB(@seed_time, INTERVAL 8 DAY),
     'qa9202_admin', DATE_SUB(@seed_time, INTERVAL 7 DAY),
     DATE_SUB(@seed_time, INTERVAL 9 DAY), DATE_SUB(@seed_time, INTERVAL 7 DAY))
ON DUPLICATE KEY UPDATE
    title = VALUES(title), status = VALUES(status), evidence_hash = VALUES(evidence_hash),
    report_json = VALUES(report_json), approved_by = VALUES(approved_by),
    approved_at = VALUES(approved_at), updated_at = VALUES(updated_at);

INSERT INTO rehealth_insurance_settlement_package (
    id, tenant_id, package_no, study_id, report_id, package_version,
    status, currency, estimated_savings, approved_amount, snapshot_hash,
    evidence_manifest_json, package_json, content_hash,
    created_by, approved_by, approved_at, created_at, updated_at
) VALUES
    (LOWER(SHA2('REHEALTH_QA_TD_V1:package:9202:1', 256)), 9202,
     'RH-9202-PKG-0001', LOWER(SHA2('REHEALTH_QA_TD_V1:study:9202:1', 256)),
     LOWER(SHA2('REHEALTH_QA_TD_V1:rwe-report:9202:1', 256)), 1,
     'draft', 'CNY', 125000.00, NULL,
     LOWER(SHA2('qa-td-study-9202-1:snapshot:v1', 256)),
     JSON_OBJECT('items', JSON_ARRAY('snapshot-v1', 'result-v1', 'report-v1'), 'synthetic', TRUE),
     JSON_OBJECT('synthetic', TRUE, 'savingsBasis', 'att_estimate'),
     LOWER(SHA2('qa-td-package-9202-1:content', 256)),
     'qa9202_analyst', NULL, NULL,
     DATE_SUB(@seed_time, INTERVAL 6 DAY), DATE_SUB(@seed_time, INTERVAL 6 DAY))
ON DUPLICATE KEY UPDATE
    status = VALUES(status), estimated_savings = VALUES(estimated_savings),
    package_json = VALUES(package_json), content_hash = VALUES(content_hash),
    updated_at = VALUES(updated_at);

INSERT INTO rehealth_insurance_settlement_approval (
    id, tenant_id, package_id, action, comment, actor_user_id, request_id, created_at
)
SELECT
    LOWER(SHA2('REHEALTH_QA_TD_V1:settlement-approval:9202:1:submit', 256)),
    @tenant, LOWER(SHA2('REHEALTH_QA_TD_V1:package:9202:1', 256)),
    'submit', '已完成证据清单与结算金额复核，提交机构审批（测试数据）',
    actor.id, 'qa-td-package-submit-9202-0001',
    DATE_SUB(@seed_time, INTERVAL 5 DAY)
FROM sys_user actor
WHERE actor.username = 'qa9202_analyst' AND actor.status = 1 AND actor.del_flag = 0
ON DUPLICATE KEY UPDATE
    action = VALUES(action), comment = VALUES(comment),
    actor_user_id = VALUES(actor_user_id), created_at = VALUES(created_at);

-- ============================================================================
-- 18. 执行后核对（预期行数）
--     sys_user(app)=12；patient_profile=12；tenant_profile=1；subject=12；
--     policy=14；coverage=28；consent=13；binding=11；intervention=12；
--     feedback=33；action=36；claim=16；manager=17；audit=12；
--     import_batch=2；study=1；snapshot=1；member=12；result=1；
--     report=1；package=1；settlement_approval=1
-- ============================================================================
SELECT 'sys_user(app)' tbl, COUNT(*) cnt FROM sys_user WHERE username LIKE 'qa9202app%'
UNION ALL SELECT 'patient_profile', COUNT(*) FROM rehealth_patient_profile p
    JOIN sys_user u ON u.id = p.user_id WHERE u.username LIKE 'qa9202app%'
UNION ALL SELECT 'tenant_profile', COUNT(*) FROM rehealth_insurance_tenant_profile WHERE tenant_id = @tenant
UNION ALL SELECT 'subject', COUNT(*) FROM rehealth_insurance_subject WHERE tenant_id = @tenant AND source_system = 'REHEALTH_QA_TD_V1'
UNION ALL SELECT 'policy', COUNT(*) FROM rehealth_insurance_policy WHERE tenant_id = @tenant AND source_system = 'REHEALTH_QA_TD_V1'
UNION ALL SELECT 'coverage', COUNT(*) FROM rehealth_insurance_coverage WHERE tenant_id = @tenant AND source_system = 'REHEALTH_QA_TD_V1'
UNION ALL SELECT 'consent', COUNT(*) FROM rehealth_insurance_consent WHERE tenant_id = @tenant AND source_system = 'REHEALTH_QA_TD_V1'
UNION ALL SELECT 'plan_binding', COUNT(*) FROM rehealth_insurance_plan_binding WHERE tenant_id = @tenant AND source_system = 'REHEALTH_QA_TD_V1'
UNION ALL SELECT 'intervention', COUNT(*) FROM rehealth_insurance_intervention WHERE tenant_id = @tenant AND source_system = 'REHEALTH_QA_TD_V1'
UNION ALL SELECT 'feedback', COUNT(*) FROM rehealth_insurance_intervention_feedback WHERE tenant_id = @tenant AND source_system = 'REHEALTH_QA_TD_V1'
UNION ALL SELECT 'action', COUNT(*) FROM rehealth_insurance_intervention_action WHERE tenant_id = @tenant AND request_id LIKE 'qa-td-action-%'
UNION ALL SELECT 'claim', COUNT(*) FROM rehealth_insurance_claim WHERE tenant_id = @tenant AND source_system = 'REHEALTH_QA_TD_V1'
UNION ALL SELECT 'subject_manager', COUNT(*) FROM rehealth_insurance_subject_manager WHERE tenant_id = @tenant AND source_system = 'REHEALTH_QA_TD_V1'
UNION ALL SELECT 'audit_event', COUNT(*) FROM rehealth_insurance_audit_event WHERE tenant_id = @tenant AND request_id LIKE 'qa-td-audit-%'
UNION ALL SELECT 'import_batch', COUNT(*) FROM rehealth_insurance_import_batch WHERE tenant_id = @tenant AND idempotency_key LIKE 'qa-td-%'
UNION ALL SELECT 'study', COUNT(*) FROM rehealth_insurance_study WHERE tenant_id = @tenant AND study_no = 'RH-9202-STUDY-0001'
UNION ALL SELECT 'snapshot', COUNT(*) FROM rehealth_insurance_study_snapshot WHERE tenant_id = @tenant AND source_watermark = '2026-08-18T23:59:59Z'
UNION ALL SELECT 'study_member', COUNT(*) FROM rehealth_insurance_study_member WHERE tenant_id = @tenant AND snapshot_id = LOWER(SHA2('REHEALTH_QA_TD_V1:snapshot:9202:1:1', 256))
UNION ALL SELECT 'study_result', COUNT(*) FROM rehealth_insurance_study_result WHERE tenant_id = @tenant AND snapshot_id = LOWER(SHA2('REHEALTH_QA_TD_V1:snapshot:9202:1:1', 256))
UNION ALL SELECT 'rwe_report', COUNT(*) FROM rehealth_insurance_rwe_report WHERE tenant_id = @tenant AND report_no = 'RH-9202-RWE-0001'
UNION ALL SELECT 'package', COUNT(*) FROM rehealth_insurance_settlement_package WHERE tenant_id = @tenant AND package_no = 'RH-9202-PKG-0001'
UNION ALL SELECT 'settlement_approval', COUNT(*) FROM rehealth_insurance_settlement_approval WHERE tenant_id = @tenant AND request_id = 'qa-td-package-submit-9202-0001';

-- 员工引用缺失提示（未执行 01 脚本时，下列表的行数会少于预期）
SELECT 'subject_manager/audit/action 依赖第 1 部分员工账号（qa9202_manager 等），若计数为 0 请确认已执行本文件第 1 部分' AS notice
WHERE NOT EXISTS (
    SELECT 1 FROM sys_user WHERE username = 'qa9202_manager' AND del_flag = 0
);

-- ============================================================================

-- ============================================================================
-- 第 3 部分：9102 分类 APP 用户（5 类 x 6 条）
-- ============================================================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';
SET @seed_actor = 'REHEALTH_QA_TD_V1';
SET @seed_time  = TIMESTAMP('2026-08-19 09:00:00');
SET @anchor_date = DATE('2026-08-19');
SET @tenant = 9102;

-- ============================================================================
-- 1. APP 用户（30 个：5 类 x 6 条）
-- ============================================================================
INSERT INTO sys_user (
    id, username, realname, password, salt, birthday, sex, email, phone,
    status, del_flag, activiti_sync, work_no, create_by, create_time,
    update_by, update_time, user_identity, login_tenant_id, sort, iz_hide_contact
)
SELECT
    LOWER(MD5(CONCAT('REHEALTH_QA_TD_V1:user:', u.username))),
    u.username, u.realname, u.password_hash, 'LQA26081',
    DATE_SUB(@anchor_date, INTERVAL u.age YEAR), u.sex, u.email, u.phone,
    1, 0, 0, CONCAT('APP-9102-', u.seq_3),
    @seed_actor, @seed_time, @seed_actor, @seed_time, 1, 0,
    919100 + u.seq, '0'
FROM (
    SELECT 'qa9102app_a01' username, 'a' cat, 1 idx, 1 seq, '沈建国' realname, 1 sex, 58 age,
           173.00 height_cm, 80.50 weight_kg, 26.90 bmi, 0.62 risk_base,
           'd3d95a8c6a2a7af4bcf701c82a2cfaf5' password_hash,
           'qa9102app_a01@app.qa.invalid' email, '00091912001' phone, 'A01' seq_3
    UNION ALL SELECT 'qa9102app_a02', 'a', 2, 2, '吕秀英', 2, 61,
           161.00, 73.80, 28.47, 0.68, 'd3d95a8c6a2a7af48d58f2c8f8e12e83',
           'qa9102app_a02@app.qa.invalid', '00091912002', 'A02'
    UNION ALL SELECT 'qa9102app_a03', 'a', 3, 3, '郝永强', 1, 55,
           172.00, 79.10, 26.74, 0.59, 'd3d95a8c6a2a7af4ae3060d44e777f52',
           'qa9102app_a03@app.qa.invalid', '00091912003', 'A03'
    UNION ALL SELECT 'qa9102app_a04', 'a', 4, 4, '石桂花', 2, 57,
           160.00, 70.20, 27.42, 0.64, 'd3d95a8c6a2a7af46e9e164f1446cfb0',
           'qa9102app_a04@app.qa.invalid', '00091912004', 'A04'
    UNION ALL SELECT 'qa9102app_a05', 'a', 5, 5, '江志军', 1, 60,
           174.00, 84.00, 27.74, 0.71, 'd3d95a8c6a2a7af44b3a34cee259e8d7',
           'qa9102app_a05@app.qa.invalid', '00091912005', 'A05'
    UNION ALL SELECT 'qa9102app_a06', 'a', 6, 6, '温丽华', 2, 52,
           162.00, 68.90, 26.25, 0.57, 'd3d95a8c6a2a7af4ce61c83cdd62c99f',
           'qa9102app_a06@app.qa.invalid', '00091912006', 'A06'
    UNION ALL SELECT 'qa9102app_b01', 'b', 1, 7, '冯建军', 1, 49,
           171.00, 70.80, 24.21, 0.38, 'd3d95a8c6a2a7af42b7cef6427c9ca17',
           'qa9102app_b01@app.qa.invalid', '00091912007', 'B01'
    UNION ALL SELECT 'qa9102app_b02', 'b', 2, 8, '潘玉梅', 2, 46,
           160.00, 60.50, 23.63, 0.33, 'd3d95a8c6a2a7af470daae52601e04f4',
           'qa9102app_b02@app.qa.invalid', '00091912008', 'B02'
    UNION ALL SELECT 'qa9102app_b03', 'b', 3, 9, '董文斌', 1, 51,
           174.00, 73.20, 24.18, 0.41, 'd3d95a8c6a2a7af4a00bc31da90b1f6d',
           'qa9102app_b03@app.qa.invalid', '00091912009', 'B03'
    UNION ALL SELECT 'qa9102app_b04', 'b', 4, 10, '苏春燕', 2, 48,
           162.00, 62.10, 23.66, 0.35, 'd3d95a8c6a2a7af4d805f953913d5330',
           'qa9102app_b04@app.qa.invalid', '00091912010', 'B04'
    UNION ALL SELECT 'qa9102app_b05', 'b', 5, 11, '黎国华', 1, 53,
           170.00, 75.40, 26.09, 0.47, 'd3d95a8c6a2a7af402c871a1fc973a9b',
           'qa9102app_b05@app.qa.invalid', '00091912011', 'B05'
    UNION ALL SELECT 'qa9102app_b06', 'b', 6, 12, '龚晓红', 2, 50,
           159.00, 59.80, 23.66, 0.31, 'd3d95a8c6a2a7af43d739f948b152b8f',
           'qa9102app_b06@app.qa.invalid', '00091912012', 'B06'
    UNION ALL SELECT 'qa9102app_c01', 'c', 1, 13, '孟庆海', 1, 56,
           172.00, 78.60, 26.57, 0.55, 'd3d95a8c6a2a7af468e4a6b8223de4be',
           'qa9102app_c01@app.qa.invalid', '00091912013', 'C01'
    UNION ALL SELECT 'qa9102app_c02', 'c', 2, 14, '梁素芳', 2, 59,
           160.00, 72.40, 28.28, 0.67, 'd3d95a8c6a2a7af40ad5a9c37165a3cf',
           'qa9102app_c02@app.qa.invalid', '00091912014', 'C02'
    UNION ALL SELECT 'qa9102app_c03', 'c', 3, 15, '于洪波', 1, 47,
           175.00, 71.20, 23.25, 0.36, 'd3d95a8c6a2a7af40e6f476e863412ad',
           'qa9102app_c03@app.qa.invalid', '00091912015', 'C03'
    UNION ALL SELECT 'qa9102app_c04', 'c', 4, 16, '龙秀兰', 2, 62,
           158.00, 73.90, 29.60, 0.74, 'd3d95a8c6a2a7af46ba5162ba48e472d',
           'qa9102app_c04@app.qa.invalid', '00091912016', 'C04'
    UNION ALL SELECT 'qa9102app_c05', 'c', 5, 17, '余志刚', 1, 52,
           173.00, 76.80, 25.66, 0.49, 'd3d95a8c6a2a7af45873cda6237cd7a2',
           'qa9102app_c05@app.qa.invalid', '00091912017', 'C05'
    UNION ALL SELECT 'qa9102app_c06', 'c', 6, 18, '姚雪梅', 2, 45,
           163.00, 60.80, 22.88, 0.29, 'd3d95a8c6a2a7af4614959d320c73852',
           'qa9102app_c06@app.qa.invalid', '00091912018', 'C06'
    UNION ALL SELECT 'qa9102app_d01', 'd', 1, 19, '常国庆', 1, 57,
           172.00, 79.80, 26.97, 0.60, 'd3d95a8c6a2a7af4a47db22c7b3f2d06',
           'qa9102app_d01@app.qa.invalid', '00091912019', 'D01'
    UNION ALL SELECT 'qa9102app_d02', 'd', 2, 20, '严慧娟', 2, 60,
           159.00, 72.60, 28.72, 0.66, 'd3d95a8c6a2a7af4d093e138abc09076',
           'qa9102app_d02@app.qa.invalid', '00091912020', 'D02'
    UNION ALL SELECT 'qa9102app_d03', 'd', 3, 21, '范永军', 1, 54,
           174.00, 77.90, 25.73, 0.51, 'd3d95a8c6a2a7af41df0ba75e30cb532',
           'qa9102app_d03@app.qa.invalid', '00091912021', 'D03'
    UNION ALL SELECT 'qa9102app_d04', 'd', 4, 22, '苗丽珍', 2, 58,
           161.00, 71.80, 27.70, 0.63, 'd3d95a8c6a2a7af4b903344c58e79b08',
           'qa9102app_d04@app.qa.invalid', '00091912022', 'D04'
    UNION ALL SELECT 'qa9102app_d05', 'd', 5, 23, '白建平', 1, 50,
           176.00, 72.40, 23.37, 0.40, 'd3d95a8c6a2a7af49edb8948e95b6640',
           'qa9102app_d05@app.qa.invalid', '00091912023', 'D05'
    UNION ALL SELECT 'qa9102app_d06', 'd', 6, 24, '崔玉凤', 2, 55,
           160.00, 68.40, 26.72, 0.56, 'd3d95a8c6a2a7af4153247b4a9b16233',
           'qa9102app_d06@app.qa.invalid', '00091912024', 'D06'
    UNION ALL SELECT 'qa9102app_e01', 'e', 1, 25, '夏志远', 1, 48,
           173.00, 71.00, 23.72, 0.34, 'd3d95a8c6a2a7af43f7c6e128bc7bb7a',
           'qa9102app_e01@app.qa.invalid', '00091912025', 'E01'
    UNION ALL SELECT 'qa9102app_e02', 'e', 2, 26, '田丽娟', 2, 51,
           161.00, 63.40, 24.46, 0.37, 'd3d95a8c6a2a7af4b52076d6b6f30b74',
           'qa9102app_e02@app.qa.invalid', '00091912026', 'E02'
    UNION ALL SELECT 'qa9102app_e03', 'e', 3, 27, '任国栋', 1, 59,
           171.00, 80.20, 27.43, 0.65, 'd3d95a8c6a2a7af44c712bd5100d9726',
           'qa9102app_e03@app.qa.invalid', '00091912027', 'E03'
    UNION ALL SELECT 'qa9102app_e04', 'e', 4, 28, '侯艳秋', 2, 46,
           163.00, 59.60, 22.43, 0.27, 'd3d95a8c6a2a7af441e32f7bcf707c6b',
           'qa9102app_e04@app.qa.invalid', '00091912028', 'E04'
    UNION ALL SELECT 'qa9102app_e05', 'e', 5, 29, '卢建峰', 1, 53,
           174.00, 76.50, 25.27, 0.45, 'd3d95a8c6a2a7af4566050b9aadb12f8',
           'qa9102app_e05@app.qa.invalid', '00091912029', 'E05'
    UNION ALL SELECT 'qa9102app_e06', 'e', 6, 30, '尹春梅', 2, 61,
           158.00, 73.10, 29.29, 0.73, 'd3d95a8c6a2a7af46c6ad4de1eb40a4f',
           'qa9102app_e06@app.qa.invalid', '00091912030', 'E06'
) u
ON DUPLICATE KEY UPDATE
    realname = VALUES(realname), password = VALUES(password), salt = VALUES(salt),
    birthday = VALUES(birthday), sex = VALUES(sex), email = VALUES(email),
    phone = VALUES(phone), status = 1, del_flag = 0, work_no = VALUES(work_no),
    update_by = VALUES(update_by), update_time = VALUES(update_time),
    login_tenant_id = 0, sort = VALUES(sort), iz_hide_contact = '0';

-- APP 账号归类（app_user + insurance_service_user，tenant_id=0）
DELETE user_role
FROM sys_user_role user_role
JOIN sys_user u ON u.id = user_role.user_id
WHERE u.username LIKE 'qa9102app\_%' AND user_role.tenant_id <> 0;

INSERT INTO sys_user_role (id, user_id, role_id, tenant_id)
SELECT LOWER(MD5(CONCAT('REHEALTH_QA_TD_V1:user-role:', u.username, ':', role.role_code))),
       u.id, role.id, 0
FROM sys_user u
JOIN sys_role role ON role.role_code IN ('app_user', 'insurance_service_user')
WHERE u.username LIKE 'qa9102app\_%'
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id), role_id = VALUES(role_id), tenant_id = 0;

-- ============================================================================
-- 2. 患者档案（30 份）
-- ============================================================================
INSERT INTO rehealth_patient_profile (
    id, user_id, name, gender, age, height_cm, weight_kg, bmi,
    family_history, smoking, drinking, diabetes_history, hypertension_history,
    profile_version, profile_json, created_at, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:profile:', u.username), 256)),
    LOWER(MD5(CONCAT('REHEALTH_QA_TD_V1:user:', u.username))),
    u.realname, CASE WHEN u.sex = 1 THEN 'male' ELSE 'female' END,
    u.age, u.height_cm, u.weight_kg, u.bmi,
    IF(u.risk_base >= 0.45, 1, 0), IF(u.risk_base >= 0.65, 1, 0),
    IF(u.seq % 4 = 0, 1, 0), IF(u.risk_base >= 0.70, 1, 0),
    IF(u.risk_base >= 0.50, 1, 0), 1,
    JSON_OBJECT('source', 'REHEALTH_QA_TD_V1', 'scenario', 'insurance_workbench_categorized_app_user',
                'synthetic', TRUE, 'clinicalUseAllowed', FALSE),
    @seed_time, @seed_time
FROM (
    SELECT 'qa9102app_a01' username, '沈建国' realname, 1 sex, 58 age, 173.00 height_cm, 80.50 weight_kg, 26.90 bmi, 0.62 risk_base, 1 seq
    UNION ALL SELECT 'qa9102app_a02', '吕秀英', 2, 61, 161.00, 73.80, 28.47, 0.68, 2
    UNION ALL SELECT 'qa9102app_a03', '郝永强', 1, 55, 172.00, 79.10, 26.74, 0.59, 3
    UNION ALL SELECT 'qa9102app_a04', '石桂花', 2, 57, 160.00, 70.20, 27.42, 0.64, 4
    UNION ALL SELECT 'qa9102app_a05', '江志军', 1, 60, 174.00, 84.00, 27.74, 0.71, 5
    UNION ALL SELECT 'qa9102app_a06', '温丽华', 2, 52, 162.00, 68.90, 26.25, 0.57, 6
    UNION ALL SELECT 'qa9102app_b01', '冯建军', 1, 49, 171.00, 70.80, 24.21, 0.38, 7
    UNION ALL SELECT 'qa9102app_b02', '潘玉梅', 2, 46, 160.00, 60.50, 23.63, 0.33, 8
    UNION ALL SELECT 'qa9102app_b03', '董文斌', 1, 51, 174.00, 73.20, 24.18, 0.41, 9
    UNION ALL SELECT 'qa9102app_b04', '苏春燕', 2, 48, 162.00, 62.10, 23.66, 0.35, 10
    UNION ALL SELECT 'qa9102app_b05', '黎国华', 1, 53, 170.00, 75.40, 26.09, 0.47, 11
    UNION ALL SELECT 'qa9102app_b06', '龚晓红', 2, 50, 159.00, 59.80, 23.66, 0.31, 12
    UNION ALL SELECT 'qa9102app_c01', '孟庆海', 1, 56, 172.00, 78.60, 26.57, 0.55, 13
    UNION ALL SELECT 'qa9102app_c02', '梁素芳', 2, 59, 160.00, 72.40, 28.28, 0.67, 14
    UNION ALL SELECT 'qa9102app_c03', '于洪波', 1, 47, 175.00, 71.20, 23.25, 0.36, 15
    UNION ALL SELECT 'qa9102app_c04', '龙秀兰', 2, 62, 158.00, 73.90, 29.60, 0.74, 16
    UNION ALL SELECT 'qa9102app_c05', '余志刚', 1, 52, 173.00, 76.80, 25.66, 0.49, 17
    UNION ALL SELECT 'qa9102app_c06', '姚雪梅', 2, 45, 163.00, 60.80, 22.88, 0.29, 18
    UNION ALL SELECT 'qa9102app_d01', '常国庆', 1, 57, 172.00, 79.80, 26.97, 0.60, 19
    UNION ALL SELECT 'qa9102app_d02', '严慧娟', 2, 60, 159.00, 72.60, 28.72, 0.66, 20
    UNION ALL SELECT 'qa9102app_d03', '范永军', 1, 54, 174.00, 77.90, 25.73, 0.51, 21
    UNION ALL SELECT 'qa9102app_d04', '苗丽珍', 2, 58, 161.00, 71.80, 27.70, 0.63, 22
    UNION ALL SELECT 'qa9102app_d05', '白建平', 1, 50, 176.00, 72.40, 23.37, 0.40, 23
    UNION ALL SELECT 'qa9102app_d06', '崔玉凤', 2, 55, 160.00, 68.40, 26.72, 0.56, 24
    UNION ALL SELECT 'qa9102app_e01', '夏志远', 1, 48, 173.00, 71.00, 23.72, 0.34, 25
    UNION ALL SELECT 'qa9102app_e02', '田丽娟', 2, 51, 161.00, 63.40, 24.46, 0.37, 26
    UNION ALL SELECT 'qa9102app_e03', '任国栋', 1, 59, 171.00, 80.20, 27.43, 0.65, 27
    UNION ALL SELECT 'qa9102app_e04', '侯艳秋', 2, 46, 163.00, 59.60, 22.43, 0.27, 28
    UNION ALL SELECT 'qa9102app_e05', '卢建峰', 1, 53, 174.00, 76.50, 25.27, 0.45, 29
    UNION ALL SELECT 'qa9102app_e06', '尹春梅', 2, 61, 158.00, 73.10, 29.29, 0.73, 30
) u
ON DUPLICATE KEY UPDATE
    name = VALUES(name), gender = VALUES(gender), age = VALUES(age),
    height_cm = VALUES(height_cm), weight_kg = VALUES(weight_kg), bmi = VALUES(bmi),
    family_history = VALUES(family_history), smoking = VALUES(smoking),
    drinking = VALUES(drinking), diabetes_history = VALUES(diabetes_history),
    hypertension_history = VALUES(hypertension_history),
    profile_json = VALUES(profile_json), updated_at = VALUES(updated_at);

-- ============================================================================
-- 3. 保险主体（30 条，全部 active + granted，保证进入工作台可见范围）
-- ============================================================================
INSERT INTO rehealth_insurance_subject (
    id, tenant_id, subject_ref, rehealth_user_id, external_subject_ref_hash,
    enrollment_status, consent_status, consent_version, consented_at,
    source_system, source_record_id, metadata_json, created_at, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:subject:9102:', u.cat, u.idx), 256)),
    @tenant, CONCAT('QA9102-', UPPER(u.cat), LPAD(u.idx, 2, '0')),
    LOWER(MD5(CONCAT('REHEALTH_QA_TD_V1:user:', u.username))),
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:external:9102:', u.username), 256)),
    'active', 'granted', 'qa-td-v1', DATE_SUB(@seed_time, INTERVAL 120 DAY),
    'REHEALTH_QA_TD_V1', CONCAT('subject-9102-', u.cat, u.idx),
    JSON_OBJECT('synthetic', TRUE, 'clinicalUseAllowed', FALSE, 'category', u.cat),
    DATE_SUB(@seed_time, INTERVAL 120 DAY), @seed_time
FROM (
    SELECT 'qa9102app_a01' username, 'a' cat, 1 idx
    UNION ALL SELECT 'qa9102app_a02', 'a', 2
    UNION ALL SELECT 'qa9102app_a03', 'a', 3
    UNION ALL SELECT 'qa9102app_a04', 'a', 4
    UNION ALL SELECT 'qa9102app_a05', 'a', 5
    UNION ALL SELECT 'qa9102app_a06', 'a', 6
    UNION ALL SELECT 'qa9102app_b01', 'b', 1
    UNION ALL SELECT 'qa9102app_b02', 'b', 2
    UNION ALL SELECT 'qa9102app_b03', 'b', 3
    UNION ALL SELECT 'qa9102app_b04', 'b', 4
    UNION ALL SELECT 'qa9102app_b05', 'b', 5
    UNION ALL SELECT 'qa9102app_b06', 'b', 6
    UNION ALL SELECT 'qa9102app_c01', 'c', 1
    UNION ALL SELECT 'qa9102app_c02', 'c', 2
    UNION ALL SELECT 'qa9102app_c03', 'c', 3
    UNION ALL SELECT 'qa9102app_c04', 'c', 4
    UNION ALL SELECT 'qa9102app_c05', 'c', 5
    UNION ALL SELECT 'qa9102app_c06', 'c', 6
    UNION ALL SELECT 'qa9102app_d01', 'd', 1
    UNION ALL SELECT 'qa9102app_d02', 'd', 2
    UNION ALL SELECT 'qa9102app_d03', 'd', 3
    UNION ALL SELECT 'qa9102app_d04', 'd', 4
    UNION ALL SELECT 'qa9102app_d05', 'd', 5
    UNION ALL SELECT 'qa9102app_d06', 'd', 6
    UNION ALL SELECT 'qa9102app_e01', 'e', 1
    UNION ALL SELECT 'qa9102app_e02', 'e', 2
    UNION ALL SELECT 'qa9102app_e03', 'e', 3
    UNION ALL SELECT 'qa9102app_e04', 'e', 4
    UNION ALL SELECT 'qa9102app_e05', 'e', 5
    UNION ALL SELECT 'qa9102app_e06', 'e', 6
) u
ON DUPLICATE KEY UPDATE
    rehealth_user_id = VALUES(rehealth_user_id),
    enrollment_status = 'active', consent_status = 'granted',
    consented_at = VALUES(consented_at), metadata_json = VALUES(metadata_json),
    updated_at = VALUES(updated_at);

-- ============================================================================
-- 4. 保单 / 保障 / 授权 / 绑定（每主体 1 条）
-- ============================================================================
INSERT INTO rehealth_insurance_policy (
    id, tenant_id, policy_no, product_code, product_name, policy_type,
    policyholder_subject_ref, insured_subject_ref, coverage_amount,
    premium_amount, deductible_amount, waiting_period_days, effective_on,
    expires_on, status, source_system, source_record_id, metadata_json,
    created_at, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:policy:9102:', u.cat, u.idx), 256)),
    @tenant, CONCAT('RH-9102-QA-', LPAD(u.seq, 4, '0')),
    CASE MOD(u.seq - 1, 4)
        WHEN 0 THEN 'GROUP-MED' WHEN 1 THEN 'LONG-MED' WHEN 2 THEN 'CI-PLUS' ELSE 'CVD-CARE' END,
    CASE MOD(u.seq - 1, 4)
        WHEN 0 THEN '悦享健康团体医疗保障计划' WHEN 1 THEN '安心守护长期医疗保障计划'
        WHEN 2 THEN '康护无忧重大疾病保障计划' ELSE '臻享心脑血管专项保障计划' END,
    CASE MOD(u.seq - 1, 4)
        WHEN 0 THEN 'group_medical' WHEN 1 THEN 'long_term_medical'
        WHEN 2 THEN 'critical_illness' ELSE 'cvd_management' END,
    NULL, CONCAT('QA9102-', UPPER(u.cat), LPAD(u.idx, 2, '0')),
    CASE MOD(u.seq - 1, 4) WHEN 0 THEN 500000.00 WHEN 1 THEN 1000000.00
        WHEN 2 THEN 800000.00 ELSE 300000.00 END,
    1000.00 + u.seq * 60, CASE WHEN MOD(u.seq - 1, 4) = 2 THEN 0.00 ELSE 500.00 END,
    CASE WHEN MOD(u.seq - 1, 4) = 0 THEN 0 ELSE 30 END,
    DATE_SUB(@anchor_date, INTERVAL 150 DAY), DATE_ADD(@anchor_date, INTERVAL 395 DAY),
    'active', 'REHEALTH_QA_TD_V1', CONCAT('policy-9102-qa-', u.seq),
    JSON_OBJECT('synthetic', TRUE, 'clinicalUseAllowed', FALSE),
    DATE_SUB(@seed_time, INTERVAL 150 DAY), @seed_time
FROM (
    SELECT 'qa9102app_a01' username, 'a' cat, 1 idx, 1 seq
    UNION ALL SELECT 'qa9102app_a02', 'a', 2, 2
    UNION ALL SELECT 'qa9102app_a03', 'a', 3, 3
    UNION ALL SELECT 'qa9102app_a04', 'a', 4, 4
    UNION ALL SELECT 'qa9102app_a05', 'a', 5, 5
    UNION ALL SELECT 'qa9102app_a06', 'a', 6, 6
    UNION ALL SELECT 'qa9102app_b01', 'b', 1, 7
    UNION ALL SELECT 'qa9102app_b02', 'b', 2, 8
    UNION ALL SELECT 'qa9102app_b03', 'b', 3, 9
    UNION ALL SELECT 'qa9102app_b04', 'b', 4, 10
    UNION ALL SELECT 'qa9102app_b05', 'b', 5, 11
    UNION ALL SELECT 'qa9102app_b06', 'b', 6, 12
    UNION ALL SELECT 'qa9102app_c01', 'c', 1, 13
    UNION ALL SELECT 'qa9102app_c02', 'c', 2, 14
    UNION ALL SELECT 'qa9102app_c03', 'c', 3, 15
    UNION ALL SELECT 'qa9102app_c04', 'c', 4, 16
    UNION ALL SELECT 'qa9102app_c05', 'c', 5, 17
    UNION ALL SELECT 'qa9102app_c06', 'c', 6, 18
    UNION ALL SELECT 'qa9102app_d01', 'd', 1, 19
    UNION ALL SELECT 'qa9102app_d02', 'd', 2, 20
    UNION ALL SELECT 'qa9102app_d03', 'd', 3, 21
    UNION ALL SELECT 'qa9102app_d04', 'd', 4, 22
    UNION ALL SELECT 'qa9102app_d05', 'd', 5, 23
    UNION ALL SELECT 'qa9102app_d06', 'd', 6, 24
    UNION ALL SELECT 'qa9102app_e01', 'e', 1, 25
    UNION ALL SELECT 'qa9102app_e02', 'e', 2, 26
    UNION ALL SELECT 'qa9102app_e03', 'e', 3, 27
    UNION ALL SELECT 'qa9102app_e04', 'e', 4, 28
    UNION ALL SELECT 'qa9102app_e05', 'e', 5, 29
    UNION ALL SELECT 'qa9102app_e06', 'e', 6, 30
) u
ON DUPLICATE KEY UPDATE
    policy_no = VALUES(policy_no), insured_subject_ref = VALUES(insured_subject_ref),
    coverage_amount = VALUES(coverage_amount), premium_amount = VALUES(premium_amount),
    effective_on = VALUES(effective_on), expires_on = VALUES(expires_on),
    status = 'active', updated_at = VALUES(updated_at);

INSERT INTO rehealth_insurance_coverage (
    id, tenant_id, policy_id, subject_ref, coverage_code, coverage_name,
    limit_amount, deductible_amount, effective_on, expires_on, status,
    source_system, source_record_id, metadata_json, created_at, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:coverage:9102:', u.cat, u.idx), 256)),
    @tenant, LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:policy:9102:', u.cat, u.idx), 256)),
    CONCAT('QA9102-', UPPER(u.cat), LPAD(u.idx, 2, '0')),
    'INPATIENT', '住院医疗费用保障',
    CASE MOD(u.seq - 1, 4) WHEN 0 THEN 500000.00 WHEN 1 THEN 1000000.00
        WHEN 2 THEN 800000.00 ELSE 300000.00 END,
    CASE WHEN MOD(u.seq - 1, 4) = 2 THEN 0.00 ELSE 500.00 END,
    DATE_SUB(@anchor_date, INTERVAL 150 DAY), DATE_ADD(@anchor_date, INTERVAL 395 DAY),
    'active', 'REHEALTH_QA_TD_V1', CONCAT('coverage-9102-qa-', u.seq),
    JSON_OBJECT('synthetic', TRUE), @seed_time, @seed_time
FROM (
    SELECT 'a' cat, 1 idx, 1 seq UNION ALL SELECT 'a', 2, 2 UNION ALL SELECT 'a', 3, 3
    UNION ALL SELECT 'a', 4, 4 UNION ALL SELECT 'a', 5, 5 UNION ALL SELECT 'a', 6, 6
    UNION ALL SELECT 'b', 1, 7 UNION ALL SELECT 'b', 2, 8 UNION ALL SELECT 'b', 3, 9
    UNION ALL SELECT 'b', 4, 10 UNION ALL SELECT 'b', 5, 11 UNION ALL SELECT 'b', 6, 12
    UNION ALL SELECT 'c', 1, 13 UNION ALL SELECT 'c', 2, 14 UNION ALL SELECT 'c', 3, 15
    UNION ALL SELECT 'c', 4, 16 UNION ALL SELECT 'c', 5, 17 UNION ALL SELECT 'c', 6, 18
    UNION ALL SELECT 'd', 1, 19 UNION ALL SELECT 'd', 2, 20 UNION ALL SELECT 'd', 3, 21
    UNION ALL SELECT 'd', 4, 22 UNION ALL SELECT 'd', 5, 23 UNION ALL SELECT 'd', 6, 24
    UNION ALL SELECT 'e', 1, 25 UNION ALL SELECT 'e', 2, 26 UNION ALL SELECT 'e', 3, 27
    UNION ALL SELECT 'e', 4, 28 UNION ALL SELECT 'e', 5, 29 UNION ALL SELECT 'e', 6, 30
) u
ON DUPLICATE KEY UPDATE
    policy_id = VALUES(policy_id), subject_ref = VALUES(subject_ref),
    limit_amount = VALUES(limit_amount), deductible_amount = VALUES(deductible_amount),
    status = 'active', updated_at = VALUES(updated_at);

INSERT INTO rehealth_insurance_consent (
    id, tenant_id, subject_ref, consent_type, consent_version, status,
    granted_at, revoked_at, evidence_ref, evidence_hash, source_system,
    source_record_id, metadata_json, created_at, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:consent:9102:', u.cat, u.idx, ':'), 256)),
    @tenant, CONCAT('QA9102-', UPPER(u.cat), LPAD(u.idx, 2, '0')),
    'insurance_health_management', 'qa-td-v1', 'granted',
    DATE_SUB(@seed_time, INTERVAL 120 DAY), NULL,
    CONCAT('RH-CONSENT-9102-Q', LPAD(u.seq, 4, '0')),
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:consent-evidence:9102:', u.cat, u.idx), 256)),
    'REHEALTH_QA_TD_V1', CONCAT('consent-9102-qa-', u.seq),
    JSON_OBJECT('synthetic', TRUE, 'scope', 'assigned-staff-full-business-data'),
    DATE_SUB(@seed_time, INTERVAL 120 DAY), @seed_time
FROM (
    SELECT 'a' cat, 1 idx, 1 seq UNION ALL SELECT 'a', 2, 2 UNION ALL SELECT 'a', 3, 3
    UNION ALL SELECT 'a', 4, 4 UNION ALL SELECT 'a', 5, 5 UNION ALL SELECT 'a', 6, 6
    UNION ALL SELECT 'b', 1, 7 UNION ALL SELECT 'b', 2, 8 UNION ALL SELECT 'b', 3, 9
    UNION ALL SELECT 'b', 4, 10 UNION ALL SELECT 'b', 5, 11 UNION ALL SELECT 'b', 6, 12
    UNION ALL SELECT 'c', 1, 13 UNION ALL SELECT 'c', 2, 14 UNION ALL SELECT 'c', 3, 15
    UNION ALL SELECT 'c', 4, 16 UNION ALL SELECT 'c', 5, 17 UNION ALL SELECT 'c', 6, 18
    UNION ALL SELECT 'd', 1, 19 UNION ALL SELECT 'd', 2, 20 UNION ALL SELECT 'd', 3, 21
    UNION ALL SELECT 'd', 4, 22 UNION ALL SELECT 'd', 5, 23 UNION ALL SELECT 'd', 6, 24
    UNION ALL SELECT 'e', 1, 25 UNION ALL SELECT 'e', 2, 26 UNION ALL SELECT 'e', 3, 27
    UNION ALL SELECT 'e', 4, 28 UNION ALL SELECT 'e', 5, 29 UNION ALL SELECT 'e', 6, 30
) u
ON DUPLICATE KEY UPDATE
    status = 'granted', granted_at = VALUES(granted_at), revoked_at = NULL,
    evidence_ref = VALUES(evidence_ref), evidence_hash = VALUES(evidence_hash),
    updated_at = VALUES(updated_at);

INSERT INTO rehealth_insurance_plan_binding (
    id, tenant_id, subject_ref, policy_id, plan_id, consent_id, status,
    bound_at, unbound_at, source_system, source_record_id, metadata_json,
    created_at, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:binding:9102:', u.cat, u.idx), 256)),
    @tenant, CONCAT('QA9102-', UPPER(u.cat), LPAD(u.idx, 2, '0')),
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:policy:9102:', u.cat, u.idx), 256)),
    CONCAT('qa9102-plan-', u.cat, u.idx),
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:consent:9102:', u.cat, u.idx, ':'), 256)),
    'active', DATE_SUB(@seed_time, INTERVAL 90 DAY), NULL,
    'REHEALTH_QA_TD_V1', CONCAT('binding-9102-qa-', u.seq),
    JSON_OBJECT('synthetic', TRUE), DATE_SUB(@seed_time, INTERVAL 90 DAY), @seed_time
FROM (
    SELECT 'a' cat, 1 idx, 1 seq UNION ALL SELECT 'a', 2, 2 UNION ALL SELECT 'a', 3, 3
    UNION ALL SELECT 'a', 4, 4 UNION ALL SELECT 'a', 5, 5 UNION ALL SELECT 'a', 6, 6
    UNION ALL SELECT 'b', 1, 7 UNION ALL SELECT 'b', 2, 8 UNION ALL SELECT 'b', 3, 9
    UNION ALL SELECT 'b', 4, 10 UNION ALL SELECT 'b', 5, 11 UNION ALL SELECT 'b', 6, 12
    UNION ALL SELECT 'c', 1, 13 UNION ALL SELECT 'c', 2, 14 UNION ALL SELECT 'c', 3, 15
    UNION ALL SELECT 'c', 4, 16 UNION ALL SELECT 'c', 5, 17 UNION ALL SELECT 'c', 6, 18
    UNION ALL SELECT 'd', 1, 19 UNION ALL SELECT 'd', 2, 20 UNION ALL SELECT 'd', 3, 21
    UNION ALL SELECT 'd', 4, 22 UNION ALL SELECT 'd', 5, 23 UNION ALL SELECT 'd', 6, 24
    UNION ALL SELECT 'e', 1, 25 UNION ALL SELECT 'e', 2, 26 UNION ALL SELECT 'e', 3, 27
    UNION ALL SELECT 'e', 4, 28 UNION ALL SELECT 'e', 5, 29 UNION ALL SELECT 'e', 6, 30
) u
ON DUPLICATE KEY UPDATE
    policy_id = VALUES(policy_id), plan_id = VALUES(plan_id), consent_id = VALUES(consent_id),
    status = 'active', bound_at = VALUES(bound_at), unbound_at = NULL,
    updated_at = VALUES(updated_at);

-- ============================================================================
-- 5. 干预服务关系
--    C 类 enrolled（触发 in_progress）；A 类 3 条 completed、B 类 3 条 completed、
--    D 类 6 条 completed；E 类无干预（空态）
-- ============================================================================
INSERT INTO rehealth_insurance_intervention (
    id, tenant_id, subject_ref, plan_id, source_plan_id, consent_id, status,
    enrolled_at, ended_at, last_feedback_at, source_system, source_record_id,
    metadata_json, created_at, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:insurance-intervention:9102:', u.cat, u.idx), 256)),
    @tenant, CONCAT('QA9102-', UPPER(u.cat), LPAD(u.idx, 2, '0')),
    CONCAT('qa9102-plan-', u.cat, u.idx),
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:plan:', u.username), 256)),
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:consent:9102:', u.cat, u.idx, ':'), 256)),
    u.status,
    DATE_SUB(@seed_time, INTERVAL 90 DAY),
    CASE WHEN u.status = 'completed' THEN DATE_SUB(@seed_time, INTERVAL 2 DAY) ELSE NULL END,
    CASE WHEN u.status = 'completed' THEN DATE_SUB(@seed_time, INTERVAL 1 DAY) ELSE NULL END,
    'REHEALTH_QA_TD_V1', CONCAT('intervention-9102-', u.cat, u.idx),
    JSON_OBJECT('synthetic', TRUE, 'clinicalUseAllowed', FALSE),
    DATE_SUB(@seed_time, INTERVAL 90 DAY), @seed_time
FROM (
    SELECT 'qa9102app_a01' username, 'a' cat, 1 idx, 'completed' status
    UNION ALL SELECT 'qa9102app_a02', 'a', 2, 'completed'
    UNION ALL SELECT 'qa9102app_a03', 'a', 3, 'completed'
    UNION ALL SELECT 'qa9102app_a04', 'a', 4, 'completed'
    UNION ALL SELECT 'qa9102app_a05', 'a', 5, 'completed'
    UNION ALL SELECT 'qa9102app_a06', 'a', 6, 'completed'
    UNION ALL SELECT 'qa9102app_b01', 'b', 1, 'completed'
    UNION ALL SELECT 'qa9102app_b02', 'b', 2, 'completed'
    UNION ALL SELECT 'qa9102app_b03', 'b', 3, 'completed'
    UNION ALL SELECT 'qa9102app_b04', 'b', 4, 'completed'
    UNION ALL SELECT 'qa9102app_b05', 'b', 5, 'completed'
    UNION ALL SELECT 'qa9102app_b06', 'b', 6, 'completed'
    UNION ALL SELECT 'qa9102app_c01', 'c', 1, 'enrolled'
    UNION ALL SELECT 'qa9102app_c02', 'c', 2, 'enrolled'
    UNION ALL SELECT 'qa9102app_c03', 'c', 3, 'enrolled'
    UNION ALL SELECT 'qa9102app_c04', 'c', 4, 'enrolled'
    UNION ALL SELECT 'qa9102app_c05', 'c', 5, 'enrolled'
    UNION ALL SELECT 'qa9102app_c06', 'c', 6, 'enrolled'
    UNION ALL SELECT 'qa9102app_d01', 'd', 1, 'completed'
    UNION ALL SELECT 'qa9102app_d02', 'd', 2, 'completed'
    UNION ALL SELECT 'qa9102app_d03', 'd', 3, 'completed'
    UNION ALL SELECT 'qa9102app_d04', 'd', 4, 'completed'
    UNION ALL SELECT 'qa9102app_d05', 'd', 5, 'completed'
    UNION ALL SELECT 'qa9102app_d06', 'd', 6, 'completed'
) u
ON DUPLICATE KEY UPDATE
    source_plan_id = VALUES(source_plan_id), consent_id = VALUES(consent_id),
    status = VALUES(status), ended_at = VALUES(ended_at),
    last_feedback_at = VALUES(last_feedback_at), metadata_json = VALUES(metadata_json),
    updated_at = VALUES(updated_at);

-- ============================================================================
-- 6. 干预反馈（28 天窗口内：A 类 1 条/人、B 类 2 条/人、C/D 类 3 条/人、E 类无）
-- ============================================================================
INSERT INTO rehealth_insurance_intervention_feedback (
    id, tenant_id, binding_id, subject_ref, intervention_id, feedback_type,
    occurred_at, completion_rate, adherence_score, plan_item_id,
    expected_count, completed_count, verification_type, calculation_version,
    outcome_summary_json, source_system, source_record_id, created_at
)
SELECT
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:feedback:9102:', u.cat, u.idx, ':', fb.fb_no), 256)),
    @tenant,
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:binding:9102:', u.cat, u.idx), 256)),
    CONCAT('QA9102-', UPPER(u.cat), LPAD(u.idx, 2, '0')),
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:insurance-intervention:9102:', u.cat, u.idx), 256)),
    CASE
        WHEN u.rate >= 0.95 THEN 'completed'
        WHEN u.rate >= 0.20 THEN 'partially_completed'
        ELSE 'skipped'
    END,
    DATE_SUB(@seed_time, INTERVAL (9 - fb.fb_no * 3) DAY),
    u.rate, u.rate,
    CONCAT('qa9102-item-', LPAD(MOD(u.seq - 1, 4) + 1, 2, '0'), '-', fb.fb_no),
    1.000, ROUND(u.rate, 3),
    'self_report', 'insurance-adherence-event-v1',
    JSON_OBJECT('synthetic', TRUE, 'clinicalUseAllowed', FALSE,
                'note', CASE fb.fb_no
                    WHEN 1 THEN '已确认计划并开始执行'
                    WHEN 2 THEN '已完成睡眠和运动记录'
                    ELSE '已回传本周执行结果' END),
    'REHEALTH_QA_TD_V1',
    CONCAT('feedback-9102-', u.cat, u.idx, '-', fb.fb_no),
    @seed_time
FROM (
    SELECT 'a' cat, 1 idx, 1 seq, 1 fb_count, 0.55 rate
    UNION ALL SELECT 'a', 2, 2, 1, 0.48
    UNION ALL SELECT 'a', 3, 3, 1, 0.62
    UNION ALL SELECT 'a', 4, 4, 1, 0.41
    UNION ALL SELECT 'a', 5, 5, 1, 0.58
    UNION ALL SELECT 'a', 6, 6, 1, 0.45
    UNION ALL SELECT 'b', 1, 7, 2, 0.72
    UNION ALL SELECT 'b', 2, 8, 2, 0.78
    UNION ALL SELECT 'b', 3, 9, 2, 0.65
    UNION ALL SELECT 'b', 4, 10, 2, 0.81
    UNION ALL SELECT 'b', 5, 11, 2, 0.60
    UNION ALL SELECT 'b', 6, 12, 2, 0.85
    UNION ALL SELECT 'c', 1, 13, 3, 0.70
    UNION ALL SELECT 'c', 2, 14, 3, 0.52
    UNION ALL SELECT 'c', 3, 15, 3, 0.88
    UNION ALL SELECT 'c', 4, 16, 3, 0.35
    UNION ALL SELECT 'c', 5, 17, 3, 0.66
    UNION ALL SELECT 'c', 6, 18, 3, 0.90
    UNION ALL SELECT 'd', 1, 19, 3, 0.92
    UNION ALL SELECT 'd', 2, 20, 3, 0.86
    UNION ALL SELECT 'd', 3, 21, 3, 0.97
    UNION ALL SELECT 'd', 4, 22, 3, 0.80
    UNION ALL SELECT 'd', 5, 23, 3, 0.94
    UNION ALL SELECT 'd', 6, 24, 3, 0.89
) u
JOIN (
    SELECT 1 fb_no, 0.0 offset_2 UNION ALL SELECT 2, 0.05 UNION ALL SELECT 3, 0.10
) fb ON fb.fb_no <= u.fb_count
ON DUPLICATE KEY UPDATE
    binding_id = VALUES(binding_id), subject_ref = VALUES(subject_ref),
    intervention_id = VALUES(intervention_id), feedback_type = VALUES(feedback_type),
    occurred_at = VALUES(occurred_at), completion_rate = VALUES(completion_rate),
    adherence_score = VALUES(adherence_score), plan_item_id = VALUES(plan_item_id),
    completed_count = VALUES(completed_count), outcome_summary_json = VALUES(outcome_summary_json),
    created_at = VALUES(created_at);

-- 反馈率按序号微调，保证类型分布（completed/partially_completed/skipped 混合）
UPDATE rehealth_insurance_intervention_feedback f
JOIN (
    SELECT LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:feedback:9102:', cat, idx, ':', fb_no), 256)) id, rate
    FROM (
        SELECT 'a' cat, 1 idx, 1 fb_no, 0.55 rate UNION ALL SELECT 'a', 2, 1, 0.48
        UNION ALL SELECT 'a', 3, 1, 0.62 UNION ALL SELECT 'a', 4, 1, 0.41
        UNION ALL SELECT 'a', 5, 1, 0.58 UNION ALL SELECT 'a', 6, 1, 0.45
        UNION ALL SELECT 'b', 1, 1, 0.72 UNION ALL SELECT 'b', 1, 2, 0.78
        UNION ALL SELECT 'b', 2, 1, 0.78 UNION ALL SELECT 'b', 2, 2, 0.85
        UNION ALL SELECT 'b', 3, 1, 0.65 UNION ALL SELECT 'b', 3, 2, 0.70
        UNION ALL SELECT 'b', 4, 1, 0.81 UNION ALL SELECT 'b', 4, 2, 0.88
        UNION ALL SELECT 'b', 5, 1, 0.60 UNION ALL SELECT 'b', 5, 2, 0.64
        UNION ALL SELECT 'b', 6, 1, 0.85 UNION ALL SELECT 'b', 6, 2, 0.92
        UNION ALL SELECT 'c', 1, 1, 0.70 UNION ALL SELECT 'c', 1, 2, 0.76 UNION ALL SELECT 'c', 1, 3, 0.82
        UNION ALL SELECT 'c', 2, 1, 0.52 UNION ALL SELECT 'c', 2, 2, 0.58 UNION ALL SELECT 'c', 2, 3, 0.60
        UNION ALL SELECT 'c', 3, 1, 0.88 UNION ALL SELECT 'c', 3, 2, 0.92 UNION ALL SELECT 'c', 3, 3, 0.97
        UNION ALL SELECT 'c', 4, 1, 0.35 UNION ALL SELECT 'c', 4, 2, 0.38 UNION ALL SELECT 'c', 4, 3, 0.42
        UNION ALL SELECT 'c', 5, 1, 0.66 UNION ALL SELECT 'c', 5, 2, 0.72 UNION ALL SELECT 'c', 5, 3, 0.78
        UNION ALL SELECT 'c', 6, 1, 0.90 UNION ALL SELECT 'c', 6, 2, 0.93 UNION ALL SELECT 'c', 6, 3, 0.96
        UNION ALL SELECT 'd', 1, 1, 0.92 UNION ALL SELECT 'd', 1, 2, 0.95 UNION ALL SELECT 'd', 1, 3, 0.98
        UNION ALL SELECT 'd', 2, 1, 0.86 UNION ALL SELECT 'd', 2, 2, 0.90 UNION ALL SELECT 'd', 2, 3, 0.94
        UNION ALL SELECT 'd', 3, 1, 0.97 UNION ALL SELECT 'd', 3, 2, 0.99 UNION ALL SELECT 'd', 3, 3, 1.00
        UNION ALL SELECT 'd', 4, 1, 0.80 UNION ALL SELECT 'd', 4, 2, 0.84 UNION ALL SELECT 'd', 4, 3, 0.88
        UNION ALL SELECT 'd', 5, 1, 0.94 UNION ALL SELECT 'd', 5, 2, 0.97 UNION ALL SELECT 'd', 5, 3, 0.99
        UNION ALL SELECT 'd', 6, 1, 0.89 UNION ALL SELECT 'd', 6, 2, 0.91 UNION ALL SELECT 'd', 6, 3, 0.95
    ) x
) t ON f.id = t.id
SET f.completion_rate = t.rate,
    f.adherence_score = t.rate,
    f.completed_count = ROUND(t.rate, 3),
    f.feedback_type = CASE WHEN t.rate >= 0.95 THEN 'completed'
                           WHEN t.rate >= 0.20 THEN 'partially_completed'
                           ELSE 'skipped' END
WHERE f.tenant_id = @tenant AND f.source_system = 'REHEALTH_QA_TD_V1';

-- ============================================================================
-- 7. 人工行动
--    C 类 2 条/人（1 pending + 1 in_progress，触发 in_progress 状态）
--    A/B/D 类各 1 条 completed；E 类无行动
-- ============================================================================
INSERT INTO rehealth_insurance_intervention_action (
    id, tenant_id, subject_ref, plan_id, action_type, title, content,
    assignee_user_id, status, due_at, completed_at, result_json,
    created_by, request_id, created_at, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:workbench-action:9102:', u.cat, u.idx, ':', a.action_no), 256)),
    @tenant, CONCAT('QA9102-', UPPER(u.cat), LPAD(u.idx, 2, '0')),
    CONCAT('qa9102-plan-', u.cat, u.idx),
    CASE WHEN a.action_no = 1 THEN 'followup' ELSE 'reminder' END,
    CASE WHEN a.action_no = 1 THEN '首次健康管理随访' ELSE '计划执行提醒' END,
    '核对计划理解情况和当前执行困难，记录后续跟进重点；不构成医疗建议。',
    assignee.id, u.action_status,
    CASE WHEN a.action_no = 1 THEN DATE_ADD(@seed_time, INTERVAL 2 DAY)
         ELSE DATE_ADD(@seed_time, INTERVAL 5 DAY) END,
    CASE WHEN u.action_status = 'completed' THEN DATE_SUB(@seed_time, INTERVAL 3 DAY) ELSE NULL END,
    JSON_OBJECT('synthetic', TRUE, 'clinicalUseAllowed', FALSE,
                'result', IF(u.action_status = 'completed', '已完成随访记录', '待执行')),
    assignee.id,
    CONCAT('qa-td-action-9102-', u.cat, u.idx, '-', a.action_no),
    DATE_SUB(@seed_time, INTERVAL 3 DAY), @seed_time
FROM (
    SELECT 'a' cat, 1 idx, 'completed' action_status, 'local_ins_9102_mgr_health' assignee_username
    UNION ALL SELECT 'a', 2, 'completed', 'local_ins_9102_mgr_health'
    UNION ALL SELECT 'a', 3, 'completed', 'local_ins_9102_mgr_health'
    UNION ALL SELECT 'a', 4, 'completed', 'local_ins_9102_mgr_health'
    UNION ALL SELECT 'a', 5, 'completed', 'local_ins_9102_mgr_health'
    UNION ALL SELECT 'a', 6, 'completed', 'local_ins_9102_mgr_health'
    UNION ALL SELECT 'b', 1, 'completed', 'local_ins_9102_mgr_health'
    UNION ALL SELECT 'b', 2, 'completed', 'local_ins_9102_mgr_health'
    UNION ALL SELECT 'b', 3, 'completed', 'local_ins_9102_mgr_health'
    UNION ALL SELECT 'b', 4, 'completed', 'local_ins_9102_mgr_health'
    UNION ALL SELECT 'b', 5, 'completed', 'local_ins_9102_mgr_health'
    UNION ALL SELECT 'b', 6, 'completed', 'local_ins_9102_mgr_health'
    UNION ALL SELECT 'c', 1, 'pending', 'local_ins_9102_operator'
    UNION ALL SELECT 'c', 2, 'pending', 'local_ins_9102_operator'
    UNION ALL SELECT 'c', 3, 'pending', 'local_ins_9102_operator'
    UNION ALL SELECT 'c', 4, 'pending', 'local_ins_9102_operator'
    UNION ALL SELECT 'c', 5, 'pending', 'local_ins_9102_operator'
    UNION ALL SELECT 'c', 6, 'pending', 'local_ins_9102_operator'
    UNION ALL SELECT 'd', 1, 'completed', 'local_ins_9102_operator'
    UNION ALL SELECT 'd', 2, 'completed', 'local_ins_9102_operator'
    UNION ALL SELECT 'd', 3, 'completed', 'local_ins_9102_operator'
    UNION ALL SELECT 'd', 4, 'completed', 'local_ins_9102_operator'
    UNION ALL SELECT 'd', 5, 'completed', 'local_ins_9102_operator'
    UNION ALL SELECT 'd', 6, 'completed', 'local_ins_9102_operator'
) u
JOIN sys_user assignee
  ON assignee.username = u.assignee_username AND assignee.status = 1 AND assignee.del_flag = 0
JOIN (
    SELECT 1 action_no UNION ALL SELECT 2
) a ON (u.cat = 'c' OR a.action_no = 1)
ON DUPLICATE KEY UPDATE
    subject_ref = VALUES(subject_ref), plan_id = VALUES(plan_id),
    assignee_user_id = VALUES(assignee_user_id), status = VALUES(status),
    due_at = VALUES(due_at), completed_at = VALUES(completed_at),
    result_json = VALUES(result_json), created_by = VALUES(created_by),
    updated_at = VALUES(updated_at);

-- C 类第 2 条行动置为 in_progress
UPDATE rehealth_insurance_intervention_action
SET status = 'in_progress', completed_at = NULL
WHERE tenant_id = @tenant AND request_id LIKE 'qa-td-action-9102-c%-2';

-- ============================================================================
-- 8. 负责人关系
--    机构管理员负责全部 30 人；健康险经理负责 A/B 类；运营专员负责 C/D 类；
--    风控经理负责 D 类；分析员负责 E 类
-- ============================================================================
INSERT INTO rehealth_insurance_subject_manager (
    id, tenant_id, manager_user_id, department_id, subject_ref,
    status, source_system, created_at, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:assignment:9102:', u.staff_username, ':', u.cat, u.idx), 256)),
    @tenant, staff.id, depart.id,
    CONCAT('QA9102-', UPPER(u.cat), LPAD(u.idx, 2, '0')),
    'active', 'REHEALTH_QA_TD_V1', @seed_time, @seed_time
FROM (
    SELECT 'a' cat, 1 idx, 'local_ins_9102_admin' staff_username, 'MIQA9102ROOT' org_code
    UNION ALL SELECT 'a', 2, 'local_ins_9102_admin', 'MIQA9102ROOT'
    UNION ALL SELECT 'a', 3, 'local_ins_9102_admin', 'MIQA9102ROOT'
    UNION ALL SELECT 'a', 4, 'local_ins_9102_admin', 'MIQA9102ROOT'
    UNION ALL SELECT 'a', 5, 'local_ins_9102_admin', 'MIQA9102ROOT'
    UNION ALL SELECT 'a', 6, 'local_ins_9102_admin', 'MIQA9102ROOT'
    UNION ALL SELECT 'b', 1, 'local_ins_9102_admin', 'MIQA9102ROOT'
    UNION ALL SELECT 'b', 2, 'local_ins_9102_admin', 'MIQA9102ROOT'
    UNION ALL SELECT 'b', 3, 'local_ins_9102_admin', 'MIQA9102ROOT'
    UNION ALL SELECT 'b', 4, 'local_ins_9102_admin', 'MIQA9102ROOT'
    UNION ALL SELECT 'b', 5, 'local_ins_9102_admin', 'MIQA9102ROOT'
    UNION ALL SELECT 'b', 6, 'local_ins_9102_admin', 'MIQA9102ROOT'
    UNION ALL SELECT 'c', 1, 'local_ins_9102_admin', 'MIQA9102ROOT'
    UNION ALL SELECT 'c', 2, 'local_ins_9102_admin', 'MIQA9102ROOT'
    UNION ALL SELECT 'c', 3, 'local_ins_9102_admin', 'MIQA9102ROOT'
    UNION ALL SELECT 'c', 4, 'local_ins_9102_admin', 'MIQA9102ROOT'
    UNION ALL SELECT 'c', 5, 'local_ins_9102_admin', 'MIQA9102ROOT'
    UNION ALL SELECT 'c', 6, 'local_ins_9102_admin', 'MIQA9102ROOT'
    UNION ALL SELECT 'd', 1, 'local_ins_9102_admin', 'MIQA9102ROOT'
    UNION ALL SELECT 'd', 2, 'local_ins_9102_admin', 'MIQA9102ROOT'
    UNION ALL SELECT 'd', 3, 'local_ins_9102_admin', 'MIQA9102ROOT'
    UNION ALL SELECT 'd', 4, 'local_ins_9102_admin', 'MIQA9102ROOT'
    UNION ALL SELECT 'd', 5, 'local_ins_9102_admin', 'MIQA9102ROOT'
    UNION ALL SELECT 'd', 6, 'local_ins_9102_admin', 'MIQA9102ROOT'
    UNION ALL SELECT 'e', 1, 'local_ins_9102_admin', 'MIQA9102ROOT'
    UNION ALL SELECT 'e', 2, 'local_ins_9102_admin', 'MIQA9102ROOT'
    UNION ALL SELECT 'e', 3, 'local_ins_9102_admin', 'MIQA9102ROOT'
    UNION ALL SELECT 'e', 4, 'local_ins_9102_admin', 'MIQA9102ROOT'
    UNION ALL SELECT 'e', 5, 'local_ins_9102_admin', 'MIQA9102ROOT'
    UNION ALL SELECT 'e', 6, 'local_ins_9102_admin', 'MIQA9102ROOT'
    UNION ALL SELECT 'a', 1, 'local_ins_9102_mgr_health', 'MIQA9102HEALTH'
    UNION ALL SELECT 'a', 2, 'local_ins_9102_mgr_health', 'MIQA9102HEALTH'
    UNION ALL SELECT 'a', 3, 'local_ins_9102_mgr_health', 'MIQA9102HEALTH'
    UNION ALL SELECT 'a', 4, 'local_ins_9102_mgr_health', 'MIQA9102HEALTH'
    UNION ALL SELECT 'a', 5, 'local_ins_9102_mgr_health', 'MIQA9102HEALTH'
    UNION ALL SELECT 'a', 6, 'local_ins_9102_mgr_health', 'MIQA9102HEALTH'
    UNION ALL SELECT 'b', 1, 'local_ins_9102_mgr_health', 'MIQA9102HEALTH'
    UNION ALL SELECT 'b', 2, 'local_ins_9102_mgr_health', 'MIQA9102HEALTH'
    UNION ALL SELECT 'b', 3, 'local_ins_9102_mgr_health', 'MIQA9102HEALTH'
    UNION ALL SELECT 'b', 4, 'local_ins_9102_mgr_health', 'MIQA9102HEALTH'
    UNION ALL SELECT 'b', 5, 'local_ins_9102_mgr_health', 'MIQA9102HEALTH'
    UNION ALL SELECT 'b', 6, 'local_ins_9102_mgr_health', 'MIQA9102HEALTH'
    UNION ALL SELECT 'c', 1, 'local_ins_9102_operator', 'MIQA9102HEALTH'
    UNION ALL SELECT 'c', 2, 'local_ins_9102_operator', 'MIQA9102HEALTH'
    UNION ALL SELECT 'c', 3, 'local_ins_9102_operator', 'MIQA9102HEALTH'
    UNION ALL SELECT 'c', 4, 'local_ins_9102_operator', 'MIQA9102HEALTH'
    UNION ALL SELECT 'c', 5, 'local_ins_9102_operator', 'MIQA9102HEALTH'
    UNION ALL SELECT 'c', 6, 'local_ins_9102_operator', 'MIQA9102HEALTH'
    UNION ALL SELECT 'd', 1, 'local_ins_9102_operator', 'MIQA9102HEALTH'
    UNION ALL SELECT 'd', 2, 'local_ins_9102_operator', 'MIQA9102HEALTH'
    UNION ALL SELECT 'd', 3, 'local_ins_9102_operator', 'MIQA9102HEALTH'
    UNION ALL SELECT 'd', 4, 'local_ins_9102_operator', 'MIQA9102HEALTH'
    UNION ALL SELECT 'd', 5, 'local_ins_9102_operator', 'MIQA9102HEALTH'
    UNION ALL SELECT 'd', 6, 'local_ins_9102_operator', 'MIQA9102HEALTH'
    UNION ALL SELECT 'd', 1, 'local_ins_9102_mgr_risk', 'MIQA9102RISK'
    UNION ALL SELECT 'd', 2, 'local_ins_9102_mgr_risk', 'MIQA9102RISK'
    UNION ALL SELECT 'd', 3, 'local_ins_9102_mgr_risk', 'MIQA9102RISK'
    UNION ALL SELECT 'd', 4, 'local_ins_9102_mgr_risk', 'MIQA9102RISK'
    UNION ALL SELECT 'd', 5, 'local_ins_9102_mgr_risk', 'MIQA9102RISK'
    UNION ALL SELECT 'd', 6, 'local_ins_9102_mgr_risk', 'MIQA9102RISK'
    UNION ALL SELECT 'e', 1, 'local_ins_9102_analyst', 'MIQA9102RISK'
    UNION ALL SELECT 'e', 2, 'local_ins_9102_analyst', 'MIQA9102RISK'
    UNION ALL SELECT 'e', 3, 'local_ins_9102_analyst', 'MIQA9102RISK'
    UNION ALL SELECT 'e', 4, 'local_ins_9102_analyst', 'MIQA9102RISK'
    UNION ALL SELECT 'e', 5, 'local_ins_9102_analyst', 'MIQA9102RISK'
    UNION ALL SELECT 'e', 6, 'local_ins_9102_analyst', 'MIQA9102RISK'
) u
JOIN sys_user staff
  ON staff.username = u.staff_username AND staff.status = 1 AND staff.del_flag = 0
JOIN sys_depart depart
  ON depart.org_code = u.org_code AND depart.tenant_id = @tenant
 AND depart.status = '1' AND depart.del_flag = '0'
ON DUPLICATE KEY UPDATE
    manager_user_id = VALUES(manager_user_id), department_id = VALUES(department_id),
    subject_ref = VALUES(subject_ref), status = 'active', updated_at = VALUES(updated_at);

-- ============================================================================
-- 9. 风险特征向量 + 风险结果（51 组）
--    A 类：14 天前 medium -> 3 天前 high；B 类：14 天前 medium -> 3 天前 medium/low
--    C 类：14 天前 high -> 3 天前 medium；D 类：60 天前 high -> 5 天前 low
--    E04~E06：仅 1 条模拟风险（is_mock=1）；E01~E03：无风险数据
-- ============================================================================
INSERT INTO rehealth_cvd_feature_vector (
    id, user_id, request_id, feature_schema_version, feature_json,
    quality_json, payload_json, created_at
)
SELECT
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:feature:9102:', r.cat, r.idx, ':', r.point_no), 256)),
    LOWER(MD5(CONCAT('REHEALTH_QA_TD_V1:user:', r.username))),
    CONCAT('qa9102-risk-', r.cat, r.idx, '-', r.point_no),
    'rehealth-cvd-feature-v1',
    JSON_OBJECT('fixture', TRUE, 'source', 'REHEALTH_QA_TD_V1'),
    JSON_OBJECT('completeness', 1.0),
    JSON_OBJECT('fixture', TRUE),
    r.evaluated_at
FROM (
    SELECT 'qa9102app_a01' username, 'a' cat, 1 idx, 1 point_no, DATE_SUB(@seed_time, INTERVAL 14 DAY) evaluated_at, 0.42 score, 'medium' level
    UNION ALL SELECT 'qa9102app_a01', 'a', 1, 2, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.62, 'high'
    UNION ALL SELECT 'qa9102app_a02', 'a', 2, 1, DATE_SUB(@seed_time, INTERVAL 14 DAY), 0.45, 'medium'
    UNION ALL SELECT 'qa9102app_a02', 'a', 2, 2, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.66, 'high'
    UNION ALL SELECT 'qa9102app_a03', 'a', 3, 1, DATE_SUB(@seed_time, INTERVAL 14 DAY), 0.40, 'medium'
    UNION ALL SELECT 'qa9102app_a03', 'a', 3, 2, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.61, 'high'
    UNION ALL SELECT 'qa9102app_a04', 'a', 4, 1, DATE_SUB(@seed_time, INTERVAL 14 DAY), 0.44, 'medium'
    UNION ALL SELECT 'qa9102app_a04', 'a', 4, 2, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.64, 'high'
    UNION ALL SELECT 'qa9102app_a05', 'a', 5, 1, DATE_SUB(@seed_time, INTERVAL 14 DAY), 0.48, 'medium'
    UNION ALL SELECT 'qa9102app_a05', 'a', 5, 2, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.70, 'high'
    UNION ALL SELECT 'qa9102app_a06', 'a', 6, 1, DATE_SUB(@seed_time, INTERVAL 14 DAY), 0.43, 'medium'
    UNION ALL SELECT 'qa9102app_a06', 'a', 6, 2, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.60, 'high'
    UNION ALL SELECT 'qa9102app_b01', 'b', 1, 1, DATE_SUB(@seed_time, INTERVAL 14 DAY), 0.40, 'medium'
    UNION ALL SELECT 'qa9102app_b01', 'b', 1, 2, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.34, 'low'
    UNION ALL SELECT 'qa9102app_b02', 'b', 2, 1, DATE_SUB(@seed_time, INTERVAL 14 DAY), 0.38, 'medium'
    UNION ALL SELECT 'qa9102app_b02', 'b', 2, 2, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.31, 'low'
    UNION ALL SELECT 'qa9102app_b03', 'b', 3, 1, DATE_SUB(@seed_time, INTERVAL 14 DAY), 0.42, 'medium'
    UNION ALL SELECT 'qa9102app_b03', 'b', 3, 2, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.38, 'medium'
    UNION ALL SELECT 'qa9102app_b04', 'b', 4, 1, DATE_SUB(@seed_time, INTERVAL 14 DAY), 0.39, 'medium'
    UNION ALL SELECT 'qa9102app_b04', 'b', 4, 2, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.33, 'low'
    UNION ALL SELECT 'qa9102app_b05', 'b', 5, 1, DATE_SUB(@seed_time, INTERVAL 14 DAY), 0.46, 'medium'
    UNION ALL SELECT 'qa9102app_b05', 'b', 5, 2, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.42, 'medium'
    UNION ALL SELECT 'qa9102app_b06', 'b', 6, 1, DATE_SUB(@seed_time, INTERVAL 14 DAY), 0.37, 'medium'
    UNION ALL SELECT 'qa9102app_b06', 'b', 6, 2, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.30, 'low'
    UNION ALL SELECT 'qa9102app_c01', 'c', 1, 1, DATE_SUB(@seed_time, INTERVAL 14 DAY), 0.58, 'high'
    UNION ALL SELECT 'qa9102app_c01', 'c', 1, 2, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.45, 'medium'
    UNION ALL SELECT 'qa9102app_c02', 'c', 2, 1, DATE_SUB(@seed_time, INTERVAL 14 DAY), 0.63, 'high'
    UNION ALL SELECT 'qa9102app_c02', 'c', 2, 2, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.52, 'medium'
    UNION ALL SELECT 'qa9102app_c03', 'c', 3, 1, DATE_SUB(@seed_time, INTERVAL 14 DAY), 0.56, 'high'
    UNION ALL SELECT 'qa9102app_c03', 'c', 3, 2, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.40, 'medium'
    UNION ALL SELECT 'qa9102app_c04', 'c', 4, 1, DATE_SUB(@seed_time, INTERVAL 14 DAY), 0.66, 'high'
    UNION ALL SELECT 'qa9102app_c04', 'c', 4, 2, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.55, 'medium'
    UNION ALL SELECT 'qa9102app_c05', 'c', 5, 1, DATE_SUB(@seed_time, INTERVAL 14 DAY), 0.60, 'high'
    UNION ALL SELECT 'qa9102app_c05', 'c', 5, 2, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.46, 'medium'
    UNION ALL SELECT 'qa9102app_c06', 'c', 6, 1, DATE_SUB(@seed_time, INTERVAL 14 DAY), 0.54, 'medium'
    UNION ALL SELECT 'qa9102app_c06', 'c', 6, 2, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.38, 'medium'
    UNION ALL SELECT 'qa9102app_d01', 'd', 1, 1, DATE_SUB(@seed_time, INTERVAL 60 DAY), 0.61, 'high'
    UNION ALL SELECT 'qa9102app_d01', 'd', 1, 2, DATE_SUB(@seed_time, INTERVAL 5 DAY), 0.22, 'low'
    UNION ALL SELECT 'qa9102app_d02', 'd', 2, 1, DATE_SUB(@seed_time, INTERVAL 60 DAY), 0.67, 'high'
    UNION ALL SELECT 'qa9102app_d02', 'd', 2, 2, DATE_SUB(@seed_time, INTERVAL 5 DAY), 0.25, 'low'
    UNION ALL SELECT 'qa9102app_d03', 'd', 3, 1, DATE_SUB(@seed_time, INTERVAL 60 DAY), 0.58, 'high'
    UNION ALL SELECT 'qa9102app_d03', 'd', 3, 2, DATE_SUB(@seed_time, INTERVAL 5 DAY), 0.21, 'low'
    UNION ALL SELECT 'qa9102app_d04', 'd', 4, 1, DATE_SUB(@seed_time, INTERVAL 60 DAY), 0.64, 'high'
    UNION ALL SELECT 'qa9102app_d04', 'd', 4, 2, DATE_SUB(@seed_time, INTERVAL 5 DAY), 0.24, 'low'
    UNION ALL SELECT 'qa9102app_d05', 'd', 5, 1, DATE_SUB(@seed_time, INTERVAL 60 DAY), 0.56, 'high'
    UNION ALL SELECT 'qa9102app_d05', 'd', 5, 2, DATE_SUB(@seed_time, INTERVAL 5 DAY), 0.19, 'low'
    UNION ALL SELECT 'qa9102app_d06', 'd', 6, 1, DATE_SUB(@seed_time, INTERVAL 60 DAY), 0.62, 'high'
    UNION ALL SELECT 'qa9102app_d06', 'd', 6, 2, DATE_SUB(@seed_time, INTERVAL 5 DAY), 0.23, 'low'
    UNION ALL SELECT 'qa9102app_e04', 'e', 4, 1, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.68, 'high'
    UNION ALL SELECT 'qa9102app_e05', 'e', 5, 1, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.71, 'high'
    UNION ALL SELECT 'qa9102app_e06', 'e', 6, 1, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.74, 'high'
) r
ON DUPLICATE KEY UPDATE
    feature_json = VALUES(feature_json), payload_json = VALUES(payload_json),
    created_at = VALUES(created_at);

INSERT INTO rehealth_cvd_risk_result (
    id, feature_vector_id, user_id, request_id, feature_schema_version,
    model_version, scorer_mode, is_mock, artifact_name, fallback_reason,
    contribution_method, risk_score, risk_level, contribution_json,
    missing_fields_json, quality_warnings_json, summary, response_json,
    evaluated_at, created_at
)
SELECT
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:risk:9102:', r.cat, r.idx, ':', r.point_no), 256)),
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:feature:9102:', r.cat, r.idx, ':', r.point_no), 256)),
    LOWER(MD5(CONCAT('REHEALTH_QA_TD_V1:user:', r.username))),
    CONCAT('qa9102-risk-', r.cat, r.idx, '-', r.point_no),
    'rehealth-cvd-feature-v1', 'rehealth-cvd-v3.1.0',
    'local_qa_fixture', r.is_mock,
    CASE WHEN r.is_mock = 1 THEN 'REHEALTH_QA_TD_V1_MOCK_NOT_A_MODEL' ELSE NULL END,
    NULL, 'shap',
    r.score, r.level,
    JSON_OBJECT('fixture', TRUE),
    NULL, NULL,
    CONCAT('合成风险快照（', UPPER(r.cat), r.idx, '-', r.point_no, '），仅供测试'),
    JSON_OBJECT(
        'source', 'REHEALTH_QA_TD_V1',
        'synthetic', TRUE,
        'factor_contributions', JSON_OBJECT(
            'sbp_mean', ROUND(0.18 + r.score * 0.2, 4),
            'ldl_c', ROUND(0.12 + r.score * 0.18, 4),
            'bmi', ROUND(0.10 + r.score * 0.15, 4)
        ),
        'factor_measured_components', JSON_OBJECT(
            'sbp_mean', ROUND(118 + r.score * 45, 1),
            'ldl_c', ROUND(2.6 + r.score * 2.2, 2),
            'bmi', ROUND(23.0 + r.score * 6.5, 2)
        )
    ),
    r.evaluated_at, r.evaluated_at
FROM (
    SELECT 'qa9102app_a01' username, 'a' cat, 1 idx, 1 point_no, DATE_SUB(@seed_time, INTERVAL 14 DAY) evaluated_at, 0.42 score, 'medium' level, 0 is_mock
    UNION ALL SELECT 'qa9102app_a01', 'a', 1, 2, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.62, 'high', 0
    UNION ALL SELECT 'qa9102app_a02', 'a', 2, 1, DATE_SUB(@seed_time, INTERVAL 14 DAY), 0.45, 'medium', 0
    UNION ALL SELECT 'qa9102app_a02', 'a', 2, 2, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.66, 'high', 0
    UNION ALL SELECT 'qa9102app_a03', 'a', 3, 1, DATE_SUB(@seed_time, INTERVAL 14 DAY), 0.40, 'medium', 0
    UNION ALL SELECT 'qa9102app_a03', 'a', 3, 2, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.61, 'high', 0
    UNION ALL SELECT 'qa9102app_a04', 'a', 4, 1, DATE_SUB(@seed_time, INTERVAL 14 DAY), 0.44, 'medium', 0
    UNION ALL SELECT 'qa9102app_a04', 'a', 4, 2, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.64, 'high', 0
    UNION ALL SELECT 'qa9102app_a05', 'a', 5, 1, DATE_SUB(@seed_time, INTERVAL 14 DAY), 0.48, 'medium', 0
    UNION ALL SELECT 'qa9102app_a05', 'a', 5, 2, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.70, 'high', 0
    UNION ALL SELECT 'qa9102app_a06', 'a', 6, 1, DATE_SUB(@seed_time, INTERVAL 14 DAY), 0.43, 'medium', 0
    UNION ALL SELECT 'qa9102app_a06', 'a', 6, 2, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.60, 'high', 0
    UNION ALL SELECT 'qa9102app_b01', 'b', 1, 1, DATE_SUB(@seed_time, INTERVAL 14 DAY), 0.40, 'medium', 0
    UNION ALL SELECT 'qa9102app_b01', 'b', 1, 2, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.34, 'low', 0
    UNION ALL SELECT 'qa9102app_b02', 'b', 2, 1, DATE_SUB(@seed_time, INTERVAL 14 DAY), 0.38, 'medium', 0
    UNION ALL SELECT 'qa9102app_b02', 'b', 2, 2, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.31, 'low', 0
    UNION ALL SELECT 'qa9102app_b03', 'b', 3, 1, DATE_SUB(@seed_time, INTERVAL 14 DAY), 0.42, 'medium', 0
    UNION ALL SELECT 'qa9102app_b03', 'b', 3, 2, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.38, 'medium', 0
    UNION ALL SELECT 'qa9102app_b04', 'b', 4, 1, DATE_SUB(@seed_time, INTERVAL 14 DAY), 0.39, 'medium', 0
    UNION ALL SELECT 'qa9102app_b04', 'b', 4, 2, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.33, 'low', 0
    UNION ALL SELECT 'qa9102app_b05', 'b', 5, 1, DATE_SUB(@seed_time, INTERVAL 14 DAY), 0.46, 'medium', 0
    UNION ALL SELECT 'qa9102app_b05', 'b', 5, 2, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.42, 'medium', 0
    UNION ALL SELECT 'qa9102app_b06', 'b', 6, 1, DATE_SUB(@seed_time, INTERVAL 14 DAY), 0.37, 'medium', 0
    UNION ALL SELECT 'qa9102app_b06', 'b', 6, 2, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.30, 'low', 0
    UNION ALL SELECT 'qa9102app_c01', 'c', 1, 1, DATE_SUB(@seed_time, INTERVAL 14 DAY), 0.58, 'high', 0
    UNION ALL SELECT 'qa9102app_c01', 'c', 1, 2, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.45, 'medium', 0
    UNION ALL SELECT 'qa9102app_c02', 'c', 2, 1, DATE_SUB(@seed_time, INTERVAL 14 DAY), 0.63, 'high', 0
    UNION ALL SELECT 'qa9102app_c02', 'c', 2, 2, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.52, 'medium', 0
    UNION ALL SELECT 'qa9102app_c03', 'c', 3, 1, DATE_SUB(@seed_time, INTERVAL 14 DAY), 0.56, 'high', 0
    UNION ALL SELECT 'qa9102app_c03', 'c', 3, 2, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.40, 'medium', 0
    UNION ALL SELECT 'qa9102app_c04', 'c', 4, 1, DATE_SUB(@seed_time, INTERVAL 14 DAY), 0.66, 'high', 0
    UNION ALL SELECT 'qa9102app_c04', 'c', 4, 2, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.55, 'medium', 0
    UNION ALL SELECT 'qa9102app_c05', 'c', 5, 1, DATE_SUB(@seed_time, INTERVAL 14 DAY), 0.60, 'high', 0
    UNION ALL SELECT 'qa9102app_c05', 'c', 5, 2, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.46, 'medium', 0
    UNION ALL SELECT 'qa9102app_c06', 'c', 6, 1, DATE_SUB(@seed_time, INTERVAL 14 DAY), 0.54, 'medium', 0
    UNION ALL SELECT 'qa9102app_c06', 'c', 6, 2, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.38, 'medium', 0
    UNION ALL SELECT 'qa9102app_d01', 'd', 1, 1, DATE_SUB(@seed_time, INTERVAL 60 DAY), 0.61, 'high', 0
    UNION ALL SELECT 'qa9102app_d01', 'd', 1, 2, DATE_SUB(@seed_time, INTERVAL 5 DAY), 0.22, 'low', 0
    UNION ALL SELECT 'qa9102app_d02', 'd', 2, 1, DATE_SUB(@seed_time, INTERVAL 60 DAY), 0.67, 'high', 0
    UNION ALL SELECT 'qa9102app_d02', 'd', 2, 2, DATE_SUB(@seed_time, INTERVAL 5 DAY), 0.25, 'low', 0
    UNION ALL SELECT 'qa9102app_d03', 'd', 3, 1, DATE_SUB(@seed_time, INTERVAL 60 DAY), 0.58, 'high', 0
    UNION ALL SELECT 'qa9102app_d03', 'd', 3, 2, DATE_SUB(@seed_time, INTERVAL 5 DAY), 0.21, 'low', 0
    UNION ALL SELECT 'qa9102app_d04', 'd', 4, 1, DATE_SUB(@seed_time, INTERVAL 60 DAY), 0.64, 'high', 0
    UNION ALL SELECT 'qa9102app_d04', 'd', 4, 2, DATE_SUB(@seed_time, INTERVAL 5 DAY), 0.24, 'low', 0
    UNION ALL SELECT 'qa9102app_d05', 'd', 5, 1, DATE_SUB(@seed_time, INTERVAL 60 DAY), 0.56, 'high', 0
    UNION ALL SELECT 'qa9102app_d05', 'd', 5, 2, DATE_SUB(@seed_time, INTERVAL 5 DAY), 0.19, 'low', 0
    UNION ALL SELECT 'qa9102app_d06', 'd', 6, 1, DATE_SUB(@seed_time, INTERVAL 60 DAY), 0.62, 'high', 0
    UNION ALL SELECT 'qa9102app_d06', 'd', 6, 2, DATE_SUB(@seed_time, INTERVAL 5 DAY), 0.23, 'low', 0
    UNION ALL SELECT 'qa9102app_e04', 'e', 4, 1, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.68, 'high', 1
    UNION ALL SELECT 'qa9102app_e05', 'e', 5, 1, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.71, 'high', 1
    UNION ALL SELECT 'qa9102app_e06', 'e', 6, 1, DATE_SUB(@seed_time, INTERVAL 3 DAY), 0.74, 'high', 1
) r
ON DUPLICATE KEY UPDATE
    feature_vector_id = VALUES(feature_vector_id), user_id = VALUES(user_id),
    risk_score = VALUES(risk_score), risk_level = VALUES(risk_level),
    is_mock = VALUES(is_mock), artifact_name = VALUES(artifact_name),
    response_json = VALUES(response_json), evaluated_at = VALUES(evaluated_at);

-- ============================================================================
-- 10. RHI 日快照（A/B/C/D 类每人 2 条：3 天前 + 1 天前；E 类无）
-- ============================================================================
INSERT INTO rehealth_rhi_daily_snapshot (
    id, user_id, scored_on, raw_score, display_score, data_confidence,
    status, product_tier, available_days, available_feature_count,
    smoothing_alpha, algorithm_version, calculation_source,
    domains_json, features_json, quality_json, created_at, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:rhi:9102:', r.cat, r.idx, ':', r.day_no), 256)),
    LOWER(MD5(CONCAT('REHEALTH_QA_TD_V1:user:', r.username))),
    DATE_SUB(@anchor_date, INTERVAL (4 - r.day_no * 2) DAY),
    r.score, r.score, 0.82, 'ready', 'standard', 7, 10,
    0.300000, 'rhi-daily-v1', 'rehealth_app',
    JSON_OBJECT('domains', JSON_ARRAY('vitality', 'metabolic', 'sleep')),
    JSON_OBJECT('features', JSON_ARRAY('heart_rate', 'steps', 'sleep_minutes')),
    NULL, @seed_time, @seed_time
FROM (
    SELECT 'qa9102app_a01' username, 'a' cat, 1 idx, 1 day_no, 62.0 score
    UNION ALL SELECT 'qa9102app_a01', 'a', 1, 2, 60.0
    UNION ALL SELECT 'qa9102app_a02', 'a', 2, 1, 58.0
    UNION ALL SELECT 'qa9102app_a02', 'a', 2, 2, 56.0
    UNION ALL SELECT 'qa9102app_a03', 'a', 3, 1, 64.0
    UNION ALL SELECT 'qa9102app_a03', 'a', 3, 2, 61.0
    UNION ALL SELECT 'qa9102app_a04', 'a', 4, 1, 55.0
    UNION ALL SELECT 'qa9102app_a04', 'a', 4, 2, 53.0
    UNION ALL SELECT 'qa9102app_a05', 'a', 5, 1, 59.0
    UNION ALL SELECT 'qa9102app_a05', 'a', 5, 2, 57.0
    UNION ALL SELECT 'qa9102app_a06', 'a', 6, 1, 61.0
    UNION ALL SELECT 'qa9102app_a06', 'a', 6, 2, 58.0
    UNION ALL SELECT 'qa9102app_b01', 'b', 1, 1, 74.0
    UNION ALL SELECT 'qa9102app_b01', 'b', 1, 2, 75.0
    UNION ALL SELECT 'qa9102app_b02', 'b', 2, 1, 78.0
    UNION ALL SELECT 'qa9102app_b02', 'b', 2, 2, 79.0
    UNION ALL SELECT 'qa9102app_b03', 'b', 3, 1, 71.0
    UNION ALL SELECT 'qa9102app_b03', 'b', 3, 2, 73.0
    UNION ALL SELECT 'qa9102app_b04', 'b', 4, 1, 77.0
    UNION ALL SELECT 'qa9102app_b04', 'b', 4, 2, 78.0
    UNION ALL SELECT 'qa9102app_b05', 'b', 5, 1, 69.0
    UNION ALL SELECT 'qa9102app_b05', 'b', 5, 2, 71.0
    UNION ALL SELECT 'qa9102app_b06', 'b', 6, 1, 80.0
    UNION ALL SELECT 'qa9102app_b06', 'b', 6, 2, 82.0
    UNION ALL SELECT 'qa9102app_c01', 'c', 1, 1, 66.0
    UNION ALL SELECT 'qa9102app_c01', 'c', 1, 2, 68.0
    UNION ALL SELECT 'qa9102app_c02', 'c', 2, 1, 60.0
    UNION ALL SELECT 'qa9102app_c02', 'c', 2, 2, 62.0
    UNION ALL SELECT 'qa9102app_c03', 'c', 3, 1, 75.0
    UNION ALL SELECT 'qa9102app_c03', 'c', 3, 2, 77.0
    UNION ALL SELECT 'qa9102app_c04', 'c', 4, 1, 54.0
    UNION ALL SELECT 'qa9102app_c04', 'c', 4, 2, 56.0
    UNION ALL SELECT 'qa9102app_c05', 'c', 5, 1, 68.0
    UNION ALL SELECT 'qa9102app_c05', 'c', 5, 2, 70.0
    UNION ALL SELECT 'qa9102app_c06', 'c', 6, 1, 79.0
    UNION ALL SELECT 'qa9102app_c06', 'c', 6, 2, 81.0
    UNION ALL SELECT 'qa9102app_d01', 'd', 1, 1, 82.0
    UNION ALL SELECT 'qa9102app_d01', 'd', 1, 2, 85.0
    UNION ALL SELECT 'qa9102app_d02', 'd', 2, 1, 78.0
    UNION ALL SELECT 'qa9102app_d02', 'd', 2, 2, 81.0
    UNION ALL SELECT 'qa9102app_d03', 'd', 3, 1, 84.0
    UNION ALL SELECT 'qa9102app_d03', 'd', 3, 2, 87.0
    UNION ALL SELECT 'qa9102app_d04', 'd', 4, 1, 76.0
    UNION ALL SELECT 'qa9102app_d04', 'd', 4, 2, 79.0
    UNION ALL SELECT 'qa9102app_d05', 'd', 5, 1, 83.0
    UNION ALL SELECT 'qa9102app_d05', 'd', 5, 2, 86.0
    UNION ALL SELECT 'qa9102app_d06', 'd', 6, 1, 80.0
    UNION ALL SELECT 'qa9102app_d06', 'd', 6, 2, 83.0
) r
ON DUPLICATE KEY UPDATE
    raw_score = VALUES(raw_score), display_score = VALUES(display_score),
    data_confidence = VALUES(data_confidence), status = 'ready',
    domains_json = VALUES(domains_json), features_json = VALUES(features_json),
    updated_at = VALUES(updated_at);

-- ============================================================================
-- 11. RDI 日快照 + 贡献项（A/B/C/D 类每人 1 条真实快照；
--     E04~E06 每人 1 条模拟快照 is_mock=1；E01~E03 无）
-- ============================================================================
INSERT INTO rehealth_rdi_daily_snapshot (
    id, user_id, scored_on, raw_score, display_score, data_confidence,
    status, is_mock, algorithm_version, calculation_source, created_at, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:rdi:9102:', r.cat, r.idx, ':1'), 256)),
    LOWER(MD5(CONCAT('REHEALTH_QA_TD_V1:user:', r.username))),
    DATE_SUB(@anchor_date, INTERVAL 1 DAY),
    r.score, r.score, 0.80, IF(r.is_mock = 1, 'mock', 'ready'), r.is_mock,
    'rdi-daily-v1', 'rehealth_app', @seed_time, @seed_time
FROM (
    SELECT 'qa9102app_a01' username, 'a' cat, 1 idx, 55.0 score, 0 is_mock
    UNION ALL SELECT 'qa9102app_a02', 'a', 2, 52.0, 0
    UNION ALL SELECT 'qa9102app_a03', 'a', 3, 58.0, 0
    UNION ALL SELECT 'qa9102app_a04', 'a', 4, 49.0, 0
    UNION ALL SELECT 'qa9102app_a05', 'a', 5, 54.0, 0
    UNION ALL SELECT 'qa9102app_a06', 'a', 6, 56.0, 0
    UNION ALL SELECT 'qa9102app_b01', 'b', 1, 70.0, 0
    UNION ALL SELECT 'qa9102app_b02', 'b', 2, 74.0, 0
    UNION ALL SELECT 'qa9102app_b03', 'b', 3, 68.0, 0
    UNION ALL SELECT 'qa9102app_b04', 'b', 4, 73.0, 0
    UNION ALL SELECT 'qa9102app_b05', 'b', 5, 66.0, 0
    UNION ALL SELECT 'qa9102app_b06', 'b', 6, 76.0, 0
    UNION ALL SELECT 'qa9102app_c01', 'c', 1, 64.0, 0
    UNION ALL SELECT 'qa9102app_c02', 'c', 2, 58.0, 0
    UNION ALL SELECT 'qa9102app_c03', 'c', 3, 72.0, 0
    UNION ALL SELECT 'qa9102app_c04', 'c', 4, 51.0, 0
    UNION ALL SELECT 'qa9102app_c05', 'c', 5, 66.0, 0
    UNION ALL SELECT 'qa9102app_c06', 'c', 6, 75.0, 0
    UNION ALL SELECT 'qa9102app_d01', 'd', 1, 80.0, 0
    UNION ALL SELECT 'qa9102app_d02', 'd', 2, 76.0, 0
    UNION ALL SELECT 'qa9102app_d03', 'd', 3, 83.0, 0
    UNION ALL SELECT 'qa9102app_d04', 'd', 4, 74.0, 0
    UNION ALL SELECT 'qa9102app_d05', 'd', 5, 81.0, 0
    UNION ALL SELECT 'qa9102app_d06', 'd', 6, 78.0, 0
    UNION ALL SELECT 'qa9102app_e04', 'e', 4, 70.0, 1
    UNION ALL SELECT 'qa9102app_e05', 'e', 5, 72.0, 1
    UNION ALL SELECT 'qa9102app_e06', 'e', 6, 68.0, 1
) r
ON DUPLICATE KEY UPDATE
    raw_score = VALUES(raw_score), display_score = VALUES(display_score),
    data_confidence = VALUES(data_confidence), status = VALUES(status),
    is_mock = VALUES(is_mock), updated_at = VALUES(updated_at);

INSERT INTO rehealth_rdi_contribution (
    id, snapshot_id, factor_code, domain_code, source_code,
    current_value, baseline_value, unit, raw_points, confidence,
    final_points, source_factor_id, algorithm_version, created_at
)
SELECT
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:rdi-contribution:9102:', c.cat, c.idx, ':', c.factor_code), 256)),
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:rdi:9102:', c.cat, c.idx, ':1'), 256)),
    c.factor_code, c.domain_code, c.source_code,
    c.current_value, c.baseline_value, c.unit, c.raw_points, 0.80,
    c.final_points, CONCAT('qa9102-', c.cat, c.idx, '-', c.factor_code),
    'rdi-daily-v1', @seed_time
FROM (
    SELECT 'a' cat, 1 idx, 'steps' factor_code, 'activity' domain_code, 'steps_daily' source_code,
           4200.000000 current_value, 7800.000000 baseline_value, '步' unit,
           -6.000000 raw_points, -6.000000 final_points
    UNION ALL SELECT 'a', 1, 'sleep', 'sleep', 'sleep_duration', 6.2, 7.4, '小时', -4.0, -4.0
    UNION ALL SELECT 'a', 1, 'activity', 'activity', 'activity_minutes', 18.0, 35.0, '分钟', -5.0, -5.0
    UNION ALL SELECT 'a', 2, 'steps', 'activity', 'steps_daily', 4000.0, 7500.0, '步', -7.0, -7.0
    UNION ALL SELECT 'a', 2, 'sleep', 'sleep', 'sleep_duration', 6.0, 7.2, '小时', -5.0, -5.0
    UNION ALL SELECT 'a', 2, 'activity', 'activity', 'activity_minutes', 15.0, 32.0, '分钟', -6.0, -6.0
    UNION ALL SELECT 'a', 3, 'steps', 'activity', 'steps_daily', 4700.0, 8000.0, '步', -5.0, -5.0
    UNION ALL SELECT 'a', 3, 'sleep', 'sleep', 'sleep_duration', 6.5, 7.5, '小时', -3.0, -3.0
    UNION ALL SELECT 'a', 3, 'activity', 'activity', 'activity_minutes', 20.0, 38.0, '分钟', -4.0, -4.0
    UNION ALL SELECT 'a', 4, 'steps', 'activity', 'steps_daily', 3800.0, 7200.0, '步', -8.0, -8.0
    UNION ALL SELECT 'a', 4, 'sleep', 'sleep', 'sleep_duration', 5.8, 7.1, '小时', -6.0, -6.0
    UNION ALL SELECT 'a', 4, 'activity', 'activity', 'activity_minutes', 14.0, 30.0, '分钟', -7.0, -7.0
    UNION ALL SELECT 'a', 5, 'steps', 'activity', 'steps_daily', 4300.0, 7700.0, '步', -6.0, -6.0
    UNION ALL SELECT 'a', 5, 'sleep', 'sleep', 'sleep_duration', 6.1, 7.3, '小时', -4.0, -4.0
    UNION ALL SELECT 'a', 5, 'activity', 'activity', 'activity_minutes', 17.0, 34.0, '分钟', -5.0, -5.0
    UNION ALL SELECT 'a', 6, 'steps', 'activity', 'steps_daily', 4500.0, 7900.0, '步', -5.0, -5.0
    UNION ALL SELECT 'a', 6, 'sleep', 'sleep', 'sleep_duration', 6.4, 7.5, '小时', -3.0, -3.0
    UNION ALL SELECT 'a', 6, 'activity', 'activity', 'activity_minutes', 19.0, 36.0, '分钟', -4.0, -4.0
    UNION ALL SELECT 'b', 1, 'steps', 'activity', 'steps_daily', 6500.0, 7200.0, '步', -2.0, -2.0
    UNION ALL SELECT 'b', 1, 'sleep', 'sleep', 'sleep_duration', 6.9, 7.2, '小时', -1.0, -1.0
    UNION ALL SELECT 'b', 1, 'activity', 'activity', 'activity_minutes', 28.0, 32.0, '分钟', -1.5, -1.5
    UNION ALL SELECT 'b', 2, 'steps', 'activity', 'steps_daily', 7000.0, 7600.0, '步', -1.5, -1.5
    UNION ALL SELECT 'b', 2, 'sleep', 'sleep', 'sleep_duration', 7.0, 7.3, '小时', -1.0, -1.0
    UNION ALL SELECT 'b', 2, 'activity', 'activity', 'activity_minutes', 30.0, 34.0, '分钟', -1.0, -1.0
    UNION ALL SELECT 'b', 3, 'steps', 'activity', 'steps_daily', 6200.0, 7000.0, '步', -2.0, -2.0
    UNION ALL SELECT 'b', 3, 'sleep', 'sleep', 'sleep_duration', 6.8, 7.1, '小时', -1.0, -1.0
    UNION ALL SELECT 'b', 3, 'activity', 'activity', 'activity_minutes', 26.0, 31.0, '分钟', -1.5, -1.5
    UNION ALL SELECT 'b', 4, 'steps', 'activity', 'steps_daily', 6900.0, 7500.0, '步', -1.5, -1.5
    UNION ALL SELECT 'b', 4, 'sleep', 'sleep', 'sleep_duration', 7.1, 7.3, '小时', -0.5, -0.5
    UNION ALL SELECT 'b', 4, 'activity', 'activity', 'activity_minutes', 31.0, 35.0, '分钟', -1.0, -1.0
    UNION ALL SELECT 'b', 5, 'steps', 'activity', 'steps_daily', 5900.0, 6800.0, '步', -2.5, -2.5
    UNION ALL SELECT 'b', 5, 'sleep', 'sleep', 'sleep_duration', 6.6, 7.0, '小时', -1.5, -1.5
    UNION ALL SELECT 'b', 5, 'activity', 'activity', 'activity_minutes', 24.0, 30.0, '分钟', -2.0, -2.0
    UNION ALL SELECT 'b', 6, 'steps', 'activity', 'steps_daily', 7200.0, 7800.0, '步', -1.0, -1.0
    UNION ALL SELECT 'b', 6, 'sleep', 'sleep', 'sleep_duration', 7.2, 7.4, '小时', -0.5, -0.5
    UNION ALL SELECT 'b', 6, 'activity', 'activity', 'activity_minutes', 33.0, 36.0, '分钟', -1.0, -1.0
    UNION ALL SELECT 'c', 1, 'steps', 'activity', 'steps_daily', 5600.0, 6400.0, '步', 1.0, 1.0
    UNION ALL SELECT 'c', 1, 'sleep', 'sleep', 'sleep_duration', 6.7, 6.5, '小时', 0.5, 0.5
    UNION ALL SELECT 'c', 1, 'activity', 'activity', 'activity_minutes', 27.0, 25.0, '分钟', 0.5, 0.5
    UNION ALL SELECT 'c', 2, 'steps', 'activity', 'steps_daily', 5100.0, 5800.0, '步', 1.5, 1.5
    UNION ALL SELECT 'c', 2, 'sleep', 'sleep', 'sleep_duration', 6.4, 6.2, '小时', 0.5, 0.5
    UNION ALL SELECT 'c', 2, 'activity', 'activity', 'activity_minutes', 24.0, 22.0, '分钟', 0.5, 0.5
    UNION ALL SELECT 'c', 3, 'steps', 'activity', 'steps_daily', 6300.0, 6900.0, '步', 1.0, 1.0
    UNION ALL SELECT 'c', 3, 'sleep', 'sleep', 'sleep_duration', 7.0, 6.8, '小时', 0.5, 0.5
    UNION ALL SELECT 'c', 3, 'activity', 'activity', 'activity_minutes', 30.0, 28.0, '分钟', 0.5, 0.5
    UNION ALL SELECT 'c', 4, 'steps', 'activity', 'steps_daily', 4600.0, 5200.0, '步', 2.0, 2.0
    UNION ALL SELECT 'c', 4, 'sleep', 'sleep', 'sleep_duration', 6.1, 5.9, '小时', 1.0, 1.0
    UNION ALL SELECT 'c', 4, 'activity', 'activity', 'activity_minutes', 21.0, 19.0, '分钟', 1.0, 1.0
    UNION ALL SELECT 'c', 5, 'steps', 'activity', 'steps_daily', 5800.0, 6600.0, '步', 1.0, 1.0
    UNION ALL SELECT 'c', 5, 'sleep', 'sleep', 'sleep_duration', 6.8, 6.6, '小时', 0.5, 0.5
    UNION ALL SELECT 'c', 5, 'activity', 'activity', 'activity_minutes', 28.0, 26.0, '分钟', 0.5, 0.5
    UNION ALL SELECT 'c', 6, 'steps', 'activity', 'steps_daily', 6800.0, 7300.0, '步', 0.5, 0.5
    UNION ALL SELECT 'c', 6, 'sleep', 'sleep', 'sleep_duration', 7.3, 7.1, '小时', 0.5, 0.5
    UNION ALL SELECT 'c', 6, 'activity', 'activity', 'activity_minutes', 32.0, 30.0, '分钟', 0.5, 0.5
    UNION ALL SELECT 'd', 1, 'steps', 'activity', 'steps_daily', 8200.0, 5200.0, '步', 6.0, 6.0
    UNION ALL SELECT 'd', 1, 'sleep', 'sleep', 'sleep_duration', 7.6, 6.4, '小时', 4.0, 4.0
    UNION ALL SELECT 'd', 1, 'activity', 'activity', 'activity_minutes', 42.0, 22.0, '分钟', 5.0, 5.0
    UNION ALL SELECT 'd', 2, 'steps', 'activity', 'steps_daily', 7800.0, 4900.0, '步', 5.5, 5.5
    UNION ALL SELECT 'd', 2, 'sleep', 'sleep', 'sleep_duration', 7.4, 6.2, '小时', 3.5, 3.5
    UNION ALL SELECT 'd', 2, 'activity', 'activity', 'activity_minutes', 39.0, 20.0, '分钟', 4.5, 4.5
    UNION ALL SELECT 'd', 3, 'steps', 'activity', 'steps_daily', 8600.0, 5500.0, '步', 6.5, 6.5
    UNION ALL SELECT 'd', 3, 'sleep', 'sleep', 'sleep_duration', 7.8, 6.5, '小时', 4.5, 4.5
    UNION ALL SELECT 'd', 3, 'activity', 'activity', 'activity_minutes', 45.0, 23.0, '分钟', 5.5, 5.5
    UNION ALL SELECT 'd', 4, 'steps', 'activity', 'steps_daily', 7400.0, 4600.0, '步', 5.0, 5.0
    UNION ALL SELECT 'd', 4, 'sleep', 'sleep', 'sleep_duration', 7.3, 6.1, '小时', 3.5, 3.5
    UNION ALL SELECT 'd', 4, 'activity', 'activity', 'activity_minutes', 38.0, 19.0, '分钟', 4.5, 4.5
    UNION ALL SELECT 'd', 5, 'steps', 'activity', 'steps_daily', 8000.0, 6000.0, '步', 5.0, 5.0
    UNION ALL SELECT 'd', 5, 'sleep', 'sleep', 'sleep_duration', 7.5, 6.8, '小时', 3.0, 3.0
    UNION ALL SELECT 'd', 5, 'activity', 'activity', 'activity_minutes', 40.0, 26.0, '分钟', 4.0, 4.0
    UNION ALL SELECT 'd', 6, 'steps', 'activity', 'steps_daily', 7600.0, 5700.0, '步', 4.5, 4.5
    UNION ALL SELECT 'd', 6, 'sleep', 'sleep', 'sleep_duration', 7.4, 6.6, '小时', 3.0, 3.0
    UNION ALL SELECT 'd', 6, 'activity', 'activity', 'activity_minutes', 38.0, 24.0, '分钟', 3.5, 3.5
    UNION ALL SELECT 'e', 4, 'steps', 'activity', 'steps_daily', 5200.0, 5000.0, '步', 0.5, 0.5
    UNION ALL SELECT 'e', 4, 'sleep', 'sleep', 'sleep_duration', 6.5, 6.4, '小时', 0.5, 0.5
    UNION ALL SELECT 'e', 4, 'activity', 'activity', 'activity_minutes', 25.0, 24.0, '分钟', 0.5, 0.5
    UNION ALL SELECT 'e', 5, 'steps', 'activity', 'steps_daily', 5600.0, 5400.0, '步', 0.5, 0.5
    UNION ALL SELECT 'e', 5, 'sleep', 'sleep', 'sleep_duration', 6.7, 6.6, '小时', 0.5, 0.5
    UNION ALL SELECT 'e', 5, 'activity', 'activity', 'activity_minutes', 27.0, 26.0, '分钟', 0.5, 0.5
    UNION ALL SELECT 'e', 6, 'steps', 'activity', 'steps_daily', 4800.0, 4600.0, '步', 0.5, 0.5
    UNION ALL SELECT 'e', 6, 'sleep', 'sleep', 'sleep_duration', 6.2, 6.1, '小时', 0.5, 0.5
    UNION ALL SELECT 'e', 6, 'activity', 'activity', 'activity_minutes', 23.0, 22.0, '分钟', 0.5, 0.5
) c
ON DUPLICATE KEY UPDATE
    snapshot_id = VALUES(snapshot_id), current_value = VALUES(current_value),
    baseline_value = VALUES(baseline_value), raw_points = VALUES(raw_points),
    final_points = VALUES(final_points), created_at = VALUES(created_at);

-- ============================================================================
-- 12. 归因结果（7 条：D 类 6 条满足改善门槛；E06 1 条证据不足）
-- ============================================================================
INSERT INTO rehealth_attribution_result (
    id, user_id, status, model_version, request_id, attribution_mode,
    is_mock, provider, history_days, min_history_days, intervention_days,
    intervention_data_sufficient, current_risk_score, current_risk_level,
    current_trend, individual_att, trend_delta, adherence_average,
    interpretation, error_code, retryable, request_json, response_json, created_at
)
SELECT
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:attribution:9102:', r.cat, r.idx), 256)),
    LOWER(MD5(CONCAT('REHEALTH_QA_TD_V1:user:', r.username))),
    r.status, 'rehealth-cvd-v3.1.0', CONCAT('qa9102-attr-', r.cat, r.idx),
    'trend', r.is_mock, 'rehealth-attribution-v1',
    r.history_days, 14, r.intervention_days, r.data_sufficient,
    r.current_score, r.current_level, r.current_trend,
    r.individual_att, r.trend_delta, r.adherence_average,
    r.interpretation, NULL, 0,
    JSON_OBJECT('fixture', TRUE, 'source', 'REHEALTH_QA_TD_V1'),
    JSON_OBJECT('fixture', TRUE, 'conclusion', r.conclusion),
    DATE_SUB(@seed_time, INTERVAL 2 DAY)
FROM (
    SELECT 'qa9102app_d01' username, 'd' cat, 1 idx, 'completed' status, 0 is_mock,
           30 history_days, 21 intervention_days, 1 data_sufficient, 0.22 current_score,
           'low' current_level, 'improving' current_trend, NULL individual_att, -0.15 trend_delta,
           0.92 adherence_average, '阶段性改善：风险趋势下降且执行稳定（测试数据）' interpretation, 'improved' conclusion
    UNION ALL SELECT 'qa9102app_d02', 'd', 2, 'completed', 0, 30, 21, 1, 0.25, 'low',
           'improving', NULL, -0.13, 0.86, '阶段性改善：睡眠与运动执行改善（测试数据）', 'improved'
    UNION ALL SELECT 'qa9102app_d03', 'd', 3, 'completed', 0, 30, 21, 1, 0.21, 'low',
           'improving', -0.12, NULL, 0.95, '阶段性改善：个体归因效应为负（测试数据）', 'improved'
    UNION ALL SELECT 'qa9102app_d04', 'd', 4, 'completed', 0, 30, 21, 1, 0.24, 'low',
           'improving', NULL, -0.14, 0.84, '阶段性改善：风险趋势下降（测试数据）', 'improved'
    UNION ALL SELECT 'qa9102app_d05', 'd', 5, 'completed', 0, 30, 21, 1, 0.19, 'low',
           'improving', NULL, -0.17, 0.93, '阶段性改善：生活方式执行达标（测试数据）', 'improved'
    UNION ALL SELECT 'qa9102app_d06', 'd', 6, 'completed', 0, 30, 21, 1, 0.23, 'low',
           'improving', NULL, -0.12, 0.88, '阶段性改善：风险趋势下降（测试数据）', 'improved'
    UNION ALL SELECT 'qa9102app_e06', 'e', 6, 'insufficient_data', 0, 5, 0, 0, 0.74, 'high',
           NULL, NULL, NULL, NULL, '历史基线与干预执行均不足，暂无法评估改善（测试数据）', 'insufficient'
) r
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id), status = VALUES(status),
    history_days = VALUES(history_days), min_history_days = 14,
    intervention_days = VALUES(intervention_days),
    intervention_data_sufficient = VALUES(intervention_data_sufficient),
    current_risk_score = VALUES(current_risk_score),
    current_risk_level = VALUES(current_risk_level), current_trend = VALUES(current_trend),
    individual_att = VALUES(individual_att), trend_delta = VALUES(trend_delta),
    adherence_average = VALUES(adherence_average), interpretation = VALUES(interpretation),
    response_json = VALUES(response_json), created_at = VALUES(created_at);

-- ============================================================================
-- 13. 干预计划（C/D 类 12 条，legacy 计划详情页展示）
-- ============================================================================
INSERT INTO rehealth_intervention_plan (
    id, user_id, plan_id, source_request_id, feature_schema_version,
    model_version, scorer_mode, is_mock, artifact_name, priority_intervention,
    rationale, expected_impact, confidence, medical_disclaimer, generated_at,
    response_json, created_at
)
SELECT
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:plan:9102:', p.cat, p.idx), 256)),
    LOWER(MD5(CONCAT('REHEALTH_QA_TD_V1:user:', p.username))),
    CONCAT('qa9102-plan-', p.cat, p.idx),
    CONCAT('qa9102-plan-req-', p.cat, p.idx),
    'rehealth-cvd-feature-v1', 'rehealth-cvd-v3.1.0', 'local_qa_fixture',
    0, NULL,
    '优先控制血压与血脂，保持每周至少 150 分钟中等强度运动',
    '基于合成风险因子贡献排序生成（测试数据）',
    '预计 90 天风险评分下降 0.10~0.20（测试数据）',
    0.85,
    '本计划为测试合成数据，不构成医疗建议，请遵医嘱。',
    DATE_SUB(@seed_time, INTERVAL 85 DAY),
    JSON_OBJECT(
        'synthetic', TRUE, 'clinicalUseAllowed', FALSE,
        'items', JSON_ARRAY(
            JSON_OBJECT('title', '每日早晚监测并记录血压'),
            JSON_OBJECT('title', '每周 150 分钟中等强度有氧运动'),
            JSON_OBJECT('title', '保持每日睡眠 7 小时并记录')
        )
    ),
    DATE_SUB(@seed_time, INTERVAL 85 DAY)
FROM (
    SELECT 'qa9102app_c01' username, 'c' cat, 1 idx
    UNION ALL SELECT 'qa9102app_c02', 'c', 2
    UNION ALL SELECT 'qa9102app_c03', 'c', 3
    UNION ALL SELECT 'qa9102app_c04', 'c', 4
    UNION ALL SELECT 'qa9102app_c05', 'c', 5
    UNION ALL SELECT 'qa9102app_c06', 'c', 6
    UNION ALL SELECT 'qa9102app_d01', 'd', 1
    UNION ALL SELECT 'qa9102app_d02', 'd', 2
    UNION ALL SELECT 'qa9102app_d03', 'd', 3
    UNION ALL SELECT 'qa9102app_d04', 'd', 4
    UNION ALL SELECT 'qa9102app_d05', 'd', 5
    UNION ALL SELECT 'qa9102app_d06', 'd', 6
) p
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id), plan_id = VALUES(plan_id),
    priority_intervention = VALUES(priority_intervention),
    response_json = VALUES(response_json), created_at = VALUES(created_at);

-- ============================================================================
-- 14. 审计事件（6 条负责人分配）
-- ============================================================================
INSERT INTO rehealth_insurance_audit_event (
    id, tenant_id, actor_user_id, action, resource_type, resource_id,
    request_id, before_hash, after_hash, metadata_json, created_at
)
SELECT
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:audit:9102:', ev.seq), 256)),
    @tenant, actor.id, 'ASSIGN_RESPONSIBLE_STAFF', 'insurance_subject_manager',
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:assignment:9102:', ev.staff_username, ':', ev.cat, ev.idx), 256)),
    CONCAT('qa-td-audit-9102-', LPAD(ev.seq, 2, '0')),
    NULL,
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:after:assignment:9102:', ev.staff_username, ':', ev.cat, ev.idx), 256)),
    JSON_OBJECT('sourceSystem', 'REHEALTH_QA_TD_V1', 'synthetic', TRUE),
    @seed_time
FROM (
    SELECT 1 seq, 'local_ins_9102_mgr_health' staff_username, 'a' cat, 1 idx
    UNION ALL SELECT 2, 'local_ins_9102_operator', 'c', 1
    UNION ALL SELECT 3, 'local_ins_9102_operator', 'd', 1
    UNION ALL SELECT 4, 'local_ins_9102_mgr_risk', 'd', 6
    UNION ALL SELECT 5, 'local_ins_9102_analyst', 'e', 1
    UNION ALL SELECT 6, 'local_ins_9102_analyst', 'e', 6
) ev
JOIN sys_user actor ON actor.username = 'local_ins_9102_admin' AND actor.status = 1 AND actor.del_flag = 0
ON DUPLICATE KEY UPDATE
    actor_user_id = VALUES(actor_user_id), action = VALUES(action),
    resource_id = VALUES(resource_id), request_id = VALUES(request_id),
    after_hash = VALUES(after_hash), metadata_json = VALUES(metadata_json),
    created_at = VALUES(created_at);

-- ============================================================================
-- 15. 执行后核对
--     预期：sys_user 30 / profile 30 / subject 30 / policy 30 / coverage 30 /
--           consent 30 / binding 30 / intervention 24 / feedback 54 /
--           action 30 / subject_manager 66 / feature_vector 51 /
--           risk_result 51 / rhi 48 / rdi 27 / rdi_contribution 81 /
--           attribution 7 / intervention_plan 12 / audit 6
-- ============================================================================
SELECT 'sys_user(app)' tbl, COUNT(*) cnt FROM sys_user WHERE username LIKE 'qa9102app\_%'
UNION ALL SELECT 'patient_profile', COUNT(*) FROM rehealth_patient_profile p
    JOIN sys_user u ON u.id = p.user_id WHERE u.username LIKE 'qa9102app\_%'
UNION ALL SELECT 'subject', COUNT(*) FROM rehealth_insurance_subject WHERE tenant_id = @tenant AND source_system = 'REHEALTH_QA_TD_V1'
UNION ALL SELECT 'policy', COUNT(*) FROM rehealth_insurance_policy WHERE tenant_id = @tenant AND source_system = 'REHEALTH_QA_TD_V1'
UNION ALL SELECT 'coverage', COUNT(*) FROM rehealth_insurance_coverage WHERE tenant_id = @tenant AND source_system = 'REHEALTH_QA_TD_V1'
UNION ALL SELECT 'consent', COUNT(*) FROM rehealth_insurance_consent WHERE tenant_id = @tenant AND source_system = 'REHEALTH_QA_TD_V1'
UNION ALL SELECT 'binding', COUNT(*) FROM rehealth_insurance_plan_binding WHERE tenant_id = @tenant AND source_system = 'REHEALTH_QA_TD_V1'
UNION ALL SELECT 'intervention', COUNT(*) FROM rehealth_insurance_intervention WHERE tenant_id = @tenant AND source_system = 'REHEALTH_QA_TD_V1'
UNION ALL SELECT 'feedback', COUNT(*) FROM rehealth_insurance_intervention_feedback WHERE tenant_id = @tenant AND source_system = 'REHEALTH_QA_TD_V1'
UNION ALL SELECT 'action', COUNT(*) FROM rehealth_insurance_intervention_action WHERE tenant_id = @tenant AND request_id LIKE 'qa-td-action-9102-%'
UNION ALL SELECT 'subject_manager', COUNT(*) FROM rehealth_insurance_subject_manager WHERE tenant_id = @tenant AND source_system = 'REHEALTH_QA_TD_V1'
UNION ALL SELECT 'feature_vector', COUNT(*) FROM rehealth_cvd_feature_vector fv
    JOIN sys_user u ON u.id = fv.user_id WHERE u.username LIKE 'qa9102app\_%'
UNION ALL SELECT 'risk_result', COUNT(*) FROM rehealth_cvd_risk_result r
    JOIN sys_user u ON u.id = r.user_id WHERE u.username LIKE 'qa9102app\_%'
UNION ALL SELECT 'rhi_snapshot', COUNT(*) FROM rehealth_rhi_daily_snapshot r
    JOIN sys_user u ON u.id = r.user_id WHERE u.username LIKE 'qa9102app\_%'
UNION ALL SELECT 'rdi_snapshot', COUNT(*) FROM rehealth_rdi_daily_snapshot r
    JOIN sys_user u ON u.id = r.user_id WHERE u.username LIKE 'qa9102app\_%'
UNION ALL SELECT 'rdi_contribution', COUNT(*) FROM rehealth_rdi_contribution c
    JOIN rehealth_rdi_daily_snapshot s ON s.id = c.snapshot_id
    JOIN sys_user u ON u.id = s.user_id WHERE u.username LIKE 'qa9102app\_%'
UNION ALL SELECT 'attribution', COUNT(*) FROM rehealth_attribution_result r
    JOIN sys_user u ON u.id = r.user_id WHERE u.username LIKE 'qa9102app\_%'
UNION ALL SELECT 'intervention_plan', COUNT(*) FROM rehealth_intervention_plan p
    JOIN sys_user u ON u.id = p.user_id WHERE u.username LIKE 'qa9102app\_%'
UNION ALL SELECT 'audit', COUNT(*) FROM rehealth_insurance_audit_event WHERE tenant_id = @tenant AND request_id LIKE 'qa-td-audit-9102-%';

-- ============================================================================
-- 16. 工作台状态分布核对（复刻 InsuranceInterventionWorkbenchService.workflowStatus）
--     预期：A 类=pending_action(6)、B/E 类=pending_review(12)、
--           C 类=in_progress(6)、D 类=improved(6)
-- ============================================================================
SELECT workflow_status, COUNT(*) AS cnt
FROM (
    SELECT s.subject_ref,
           CASE
               WHEN MAX(CASE
                   WHEN at2.is_mock = 0 AND at2.intervention_data_sufficient = 1
                    AND at2.history_days >= 14 AND at2.intervention_days >= 7
                    AND (COALESCE(at2.individual_att, at2.trend_delta) < 0)
                   THEN 1 ELSE 0 END) = 1 THEN 'improved'
               WHEN MAX(CASE WHEN a.status IN ('pending', 'in_progress') THEN 1 ELSE 0 END) = 1
                 OR MAX(CASE WHEN i.status IN ('active', 'enrolled', 'in_progress') THEN 1 ELSE 0 END) = 1
                   THEN 'in_progress'
               WHEN MAX(rr.is_mock) = 0 AND MAX(rr.risk_level) = 'high' THEN 'pending_action'
               ELSE 'pending_review'
           END AS workflow_status
    FROM rehealth_insurance_subject s
    JOIN rehealth_insurance_subject_manager m
      ON m.tenant_id = s.tenant_id AND m.subject_ref = s.subject_ref AND m.status = 'active'
     AND m.manager_user_id = (SELECT id FROM sys_user WHERE username = 'local_ins_9102_admin' LIMIT 1)
    LEFT JOIN rehealth_cvd_risk_result rr
      ON rr.user_id = s.rehealth_user_id
     AND rr.evaluated_at = (SELECT MAX(x.evaluated_at) FROM rehealth_cvd_risk_result x WHERE x.user_id = rr.user_id)
    LEFT JOIN rehealth_insurance_intervention_action a
      ON a.tenant_id = s.tenant_id AND a.subject_ref = s.subject_ref
    LEFT JOIN rehealth_insurance_intervention i
      ON i.tenant_id = s.tenant_id AND i.subject_ref = s.subject_ref
    LEFT JOIN rehealth_attribution_result at2
      ON at2.user_id = s.rehealth_user_id
     AND at2.created_at = (SELECT MAX(y.created_at) FROM rehealth_attribution_result y WHERE y.user_id = at2.user_id)
    WHERE s.tenant_id = @tenant AND s.source_system = 'REHEALTH_QA_TD_V1'
      AND s.enrollment_status = 'active' AND s.consent_status = 'granted'
    GROUP BY s.subject_ref
) t
GROUP BY workflow_status
ORDER BY workflow_status;

-- 员工账号依赖提示（未执行 04_test_data.sql 的 9102 机构种子时，负责人行数会减少）
SELECT 'subject_manager 依赖 local_ins_9102_* 员工账号；若计数不足请先执行 04_test_data.sql 的多机构种子' AS notice
WHERE NOT EXISTS (SELECT 1 FROM sys_user WHERE username = 'local_ins_9102_admin' AND del_flag = 0);

-- ============================================================================

-- ============================================================================
-- 第 4 部分：9102 版本化关怀计划与依从性执行事实
-- ============================================================================

SET NAMES utf8mb4;
SET time_zone = '+00:00';
SET @seed_actor = 'REHEALTH_QA_TD_V1';
SET @seed_time  = TIMESTAMP('2026-08-19 09:00:00');
SET @anchor_date = DATE('2026-08-19');
SET @tenant = 9102;

-- ============================================================================
-- 1. 关怀计划主表（24 个计划：A/B/C/D 四类各 6 人）
-- ============================================================================
INSERT INTO rehealth_care_plan (
    id, tenant_id, owner_type, owner_org_ref, subject_ref, rehealth_user_id,
    source_plan_id, status, current_revision_id, draft_revision_id,
    lock_version, created_by, created_at, updated_by, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:care-plan:9102:', u.cat, u.idx), 256)),
    @tenant, 'insurance', '9102',
    CONCAT('QA9102-', UPPER(u.cat), LPAD(u.idx, 2, '0')),
    LOWER(MD5(CONCAT('REHEALTH_QA_TD_V1:user:', u.username))),
    CONCAT('qa9102-plan-', u.cat, u.idx),
    'active',
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:revision:9102:', u.cat, u.idx, ':', IF(u.cat = 'd', 2, 1)), 256)),
    CASE WHEN u.cat = 'c'
         THEN LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:revision:9102:c', u.idx, ':2'), 256))
         ELSE NULL END,
    2, creator.id, DATE_SUB(@seed_time, INTERVAL 60 DAY), creator.id, @seed_time
FROM (
    SELECT 'qa9102app_a01' username, 'a' cat, 1 idx, 'local_ins_9102_mgr_health' creator_username
    UNION ALL SELECT 'qa9102app_a02', 'a', 2, 'local_ins_9102_mgr_health'
    UNION ALL SELECT 'qa9102app_a03', 'a', 3, 'local_ins_9102_mgr_health'
    UNION ALL SELECT 'qa9102app_a04', 'a', 4, 'local_ins_9102_mgr_health'
    UNION ALL SELECT 'qa9102app_a05', 'a', 5, 'local_ins_9102_mgr_health'
    UNION ALL SELECT 'qa9102app_a06', 'a', 6, 'local_ins_9102_mgr_health'
    UNION ALL SELECT 'qa9102app_b01', 'b', 1, 'local_ins_9102_mgr_health'
    UNION ALL SELECT 'qa9102app_b02', 'b', 2, 'local_ins_9102_mgr_health'
    UNION ALL SELECT 'qa9102app_b03', 'b', 3, 'local_ins_9102_mgr_health'
    UNION ALL SELECT 'qa9102app_b04', 'b', 4, 'local_ins_9102_mgr_health'
    UNION ALL SELECT 'qa9102app_b05', 'b', 5, 'local_ins_9102_mgr_health'
    UNION ALL SELECT 'qa9102app_b06', 'b', 6, 'local_ins_9102_mgr_health'
    UNION ALL SELECT 'qa9102app_c01', 'c', 1, 'local_ins_9102_operator'
    UNION ALL SELECT 'qa9102app_c02', 'c', 2, 'local_ins_9102_operator'
    UNION ALL SELECT 'qa9102app_c03', 'c', 3, 'local_ins_9102_operator'
    UNION ALL SELECT 'qa9102app_c04', 'c', 4, 'local_ins_9102_operator'
    UNION ALL SELECT 'qa9102app_c05', 'c', 5, 'local_ins_9102_operator'
    UNION ALL SELECT 'qa9102app_c06', 'c', 6, 'local_ins_9102_operator'
    UNION ALL SELECT 'qa9102app_d01', 'd', 1, 'local_ins_9102_operator'
    UNION ALL SELECT 'qa9102app_d02', 'd', 2, 'local_ins_9102_operator'
    UNION ALL SELECT 'qa9102app_d03', 'd', 3, 'local_ins_9102_operator'
    UNION ALL SELECT 'qa9102app_d04', 'd', 4, 'local_ins_9102_operator'
    UNION ALL SELECT 'qa9102app_d05', 'd', 5, 'local_ins_9102_operator'
    UNION ALL SELECT 'qa9102app_d06', 'd', 6, 'local_ins_9102_operator'
) u
JOIN sys_user creator
  ON creator.username = u.creator_username AND creator.status = 1 AND creator.del_flag = 0
ON DUPLICATE KEY UPDATE
    owner_org_ref = VALUES(owner_org_ref), rehealth_user_id = VALUES(rehealth_user_id),
    status = 'active', current_revision_id = VALUES(current_revision_id),
    draft_revision_id = VALUES(draft_revision_id), lock_version = 2,
    created_by = VALUES(created_by), updated_by = VALUES(updated_by),
    updated_at = VALUES(updated_at);

-- ============================================================================
-- 2. 计划版本（36 条：A/B 各 1 个已发布；C 已发布 v1 + 草稿 v2；
--    D 已发布 v1（已失效）+ 已发布 v2）
-- ============================================================================
INSERT INTO rehealth_care_plan_revision (
    id, tenant_id, plan_id, revision_no, status, title, summary, change_reason,
    content_hash, effective_from, effective_to, published_by, published_at,
    withdrawn_by, withdrawn_at, created_by, created_at, updated_by, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:revision:9102:', r.cat, r.idx, ':', r.revision_no), 256)),
    @tenant,
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:care-plan:9102:', r.cat, r.idx), 256)),
    r.revision_no, r.status,
    CONCAT('心血管健康管理计划（', UPPER(r.cat), r.idx, CASE WHEN r.revision_no = 2 THEN '·第2版' ELSE '' END, '）'),
    '围绕血压监测、规律运动和睡眠管理的生活方式干预计划（测试数据）',
    CASE WHEN r.revision_no = 2 THEN '根据近期执行情况调整运动与监测安排' ELSE '初始计划发布' END,
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:revision-content:9102:', r.cat, r.idx, ':', r.revision_no), 256)),
    CASE WHEN r.status = 'published' THEN r.effective_from ELSE NULL END,
    CASE WHEN r.cat = 'd' AND r.revision_no = 1 THEN DATE_SUB(@seed_time, INTERVAL 15 DAY) ELSE NULL END,
    CASE WHEN r.status = 'published' THEN publisher.id ELSE NULL END,
    CASE WHEN r.status = 'published' THEN r.published_at ELSE NULL END,
    NULL, NULL,
    creator.id, r.created_at, creator.id, @seed_time
FROM (
    SELECT 'a' cat, 1 idx, 1 revision_no, 'published' status,
           DATE_SUB(@seed_time, INTERVAL 60 DAY) effective_from,
           DATE_SUB(@seed_time, INTERVAL 60 DAY) published_at,
           DATE_SUB(@seed_time, INTERVAL 60 DAY) created_at
    UNION ALL SELECT 'a', 2, 1, 'published', DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY)
    UNION ALL SELECT 'a', 3, 1, 'published', DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY)
    UNION ALL SELECT 'a', 4, 1, 'published', DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY)
    UNION ALL SELECT 'a', 5, 1, 'published', DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY)
    UNION ALL SELECT 'a', 6, 1, 'published', DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY)
    UNION ALL SELECT 'b', 1, 1, 'published', DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY)
    UNION ALL SELECT 'b', 2, 1, 'published', DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY)
    UNION ALL SELECT 'b', 3, 1, 'published', DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY)
    UNION ALL SELECT 'b', 4, 1, 'published', DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY)
    UNION ALL SELECT 'b', 5, 1, 'published', DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY)
    UNION ALL SELECT 'b', 6, 1, 'published', DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY)
    UNION ALL SELECT 'c', 1, 1, 'published', DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY)
    UNION ALL SELECT 'c', 1, 2, 'draft', NULL, NULL, DATE_SUB(@seed_time, INTERVAL 4 DAY)
    UNION ALL SELECT 'c', 2, 1, 'published', DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY)
    UNION ALL SELECT 'c', 2, 2, 'draft', NULL, NULL, DATE_SUB(@seed_time, INTERVAL 4 DAY)
    UNION ALL SELECT 'c', 3, 1, 'published', DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY)
    UNION ALL SELECT 'c', 3, 2, 'draft', NULL, NULL, DATE_SUB(@seed_time, INTERVAL 4 DAY)
    UNION ALL SELECT 'c', 4, 1, 'published', DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY)
    UNION ALL SELECT 'c', 4, 2, 'draft', NULL, NULL, DATE_SUB(@seed_time, INTERVAL 4 DAY)
    UNION ALL SELECT 'c', 5, 1, 'published', DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY)
    UNION ALL SELECT 'c', 5, 2, 'draft', NULL, NULL, DATE_SUB(@seed_time, INTERVAL 4 DAY)
    UNION ALL SELECT 'c', 6, 1, 'published', DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY)
    UNION ALL SELECT 'c', 6, 2, 'draft', NULL, NULL, DATE_SUB(@seed_time, INTERVAL 4 DAY)
    UNION ALL SELECT 'd', 1, 1, 'published', DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY)
    UNION ALL SELECT 'd', 1, 2, 'published', DATE_SUB(@seed_time, INTERVAL 15 DAY), DATE_SUB(@seed_time, INTERVAL 15 DAY), DATE_SUB(@seed_time, INTERVAL 16 DAY)
    UNION ALL SELECT 'd', 2, 1, 'published', DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY)
    UNION ALL SELECT 'd', 2, 2, 'published', DATE_SUB(@seed_time, INTERVAL 15 DAY), DATE_SUB(@seed_time, INTERVAL 15 DAY), DATE_SUB(@seed_time, INTERVAL 16 DAY)
    UNION ALL SELECT 'd', 3, 1, 'published', DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY)
    UNION ALL SELECT 'd', 3, 2, 'published', DATE_SUB(@seed_time, INTERVAL 15 DAY), DATE_SUB(@seed_time, INTERVAL 15 DAY), DATE_SUB(@seed_time, INTERVAL 16 DAY)
    UNION ALL SELECT 'd', 4, 1, 'published', DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY)
    UNION ALL SELECT 'd', 4, 2, 'published', DATE_SUB(@seed_time, INTERVAL 15 DAY), DATE_SUB(@seed_time, INTERVAL 15 DAY), DATE_SUB(@seed_time, INTERVAL 16 DAY)
    UNION ALL SELECT 'd', 5, 1, 'published', DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY)
    UNION ALL SELECT 'd', 5, 2, 'published', DATE_SUB(@seed_time, INTERVAL 15 DAY), DATE_SUB(@seed_time, INTERVAL 15 DAY), DATE_SUB(@seed_time, INTERVAL 16 DAY)
    UNION ALL SELECT 'd', 6, 1, 'published', DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY), DATE_SUB(@seed_time, INTERVAL 60 DAY)
    UNION ALL SELECT 'd', 6, 2, 'published', DATE_SUB(@seed_time, INTERVAL 15 DAY), DATE_SUB(@seed_time, INTERVAL 15 DAY), DATE_SUB(@seed_time, INTERVAL 16 DAY)
) r
JOIN sys_user creator
  ON creator.username = CASE WHEN r.cat IN ('a', 'b') THEN 'local_ins_9102_mgr_health' ELSE 'local_ins_9102_operator' END
 AND creator.status = 1 AND creator.del_flag = 0
JOIN sys_user publisher
  ON publisher.username = CASE WHEN r.cat IN ('a', 'b') THEN 'local_ins_9102_mgr_health' ELSE 'local_ins_9102_operator' END
 AND publisher.status = 1 AND publisher.del_flag = 0
ON DUPLICATE KEY UPDATE
    plan_id = VALUES(plan_id), status = VALUES(status), title = VALUES(title),
    summary = VALUES(summary), change_reason = VALUES(change_reason),
    content_hash = VALUES(content_hash), effective_from = VALUES(effective_from),
    effective_to = VALUES(effective_to), published_by = VALUES(published_by),
    published_at = VALUES(published_at), updated_at = VALUES(updated_at);

-- ============================================================================
-- 3. 计划项目快照（每版本 3 个项目：监测/运动/睡眠；共 108 条）
-- ============================================================================
INSERT INTO rehealth_care_plan_item (
    id, tenant_id, plan_id, revision_id, logical_item_id, category,
    title, instructions, schedule_json, scoring_weight,
    allow_not_applicable, display_order, created_at
)
SELECT
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:care-item:9102:', i.cat, i.idx, ':', i.revision_no, ':', i.pno), 256)),
    @tenant,
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:care-plan:9102:', i.cat, i.idx), 256)),
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:revision:9102:', i.cat, i.idx, ':', i.revision_no), 256)),
    CONCAT('qa9102-li-', LPAD(i.pno, 2, '0')),
    p.category, p.title, p.instructions,
    p.schedule_json, 1.000, 1, i.pno, @seed_time
FROM (
    SELECT 'a' cat, 1 idx, 1 revision_no, 1 pno
    UNION ALL SELECT 'a', 1, 1, 2
    UNION ALL SELECT 'a', 1, 1, 3
    UNION ALL SELECT 'a', 2, 1, 1 UNION ALL SELECT 'a', 2, 1, 2 UNION ALL SELECT 'a', 2, 1, 3
    UNION ALL SELECT 'a', 3, 1, 1 UNION ALL SELECT 'a', 3, 1, 2 UNION ALL SELECT 'a', 3, 1, 3
    UNION ALL SELECT 'a', 4, 1, 1 UNION ALL SELECT 'a', 4, 1, 2 UNION ALL SELECT 'a', 4, 1, 3
    UNION ALL SELECT 'a', 5, 1, 1 UNION ALL SELECT 'a', 5, 1, 2 UNION ALL SELECT 'a', 5, 1, 3
    UNION ALL SELECT 'a', 6, 1, 1 UNION ALL SELECT 'a', 6, 1, 2 UNION ALL SELECT 'a', 6, 1, 3
    UNION ALL SELECT 'b', 1, 1, 1 UNION ALL SELECT 'b', 1, 1, 2 UNION ALL SELECT 'b', 1, 1, 3
    UNION ALL SELECT 'b', 2, 1, 1 UNION ALL SELECT 'b', 2, 1, 2 UNION ALL SELECT 'b', 2, 1, 3
    UNION ALL SELECT 'b', 3, 1, 1 UNION ALL SELECT 'b', 3, 1, 2 UNION ALL SELECT 'b', 3, 1, 3
    UNION ALL SELECT 'b', 4, 1, 1 UNION ALL SELECT 'b', 4, 1, 2 UNION ALL SELECT 'b', 4, 1, 3
    UNION ALL SELECT 'b', 5, 1, 1 UNION ALL SELECT 'b', 5, 1, 2 UNION ALL SELECT 'b', 5, 1, 3
    UNION ALL SELECT 'b', 6, 1, 1 UNION ALL SELECT 'b', 6, 1, 2 UNION ALL SELECT 'b', 6, 1, 3
    UNION ALL SELECT 'c', 1, 1, 1 UNION ALL SELECT 'c', 1, 1, 2 UNION ALL SELECT 'c', 1, 1, 3
    UNION ALL SELECT 'c', 1, 2, 1 UNION ALL SELECT 'c', 1, 2, 2 UNION ALL SELECT 'c', 1, 2, 3
    UNION ALL SELECT 'c', 2, 1, 1 UNION ALL SELECT 'c', 2, 1, 2 UNION ALL SELECT 'c', 2, 1, 3
    UNION ALL SELECT 'c', 2, 2, 1 UNION ALL SELECT 'c', 2, 2, 2 UNION ALL SELECT 'c', 2, 2, 3
    UNION ALL SELECT 'c', 3, 1, 1 UNION ALL SELECT 'c', 3, 1, 2 UNION ALL SELECT 'c', 3, 1, 3
    UNION ALL SELECT 'c', 3, 2, 1 UNION ALL SELECT 'c', 3, 2, 2 UNION ALL SELECT 'c', 3, 2, 3
    UNION ALL SELECT 'c', 4, 1, 1 UNION ALL SELECT 'c', 4, 1, 2 UNION ALL SELECT 'c', 4, 1, 3
    UNION ALL SELECT 'c', 4, 2, 1 UNION ALL SELECT 'c', 4, 2, 2 UNION ALL SELECT 'c', 4, 2, 3
    UNION ALL SELECT 'c', 5, 1, 1 UNION ALL SELECT 'c', 5, 1, 2 UNION ALL SELECT 'c', 5, 1, 3
    UNION ALL SELECT 'c', 5, 2, 1 UNION ALL SELECT 'c', 5, 2, 2 UNION ALL SELECT 'c', 5, 2, 3
    UNION ALL SELECT 'c', 6, 1, 1 UNION ALL SELECT 'c', 6, 1, 2 UNION ALL SELECT 'c', 6, 1, 3
    UNION ALL SELECT 'c', 6, 2, 1 UNION ALL SELECT 'c', 6, 2, 2 UNION ALL SELECT 'c', 6, 2, 3
    UNION ALL SELECT 'd', 1, 1, 1 UNION ALL SELECT 'd', 1, 1, 2 UNION ALL SELECT 'd', 1, 1, 3
    UNION ALL SELECT 'd', 1, 2, 1 UNION ALL SELECT 'd', 1, 2, 2 UNION ALL SELECT 'd', 1, 2, 3
    UNION ALL SELECT 'd', 2, 1, 1 UNION ALL SELECT 'd', 2, 1, 2 UNION ALL SELECT 'd', 2, 1, 3
    UNION ALL SELECT 'd', 2, 2, 1 UNION ALL SELECT 'd', 2, 2, 2 UNION ALL SELECT 'd', 2, 2, 3
    UNION ALL SELECT 'd', 3, 1, 1 UNION ALL SELECT 'd', 3, 1, 2 UNION ALL SELECT 'd', 3, 1, 3
    UNION ALL SELECT 'd', 3, 2, 1 UNION ALL SELECT 'd', 3, 2, 2 UNION ALL SELECT 'd', 3, 2, 3
    UNION ALL SELECT 'd', 4, 1, 1 UNION ALL SELECT 'd', 4, 1, 2 UNION ALL SELECT 'd', 4, 1, 3
    UNION ALL SELECT 'd', 4, 2, 1 UNION ALL SELECT 'd', 4, 2, 2 UNION ALL SELECT 'd', 4, 2, 3
    UNION ALL SELECT 'd', 5, 1, 1 UNION ALL SELECT 'd', 5, 1, 2 UNION ALL SELECT 'd', 5, 1, 3
    UNION ALL SELECT 'd', 5, 2, 1 UNION ALL SELECT 'd', 5, 2, 2 UNION ALL SELECT 'd', 5, 2, 3
    UNION ALL SELECT 'd', 6, 1, 1 UNION ALL SELECT 'd', 6, 1, 2 UNION ALL SELECT 'd', 6, 1, 3
    UNION ALL SELECT 'd', 6, 2, 1 UNION ALL SELECT 'd', 6, 2, 2 UNION ALL SELECT 'd', 6, 2, 3
) i
JOIN (
    SELECT 1 pno, 'monitoring' category, '每日血压监测与记录' title,
           '早晚各测一次血压并记录；不根据单次读数调整治疗。' instructions,
           '{"type":"daily","times":["08:00","20:00"]}' schedule_json
    UNION ALL SELECT 2, 'exercise', '每周 150 分钟有氧运动',
           '中等强度有氧运动，可分次完成，单次不少于 20 分钟。',
           '{"type":"weekly","minutes":150}'
    UNION ALL SELECT 3, 'sleep', '保持每日 7 小时睡眠',
           '固定就寝与起床时间，睡前避免咖啡因与屏幕。',
           '{"type":"daily","hours":7}'
) p ON p.pno = i.pno
ON DUPLICATE KEY UPDATE
    plan_id = VALUES(plan_id), revision_id = VALUES(revision_id),
    logical_item_id = VALUES(logical_item_id), category = VALUES(category),
    title = VALUES(title), instructions = VALUES(instructions),
    schedule_json = VALUES(schedule_json), display_order = VALUES(display_order);

-- ============================================================================
-- 4. 任务实例（occurrence，28 天窗口内 + 未来任务；覆盖 cancelled 场景）
--    时间模式（相对锚点，days_ago 为负表示未来）：
--      o1: 1 天前（已到期）；o2: 4 天前；o3: 7 天前；
--      o4: 未来 1 天（C/D 类新增版本使用，未到期且无执行 -> 不计分母）
-- ============================================================================
INSERT INTO rehealth_care_plan_occurrence (
    id, tenant_id, plan_id, revision_id, plan_item_id, logical_item_id,
    subject_ref, scheduled_at, due_at, status, exclusion_reason,
    created_at, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:occurrence:9102:', o.cat, o.idx, ':', o.revision_no, ':', o.pno, ':', o.tpl), 256)),
    @tenant,
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:care-plan:9102:', o.cat, o.idx), 256)),
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:revision:9102:', o.cat, o.idx, ':', o.revision_no), 256)),
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:care-item:9102:', o.cat, o.idx, ':', o.revision_no, ':', o.pno), 256)),
    CONCAT('qa9102-li-', LPAD(o.pno, 2, '0')),
    CONCAT('QA9102-', UPPER(o.cat), LPAD(o.idx, 2, '0')),
    CASE WHEN o.days_ago < 0 THEN DATE_ADD(@seed_time, INTERVAL (-o.days_ago) DAY)
         ELSE DATE_SUB(@seed_time, INTERVAL o.days_ago DAY) END,
    CASE WHEN o.days_ago < 0 THEN DATE_ADD(@seed_time, INTERVAL (1 - o.days_ago) DAY)
         ELSE DATE_SUB(@seed_time, INTERVAL (o.days_ago - 1) DAY) END,
    o.status,
    CASE WHEN o.status = 'cancelled' THEN '机构调整计划暂停该项（测试数据）' ELSE NULL END,
    CASE WHEN o.days_ago < 0 THEN DATE_ADD(@seed_time, INTERVAL (-o.days_ago) DAY)
         ELSE DATE_SUB(@seed_time, INTERVAL o.days_ago DAY) END,
    @seed_time
FROM (
    -- A 类 v1：o1/o2/o3；a04 项目 2 的 o3 为 cancelled
    SELECT 'a' cat, 1 idx, 1 revision_no, 1 pno, 1 tpl, 1 days_ago, 'scheduled' status
    UNION ALL SELECT 'a', 1, 1, 1, 2, 4, 'scheduled'
    UNION ALL SELECT 'a', 1, 1, 1, 3, 7, 'scheduled'
    UNION ALL SELECT 'a', 1, 1, 2, 1, 1, 'scheduled' UNION ALL SELECT 'a', 1, 1, 2, 2, 4, 'scheduled' UNION ALL SELECT 'a', 1, 1, 2, 3, 7, 'scheduled'
    UNION ALL SELECT 'a', 1, 1, 3, 1, 1, 'scheduled' UNION ALL SELECT 'a', 1, 1, 3, 2, 4, 'scheduled' UNION ALL SELECT 'a', 1, 1, 3, 3, 7, 'scheduled'
    UNION ALL SELECT 'a', 2, 1, 1, 1, 1, 'scheduled' UNION ALL SELECT 'a', 2, 1, 1, 2, 4, 'scheduled' UNION ALL SELECT 'a', 2, 1, 1, 3, 7, 'scheduled'
    UNION ALL SELECT 'a', 2, 1, 2, 1, 1, 'scheduled' UNION ALL SELECT 'a', 2, 1, 2, 2, 4, 'scheduled' UNION ALL SELECT 'a', 2, 1, 2, 3, 7, 'scheduled'
    UNION ALL SELECT 'a', 2, 1, 3, 1, 1, 'scheduled' UNION ALL SELECT 'a', 2, 1, 3, 2, 4, 'scheduled' UNION ALL SELECT 'a', 2, 1, 3, 3, 7, 'scheduled'
    UNION ALL SELECT 'a', 3, 1, 1, 1, 1, 'scheduled' UNION ALL SELECT 'a', 3, 1, 1, 2, 4, 'scheduled' UNION ALL SELECT 'a', 3, 1, 1, 3, 7, 'scheduled'
    UNION ALL SELECT 'a', 3, 1, 2, 1, 1, 'scheduled' UNION ALL SELECT 'a', 3, 1, 2, 2, 4, 'scheduled' UNION ALL SELECT 'a', 3, 1, 2, 3, 7, 'scheduled'
    UNION ALL SELECT 'a', 3, 1, 3, 1, 1, 'scheduled' UNION ALL SELECT 'a', 3, 1, 3, 2, 4, 'scheduled' UNION ALL SELECT 'a', 3, 1, 3, 3, 7, 'scheduled'
    UNION ALL SELECT 'a', 4, 1, 1, 1, 1, 'scheduled' UNION ALL SELECT 'a', 4, 1, 1, 2, 4, 'scheduled' UNION ALL SELECT 'a', 4, 1, 1, 3, 7, 'scheduled'
    UNION ALL SELECT 'a', 4, 1, 2, 1, 1, 'scheduled' UNION ALL SELECT 'a', 4, 1, 2, 2, 4, 'scheduled' UNION ALL SELECT 'a', 4, 1, 2, 3, 7, 'cancelled'
    UNION ALL SELECT 'a', 4, 1, 3, 1, 1, 'scheduled' UNION ALL SELECT 'a', 4, 1, 3, 2, 4, 'scheduled' UNION ALL SELECT 'a', 4, 1, 3, 3, 7, 'scheduled'
    UNION ALL SELECT 'a', 5, 1, 1, 1, 1, 'scheduled' UNION ALL SELECT 'a', 5, 1, 1, 2, 4, 'scheduled' UNION ALL SELECT 'a', 5, 1, 1, 3, 7, 'scheduled'
    UNION ALL SELECT 'a', 5, 1, 2, 1, 1, 'scheduled' UNION ALL SELECT 'a', 5, 1, 2, 2, 4, 'scheduled' UNION ALL SELECT 'a', 5, 1, 2, 3, 7, 'scheduled'
    UNION ALL SELECT 'a', 5, 1, 3, 1, 1, 'scheduled' UNION ALL SELECT 'a', 5, 1, 3, 2, 4, 'scheduled' UNION ALL SELECT 'a', 5, 1, 3, 3, 7, 'scheduled'
    UNION ALL SELECT 'a', 6, 1, 1, 1, 1, 'scheduled' UNION ALL SELECT 'a', 6, 1, 1, 2, 4, 'scheduled' UNION ALL SELECT 'a', 6, 1, 1, 3, 7, 'scheduled'
    UNION ALL SELECT 'a', 6, 1, 2, 1, 1, 'scheduled' UNION ALL SELECT 'a', 6, 1, 2, 2, 4, 'scheduled' UNION ALL SELECT 'a', 6, 1, 2, 3, 7, 'scheduled'
    UNION ALL SELECT 'a', 6, 1, 3, 1, 1, 'scheduled' UNION ALL SELECT 'a', 6, 1, 3, 2, 4, 'scheduled' UNION ALL SELECT 'a', 6, 1, 3, 3, 7, 'scheduled'
    -- B 类 v1：o1/o2/o3
    UNION ALL SELECT 'b', 1, 1, 1, 1, 1, 'scheduled' UNION ALL SELECT 'b', 1, 1, 1, 2, 4, 'scheduled' UNION ALL SELECT 'b', 1, 1, 1, 3, 7, 'scheduled'
    UNION ALL SELECT 'b', 1, 1, 2, 1, 1, 'scheduled' UNION ALL SELECT 'b', 1, 1, 2, 2, 4, 'scheduled' UNION ALL SELECT 'b', 1, 1, 2, 3, 7, 'scheduled'
    UNION ALL SELECT 'b', 1, 1, 3, 1, 1, 'scheduled' UNION ALL SELECT 'b', 1, 1, 3, 2, 4, 'scheduled' UNION ALL SELECT 'b', 1, 1, 3, 3, 7, 'scheduled'
    UNION ALL SELECT 'b', 2, 1, 1, 1, 1, 'scheduled' UNION ALL SELECT 'b', 2, 1, 1, 2, 4, 'scheduled' UNION ALL SELECT 'b', 2, 1, 1, 3, 7, 'scheduled'
    UNION ALL SELECT 'b', 2, 1, 2, 1, 1, 'scheduled' UNION ALL SELECT 'b', 2, 1, 2, 2, 4, 'scheduled' UNION ALL SELECT 'b', 2, 1, 2, 3, 7, 'scheduled'
    UNION ALL SELECT 'b', 2, 1, 3, 1, 1, 'scheduled' UNION ALL SELECT 'b', 2, 1, 3, 2, 4, 'scheduled' UNION ALL SELECT 'b', 2, 1, 3, 3, 7, 'scheduled'
    UNION ALL SELECT 'b', 3, 1, 1, 1, 1, 'scheduled' UNION ALL SELECT 'b', 3, 1, 1, 2, 4, 'scheduled' UNION ALL SELECT 'b', 3, 1, 1, 3, 7, 'scheduled'
    UNION ALL SELECT 'b', 3, 1, 2, 1, 1, 'scheduled' UNION ALL SELECT 'b', 3, 1, 2, 2, 4, 'scheduled' UNION ALL SELECT 'b', 3, 1, 2, 3, 7, 'scheduled'
    UNION ALL SELECT 'b', 3, 1, 3, 1, 1, 'scheduled' UNION ALL SELECT 'b', 3, 1, 3, 2, 4, 'scheduled' UNION ALL SELECT 'b', 3, 1, 3, 3, 7, 'scheduled'
    UNION ALL SELECT 'b', 4, 1, 1, 1, 1, 'scheduled' UNION ALL SELECT 'b', 4, 1, 1, 2, 4, 'scheduled' UNION ALL SELECT 'b', 4, 1, 1, 3, 7, 'scheduled'
    UNION ALL SELECT 'b', 4, 1, 2, 1, 1, 'scheduled' UNION ALL SELECT 'b', 4, 1, 2, 2, 4, 'scheduled' UNION ALL SELECT 'b', 4, 1, 2, 3, 7, 'scheduled'
    UNION ALL SELECT 'b', 4, 1, 3, 1, 1, 'scheduled' UNION ALL SELECT 'b', 4, 1, 3, 2, 4, 'scheduled' UNION ALL SELECT 'b', 4, 1, 3, 3, 7, 'scheduled'
    UNION ALL SELECT 'b', 5, 1, 1, 1, 1, 'scheduled' UNION ALL SELECT 'b', 5, 1, 1, 2, 4, 'scheduled' UNION ALL SELECT 'b', 5, 1, 1, 3, 7, 'scheduled'
    UNION ALL SELECT 'b', 5, 1, 2, 1, 1, 'scheduled' UNION ALL SELECT 'b', 5, 1, 2, 2, 4, 'scheduled' UNION ALL SELECT 'b', 5, 1, 2, 3, 7, 'scheduled'
    UNION ALL SELECT 'b', 5, 1, 3, 1, 1, 'scheduled' UNION ALL SELECT 'b', 5, 1, 3, 2, 4, 'scheduled' UNION ALL SELECT 'b', 5, 1, 3, 3, 7, 'scheduled'
    UNION ALL SELECT 'b', 6, 1, 1, 1, 1, 'scheduled' UNION ALL SELECT 'b', 6, 1, 1, 2, 4, 'scheduled' UNION ALL SELECT 'b', 6, 1, 1, 3, 7, 'scheduled'
    UNION ALL SELECT 'b', 6, 1, 2, 1, 1, 'scheduled' UNION ALL SELECT 'b', 6, 1, 2, 2, 4, 'scheduled' UNION ALL SELECT 'b', 6, 1, 2, 3, 7, 'scheduled'
    UNION ALL SELECT 'b', 6, 1, 3, 1, 1, 'scheduled' UNION ALL SELECT 'b', 6, 1, 3, 2, 4, 'scheduled' UNION ALL SELECT 'b', 6, 1, 3, 3, 7, 'scheduled'
    -- C 类 v1：o1/o2/o3（+o4 未来由 v1 提供）；v2 草稿无任务
    UNION ALL SELECT 'c', 1, 1, 1, 1, 1, 'scheduled' UNION ALL SELECT 'c', 1, 1, 1, 2, 4, 'scheduled' UNION ALL SELECT 'c', 1, 1, 1, 3, 7, 'scheduled' UNION ALL SELECT 'c', 1, 1, 1, 4, -1, 'scheduled'
    UNION ALL SELECT 'c', 1, 1, 2, 1, 1, 'scheduled' UNION ALL SELECT 'c', 1, 1, 2, 2, 4, 'scheduled' UNION ALL SELECT 'c', 1, 1, 2, 3, 7, 'scheduled' UNION ALL SELECT 'c', 1, 1, 2, 4, -1, 'scheduled'
    UNION ALL SELECT 'c', 1, 1, 3, 1, 1, 'scheduled' UNION ALL SELECT 'c', 1, 1, 3, 2, 4, 'scheduled' UNION ALL SELECT 'c', 1, 1, 3, 3, 7, 'scheduled' UNION ALL SELECT 'c', 1, 1, 3, 4, -1, 'scheduled'
    UNION ALL SELECT 'c', 2, 1, 1, 1, 1, 'scheduled' UNION ALL SELECT 'c', 2, 1, 1, 2, 4, 'scheduled' UNION ALL SELECT 'c', 2, 1, 1, 3, 7, 'scheduled' UNION ALL SELECT 'c', 2, 1, 1, 4, -1, 'scheduled'
    UNION ALL SELECT 'c', 2, 1, 2, 1, 1, 'scheduled' UNION ALL SELECT 'c', 2, 1, 2, 2, 4, 'scheduled' UNION ALL SELECT 'c', 2, 1, 2, 3, 7, 'scheduled' UNION ALL SELECT 'c', 2, 1, 2, 4, -1, 'scheduled'
    UNION ALL SELECT 'c', 2, 1, 3, 1, 1, 'scheduled' UNION ALL SELECT 'c', 2, 1, 3, 2, 4, 'scheduled' UNION ALL SELECT 'c', 2, 1, 3, 3, 7, 'scheduled' UNION ALL SELECT 'c', 2, 1, 3, 4, -1, 'scheduled'
    UNION ALL SELECT 'c', 3, 1, 1, 1, 1, 'scheduled' UNION ALL SELECT 'c', 3, 1, 1, 2, 4, 'scheduled' UNION ALL SELECT 'c', 3, 1, 1, 3, 7, 'scheduled' UNION ALL SELECT 'c', 3, 1, 1, 4, -1, 'scheduled'
    UNION ALL SELECT 'c', 3, 1, 2, 1, 1, 'scheduled' UNION ALL SELECT 'c', 3, 1, 2, 2, 4, 'scheduled' UNION ALL SELECT 'c', 3, 1, 2, 3, 7, 'scheduled' UNION ALL SELECT 'c', 3, 1, 2, 4, -1, 'scheduled'
    UNION ALL SELECT 'c', 3, 1, 3, 1, 1, 'scheduled' UNION ALL SELECT 'c', 3, 1, 3, 2, 4, 'scheduled' UNION ALL SELECT 'c', 3, 1, 3, 3, 7, 'scheduled' UNION ALL SELECT 'c', 3, 1, 3, 4, -1, 'scheduled'
    UNION ALL SELECT 'c', 4, 1, 1, 1, 1, 'scheduled' UNION ALL SELECT 'c', 4, 1, 1, 2, 4, 'scheduled' UNION ALL SELECT 'c', 4, 1, 1, 3, 7, 'scheduled' UNION ALL SELECT 'c', 4, 1, 1, 4, -1, 'scheduled'
    UNION ALL SELECT 'c', 4, 1, 2, 1, 1, 'scheduled' UNION ALL SELECT 'c', 4, 1, 2, 2, 4, 'scheduled' UNION ALL SELECT 'c', 4, 1, 2, 3, 7, 'scheduled' UNION ALL SELECT 'c', 4, 1, 2, 4, -1, 'scheduled'
    UNION ALL SELECT 'c', 4, 1, 3, 1, 1, 'scheduled' UNION ALL SELECT 'c', 4, 1, 3, 2, 4, 'scheduled' UNION ALL SELECT 'c', 4, 1, 3, 3, 7, 'scheduled' UNION ALL SELECT 'c', 4, 1, 3, 4, -1, 'scheduled'
    UNION ALL SELECT 'c', 5, 1, 1, 1, 1, 'scheduled' UNION ALL SELECT 'c', 5, 1, 1, 2, 4, 'scheduled' UNION ALL SELECT 'c', 5, 1, 1, 3, 7, 'scheduled' UNION ALL SELECT 'c', 5, 1, 1, 4, -1, 'scheduled'
    UNION ALL SELECT 'c', 5, 1, 2, 1, 1, 'scheduled' UNION ALL SELECT 'c', 5, 1, 2, 2, 4, 'scheduled' UNION ALL SELECT 'c', 5, 1, 2, 3, 7, 'scheduled' UNION ALL SELECT 'c', 5, 1, 2, 4, -1, 'scheduled'
    UNION ALL SELECT 'c', 5, 1, 3, 1, 1, 'scheduled' UNION ALL SELECT 'c', 5, 1, 3, 2, 4, 'scheduled' UNION ALL SELECT 'c', 5, 1, 3, 3, 7, 'scheduled' UNION ALL SELECT 'c', 5, 1, 3, 4, -1, 'scheduled'
    UNION ALL SELECT 'c', 6, 1, 1, 1, 1, 'scheduled' UNION ALL SELECT 'c', 6, 1, 1, 2, 4, 'scheduled' UNION ALL SELECT 'c', 6, 1, 1, 3, 7, 'scheduled' UNION ALL SELECT 'c', 6, 1, 1, 4, -1, 'scheduled'
    UNION ALL SELECT 'c', 6, 1, 2, 1, 1, 'scheduled' UNION ALL SELECT 'c', 6, 1, 2, 2, 4, 'scheduled' UNION ALL SELECT 'c', 6, 1, 2, 3, 7, 'scheduled' UNION ALL SELECT 'c', 6, 1, 2, 4, -1, 'scheduled'
    UNION ALL SELECT 'c', 6, 1, 3, 1, 1, 'scheduled' UNION ALL SELECT 'c', 6, 1, 3, 2, 4, 'scheduled' UNION ALL SELECT 'c', 6, 1, 3, 3, 7, 'scheduled' UNION ALL SELECT 'c', 6, 1, 3, 4, -1, 'scheduled'
    -- D 类 v1：o1/o2/o3；v2：o1/o2 已执行 + o4 未来
    UNION ALL SELECT 'd', 1, 1, 1, 1, 1, 'scheduled' UNION ALL SELECT 'd', 1, 1, 1, 2, 4, 'scheduled' UNION ALL SELECT 'd', 1, 1, 1, 3, 7, 'scheduled'
    UNION ALL SELECT 'd', 1, 1, 2, 1, 1, 'scheduled' UNION ALL SELECT 'd', 1, 1, 2, 2, 4, 'scheduled' UNION ALL SELECT 'd', 1, 1, 2, 3, 7, 'scheduled'
    UNION ALL SELECT 'd', 1, 1, 3, 1, 1, 'scheduled' UNION ALL SELECT 'd', 1, 1, 3, 2, 4, 'scheduled' UNION ALL SELECT 'd', 1, 1, 3, 3, 7, 'scheduled'
    UNION ALL SELECT 'd', 1, 2, 1, 1, 1, 'scheduled' UNION ALL SELECT 'd', 1, 2, 1, 2, 2, 'scheduled' UNION ALL SELECT 'd', 1, 2, 1, 4, -1, 'scheduled'
    UNION ALL SELECT 'd', 1, 2, 2, 1, 1, 'scheduled' UNION ALL SELECT 'd', 1, 2, 2, 2, 2, 'scheduled' UNION ALL SELECT 'd', 1, 2, 2, 4, -1, 'scheduled'
    UNION ALL SELECT 'd', 1, 2, 3, 1, 1, 'scheduled' UNION ALL SELECT 'd', 1, 2, 3, 2, 2, 'scheduled' UNION ALL SELECT 'd', 1, 2, 3, 4, -1, 'scheduled'
    UNION ALL SELECT 'd', 2, 1, 1, 1, 1, 'scheduled' UNION ALL SELECT 'd', 2, 1, 1, 2, 4, 'scheduled' UNION ALL SELECT 'd', 2, 1, 1, 3, 7, 'scheduled'
    UNION ALL SELECT 'd', 2, 1, 2, 1, 1, 'scheduled' UNION ALL SELECT 'd', 2, 1, 2, 2, 4, 'scheduled' UNION ALL SELECT 'd', 2, 1, 2, 3, 7, 'scheduled'
    UNION ALL SELECT 'd', 2, 1, 3, 1, 1, 'scheduled' UNION ALL SELECT 'd', 2, 1, 3, 2, 4, 'scheduled' UNION ALL SELECT 'd', 2, 1, 3, 3, 7, 'scheduled'
    UNION ALL SELECT 'd', 2, 2, 1, 1, 1, 'scheduled' UNION ALL SELECT 'd', 2, 2, 1, 2, 2, 'scheduled' UNION ALL SELECT 'd', 2, 2, 1, 4, -1, 'scheduled'
    UNION ALL SELECT 'd', 2, 2, 2, 1, 1, 'scheduled' UNION ALL SELECT 'd', 2, 2, 2, 2, 2, 'scheduled' UNION ALL SELECT 'd', 2, 2, 2, 4, -1, 'scheduled'
    UNION ALL SELECT 'd', 2, 2, 3, 1, 1, 'scheduled' UNION ALL SELECT 'd', 2, 2, 3, 2, 2, 'scheduled' UNION ALL SELECT 'd', 2, 2, 3, 4, -1, 'scheduled'
    UNION ALL SELECT 'd', 3, 1, 1, 1, 1, 'scheduled' UNION ALL SELECT 'd', 3, 1, 1, 2, 4, 'scheduled' UNION ALL SELECT 'd', 3, 1, 1, 3, 7, 'scheduled'
    UNION ALL SELECT 'd', 3, 1, 2, 1, 1, 'scheduled' UNION ALL SELECT 'd', 3, 1, 2, 2, 4, 'scheduled' UNION ALL SELECT 'd', 3, 1, 2, 3, 7, 'scheduled'
    UNION ALL SELECT 'd', 3, 1, 3, 1, 1, 'scheduled' UNION ALL SELECT 'd', 3, 1, 3, 2, 4, 'scheduled' UNION ALL SELECT 'd', 3, 1, 3, 3, 7, 'scheduled'
    UNION ALL SELECT 'd', 3, 2, 1, 1, 1, 'scheduled' UNION ALL SELECT 'd', 3, 2, 1, 2, 2, 'scheduled' UNION ALL SELECT 'd', 3, 2, 1, 4, -1, 'scheduled'
    UNION ALL SELECT 'd', 3, 2, 2, 1, 1, 'scheduled' UNION ALL SELECT 'd', 3, 2, 2, 2, 2, 'scheduled' UNION ALL SELECT 'd', 3, 2, 2, 4, -1, 'scheduled'
    UNION ALL SELECT 'd', 3, 2, 3, 1, 1, 'scheduled' UNION ALL SELECT 'd', 3, 2, 3, 2, 2, 'scheduled' UNION ALL SELECT 'd', 3, 2, 3, 4, -1, 'scheduled'
    UNION ALL SELECT 'd', 4, 1, 1, 1, 1, 'scheduled' UNION ALL SELECT 'd', 4, 1, 1, 2, 4, 'scheduled' UNION ALL SELECT 'd', 4, 1, 1, 3, 7, 'scheduled'
    UNION ALL SELECT 'd', 4, 1, 2, 1, 1, 'scheduled' UNION ALL SELECT 'd', 4, 1, 2, 2, 4, 'scheduled' UNION ALL SELECT 'd', 4, 1, 2, 3, 7, 'scheduled'
    UNION ALL SELECT 'd', 4, 1, 3, 1, 1, 'scheduled' UNION ALL SELECT 'd', 4, 1, 3, 2, 4, 'scheduled' UNION ALL SELECT 'd', 4, 1, 3, 3, 7, 'scheduled'
    UNION ALL SELECT 'd', 4, 2, 1, 1, 1, 'scheduled' UNION ALL SELECT 'd', 4, 2, 1, 2, 2, 'scheduled' UNION ALL SELECT 'd', 4, 2, 1, 4, -1, 'scheduled'
    UNION ALL SELECT 'd', 4, 2, 2, 1, 1, 'scheduled' UNION ALL SELECT 'd', 4, 2, 2, 2, 2, 'scheduled' UNION ALL SELECT 'd', 4, 2, 2, 4, -1, 'scheduled'
    UNION ALL SELECT 'd', 4, 2, 3, 1, 1, 'scheduled' UNION ALL SELECT 'd', 4, 2, 3, 2, 2, 'scheduled' UNION ALL SELECT 'd', 4, 2, 3, 4, -1, 'scheduled'
    UNION ALL SELECT 'd', 5, 1, 1, 1, 1, 'scheduled' UNION ALL SELECT 'd', 5, 1, 1, 2, 4, 'scheduled' UNION ALL SELECT 'd', 5, 1, 1, 3, 7, 'scheduled'
    UNION ALL SELECT 'd', 5, 1, 2, 1, 1, 'scheduled' UNION ALL SELECT 'd', 5, 1, 2, 2, 4, 'scheduled' UNION ALL SELECT 'd', 5, 1, 2, 3, 7, 'scheduled'
    UNION ALL SELECT 'd', 5, 1, 3, 1, 1, 'scheduled' UNION ALL SELECT 'd', 5, 1, 3, 2, 4, 'scheduled' UNION ALL SELECT 'd', 5, 1, 3, 3, 7, 'scheduled'
    UNION ALL SELECT 'd', 5, 2, 1, 1, 1, 'scheduled' UNION ALL SELECT 'd', 5, 2, 1, 2, 2, 'scheduled' UNION ALL SELECT 'd', 5, 2, 1, 4, -1, 'scheduled'
    UNION ALL SELECT 'd', 5, 2, 2, 1, 1, 'scheduled' UNION ALL SELECT 'd', 5, 2, 2, 2, 2, 'scheduled' UNION ALL SELECT 'd', 5, 2, 2, 4, -1, 'scheduled'
    UNION ALL SELECT 'd', 5, 2, 3, 1, 1, 'scheduled' UNION ALL SELECT 'd', 5, 2, 3, 2, 2, 'scheduled' UNION ALL SELECT 'd', 5, 2, 3, 4, -1, 'scheduled'
    UNION ALL SELECT 'd', 6, 1, 1, 1, 1, 'scheduled' UNION ALL SELECT 'd', 6, 1, 1, 2, 4, 'scheduled' UNION ALL SELECT 'd', 6, 1, 1, 3, 7, 'scheduled'
    UNION ALL SELECT 'd', 6, 1, 2, 1, 1, 'scheduled' UNION ALL SELECT 'd', 6, 1, 2, 2, 4, 'scheduled' UNION ALL SELECT 'd', 6, 1, 2, 3, 7, 'scheduled'
    UNION ALL SELECT 'd', 6, 1, 3, 1, 1, 'scheduled' UNION ALL SELECT 'd', 6, 1, 3, 2, 4, 'scheduled' UNION ALL SELECT 'd', 6, 1, 3, 3, 7, 'scheduled'
    UNION ALL SELECT 'd', 6, 2, 1, 1, 1, 'scheduled' UNION ALL SELECT 'd', 6, 2, 1, 2, 2, 'scheduled' UNION ALL SELECT 'd', 6, 2, 1, 4, -1, 'scheduled'
    UNION ALL SELECT 'd', 6, 2, 2, 1, 1, 'scheduled' UNION ALL SELECT 'd', 6, 2, 2, 2, 2, 'scheduled' UNION ALL SELECT 'd', 6, 2, 2, 4, -1, 'scheduled'
    UNION ALL SELECT 'd', 6, 2, 3, 1, 1, 'scheduled' UNION ALL SELECT 'd', 6, 2, 3, 2, 2, 'scheduled' UNION ALL SELECT 'd', 6, 2, 3, 4, -1, 'scheduled'
) o
ON DUPLICATE KEY UPDATE
    plan_id = VALUES(plan_id), revision_id = VALUES(revision_id),
    plan_item_id = VALUES(plan_item_id), logical_item_id = VALUES(logical_item_id),
    subject_ref = VALUES(subject_ref), scheduled_at = VALUES(scheduled_at),
    due_at = VALUES(due_at), status = VALUES(status),
    exclusion_reason = VALUES(exclusion_reason), updated_at = VALUES(updated_at);

-- ============================================================================
-- 5. 执行事实（execution）
--    A 类：o1 completed(1.0)、o2 skipped(0)、o3 无执行（逾期）
--    B 类：o1 completed、o2 partially(0.5)、o3 completed
--    C 类：o1 completed、o2 partially、o3 not_applicable(NULL)、o4 无执行（未来）
--    D 类：v1 o1/o2/o3 全 completed；v2 o1/o2 completed、o4 未来无执行
--    a04 项目 2 的 o3 为 cancelled，无执行事实
-- ============================================================================
INSERT INTO rehealth_care_plan_execution (
    id, tenant_id, occurrence_id, plan_id, revision_id, plan_item_id,
    logical_item_id, subject_ref, feedback_type, score_value,
    verification_type, note, occurred_at, source_system, source_record_id, created_at
)
SELECT
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:execution:9102:', e.cat, e.idx, ':', e.revision_no, ':', e.pno, ':', e.tpl), 256)),
    @tenant,
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:occurrence:9102:', e.cat, e.idx, ':', e.revision_no, ':', e.pno, ':', e.tpl), 256)),
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:care-plan:9102:', e.cat, e.idx), 256)),
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:revision:9102:', e.cat, e.idx, ':', e.revision_no), 256)),
    LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:care-item:9102:', e.cat, e.idx, ':', e.revision_no, ':', e.pno), 256)),
    CONCAT('qa9102-li-', LPAD(e.pno, 2, '0')),
    CONCAT('QA9102-', UPPER(e.cat), LPAD(e.idx, 2, '0')),
    e.feedback_type, e.score_value,
    CASE WHEN e.cat = 'd' THEN 'device_verification' ELSE 'self_report' END,
    CASE e.feedback_type
        WHEN 'completed' THEN '已完成并记录'
        WHEN 'partially_completed' THEN '部分完成，已记录'
        WHEN 'skipped' THEN '当日未执行'
        ELSE NULL END,
    CASE WHEN e.days_ago < 0 THEN DATE_ADD(@seed_time, INTERVAL (-e.days_ago) DAY)
         ELSE DATE_SUB(@seed_time, INTERVAL e.days_ago DAY) END,
    'REHEALTH_QA_TD_V1',
    CONCAT('qa-td-exec-9102-', e.cat, e.idx, '-', e.revision_no, '-', e.pno, '-', e.tpl),
    @seed_time
FROM (
    -- A 类：o1 completed，o2 skipped（o3 无执行）
    SELECT 'a' cat, 1 idx, 1 revision_no, 1 pno, 1 tpl, 1 days_ago, 'completed' feedback_type, 1.0 score_value
    UNION ALL SELECT 'a', 1, 1, 1, 2, 4, 'skipped', 0.0
    UNION ALL SELECT 'a', 1, 1, 2, 1, 1, 'completed', 1.0 UNION ALL SELECT 'a', 1, 1, 2, 2, 4, 'skipped', 0.0
    UNION ALL SELECT 'a', 1, 1, 3, 1, 1, 'completed', 1.0 UNION ALL SELECT 'a', 1, 1, 3, 2, 4, 'skipped', 0.0
    UNION ALL SELECT 'a', 2, 1, 1, 1, 1, 'completed', 1.0 UNION ALL SELECT 'a', 2, 1, 1, 2, 4, 'skipped', 0.0
    UNION ALL SELECT 'a', 2, 1, 2, 1, 1, 'completed', 1.0 UNION ALL SELECT 'a', 2, 1, 2, 2, 4, 'skipped', 0.0
    UNION ALL SELECT 'a', 2, 1, 3, 1, 1, 'completed', 1.0 UNION ALL SELECT 'a', 2, 1, 3, 2, 4, 'skipped', 0.0
    UNION ALL SELECT 'a', 3, 1, 1, 1, 1, 'completed', 1.0 UNION ALL SELECT 'a', 3, 1, 1, 2, 4, 'skipped', 0.0
    UNION ALL SELECT 'a', 3, 1, 2, 1, 1, 'completed', 1.0 UNION ALL SELECT 'a', 3, 1, 2, 2, 4, 'skipped', 0.0
    UNION ALL SELECT 'a', 3, 1, 3, 1, 1, 'completed', 1.0 UNION ALL SELECT 'a', 3, 1, 3, 2, 4, 'skipped', 0.0
    UNION ALL SELECT 'a', 4, 1, 1, 1, 1, 'completed', 1.0 UNION ALL SELECT 'a', 4, 1, 1, 2, 4, 'skipped', 0.0
    UNION ALL SELECT 'a', 4, 1, 2, 1, 1, 'completed', 1.0 UNION ALL SELECT 'a', 4, 1, 2, 2, 4, 'skipped', 0.0
    UNION ALL SELECT 'a', 4, 1, 3, 1, 1, 'completed', 1.0 UNION ALL SELECT 'a', 4, 1, 3, 2, 4, 'skipped', 0.0
    UNION ALL SELECT 'a', 5, 1, 1, 1, 1, 'completed', 1.0 UNION ALL SELECT 'a', 5, 1, 1, 2, 4, 'skipped', 0.0
    UNION ALL SELECT 'a', 5, 1, 2, 1, 1, 'completed', 1.0 UNION ALL SELECT 'a', 5, 1, 2, 2, 4, 'skipped', 0.0
    UNION ALL SELECT 'a', 5, 1, 3, 1, 1, 'completed', 1.0 UNION ALL SELECT 'a', 5, 1, 3, 2, 4, 'skipped', 0.0
    UNION ALL SELECT 'a', 6, 1, 1, 1, 1, 'completed', 1.0 UNION ALL SELECT 'a', 6, 1, 1, 2, 4, 'skipped', 0.0
    UNION ALL SELECT 'a', 6, 1, 2, 1, 1, 'completed', 1.0 UNION ALL SELECT 'a', 6, 1, 2, 2, 4, 'skipped', 0.0
    UNION ALL SELECT 'a', 6, 1, 3, 1, 1, 'completed', 1.0 UNION ALL SELECT 'a', 6, 1, 3, 2, 4, 'skipped', 0.0
    -- B 类：o1 completed，o2 partially，o3 completed
    UNION ALL SELECT 'b', 1, 1, 1, 1, 1, 'completed', 1.0 UNION ALL SELECT 'b', 1, 1, 1, 2, 4, 'partially_completed', 0.5 UNION ALL SELECT 'b', 1, 1, 1, 3, 7, 'completed', 1.0
    UNION ALL SELECT 'b', 1, 1, 2, 1, 1, 'completed', 1.0 UNION ALL SELECT 'b', 1, 1, 2, 2, 4, 'partially_completed', 0.5 UNION ALL SELECT 'b', 1, 1, 2, 3, 7, 'completed', 1.0
    UNION ALL SELECT 'b', 1, 1, 3, 1, 1, 'completed', 1.0 UNION ALL SELECT 'b', 1, 1, 3, 2, 4, 'partially_completed', 0.5 UNION ALL SELECT 'b', 1, 1, 3, 3, 7, 'completed', 1.0
    UNION ALL SELECT 'b', 2, 1, 1, 1, 1, 'completed', 1.0 UNION ALL SELECT 'b', 2, 1, 1, 2, 4, 'partially_completed', 0.5 UNION ALL SELECT 'b', 2, 1, 1, 3, 7, 'completed', 1.0
    UNION ALL SELECT 'b', 2, 1, 2, 1, 1, 'completed', 1.0 UNION ALL SELECT 'b', 2, 1, 2, 2, 4, 'partially_completed', 0.5 UNION ALL SELECT 'b', 2, 1, 2, 3, 7, 'completed', 1.0
    UNION ALL SELECT 'b', 2, 1, 3, 1, 1, 'completed', 1.0 UNION ALL SELECT 'b', 2, 1, 3, 2, 4, 'partially_completed', 0.5 UNION ALL SELECT 'b', 2, 1, 3, 3, 7, 'completed', 1.0
    UNION ALL SELECT 'b', 3, 1, 1, 1, 1, 'completed', 1.0 UNION ALL SELECT 'b', 3, 1, 1, 2, 4, 'partially_completed', 0.5 UNION ALL SELECT 'b', 3, 1, 1, 3, 7, 'completed', 1.0
    UNION ALL SELECT 'b', 3, 1, 2, 1, 1, 'completed', 1.0 UNION ALL SELECT 'b', 3, 1, 2, 2, 4, 'partially_completed', 0.5 UNION ALL SELECT 'b', 3, 1, 2, 3, 7, 'completed', 1.0
    UNION ALL SELECT 'b', 3, 1, 3, 1, 1, 'completed', 1.0 UNION ALL SELECT 'b', 3, 1, 3, 2, 4, 'partially_completed', 0.5 UNION ALL SELECT 'b', 3, 1, 3, 3, 7, 'completed', 1.0
    UNION ALL SELECT 'b', 4, 1, 1, 1, 1, 'completed', 1.0 UNION ALL SELECT 'b', 4, 1, 1, 2, 4, 'partially_completed', 0.5 UNION ALL SELECT 'b', 4, 1, 1, 3, 7, 'completed', 1.0
    UNION ALL SELECT 'b', 4, 1, 2, 1, 1, 'completed', 1.0 UNION ALL SELECT 'b', 4, 1, 2, 2, 4, 'partially_completed', 0.5 UNION ALL SELECT 'b', 4, 1, 2, 3, 7, 'completed', 1.0
    UNION ALL SELECT 'b', 4, 1, 3, 1, 1, 'completed', 1.0 UNION ALL SELECT 'b', 4, 1, 3, 2, 4, 'partially_completed', 0.5 UNION ALL SELECT 'b', 4, 1, 3, 3, 7, 'completed', 1.0
    UNION ALL SELECT 'b', 5, 1, 1, 1, 1, 'completed', 1.0 UNION ALL SELECT 'b', 5, 1, 1, 2, 4, 'partially_completed', 0.5 UNION ALL SELECT 'b', 5, 1, 1, 3, 7, 'completed', 1.0
    UNION ALL SELECT 'b', 5, 1, 2, 1, 1, 'completed', 1.0 UNION ALL SELECT 'b', 5, 1, 2, 2, 4, 'partially_completed', 0.5 UNION ALL SELECT 'b', 5, 1, 2, 3, 7, 'completed', 1.0
    UNION ALL SELECT 'b', 5, 1, 3, 1, 1, 'completed', 1.0 UNION ALL SELECT 'b', 5, 1, 3, 2, 4, 'partially_completed', 0.5 UNION ALL SELECT 'b', 5, 1, 3, 3, 7, 'completed', 1.0
    UNION ALL SELECT 'b', 6, 1, 1, 1, 1, 'completed', 1.0 UNION ALL SELECT 'b', 6, 1, 1, 2, 4, 'partially_completed', 0.5 UNION ALL SELECT 'b', 6, 1, 1, 3, 7, 'completed', 1.0
    UNION ALL SELECT 'b', 6, 1, 2, 1, 1, 'completed', 1.0 UNION ALL SELECT 'b', 6, 1, 2, 2, 4, 'partially_completed', 0.5 UNION ALL SELECT 'b', 6, 1, 2, 3, 7, 'completed', 1.0
    UNION ALL SELECT 'b', 6, 1, 3, 1, 1, 'completed', 1.0 UNION ALL SELECT 'b', 6, 1, 3, 2, 4, 'partially_completed', 0.5 UNION ALL SELECT 'b', 6, 1, 3, 3, 7, 'completed', 1.0
    -- C 类：o1 completed，o2 partially，o3 not_applicable
    UNION ALL SELECT 'c', 1, 1, 1, 1, 1, 'completed', 1.0 UNION ALL SELECT 'c', 1, 1, 1, 2, 4, 'partially_completed', 0.5 UNION ALL SELECT 'c', 1, 1, 1, 3, 7, 'not_applicable', NULL
    UNION ALL SELECT 'c', 1, 1, 2, 1, 1, 'completed', 1.0 UNION ALL SELECT 'c', 1, 1, 2, 2, 4, 'partially_completed', 0.5 UNION ALL SELECT 'c', 1, 1, 2, 3, 7, 'not_applicable', NULL
    UNION ALL SELECT 'c', 1, 1, 3, 1, 1, 'completed', 1.0 UNION ALL SELECT 'c', 1, 1, 3, 2, 4, 'partially_completed', 0.5 UNION ALL SELECT 'c', 1, 1, 3, 3, 7, 'not_applicable', NULL
    UNION ALL SELECT 'c', 2, 1, 1, 1, 1, 'completed', 1.0 UNION ALL SELECT 'c', 2, 1, 1, 2, 4, 'partially_completed', 0.5 UNION ALL SELECT 'c', 2, 1, 1, 3, 7, 'not_applicable', NULL
    UNION ALL SELECT 'c', 2, 1, 2, 1, 1, 'completed', 1.0 UNION ALL SELECT 'c', 2, 1, 2, 2, 4, 'partially_completed', 0.5 UNION ALL SELECT 'c', 2, 1, 2, 3, 7, 'not_applicable', NULL
    UNION ALL SELECT 'c', 2, 1, 3, 1, 1, 'completed', 1.0 UNION ALL SELECT 'c', 2, 1, 3, 2, 4, 'partially_completed', 0.5 UNION ALL SELECT 'c', 2, 1, 3, 3, 7, 'not_applicable', NULL
    UNION ALL SELECT 'c', 3, 1, 1, 1, 1, 'completed', 1.0 UNION ALL SELECT 'c', 3, 1, 1, 2, 4, 'partially_completed', 0.5 UNION ALL SELECT 'c', 3, 1, 1, 3, 7, 'not_applicable', NULL
    UNION ALL SELECT 'c', 3, 1, 2, 1, 1, 'completed', 1.0 UNION ALL SELECT 'c', 3, 1, 2, 2, 4, 'partially_completed', 0.5 UNION ALL SELECT 'c', 3, 1, 2, 3, 7, 'not_applicable', NULL
    UNION ALL SELECT 'c', 3, 1, 3, 1, 1, 'completed', 1.0 UNION ALL SELECT 'c', 3, 1, 3, 2, 4, 'partially_completed', 0.5 UNION ALL SELECT 'c', 3, 1, 3, 3, 7, 'not_applicable', NULL
    UNION ALL SELECT 'c', 4, 1, 1, 1, 1, 'completed', 1.0 UNION ALL SELECT 'c', 4, 1, 1, 2, 4, 'partially_completed', 0.5 UNION ALL SELECT 'c', 4, 1, 1, 3, 7, 'not_applicable', NULL
    UNION ALL SELECT 'c', 4, 1, 2, 1, 1, 'completed', 1.0 UNION ALL SELECT 'c', 4, 1, 2, 2, 4, 'partially_completed', 0.5 UNION ALL SELECT 'c', 4, 1, 2, 3, 7, 'not_applicable', NULL
    UNION ALL SELECT 'c', 4, 1, 3, 1, 1, 'completed', 1.0 UNION ALL SELECT 'c', 4, 1, 3, 2, 4, 'partially_completed', 0.5 UNION ALL SELECT 'c', 4, 1, 3, 3, 7, 'not_applicable', NULL
    UNION ALL SELECT 'c', 5, 1, 1, 1, 1, 'completed', 1.0 UNION ALL SELECT 'c', 5, 1, 1, 2, 4, 'partially_completed', 0.5 UNION ALL SELECT 'c', 5, 1, 1, 3, 7, 'not_applicable', NULL
    UNION ALL SELECT 'c', 5, 1, 2, 1, 1, 'completed', 1.0 UNION ALL SELECT 'c', 5, 1, 2, 2, 4, 'partially_completed', 0.5 UNION ALL SELECT 'c', 5, 1, 2, 3, 7, 'not_applicable', NULL
    UNION ALL SELECT 'c', 5, 1, 3, 1, 1, 'completed', 1.0 UNION ALL SELECT 'c', 5, 1, 3, 2, 4, 'partially_completed', 0.5 UNION ALL SELECT 'c', 5, 1, 3, 3, 7, 'not_applicable', NULL
    UNION ALL SELECT 'c', 6, 1, 1, 1, 1, 'completed', 1.0 UNION ALL SELECT 'c', 6, 1, 1, 2, 4, 'partially_completed', 0.5 UNION ALL SELECT 'c', 6, 1, 1, 3, 7, 'not_applicable', NULL
    UNION ALL SELECT 'c', 6, 1, 2, 1, 1, 'completed', 1.0 UNION ALL SELECT 'c', 6, 1, 2, 2, 4, 'partially_completed', 0.5 UNION ALL SELECT 'c', 6, 1, 2, 3, 7, 'not_applicable', NULL
    UNION ALL SELECT 'c', 6, 1, 3, 1, 1, 'completed', 1.0 UNION ALL SELECT 'c', 6, 1, 3, 2, 4, 'partially_completed', 0.5 UNION ALL SELECT 'c', 6, 1, 3, 3, 7, 'not_applicable', NULL
    -- D 类：v1 o1/o2/o3 全 completed；v2 o1/o2 completed
    UNION ALL SELECT 'd', 1, 1, 1, 1, 1, 'completed', 1.0 UNION ALL SELECT 'd', 1, 1, 1, 2, 4, 'completed', 1.0 UNION ALL SELECT 'd', 1, 1, 1, 3, 7, 'completed', 1.0
    UNION ALL SELECT 'd', 1, 1, 2, 1, 1, 'completed', 1.0 UNION ALL SELECT 'd', 1, 1, 2, 2, 4, 'completed', 1.0 UNION ALL SELECT 'd', 1, 1, 2, 3, 7, 'completed', 1.0
    UNION ALL SELECT 'd', 1, 1, 3, 1, 1, 'completed', 1.0 UNION ALL SELECT 'd', 1, 1, 3, 2, 4, 'completed', 1.0 UNION ALL SELECT 'd', 1, 1, 3, 3, 7, 'completed', 1.0
    UNION ALL SELECT 'd', 1, 2, 1, 1, 1, 'completed', 1.0 UNION ALL SELECT 'd', 1, 2, 1, 2, 2, 'completed', 1.0
    UNION ALL SELECT 'd', 1, 2, 2, 1, 1, 'completed', 1.0 UNION ALL SELECT 'd', 1, 2, 2, 2, 2, 'completed', 1.0
    UNION ALL SELECT 'd', 1, 2, 3, 1, 1, 'completed', 1.0 UNION ALL SELECT 'd', 1, 2, 3, 2, 2, 'completed', 1.0
    UNION ALL SELECT 'd', 2, 1, 1, 1, 1, 'completed', 1.0 UNION ALL SELECT 'd', 2, 1, 1, 2, 4, 'completed', 1.0 UNION ALL SELECT 'd', 2, 1, 1, 3, 7, 'completed', 1.0
    UNION ALL SELECT 'd', 2, 1, 2, 1, 1, 'completed', 1.0 UNION ALL SELECT 'd', 2, 1, 2, 2, 4, 'completed', 1.0 UNION ALL SELECT 'd', 2, 1, 2, 3, 7, 'completed', 1.0
    UNION ALL SELECT 'd', 2, 1, 3, 1, 1, 'completed', 1.0 UNION ALL SELECT 'd', 2, 1, 3, 2, 4, 'completed', 1.0 UNION ALL SELECT 'd', 2, 1, 3, 3, 7, 'completed', 1.0
    UNION ALL SELECT 'd', 2, 2, 1, 1, 1, 'completed', 1.0 UNION ALL SELECT 'd', 2, 2, 1, 2, 2, 'completed', 1.0
    UNION ALL SELECT 'd', 2, 2, 2, 1, 1, 'completed', 1.0 UNION ALL SELECT 'd', 2, 2, 2, 2, 2, 'completed', 1.0
    UNION ALL SELECT 'd', 2, 2, 3, 1, 1, 'completed', 1.0 UNION ALL SELECT 'd', 2, 2, 3, 2, 2, 'completed', 1.0
    UNION ALL SELECT 'd', 3, 1, 1, 1, 1, 'completed', 1.0 UNION ALL SELECT 'd', 3, 1, 1, 2, 4, 'completed', 1.0 UNION ALL SELECT 'd', 3, 1, 1, 3, 7, 'completed', 1.0
    UNION ALL SELECT 'd', 3, 1, 2, 1, 1, 'completed', 1.0 UNION ALL SELECT 'd', 3, 1, 2, 2, 4, 'completed', 1.0 UNION ALL SELECT 'd', 3, 1, 2, 3, 7, 'completed', 1.0
    UNION ALL SELECT 'd', 3, 1, 3, 1, 1, 'completed', 1.0 UNION ALL SELECT 'd', 3, 1, 3, 2, 4, 'completed', 1.0 UNION ALL SELECT 'd', 3, 1, 3, 3, 7, 'completed', 1.0
    UNION ALL SELECT 'd', 3, 2, 1, 1, 1, 'completed', 1.0 UNION ALL SELECT 'd', 3, 2, 1, 2, 2, 'completed', 1.0
    UNION ALL SELECT 'd', 3, 2, 2, 1, 1, 'completed', 1.0 UNION ALL SELECT 'd', 3, 2, 2, 2, 2, 'completed', 1.0
    UNION ALL SELECT 'd', 3, 2, 3, 1, 1, 'completed', 1.0 UNION ALL SELECT 'd', 3, 2, 3, 2, 2, 'completed', 1.0
    UNION ALL SELECT 'd', 4, 1, 1, 1, 1, 'completed', 1.0 UNION ALL SELECT 'd', 4, 1, 1, 2, 4, 'completed', 1.0 UNION ALL SELECT 'd', 4, 1, 1, 3, 7, 'completed', 1.0
    UNION ALL SELECT 'd', 4, 1, 2, 1, 1, 'completed', 1.0 UNION ALL SELECT 'd', 4, 1, 2, 2, 4, 'completed', 1.0 UNION ALL SELECT 'd', 4, 1, 2, 3, 7, 'completed', 1.0
    UNION ALL SELECT 'd', 4, 1, 3, 1, 1, 'completed', 1.0 UNION ALL SELECT 'd', 4, 1, 3, 2, 4, 'completed', 1.0 UNION ALL SELECT 'd', 4, 1, 3, 3, 7, 'completed', 1.0
    UNION ALL SELECT 'd', 4, 2, 1, 1, 1, 'completed', 1.0 UNION ALL SELECT 'd', 4, 2, 1, 2, 2, 'completed', 1.0
    UNION ALL SELECT 'd', 4, 2, 2, 1, 1, 'completed', 1.0 UNION ALL SELECT 'd', 4, 2, 2, 2, 2, 'completed', 1.0
    UNION ALL SELECT 'd', 4, 2, 3, 1, 1, 'completed', 1.0 UNION ALL SELECT 'd', 4, 2, 3, 2, 2, 'completed', 1.0
    UNION ALL SELECT 'd', 5, 1, 1, 1, 1, 'completed', 1.0 UNION ALL SELECT 'd', 5, 1, 1, 2, 4, 'completed', 1.0 UNION ALL SELECT 'd', 5, 1, 1, 3, 7, 'completed', 1.0
    UNION ALL SELECT 'd', 5, 1, 2, 1, 1, 'completed', 1.0 UNION ALL SELECT 'd', 5, 1, 2, 2, 4, 'completed', 1.0 UNION ALL SELECT 'd', 5, 1, 2, 3, 7, 'completed', 1.0
    UNION ALL SELECT 'd', 5, 1, 3, 1, 1, 'completed', 1.0 UNION ALL SELECT 'd', 5, 1, 3, 2, 4, 'completed', 1.0 UNION ALL SELECT 'd', 5, 1, 3, 3, 7, 'completed', 1.0
    UNION ALL SELECT 'd', 5, 2, 1, 1, 1, 'completed', 1.0 UNION ALL SELECT 'd', 5, 2, 1, 2, 2, 'completed', 1.0
    UNION ALL SELECT 'd', 5, 2, 2, 1, 1, 'completed', 1.0 UNION ALL SELECT 'd', 5, 2, 2, 2, 2, 'completed', 1.0
    UNION ALL SELECT 'd', 5, 2, 3, 1, 1, 'completed', 1.0 UNION ALL SELECT 'd', 5, 2, 3, 2, 2, 'completed', 1.0
    UNION ALL SELECT 'd', 6, 1, 1, 1, 1, 'completed', 1.0 UNION ALL SELECT 'd', 6, 1, 1, 2, 4, 'completed', 1.0 UNION ALL SELECT 'd', 6, 1, 1, 3, 7, 'completed', 1.0
    UNION ALL SELECT 'd', 6, 1, 2, 1, 1, 'completed', 1.0 UNION ALL SELECT 'd', 6, 1, 2, 2, 4, 'completed', 1.0 UNION ALL SELECT 'd', 6, 1, 2, 3, 7, 'completed', 1.0
    UNION ALL SELECT 'd', 6, 1, 3, 1, 1, 'completed', 1.0 UNION ALL SELECT 'd', 6, 1, 3, 2, 4, 'completed', 1.0 UNION ALL SELECT 'd', 6, 1, 3, 3, 7, 'completed', 1.0
    UNION ALL SELECT 'd', 6, 2, 1, 1, 1, 'completed', 1.0 UNION ALL SELECT 'd', 6, 2, 1, 2, 2, 'completed', 1.0
    UNION ALL SELECT 'd', 6, 2, 2, 1, 1, 'completed', 1.0 UNION ALL SELECT 'd', 6, 2, 2, 2, 2, 'completed', 1.0
    UNION ALL SELECT 'd', 6, 2, 3, 1, 1, 'completed', 1.0 UNION ALL SELECT 'd', 6, 2, 3, 2, 2, 'completed', 1.0
) e
ON DUPLICATE KEY UPDATE
    occurrence_id = VALUES(occurrence_id), plan_id = VALUES(plan_id),
    revision_id = VALUES(revision_id), plan_item_id = VALUES(plan_item_id),
    logical_item_id = VALUES(logical_item_id), subject_ref = VALUES(subject_ref),
    feedback_type = VALUES(feedback_type), score_value = VALUES(score_value),
    verification_type = VALUES(verification_type), note = VALUES(note),
    occurred_at = VALUES(occurred_at), created_at = VALUES(created_at);

-- ============================================================================
-- 6. 执行后核对（预期：plan 24 / revision 36 / item 108 /
--    occurrence 288（A 54 + B 54 + C 72 + D 108）/ execution 234）
-- ============================================================================
SELECT 'care_plan' tbl, COUNT(*) cnt FROM rehealth_care_plan WHERE tenant_id = @tenant AND owner_type = 'insurance' AND created_by IN (
    SELECT id FROM sys_user WHERE username IN ('local_ins_9102_mgr_health', 'local_ins_9102_operator')
)
UNION ALL SELECT 'revision', COUNT(*) FROM rehealth_care_plan_revision WHERE tenant_id = @tenant AND created_by IN (
    SELECT id FROM sys_user WHERE username IN ('local_ins_9102_mgr_health', 'local_ins_9102_operator')
)
UNION ALL SELECT 'item', COUNT(*) FROM rehealth_care_plan_item i
    JOIN rehealth_care_plan p ON p.id = i.plan_id AND p.tenant_id = i.tenant_id
    WHERE i.tenant_id = @tenant AND p.owner_type = 'insurance' AND p.created_by IN (
        SELECT id FROM sys_user WHERE username IN ('local_ins_9102_mgr_health', 'local_ins_9102_operator'))
UNION ALL SELECT 'occurrence', COUNT(*) FROM rehealth_care_plan_occurrence o
    JOIN rehealth_care_plan p ON p.id = o.plan_id AND p.tenant_id = o.tenant_id
    WHERE o.tenant_id = @tenant AND p.owner_type = 'insurance' AND p.created_by IN (
        SELECT id FROM sys_user WHERE username IN ('local_ins_9102_mgr_health', 'local_ins_9102_operator'))
UNION ALL SELECT 'execution', COUNT(*) FROM rehealth_care_plan_execution x
    JOIN rehealth_care_plan p ON p.id = x.plan_id AND p.tenant_id = x.tenant_id
    WHERE x.tenant_id = @tenant AND p.owner_type = 'insurance' AND p.created_by IN (
        SELECT id FROM sys_user WHERE username IN ('local_ins_9102_mgr_health', 'local_ins_9102_operator'));

-- ============================================================================
-- 7. 依从性聚合复刻核对（复刻 latestFeedback 版本化聚合 SQL）
--    预期 28 天窗口内：A 类 ~33%、B 类 ~83%、C 类 ~75%（含未来任务不计）、
--    D 类 100%（a04 因取消任务略高，见下）
-- ============================================================================
SELECT plan.subject_ref,
       ROUND(SUM(CASE WHEN x.feedback_type = 'not_applicable' THEN 0
                      ELSE COALESCE(i.scoring_weight, 1) * COALESCE(x.score_value, 0) END)
             / NULLIF(SUM(CASE WHEN x.feedback_type = 'not_applicable' THEN 0
                               ELSE COALESCE(i.scoring_weight, 1) END), 0), 4) AS adherence_28d
FROM rehealth_care_plan_occurrence occ
JOIN rehealth_care_plan plan
  ON plan.tenant_id = occ.tenant_id AND plan.id = occ.plan_id
 AND plan.owner_type = 'insurance' AND plan.subject_ref = occ.subject_ref
JOIN rehealth_care_plan_item i
  ON i.tenant_id = occ.tenant_id AND i.id = occ.plan_item_id
LEFT JOIN rehealth_care_plan_execution x
  ON x.id = (SELECT latest.id FROM rehealth_care_plan_execution latest
             WHERE latest.tenant_id = occ.tenant_id AND latest.occurrence_id = occ.id
             ORDER BY latest.occurred_at DESC, latest.created_at DESC, latest.id DESC LIMIT 1)
WHERE occ.tenant_id = @tenant
  AND occ.status = 'scheduled'
  AND occ.scheduled_at >= TIMESTAMP('2026-07-29 00:00:00')
  AND occ.scheduled_at < TIMESTAMP('2026-08-26 00:00:00')
  AND (occ.due_at <= TIMESTAMP('2026-08-19 09:00:00') OR x.id IS NOT NULL)
GROUP BY plan.subject_ref, plan.id
ORDER BY plan.subject_ref;

-- ============================================================================

-- ============================================================================
-- 清理（按依赖逆序，只删除 REHEALTH_QA_TD_V1 命名空间数据；仅供参考，
-- 正式清理脚本见 99_cleanup_testdata.sql）
-- ============================================================================

-- 来源：第 1 部分（平台用户体系）
-- 清理方式（正式清理脚本见 99_cleanup_testdata.sql；本注释仅供手动清理参考）
-- 删除顺序：关系表 -> 用户 -> 部门 -> 租户，只删除 REHEALTH_QA_TD_V1 命名空间数据：
--
-- DELETE FROM sys_user_role    WHERE user_id IN (SELECT id FROM sys_user WHERE username LIKE 'qa9201\_%' OR username LIKE 'qa9202\_%');
-- DELETE FROM sys_user_depart  WHERE user_id IN (SELECT id FROM sys_user WHERE username LIKE 'qa9201\_%' OR username LIKE 'qa9202\_%');
-- DELETE FROM sys_user_tenant  WHERE user_id IN (SELECT id FROM sys_user WHERE username LIKE 'qa9201\_%' OR username LIKE 'qa9202\_%');
-- DELETE FROM sys_user         WHERE username LIKE 'qa9201\_%' OR username LIKE 'qa9202\_%';
-- DELETE FROM sys_depart       WHERE org_code LIKE 'QA9201%' OR org_code LIKE 'QA9202%';
-- DELETE FROM sys_tenant       WHERE id IN (9201, 9202);
-- ============================================================================

-- 来源：第 2 部分（9202 保险域）
-- 清理方式（正式清理脚本见 99_cleanup_testdata.sql；本注释仅供手动清理参考）
-- 删除顺序：明细/关系 -> 主表 -> 用户/角色关系，只删除 REHEALTH_QA_TD_V1 命名空间：
--
-- DELETE FROM rehealth_insurance_settlement_approval WHERE tenant_id = 9202 AND request_id LIKE 'qa-td-%';
-- DELETE FROM rehealth_insurance_settlement_package  WHERE tenant_id = 9202 AND package_no = 'RH-9202-PKG-0001';
-- DELETE FROM rehealth_insurance_rwe_report       WHERE tenant_id = 9202 AND report_no = 'RH-9202-RWE-0001';
-- DELETE FROM rehealth_insurance_study_result    WHERE tenant_id = 9202 AND created_by = 'qa9202_analyst' AND snapshot_id = LOWER(SHA2('REHEALTH_QA_TD_V1:snapshot:9202:1:1', 256));
-- DELETE FROM rehealth_insurance_study_member    WHERE tenant_id = 9202 AND snapshot_id = LOWER(SHA2('REHEALTH_QA_TD_V1:snapshot:9202:1:1', 256));
-- DELETE FROM rehealth_insurance_study_snapshot  WHERE tenant_id = 9202 AND source_watermark = '2026-08-18T23:59:59Z';
-- DELETE FROM rehealth_insurance_study           WHERE tenant_id = 9202 AND study_no = 'RH-9202-STUDY-0001';
-- DELETE FROM rehealth_insurance_import_batch    WHERE tenant_id = 9202 AND idempotency_key LIKE 'qa-td-%';
-- DELETE FROM rehealth_insurance_audit_event     WHERE tenant_id = 9202 AND request_id LIKE 'qa-td-audit-%';
-- DELETE FROM rehealth_insurance_subject_manager WHERE tenant_id = 9202 AND source_system = 'REHEALTH_QA_TD_V1';
-- DELETE FROM rehealth_insurance_claim           WHERE tenant_id = 9202 AND source_system = 'REHEALTH_QA_TD_V1';
-- DELETE FROM rehealth_insurance_intervention_action       WHERE tenant_id = 9202 AND request_id LIKE 'qa-td-action-%';
-- DELETE FROM rehealth_insurance_intervention_feedback     WHERE tenant_id = 9202 AND source_system = 'REHEALTH_QA_TD_V1';
-- DELETE FROM rehealth_insurance_intervention    WHERE tenant_id = 9202 AND source_system = 'REHEALTH_QA_TD_V1';
-- DELETE FROM rehealth_insurance_plan_binding    WHERE tenant_id = 9202 AND source_system = 'REHEALTH_QA_TD_V1';
-- DELETE FROM rehealth_insurance_consent         WHERE tenant_id = 9202 AND source_system = 'REHEALTH_QA_TD_V1';
-- DELETE FROM rehealth_insurance_coverage        WHERE tenant_id = 9202 AND source_system = 'REHEALTH_QA_TD_V1';
-- DELETE FROM rehealth_insurance_policy          WHERE tenant_id = 9202 AND source_system = 'REHEALTH_QA_TD_V1';
-- DELETE FROM rehealth_insurance_subject         WHERE tenant_id = 9202 AND source_system = 'REHEALTH_QA_TD_V1';
-- DELETE FROM rehealth_insurance_tenant_profile  WHERE tenant_id = 9202 AND organization_name = '睿安演示保险服务（测试）';
-- DELETE FROM rehealth_patient_profile           WHERE id IN (
--     SELECT LOWER(SHA2(CONCAT('REHEALTH_QA_TD_V1:profile:', username), 256))
--     FROM sys_user WHERE username LIKE 'qa9202app%');
-- DELETE ur FROM sys_user_role ur JOIN sys_user u ON u.id = ur.user_id WHERE u.username LIKE 'qa9202app%';
-- DELETE FROM sys_user       WHERE username LIKE 'qa9202app%';
-- ============================================================================

-- 来源：第 3 部分（9102 分类 APP 用户）
-- 清理方式（正式清理脚本见 99_cleanup_testdata.sql；本注释仅供手动清理参考）
-- 只删除 REHEALTH_QA_TD_V1 命名空间与 qa9102app_% 账号相关数据：
--
-- DELETE FROM rehealth_insurance_audit_event            WHERE tenant_id = 9102 AND request_id LIKE 'qa-td-audit-9102-%';
-- DELETE FROM rehealth_insurance_subject_manager        WHERE tenant_id = 9102 AND source_system = 'REHEALTH_QA_TD_V1';
-- DELETE FROM rehealth_insurance_intervention_action    WHERE tenant_id = 9102 AND request_id LIKE 'qa-td-action-9102-%';
-- DELETE FROM rehealth_insurance_intervention_feedback  WHERE tenant_id = 9102 AND source_system = 'REHEALTH_QA_TD_V1';
-- DELETE FROM rehealth_insurance_intervention           WHERE tenant_id = 9102 AND source_system = 'REHEALTH_QA_TD_V1';
-- DELETE FROM rehealth_insurance_plan_binding           WHERE tenant_id = 9102 AND source_system = 'REHEALTH_QA_TD_V1';
-- DELETE FROM rehealth_insurance_consent                WHERE tenant_id = 9102 AND source_system = 'REHEALTH_QA_TD_V1';
-- DELETE FROM rehealth_insurance_coverage               WHERE tenant_id = 9102 AND source_system = 'REHEALTH_QA_TD_V1';
-- DELETE FROM rehealth_insurance_policy                 WHERE tenant_id = 9102 AND source_system = 'REHEALTH_QA_TD_V1';
-- DELETE FROM rehealth_insurance_subject                WHERE tenant_id = 9102 AND source_system = 'REHEALTH_QA_TD_V1';
-- DELETE FROM rehealth_attribution_result   WHERE user_id IN (SELECT id FROM sys_user WHERE username LIKE 'qa9102app\_%');
-- DELETE FROM rehealth_intervention_plan    WHERE user_id IN (SELECT id FROM sys_user WHERE username LIKE 'qa9102app\_%');
-- DELETE FROM rehealth_cvd_risk_result      WHERE user_id IN (SELECT id FROM sys_user WHERE username LIKE 'qa9102app\_%');
-- DELETE FROM rehealth_cvd_feature_vector   WHERE user_id IN (SELECT id FROM sys_user WHERE username LIKE 'qa9102app\_%');
-- DELETE FROM rehealth_rdi_contribution    WHERE snapshot_id IN (SELECT id FROM rehealth_rdi_daily_snapshot WHERE user_id IN (SELECT id FROM sys_user WHERE username LIKE 'qa9102app\_%'));
-- DELETE FROM rehealth_rdi_daily_snapshot   WHERE user_id IN (SELECT id FROM sys_user WHERE username LIKE 'qa9102app\_%');
-- DELETE FROM rehealth_rhi_daily_snapshot   WHERE user_id IN (SELECT id FROM sys_user WHERE username LIKE 'qa9102app\_%');
-- DELETE FROM rehealth_patient_profile      WHERE user_id IN (SELECT id FROM sys_user WHERE username LIKE 'qa9102app\_%');
-- DELETE ur FROM sys_user_role ur JOIN sys_user u ON u.id = ur.user_id WHERE u.username LIKE 'qa9102app\_%';
-- DELETE FROM sys_user                      WHERE username LIKE 'qa9102app\_%';
-- ============================================================================

-- 来源：第 4 部分（版本化关怀计划与依从性）
-- 清理方式（正式清理脚本见 99_cleanup_testdata.sql）
-- 删除顺序：execution -> occurrence -> item -> revision -> plan：
--
-- DELETE FROM rehealth_care_plan_execution  WHERE tenant_id = 9102 AND source_system = 'REHEALTH_QA_TD_V1';
-- DELETE FROM rehealth_care_plan_occurrence WHERE tenant_id = 9102 AND subject_ref LIKE 'QA9102-%';
-- DELETE FROM rehealth_care_plan_item      WHERE tenant_id = 9102 AND plan_id IN (
--     SELECT id FROM rehealth_care_plan WHERE tenant_id = 9102 AND owner_type = 'insurance'
--       AND created_by IN (SELECT id FROM sys_user WHERE username IN ('local_ins_9102_mgr_health','local_ins_9102_operator')));
-- DELETE FROM rehealth_care_plan_revision  WHERE tenant_id = 9102 AND plan_id IN (
--     SELECT id FROM rehealth_care_plan WHERE tenant_id = 9102 AND owner_type = 'insurance'
--       AND created_by IN (SELECT id FROM sys_user WHERE username IN ('local_ins_9102_mgr_health','local_ins_9102_operator')));
-- DELETE FROM rehealth_care_plan           WHERE tenant_id = 9102 AND owner_type = 'insurance'
--   AND created_by IN (SELECT id FROM sys_user WHERE username IN ('local_ins_9102_mgr_health','local_ins_9102_operator'));
-- ============================================================================
