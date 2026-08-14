-- Local-only complete APP-user fixtures for the three LOCAL_MULTI_INSURER_QA tenants.
--
-- This script deliberately reuses sys_user/sys_role plus the existing insurance
-- subject, policy, consent, plan, manager, and audit tables. APP users are NOT
-- inserted into sys_user_tenant or sys_user_depart. Every row is synthetic,
-- non-clinical, repeatable, and forbidden outside local development. Each
-- insurer receives 12 subjects so all four workbench workflow states have at
-- least three rows; each subject detail receives at least three display items.

SET NAMES utf8mb4;

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
    (1,  'local_app_9101_01', '[合成] 睿安用户01', '1c8b16ac552e6afbdf0373b1b0f4e86cf50be5a6147a4979', '00092000001', 'app01@local.qa.invalid', 1, 'male',   44, 175.00, 70.10, 22.89, 0.260000),
    (2,  'local_app_9101_02', '[合成] 睿安用户02', '1c8b16ac552e6afbdf0373b1b0f4e86c6977237059d17f4b', '00092000002', 'app02@local.qa.invalid', 2, 'female', 49, 163.00, 61.20, 23.03, 0.350000),
    (3,  'local_app_9101_03', '[合成] 睿安用户03', '1c8b16ac552e6afbdf0373b1b0f4e86cb37a058454ba5dfc', '00092000003', 'app03@local.qa.invalid', 1, 'male',   54, 172.00, 78.40, 26.50, 0.480000),
    (4,  'local_app_9101_04', '[合成] 睿安用户04', '1c8b16ac552e6afbdf0373b1b0f4e86ce5dc1f0b0f86675d', '00092000004', 'app04@local.qa.invalid', 2, 'female', 59, 160.00, 71.00, 27.73, 0.700000),
    (5,  'local_app_9102_01', '[合成] 康泰用户01', '1c8b16ac552e6afb172c2f8c31ad5b828d2bd45da9912a9b', '00092000005', 'app05@local.qa.invalid', 2, 'female', 42, 166.00, 60.80, 22.06, 0.230000),
    (6,  'local_app_9102_02', '[合成] 康泰用户02', '1c8b16ac552e6afb172c2f8c31ad5b824563e9e55a9d952a', '00092000006', 'app06@local.qa.invalid', 1, 'male',   47, 178.00, 76.20, 24.05, 0.330000),
    (7,  'local_app_9102_03', '[合成] 康泰用户03', '1c8b16ac552e6afb172c2f8c31ad5b829ef8c9b6a63f3755', '00092000007', 'app07@local.qa.invalid', 2, 'female', 55, 158.00, 67.40, 27.00, 0.520000),
    (8,  'local_app_9102_04', '[合成] 康泰用户04', '1c8b16ac552e6afb172c2f8c31ad5b8241d66a504bcaeb62', '00092000008', 'app08@local.qa.invalid', 1, 'male',   62, 170.00, 84.20, 29.13, 0.720000),
    (9,  'local_app_9103_01', '[合成] 华宁用户01', '1c8b16ac552e6afbabb9b09bda7b8fe85ecc547f487663f0', '00092000009', 'app09@local.qa.invalid', 1, 'male',   45, 176.00, 71.60, 23.11, 0.280000),
    (10, 'local_app_9103_02', '[合成] 华宁用户02', '1c8b16ac552e6afbabb9b09bda7b8fe8a43c32a4e082f84a', '00092000010', 'app10@local.qa.invalid', 2, 'female', 50, 164.00, 63.50, 23.61, 0.390000),
    (11, 'local_app_9103_03', '[合成] 华宁用户03', '1c8b16ac552e6afbabb9b09bda7b8fe8899a1524a55b1110', '00092000011', 'app11@local.qa.invalid', 1, 'male',   57, 173.00, 81.20, 27.13, 0.580000),
    (12, 'local_app_9103_04', '[合成] 华宁用户04', '1c8b16ac552e6afbabb9b09bda7b8fe86471d82a6d9bf4d9', '00092000012', 'app12@local.qa.invalid', 2, 'female', 64, 159.00, 75.20, 29.75, 0.760000),
    (13, 'local_app_shared_01', '[合成] 三机构共享用户01', '1c8b16ac552e6afb4efaae7ea43c0964991df824007b1fde', '00092000013', 'app13@local.qa.invalid', 1, 'male',   50, 175.00, 68.60, 22.40, 0.310000),
    (14, 'local_app_shared_02', '[合成] 三机构共享用户02', '1c8b16ac552e6afb4efaae7ea43c09642902a24009a903e4', '00092000014', 'app14@local.qa.invalid', 2, 'female', 53, 162.00, 66.80, 25.45, 0.460000);

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
    1, 0, 0, CONCAT('MIQA-APP-', LPAD(profile_no, 3, '0')),
    @seed_actor, @seed_time, @seed_actor, @seed_time, 1, 0,
    920000 + profile_no, '0'
