# ReHealth Mobile API E1

Status: E1 implementation contract.  
Module: `jeecg-boot/jeecg-boot-module/jeecg-module-rehealth`.

## Boundary

Production ReHealth backend code lives in `jeecg-module-rehealth`; the upstream Demo module is not part of this repository's runtime.

Java backend owns API, persistence boundaries and orchestration. Python
`model-service` remains the authority for CVD risk/SHAP, while PIAS remains the
authority for attribution. The authenticated personalized wellness-plan path is
an explicit LangChain4j use case in Jeecg: it assembles bounded authoritative
context and produces structured conservative actions, but does not implement
CatBoost, SHAP, diagnosis, medication changes or causal attribution.

## Canonical Risk Path

The canonical backend risk path is:

```text
Android
  -> POST /rehealth/mobile/features/evaluate
  -> ReHealthMobileService
  -> ModelServiceClient
  -> POST model-service /v1/cvd/risk/evaluate
  -> backend response
  -> Android UI
```

`POST /rehealth/mobile/features/evaluate` is the only production-style mobile
risk evaluation entry point. Backend Java does not call `rehealth-algorithms`
`/api/pias/predict` or `/api/pias/v2/predict` for production scoring.

## Base Path

Monolith dev URL:

```text
http://localhost:8080/jeecg-boot/rehealth/mobile
```

Controller mapping:

```text
/rehealth/mobile
```

Only `GET /rehealth/mobile/health` is marked `@IgnoreAuth`. All production-style mobile endpoints use the normal JeecgBoot auth flow.

## Endpoints

| Method | Path | E1 behavior |
| --- | --- | --- |
| `GET` | `/rehealth/mobile/config` | Returns API version, endpoint list, model contract, and E1 limitations. |
| `GET` | `/rehealth/mobile/profile` | Reads the current authenticated user's persisted health profile. |
| `PUT` | `/rehealth/mobile/profile` | Upserts typed profile fields, calculates BMI server-side, and returns `version`; stale versions return `409`, while disabled software_db returns retryable `503`. |
| `POST` | `/rehealth/mobile/interviews` | Persists typed answers and baseline summary under the current authenticated user. |
| `GET` | `/rehealth/mobile/interviews/latest` | Reads the current authenticated user's latest persisted interview. |
| `POST` | `/rehealth/mobile/devices/bind` | Persists the current authenticated user's binding when software_db is enabled. |
| `POST` | `/rehealth/mobile/measurements/batch` | Gateway-routed Device Service authority validates `telemetry-v2` and transactionally writes measurement/sleep/activity/diet rows to TimescaleDB; duplicate retries return the existing receipt. |
| `GET` | `/rehealth/mobile/measurements/recent?limit=50` | Reads only the authenticated user's newest normalized measurement, sleep, and activity rows; `limit` is clamped to 1–200 and raw signal payloads are never returned. |
| `POST` | `/rehealth/mobile/features/evaluate` | Calls `model-service` `POST /v1/cvd/risk/evaluate`; returns controlled error if unavailable; 透传 model-service 的 model_trace 由 M1 引入的 governance trace 块到 Android 客户端，nullable 字段；详见 model-service/docs/MODEL_REGISTRY.md. |
| `POST` | `/rehealth/mobile/rhi/evaluate-series` | Authenticated RHI preview. Accepts 1–120 ordered daily RHI v2 requests, calls `model-service POST /v2/rhi/evaluate` sequentially, and returns the same number of ordered evaluations. It does not persist an authoritative RHI snapshot. |
| `GET` | `/rehealth/mobile/risk/latest` | Reads the authenticated user's latest persisted risk. |
| `POST` | `/rehealth/mobile/interventions/generate` | Ignores client-owned health context, reloads profile/interview/latest risk plus tenant-scoped Device Service telemetry context, generates structured actions through LangChain4j, then persists the versioned JSON plan. |
| `GET` | `/rehealth/mobile/interventions/today` | Reads only the authenticated user's structured plan generated during the current `rehealth.mobile.time-zone` calendar day. |
| `POST` | `/rehealth/mobile/interventions/{id}/feedback` | Persists feedback under the authenticated user and returns a typed durable acknowledgement. |

Additional implemented E1 support endpoints:

| Method | Path | E1 behavior |
| --- | --- | --- |
| `GET` | `/rehealth/mobile/health` | Dev health check for the ReHealth module. |
| `POST` | `/rehealth/mobile/attribution/events` | Authenticated proxy to PIAS `POST /api/pias/v2/attribute/individual`; persists request/result under the current user when software_db is enabled. |
| `POST` | `/rehealth/mobile/agent/messages` | Authenticated health-agent proxy; backend assembles persisted user context, rate limits, and calls model-service. Provider credentials never enter the APK. |

## Retired Legacy Risk Paths

The backend production module does not expose these prototype paths:

- `POST /rehealth/mobile/ring/snapshots`
- `GET /rehealth/mobile/patient/risk-score`
- `GET /rehealth/mobile/patient/intervention-plan`
- `POST /api/pias/predict`
- `POST /api/pias/v2/predict`

If these strings appear in historical docs or status notes, they are references
to retired prototype behavior. They must not be used as production risk or
intervention paths.

## Model-Service Configuration

Dev default in `application-dev.yml`:

```yaml
rehealth:
  model-service:
    base-url: http://127.0.0.1:8000
    timeout-seconds: 10
```

`ModelServiceClient` targets:

- `GET /health`
- `POST /v1/cvd/risk/evaluate`
- `POST /v1/cvd/intervention/generate`
- `POST /api/pias/v2/attribute/individual` through the separately configurable `rehealth.attribution-service.base-url`

