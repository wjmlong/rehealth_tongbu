-- 警告：仅限本地/测试环境。严禁在生产数据库执行。
-- 使用 psql -v anchor_date=2026-08-19 执行；原脚本通过 :anchor_date 锚定日期。
SET TIME ZONE 'UTC';

-- ============================================================================
-- 原始来源：backend/deploy/rehealth/scripts/seed-multi-insurer-app-user-hardware-test-data.sql
-- ============================================================================
-- TimescaleDB portion of LOCAL_MULTI_INSURER_APP_QA.
--
-- Mirrors the Android Debug full-chain 50M generator: 118 days (90 visible +
-- 28 warm-up), ten daily measurements, sleep, and activity. One synthetic diet
-- row per day is added so insurer assigned-user detail can exercise nutrition.
-- Shared APP accounts receive tenant-scoped copies only because the current
-- hardware schema and Device Service reads require tenant_id.

\set ON_ERROR_STOP on
SET TIME ZONE 'Asia/Shanghai';

BEGIN;

CREATE TEMP TABLE miqa_relationship (
    tenant_id text NOT NULL,
    member_no integer NOT NULL,
    username text NOT NULL,
    PRIMARY KEY (tenant_id, member_no)
) ON COMMIT DROP;

INSERT INTO miqa_relationship (tenant_id, member_no, username) VALUES
    ('9101', 1, 'local_app_9101_01'), ('9101', 2, 'local_app_9101_02'),
    ('9101', 3, 'local_app_9101_03'), ('9101', 4, 'local_app_9101_04'),
    ('9101', 5, 'local_app_shared_01'), ('9101', 6, 'local_app_shared_02'),
    ('9101', 7, 'local_app_9102_04'), ('9101', 8, 'local_app_9103_04'),
    ('9101', 9, 'local_app_9102_03'), ('9101', 10, 'local_app_9103_03'),
    ('9101', 11, 'local_app_9102_02'), ('9101', 12, 'local_app_9103_02'),
    ('9102', 1, 'local_app_9102_01'), ('9102', 2, 'local_app_9102_02'),
    ('9102', 3, 'local_app_9102_03'), ('9102', 4, 'local_app_9102_04'),
    ('9102', 5, 'local_app_shared_01'), ('9102', 6, 'local_app_shared_02'),
    ('9102', 7, 'local_app_9101_04'), ('9102', 8, 'local_app_9103_04'),
    ('9102', 9, 'local_app_9101_03'), ('9102', 10, 'local_app_9103_03'),
    ('9102', 11, 'local_app_9101_02'), ('9102', 12, 'local_app_9103_02'),
    ('9103', 1, 'local_app_9103_01'), ('9103', 2, 'local_app_9103_02'),
    ('9103', 3, 'local_app_9103_03'), ('9103', 4, 'local_app_9103_04'),
    ('9103', 5, 'local_app_shared_01'), ('9103', 6, 'local_app_shared_02'),
    ('9103', 7, 'local_app_9101_04'), ('9103', 8, 'local_app_9102_04'),
    ('9103', 9, 'local_app_9101_03'), ('9103', 10, 'local_app_9102_03'),
    ('9103', 11, 'local_app_9101_02'), ('9103', 12, 'local_app_9102_02');

DELETE FROM hardware_upload_batch batch
USING miqa_relationship rel
WHERE batch.id = md5(
    'LOCAL_MULTI_INSURER_APP_QA:batch:' || rel.tenant_id || ':' || rel.username
)::uuid;

