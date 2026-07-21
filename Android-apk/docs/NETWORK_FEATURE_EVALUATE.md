# Network Feature Evaluate (D1)

> Android workstream D1: connect the local C1 feature extractor to the backend E1
> `/rehealth/mobile/features/evaluate` endpoint and replace the purely mock PHM path
> with a remote-capable service while keeping local fallback safe.

Status: D1 narrowed scope. Production durable telemetry sync is intentionally E2.

## Scope

D1 delivers:

- A typed Retrofit/Moshi backend API client for the E1 D1-safe endpoints.
- DTOs aligned to the Android C1 `CvdFeatureVector`, `MOBILE_API.md`, and the
  model-service `API_CONTRACT.md`.
- A configurable backend base URL.
- `RemotePhmService` that orchestrates feature evaluation, risk retrieval, and
  intervention retrieval/feedback, with a single lightweight timeout-only retry on
  feature evaluation.
- `MockPhmService` retained as a local/dev fallback when the backend is unreachable,
  misconfigured, returns a non-success envelope, or the request DTO mapping fails.
- Safe, typed error handling: backend unavailable, timeout, model-service
  unavailable, invalid DTO, missing feature fields, HTTP status.
- Minimal, additive UI wiring: the "端侧健康模型" page now surfaces whether the
  backend E1 health check is reachable and which mode (mock/cloud/local fallback) is
  being used. No large redesign.

## D1-safe endpoints

Implemented and used by the client:

| Method | Path | Use in D1 |
| --- | --- | --- |
| `GET`  | `/rehealth/mobile/health` | Backend reachability check from the app. |
| `GET`  | `/rehealth/mobile/config` | Optional client config/version fetch. |
| `POST` | `/rehealth/mobile/features/evaluate` | **Primary D1 use**: C1 feature vector -> risk result. |
| `GET`  | `/rehealth/mobile/risk/latest` | Read latest risk (E1 returns null until persistence is ready). |
| `GET`  | `/rehealth/mobile/interventions/today` | Read today's intervention plan (E1 returns null until persistence is ready). |
| `POST` | `/rehealth/mobile/interventions/{id}/feedback` | Submit user feedback for an intervention. |

## Intentionally deferred to E2

D1 deliberately does **not** build production telemetry ingestion. Deferred items:

- `POST /rehealth/mobile/measurements/batch` durable upload. The endpoint exists in E1,
  but E1 returns `status=INGEST_INTERFACE_READY_E2_PENDING`. D1 does **not** implement a
  production worker that uploads raw ring measurements through that endpoint.
- `hardware_db`, MQ, and high-concurrency wearable ingest.
- Raw PPG/RRI or high-frequency signal upload. The Android client does not upload raw
  signal chunks as part of the feature-evaluation flow.
- Token refresh and durable auth-backed queue resilience (`ModelServiceClient` /
  JeecgBoot token expiry recovery). D1 reads the configured token from BuildConfig.
- Cross-process WorkManager persistence guarantee for feature evaluation; the simple
  retry helper lives inside `RemotePhmService` in-process and is not durable.

## Base URL configuration

Default dev URL (Android emulator → host loopback):

```text
http://10.0.2.2:8080/jeecg-boot
```

Source: `app/build.gradle.kts`

```kotlin
buildConfigField("String", "REHEALTH_API_BASE_URL", "\"http://10.0.2.2:8080/jeecg-boot\"")
buildConfigField("String", "REHEALTH_API_TOKEN", "\"\"")
```

Override for physical-device QA: change those fields (or the local.properties-driven
build type) to the LAN IP of the backend host, e.g.:

```text
http://192.168.1.50:8080/jeecg-boot
```

`BackendConfig.normalizeBaseUrl` validates the URL shape at construction and trims the
trailing slash. The Retrofit base is `normalizeBaseUrl + "/"`.

## Flow

```text
HealthFeatureExtractor (C1) -> CvdFeatureVector (camelCase)
   -> CvdFeatureVectorDtoMapper.toFeatureEvaluateRequest(vector)
   -> FeatureEvaluateRequest { featureVector: snake_case DTO + featureQuality map, requestId }
   -> ReHealthMobileApi.evaluateFeatures(request)
   -> unwrap JeecgBoot Result<> envelope
   -> RemotePhmOutcome.Success(RiskResultDto) | Failure(RemotePhmError)
   -> RemotePhmService.evaluateFeatures()
       success -> FeatureEvaluationOutcome(result, usedMockFallback=false)
       failure -> usedMockFallback=true, error typed, MockPhmService remains as the
                  local snapshot the legacy UI consumes.
```