The retained model-service intervention endpoint is a compatibility path; the
mobile `POST /interventions/generate` implementation uses Jeecg LangChain4j and
does not forward client profile/risk fields to it.

## Personalized Intervention Context

Each generation performs fresh, fail-closed reads in this order:

1. authenticated `software_db` profile;
2. latest health interview;
3. latest persisted CVD risk;
4. Device Service current-local-day activity, sleep, measurements and diet,
   followed by bounded recent/prior 7-day descriptive changes.

The Device Service request includes the resolved tenant, authenticated user and
`rehealth.mobile.time-zone`, and requires the internal service credential.
Client `riskResult`, `featureVector` and `patientContext` remain accepted only
for wire compatibility and are ignored as evidence. The response keeps legacy
summary fields and adds `summary`, `focus_date`, context freshness fields, and
1–5 `items` containing category, title, action, rationale, target, timing,
priority and evidence references. Allowed categories are diet, exercise, sleep,
blood pressure, metabolic and follow-up.

No deterministic mock plan is returned when Device Service, the provider or
software persistence is unavailable.

Attribution request shape:

```json
{
  "risk_history": [
    {"date": "2026-07-22", "Y": 0.219, "Z": 1}
  ],
  "forecast_days": 30,
  "language": "zh"
}
```

`risk_history` currently comes from authenticated Android local Room history. The response preserves PIAS
`status`, `current_state`, `forecast`, `intervention_effect`, and user report fields;
missing forecast/ATT values are not synthesized.

If model-service is unavailable or misconfigured, E1 returns a controlled `Result.error` response. It does not silently return fake production results.

## software_db Configuration

Run `db/software/mysql/V1__create_rehealth_software_tables.sql` on the primary Jeecg datasource and configure:

```yaml
rehealth:
  software-db:
    enabled: true
```

Profiles, interviews, device bindings, feature/risk results, interventions, feedback, and attribution results are scoped using the authenticated `LoginUser.id`. Android stores a completed interview locally first, enqueues the typed payload, and retries it through WorkManager; a disabled software_db never produces a false durable success.

`PatientProfileDto.version` is an optimistic-lock token. Clients should GET the profile, preserve the returned
version while editing, and send it with PUT. The server ignores request `patientId`, derives ownership from the
authenticated principal, validates ranges, and derives BMI from height and weight. Operational profile and
interview fields are stored in typed columns/child rows; model evidence snapshots remain JSON by design.

All software-db-backed reads and writes return a failed Jeecg envelope with
`code=503` when persistence is disabled. Risk, intervention, and attribution
model calls also require software-db persistence before returning success, so a
model response can never be reported as durable when its database write was
skipped.

Today's intervention window is calculated in
`rehealth.mobile.time-zone` (default `Asia/Shanghai`) using
`generated_at >= startOfDay` and `< startOfNextDay`.

Feedback success response:

```json
{
  "interventionId": "plan-id",
  "status": "completed",
  "persisted": true,
  "persistenceStage": "software_db"
}
```

Risk, intervention, and attribution model calls also write minimal audit metadata to `rehealth_model_request_log`: request ID, operation, model version, outcome, and timestamp. Request bodies, telemetry values, tokens, phone numbers, and other health payloads are excluded.

## D1 Notes

Android may mark a batch complete only when the response has
`accepted=true`, `persisted=true`, and an `ACCEPTED_*` status. E2.1 provides
this durable Device Service/TimescaleDB contract. A failed envelope with
`code=503` means
the local queue must retry the same `batchId`.

## E2.1 Telemetry Separation

`POST /rehealth/mobile/measurements/batch` is telemetry ingest only. It does not
trigger risk scoring or call model-service. The request `userId` remains in the
DTO for Android compatibility, but the backend overwrites it with the current
Jeecg `LoginUser.id`; clients cannot choose row ownership.

`telemetry-v2` adds optional `dietRecords` and returns `dietRecordCount`; older
`d2-v1` and `telemetry-v1` batches remain accepted. Diet rows contain structured
meal text/nutrients only—no raw images—and share the batch transaction,
idempotency receipt and rollback semantics.

Successful new response:

```json
{
  "status": "ACCEPTED_PERSISTED",
  "accepted": true,
  "persisted": true,
  "queued": false,
  "durableQueue": false,
  "queueType": "direct-hardware-db",
  "ingestStage": "HARDWARE_DB_COMMITTED"
}
```

An idempotent retry returns `ACCEPTED_DUPLICATE`, `persisted=true`, and the
original `receiptId`. Raw signal chunks and raw payload-like fields remain
rejected by default.

The current direct JDBC path is the durable MVP. MQ/stream workers and
high-concurrency pressure testing remain a production follow-up.

`GET /measurements/recent` uses the same separate `hardware` datasource and authenticated ownership boundary. If hardware persistence is disabled, the endpoint returns a retryable `503` envelope instead of falling back to mock or cross-user data.

Patient mobile APIs cover P, I, and later individual A only. Group attribution
and settlement evidence require separate backend admin RBAC. Individual A must
eventually be assembled by backend from persisted records, not client-supplied
risk history.

## Android Client Contract

Active Android code uses only the typed, authenticated client. The retired
`/ring/snapshots`, `/patient/mvp`, `/patient/*`, and legacy check-in client were
removed. Device binding sends a stable hashed hardware identity; telemetry is
queued from Room and excludes raw signal bytes. Health-agent requests are
backend-proxied and Android build configuration contains no model-provider key.
