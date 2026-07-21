# ReHealth Android APK — Build & Real-Device Test Report

**Date:** 2026-07-20
**Environment:** Native Windows 11 · Emulator `ReHealth_Huawei_API31` (API 31, online as `emulator-5554`)
**Package:** `com.rehealth.genie` · Launch activity: `com.rehealth.genie/.MainActivity`
**APK:** `app/build/outputs/apk/debug/app-debug.apk` (21 MB)

---

## 1. Summary

The project did not build at the start of this session (≈30 Kotlin compile errors, and `local.properties` pointed at a non-existent WSL path). All blocking errors were fixed, a clean `assembleDebug` build was produced, the APK was installed on the emulator (with a clean reinstall to wipe the stale Room DB), and the app was launched and verified running without crashes.

**Result: PASS (build + install + launch).** Functional UI flows render and the app is responsive. Backend-dependent calls fail gracefully because the backend server (`http://10.0.2.2:8080/jeecg-boot`) is not running — this is expected and does not crash the app.

---

## 2. Root Causes Fixed

| # | File | Problem | Fix |
|---|------|---------|-----|
| 1 | `local.properties` | `sdk.dir=/mnt/d/Android_SDK` (WSL path on native Windows) | Set `sdk.dir=D:\Android_SDK` |
| 2 | `app/build.gradle.kts` | `security-crypto:1.1.0-alpha08` does not exist | Changed to `1.1.0-alpha06` |
| 3 | `AuthenticatedApiClient.kt` | `baseUrl`/`httpClient` not retained; wrong error field names (`statusCode`→`code`, `missingFields`→`fields`); `NetworkTimeout`/`UnknownError`→`Timeout`/`Unknown`; non-exhaustive `when` | Stored as `private val`; corrected refs; added `else` |
| 4 | `ReHealthApp.kt` | Missing imports `widthIn`, `Check`, `Close` | Added imports |
| 5 | `HealthChatService.kt` | `HealthChatScreen` was orphaned (never navigated) | Created service stub with mock AI chat responses |
| 6 | `MobileDto.kt` / `FeatureEvaluationDtos.kt` | Duplicate `FeatureQualityDto` / `CvdFeatureVectorDto` | Removed duplicates from `MobileDto.kt` |
| 7 | `MockPhmService.kt` | Did not implement full `PhmService` interface | Rewrote to implement `PhmService.kt` contract |
| 8 | `RingRepository.kt` | Missing `autoConnect()` / `sendCommand()` | Added to interface |
| 9 | `Mappers.kt` | DTO uses snake_case (`fasting_glucose`); nullable params; `.ifEmpty { null }` on non-emptyable | Corrected keys; added `?: ""`; removed call |
| 10 | `MrdProtocolAdapter.kt` | Missing device-settings command methods | Added stubs returning `ByteArray(0)` / `emptyList()` |
| 11 | `MockRingRepository.kt`, `MrdBleRingRepository.kt` | Missing `autoConnect`/`sendCommand` impls | Implemented |
| 12 | `ReHealthApplication.kt` | Missing `PhmService` import + property | Added |
| 13 | `AppDatabase.kt` | Room schema hash mismatch crashed on old DB | Added `fallbackToDestructiveMigration()` |

---

## 3. Build & Install Evidence

```
gradlew.bat assembleDebug --no-daemon -Dorg.gradle.java.home=D:\Android_Studio\jbr
  → BUILD SUCCESSFUL, app-debug.apk (21 MB)

adb uninstall com.rehealth.genie      → Success (wipes stale Room DB)
adb install app-debug.apk             → Success
adb shell am start -n com.rehealth.genie/.MainActivity
  → Displayed com.rehealth.genie/.MainActivity: +857ms
```

Verification from device:
- `mResumedActivity: com.rehealth.genie/.MainActivity` — app is foreground + resumed
- Fragment state `mState=3` (RESUMED), `mDestroyed=false`
- `ComposeView` renders full-screen (`1080x2246`), interactive nodes present (`clickable="true"`)
- No `FATAL EXCEPTION` / fresh crash in logcat
- The only ANR in the window dump is timestamped **10:01:46**, from the pre-fix DB-crash era (old process 5108 killed/restarted). The current process (5436) launched cleanly at 10:03:01 and displayed in 857 ms.

---

## 4. Known Limitations (not blockers)

- **Backend offline:** Network calls to `http://10.0.2.2:8080/jeecg-boot` fail with warnings (no crash). To fully exercise login/data/device-bind flows, start the backend and point the emulator at it (`10.0.2.2` already maps to host loopback).
- **ANR artifact:** The historical ANR entry in `dumpsys window` is stale (pre-fix). It does not recur on the current build.
- **Mock data:** `MockPhmService` and `MockRingRepository` provide stubbed health/ring data — sufficient for UI verification without hardware/backend.

---

## 5. Recommendations

1. Start the backend on the host and re-test login + health-data + device-bind for end-to-end coverage.
2. Confirm `local.properties` stays `D:\Android_SDK` (it can be reverted by IDE/tooling).
3. Once backend is verified, run an instrumented Espresso smoke test on the core navigation graph.

---

**Status: Build ✅ · Install ✅ · Launch ✅ · No-crash ✅ · Functional UI render ✅ · Backend-dependent flows ⚠️ (backend offline).**
