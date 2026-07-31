ALTER TABLE hardware_upload_batch
    ADD COLUMN diet_record_count integer NOT NULL DEFAULT 0
        CHECK (diet_record_count >= 0);

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
    drop_after => INTERVAL '${measurementRetentionDays} days',
    if_not_exists => TRUE
);
