CREATE TABLE hardware_upload_batch (
    id uuid PRIMARY KEY,
    receipt_id uuid NOT NULL UNIQUE,
    tenant_id varchar(64) NOT NULL,
    user_id varchar(64) NOT NULL,
    device_id varchar(128) NOT NULL,
    batch_id varchar(128) NOT NULL,
    source varchar(64),
    collected_from timestamp with time zone,
    collected_to timestamp with time zone,
    received_at timestamp with time zone NOT NULL,
    committed_at timestamp with time zone,
    status varchar(32) NOT NULL,
    record_count integer NOT NULL DEFAULT 0 CHECK (record_count >= 0),
    measurement_count integer NOT NULL DEFAULT 0 CHECK (measurement_count >= 0),
    sleep_session_count integer NOT NULL DEFAULT 0 CHECK (sleep_session_count >= 0),
    activity_count integer NOT NULL DEFAULT 0 CHECK (activity_count >= 0),
    signal_metadata_count integer NOT NULL DEFAULT 0 CHECK (signal_metadata_count >= 0),
    quality_summary jsonb NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT uq_hardware_batch_source UNIQUE (tenant_id, user_id, device_id, batch_id),
    CONSTRAINT ck_hardware_batch_status CHECK (
        status IN ('RECEIVED', 'PERSISTED', 'EVENT_PENDING', 'EVENT_PUBLISHED',
                   'REJECTED', 'RETRYABLE_FAILURE', 'DLQ_REVIEW', 'RESOLVED')
    )
);

CREATE INDEX idx_hardware_batch_user_time
    ON hardware_upload_batch (tenant_id, user_id, received_at DESC);
CREATE INDEX idx_hardware_batch_device_time
    ON hardware_upload_batch (tenant_id, device_id, received_at DESC);

CREATE TABLE hardware_measurement (
    id uuid NOT NULL,
    upload_batch_id uuid NOT NULL REFERENCES hardware_upload_batch (id) ON DELETE CASCADE,
    tenant_id varchar(64) NOT NULL,
    user_id varchar(64) NOT NULL,
    device_id varchar(128) NOT NULL,
    source_record_id varchar(128) NOT NULL,
    metric_type varchar(64) NOT NULL,
    observed_at timestamp with time zone NOT NULL,
    primary_value numeric(20, 6) NOT NULL,
    secondary_value numeric(20, 6),
    unit varchar(32) NOT NULL,
    quality_code varchar(64),
    source varchar(64),
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    PRIMARY KEY (id, observed_at),
    CONSTRAINT uq_hardware_measurement_source
        UNIQUE (tenant_id, user_id, device_id, observed_at, metric_type, source_record_id)
);

CREATE INDEX idx_hardware_measurement_scoped_recent
    ON hardware_measurement (tenant_id, user_id, device_id, observed_at DESC);
CREATE INDEX idx_hardware_measurement_metric_time
    ON hardware_measurement (tenant_id, metric_type, observed_at DESC);

CREATE TABLE hardware_sleep_session (
    id uuid NOT NULL,
    upload_batch_id uuid NOT NULL REFERENCES hardware_upload_batch (id) ON DELETE CASCADE,
    tenant_id varchar(64) NOT NULL,
    user_id varchar(64) NOT NULL,
    device_id varchar(128) NOT NULL,
    source_record_id varchar(128) NOT NULL,
    started_at timestamp with time zone NOT NULL,
    ended_at timestamp with time zone NOT NULL,
    deep_minutes integer NOT NULL DEFAULT 0 CHECK (deep_minutes >= 0),
    light_minutes integer NOT NULL DEFAULT 0 CHECK (light_minutes >= 0),
    awake_minutes integer NOT NULL DEFAULT 0 CHECK (awake_minutes >= 0),
    rem_minutes integer NOT NULL DEFAULT 0 CHECK (rem_minutes >= 0),
    interruption_minutes integer NOT NULL DEFAULT 0 CHECK (interruption_minutes >= 0),
    source varchar(64),
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    PRIMARY KEY (id, started_at),
    CONSTRAINT uq_hardware_sleep_source
        UNIQUE (tenant_id, user_id, device_id, started_at, source_record_id),
    CONSTRAINT ck_hardware_sleep_window CHECK (ended_at >= started_at)
);

CREATE INDEX idx_hardware_sleep_scoped_recent
    ON hardware_sleep_session (tenant_id, user_id, device_id, started_at DESC);

CREATE TABLE hardware_activity (
    id uuid NOT NULL,
    upload_batch_id uuid NOT NULL REFERENCES hardware_upload_batch (id) ON DELETE CASCADE,
    tenant_id varchar(64) NOT NULL,
    user_id varchar(64) NOT NULL,
    device_id varchar(128) NOT NULL,
    source_record_id varchar(128) NOT NULL,
    started_at timestamp with time zone NOT NULL,
    ended_at timestamp with time zone,
    activity_type varchar(64) NOT NULL,
    steps integer NOT NULL DEFAULT 0 CHECK (steps >= 0),
    distance_meters numeric(20, 3) NOT NULL DEFAULT 0 CHECK (distance_meters >= 0),
    calories_kcal numeric(20, 3) NOT NULL DEFAULT 0 CHECK (calories_kcal >= 0),
    duration_minutes integer NOT NULL DEFAULT 0 CHECK (duration_minutes >= 0),
    average_heart_rate numeric(10, 3),
    source varchar(64),
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    PRIMARY KEY (id, started_at),
    CONSTRAINT uq_hardware_activity_source
        UNIQUE (tenant_id, user_id, device_id, started_at, activity_type, source_record_id),
    CONSTRAINT ck_hardware_activity_window CHECK (ended_at IS NULL OR ended_at >= started_at)
);

