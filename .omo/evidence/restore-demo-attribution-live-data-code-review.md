# Final Contract Code Review

## Verdict

- Overall: **PASS for code submission; NOT production-release ready**.
- `codeQualityStatus`: **WATCH**
- `recommendation`: **APPROVE**
- `reportPath`: `.omo/evidence/restore-demo-attribution-live-data-code-review.md`
- `blockers`: **None for code submission.** The two untracked regression tests listed below must be included in the submission commit.
- Attempt lookup: `omo ulw-loop status --json` was unavailable because `omo` is not on `PATH`. `.omo/boulder.json` identifies a normal active work plan, not an ULW-loop attempt, so this report uses the required fallback path.

## Reviewed Scope

- Stable implementation commit: `bda9dde3cab64b4621f61513c2c7988f9d7b4098`.
- Pending blocker-closure tests:
  - `Android-apk/app/src/test/java/com/rehealth/genie/network/ReHealthMobileApiRouteContractTest.kt`
  - `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/test/java/org/jeecg/modules/rehealth/mobile/dto/CvdFeatureVectorDtoJacksonBindingTest.java`
- Contract focus: Retrofit context-path preservation, CVD/attribution Jackson/Fastjson/Moshi mapping, Room v3-to-v4 migration, authenticated attribution, telemetry consumer, and BLE scan/log privacy.

## CRITICAL

None.

## HIGH

None. The previous two HIGH contract blockers are closed:

1. All backend-relative mobile Retrofit annotations are relative and preserve `/jeecg-boot/`, including evaluate/latest/intervention/feedback/measurement/attribution at `Android-apk/app/src/main/java/com/rehealth/genie/network/ReHealthApi.kt:32`.
2. All six affected CVD fields now have Jackson canonical snake_case properties plus camelCase aliases, while retaining Fastjson outbound names, at `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/java/org/jeecg/modules/rehealth/mobile/dto/CvdFeatureVectorDto.java:23`.

## MEDIUM

### Authenticated attribution test is implementation-oriented rather than an HTTP auth test

- `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/test/java/org/jeecg/modules/rehealth/mobile/controller/ReHealthMobileControllerAttributionContractTest.java:15` proves the route exists and lacks `@IgnoreAuth`, but it does not issue an unauthenticated request and assert rejection.
- Production remains correctly protected: attribution has no `@IgnoreAuth` at `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/java/org/jeecg/modules/rehealth/mobile/controller/ReHealthMobileController.java:125`, and the Shiro fallback is `/** -> jwt` at `backend/jeecg-boot/jeecg-boot-base-core/src/main/java/org/jeecg/config/shiro/ShiroConfig.java:185`.
- This is test-quality debt, not a correctness blocker for this submission. Add a real missing-token/invalid-token integration scenario before production release.

## LOW

- `Android-apk/app/src/main/java/com/rehealth/genie/data/AppDatabase.kt:208` still enables broad destructive fallback for database versions without a registered path. The required 3-to-4 path is explicitly registered and was runtime-verified, so the stated upgrade is safe; remove or narrowly justify destructive fallback before production.
- The long comment in `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/main/java/org/jeecg/modules/rehealth/mobile/dto/CvdFeatureVectorDto.java:16` still over-emphasizes Fastjson for inbound parsing even though Spring inbound parsing is Jackson. It is misleading but does not change behavior.

## Contract Results