INSERT INTO hardware_upload_batch (
    id, receipt_id, tenant_id, user_id, device_id, batch_id, source,
    collected_from, collected_to, received_at, committed_at, status,
    record_count, measurement_count, sleep_session_count, activity_count,
    signal_metadata_count, quality_summary, diet_record_count
)
SELECT
    md5('LOCAL_MULTI_INSURER_APP_QA:batch:' || rel.tenant_id || ':' || rel.username)::uuid,
    md5('LOCAL_MULTI_INSURER_APP_QA:receipt:' || rel.tenant_id || ':' || rel.username)::uuid,
    rel.tenant_id,
    md5('LOCAL_MULTI_INSURER_APP_QA:user:' || rel.username),
    'miqa-device-' || rel.username,
    'miqa-118d-' || rel.tenant_id || '-' || rel.member_no || '-' || :'anchor_date',
    'LOCAL_MULTI_INSURER_APP_QA',
    (DATE :'anchor_date' - 117 + TIME '00:00') AT TIME ZONE 'Asia/Shanghai',
    LEAST((DATE :'anchor_date' + TIME '23:59') AT TIME ZONE 'Asia/Shanghai', now()),
    now(), now(), 'PERSISTED',
    1534, 1180, 118, 118, 0,
    jsonb_build_object(
        'testData', true,
        'productionEligible', false,
        'scenario', 'android_debug_full_chain_complete_user',
        'historyDays', 118,
        'quality', 96,
        'sourceSystem', 'LOCAL_MULTI_INSURER_APP_QA'
    ),
    118
FROM miqa_relationship rel;

CREATE TEMP TABLE miqa_day ON COMMIT DROP AS
SELECT
    rel.tenant_id,
    rel.member_no,
    rel.username,
    md5('LOCAL_MULTI_INSURER_APP_QA:user:' || rel.username) AS user_id,
    'miqa-device-' || rel.username AS device_id,
    md5('LOCAL_MULTI_INSURER_APP_QA:batch:' || rel.tenant_id || ':' || rel.username)::uuid AS batch_id,
    day_value::date AS day_date,
    day_index,
    day_index / 117.0 AS trend,
    sin(day_index * 2.0 * pi() / 14.0) AS wave,
    (rel.member_no - 6.5) * 0.24 AS member_bias
FROM miqa_relationship rel
CROSS JOIN LATERAL (
    SELECT
        generated.day_value,
        (row_number() OVER (ORDER BY generated.day_value) - 1)::integer AS day_index
    FROM generate_series(
        DATE :'anchor_date' - 117,
        DATE :'anchor_date',
        INTERVAL '1 day'
    ) AS generated(day_value)
) days;

INSERT INTO hardware_measurement (
    id, upload_batch_id, tenant_id, user_id, device_id, source_record_id,
    metric_type, observed_at, primary_value, secondary_value, unit,
    quality_code, source
)
SELECT
    md5(
        'LOCAL_MULTI_INSURER_APP_QA:measurement:' || day.tenant_id || ':' ||
        day.username || ':' || day.day_date || ':' || metric.suffix
    )::uuid,
    day.batch_id, day.tenant_id, day.user_id, day.device_id,
    'miqa-' || day.tenant_id || '-' || day.username || '-' || day.day_date || '-' || metric.suffix,
    metric.metric_type,
    LEAST(metric.observed_at, now() - INTERVAL '1 minute'),
    round(metric.primary_value::numeric, 1),
    CASE WHEN metric.secondary_value IS NULL THEN NULL ELSE round(metric.secondary_value::numeric, 1) END,
    metric.unit, 'GOOD_96', 'LOCAL_MULTI_INSURER_APP_QA'
