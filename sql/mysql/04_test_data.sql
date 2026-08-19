-- 警告：仅限本地/测试环境。严禁在生产数据库执行。
-- 统一夹具把保险工作流挂到多保险机构脚本创建的 9101 租户，保证可在空库连续执行。
SET NAMES utf8mb4;
SET time_zone = '+00:00';
SET @seed_tenant_id = 9101;
SET @seed_actor = 'local_ins_9101_admin';
SET @anchor_date = DATE('2026-08-19');
SET @seed_time = TIMESTAMP('2026-08-19 09:00:00');

-- ============================================================================
-- 原始来源：backend/deploy/rehealth/scripts/seed-multi-insurer-tenant-test-data.sql
-- ============================================================================
-- Local-only multi-insurer tenant and staff fixtures.
--
-- All organizations and people are synthetic. This script is repeatable and
-- uses stable IDs derived from LOCAL_MULTI_INSURER_QA keys. Never apply it to
-- staging or production, and never contact the synthetic phone/email values.

SET NAMES utf8mb4;

DROP TEMPORARY TABLE IF EXISTS tmp_local_insurer_tenants;
CREATE TEMPORARY TABLE tmp_local_insurer_tenants (
    tenant_id INT NOT NULL PRIMARY KEY,
    tenant_code VARCHAR(16) NOT NULL,
    organization_name VARCHAR(100) NOT NULL,
    license_no VARCHAR(100) NOT NULL,
    company_address VARCHAR(100) NOT NULL
);

INSERT INTO tmp_local_insurer_tenants
    (tenant_id, tenant_code, organization_name, license_no, company_address)
VALUES
    (9101, 'RHAI', '睿安健康保险', 'RH-INS-9101', '上海市浦东新区健康路88号'),
    (9102, 'KTLI', '康泰人寿保险', 'RH-INS-9102', '北京市朝阳区安康路66号'),
    (9103, 'HNPI', '华宁财产保险', 'RH-INS-9103', '深圳市福田区康宁路99号');

DROP TEMPORARY TABLE IF EXISTS tmp_local_insurer_departments;
CREATE TEMPORARY TABLE tmp_local_insurer_departments (
    tenant_id INT NOT NULL,
    department_key VARCHAR(16) NOT NULL,
    parent_key VARCHAR(16) NULL,
    department_name VARCHAR(100) NOT NULL,
    department_order INT NOT NULL,
    org_category VARCHAR(10) NOT NULL,
    org_type VARCHAR(10) NOT NULL,
    PRIMARY KEY (tenant_id, department_key)
);

INSERT INTO tmp_local_insurer_departments
    (tenant_id, department_key, parent_key, department_name, department_order, org_category, org_type)
SELECT tenant_id, 'ROOT', NULL, organization_name, 0, '1', '1'
FROM tmp_local_insurer_tenants;

INSERT INTO tmp_local_insurer_departments
    (tenant_id, department_key, parent_key, department_name, department_order, org_category, org_type)
SELECT tenant_id, 'HEALTH', 'ROOT', '健康险运营部', 1, '2', '2'
FROM tmp_local_insurer_tenants;

INSERT INTO tmp_local_insurer_departments
    (tenant_id, department_key, parent_key, department_name, department_order, org_category, org_type)
SELECT tenant_id, 'RISK', 'ROOT', '精算与风控部', 2, '2', '2'
FROM tmp_local_insurer_tenants;

DROP TEMPORARY TABLE IF EXISTS tmp_local_insurer_people;
CREATE TEMPORARY TABLE tmp_local_insurer_people (
    tenant_id INT NOT NULL,
    username VARCHAR(100) NOT NULL,
    realname VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(45) NOT NULL,
    email VARCHAR(45) NOT NULL,
    sex TINYINT NOT NULL,
    department_key VARCHAR(16) NOT NULL,
    role_code VARCHAR(100) NULL,
    membership_status VARCHAR(1) NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (tenant_id, username)
);

INSERT INTO tmp_local_insurer_people
    (tenant_id, username, realname, password_hash, phone, email, sex,
     department_key, role_code, membership_status, sort_order)
VALUES
    (9101, 'local_ins_9101_admin', '睿安保险·机构管理员', 'b98538e991481b7e09fc63b7d6402441b4f05befdf0ca1e6', '00091010001', 'admin.9101@local.rehealth.invalid', 2, 'ROOT', 'insurance_org_admin', '1', 910101),
    (9101, 'local_ins_9101_mgr_health', '睿安保险·健康险经理', 'b98538e991481b7e5ea89a2ac81c453cb761b6eb7e15d3f407e5bb5f7ba10163', '00091010002', 'health.9101@local.rehealth.invalid', 1, 'HEALTH', 'insurance_department_manager', '1', 910102),
    (9101, 'local_ins_9101_mgr_risk', '睿安保险·风控经理', 'b98538e991481b7e5ea89a2ac81c453c10353854adf0c952', '00091010003', 'risk.9101@local.rehealth.invalid', 2, 'RISK', 'insurance_department_manager', '1', 910103),
    (9101, 'local_ins_9101_analyst', '睿安保险·数据分析员', 'b98538e991481b7e09fc63b7d64024413acd4e9290ebf248', '00091010004', 'analyst.9101@local.rehealth.invalid', 1, 'RISK', 'insurer_analyst', '1', 910104),
    (9101, 'local_ins_9101_operator', '睿安保险·运营专员', 'b98538e991481b7eeb4d6a463803c6831a304657c1e075e5', '00091010005', 'operator.9101@local.rehealth.invalid', 2, 'HEALTH', 'insurance_operator', '1', 910105),
    (9101, 'local_ins_9101_viewer', '睿安保险·只读查看员', 'b98538e991481b7e4415c0445ce9113e35f3e1ab7a146a6a', '00091010006', 'viewer.9101@local.rehealth.invalid', 1, 'ROOT', 'insurer_viewer', '1', 910106),
    (9101, 'local_ins_9101_invitee', '睿安保险·待接受邀请', 'b98538e991481b7e9c6b82422e61265d5ab1f21ef76762ef', '00091010007', 'invite.9101@local.rehealth.invalid', 2, 'HEALTH', NULL, '5', 910107),

    (9102, 'local_ins_9102_admin', '康泰人寿·机构管理员', 'b98538e991481b7ea0f067c07598260d5ffeeef40b28f2b1', '00091020001', 'admin.9102@local.rehealth.invalid', 1, 'ROOT', 'insurance_org_admin', '1', 910201),
    (9102, 'local_ins_9102_mgr_health', '康泰人寿·健康险经理', 'b98538e991481b7e3dd575ea41ee84a9c8d1e2f2c75342a85df8a33adeafda3f', '00091020002', 'health.9102@local.rehealth.invalid', 2, 'HEALTH', 'insurance_department_manager', '1', 910202),
    (9102, 'local_ins_9102_mgr_risk', '康泰人寿·风控经理', 'b98538e991481b7e3dd575ea41ee84a9ba381f821c082f6c', '00091020003', 'risk.9102@local.rehealth.invalid', 1, 'RISK', 'insurance_department_manager', '1', 910203),
    (9102, 'local_ins_9102_analyst', '康泰人寿·数据分析员', 'b98538e991481b7ea0f067c07598260d2cf426d6f8d4400a', '00091020004', 'analyst.9102@local.rehealth.invalid', 2, 'RISK', 'insurer_analyst', '1', 910204),
    (9102, 'local_ins_9102_operator', '康泰人寿·运营专员', 'b98538e991481b7e582489b4abad21fc7148c68d5e44c115', '00091020005', 'operator.9102@local.rehealth.invalid', 1, 'HEALTH', 'insurance_operator', '1', 910205),
    (9102, 'local_ins_9102_viewer', '康泰人寿·只读查看员', 'b98538e991481b7e9d36bfd5c812abb736a26806695fd951', '00091020006', 'viewer.9102@local.rehealth.invalid', 2, 'ROOT', 'insurer_viewer', '1', 910206),
    (9102, 'local_ins_9102_invitee', '康泰人寿·待接受邀请', 'b98538e991481b7eab0a9718efacfa73ee0e589f4446e3d9', '00091020007', 'invite.9102@local.rehealth.invalid', 1, 'HEALTH', NULL, '5', 910207),

    (9103, 'local_ins_9103_admin', '华宁财险·机构管理员', 'b98538e991481b7ebc4e6576606bfe1532d8144ef35a8016', '00091030001', 'admin.9103@local.rehealth.invalid', 2, 'ROOT', 'insurance_org_admin', '1', 910301),
    (9103, 'local_ins_9103_mgr_health', '华宁财险·健康险经理', 'b98538e991481b7e3ae440a8a323ae7df58499bded1c7bf490c0db21842b54df', '00091030002', 'health.9103@local.rehealth.invalid', 1, 'HEALTH', 'insurance_department_manager', '1', 910302),
    (9103, 'local_ins_9103_mgr_risk', '华宁财险·风控经理', 'b98538e991481b7e3ae440a8a323ae7dd16d72a56ad0f601', '00091030003', 'risk.9103@local.rehealth.invalid', 2, 'RISK', 'insurance_department_manager', '1', 910303),
    (9103, 'local_ins_9103_analyst', '华宁财险·数据分析员', 'b98538e991481b7ebc4e6576606bfe15429e69a63168b353', '00091030004', 'analyst.9103@local.rehealth.invalid', 1, 'RISK', 'insurer_analyst', '1', 910304),
    (9103, 'local_ins_9103_operator', '华宁财险·运营专员', 'b98538e991481b7ee722de8c48449a2728db50b083192a56', '00091030005', 'operator.9103@local.rehealth.invalid', 2, 'HEALTH', 'insurance_operator', '1', 910305),
    (9103, 'local_ins_9103_viewer', '华宁财险·只读查看员', 'b98538e991481b7e044b5d6e28237150edf155be62ba9948', '00091030006', 'viewer.9103@local.rehealth.invalid', 1, 'ROOT', 'insurer_viewer', '1', 910306),
    (9103, 'local_ins_9103_invitee', '华宁财险·待接受邀请', 'b98538e991481b7e3270657057b15629794460bd2266df28', '00091030007', 'invite.9103@local.rehealth.invalid', 2, 'HEALTH', NULL, '5', 910307),

    (9101, 'local_ins_shared_auditor', '三机构共享合规审计员', 'b98538e991481b7eb7a1c587ac766f293752cd4e0e24dd29ee5570bd41de73c8', '00091990001', 'auditor.shared@local.rehealth.invalid', 2, 'ROOT', 'insurer_auditor', '1', 919901),
    (9102, 'local_ins_shared_auditor', '三机构共享合规审计员', 'b98538e991481b7eb7a1c587ac766f293752cd4e0e24dd29ee5570bd41de73c8', '00091990001', 'auditor.shared@local.rehealth.invalid', 2, 'ROOT', 'insurer_auditor', '1', 919901),
    (9103, 'local_ins_shared_auditor', '三机构共享合规审计员', 'b98538e991481b7eb7a1c587ac766f293752cd4e0e24dd29ee5570bd41de73c8', '00091990001', 'auditor.shared@local.rehealth.invalid', 2, 'ROOT', 'insurer_auditor', '1', 919901);

-- Use natural-looking names in the UI while stable LOCAL QA usernames,
-- reserved .invalid email addresses, and non-routable phones keep the
-- fixtures unmistakably non-production.
UPDATE tmp_local_insurer_people
SET realname = CASE username
    WHEN 'local_ins_9101_admin' THEN '林书瑶'
    WHEN 'local_ins_9101_mgr_health' THEN '周启明'
    WHEN 'local_ins_9101_mgr_risk' THEN '宋雨桐'
    WHEN 'local_ins_9101_analyst' THEN '陈一帆'
    WHEN 'local_ins_9101_operator' THEN '何静'
    WHEN 'local_ins_9101_viewer' THEN '罗文博'
    WHEN 'local_ins_9101_invitee' THEN '郑欣怡'
    WHEN 'local_ins_9102_admin' THEN '王景川'
    WHEN 'local_ins_9102_mgr_health' THEN '赵文静'
    WHEN 'local_ins_9102_mgr_risk' THEN '孙浩然'
    WHEN 'local_ins_9102_analyst' THEN '刘思齐'
    WHEN 'local_ins_9102_operator' THEN '蒋婉清'
    WHEN 'local_ins_9102_viewer' THEN '杜嘉诚'
    WHEN 'local_ins_9102_invitee' THEN '方雅雯'
    WHEN 'local_ins_9103_admin' THEN '许安然'
    WHEN 'local_ins_9103_mgr_health' THEN '顾承泽'
    WHEN 'local_ins_9103_mgr_risk' THEN '唐敏'
    WHEN 'local_ins_9103_analyst' THEN '梁知行'
    WHEN 'local_ins_9103_operator' THEN '邱若琳'
    WHEN 'local_ins_9103_viewer' THEN '韩泽宇'
    WHEN 'local_ins_9103_invitee' THEN '姚清宁'
    WHEN 'local_ins_shared_auditor' THEN '秦悦'
    ELSE realname
END;

START TRANSACTION;

INSERT INTO sys_tenant (
    id, name, create_time, create_by, begin_date, end_date, status, trade,
    company_size, company_address, company_logo, house_number, work_place,
    secondary_domain, login_bkgd_img, position, department, del_flag,
    update_by, update_time, apply_status
)
SELECT tenant_id, organization_name, @seed_time, @seed_actor, @seed_time,
       TIMESTAMP('2099-12-31 23:59:59'), 1, '6', '3', company_address,
       NULL, CONCAT('L', tenant_id), company_address, NULL, NULL, NULL, NULL,
       0, @seed_actor, @seed_time, 0
FROM tmp_local_insurer_tenants
ON DUPLICATE KEY UPDATE
    name = VALUES(name), status = 1, trade = VALUES(trade),
    company_size = VALUES(company_size), company_address = VALUES(company_address),
    del_flag = 0, update_by = VALUES(update_by), update_time = VALUES(update_time);

-- Allow the local QA actor (admin by default) to switch into each seeded
-- tenant from the Jeecg tenant selector and inspect the tenant-filtered
-- department management page. Keep the actor's existing login_tenant_id.
UPDATE sys_user_tenant membership
JOIN sys_user actor ON actor.id = membership.user_id
JOIN tmp_local_insurer_tenants tenant ON tenant.tenant_id = membership.tenant_id
SET membership.status = '1',
    membership.update_by = @seed_actor,
    membership.update_time = @seed_time
WHERE actor.username = @seed_actor;

INSERT INTO sys_user_tenant (
    id, user_id, tenant_id, status, create_by, create_time, update_by, update_time
)
SELECT LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_QA:actor-membership:', tenant.tenant_id, ':', @seed_actor))),
       actor.id, tenant.tenant_id, '1', @seed_actor, @seed_time, @seed_actor, @seed_time
FROM tmp_local_insurer_tenants tenant
JOIN sys_user actor ON actor.username = @seed_actor AND actor.del_flag = 0
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_user_tenant existing
    WHERE existing.user_id = actor.id
      AND existing.tenant_id = tenant.tenant_id
);

INSERT INTO rehealth_insurance_tenant_profile (
    id, tenant_id, organization_name, license_no, insurance_type,
    compliance_email, regulatory_email, data_retention_years,
    mask_sensitive_data, access_log_enabled, notification_config_json,
    version, created_at, updated_at
)
SELECT LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_QA:profile:', tenant_id))),
       tenant_id, organization_name, license_no, 'mixed',
       CONCAT('compliance.', tenant_id, '@local.rehealth.invalid'),
       CONCAT('regulatory.', tenant_id, '@local.rehealth.invalid'),
       7, 1, 1, JSON_OBJECT('fixture', TRUE, 'source', 'LOCAL_MULTI_INSURER_QA'),
       1, @seed_time, @seed_time
FROM tmp_local_insurer_tenants
ON DUPLICATE KEY UPDATE
    organization_name = VALUES(organization_name), license_no = VALUES(license_no),
    insurance_type = VALUES(insurance_type), compliance_email = VALUES(compliance_email),
    regulatory_email = VALUES(regulatory_email), version = VALUES(version),
    updated_at = VALUES(updated_at);

INSERT INTO sys_depart (
    id, parent_id, depart_name, depart_order, org_category, org_type,
    org_code, description, status, del_flag, create_by, create_time,
    update_by, update_time, tenant_id, iz_leaf
)
SELECT LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_QA:department:', tenant_id, ':', department_key))),
       CASE WHEN parent_key IS NULL THEN NULL ELSE LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_QA:department:', tenant_id, ':', parent_key))) END,
       department_name, department_order, org_category, org_type,
       CONCAT('RH', tenant_id, department_key),
       'LOCAL_MULTI_INSURER_QA synthetic organization data', '1', '0',
       @seed_actor, @seed_time, @seed_actor, @seed_time, tenant_id,
       CASE WHEN department_key IN ('HEALTH', 'RISK') THEN 1 ELSE 0 END
FROM tmp_local_insurer_departments
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id), depart_name = VALUES(depart_name),
    depart_order = VALUES(depart_order), org_category = VALUES(org_category),
    org_type = VALUES(org_type), description = VALUES(description), status = '1',
    del_flag = '0', update_by = VALUES(update_by), update_time = VALUES(update_time),
    tenant_id = VALUES(tenant_id), iz_leaf = VALUES(iz_leaf);

-- The QA actor is a real tenant member while inspecting the seeded organizations,
-- so keep its user-management department projection complete as well. Do not
-- replace any existing relationship; only add the current tenant's root node.
INSERT INTO sys_user_depart (ID, user_id, dep_id)
SELECT LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_QA:actor-department:', tenant.tenant_id, ':', @seed_actor))),
       actor.id,
       LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_QA:department:', tenant.tenant_id, ':ROOT')))
FROM tmp_local_insurer_tenants tenant
JOIN sys_user actor ON actor.username = @seed_actor AND actor.del_flag = 0
WHERE NOT EXISTS (
    SELECT 1
    FROM sys_user_depart existing
    WHERE existing.user_id = actor.id
      AND existing.dep_id = LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_QA:department:', tenant.tenant_id, ':ROOT')))
);

INSERT INTO sys_user (
    id, username, realname, password, salt, sex, email, phone, org_code,
    status, del_flag, activiti_sync, work_no, create_by, create_time,
    update_by, update_time, user_identity, login_tenant_id, sort,
    iz_hide_contact
)
SELECT LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_QA:user:', username))),
       username, MAX(realname), MAX(password_hash), 'LQA26081', MAX(sex),
       MAX(email), MAX(phone), NULL, 1, 0, 0,
       CONCAT('INS-', REPLACE(username, 'local_ins_', '')),
       @seed_actor, @seed_time, @seed_actor, @seed_time, 1,
       MIN(tenant_id), MAX(sort_order), '0'
FROM tmp_local_insurer_people
GROUP BY username
ON DUPLICATE KEY UPDATE
    realname = VALUES(realname), password = VALUES(password), salt = VALUES(salt),
    sex = VALUES(sex), email = VALUES(email), phone = VALUES(phone), status = 1,
    del_flag = 0, update_by = VALUES(update_by), update_time = VALUES(update_time),
    login_tenant_id = VALUES(login_tenant_id), sort = VALUES(sort);

INSERT INTO sys_user_tenant (
    id, user_id, tenant_id, status, create_by, create_time, update_by, update_time
)
SELECT LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_QA:membership:', tenant_id, ':', username))),
       LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_QA:user:', username))),
       tenant_id, membership_status, @seed_actor, @seed_time, @seed_actor, @seed_time
FROM tmp_local_insurer_people
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id), tenant_id = VALUES(tenant_id), status = VALUES(status),
    update_by = VALUES(update_by), update_time = VALUES(update_time);

INSERT INTO sys_user_depart (ID, user_id, dep_id)
SELECT LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_QA:user-department:', tenant_id, ':', username))),
       LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_QA:user:', username))),
       LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_QA:department:', tenant_id, ':', department_key)))
FROM tmp_local_insurer_people
ON DUPLICATE KEY UPDATE dep_id = VALUES(dep_id);

INSERT INTO sys_user_role (id, user_id, role_id, tenant_id)
SELECT LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_QA:user-role:', people.tenant_id, ':', people.username, ':', people.role_code))),
       LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_QA:user:', people.username))),
       role.id, people.tenant_id
FROM tmp_local_insurer_people people
JOIN sys_role role ON role.role_code = people.role_code
WHERE people.role_code IS NOT NULL
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id), role_id = VALUES(role_id), tenant_id = VALUES(tenant_id);

COMMIT;

-- ============================================================================
-- 原始来源：backend/deploy/rehealth/scripts/seed-multi-insurer-app-user-test-data.sql
-- ============================================================================
-- Local-only complete APP-user fixtures for the three LOCAL_MULTI_INSURER_QA tenants.
--
-- This script deliberately reuses sys_user/sys_role plus the existing insurance
-- subject, policy, consent, plan, manager, and audit tables. APP users are NOT
-- inserted into sys_user_tenant or sys_user_depart. Every row is synthetic,
-- non-clinical, repeatable, and forbidden outside local development. Each
-- insurer receives 12 subjects so all four workbench workflow states have at
-- least three rows; each subject detail receives at least three display items.

SET NAMES utf8mb4;
SET @anchor_date = COALESCE(@anchor_date, CURRENT_DATE());
SET @seed_time = COALESCE(@seed_time, TIMESTAMP(@anchor_date, '10:00:00'));
SET @seed_actor = COALESCE(@seed_actor, 'admin');

DROP TEMPORARY TABLE IF EXISTS tmp_miqa_app_user;
CREATE TEMPORARY TABLE tmp_miqa_app_user (
    profile_no INT NOT NULL PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    realname VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    phone VARCHAR(45) NOT NULL UNIQUE,
    email VARCHAR(45) NOT NULL UNIQUE,
    sex TINYINT NOT NULL,
    gender VARCHAR(16) NOT NULL,
    age SMALLINT NOT NULL,
    height_cm DECIMAL(6,2) NOT NULL,
    weight_kg DECIMAL(6,2) NOT NULL,
    bmi DECIMAL(5,2) NOT NULL,
    risk_base DECIMAL(8,6) NOT NULL
);

