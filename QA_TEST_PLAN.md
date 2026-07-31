# ReHealth MVP QA Test Plan

Last reviewed: 2026-07-30
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
   - Confirm completing the first health interview enters the main screen directly without
     forcing device setup. Open “我的 > 设备绑定” and confirm wearable binding remains available.
     Log out and back in during the same app process and confirm the completed interview is not
     shown again.
   - Confirm no crash on first run and no production medical diagnosis wording.

2. Voice permission
   - Remove microphone permission, open the health interview, and tap the microphone.
     Confirm the app explains the purpose and that recordings are not stored before launching
     the Android permission request. Deny it and verify the app offers system settings while
     text input remains usable; grant it and verify speech recognition starts.
   - From the main Home tab, tap the microphone and confirm it launches the system speech
     recognizer instead of health onboarding. Confirm recognized text returns to the input field
     for review and is not sent until the user explicitly submits it.
   - Confirm completing an interview cannot leave the result page until its Room queue insert
     succeeds. After upload, verify the latest interview is stored in the normalized
     `software_db` interview/answer/baseline/focus tables and can be queried after re-login.
   - Enter `32 岁，身高 168 cm，体重 62 kg` in the basic-profile interview answer.
     After sync, verify those values are merged into `rehealth_patient_profile` without
     clearing an existing name, gender, diagnoses, medication, allergy or history field.

3. Health assistant memory and safety
   - Apply `software-V20260730.1`, enable `REHEALTH_SOFTWARE_DB_ENABLED=true`, and
     send two related questions. Verify the second Java LangChain4j prompt receives the
     bounded prior messages and freshly queried profile/interview/risk/intervention context.
   - Force-stop and reopen the app, then log out and log back into the same account.
     Verify Room shows the latest local messages immediately and
     `GET /rehealth/mobile/agent/conversations/latest` reconciles the server history.
   - Log in as another user/tenant and verify neither Room nor MySQL returns the first
     user's conversation. Reuse a `requestId` with different content and expect `409`.
   - Disable the network while sending: the user message must remain in Room as failed;
     no locally synthesized assistant answer may appear.
   - Send a response containing headings, lists, bold text, inline code, a Markdown link,
     a remote image and raw HTML. Confirm the supported formatting renders on Home and chat,
     while HTML is inert, no remote image is loaded, and no link target opens automatically.
   - Send a question from Home, leave and return to the Home tab, and reopen the app as the same
     user. Confirm the Home preview is sourced from the same latest Room conversation rather
     than a temporary one-turn state.
   - Upgrade an installed v6 database containing two conversations to v7. Confirm both message
     histories remain, the latest conversation is active, and the generated titles are readable.
     From Home, create a conversation, switch between conversations, then verify per-conversation
     delete and clear-all require confirmation. Confirm deleted local conversations do not return
     after latest refresh, while the UI states that authoritative cloud history is unchanged.
   - Open the Model tab and confirm it shows user-facing risk status without endpoint paths,
     request IDs, contribution values, temperature input, or claims that the cloud model runs
     on-device.
   - Upgrade a v7 database containing health, wearable, queue, risk and chat rows to v8.
     Confirm all prior rows remain and the two local RDI tables are created.
   - Open the Attribution and Model tabs and compare with the `fc1f6d5` baseline. Attribution
     must retain period selection, improvement summary, PIAS, activity, 16-factor groups and
     intervention plan. Model must retain its existing compact risk/input cards.
   - In Attribution, confirm “健康改善得分” shows RHI-100 without “百分点”, and verify
     that a healthier input direction raises the score. For 7 days, verify the value is the
     current RHI calculated from recent seven-day valid Room data. For 30/90 days, verify
     the value and chart use the median/history of valid daily RHI values; fewer than 7/14
     valid days must show an accumulating state.
   - Confirm the right-side current clinical risk and PIAS personal-risk trend remain on the
     existing confirmed CVD-16 interface and are separate from RHI. Missing
     wearable data must not be replaced with a normal score, and switching periods quickly
     must not display a result from the previously selected period.
   - Open “我的 > 编辑健康与归因指标”. Confirm sedentary hours, waist, 最大摄氧量,
     糖化血红蛋白 and 估算肾小球滤过率 can be saved and restored after process restart. Clearing any field must
     persist `NULL`/missing and lower confidence instead of inserting a normal default.
   - Confirm “编辑个人资料” and “健康与归因指标” use the app's white rounded dialog,
     dark title, mint focused input border and mint primary action. The health indicator dialog
     must not expose internal contribution-weight calculation text.
   - Confirm RHI accepts SBP/DBP only after the user confirms a valid 3–7 day upper-arm cuff
     mean. Cuffless ring BP remains visible in Data but does not change RHI.
   - Confirm a hospital lab requires at least one value, a valid report date and explicit
     confirmation. Verify all five values are entered as mmol/L, and stale reports shrink
     confidence rather than being silently refreshed or silently unit-converted.
   - Open Data and confirm the RDI-16 risk card displays `riskScore × 100` with one decimal only
     for a reachable, finite, in-range `isMock=false` response from the existing 16-feature
     evaluation path. Mock, failed, invalid, or absent results must
     display `--`; no local fallback risk may be invented.
   - Confirm the Data health-index ring is not a fixed value: Today/7 days display the
     current RHI, 30/90 days display the valid-day median, the arc follows the score, and
     insufficient data displays the RHI accumulating state.
   - Open “我的” and confirm the avatar has no bottom-right camera badge. Tap the avatar and
     select an image through the Android system picker.
     Confirm the preview updates, survives app restart and same-user re-login, and is not visible
     to another user. Verify no avatar upload request is sent and no new media permission is asked.
   - Ask for a diagnosis/prescription and enter urgent chest-pain/breathing wording.
     Verify the Java safety policy refuses diagnosis and escalates urgent care, while every
     answer displays “仅供健康参考，不能替代医疗诊断”.
   - Run once with `REHEALTH_HEALTH_AGENT_ENGINE=model-service` and once with
     `langchain4j`; confirm the public Android endpoint and response fields stay stable.

