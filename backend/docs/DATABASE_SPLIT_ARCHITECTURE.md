# ReHealth Backend Database Split Architecture

Review: E0.5_backend_module_selection_and_database_split  
Date: 2026-07-09  
Scope: software database and hardware telemetry database split before E1.

Low-code management pages and handwritten execution services must also follow
[`REHEALTH_LOW_CODE_BOUNDARY.md`](./REHEALTH_LOW_CODE_BOUNDARY.md).

## Decision

ReHealth backend needs two database ownership domains:

1. `software_db`: user/business/application records.
2. `hardware_db`: high-concurrency wearable/ring telemetry and ingestion records.

Android never writes directly to either database. Android persists locally first, then uploads through backend APIs. Model-service never owns JeecgBoot databases.

## Database Responsibilities

| Database | Owns | Does not own |
| --- | --- | --- |
| `software_db` | Users, roles, permissions, device binding, user profile, health interview, CVD feature vectors, risk results, intervention plans, intervention feedback, upload status, admin/business records, model request metadata. | Raw high-frequency telemetry samples, PPG/RRI chunks, raw upload payloads beyond operational logs, time-series retention. |
| `hardware_db` | Wearable telemetry batches, heart rate, blood oxygen, blood pressure, temperature, sleep, steps/activity, HRV, RRI/PPG metadata if legally/clinically allowed, raw upload batch logs, data quality flags, ingestion events, idempotency records. | User account authority, roles/permissions, intervention plans, model risk outputs, admin/doctor workflows. |

## `software_db` Design

| Dimension | Design |
| --- | --- |
| Owner service | Jeecg system service owns system tables. ReHealth mobile/risk/admin services own `rehealth_*` business tables. |
| Table categories | System user/role/permission, ReHealth patient profile, device binding, health interview, CVD feature vector, risk result, intervention plan, intervention feedback, model request log, upload batch status, admin/business workflow records. |
| Read/write pattern | Moderate write volume; frequent reads for latest profile/risk/intervention; admin queries by user/device/date; transactional updates within one business aggregate. |
| Expected concurrency | Low to medium for MVP. Higher for login/latest-risk reads, but far below telemetry ingestion volume. |
| Retention strategy | Account/profile/binding retained while account is active and per compliance policy. Feature vectors/risk/intervention/feedback retained long enough for longitudinal trend and audit. Model request metadata retained with redaction and payload minimization. |
| Transaction consistency | Strong consistency inside software aggregates, for example profile update, feature vector plus risk result, intervention plus feedback. Avoid cross-database transactions with hardware telemetry. |
| Storage fit | Relational database. JeecgBoot default MySQL-compatible path is acceptable. PostgreSQL is also supported by dependencies, but JeecgBoot scripts/defaults are MySQL-oriented. |
| MVP implementation choice | MySQL database/schema, using JeecgBoot dynamic datasource primary `software` or existing `master` mapped to software only. |
| Production implementation choice | MySQL/PostgreSQL/TiDB depending deployment standards. Use read replicas for admin/report reads if needed. Keep PHI/PII encryption, audit, backup, and access controls explicit. |

Suggested `software_db` tables:

| Category | Tables |
| --- | --- |
| Profile/interview | `rehealth_patient_profile`, `rehealth_health_interview` |
| Device/business | `rehealth_device_binding`, `rehealth_upload_batch` |
| Features/risk | `rehealth_cvd_feature_vector`, `rehealth_cvd_risk_result`, `rehealth_model_request_log` |
| Intervention/feedback | `rehealth_intervention_plan`, `rehealth_intervention_feedback`, `rehealth_attribution_event` |
| Admin/business | Future `rehealth_admin_case`, `rehealth_doctor_note`, `rehealth_operation_record` as needed |

## `hardware_db` Design

| Dimension | Design |
| --- | --- |
| Owner service | ReHealth ingest service/module. Other services read summaries through service APIs or materialized software records, not ad hoc joins. |
| Table categories | Raw upload batch logs, normalized measurement rows, sleep sessions, activity/steps sessions, HRV rows, RRI/PPG metadata/chunks if allowed, device telemetry quality flags, ingestion events, idempotency keys. |
| Read/write pattern | High write volume, append-heavy, batch ingestion. Reads mostly by user/device/time range for summarization, latest-cache refresh, QA, replay, and feature extraction. |
| Expected concurrency | Medium in MVP, high in pilot/production because every wearable can upload batches repeatedly. Bursty uploads after offline periods are expected. |
| Retention strategy | Raw high-volume data has tiered retention. Keep recent detailed telemetry hot, roll up older data into summaries, archive or delete raw chunks per consent/compliance. Raw PPG/RRI should have stricter retention and explicit allowance. |
| Transaction consistency | Idempotent batch acceptance is required. Strong consistency per batch write is useful. Cross-database consistency with `software_db` should be eventual, using status/events/retry instead of distributed transactions. |
| Storage fit | Append-heavy time-series or analytical storage fits production. Relational MySQL can work for MVP with batch tables, indexes, and retention limits. |
| MVP implementation choice | Separate MySQL database/schema `rehealth_hardware` with normalized batch/measurement/session tables and strict payload size/idempotency limits. |
| Production implementation choice | Time-series DB or ClickHouse for high-volume telemetry; PostgreSQL/TimescaleDB is another option if relational/time-series hybrid is preferred. Keep MySQL/PostgreSQL for ingestion metadata if analytical store is separate. |

