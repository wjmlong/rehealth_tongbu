# ReHealth Prototype Inventory

Date: 2026-07-09  
Scope: E1 cleanup of prototype ReHealth Java source under `jeecg-module-demo`.

## Inspected Path

```text
jeecg-boot/jeecg-boot-module/jeecg-module-demo/src/main/java/org/jeecg/modules/rehealth/
```

## File Inventory

Current E1 state:

```text
No Java files remain under the inspected demo-module ReHealth path.
```

Historical E0.5 inventory found one untracked prototype file:

```text
mobile/ReHealthMobileController.java
```

## Prototype Findings From E0.5

The removed prototype:

- Lived inside `jeecg-module-demo`.
- Exposed `/rehealth/mobile/**`.
- Used `@IgnoreAuth` on all endpoints.
- Stored snapshots, profile, and check-ins in static in-memory collections.
- Used Java-side mock risk scoring.
- Called obsolete `rehealth.algorithm.base-url + /api/pias/predict`, not the accepted model-service contract.
- Built intervention plans in Java.
- Did not persist software business records or hardware telemetry.
- Did not fit the dual-database/microservice design.

## E1 Decision

Decision: remove/quarantine by deletion from the demo Java source tree and replace with dedicated production module `jeecg-module-rehealth`.

Reason: untracked Java source under `jeecg-module-demo` can still compile and expose unsafe routes. The dedicated module now contains the E1 mobile API, model-service client boundary, software business repository port, and hardware ingestion port.

## P0c Legacy Path Retirement

Production risk evaluation is restricted to:

```text
POST /rehealth/mobile/features/evaluate
  -> ReHealthMobileService
  -> ModelServiceClient
  -> POST /v1/cvd/risk/evaluate
```

The following prototype paths remain retired and must not be restored as
production scoring or intervention paths:

- `POST /rehealth/mobile/ring/snapshots`
- `GET /rehealth/mobile/patient/risk-score`
- `GET /rehealth/mobile/patient/intervention-plan`
- `POST /api/pias/predict`
- `POST /api/pias/v2/predict`

`rehealth-algorithms` is a model training and HealthAgent/PIAS research repository, not the
production scoring authority. Backend production code must use Python
`model-service` through `ModelServiceClient`.

E2 telemetry ingest through `/rehealth/mobile/measurements/batch` is separate
from this risk path. Telemetry ingest must not synchronously trigger production
risk scoring.

## Verification

Current source scan:

```text
jeecg-module-demo/src/main/java/org/jeecg/modules/rehealth/ contains no Java files.
```

The only remaining ReHealth mobile API controller is:

```text
jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/java/org/jeecg/modules/rehealth/mobile/controller/ReHealthMobileController.java
```

P0c source scan also confirms that `jeecg-module-demo` contains no ReHealth Java
routes and no production backend code calls the old PIAS `/api/pias/**` endpoints.

## M1-P0c Followup: model_trace Passthrough

M1 introduced a nullable `model_trace` governance block in the model-service
`RiskEvaluateResponse`. Backend `jeecg-module-rehealth` treats `model_trace` as a
//update-begin---author:codex---date:2026-07-10 for：【M1-P0c followup】Passthrough-only doc note for model_trace-----------
pass-through field: `RiskEvaluateResponseDto.modelTrace` (type `ModelTraceDto`)
is populated by FastJSON recursive deserialization in `HttpModelServiceClient`
and forwarded to the Android client without backend interpretation, persistence,
or strong dependency. `model_trace` is nullable and omittable.
//update-end---author:codex---date:2026-07-10 for：【M1-P0c followup】Passthrough-only doc note for model_trace-----------
See `backend/docs/MOBILE_API.md` (features/evaluate row) and
`model-service/docs/MODEL_REGISTRY.md`.
