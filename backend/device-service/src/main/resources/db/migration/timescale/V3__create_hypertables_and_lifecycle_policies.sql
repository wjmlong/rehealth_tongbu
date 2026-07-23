SELECT create_hypertable(
    'hardware_measurement',
    by_range('observed_at', INTERVAL '1 day'),
    if_not_exists => TRUE
);
SELECT create_hypertable(
    'hardware_sleep_session',
    by_range('started_at', INTERVAL '7 days'),
    if_not_exists => TRUE
);
SELECT create_hypertable(
    'hardware_activity',
    by_range('started_at', INTERVAL '7 days'),
    if_not_exists => TRUE
);
SELECT create_hypertable(
    'hardware_data_quality_event',
    by_range('event_at', INTERVAL '7 days'),
    if_not_exists => TRUE
);

ALTER TABLE hardware_measurement SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'tenant_id,user_id,device_id',
    timescaledb.compress_orderby = 'observed_at DESC'
);
ALTER TABLE hardware_sleep_session SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'tenant_id,user_id,device_id',
    timescaledb.compress_orderby = 'started_at DESC'
);
ALTER TABLE hardware_activity SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'tenant_id,user_id,device_id',
    timescaledb.compress_orderby = 'started_at DESC'
);
ALTER TABLE hardware_data_quality_event SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'tenant_id,user_id,device_id',
    timescaledb.compress_orderby = 'event_at DESC'
);

SELECT add_compression_policy(
    'hardware_measurement', compress_after => INTERVAL '7 days', if_not_exists => TRUE
);
SELECT add_compression_policy(
    'hardware_sleep_session', compress_after => INTERVAL '7 days', if_not_exists => TRUE
);
SELECT add_compression_policy(
    'hardware_activity', compress_after => INTERVAL '7 days', if_not_exists => TRUE
);
SELECT add_compression_policy(
    'hardware_data_quality_event', compress_after => INTERVAL '7 days', if_not_exists => TRUE
);

SELECT add_retention_policy(
    'hardware_measurement',
    drop_after => INTERVAL '${measurementRetentionDays} days',
    if_not_exists => TRUE
);
SELECT add_retention_policy(
    'hardware_sleep_session',
    drop_after => INTERVAL '${measurementRetentionDays} days',
    if_not_exists => TRUE
);
SELECT add_retention_policy(
    'hardware_activity',
    drop_after => INTERVAL '${measurementRetentionDays} days',
    if_not_exists => TRUE
);
SELECT add_retention_policy(
    'hardware_data_quality_event',
    drop_after => INTERVAL '${operationalRetentionDays} days',
    if_not_exists => TRUE
);

CREATE OR REPLACE PROCEDURE rehealth_apply_ordinary_retention(
    job_id integer,
    config jsonb
)
LANGUAGE plpgsql
AS $$
BEGIN
    DELETE FROM hardware_signal_chunk_metadata
    WHERE created_at < now() - make_interval(
        days => (config->>'signal_metadata_retention_days')::integer
    );

    DELETE FROM hardware_outbox
    WHERE status = 'PUBLISHED'
      AND published_at < now() - make_interval(
          days => (config->>'published_outbox_retention_days')::integer
      );

    DELETE FROM hardware_reconciliation
    WHERE state IN ('EVENT_PUBLISHED', 'REJECTED', 'RESOLVED')
      AND updated_at < now() - make_interval(
          days => (config->>'operational_retention_days')::integer
      );

    DELETE FROM hardware_upload_batch batch
    WHERE batch.received_at < now() - make_interval(
              days => (config->>'operational_retention_days')::integer
          )
      AND NOT EXISTS (
          SELECT 1
          FROM hardware_outbox outbox
          WHERE outbox.upload_batch_id = batch.id
      )
      AND NOT EXISTS (
          SELECT 1
          FROM hardware_reconciliation reconciliation
          WHERE reconciliation.upload_batch_id = batch.id
            AND reconciliation.state NOT IN ('EVENT_PUBLISHED', 'REJECTED', 'RESOLVED')
      );
END
$$;

SELECT add_job(
    'rehealth_apply_ordinary_retention',
    INTERVAL '1 day',
    config => jsonb_build_object(
        'signal_metadata_retention_days', ${signalMetadataRetentionDays},
        'operational_retention_days', ${operationalRetentionDays},
        'published_outbox_retention_days', ${publishedOutboxRetentionDays}
    )
);
