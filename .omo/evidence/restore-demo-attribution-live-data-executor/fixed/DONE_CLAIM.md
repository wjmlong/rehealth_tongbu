# Executor DoneClaim

Date: 2026-07-22

Status: implementation and executor verification complete; parent release gate still blocked by the items under Remaining blockers.

## Changed files

- `Android-apk/DESIGN.md`
- `Android-apk/app/src/main/java/com/rehealth/genie/ui/theme/AttributionTokens.kt`
- `Android-apk/app/src/main/java/com/rehealth/genie/ui/AttributionScreen.kt`
- `Android-apk/app/src/main/java/com/rehealth/genie/ui/AttributionDataProvenance.kt`
- `Android-apk/app/src/main/java/com/rehealth/genie/ui/AttributionUiModels.kt`
- `Android-apk/app/src/main/java/com/rehealth/genie/ui/ReHealthApp.kt`
- `Android-apk/app/src/test/java/com/rehealth/genie/ui/AttributionDataProvenanceTest.kt`
- `Android-apk/app/src/test/java/com/rehealth/genie/ui/AttributionUiStateTest.kt`
- `Android-apk/app/src/main/java/com/rehealth/genie/data/AppDatabase.kt`
- `Android-apk/app/src/main/java/com/rehealth/genie/data/RiskHistoryMigrationSql.kt`
- `Android-apk/app/src/test/java/com/rehealth/genie/data/RiskHistoryMigrationTest.kt`
- `Android-apk/app/src/test/java/com/rehealth/genie/network/PiasApiClientTest.kt`

## Completed behavior and evidence

1. Attribution reference anatomy and token system
   - Scenario: open the attribution tab on the preserved-data 1080x2400 emulator, scroll through all factor groups, and expand factor 1.
   - Invocation: install `Android-apk/app/build/outputs/apk/debug/app-debug.apk` with `adb -s emulator-5554 install -r`, launch `com.rehealth.genie/.MainActivity`, tap the attribution tab, then use ADB swipe/tap plus `uiautomator dump` and `screencap`.
   - Binary observable: stable ranks 1-16, signed returned score or `--`, separate honest absolute-contribution share or `贡献占比 --`, indented progress/evidence anatomy, summary title `健康改善得分`, and intact disclaimer phrase `医学诊断`.
   - Artifacts: `attribution-top.png/xml`, `attribution-middle.png/xml`, `factor-expanded.png/xml`, and `attribution-bottom.png/xml` in this directory.

2. Fallback provenance
   - Scenario: local heuristic and `local-computed` profiles are rejected, while a remote profile with explicit false answers is retained.
   - Invocation: `./gradlew.bat --no-daemon testDebugUnitTest --tests 'com.rehealth.genie.ui.AttributionDataProvenanceTest'` as part of the focused test command.
   - Binary observable: 3 tests, 0 failures, 0 errors; local fallback booleans no longer render as real `否/无` or feed the remote feature-evaluation snapshot.
   - Artifacts: `focused-tests.log` and Gradle XML `TEST-com.rehealth.genie.ui.AttributionDataProvenanceTest.xml`.

3. PIAS accumulating metadata
   - Scenario: 5 recorded days with a 14-day minimum reports 9 remaining days.
   - Invocation: focused `AttributionUiStateTest` Gradle run.
   - Binary observable: `remainingDays == 9`; accumulating copy displays the omitted count.
   - Artifacts: `focused-tests.log` and Gradle XML `TEST-com.rehealth.genie.ui.AttributionUiStateTest.xml`.

4. Room v3-to-v4 migration safety
   - Scenario: exact legacy risk schema is rebuilt and preserved under `__legacy_unscoped__`; canonical schema only adds its index; unknown shape fails closed; missing/existing upload queues are reconciled to the canonical Room shape.
   - Invocation: focused `RiskHistoryMigrationTest` Gradle run, followed by preserved-data APK install/launch.
   - Binary observable: 5 tests, 0 failures, 0 errors; APK install `Success`; `MainActivity` resumed without a crash.
   - Artifacts: `focused-tests.log`, `install.txt`, `launch.txt`, `focus.txt`, and `runtime-verification.txt`.

5. PIAS response parsing
   - Scenario: an accumulating API response preserves `status`, `history_days`, and `min_history_days` in the typed result.
   - Invocation: focused `PiasApiClientTest` Gradle run.
   - Binary observable: 1 test, 0 failures, 0 errors.
   - Artifacts: `focused-tests.log` and Gradle XML `TEST-com.rehealth.genie.network.PiasApiClientTest.xml`.

6. Full build gate
   - Scenario: compile and package both debug and release variants with unit tests and lint.
   - Invocation: `./gradlew.bat --no-daemon testDebugUnitTest lintDebug assembleDebug assembleRelease`.
   - Binary observable: `BUILD SUCCESSFUL in 3m 11s`; debug and release APK tasks completed.
   - Artifact: `final-gradle-verification.log`.

## Remaining blockers

- Direct PIAS client authentication is not fixed in this lane; the parent assigned the backend/API security path elsewhere.
- Legacy risk rows have unknown ownership and are intentionally stored as `__legacy_unscoped__`; authenticated per-user queries cannot surface them until an explicit ownership/mapping policy is approved.
- Emulator contains no real confirmed risk history, Room activity, or server intervention, and mock/seeding is prohibited. Rendered evidence therefore covers the honest same-layout empty state; populated variants are covered only by source and mapper tests.
- No Android instrumentation source set exists. Instrumentation is unavailable and not claimed; see `instrumentation-availability.txt`.
- The final token/provenance revision has executor screenshots but has not received a fresh independent dual visual-review pass after the last changes.

## Scope notes

- No backend/API implementation, navigation, second attribution page, or unrelated UI was changed in this closeout.
- No commit was created in this executor lane; the parent owns commit/push.
