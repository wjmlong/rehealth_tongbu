# ReHealth DB Schema E1

Status: E1 schema boundary document. No database migrations are added in E1.

## Decision

ReHealth uses two logical databases:

- `software_db`: user/business/application records.
- `hardware_db`: high-volume wearable telemetry and ingestion records.

E1 creates Java interfaces and DTO boundaries only. E2 owns physical hardware ingestion tables, MQ, and high-concurrency writer implementation.

## software_db Boundary

Owner: ReHealth mobile/risk/admin business code plus existing Jeecg system account/auth tables.

E1 Java boundary:

```text
org.jeecg.modules.rehealth.repository.ReHealthBusinessRepository
org.jeecg.modules.rehealth.repository.impl.E1PendingSoftwareDbReHealthBusinessRepository
```

Planned tables:

| Table | Purpose | E1 status |
| --- | --- | --- |
| `rehealth_device_binding` | User-to-device binding. | Interface only. |
| `rehealth_patient_profile` | ReHealth profile reference. | Deferred. |
| `rehealth_health_interview` | Health interview/business profile fields. | Deferred. |
| `rehealth_cvd_feature_vector` | CVD 16 vector and feature quality metadata. | Interface only via `/features/evaluate`. |
| `rehealth_cvd_risk_result` | Risk score, level, contributions, model version, missing fields, warnings, summary. | Interface only. |
| `rehealth_intervention_plan` | Conservative model-service intervention response. | Interface only. |
| `rehealth_intervention_feedback` | User feedback/adherence/check-in. | Interface only via `/interventions/{id}/feedback`. |
| `rehealth_model_request_log` | Minimal request metadata without raw PII or raw telemetry payloads. | Deferred. |
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

E1 `/measurements/batch` returns:

- `accepted=false`
- `persisted=false`
- `status=INGEST_INTERFACE_READY_E2_PENDING`

This is intentional so Android D1 does not mistake API reachability for durable hardware ingestion.

## E2 Migration Requirements

E2 must add:

- Real `hardware_db` datasource configuration or external time-series/ClickHouse choice.
- Idempotency key constraints for batches.
- Batch writer and validation rules.
- MQ or stream transport if required by concurrency target.
- Retention policy for raw telemetry, especially PPG/RRI.
- Migrations or explicit schema deployment scripts.
