# ReHealth Hardware Ingest Architecture E2.1

Date: 2026-07-13
Module: `jeecg-boot/jeecg-boot-module/jeecg-module-rehealth`

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
    enabled: true
    app-id: "<viomi-issued-app-id>"
    app-key: "<viomi-issued-app-key>"
    require-auth: true
    user-id: viomi-gateway
    source: viomi
```

`app-id`/`app-key` are issued by Viomi during onboarding and must be supplied via
environment/secret in every environment. For local testing without a real Viomi
token, set `rehealth.viomi.require-auth=false`.

## Production Follow-up

The direct JDBC transaction is appropriate for an MVP pilot, not the final
high-concurrency topology. A later task should add one durable queue/stream,
consumer batch writers, pressure tests, observability, partitioning/retention,
and dead-letter handling without changing the Android batch contract.
