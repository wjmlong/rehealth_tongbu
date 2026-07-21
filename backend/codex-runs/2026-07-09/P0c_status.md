# P0c Backend Legacy Path Retirement Status

Date: 2026-07-09
Branch: `work/P0c_backend_legacy_path_retirement`

## Scope

P0c only. No Android, model-service, rehealth-android, hardware_db/MQ,
frontend Vue admin, BLE/background, or unrelated Jeecg module files changed.

## Pre-Edit Inventory

1. Backend branch at start: `work/E2_backend_hardware_ingest`.
2. Git status at start: clean.
3. `/rehealth/mobile/features/evaluate` exists in `ReHealthMobileController`.
4. `HttpModelServiceClient` calls model-service `POST /v1/cvd/risk/evaluate`.
5. `/ring/snapshots` does not exist in backend production Java code.
6. `/patient/risk-score` and `/patient/intervention-plan` do not exist in backend production Java code.
7. Backend production Java code does not call `rehealth-android` `/api/pias/predict` or `/api/pias/v2/predict`.
8. `application-dev.yml` uses `rehealth.model-service.base-url: http://127.0.0.1:8000`.
9. `jeecg-module-demo` contains no ReHealth Java mobile routes.
10. In `jeecg-module-rehealth`, `@IgnoreAuth` exists only on `GET /rehealth/mobile/health`.

## Existing Implementation Summary

- `POST /rehealth/mobile/features/evaluate` is the canonical backend risk endpoint.
- `ReHealthMobileServiceImpl.evaluateFeatures` delegates risk evaluation to `ModelServiceClient`.
- `HttpModelServiceClient.evaluateRisk` posts to `/v1/cvd/risk/evaluate`.
- `HttpModelServiceClient.generateIntervention` posts to `/v1/cvd/intervention/generate`.
- `POST /rehealth/mobile/measurements/batch` routes through `HardwareIngestionPort` and does not call model-service.
- `GET /risk/latest` and `GET /interventions/today` read from the pending software repository boundary; they are not active production scoring/generation paths.

## Changes

- Added `docs/CANONICAL_BACKEND_RISK_PATH.md`.
- Updated `docs/MOBILE_API.md` with the canonical risk path, retired legacy path list, model-service authority, and E2 telemetry separation.
- Updated `docs/REHEALTH_PROTOTYPE_INVENTORY.md` with P0c legacy path retirement notes.

## Validation

- Passed: `D:\rehealthAI\tools\apache-maven-3.9.11\bin\mvn.cmd -pl jeecg-boot-module/jeecg-module-rehealth -am package -DskipTests`
  - `jeecg-module-rehealth` compiled 34 source files.
  - Result: `BUILD SUCCESS`.
- Passed: `D:\rehealthAI\tools\apache-maven-3.9.11\bin\mvn.cmd -pl jeecg-module-system/jeecg-system-start -am package -DskipTests`
  - Reactor included `jeecg-module-demo`, `jeecg-module-rehealth`, and `jeecg-system-start`.
  - Result: `BUILD SUCCESS`.
- Passed: `git diff --check` with CRLF normalization warnings only.
- Passed: self-review source scan of `jeecg-module-rehealth` Java and `application-dev.yml` found no active legacy path or algorithm URL:
  - `/rehealth/mobile/ring/snapshots`
  - `/rehealth/mobile/patient/risk-score`
  - `/rehealth/mobile/patient/intervention-plan`
  - `/api/pias/predict`
  - `rehealth.algorithm.base-url`
  - `algorithmBaseUrl`
- Confirmed `Android-apk`, `model-service`, and `rehealth-android` were not modified by P0c.

## Self-Review Notes

- P0c did not add compatibility endpoints or redirects for retired paths.
- P0c did not add Java fallback scoring or calls to `rehealth-android`.
- Canonical backend risk path remains `POST /rehealth/mobile/features/evaluate -> ModelServiceClient -> model-service /v1/cvd/risk/evaluate`.
- `/rehealth/mobile/measurements/batch` remains telemetry ingest only and does not call model-service.
- Historical references remain in audit/prototype documents so reviewers can trace why the paths were removed.

## Risks

- P0c is documentation and release-boundary hardening only because the Java implementation already used the canonical model-service path.
- Backend software_db persistence for latest risk/intervention remains pending by existing E1 design.
- E2 telemetry ingest remains dev-queue/hardware-writer pending and is separate from P0c risk evaluation.
- Android legacy/debug references to `/ring/snapshots` are out of P0c scope and should remain non-primary/non-production.

## Final Git Status

Pre-commit status contained only intended P0c files:

- `docs/CANONICAL_BACKEND_RISK_PATH.md`
- `docs/MOBILE_API.md`
- `docs/REHEALTH_PROTOTYPE_INVENTORY.md`
- `codex-runs/2026-07-09/P0c_status.md`

Expected after commit:

```text
## work/P0c_backend_legacy_path_retirement
```