- **Retrofit canonical mobile routes: PASS.** Fresh MockWebServer test verified `/jeecg-boot/rehealth/mobile/features/evaluate`, `/risk/latest`, `/interventions/today`, feedback, attribution, and measurement paths with `X-Access-Token`.
- **CVD Jackson boundary: PASS.** Fresh WSL Maven run compiled the reactor and ran two DTO binding tests: 2 tests, 0 failures/errors/skips. Snake_case and camelCase deserialize; output is canonical snake_case.
- **Attribution mapping: PASS.** Request DTOs map `risk_history`, `forecast_days`, `Y`, and `Z`; the rich PIAS response maps forecast/current state/ATT/status metadata through backend Fastjson, Spring Jackson, Android Moshi, and `RemotePhmService`.
- **Room migration: PASS for v3-to-v4.** `Migration3To4` creates/repairs the risk-history table and index at `Android-apk/app/src/main/java/com/rehealth/genie/data/AppDatabase.kt:62`; emulator evidence shows database version 4 and populated `cvd_risk_history` without clearing app data.
- **Authenticated attribution: PASS in production flow.** Android routes through `AuthenticatedApiClient.attributeIndividual` at `Android-apk/app/src/main/java/com/rehealth/genie/network/AuthenticatedApiClient.kt:60`; backend route is JWT-protected by default. No active direct Android PIAS client remains.
- **Telemetry consumer: PASS.** `MeasurementSyncWorker` drains pending measurement items at `Android-apk/app/src/main/java/com/rehealth/genie/work/MeasurementSyncWorker.kt:50`; `SyncRepository` parses the typed batch, handles durable acceptance, dead letters permanent failures, retries transient failures, and pauses on 401 at `Android-apk/app/src/main/java/com/rehealth/genie/data/sync/SyncRepository.kt:46`.
- **BLE scan/log: PASS at code level.** Scan uses `BluetoothLeScanner` callbacks and handles permission/off/failure states at `Android-apk/app/src/main/java/com/rehealth/genie/ring/mrd/MrdBleRingRepository.kt:57`; device address, raw advertisement, raw packets, parsed JSON, UUID lists, and exception messages were removed from logs. Payload logs expose only counts/types/status.

## Verification

- Fresh Android focused gate: `:app:testDebugUnitTest` for route, attribution, and measurement wire tests with `--rerun-tasks` — **BUILD SUCCESSFUL**, 26 tasks executed.
- Fresh backend focused gate under WSL: `mvn -pl jeecg-boot-module/jeecg-module-rehealth -am -Dtest=CvdFeatureVectorDtoJacksonBindingTest -Dsurefire.failIfNoSpecifiedTests=false test` — **BUILD SUCCESS**, 2 tests passed.
- Existing full Android evidence was inspected, not rerun: test, lint, debug APK, and release APK gates report PASS in `.omo/evidence/final-android-gate/android-full-gate-summary.txt`.
- Room runtime evidence inspected: `.omo/evidence/restore-demo-attribution-live-data-executor/fixed/runtime-verification.txt` and `.omo/evidence/final-emulator-ui/dbinfo-room.txt`.
- Working tree has no tracked diff; the two blocker-closure tests and review/evidence artifacts are untracked.

## Skill-Perspective Check

- `remove-ai-slops`: **ran**. The two new blocker-closure tests exercise observable HTTP and serialization behavior; they are not deletion-only, removal-verification, tautological, prompt-text, or constant-mirroring tests. No unnecessary production extraction/parsing/normalization was added. The reflection-only auth test is the one implementation-oriented weakness and is recorded as MEDIUM.
- `programming`: **ran**. DTOs parse external JSON into typed boundary objects and no untyped escape hatch was introduced. The route test combines four wire actions in one test rather than one `When` per test, but it still distinguishes every contract path and remains useful; splitting it is optional test hygiene, not a blocker.
- `rehealth-android-mvp`: **ran**. Review preserved local-first BLE/Room collection, authenticated backend boundaries, model inference outside Android/Jeecg, and conservative release claims.

## Production Release Blockers (Not Submission Blockers)

1. **DeepSeek secret boundary:** `Android-apk/app/build.gradle.kts:45` can embed `DEEPSEEK_API_KEY` in the APK and `Android-apk/app/src/main/java/com/rehealth/genie/chat/DeepSeekClient.kt:58` calls DeepSeek directly. Move this behind an authenticated backend before release.
2. **Health-data consent:** login/register privacy checkboxes exist, but explicit collection/upload consent plus withdrawal/enforcement for telemetry and CVD history is not evidenced.
3. **Telemetry producer:** the durable queue consumer is implemented, but no production BLE/Room-to-`UploadQueueEntity(kind="measurement_batch")` producer was found. Collection therefore does not yet automatically feed upload.
4. **Physical MR11 QA:** scanner callbacks, permission revocation, reconnect stability, packet parsing, battery use, and long-running collection still require a real MR11 and Android 13+ device.
5. **Production backend configuration:** dev config points model/attribution services to `127.0.0.1:8000`, hardware DB defaults disabled, and the release environment needs HTTPS public base URLs, secrets, datasource enablement, migrations, and end-to-end validation.

## Final Decision

**PASS / APPROVE for submission once the two untracked regression tests are included.** No CRITICAL or HIGH code finding remains. Production release remains blocked by the five operational/product gaps above.
