# ReHealth MVP Acceptance Report

Date: 2026-07-09
Workstream: G_qa_release_acceptance
Decision: acceptance audit complete. This is not final release approval.

## Executive Decision

The MVP engineering state is partially accepted for continued development, but final release is blocked.

Android, backend, and model-service compile/test at their current scopes. The Android source tree is now clean with D1 committed. The release gate is still blocked by missing Android D status, lack of B1 physical background BLE QA evidence, production-unsafe raw BLE logging, and the fact that `/measurements/batch` is still E2-pending.

## Accepted Workstreams

- A1 Android build health: accepted. Build docs exist and Android builds pass.
- C1 Android feature extractor: accepted. CVD 16 extractor compiles and has unit coverage.
- E1 backend mobile API: accepted with E2 limitations. Dedicated `jeecg-module-rehealth` exists, packages successfully, and Java does not implement CatBoost/SHAP/scoring.
- F1 model-service: accepted for mock-marked integration. FastAPI tests pass and `MockRiskScorer` returns `is_mock=true`.

## Blocked Or Conditional Workstreams

- B1 BLE/background collection: accepted with blockers, not release-approved.
- D1 Android network feature evaluation: code accepted as committed/compiling, but process-blocked because Android D status is missing under `Android-apk`.
- E2 durable hardware ingestion: not started. May begin on a clean backend branch after the missing Android D status is restored or explicitly superseded, and B1 blockers are assigned.

## B1 Status

Classification: Accepted with blockers.

Evidence:

- B1 commit exists: `2cb9a6d feat(android): add resilient ring background collection`.
- B1 changed files include `AndroidManifest.xml`, `RingForegroundService.kt`, `RingBackgroundRecoveryWorker.kt`, notification channel, BLE guards, collection policy/settings, `RingViewModel`, `MrdBleRingRepository`, unit test, `BLE_BACKGROUND_QA.md`, and `B_status.md`.
- Foreground service is declared non-exported with connected-device foreground type.
- Notification channel and Stop action exist.
- WorkManager recovery worker exists and uses `NetworkType.NOT_REQUIRED`.
- Service calls `repository.syncAll()` and does not call backend/model-service directly.
- `MrdBleRingRepository.persistPackets()` writes parsed measurements, sleep, activity, and raw chunks to Room through `dao.insertBatch()`.

Blockers:

- No physical locked-screen / killed-app / Bluetooth-off / permission-denied MRD ring QA evidence.
- No production UI toggle and no Android 13 notification permission UX.
- Current MRD code logs raw packet hex / parsed JSON, which violates production log policy for health data.
- Legacy `RingViewModel` still contains optional cloud snapshot upload through `ReHealthBackendClient`; although not inside the BLE service/repository, this keeps ring orchestration mixed with a non-D1 backend path.
- Reboot behavior is not implemented or accepted.

## D1 Status

Classification: Accepted with process blocker.

Evidence:

- D1 commit exists: `5528e22 feat(android): connect feature evaluation to backend`.
- D1 changed files include `app/build.gradle.kts`, `ReHealthApplication.kt`, `ReHealthApp.kt`, new `network/`, `network/dto/`, `phm/RemotePhmService.kt`, new `features/CvdFeatureVectorDtoMapper.kt`, new tests, and `docs/NETWORK_FEATURE_EVALUATE.md`.
- D1 typed API intentionally excludes `/measurements/batch`.
- D1 does not modify BLE service/repository internals in the current diff.
- `MockPhmService` remains available.

Blockers:

- `Android-apk/codex-runs/2026-07-09/D_status.md` is missing.
- Legacy snapshot client remains and should be dev-gated, retired, or documented as non-production.

## E1 Status

Classification: Accepted with documented E2 limitations.

Evidence:

- `jeecg-boot-module/jeecg-module-rehealth` exists.
- Prototype ReHealth source is absent from `jeecg-module-demo/src/main/java/org/jeecg/modules/rehealth`.
- `ModelServiceClient` and `HttpModelServiceClient` exist.
- Java forwards to model-service endpoints and does not implement CatBoost, SHAP, LLM, or Java risk scoring.
- `HardwareIngestionPort` exists.
- `E2PendingHardwareIngestionPort` returns `INGEST_INTERFACE_READY_E2_PENDING`, `accepted=false`, and `persisted=false`.
- `software_db` / `hardware_db` split is documented.

Limitations:

