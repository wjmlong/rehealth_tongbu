# ReHealth MVP QA Test Plan

Date: 2026-07-09
Owner: G_qa_release_acceptance
Scope: acceptance audit plan for Android MVP, backend E1, and model-service F1. This is not final release approval.

## Test Environment

- Android app: `D:\rehealthAI\Android-apk`
- Backend: `D:\rehealthAI\backend\jeecg-boot`
- Model service: `D:\rehealthAI\model-service`
- Physical QA required: BLE-capable Android phone and MRD ring.
- Command-line Java: set `JAVA_HOME=D:\Android_Studio\jbr`.
- Command-line Python fallback: `C:\Users\kiki\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe`.

## Automated Validation

Run before every candidate handoff:

```powershell
cd D:\rehealthAI\Android-apk
$env:JAVA_HOME = "D:\Android_Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
git diff --check
```

```powershell
cd D:\rehealthAI\backend\jeecg-boot
$env:JAVA_HOME = "D:\Android_Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
D:\rehealthAI\tools\apache-maven-3.9.11\bin\mvn.cmd -pl jeecg-boot-module/jeecg-module-rehealth -am package -DskipTests
D:\rehealthAI\tools\apache-maven-3.9.11\bin\mvn.cmd -pl jeecg-module-system/jeecg-system-start -am package -DskipTests
```

```powershell
cd D:\rehealthAI\model-service
C:\Users\kiki\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe -m pytest
C:\Users\kiki\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe -m compileall app
```

## Manual Android QA

1. Android install/onboarding
   - Install `app/build/outputs/apk/debug/app-debug.apk`.
   - Launch app and complete onboarding/login demo flow.
   - Confirm no crash on first run and no production medical diagnosis wording.

2. Ring permission
   - On Android 12+, deny and then grant `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT`.
   - On Android 13+, verify notification permission behavior before enabling background collection.
   - Confirm denial pauses collection safely.

3. Ring scan/connect
   - Turn Bluetooth on.
   - Scan from device binding screen.
   - Connect MRD ring.
   - Confirm connection state updates and no duplicate scan/connect loop is created.

4. Manual measurement
   - Trigger HR, SpO2, BP, and temperature measurement where firmware supports the metric.
   - Confirm each successful result is written to Room before any upload attempt.
   - Confirm unsupported metrics fail with safe UI text.

5. Background collection
   - Start B1 background collection through service/ViewModel API or approved debug path.
   - Confirm foreground notification appears with Stop action.
   - Put app in background and wait at least one 15 minute interval.
   - Confirm no tight loop and no duplicate collection while foreground sync is active.

6. Room persistence
   - Inspect local Room tables:
     - `ring_measurements`
     - `ring_sleep_sessions`
     - `ring_activities`
     - `ring_signal_chunks`
   - Confirm collected data persists across app restart.
   - Confirm Room is the first persistence layer.

7. Feature extraction
   - Generate a CVD vector from local profile plus Room data.
   - Confirm all 16 fields are present in the contract.
   - Confirm nullable labs remain null and are marked `MISSING`.
   - Confirm `featureQuality` is keyed by snake_case field names.

8. Backend feature evaluation
   - Run backend E1 and model-service F1.
   - Configure Android base URL for emulator or physical device LAN.
   - Submit feature evaluation through `POST /rehealth/mobile/features/evaluate`.
   - Confirm model-service errors fall back to `MockPhmService` and do not block BLE collection.

9. Model-service risk result
   - Confirm response includes `risk_score`, `risk_level`, `feature_contributions`, `model_version`, `is_mock`, `missing_fields`, `quality_warnings`, and `summary`.
   - Confirm Android/backend map snake_case response fields to camelCase DTO properties where needed.
   - Confirm `is_mock=true` is visible and not described as production model output.

10. Intervention retrieval
    - Call `POST /v1/cvd/intervention/generate` through backend support endpoint or model-service directly.
    - Confirm intervention text is conservative wellness support only.
    - Confirm `medical_disclaimer` is present.

11. Feedback submission
    - Submit `POST /rehealth/mobile/interventions/{id}/feedback`.
    - Confirm E1 returns explicit software persistence-pending status.
    - Confirm no raw health data, phone number, token, or identifier is logged.

12. Offline, no backend, no model-service
    - Disable network.
    - Stop backend.
    - Stop model-service.
    - Confirm BLE/manual/background collection continues locally.
    - Confirm feature evaluation reports fallback mode and no data loss.

13. Bluetooth off
    - Turn Bluetooth off while background collection is active.
    - Confirm notification reports paused/off state.
    - Confirm collection retries later and does not crash.

14. Permission denied
    - Deny BLE permission and start collection.
    - Confirm service reports permission required and does not attempt BLE operations.

15. App killed
    - Kill app process while background collection is active.
    - Reopen app.
    - Confirm WorkManager recovery is scheduled and no duplicate aggressive loops appear.

16. Lock screen
    - Lock device during active background collection.
    - Wait at least one collection interval.
    - Confirm records are persisted locally after unlock.

17. Reboot
    - Reboot device after enabling background collection.
    - Confirm current B1 limitation: no boot receiver is documented, so collection is not release-approved across reboot until explicitly implemented or product accepts manual restart.

18. Duplicate collection prevention
    - Start foreground/manual sync while background service interval is due.
    - Confirm background cycle skips when `RingConnectionState.SYNCING`.
    - Confirm Room primary keys/on-conflict behavior avoid duplicate latest rows.

## Failure Cases To Record

- Backend unavailable.
- Model-service unavailable.
- Model-service returns HTTP 422.
- Empty backend response body.
- Token rejected or missing.
- Bluetooth unsupported/off.
- BLE permission denied.
- Notification permission denied.
- App killed.
- Lock screen collection.
- Reboot.
- Duplicate start/stop of foreground service.

## Exit Criteria

- All automated commands pass or each failure has a dated blocker.
- Android physical ring QA evidence exists for scan/connect, manual metrics, background collection, lock screen, app killed, and Bluetooth off.
- No production claim is made while `MockRiskScorer` is active.
- `/measurements/batch` is not treated as durable telemetry sync until E2.
- No raw PPG/RRI or raw packet payload is uploaded by default.
- No raw health data or raw BLE packets are logged in production builds.
