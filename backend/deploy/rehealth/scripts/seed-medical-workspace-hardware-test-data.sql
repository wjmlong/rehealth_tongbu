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
