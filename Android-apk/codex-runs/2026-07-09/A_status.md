# A Android Build Health Status

Date: 2026-07-09

## Scope

Workstream: `A_android_build_health`

Allowed surface reviewed:

- `settings.gradle.kts`
- `build.gradle.kts`
- `app/build.gradle.kts`
- `gradle.properties`
- `README.md`
- `app/src/main/AndroidManifest.xml`

No BLE, MRD parser, UI flow, Room schema, backend, `rehealth-android`, or `model-service` files were changed.

## Existing Implementation Summary

- Single-module Android app: `:app`.
- AGP `8.7.3`, Kotlin `2.0.21`, Gradle wrapper `8.9`.
- `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`.
- Compose, Room/KSP, OkHttp, Gson, and local MRD SDK AAR are configured.
- Manifest includes BLE permissions, optional BLE feature, internet permission, and a single launcher activity.
- `local.properties` exists locally and is ignored by git.
- No unit-test or instrumentation-test source directories are currently present.

## What Changed

- Added `BUILD_NOTES.md` with toolchain requirements, build command, APK output path, local setup notes, Android Studio checklist, and known risks.
- Added this status file for the workstream run.
- Follow-up validation found Android Studio installed at `D:\Android_Studio` and used its bundled JBR to complete the build/test smoke checks.

## Validation Commands

Attempted from `Android-apk`:

```powershell
$env:JAVA_HOME = "D:\Android_Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

Results:

```text
.\gradlew.bat assembleDebug      BUILD SUCCESSFUL
.\gradlew.bat testDebugUnitTest  BUILD SUCCESSFUL, testDebugUnitTest NO-SOURCE
```

Debug APK path verified:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Manual Android Studio Checklist

1. Open `D:\rehealthAI\Android-apk`.
2. Use Gradle JDK 17.
3. Install Android SDK Platform 36.
4. Sync Gradle.
5. Build `app`.
6. Confirm debug APK exists at `app/build/outputs/apk/debug/app-debug.apk`.
7. Install on a physical BLE-capable Android device for MRD ring smoke testing.

## Known Risks

- Command-line shells still need `JAVA_HOME` set to `D:\Android_Studio\jbr` or another Java 17+ JDK; `java.exe` is not on the default PATH.
- AGP 8.7 officially supports API 35, while this project uses API 36 with `android.suppressUnsupportedCompileSdk=36`.
- Current smoke build passed, but no unit-test source exists yet, so `testDebugUnitTest` only validates task wiring.
- MRD ring runtime validation still needs a physical device and ring; this workstream only verified build health surface.

## Next Recommended Task

Keep using Android Studio JBR or expose a Java 17+ JDK, then continue with the next workstream:

```powershell
cd D:\rehealthAI\Android-apk
$env:JAVA_HOME = "D:\Android_Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

For the next Android implementation lane, prioritize a simulator-friendly fake/replay ring path or the BLE foreground-service workstream, depending on whether the immediate goal is emulator UI iteration or real ring background collection.
