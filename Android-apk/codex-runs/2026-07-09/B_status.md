# B Android BLE Background Status

Date: 2026-07-09

## What Changed

- Added `RingForegroundService` for local-first low-frequency ring collection.
- Added a persistent notification channel and Stop action for active collection.
- Added `RingBackgroundRecoveryWorker` as a conservative WorkManager recovery path.
- Added shared BLE guard and interval policy helpers.
- Added ViewModel start/stop APIs for the background service without changing manual measurement behavior.
- Declared foreground-service and notification permissions plus the non-exported service in the manifest.
- Added unit coverage for the 15-minute collection policy.

## Scope Boundaries

- No backend files changed.
- No model-service files changed.
- No rehealth-android files changed.
- No Android network client or RemotePhmService changes.
- No `/measurements/batch` sync implementation.
- No raw PPG/RRI upload.
- Background collection calls only `RingRepository.syncAll()`, which persists through Room.

## Validation

Passed:

- `.\gradlew.bat testDebugUnitTest`
- `.\gradlew.bat assembleDebug`
- `git diff --check`

Additional checks:

- Searched new background packages for backend/model-service/upload references: none found.
- Verified manifest/service/channel/worker/15-minute interval references are present.
- `git status --short --branch` remains dirty because this checkout already contains uncommitted C/D work in shared Android files and network/feature packages.

## Known Risks

- B1 exposes service/ViewModel APIs but does not add a production UI toggle because UI files are outside this workstream scope.
- Real locked-screen BLE behavior still needs validation on a physical MRD ring and target Android versions.
- Android 13+ notification permission UX should be handled before a production release toggle.
- Commit staging must avoid unrelated existing C/D files in this worktree.

## Next Recommended Task

Add the product-owned UI toggle and real-device QA evidence for background ring collection.