Suggested `hardware_db` tables:

| Category | Tables |
| --- | --- |
| Device/ingestion | `rehealth_hw_device`, `rehealth_hw_measurement_batch`, `rehealth_hw_ingestion_event` |
| Measurements | `rehealth_hw_measurement`, optionally partitioned by date/type |
| Sleep/activity | `rehealth_hw_sleep_session`, `rehealth_hw_activity_session`, `rehealth_hw_step_summary` |
| Signals | `rehealth_hw_hrv`, `rehealth_hw_rri_metadata`, `rehealth_hw_ppg_chunk` if allowed |
| Quality | `rehealth_hw_quality_flag`, `rehealth_hw_batch_rejection` |

High-volume table requirements:

- Internal `user_id`/patient reference, never phone number.
- `device_id`.
- Client `batch_id` or idempotency key.
- `measured_at` or `started_at`/`ended_at`.
- `received_at`.
- Metric type, unit, source, quality/status.
- Unique constraint on `(user_id, device_id, batch_id)` or an equivalent idempotency key.
- Indexes on `(user_id, measured_at)`, `(device_id, measured_at)`, and `(batch_id)`.
- Partitioning/TTL strategy before pilot-scale data.

## JeecgBoot Datasource Strategy

JeecgBoot base core already includes `dynamic-datasource-spring-boot3-starter`. E1 should use explicit datasource names instead of overloading the default `master` database with hardware telemetry.

Recommended naming:

```yaml
spring:
  datasource:
    dynamic:
      primary: software
      datasource:
        software:
          url: jdbc:mysql://.../rehealth_software
        hardware:
          url: jdbc:mysql://.../rehealth_hardware
```

Local MVP option:

- Map `software` to the existing Jeecg database if a separate `rehealth_software` database is not available.
- Still create `hardware` as a separate database/schema for telemetry.
- If a second datasource is unavailable, E1 must document the limitation and must not pretend hardware telemetry is production-ready.

Mapper/service convention:

- Software/business mappers use primary datasource or `@DS("software")`.
- Hardware mappers use `@DS("hardware")`.
- No cross-database transactions.
- If both domains must be touched, write accepted telemetry first, then write a software status/event. Retry the second write asynchronously if needed.

## Data Flow

### Android Measurement Upload Flow

```text
MRD ring/BLE
  -> Android repository/service
  -> Room local persistence
  -> Android upload queue
  -> POST /rehealth/mobile/telemetry/batches
  -> backend auth/device ownership validation
  -> idempotency check
  -> durable batch write
  -> accepted/rejected/retryable response
```

Rules:

- Android uploads batches, not one HTTP request per sample.
- Android collection must not block on backend/model-service availability.
- Backend must accept and persist valid telemetry before triggering model evaluation.
- Backend must not log raw telemetry payloads, tokens, phone numbers, or identifiers.

### Hardware Data Ingestion Flow

E1 MVP synchronous path:

```text
Mobile API
  -> ReHealthIngestService.validate()
  -> hardware_db.batch + hardware_db.measurements/sessions
  -> software_db.upload_batch status
  -> response to Android
```

High-concurrency path:

```text
Gateway
  -> rehealth-ingest-service
  -> small ingestion receipt/status write
  -> MQ topic/queue
  -> telemetry consumer workers
  -> hardware_db normalized writes
  -> dead-letter/retry on failure
  -> software_db upload status/materialized summaries
```

MQ recommendation:

- Do not add MQ in E1 unless explicitly required.
- Choose exactly one MQ for production after ops review: RabbitMQ for simpler reliable queues, RocketMQ for higher-throughput ordered/event-stream workloads.
- Do not enable RabbitMQ and RocketMQ simultaneously for the MVP.

### Feature Extraction And Risk Evaluation Flow

Preferred MVP flow:

```text
Android CVD feature extractor
  -> POST /rehealth/mobile/features/evaluate
  -> backend validates CVD 16 + featureQuality
  -> software_db.rehealth_cvd_feature_vector
  -> ModelServiceClient POST /v1/cvd/risk/evaluate
  -> software_db.rehealth_cvd_risk_result
  -> ModelServiceClient POST /v1/cvd/intervention/generate
  -> software_db.rehealth_intervention_plan
  -> response to Android
```

