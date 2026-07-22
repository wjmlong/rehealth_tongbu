# ReHealth Hardware DB Schema E2.1

Date: 2026-07-13
Status: MySQL MVP migration implemented

## Ownership

The ReHealth hardware ingest boundary exclusively owns the separate dynamic
datasource named `hardware`. The Jeecg default `master` datasource remains the
software database. Application code must not cross-join the two databases or
open a distributed transaction.

Migration:
`jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/resources/db/hardware/mysql/V1__create_hardware_telemetry_tables.sql`

JeecgBoot excludes Flyway auto-configuration, so V1 is applied explicitly by
the deployment runbook. It is add-only: six new tables, indexes, unique keys,
and foreign keys; no existing table is altered or dropped.

## Tables

### `hardware_upload_batch`

One durable receipt per authenticated owner, device, and Android batch.

Key columns: `id`, `receipt_id`, `batch_id`, `user_id`, `device_id`, source and
collection timestamps, committed status, row counts, and non-raw `quality_json`.

Constraints:

- Primary key `id`.
- Unique `(user_id, device_id, batch_id)` for retry idempotency.
- Unique `receipt_id`.
- User/device collection-time indexes.

### `hardware_measurement`

Normalized scalar telemetry such as HR, SpO2, BP, temperature, steps, and HRV.
It stores client record ID, metric type/time, primary and optional secondary
numeric value, unit, quality code, and source. It does not store raw payloads.

### `hardware_sleep_session`

Normalized sleep windows with start/end time and deep, light, awake, REM, and
interruption minutes.

### `hardware_activity`

Normalized activity windows with type, steps, distance, calories, duration, and
optional average heart rate.

### `hardware_signal_chunk_metadata`

Reserved metadata-only table for a future approved raw signal workflow. E2.1
does not write it. It has no payload/body column; `payload_ref` would point to an
approved external encrypted object store and requires an expiry timestamp.

### `hardware_data_quality_event`

Reserved for non-payload quality/audit events. E2.1 stores accepted batch
quality metadata in `hardware_upload_batch.quality_json`; future consumers may
materialize operational events here without copying raw health payloads.

## Transaction Boundary

A new batch row and all measurement/sleep/activity rows commit in one local
hardware transaction. Any child insert failure rolls the entire batch back.
Only after that transaction returns successfully may the API report
`persisted=true`.

Duplicate retries read the already committed receipt and do not touch child
tables. Concurrent retries are serialized by the unique database constraint.

## Identity Boundary

`user_id` is the current authenticated Jeecg `LoginUser.id`, not a client-owned
identifier. Android's `userId` JSON field remains for wire compatibility but is
overwritten before validation and persistence. The database contains internal
IDs only, not phone numbers or names.

## Recent Telemetry Query

`GET /rehealth/mobile/measurements/recent?limit=50` reads normalized rows from `hardware_measurement`, `hardware_sleep_session`, and `hardware_activity`. Every query is filtered by the authenticated Jeecg user ID, ordered newest first, and limited to 1–200 rows per category. The response excludes raw signal chunks and payload references. A disabled hardware datasource returns a controlled `503` response.

## Retention

| Data | MVP policy | Implementation status |
| --- | --- | --- |
| Normal telemetry | 30 days hot | Configured policy; purge/rollup job pending. |
| Upload receipts | 180 days | Policy pending operational job. |
| Raw signal payload | 0 days / disabled | Rejected; no payload column in V1. |
| Signal metadata | 0 days unless approved | Table reserved, writer disabled. |
| Quality events | 180 days | Table reserved; purge job pending. |

Production migration may move high-volume rows to ClickHouse/TimescaleDB or
partitioned MySQL after measured load tests. The Android batch ID and backend
idempotency contract must remain stable.

## PIAS Use

This schema stores telemetry facts, not model conclusions. Risk results,
interventions, feedback, individual attribution, group jobs, and settlement
evidence belong to `software_db` in later E1.1/E1.2 work. model-service has no
direct database access, and patient clients must not supply authoritative risk
history or settlement parameters.
