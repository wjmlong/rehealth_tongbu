# D_android_network_feature_evaluate Status

Workstream: `D_android_network_feature_evaluate` (D1 narrowed scope).
Branch: `work/C_android_feature_extractor` (no dedicated D branch available in this repo; committed on the existing work branch where the prompt committed me to operate).
Target repo: `D:\rehealthAI\Android-apk`.

## What changed

Added a typed Retrofit/Moshi backend API client and a remote-capable PHM service that
connects the local C1 feature extractor to the backend E1 feature-evaluation API, with
local mock fallback preserved.

New files:

- `app/src/main/java/com/rehealth/genie/network/ReHealthApi.kt`
  Retrofit interface covering the E1 D1-safe endpoints (`GET /rehealth/mobile/health`,
  `GET /rehealth/mobile/config`, `POST /rehealth/mobile/features/evaluate`,
  `GET /rehealth/mobile/risk/latest`, `GET /rehealth/mobile/interventions/today`,
  `POST /rehealth/mobile/interventions/{id}/feedback`). Defines the JeecgBoot `Result`
  envelope type used by the unwrap layer.
- `app/src/main/java/com/rehealth/genie/network/ReHealthMobileApi.kt`
  Wraps the Retrofit interface: builds the Moshi-backed Retrofit instance with an auth
  interceptor, unwraps the JeecgBoot envelope, and maps HTTP/timeout/JSON errors into
  the typed `RemotePhmError` hierarchy and a `RemotePhmOutcome` sealed result.
- `app/src/main/java/com/rehealth/genie/network/BackendConfig.kt`
  Configurable base URL normalization and shared OkHttp factory with explicit timeouts.
  Dev default is the Android emulator loopback `http://10.0.2.2:8080/jeecg-boot`. Documented
  LAN-IP replacement for physical-device QA.
- `app/src/main/java/com/rehealth/genie/network/RemotePhmError.kt`
  Sealed error hierarchy plus timeout detection and a `Throwable.toRemotePhmError()` mapper.
  Known ReHealth backend error codes are mapped (55001/55002/55003/55004).
- `app/src/main/java/com/rehealth/genie/network/dto/FeatureEvaluationDtos.kt`
  DTOs aligned to `CvdFeatureVector.kt`, `MOBILE_API.md`, and `API_CONTRACT.md`:
  `CvdFeatureVectorDto`, `FeatureQualityDto`, `FeatureEvaluateRequest`, `RiskResultDto`,
  `InterventionPlanDto`, `InterventionFeedbackRequest/Response`, `HealthCheckResponse`,
  `MobileConfigResponse`. Field keys use snake_case per the canonical model contract.
- `app/src/main/java/com/rehealth/genie/features/CvdFeatureVectorDtoMapper.kt`
  Pure mapping from C1 `CvdFeatureVector` (camelCase) to the snake_case request DTO.
  Enforces `featureQuality` coverage for all canonical CVD 16 fields and throws if any
  entry is missing, so malformed feature vectors never reach the backend/model-service.
- `app/src/main/java/com/rehealth/genie/phm/RemotePhmService.kt`
  Remote-capable PHM service with a single timeout-only retry on feature evaluation and
  a local `MockPhmService` fallback. Returns a `FeatureEvaluationOutcome` describing
  whether mock fallback was used and which typed error occurred, without embedding raw
  health data.
- `app/src/test/java/com/rehealth/genie/features/CvdFeatureVectorDtoMapperTest.kt`
  DTO mapping tests for full vector, missing quality entries, null fields, requestId
  generation, and quality DTO field fidelity.
- `app/src/test/java/com/rehealth/genie/phm/RemotePhmServiceRemoteFailureTest.kt`
  RemotePhmService failure-handling tests via MockWebServer: backend
  unavailable, HTTP 5xx, model-service unavailable (55002), invalid upstream response
  (55003), contract violation (55004), empty body, success envelope, retry path,
  `risk/latest` null-result tolerance, and null-API backend-unavailable reporting.

Modified files:

- `app/build.gradle.kts`
  Added Retrofit 2.11.0, converter-moshi, moshi 1.15.1, moshi-kotlin,
  logging-interceptor 4.12.0, androidx.work:work-runtime-ktx:2.10.0 (reserved for E2;
  not wired to a production telemetry worker), and test-only MockWebServer and
  kotlinx-coroutines-test/`-core`. Each production dependency includes a comment
  explaining why and what the rejected alternative was.
- `app/src/main/java/com/rehealth/genie/ReHealthApplication.kt`
  Added lazy `reHealthMobileApi` and `remotePhmService` properties, reusing the
  existing `BuildConfig.REHEALTH_API_BASE_URL`/`REHEALTH_API_TOKEN`. Existing
  `backendClient` and ring repository wiring unchanged.
- `app/src/main/java/com/rehealth/genie/ui/ReHealthApp.kt`
  Minimal additive block in `ModelScreen` that calls `GET /rehealth/mobile/health` and
  surfaces reachability + mode (mock/cloud/local fallback) status. Business logic lives
  in `refreshRemoteFeatureEvaluateStatus`; the composable only renders display strings.

New doc:

- `app/src/main/../docs/NETWORK_FEATURE_EVALUATE.md` (i.e.
  `Android-apk/docs/NETWORK_FEATURE_EVALUATE.md`) with scope, D1-safe endpoints,
  E2 deferrals, base-URL config, flow, error handling, files, validation, and risks.