FROM miqa_day day
CROSS JOIN LATERAL (
    VALUES
        ('hr-night', 'HEART_RATE',
         (day.day_date + TIME '02:30') AT TIME ZONE 'Asia/Shanghai',
         77.5 - day.trend * 10.0 + day.wave * 1.2 + day.member_bias,
         NULL::double precision, 'bpm'),
        ('hr-morning', 'HEART_RATE',
         (day.day_date + TIME '08:00') AT TIME ZONE 'Asia/Shanghai',
         79.0 - day.trend * 10.0 + day.wave * 1.2 + day.member_bias,
         NULL::double precision, 'bpm'),
        ('hrv', 'HRV',
         (day.day_date + TIME '02:31') AT TIME ZONE 'Asia/Shanghai',
         34.0 + day.trend * 22.0 - day.wave * 1.5 - day.member_bias,
         NULL::double precision, 'ms'),
        ('spo2-a', 'BLOOD_OXYGEN',
         (day.day_date + TIME '02:32') AT TIME ZONE 'Asia/Shanghai',
         95.2 + day.trend * 2.0 - day.member_bias * 0.08,
         NULL::double precision, '%'),
        ('spo2-b', 'BLOOD_OXYGEN',
         (day.day_date + TIME '02:33') AT TIME ZONE 'Asia/Shanghai',
         94.7 + day.trend * 2.0 - day.member_bias * 0.08,
         NULL::double precision, '%'),
        ('bp', 'BLOOD_PRESSURE',
         (day.day_date + TIME '08:04') AT TIME ZONE 'Asia/Shanghai',
         118.0 + day.wave + day.member_bias,
         76.0 + day.wave * 0.5 + day.member_bias * 0.4, 'mmHg'),
        ('steps', 'STEPS',
         (day.day_date + TIME '20:00') AT TIME ZONE 'Asia/Shanghai',
         round(3500 + day.trend * 4500.0 + day.wave * 400.0 - day.member_bias * 90),
         NULL::double precision, 'steps'),
        ('bmi', 'BMI',
         (day.day_date + TIME '08:05') AT TIME ZONE 'Asia/Shanghai',
         (72.5 + day.member_bias - day.trend * 3.9) / (1.75 * 1.75),
         NULL::double precision, 'kg/m2'),
        ('fat', 'FAT_MASS',
         (day.day_date + TIME '08:06') AT TIME ZONE 'Asia/Shanghai',
         (72.5 + day.member_bias - day.trend * 3.9) * 0.19,
         NULL::double precision, 'kg'),
        ('lean', 'FAT_FREE_MASS',
         (day.day_date + TIME '08:06') AT TIME ZONE 'Asia/Shanghai',
         (72.5 + day.member_bias - day.trend * 3.9) * 0.81,
         NULL::double precision, 'kg')
) AS metric(suffix, metric_type, observed_at, primary_value, secondary_value, unit);

INSERT INTO hardware_sleep_session (
    id, upload_batch_id, tenant_id, user_id, device_id, source_record_id,
    started_at, ended_at, deep_minutes, light_minutes, awake_minutes,
    rem_minutes, interruption_minutes, source
)
SELECT
    md5(
        'LOCAL_MULTI_INSURER_APP_QA:sleep:' || day.tenant_id || ':' ||
        day.username || ':' || day.day_date
    )::uuid,
    day.batch_id, day.tenant_id, day.user_id, day.device_id,
    'miqa-' || day.tenant_id || '-' || day.username || '-' || day.day_date || '-sleep',
    values.sleep_start,
    LEAST(values.sleep_start + values.sleep_duration * INTERVAL '1 minute', now() - INTERVAL '1 minute'),
    round((values.sleep_duration - values.awake_minutes) * 0.22)::integer,
    (values.sleep_duration - values.awake_minutes)
        - round((values.sleep_duration - values.awake_minutes) * 0.22)::integer
        - round((values.sleep_duration - values.awake_minutes) * 0.20)::integer,
    values.awake_minutes,
    round((values.sleep_duration - values.awake_minutes) * 0.20)::integer,
    round(14 - day.trend * 8.0)::integer,
    'LOCAL_MULTI_INSURER_APP_QA'
FROM miqa_day day
CROSS JOIN LATERAL (
    SELECT
        (
            day.day_date::timestamp
            + (30 - day.trend * 90.0 + round(day.wave * 28.0 * (1.0 - day.trend))) * INTERVAL '1 minute'
        ) AT TIME ZONE 'Asia/Shanghai' AS sleep_start,
        round(360 + day.trend * 130.0)::integer AS sleep_duration,
        round(round(360 + day.trend * 130.0) * (0.22 - day.trend * 0.14))::integer AS awake_minutes
) values;

INSERT INTO hardware_activity (
    id, upload_batch_id, tenant_id, user_id, device_id, source_record_id,
    started_at, ended_at, activity_type, steps, distance_meters,
    calories_kcal, duration_minutes, average_heart_rate, source
)
SELECT
    md5(
        'LOCAL_MULTI_INSURER_APP_QA:activity:' || day.tenant_id || ':' ||
        day.username || ':' || day.day_date
    )::uuid,
    day.batch_id, day.tenant_id, day.user_id, day.device_id,
    'miqa-' || day.tenant_id || '-' || day.username || '-' || day.day_date || '-activity',
    values.activity_start,
    LEAST(values.activity_start + values.exercise_minutes * INTERVAL '1 minute', now() - INTERVAL '1 minute'),
    'walking', values.steps,
    round((values.steps * 0.68)::numeric, 3),
    round((values.steps * 0.036)::numeric, 3),
    values.exercise_minutes,
    round((112.0 - day.trend * 5.0 + day.wave * 2.0 + day.member_bias)::numeric, 3),
    'LOCAL_MULTI_INSURER_APP_QA'