4. Ring permission
   - On Android 12+, deny and then grant `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT`.
   - On Android 13+, verify notification permission behavior before enabling background collection.
   - Confirm denial pauses collection safely.

5. Ring scan/connect
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
   - Sync two valid activity rows for the current local calendar day and one for the previous
     day. Confirm “我的 > 每日步数” sums only the current-day Room activity rows and does not
     prefer a stale standalone `STEPS` measurement.
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

6. Manual measurement
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

7. Background collection
   - Start B1 background collection through service/ViewModel API or approved debug path.
   - Confirm foreground notification appears with Stop action.
   - Put app in background and wait at least one 15 minute interval.
   - Confirm no tight loop and no duplicate collection while foreground sync is active.
   - Kill and reopen the process, then confirm the active Provider reconnects
     only its bound address. For HBand, verify encrypted real demographics are
     restored without a network request or Demo fallback.
   - Log out while collection is active and confirm the service stops and the
     device disconnects.

8. Room persistence
   - Inspect local Room tables:
     - `ring_measurements`
     - `ring_sleep_sessions`
     - `ring_activities`
     - `ring_signal_chunks`
   - Confirm collected data persists across app restart.
   - Confirm Room is the first persistence layer.

9. Feature extraction
   - Generate a CVD vector from local profile plus Room data.
   - Confirm all 16 fields are present in the contract.
   - Confirm nullable labs remain null and are marked `MISSING`.
   - Confirm `featureQuality` is keyed by snake_case field names.

10. Backend feature evaluation
   - Run backend E1 and model-service F1.
   - Configure Android base URL for emulator or physical device LAN.
   - Submit feature evaluation through `POST /rehealth/mobile/features/evaluate`.
   - Confirm model-service errors surface an unavailable state without synthetic risk output and do not block BLE collection.

