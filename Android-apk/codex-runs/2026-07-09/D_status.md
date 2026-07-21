# D_android_network_feature_evaluate Status

Date: 2026-07-09

## Scope

Workstream: `D1_status_fixup`

This file restores/supersedes the missing D1 status artifact for the Android network
feature-evaluate integration. No backend, model-service, rehealth-android, BLE behavior,
or production telemetry sync code is changed by this fixup.

## D1 Commit

- D1 commit hash: `5528e22`
- Full commit: `5528e22a6c3704a3120df5cbf5c07e4f7398dd69`
- Commit subject: `feat(android): connect feature evaluation to backend`

## Changed Files Summary

D1 added the Android remote feature-evaluation path and supporting documentation:

- `app/build.gradle.kts`: added Retrofit, Moshi, OkHttp logging, WorkManager dependency
  reservation, and test-only networking/coroutines dependencies.
- `app/src/main/java/com/rehealth/genie/ReHealthApplication.kt`: added lazy backend API
  and remote PHM service wiring.
- `app/src/main/java/com/rehealth/genie/features/CvdFeatureVectorDtoMapper.kt`: mapped
  C1 feature vectors into backend feature-evaluate DTOs.
- `app/src/main/java/com/rehealth/genie/network/BackendConfig.kt`: normalized and
  validated backend base URL configuration.
- `app/src/main/java/com/rehealth/genie/network/ReHealthApi.kt`: added typed API client
  construction.
- `app/src/main/java/com/rehealth/genie/network/ReHealthMobileApi.kt`: added the D1-safe
  mobile API interface and response-envelope handling.
- `app/src/main/java/com/rehealth/genie/network/RemotePhmError.kt`: added typed remote
  failure states without logging raw health data.
- `app/src/main/java/com/rehealth/genie/network/dto/FeatureEvaluationDtos.kt`: added
  request/response DTOs for feature evaluation, risk, interventions, and feedback.
- `app/src/main/java/com/rehealth/genie/phm/RemotePhmService.kt`: added remote-capable
  PHM orchestration with safe mock fallback.
- `app/src/main/java/com/rehealth/genie/ui/ReHealthApp.kt`: added minimal backend
  reachability/mode status in the model screen.
- `app/src/test/java/com/rehealth/genie/features/CvdFeatureVectorDtoMapperTest.kt`: added
  DTO mapper unit coverage.
- `app/src/test/java/com/rehealth/genie/phm/RemotePhmServiceRemoteFailureTest.kt`: added
  remote failure/fallback unit coverage.
- `docs/NETWORK_FEATURE_EVALUATE.md`: documented the D1 network feature-evaluate
  contract, flow, validation, and risks.

## Endpoint List Used

D1 uses the backend through the typed Android mobile API client under the configured
base URL, defaulting to:

```text
http://10.0.2.2:8080/jeecg-boot
```

### D1-Safe Endpoints

- `GET /rehealth/mobile/health`
- `GET /rehealth/mobile/config`
- `POST /rehealth/mobile/features/evaluate`
- `GET /rehealth/mobile/risk/latest`
- `GET /rehealth/mobile/interventions/today`
- `POST /rehealth/mobile/interventions/{id}/feedback`

## Explicitly Deferred

- Production `/measurements/batch` telemetry sync.
- Raw PPG/RRI upload.
- High-concurrency hardware telemetry upload.
- `hardware_db`/MQ ingestion.

## Validation Results From G1

- `testDebugUnitTest` PASS.
- `assembleDebug` PASS.
- `git diff --check` PASS.

## Fixup Validation

Commands run from `D:\rehealthAI\Android-apk`:

```powershell
$env:JAVA_HOME = "D:\Android_Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
git diff --check
git status --short --branch
```

Results:

- Initial `.\gradlew.bat testDebugUnitTest`: failed because pre-existing uncommitted
  telemetry sync work added `TelemetryBatchMapperTest`, which failed outside the D1
  committed scope.
- Preserved those unrelated pre-existing telemetry/sync worktree changes in:
  `stash@{0}: On work/C_android_feature_extractor: preexisting telemetry sync work before D1 status fixup`.
- Re-run `.\gradlew.bat testDebugUnitTest`: PASS.
- `.\gradlew.bat assembleDebug`: PASS.
- `git diff --check`: PASS.
- `git status --short --branch` before committing this doc:

```text
## work/C_android_feature_extractor
?? codex-runs/2026-07-09/D_status.md
```

## Known Risks

- D1 intentionally keeps production telemetry ingestion out of scope; durable
  `/measurements/batch` upload still requires a separate E2/D2 implementation.
- Raw ring signal upload, high-concurrency hardware telemetry, `hardware_db`, and MQ
  ingestion remain deferred and must not be inferred from the D1 feature-evaluate client.
- The Android app still depends on a configured backend base URL and token for protected
  endpoints; unreachable or unauthorized backends fall back to local/mock PHM behavior.
- Token refresh is not implemented by D1.
- Existing legacy snapshot upload behavior is unchanged and separate from the D1-safe
  feature-evaluation contract.

## Final Git Status

Observed after committing this status file and creating the D review branch:

```text
## work/D_android_network_feature_evaluate
```

No backend, model-service, or rehealth-android files are changed by this fixup.