FROM miqa_day day
CROSS JOIN LATERAL (
    SELECT
        round(3500 + day.trend * 4500.0 + day.wave * 400.0 - day.member_bias * 90)::integer AS steps,
        round(20 + day.trend * 28.0)::integer AS exercise_minutes,
        LEAST(
            (day.day_date + TIME '18:00') AT TIME ZONE 'Asia/Shanghai',
            now() - (round(20 + day.trend * 28.0) + 1) * INTERVAL '1 minute'
        ) AS activity_start
) values;

INSERT INTO hardware_diet_record (
    id, upload_batch_id, tenant_id, user_id, device_id, source_record_id,
    consumed_at, meal_type, description, calories_kcal, protein_grams,
    carbohydrate_grams, fat_grams, fiber_grams, sodium_milligrams, source
)
SELECT
    md5(
        'LOCAL_MULTI_INSURER_APP_QA:diet:' || day.tenant_id || ':' ||
        day.username || ':' || day.day_date
    )::uuid,
    day.batch_id, day.tenant_id, day.user_id, day.device_id,
    'miqa-' || day.tenant_id || '-' || day.username || '-' || day.day_date || '-diet',
    LEAST((day.day_date + TIME '12:30') AT TIME ZONE 'Asia/Shanghai', now() - INTERVAL '1 minute'),
    'lunch', '合成均衡午餐：全谷物、蔬菜和优质蛋白',
    round((620 - day.trend * 45 + day.member_bias * 4)::numeric, 2),
    32.00, 72.00, 21.00, 9.00, 680.00,
    'LOCAL_MULTI_INSURER_APP_QA'
FROM miqa_day day;

COMMIT;

-- ============================================================================
-- 原始来源：backend/deploy/rehealth/scripts/seed-medical-workspace-hardware-test-data.sql
-- ============================================================================
-- TimescaleDB portion of LOCAL_MEDICAL_TEST_SEED.
-- Local/test only. All rows are synthetic and are removed through their batch.

\set ON_ERROR_STOP on
SET TIME ZONE 'Asia/Shanghai';

BEGIN;

CREATE TEMP TABLE mhqa_bound_patient (
    tenant_id text NOT NULL,
    patient_no integer NOT NULL,
    username text NOT NULL,
    PRIMARY KEY (tenant_id, patient_no)
) ON COMMIT DROP;

-- The first 18 software patients are deliberately device-bound.
INSERT INTO mhqa_bound_patient (tenant_id, patient_no, username)
SELECT CASE WHEN patient_no <= 14 THEN '9261' ELSE '9262' END,
       patient_no,
       'local_medical_patient_' || lpad(patient_no::text, 3, '0')
FROM generate_series(1, 18) AS patient(patient_no);

-- A batch owns all child rows and provides a narrow, cascade-safe idempotency boundary.
DELETE FROM hardware_upload_batch batch
USING mhqa_bound_patient patient
WHERE batch.id = md5(
    'LOCAL_MEDICAL_TEST_SEED:batch:' || patient.tenant_id || ':' || patient.username
)::uuid;

