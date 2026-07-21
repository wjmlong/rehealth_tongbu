# A Android Build Prompt

Read AGENTS.md, ENGINEERING.md, and CODEX_ORCHESTRATION.md.

Workstream: A_android_build.

Goal:
Make the Android-apk repository build reliably in Android Studio/JDK17 and from CLI.

Tasks:
1. Inspect settings.gradle.kts, root build.gradle.kts, app/build.gradle.kts, AndroidManifest.xml.
2. Run assembleDebug if possible.
3. Fix only build/configuration issues.
4. Do not change BLE, UI, network, feature extraction, or business logic unless required by build failure.
5. Create or update BUILD_NOTES.md.

Finish with:
- changed files
- build command and result
- remaining local setup requirements
- APK output path
