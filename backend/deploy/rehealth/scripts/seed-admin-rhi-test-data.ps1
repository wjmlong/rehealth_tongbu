[CmdletBinding()]
param(
    [string]$SoftwareContainer = 'rehealth-software-db-1',
    [string]$HardwareContainer = 'rehealth-hardware-db-1',
    [datetime]$AnchorDate = (Get-Date).Date
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

foreach ($container in @($SoftwareContainer, $HardwareContainer)) {
    $running = & docker inspect --format '{{.State.Running}}' $container 2>$null
    if ($LASTEXITCODE -ne 0 -or ($running | Select-Object -First 1) -ne 'true') {
        throw "Required database container is not running: $container"
    }
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
$anchorDateText = $AnchorDate.ToString('yyyy-MM-dd')
$firstDay = $AnchorDate.Date.AddDays(-117)
$firstDayOffset = [DateTimeOffset]::new($firstDay, [TimeSpan]::FromHours(8))
$firstDayEpochMs = $firstDayOffset.ToUnixTimeMilliseconds()
$seedDeviceId = 'local-admin-rhi-device'
$seedBatchId = 'a11d5eed-0000-4000-8000-000000000001'
$seedReceiptId = 'a11d5eed-0000-4000-8000-000000000002'

$softwareSql = @"
SET NAMES utf8mb4;
SET @user_id = '$adminUserId';
SET @seed_device_id = '$seedDeviceId';
SET @now = NOW(3);
SET @first_day_ms = $firstDayEpochMs;
SET @client_updated_ms = UNIX_TIMESTAMP(UTC_TIMESTAMP(3)) * 1000;

START TRANSACTION;

INSERT INTO rehealth_software.rehealth_patient_profile (
    id, user_id, name, gender, age, height_cm, weight_kg, bmi,
    family_history, smoking, drinking, diabetes_history, hypertension_history,
    profile_version, profile_json, created_at, updated_at
) VALUES (
    UUID(), @user_id, 'RHI QA 50M', 'male', 50, 175.00, 68.60, 22.40,
    0, 0, 0, 0, 0,
    1, JSON_OBJECT('source', 'LOCAL_TEST_SEED', 'scenario', 'rhi_debug_full_chain_50m'), @now, @now
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
    profile_json = VALUES(profile_json),
    updated_at = VALUES(updated_at);

INSERT INTO rehealth_software.rehealth_rhi_manual_health_input (
    user_id, sedentary_hours_per_day, waist_circumference_cm, vo2_max_ml_kg_min,
    hba1c_percent, egfr_ml_min_1_73m2, cuff_sbp_7d_mean, cuff_dbp_7d_mean,
    cuff_valid_days, cuff_confirmed, fasting_glucose_mmol_l,
    total_cholesterol_mmol_l, ldl_mmol_l, hdl_mmol_l, triglycerides_mmol_l,
    lab_confirmed, lab_recorded_at, client_updated_at, created_at, updated_at
) VALUES (
    @user_id, 6.50, 84.00, 38.00,
    5.30, 96.00, 118.00, 76.00,
    7, 1, 5.000,
    4.500, 2.500, 1.350, 1.100,
    1, @first_day_ms, @client_updated_ms, @now, @now
)
ON DUPLICATE KEY UPDATE
    sedentary_hours_per_day = VALUES(sedentary_hours_per_day),
    waist_circumference_cm = VALUES(waist_circumference_cm),
    vo2_max_ml_kg_min = VALUES(vo2_max_ml_kg_min),
    hba1c_percent = VALUES(hba1c_percent),
    egfr_ml_min_1_73m2 = VALUES(egfr_ml_min_1_73m2),
    cuff_sbp_7d_mean = VALUES(cuff_sbp_7d_mean),
    cuff_dbp_7d_mean = VALUES(cuff_dbp_7d_mean),
    cuff_valid_days = VALUES(cuff_valid_days),
    cuff_confirmed = VALUES(cuff_confirmed),
    fasting_glucose_mmol_l = VALUES(fasting_glucose_mmol_l),
    total_cholesterol_mmol_l = VALUES(total_cholesterol_mmol_l),
    ldl_mmol_l = VALUES(ldl_mmol_l),
    hdl_mmol_l = VALUES(hdl_mmol_l),
    triglycerides_mmol_l = VALUES(triglycerides_mmol_l),
    lab_confirmed = VALUES(lab_confirmed),
    lab_recorded_at = VALUES(lab_recorded_at),
    client_updated_at = VALUES(client_updated_at),
    updated_at = VALUES(updated_at);

INSERT INTO rehealth_software.rehealth_device_binding (
    id, user_id, device_id, device_name, manufacturer, device_model, model,
    firmware_version, hardware_address_hash, status, bound_at, updated_at
) VALUES (
    'seed-admin-rhi-device-binding-v1', @user_id, @seed_device_id,
    'RHI Full-chain Synthetic Wearable', 'ReHealth QA', 'RH-QA-50M', 'RH-QA-50M',
    'qa-1.0', 'local-test-data-no-real-address-rhi', 'BOUND', @now, @now
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

COMMIT;
"@

& docker exec -e "MYSQL_PWD=$softwarePassword" $SoftwareContainer `
    mysql --default-character-set=utf8mb4 -u rehealth_software -e $softwareSql
if ($LASTEXITCODE -ne 0) {
    throw 'Failed to seed the admin RHI profile and manual inputs in software_db.'
}

$hardwareSql = @"
\set ON_ERROR_STOP on
SET TIME ZONE 'Asia/Shanghai';
BEGIN;

DELETE FROM hardware_upload_batch WHERE id = '$seedBatchId'::uuid;

INSERT INTO hardware_upload_batch (
    id, receipt_id, tenant_id, user_id, device_id, batch_id, source,
    collected_from, collected_to, received_at, committed_at, status,
    record_count, measurement_count, sleep_session_count, activity_count,
    signal_metadata_count, quality_summary
) VALUES (
    '$seedBatchId'::uuid, '$seedReceiptId'::uuid, '$tenantId', '$adminUserId',
    '$seedDeviceId', 'local-admin-rhi-118d-$anchorDateText', 'LOCAL_TEST_SEED',
    (DATE '$anchorDateText' - 117 + TIME '00:00') AT TIME ZONE 'Asia/Shanghai',
    now(), now(), now(), 'PERSISTED',
    1416, 1180, 118, 118, 0,
    jsonb_build_object(
        'testData', true,
        'productionEligible', false,
        'scenario', 'rhi_debug_full_chain_50m',
        'historyDays', 118,
        'quality', 96
    )
);

CREATE TEMP TABLE seed_rhi_days ON COMMIT DROP AS
WITH raw_days AS (
    SELECT
        day_value::date AS day_date,
        (row_number() OVER (ORDER BY day_value) - 1)::integer AS day_index
    FROM generate_series(
        DATE '$anchorDateText' - 117,
        DATE '$anchorDateText',
        INTERVAL '1 day'
    ) AS day_value
)
SELECT
    day_date,
    day_index,
    day_index / 117.0 AS trend,
    sin(day_index * 2.0 * pi() / 14.0) AS wave
FROM raw_days;

INSERT INTO hardware_measurement (
    id, upload_batch_id, tenant_id, user_id, device_id, source_record_id,
    metric_type, observed_at, primary_value, secondary_value, unit,
    quality_code, source
)
SELECT
    md5('admin-rhi-' || day_date || '-' || metric.suffix)::uuid,
    '$seedBatchId'::uuid,
    '$tenantId',
    '$adminUserId',
    '$seedDeviceId',
    'admin-rhi-' || day_date || '-' || metric.suffix,
    metric.metric_type,
    metric.observed_at,
    round(metric.primary_value::numeric, 1),
    CASE WHEN metric.secondary_value IS NULL THEN NULL ELSE round(metric.secondary_value::numeric, 1) END,
    metric.unit,
    'GOOD_96',
    'LOCAL_TEST_SEED'
FROM seed_rhi_days
CROSS JOIN LATERAL (
    VALUES
        ('hr-night', 'HEART_RATE',
         (day_date + TIME '02:30') AT TIME ZONE 'Asia/Shanghai',
         77.5 - trend * 10.0 + wave * 1.2, NULL::double precision, 'bpm'),
        ('hr-morning', 'HEART_RATE',
         (day_date + TIME '08:00') AT TIME ZONE 'Asia/Shanghai',
         79.0 - trend * 10.0 + wave * 1.2, NULL::double precision, 'bpm'),
        ('hrv', 'HRV',
         (day_date + TIME '02:31') AT TIME ZONE 'Asia/Shanghai',
         34.0 + trend * 22.0 - wave * 1.5, NULL::double precision, 'ms'),
        ('spo2-a', 'BLOOD_OXYGEN',
         (day_date + TIME '02:32') AT TIME ZONE 'Asia/Shanghai',
         95.2 + trend * 2.0, NULL::double precision, '%'),
        ('spo2-b', 'BLOOD_OXYGEN',
         (day_date + TIME '02:33') AT TIME ZONE 'Asia/Shanghai',
         94.7 + trend * 2.0, NULL::double precision, '%'),
        ('bp', 'BLOOD_PRESSURE',
         (day_date + TIME '08:04') AT TIME ZONE 'Asia/Shanghai',
         118.0 + wave, 76.0 + wave * 0.5, 'mmHg'),
        ('steps', 'STEPS',
         LEAST(
             (day_date + TIME '20:00') AT TIME ZONE 'Asia/Shanghai',
             now() - INTERVAL '1 minute'
         ),
         round(3500 + trend * 4500.0 + wave * 400.0), NULL::double precision, 'steps'),
        ('bmi', 'BMI',
         (day_date + TIME '08:05') AT TIME ZONE 'Asia/Shanghai',
         (72.5 - trend * 3.9) / (1.75 * 1.75), NULL::double precision, 'kg/m2'),
        ('fat', 'FAT_MASS',
         (day_date + TIME '08:06') AT TIME ZONE 'Asia/Shanghai',
         (72.5 - trend * 3.9) * 0.19, NULL::double precision, 'kg'),
        ('lean', 'FAT_FREE_MASS',
         (day_date + TIME '08:06') AT TIME ZONE 'Asia/Shanghai',
         (72.5 - trend * 3.9) * 0.81, NULL::double precision, 'kg')
) AS metric(suffix, metric_type, observed_at, primary_value, secondary_value, unit);

INSERT INTO hardware_sleep_session (
    id, upload_batch_id, tenant_id, user_id, device_id, source_record_id,
    started_at, ended_at, deep_minutes, light_minutes, awake_minutes,
    rem_minutes, interruption_minutes, source
)
SELECT
    md5('admin-rhi-' || day_date || '-sleep')::uuid,
    '$seedBatchId'::uuid,
    '$tenantId',
    '$adminUserId',
    '$seedDeviceId',
    'admin-rhi-' || day_date || '-sleep',
    sleep_start,
    sleep_start + sleep_duration * INTERVAL '1 minute',
    round((sleep_duration - awake_minutes) * 0.22)::integer,
    (sleep_duration - awake_minutes)
        - round((sleep_duration - awake_minutes) * 0.22)::integer
        - round((sleep_duration - awake_minutes) * 0.20)::integer,
    awake_minutes,
    round((sleep_duration - awake_minutes) * 0.20)::integer,
    round(14 - trend * 8.0)::integer,
    'LOCAL_TEST_SEED'
FROM (
    SELECT
        *,
        (
            day_date::timestamp
            + (30 - trend * 90.0 + round(wave * 28.0 * (1.0 - trend))) * INTERVAL '1 minute'
        ) AT TIME ZONE 'Asia/Shanghai' AS sleep_start,
        round(360 + trend * 130.0)::integer AS sleep_duration,
        round(
            round(360 + trend * 130.0) * (0.22 - trend * 0.14)
        )::integer AS awake_minutes
    FROM seed_rhi_days
) AS sleep_values;

INSERT INTO hardware_activity (
    id, upload_batch_id, tenant_id, user_id, device_id, source_record_id,
    started_at, ended_at, activity_type, steps, distance_meters,
    calories_kcal, duration_minutes, average_heart_rate, source
)
SELECT
    md5('admin-rhi-' || day_date || '-activity')::uuid,
    '$seedBatchId'::uuid,
    '$tenantId',
    '$adminUserId',
    '$seedDeviceId',
    'admin-rhi-' || day_date || '-activity',
    activity_start,
    activity_start + exercise_minutes * INTERVAL '1 minute',
    'walking',
    steps,
    round((steps * 0.68)::numeric, 3),
    round((steps * 0.036)::numeric, 3),
    exercise_minutes,
    round((112.0 - trend * 5.0 + wave * 2.0)::numeric, 3),
    'LOCAL_TEST_SEED'
FROM (
    SELECT
        *,
        round(3500 + trend * 4500.0 + wave * 400.0)::integer AS steps,
        round(20 + trend * 28.0)::integer AS exercise_minutes,
        LEAST(
            (day_date + TIME '18:00') AT TIME ZONE 'Asia/Shanghai',
            now() - (round(20 + trend * 28.0) + 1) * INTERVAL '1 minute'
        ) AS activity_start
    FROM seed_rhi_days
) AS activity_values;

COMMIT;
"@

$hardwareSql | & docker exec -i -e "PGPASSWORD=$hardwarePassword" $HardwareContainer `
    psql -X -q -U rehealth_hardware -d rehealth_hardware
if ($LASTEXITCODE -ne 0) {
    throw 'Failed to seed the 118-day admin RHI history in hardware_db.'
}

$softwareVerificationSql = @"
SELECT CONCAT_WS('|',
    p.user_id,
    p.age,
    p.gender,
    p.bmi,
    m.cuff_valid_days,
    m.cuff_confirmed,
    m.lab_confirmed,
    m.waist_circumference_cm,
    m.vo2_max_ml_kg_min
)
FROM rehealth_software.rehealth_patient_profile p
JOIN rehealth_software.rehealth_rhi_manual_health_input m ON m.user_id = p.user_id
WHERE p.user_id = '$adminUserId';
"@
$softwareVerification = & docker exec -e "MYSQL_PWD=$softwarePassword" $SoftwareContainer `
    mysql --default-character-set=utf8mb4 -u rehealth_software --batch --raw `
    --skip-column-names -e $softwareVerificationSql
if ($LASTEXITCODE -ne 0) {
    throw 'Failed to verify the admin RHI software data.'
}

$hardwareVerificationSql = @"
SELECT concat_ws('|',
    b.user_id,
    b.tenant_id,
    b.record_count,
    (SELECT count(*) FROM hardware_measurement m WHERE m.upload_batch_id = b.id),
    (SELECT count(*) FROM hardware_sleep_session s WHERE s.upload_batch_id = b.id),
    (SELECT count(*) FROM hardware_activity a WHERE a.upload_batch_id = b.id),
    (SELECT count(DISTINCT (m.observed_at AT TIME ZONE 'Asia/Shanghai')::date)
     FROM hardware_measurement m WHERE m.upload_batch_id = b.id),
    (SELECT min((m.observed_at AT TIME ZONE 'Asia/Shanghai')::date)
     FROM hardware_measurement m WHERE m.upload_batch_id = b.id),
    (SELECT max((m.observed_at AT TIME ZONE 'Asia/Shanghai')::date)
     FROM hardware_measurement m WHERE m.upload_batch_id = b.id)
)
FROM hardware_upload_batch b
WHERE b.id = '$seedBatchId'::uuid;
"@
$hardwareVerification = & docker exec -e "PGPASSWORD=$hardwarePassword" $HardwareContainer `
    psql -X -q -At -U rehealth_hardware -d rehealth_hardware -c $hardwareVerificationSql
if ($LASTEXITCODE -ne 0) {
    throw 'Failed to verify the admin RHI hardware data.'
}

$softwareFields = (($softwareVerification | Select-Object -First 1).Trim()).Split('|')
$hardwareFields = (($hardwareVerification | Select-Object -First 1).Trim()).Split('|')
if ($softwareFields.Count -ne 9 -or $softwareFields[0] -ne $adminUserId -or
    $softwareFields[4] -ne '7' -or $softwareFields[5] -ne '1' -or $softwareFields[6] -ne '1') {
    throw "Software verification did not meet the RHI fixture requirements: $softwareVerification"
}
if ($hardwareFields.Count -ne 9 -or $hardwareFields[0] -ne $adminUserId -or
    $hardwareFields[1] -ne $tenantId -or $hardwareFields[2] -ne '1416' -or
    $hardwareFields[3] -ne '1180' -or $hardwareFields[4] -ne '118' -or
    $hardwareFields[5] -ne '118' -or $hardwareFields[6] -ne '118') {
    throw "Hardware verification did not meet the RHI fixture requirements: $hardwareVerification"
}

Write-Output 'Admin RHI test data seeded successfully.'
Write-Output "  user_id: $adminUserId"
Write-Output "  tenant_id: $tenantId"
Write-Output "  anchor_date: $anchorDateText"
Write-Output "  device_id: $seedDeviceId"
Write-Output '  software: profile=1, manual_input=1, cuff_valid_days=7, lab_confirmed=true'
Write-Output '  hardware: measurements=1180, sleep_sessions=118, activities=118, history_days=118'
Write-Output '  source: LOCAL_TEST_SEED (synthetic QA only; not production or clinical data)'