- No software_db tables/mappers yet.
- No hardware_db/MQ ingestion writer yet.
- `/measurements/batch` is an interface boundary only, not durable telemetry sync.

## F1 Status

Classification: Accepted for mock-marked integration.

Evidence:

- `/health` exists.
- `/v1/cvd/risk/evaluate` exists.
- `/v1/cvd/intervention/generate` exists.
- `/v1/cvd/attribution/individual` exists.
- `MockRiskScorer.is_mock` returns `true`.
- `docs/API_CONTRACT.md` exists.
- Tests pass with bundled Python.

Limitations:

- No validated CatBoost artifact.
- No production SHAP/contribution pipeline.
- Production responses with `is_mock=false` are not approved.

## End-To-End Contract Audit

Result: Mostly aligned, with backend DTO naming nuance.

- All 16 CVD fields align across Android C1 docs, Android `CvdFeatureVector.kt`, model-service schema, and model-service contract.
- Nullable labs align: fasting glucose, total cholesterol, LDL, HDL, triglycerides are nullable.
- `featureQuality` naming is documented and Android D1 DTO preserves `featureQuality` as the envelope key with snake_case quality map keys.
- `risk_score` to `riskScore` mapping is handled in backend `RiskEvaluateResponseDto` via `@JSONField`.
- `risk_level` to `riskLevel` mapping is handled in backend `RiskEvaluateResponseDto`.
- `is_mock` to `isMock` mapping is handled in backend `RiskEvaluateResponseDto`.
- `model_version` to `modelVersion` mapping is handled in backend `RiskEvaluateResponseDto`.
- Android D1 `RiskResultDto` uses snake_case properties directly for model/backend response stability.
- Backend `CvdFeatureVectorDto` uses camelCase Java fields for feature values; Android D1 sends snake_case DTOs and model-service accepts both wrapped and flat/camel aliases. Keep this covered by integration tests before E2.

## Git State Audit

| Repo | Branch | State | Latest relevant commit | Push decision |
| --- | --- | --- | --- | --- |
| `Android-apk` | `work/C_android_feature_extractor` | Clean; no upstream shown | `5528e22 feat(android): connect feature evaluation to backend`; `2cb9a6d feat(android): add resilient ring background collection` | Push or set upstream if intended for review |
| `backend` | `work/E_backend_mobile_api` | Clean; no upstream shown | `a92ef69 feat(backend): add dedicated rehealth mobile API module` | Push or set upstream if not already on remote |
| `model-service` | `main...origin/main` | Clean | `9ddf227 docs(model-service): record F1b versioning status` | No push needed |
| `rehealth-android` | `main...origin/main` | Clean | `96af186 feat: add real Xihu map test page` | No push needed |

## Validation Results

- Android `.\gradlew.bat testDebugUnitTest`: PASS.
- Android `.\gradlew.bat assembleDebug`: PASS.
- Android `git diff --check`: PASS with CRLF warnings only.
- Backend `mvn -pl jeecg-boot-module/jeecg-module-rehealth -am package -DskipTests`: PASS.
- Backend `mvn -pl jeecg-module-system/jeecg-system-start -am package -DskipTests`: PASS.
- Model-service normal `python -m pytest`: failed due to environment Python stub with no useful output.
- Model-service bundled Python `-m pytest`: PASS, 12 tests.
- Model-service normal `python -m compileall app`: failed due to environment Python stub with no useful output.
- Model-service bundled Python `-m compileall app`: PASS.

## E2 Readiness Decision

E2 should not start as final-release work until the missing Android D status is restored or explicitly superseded.

E2 may start as a clean backend implementation branch after:

1. `Android-apk/codex-runs/2026-07-09/D_status.md` is restored or intentionally superseded.
2. B1 blockers are accepted as known non-release limitations or assigned to a follow-up.
3. Android/backend branches are pushed or otherwise made reviewable.

Recommended E2 scope once unblocked: implement `hardware_db`/MQ ingestion behind `HardwareIngestionPort`, idempotency keys, persistence schema/migrations, and then add Android `MeasurementSyncWorker` against `/measurements/batch`.

## Exact Next Recommended Task

Task: Android engineering cleanup gate before E2.

Scope:

- Add the missing Android D status doc.
- Remove or build-gate raw BLE packet logging.
- Decide whether legacy `/ring/snapshots` upload is dev-only, retired, or migrated.
- Collect physical MRD ring QA evidence for B1 background collection.

Only after that should E2 durable telemetry ingestion proceed as release-track work.
