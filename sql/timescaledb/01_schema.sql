-- ReHealth hardware_db 当前结构快照
-- 目标版本：PostgreSQL 17 + TimescaleDB 2.21；时区 UTC
-- 默认保留期来自 device-service/application.yml：测量 730 天、信号元数据 90 天、运营数据 1095 天、已发布 Outbox 30 天。
SET TIME ZONE 'UTC';

CREATE EXTENSION IF NOT EXISTS timescaledb;

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
    diet_record_count integer NOT NULL DEFAULT 0 CHECK (diet_record_count >= 0),
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


CREATE TABLE hardware_diet_record (
    id uuid NOT NULL,
    upload_batch_id uuid NOT NULL REFERENCES hardware_upload_batch (id) ON DELETE CASCADE,
    tenant_id varchar(64) NOT NULL,
    user_id varchar(64) NOT NULL,
    device_id varchar(128) NOT NULL,
    source_record_id varchar(128) NOT NULL,
    consumed_at timestamp with time zone NOT NULL,
    meal_type varchar(16) NOT NULL,
    description varchar(256) NOT NULL,
    calories_kcal numeric(12, 3),
    protein_grams numeric(12, 3),
    carbohydrate_grams numeric(12, 3),
    fat_grams numeric(12, 3),
    fiber_grams numeric(12, 3),
    sodium_milligrams numeric(12, 3),
    source varchar(64),
    created_at timestamp with time zone NOT NULL DEFAULT now(),
    PRIMARY KEY (id, consumed_at),
    CONSTRAINT uq_hardware_diet_source
        UNIQUE (tenant_id, user_id, device_id, consumed_at, source_record_id),
    CONSTRAINT ck_hardware_diet_meal_type
        CHECK (meal_type IN ('breakfast', 'lunch', 'dinner', 'snack')),
    CONSTRAINT ck_hardware_diet_non_negative
        CHECK (
            (calories_kcal IS NULL OR calories_kcal >= 0)
            AND (protein_grams IS NULL OR protein_grams >= 0)
            AND (carbohydrate_grams IS NULL OR carbohydrate_grams >= 0)
            AND (fat_grams IS NULL OR fat_grams >= 0)
            AND (fiber_grams IS NULL OR fiber_grams >= 0)
            AND (sodium_milligrams IS NULL OR sodium_milligrams >= 0)
        )
);

CREATE INDEX idx_hardware_diet_scoped_recent
    ON hardware_diet_record (tenant_id, user_id, consumed_at DESC);

SELECT create_hypertable(
    'hardware_diet_record',
    by_range('consumed_at', INTERVAL '7 days'),
    if_not_exists => TRUE
);

ALTER TABLE hardware_diet_record SET (
    timescaledb.compress,
    timescaledb.compress_segmentby = 'tenant_id,user_id,device_id',
    timescaledb.compress_orderby = 'consumed_at DESC'
);

SELECT add_compression_policy(
    'hardware_diet_record',
    compress_after => INTERVAL '7 days',
    if_not_exists => TRUE
);

SELECT add_retention_policy(
    'hardware_diet_record',
    drop_after => INTERVAL '730 days',
    if_not_exists => TRUE
);


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
    drop_after => INTERVAL '730 days',
    if_not_exists => TRUE
);
SELECT add_retention_policy(
    'hardware_sleep_session',
    drop_after => INTERVAL '730 days',
    if_not_exists => TRUE
);
SELECT add_retention_policy(
    'hardware_activity',
    drop_after => INTERVAL '730 days',
    if_not_exists => TRUE
);
SELECT add_retention_policy(
    'hardware_data_quality_event',
    drop_after => INTERVAL '1095 days',
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
        'signal_metadata_retention_days', 90,
        'operational_retention_days', 1095,
        'published_outbox_retention_days', 30
    )
);



