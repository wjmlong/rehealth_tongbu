# ReHealth DB Schema

Status: software_db and hardware_db MVP schema scripts are implemented; production provisioning remains deployment-owned.

## Decision

ReHealth uses two logical databases:

- `software_db`: user/business/application records.
- `hardware_db`: high-volume wearable telemetry and ingestion records.

The ReHealth module provides conditional JDBC writers for both database domains. They stay disabled until their schemas and datasource settings are provisioned.

## software_db Boundary

Owner: ReHealth mobile/risk/admin business code plus existing Jeecg system account/auth tables.

E1 Java boundary:

```text
org.jeecg.modules.rehealth.repository.ReHealthBusinessRepository
org.jeecg.modules.rehealth.repository.impl.E1PendingSoftwareDbReHealthBusinessRepository
org.jeecg.modules.rehealth.repository.impl.JdbcSoftwareDbReHealthBusinessRepository
```

Planned tables:

| Table | Purpose | E1 status |
| --- | --- | --- |
| `rehealth_device_binding` | User-to-device binding. | Implemented. |
| `rehealth_patient_profile` | ReHealth profile reference. | Implemented via authenticated `GET/PUT /profile`. |
| `rehealth_health_interview` | Health interview/business profile fields. | Implemented via authenticated `POST /interviews` and `GET /interviews/latest`. |
| `rehealth_cvd_feature_vector` | CVD 16 vector and feature quality metadata. | Implemented via `/features/evaluate`. |
| `rehealth_cvd_risk_result` | Risk score, level, contributions, model version, missing fields, warnings, summary. | Implemented with per-user latest read. |
| `rehealth_intervention_plan` | Conservative model-service intervention response. | Implemented with per-user latest read. |
| `rehealth_intervention_feedback` | User feedback/adherence/check-in. | Implemented via `/interventions/{id}/feedback`. |
| `rehealth_attribution_result` | PIAS request and result snapshot. | Implemented via `/attribution/events`. |
| `rehealth_model_request_log` | Minimal request metadata without raw PII or raw telemetry payloads. | Implemented for risk, intervention, and attribution model calls. |
| `rehealth_upload_batch` | Software-side upload status/materialized summary. | Deferred. |

Transaction strategy:

- Strong consistency only inside one software aggregate.
- No cross-database transaction with `hardware_db`.
- Do not log raw health data, tokens, phone numbers, or identifiers.

## hardware_db Boundary

Owner: `backend/device-service`, with a future dedicated ingest service remaining an optional scaling step.

The PostgreSQL 17 / TimescaleDB schema is versioned by Flyway under
`backend/device-service/src/main/resources/db/migration/timescale`. Todo 8 owns
the telemetry-port adapter; enabling migrations does not by itself make the
currently unavailable adapter ready.

| Table | Purpose |
| --- | --- |
| `hardware_upload_batch` | Idempotent upload receipt and batch state. |
| `hardware_measurement` | Normalized scalar measurements; one-day `observed_at` chunks. |
| `hardware_sleep_session` | Normalized sleep sessions; seven-day `started_at` chunks. |
| `hardware_activity` | Normalized activity sessions; seven-day `started_at` chunks. |
| `hardware_signal_chunk_metadata` | Signal metadata only; no raw waveform payload. |
| `hardware_data_quality_event` | Quality events; seven-day `event_at` chunks. |
| `hardware_reconciliation` | Reconciliation state and retry metadata. |
| `hardware_outbox` | Durable publication state. |
| `hardware_migration_checkpoint` | Legacy migration checkpoints. |

All domain times are `TIMESTAMPTZ`. Source uniqueness includes tenant, user,
device, event time, record type, and source record ID. Telemetry/session/quality
hypertables compress chunks after seven days.

| Data class | Default retention | Configuration |
| --- | --- | --- |
| Measurement, sleep, activity | 730 days | `REHEALTH_MEASUREMENT_RETENTION_DAYS` |
| Signal metadata | 90 days | `REHEALTH_SIGNAL_METADATA_RETENTION_DAYS` |
| Quality and operational history | 1,095 days | `REHEALTH_OPERATIONAL_RETENTION_DAYS` |
| Published outbox rows | 30 days | `REHEALTH_PUBLISHED_OUTBOX_RETENTION_DAYS` |

The ordinary-table lifecycle job removes only terminal data. Failed or
unresolved outbox records are never automatically deleted, and upload batches
with unresolved reconciliation or outbox work are retained.

## Provisioning

Apply `db/software/mysql/V1__create_rehealth_software_tables.sql` to the Jeecg primary software datasource, then set `rehealth.software-db.enabled=true`. Every mobile business write/read derives ownership from the authenticated Jeecg user; client-supplied user IDs are not accepted for these records.

For `hardware_db`, set `REHEALTH_HARDWARE_DB_ENABLED=true`,
`REHEALTH_HARDWARE_DB_URL`, `REHEALTH_HARDWARE_DB_USERNAME`, and either
`REHEALTH_HARDWARE_DB_PASSWORD` or `REHEALTH_HARDWARE_DB_PASSWORD_FILE`.
Startup validates and applies the Timescale Flyway migrations before any
hardware write adapter is created. PostgreSQL without the Timescale extension
fails in the prerequisite migration before application tables are written.

Legacy MySQL `DATETIME(3)` values are interpreted as UTC by
`rehealth_legacy_mysql_datetime_utc(timestamp)` before conversion to
`TIMESTAMPTZ`; migration callers must not apply the server session timezone.
