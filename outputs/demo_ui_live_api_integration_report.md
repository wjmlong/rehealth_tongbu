# Demo UI Live API Integration Report

Generated: 2026-07-21

## 1. Final conclusion

The existing Android application already contains the real MRD BLE repository, Room
persistence, typed JeecgBoot API client, authentication, durable upload queue,
WorkManager sync, foreground collection service, feature extraction, and feedback
queue. This change preserves the existing Compose dashboard and device-page layout
while fixing the attribution page so it no longer renders generated risk history or
always routes through `MockPhmService`.

The attribution page now uses a real PIAS HTTP client. It displays an explicit empty
state until enough real risk-history records exist, and provides retry/error states
for an unavailable PIAS service. The current mobile backend exposes only
`risk/latest`, not a patient risk-history endpoint, so a real multi-day PIAS report
cannot yet be produced by the application. No mock history is substituted.

APK packaging was attempted on Windows. Kotlin/KSP compilation progressed without a
source error, but final packaging could not be completed reliably because concurrent
pre-existing Gradle/Kotlin daemon processes stopped or removed incremental build
outputs. APK success is therefore not claimed.

## 2. Branch information

| Item | Value |
| --- | --- |
| Checked-out branch | `main` |
| Requested base branch | `codex/real-device` |
| Actual branch availability | neither local nor `origin` contains that branch after `git fetch --prune` |
| Work branch | none created, per request |
| Live capability source | existing files in this working tree, documented by `outputs/realdevice_vs_local_comparison.md` |
| Commit | pending at report generation |

## 3. Changed files

| File | Purpose |
| --- | --- |
| `Android-apk/app/build.gradle.kts` | Reads API and PIAS URLs from local configuration/environment, removes the source default signing secret, and disables debug fake-data seeding. |
| `Android-apk/app/src/main/java/com/rehealth/genie/ReHealthApplication.kt` | Wires the PIAS client into the existing remote PHM service and removes the hard-coded debug JWT. |
| `Android-apk/app/src/main/java/com/rehealth/genie/network/PiasApiClient.kt` | Adds a stateless typed request/response client for real PIAS individual attribution. |
| `Android-apk/app/src/main/java/com/rehealth/genie/phm/RemotePhmService.kt` | Maps real PIAS attribution response DTOs to the existing UI domain model; no local attribution fallback is returned. |
| `Android-apk/app/src/main/java/com/rehealth/genie/ui/AttributionReportScreen.kt` | Retains visual layout and retry interaction, replaces generated history with real-data empty state. |
| `outputs/demo_ui_live_api_integration_report.md` | Replaces the previous inaccurate completion claim with this report. |

No generated screenshots, XML dumps, `.omo` session files, automation scripts, local
properties backup, or daemon logs are included in the intended commit.

## 4. Capability migration

| Capability | Current source and status |
| --- | --- |
| Real device | `MrdBleRingRepository` persists collected values through Room before cloud upload. |
| Real API | `ReHealthMobileApi`, `AuthenticatedApiClient`, and `ReHealthBackendClient` call JeecgBoot. |
| Authentication | `SessionStore`, login/register ViewModels, and auth-aware API client; hard-coded debug token removed. |
| Sync | Room upload queue, `SyncRepository`, `MeasurementSyncWorker`, and retry/backoff already exist. |
| Feature scoring | `HealthFeatureExtractor` -> typed `/features/evaluate` through `RemotePhmService`. |
| Attribution | New `PiasApiClient` -> `/api/pias/v2/attribute/individual`; only runs with genuine history. |
| Mock separation | Fake ring disabled in debug default; existing mock PHM remains only as an explicitly labeled feature-scoring fallback. Attribution has no mock fallback. |

## 5. Core page mapping

```text
AttributionReportScreen
  -> RemotePhmService
  -> PiasApiClient
  -> PIAS /api/pias/v2/attribute/individual
  -> IndividualAttributionResponseDto
  -> IndividualAttributionResult

Dashboard / second Compose page
  -> RingViewModel and refreshRemoteFeatureEvaluateStatus
  -> RingRepository + HealthFeatureExtractor + RemotePhmService
  -> MRD BLE / Room + JeecgBoot /features/evaluate
  -> RiskResultDto and RingUiState
  -> existing Compose cards, charts, and feedback controls

Device page
  -> RingViewModel
  -> MrdBleRingRepository
  -> MRD SDK -> Room -> ReHealthBackendClient snapshot upload
  -> RingUiState
  -> existing Compose device UI
```

## 6. Verification

| Command | Result |
| --- | --- |
| `git fetch --prune origin` | Success. Only `main` and `origin/main` exist. |
| `gradlew.bat testDebugUnitTest --no-daemon --max-workers=1` | Success. 28 tests across 4 suites, 0 failures. |
| `gradlew.bat assembleDebug` | Kotlin compilation reached `compileDebugKotlin` without reporting a source error; first package attempt failed because an AGP incremental temporary file was missing. |
| `gradlew.bat clean assembleDebug --no-daemon --max-workers=1` | Rebuilt through KSP and started APK output, but concurrent pre-existing daemon activity stopped/removed outputs before terminal success. APK success is not claimed. |
| `git diff --check` | Success; no whitespace errors. |

## 7. Remaining issues and risks

- The mobile backend needs an authenticated patient risk-history endpoint, or Android
  needs a Room migration that persists daily non-mock scoring results. This is required
  before the PIAS attribution page can produce a multi-day real report.
- Real MRD pairing, permissions, physical-device collection, login, JeecgBoot upload,
  and PIAS response require device/backend QA and were not claimed as exercised here.
- `Android-apk/local.properties` currently names a WSL SDK directory; Windows builds
  proceed via `ANDROID_HOME=D:\Android_SDK` but report that local-properties warning.
- The workspace contains extensive untracked automation output from earlier work. It
  must remain out of the code commit.