INSERT INTO hardware_upload_batch (
    id, receipt_id, tenant_id, user_id, device_id, batch_id, source,
    collected_from, collected_to, received_at, committed_at, status,
    record_count, measurement_count, sleep_session_count, activity_count,
    signal_metadata_count, quality_summary, diet_record_count
)
SELECT
    md5('LOCAL_MEDICAL_TEST_SEED:batch:' || patient.tenant_id || ':' || patient.username)::uuid,
    md5('LOCAL_MEDICAL_TEST_SEED:receipt:' || patient.tenant_id || ':' || patient.username)::uuid,
    patient.tenant_id,
    md5('LOCAL_MEDICAL_TEST_SEED:user:' || patient.username),
    'mhqa-device-' || lpad(patient.patient_no::text, 3, '0'),
    'mhqa-30d-' || patient.patient_no || '-' || :'anchor_date',
    'LOCAL_MEDICAL_TEST_SEED',
    (DATE :'anchor_date' - 29 + TIME '00:00') AT TIME ZONE 'Asia/Shanghai',
    LEAST((DATE :'anchor_date' + TIME '23:59') AT TIME ZONE 'Asia/Shanghai', now() - INTERVAL '1 minute'),
    now(), now(), 'PERSISTED',
    210, 150, 30, 30, 0,
    jsonb_build_object(
        'testData', true,
        'synthetic', true,
        'clinicalUseAllowed', false,
        'historyDays', 30,
        'quality', 94,
        'sourceSystem', 'LOCAL_MEDICAL_TEST_SEED'
    ),
    0
FROM mhqa_bound_patient patient;

CREATE TEMP TABLE mhqa_day ON COMMIT DROP AS
SELECT
    patient.tenant_id,
    patient.patient_no,
    patient.username,
    md5('LOCAL_MEDICAL_TEST_SEED:user:' || patient.username) AS user_id,
    'mhqa-device-' || lpad(patient.patient_no::text, 3, '0') AS device_id,
    md5('LOCAL_MEDICAL_TEST_SEED:batch:' || patient.tenant_id || ':' || patient.username)::uuid AS batch_id,
    generated.day_value::date AS day_date,
    (row_number() OVER (PARTITION BY patient.username ORDER BY generated.day_value) - 1)::integer AS day_index
FROM mhqa_bound_patient patient
CROSS JOIN generate_series(DATE :'anchor_date' - 29, DATE :'anchor_date', INTERVAL '1 day') generated(day_value);

INSERT INTO hardware_measurement (
    id, upload_batch_id, tenant_id, user_id, device_id, source_record_id,
    metric_type, observed_at, primary_value, secondary_value, unit,
    quality_code, source
)
SELECT
    md5('LOCAL_MEDICAL_TEST_SEED:measurement:' || day.username || ':' || day.day_date || ':' || metric.suffix)::uuid,
    day.batch_id, day.tenant_id, day.user_id, day.device_id,
    'mhqa-' || day.username || '-' || day.day_date || '-' || metric.suffix,
    metric.metric_type,
    LEAST(metric.observed_at, now() - INTERVAL '1 minute'),
    round(metric.primary_value::numeric, 1),
    CASE WHEN metric.secondary_value IS NULL THEN NULL ELSE round(metric.secondary_value::numeric, 1) END,
    metric.unit, 'SYNTHETIC_GOOD_94', 'LOCAL_MEDICAL_TEST_SEED'
FROM mhqa_day day
CROSS JOIN LATERAL (
    SELECT day.day_index / 29.0 AS trend,
           sin(day.day_index * 2.0 * pi() / 7.0) AS wave,
           (day.patient_no - 9.5) * 0.35 AS patient_bias
) values
CROSS JOIN LATERAL (
    VALUES
        ('hr', 'HEART_RATE',
         (day.day_date + TIME '08:00') AT TIME ZONE 'Asia/Shanghai',
         82.0 - values.trend * 7.0 + values.wave * 2.0 + values.patient_bias,
         NULL::double precision, 'bpm'),
        ('bp', 'BLOOD_PRESSURE',
         (day.day_date + TIME '08:05') AT TIME ZONE 'Asia/Shanghai',
         138.0 - values.trend * 8.0 + values.wave * 2.5 + values.patient_bias,
         88.0 - values.trend * 5.0 + values.wave + values.patient_bias * 0.35, 'mmHg'),
        ('spo2', 'BLOOD_OXYGEN',
         (day.day_date + TIME '08:10') AT TIME ZONE 'Asia/Shanghai',
         95.0 + values.trend * 1.5 - values.patient_bias * 0.04,
         NULL::double precision, '%'),
        ('steps', 'STEPS',
         (day.day_date + TIME '20:00') AT TIME ZONE 'Asia/Shanghai',
         round(3800 + values.trend * 2600 + values.wave * 280 - values.patient_bias * 45),
         NULL::double precision, 'steps'),
        ('weight', 'WEIGHT',
         (day.day_date + TIME '07:50') AT TIME ZONE 'Asia/Shanghai',
         62.0 + day.patient_no * 0.9 - values.trend * 1.3,
         NULL::double precision, 'kg')
) AS metric(suffix, metric_type, observed_at, primary_value, secondary_value, unit);