INSERT INTO tmp_miqa_app_user (
    profile_no, username, realname, password_hash, phone, email, sex, gender,
    age, height_cm, weight_kg, bmi, risk_base
) VALUES
    (1,  'local_app_9101_01', '张明远', '1c8b16ac552e6afbdf0373b1b0f4e86cf50be5a6147a4979', '00092000001', 'mingyuan.zhang@app.qa.invalid', 1, 'male',   44, 175.00, 70.10, 22.89, 0.260000),
    (2,  'local_app_9101_02', '李慧敏', '1c8b16ac552e6afbdf0373b1b0f4e86c6977237059d17f4b', '00092000002', 'huimin.li@app.qa.invalid', 2, 'female', 49, 163.00, 61.20, 23.03, 0.350000),
    (3,  'local_app_9101_03', '王建国', '1c8b16ac552e6afbdf0373b1b0f4e86cb37a058454ba5dfc', '00092000003', 'jianguo.wang@app.qa.invalid', 1, 'male',   54, 172.00, 78.40, 26.50, 0.480000),
    (4,  'local_app_9101_04', '陈玉兰', '1c8b16ac552e6afbdf0373b1b0f4e86ce5dc1f0b0f86675d', '00092000004', 'yulan.chen@app.qa.invalid', 2, 'female', 59, 160.00, 71.00, 27.73, 0.700000),
    (5,  'local_app_9102_01', '刘志强', '1c8b16ac552e6afb172c2f8c31ad5b828d2bd45da9912a9b', '00092000005', 'zhiqiang.liu@app.qa.invalid', 1, 'male',   42, 176.00, 68.30, 22.05, 0.230000),
    (6,  'local_app_9102_02', '周婉婷', '1c8b16ac552e6afb172c2f8c31ad5b824563e9e55a9d952a', '00092000006', 'wanting.zhou@app.qa.invalid', 2, 'female', 47, 164.00, 64.70, 24.06, 0.330000),
    (7,  'local_app_9102_03', '赵国庆', '1c8b16ac552e6afb172c2f8c31ad5b829ef8c9b6a63f3755', '00092000007', 'guoqing.zhao@app.qa.invalid', 1, 'male',   55, 171.00, 78.90, 26.98, 0.520000),
    (8,  'local_app_9102_04', '孙晓梅', '1c8b16ac552e6afb172c2f8c31ad5b8241d66a504bcaeb62', '00092000008', 'xiaomei.sun@app.qa.invalid', 2, 'female', 62, 160.00, 74.60, 29.14, 0.720000),
    (9,  'local_app_9103_01', '吴志远', '1c8b16ac552e6afbabb9b09bda7b8fe85ecc547f487663f0', '00092000009', 'zhiyuan.wu@app.qa.invalid', 1, 'male',   45, 176.00, 71.60, 23.11, 0.280000),
    (10, 'local_app_9103_02', '郑丽华', '1c8b16ac552e6afbabb9b09bda7b8fe8a43c32a4e082f84a', '00092000010', 'lihua.zheng@app.qa.invalid', 2, 'female', 50, 164.00, 63.50, 23.61, 0.390000),
    (11, 'local_app_9103_03', '胡建新', '1c8b16ac552e6afbabb9b09bda7b8fe8899a1524a55b1110', '00092000011', 'jianxin.hu@app.qa.invalid', 1, 'male',   57, 173.00, 81.20, 27.13, 0.580000),
    (12, 'local_app_9103_04', '林秀珍', '1c8b16ac552e6afbabb9b09bda7b8fe86471d82a6d9bf4d9', '00092000012', 'xiuzhen.lin@app.qa.invalid', 2, 'female', 64, 159.00, 75.20, 29.75, 0.760000),
    (13, 'local_app_shared_01', '何俊杰', '1c8b16ac552e6afb4efaae7ea43c0964991df824007b1fde', '00092000013', 'junjie.he@app.qa.invalid', 1, 'male',   50, 175.00, 68.60, 22.40, 0.310000),
    (14, 'local_app_shared_02', '高雅琴', '1c8b16ac552e6afb4efaae7ea43c09642902a24009a903e4', '00092000014', 'yaqin.gao@app.qa.invalid', 2, 'female', 53, 162.00, 66.80, 25.45, 0.460000);

DROP TEMPORARY TABLE IF EXISTS tmp_miqa_app_relationship;
CREATE TEMPORARY TABLE tmp_miqa_app_relationship (
    tenant_id INT NOT NULL,
    member_no INT NOT NULL,
    username VARCHAR(100) NOT NULL,
    PRIMARY KEY (tenant_id, member_no),
    UNIQUE KEY uk_miqa_relationship_user (tenant_id, username)
);

INSERT INTO tmp_miqa_app_relationship (tenant_id, member_no, username) VALUES
    (9101, 1, 'local_app_9101_01'), (9101, 2, 'local_app_9101_02'),
    (9101, 3, 'local_app_9101_03'), (9101, 4, 'local_app_9101_04'),
    (9101, 5, 'local_app_shared_01'), (9101, 6, 'local_app_shared_02'),
    (9101, 7, 'local_app_9102_04'), (9101, 8, 'local_app_9103_04'),
    (9101, 9, 'local_app_9102_03'), (9101, 10, 'local_app_9103_03'),
    (9101, 11, 'local_app_9102_02'), (9101, 12, 'local_app_9103_02'),
    (9102, 1, 'local_app_9102_01'), (9102, 2, 'local_app_9102_02'),
    (9102, 3, 'local_app_9102_03'), (9102, 4, 'local_app_9102_04'),
    (9102, 5, 'local_app_shared_01'), (9102, 6, 'local_app_shared_02'),
    (9102, 7, 'local_app_9101_04'), (9102, 8, 'local_app_9103_04'),
    (9102, 9, 'local_app_9101_03'), (9102, 10, 'local_app_9103_03'),
    (9102, 11, 'local_app_9101_02'), (9102, 12, 'local_app_9103_02'),
    (9103, 1, 'local_app_9103_01'), (9103, 2, 'local_app_9103_02'),
    (9103, 3, 'local_app_9103_03'), (9103, 4, 'local_app_9103_04'),
    (9103, 5, 'local_app_shared_01'), (9103, 6, 'local_app_shared_02'),
    (9103, 7, 'local_app_9101_04'), (9103, 8, 'local_app_9102_04'),
    (9103, 9, 'local_app_9101_03'), (9103, 10, 'local_app_9102_03'),
    (9103, 11, 'local_app_9101_02'), (9103, 12, 'local_app_9102_02');

DROP TEMPORARY TABLE IF EXISTS tmp_miqa_risk_day;
CREATE TEMPORARY TABLE tmp_miqa_risk_day (days_ago INT NOT NULL PRIMARY KEY);
INSERT INTO tmp_miqa_risk_day (days_ago) VALUES
    (0),(1),(2),(3),(4),(5),(6),(7),(8),(9),
    (10),(11),(12),(13),(14),(15),(16),(17),(18),(19),
    (20),(21),(22),(23),(24),(25),(26),(27),(28),(29);

DROP TEMPORARY TABLE IF EXISTS tmp_miqa_assignment;
CREATE TEMPORARY TABLE tmp_miqa_assignment (
    tenant_id INT NOT NULL,
    staff_username VARCHAR(100) NOT NULL,
    member_no INT NOT NULL,
    PRIMARY KEY (tenant_id, staff_username, member_no)
);

INSERT INTO tmp_miqa_assignment (tenant_id, staff_username, member_no)
SELECT tenant_id, CONCAT('local_ins_', tenant_id, '_admin'), member_no
FROM (SELECT 9101 tenant_id UNION ALL SELECT 9102 UNION ALL SELECT 9103) tenants
JOIN (
    SELECT 1 member_no UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 8
    UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12
) members
UNION ALL
SELECT tenant_id, CONCAT('local_ins_', tenant_id, '_mgr_health'), member_no
FROM (SELECT 9101 tenant_id UNION ALL SELECT 9102 UNION ALL SELECT 9103) tenants
JOIN (SELECT 1 member_no UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4 UNION ALL SELECT 5 UNION ALL SELECT 6) members
UNION ALL
SELECT tenant_id, CONCAT('local_ins_', tenant_id, '_mgr_risk'), member_no
FROM (SELECT 9101 tenant_id UNION ALL SELECT 9102 UNION ALL SELECT 9103) tenants
JOIN (SELECT 7 member_no UNION ALL SELECT 8 UNION ALL SELECT 9 UNION ALL SELECT 10 UNION ALL SELECT 11 UNION ALL SELECT 12) members
UNION ALL
SELECT tenant_id, CONCAT('local_ins_', tenant_id, '_analyst'), member_no
FROM (SELECT 9101 tenant_id UNION ALL SELECT 9102 UNION ALL SELECT 9103) tenants
JOIN (SELECT 1 member_no UNION ALL SELECT 4 UNION ALL SELECT 7 UNION ALL SELECT 10) members
UNION ALL
SELECT tenant_id, CONCAT('local_ins_', tenant_id, '_operator'), member_no
FROM (SELECT 9101 tenant_id UNION ALL SELECT 9102 UNION ALL SELECT 9103) tenants
JOIN (SELECT 2 member_no UNION ALL SELECT 5 UNION ALL SELECT 8 UNION ALL SELECT 11) members
UNION ALL
SELECT tenant_id, CONCAT('local_ins_', tenant_id, '_viewer'), member_no
FROM (SELECT 9101 tenant_id UNION ALL SELECT 9102 UNION ALL SELECT 9103) tenants
JOIN (SELECT 3 member_no UNION ALL SELECT 6 UNION ALL SELECT 9 UNION ALL SELECT 12) members
UNION ALL
SELECT tenant_id, 'local_ins_shared_auditor', member_no
FROM (SELECT 9101 tenant_id UNION ALL SELECT 9102 UNION ALL SELECT 9103) tenants
JOIN (SELECT 1 member_no UNION ALL SELECT 6 UNION ALL SELECT 7 UNION ALL SELECT 12) members;

START TRANSACTION;

INSERT INTO sys_role (id, role_name, role_code, description, create_by, create_time, update_by, update_time, tenant_id)
VALUES
    (LOWER(MD5('LOCAL_MULTI_INSURER_APP_QA:role:app_user')), 'APP 用户', 'app_user',
     'LOCAL_MULTI_INSURER_APP_QA platform APP classification only', @seed_actor, @seed_time, @seed_actor, @seed_time, 0),
    (LOWER(MD5('LOCAL_MULTI_INSURER_APP_QA:role:insurance_service_user')), '保险服务用户', 'insurance_service_user',
     'LOCAL_MULTI_INSURER_APP_QA derived insurance-service classification only', @seed_actor, @seed_time, @seed_actor, @seed_time, 0)
ON DUPLICATE KEY UPDATE
    role_name = VALUES(role_name), description = VALUES(description),
    update_by = VALUES(update_by), update_time = VALUES(update_time);

INSERT INTO sys_user (
    id, username, realname, password, salt, birthday, sex, email, phone,
    status, del_flag, activiti_sync, work_no, create_by, create_time,
    update_by, update_time, user_identity, login_tenant_id, sort, iz_hide_contact
)
SELECT
    LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_APP_QA:user:', username))),
    username, realname, password_hash, 'LQA26081',
    DATE_SUB(@anchor_date, INTERVAL age YEAR), sex, email, phone,
    1, 0, 0, CONCAT('APP-', LPAD(profile_no, 6, '0')),
    @seed_actor, @seed_time, @seed_actor, @seed_time, 1, 0,
    920000 + profile_no, '0'
FROM tmp_miqa_app_user
ON DUPLICATE KEY UPDATE
    realname = VALUES(realname), password = VALUES(password), salt = VALUES(salt),
    birthday = VALUES(birthday), sex = VALUES(sex), email = VALUES(email),
    phone = VALUES(phone), status = 1, del_flag = 0,
    work_no = VALUES(work_no),
    update_by = VALUES(update_by), update_time = VALUES(update_time),
    login_tenant_id = 0, sort = VALUES(sort), iz_hide_contact = '0';

-- These seed-owned APP accounts model service recipients, never institution staff.
DELETE membership
FROM sys_user_tenant membership
JOIN tmp_miqa_app_user app
  ON membership.user_id = LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_APP_QA:user:', app.username)));

DELETE user_department
FROM sys_user_depart user_department
JOIN tmp_miqa_app_user app
  ON user_department.user_id = LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_APP_QA:user:', app.username)));

DELETE user_role
FROM sys_user_role user_role
JOIN tmp_miqa_app_user app
  ON user_role.user_id = LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_APP_QA:user:', app.username)))
WHERE user_role.tenant_id <> 0;

INSERT INTO sys_user_role (id, user_id, role_id, tenant_id)
SELECT
    LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_APP_QA:user-role:', app.username, ':', role.role_code))),
    LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_APP_QA:user:', app.username))),
    role.id, 0
FROM tmp_miqa_app_user app
JOIN sys_role role ON role.role_code IN ('app_user', 'insurance_service_user')
WHERE 1 = 1
ON DUPLICATE KEY UPDATE
    user_id = VALUES(user_id), role_id = VALUES(role_id), tenant_id = 0;

