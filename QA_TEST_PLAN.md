# ReHealth MVP QA Test Plan

## Viomi cloud-watch QA

1. In both Debug and Release, select the single user-facing “云米” device type (covering S8/S9/GS20/GS17/A67/K9L) and verify the page shows IMEI binding without Bluetooth permission. Debug merged assets must retain the real catalog in addition to its Mock-only incremental catalog.
2. An IMEI outside the configured Viomi account must fail binding and remain disconnected.
3. A valid IMEI must create only a hashed server-side device identity; logs must contain no IMEI, AppKey, AccessToken, or raw health data.
4. Sync the last seven days and verify heart rate, systolic/diastolic pressure, blood oxygen, and UTC timestamps; Room changes only after backend persistence succeeds.
5. Repeating a window must be idempotent; `NO_NEW_DATA` is successful.
6. With network or hardware persistence unavailable, no new Viomi Room record is written; retry succeeds after recovery.
7. Verify `viomi_cloud` records never create a second `/measurements/batch` upload.

Last reviewed: 2026-08-04
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
$env:REHEALTH_RELEASE_API_BASE_URL="https://api.example.com/jeecg-boot/"
.\gradlew.bat verifyReleaseConfiguration lintRelease assembleRelease
.\gradlew.bat verifyPublishConfiguration
cd ..
```

Release 产物还必须执行 APK 内容审计：合并商品资源只允许 HBand/Viomi Cloud；不得包含
Mock 商品、全链路演练文案、`synthetic_qa`、Debug Factor16 版本、测试码、占位地址或
Provider API Key。Manifest 必须为 `debuggable=false`、`usesCleartextTraffic=false`，且不得
声明 `QUERY_ALL_PACKAGES` 或尚未接线的 Health Connect 写权限；最后使用 `apksigner verify`
校验正式签名并记录 SHA-256。APK/AAB 上传证书必须匹配发布清单批准的指纹；生产 API 与
重定向不得从 HTTPS 降级到 HTTP。

```powershell
mvn -f backend/contracts/telemetry/pom.xml test
mvn -f backend/device-service/pom.xml test
mvn -f backend/jeecg-boot/pom.xml -pl jeecg-boot-module/jeecg-module-rehealth -am -DskipTests=false test
```

```powershell
python -m pytest model-service
python -m compileall model-service/app
python backend/contracts/scripts/validate_contracts.py
python backend/qa/rehealth_stack_gate.py topology --compose backend/deploy/rehealth/docker-compose.yml --profiles staging,production --report topology.json
git diff --check
```

## 保险风险分层 QA

1. 使用两个保险租户及机构管理员、部门经理、运营员、查看员账号，确认风险列表、筛选选项、导出和批量激励都只覆盖当前账号的 `rehealth_insurance_subject_manager` 负责关系。
2. 分别组合风险等级、渠道、年龄段和关键词，核对列表记录、总数、分页及导出 CSV 完全一致；切换租户后渠道选项不得残留上一租户数据。
3. 导出超过 10000 条时必须拒绝并提示缩小范围；姓名或渠道以 `= + - @` 开头时，CSV 不得被电子表格解释为公式。
4. 选择 1、100、101 人验证批量上限；其中任一对象无效、跨租户或不在当前负责人范围时，不得写入任何行动。
5. 机构管理员、部门经理和运营员可创建激励行动并看到审计记录；查看员、分析员和审计员的批量按钮不可用，直接请求接口返回 403。

## 保险机构计划版本 QA

1. 在同一租户、同一负责对象下创建草稿并多次编辑，确认只有草稿内容变化，`lock_version` 每次加一；使用旧 `expected_lock_version` 返回 409 且不产生部分写入。
2. 发布版本 1 后直接调用草稿更新必须返回 409；从版本 1 克隆版本 2 草稿时，版本号递增且同一业务项目保留 `logical_item_id`，版本内 `item_id` 必须不同。
3. 发布版本 2 时确认版本 1 的 `effective_to` 等于版本 2 的 `effective_from`；旧版本在该时间之后的 `scheduled` 任务变为 `cancelled/superseded_by_revision`，不删除历史版本或历史任务。
4. 放弃草稿和撤回发布版本都要求原因并写 `rehealth_care_plan_audit_event`；审计只保存操作者、动作、内容哈希和原因，不复制计划正文或健康数据。
5. 查看员、分析员和审计员只能使用 `care-plan:view`；运营员可编辑草稿但不能发布；机构管理员和部门经理可发布/撤回。跨租户、未负责或授权已撤销的对象均返回 403/404。
6. 保险计划提交 `medication`、`diagnosis` 或 `treatment` 类项目必须拒绝；生活方式、提醒、教育、监测和跟进类项目可保存。
7. 机构发布不自动修改旧 App `insurance_plan_binding`；App 归因页通过版本化计划聚合接口读取生效版本，旧绑定与新版任务反馈必须互不串线。
8. 分别用 `daily`、`weekly`、`once` 规则核对滚动 28 日任务展开、版本生效边界和稳定 `occurrence_id`；未知规则只显示不支持，不得虚构任务。
9. 在归因页核对机构、版本、今日任务和 28 日依从性；完成计 100%、部分完成 50%、跳过计 0、不适用排除，缺失的已到期任务计 0，无有效分母显示暂无数据。
10. 断网提交四类反馈后确认 Room v19 保留 `occurrence_id` 并显示待同步；恢复网络后以本地反馈 ID 幂等上传，同一请求重试不得重复计分，跨用户、跨租户和撤销授权必须拒绝。

## Manual Android QA

1. Android install/onboarding
   - Install `app/build/outputs/apk/debug/app-debug.apk`.
   - Launch app, request a registration SMS code, and confirm `/sys/registerSms` reaches
     the registration controller without Jeecg `X-Sign`/`X-Timestamp` headers or a
     “请求参数不完整” response. Confirm Redis phone/IP quotas still reject repeated abuse.
   - With local `JEECG_SMS_DEV_MODE=true`, confirm a successful response leaves the code
     field empty; manually enter `123456` to complete registration and auto-login. A failed
     request must also leave the code field unchanged.
   - Tap “注册并登录” with an incomplete/invalid form and with the agreement unchecked.
     Confirm the button remains tappable and shows the corresponding form/agreement hint;
     during an in-flight registration request it is disabled and cannot submit twice.
   - In staging, set `JEECG_SMS_DEV_MODE=false` and `JEECG_SMS_DYPNS_ENABLED=true`, mount
     the dedicated RAM secret files, and configure the exact gifted sign plus login/register
     template `100001`. Confirm the test phone receives a six-digit code whose message says it
     is valid for five minutes; a wrong code returns failure even when the provider API call itself
     returns successfully, while the correct code produces `VerifyResult=PASS`, creates one account,
     and auto-logs in. Confirm Redis contains only hashed registration session/cooldown/rate/lock
     keys and no production code, and that neither the full phone, code, nor AccessKey appears in logs.
     Request again after 60 seconds and confirm the new code overwrites the old code. Repeat with one
     required value missing and confirm the endpoint fails without falling back to the fixed code,
     OSS credentials, standard `Dysmsapi`, or Jeecg's legacy sign/template.
   - Save a personal profile and complete a health interview, log out, then log in again.
     Confirm the profile, typed health-history fields, latest interview baseline and focus areas
     are queried and displayed without requiring another edit. Make the risk/model endpoint
     unavailable and confirm profile/history reading still succeeds.
   - Clear the Debug app's local Room data while keeping the server-side account telemetry,
     then log in. Confirm exactly one authenticated `GET /measurements/recent?limit=200` is made
     for that login token before profile refresh, returned measurement/sleep/activity rows are
     restored with stable IDs, and the Data page recomputes RHI with non-zero valid days. Repeat
     profile refresh in the same session and confirm it does not issue another restore request.
     Repeat login with the backend unavailable and confirm Home still opens, BLE collection remains
     available, and a later foreground refresh can retry the restore.
   - Confirm completing the first health interview enters the main screen directly without
     forcing device setup. Open “我的 > 设备绑定” and confirm wearable binding remains available.
     Log out and back in during the same app process and confirm the completed interview is not
     shown again.
   - Register a second account and confirm only that new account receives the health initial
     interview. Log into a pre-existing account with no local onboarding marker and confirm it
     enters Home directly. Interrupt a new account's interview, log in again, and confirm it resumes.
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
   - Log out and log in again. Confirm Home starts a fresh active conversation while history
     remains selectable. Send enough messages to scroll; confirm all current messages are
     reachable and the large mascot/greeting collapses while scrolling. Ask “我是谁” after
     saving a nickname and confirm the server-authorized assistant context contains that name.
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
   must retain period selection, improvement summary, activity, 16-factor groups and
     intervention plan. Model must retain its existing compact risk/input cards.
   - In Attribution, confirm “健康改善得分” shows RHI-100 without “百分点”, and verify
     that a healthier input direction raises the score. Switching 7/30/90 days must recompute
     the signed difference from the earliest valid RHI inside that same window and redraw the
     chart from the matching history. Missing data must not be replaced with a normal score.
   - Confirm the right-side `RDI-16 风险指数` is the average of confirmed, persisted RDI-16
     scores inside the selected 7/30/90-day window. Switching periods quickly must not display
     PIAS or a result from the previously selected period.
   - Open “我的 > 编辑健康与归因指标”. Confirm sedentary hours, waist, 最大摄氧量,
     糖化血红蛋白 and 估算肾小球滤过率 can be saved and restored after process restart. Clearing any field must
     persist `NULL`/missing and lower confidence instead of inserting a normal default.
   - Confirm “编辑个人资料” and “健康与归因指标” use the app's white rounded dialog,
     dark title, mint focused input border and mint primary action. The health indicator dialog
     must not expose internal contribution-weight calculation text.
   - Change age, gender, BMI or a history/lifestyle field in the typed profile and save.
     Open Attribution and confirm the matching factor value updates without a device sync,
     then confirm a new feature evaluation is requested. Repeat with confirmed cuff BP or
     dated lab values and verify those factors update from the new Room snapshot.
   - Confirm RHI accepts SBP/DBP only after the user confirms a valid 3–7 day upper-arm cuff
     mean. Cuffless ring BP remains visible in Data but does not change RHI.
   - Confirm a hospital lab requires at least one value, a valid report date and explicit
     confirmation. Verify all five values are entered as mmol/L, and stale reports shrink
     confidence rather than being silently refreshed or silently unit-converted.
   - Open Data and confirm the RDI-16 risk card displays `riskScore × 100` with one decimal only
     for a reachable, finite, in-range `isMock=false` response from the existing 16-feature
     evaluation path. Mock, failed, invalid, or absent results must
     display `--`; no local fallback risk may be invented.
   - Confirm the Data health-index ring is not a fixed value: Today displays only the current
     calendar day's valid RHI; 7/30/90 days display the valid-day median for that calendar
     window with 3/7/14-day minimum coverage. Fewer than three valid days in the 7-day view
     must show accumulation instead of copying Today; the arc must follow the selected score.
   - Open “我的” and confirm the avatar has no bottom-right camera badge. Tap the avatar and
     select an image through the Android system picker.
     Confirm the preview updates, survives app restart and same-user re-login, and is not visible
     to another user. Verify no avatar upload request is sent and no new media permission is asked.
   - On Home, tap “拍照记录”, grant camera permission, and confirm the system camera writes to an
     app-private `FileProvider` URI. Cancel once and confirm no upload or record is created. Capture
     one meal and one text document; confirm upload progress is visible and FOOD/OCR results appear
     in “今日行为记录” on both Home and Data with the correct local time. Confirm a FOOD result with
     valid estimated calories also appears exactly once in the current user's “今日餐食记录”, uses
     the local-time meal slot, and enters the existing durable diet queue; OCR/OTHER must not. Restart
     or sign out/in, refresh today's behavior records, and confirm the FOOD meal is restored exactly once.
   - On a MIUI device, capture immediately after the camera opens and confirm the app waits for the
     private file write to stabilize before decoding. Repeat after an Activity recreation; neither
     case may show “照片读取失败” for a valid non-empty JPEG. Confirm high-resolution input is sampled
     before scaling and does not cause an out-of-memory crash.
   - Confirm nutrition values are labeled as estimates, the complete OCR text is present in the
     returned record, and no raw photo, provider key, access token, or image base64 appears in Room,
     `software_db`, logcat, or JeecgBoot logs. Confirm the temporary camera file is removed.
   - Reuse the same `requestId` and expect the existing owner-scoped record without a second model
     call. Log in as another user/tenant and confirm the first user's record is absent. Disable the
     network or provider and confirm a controlled error is shown without a fake behavior record.
     Also confirm the previous account's meal list, RHI/RDI summary, measurements, sleep, activity,
     ECG, profile, and risk values are not visible while the new account is loading.
   - Delay a valid vision response beyond the shared 20-second API read timeout but within the
     configured 75-second provider timeout. Confirm photo analysis continues and persists exactly
     one record. Delay it past the provider timeout and confirm the app shows “图片识别超时” instead
     of “网络连接失败”, creates no placeholder record, and JeecgBoot does not repeat the model call.
   - Open the Data tab and confirm “今日” is selected. Sync a sleep session that starts before
     midnight and ends today; verify today's duration equals valid stage totals (or elapsed time
     only when stages are absent). Verify 7/30-day risk and health index use only confirmed daily
     results and show their valid-day count.
   - Confirm the device-data notice appears before the metric sections and centrally states which
     values are device estimates plus the medical disclaimer. Start blood glucose, blood component,
     and body composition measurements; while the action reads “测量中”, the action and footer status
     must remain on one line without changing the card height or clipping the action label.
   - For HBand HRV/stress/MET, verify capability flags alone never reveal a card. A card appears
     only after Room contains a valid real-Provider value: HRV/MET `> 0`, stress `1..100`.
     History-only HRV/stress cards have no measure action; MET never has a real-time action.
     Missing, zero, invalid, mock, or synthetic values keep the card hidden.
   - Ask for a diagnosis/prescription and enter urgent chest-pain/breathing wording.
     Verify the Java safety policy refuses diagnosis and escalates urgent care, while every
     answer displays “仅供健康参考，不能替代医疗诊断”.
   - With the default `REHEALTH_HEALTH_AGENT_ENGINE=langchain4j`, ask “我是谁/我叫什么”. Confirm
     the reply uses the latest authenticated profile nickname. Attempt to mention another user ID
     and confirm no other profile is returned. Run `model-service` once only as an explicit rollback
     check; the public Android endpoint and response fields must stay stable.

4. Ring permission
   - On Android 12+, deny and then grant `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT`.
   - On Android 13+, verify notification permission behavior before enabling background collection.
   - Confirm denial pauses collection safely.

5. Ring scan/connect
   - Turn Bluetooth on.
   - Scan from device binding screen.
   - Confirm the Release page offers exactly “HBand（MT116 蓝牙）” and “云米（IMEI 云端）”.
   - Select HBand and connect the MT116 chosen by the current `productCode`.
   - Confirm a fresh Release install defaults to HBand; an upgraded MRD/RWFit selection migrates
     to HBand, while an existing Viomi binding remains selected.
   - Confirm connection state updates, the active binding survives app restart,
     and no duplicate scan/connect loop is created.
   - After clearing app data, start background collection before binding and
     confirm no fixed-address connection, automatic scan, or fabricated row occurs.
   - Confirm the current `productCode` activates exactly one Provider.
   - In Debug, confirm switching products requires confirmation, pauses active
     collection, disconnects the old Provider, preserves Room history, and does
     not let the old Provider reconnect. Confirm the selector is absent in Release.
   - Sync overlapping local and cloud activity snapshots for the current local calendar day and one
     row for the previous day. Confirm “我的 > 每日步数” displays the current-day maximum cumulative
     watch total (not the sum of duplicate rows) and does not prefer a stale standalone `STEPS` measurement.
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
     blood glucose, ECG, blood/body component, sleep, steps, and activity cards remain visible
     with `--` where no real record exists. HRV, stress, and MET are absent until a valid real
     value exists. Unsupported actions on the remaining cards are visible but disabled; there
     is no “查看全部” interaction.
   - Trigger only metrics advertised by the active Provider. RWFit manual measure
     currently supports HR, SpO2 and HRV; BP/temperature/stress are not requested.
      HBand `RH-HB-E01` manual measure supports HR, SpO2, BP, blood glucose, ECG,
      blood component, and body composition. HRV/stress are additionally measurable only through
      available package-4 Mini Checkup. MET, steps, sleep, and activity are sync-only.
   - Confirm HRV/pressure prefer Mini Checkup or real history and MET reads real history only,
      even when the dedicated capability flags are true. The fixed SDK retains the dedicated
      HRV/MET APIs for compatibility/diagnostics, but the product page must not issue them.
      The MT116 all-zero `unknown action` evidence covers this policy;
      `HBandMetricFlow` must report the route without device identifiers or raw health values.
   - Start ECG from both the data card and the single-lead detail page, and start body composition
     from its data card. Confirm each flow shows the matching instructions before any SDK command,
     requires continuous opposite-hand contact with the metal electrode and a stable posture,
     starts only after confirmation, and sends no command when cancelled.
   - Before connecting, confirm `同步睡眠、步数与活动` is disabled and neither entering “我的” nor
     the in-process automatic cycle reconnects/sends BLE sync commands. After connecting, tap it and
     verify a daily sleep/steps/activity sync starts, progress advances monotonically from real SDK
     phases, and cards refresh from Room after completion. On HBand, the first/gap sync may read origin
     history; a second recent sync uses the overlap window and skips the long origin-history command.
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
   - For local `admin` RHI QA, run
     `backend/deploy/rehealth/scripts/seed-admin-rhi-test-data.ps1`. Confirm it
     reports 1,180 measurements, 118 sleep sessions, 118 activities, 118
     distinct history days, seven confirmed cuff days, and confirmed labs.
     Confirm the fixed TimescaleDB batch can be re-seeded without increasing
     these counts and all synthetic rows remain marked `LOCAL_TEST_SEED`.
   - Use the seeded inputs to exercise local or remote preview calculation, but
     do not interpret the result as a disease probability or an authoritative
     cloud RHI snapshot. The current backend still expects the client/feature
     pipeline to assemble the 32-field RHI request.
   - In “个人风险趋势”, confirm the blue solid line uses only confirmed RDI-16 history
     from the selected 7/30/90-day window. Until the RDI-16 contract supplies native scenario
     fields, “维持现状”, “执行计划”, “预计降低” and “95% 区间” must remain visibly
     unavailable and must not reuse PIAS values. Confirm the three largest trusted Room RDI
     impact factors appear below the three scenario data cells inside the same card. The card
     must state that scenario simulation is not a future disease probability.

12. Intervention retrieval
    - For local `admin` API QA, run
      `backend/deploy/rehealth/scripts/seed-admin-intervention-test-data.ps1` first.
      Confirm its software and hardware row-count checks pass, retain tenant `1000`
      from the authenticated login, and treat the seeded `is_mock=true` risk and
      `LOCAL_TEST_SEED` telemetry as test-only inputs.
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
    - With no persisted plan, confirm Android shows “生成个性化干预计划” and does not
      silently POST during profile refresh. Tap once, confirm the button is disabled with a
      progress state, and verify a successful snake_case or deployed camelCase response is
      rendered immediately. On failure, verify the controlled server message remains visible
      and the button can retry. With an existing plan, confirm the attribution page defaults to
      an expanded 01–05 numbered action list, shows “围绕 16 项健康输入安排下一步行动” and
      “已展开”, and the full-width “收起干预计划” button changes the card to “已收起” with an
      “展开干预计划” action. Existing plans must not show an extra regenerate button.
    - With DeepSeek v4, confirm the structured intervention call disables thinking mode,
      returns non-empty JSON `content`, and a validation retry still persists at most one plan.
    - In the Android attribution page, record a meal while offline and confirm it
      appears immediately from Room with a local-only status. Restore a real device
      binding and network, then confirm one stable `telemetry-v2` queue item is created,
      WorkManager retries safely, and the row changes to synced only after durable
      server persistence. Reopen the app and confirm the current user's meal remains;
      another authenticated user must not see it.

12a. Manual RHI health archive persistence
    - Log in as `admin`, edit sedentary hours and waist in “我的 > 健康档案”, then save.
    - Confirm Room updates immediately and one stable `rhi-manual:<userId>` queue row is created.
    - Confirm `PUT /rehealth/mobile/rhi/manual-inputs`, subsequent GET, and
      `rehealth_rhi_manual_health_input` contain the same nullable values and `updatedAt`.
    - Save offline, restore the network, and confirm WorkManager retries without losing local data.
    - Send an older `updatedAt` from another client and confirm it cannot overwrite the newer row.

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
