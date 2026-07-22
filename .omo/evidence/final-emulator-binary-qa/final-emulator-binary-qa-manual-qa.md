# Final Emulator Binary QA

APK: D:\rehealthAI\Android-apk\app\build\outputs\apk\debug\app-debug.apk
Device: emulator-5554 (Android emulator, sdk_gphone64_x86_64)
Data policy: app data was not cleared.
Overall verdict: **FAIL / BLOCKED BY EMULATOR**. APK SHA-256 passed, but Android system_server/system providers repeatedly crashed or were unavailable, preventing installation/start/activity UI/screenshot scenarios. No real MRD11 data or API-ready animation can be verified without a healthy emulator and backend/device prerequisites.

## Surface Evidence

| Scenario | Criterion | Surface | Exact invocation | Verdict | Artifact refs |
|---|---|---|---|---|---|
| HASH-01 | Final APK digest | Local APK | Get-FileHash -Algorithm SHA256 app-debug.apk | PASS | hash-verification.txt, pk.sha256.txt |
| EMU-01 | Install without clearing data | Android package manager on emulator-5554 | db -s emulator-5554 install -r app-debug.apk | FAIL (blocker: Cannot access system provider: 'settings' before system providers are installed!; earlier Can't find service: package) | invocation-blocker.txt, logcat.txt |
| ATTR-TOP | Attribution page top region | com.rehealth.genie attribution UI | db -s emulator-5554 shell am start -n com.rehealth.genie/com.rehealth.genie.MainActivity; inspect top | FAIL (blocker: Can't find service: activity; app could not launch) | invocation-blocker.txt, logcat.txt |
| ATTR-MID | Attribution page middle region | Attribution UI | Same launch invocation; inspect middle | FAIL (same emulator blocker) | invocation-blocker.txt, logcat.txt |
| ATTR-BOTTOM | Attribution page bottom region | Attribution UI | Same launch invocation; inspect bottom | FAIL (same emulator blocker) | invocation-blocker.txt, logcat.txt |
| ATTR-RANGE | 7/30/90 day selectors | Attribution UI | Launch then tap 7, 30, 90 day controls | FAIL (same emulator blocker) | invocation-blocker.txt, logcat.txt |
| ATTR-EXPAND | Factor expansion | Attribution UI | Launch then tap factor expand control | FAIL (same emulator blocker) | invocation-blocker.txt, logcat.txt |
| SECOND-PAGE | Open second page | Android app navigation | Launch then invoke second-page navigation | FAIL (same emulator blocker) | invocation-blocker.txt, logcat.txt |
| SCREENSHOT-01 | Binary-safe PNG capture | Emulator framebuffer | db -s emulator-5554 shell screencap -p /sdcard/final_qa.png; db -s emulator-5554 pull /sdcard/final_qa.png <evidence-dir> | FAIL (remote file not created; no PNG to validate) | invocation-blocker.txt |
| LOG-01 | No crash / Room migration errors | Emulator logcat | db -s emulator-5554 logcat -d -t 1000 | FAIL (system-server crash observed; no app launch possible; no Room migration evidence) | logcat.txt, logcat.sha256.txt |

## Adversarial Cases

| Scenario | Criterion | Adversarial class | Expected behavior | Verdict | Artifact refs |
|---|---|---|---|---|---|
| ADV-01 | Startup resilience | system-server/package-service unavailable | QA must surface a concrete blocker, not infer UI success | PASS (blocker surfaced and recorded) | logcat.txt, invocation-blocker.txt |
| ADV-02 | Empty-state honesty | no real MRD/API data | Show explicit empty/mock state when app is runnable | FAIL (app could not start; cannot verify) | invocation-blocker.txt |
| ADV-03 | Navigation robustness | second-page opening | Navigation should complete without crash | FAIL (activity service unavailable) | logcat.txt |
| ADV-04 | Persistence safety | reinstall with existing app data | install -r must not clear data | FAIL (install transaction blocked before verification; no clear-data command issued) | invocation-blocker.txt |
| ADV-05 | Migration safety | existing Room database | No Room migration exception on launch | FAIL (launch blocked; no valid app process) | logcat.txt |
| ADV-06 | Animation readiness | API-ready/MRD11 readiness animation | Animation should be observable with real backend/device readiness | NOT_APPLICABLE — no healthy app surface/backend or MRD11 device available | invocation-blocker.txt |

## Artifact References

| ID | Kind | Description | Path |
|---|---|---|---|
| hash-verification.txt | text | Expected vs observed APK digest | .omo/evidence/final-emulator-binary-qa/hash-verification.txt |
| pk.sha256.txt | checksum | SHA-256 hash file | .omo/evidence/final-emulator-binary-qa/apk.sha256.txt |
| invocation-blocker.txt | text | Exact ADB invocations and emulator blocker | .omo/evidence/final-emulator-binary-qa/invocation-blocker.txt |
| logcat.txt | log | 1000-line emulator logcat capture | .omo/evidence/final-emulator-binary-qa/logcat.txt |
| logcat.sha256.txt | checksum | Log artifact checksum | .omo/evidence/final-emulator-binary-qa/logcat.sha256.txt |
| manualQa | markdown | This QA matrix | .omo/evidence/final-emulator-binary-qa/final-emulator-binary-qa-manual-qa.md |
