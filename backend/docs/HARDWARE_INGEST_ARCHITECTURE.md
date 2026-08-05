# ReHealth Hardware Ingest Architecture

Date: 2026-07-31
Current module: `device-service`

## Current Device Service Flow

```text
authenticated Android batch (telemetry-v2)
  -> Gateway telemetry authority route
  -> Device Service identity/device authorization
  -> shared TelemetryContractValidator
  -> one TimescaleDB transaction
       -> upload receipt + measurement + sleep + activity + diet
       -> reconciliation + Transactional Outbox
  -> durable ACCEPTED_PERSISTED / idempotent ACCEPTED_DUPLICATE
```

`dietRecords` is optional for backward compatibility. Each record requires a
stable ID, `consumedAt`, one of `breakfast|lunch|dinner|snack`, and a bounded
description; present nutrient values must be finite and non-negative. Raw meal
images are outside this contract. Android must persist a captured meal locally
before it enters the retryable telemetry batch.

Personalized intervention generation is a separate read path and never runs
inside ingestion. Jeecg calls the credential-protected Device Service
`/rehealth/internal/v1/operations/users/{userId}/intervention-context` endpoint,
which reads only the authenticated tenant/user's current natural day and
bounded recent trends. Upload success therefore does not depend on the LLM,
software_db, or intervention availability.

## Legacy Jeecg/MySQL E2.1 Reference

## Scope

E2.1 replaces the dev-only in-memory acceptance path behind
`POST /rehealth/mobile/measurements/batch` with a synchronous, transactional
MVP write to the separate `hardware` datasource/schema.

This path does not write telemetry to `software_db`, call model-service, run
CatBoost/SHAP, or perform intervention, attribution, or settlement work.

## Runtime Flow

```text
authenticated Android request
  -> ReHealthMobileController replaces body userId with LoginUser.id
  -> HardwareTelemetryIngestionService
  -> TelemetryBatchValidator
       -> reject empty/oversized/raw-signal input
  -> JdbcHardwareTelemetryWriter
       -> resolve physical dynamic datasource named hardware
       -> one hardware-local transaction
       -> idempotency lookup/unique constraint
       -> batch + measurements + sleep + activity
       -> commit
  -> ACCEPTED_PERSISTED or ACCEPTED_DUPLICATE
```

`accepted=true` and `persisted=true` are returned only after the hardware-local
transaction commits, or when a previously committed batch is found.

## Ownership And Isolation

- The JSON `userId` field remains for Android D2 DTO compatibility.
- The controller overwrites it with the current Jeecg `LoginUser.id`.
- Idempotency is scoped by `(authenticated user_id, device_id, batch_id)`.
- A client cannot select another user's hardware rows by changing `userId`.
- The writer obtains the physical datasource from
  `DynamicRoutingDataSource.getDataSources().get("hardware")`; it does not use
  the default `master` route.
- The transaction manager is created for that physical datasource only. There
  is no software/hardware distributed transaction.

Device ownership validation against a durable `software_db` binding remains an
E1.1 dependency. Until that exists, authentication protects user ownership but
does not prove that the submitted `deviceId` is bound to that user.

## Idempotency

`hardware_upload_batch` has a unique key on
`(user_id, device_id, batch_id)`. A normal retry returns the original receipt.
Concurrent insert races are resolved by the same database constraint and a
post-rollback lookup. Child rows are not inserted again.

Statuses:

| Status | Meaning |
| --- | --- |
| `ACCEPTED_PERSISTED` | New batch and normalized rows committed. |
| `ACCEPTED_DUPLICATE` | The same owner/device/batch was already committed. |
| `REJECTED_INVALID` | Validation rejected the request before persistence. |

When the datasource is disabled, missing, or a transaction fails, the endpoint
returns a failed Jeecg envelope with `code=503`. Android must retain the local
batch and retry with the same `batchId`.

## Raw Signal Policy

Raw signal chunks, `payload_base64`, raw payload fields, PPG/RRI/waveform keys,
and nested equivalents are rejected by default. The V1 schema contains only a
future metadata table and no raw payload column. Enabling raw signal upload
still requires separate consent, retention, encryption, and object-storage
approval; E2.1 does not implement that path.

## PIAS Boundary

Hardware telemetry is an authenticated fact source for later backend
orchestration. Patient clients do not submit risk history for attribution and
do not call group attribution or settlement. A later E1.1/E1.2 backend flow
must build individual attribution inputs from persisted risk, intervention,
feedback, and telemetry-derived summaries. Settlement remains an admin-only
evidence workflow and never runs in telemetry ingestion.

## Viomi Adapter (云米主动上报回调)

The authenticated mobile API also supports the required on-demand pull flow:

```text
Android -> POST /rehealth/mobile/viomi/bind (IMEI + productCode)
Android -> POST /rehealth/mobile/viomi/sync (IMEI + epoch-ms window + metrics)
Backend -> Viomi token/device/history OpenAPI -> HardwareIngestionPort -> hardware_db
Backend -> normalized persisted measurements -> Android Room
```