## Implementation decisions

- **Reuse vs. introduce**: existing `ReHealthBackendClient` uses OkHttp + Gson with manual
  JSON path digging (`deepFind`). Feature-evaluate DTOs are nested (snake_case feature
  vector + 16-entry `featureQuality` map) and parsing them by hand is error-prone, so D1
  introduces Retrofit + Moshi for a typed, contract-aligned client. The legacy snapshot
  client is intentionally left untouched so the existing `RingViewModel` snapshot path
  keeps working (BLE collection stays independent from D1).
- **Mock fallback kept**: `MockPhmService` is unchanged and remains the snapshot the legacy
  UI consumes. `RemotePhmService.mock()` exposes it for dev/demo paths.
- **No durable telemetry sync**: `/measurements/batch` is intentionally absent from the
  new Retrofit interface. E2 owns durable hardware ingestion; D1 builds only the
  feature-evaluation/risk/intervention/feedback path.
- **WorkManager**: dependency added as the future home for the E2 durable upload queue,
  but D1 does not register a production telemetry worker.

## Tests run

```powershell
.\gradlew.bat testDebugUnitTest   # BUILD SUCCESSFUL; 24 tests, 0 failures
.\gradlew.bat assembleDebug       # BUILD SUCCESSFUL
```

`java.exe` not on the default PATH; built with `$env:JAVA_HOME = "D:\Android_Studio\jbr"`.

## Self-review

- Searched new/modified files for `TODO`, `FIXME`, hardcoded production URLs, raw
  PPG/RRI upload, and `/measurements/batch` production dependency:
  - No TODO/FIXME markers in the new files.
  - No hardcoded production URLs: base URL comes from `BuildConfig`; the only constant
    (`BackendConfig.DEFAULT_BASE_URL`) is the documented Android emulator loopback.
  - No raw PPG/RRI upload, no high-frequency signal upload, no `/measurements/batch`
    production worker.
- Cross-checked DTO field names against `CvdFeatureVector.kt`,
  `FEATURE_EXTRACTOR.md`, `MOBILE_API.md`, and `API_CONTRACT.md` (snake_case lab fields,
  camelCase-parented `featureQuality` map matching the contract example).
- Confirmed backend/model-service/rehealth-android repositories were not modified.
- Confirmed BLE/repository/service internals were not modified; only additive UI status
  block added and application-level DI wiring.

## Known risks

- The legacy `ReHealthBackendClient` (`/rehealth/mobile/ring/snapshots`) is still used
  by `RingViewModel`. Migrating that path onto the new typed client is a follow-up task
  but is out of D1 scope to avoid touching `RingViewModel` (not in D1's allowed files).
- D1 does not implement token refresh; when the configured `X-Access-Token` is rejected,
  the app stays on local mock fallback. Acceptable for dev; revisit for hardened QA.
- Empty 200 responses are detected via content-length; chunked proxies may report -1,
  in which case the wrapper falls through to the JSON-parse failure path. Acceptable
  for D1; revisit when a production gateway is in front of E1.
- ModelScreen performs a `GET /rehealth/mobile/health` on each screen entry. Read-only
  and idempotent, but adds backend traffic. Can be gated or debounced in a follow-up.
- `risk/latest` and `interventions/today` intentionally return null in E1 until
  persistence is implemented; the app tolerates null and shows fallback messaging.

## Definition of Done check

- Existing Android network code inspected: yes (`ReHealthBackendClient`).
- Backend API client exists: yes (`ReHealthApi` + `ReHealthMobileApi`).
- Backend base URL configurable: yes (`BuildConfig` + `BackendConfig.normalizeBaseUrl`).
- CVD feature evaluate request DTO exists: yes (`FeatureEvaluateRequest`).
- Risk/intervention response DTOs exist: yes (`RiskResultDto`, `InterventionPlanDto`).
- RemotePhmService or equivalent exists: yes.
- Mock fallback remains available: yes (`MockPhmService` unchanged).
- Feature evaluation path implemented or exact UI wiring limitation documented: implemented
  for evaluate + risk/latest + interventions/today + feedback; minimal additive UI in
  ModelScreen (backend reachability + mode). Wider UI integration (risk card, intervention
  cards) intentionally left to a later slice and documented.
- `/measurements/batch` is not used as production durable telemetry sync: confirmed.
- `NETWORK_FEATURE_EVALUATE.md` written: yes.
- `D_status.md` written: yes.
- Unit tests added where practical: yes (DTO mapping + remote failure handling).
- `testDebugUnitTest` passes: yes.
- `assembleDebug` passes: yes.
- No backend files modified: confirmed.
- No model-service files modified: confirmed.
- No rehealth-android files modified: confirmed.
- No BLE internals modified: confirmed.

## Final git status

See the final response's `git status --short --branch` output. Commit will use message:

```text
feat(android): connect feature evaluation to backend
```

## Exact next recommended task

- E2: implement durable telemetry ingestion writer and `hardware_db`/MQ path, then add an
  Android `MeasurementSyncWorker` (WorkManager) that uploads Room `ring_measurements`
  batches through `/rehealth/mobile/measurements/batch` with idempotency keys and retry.
- D2: wire `RemotePhmService.evaluateFeatures` results (risk level, contributions,
  today's intervention) into the Data/Attribution/Model Compose screens behind a
  `PhmViewModel`, replacing the legacy mock-only risk display in `RingViewModel`.
