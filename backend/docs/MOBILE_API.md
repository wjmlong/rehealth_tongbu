# ReHealth Mobile API E1

Status: E1 implementation contract.  
Module: `jeecg-boot/jeecg-boot-module/jeecg-module-rehealth`.

## Boundary

Production ReHealth backend code lives in `jeecg-module-rehealth`, not `jeecg-module-demo`.

Java backend owns API, persistence boundaries, and orchestration. Python `model-service` remains the authority for risk evaluation, intervention generation, and attribution. Java does not implement CatBoost, SHAP, CVD scoring, clinical diagnosis, treatment logic, or model fallback scoring.

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
| `POST` | `/rehealth/mobile/devices/bind` | Routes to `ReHealthBusinessRepository`; E1 returns explicit software_db persistence-pending status. |
| `POST` | `/rehealth/mobile/measurements/batch` | Validates and transactionally writes the D2 batch to the separate `hardware` datasource; duplicate retries return the existing receipt. |
| `POST` | `/rehealth/mobile/features/evaluate` | Calls `model-service` `POST /v1/cvd/risk/evaluate`; returns controlled error if unavailable; 透传 model-service 的 model_trace 由 M1 引入的 governance trace 块到 Android 客户端，nullable 字段；详见 model-service/docs/MODEL_REGISTRY.md. |
| `GET` | `/rehealth/mobile/risk/latest` | Reads latest risk through software repository boundary; E1 returns `null` until persistence is implemented. |
| `GET` | `/rehealth/mobile/interventions/today` | Reads latest intervention through software repository boundary; E1 returns `null` until persistence is implemented. |
| `POST` | `/rehealth/mobile/interventions/{id}/feedback` | Routes feedback through software repository boundary with explicit persistence-pending status. |

Additional implemented E1 support endpoints:

| Method | Path | E1 behavior |
| --- | --- | --- |
| `GET` | `/rehealth/mobile/health` | Dev health check for the ReHealth module. |
| `POST` | `/rehealth/mobile/interventions/generate` | Calls `model-service` `POST /v1/cvd/intervention/generate`; useful for backend/D1 integration testing. |
| `POST` | `/rehealth/mobile/attribution/events` | Authenticated proxy to PIAS `POST /api/pias/v2/attribute/individual`; accepts Android's local confirmed risk history while backend attribution persistence remains pending and passes through partial/ready forecast and ATT fields. |

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

`risk_history` currently comes from authenticated Android local Room history because
backend E1 attribution persistence is still pending. The response preserves PIAS
`status`, `current_state`, `forecast`, `intervention_effect`, and user report fields;
missing forecast/ATT values are not synthesized.

If model-service is unavailable or misconfigured, E1 returns a controlled `Result.error` response. It does not silently return fake production results.

## D1 Notes

Android D2 may mark a batch complete only when the response has
`accepted=true`, `persisted=true`, and an `ACCEPTED_*` status. E2.1 provides
this durable MySQL MVP contract. A failed Jeecg envelope with `code=503` means
the local queue must retry the same `batchId`.

## E2.1 Telemetry Separation

`POST /rehealth/mobile/measurements/batch` is telemetry ingest only. It does not
trigger risk scoring or call model-service. The request `userId` remains in the
DTO for Android compatibility, but the backend overwrites it with the current
Jeecg `LoginUser.id`; clients cannot choose row ownership.

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

Patient mobile APIs cover P, I, and later individual A only. Group attribution
and settlement evidence require separate backend admin RBAC. Individual A must
eventually be assembled by backend from persisted records, not client-supplied
risk history.