FROM tmp_miqa_app_user
ON DUPLICATE KEY UPDATE
    realname = VALUES(realname), password = VALUES(password), salt = VALUES(salt),
    birthday = VALUES(birthday), sex = VALUES(sex), email = VALUES(email),
    phone = VALUES(phone), status = 1, del_flag = 0,
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
    CONCAT('miqa-device-', username), '睿禾全链路合成戒指', 'ReHealth QA',
    'RH-QA-50M', 'RH-QA-50M', 'qa-1.0',
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
        JSON_OBJECT('topic', 'safety', 'content', '测试数据，无真实诊断或用药信息')
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
    UNION ALL SELECT 3, 'safety', 'safety', '仅使用合成测试数据，不包含真实诊断'
) item
WHERE 1 = 1
ON DUPLICATE KEY UPDATE content = VALUES(content), topic = VALUES(topic);

INSERT INTO rehealth_health_interview_baseline (id, interview_id, label, item_value, sort_order)
SELECT
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:interview-baseline:', app.username, ':', item.sort_order), 256)),
    LOWER(SHA2(CONCAT('LOCAL_MULTI_INSURER_APP_QA:interview:', app.username), 256)),
    item.label,
    CASE item.sort_order WHEN 1 THEN CAST(app.age AS CHAR) WHEN 2 THEN CAST(app.bmi AS CHAR) ELSE '完整合成基线' END,
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
    SELECT 1 item_no, 'FOOD' category, '合成早餐记录' title, '全谷物、鸡蛋和水果' summary,
           460.00 calories, 24.00 protein, 58.00 carbs, 16.00 fat
    UNION ALL
    SELECT 2, 'ACTIVITY', '合成步行记录', '完成中等强度步行', NULL, NULL, NULL, NULL
    UNION ALL
    SELECT 3, 'OTHER', '合成健康任务', '完成保险健康管理随访任务', NULL, NULL, NULL, NULL
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
    JSON_OBJECT('age', 0.20, 'bmi', 0.18, 'sbp', 0.16, 'exercise_days', -0.12),
    JSON_OBJECT('profile', 0.38, 'vitals', 0.30, 'lifestyle', 0.18, 'labs', 0.14),
    JSON_OBJECT('measured', 1.0), JSON_OBJECT('supported', 1.0),
    JSON_ARRAY(), JSON_ARRAY('SYNTHETIC LOCAL QA - NOT FOR CLINICAL OR INSURANCE DECISIONS'),
    'Android Debug 全链路公式衍生的本地合成风险，仅用于权限和界面测试。',
    JSON_OBJECT(
        'sourceSystem', 'LOCAL_MULTI_INSURER_APP_QA', 'isMock', FALSE,
        'clinicalUseAllowed', FALSE, 'syntheticLocalQa', TRUE,
        'factorContributionVersion', 'factor16-rule-v1.0.0',
        'factor_contributions', JSON_OBJECT(
            '收缩压', ROUND(0.12 + app.risk_base * 0.18, 4),
            'BMI', ROUND(0.08 + app.risk_base * 0.14, 4),
            '睡眠规律', ROUND(-0.05 - (1 - app.risk_base) * 0.08, 4),
            '每周运动天数', ROUND(-0.04 - (1 - app.risk_base) * 0.06, 4)
        ),
        'factor_measured_components', JSON_OBJECT(
            '收缩压', ROUND(112 + app.risk_base * 28, 1),
            'BMI', app.bmi,
            '睡眠规律', ROUND(70 + (1 - app.risk_base) * 20, 1),
            '每周运动天数', 5
        )
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
    '合成归因结果，仅用于验证 PIAS 数据展示和保险员工权限。',
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
    '保持规律睡眠、每周完成五次中等强度活动并持续记录血压。',
    '依据完整合成档案、设备趋势和 CVD-16 合成结果生成。',
    '仅验证计划、反馈和负责人查询链路。', 0.80,
    '合成测试计划，不构成医疗建议，不替代医生诊疗。',
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
    rel.tenant_id, CONCAT('MIQA-', rel.tenant_id, '-POL-', LPAD(rel.member_no, 4, '0')),
    'MIQA-CVD', '合成心血管健康管理保险计划', 'health_management',
    LOWER(SHA2(CONCAT(rel.tenant_id, ':', LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_APP_QA:user:', rel.username)))), 256)),
    LOWER(SHA2(CONCAT(rel.tenant_id, ':', LOWER(MD5(CONCAT('LOCAL_MULTI_INSURER_APP_QA:user:', rel.username)))), 256)),
    300000.00, 1200.00 + rel.member_no * 80, 500.00, 30,
    DATE_SUB(@anchor_date, INTERVAL 180 DAY), DATE_ADD(@anchor_date, INTERVAL 365 DAY),
    'active', 'LOCAL_MULTI_INSURER_APP_QA', CONCAT('policy-', rel.tenant_id, '-', rel.member_no),
    JSON_OBJECT('synthetic', TRUE, 'clinicalUseAllowed', FALSE),
    DATE_SUB(@seed_time, INTERVAL 180 DAY), @seed_time
FROM tmp_miqa_app_relationship rel
ON DUPLICATE KEY UPDATE
    product_code = VALUES(product_code), product_name = VALUES(product_name),
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
    'CVD-MGMT', '合成心血管健康管理保障', 300000.00, 500.00,
    DATE_SUB(@anchor_date, INTERVAL 180 DAY), DATE_ADD(@anchor_date, INTERVAL 365 DAY),
    'active', 'LOCAL_MULTI_INSURER_APP_QA', CONCAT('coverage-', rel.tenant_id, '-', rel.member_no),
    JSON_OBJECT('synthetic', TRUE), DATE_SUB(@seed_time, INTERVAL 180 DAY), @seed_time
FROM tmp_miqa_app_relationship rel
ON DUPLICATE KEY UPDATE
    policy_id = VALUES(policy_id), subject_ref = VALUES(subject_ref),
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
    CONCAT('MIQA-CONSENT-', rel.tenant_id, '-', rel.member_no),
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
    occurred_at, completion_rate, adherence_score, outcome_summary_json,
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
    item.feedback_type, DATE_SUB(@seed_time, INTERVAL item.days_ago DAY),
    item.completion_rate, item.adherence_score,
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
CROSS JOIN (
    SELECT 1 feedback_no, 'plan_started' feedback_type, 7 days_ago,
           0.48 completion_rate, 0.56 adherence_score, '已确认计划并开始执行' note
    UNION ALL
    SELECT 2, 'weekly_progress', 4, 0.68, 0.74, '已完成睡眠和运动记录'
    UNION ALL
    SELECT 3, 'weekly_progress', 1, 0.86, 0.88, '已回传本周执行结果'
) item
WHERE 1 = 1
ON DUPLICATE KEY UPDATE
    binding_id = VALUES(binding_id), subject_ref = VALUES(subject_ref),
    intervention_id = VALUES(intervention_id), feedback_type = VALUES(feedback_type),
    occurred_at = VALUES(occurred_at), completion_rate = VALUES(completion_rate),
    adherence_score = VALUES(adherence_score), outcome_summary_json = VALUES(outcome_summary_json),
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
    CONCAT('miqa-health-plan-', app.profile_no), item.action_type, item.title, item.content,
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
        'result', IF(rel.member_no IN (3, 9, 10) AND item.action_no IN (1, 2), '待执行', '已完成本地验收记录')
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
           '核对计划理解情况和当前执行困难；仅作合成本地界面验收。' content, 1 due_days
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
    rel.tenant_id, CONCAT('MIQA-', rel.tenant_id, '-CLAIM-', LPAD(rel.member_no, 4, '0')),
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
    policy_id = VALUES(policy_id), subject_ref = VALUES(subject_ref),
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
