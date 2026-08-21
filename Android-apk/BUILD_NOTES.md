# Android Build Notes

Last updated: 2026-08-21

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
- Gradle wrapper: Gradle 8.11.1 (`gradle/wrapper/gradle-wrapper.properties`).
- Android Gradle Plugin: 8.10.1.
- Kotlin: 2.2.20（Kotlin Compose 插件与 KSP 同为 `2.2.20` / `2.2.20-2.0.4`）。
- Android SDK: install Android SDK Platform 36 because `compileSdk` and `targetSdk` are both 36（`minSdk` 26）。
- Android Studio: use a version that can open AGP 8.10 projects; Android Studio 2026.1 with bundled JBR 21 was validated locally.

Note: `gradle.properties` 仍保留 `android.suppressUnsupportedCompileSdk=36` 作为本地构建兼容开关；AGP 8.10.1 已支持 API 36，是否移除该开关待工具链升级评审确认。

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

当前仓库已包含 64 个 JVM 单元测试文件和 13 个 `androidTest` 仪器测试文件，
`testDebugUnitTest` 不再是 `NO-SOURCE` 空跑。发布相关命令见根 `README.md` 与
`Android-apk/README.md`（`verifyReleaseConfiguration`、`lintRelease`、
`assembleRelease`、`verifyPublishConfiguration`）。

## Android Studio Open Checklist

1. Choose `File > Open` and select this repository's `Android-apk` directory.
2. Confirm the selected Gradle JDK is Java 17.
3. Let Gradle sync complete.
4. Confirm SDK Platform 36 is installed if sync reports a missing SDK.
5. Select the `app` run configuration.
6. Build `app` or run `assembleDebug`.
7. Install the generated debug APK on a BLE-capable Android device for HBand/Viomi validation.

## Current Validation Result

历史记录（2026-07-09）：初次命令行构建因未设置 `JAVA_HOME` 失败；设置
`JAVA_HOME=D:\Android_Studio\jbr` 后 `assembleDebug` 与 `testDebugUnitTest`
均 `BUILD SUCCESSFUL`。

当前状态（2026-08-21）：`testDebugUnitTest`、`assembleDebug`、`lintRelease`、
`verifyReleaseConfiguration`、`verifyPublishConfiguration` 与签名 Release 构建
均已通过；Room 13→14 迁移与 RHI 四表持久化等仪器测试已在 MuMu（API 35）验证。
具体验收与阻塞项以根 `STATUS.md` 为准。

## Known Local Setup Issues

- `java.exe` is still not on the default shell `PATH`; set `JAVA_HOME` for command-line Gradle runs or build from Android Studio.
- `local.properties` points at `C:\Users\kiki\AppData\Local\Android\Sdk`; this is appropriate as an ignored local file, but each developer must have their own SDK path.
- API 36 with AGP 8.10.1 仍通过 `android.suppressUnsupportedCompileSdk=36` 保留抑制开关；升级工具链时一并评审。