-- 中文数据库注释
COMMENT ON TABLE hardware_activity IS '硬件活动表；保存规范化活动、步数、距离、热量、时长和心率。';
COMMENT ON TABLE hardware_data_quality_event IS '硬件数据质量事件表；保存遥测质量事件、严重程度和详情码。';
COMMENT ON TABLE hardware_diet_record IS '硬件域饮食行为表；保存随 telemetry-v2 批次提交的规范化饮食行为。';
COMMENT ON TABLE hardware_measurement IS '硬件标量测量表；保存规范化标量测量，是当前 TimescaleDB 权威遥测事实表。';
COMMENT ON TABLE hardware_migration_checkpoint IS '硬件迁移检查点表；保存旧 MySQL 硬件数据迁移位置、行数、哈希和校验状态。';
COMMENT ON TABLE hardware_outbox IS '遥测事务 Outbox 表；与遥测事实同事务写入，随后可靠发布隐私安全 Kafka 事件。';
COMMENT ON TABLE hardware_reconciliation IS '硬件批次对账表；保存每个上传批次唯一的对账状态、重试和人工处理元数据。';
COMMENT ON TABLE hardware_signal_chunk_metadata IS '硬件信号元数据表；只保存信号时间窗、采样率和质量元数据，不保存原始波形。';
COMMENT ON TABLE hardware_sleep_session IS '硬件睡眠会话表；保存规范化睡眠会话和阶段分钟数。';
COMMENT ON TABLE hardware_upload_batch IS '硬件上传批次表；保存遥测上传批次、幂等收据、统计数量和持久化生命周期状态。';
COMMENT ON COLUMN hardware_activity.id IS '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。';
COMMENT ON COLUMN hardware_activity.upload_batch_id IS '上传批次 ID；关联产生或上传当前记录的批次；具体目标表取决于存储域。';
COMMENT ON COLUMN hardware_activity.tenant_id IS '租户 ID；用于多租户数据隔离；通常逻辑关联 sys_tenant.id。';
COMMENT ON COLUMN hardware_activity.user_id IS '用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。';
COMMENT ON COLUMN hardware_activity.device_id IS '稳定设备 ID；数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。';
COMMENT ON COLUMN hardware_activity.source_record_id IS '来源记录 ID；上游数据源中的稳定记录标识，通常参与幂等唯一约束。';
COMMENT ON COLUMN hardware_activity.started_at IS '开始时间；会话、活动或信号时间窗开始时间。';
COMMENT ON COLUMN hardware_activity.ended_at IS '结束时间；会话、活动或信号时间窗结束时间。';
COMMENT ON COLUMN hardware_activity.activity_type IS '活动类型；标识活动记录的类型；具体允许值由设备 Provider 映射定义。';
COMMENT ON COLUMN hardware_activity.steps IS '步数；活动时间窗或自然日内的设备步数。';
COMMENT ON COLUMN hardware_activity.distance_meters IS '距离；活动距离，单位米。';
COMMENT ON COLUMN hardware_activity.calories_kcal IS '热量；餐食或活动能量，单位千卡。';
COMMENT ON COLUMN hardware_activity.duration_minutes IS '持续时长；活动持续分钟数。';
COMMENT ON COLUMN hardware_activity.average_heart_rate IS '平均心率；活动或 ECG 测量期间的平均心率。';
COMMENT ON COLUMN hardware_activity.source IS '数据来源；标识设备 Provider、采集通道或业务来源；允许值由对应模块定义。';
COMMENT ON COLUMN hardware_activity.created_at IS '创建时间；记录首次创建时间。';
COMMENT ON COLUMN hardware_data_quality_event.id IS '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。';
COMMENT ON COLUMN hardware_data_quality_event.upload_batch_id IS '上传批次 ID；关联产生或上传当前记录的批次；具体目标表取决于存储域。';
COMMENT ON COLUMN hardware_data_quality_event.tenant_id IS '租户 ID；用于多租户数据隔离；通常逻辑关联 sys_tenant.id。';
COMMENT ON COLUMN hardware_data_quality_event.user_id IS '用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。';
COMMENT ON COLUMN hardware_data_quality_event.device_id IS '稳定设备 ID；数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。';
COMMENT ON COLUMN hardware_data_quality_event.source_record_id IS '来源记录 ID；上游数据源中的稳定记录标识，通常参与幂等唯一约束。';
COMMENT ON COLUMN hardware_data_quality_event.event_type IS '事件类型；标识质量、Outbox、归因或审计事件的业务类型。';
COMMENT ON COLUMN hardware_data_quality_event.severity IS '严重程度；质量事件严重程度，受数据库 CHECK 约束。';
COMMENT ON COLUMN hardware_data_quality_event.detail_code IS '质量详情码；质量事件的稳定细分代码。';
COMMENT ON COLUMN hardware_data_quality_event.event_at IS '事件发生时间；硬件质量事件实际发生时间。';
COMMENT ON COLUMN hardware_data_quality_event.created_at IS '创建时间；记录首次创建时间。';
COMMENT ON COLUMN hardware_diet_record.id IS '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。';
COMMENT ON COLUMN hardware_diet_record.upload_batch_id IS '上传批次 ID；关联产生或上传当前记录的批次；具体目标表取决于存储域。';
COMMENT ON COLUMN hardware_diet_record.tenant_id IS '租户 ID；用于多租户数据隔离；通常逻辑关联 sys_tenant.id。';
COMMENT ON COLUMN hardware_diet_record.user_id IS '用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。';
COMMENT ON COLUMN hardware_diet_record.device_id IS '稳定设备 ID；数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。';
COMMENT ON COLUMN hardware_diet_record.source_record_id IS '来源记录 ID；上游数据源中的稳定记录标识，通常参与幂等唯一约束。';
COMMENT ON COLUMN hardware_diet_record.consumed_at IS '进餐时间；用户实际进餐或记录餐食的时间。';
COMMENT ON COLUMN hardware_diet_record.meal_type IS '餐次类型；餐食所属的早餐、午餐、晚餐或加餐类别。';
COMMENT ON COLUMN hardware_diet_record.description IS '描述；当前记录的业务内容描述。';
COMMENT ON COLUMN hardware_diet_record.calories_kcal IS '热量；餐食或活动能量，单位千卡。';
COMMENT ON COLUMN hardware_diet_record.protein_grams IS '蛋白质；餐食蛋白质估计值，单位克。';
COMMENT ON COLUMN hardware_diet_record.carbohydrate_grams IS '碳水化合物；餐食碳水化合物估计值，单位克。';
COMMENT ON COLUMN hardware_diet_record.fat_grams IS '脂肪；餐食脂肪估计值，单位克。';
COMMENT ON COLUMN hardware_diet_record.fiber_grams IS '膳食纤维；餐食膳食纤维估计值，单位克。';
COMMENT ON COLUMN hardware_diet_record.sodium_milligrams IS '钠；餐食钠估计值，单位毫克。';
COMMENT ON COLUMN hardware_diet_record.source IS '数据来源；标识设备 Provider、采集通道或业务来源；允许值由对应模块定义。';
COMMENT ON COLUMN hardware_diet_record.created_at IS '创建时间；记录首次创建时间。';
COMMENT ON COLUMN hardware_measurement.id IS '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。';
COMMENT ON COLUMN hardware_measurement.upload_batch_id IS '上传批次 ID；关联产生或上传当前记录的批次；具体目标表取决于存储域。';
COMMENT ON COLUMN hardware_measurement.tenant_id IS '租户 ID；用于多租户数据隔离；通常逻辑关联 sys_tenant.id。';
COMMENT ON COLUMN hardware_measurement.user_id IS '用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。';
COMMENT ON COLUMN hardware_measurement.device_id IS '稳定设备 ID；数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。';
COMMENT ON COLUMN hardware_measurement.source_record_id IS '来源记录 ID；上游数据源中的稳定记录标识，通常参与幂等唯一约束。';
COMMENT ON COLUMN hardware_measurement.metric_type IS '指标类型；标识该规范化测量代表的健康指标；允许值由 Provider 映射和遥测契约定义。';
COMMENT ON COLUMN hardware_measurement.observed_at IS '观测时间；硬件标量测量发生时间，使用 TIMESTAMPTZ。';
COMMENT ON COLUMN hardware_measurement.primary_value IS '主测量值；规范化测量的主要数值，例如单值指标或血压收缩压分量。';
COMMENT ON COLUMN hardware_measurement.secondary_value IS '次测量值；规范化测量的可选第二数值，例如成对测量的第二分量。';
COMMENT ON COLUMN hardware_measurement.unit IS '计量单位；说明数值字段采用的计量单位，解释数值时必须同时读取。';
COMMENT ON COLUMN hardware_measurement.quality_code IS '质量代码；规范化的设备或遥测质量代码。';
COMMENT ON COLUMN hardware_measurement.source IS '数据来源；标识设备 Provider、采集通道或业务来源；允许值由对应模块定义。';
COMMENT ON COLUMN hardware_measurement.created_at IS '创建时间；记录首次创建时间。';
COMMENT ON COLUMN hardware_migration_checkpoint.id IS '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。';
COMMENT ON COLUMN hardware_migration_checkpoint.source_name IS '迁移来源名称；旧数据迁移来源系统或表的稳定名称。';
COMMENT ON COLUMN hardware_migration_checkpoint.checkpoint_key IS '迁移检查点键；标识某个迁移分片或范围的稳定检查点。';
COMMENT ON COLUMN hardware_migration_checkpoint.source_position IS '来源位置；已处理到的来源偏移、水位或主键位置。';
COMMENT ON COLUMN hardware_migration_checkpoint.row_count IS '迁移行数；当前迁移检查点覆盖的记录行数。';
COMMENT ON COLUMN hardware_migration_checkpoint.source_hash IS '来源哈希；迁移来源范围的完整性摘要。';
COMMENT ON COLUMN hardware_migration_checkpoint.target_hash IS '目标哈希；迁移目标范围的完整性摘要，用于与来源对账。';
COMMENT ON COLUMN hardware_migration_checkpoint.status IS '状态；状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。';
COMMENT ON COLUMN hardware_migration_checkpoint.checked_at IS '反馈打卡时间；用户对干预行动提交反馈的时间。';
COMMENT ON COLUMN hardware_outbox.id IS '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。';
COMMENT ON COLUMN hardware_outbox.upload_batch_id IS '上传批次 ID；关联产生或上传当前记录的批次；具体目标表取决于存储域。';
COMMENT ON COLUMN hardware_outbox.tenant_id IS '租户 ID；用于多租户数据隔离；通常逻辑关联 sys_tenant.id。';
COMMENT ON COLUMN hardware_outbox.aggregate_type IS '聚合类型；Outbox 事件所属业务聚合类型。';
COMMENT ON COLUMN hardware_outbox.aggregate_id IS '聚合 ID；Outbox 事件所属业务聚合标识。';
COMMENT ON COLUMN hardware_outbox.event_type IS '事件类型；标识质量、Outbox、归因或审计事件的业务类型。';
COMMENT ON COLUMN hardware_outbox.event_version IS '事件版本；同一聚合事件类型的版本号，必须大于零。';
COMMENT ON COLUMN hardware_outbox.status IS '状态；状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。';
COMMENT ON COLUMN hardware_outbox.event_metadata IS '事件元数据；Kafka 发布所需的最小隐私安全元数据，不含原始健康值。';
COMMENT ON COLUMN hardware_outbox.attempt_count IS '尝试次数；对账、Outbox 或迁移任务的累计处理次数。';
COMMENT ON COLUMN hardware_outbox.available_at IS '可处理时间；Outbox 事件允许发布器领取的最早时间。';
COMMENT ON COLUMN hardware_outbox.published_at IS '发布时间；Outbox 事件成功发布到 Kafka 的时间。';
COMMENT ON COLUMN hardware_outbox.last_error_code IS '最近错误码；最近一次失败的脱敏稳定错误码。';
COMMENT ON COLUMN hardware_outbox.created_at IS '创建时间；记录首次创建时间。';
COMMENT ON COLUMN hardware_outbox.updated_at IS '更新时间；记录最后更新时间；部分表用于客户端与服务端新旧副本合并。';
COMMENT ON COLUMN hardware_reconciliation.id IS '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。';
COMMENT ON COLUMN hardware_reconciliation.upload_batch_id IS '上传批次 ID；关联产生或上传当前记录的批次；具体目标表取决于存储域。';
COMMENT ON COLUMN hardware_reconciliation.tenant_id IS '租户 ID；用于多租户数据隔离；通常逻辑关联 sys_tenant.id。';
COMMENT ON COLUMN hardware_reconciliation.state IS 'TODO：字段中文业务含义待确认';
COMMENT ON COLUMN hardware_reconciliation.attempt_count IS '尝试次数；对账、Outbox 或迁移任务的累计处理次数。';
COMMENT ON COLUMN hardware_reconciliation.last_error_code IS '最近错误码；最近一次失败的脱敏稳定错误码。';
COMMENT ON COLUMN hardware_reconciliation.operator_actor_id IS '处理人 ID；对账进入人工处理时的操作者标识。';
COMMENT ON COLUMN hardware_reconciliation.operator_reason IS '处理原因；人工对账或处置的原因说明。';
COMMENT ON COLUMN hardware_reconciliation.created_at IS '创建时间；记录首次创建时间。';
COMMENT ON COLUMN hardware_reconciliation.updated_at IS '更新时间；记录最后更新时间；部分表用于客户端与服务端新旧副本合并。';
COMMENT ON COLUMN hardware_signal_chunk_metadata.id IS '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。';
COMMENT ON COLUMN hardware_signal_chunk_metadata.upload_batch_id IS '上传批次 ID；关联产生或上传当前记录的批次；具体目标表取决于存储域。';
COMMENT ON COLUMN hardware_signal_chunk_metadata.tenant_id IS '租户 ID；用于多租户数据隔离；通常逻辑关联 sys_tenant.id。';
COMMENT ON COLUMN hardware_signal_chunk_metadata.user_id IS '用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。';
COMMENT ON COLUMN hardware_signal_chunk_metadata.device_id IS '稳定设备 ID；数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。';
COMMENT ON COLUMN hardware_signal_chunk_metadata.source_record_id IS '来源记录 ID；上游数据源中的稳定记录标识，通常参与幂等唯一约束。';
COMMENT ON COLUMN hardware_signal_chunk_metadata.signal_type IS '信号类型；标识信号/ECG 分块或元数据的信号类别。';
COMMENT ON COLUMN hardware_signal_chunk_metadata.started_at IS '开始时间；会话、活动或信号时间窗开始时间。';
COMMENT ON COLUMN hardware_signal_chunk_metadata.ended_at IS '结束时间；会话、活动或信号时间窗结束时间。';
COMMENT ON COLUMN hardware_signal_chunk_metadata.sample_rate_hz IS '采样率；信号采样频率，单位 Hz。';
COMMENT ON COLUMN hardware_signal_chunk_metadata.sample_count IS '采样点数；当前信号块包含的样本数量。';
COMMENT ON COLUMN hardware_signal_chunk_metadata.quality_code IS '质量代码；规范化的设备或遥测质量代码。';
COMMENT ON COLUMN hardware_signal_chunk_metadata.created_at IS '创建时间；记录首次创建时间。';
COMMENT ON COLUMN hardware_sleep_session.id IS '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。';
COMMENT ON COLUMN hardware_sleep_session.upload_batch_id IS '上传批次 ID；关联产生或上传当前记录的批次；具体目标表取决于存储域。';
COMMENT ON COLUMN hardware_sleep_session.tenant_id IS '租户 ID；用于多租户数据隔离；通常逻辑关联 sys_tenant.id。';
COMMENT ON COLUMN hardware_sleep_session.user_id IS '用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。';
COMMENT ON COLUMN hardware_sleep_session.device_id IS '稳定设备 ID；数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。';
COMMENT ON COLUMN hardware_sleep_session.source_record_id IS '来源记录 ID；上游数据源中的稳定记录标识，通常参与幂等唯一约束。';
COMMENT ON COLUMN hardware_sleep_session.started_at IS '开始时间；会话、活动或信号时间窗开始时间。';
COMMENT ON COLUMN hardware_sleep_session.ended_at IS '结束时间；会话、活动或信号时间窗结束时间。';
COMMENT ON COLUMN hardware_sleep_session.deep_minutes IS '深睡时长；深睡阶段分钟数。';
COMMENT ON COLUMN hardware_sleep_session.light_minutes IS '浅睡时长；浅睡阶段分钟数。';
COMMENT ON COLUMN hardware_sleep_session.awake_minutes IS '清醒时长；睡眠会话内清醒分钟数。';
COMMENT ON COLUMN hardware_sleep_session.rem_minutes IS 'REM 时长；快速眼动睡眠阶段分钟数。';
COMMENT ON COLUMN hardware_sleep_session.interruption_minutes IS '中断时长；睡眠中断分钟数。';
COMMENT ON COLUMN hardware_sleep_session.source IS '数据来源；标识设备 Provider、采集通道或业务来源；允许值由对应模块定义。';
COMMENT ON COLUMN hardware_sleep_session.created_at IS '创建时间；记录首次创建时间。';
COMMENT ON COLUMN hardware_upload_batch.id IS '主键 ID；当前表记录的唯一标识；生成方式需结合列类型和写入代码判断。';
COMMENT ON COLUMN hardware_upload_batch.receipt_id IS '持久化收据 ID；服务端为已接收批次生成的唯一收据标识。';
COMMENT ON COLUMN hardware_upload_batch.tenant_id IS '租户 ID；用于多租户数据隔离；通常逻辑关联 sys_tenant.id。';
COMMENT ON COLUMN hardware_upload_batch.user_id IS '用户 ID；当前记录所属认证用户的内部标识，通常逻辑关联 sys_user.id。';
COMMENT ON COLUMN hardware_upload_batch.device_id IS '稳定设备 ID；数据所属的稳定设备标识；通过设备绑定或上传批次建立逻辑归属。';
COMMENT ON COLUMN hardware_upload_batch.batch_id IS '客户端批次 ID；客户端生成的稳定遥测批次业务键，重试时保持不变。';
COMMENT ON COLUMN hardware_upload_batch.source IS '数据来源；标识设备 Provider、采集通道或业务来源；允许值由对应模块定义。';
COMMENT ON COLUMN hardware_upload_batch.collected_from IS '采集窗口起点；上传批次覆盖的最早采集时间。';
COMMENT ON COLUMN hardware_upload_batch.collected_to IS '采集窗口终点；上传批次覆盖的最晚采集时间。';
COMMENT ON COLUMN hardware_upload_batch.received_at IS '接收时间；服务端收到上传批次的时间。';
COMMENT ON COLUMN hardware_upload_batch.committed_at IS '持久化完成时间；批次完成约定 durable write 的时间。';
COMMENT ON COLUMN hardware_upload_batch.status IS '状态；状态字段；具体枚举优先以数据库 CHECK、字段注释或业务代码为准。';
COMMENT ON COLUMN hardware_upload_batch.record_count IS '记录总数；批次中全部规范化记录数量。';
COMMENT ON COLUMN hardware_upload_batch.measurement_count IS '测量记录数；批次中的标量测量条数。';
COMMENT ON COLUMN hardware_upload_batch.sleep_session_count IS '睡眠会话数；批次中的睡眠会话条数。';
COMMENT ON COLUMN hardware_upload_batch.activity_count IS '活动记录数；批次中的活动记录条数。';
COMMENT ON COLUMN hardware_upload_batch.signal_metadata_count IS '信号元数据数；批次中的信号元数据条数，不含原始波形。';
COMMENT ON COLUMN hardware_upload_batch.quality_summary IS '质量摘要；批次级结构化质量汇总，不保存原始健康载荷。';
COMMENT ON COLUMN hardware_upload_batch.diet_record_count IS '饮食记录数；批次中的饮食行为条数。';
COMMENT ON INDEX hardware_activity_pkey IS '用于 hardware_activity_pkey 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX hardware_activity_started_at_idx IS '用于 hardware_activity_started_at_idx 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX hardware_data_quality_event_event_at_idx IS '用于 hardware_data_quality_event_event_at_idx 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX hardware_data_quality_event_pkey IS '用于 hardware_data_quality_event_pkey 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX hardware_diet_record_consumed_at_idx IS '用于 hardware_diet_record_consumed_at_idx 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX hardware_diet_record_pkey IS '用于 hardware_diet_record_pkey 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX hardware_measurement_observed_at_idx IS '用于 hardware_measurement_observed_at_idx 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX hardware_measurement_pkey IS '用于 hardware_measurement_pkey 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX hardware_migration_checkpoint_pkey IS '用于 hardware_migration_checkpoint_pkey 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX hardware_outbox_pkey IS '用于 hardware_outbox_pkey 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX hardware_reconciliation_pkey IS '用于 hardware_reconciliation_pkey 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX hardware_reconciliation_upload_batch_id_key IS '用于 hardware_reconciliation_upload_batch_id_key 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX hardware_signal_chunk_metadata_pkey IS '用于 hardware_signal_chunk_metadata_pkey 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX hardware_sleep_session_pkey IS '用于 hardware_sleep_session_pkey 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX hardware_sleep_session_started_at_idx IS '用于 hardware_sleep_session_started_at_idx 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX hardware_upload_batch_pkey IS '用于 hardware_upload_batch_pkey 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX hardware_upload_batch_receipt_id_key IS '用于 hardware_upload_batch_receipt_id_key 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX idx_hardware_activity_scoped_recent IS '用于 idx_hardware_activity_scoped_recent 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX idx_hardware_batch_device_time IS '用于 idx_hardware_batch_device_time 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX idx_hardware_batch_user_time IS '用于 idx_hardware_batch_user_time 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX idx_hardware_diet_scoped_recent IS '用于 idx_hardware_diet_scoped_recent 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX idx_hardware_measurement_metric_time IS '用于 idx_hardware_measurement_metric_time 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX idx_hardware_measurement_scoped_recent IS '用于 idx_hardware_measurement_scoped_recent 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX idx_hardware_outbox_dispatch IS '用于 idx_hardware_outbox_dispatch 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX idx_hardware_outbox_published_retention IS '用于 idx_hardware_outbox_published_retention 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX idx_hardware_quality_scoped_recent IS '用于 idx_hardware_quality_scoped_recent 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX idx_hardware_reconciliation_state_time IS '用于 idx_hardware_reconciliation_state_time 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX idx_hardware_signal_metadata_retention IS '用于 idx_hardware_signal_metadata_retention 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX idx_hardware_sleep_scoped_recent IS '用于 idx_hardware_sleep_scoped_recent 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX uq_hardware_activity_source IS '用于 uq_hardware_activity_source 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX uq_hardware_batch_source IS '用于 uq_hardware_batch_source 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX uq_hardware_diet_source IS '用于 uq_hardware_diet_source 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX uq_hardware_measurement_source IS '用于 uq_hardware_measurement_source 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX uq_hardware_migration_checkpoint IS '用于 uq_hardware_migration_checkpoint 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX uq_hardware_outbox_event IS '用于 uq_hardware_outbox_event 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX uq_hardware_quality_source IS '用于 uq_hardware_quality_source 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX uq_hardware_signal_metadata_source IS '用于 uq_hardware_signal_metadata_source 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON INDEX uq_hardware_sleep_source IS '用于 uq_hardware_sleep_source 对应业务查询或生命周期清理的索引；具体列见索引定义。';
COMMENT ON PROCEDURE rehealth_apply_ordinary_retention(integer, jsonb) IS '按配置清理普通硬件运营表与已发布 Outbox 历史数据。';
