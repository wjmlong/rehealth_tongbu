# Android final build gate

- Invocation: `cd Android-apk; $env:JAVA_HOME='D:\\Android_Studio\\jbr'; .\\gradlew.bat testDebugUnitTest lintDebug assembleDebug assembleRelease --max-workers=1 --console=plain`
- Surface: Windows PowerShell, existing Android Studio JBR (`openjdk 21.0.10`), serial Gradle worker.
- Result: `BUILD SUCCESSFUL in 2m 9s`; all four requested tasks completed.
- Repository SHA at execution: `bda9dde3cab64b4621f61513c2c7988f9d7b4098`.
- APK artifacts:
  - `Android-apk/app/build/outputs/apk/debug/app-debug.apk`, 21,486,036 bytes, SHA-256 `34E889BE446E3DC56E77F4205DCE9B55AE6B45C2EF0151944B71679D1913A244`.
  - `Android-apk/app/build/outputs/apk/release/app-release-unsigned.apk`, 14,454,041 bytes, SHA-256 `87082455398FE3D19BE5585B31A559669E12600783C3E71083AF04840F63CED3`.

