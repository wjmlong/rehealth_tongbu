# MRD BLE scan migration evidence

## DoneClaim
- Scenario: production MRD repository scan implementation inspection.
- Invocation: `rg -n -S 'startLeScan|stopLeScan|LeScanCallback' Android-apk/app/src/main/java/com/rehealth/genie/ring/mrd/MrdBleRingRepository.kt`.
- Observable: `OLD_API_NONE` (no deprecated adapter scan API in the production MRD file).
- Artifact: this evidence file.

## Implementation
- `MrdBleRingRepository.scan()` now obtains `bluetoothAdapter.bluetoothLeScanner`, uses `ScanCallback` (`onScanResult`, `onBatchScanResults`, `onScanFailed`), starts with `scanner.startScan(callback)`, preserves the 6,000 ms window, and stops with `scanner.stopScan(callback)`.
- Existing permission checks, Bluetooth state checks, MRD/name/RSSI candidate rules, state flow transitions, and top-12 sorting remain unchanged.
- Scan logs no longer include Bluetooth address, display name, or advertisement raw bytes; only scan error code and final candidate count are logged.

## Tests/builds
- Baseline invocation: `gradlew.bat :app:testDebugUnitTest --tests com.rehealth.genie.ring.RingBackgroundCollectionPolicyTest`; blocked before Gradle by missing `JAVA_HOME`/`java`.
- Retried with `JAVA_HOME=D:\Android_Studio\jbr`: focused invocation `gradlew.bat :app:testDebugUnitTest --tests com.rehealth.genie.ring.mrd.MrdBleRingRepositoryScanTest --tests com.rehealth.genie.ring.RingBackgroundCollectionPolicyTest` reached production `compileDebugKotlin`, then failed in pre-existing `ReHealthMobileApiAttributionTest.kt:37` unresolved `attributeIndividual` during unit-test compilation.
- Production invocation `gradlew.bat :app:compileDebugKotlin` after JBR setup reached KSP and failed flushing existing incremental cache `app/build/kspCaches/debug/...lookups.tab`; no MRD source compile error remained.
- Static invocation `git diff --check -- <owned files>` produced no whitespace errors.

## Manual QA / risks
- Required real-device QA: Android 12+ with granted `BLUETOOTH_SCAN`/`BLUETOOTH_CONNECT`, MRD ring advertising, invoke bind/scan UI, verify 6-second discovery, candidate ordering, and connect behavior.
- Verify scan failure and permission-revocation paths on a physical device; scanner callbacks are platform/runtime behavior not covered by local JVM tests.