CREATE INDEX idx_hardware_activity_scoped_recent
    ON hardware_activity (tenant_id, user_id, device_id, started_at DESC);

CREATE TABLE hardware_signal_chunk_metadata (
    id uuid PRIMARY KEY,
    upload_batch_id uuid NOT NULL REFERENCES hardware_upload_batch (id) ON DELETE CASCADE,
    tenant_id varchar(64) NOT NULL,
    user_id varchar(64) NOT NULL,
    device_id varchar(128) NOT NULL,
    source_record_id varchar(128) NOT NULL,
    signal_type varchar(32) NOT NULL,
    started_at timestamp with time zone NOT NULL,
    ended_at timestamp with time zone,
    sample_rate_hz numeric(10, 3),
    sample_count integer CHECK (sample_count IS NULL OR sample_count >= 0),
    quality_code varchar(64),
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT uq_hardware_signal_metadata_source
        UNIQUE (tenant_id, user_id, device_id, started_at, signal_type, source_record_id),
    CONSTRAINT ck_hardware_signal_window CHECK (ended_at IS NULL OR ended_at >= started_at)
);

CREATE INDEX idx_hardware_signal_metadata_retention
    ON hardware_signal_chunk_metadata (created_at);

CREATE TABLE hardware_data_quality_event (
    id uuid NOT NULL,
    upload_batch_id uuid REFERENCES hardware_upload_batch (id) ON DELETE CASCADE,
    tenant_id varchar(64) NOT NULL,
    user_id varchar(64) NOT NULL,
    device_id varchar(128) NOT NULL,
    source_record_id varchar(128) NOT NULL,
    event_type varchar(64) NOT NULL,
    severity varchar(32) NOT NULL,
    detail_code varchar(128),
    event_at timestamp with time zone NOT NULL,
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    PRIMARY KEY (id, event_at),
    CONSTRAINT uq_hardware_quality_source
        UNIQUE (tenant_id, user_id, device_id, event_at, event_type, source_record_id),
    CONSTRAINT ck_hardware_quality_severity CHECK (severity IN ('INFO', 'WARN', 'ERROR'))
);

CREATE INDEX idx_hardware_quality_scoped_recent
    ON hardware_data_quality_event (tenant_id, user_id, device_id, event_at DESC);

CREATE TABLE hardware_reconciliation (
    id uuid PRIMARY KEY,
    upload_batch_id uuid NOT NULL UNIQUE REFERENCES hardware_upload_batch (id) ON DELETE CASCADE,
    tenant_id varchar(64) NOT NULL,
    state varchar(32) NOT NULL,
    attempt_count integer NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    last_error_code varchar(128),
    operator_actor_id varchar(128),
    operator_reason varchar(512),
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT ck_hardware_reconciliation_state CHECK (
        state IN ('RECEIVED', 'PERSISTED', 'EVENT_PENDING', 'EVENT_PUBLISHED',
                  'REJECTED', 'RETRYABLE_FAILURE', 'DLQ_REVIEW', 'RESOLVED')
    )
);

CREATE INDEX idx_hardware_reconciliation_state_time
    ON hardware_reconciliation (tenant_id, state, updated_at);

CREATE TABLE hardware_outbox (
    id uuid PRIMARY KEY,
    upload_batch_id uuid NOT NULL REFERENCES hardware_upload_batch (id) ON DELETE CASCADE,
    tenant_id varchar(64) NOT NULL,
    aggregate_type varchar(64) NOT NULL,
    aggregate_id varchar(128) NOT NULL,
    event_type varchar(128) NOT NULL,
    event_version integer NOT NULL CHECK (event_version > 0),
    status varchar(32) NOT NULL,
    event_metadata jsonb NOT NULL DEFAULT '{}'::jsonb,
    attempt_count integer NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    available_at timestamp with time zone NOT NULL DEFAULT now(),
    published_at timestamp with time zone,
    last_error_code varchar(128),
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT uq_hardware_outbox_event
        UNIQUE (aggregate_type, aggregate_id, event_type, event_version),
    CONSTRAINT ck_hardware_outbox_status CHECK (
        status IN ('PENDING', 'PUBLISHING', 'PUBLISHED', 'FAILED', 'DLQ_REVIEW')
    )
);

CREATE INDEX idx_hardware_outbox_dispatch
    ON hardware_outbox (status, available_at)
    WHERE status IN ('PENDING', 'FAILED');
CREATE INDEX idx_hardware_outbox_published_retention
    ON hardware_outbox (published_at)
    WHERE status = 'PUBLISHED';

CREATE TABLE hardware_migration_checkpoint (
    id uuid PRIMARY KEY,
    source_name varchar(128) NOT NULL,
    checkpoint_key varchar(256) NOT NULL,
    source_position varchar(512) NOT NULL,
    row_count bigint NOT NULL DEFAULT 0 CHECK (row_count >= 0),
    source_hash varchar(128),
    target_hash varchar(128),
    status varchar(32) NOT NULL,
    checked_at timestamp with time zone NOT NULL DEFAULT now(),
    CONSTRAINT uq_hardware_migration_checkpoint UNIQUE (source_name, checkpoint_key),
    CONSTRAINT ck_hardware_migration_status CHECK (
        status IN ('PENDING', 'VERIFIED', 'DRIFTED', 'BLOCKED')
    )
);
