CREATE EXTENSION IF NOT EXISTS timescaledb;

DO $$
DECLARE
    installed_version integer[];
BEGIN
    IF current_setting('server_version_num')::integer < 170000 THEN
        RAISE EXCEPTION 'ReHealth hardware_db requires PostgreSQL 17 or newer';
    END IF;

    SELECT string_to_array(regexp_replace(extversion, '[^0-9.].*$', ''), '.')::integer[]
    INTO installed_version
    FROM pg_extension
    WHERE extname = 'timescaledb';

    IF installed_version IS NULL OR installed_version < ARRAY[2, 18, 0] THEN
        RAISE EXCEPTION 'ReHealth hardware_db requires TimescaleDB 2.18 or newer';
    END IF;
END
$$;

CREATE OR REPLACE FUNCTION rehealth_legacy_mysql_datetime_utc(
    legacy_value timestamp without time zone
) RETURNS timestamp with time zone
LANGUAGE sql
IMMUTABLE
STRICT
RETURN legacy_value AT TIME ZONE 'UTC';
