# ReHealth Backend API E1

Status: E1 implementation contract.
Module: `jeecg-boot/jeecg-boot-module/jeecg-module-rehealth`.

## Module Boundary

Production ReHealth backend code lives in `jeecg-module-rehealth`.

The previous prototype controller under `jeecg-module-demo/src/main/java/org/jeecg/modules/rehealth/` was removed from the Java source tree because it used `@IgnoreAuth`, in-memory state, obsolete model URLs, and Java-side mock scoring.

## Endpoint List

Base path in monolith mode:

```text
/jeecg-boot/rehealth/mobile
```

Implemented E1 endpoints:

```text
GET  /rehealth/mobile/health
GET  /rehealth/mobile/config
POST /rehealth/mobile/devices/bind
POST /rehealth/mobile/measurements/batch
POST /rehealth/mobile/features/evaluate
GET  /rehealth/mobile/risk/latest
POST /rehealth/mobile/interventions/generate
GET  /rehealth/mobile/interventions/today
POST /rehealth/mobile/interventions/{id}/feedback
POST /rehealth/mobile/attribution/events
```

Only `/health` is marked `@IgnoreAuth`. Other mobile endpoints should use normal JeecgBoot authentication/authorization.

## Model-Service Contract

`ModelServiceClient` is the only Java boundary for model calls.

Configured property:

```yaml
rehealth:
  model-service:
    base-url: http://127.0.0.1:8000
    timeout-seconds: 10
```

Calls:

- `GET /health`
- `POST /v1/cvd/risk/evaluate`
- `POST /v1/cvd/intervention/generate`
- `POST /v1/cvd/attribution/individual`

Java backend does not implement CatBoost, SHAP, CVD scoring, intervention generation, or attribution logic.

## Database Split Status

E1 defines the software and hardware boundaries but does not implement database persistence.

`software_db` boundary:

- `ReHealthBusinessRepository`
- current implementation: `E1PendingSoftwareDbReHealthBusinessRepository`
- status: interface ready, tables/mappers pending

`hardware_db` boundary:

- `HardwareIngestionPort`
- current implementation: `E2PendingHardwareIngestionPort`
- status: ingestion port ready, MQ and hardware_db writes pending E2

Telemetry upload is routed to `HardwareIngestionPort`; it is not written directly into ordinary business tables.

## D1 Integration Notes

Android D1 can use `/features/evaluate` once backend config points to a running model-service. Telemetry upload to `/measurements/batch` currently returns an explicit E2-pending response and must not be treated as durable sync completion.
