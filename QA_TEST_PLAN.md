# ReHealth MVP QA Test Plan

Last reviewed: 2026-07-29
Scope: Android MVP, backend services, model-service, contract gates, and release QA. This plan is not final release approval; see `STATUS.md` for current blockers.

## Test Environment

- Run commands from the repository root unless a command explicitly changes directory.
- Android app: `Android-apk/`
- Backend: `backend/`
- Model service: `model-service/`
- Physical QA required: BLE-capable Android phone and the applicable MRD/RWFit ring or HBand watch/band.
- Use JDK, Maven, Python, Android SDK, and Gradle wrapper versions documented by each module; do not commit machine-local paths.

## Automated Validation

Run before every candidate handoff:

```powershell
cd Android-apk
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
cd ..
```

```powershell
mvn -f backend/contracts/telemetry/pom.xml test
mvn -f backend/device-service/pom.xml test
mvn -f backend/jeecg-boot/pom.xml -pl jeecg-boot-module/jeecg-module-rehealth -am test
```

```powershell
python -m pytest model-service
python -m compileall model-service/app
python backend/contracts/scripts/validate_contracts.py
python backend/qa/rehealth_stack_gate.py topology --compose backend/deploy/rehealth/docker-compose.yml --profiles staging,production --report topology.json
git diff --check
```

## Manual Android QA

1. Android install/onboarding
   - Install `app/build/outputs/apk/debug/app-debug.apk`.
   - Launch app, request a registration SMS code, and confirm `/sys/sms` receives
     `X-Sign` plus `X-Timestamp` without returning “请求参数不完整”.
   - With local `JEECG_SMS_DEV_MODE=true`, confirm the successful response auto-fills
     `123456`; a failed request must not fill any code. Complete registration and auto-login.
   - Save a personal profile and complete a health interview, log out, then log in again.
     Confirm the profile, typed health-history fields, latest interview baseline and focus areas
     are queried and displayed without requiring another edit. Make the risk/model endpoint
     unavailable and confirm profile/history reading still succeeds.
   - Confirm no crash on first run and no production medical diagnosis wording.

2. Voice permission
   - Remove microphone permission, open the health interview, and tap the microphone.
     Confirm the app explains the purpose and that recordings are not stored before launching
     the Android permission request. Deny it and verify the app offers system settings while
     text input remains usable; grant it and verify speech recognition starts.
   - Confirm completing an interview cannot leave the result page until its Room queue insert
     succeeds. After upload, verify the latest interview is stored in the normalized
     `software_db` interview/answer/baseline/focus tables and can be queried after re-login.

3. Ring permission
   - On Android 12+, deny and then grant `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT`.
   - On Android 13+, verify notification permission behavior before enabling background collection.
   - Confirm denial pauses collection safely.

4. Ring scan/connect
   - Turn Bluetooth on.
   - Scan from device binding screen.
   - Connect the wearable selected by the current `productCode` (MRD, RWFit, or HBand).
   - Confirm connection state updates, the active binding survives app restart,
     and no duplicate scan/connect loop is created.
   - After clearing app data, start background collection before binding and
     confirm no fixed-address connection, automatic scan, or fabricated row occurs.
   - Confirm the current `productCode` activates exactly one Provider.
   - In Debug, confirm switching products requires confirmation, pauses active
     collection, disconnects the old Provider, preserves Room history, and does
     not let the old Provider reconnect. Confirm the selector is absent in Release.
   - For RWFit, build with
     `-Prehealth.debug.wearable.product.code=RH-RW-P01`, then record model,
     firmware and capability output per `Android-apk/docs/wearable/RWFIT_DEVICE_QA.md`.
   - For HBand, build with
     `-Prehealth.debug.wearable.product.code=RH-HB-E01`, then follow
     `Android-apk/docs/wearable/HBAND_DEVICE_QA.md`; this row remains pending until
     the purchased wearable capability and accuracy matrix is completed.
   - Before HBand connection, edit the personal profile and select sex, then enter
     age `1..120`, height `50..250 cm`, and weight `10..300 kg`. Confirm saving
     refreshes the profile and allows the HBand Provider to consume the encrypted cache.
   - Confirm the profile response includes an incrementing `version`, BMI is calculated by
     the server, and submitting an older version returns `409` without overwriting newer data.
   - On an upgraded environment, verify profile lists and the latest interview match their
     pre-migration JSON values after `V20260729_1__normalize_business_records.sql`.
   - Install the full APK after HBand dependency changes and confirm manager initialization
     and BLE connection callbacks do not report missing `WatchOpImpl`, `OnWatchCallback`,
     `McuMgrBleTransport`, Nordic scanner classes, or `libnative-lib.so`.

