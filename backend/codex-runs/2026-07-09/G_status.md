# G QA Release Acceptance Status

Date: 2026-07-09
Workstream: `G_qa_release_acceptance`
Approved task: G1 only
Status: complete

## Scope

This G1 run was an acceptance audit, blocker register, and release-readiness gate. It did not implement product features and did not modify Android/backend/model-service source code.

Docs created:

- `D:\rehealthAI\QA_TEST_PLAN.md`
- `D:\rehealthAI\RELEASE_CHECKLIST.md`
- `D:\rehealthAI\ACCEPTANCE_REPORT.md`
- `D:\rehealthAI\codex-runs\2026-07-09\G_status.md`

## Required Inputs Read

- `AGENTS.md`
- `ENGINEERING.md`
- `CODEX_ORCHESTRATION.md`
- `Android-apk\README.md`
- `Android-apk\BUILD_NOTES.md`
- `Android-apk\docs\FEATURE_EXTRACTOR.md`
- `Android-apk\docs\NETWORK_FEATURE_EVALUATE.md`
- `Android-apk\docs\BLE_BACKGROUND_QA.md`
- `Android-apk\codex-runs\2026-07-09\A_status.md`
- `Android-apk\codex-runs\2026-07-09\C_status.md`
- `Android-apk\codex-runs\2026-07-09\B_status.md`
- Backend docs and E status docs
- Model-service README, API contract, and F status doc

Missing expected Android input:

- `Android-apk\codex-runs\2026-07-09\D_status.md`

Note: a root `codex-runs\2026-07-09\D_status.md` exists, but the Android workstream status file is missing from the requested Android path.

## Validation Commands

Android from `D:\rehealthAI\Android-apk`:

```powershell
$env:JAVA_HOME = "D:\Android_Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
git diff --check
```

Results:

- `testDebugUnitTest`: PASS.
- `assembleDebug`: PASS.
- `git diff --check`: PASS with CRLF warnings only.

Backend from `D:\rehealthAI\backend\jeecg-boot`:

```powershell
$env:JAVA_HOME = "D:\Android_Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
D:\rehealthAI\tools\apache-maven-3.9.11\bin\mvn.cmd -pl jeecg-boot-module/jeecg-module-rehealth -am package -DskipTests
D:\rehealthAI\tools\apache-maven-3.9.11\bin\mvn.cmd -pl jeecg-module-system/jeecg-system-start -am package -DskipTests
```

Results:

- `jeecg-module-rehealth`: PASS.
- `jeecg-system-start`: PASS.

Model-service from `D:\rehealthAI\model-service`:

```powershell
python -m pytest
python -m compileall app
C:\Users\kiki\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe -m pytest
C:\Users\kiki\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe -m compileall app
```

Results:

- Normal `python -m pytest`: FAIL due to local Python launcher/stub, no useful output.
- Normal `python -m compileall app`: FAIL due to local Python launcher/stub, no useful output.
- Bundled Python `pytest`: PASS, 12 tests.
- Bundled Python `compileall`: PASS.

## Git State Summary

- `Android-apk`: branch `work/C_android_feature_extractor`, clean, latest relevant commit `5528e22 feat(android): connect feature evaluation to backend`. Push or set upstream if intended for review.
- `backend`: branch `work/E_backend_mobile_api`, clean. Push or set upstream if remote state requires it.
- `model-service`: branch `main...origin/main`, clean.
- `rehealth-android`: branch `main...origin/main`, clean.
- Root `D:\rehealthAI` is not a git repository, so root QA docs cannot be committed from the workspace root.

## Acceptance Summary

- A1: accepted.
- C1: accepted.
- E1: accepted with documented E2 limitations.
- F1: accepted for mock-marked integration.
- B1: accepted with blockers.
- D1: accepted with process blocker because `Android-apk\codex-runs\2026-07-09\D_status.md` is missing.

## B1 Blocker Register

1. No physical MRD ring evidence for background collection under lock screen, killed app, Bluetooth off, permission denied, or reboot.
2. No production UI toggle and no Android 13 notification permission UX.
3. Raw BLE packet hex / parsed JSON is logged in current MRD BLE code and must be removed or build-gated before production release.
4. Legacy `RingViewModel` still contains optional cloud snapshot upload through `ReHealthBackendClient`; it is outside BLE service/repository but keeps ring orchestration tied to a non-D1 backend path.
5. Reboot persistence is not implemented or accepted.

Classification: B1 accepted with blockers, not release-approved.

## E2 Decision

E2 is not approved to start as final-release work until the missing Android D status is restored or explicitly superseded.

E2 may start on a clean backend branch after missing Android D status is restored, Android/backend branches are made reviewable, and B1 blockers are either fixed or formally accepted as out-of-scope for the next slice.

## Files Changed

- `D:\rehealthAI\QA_TEST_PLAN.md`
- `D:\rehealthAI\RELEASE_CHECKLIST.md`
- `D:\rehealthAI\ACCEPTANCE_REPORT.md`
- `D:\rehealthAI\codex-runs\2026-07-09\G_status.md`

No Android/backend/model-service source files were changed by G1.
