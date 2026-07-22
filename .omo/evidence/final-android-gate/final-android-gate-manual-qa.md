# Android Full Gate Manual QA

## manualQa

### surfaceEvidence

| scenario id | criterion reference | surface | exact invocation | verdict | artifactRefs |
|---|---|---|---|---|---|
| AND-GATE-1 | Android unit-test gate | Gradle JVM test surface | `cd Android-apk; $env:JAVA_HOME='D:\\Android_Studio\\jbr'; .\\gradlew.bat testDebugUnitTest --console=plain` | PASS | `art-test-log` |
| AND-GATE-2 | Android lint gate | Gradle Android lint surface | `cd Android-apk; $env:JAVA_HOME='D:\\Android_Studio\\jbr'; .\\gradlew.bat lintDebug --console=plain` | PASS | `art-lint-log` |
| AND-GATE-3 | Debug APK build | Gradle debug packaging surface | `cd Android-apk; $env:JAVA_HOME='D:\\Android_Studio\\jbr'; .\\gradlew.bat assembleDebug --console=plain` | PASS | `art-debug-log`, `art-debug-apk` |
| AND-GATE-4 | Release APK build | Gradle release packaging surface | `cd Android-apk; $env:JAVA_HOME='D:\\Android_Studio\\jbr'; .\\gradlew.bat assembleRelease --console=plain` | PASS | `art-release-log`, `art-release-apk` |
| AND-GATE-5 | Release cleartext policy | Built release APK manifest via aapt | `aapt dump xmltree app\\build\\outputs\\apk\\release\\app-release-unsigned.apk AndroidManifest.xml` | PASS | `art-aapt-manifest` |
| AND-GATE-6 | Release APK identity/integrity | Filesystem/hash surface | `Get-FileHash app\\build\\outputs\\apk\\debug\\app-debug.apk,app\\build\\outputs\\apk\\release\\app-release-unsigned.apk -Algorithm SHA256` | PASS | `art-apk-hashes` |

### adversarialCases

| scenario id | criterion reference | adversarial class | expected behavior | verdict | artifactRefs |
|---|---|---|---|---|---|
| ADV-AND-1 | Release secret/model-service isolation | Static release payload scan | Release APK should not expose `MODEL_SERVICE_BASE_URL`, `PiasApiClient`, `DeepSeekApiClient`, or provider identifiers. | PASS | `art-apk-string-scan` |
| ADV-AND-2 | Release cleartext transport | Manifest policy inspection | Release manifest must resolve `android:usesCleartextTraffic="false"`. | PASS | `art-aapt-manifest` |
| ADV-AND-3 | Release secret injection | Source/build configuration scan | Release path should not inject provider API keys or direct bearer credentials into app code. | FAIL | `art-source-secret-scan` |
| ADV-AND-4 | Concurrent KSP/build race | Serial daemon/build execution | After stopping daemons, serial gate should complete without KSP cache race. | PASS | `art-test-log`, `art-release-log` |
| ADV-AND-5 | Unsupported real-device UI/BLE scenario | Physical-device prerequisite | Real MRD ring and emulator/device interaction would require an attached configured device. | NOT_APPLICABLE | `art-gate-summary` |

### artifactRefs

| id | kind | description | path |
|---|---|---|---|
| art-test-log | log | `testDebugUnitTest` complete Gradle transcript | `.omo/evidence/final-android-gate/testDebugUnitTest.log` |
| art-lint-log | log | `lintDebug` complete Gradle transcript | `.omo/evidence/final-android-gate/lintDebug.log` |
| art-debug-log | log | `assembleDebug` complete Gradle transcript | `.omo/evidence/final-android-gate/assembleDebug.log` |
| art-release-log | log | `assembleRelease` complete Gradle transcript | `.omo/evidence/final-android-gate/assembleRelease.log` |
| art-debug-apk | binary | Debug APK output (21,496,929 bytes) | `Android-apk/app/build/outputs/apk/debug/app-debug.apk` |
| art-release-apk | binary | Unsigned release APK output (14,454,041 bytes) | `Android-apk/app/build/outputs/apk/release/app-release-unsigned.apk` |
| art-apk-hashes | text | SHA-256 hashes for both APKs | `.omo/evidence/final-android-gate/android-full-gate-summary.txt` |
| art-aapt-manifest | text | aapt manifest inspection showing cleartext disabled | `.omo/evidence/final-android-gate/release-manifest-aapt.txt` |
| art-apk-string-scan | text | Release APK payload identifier scan | `.omo/evidence/final-android-gate/release-apk-string-scan.txt` |
| art-source-secret-scan | text | Source scan showing DEEPSEEK_API_KEY and bearer wiring | `.omo/evidence/final-android-gate/static-secret-model-scan.txt` |
| art-gate-summary | text | Overall command, artifact, and static scan summary | `.omo/evidence/final-android-gate/android-full-gate-summary.txt` |