5. Manual measurement
   - Open the data page before connecting a device. Confirm heart rate, SpO2, BP,
     HRV, blood glucose, stress, MET, ECG, blood/body component, sleep, steps, and activity cards remain
     visible with `--` where no real record exists. Unsupported actions remain visible
     but disabled; there is no “查看全部” interaction.
   - Trigger only metrics advertised by the active Provider. RWFit manual measure
     currently supports HR, SpO2 and HRV; BP/temperature/stress are not requested.
     HBand `RH-HB-E01` manual measure supports HR, SpO2, HRV, BP,
     blood glucose, stress, MET, ECG, blood component, and body composition. The purchased
     MT116 advertises HRV/stress/MET direct switches but rejects all three direct commands;
     HRV/stress therefore prioritize device `miniCheckup`, while MET uses a latest-history
     “获取” action. Steps, sleep, and activity are sync-only.
   - On MT116, tap HRV, pressure, and MET. Confirm HRV/pressure use mini-checkup or real history,
     MET only reads real history, `HBandMetricFlow` reports the selected route, no SDK unsupported-feature
     toast appears, and an absent/zero result creates no Room row.
   - Start ECG from both the data card and the single-lead detail page, and start body composition
     from its data card. Confirm each flow shows the matching instructions before any SDK command,
     requires continuous opposite-hand contact with the metal electrode and a stable posture,
     starts only after confirmation, and sends no command when cancelled.
   - On the Data tab, tap `同步睡眠、步数与活动`; verify the full device-history sync starts,
     the button is disabled while progress is active, and the sleep/steps/activity cards refresh from Room after completion.
   - For HBand blood components, verify five independent values and device-selected
     units. For body composition, verify all 14 values are independently persisted.
   - For HBand full sync, verify the dedicated sleep command completes before origin history, total-only
     sleep displays its duration without invented stages, and five-minute
     steps are aggregated per day, and ECG history is attempted before other long reads.
     Capability-gated manual measurement and body-composition history are persisted;
     raw ECG samples stay local. HBand temperature must not appear or be requested.
   - Verify blood-glucose calibration and menstrual-cycle settings are capability
     gated, require explicit input, and do not create `ring_measurements` rows.
   - Confirm each successful result is written to Room before any upload attempt.
   - For HBand ECG, confirm the local signal chunk is never included in the
     telemetry upload payload; only the non-diagnostic average-HR summary may sync.
     Open the single-lead detail page and verify live waveform/progress/contact guidance,
     then select recent history records. New rows must be calibrated `FLOAT32_LE` mV
     with gain/lead/sample metadata; migrated `INT32_LE` rows must remain relative-only.
     Label I/V1 only when returned by the device, identify the view as portable single-lead
     rather than 12-lead ECG, suppress SDK disease-risk diagnosis, and show the mandatory
     “仅供健康参考，不能替代医疗诊断” warning.
   - Confirm unsupported metrics fail with safe UI text.

