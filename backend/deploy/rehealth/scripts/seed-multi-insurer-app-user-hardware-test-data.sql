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
