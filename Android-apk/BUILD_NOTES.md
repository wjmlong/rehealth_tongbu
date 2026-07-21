# Android Build Notes

Last updated: 2026-07-09

## Project Structure

- Android project root: `Android-apk`
- Gradle root project: `ReHealthGenie`
- Modules: `:app`
- Application ID: `com.rehealth.genie`
- Main app manifest: `app/src/main/AndroidManifest.xml`
- MRD SDK AAR: `app/libs/sdk_mrd2026_1.3.0.aar`
- Debug APK output: `app/build/outputs/apk/debug/app-debug.apk`

## Toolchain

- JDK: Java 17 or newer is required to launch Gradle. The app module still compiles Java/Kotlin bytecode with target 17.
- Gradle wrapper: Gradle 8.9 (`gradle/wrapper/gradle-wrapper.properties`).
- Android Gradle Plugin: 8.7.3.
- Kotlin: 2.0.21.
- Compose compiler: Kotlin Compose plugin 2.0.21.
- Android SDK: install Android SDK Platform 36 because `compileSdk` and `targetSdk` are both 36.
- Android Studio: use a version that can open AGP 8.7 projects, with Android Studio Ladybug 2024.2.1 or newer as the practical baseline. Android Studio 2026.1 with bundled JBR 21 was validated locally.

Note: AGP 8.7 officially supports up to API 35, while this project currently compiles against API 36 and sets `android.suppressUnsupportedCompileSdk=36`. Keep this as a known local-build risk until the team intentionally upgrades AGP/Gradle or lowers `compileSdk`.

## Local Setup

1. Install Android Studio.
2. Install Android SDK Platform 36 from SDK Manager.
3. Ensure a Java 17+ runtime is available to Gradle.
4. Either open the project in Android Studio and use the bundled Gradle JDK, or set `JAVA_HOME` before running command-line builds:

```powershell
$env:JAVA_HOME = "C:\Path\To\jdk-17"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

For this workstation, Android Studio is installed at `D:\Android_Studio`, so command-line builds can use:

```powershell
$env:JAVA_HOME = "D:\Android_Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

Do not commit `local.properties`; it contains the machine-specific Android SDK path and is ignored by git.

## Command-Line Build

From `Android-apk`:

```powershell
.\gradlew.bat assembleDebug
```

Optional unit-test command:

```powershell
.\gradlew.bat testDebugUnitTest
```

As of this update, no `app/src/test` or `app/src/androidTest` sources are present, but the Gradle unit-test task should still be used as the default smoke check when the local JDK is available.

## Android Studio Open Checklist

1. Choose `File > Open` and select `D:\rehealthAI\Android-apk`.
2. Confirm the selected Gradle JDK is Java 17.
3. Let Gradle sync complete.
4. Confirm SDK Platform 36 is installed if sync reports a missing SDK.
5. Select the `app` run configuration.
6. Build `app` or run `assembleDebug`.
7. Install the generated debug APK on a BLE-capable Android device for MRD ring validation.

## Current Validation Result

Commands attempted on 2026-07-09:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

Initial attempts failed before Gradle project evaluation because `JAVA_HOME` was not set. After installing Android Studio and running with `JAVA_HOME=D:\Android_Studio\jbr`, both commands completed successfully:

```text
BUILD SUCCESSFUL
```

`testDebugUnitTest` currently reports `NO-SOURCE` because no unit-test source files are present yet. The debug APK is available at `app/build/outputs/apk/debug/app-debug.apk` and was about 18 MB in the local validation run.

## Known Local Setup Issues

- `java.exe` is still not on the default shell `PATH`; set `JAVA_HOME` for command-line Gradle runs or build from Android Studio.
- `local.properties` points at `C:\Users\kiki\AppData\Local\Android\Sdk`; this is appropriate as an ignored local file, but each developer must have their own SDK path.
- API 36 with AGP 8.7.3 is currently suppressed via `android.suppressUnsupportedCompileSdk=36`; revisit when upgrading AGP/Gradle.