Backend may also compute server-side summaries from `hardware_db` later, but E1 should honor the Android C1 feature extractor contract and model-service API contract.

### Latest Data Cache Flow

Purpose: keep mobile latest screens fast without scanning raw telemetry.

MVP option:

```text
accepted telemetry batch
  -> update latest measurement summary record/cache
  -> GET /rehealth/mobile/risk/latest reads software_db latest risk
  -> GET /rehealth/mobile/interventions/today reads software_db current plan
```

Production option:

```text
hardware_db append
  -> stream/worker computes per-user latest summaries
  -> Redis latest cache + software_db materialized summary
  -> mobile reads latest endpoints
```

Cache rules:

- Cache contains latest derived summaries, not raw PPG/RRI payloads.
- Cache misses fall back to persisted latest records.
- Cache invalidation follows accepted batch time ranges and risk/intervention generation events.

### Model-Service Call Flow

Health check:

```text
backend startup/periodic check
  -> GET model-service /health
  -> expose status summary in /rehealth/mobile/config
```

Risk:

```text
backend
  -> POST model-service /v1/cvd/risk/evaluate
  -> persist risk_score, risk_level, contributions, model_version, is_mock, missing_fields, quality_warnings, summary
```

Intervention:

```text
backend
  -> POST model-service /v1/cvd/intervention/generate
  -> persist plan_id, generated_at, action, rationale, contraindications, confidence, model_version, is_mock, disclaimer
```

Attribution later:

```text
backend
  -> build event history from risk/intervention/feedback records
  -> POST model-service /v1/cvd/attribution/individual
  -> persist or return attribution summary
```

Failure behavior:

- Model-service unavailable must not reject already accepted telemetry.
- Feature/risk evaluation can return retryable service error after feature vector persistence.
- No Java clinical scoring fallback unless explicitly named dev-only and excluded from production behavior.

## Service Ownership Matrix

| Service/module | Writes | Reads | Notes |
| --- | --- | --- | --- |
| Jeecg system service | `software_db` system tables | System/user/role/permission | Existing ownership remains. |
| `rehealth-mobile-service` | Profile, device binding, upload status, feedback | Latest risk/intervention/profile/config | Android-facing API. |
| `rehealth-ingest-service` | `hardware_db` telemetry and ingestion records | Device binding via service/software lookup | Must stay independent from model-service availability. |
| `rehealth-risk-orchestration-service` | Feature vectors, risk results, intervention plans | Feature vectors, latest profile, model status | Calls Python model-service. |
| `rehealth-admin-service` | Admin/business records | Software summaries, not raw telemetry by default | Later. |
| Python `model-service` | No Jeecg DB writes | None direct | Receives HTTP requests only. |

## Consistency Boundaries

Use strong transactions for:

- Device binding updates in `software_db`.
- Profile/interview updates in `software_db`.
- Single telemetry batch write in `hardware_db`.
- Feature vector plus risk result in `software_db` when both are produced in one workflow.
- Intervention feedback write in `software_db`.

Use eventual consistency for:

- Telemetry acceptance in `hardware_db` plus upload status in `software_db`.
- Telemetry summary/cache updates.
- MQ consumer writes.
- Model-service availability and retry.
- Admin/report materializations.

Avoid:

- Seata/distributed transactions in E1.
- Cross-database joins in application queries.
- Java-side model inference.
- Storing raw health payloads in logs.

## Revised E1 Database Scope

E1 should implement now, if approved:

- `software_db` MVP tables for device binding, profile/interview, feature vector, risk result, intervention plan, feedback, upload batch status.
- `hardware_db` MVP tables for measurement batch, normalized measurement rows, sleep/activity sessions, and ingestion event/rejection records.
- Idempotency behavior for telemetry batches.
- Clear datasource annotations or service boundaries.
- Schema docs and migrations if database schema is changed.

E1 should move to E2:

- MQ write/consumer path.
- ClickHouse/time-series database migration.
- Raw PPG/RRI storage beyond metadata unless explicitly allowed.
- Partitioning/sharding implementation.
- Retention purge jobs.
- Admin analytics/reporting.

## D1 Readiness Gate

Android D1 can integrate safely after E1 provides:

- Stable `/rehealth/mobile/**` endpoint paths.
- Request/response DTOs for telemetry upload, feature evaluation, latest risk/intervention, and feedback.
- Upload batch idempotency key behavior.
- Accepted/rejected/retryable error shape.
- Confirmation of whether telemetry is persisted to `hardware_db` or an exact documented dev limitation.
- Confirmed mapping to `model-service/docs/API_CONTRACT.md`.

Until then, Android should not hardcode backend behavior beyond the CVD feature/model contract.
