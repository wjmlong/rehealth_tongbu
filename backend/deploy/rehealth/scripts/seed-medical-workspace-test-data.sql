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
