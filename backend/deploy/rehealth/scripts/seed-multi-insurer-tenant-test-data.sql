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
    (9101, 'MIQA01', '[LOCAL QA] 睿安健康保险', 'LOCAL-QA-INS-9101', '合成测试地址·上海'),
    (9102, 'MIQA02', '[LOCAL QA] 康泰人寿保险', 'LOCAL-QA-INS-9102', '合成测试地址·北京'),
    (9103, 'MIQA03', '[LOCAL QA] 华宁财产保险', 'LOCAL-QA-INS-9103', '合成测试地址·深圳');

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
       CONCAT('MIQA', tenant_id, department_key),
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
       CONCAT('MIQA-', REPLACE(username, 'local_ins_', '')),
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
