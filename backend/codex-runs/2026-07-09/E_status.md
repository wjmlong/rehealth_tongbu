# E Backend Mobile API Status

Date: 2026-07-09  
Workstream: E_backend_mobile_api  
Status: implemented and validated

## Summary

- E0.5 accepted architecture is implemented as a dedicated `jeecg-module-rehealth` Maven module.
- Production ReHealth code is not continued in `jeecg-module-demo`.
- The old prototype path was inspected; no Java files remain there.
- Mobile API controller, DTOs, service layer, model-service client abstraction, software repository boundary, and hardware ingest boundary exist.
- Java backend does not implement CatBoost, SHAP, CVD scoring, intervention generation, or attribution logic.
- Hardware database and MQ batch writer remain E2.

## Implemented Endpoints

```text
GET  /rehealth/mobile/config
POST /rehealth/mobile/devices/bind
POST /rehealth/mobile/measurements/batch
POST /rehealth/mobile/features/evaluate
GET  /rehealth/mobile/risk/latest
GET  /rehealth/mobile/interventions/today
POST /rehealth/mobile/interventions/{id}/feedback
```

Support endpoints:

```text
GET  /rehealth/mobile/health
POST /rehealth/mobile/interventions/generate
POST /rehealth/mobile/attribution/events
```

## Validation Plan

Run from `D:/rehealthAI/backend/jeecg-boot`:

```powershell
mvn -pl jeecg-boot-module/jeecg-module-rehealth -am package
git diff --check
git status --short
```

If local `mvn` is unavailable, use the bundled Maven path already observed in prior E1 validation:

```powershell
D:/rehealthAI/tools/apache-maven-3.9.11/bin/mvn.cmd -pl jeecg-boot-module/jeecg-module-rehealth -am package
```

## Validation Results

- `mvn -pl jeecg-boot-module/jeecg-module-rehealth -am package` could not run because `mvn` is not on PATH in this shell.
- `D:/rehealthAI/tools/apache-maven-3.9.11/bin/mvn.cmd -pl jeecg-boot-module/jeecg-module-rehealth -am package` passed with `BUILD SUCCESS`.
- `D:/rehealthAI/tools/apache-maven-3.9.11/bin/mvn.cmd -pl jeecg-module-system/jeecg-system-start -am package` passed with `BUILD SUCCESS`, confirming the monolith start module can package with the registered ReHealth dependency.
- Project Surefire configuration skips tests in these Maven package runs.
- `git diff --check` passed; Git only reported CRLF normalization warnings for touched text files.

## Self-Review Results

- `@IgnoreAuth` exists only on `GET /rehealth/mobile/health`.
- No ReHealth Java source remains under `jeecg-module-demo/src/main/java/org/jeecg/modules/rehealth`.
- No Java-side CatBoost, SHAP, clinical scoring, intervention generation, or attribution logic was added.
- Model-service URL is configurable by `rehealth.model-service.base-url`; dev default is documented in `application-dev.yml`.
- `features/evaluate`, `interventions/generate`, and `attribution/events` return controlled API errors when model-service is unavailable or misconfigured.
- `Android-apk`, `model-service`, and `rehealth-android` git statuses were checked and had no modified files.

## Known E1 Limits

- `software_db` tables/mappers are not implemented in E1.
- `hardware_db` ingestion and MQ are not implemented in E1.
- `/measurements/batch` returns explicit E2-pending response and is not durable sync completion.
- Risk/intervention/attribution calls require running Python model-service at configured base URL.
- E2 must replace `E2PendingHardwareIngestionPort` with MQ/hardware_db ingestion before Android treats measurement upload as durable sync.