INSERT INTO rehealth_patient_profile (
    id, user_id, name, gender, age, height_cm, weight_kg, bmi,
    family_history, smoking, drinking, diabetes_history, hypertension_history,
    profile_version, profile_json, created_at, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:profile:', username), 256)),
    LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_APP_QA:user:', username))),
    realname, gender, age, height_cm, weight_kg, bmi,
    IF(risk_base >= 0.45, 1, 0), IF(risk_base >= 0.65, 1, 0),
    IF(profile_no % 4 = 0, 1, 0), IF(risk_base >= 0.70, 1, 0),
    IF(risk_base >= 0.50, 1, 0), 1,
    JSON_OBJECT(
        'source', 'LOCAL_MULTI_INSURER_APP_QA',
        'scenario', 'android_debug_full_chain_complete_user',
        'synthetic', TRUE, 'clinicalUseAllowed', FALSE
    ), @seed_time, @seed_time
FROM tmp_miqa_app_user
ON DUPLICATE KEY UPDATE
    name = VALUES(name), gender = VALUES(gender), age = VALUES(age),
    height_cm = VALUES(height_cm), weight_kg = VALUES(weight_kg), bmi = VALUES(bmi),
    family_history = VALUES(family_history), smoking = VALUES(smoking),
    drinking = VALUES(drinking), diabetes_history = VALUES(diabetes_history),
    hypertension_history = VALUES(hypertension_history),
    profile_json = VALUES(profile_json), updated_at = VALUES(updated_at);

INSERT INTO rehealth_rhi_manual_health_input (
    user_id, sedentary_hours_per_day, waist_circumference_cm, vo2_max_ml_kg_min,
    hba1c_percent, egfr_ml_min_1_73m2, cuff_sbp_7d_mean, cuff_dbp_7d_mean,
    cuff_valid_days, cuff_confirmed, fasting_glucose_mmol_l,
    total_cholesterol_mmol_l, ldl_mmol_l, hdl_mmol_l, triglycerides_mmol_l,
    lab_confirmed, lab_recorded_at, client_updated_at, created_at, updated_at
)
SELECT
    LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_APP_QA:user:', username))),
    ROUND(5.5 + risk_base * 3.0, 2), ROUND(78 + risk_base * 20, 2),
    ROUND(42 - risk_base * 12, 2), ROUND(5.0 + risk_base * 1.8, 2),
    ROUND(105 - risk_base * 28, 2), ROUND(112 + risk_base * 28, 2),
    ROUND(72 + risk_base * 14, 2), 7, 1,
    ROUND(4.7 + risk_base * 2.0, 3), ROUND(4.1 + risk_base * 1.6, 3),
    ROUND(2.1 + risk_base * 1.8, 3), ROUND(1.55 - risk_base * 0.45, 3),
    ROUND(0.9 + risk_base * 1.7, 3), 1,
    UNIX_TIMESTAMP(@anchor_date) * 1000, UNIX_TIMESTAMP(@seed_time) * 1000,
    @seed_time, @seed_time
FROM tmp_miqa_app_user
ON DUPLICATE KEY UPDATE
    sedentary_hours_per_day = VALUES(sedentary_hours_per_day),
    waist_circumference_cm = VALUES(waist_circumference_cm),
    vo2_max_ml_kg_min = VALUES(vo2_max_ml_kg_min), hba1c_percent = VALUES(hba1c_percent),
    egfr_ml_min_1_73m2 = VALUES(egfr_ml_min_1_73m2),
    cuff_sbp_7d_mean = VALUES(cuff_sbp_7d_mean), cuff_dbp_7d_mean = VALUES(cuff_dbp_7d_mean),
    cuff_valid_days = 7, cuff_confirmed = 1,
    fasting_glucose_mmol_l = VALUES(fasting_glucose_mmol_l),
    total_cholesterol_mmol_l = VALUES(total_cholesterol_mmol_l),
    ldl_mmol_l = VALUES(ldl_mmol_l), hdl_mmol_l = VALUES(hdl_mmol_l),
    triglycerides_mmol_l = VALUES(triglycerides_mmol_l), lab_confirmed = 1,
    lab_recorded_at = VALUES(lab_recorded_at), client_updated_at = VALUES(client_updated_at),
    updated_at = VALUES(updated_at);

INSERT INTO rehealth_device_binding (
    id, user_id, device_id, device_name, manufacturer, device_model, model,
    firmware_version, hardware_address_hash, status, bound_at, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:device-binding:', username), 256)),
    LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_APP_QA:user:', username))),
    CONCAT('miqa-device-', username), '睿禾智能健康戒指', 'ReHealth',
    'RH-RING-50M', 'RH-RING-50M', '1.0.0',
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:device-address:', username), 256)),
    'BOUND', @seed_time, @seed_time
FROM tmp_miqa_app_user
ON DUPLICATE KEY UPDATE
    device_name = VALUES(device_name), manufacturer = VALUES(manufacturer),
    device_model = VALUES(device_model), model = VALUES(model),
    firmware_version = VALUES(firmware_version),
    hardware_address_hash = VALUES(hardware_address_hash), status = 'BOUND',
    updated_at = VALUES(updated_at);

INSERT INTO rehealth_health_interview (
    id, user_id, generated_at, answers_json, baseline_json, created_at
)
SELECT
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:interview:', username), 256)),
    LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_APP_QA:user:', username))),
    @seed_time,
    JSON_ARRAY(
        JSON_OBJECT('topic', 'goal', 'content', '希望改善睡眠、活动和心血管风险趋势'),
        JSON_OBJECT('topic', 'routine', 'content', '每日佩戴设备并完成健康管理任务'),
        JSON_OBJECT('topic', 'safety', 'content', '暂无明确诊断或用药信息')
    ),
    JSON_ARRAY(
        JSON_OBJECT('label', '年龄', 'value', age),
        JSON_OBJECT('label', 'BMI', 'value', bmi),
        JSON_OBJECT('label', '静息血压', 'value', CONCAT(ROUND(112 + risk_base * 28), '/', ROUND(72 + risk_base * 14)))
    ), @seed_time
FROM tmp_miqa_app_user
ON DUPLICATE KEY UPDATE
    generated_at = VALUES(generated_at), answers_json = VALUES(answers_json),
    baseline_json = VALUES(baseline_json), created_at = VALUES(created_at);

INSERT INTO rehealth_health_interview_answer (id, interview_id, question_id, topic, content, sort_order)
SELECT
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:interview-answer:', app.username, ':', item.sort_order), 256)),
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:interview:', app.username), 256)),
    item.question_id, item.topic, item.content, item.sort_order
FROM tmp_miqa_app_user app
CROSS JOIN (
    SELECT 1 sort_order, 'goal' question_id, 'goal' topic, '改善睡眠、活动和心血管风险趋势' content
    UNION ALL SELECT 2, 'routine', 'routine', '每日佩戴设备并完成健康管理任务'
    UNION ALL SELECT 3, 'safety', 'safety', '暂无明确诊断或用药信息'
) item
WHERE 1 = 1
ON DUPLICATE KEY UPDATE content = VALUES(content), topic = VALUES(topic);

INSERT INTO rehealth_health_interview_baseline (id, interview_id, label, item_value, sort_order)
SELECT
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:interview-baseline:', app.username, ':', item.sort_order), 256)),
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:interview:', app.username), 256)),
    item.label,
    CASE item.sort_order WHEN 1 THEN CAST(app.age AS CHAR) WHEN 2 THEN CAST(app.bmi AS CHAR) ELSE '档案信息完整' END,
    item.sort_order
FROM tmp_miqa_app_user app
CROSS JOIN (
    SELECT 1 sort_order, '年龄' label UNION ALL
    SELECT 2, 'BMI' UNION ALL SELECT 3, '数据状态'
) item
WHERE 1 = 1
ON DUPLICATE KEY UPDATE label = VALUES(label), item_value = VALUES(item_value);

INSERT INTO rehealth_health_interview_focus (id, interview_id, focus_area, sort_order)
SELECT
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:interview-focus:', username), 256)),
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:interview:', username), 256)),
    '睡眠、运动、血压和保险健康管理计划', 1
FROM tmp_miqa_app_user
ON DUPLICATE KEY UPDATE focus_area = VALUES(focus_area);

INSERT INTO rehealth_behavior_record (
    id, tenant_id, user_id, request_id, category, title, summary, items_json,
    calories_kcal, protein_grams, carbohydrate_grams, fat_grams, ocr_text,
    confidence, model_version, occurred_at, created_at
)
SELECT
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:behavior:', rel.tenant_id, ':', rel.username, ':', item.item_no), 256)),
    CAST(rel.tenant_id AS CHAR),
    LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_APP_QA:user:', rel.username))),
    CONCAT('miqa-behavior-', rel.tenant_id, '-', rel.member_no, '-', item.item_no),
    item.category, item.title, item.summary,
    JSON_ARRAY(JSON_OBJECT('name', item.title, 'synthetic', TRUE)),
    item.calories, item.protein, item.carbs, item.fat, NULL, 0.99,
    'LOCAL_MULTI_INSURER_APP_QA_NOT_A_MODEL',
    DATE_SUB(@seed_time, INTERVAL item.item_no DAY), @seed_time
FROM tmp_miqa_app_relationship rel
CROSS JOIN (
    SELECT 1 item_no, 'FOOD' category, '早餐记录' title, '全谷物、鸡蛋和水果' summary,
           460.00 calories, 24.00 protein, 58.00 carbs, 16.00 fat
    UNION ALL
    SELECT 2, 'ACTIVITY', '步行记录', '完成中等强度步行', NULL, NULL, NULL, NULL
    UNION ALL
    SELECT 3, 'OTHER', '健康管理任务', '完成保险健康管理随访任务', NULL, NULL, NULL, NULL
) item
WHERE 1 = 1
ON DUPLICATE KEY UPDATE
    title = VALUES(title), summary = VALUES(summary), items_json = VALUES(items_json),
    calories_kcal = VALUES(calories_kcal), protein_grams = VALUES(protein_grams),
    carbohydrate_grams = VALUES(carbohydrate_grams), fat_grams = VALUES(fat_grams),
    occurred_at = VALUES(occurred_at), created_at = VALUES(created_at);

INSERT INTO rehealth_cvd_feature_vector (
    id, user_id, request_id, feature_schema_version, feature_json,
    quality_json, payload_json, created_at
)
SELECT
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:feature:', app.username, ':', day.days_ago), 256)),
    LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_APP_QA:user:', app.username))),
    CONCAT('miqa-cvd16-', app.profile_no, '-', DATE_FORMAT(DATE_SUB(@anchor_date, INTERVAL day.days_ago DAY), '%Y%m%d')),
    'cvd-16-v1',
    JSON_OBJECT(
        'age', app.age, 'gender', IF(app.gender = 'male', 1, 0), 'bmi', app.bmi,
        'sbp', ROUND(112 + app.risk_base * 28, 1), 'dbp', ROUND(72 + app.risk_base * 14, 1),
        'fasting_glucose', ROUND(4.7 + app.risk_base * 2.0, 2),
        'total_cholesterol', ROUND(4.1 + app.risk_base * 1.6, 2),
        'ldl', ROUND(2.1 + app.risk_base * 1.8, 2),
        'hdl', ROUND(1.55 - app.risk_base * 0.45, 2),
        'triglycerides', ROUND(0.9 + app.risk_base * 1.7, 2),
        'exercise_days', 5, 'smoking', IF(app.risk_base >= 0.65, 1, 0),
        'drinking', IF(app.profile_no % 4 = 0, 1, 0),
        'diabetes_history', IF(app.risk_base >= 0.70, 1, 0),
        'hypertension_history', IF(app.risk_base >= 0.50, 1, 0),
        'family_history', IF(app.risk_base >= 0.45, 1, 0)
    ),
    JSON_OBJECT('source', 'synthetic_qa', 'completeFields', 16, 'quality', 96),
    JSON_OBJECT('sourceSystem', 'LOCAL_MULTI_INSURER_APP_QA', 'clinicalUseAllowed', FALSE),
    DATE_SUB(@anchor_date, INTERVAL day.days_ago DAY)
FROM tmp_miqa_app_user app
CROSS JOIN tmp_miqa_risk_day day
WHERE 1 = 1
ON DUPLICATE KEY UPDATE
    feature_json = VALUES(feature_json), quality_json = VALUES(quality_json),
    payload_json = VALUES(payload_json), created_at = VALUES(created_at);

INSERT INTO rehealth_cvd_risk_result (
    id, feature_vector_id, user_id, request_id, feature_schema_version,
    model_version, scorer_mode, is_mock, artifact_name, fallback_reason,
    contribution_method, factor_contribution_version, risk_score, risk_level,
    contribution_json, factor_contribution_json, factor_measured_component_json,
    factor_control_support_json, missing_fields_json, quality_warnings_json,
    summary, response_json, evaluated_at, created_at
)
SELECT
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:risk:', app.username, ':', day.days_ago), 256)),
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:feature:', app.username, ':', day.days_ago), 256)),
    LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_APP_QA:user:', app.username))),
    CONCAT('miqa-cvd16-', app.profile_no, '-', DATE_FORMAT(DATE_SUB(@anchor_date, INTERVAL day.days_ago DAY), '%Y%m%d')),
    'cvd-16-v1', 'local-qa-seeded-nonclinical-v1', 'local_qa_fixture', 0,
    'LOCAL_MULTI_INSURER_APP_QA_NOT_A_MODEL', 'synthetic local fixture',
    'fixture', 'factor16-rule-v1.0.0',
    LEAST(0.95, app.risk_base + day.days_ago * 0.002),
    CASE
        WHEN app.risk_base + day.days_ago * 0.002 >= 0.70 THEN 'high'
        WHEN app.risk_base + day.days_ago * 0.002 >= 0.40 THEN 'medium'
        ELSE 'low'
    END,
    CASE MOD(app.profile_no - 1, 4)
        WHEN 0 THEN JSON_OBJECT(
            '收缩压偏高', ROUND(0.14 + app.risk_base * 0.16, 4),
            '膳食钠摄入', ROUND(0.10 + app.risk_base * 0.12, 4),
            'BMI偏高', ROUND(0.06 + app.risk_base * 0.10, 4),
            '规律运动', ROUND(-0.04 - (1 - app.risk_base) * 0.06, 4))
        WHEN 1 THEN JSON_OBJECT(
            'LDL-C偏高', ROUND(0.16 + app.risk_base * 0.14, 4),
            '总胆固醇偏高', ROUND(0.12 + app.risk_base * 0.10, 4),
            '心血管家族史', ROUND(0.09 + app.age / 1000.0, 4),
            '规律运动', ROUND(-0.03 - (1 - app.risk_base) * 0.05, 4))
        WHEN 2 THEN JSON_OBJECT(
            '睡眠效率偏低', ROUND(0.15 + app.risk_base * 0.12, 4),
            '每周运动不足', ROUND(0.12 + app.risk_base * 0.10, 4),
            '静息心率偏高', ROUND(0.08 + app.risk_base * 0.08, 4),
            '睡眠时长达标', ROUND(-0.03 - (1 - app.risk_base) * 0.04, 4))
        ELSE JSON_OBJECT(
            '空腹血糖偏高', ROUND(0.17 + app.risk_base * 0.13, 4),
            'BMI偏高', ROUND(0.12 + app.risk_base * 0.11, 4),
            '腰围偏高', ROUND(0.10 + app.risk_base * 0.09, 4),
            '规律运动', ROUND(-0.03 - (1 - app.risk_base) * 0.04, 4))
    END,
    JSON_OBJECT('profile', 0.38, 'vitals', 0.30, 'lifestyle', 0.18, 'labs', 0.14),
    JSON_OBJECT('measured', 1.0), JSON_OBJECT('supported', 1.0),
    JSON_ARRAY(), JSON_ARRAY('风险结果仅供健康管理参考，不用于临床或保险决策'),
    '基于近期健康档案与设备趋势生成的心血管风险评估结果。',
    JSON_OBJECT(
        'sourceSystem', 'LOCAL_MULTI_INSURER_APP_QA', 'isMock', FALSE,
        'clinicalUseAllowed', FALSE, 'syntheticLocalQa', TRUE,
        'factorContributionVersion', 'factor16-rule-v1.0.0',
        'factor_contributions', CASE MOD(app.profile_no - 1, 4)
            WHEN 0 THEN JSON_OBJECT(
                '收缩压', ROUND(0.14 + app.risk_base * 0.16, 4),
                '膳食钠摄入', ROUND(0.10 + app.risk_base * 0.12, 4),
                'BMI', ROUND(0.06 + app.risk_base * 0.10, 4),
                '每周运动天数', ROUND(-0.04 - (1 - app.risk_base) * 0.06, 4))
            WHEN 1 THEN JSON_OBJECT(
                'LDL-C', ROUND(0.16 + app.risk_base * 0.14, 4),
                '总胆固醇', ROUND(0.12 + app.risk_base * 0.10, 4),
                '心血管家族史', ROUND(0.09 + app.age / 1000.0, 4),
                '每周运动天数', ROUND(-0.03 - (1 - app.risk_base) * 0.05, 4))
            WHEN 2 THEN JSON_OBJECT(
                '睡眠效率', ROUND(0.15 + app.risk_base * 0.12, 4),
                '每周运动天数', ROUND(0.12 + app.risk_base * 0.10, 4),
                '静息心率', ROUND(0.08 + app.risk_base * 0.08, 4),
                '睡眠时长', ROUND(-0.03 - (1 - app.risk_base) * 0.04, 4))
            ELSE JSON_OBJECT(
                '空腹血糖', ROUND(0.17 + app.risk_base * 0.13, 4),
                'BMI', ROUND(0.12 + app.risk_base * 0.11, 4),
                '腰围', ROUND(0.10 + app.risk_base * 0.09, 4),
                '每周运动天数', ROUND(-0.03 - (1 - app.risk_base) * 0.04, 4))
        END,
        'factor_measured_components', CASE MOD(app.profile_no - 1, 4)
            WHEN 0 THEN JSON_OBJECT(
                '收缩压', ROUND(122 + app.risk_base * 28, 1),
                '膳食钠摄入', ROUND(5.2 + app.risk_base * 3.5, 1),
                'BMI', app.bmi,
                '每周运动天数', 4)
            WHEN 1 THEN JSON_OBJECT(
                'LDL-C', ROUND(2.6 + app.risk_base * 2.1, 1),
                '总胆固醇', ROUND(4.3 + app.risk_base * 2.2, 1),
                '心血管家族史', 1,
                '每周运动天数', 3)
            WHEN 2 THEN JSON_OBJECT(
                '睡眠效率', ROUND(90 - app.risk_base * 24, 1),
                '每周运动天数', 2,
                '静息心率', ROUND(62 + app.risk_base * 24, 1),
                '睡眠时长', ROUND(7.8 - app.risk_base * 2.2, 1))
            ELSE JSON_OBJECT(
                '空腹血糖', ROUND(4.8 + app.risk_base * 3.2, 1),
                'BMI', app.bmi,
                '腰围', ROUND(72 + app.risk_base * 30, 1),
                '每周运动天数', 3)
        END
    ),
    DATE_ADD(DATE_SUB(@anchor_date, INTERVAL day.days_ago DAY), INTERVAL 20 HOUR),
    DATE_ADD(DATE_SUB(@anchor_date, INTERVAL day.days_ago DAY), INTERVAL 20 HOUR)
FROM tmp_miqa_app_user app
CROSS JOIN tmp_miqa_risk_day day
WHERE 1 = 1
ON DUPLICATE KEY UPDATE
    model_version = VALUES(model_version), scorer_mode = VALUES(scorer_mode),
    is_mock = 0, artifact_name = VALUES(artifact_name), risk_score = VALUES(risk_score),
    risk_level = VALUES(risk_level), contribution_json = VALUES(contribution_json),
    factor_contribution_json = VALUES(factor_contribution_json),
    factor_contribution_version = VALUES(factor_contribution_version),
    missing_fields_json = VALUES(missing_fields_json),
    quality_warnings_json = VALUES(quality_warnings_json), summary = VALUES(summary),
    response_json = VALUES(response_json), evaluated_at = VALUES(evaluated_at),
    created_at = VALUES(created_at);

INSERT INTO rehealth_attribution_result (
    id, user_id, status, model_version, request_id, attribution_mode, is_mock,
    provider, history_days, min_history_days, intervention_days,
    intervention_data_sufficient, current_risk_score, current_risk_level,
    current_trend, individual_att, trend_delta, adherence_average,
    interpretation, request_json, response_json, created_at
)
SELECT
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:attribution:', username), 256)),
    LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_APP_QA:user:', username))),
    'ready', 'local-qa-pias-nonclinical-v1', CONCAT('miqa-pias-', profile_no),
    'synthetic_qa', IF(profile_no IN (2, 6, 10), 0, 1), 'LOCAL_MULTI_INSURER_APP_QA_NOT_A_MODEL',
    30, 14, 14, 1, risk_base,
    CASE WHEN risk_base >= 0.70 THEN 'high' WHEN risk_base >= 0.40 THEN 'medium' ELSE 'low' END,
    'improving', -0.035, -0.058, 0.82,
    '归因结果用于健康管理趋势参考，不作为诊断、核保或理赔结论。',
    JSON_OBJECT('sourceSystem', 'LOCAL_MULTI_INSURER_APP_QA', 'historyDays', 30),
    JSON_OBJECT(
        'status', 'ready', 'isMock', IF(profile_no IN (2, 6, 10), FALSE, TRUE),
        'clinicalUseAllowed', FALSE, 'syntheticLocalQa', TRUE
    ),
    @seed_time
FROM tmp_miqa_app_user
ON DUPLICATE KEY UPDATE
    status = VALUES(status), model_version = VALUES(model_version),
    is_mock = VALUES(is_mock), intervention_data_sufficient = VALUES(intervention_data_sufficient),
    history_days = VALUES(history_days), current_risk_score = VALUES(current_risk_score),
    current_risk_level = VALUES(current_risk_level), current_trend = VALUES(current_trend),
    individual_att = VALUES(individual_att), trend_delta = VALUES(trend_delta),
    adherence_average = VALUES(adherence_average), interpretation = VALUES(interpretation),
    response_json = VALUES(response_json), created_at = VALUES(created_at);

INSERT INTO rehealth_intervention_plan (
    id, user_id, plan_id, source_request_id, feature_schema_version,
    model_version, scorer_mode, is_mock, artifact_name, priority_intervention,
    rationale, expected_impact, confidence, medical_disclaimer,
    generated_at, response_json, created_at
)
SELECT
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:plan:', username), 256)),
    LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_APP_QA:user:', username))),
    CONCAT('miqa-health-plan-', profile_no), CONCAT('miqa-plan-request-', profile_no),
    'cvd-16-v1', 'local-qa-seeded-nonclinical-v1', 'local_qa_fixture', 1,
    'LOCAL_MULTI_INSURER_APP_QA_NOT_A_MODEL',
    CASE MOD(profile_no - 1, 4)
        WHEN 0 THEN '连续 7 日早晚记录血压，配合低盐饮食并完成一次健康管理随访。'
        WHEN 1 THEN '补充血脂复查，确认用药执行情况，并完成膳食结构评估。'
        WHEN 2 THEN '固定就寝时间，每周完成四次有氧活动并复盘睡眠效率。'
        ELSE '连续记录空腹血糖、体重与腰围，完成代谢风险复核。'
    END,
    '依据健康档案、设备趋势和 CVD-16 风险结果生成。',
    '结合近期健康指标、计划执行情况和负责人随访记录持续评估。', 0.80,
    '本计划仅用于健康管理参考，不构成医疗建议，不替代医生诊疗。',
    DATE_SUB(@seed_time, INTERVAL 14 DAY),
    JSON_OBJECT(
        'sourceSystem', 'LOCAL_MULTI_INSURER_APP_QA', 'isMock', TRUE, 'clinicalUseAllowed', FALSE,
        'items', JSON_ARRAY(
            JSON_OBJECT('title', '完成连续 7 日上臂袖带血压记录', 'action', '每日早晚各测量一次并由 APP 回传', 'target', '连续记录 7 日'),
            JSON_OBJECT('title', '完成本周运动计划', 'action', '进行 5 次中等强度步行，每次 30 分钟', 'target', '本周完成 5 次'),
            JSON_OBJECT('title', '改善睡眠规律', 'action', '固定就寝时间并记录睡眠时长', 'target', '连续 7 日保持规律')
        )
    ),
    DATE_SUB(@seed_time, INTERVAL 14 DAY)
FROM tmp_miqa_app_user
ON DUPLICATE KEY UPDATE
    model_version = VALUES(model_version), scorer_mode = VALUES(scorer_mode),
    is_mock = 1, priority_intervention = VALUES(priority_intervention),
    rationale = VALUES(rationale), expected_impact = VALUES(expected_impact),
    confidence = VALUES(confidence), medical_disclaimer = VALUES(medical_disclaimer),
    response_json = VALUES(response_json), generated_at = VALUES(generated_at);

-- Snapshot IDs are stable by relative day while the unique business key uses
-- the calendar date. Remove only this fixture's projections before rebuilding
-- them so changing AnchorDate cannot make old relative-day IDs collide.
DELETE contribution
FROM rehealth_rdi_contribution contribution
JOIN rehealth_rdi_daily_snapshot snapshot ON snapshot.id = contribution.snapshot_id
WHERE snapshot.calculation_source = 'LOCAL_MULTI_INSURER_APP_QA';

DELETE FROM rehealth_rdi_daily_snapshot
WHERE calculation_source = 'LOCAL_MULTI_INSURER_APP_QA';

DELETE FROM rehealth_rhi_daily_snapshot
WHERE calculation_source = 'LOCAL_MULTI_INSURER_APP_QA';

INSERT INTO rehealth_rhi_daily_snapshot (
    id, user_id, scored_on, raw_score, display_score, data_confidence,
    status, product_tier, available_days, available_feature_count,
    smoothing_alpha, algorithm_version, calculation_source,
    domains_json, features_json, quality_json, created_at, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:rhi:', app.username, ':', day.days_ago), 256)),
    LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_APP_QA:user:', app.username))),
    DATE_SUB(@anchor_date, INTERVAL day.days_ago DAY),
    ROUND(58 + (1 - app.risk_base) * 24 - day.days_ago * 0.35, 4),
    ROUND(58 + (1 - app.risk_base) * 24 - day.days_ago * 0.35, 4),
    ROUND(0.82 + (app.profile_no % 3) * 0.04, 6),
    'ready', 'STANDARD', 30, 16, 0.300000,
    'rhi-local-qa-display-v1', 'LOCAL_MULTI_INSURER_APP_QA',
    JSON_OBJECT(
        'activity', ROUND(62 + (1 - app.risk_base) * 18, 2),
        'sleep', ROUND(60 + (1 - app.risk_base) * 20, 2),
        'cardiovascular', ROUND(58 + (1 - app.risk_base) * 22, 2)
    ),
    JSON_OBJECT('availableFeatureCount', 16, 'syntheticLocalQa', TRUE),
    JSON_OBJECT('synthetic', TRUE, 'clinicalUseAllowed', FALSE, 'quality', 96),
    DATE_ADD(DATE_SUB(@anchor_date, INTERVAL day.days_ago DAY), INTERVAL 21 HOUR),
    DATE_ADD(DATE_SUB(@anchor_date, INTERVAL day.days_ago DAY), INTERVAL 21 HOUR)
FROM tmp_miqa_app_user app
CROSS JOIN tmp_miqa_risk_day day
WHERE day.days_ago < 7
ON DUPLICATE KEY UPDATE
    scored_on = VALUES(scored_on), raw_score = VALUES(raw_score),
    display_score = VALUES(display_score), data_confidence = VALUES(data_confidence),
    status = VALUES(status), product_tier = VALUES(product_tier),
    available_days = VALUES(available_days), available_feature_count = VALUES(available_feature_count),
    smoothing_alpha = VALUES(smoothing_alpha), algorithm_version = VALUES(algorithm_version),
    calculation_source = VALUES(calculation_source), domains_json = VALUES(domains_json),
    features_json = VALUES(features_json), quality_json = VALUES(quality_json),
    created_at = VALUES(created_at), updated_at = VALUES(updated_at);

-- RDI remains a user-owned daily aggregate. These fixtures are deliberately
-- marked Mock so they can exercise the UI without becoming business evidence.
INSERT INTO rehealth_rdi_daily_snapshot (
    id, user_id, scored_on, raw_score, display_score, data_confidence,
    status, is_mock, algorithm_version, calculation_source, created_at, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:rdi:', app.username, ':', day.days_ago), 256)),
    LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_APP_QA:user:', app.username))),
    DATE_SUB(@anchor_date, INTERVAL day.days_ago DAY),
    ROUND(38 + app.risk_base * 45 + day.days_ago * 0.40, 4),
    ROUND(38 + app.risk_base * 45 + day.days_ago * 0.40, 4),
    ROUND(0.78 + (app.profile_no % 3) * 0.05, 6),
    'DEBUG_MOCK', 1, 'rdi-rule-1.0.1', 'LOCAL_MULTI_INSURER_APP_QA',
    DATE_ADD(DATE_SUB(@anchor_date, INTERVAL day.days_ago DAY), INTERVAL 21 HOUR),
    DATE_ADD(DATE_SUB(@anchor_date, INTERVAL day.days_ago DAY), INTERVAL 21 HOUR)
FROM tmp_miqa_app_user app
CROSS JOIN tmp_miqa_risk_day day
WHERE day.days_ago < 7
ON DUPLICATE KEY UPDATE
    raw_score = VALUES(raw_score), display_score = VALUES(display_score),
    data_confidence = VALUES(data_confidence), status = VALUES(status),
    is_mock = VALUES(is_mock), algorithm_version = VALUES(algorithm_version),
    calculation_source = VALUES(calculation_source), updated_at = VALUES(updated_at);

INSERT INTO rehealth_rdi_contribution (
    id, snapshot_id, factor_code, domain_code, source_code, current_value,
    baseline_value, unit, raw_points, confidence, final_points,
    source_factor_id, algorithm_version, created_at
)
SELECT
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:rdi-contribution:', app.username, ':',
                      day.days_ago, ':', factor.factor_code), 256)),
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:rdi:', app.username, ':', day.days_ago), 256)),
    factor.factor_code, factor.domain_code, 'LOCAL_MULTI_INSURER_APP_QA',
    CASE factor.factor_code
      WHEN 'steps' THEN 4200 + app.profile_no * 90
      WHEN 'sleep_duration' THEN 390 + app.profile_no * 3
      ELSE 30 + app.profile_no * 0.8
    END,
    CASE factor.factor_code WHEN 'steps' THEN 6800 WHEN 'sleep_duration' THEN 480 ELSE 42 END,
    factor.unit,
    ROUND(app.risk_base * factor.weight, 6),
    ROUND(0.78 + (app.profile_no % 3) * 0.05, 6),
    ROUND(app.risk_base * factor.weight * (0.78 + (app.profile_no % 3) * 0.05), 6),
    CONCAT('LOCAL_MULTI_INSURER_APP_QA:', factor.factor_code, ':', app.username, ':', day.days_ago),
    'rdi-rule-1.0.1',
    DATE_ADD(DATE_SUB(@anchor_date, INTERVAL day.days_ago DAY), INTERVAL 21 HOUR)
FROM tmp_miqa_app_user app
CROSS JOIN tmp_miqa_risk_day day
CROSS JOIN (
    SELECT 'steps' factor_code, 'activity' domain_code, 'steps/day' unit, 1.20 weight
    UNION ALL SELECT 'sleep_duration', 'sleep', 'min/night', 1.00
    UNION ALL SELECT 'nocturnal_hrv', 'recovery', 'ms', 0.80
) factor
WHERE day.days_ago < 7
ON DUPLICATE KEY UPDATE
    current_value = VALUES(current_value), baseline_value = VALUES(baseline_value),
    raw_points = VALUES(raw_points), confidence = VALUES(confidence),
    final_points = VALUES(final_points), source_factor_id = VALUES(source_factor_id),
    algorithm_version = VALUES(algorithm_version), created_at = VALUES(created_at);

INSERT INTO rehealth_insurance_subject (
    id, tenant_id, subject_ref, rehealth_user_id, external_subject_ref_hash,
    enrollment_status, consent_status, consent_version, consented_at,
    source_system, source_record_id, metadata_json, created_at, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:subject:', rel.tenant_id, ':', rel.username), 256)),
    rel.tenant_id,
    LOWER(SHA2(CONCAT(rel.tenant_id, ':', LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_APP_QA:user:', rel.username)))), 256)),
    LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_APP_QA:user:', rel.username))),
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:external:', rel.tenant_id, ':', rel.username), 256)),
    'active', 'granted', 'local-miqa-v1', DATE_SUB(@seed_time, INTERVAL 180 DAY),
    'LOCAL_MULTI_INSURER_APP_QA', CONCAT('subject-', rel.tenant_id, '-', rel.member_no),
    JSON_OBJECT('synthetic', TRUE, 'sharedAcrossInsurers', rel.username LIKE 'local_app_shared_%', 'clinicalUseAllowed', FALSE),
    DATE_SUB(@seed_time, INTERVAL 180 DAY), @seed_time
FROM tmp_miqa_app_relationship rel
ON DUPLICATE KEY UPDATE
    subject_ref = VALUES(subject_ref), rehealth_user_id = VALUES(rehealth_user_id),
    enrollment_status = 'active', consent_status = 'granted',
    consent_version = VALUES(consent_version), consented_at = VALUES(consented_at),
    metadata_json = VALUES(metadata_json), updated_at = VALUES(updated_at);

INSERT INTO rehealth_insurance_policy (
    id, tenant_id, policy_no, product_code, product_name, policy_type,
    policyholder_subject_ref, insured_subject_ref, coverage_amount,
    premium_amount, deductible_amount, waiting_period_days, effective_on,
    expires_on, status, source_system, source_record_id, metadata_json,
    created_at, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:policy:', rel.tenant_id, ':', rel.username), 256)),
    rel.tenant_id, CONCAT('RH-', rel.tenant_id, '-POL-', LPAD(rel.member_no, 4, '0')),
    CASE MOD(rel.member_no - 1, 4) WHEN 0 THEN 'GROUP-MED' WHEN 1 THEN 'LONG-MED' WHEN 2 THEN 'CI-PLUS' ELSE 'CVD-CARE' END,
    CASE MOD(rel.member_no - 1, 4) WHEN 0 THEN '悦享健康团体医疗保障计划' WHEN 1 THEN '安心守护长期医疗保障计划' WHEN 2 THEN '康护无忧重大疾病保障计划' ELSE '臻享心脑血管专项保障计划' END,
    CASE MOD(rel.member_no - 1, 4) WHEN 0 THEN 'group_medical' WHEN 1 THEN 'long_term_medical' WHEN 2 THEN 'critical_illness' ELSE 'cvd_management' END,
    LOWER(SHA2(CONCAT(rel.tenant_id, ':', LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_APP_QA:user:', rel.username)))), 256)),
    LOWER(SHA2(CONCAT(rel.tenant_id, ':', LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_APP_QA:user:', rel.username)))), 256)),
    CASE MOD(rel.member_no - 1, 4) WHEN 0 THEN 500000.00 WHEN 1 THEN 1000000.00 WHEN 2 THEN 800000.00 ELSE 300000.00 END,
    980.00 + rel.member_no * 95,
    CASE WHEN MOD(rel.member_no - 1, 4) = 2 THEN 0.00 ELSE 500.00 END,
    CASE WHEN MOD(rel.member_no - 1, 4) = 0 THEN 0 ELSE 30 END,
    DATE_SUB(@anchor_date, INTERVAL 180 DAY), DATE_ADD(@anchor_date, INTERVAL 365 DAY),
    'active', 'LOCAL_MULTI_INSURER_APP_QA', CONCAT('policy-', rel.tenant_id, '-', rel.member_no),
    JSON_OBJECT('synthetic', TRUE, 'clinicalUseAllowed', FALSE,
        'channel', CASE MOD(rel.member_no - 1, 4) WHEN 0 THEN '企业团险' WHEN 1 THEN '银行保险' WHEN 2 THEN '保险经纪' ELSE '个人直销' END),
    DATE_SUB(@seed_time, INTERVAL 180 DAY), @seed_time
FROM tmp_miqa_app_relationship rel
ON DUPLICATE KEY UPDATE
    policy_no = VALUES(policy_no), product_code = VALUES(product_code), product_name = VALUES(product_name),
    insured_subject_ref = VALUES(insured_subject_ref), coverage_amount = VALUES(coverage_amount),
    premium_amount = VALUES(premium_amount), effective_on = VALUES(effective_on),
    expires_on = VALUES(expires_on), status = 'active',
    metadata_json = VALUES(metadata_json), updated_at = VALUES(updated_at);

INSERT INTO rehealth_insurance_coverage (
    id, tenant_id, policy_id, subject_ref, coverage_code, coverage_name,
    limit_amount, deductible_amount, effective_on, expires_on, status,
    source_system, source_record_id, metadata_json, created_at, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:coverage:', rel.tenant_id, ':', rel.username), 256)),
    rel.tenant_id,
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:policy:', rel.tenant_id, ':', rel.username), 256)),
    LOWER(SHA2(CONCAT(rel.tenant_id, ':', LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_APP_QA:user:', rel.username)))), 256)),
    CASE MOD(rel.member_no - 1, 4) WHEN 0 THEN 'GROUP-MED' WHEN 1 THEN 'LONG-MED' WHEN 2 THEN 'CI-PLUS' ELSE 'CVD-CARE' END,
    CASE MOD(rel.member_no - 1, 4) WHEN 0 THEN '团体医疗费用保障' WHEN 1 THEN '长期医疗费用保障' WHEN 2 THEN '重大疾病定额给付保障' ELSE '心脑血管健康管理保障' END,
    CASE MOD(rel.member_no - 1, 4) WHEN 0 THEN 500000.00 WHEN 1 THEN 1000000.00 WHEN 2 THEN 800000.00 ELSE 300000.00 END,
    CASE WHEN MOD(rel.member_no - 1, 4) = 2 THEN 0.00 ELSE 500.00 END,
    DATE_SUB(@anchor_date, INTERVAL 180 DAY), DATE_ADD(@anchor_date, INTERVAL 365 DAY),
    'active', 'LOCAL_MULTI_INSURER_APP_QA', CONCAT('coverage-', rel.tenant_id, '-', rel.member_no),
    JSON_OBJECT('synthetic', TRUE), DATE_SUB(@seed_time, INTERVAL 180 DAY), @seed_time
FROM tmp_miqa_app_relationship rel
ON DUPLICATE KEY UPDATE
    policy_id = VALUES(policy_id), subject_ref = VALUES(subject_ref),
    coverage_code = VALUES(coverage_code), coverage_name = VALUES(coverage_name),
    limit_amount = VALUES(limit_amount), status = 'active',
    metadata_json = VALUES(metadata_json), updated_at = VALUES(updated_at);

INSERT INTO rehealth_insurance_consent (
    id, tenant_id, subject_ref, consent_type, consent_version, status,
    granted_at, revoked_at, evidence_ref, evidence_hash, source_system,
    source_record_id, metadata_json, created_at, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:consent:', rel.tenant_id, ':', rel.username), 256)),
    rel.tenant_id,
    LOWER(SHA2(CONCAT(rel.tenant_id, ':', LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_APP_QA:user:', rel.username)))), 256)),
    'insurance_health_management', 'local-miqa-v1', 'granted',
    DATE_SUB(@seed_time, INTERVAL 180 DAY), NULL,
    CONCAT('RH-CONSENT-', rel.tenant_id, '-', LPAD(rel.member_no, 4, '0')),
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:consent-evidence:', rel.tenant_id, ':', rel.username), 256)),
    'LOCAL_MULTI_INSURER_APP_QA', CONCAT('consent-', rel.tenant_id, '-', rel.member_no),
    JSON_OBJECT('synthetic', TRUE, 'scope', 'assigned-staff-full-business-data'),
    DATE_SUB(@seed_time, INTERVAL 180 DAY), @seed_time
FROM tmp_miqa_app_relationship rel
ON DUPLICATE KEY UPDATE
    status = 'granted', granted_at = VALUES(granted_at), revoked_at = NULL,
    evidence_ref = VALUES(evidence_ref), evidence_hash = VALUES(evidence_hash),
    metadata_json = VALUES(metadata_json), updated_at = VALUES(updated_at);

INSERT INTO rehealth_insurance_plan_binding (
    id, tenant_id, subject_ref, policy_id, plan_id, consent_id, status,
    bound_at, unbound_at, source_system, source_record_id, metadata_json,
    created_at, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:binding:', rel.tenant_id, ':', rel.username), 256)),
    rel.tenant_id,
    LOWER(SHA2(CONCAT(rel.tenant_id, ':', LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_APP_QA:user:', rel.username)))), 256)),
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:policy:', rel.tenant_id, ':', rel.username), 256)),
    CONCAT('miqa-health-plan-', app.profile_no),
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:consent:', rel.tenant_id, ':', rel.username), 256)),
    'active', DATE_SUB(@seed_time, INTERVAL 90 DAY), NULL,
    'LOCAL_MULTI_INSURER_APP_QA', CONCAT('binding-', rel.tenant_id, '-', rel.member_no),
    JSON_OBJECT('synthetic', TRUE), DATE_SUB(@seed_time, INTERVAL 90 DAY), @seed_time
FROM tmp_miqa_app_relationship rel
JOIN tmp_miqa_app_user app ON app.username = rel.username
WHERE 1 = 1
ON DUPLICATE KEY UPDATE
    policy_id = VALUES(policy_id), plan_id = VALUES(plan_id), consent_id = VALUES(consent_id),
    status = 'active', bound_at = VALUES(bound_at), unbound_at = NULL,
    metadata_json = VALUES(metadata_json), updated_at = VALUES(updated_at);

INSERT INTO rehealth_insurance_intervention (
    id, tenant_id, subject_ref, plan_id, source_plan_id, consent_id, status,
    enrolled_at, ended_at, last_feedback_at, source_system, source_record_id,
    metadata_json, created_at, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:insurance-intervention:', rel.tenant_id, ':', rel.username), 256)),
    rel.tenant_id,
    LOWER(SHA2(CONCAT(rel.tenant_id, ':', LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_APP_QA:user:', rel.username)))), 256)),
    CONCAT('miqa-health-plan-', app.profile_no),
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:plan:', rel.username), 256)),
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:consent:', rel.tenant_id, ':', rel.username), 256)),
    IF(rel.member_no IN (3, 9, 10), 'active', 'completed'),
    DATE_SUB(@seed_time, INTERVAL 90 DAY),
    IF(rel.member_no IN (3, 9, 10), NULL, DATE_SUB(@seed_time, INTERVAL 2 DAY)),
    DATE_SUB(@seed_time, INTERVAL 1 DAY),
    'LOCAL_MULTI_INSURER_APP_QA', CONCAT('intervention-', rel.tenant_id, '-', rel.member_no),
    JSON_OBJECT('synthetic', TRUE, 'completionRate', 0.82, 'clinicalUseAllowed', FALSE),
    DATE_SUB(@seed_time, INTERVAL 90 DAY), @seed_time
FROM tmp_miqa_app_relationship rel
JOIN tmp_miqa_app_user app ON app.username = rel.username
WHERE 1 = 1
ON DUPLICATE KEY UPDATE
    plan_id = VALUES(plan_id), source_plan_id = VALUES(source_plan_id),
    consent_id = VALUES(consent_id), status = VALUES(status), ended_at = VALUES(ended_at),
    last_feedback_at = VALUES(last_feedback_at), metadata_json = VALUES(metadata_json),
    updated_at = VALUES(updated_at);

INSERT INTO rehealth_insurance_intervention_feedback (
    id, tenant_id, binding_id, subject_ref, intervention_id, feedback_type,
    occurred_at, completion_rate, adherence_score, plan_item_id,
    expected_count, completed_count, verification_type, calculation_version, outcome_summary_json,
    source_system, source_record_id, created_at
)
SELECT
    LOWER(SHA2(CONCAT(
        'LOCAL_MULTI_INSURER_APP_QA:feedback:', rel.tenant_id, ':', rel.username,
        IF(item.feedback_no = 1, '', CONCAT(':', item.feedback_no))
    ), 256)),
    rel.tenant_id,
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:binding:', rel.tenant_id, ':', rel.username), 256)),
    LOWER(SHA2(CONCAT(rel.tenant_id, ':', LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_APP_QA:user:', rel.username)))), 256)),
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:insurance-intervention:', rel.tenant_id, ':', rel.username), 256)),
    CASE
        WHEN GREATEST(0, LEAST(1, 1.05 - app.risk_base + item.score_offset)) >= 0.95 THEN 'completed'
        WHEN GREATEST(0, LEAST(1, 1.05 - app.risk_base + item.score_offset)) >= 0.20 THEN 'partially_completed'
        ELSE 'skipped'
    END,
    DATE_SUB(@seed_time, INTERVAL item.days_ago DAY),
    ROUND(GREATEST(0, LEAST(1, 1.05 - app.risk_base + item.score_offset)), 2),
    ROUND(GREATEST(0, LEAST(1, 1.05 - app.risk_base + item.score_offset)), 2),
    CONCAT('miqa-plan-item-', MOD(app.profile_no - 1, 4) + 1, '-', item.feedback_no),
    1.000, ROUND(GREATEST(0, LEAST(1, 1.05 - app.risk_base + item.score_offset)), 2),
    'self_report', 'insurance-adherence-event-v1',
    JSON_OBJECT(
        'synthetic', TRUE, 'clinicalUseAllowed', FALSE,
        'sleepImproved', item.feedback_no >= 2,
        'activityGoalMet', item.feedback_no = 3,
        'note', item.note
    ),
    'LOCAL_MULTI_INSURER_APP_QA', CONCAT(
        'feedback-', rel.tenant_id, '-', rel.member_no,
        IF(item.feedback_no = 1, '', CONCAT('-', item.feedback_no))
    ),
    @seed_time
FROM tmp_miqa_app_relationship rel
JOIN tmp_miqa_app_user app ON app.username = rel.username
CROSS JOIN (
    SELECT 1 feedback_no, 7 days_ago, -0.15 score_offset, '已确认计划并开始执行' note
    UNION ALL
    SELECT 2, 4, 0.00, '已完成睡眠和运动记录'
    UNION ALL
    SELECT 3, 1, 0.15, '已回传本周执行结果'
) item
WHERE 1 = 1
ON DUPLICATE KEY UPDATE
    binding_id = VALUES(binding_id), subject_ref = VALUES(subject_ref),
    intervention_id = VALUES(intervention_id), feedback_type = VALUES(feedback_type),
    occurred_at = VALUES(occurred_at), completion_rate = VALUES(completion_rate),
    adherence_score = VALUES(adherence_score), plan_item_id = VALUES(plan_item_id),
    expected_count = VALUES(expected_count), completed_count = VALUES(completed_count),
    verification_type = VALUES(verification_type), calculation_version = VALUES(calculation_version),
    outcome_summary_json = VALUES(outcome_summary_json),
    created_at = VALUES(created_at);

INSERT INTO rehealth_insurance_intervention_action (
    id, tenant_id, subject_ref, plan_id, action_type, title, content,
    assignee_user_id, status, due_at, completed_at, result_json,
    created_by, request_id, created_at, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:workbench-action:', rel.tenant_id, ':', rel.username, ':', item.action_no), 256)),
    rel.tenant_id,
    LOWER(SHA2(CONCAT(rel.tenant_id, ':', LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_APP_QA:user:', rel.username)))), 256)),
    CONCAT('miqa-health-plan-', app.profile_no), item.action_type,
    CASE item.action_no
        WHEN 1 THEN CASE MOD(app.profile_no - 1, 4)
            WHEN 0 THEN '血压管理首次随访'
            WHEN 1 THEN '血脂资料首次复核'
            WHEN 2 THEN '睡眠与运动首次随访'
            ELSE '代谢指标首次复核' END
        WHEN 2 THEN CASE MOD(app.profile_no - 1, 4)
            WHEN 0 THEN '血压记录完成提醒'
            WHEN 1 THEN '血脂复查预约提醒'
            WHEN 2 THEN '睡眠与运动计划提醒'
            ELSE '血糖与体重记录提醒' END
        ELSE CASE MOD(app.profile_no - 1, 4)
            WHEN 0 THEN '血压趋势阶段复核'
            WHEN 1 THEN '血脂管理阶段复核'
            WHEN 2 THEN '生活方式执行复核'
            ELSE '代谢管理阶段复核' END
    END,
    CASE MOD(app.profile_no - 1, 4)
        WHEN 0 THEN '核对早晚血压记录、低盐饮食执行和复测安排；不根据单次读数调整治疗。'
        WHEN 1 THEN '核对血脂复查、用药确认和膳食记录；具体用药由医生审核。'
        WHEN 2 THEN '核对睡眠记录、有氧活动次数和当前执行困难。'
        ELSE '核对空腹血糖、体重和腰围记录，确认是否需要人工复核。'
    END,
    assignee.id,
    CASE
        WHEN rel.member_no IN (3, 9, 10) AND item.action_no = 1 THEN 'pending'
        WHEN rel.member_no IN (3, 9, 10) AND item.action_no = 2 THEN 'in_progress'
        WHEN item.action_no = 3 THEN 'completed'
        ELSE 'completed'
    END,
    DATE_ADD(@seed_time, INTERVAL item.due_days DAY),
    CASE
        WHEN rel.member_no IN (3, 9, 10) AND item.action_no IN (1, 2) THEN NULL
        ELSE DATE_SUB(@seed_time, INTERVAL (4 - item.action_no) DAY)
    END,
    JSON_OBJECT(
        'synthetic', TRUE, 'clinicalUseAllowed', FALSE,
        'result', IF(rel.member_no IN (3, 9, 10) AND item.action_no IN (1, 2), '待执行', '已完成随访记录')
    ),
    assignee.id,
    CONCAT('miqa-workbench-', rel.tenant_id, '-', LPAD(rel.member_no, 2, '0'), '-', item.action_no),
    DATE_SUB(@seed_time, INTERVAL (10 - item.action_no) DAY),
    DATE_SUB(@seed_time, INTERVAL (4 - item.action_no) DAY)
FROM tmp_miqa_app_relationship rel
JOIN tmp_miqa_app_user app ON app.username = rel.username
JOIN sys_user assignee
  ON assignee.username = CONCAT('local_ins_', rel.tenant_id, '_admin')
 AND assignee.status = 1 AND assignee.del_flag = 0
CROSS JOIN (
    SELECT 1 action_no, 'followup' action_type, '首次健康管理随访' title,
           '核对计划理解情况和当前执行困难，记录后续跟进重点。' content, 1 due_days
    UNION ALL
    SELECT 2, 'reminder', '计划执行提醒',
           '提醒完成运动、睡眠和血压记录；不构成医疗建议。', 3
    UNION ALL
    SELECT 3, 'review', '阶段执行记录复核',
           '复核 APP 回传记录的完整性，不把单次波动作为改善结论。', 7
) item
WHERE 1 = 1
ON DUPLICATE KEY UPDATE
    subject_ref = VALUES(subject_ref), plan_id = VALUES(plan_id),
    action_type = VALUES(action_type), title = VALUES(title), content = VALUES(content),
    assignee_user_id = VALUES(assignee_user_id), status = VALUES(status),
    due_at = VALUES(due_at), completed_at = VALUES(completed_at),
    result_json = VALUES(result_json), created_by = VALUES(created_by),
    created_at = VALUES(created_at), updated_at = VALUES(updated_at);

-- Re-running the local fixture resets ad-hoc workbench actions created against
-- these synthetic subjects, so the four deterministic workflow cohorts remain
-- three rows each. No non-QA subject is affected.
UPDATE rehealth_insurance_intervention_action action
JOIN rehealth_insurance_subject subject
  ON subject.tenant_id = action.tenant_id
 AND subject.subject_ref = action.subject_ref
 AND subject.source_system = 'LOCAL_MULTI_INSURER_APP_QA'
SET action.status = 'completed',
    action.completed_at = COALESCE(action.completed_at, @seed_time),
    action.updated_at = @seed_time
WHERE action.request_id NOT LIKE 'miqa-workbench-%'
  AND action.status IN ('pending', 'in_progress');

INSERT INTO rehealth_insurance_claim (
    id, tenant_id, claim_no, policy_id, subject_ref, claim_type,
    event_on, submitted_at, decided_at, status, billed_amount,
    approved_amount, paid_amount, currency, coverage_code, outcome_code,
    source_system, source_record_id, metadata_json, created_at, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:claim:', rel.tenant_id, ':', rel.username), 256)),
    rel.tenant_id, CONCAT('RH-', rel.tenant_id, '-CLAIM-', LPAD(rel.member_no, 4, '0')),
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:policy:', rel.tenant_id, ':', rel.username), 256)),
    LOWER(SHA2(CONCAT(rel.tenant_id, ':', LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_APP_QA:user:', rel.username)))), 256)),
    IF(rel.member_no % 2 = 0, 'outpatient', 'health_management'),
    DATE_SUB(@anchor_date, INTERVAL (rel.member_no * 8) DAY),
    DATE_SUB(@seed_time, INTERVAL (rel.member_no * 8) DAY),
    DATE_SUB(@seed_time, INTERVAL (rel.member_no * 8 - 3) DAY),
    'paid', 1200.00 + rel.member_no * 160, 900.00 + rel.member_no * 120,
    900.00 + rel.member_no * 120, 'CNY', 'CVD-MGMT', 'paid',
    'LOCAL_MULTI_INSURER_APP_QA', CONCAT('claim-', rel.tenant_id, '-', rel.member_no),
    JSON_OBJECT('synthetic', TRUE, 'clinicalUseAllowed', FALSE),
    DATE_SUB(@seed_time, INTERVAL (rel.member_no * 8) DAY), @seed_time
FROM tmp_miqa_app_relationship rel
ON DUPLICATE KEY UPDATE
    claim_no = VALUES(claim_no), policy_id = VALUES(policy_id), subject_ref = VALUES(subject_ref),
    event_on = VALUES(event_on), submitted_at = VALUES(submitted_at),
    decided_at = VALUES(decided_at), status = 'paid',
    billed_amount = VALUES(billed_amount), approved_amount = VALUES(approved_amount),
    paid_amount = VALUES(paid_amount), metadata_json = VALUES(metadata_json),
    updated_at = VALUES(updated_at);

DELETE audit_event
FROM rehealth_insurance_audit_event audit_event
WHERE audit_event.request_id LIKE 'miqa-assignment-%'
  AND JSON_UNQUOTE(JSON_EXTRACT(audit_event.metadata_json, '$.sourceSystem')) = 'LOCAL_MULTI_INSURER_APP_QA';

DELETE manager
FROM rehealth_insurance_subject_manager manager
WHERE manager.source_system = 'LOCAL_MULTI_INSURER_APP_QA';

INSERT INTO rehealth_insurance_subject_manager (
    id, tenant_id, manager_user_id, department_id, subject_ref,
    status, source_system, created_at, updated_at
)
SELECT
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:assignment:', assignment.tenant_id, ':', assignment.staff_username, ':', rel.username), 256)),
    assignment.tenant_id, staff.id, department.id,
    LOWER(SHA2(CONCAT(assignment.tenant_id, ':', LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_APP_QA:user:', rel.username)))), 256)),
    'active', 'LOCAL_MULTI_INSURER_APP_QA', @seed_time, @seed_time
FROM tmp_miqa_assignment assignment
JOIN tmp_miqa_app_relationship rel
  ON rel.tenant_id = assignment.tenant_id AND rel.member_no = assignment.member_no
JOIN sys_user staff ON staff.username = assignment.staff_username AND staff.status = 1 AND staff.del_flag = 0
JOIN sys_user_depart staff_department ON staff_department.user_id = staff.id
JOIN sys_depart department
  ON department.id = staff_department.dep_id
 AND department.tenant_id = assignment.tenant_id
 AND department.status = '1' AND department.del_flag = '0'
WHERE 1 = 1
ON DUPLICATE KEY UPDATE
    manager_user_id = VALUES(manager_user_id), department_id = VALUES(department_id),
    subject_ref = VALUES(subject_ref), status = 'active', updated_at = VALUES(updated_at);

INSERT INTO rehealth_insurance_audit_event (
    id, tenant_id, actor_user_id, action, resource_type, resource_id,
    request_id, before_hash, after_hash, metadata_json, created_at
)
SELECT
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:audit-assignment:', assignment.tenant_id, ':', assignment.staff_username, ':', rel.username), 256)),
    assignment.tenant_id, admin_user.id, 'ASSIGN_RESPONSIBLE_STAFF',
    'insurance_subject_manager',
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:assignment:', assignment.tenant_id, ':', assignment.staff_username, ':', rel.username), 256)),
    CONCAT('miqa-assignment-', assignment.tenant_id, '-', assignment.staff_username, '-', assignment.member_no),
    NULL,
    LOWER(SHA2(CONCAT(assignment.tenant_id, ':', assignment.staff_username, ':', rel.username, ':active'), 256)),
    JSON_OBJECT(
        'sourceSystem', 'LOCAL_MULTI_INSURER_APP_QA',
        'staffUsername', assignment.staff_username,
        'appUsername', rel.username,
        'synthetic', TRUE
    ), @seed_time
FROM tmp_miqa_assignment assignment
JOIN tmp_miqa_app_relationship rel
  ON rel.tenant_id = assignment.tenant_id AND rel.member_no = assignment.member_no
JOIN sys_user admin_user
  ON admin_user.username = CONCAT('local_ins_', assignment.tenant_id, '_admin')
WHERE 1 = 1
ON DUPLICATE KEY UPDATE
    actor_user_id = VALUES(actor_user_id), action = VALUES(action),
    resource_id = VALUES(resource_id), request_id = VALUES(request_id),
    after_hash = VALUES(after_hash), metadata_json = VALUES(metadata_json),
    created_at = VALUES(created_at);

COMMIT;

DROP TEMPORARY TABLE IF EXISTS tmp_miqa_assignment;
DROP TEMPORARY TABLE IF EXISTS tmp_miqa_risk_day;
DROP TEMPORARY TABLE IF EXISTS tmp_miqa_app_relationship;
DROP TEMPORARY TABLE IF EXISTS tmp_miqa_app_user;

-- ============================================================================
-- 原始来源：backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/testdata/software/mysql/seed-versioned-care-plan-test-data.sql
-- ============================================================================
-- LOCAL_VERSIONED_CARE_PLAN_QA
--
-- Repeatable local-only fixtures for the versioned institution care-plan schema.
-- This file is intentionally outside db/software/mysql so Flyway never executes
-- local fixtures automatically. It is loaded only by the guarded deploy runner.
-- Prerequisite: seed-multi-insurer-app-user-test-data.ps1 has created the 36
-- LOCAL_MULTI_INSURER_APP_QA insurer-subject relationships for tenants 9101-9103.
-- The PowerShell wrapper performs schema, ownership and collision checks first.

DROP TEMPORARY TABLE IF EXISTS tmp_versioned_care_plan_subject;
CREATE TEMPORARY TABLE tmp_versioned_care_plan_subject (
    tenant_id INT NOT NULL,
    subject_ref CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    rehealth_user_id VARCHAR(64) NOT NULL,
    source_record_id VARCHAR(128) NOT NULL,
    actor_user_id VARCHAR(64) NOT NULL,
    plan_id VARCHAR(64) NOT NULL,
    revision_id VARCHAR(64) NOT NULL,
    content_hash CHAR(64) CHARACTER SET ascii COLLATE ascii_bin NOT NULL,
    PRIMARY KEY (tenant_id, subject_ref),
    UNIQUE KEY uk_tmp_versioned_plan (plan_id),
    UNIQUE KEY uk_tmp_versioned_revision (revision_id)
) ENGINE=InnoDB COMMENT='当前会话的版本化计划测试数据对象清单';

INSERT INTO tmp_versioned_care_plan_subject (
    tenant_id, subject_ref, rehealth_user_id, source_record_id, actor_user_id,
    plan_id, revision_id, content_hash
)
SELECT subject.tenant_id,
       subject.subject_ref,
       subject.rehealth_user_id,
       subject.source_record_id,
       actor.id,
       LOWER(SHA2(CONCAT('LOCAL_VERSIONED_CARE_PLAN_QA:plan:', subject.tenant_id, ':', subject.subject_ref), 256)),
       LOWER(SHA2(CONCAT('LOCAL_VERSIONED_CARE_PLAN_QA:revision:1:', subject.tenant_id, ':', subject.subject_ref), 256)),
       LOWER(SHA2(CONCAT('LOCAL_VERSIONED_CARE_PLAN_QA:content:1:', subject.tenant_id, ':', subject.subject_ref), 256))
FROM rehealth_insurance_subject subject
JOIN sys_user actor
  ON actor.username = CONCAT('local_ins_', subject.tenant_id, '_admin')
 AND actor.status = 1
 AND actor.del_flag = 0
JOIN sys_user_tenant membership
  ON membership.user_id = actor.id
 AND membership.tenant_id = subject.tenant_id
 AND membership.status = 1
WHERE subject.source_system = 'LOCAL_MULTI_INSURER_APP_QA'
  AND subject.tenant_id IN (9101, 9102, 9103)
  AND subject.enrollment_status = 'active';

INSERT INTO rehealth_care_plan (
    id, tenant_id, owner_type, owner_org_ref, subject_ref, rehealth_user_id,
    source_plan_id, status, current_revision_id, draft_revision_id, lock_version,
    created_by, created_at, updated_by, updated_at
)
SELECT plan_id,
       tenant_id,
       'insurance',
       CAST(tenant_id AS CHAR),
       subject_ref,
       rehealth_user_id,
       CONCAT('LOCAL_VERSIONED_CARE_PLAN_QA:', tenant_id, ':', source_record_id),
       'active',
       revision_id,
       NULL,
       1,
       actor_user_id,
       @seed_time,
       actor_user_id,
       @seed_time
FROM tmp_versioned_care_plan_subject
ON DUPLICATE KEY UPDATE
    owner_type = VALUES(owner_type),
    owner_org_ref = VALUES(owner_org_ref),
    subject_ref = VALUES(subject_ref),
    rehealth_user_id = VALUES(rehealth_user_id),
    source_plan_id = VALUES(source_plan_id),
    status = VALUES(status),
    current_revision_id = VALUES(current_revision_id),
    draft_revision_id = VALUES(draft_revision_id),
    lock_version = VALUES(lock_version),
    updated_by = VALUES(updated_by),
    updated_at = VALUES(updated_at);

INSERT INTO rehealth_care_plan_revision (
    id, tenant_id, plan_id, revision_no, status, title, summary, change_reason,
    content_hash, effective_from, effective_to, published_by, published_at,
    withdrawn_by, withdrawn_at, created_by, created_at, updated_by, updated_at
)
SELECT revision_id,
       tenant_id,
       plan_id,
       1,
       'published',
       CASE MOD(tenant_id, 3)
           WHEN 0 THEN '日常活力提升计划'
           WHEN 1 THEN '心血管健康管理计划'
           ELSE '生活方式改善计划'
       END,
       '通过规律活动、均衡饮食和周期回顾，逐步形成可持续的健康习惯。',
       '首次制定健康管理计划',
       content_hash,
       TIMESTAMP(@anchor_date, '00:00:00'),
       NULL,
       actor_user_id,
       @seed_time,
       NULL,
       NULL,
       actor_user_id,
       @seed_time,
       actor_user_id,
       @seed_time
FROM tmp_versioned_care_plan_subject
ON DUPLICATE KEY UPDATE
    status = VALUES(status),
    title = VALUES(title),
    summary = VALUES(summary),
    change_reason = VALUES(change_reason),
    content_hash = VALUES(content_hash),
    effective_from = VALUES(effective_from),
    effective_to = VALUES(effective_to),
    published_by = VALUES(published_by),
    published_at = VALUES(published_at),
    withdrawn_by = VALUES(withdrawn_by),
    withdrawn_at = VALUES(withdrawn_at),
    updated_by = VALUES(updated_by),
    updated_at = VALUES(updated_at);

INSERT INTO rehealth_care_plan_item (
    id, tenant_id, plan_id, revision_id, logical_item_id, category, title,
    instructions, schedule_json, scoring_weight, allow_not_applicable,
    display_order, created_at
)
SELECT LOWER(SHA2(CONCAT('LOCAL_VERSIONED_CARE_PLAN_QA:item:', definition.item_no, ':', seeded.tenant_id, ':', seeded.subject_ref), 256)),
       seeded.tenant_id,
       seeded.plan_id,
       seeded.revision_id,
       LOWER(SHA2(CONCAT('LOCAL_VERSIONED_CARE_PLAN_QA:logical-item:', definition.item_no, ':', seeded.tenant_id, ':', seeded.subject_ref), 256)),
       definition.category,
       definition.title,
       definition.instructions,
       definition.schedule_json,
       definition.scoring_weight,
       1,
       definition.item_no,
       @seed_time
FROM tmp_versioned_care_plan_subject seeded
CROSS JOIN (
    SELECT 1 item_no,
           'exercise' category,
           '规律舒缓活动' title,
           '结合自身状态完成适度活动；如有明显不适，请停止并咨询专业人员。' instructions,
           '{"type":"weekly","days":[1,3,5],"time":"19:00","timezone":"Asia/Shanghai"}' schedule_json,
           1.000 scoring_weight
    UNION ALL
    SELECT 2,
           'nutrition',
           '均衡饮食记录',
           '记录主要餐食并保持饮食多样化，按个人实际情况逐步调整。',
           '{"type":"daily","time":"20:00","timezone":"Asia/Shanghai"}',
           1.000
    UNION ALL
    SELECT 3,
           'follow_up',
           '每周健康回顾',
           '回顾本周执行感受和身体状态，有疑问时联系健康管理人员。',
           '{"type":"weekly","days":[7],"time":"10:00","timezone":"Asia/Shanghai"}',
           0.500
) definition
WHERE 1 = 1
ON DUPLICATE KEY UPDATE
    category = VALUES(category),
    title = VALUES(title),
    instructions = VALUES(instructions),
    schedule_json = VALUES(schedule_json),
    scoring_weight = VALUES(scoring_weight),
    allow_not_applicable = VALUES(allow_not_applicable),
    display_order = VALUES(display_order),
    created_at = VALUES(created_at);

INSERT INTO rehealth_care_plan_occurrence (
    id, tenant_id, plan_id, revision_id, plan_item_id, logical_item_id,
    subject_ref, scheduled_at, due_at, status, exclusion_reason, created_at, updated_at
)
SELECT LOWER(SHA2(CONCAT('LOCAL_VERSIONED_CARE_PLAN_QA:occurrence:', definition.item_no, ':', seeded.tenant_id, ':', seeded.subject_ref), 256)),
       seeded.tenant_id,
       seeded.plan_id,
       seeded.revision_id,
       LOWER(SHA2(CONCAT('LOCAL_VERSIONED_CARE_PLAN_QA:item:', definition.item_no, ':', seeded.tenant_id, ':', seeded.subject_ref), 256)),
       LOWER(SHA2(CONCAT('LOCAL_VERSIONED_CARE_PLAN_QA:logical-item:', definition.item_no, ':', seeded.tenant_id, ':', seeded.subject_ref), 256)),
       seeded.subject_ref,
       TIMESTAMP(DATE_ADD(@anchor_date, INTERVAL definition.day_offset DAY), definition.scheduled_time),
       DATE_ADD(TIMESTAMP(DATE_ADD(@anchor_date, INTERVAL definition.day_offset DAY), definition.scheduled_time), INTERVAL 2 HOUR),
       'scheduled',
       NULL,
       @seed_time,
       @seed_time
FROM tmp_versioned_care_plan_subject seeded
CROSS JOIN (
    SELECT 1 item_no, 1 day_offset, '19:00:00' scheduled_time
    UNION ALL SELECT 2, 2, '20:00:00'
    UNION ALL SELECT 3, 3, '10:00:00'
) definition
WHERE 1 = 1
ON DUPLICATE KEY UPDATE
    subject_ref = VALUES(subject_ref),
    scheduled_at = VALUES(scheduled_at),
    due_at = VALUES(due_at),
    status = VALUES(status),
    exclusion_reason = VALUES(exclusion_reason),
    updated_at = VALUES(updated_at);

INSERT INTO rehealth_care_plan_audit_event (
    id, tenant_id, owner_type, actor_user_id, action, plan_id, revision_id,
    before_hash, after_hash, reason, created_at
)
SELECT LOWER(SHA2(CONCAT('LOCAL_VERSIONED_CARE_PLAN_QA:audit:', action_seed.action_order, ':', seeded.tenant_id, ':', seeded.subject_ref), 256)),
       seeded.tenant_id,
       'insurance',
       seeded.actor_user_id,
       action_seed.action,
       seeded.plan_id,
       seeded.revision_id,
       CASE WHEN action_seed.action = 'publish' THEN seeded.content_hash ELSE NULL END,
       seeded.content_hash,
       '首次制定健康管理计划',
       DATE_ADD(@seed_time, INTERVAL action_seed.offset_second SECOND)
FROM tmp_versioned_care_plan_subject seeded
CROSS JOIN (
    SELECT 1 action_order, 'create_draft' action, 0 offset_second
    UNION ALL SELECT 2, 'publish', 1
) action_seed
WHERE 1 = 1
ON DUPLICATE KEY UPDATE
    actor_user_id = VALUES(actor_user_id),
    action = VALUES(action),
    before_hash = VALUES(before_hash),
    after_hash = VALUES(after_hash),
    reason = VALUES(reason),
    created_at = VALUES(created_at);

DROP TEMPORARY TABLE tmp_versioned_care_plan_subject;

-- ============================================================================
-- 原始来源：backend/deploy/rehealth/scripts/seed-insurance-workflow-test-data.sql
-- ============================================================================
-- Local-only insurer workflow acceptance fixtures.
--
-- This script is intentionally repeatable. All business rows use the explicit
-- source marker LOCAL_INSURANCE_QA and contain synthetic, non-clinical data.
-- Never apply it to staging or production and never use the seeded risk values
-- for medical, underwriting, claim or settlement decisions.

SET NAMES utf8mb4;

SET @seed_actor_id = (
    SELECT id
    FROM sys_user
    WHERE username = @seed_actor
      AND del_flag = 0
    LIMIT 1
);

SET @manager_password_01 = '5b7e5dd2a7afdf0696c03f9b9ffed4d2c9ed320884af19e8f9474bd3a789f2e8';
SET @manager_password_02 = '5b7e5dd2a7afdf0696c03f9b9ffed4d2c9ed320884af19e8d97e8d651f5670e1';

DROP TEMPORARY TABLE IF EXISTS tmp_local_insurance_qa_cohort;
CREATE TEMPORARY TABLE tmp_local_insurance_qa_cohort (
    member_no INT NOT NULL PRIMARY KEY,
    cohort_group VARCHAR(16) NOT NULL,
    age SMALLINT NOT NULL,
    gender VARCHAR(16) NOT NULL,
    height_cm DECIMAL(6,2) NOT NULL,
    weight_kg DECIMAL(6,2) NOT NULL,
    bmi DECIMAL(5,2) NOT NULL,
    risk_score DECIMAL(8,6) NOT NULL,
    risk_level VARCHAR(16) NOT NULL,
    paid_amount DECIMAL(18,2) NOT NULL
);

INSERT INTO tmp_local_insurance_qa_cohort
    (member_no, cohort_group, age, gender, height_cm, weight_kg, bmi, risk_score, risk_level, paid_amount)
VALUES
    (1,  'treated', 44, 'female', 165.00, 63.50, 23.32, 0.310000, 'low',    1800.00),
    (2,  'treated', 49, 'male',   174.00, 77.00, 25.43, 0.460000, 'medium', 3200.00),
    (3,  'treated', 53, 'female', 160.00, 69.40, 27.11, 0.570000, 'medium', 4600.00),
    (4,  'treated', 57, 'male',   172.00, 83.75, 28.31, 0.690000, 'high',   7100.00),
    (5,  'treated', 61, 'female', 158.00, 73.10, 29.28, 0.760000, 'high',   8800.00),
    (6,  'treated', 64, 'male',   170.00, 88.40, 30.59, 0.830000, 'high',  11200.00),
    (7,  'control', 45, 'female', 165.00, 63.55, 23.34, 0.320000, 'low',    4100.00),
    (8,  'control', 48, 'male',   174.00, 76.95, 25.42, 0.450000, 'medium', 6500.00),
    (9,  'control', 54, 'female', 160.00, 69.32, 27.08, 0.580000, 'medium', 9200.00),
    (10, 'control', 56, 'male',   172.00, 83.50, 28.22, 0.680000, 'high',  12800.00),
    (11, 'control', 60, 'female', 158.00, 73.08, 29.27, 0.750000, 'high',  15600.00),
    (12, 'control', 65, 'male',   170.00, 88.45, 30.61, 0.840000, 'high',  19200.00);

START TRANSACTION;

INSERT INTO sys_depart (
    id, parent_id, depart_name, depart_order, org_category, org_type,
    org_code, status, del_flag, create_by, create_time, update_by,
    update_time, tenant_id, iz_leaf
)
VALUES
    ('iqdep000000000000000000000001', NULL, '安和健康保险', 1, '1', '1',
     'AH01', '1', '0', @seed_actor_id, TIMESTAMP('2026-08-13 09:00:00'),
     @seed_actor_id, TIMESTAMP('2026-08-13 09:00:00'), @seed_tenant_id, 0),
    ('iqdep000000000000000000000002', 'iqdep000000000000000000000001', '健康险一部', 1, '2', '2',
     'AH0101', '1', '0', @seed_actor_id, TIMESTAMP('2026-08-13 09:00:00'),
     @seed_actor_id, TIMESTAMP('2026-08-13 09:00:00'), @seed_tenant_id, 1),
    ('iqdep000000000000000000000003', 'iqdep000000000000000000000001', '健康险二部', 2, '2', '2',
     'AH0102', '1', '0', @seed_actor_id, TIMESTAMP('2026-08-13 09:00:00'),
     @seed_actor_id, TIMESTAMP('2026-08-13 09:00:00'), @seed_tenant_id, 1)
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id),
    depart_name = VALUES(depart_name),
    org_code = VALUES(org_code),
    status = VALUES(status),
    del_flag = VALUES(del_flag),
    update_by = VALUES(update_by),
    update_time = VALUES(update_time),
    tenant_id = VALUES(tenant_id),
    iz_leaf = VALUES(iz_leaf);

INSERT INTO sys_user (
    id, username, realname, password, salt, sex, status, del_flag,
    create_by, create_time, update_by, update_time, user_identity,
    login_tenant_id, sort
)
VALUES
    ('iqmgr000000000000000000000001', 'local_insurance_manager_01', '陈立峰', @manager_password_01, 'QA260813', 1, 1, 0,
     @seed_actor_id, TIMESTAMP('2026-08-13 09:00:00'), @seed_actor_id, TIMESTAMP('2026-08-13 09:00:00'), 1, @seed_tenant_id, 8001),
    ('iqmgr000000000000000000000002', 'local_insurance_manager_02', '林雅雯', @manager_password_02, 'QA260813', 2, 1, 0,
     @seed_actor_id, TIMESTAMP('2026-08-13 09:00:00'), @seed_actor_id, TIMESTAMP('2026-08-13 09:00:00'), 1, @seed_tenant_id, 8002)
ON DUPLICATE KEY UPDATE
    realname = VALUES(realname),
    password = VALUES(password),
    salt = VALUES(salt),
    status = VALUES(status),
    del_flag = VALUES(del_flag),
    login_tenant_id = VALUES(login_tenant_id),
    update_by = VALUES(update_by),
    update_time = VALUES(update_time),
    login_tenant_id = VALUES(login_tenant_id),
    sort = VALUES(sort);

INSERT INTO sys_user_tenant (
    id, user_id, tenant_id, status, create_by, create_time, update_by, update_time
)
VALUES
    ('iqmt000000000000000000000001', 'iqmgr000000000000000000000001', @seed_tenant_id, '1', @seed_actor_id, TIMESTAMP('2026-08-13 09:00:00'), @seed_actor_id, TIMESTAMP('2026-08-13 09:00:00')),
    ('iqmt000000000000000000000002', 'iqmgr000000000000000000000002', @seed_tenant_id, '1', @seed_actor_id, TIMESTAMP('2026-08-13 09:00:00'), @seed_actor_id, TIMESTAMP('2026-08-13 09:00:00'))
ON DUPLICATE KEY UPDATE
    tenant_id = VALUES(tenant_id), status = VALUES(status), update_by = VALUES(update_by), update_time = VALUES(update_time);

INSERT INTO sys_user_depart (ID, user_id, dep_id)
VALUES
    ('iqmd000000000000000000000001', 'iqmgr000000000000000000000001', 'iqdep000000000000000000000002'),
    ('iqmd000000000000000000000002', 'iqmgr000000000000000000000002', 'iqdep000000000000000000000003')
ON DUPLICATE KEY UPDATE dep_id = VALUES(dep_id);

INSERT INTO sys_user_role (id, user_id, role_id, tenant_id)
SELECT 'iqmr000000000000000000000001', 'iqmgr000000000000000000000001', id, @seed_tenant_id
FROM sys_role WHERE role_code = 'insurer_analyst'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id), tenant_id = VALUES(tenant_id);

INSERT INTO sys_user_role (id, user_id, role_id, tenant_id)
SELECT 'iqmr000000000000000000000002', 'iqmgr000000000000000000000002', id, @seed_tenant_id
FROM sys_role WHERE role_code = 'insurer_analyst'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id), tenant_id = VALUES(tenant_id);

-- Activates department/subject scope enforcement while retaining analyst
-- permissions for the local workflow demo accounts.
INSERT INTO sys_user_role (id, user_id, role_id, tenant_id)
SELECT 'iqms000000000000000000000001', 'iqmgr000000000000000000000001', id, @seed_tenant_id
FROM sys_role WHERE role_code = 'insurance_department_manager'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id), tenant_id = VALUES(tenant_id);

INSERT INTO sys_user_role (id, user_id, role_id, tenant_id)
SELECT 'iqms000000000000000000000002', 'iqmgr000000000000000000000002', id, @seed_tenant_id
FROM sys_role WHERE role_code = 'insurance_department_manager'
ON DUPLICATE KEY UPDATE role_id = VALUES(role_id), tenant_id = VALUES(tenant_id);

INSERT INTO sys_user (
    id, username, realname, password, salt, sex, status, del_flag,
    create_by, create_time, update_by, update_time, user_identity,
    login_tenant_id, sort
)
SELECT
    CONCAT('iq', LPAD(member_no, 30, '0')),
    CONCAT('local_insurance_qa_', LPAD(member_no, 2, '0')),
    ELT(member_no, '张明远', '李慧敏', '王建国', '陈玉兰', '刘志强', '周婉婷', '赵国庆', '孙晓梅', '吴志远', '郑丽华', '胡建新', '林秀珍'),
    NULL,
    NULL,
    CASE WHEN gender = 'male' THEN 1 ELSE 2 END,
    1,
    0,
    @seed_actor_id,
    TIMESTAMP('2026-08-13 09:00:00'),
    @seed_actor_id,
    TIMESTAMP('2026-08-13 09:00:00'),
    1,
    @seed_tenant_id,
    9000 + member_no
FROM tmp_local_insurance_qa_cohort
ON DUPLICATE KEY UPDATE
    realname = VALUES(realname),
    sex = VALUES(sex),
    status = VALUES(status),
    del_flag = VALUES(del_flag),
    login_tenant_id = VALUES(login_tenant_id),
    update_by = VALUES(update_by),
    update_time = VALUES(update_time),
    sort = VALUES(sort);

INSERT INTO sys_user_tenant (
    id, user_id, tenant_id, status, create_by, create_time, update_by, update_time
)
SELECT
    CONCAT('it', LPAD(member_no, 30, '0')),
    CONCAT('iq', LPAD(member_no, 30, '0')),
    @seed_tenant_id,
    '1',
    @seed_actor_id,
    TIMESTAMP('2026-08-13 09:00:00'),
    @seed_actor_id,
    TIMESTAMP('2026-08-13 09:00:00')
FROM tmp_local_insurance_qa_cohort
ON DUPLICATE KEY UPDATE
    tenant_id = VALUES(tenant_id),
    status = VALUES(status),
    update_by = VALUES(update_by),
    update_time = VALUES(update_time);

INSERT INTO rehealth_patient_profile (
    id, user_id, name, gender, age, height_cm, weight_kg, bmi,
    family_history, smoking, drinking, diabetes_history,
    hypertension_history, profile_version, created_at, updated_at
)
SELECT
    CONCAT('ip', LPAD(member_no, 30, '0')),
    CONCAT('iq', LPAD(member_no, 30, '0')),
    ELT(member_no, '张明远', '李慧敏', '王建国', '陈玉兰', '刘志强', '周婉婷', '赵国庆', '孙晓梅', '吴志远', '郑丽华', '胡建新', '林秀珍'),
    gender,
    age,
    height_cm,
    weight_kg,
    bmi,
    CASE WHEN member_no IN (3, 4, 5, 6, 9, 10, 11, 12) THEN 1 ELSE 0 END,
    CASE WHEN member_no IN (4, 6, 10, 12) THEN 1 ELSE 0 END,
    0,
    CASE WHEN member_no IN (5, 6, 11, 12) THEN 1 ELSE 0 END,
    CASE WHEN member_no IN (3, 4, 5, 6, 9, 10, 11, 12) THEN 1 ELSE 0 END,
    1,
    TIMESTAMP('2025-12-01 09:00:00'),
    TIMESTAMP('2025-12-01 09:00:00')
FROM tmp_local_insurance_qa_cohort
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    gender = VALUES(gender),
    age = VALUES(age),
    height_cm = VALUES(height_cm),
    weight_kg = VALUES(weight_kg),
    bmi = VALUES(bmi),
    family_history = VALUES(family_history),
    smoking = VALUES(smoking),
    drinking = VALUES(drinking),
    diabetes_history = VALUES(diabetes_history),
    hypertension_history = VALUES(hypertension_history),
    updated_at = VALUES(updated_at);

INSERT INTO rehealth_cvd_feature_vector (
    id, user_id, request_id, feature_schema_version, feature_json,
    quality_json, payload_json, created_at
)
SELECT
    CONCAT('if', LPAD(member_no, 30, '0')),
    CONCAT('iq', LPAD(member_no, 30, '0')),
    CONCAT('local-insurance-qa-risk-', LPAD(member_no, 2, '0')),
    'cvd-16-v1',
    JSON_OBJECT(
        'age', age,
        'gender', gender,
        'bmi', bmi,
        'familyHistory', CASE WHEN member_no IN (3, 4, 5, 6, 9, 10, 11, 12) THEN 1 ELSE 0 END,
        'smoking', CASE WHEN member_no IN (4, 6, 10, 12) THEN 1 ELSE 0 END,
        'diabetesHistory', CASE WHEN member_no IN (5, 6, 11, 12) THEN 1 ELSE 0 END,
        'hypertensionHistory', CASE WHEN member_no IN (3, 4, 5, 6, 9, 10, 11, 12) THEN 1 ELSE 0 END
    ),
    JSON_OBJECT(
        'fixture', true,
        'source', 'LOCAL_INSURANCE_QA',
        'clinicalUseAllowed', false
    ),
    JSON_OBJECT(
        'fixture', true,
        'sourceSystem', 'LOCAL_INSURANCE_QA',
        'memberNo', member_no
    ),
    DATE_ADD(TIMESTAMP('2025-12-15 08:00:00'), INTERVAL member_no MINUTE)
FROM tmp_local_insurance_qa_cohort
ON DUPLICATE KEY UPDATE
    feature_json = VALUES(feature_json),
    quality_json = VALUES(quality_json),
    payload_json = VALUES(payload_json),
    created_at = VALUES(created_at);

INSERT INTO rehealth_cvd_risk_result (
    id, feature_vector_id, user_id, request_id, feature_schema_version,
    model_version, scorer_mode, is_mock, artifact_name, fallback_reason,
    contribution_method, risk_score, risk_level, contribution_json,
    missing_fields_json, quality_warnings_json, summary, response_json,
    evaluated_at, created_at
)
SELECT
    CONCAT('ir', LPAD(member_no, 30, '0')),
    CONCAT('if', LPAD(member_no, 30, '0')),
    CONCAT('iq', LPAD(member_no, 30, '0')),
    CONCAT('local-insurance-qa-risk-', LPAD(member_no, 2, '0')),
    'cvd-16-v1',
    'local-qa-seeded-nonclinical-v1',
    'local_qa_fixture',
    0,
    'LOCAL_INSURANCE_QA_NOT_A_MODEL',
    NULL,
    'fixture',
    risk_score,
    risk_level,
    JSON_OBJECT(
        'fixture', true,
        'factors', JSON_ARRAY('synthetic age', 'synthetic BMI')
    ),
    JSON_ARRAY(),
    JSON_ARRAY('风险结果仅供健康管理参考，不用于临床或保险决策'),
    '风险评估结果仅供健康管理参考，不用于诊断、核保、理赔或结算决策。',
    JSON_OBJECT(
        'fixture', true,
        'sourceSystem', 'LOCAL_INSURANCE_QA',
        'clinicalUseAllowed', false,
        'riskScore', risk_score,
        'riskLevel', risk_level,
        'modelVersion', 'local-qa-seeded-nonclinical-v1'
    ),
    DATE_ADD(TIMESTAMP('2025-12-15 08:00:00'), INTERVAL member_no MINUTE),
    DATE_ADD(TIMESTAMP('2025-12-15 08:00:00'), INTERVAL member_no MINUTE)
FROM tmp_local_insurance_qa_cohort
ON DUPLICATE KEY UPDATE
    model_version = VALUES(model_version),
    scorer_mode = VALUES(scorer_mode),
    is_mock = VALUES(is_mock),
    artifact_name = VALUES(artifact_name),
    risk_score = VALUES(risk_score),
    risk_level = VALUES(risk_level),
    contribution_json = VALUES(contribution_json),
    quality_warnings_json = VALUES(quality_warnings_json),
    summary = VALUES(summary),
    response_json = VALUES(response_json),
    evaluated_at = VALUES(evaluated_at),
    created_at = VALUES(created_at);

INSERT INTO rehealth_insurance_subject (
    id, tenant_id, subject_ref, rehealth_user_id,
    external_subject_ref_hash, enrollment_status, consent_status,
    consent_version, consented_at, source_system, source_record_id,
    metadata_json, created_at, updated_at
)
SELECT
    CONCAT('is', LPAD(member_no, 30, '0')),
    @seed_tenant_id,
    SHA2(CONCAT(@seed_tenant_id, ':', CONCAT('iq', LPAD(member_no, 30, '0'))), 256),
    CONCAT('iq', LPAD(member_no, 30, '0')),
    SHA2(CONCAT('LOCAL_INSURANCE_QA:', member_no), 256),
    'active',
    'granted',
    'local-qa-v1',
    TIMESTAMP('2025-01-01 09:00:00'),
    'LOCAL_INSURANCE_QA',
    CONCAT('subject-', LPAD(member_no, 2, '0')),
    JSON_OBJECT(
        'fixture', true,
        'cohortGroup', cohort_group,
        'clinicalUseAllowed', false
    ),
    TIMESTAMP('2025-01-01 09:00:00'),
    TIMESTAMP('2026-08-13 09:00:00')
FROM tmp_local_insurance_qa_cohort
ON DUPLICATE KEY UPDATE
    subject_ref = VALUES(subject_ref),
    rehealth_user_id = VALUES(rehealth_user_id),
    enrollment_status = VALUES(enrollment_status),
    consent_status = VALUES(consent_status),
    consent_version = VALUES(consent_version),
    consented_at = VALUES(consented_at),
    metadata_json = VALUES(metadata_json),
    updated_at = VALUES(updated_at);

INSERT INTO rehealth_insurance_subject_manager (
    id, tenant_id, manager_user_id, department_id, subject_ref,
    status, source_system, created_at, updated_at
)
SELECT
    CONCAT('iqassign', LPAD(member_no, 24, '0')),
    @seed_tenant_id,
    CASE WHEN member_no <= 6 THEN 'iqmgr000000000000000000000001' ELSE 'iqmgr000000000000000000000002' END,
    CASE WHEN member_no <= 6 THEN 'iqdep000000000000000000000002' ELSE 'iqdep000000000000000000000003' END,
    SHA2(CONCAT(@seed_tenant_id, ':', CONCAT('iq', LPAD(member_no, 30, '0'))), 256),
    'active',
    'LOCAL_INSURANCE_QA',
    TIMESTAMP('2026-08-13 09:00:00'),
    TIMESTAMP('2026-08-13 09:00:00')
FROM tmp_local_insurance_qa_cohort
ON DUPLICATE KEY UPDATE
    department_id = VALUES(department_id),
    status = VALUES(status),
    updated_at = VALUES(updated_at);

INSERT INTO rehealth_insurance_policy (
    id, tenant_id, policy_no, product_code, product_name, policy_type,
    policyholder_subject_ref, insured_subject_ref, coverage_amount,
    premium_amount, deductible_amount, waiting_period_days, effective_on,
    expires_on, status, source_system, source_record_id, metadata_json,
    created_at, updated_at
)
SELECT
    CONCAT('iy', LPAD(member_no, 30, '0')),
    @seed_tenant_id,
    CONCAT('AH-2025-CVD-', LPAD(member_no, 4, '0')),
    'AH-CVD-MGMT',
    '心血管健康管理计划',
    'health_management',
    SHA2(CONCAT(@seed_tenant_id, ':', CONCAT('iq', LPAD(member_no, 30, '0'))), 256),
    SHA2(CONCAT(@seed_tenant_id, ':', CONCAT('iq', LPAD(member_no, 30, '0'))), 256),
    200000.00,
    1200.00 + member_no * 20,
    500.00,
    30,
    DATE('2025-01-01'),
    DATE('2026-12-31'),
    'active',
    'LOCAL_INSURANCE_QA',
    CONCAT('policy-', LPAD(member_no, 2, '0')),
    JSON_OBJECT('fixture', true, 'clinicalUseAllowed', false),
    TIMESTAMP('2025-01-01 09:00:00'),
    TIMESTAMP('2026-08-13 09:00:00')
FROM tmp_local_insurance_qa_cohort
ON DUPLICATE KEY UPDATE
    policy_no = VALUES(policy_no),
    product_code = VALUES(product_code),
    product_name = VALUES(product_name),
    policy_type = VALUES(policy_type),
    insured_subject_ref = VALUES(insured_subject_ref),
    coverage_amount = VALUES(coverage_amount),
    premium_amount = VALUES(premium_amount),
    effective_on = VALUES(effective_on),
    expires_on = VALUES(expires_on),
    status = VALUES(status),
    metadata_json = VALUES(metadata_json),
    updated_at = VALUES(updated_at);

INSERT INTO rehealth_insurance_coverage (
    id, tenant_id, policy_id, subject_ref, coverage_code, coverage_name,
    limit_amount, deductible_amount, effective_on, expires_on, status,
    source_system, source_record_id, metadata_json, created_at, updated_at
)
SELECT
    CONCAT('ic', LPAD(member_no, 30, '0')),
    @seed_tenant_id,
    CONCAT('iy', LPAD(member_no, 30, '0')),
    SHA2(CONCAT(@seed_tenant_id, ':', CONCAT('iq', LPAD(member_no, 30, '0'))), 256),
    'CVD-MGMT',
    '心血管健康管理保障',
    200000.00,
    500.00,
    DATE('2025-01-01'),
    DATE('2026-12-31'),
    'active',
    'LOCAL_INSURANCE_QA',
    CONCAT('coverage-', LPAD(member_no, 2, '0')),
    JSON_OBJECT('fixture', true),
    TIMESTAMP('2025-01-01 09:00:00'),
    TIMESTAMP('2026-08-13 09:00:00')
FROM tmp_local_insurance_qa_cohort
ON DUPLICATE KEY UPDATE
    policy_id = VALUES(policy_id),
    subject_ref = VALUES(subject_ref),
    coverage_code = VALUES(coverage_code),
    coverage_name = VALUES(coverage_name),
    limit_amount = VALUES(limit_amount),
    status = VALUES(status),
    metadata_json = VALUES(metadata_json),
    updated_at = VALUES(updated_at);

INSERT INTO rehealth_insurance_consent (
    id, tenant_id, subject_ref, consent_type, consent_version, status,
    granted_at, revoked_at, evidence_ref, evidence_hash, source_system,
    source_record_id, metadata_json, created_at, updated_at
)
SELECT
    CONCAT('in', LPAD(member_no, 30, '0')),
    @seed_tenant_id,
    SHA2(CONCAT(@seed_tenant_id, ':', CONCAT('iq', LPAD(member_no, 30, '0'))), 256),
    'insurance_analytics',
    'local-qa-v1',
    'granted',
    TIMESTAMP('2025-01-01 09:00:00'),
    NULL,
    CONCAT('AH-CONSENT-', LPAD(member_no, 4, '0')),
    SHA2(CONCAT('LOCAL_INSURANCE_QA_CONSENT:', member_no), 256),
    'LOCAL_INSURANCE_QA',
    CONCAT('consent-', LPAD(member_no, 2, '0')),
    JSON_OBJECT('fixture', true, 'rawHealthDataShared', false),
    TIMESTAMP('2025-01-01 09:00:00'),
    TIMESTAMP('2026-08-13 09:00:00')
FROM tmp_local_insurance_qa_cohort
ON DUPLICATE KEY UPDATE
    status = VALUES(status),
    granted_at = VALUES(granted_at),
    revoked_at = VALUES(revoked_at),
    evidence_ref = VALUES(evidence_ref),
    evidence_hash = VALUES(evidence_hash),
    metadata_json = VALUES(metadata_json),
    updated_at = VALUES(updated_at);

INSERT INTO rehealth_intervention_plan (
    id, user_id, plan_id, source_request_id, feature_schema_version,
    model_version, scorer_mode, is_mock, artifact_name,
    priority_intervention, rationale, expected_impact, confidence,
    medical_disclaimer, generated_at, response_json, created_at
)
SELECT
    CONCAT('ig', LPAD(member_no, 30, '0')),
    CONCAT('iq', LPAD(member_no, 30, '0')),
    CONCAT('local-insurance-qa-plan-', LPAD(member_no, 2, '0')),
    CONCAT('local-insurance-qa-risk-', LPAD(member_no, 2, '0')),
    'cvd-16-v1',
    'local-qa-seeded-nonclinical-v1',
    'local_qa_fixture',
    1,
    'LOCAL_INSURANCE_QA_NOT_A_MODEL',
    '每周完成经授权的健康管理任务',
    '结合近期健康指标与计划执行情况进行持续管理。',
    '持续完成计划有助于改善可干预健康行为。',
    0.50,
    '本计划仅用于健康管理参考，不构成医疗建议，不替代医生诊疗。',
    DATE_ADD(TIMESTAMP('2026-01-05 09:00:00'), INTERVAL member_no MINUTE),
    JSON_OBJECT(
        'fixture', true,
        'sourceSystem', 'LOCAL_INSURANCE_QA',
        'clinicalUseAllowed', false
    ),
    DATE_ADD(TIMESTAMP('2026-01-05 09:00:00'), INTERVAL member_no MINUTE)
FROM tmp_local_insurance_qa_cohort
WHERE cohort_group = 'treated'
ON DUPLICATE KEY UPDATE
    source_request_id = VALUES(source_request_id),
    model_version = VALUES(model_version),
    scorer_mode = VALUES(scorer_mode),
    is_mock = VALUES(is_mock),
    priority_intervention = VALUES(priority_intervention),
    rationale = VALUES(rationale),
    expected_impact = VALUES(expected_impact),
    medical_disclaimer = VALUES(medical_disclaimer),
    response_json = VALUES(response_json),
    generated_at = VALUES(generated_at);

INSERT INTO rehealth_insurance_intervention (
    id, tenant_id, subject_ref, plan_id, source_plan_id, consent_id,
    status, enrolled_at, ended_at, last_feedback_at, source_system,
    source_record_id, metadata_json, created_at, updated_at
)
SELECT
    CONCAT('ii', LPAD(member_no, 30, '0')),
    @seed_tenant_id,
    SHA2(CONCAT(@seed_tenant_id, ':', CONCAT('iq', LPAD(member_no, 30, '0'))), 256),
    CONCAT('local-insurance-qa-plan-', LPAD(member_no, 2, '0')),
    CONCAT('ig', LPAD(member_no, 30, '0')),
    CONCAT('in', LPAD(member_no, 30, '0')),
    'active',
    DATE_ADD(TIMESTAMP('2026-01-10 09:00:00'), INTERVAL member_no MINUTE),
    NULL,
    DATE_ADD(TIMESTAMP('2026-07-31 09:00:00'), INTERVAL member_no MINUTE),
    'LOCAL_INSURANCE_QA',
    CONCAT('intervention-', LPAD(member_no, 2, '0')),
    JSON_OBJECT(
        'fixture', true,
        'completionRate', ROUND(0.70 + member_no * 0.03, 2),
        'clinicalUseAllowed', false
    ),
    DATE_ADD(TIMESTAMP('2026-01-10 09:00:00'), INTERVAL member_no MINUTE),
    TIMESTAMP('2026-08-13 09:00:00')
FROM tmp_local_insurance_qa_cohort
WHERE cohort_group = 'treated'
ON DUPLICATE KEY UPDATE
    plan_id = VALUES(plan_id),
    source_plan_id = VALUES(source_plan_id),
    consent_id = VALUES(consent_id),
    status = VALUES(status),
    enrolled_at = VALUES(enrolled_at),
    ended_at = VALUES(ended_at),
    last_feedback_at = VALUES(last_feedback_at),
    metadata_json = VALUES(metadata_json),
    updated_at = VALUES(updated_at);

INSERT INTO rehealth_insurance_claim (
    id, tenant_id, claim_no, policy_id, subject_ref, claim_type,
    event_on, submitted_at, decided_at, status, billed_amount,
    approved_amount, paid_amount, currency, coverage_code, outcome_code,
    source_system, source_record_id, metadata_json, created_at, updated_at
)
SELECT
    CONCAT('il', LPAD(member_no, 30, '0')),
    @seed_tenant_id,
    CONCAT('AH-CLAIM-', LPAD(member_no, 4, '0')),
    CONCAT('iy', LPAD(member_no, 30, '0')),
    SHA2(CONCAT(@seed_tenant_id, ':', CONCAT('iq', LPAD(member_no, 30, '0'))), 256),
    CASE WHEN member_no % 3 = 0 THEN 'outpatient' WHEN member_no % 3 = 1 THEN 'inpatient' ELSE 'chronic' END,
    ADDDATE(DATE('2026-02-01'), member_no * 10),
    ADDDATE(TIMESTAMP('2026-02-02 10:00:00'), member_no * 10),
    ADDDATE(TIMESTAMP('2026-02-08 16:00:00'), member_no * 10),
    'paid',
    paid_amount * 1.20,
    paid_amount,
    paid_amount,
    'CNY',
    'CVD-MGMT',
    'paid',
    'LOCAL_INSURANCE_QA',
    CONCAT('claim-', LPAD(member_no, 2, '0')),
    JSON_OBJECT(
        'fixture', true,
        'cohortGroup', cohort_group,
        'clinicalUseAllowed', false
    ),
    ADDDATE(TIMESTAMP('2026-02-02 10:00:00'), member_no * 10),
    TIMESTAMP('2026-08-13 09:00:00')
FROM tmp_local_insurance_qa_cohort
ON DUPLICATE KEY UPDATE
    claim_no = VALUES(claim_no),
    policy_id = VALUES(policy_id),
    subject_ref = VALUES(subject_ref),
    claim_type = VALUES(claim_type),
    event_on = VALUES(event_on),
    submitted_at = VALUES(submitted_at),
    decided_at = VALUES(decided_at),
    status = VALUES(status),
    billed_amount = VALUES(billed_amount),
    approved_amount = VALUES(approved_amount),
    paid_amount = VALUES(paid_amount),
    metadata_json = VALUES(metadata_json),
    updated_at = VALUES(updated_at);

INSERT INTO rehealth_insurance_study (
    id, tenant_id, study_no, title, period_start, period_end,
    population_rule_json, intervention_rule_json, outcome_rule_json,
    methodology, status, model_version, created_by, approved_by,
    approved_at, created_at, updated_at
)
VALUES (
    'local-insurance-qa-study-2026',
    @seed_tenant_id,
    'AH-CVD-RWE-2026',
    '心血管健康管理项目效果评估研究',
    DATE('2026-01-01'),
    DATE('2026-08-13'),
    JSON_OBJECT(
        'sourceSystem', 'LOCAL_INSURANCE_QA',
        'consentStatus', 'granted',
        'activePolicyRequired', true,
        'clinicalUseAllowed', false
    ),
    JSON_OBJECT(
        'treated', 'active intervention enrolled in study period',
        'control', 'no active intervention in study period'
    ),
    JSON_OBJECT(
        'metric', 'paid claim amount in CNY',
        'warning', 'observational estimate; actuarial review required'
    ),
    'psm',
    'draft',
    'local-qa-psm-v1',
    @seed_actor_id,
    NULL,
    NULL,
    TIMESTAMP('2026-08-13 09:00:00'),
    TIMESTAMP('2026-08-13 09:00:00')
)
ON DUPLICATE KEY UPDATE
    study_no = VALUES(study_no),
    title = VALUES(title),
    period_start = VALUES(period_start),
    period_end = VALUES(period_end),
    population_rule_json = VALUES(population_rule_json),
    intervention_rule_json = VALUES(intervention_rule_json),
    outcome_rule_json = VALUES(outcome_rule_json),
    methodology = VALUES(methodology),
    status = VALUES(status),
    model_version = VALUES(model_version),
    updated_at = VALUES(updated_at);

COMMIT;

DROP TEMPORARY TABLE IF EXISTS tmp_local_insurance_qa_cohort;

-- ============================================================================
-- 原始来源：backend/deploy/rehealth/scripts/seed-medical-workspace-test-data.sql
-- ============================================================================
-- ReHealth medical workspace synthetic fixtures (MySQL 8.0+).
-- Source marker: LOCAL_MEDICAL_TEST_SEED
-- Scope: local development and isolated test environments only.
-- Safety: all people and contact details are fictional; model outputs are Mock.

SET NAMES utf8mb4;
SET @anchor_date = COALESCE(@anchor_date, CURRENT_DATE());
SET @seed_time = COALESCE(@seed_time, TIMESTAMP(@anchor_date, '10:00:00'));
SET @seed_actor = 'LOCAL_MEDICAL_TEST_SEED';

-- Fail closed if the fixed tenant or canonical permission IDs belong to other data.
DROP TEMPORARY TABLE IF EXISTS tmp_mhqa_guard;
CREATE TEMPORARY TABLE tmp_mhqa_guard (
    ok TINYINT NOT NULL,
    CONSTRAINT chk_tmp_mhqa_guard CHECK (ok = 1)
);
INSERT INTO tmp_mhqa_guard (ok)
SELECT IF(
    EXISTS (
        SELECT 1 FROM sys_tenant
        WHERE (id = 9261 AND name <> '睿禾滨江心血管健康管理中心（测试）')
           OR (id = 9262 AND name <> '睿禾南山慢病管理中心（测试）')
    ) OR EXISTS (
        SELECT 1 FROM sys_permission
        WHERE (id = '2030500000000000001' AND name <> 'ReHealth患者数据')
           OR (id = '2030500000000000002' AND perms <> 'rehealth:admin:patient:view')
    ) OR EXISTS (
        SELECT 1 FROM sys_user
        WHERE username REGEXP '^local_medical_(admin|doctor)(_nanshan)?$|^local_medical_patient_[0-9]{3}$'
          AND id <> LOWER(MD5(CONCAT('LOCAL_MEDICAL_TEST_SEED:user:', username)))
    ) OR EXISTS (
        SELECT 1 FROM sys_user
        WHERE (phone BETWEEN '00093000001' AND '00093000024'
               OR email REGEXP '^patient[0-9]{3}@medical\\.qa\\.invalid$')
          AND username NOT REGEXP '^local_medical_patient_[0-9]{3}$'
    ), 0, 1
);

DROP TEMPORARY TABLE IF EXISTS tmp_mhqa_staff;
CREATE TEMPORARY TABLE tmp_mhqa_staff (
    tenant_id INT NOT NULL,
    staff_no INT NOT NULL,
    username VARCHAR(100) NOT NULL,
    realname VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role_code VARCHAR(100) NOT NULL,
    department_code VARCHAR(64) NOT NULL,
    phone VARCHAR(45) NOT NULL,
    email VARCHAR(45) NOT NULL,
    PRIMARY KEY (tenant_id, staff_no),
    UNIQUE KEY uk_tmp_mhqa_staff_username (username)
);

INSERT INTO tmp_mhqa_staff (
    tenant_id, staff_no, username, realname, password_hash, role_code,
    department_code, phone, email
) VALUES
    (9261, 1, 'local_medical_admin', '滨江机构管理员（测试）',
     'd0631a538d2cf21a6ff88c2986345feecc9f80a9c1b49ca0', 'hospital_admin',
     'MHQA9261', '00092610001', 'admin.bj@medical.qa.invalid'),
    (9261, 2, 'local_medical_doctor', '王医生（测试）',
     'd0631a538d2cf21a075dc16e21ae8117836c8abaed847645', 'hospital_doctor',
     'MHQA9261A01', '00092610002', 'doctor.bj@medical.qa.invalid'),
    (9262, 1, 'local_medical_admin_nanshan', '南山机构管理员（测试）',
     'd0631a538d2cf21a6ff88c2986345feefb97146009fff27561e2eaa49b974bae', 'hospital_admin',
     'MHQA9262', '00092620001', 'admin.ns@medical.qa.invalid'),
    (9262, 2, 'local_medical_doctor_nanshan', '李医生（测试）',
     'd0631a538d2cf21a075dc16e21ae8117bdb3ace413741de7b645688906b3389e', 'hospital_doctor',
     'MHQA9262A01', '00092620002', 'doctor.ns@medical.qa.invalid');

DROP TEMPORARY TABLE IF EXISTS tmp_mhqa_patient;
CREATE TEMPORARY TABLE tmp_mhqa_patient (
    patient_no INT NOT NULL PRIMARY KEY,
    tenant_id INT NOT NULL,
    username VARCHAR(100) NOT NULL UNIQUE,
    realname VARCHAR(100) NOT NULL,
    sex TINYINT NOT NULL,
    gender VARCHAR(16) NOT NULL,
    age SMALLINT NOT NULL,
    height_cm DECIMAL(6,2) NOT NULL,
    weight_kg DECIMAL(6,2) NOT NULL,
    bmi DECIMAL(5,2) NOT NULL,
    phone VARCHAR(45) NOT NULL UNIQUE,
    email VARCHAR(45) NOT NULL UNIQUE,
    risk_score DECIMAL(8,6),
    risk_level VARCHAR(16),
    device_bound TINYINT NOT NULL,
    action_status VARCHAR(16) NOT NULL,
    user_status TINYINT NOT NULL DEFAULT 1,
    del_flag TINYINT NOT NULL DEFAULT 0,
    scenario_code VARCHAR(64) NOT NULL
);

INSERT INTO tmp_mhqa_patient (
    patient_no, tenant_id, username, realname, sex, gender, age,
    height_cm, weight_kg, bmi, phone, email, risk_score, risk_level,
    device_bound, action_status, user_status, del_flag, scenario_code
) VALUES
    (1, 9261, 'local_medical_patient_001', '张明远（测试）', 1, 'male', 68, 172.00, 82.40, 27.85, '00093000001', 'patient001@medical.qa.invalid', 0.820000, 'high',   1, 'pending',   1, 0, 'hypertension_smoking'),
    (2, 9261, 'local_medical_patient_002', '李慧敏（测试）', 2, 'female', 64, 160.00, 74.20, 28.98, '00093000002', 'patient002@medical.qa.invalid', 0.780000, 'high',   1, 'pending',   1, 0, 'diabetes_weight'),
    (3, 9261, 'local_medical_patient_003', '王建国（测试）', 1, 'male', 61, 174.00, 81.00, 26.75, '00093000003', 'patient003@medical.qa.invalid', 0.750000, 'high',   1, 'pending',   1, 0, 'lipid_family_history'),
    (4, 9261, 'local_medical_patient_004', '陈玉兰（测试）', 2, 'female', 59, 158.00, 70.40, 28.20, '00093000004', 'patient004@medical.qa.invalid', 0.720000, 'high',   1, 'pending',   1, 0, 'sleep_hypertension'),
    (5, 9261, 'local_medical_patient_005', '刘志强（测试）', 1, 'male', 57, 176.00, 86.10, 27.80, '00093000005', 'patient005@medical.qa.invalid', 0.710000, 'high',   1, 'pending',   1, 0, 'metabolic_activity'),
    (6, 9261, 'local_medical_patient_006', '周婉婷（测试）', 2, 'female', 55, 162.00, 72.50, 27.63, '00093000006', 'patient006@medical.qa.invalid', 0.700000, 'high',   1, 'pending',   1, 0, 'glucose_sleep'),
    (7, 9261, 'local_medical_patient_007', '赵国庆（测试）', 1, 'male', 58, 170.00, 78.30, 27.09, '00093000007', 'patient007@medical.qa.invalid', 0.650000, 'medium', 1, 'pending',   1, 0, 'blood_pressure_review'),
    (8, 9261, 'local_medical_patient_008', '孙晓梅（测试）', 2, 'female', 52, 161.00, 65.60, 25.31, '00093000008', 'patient008@medical.qa.invalid', 0.610000, 'medium', 1, 'confirmed', 1, 0, 'lipid_followup'),
    (9, 9261, 'local_medical_patient_009', '吴志远（测试）', 1, 'male', 50, 178.00, 79.20, 25.00, '00093000009', 'patient009@medical.qa.invalid', 0.580000, 'medium', 1, 'confirmed', 1, 0, 'activity_plan'),
    (10,9261, 'local_medical_patient_010', '郑丽华（测试）', 2, 'female', 49, 164.00, 66.20, 24.61, '00093000010', 'patient010@medical.qa.invalid', 0.540000, 'medium', 1, 'confirmed', 1, 0, 'sleep_plan'),
    (11,9261, 'local_medical_patient_011', '胡建新（测试）', 1, 'male', 47, 173.00, 76.40, 25.53, '00093000011', 'patient011@medical.qa.invalid', 0.510000, 'medium', 1, 'confirmed', 1, 0, 'weight_management'),
    (12,9261, 'local_medical_patient_012', '林秀珍（测试）', 2, 'female', 46, 159.00, 63.10, 24.96, '00093000012', 'patient012@medical.qa.invalid', 0.470000, 'medium', 1, 'confirmed', 1, 0, 'diet_followup'),
    (13,9261, 'local_medical_patient_013', '何俊杰（测试）', 1, 'male', 45, 175.00, 74.30, 24.26, '00093000013', 'patient013@medical.qa.invalid', 0.430000, 'medium', 1, 'confirmed', 1, 0, 'exercise_followup'),
    (14,9261, 'local_medical_patient_014', '高雅琴（测试）', 2, 'female', 43, 163.00, 60.20, 22.66, '00093000014', 'patient014@medical.qa.invalid', 0.380000, 'low',    1, 'confirmed', 1, 0, 'maintenance'),
    (15,9262, 'local_medical_patient_015', '马文博（测试）', 1, 'male', 42, 177.00, 72.50, 23.14, '00093000015', 'patient015@medical.qa.invalid', 0.350000, 'low',    1, 'confirmed', 1, 0, 'maintenance'),
    (16,9262, 'local_medical_patient_016', '罗静怡（测试）', 2, 'female', 40, 165.00, 59.80, 21.97, '00093000016', 'patient016@medical.qa.invalid', 0.310000, 'low',    1, 'confirmed', 1, 0, 'maintenance'),
    (17,9262, 'local_medical_patient_017', '唐建华（测试）', 1, 'male', 39, 174.00, 70.10, 23.15, '00093000017', 'patient017@medical.qa.invalid', 0.280000, 'low',    1, 'confirmed', 1, 0, 'maintenance'),
    (18,9262, 'local_medical_patient_018', '许美玲（测试）', 2, 'female', 38, 162.00, 57.50, 21.91, '00093000018', 'patient018@medical.qa.invalid', 0.240000, 'low',    1, 'confirmed', 2, 0, 'disabled_account'),
    (19,9262, 'local_medical_patient_019', '郭志鹏（测试）', 1, 'male', 37, 176.00, 69.20, 22.34, '00093000019', 'patient019@medical.qa.invalid', 0.180000, 'low',    0, 'confirmed', 1, 0, 'unbound_device'),
    (20,9262, 'local_medical_patient_020', '梁雪琴（测试）', 2, 'female', 36, 164.00, 58.90, 21.90, '00093000020', 'patient020@medical.qa.invalid', 0.120000, 'low',    0, 'confirmed', 1, 0, 'unbound_device'),
    (21,9262, 'local_medical_patient_021', '方晨宇（测试）', 1, 'male', 54, 171.00, 75.00, 25.65, '00093000021', 'patient021@medical.qa.invalid', NULL, NULL, 0, 'none', 1, 0, 'missing_risk'),
    (22,9262, 'local_medical_patient_022', '蒋月华（测试）', 2, 'female', 51, 160.00, 64.30, 25.12, '00093000022', 'patient022@medical.qa.invalid', NULL, NULL, 0, 'none', 2, 0, 'disabled_account_no_risk'),
    (23,9262, 'local_medical_patient_023', '谢宏伟（测试）', 1, 'male', 48, 173.00, 73.60, 24.59, '00093000023', 'patient023@medical.qa.invalid', NULL, NULL, 0, 'none', 1, 1, 'logically_deleted'),
    (24,9262, 'local_medical_patient_024', '杜佳宁（测试）', 2, 'female', 44, 166.00, 61.40, 22.28, '00093000024', 'patient024@medical.qa.invalid', NULL, NULL, 0, 'none', 1, 0, 'multi_tenant_fail_closed');

DROP TEMPORARY TABLE IF EXISTS tmp_mhqa_day;
CREATE TEMPORARY TABLE tmp_mhqa_day (days_ago INT NOT NULL PRIMARY KEY);
INSERT INTO tmp_mhqa_day (days_ago) VALUES (0),(1),(2),(3),(4),(5),(6);

START TRANSACTION;

INSERT INTO sys_tenant (
    id, name, create_time, create_by, begin_date, end_date, status, trade,
    company_size, company_address, del_flag, update_by, update_time, apply_status
) VALUES
    (9261, '睿禾滨江心血管健康管理中心（测试）', @seed_time, @seed_actor,
     @anchor_date, DATE_ADD(@anchor_date, INTERVAL 3 YEAR), 1, 'medical', '100-499',
     '浙江省杭州市滨江区（测试地址）', 0, @seed_actor, @seed_time, 1),
    (9262, '睿禾南山慢病管理中心（测试）', @seed_time, @seed_actor,
     @anchor_date, DATE_ADD(@anchor_date, INTERVAL 3 YEAR), 1, 'medical', '50-99',
     '广东省深圳市南山区（测试地址）', 0, @seed_actor, @seed_time, 1)
ON DUPLICATE KEY UPDATE
    status = 1, del_flag = 0, update_by = VALUES(update_by), update_time = VALUES(update_time);

-- Reuse the canonical patient-view permission IDs from the product Flyway migration.
INSERT INTO sys_permission (
    id, parent_id, name, url, component, is_route, component_name, redirect,
    menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf,
    keep_alive, hidden, hide_tab, description, create_by, create_time,
    update_by, update_time, del_flag, rule_flag, status, internal_or_external
)
SELECT '2030500000000000001', NULL, 'ReHealth患者数据', NULL, NULL, 0, NULL, NULL,
       1, NULL, '0', 100, 0, NULL, 0, 0, 1, 0,
       'ReHealth后台患者健康数据权限组', 'system', @seed_time, NULL, NULL, 0, 0, '1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id='2030500000000000001' OR name='ReHealth患者数据');

INSERT INTO sys_permission (
    id, parent_id, name, url, component, is_route, component_name, redirect,
    menu_type, perms, perms_type, sort_no, always_show, icon, is_leaf,
    keep_alive, hidden, hide_tab, description, create_by, create_time,
    update_by, update_time, del_flag, rule_flag, status, internal_or_external
)
SELECT '2030500000000000002',
       (SELECT id FROM sys_permission WHERE name='ReHealth患者数据' ORDER BY create_time LIMIT 1),
       '查看患者健康数据', NULL, NULL, 0, NULL, NULL,
       2, 'rehealth:admin:patient:view', '1', 1, 0, NULL, 1, 0, 1, 0,
       '读取租户隔离且最小化个人信息的患者健康数据', 'system', @seed_time,
       NULL, NULL, 0, 0, '1', 0
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM sys_permission WHERE id='2030500000000000002' OR perms='rehealth:admin:patient:view');

INSERT INTO sys_role (
    id, role_name, role_code, description, create_by, create_time, update_by, update_time, tenant_id
) VALUES
    (LOWER(MD5('LOCAL_MEDICAL_TEST_SEED:role:hospital_admin')), '医疗机构管理员', 'hospital_admin',
     'LOCAL_MEDICAL_TEST_SEED medical administrator role', @seed_actor, @seed_time, @seed_actor, @seed_time, 0),
    (LOWER(MD5('LOCAL_MEDICAL_TEST_SEED:role:hospital_doctor')), '医疗机构医生', 'hospital_doctor',
     'LOCAL_MEDICAL_TEST_SEED medical doctor role', @seed_actor, @seed_time, @seed_actor, @seed_time, 0),
    (LOWER(MD5('LOCAL_MEDICAL_TEST_SEED:role:app_user')), 'APP 用户', 'app_user',
     'LOCAL_MEDICAL_TEST_SEED APP patient classification', @seed_actor, @seed_time, @seed_actor, @seed_time, 0)
ON DUPLICATE KEY UPDATE role_name = VALUES(role_name), update_time = VALUES(update_time);

INSERT INTO sys_role_permission (id, role_id, permission_id, operate_date, operate_ip)
SELECT LOWER(MD5(CONCAT('LOCAL_MEDICAL_TEST_SEED:role-permission:', role.role_code))),
       role.id, permission.id, @seed_time, '127.0.0.1'
FROM sys_role role
JOIN sys_permission permission ON permission.perms='rehealth:admin:patient:view'
WHERE role.role_code IN ('hospital_admin','hospital_doctor')
ON DUPLICATE KEY UPDATE role_id=VALUES(role_id), permission_id=VALUES(permission_id), operate_date=VALUES(operate_date);

INSERT INTO sys_depart (
    id, parent_id, depart_name, depart_order, description, org_category, org_type,
    org_code, address, status, del_flag, create_by, create_time, update_by,
    update_time, tenant_id, iz_leaf
) VALUES
    (LOWER(MD5('LOCAL_MEDICAL_TEST_SEED:depart:9261:root')), NULL, '滨江心血管健康管理中心（测试）', 1,
     'LOCAL_MEDICAL_TEST_SEED', '1', '1', 'MHQA9261', '浙江省杭州市滨江区（测试地址）', '1', '0', @seed_actor, @seed_time, @seed_actor, @seed_time, 9261, 0),
    (LOWER(MD5('LOCAL_MEDICAL_TEST_SEED:depart:9261:cardiology')), LOWER(MD5('LOCAL_MEDICAL_TEST_SEED:depart:9261:root')), '心血管管理组（测试）', 1,
     'LOCAL_MEDICAL_TEST_SEED', '2', '2', 'MHQA9261A01', '浙江省杭州市滨江区（测试地址）', '1', '0', @seed_actor, @seed_time, @seed_actor, @seed_time, 9261, 1),
    (LOWER(MD5('LOCAL_MEDICAL_TEST_SEED:depart:9261:health')), LOWER(MD5('LOCAL_MEDICAL_TEST_SEED:depart:9261:root')), '健康管理组（测试）', 2,
     'LOCAL_MEDICAL_TEST_SEED', '2', '2', 'MHQA9261A02', '浙江省杭州市滨江区（测试地址）', '1', '0', @seed_actor, @seed_time, @seed_actor, @seed_time, 9261, 1),
    (LOWER(MD5('LOCAL_MEDICAL_TEST_SEED:depart:9262:root')), NULL, '南山慢病管理中心（测试）', 1,
     'LOCAL_MEDICAL_TEST_SEED', '1', '1', 'MHQA9262', '广东省深圳市南山区（测试地址）', '1', '0', @seed_actor, @seed_time, @seed_actor, @seed_time, 9262, 0),
    (LOWER(MD5('LOCAL_MEDICAL_TEST_SEED:depart:9262:cardiology')), LOWER(MD5('LOCAL_MEDICAL_TEST_SEED:depart:9262:root')), '慢病门诊组（测试）', 1,
     'LOCAL_MEDICAL_TEST_SEED', '2', '2', 'MHQA9262A01', '广东省深圳市南山区（测试地址）', '1', '0', @seed_actor, @seed_time, @seed_actor, @seed_time, 9262, 1),
    (LOWER(MD5('LOCAL_MEDICAL_TEST_SEED:depart:9262:health')), LOWER(MD5('LOCAL_MEDICAL_TEST_SEED:depart:9262:root')), '随访管理组（测试）', 2,
     'LOCAL_MEDICAL_TEST_SEED', '2', '2', 'MHQA9262A02', '广东省深圳市南山区（测试地址）', '1', '0', @seed_actor, @seed_time, @seed_actor, @seed_time, 9262, 1)
ON DUPLICATE KEY UPDATE depart_name=VALUES(depart_name), status='1', del_flag='0', update_time=VALUES(update_time);

INSERT INTO sys_user (
    id, username, realname, password, salt, birthday, sex, email, phone,
    org_code, status, del_flag, activiti_sync, work_no, create_by, create_time,
    update_by, update_time, user_identity, login_tenant_id, sort, iz_hide_contact
)
SELECT LOWER(MD5(CONCAT('LOCAL_MEDICAL_TEST_SEED:user:', username))), username, realname,
       password_hash, 'MHQA2608', DATE_SUB(@anchor_date, INTERVAL 35 + staff_no * 4 YEAR),
       IF(role_code='hospital_doctor', 1, 2), email, phone, department_code,
       1, 0, 0, CONCAT('MHQA-', tenant_id, '-S', staff_no), @seed_actor, @seed_time,
       @seed_actor, @seed_time, 2, tenant_id, staff_no, '0'
FROM tmp_mhqa_staff
ON DUPLICATE KEY UPDATE realname=VALUES(realname), password=VALUES(password), salt=VALUES(salt),
    email=VALUES(email), phone=VALUES(phone), org_code=VALUES(org_code), status=1, del_flag=0,
    update_by=VALUES(update_by), update_time=VALUES(update_time), login_tenant_id=VALUES(login_tenant_id);

INSERT INTO sys_user (
    id, username, realname, password, salt, birthday, sex, email, phone,
    status, del_flag, activiti_sync, work_no, create_by, create_time,
    update_by, update_time, user_identity, login_tenant_id, sort, iz_hide_contact
)
SELECT LOWER(MD5(CONCAT('LOCAL_MEDICAL_TEST_SEED:user:', username))), username, realname,
       NULL, NULL, DATE_SUB(@anchor_date, INTERVAL age YEAR), sex, email, phone,
       user_status, del_flag, 0, CONCAT('MHQA-P-', LPAD(patient_no,3,'0')),
       @seed_actor, DATE_SUB(@seed_time, INTERVAL 24 - patient_no DAY),
       @seed_actor, @seed_time, 1, tenant_id, 1000 + patient_no, '1'
FROM tmp_mhqa_patient
ON DUPLICATE KEY UPDATE realname=VALUES(realname), birthday=VALUES(birthday), sex=VALUES(sex),
    email=VALUES(email), phone=VALUES(phone), status=VALUES(status), del_flag=VALUES(del_flag),
    update_by=VALUES(update_by), update_time=VALUES(update_time), login_tenant_id=VALUES(login_tenant_id);

INSERT INTO sys_user_tenant (id, user_id, tenant_id, status, create_by, create_time, update_by, update_time)
SELECT LOWER(MD5(CONCAT('LOCAL_MEDICAL_TEST_SEED:user-tenant:', tenant_id, ':', username))),
       LOWER(MD5(CONCAT('LOCAL_MEDICAL_TEST_SEED:user:', username))), tenant_id, '1',
       @seed_actor, @seed_time, @seed_actor, @seed_time
FROM tmp_mhqa_staff
UNION ALL
SELECT LOWER(MD5(CONCAT('LOCAL_MEDICAL_TEST_SEED:user-tenant:', tenant_id, ':', username))),
       LOWER(MD5(CONCAT('LOCAL_MEDICAL_TEST_SEED:user:', username))), tenant_id, '1',
       @seed_actor, @seed_time, @seed_actor, @seed_time
FROM tmp_mhqa_patient
UNION ALL
SELECT LOWER(MD5('LOCAL_MEDICAL_TEST_SEED:user-tenant:9261:local_medical_patient_024')),
       LOWER(MD5('LOCAL_MEDICAL_TEST_SEED:user:local_medical_patient_024')), 9261, '1',
       @seed_actor, @seed_time, @seed_actor, @seed_time
ON DUPLICATE KEY UPDATE status='1', update_by=VALUES(update_by), update_time=VALUES(update_time);

INSERT INTO sys_user_role (id, user_id, role_id, tenant_id)
SELECT LOWER(MD5(CONCAT('LOCAL_MEDICAL_TEST_SEED:user-role:', staff.username, ':', staff.role_code))),
       LOWER(MD5(CONCAT('LOCAL_MEDICAL_TEST_SEED:user:', staff.username))), role.id, staff.tenant_id
FROM tmp_mhqa_staff staff JOIN sys_role role ON role.role_code=staff.role_code
UNION ALL
SELECT LOWER(MD5(CONCAT('LOCAL_MEDICAL_TEST_SEED:user-role:', patient.username, ':app_user'))),
       LOWER(MD5(CONCAT('LOCAL_MEDICAL_TEST_SEED:user:', patient.username))), role.id, 0
FROM tmp_mhqa_patient patient JOIN sys_role role ON role.role_code='app_user'
ON DUPLICATE KEY UPDATE user_id=VALUES(user_id), role_id=VALUES(role_id), tenant_id=VALUES(tenant_id);

INSERT INTO sys_user_depart (id, user_id, dep_id)
SELECT LOWER(MD5(CONCAT('LOCAL_MEDICAL_TEST_SEED:user-depart:', staff.username))),
       LOWER(MD5(CONCAT('LOCAL_MEDICAL_TEST_SEED:user:', staff.username))), depart.id
FROM tmp_mhqa_staff staff JOIN sys_depart depart
  ON depart.tenant_id=staff.tenant_id AND depart.org_code=staff.department_code
ON DUPLICATE KEY UPDATE dep_id=VALUES(dep_id);

INSERT INTO rehealth_patient_profile (
    id, user_id, name, gender, age, height_cm, weight_kg, bmi,
    family_history, smoking, drinking, diabetes_history, hypertension_history,
    profile_version, profile_json, created_at, updated_at
)
SELECT LOWER(SHA2(CONCAT('LOCAL_MEDICAL_TEST_SEED:profile:', username),256)),
       LOWER(MD5(CONCAT('LOCAL_MEDICAL_TEST_SEED:user:', username))), realname,
       gender, age, height_cm, weight_kg, bmi,
       IF(COALESCE(risk_score,0)>=0.60,1,0), IF(COALESCE(risk_score,0)>=0.72,1,0),
       IF(MOD(patient_no,5)=0,1,0), IF(scenario_code LIKE '%diabetes%' OR scenario_code LIKE '%glucose%',1,0),
       IF(COALESCE(risk_score,0)>=0.64,1,0), 1,
       JSON_OBJECT('source','LOCAL_MEDICAL_TEST_SEED','testData',TRUE,'clinicalUseAllowed',FALSE,
                   'scenarioCode',scenario_code,'tenantId',tenant_id),
       DATE_SUB(@seed_time, INTERVAL 24-patient_no DAY), @seed_time
FROM tmp_mhqa_patient
ON DUPLICATE KEY UPDATE name=VALUES(name), gender=VALUES(gender), age=VALUES(age),
    height_cm=VALUES(height_cm), weight_kg=VALUES(weight_kg), bmi=VALUES(bmi),
    family_history=VALUES(family_history), smoking=VALUES(smoking), drinking=VALUES(drinking),
    diabetes_history=VALUES(diabetes_history), hypertension_history=VALUES(hypertension_history),
    profile_version=VALUES(profile_version), profile_json=VALUES(profile_json), updated_at=VALUES(updated_at);

INSERT INTO rehealth_device_binding (
    id, user_id, device_id, device_name, manufacturer, device_model, model,
    firmware_version, hardware_address_hash, status, bound_at, updated_at
)
SELECT LOWER(SHA2(CONCAT('LOCAL_MEDICAL_TEST_SEED:binding:',username),256)),
       LOWER(MD5(CONCAT('LOCAL_MEDICAL_TEST_SEED:user:',username))),
       CONCAT('mhqa-device-',LPAD(patient_no,3,'0')), '睿禾合成测试设备', 'ReHealth QA',
       'RH-MEDICAL-QA', 'RH-MEDICAL-QA', 'qa-1.0',
       LOWER(SHA2(CONCAT('LOCAL_MEDICAL_TEST_SEED:address:',username),256)),
       'BOUND', DATE_SUB(@seed_time, INTERVAL 29 DAY), @seed_time
FROM tmp_mhqa_patient WHERE device_bound=1
ON DUPLICATE KEY UPDATE status='BOUND', device_name=VALUES(device_name), updated_at=VALUES(updated_at);

INSERT INTO rehealth_cvd_feature_vector (
    id, user_id, request_id, feature_schema_version, feature_json,
    quality_json, payload_json, created_at
)
SELECT LOWER(SHA2(CONCAT('LOCAL_MEDICAL_TEST_SEED:feature:',username),256)),
       LOWER(MD5(CONCAT('LOCAL_MEDICAL_TEST_SEED:user:',username))),
       CONCAT('mhqa-cvd16-',LPAD(patient_no,3,'0')), 'cvd-16-v1',
       JSON_OBJECT('age',age,'sex',IF(gender='male',1,0),'bmi',bmi,
           'systolic_bp',ROUND(115+risk_score*55),'diastolic_bp',ROUND(75+risk_score*20),
           'fasting_glucose',ROUND(4.7+risk_score*3.2,2),
           'total_cholesterol',ROUND(4.2+risk_score*2.4,2),
           'ldl',ROUND(2.2+risk_score*2.5,2),'hdl',ROUND(1.55-patient.risk_score*0.55,2),
           'triglycerides',ROUND(0.9+risk_score*2,2),
           'exercise_days',GREATEST(0,ROUND(6-risk_score*5)),
           'smoking',IF(risk_score>=0.72,1,0),'drinking',IF(MOD(patient_no,5)=0,1,0),
           'family_history',IF(risk_score>=0.60,1,0),
           'diabetes',IF(scenario_code LIKE '%diabetes%' OR scenario_code LIKE '%glucose%',1,0),
           'hypertension',IF(risk_score>=0.64,1,0)),
       JSON_OBJECT('source','LOCAL_MEDICAL_TEST_SEED','synthetic',TRUE,'completeFields',16),
       JSON_OBJECT('requestId',CONCAT('mhqa-cvd16-',LPAD(patient_no,3,'0')),
                   'testData',TRUE,'clinicalUseAllowed',FALSE), @seed_time
FROM tmp_mhqa_patient patient WHERE risk_level IS NOT NULL
ON DUPLICATE KEY UPDATE feature_json=VALUES(feature_json), quality_json=VALUES(quality_json),
    payload_json=VALUES(payload_json), created_at=VALUES(created_at);

INSERT INTO rehealth_cvd_risk_result (
    id, feature_vector_id, user_id, request_id, feature_schema_version,
    model_version, scorer_mode, is_mock, artifact_name, fallback_reason,
    contribution_method, factor_contribution_version, risk_score, risk_level,
    contribution_json, factor_contribution_json, missing_fields_json,
    quality_warnings_json, summary, response_json, evaluated_at, created_at
)
SELECT LOWER(SHA2(CONCAT('LOCAL_MEDICAL_TEST_SEED:risk:',username),256)),
       LOWER(SHA2(CONCAT('LOCAL_MEDICAL_TEST_SEED:feature:',username),256)),
       LOWER(MD5(CONCAT('LOCAL_MEDICAL_TEST_SEED:user:',username))),
       CONCAT('mhqa-cvd16-',LPAD(patient_no,3,'0')), 'cvd-16-v1',
       'local-medical-test-seed-not-a-model-v1', 'local_qa_fixture', 1,
       'LOCAL_MEDICAL_TEST_SEED_NOT_A_MODEL', 'synthetic local fixture',
       'TEST_SEED', 'factor16-test-seed-v1', risk_score, risk_level,
       JSON_OBJECT('blood_pressure',ROUND(risk_score*0.32,4),'activity',ROUND(risk_score*0.24,4),
                   'sleep',ROUND(risk_score*0.18,4),'metabolic',ROUND(risk_score*0.16,4)),
       JSON_ARRAY(
           JSON_OBJECT('factor','blood_pressure','contribution',ROUND(risk_score*0.32,4),'direction','risk'),
           JSON_OBJECT('factor','activity','contribution',ROUND(risk_score*0.24,4),'direction','risk'),
           JSON_OBJECT('factor','sleep','contribution',ROUND(risk_score*0.18,4),'direction','risk')),
       JSON_ARRAY(), JSON_ARRAY('LOCAL_MEDICAL_TEST_SEED_NOT_FOR_CLINICAL_USE'),
       CONCAT('合成测试风险场景：',scenario_code,'；不得用于诊断或医疗决策。'),
       JSON_OBJECT('risk_score',risk_score,'risk_level',risk_level,'is_mock',TRUE,
                   'model_version','local-medical-test-seed-not-a-model-v1',
                   'testData',TRUE,'clinicalUseAllowed',FALSE), @seed_time, @seed_time
FROM tmp_mhqa_patient WHERE risk_level IS NOT NULL
ON DUPLICATE KEY UPDATE is_mock=1, risk_score=VALUES(risk_score), risk_level=VALUES(risk_level),
    response_json=VALUES(response_json), quality_warnings_json=VALUES(quality_warnings_json),
    evaluated_at=VALUES(evaluated_at), created_at=VALUES(created_at);

INSERT INTO rehealth_intervention_plan (
    id, user_id, plan_id, source_request_id, feature_schema_version,
    model_version, scorer_mode, is_mock, artifact_name, priority_intervention,
    rationale, expected_impact, confidence, medical_disclaimer,
    generated_at, response_json, created_at
)
SELECT LOWER(SHA2(CONCAT('LOCAL_MEDICAL_TEST_SEED:plan:',username),256)),
       LOWER(MD5(CONCAT('LOCAL_MEDICAL_TEST_SEED:user:',username))),
       CONCAT('mhqa-plan-',LPAD(patient_no,3,'0')), CONCAT('mhqa-plan-request-',LPAD(patient_no,3,'0')),
       'cvd-16-v1','local-medical-test-seed-not-a-model-v1','local_qa_fixture',1,
       'LOCAL_MEDICAL_TEST_SEED_NOT_A_MODEL',
       CASE
         WHEN scenario_code LIKE '%hypertension%' OR scenario_code LIKE '%blood_pressure%' THEN '连续 7 日记录上臂袖带血压并由医生复核。'
         WHEN scenario_code LIKE '%diabetes%' OR scenario_code LIKE '%glucose%' THEN '记录空腹血糖、饮食和体重，完成代谢风险复核。'
         WHEN scenario_code LIKE '%lipid%' THEN '补充血脂复查并核对当前用药执行情况。'
         WHEN scenario_code LIKE '%sleep%' THEN '固定就寝时间并连续记录 7 日睡眠与晨间状态。'
         ELSE '每周完成 4 次中等强度活动并记录执行情况。'
       END,
       '根据合成档案、Mock 风险和测试场景生成，仅用于验证患者管理流程。',
       '验证行动确认、执行记录与复测状态展示。', 0.80,
       '合成测试计划，不构成医疗建议，不替代医生诊疗。',
       DATE_SUB(@seed_time, INTERVAL 14 DAY),
       JSON_OBJECT('sourceSystem','LOCAL_MEDICAL_TEST_SEED','isMock',TRUE,
           'testData',TRUE,'clinicalUseAllowed',FALSE,'actionConfirmationStatus',action_status,
           'items',JSON_ARRAY(
             JSON_OBJECT('id','action-01','title','完成 7 日健康指标记录','action','按计划记录并回传','target','连续 7 日','confirmationStatus',action_status),
             JSON_OBJECT('id','action-02','title','完成一次医护随访','action','核对执行障碍与安全边界','target','本周内','confirmationStatus',action_status),
             JSON_OBJECT('id','action-03','title','安排复测','action','用连续数据复核变化','target','两周后','confirmationStatus',action_status))),
       DATE_SUB(@seed_time, INTERVAL 14 DAY)
FROM tmp_mhqa_patient WHERE action_status<>'none'
ON DUPLICATE KEY UPDATE is_mock=1, priority_intervention=VALUES(priority_intervention),
    rationale=VALUES(rationale), expected_impact=VALUES(expected_impact),
    response_json=VALUES(response_json), generated_at=VALUES(generated_at);

INSERT INTO rehealth_intervention_feedback (
    id, user_id, plan_record_id, plan_id, intervention_id, idempotency_key,
    status, adherence, note, checked_at, created_at
)
SELECT LOWER(SHA2(CONCAT('LOCAL_MEDICAL_TEST_SEED:feedback:',patient.username,':',action.item_no),256)),
       LOWER(MD5(CONCAT('LOCAL_MEDICAL_TEST_SEED:user:',patient.username))),
       LOWER(SHA2(CONCAT('LOCAL_MEDICAL_TEST_SEED:plan:',patient.username),256)),
       CONCAT('mhqa-plan-',LPAD(patient.patient_no,3,'0')),
       CONCAT('action-0',action.item_no),
       CONCAT('mhqa-feedback-',LPAD(patient.patient_no,3,'0'),'-',action.item_no),
       CASE
         WHEN patient.action_status='pending' THEN 'not_started'
         WHEN patient.risk_level='low' THEN 'completed'
         WHEN action.item_no=1 THEN 'completed'
         WHEN action.item_no=2 THEN 'in_progress'
         ELSE 'pending'
       END,
       CASE
         WHEN patient.action_status='pending' THEN 0.00
         WHEN patient.risk_level='low' THEN 1.00
         WHEN action.item_no=1 THEN 1.00
         WHEN action.item_no=2 THEN 0.50
         ELSE 0.00
       END,
       CONCAT('LOCAL_MEDICAL_TEST_SEED 行动 ',action.item_no,'，仅用于测试执行状态。'),
       CASE WHEN patient.action_status='pending' OR action.item_no=3 THEN NULL
            ELSE DATE_SUB(@seed_time, INTERVAL action.item_no DAY) END,
       DATE_SUB(@seed_time, INTERVAL 3-action.item_no DAY)
FROM tmp_mhqa_patient patient
CROSS JOIN (SELECT 1 item_no UNION ALL SELECT 2 UNION ALL SELECT 3) action
WHERE patient.action_status<>'none'
ON DUPLICATE KEY UPDATE status=VALUES(status), adherence=VALUES(adherence),
    note=VALUES(note), checked_at=VALUES(checked_at), created_at=VALUES(created_at);

-- Rebuild only seed-owned daily projections so AnchorDate can move safely.
DELETE contribution FROM rehealth_rdi_contribution contribution
JOIN rehealth_rdi_daily_snapshot snapshot ON snapshot.id=contribution.snapshot_id
WHERE snapshot.calculation_source='LOCAL_MEDICAL_TEST_SEED';
DELETE FROM rehealth_rdi_daily_snapshot WHERE calculation_source='LOCAL_MEDICAL_TEST_SEED';
DELETE FROM rehealth_rhi_daily_snapshot WHERE calculation_source='LOCAL_MEDICAL_TEST_SEED';

INSERT INTO rehealth_rhi_daily_snapshot (
    id,user_id,scored_on,raw_score,display_score,data_confidence,status,product_tier,
    available_days,available_feature_count,smoothing_alpha,algorithm_version,
    calculation_source,domains_json,features_json,quality_json,created_at,updated_at
)
SELECT LOWER(SHA2(CONCAT('LOCAL_MEDICAL_TEST_SEED:rhi:',patient.username,':',day.days_ago),256)),
       LOWER(MD5(CONCAT('LOCAL_MEDICAL_TEST_SEED:user:',patient.username))),
       DATE_SUB(@anchor_date,INTERVAL day.days_ago DAY),
       ROUND(58+(1-patient.risk_score)*25-day.days_ago*0.40,4),
       ROUND(58+(1-patient.risk_score)*25-day.days_ago*0.40,4),
       ROUND(0.78+MOD(patient.patient_no,4)*0.04,6),'ready','STANDARD',30,16,0.300000,
       'rhi-medical-test-seed-v1','LOCAL_MEDICAL_TEST_SEED',
       JSON_OBJECT('activity',ROUND(60+(1-patient.risk_score)*20,2),
                   'sleep',ROUND(58+(1-patient.risk_score)*22,2),
                   'cardiovascular',ROUND(55+(1-patient.risk_score)*25,2)),
       JSON_OBJECT('availableFeatureCount',16,'testData',TRUE),
       JSON_OBJECT('synthetic',TRUE,'clinicalUseAllowed',FALSE,'source','LOCAL_MEDICAL_TEST_SEED'),
       DATE_ADD(DATE_SUB(@anchor_date,INTERVAL day.days_ago DAY),INTERVAL 21 HOUR),
       DATE_ADD(DATE_SUB(@anchor_date,INTERVAL day.days_ago DAY),INTERVAL 21 HOUR)
FROM tmp_mhqa_patient patient CROSS JOIN tmp_mhqa_day day
WHERE patient.risk_level IS NOT NULL;

INSERT INTO rehealth_rdi_daily_snapshot (
    id,user_id,scored_on,raw_score,display_score,data_confidence,status,is_mock,
    algorithm_version,calculation_source,created_at,updated_at
)
SELECT LOWER(SHA2(CONCAT('LOCAL_MEDICAL_TEST_SEED:rdi:',patient.username,':',day.days_ago),256)),
       LOWER(MD5(CONCAT('LOCAL_MEDICAL_TEST_SEED:user:',patient.username))),
       DATE_SUB(@anchor_date,INTERVAL day.days_ago DAY),
       ROUND(35+patient.risk_score*48+day.days_ago*0.45,4),
       ROUND(35+patient.risk_score*48+day.days_ago*0.45,4),
       ROUND(0.75+MOD(patient.patient_no,4)*0.05,6),'DEBUG_MOCK',1,
       'rdi-rule-1.0.1','LOCAL_MEDICAL_TEST_SEED',
       DATE_ADD(DATE_SUB(@anchor_date,INTERVAL day.days_ago DAY),INTERVAL 21 HOUR),
       DATE_ADD(DATE_SUB(@anchor_date,INTERVAL day.days_ago DAY),INTERVAL 21 HOUR)
FROM tmp_mhqa_patient patient CROSS JOIN tmp_mhqa_day day
WHERE patient.risk_level IS NOT NULL;

INSERT INTO rehealth_rdi_contribution (
    id,snapshot_id,factor_code,domain_code,source_code,current_value,baseline_value,
    unit,raw_points,confidence,final_points,source_factor_id,algorithm_version,created_at
)
SELECT LOWER(SHA2(CONCAT('LOCAL_MEDICAL_TEST_SEED:rdi-contribution:',patient.username,':',day.days_ago,':',factor.factor_code),256)),
       LOWER(SHA2(CONCAT('LOCAL_MEDICAL_TEST_SEED:rdi:',patient.username,':',day.days_ago),256)),
       factor.factor_code,factor.domain_code,'LOCAL_MEDICAL_TEST_SEED',
       CASE factor.factor_code WHEN 'steps' THEN 3800+patient.patient_no*110
            WHEN 'sleep_duration' THEN 370+patient.patient_no*4 ELSE 28+patient.patient_no*0.7 END,
       CASE factor.factor_code WHEN 'steps' THEN 7000 WHEN 'sleep_duration' THEN 480 ELSE 45 END,
       factor.unit,ROUND(patient.risk_score*factor.weight,6),
       ROUND(0.75+MOD(patient.patient_no,4)*0.05,6),
       ROUND(patient.risk_score*factor.weight*(0.75+MOD(patient.patient_no,4)*0.05),6),
       CONCAT('LOCAL_MEDICAL_TEST_SEED:',factor.factor_code,':',patient.username,':',day.days_ago),
       'rdi-rule-1.0.1',DATE_ADD(DATE_SUB(@anchor_date,INTERVAL day.days_ago DAY),INTERVAL 21 HOUR)
FROM tmp_mhqa_patient patient CROSS JOIN tmp_mhqa_day day
CROSS JOIN (
    SELECT 'steps' factor_code,'activity' domain_code,'steps/day' unit,1.20 weight
    UNION ALL SELECT 'sleep_duration','sleep','min/night',1.00
    UNION ALL SELECT 'nocturnal_hrv','recovery','ms',0.80
) factor
WHERE patient.risk_level IS NOT NULL;

COMMIT;

SELECT 'medical_tenants' metric,COUNT(*) value FROM sys_tenant WHERE id IN (9261,9262)
UNION ALL SELECT 'staff_accounts',COUNT(*) FROM sys_user WHERE create_by='LOCAL_MEDICAL_TEST_SEED' AND username LIKE 'local_medical_%' AND username NOT LIKE 'local_medical_patient_%'
UNION ALL SELECT 'patient_accounts',COUNT(*) FROM sys_user WHERE create_by='LOCAL_MEDICAL_TEST_SEED' AND username LIKE 'local_medical_patient_%'
UNION ALL SELECT 'profiles',COUNT(*) FROM rehealth_patient_profile WHERE JSON_UNQUOTE(JSON_EXTRACT(profile_json,'$.source'))='LOCAL_MEDICAL_TEST_SEED'
UNION ALL SELECT 'device_bindings',COUNT(*) FROM rehealth_device_binding WHERE device_id LIKE 'mhqa-device-%'
UNION ALL SELECT 'mock_risks',COUNT(*) FROM rehealth_cvd_risk_result WHERE artifact_name='LOCAL_MEDICAL_TEST_SEED_NOT_A_MODEL' AND is_mock=1
UNION ALL SELECT 'plans',COUNT(*) FROM rehealth_intervention_plan WHERE artifact_name='LOCAL_MEDICAL_TEST_SEED_NOT_A_MODEL' AND is_mock=1
UNION ALL SELECT 'feedback',COUNT(*) FROM rehealth_intervention_feedback WHERE idempotency_key LIKE 'mhqa-feedback-%'
UNION ALL SELECT 'rhi_snapshots',COUNT(*) FROM rehealth_rhi_daily_snapshot WHERE calculation_source='LOCAL_MEDICAL_TEST_SEED'
UNION ALL SELECT 'rdi_snapshots',COUNT(*) FROM rehealth_rdi_daily_snapshot WHERE calculation_source='LOCAL_MEDICAL_TEST_SEED' AND is_mock=1
UNION ALL SELECT 'rdi_contributions',COUNT(*) FROM rehealth_rdi_contribution WHERE source_code='LOCAL_MEDICAL_TEST_SEED';


-- 当前版本计划任务执行事实：从已生成的任务实例构造幂等反馈，覆盖移动端展示链路。
INSERT INTO rehealth_care_plan_execution (
    id, tenant_id, occurrence_id, plan_id, revision_id, plan_item_id,
    logical_item_id, subject_ref, feedback_type, score_value,
    verification_type, note, occurred_at, source_system,
    source_record_id, created_at
)
SELECT
    LOWER(MD5(CONCAT('LOCAL_CARE_PLAN_EXECUTION_QA:', occurrence.id))),
    occurrence.tenant_id, occurrence.id, occurrence.plan_id,
    occurrence.revision_id, occurrence.plan_item_id, occurrence.logical_item_id,
    occurrence.subject_ref,
    CASE MOD(CRC32(occurrence.id), 3)
        WHEN 0 THEN 'completed'
        WHEN 1 THEN 'partially_completed'
        ELSE 'skipped'
    END,
    CASE MOD(CRC32(occurrence.id), 3)
        WHEN 0 THEN 1.0000
        WHEN 1 THEN 0.5000
        ELSE 0.0000
    END,
    'self_report', '按计划完成情况提交', occurrence.scheduled_at,
    'LOCAL_CARE_PLAN_EXECUTION_QA',
    CONCAT('LOCAL_CARE_PLAN_EXECUTION_QA:', occurrence.id), @seed_time
FROM rehealth_care_plan_occurrence occurrence
WHERE occurrence.status = 'scheduled'
ON DUPLICATE KEY UPDATE
    feedback_type = VALUES(feedback_type),
    score_value = VALUES(score_value),
    note = VALUES(note),
    occurred_at = VALUES(occurred_at);
