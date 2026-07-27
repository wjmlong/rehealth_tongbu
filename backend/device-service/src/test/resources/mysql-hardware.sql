INSERT INTO hardware_upload_batch (
    id, receipt_id, batch_id, user_id, device_id, source,
    collected_from, collected_to, received_at, committed_at, status,
    record_count, measurement_count, sleep_session_count, activity_count,
    signal_chunk_count, quality_json
) VALUES (
    '10000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000002',
    'task10-batch-1', 'task10-user-1', 'task10-device-1', 'mrd',
    '2026-07-23 22:00:00.000', '2026-07-24 00:03:00.000',
    '2026-07-23 23:59:59.000', '2026-07-23 23:59:59.500',
    'COMMITTED', 4, 1, 1, 1, 1, NULL
) ON DUPLICATE KEY UPDATE id = VALUES(id);

INSERT INTO hardware_measurement (
    id, upload_batch_id, client_record_id, user_id, device_id,
    metric_type, measured_at, primary_value, secondary_value,
    unit, quality_code, source, created_at
) VALUES (
    '20000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000001',
    NULL, 'task10-user-1', 'task10-device-1',
    'heart_rate', '2026-07-23 23:59:59.123', 70.000000, NULL,
    'bpm', NULL, 'mrd', '2026-07-23 23:59:59.500'
) ON DUPLICATE KEY UPDATE id = VALUES(id);

INSERT INTO hardware_sleep_session (
    id, upload_batch_id, client_record_id, user_id, device_id,
    started_at, ended_at, deep_minutes, light_minutes, awake_minutes,
    rem_minutes, interruption_minutes, source, created_at
) VALUES (
    '30000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000001',
    'sleep-source-1', 'task10-user-1', 'task10-device-1',
    '2026-07-23 22:00:00.000', '2026-07-24 00:00:00.000',
    30, 60, 10, 20, 5, NULL, '2026-07-24 00:00:00.100'
) ON DUPLICATE KEY UPDATE id = VALUES(id);

INSERT INTO hardware_activity (
    id, upload_batch_id, client_record_id, user_id, device_id,
    started_at, ended_at, activity_type, steps, distance_meters,
    calories_kcal, duration_minutes, average_heart_rate, source, created_at
) VALUES (
    '40000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000001',
    NULL, 'task10-user-1', 'task10-device-1',
    '2026-07-24 00:01:00.000', NULL, 'walking',
    165, 120.500, 9.250, 2, NULL, 'mrd', '2026-07-24 00:01:00.100'
) ON DUPLICATE KEY UPDATE id = VALUES(id);

INSERT INTO hardware_signal_chunk_metadata (
    id, upload_batch_id, user_id, device_id, signal_type, started_at,
    sample_rate_hz, sample_count, payload_ref, retention_expires_at, created_at
) VALUES (
    '50000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000001',
    'task10-user-1', 'task10-device-1', 'rri',
    '2026-07-24 00:02:00.000', NULL, 0,
    'legacy://payload-is-intentionally-not-migrated',
    '2026-10-22 00:02:00.000', '2026-07-24 00:02:00.100'
) ON DUPLICATE KEY UPDATE id = VALUES(id);

INSERT INTO hardware_data_quality_event (
    id, upload_batch_id, user_id, device_id, event_type, severity,
    message, occurred_at, created_at
) VALUES (
    '60000000-0000-0000-0000-000000000001',
    '10000000-0000-0000-0000-000000000001',
    'task10-user-1', 'task10-device-1',
    'LEGACY_SIGNAL_GAP', 'WARN', 'synthetic fixture detail',
    '2026-07-24 00:03:00.000', '2026-07-24 00:03:00.100'
) ON DUPLICATE KEY UPDATE id = VALUES(id);