`AppId`, `AppKey`, and the cached `AccessToken` remain server-side. Bind verifies
that the IMEI is visible to the configured Viomi account, stores only a hashed
device identity in software_db, and scopes the binding to the authenticated user.
Device-list requests use the `UserId` returned by Viomi's token response; the
configured `REHEALTH_VIOMI_USER_ID` is only a compatibility fallback when that
field is absent.
Sync supports `HEART_RATE`, `BLOOD_PRESSURE`, and `BLOOD_OXYGEN`, parses vendor
timestamps as UTC, and caps a request at 31 days. Records are returned to Android
only after durable hardware ingest succeeds. `NO_NEW_DATA` is a successful no-op.

`POST /rehealth/viomi/report` lets the Viomi (miwitracker) platform push wearable
telemetry to this backend. The watch does **not** call `measurements/batch`
directly; Viomi's cloud calls our callback after receiving the watch data.

### Flow

```text
Viomi cloud (signed JWT HS256 with shared AppKey)
  -> POST /rehealth/viomi/report   (@IgnoreAuth; no Jeecg session)
  -> ViomiReportController writes the exact {"code":1,"msg":"操作成功"} ack
  -> ViomiReportService verifies JWT, maps payload, calls HardwareIngestionPort
  -> HardwareTelemetryIngestionService
  -> TelemetryBatchValidator
  -> JdbcHardwareTelemetryWriter (same hardware datasource + idempotency)
```

### Authentication

- The token is delivered in `Authorization: Bearer <jwt>` or in the body field
  `AccessToken`.
- Verified with HMAC-SHA256 (JWT HS256) using `rehealth.viomi.app-key`.
- Claims `appId` / `imei` are read from the JWT payload (fallback to the body
  `Imei` field and configured `rehealth.viomi.app-id`).
- When `rehealth.viomi.require-auth=true` (default) and verification fails, the
  endpoint returns `{"code":0,"msg":"操作失败"}` so Viomi retries.

### Field mapping (Viomi -> ReHealth metricType)

| Viomi DataType | Viomi field | ReHealth metricType | unit |
| --- | --- | --- | --- |
| `Health` | `heartRate` | `HEART_RATE` | bpm |
| `Health` | `bloodOxygen` | `SPO2` | % |
| `Health` | `bloodPressureMax`/`bloodPressureMin` | `BLOOD_PRESSURE` (primary=systolic, secondary=diastolic) | mmHg |
| `Health` | `steps` | `STEPS` | steps |
| `Health` | `distance` | `DISTANCE` | m |
| `Health` | `calorie` | `CALORIE` | kcal |
| `Health` | `deepSleep`/`lighSleep`/`totalSleep`/`sleepTime` | `hardware_sleep_session` (deep/light minutes) | - |
| `StepRoll`/`StepRolls` | `step`/`roll`/`distance`/`calorie` | `STEPS` / `ROLL` / `DISTANCE` / `CALORIE` | steps / count / m / kcal |
| `Temperature` | `temperature` | `BODY_TEMPERATURE` | °C |
| `Location` | `battery` | `DEVICE_BATTERY` | % |

Empty/blank Viomi values are skipped so a partial payload persists the available
metrics. `ResultData` is a JSON string and is parsed per `DataType`.

### Ownership and idempotency

- `deviceId` = Viomi `imei`.
- `userId` = configured `rehealth.viomi.user-id` (default `viomi-gateway`),
  i.e. a platform gateway account, because the callback has no Jeecg user
  session. Per-IMEI → real-user binding through `software_db` device binding is a
  follow-up (depends on E1.1).
- `batchId` = `viomi-{imei}-{dataType}-{reqId|hash}`, giving idempotency under
  the same `(user_id, device_id, batch_id)` unique key.

### Response contract

```json
{ "code": 1, "msg": "操作成功" }
```

on success, and:

```json
{ "code": 0, "msg": "操作失败" }
```

on auth failure, validation rejection, or persistence error. The transport is
always HTTP 200 so Viomi marks the report delivered based on `code`.

### Configuration

```yaml
rehealth:
  viomi:
    enabled: ${REHEALTH_VIOMI_ENABLED:true}
    app-id: ${REHEALTH_VIOMI_APP_ID:}
    app-key: ${REHEALTH_VIOMI_APP_KEY:}
    require-auth: ${REHEALTH_VIOMI_REQUIRE_AUTH:true}
    user-id: ${REHEALTH_VIOMI_USER_ID:viomi-gateway}
    source: ${REHEALTH_VIOMI_SOURCE:viomi}
```

`app-id`/`app-key` are issued by Viomi during onboarding and injected via the
`REHEALTH_VIOMI_APP_ID` / `REHEALTH_VIOMI_APP_KEY` environment variables (no
secrets in source). `require-auth` defaults to `true`; for local integration
before Viomi issues real credentials, set `REHEALTH_VIOMI_REQUIRE_AUTH=false`
so reports pass through (still persisted under the configured `user-id`).

## Production Follow-up

The direct JDBC transaction is appropriate for an MVP pilot, not the final
high-concurrency topology. A later task should add one durable queue/stream,
consumer batch writers, pressure tests, observability, partitioning/retention,
and dead-letter handling without changing the Android batch contract.
