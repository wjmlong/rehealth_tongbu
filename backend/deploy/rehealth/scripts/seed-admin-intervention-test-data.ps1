[CmdletBinding()]
param(
    [string]$SoftwareContainer = 'rehealth-software-db-1',
    [string]$HardwareContainer = 'rehealth-hardware-db-1'
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version Latest

$utf8NoBom = New-Object System.Text.UTF8Encoding($false)
$OutputEncoding = $utf8NoBom
[Console]::OutputEncoding = $utf8NoBom

$deployRoot = Split-Path -Parent $PSScriptRoot
$softwarePasswordPath = Join-Path $deployRoot 'secrets\software_db_password'
$hardwarePasswordPath = Join-Path $deployRoot 'secrets\hardware_db_password'

foreach ($requiredPath in @($softwarePasswordPath, $hardwarePasswordPath)) {
    if (-not (Test-Path -LiteralPath $requiredPath -PathType Leaf)) {
        throw "Required local secret file is missing: $requiredPath"
    }
}

$softwarePassword = (Get-Content -Raw -LiteralPath $softwarePasswordPath).Trim()
$hardwarePassword = (Get-Content -Raw -LiteralPath $hardwarePasswordPath).Trim()
if ([string]::IsNullOrWhiteSpace($softwarePassword) -or [string]::IsNullOrWhiteSpace($hardwarePassword)) {
    throw 'Database password files must not be empty.'
}

$adminLookupSql = @"
SELECT CONCAT(id, '|', COALESCE(login_tenant_id, 0))
FROM rehealth_software.sys_user
WHERE username = 'admin' AND status = 1 AND del_flag = 0
LIMIT 1;
"@
$adminRow = & docker exec -e "MYSQL_PWD=$softwarePassword" $SoftwareContainer `
    mysql --default-character-set=utf8mb4 -u rehealth_software --batch --raw `
    --skip-column-names -e $adminLookupSql
if ($LASTEXITCODE -ne 0) {
    throw 'Failed to resolve the active admin account from software_db.'
}
$adminRow = ($adminRow | Select-Object -First 1).Trim()
$adminParts = $adminRow.Split('|')
if ($adminParts.Count -ne 2 -or $adminParts[0] -notmatch '^[A-Za-z0-9-]{1,64}$' -or $adminParts[1] -notmatch '^\d+$') {
    throw 'The active admin account or its login tenant could not be resolved safely.'
}

$adminUserId = $adminParts[0]
$tenantId = $adminParts[1]
$seedDeviceId = 'local-admin-intervention-device'
$seedRequestId = 'local-admin-intervention-seed-v1'

$softwareSql = @"
SET NAMES utf8mb4;
SET @user_id = '$adminUserId';
SET @tenant_id = '$tenantId';
SET @seed_request_id = '$seedRequestId';
SET @seed_device_id = '$seedDeviceId';
SET @now = NOW(3);

START TRANSACTION;

INSERT INTO rehealth_patient_profile (
    id, user_id, name, gender, age, height_cm, weight_kg, bmi,
    family_history, smoking, drinking, diabetes_history, hypertension_history,
    profile_version, created_at, updated_at
) VALUES (
    UUID(), @user_id, 'Admin Intervention QA', 'male', 52, 172.00, 81.00, 27.38,
    1, 0, 1, 0, 1,
    1, @now, @now
)
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
    profile_version = profile_version + 1,
    updated_at = VALUES(updated_at);

INSERT INTO rehealth_device_binding (
    id, user_id, device_id, device_name, manufacturer, device_model, model,
    firmware_version, hardware_address_hash, status, bound_at, updated_at
) VALUES (
    'seed-admin-intervention-device-binding-v1', @user_id, @seed_device_id,
    'Admin Intervention Test Wearable', 'ReHealth QA', 'LOCAL-SEED', 'LOCAL-SEED',
    '1.0-test', 'local-test-data-no-real-address', 'BOUND', @now, @now
)
ON DUPLICATE KEY UPDATE
    device_name = VALUES(device_name),
    manufacturer = VALUES(manufacturer),
    device_model = VALUES(device_model),
    model = VALUES(model),
    firmware_version = VALUES(firmware_version),
    hardware_address_hash = VALUES(hardware_address_hash),
    status = 'BOUND',
    updated_at = VALUES(updated_at);

INSERT INTO rehealth_health_interview (id, user_id, generated_at, created_at)
VALUES ('seed-admin-intervention-interview-v1', @user_id, @now, @now)
ON DUPLICATE KEY UPDATE
    generated_at = VALUES(generated_at),
    created_at = VALUES(created_at);

DELETE FROM rehealth_health_interview_answer
WHERE interview_id = 'seed-admin-intervention-interview-v1';
DELETE FROM rehealth_health_interview_baseline
WHERE interview_id = 'seed-admin-intervention-interview-v1';
DELETE FROM rehealth_health_interview_focus
WHERE interview_id = 'seed-admin-intervention-interview-v1';

INSERT INTO rehealth_health_interview_answer (
    id, interview_id, question_id, topic, content, sort_order
) VALUES
    ('seed-admin-intervention-answer-01', 'seed-admin-intervention-interview-v1',
     'sleep-duration', 'sleep', 'Usually sleeps about 6 hours and often goes to bed after midnight.', 0),
    ('seed-admin-intervention-answer-02', 'seed-admin-intervention-interview-v1',
     'daily-activity', 'exercise', 'Mostly sedentary at work and exercises about 1 to 2 days per week.', 1),
    ('seed-admin-intervention-answer-03', 'seed-admin-intervention-interview-v1',
     'diet-pattern', 'diet', 'Often eats salty takeaway food and wants practical lower-sodium choices.', 2),
    ('seed-admin-intervention-answer-04', 'seed-admin-intervention-interview-v1',
     'health-goal', 'goal', 'Wants to improve sleep, daily steps, weight control, and blood-pressure habits.', 3);

INSERT INTO rehealth_health_interview_baseline (
    id, interview_id, label, item_value, sort_order
) VALUES
    ('seed-admin-intervention-baseline-01', 'seed-admin-intervention-interview-v1',
     'Average sleep', 'About 6 hours per night', 0),
    ('seed-admin-intervention-baseline-02', 'seed-admin-intervention-interview-v1',
     'Weekly exercise', '1 to 2 days', 1),
    ('seed-admin-intervention-baseline-03', 'seed-admin-intervention-interview-v1',
     'Diet preference', 'Salty takeaway food', 2);

INSERT INTO rehealth_health_interview_focus (
    id, interview_id, focus_area, sort_order
) VALUES
    ('seed-admin-intervention-focus-01', 'seed-admin-intervention-interview-v1', 'sleep', 0),
    ('seed-admin-intervention-focus-02', 'seed-admin-intervention-interview-v1', 'exercise', 1),
    ('seed-admin-intervention-focus-03', 'seed-admin-intervention-interview-v1', 'blood_pressure', 2),
    ('seed-admin-intervention-focus-04', 'seed-admin-intervention-interview-v1', 'diet', 3);

INSERT INTO rehealth_cvd_feature_vector (
    id, user_id, request_id, feature_schema_version,
    feature_json, quality_json, payload_json, created_at
) VALUES (
    'seed-admin-intervention-feature-v1', @user_id, @seed_request_id, 'cvd-16-v1',
    JSON_OBJECT(
        'age', 52, 'gender', 1, 'bmi', 27.38, 'sbp', 138, 'dbp', 88,
        'fasting_glucose', 5.7, 'total_cholesterol', 5.2, 'ldl', 3.2,
        'hdl', 1.1, 'triglycerides', 1.8, 'exercise_days', 2,
        'smoking', 0, 'drinking', 1, 'diabetes_history', 0,
        'hypertension_history', 1, 'family_history', 1
    ),
    JSON_OBJECT('source', 'LOCAL_TEST_SEED', 'production_eligible', FALSE),
    JSON_OBJECT(
        'requestId', @seed_request_id,
        'testData', TRUE,
        'featureVector', JSON_OBJECT(
            'age', 52, 'gender', 1, 'bmi', 27.38, 'sbp', 138, 'dbp', 88,
            'fasting_glucose', 5.7, 'total_cholesterol', 5.2, 'ldl', 3.2,
            'hdl', 1.1, 'triglycerides', 1.8, 'exercise_days', 2,
            'smoking', 0, 'drinking', 1, 'diabetes_history', 0,
            'hypertension_history', 1, 'family_history', 1
        )
    ),
    @now
)
ON DUPLICATE KEY UPDATE
    feature_schema_version = VALUES(feature_schema_version),
    feature_json = VALUES(feature_json),
    quality_json = VALUES(quality_json),
    payload_json = VALUES(payload_json),
    created_at = VALUES(created_at);

INSERT INTO rehealth_cvd_risk_result (
    id, feature_vector_id, user_id, request_id, feature_schema_version,
    model_version, scorer_mode, is_mock, artifact_name, fallback_reason,
    contribution_method, factor_contribution_version, risk_score, risk_level,
    contribution_json, factor_contribution_json, factor_measured_component_json,
    factor_control_support_json, missing_fields_json, quality_warnings_json,
    summary, response_json, evaluated_at, created_at
) VALUES (
    'seed-admin-intervention-risk-v1', 'seed-admin-intervention-feature-v1',
    @user_id, @seed_request_id, 'cvd-16-v1',
    'local-admin-test-seed-1.0', 'TEST_SEED', TRUE,
    'local-admin-intervention-test-data', 'LOCAL_TEST_DATA_ONLY',
    'TEST_SEED', 'factor16-test-seed-v1', 0.43, 'moderate',
    JSON_OBJECT('sbp', 0.18, 'bmi', 0.14, 'exercise_days', 0.17, 'hdl', 0.05),
    JSON_OBJECT('blood_pressure', 0.23, 'activity', 0.18, 'weight', 0.14, 'sleep', 0.10),
    JSON_OBJECT('blood_pressure', 0.23, 'activity', 0.18, 'weight', 0.14, 'sleep', 0.10),
    JSON_OBJECT(), JSON_ARRAY(),
    JSON_ARRAY('LOCAL_TEST_DATA_ONLY_NOT_FOR_CLINICAL_USE'),
    'Local QA test risk only: moderate lifestyle-improvement context; not a diagnosis.',
    JSON_OBJECT(
        'risk_score', 0.43,
        'risk_level', 'moderate',
        'model_version', 'local-admin-test-seed-1.0',
        'is_mock', TRUE,
        'summary', 'Local QA test risk only; not a diagnosis.',
        'missing_fields', JSON_ARRAY(),
        'quality_warnings', JSON_ARRAY('LOCAL_TEST_DATA_ONLY_NOT_FOR_CLINICAL_USE')
    ),
    @now, @now
)
ON DUPLICATE KEY UPDATE
    feature_vector_id = VALUES(feature_vector_id),
    feature_schema_version = VALUES(feature_schema_version),
    model_version = VALUES(model_version),
    scorer_mode = VALUES(scorer_mode),
    is_mock = VALUES(is_mock),
    artifact_name = VALUES(artifact_name),
    fallback_reason = VALUES(fallback_reason),
    contribution_method = VALUES(contribution_method),
    factor_contribution_version = VALUES(factor_contribution_version),
    risk_score = VALUES(risk_score),
    risk_level = VALUES(risk_level),
    contribution_json = VALUES(contribution_json),
    factor_contribution_json = VALUES(factor_contribution_json),
    factor_measured_component_json = VALUES(factor_measured_component_json),
    factor_control_support_json = VALUES(factor_control_support_json),
    missing_fields_json = VALUES(missing_fields_json),
    quality_warnings_json = VALUES(quality_warnings_json),
    summary = VALUES(summary),
    response_json = VALUES(response_json),
    evaluated_at = VALUES(evaluated_at),
    created_at = VALUES(created_at);

COMMIT;
"@

$softwareSql | & docker exec -i -e "MYSQL_PWD=$softwarePassword" $SoftwareContainer `
    mysql --default-character-set=utf8mb4 -u rehealth_software rehealth_software
if ($LASTEXITCODE -ne 0) {
    throw 'Failed to seed admin intervention context in software_db.'
}

$hardwareSql = @"
\set ON_ERROR_STOP on
BEGIN;

DELETE FROM hardware_upload_batch
WHERE id = '00000000-0000-4000-8000-00000000a001'::uuid
   OR receipt_id = '00000000-0000-4000-8000-00000000a002'::uuid
   OR (
       tenant_id = '$tenantId'
       AND user_id = '$adminUserId'
       AND device_id = '$seedDeviceId'
       AND batch_id = '$seedRequestId'
   );

INSERT INTO hardware_upload_batch (
    id, receipt_id, tenant_id, user_id, device_id, batch_id, source,
    collected_from, collected_to, received_at, committed_at, status,
    record_count, measurement_count, sleep_session_count, activity_count,
    signal_metadata_count, quality_summary, diet_record_count
) VALUES (
    '00000000-0000-4000-8000-00000000a001'::uuid,
    '00000000-0000-4000-8000-00000000a002'::uuid,
    '$tenantId', '$adminUserId', '$seedDeviceId', '$seedRequestId', 'LOCAL_TEST_SEED',
    now() - interval '14 days', now(), now(), now(), 'PERSISTED',
    48, 17, 14, 14, 0,
    '{"testData":true,"productionEligible":false,"purpose":"admin-intervention-local-qa"}'::jsonb,
    3
);

WITH seed AS (
    SELECT
        generate_series(0, 13) AS day_offset,
        timezone('Asia/Shanghai', now())::date AS local_today
)
INSERT INTO hardware_activity (
    id, upload_batch_id, tenant_id, user_id, device_id, source_record_id,
    started_at, ended_at, activity_type, steps, distance_meters,
    calories_kcal, duration_minutes, average_heart_rate, source
)
SELECT
    md5('admin-intervention-activity-' || day_offset)::uuid,
    '00000000-0000-4000-8000-00000000a001'::uuid,
    '$tenantId', '$adminUserId', '$seedDeviceId',
    'local-test-activity-' || day_offset,
    CASE WHEN day_offset = 0
        THEN now() - interval '90 minutes'
        ELSE ((local_today - day_offset) + time '08:00') AT TIME ZONE 'Asia/Shanghai'
    END,
    CASE WHEN day_offset = 0
        THEN now() - interval '55 minutes'
        ELSE ((local_today - day_offset) + time '08:00') AT TIME ZONE 'Asia/Shanghai'
             + make_interval(mins => CASE WHEN day_offset < 7 THEN 25 ELSE 45 END)
    END,
    'walking',
    CASE WHEN day_offset < 7 THEN 3800 + (day_offset * 120) ELSE 6800 + ((day_offset - 7) * 100) END,
    CASE WHEN day_offset < 7 THEN 2700 + (day_offset * 80) ELSE 4800 + ((day_offset - 7) * 70) END,
    CASE WHEN day_offset < 7 THEN 145 + (day_offset * 3) ELSE 255 + ((day_offset - 7) * 4) END,
    CASE WHEN day_offset < 7 THEN 25 ELSE 45 END,
    CASE WHEN day_offset < 7 THEN 92 ELSE 88 END,
    'LOCAL_TEST_SEED'
FROM seed;

WITH seed AS (
    SELECT
        generate_series(0, 13) AS day_offset,
        timezone('Asia/Shanghai', now())::date AS local_today
), windows AS (
    SELECT
        day_offset,
        CASE WHEN day_offset = 0
            THEN now() - interval '30 minutes'
            ELSE ((local_today - day_offset) + time '07:00') AT TIME ZONE 'Asia/Shanghai'
        END AS ended_at,
        CASE WHEN day_offset < 7 THEN 360 ELSE 430 END AS sleep_minutes
    FROM seed
)
INSERT INTO hardware_sleep_session (
    id, upload_batch_id, tenant_id, user_id, device_id, source_record_id,
    started_at, ended_at, deep_minutes, light_minutes, awake_minutes,
    rem_minutes, interruption_minutes, source
)
SELECT
    md5('admin-intervention-sleep-' || day_offset)::uuid,
    '00000000-0000-4000-8000-00000000a001'::uuid,
    '$tenantId', '$adminUserId', '$seedDeviceId',
    'local-test-sleep-' || day_offset,
    ended_at - make_interval(mins => sleep_minutes), ended_at,
    CASE WHEN day_offset < 7 THEN 70 ELSE 95 END,
    CASE WHEN day_offset < 7 THEN 210 ELSE 245 END,
    CASE WHEN day_offset < 7 THEN 35 ELSE 25 END,
    CASE WHEN day_offset < 7 THEN 80 ELSE 90 END,
    CASE WHEN day_offset < 7 THEN 20 ELSE 10 END,
    'LOCAL_TEST_SEED'
FROM windows;

WITH seed AS (
    SELECT
        generate_series(0, 13) AS day_offset,
        timezone('Asia/Shanghai', now())::date AS local_today
)
INSERT INTO hardware_measurement (
    id, upload_batch_id, tenant_id, user_id, device_id, source_record_id,
    metric_type, observed_at, primary_value, secondary_value, unit,
    quality_code, source
)
SELECT
    md5('admin-intervention-resting-heart-rate-' || day_offset)::uuid,
    '00000000-0000-4000-8000-00000000a001'::uuid,
    '$tenantId', '$adminUserId', '$seedDeviceId',
    'local-test-resting-heart-rate-' || day_offset,
    'resting_heart_rate',
    CASE WHEN day_offset = 0
        THEN now() - interval '45 minutes'
        ELSE ((local_today - day_offset) + time '09:00') AT TIME ZONE 'Asia/Shanghai'
    END,
    CASE WHEN day_offset < 7 THEN 82 + (day_offset % 2) ELSE 76 + (day_offset % 2) END,
    NULL, 'bpm', 'TEST_DATA', 'LOCAL_TEST_SEED'
FROM seed;

INSERT INTO hardware_measurement (
    id, upload_batch_id, tenant_id, user_id, device_id, source_record_id,
    metric_type, observed_at, primary_value, secondary_value, unit,
    quality_code, source
) VALUES
    (md5('admin-intervention-blood-pressure')::uuid,
     '00000000-0000-4000-8000-00000000a001'::uuid,
     '$tenantId', '$adminUserId', '$seedDeviceId', 'local-test-blood-pressure-today',
     'blood_pressure', now() - interval '30 minutes', 138, 88, 'mmHg',
     'TEST_DATA_NOT_CUFF_VALIDATED', 'LOCAL_TEST_SEED'),
    (md5('admin-intervention-blood-oxygen')::uuid,
     '00000000-0000-4000-8000-00000000a001'::uuid,
     '$tenantId', '$adminUserId', '$seedDeviceId', 'local-test-blood-oxygen-today',
     'blood_oxygen', now() - interval '20 minutes', 96, NULL, '%',
     'TEST_DATA', 'LOCAL_TEST_SEED'),
    (md5('admin-intervention-weight')::uuid,
     '00000000-0000-4000-8000-00000000a001'::uuid,
     '$tenantId', '$adminUserId', '$seedDeviceId', 'local-test-weight-today',
     'weight', now() - interval '10 minutes', 81, NULL, 'kg',
     'TEST_DATA', 'LOCAL_TEST_SEED');

WITH local_day AS (
    SELECT timezone('Asia/Shanghai', now())::date AS today
)
INSERT INTO hardware_diet_record (
    id, upload_batch_id, tenant_id, user_id, device_id, source_record_id,
    consumed_at, meal_type, description, calories_kcal, protein_grams,
    carbohydrate_grams, fat_grams, fiber_grams, sodium_milligrams, source
)
SELECT * FROM (
    SELECT
        md5('admin-intervention-breakfast')::uuid,
        '00000000-0000-4000-8000-00000000a001'::uuid,
        '$tenantId'::varchar, '$adminUserId'::varchar, '$seedDeviceId'::varchar,
        'local-test-breakfast-today'::varchar,
        LEAST((today + time '08:00') AT TIME ZONE 'Asia/Shanghai', now() - interval '15 minutes'),
        'breakfast'::varchar, 'Test meal: congee, egg, and salted pickles'::varchar,
        430::numeric, 18::numeric, 62::numeric, 12::numeric, 4::numeric, 1250::numeric,
        'LOCAL_TEST_SEED'::varchar
    FROM local_day
    UNION ALL
    SELECT
        md5('admin-intervention-lunch')::uuid,
        '00000000-0000-4000-8000-00000000a001'::uuid,
        '$tenantId'::varchar, '$adminUserId'::varchar, '$seedDeviceId'::varchar,
        'local-test-lunch-today'::varchar,
        LEAST((today + time '12:00') AT TIME ZONE 'Asia/Shanghai', now() - interval '10 minutes'),
        'lunch'::varchar, 'Test meal: takeaway noodles with vegetables'::varchar,
        720::numeric, 24::numeric, 96::numeric, 24::numeric, 6::numeric, 1850::numeric,
        'LOCAL_TEST_SEED'::varchar
    FROM local_day
    UNION ALL
    SELECT
        md5('admin-intervention-snack')::uuid,
        '00000000-0000-4000-8000-00000000a001'::uuid,
        '$tenantId'::varchar, '$adminUserId'::varchar, '$seedDeviceId'::varchar,
        'local-test-snack-today'::varchar,
        now() - interval '5 minutes',
        'snack'::varchar, 'Test meal: sweetened beverage'::varchar,
        180::numeric, 0::numeric, 45::numeric, 0::numeric, 0::numeric, 35::numeric,
        'LOCAL_TEST_SEED'::varchar
    FROM local_day
) AS meals;

COMMIT;
"@

$hardwareSql | & docker exec -i -e "PGPASSWORD=$hardwarePassword" $HardwareContainer `
    psql -U rehealth_hardware -d rehealth_hardware -X --quiet
if ($LASTEXITCODE -ne 0) {
    throw 'Failed to seed admin intervention telemetry in TimescaleDB.'
}

$verifySoftwareSql = @"
SELECT
    (SELECT COUNT(*) FROM rehealth_patient_profile WHERE user_id = '$adminUserId') AS profiles,
    (SELECT COUNT(*) FROM rehealth_health_interview WHERE user_id = '$adminUserId') AS interviews,
    (SELECT COUNT(*) FROM rehealth_cvd_risk_result
        WHERE user_id = '$adminUserId' AND request_id = '$seedRequestId') AS seed_risks,
    (SELECT COUNT(*) FROM rehealth_device_binding
        WHERE user_id = '$adminUserId' AND device_id = '$seedDeviceId' AND status = 'BOUND') AS seed_bindings;
"@
$verifyHardwareSql = @"
SELECT
    (SELECT COUNT(*) FROM hardware_activity WHERE tenant_id = '$tenantId' AND user_id = '$adminUserId'
        AND device_id = '$seedDeviceId') AS activities,
    (SELECT COUNT(*) FROM hardware_sleep_session WHERE tenant_id = '$tenantId' AND user_id = '$adminUserId'
        AND device_id = '$seedDeviceId') AS sleep_sessions,
    (SELECT COUNT(*) FROM hardware_measurement WHERE tenant_id = '$tenantId' AND user_id = '$adminUserId'
        AND device_id = '$seedDeviceId') AS measurements,
    (SELECT COUNT(*) FROM hardware_diet_record WHERE tenant_id = '$tenantId' AND user_id = '$adminUserId'
        AND device_id = '$seedDeviceId') AS diet_records;
"@

Write-Host "Seeded local intervention test context for admin user $adminUserId in tenant $tenantId."
& docker exec -e "MYSQL_PWD=$softwarePassword" $SoftwareContainer `
    mysql --default-character-set=utf8mb4 -u rehealth_software rehealth_software `
    --batch --raw -e $verifySoftwareSql
if ($LASTEXITCODE -ne 0) {
    throw 'software_db verification failed.'
}
& docker exec -e "PGPASSWORD=$hardwarePassword" $HardwareContainer `
    psql -U rehealth_hardware -d rehealth_hardware -X -P pager=off -c $verifyHardwareSql
if ($LASTEXITCODE -ne 0) {
    throw 'TimescaleDB verification failed.'
}

Write-Host 'Test data is marked LOCAL_TEST_SEED / is_mock=true and must not be used for clinical or production decisions.'