INSERT INTO hardware_sleep_session (
    id, upload_batch_id, tenant_id, user_id, device_id, source_record_id,
    started_at, ended_at, deep_minutes, light_minutes, awake_minutes,
    rem_minutes, interruption_minutes, source
)
SELECT
    md5('LOCAL_MEDICAL_TEST_SEED:sleep:' || day.username || ':' || day.day_date)::uuid,
    day.batch_id, day.tenant_id, day.user_id, day.device_id,
    'mhqa-' || day.username || '-' || day.day_date || '-sleep',
    sleep.started_at,
    LEAST(sleep.started_at + sleep.total_minutes * INTERVAL '1 minute', now() - INTERVAL '1 minute'),
    round((sleep.total_minutes - sleep.awake_minutes) * 0.23)::integer,
    round((sleep.total_minutes - sleep.awake_minutes) * 0.57)::integer,
    sleep.awake_minutes,
    round((sleep.total_minutes - sleep.awake_minutes) * 0.20)::integer,
    greatest(3, 13 - round(day.day_index / 4.0))::integer,
    'LOCAL_MEDICAL_TEST_SEED'
FROM mhqa_day day
CROSS JOIN LATERAL (
    SELECT (day.day_date + TIME '00:10' - (day.day_index * INTERVAL '2 minute')) AT TIME ZONE 'Asia/Shanghai' AS started_at,
           (395 + round(day.day_index * 1.5))::integer AS total_minutes,
           greatest(18, 48 - day.day_index)::integer AS awake_minutes
) sleep;

INSERT INTO hardware_activity (
    id, upload_batch_id, tenant_id, user_id, device_id, source_record_id,
    started_at, ended_at, activity_type, steps, distance_meters,
    calories_kcal, duration_minutes, average_heart_rate, source
)
SELECT
    md5('LOCAL_MEDICAL_TEST_SEED:activity:' || day.username || ':' || day.day_date)::uuid,
    day.batch_id, day.tenant_id, day.user_id, day.device_id,
    'mhqa-' || day.username || '-' || day.day_date || '-activity',
    activity.started_at,
    LEAST(activity.started_at + activity.duration_minutes * INTERVAL '1 minute', now() - INTERVAL '1 minute'),
    'walking', activity.steps,
    round((activity.steps * 0.68)::numeric, 3),
    round((activity.steps * 0.035)::numeric, 3),
    activity.duration_minutes,
    round((108 + sin(day.day_index * 2.0 * pi() / 7.0) * 3)::numeric, 3),
    'LOCAL_MEDICAL_TEST_SEED'
FROM mhqa_day day
CROSS JOIN LATERAL (
    SELECT LEAST((day.day_date + TIME '18:00') AT TIME ZONE 'Asia/Shanghai', now() - INTERVAL '50 minute') AS started_at,
           (2200 + day.day_index * 75 + day.patient_no * 18)::integer AS steps,
           (24 + day.day_index / 3)::integer AS duration_minutes
) activity;

COMMIT;

SELECT 'hardware_batches' AS entity, COUNT(*) AS row_count
FROM hardware_upload_batch WHERE source='LOCAL_MEDICAL_TEST_SEED'
UNION ALL SELECT 'measurements', COUNT(*) FROM hardware_measurement WHERE source='LOCAL_MEDICAL_TEST_SEED'
UNION ALL SELECT 'sleep_sessions', COUNT(*) FROM hardware_sleep_session WHERE source='LOCAL_MEDICAL_TEST_SEED'
UNION ALL SELECT 'activities', COUNT(*) FROM hardware_activity WHERE source='LOCAL_MEDICAL_TEST_SEED';