11. Model-service risk result
   - Confirm response includes `risk_score`, `risk_level`, `feature_contributions`,
     `factor_contributions`, `factor_contribution_version`, the two 80/20 component
     maps, `model_version`, `is_mock`, `missing_fields`, `quality_warnings`, and `summary`.
   - Confirm the Attribution 16 rows use `factor_contributions`, show the exact vector
     values sent for evaluation, and label the rule as Factor16 rather than RDI16.
   - Expand all 16 rows. Confirm each explanation contains the row's current value
     and each row has a conservative field-specific suggestion. The detail card must
     not display source, rule version, rule-contribution explanation, or 80/20 component text.
     Missing values must remain explicit.
   - Confirm cuffless wearable BP stays on Data but produces missing/low-confidence
     Factor16 BP; only a confirmed 3–7 day upper-arm cuff mean unlocks SBP/DBP.
   - Confirm hospital labs require a report date and explicit source confirmation.
     Without verified control-support trend the 20% component remains exactly zero.
   - Confirm Android/backend map snake_case response fields to camelCase DTO properties where needed.
   - Confirm `is_mock=true` is visible and not described as production model output.
   - In “个人风险趋势”, confirm the solid line uses confirmed RDI-16 history,
     the gray dashed line uses PIAS `forecast_no_action`, the green dashed line
     uses `forecast_with_plan`, and the light area uses the returned confidence
     interval. The card must state that the scenario is not a future disease probability.

12. Intervention retrieval
    - Apply TimescaleDB V4, upload a `telemetry-v2` batch containing today's
      `dietRecords` plus activity/sleep/measurement rows, then call
      `POST /rehealth/mobile/interventions/generate` with only a stable `request_id`.
    - Confirm Jeecg reloads the authenticated user's profile, latest interview,
      latest persisted risk and tenant-scoped Device Service context on every generation;
      client `riskResult`, `featureVector` and `patientContext` must not override them.
    - Confirm the response contains 1–5 ordered `items` with `category`, `title`,
      `action`, `rationale`, `target`, `timing`, `priority` and `evidenceRefs`;
      today’s recorded meal is reflected ahead of older trends.
    - Confirm intervention text is conservative wellness support only, contains no
      diagnosis or medication change, `is_mock=false`, and `medical_disclaimer` is present.
    - Confirm a Device Service/LLM/software_db failure returns controlled failure and
      does not persist or display a fabricated fallback plan.
    - In the Android attribution page, record a meal while offline and confirm it
      appears immediately from Room with a local-only status. Restore a real device
      binding and network, then confirm one stable `telemetry-v2` queue item is created,
      WorkManager retries safely, and the row changes to synced only after durable
      server persistence. Reopen the app and confirm the current user's meal remains;
      another authenticated user must not see it.

13. Feedback submission
    - Submit `POST /rehealth/mobile/interventions/{id}/feedback`.
    - Confirm E1 returns explicit software persistence-pending status.
    - Confirm no raw health data, phone number, token, or identifier is logged.

14. Offline, no backend, no model-service
    - Disable network.
    - Stop backend.
    - Stop model-service.
    - Confirm BLE/manual/background collection continues locally.
    - Confirm feature evaluation reports fallback mode and no data loss.

15. Bluetooth off
    - Turn Bluetooth off while background collection is active.
    - Confirm notification reports paused/off state.
    - Confirm collection retries later and does not crash.

16. Permission denied
    - Deny BLE permission and start collection.
    - Confirm service reports permission required and does not attempt BLE operations.

17. App killed
    - Kill app process while background collection is active.
    - Reopen app.
    - Confirm WorkManager recovery is scheduled and no duplicate aggressive loops appear.

18. Lock screen
    - Lock device during active background collection.
    - Wait at least one collection interval.
    - Confirm records are persisted locally after unlock.

19. Reboot
    - Reboot device after enabling background collection.
    - Confirm current B1 limitation: no boot receiver is documented, so collection is not release-approved across reboot until explicitly implemented or product accepts manual restart.

20. Duplicate collection prevention
    - Start foreground/manual sync while background service interval is due.
    - Confirm background cycle skips when `RingConnectionState.SYNCING`.
    - Confirm Room primary keys/on-conflict behavior avoid duplicate latest rows.

21. Health-agent provider rollback
   - Put the provider key only in the ignored `secrets/provider_credential` file.
   - With `REHEALTH_HEALTH_AGENT_ENGINE=langchain4j`, confirm the configured OpenAI-compatible provider/model and `provider=langchain4j-openai-compatible`.
   - Switch only the engine variable to `model-service`, restart JeecgBoot, and confirm the same mobile endpoint succeeds through the retained Python provider.
   - Confirm the API key, access token, prompt, complete message content, and authorized health context are absent from Git status and production logs.

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
