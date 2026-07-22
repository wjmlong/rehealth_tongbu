# BLE/DeepSeek release log redaction evidence

## Success criteria

- Scenario: inspect production logging in `MrdBleRingRepository.kt`, `VendorRingRepository.kt`, and `DeepSeekClient.kt`.
- Invocation: PowerShell static scan at 2026-07-22 using `rg` plus a line-level sensitive-term filter.
- Binary observable: `STATIC_SCAN_PASS: no raw BLE address/advertising hex/JSON/payload/PPG/token/response/throwable log values in target files`.
- Artifact: this report and `static-scan.txt`.

- Scenario: compile changed Android production sources.
- Invocation: `D:\Android-apk\gradlew.bat :app:compileDebugKotlin` with `JAVA_HOME=D:\Android_Studio\jbr`.
- Binary observable: Gradle `BUILD SUCCESSFUL`; exit code `0`.
- Artifact: `compile-debug-kotlin.txt` (captured command result summary).

- Scenario: focused regression test for safe log summaries.
- Invocation: `D:\Android-apk\gradlew.bat :app:testDebugUnitTest --tests com.rehealth.genie.logging.SafeLogValuesTest`.
- Binary observable: blocked before test execution by KSP incremental cache `NoSuchFileException`; a prior full unit-test attempt was blocked by pre-existing `ReHealthMobileApiAttributionTest.kt:37` unresolved `attributeIndividual`.
- Artifact: `focused-test-blocked.txt`.

## Changed files

- `Android-apk/app/src/main/java/com/rehealth/genie/logging/SafeLogValues.kt`
- `Android-apk/app/src/test/java/com/rehealth/genie/logging/SafeLogValuesTest.kt`
- `Android-apk/app/src/main/java/com/rehealth/genie/ring/mrd/MrdBleRingRepository.kt` (non-scan logging only; concurrent scan worker changes preserved)
- `Android-apk/app/src/main/java/com/rehealth/genie/ring/vendor/VendorRingRepository.kt`
- `Android-apk/app/src/main/java/com/rehealth/genie/chat/DeepSeekClient.kt`
