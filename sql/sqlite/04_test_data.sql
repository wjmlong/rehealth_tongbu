-- 警告：仅限 SQLite/Room 空库或本地测试环境。严禁覆盖真实用户数据库。
PRAGMA foreign_keys = ON;
BEGIN TRANSACTION;

INSERT OR REPLACE INTO health_records(id, type, value, unit, recordedAt, source)
VALUES ('local-health-001', 'resting_heart_rate', '68', 'bpm', 1787101200000, 'LOCAL_SQL_QA');

INSERT OR REPLACE INTO ring_measurements(
    id, metric_type, measured_at, primary_value, secondary_value,
    unit, quality, source, raw_payload, owner_user_id, device_id
) VALUES (
    'local-ring-measurement-001', 'heart_rate', 1787101200000, 68.0, NULL,
    'bpm', 100, 'LOCAL_SQL_QA', NULL, 'local-user-001', 'local-ring-001'
);

INSERT OR REPLACE INTO ring_sleep_sessions(
    id, started_at, ended_at, deep_minutes, light_minutes, awake_minutes,
    rem_minutes, interruption_minutes, source, raw_payload,
    total_sleep_minutes, owner_user_id, device_id
) VALUES (
    'local-ring-sleep-001', 1787058000000, 1787086800000, 105, 245, 25,
    105, 20, 'LOCAL_SQL_QA', NULL, 455, 'local-user-001', 'local-ring-001'
);

INSERT OR REPLACE INTO ring_activities(
    id, started_at, ended_at, activity_type, steps, distance_meters,
    calories_kcal, duration_minutes, average_heart_rate, source,
    raw_payload, owner_user_id, device_id
) VALUES (
    'local-ring-activity-001', 1787104800000, 1787108400000, 'walking',
    6200, 4520.0, 285.0, 60, 102.0, 'LOCAL_SQL_QA', NULL,
    'local-user-001', 'local-ring-001'
);

INSERT OR REPLACE INTO cvd_risk_history(
    user_id, evaluated_on, risk_score, risk_level, evaluated_at
) VALUES ('local-user-001', '2026-08-19', 0.240000, 'low', 1787108400000);

INSERT OR REPLACE INTO intervention_feedback_queue(
    id, owner_user_id, intervention_id, binding_id, tenant_id,
    plan_item_id, occurrence_id, status, note, expected_count,
    completed_count, verification_type, checked_at, created_at,
    upload_status, upload_attempts, last_error, next_retry_at
) VALUES (
    'local-feedback-001', 'local-user-001', 'local-plan-001',
    'local-binding-001', 9101, 'local-plan-item-001',
    'local-occurrence-001', 'completed', '按计划完成', 1.0, 1.0,
    'self_report', 1787108400000, 1787108400000, 'pending', 0, NULL,
    1787108400000
);

COMMIT;
