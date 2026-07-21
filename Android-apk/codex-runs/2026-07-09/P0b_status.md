# P0b Android Canonical Risk UI Path Status

Date: 2026-07-09

## Start State

- Initial branch observed: `work/D_android_network_feature_evaluate`
- Initial HEAD: `a0079c4 docs(android): restore D1 network integration status`
- P0b branch created: `work/P0b_android_canonical_risk_ui_path`
- D1 commit present: `5528e22 feat(android): connect feature evaluation to backend`
- B1 commit present: `2cb9a6d feat(android): add resilient ring background collection`
- B1-owned files not touched.
- Prohibited stash observed and not applied:
  `stash@{0}: On work/C_android_feature_extractor: preexisting telemetry sync work before D1 status fixup`

## Implementation Summary

- Made the Data/Model risk UI read from a canonical risk status instead of legacy
  `RingViewModel.cloudRisk*` fields.
- Canonical UI flow reads local profile plus Room ring measurements/activities, builds
  `HealthMemorySnapshot`, runs `HealthFeatureExtractor`, maps to `CvdFeatureVector`, and
  calls `RemotePhmService.evaluateFeatures()`.
- `RemotePhmService` remains the primary service for risk evaluation.
- `MockPhmService` is used only through explicit fallback when remote evaluation fails.
- UI displays/preserves risk score, risk level, feature contributions, model version,
  request id, and mock/fallback mode.
- Risk DTOs now accept both snake_case and camelCase response fields.

## Files Changed

- `app/src/main/java/com/rehealth/genie/network/dto/FeatureEvaluationDtos.kt`
- `app/src/main/java/com/rehealth/genie/ui/ReHealthApp.kt`
- `app/src/test/java/com/rehealth/genie/phm/RemotePhmServiceRemoteFailureTest.kt`
- `docs/CANONICAL_RISK_PATH.md`
- `codex-runs/2026-07-09/P0b_status.md`

## Validation Results

Commands run from `D:\rehealthAI\Android-apk` with `JAVA_HOME=D:\Android_Studio\jbr`:

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

Results:

- `testDebugUnitTest`: PASS
- `assembleDebug`: PASS

Pending finish-step commands after this status file is written:

- `git diff --check`: PASS
- self-review searches: PASS, scoped results summarized below
- pre-commit `git status --short --branch`: only intended P0b files modified/added

## Legacy Path Audit

- `ReHealthBackendClient.uploadRingSnapshot` still exists for legacy snapshot/debug
  behavior.
- `/rehealth/mobile/ring/snapshots` still exists only in that legacy client path and
  old docs; it is not the primary Data/Model risk UI source.
- `/patient/risk-score` and `/patient/intervention-plan` remain only in legacy docs.
- `RingViewModel` still owns legacy cloud snapshot sync state; P0b did not edit it.

Self-review search summary:

- `MockPhmService`: present in `ReHealthApplication`/`RemotePhmService` fallback wiring,
  `MockPhmService.kt`, tests/docs/status only; no direct `ModelScreen` primary risk
  source remains.
- `RemotePhmService`: primary risk service in app wiring and UI helper.
- `ReHealthMobileApi` / `features/evaluate`: canonical typed backend path remains present.
- `ReHealthBackendClient`, `uploadRingSnapshot`, `ring/snapshots`: legacy path still
  present in `ReHealthBackendClient`/`RingViewModel`; not used as primary Data/Model risk
  UI source.
- `patient/risk-score`, `patient/intervention-plan`: legacy docs only.
- `measurements/batch`: comments/docs only; no Android API method or UI integration.
- `ring_signal_chunks`, `PPG`, `RRI`: pre-existing Room/ring/BLE code and docs only; P0b
  did not add upload integration.

## Telemetry Prohibition Confirmation

- P0b does not call `/rehealth/mobile/measurements/batch`.
- P0b does not upload raw PPG.
- P0b does not upload raw RRI.
- P0b does not upload `ring_signal_chunks`.
- P0b did not apply `stash@{0}`.

## Known Risks

- The UI uses the current `patientMvp.profile` as the available profile source. If that
  is still local/demo fallback data, the remote evaluator is correctly labeled but the
  input profile quality remains limited.
- Canonical intervention generation is not promoted into the Android UI in P0b. Existing
  intervention display remains a follow-up unless the backend canonical path is safely
  wired.
- Legacy snapshot upload remains in `RingViewModel` for existing sync behavior, outside
  this branch's allowed edit scope.

## Final Git Status

Pre-commit status:

```text
## work/P0b_android_canonical_risk_ui_path
 M app/src/main/java/com/rehealth/genie/network/dto/FeatureEvaluationDtos.kt
 M app/src/main/java/com/rehealth/genie/ui/ReHealthApp.kt
 M app/src/test/java/com/rehealth/genie/phm/RemotePhmServiceRemoteFailureTest.kt
?? codex-runs/2026-07-09/P0b_status.md
?? docs/CANONICAL_RISK_PATH.md
```

Expected post-commit status: clean on `work/P0b_android_canonical_risk_ui_path`.