The feature-evaluation request DTO keeps `featureQuality` entries for every canonical
CVD 16 field, keyed by snake_case name. `CvdFeatureVectorDtoMapper` enforces this and
throws `IllegalStateException` if any field is missing its quality entry, so the
backend/model-service never sees a partial payload that would fail with HTTP 422.

## Error handling

`RemotePhmError` is a sealed hierarchy:

- `BackendUnavailable` — connect failure / IOException that is not a timeout.
- `Timeout` — OkHttp connect/read/write timeout. Triggers a single in-process retry in
  `RemotePhmService.evaluateFeatures` (`maxAttempts = 2`, fixed `retryDelayMillis`).
- `HttpStatusError` — non-success HTTP status, or success=false envelope with an
  otherwise-unknown JeecgBoot error code.
- `ModelServiceUnavailable` — backend reports model-service is down/unconfigured
  (JeecgBoot codes 55001/55002).
- `InvalidDto` — empty body, unparsable body, or success=true with no `result`.
- `MissingFeatureFields` — backend reports a model contract violation (code 55004) or
  local DTO mapping detected missing `featureQuality` coverage.
- `Unknown` — anything else.

No raw health data is embedded in any error message. The fallback summary surfaced to
the UI uses the stable `eventName` (e.g. `backend_unavailable`, `timeout`).

## Files

New:

- `app/src/main/AndroidManifest.xml` is untouched.
- `app/src/main/java/com/rehealth/genie/network/ReHealthApi.kt`
- `app/src/main/java/com/rehealth/genie/network/ReHealthMobileApi.kt`
- `app/src/main/java/com/rehealth/genie/network/BackendConfig.kt`
- `app/src/main/java/com/rehealth/genie/network/RemotePhmError.kt`
- `app/src/main/java/com/rehealth/genie/network/dto/FeatureEvaluationDtos.kt`
- `app/src/main/java/com/rehealth/genie/features/CvdFeatureVectorDtoMapper.kt`
- `app/src/main/java/com/rehealth/genie/phm/RemotePhmService.kt`
- `app/src/test/java/com/rehealth/genie/features/CvdFeatureVectorDtoMapperTest.kt`
- `app/src/test/java/com/rehealth/genie/phm/RemotePhmServiceRemoteFailureTest.kt`

Modified:

- `app/build.gradle.kts` — added Retrofit/Moshi/logging-interceptor, WorkManager
  (reserved for E2; not wired to a production telemetry worker), and
  test-only MockWebServer/coroutines-test.
- `app/src/main/java/com/rehealth/genie/ReHealthApplication.kt` — added
  `reHealthMobileApi` and `remotePhmService` lazy properties. Existing `backendClient`
  (the legacy snapshot client) and ring repository wiring are unchanged.
- `app/src/main/java/com/rehealth/genie/ui/ReHealthApp.kt` — minimal additive block in
  `ModelScreen` that shows backend reachability and mode. No business logic inside the
  composable; parsing lives in `refreshRemoteFeatureEvaluateStatus`.

Unchanged and out of scope:

- `MrdBleRingRepository`, `RingForegroundService`, BLE protocol parsing: untouched.
- `RingViewModel` and the existing `cloudRiskLevel`/`cloudRiskScore`/`patient mvp` flow:
  untouched. D1's remote feature-evaluate path is separate and additive.
- Legacy `ReHealthBackendClient`: untouched; remains for the existing snapshot path.
- `backend/`, `model-service/`, `rehealth-android/`: untouched.

## Validation

From `D:\rehealthAI\Android-apk`:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Both pass. Test report: 24 unit tests (C1 mapper + extractor + RemotePhmService remote
failure handling), 0 failures.

## Risks

- Empty 200 response bodies are detected via content-length; some chunked-encoding
  proxies may report content-length `-1`, in which case the wrapper falls into the
  JSON-parse failure path and reports `Unknown`/`BackendUnavailable`. Acceptable for D1;
  revisit when a real production gateway is in front of E1.
- The ModelScreen status block performs a `GET /rehealth/mobile/health` on every
  screen entry. It is read-only and idempotent, but counts as backend traffic.
- The legacy `ReHealthBackendClient` snapshot upload (`/rehealth/mobile/ring/snapshots`)
  is unchanged and still used by `RingViewModel`. That path is not part of the D1
  feature-evaluation contract; if cross-workstream D2/E2 work standardizes on the new
  typed client, a follow-up task should migrate the snapshot path.
- D1 does not implement token refresh. When the configured `X-Access-Token` is rejected
  by protected endpoints, the app stays on local mock fallback.
- No production telemetry upload. Handheld/physical-device testing requires the E2 work
  to add durable `/measurements/batch` with idempotency and a real hardware_db writer.
