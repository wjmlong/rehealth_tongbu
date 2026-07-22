# ReHealth DB Schema E1

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
| `rehealth_model_request_log` | Minimal request metadata without raw PII or raw telemetry payloads. | Schema ready; audit writer pending. |
| `rehealth_upload_batch` | Software-side upload status/materialized summary. | Deferred. |

Transaction strategy:

- Strong consistency only inside one software aggregate.
- No cross-database transaction with `hardware_db`.
- Do not log raw health data, tokens, phone numbers, or identifiers.

## hardware_db Boundary

Owner: future `rehealth-ingest-service`/hardware ingestion module.

E1 Java boundary:

```text
org.jeecg.modules.rehealth.ingest.HardwareIngestionPort
org.jeecg.modules.rehealth.ingest.impl.E2PendingHardwareIngestionPort
```

Planned tables:

| Table | Purpose | E1 status |
| --- | --- | --- |
| `rehealth_hw_measurement_batch` | Idempotent raw upload batch/receipt. | Deferred to E2. |
| `rehealth_hw_measurement` | HR, SpO2, BP, temperature, and related measurement rows. | Deferred to E2. |
| `rehealth_hw_sleep_session` | Sleep session details. | Deferred to E2. |
| `rehealth_hw_activity_session` | Steps/activity sessions. | Deferred to E2. |
| `rehealth_hw_hrv` | HRV values. | Deferred to E2. |
| `rehealth_hw_rri_metadata` | RRI metadata if allowed. | Deferred to E2. |
| `rehealth_hw_ppg_chunk` | PPG metadata/chunks if allowed. | Deferred to E2 and consent review. |
| `rehealth_hw_quality_flag` | Data quality flags. | Deferred to E2. |
| `rehealth_hw_ingestion_event` | Ingestion state, rejection, retry, dead-letter metadata. | Deferred to E2. |

When `rehealth.hardware-db.enabled=true`, `/measurements/batch` writes a transactionally idempotent batch through `JdbcHardwareTelemetryWriter`. When disabled, it returns 503 so Android keeps the batch queued.

## Provisioning

Apply `db/software/mysql/V1__create_rehealth_software_tables.sql` to the Jeecg primary software datasource, then set `rehealth.software-db.enabled=true`. Every mobile business write/read derives ownership from the authenticated Jeecg user; client-supplied user IDs are not accepted for these records.

## E2 Migration Requirements

E2 must add:

- Real `hardware_db` datasource configuration or external time-series/ClickHouse choice.
- Idempotency key constraints for batches.
- Batch writer and validation rules.
- MQ or stream transport if required by concurrency target.
- Retention policy for raw telemetry, especially PPG/RRI.
- Migrations or explicit schema deployment scripts.