6. Background collection
   - Start B1 background collection through service/ViewModel API or approved debug path.
   - Confirm foreground notification appears with Stop action.
   - Put app in background and wait at least one 15 minute interval.
   - Confirm no tight loop and no duplicate collection while foreground sync is active.
   - Kill and reopen the process, then confirm the active Provider reconnects
     only its bound address. For HBand, verify encrypted real demographics are
     restored without a network request or Demo fallback.
   - Log out while collection is active and confirm the service stops and the
     device disconnects.

7. Room persistence
   - Inspect local Room tables:
     - `ring_measurements`
     - `ring_sleep_sessions`
     - `ring_activities`
     - `ring_signal_chunks`
   - Confirm collected data persists across app restart.
   - Confirm Room is the first persistence layer.

8. Feature extraction
   - Generate a CVD vector from local profile plus Room data.
   - Confirm all 16 fields are present in the contract.
   - Confirm nullable labs remain null and are marked `MISSING`.
   - Confirm `featureQuality` is keyed by snake_case field names.

9. Backend feature evaluation
   - Run backend E1 and model-service F1.
   - Configure Android base URL for emulator or physical device LAN.
   - Submit feature evaluation through `POST /rehealth/mobile/features/evaluate`.
   - Confirm model-service errors surface an unavailable state without synthetic risk output and do not block BLE collection.

10. Model-service risk result
   - Confirm response includes `risk_score`, `risk_level`, `feature_contributions`, `model_version`, `is_mock`, `missing_fields`, `quality_warnings`, and `summary`.
   - Confirm Android/backend map snake_case response fields to camelCase DTO properties where needed.
   - Confirm `is_mock=true` is visible and not described as production model output.

11. Intervention retrieval
    - Call `POST /v1/cvd/intervention/generate` through backend support endpoint or model-service directly.
    - Confirm intervention text is conservative wellness support only.
    - Confirm `medical_disclaimer` is present.

12. Feedback submission
    - Submit `POST /rehealth/mobile/interventions/{id}/feedback`.
    - Confirm E1 returns explicit software persistence-pending status.
    - Confirm no raw health data, phone number, token, or identifier is logged.

13. Offline, no backend, no model-service
    - Disable network.
    - Stop backend.
    - Stop model-service.
    - Confirm BLE/manual/background collection continues locally.
    - Confirm feature evaluation reports fallback mode and no data loss.

14. Bluetooth off
    - Turn Bluetooth off while background collection is active.
    - Confirm notification reports paused/off state.
    - Confirm collection retries later and does not crash.

15. Permission denied
    - Deny BLE permission and start collection.
    - Confirm service reports permission required and does not attempt BLE operations.

16. App killed
    - Kill app process while background collection is active.
    - Reopen app.
    - Confirm WorkManager recovery is scheduled and no duplicate aggressive loops appear.

17. Lock screen
    - Lock device during active background collection.
    - Wait at least one collection interval.
    - Confirm records are persisted locally after unlock.

18. Reboot
    - Reboot device after enabling background collection.
    - Confirm current B1 limitation: no boot receiver is documented, so collection is not release-approved across reboot until explicitly implemented or product accepts manual restart.

19. Duplicate collection prevention
    - Start foreground/manual sync while background service interval is due.
    - Confirm background cycle skips when `RingConnectionState.SYNCING`.
    - Confirm Room primary keys/on-conflict behavior avoid duplicate latest rows.

20. Authenticated AI health chat
   - Put the DeepSeek key only in ignored `model-service/config/ai-chat.local.yml`.
   - Confirm provider `https://api.deepseek.com` and configured model `deepseek-v4-flash`.
   - Log in through `/sys/mLogin`, then send a message through `POST /rehealth/mobile/agent/messages`.
   - Confirm `status=ok`, `model_version=deepseek-v4-flash`, `provider=configured`, `is_demo=false`, and a non-empty `medical_disclaimer`.
   - Ask for a diagnosis or medication prescription and confirm the response is `safety_refusal`.
   - Confirm the API key, access token, prompt, and authorized health context are absent from Git status and logs.

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
