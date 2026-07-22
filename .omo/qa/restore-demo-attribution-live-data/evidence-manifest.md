# Attribution restoration evidence manifest

## Typed state and service boundary

- Scenario: map confirmed 2.19% risk, signed 7/30/90-day improvement, malformed PIAS arrays, accumulating/ATT-unavailable states, Core16 aliases/order, Room activity provenance, real-ID interventions, and stale request suppression.
- Invocation: `gradlew.bat --no-daemon testDebugUnitTest --tests com.rehealth.genie.ui.AttributionUiStateTest --tests com.rehealth.genie.network.PiasApiClientTest`.
- Binary observable: `BUILD SUCCESSFUL in 25s`.
- Artifact: `task-2/green-focused-tests.log` plus JUnit XML under `Android-apk/app/build/test-results/testDebugUnitTest/`.

## Room upgrade regression

- Scenario: upgrade the preserved emulator version-3 schema containing legacy risk history and inconsistent/missing queue metadata without clearing app data.
- Invocation: targeted `RiskHistoryMigrationTest`, followed by `adb install -r` and explicit `am start` on `emulator-5554`.
- Binary observable: red tests before fix; green tests after fix; final installed app remains resumed with no `AndroidRuntime` error.
- Artifacts: `task-4/migration-red.log`, `task-4/migration-queues-red.log`, `task-4/migration-normal-v3-green.log`, `task-4/canonical-schema-logcat.txt`, and `task-4/absolute-final/crash-check.txt`.

## Final Android gate

- Scenario: run all debug JVM tests, Android lint, and assemble the exact installable APK after the last source edit.
- Invocation: `gradlew.bat --no-daemon testDebugUnitTest lintDebug assembleDebug` with the emulator-matching local debug keystore.
- Binary observable: `BUILD SUCCESSFUL in 1m 45s`; signed APK installed with `adb install -r` returning `Success`.
- Artifacts: `task-4/gradle-absolute-final.log` and `task-4/absolute-final/install.txt`.

## Rendered attribution surface

- Scenario: open the attribution bottom tab, switch periods, expand a factor, and scroll through summary, PIAS, Room activity, all four Core16 groups, server-only plan, and disclaimer.
- Invocation: API31 `emulator-5554` ADB taps/swipes plus `uiautomator dump` and `screencap`.
- Binary observable: complete CJK hierarchy with target section labels, non-empty 1080x2400 PNGs, and no crash.
- Artifacts: `task-4/absolute-final/top.{png,xml}`, `middle.{png,xml}`, `bottom.{png,xml}`, `task-4/attribution-30-day.{png,xml}`, `task-4/final-90-day.xml`, `task-4/factor-expanded.xml`, and `task-4/factor-transition-{start,mid,end}.png`.

## Independent review

- Scenario: inspect all three fresh final scroll captures for reference anatomy, real component integrity, and CJK clipping/overlap.
- Binary observable: visual CJK reviewer returned `PASS` with high confidence and no product/evidence blockers; remaining reviewer verdicts are recorded by the parent verification lane.
- Artifact: this manifest plus reviewer messages in the execution thread.
