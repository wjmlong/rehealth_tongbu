# Manual QA — Restore demo attribution live data

Surface under test: `com.rehealth.genie/.MainActivity` on API 31 `emulator-5554`, exact APK `Android-apk/app/build/outputs/apk/debug/app-debug.apk` built from the current worktree. Installation used `adb install -r`; app data was not cleared.

## manualQa

### surfaceEvidence

| scenario id | criterion reference | surface | exact invocation | verdict | artifactRefs |
|---|---|---|---|---|---|
| S1 | migration/no-crash | Android app launch + preserved Room data | `adb -s emulator-5554 install -r .../app-debug.apk`; `adb shell am force-stop com.rehealth.genie`; `adb shell am start -n com.rehealth.genie/.MainActivity`; wait 15s; `dumpsys activity activities`; `uiautomator dump` | PASS | A1,A2,A3 |
| S2 | attribution tab entry | Attribution bottom tab | `adb shell input tap 540 2100`; `screencap -p`; `uiautomator dump` | PASS | A4,A5 |
| S3 | period selectors 7/30/90 | Attribution period selector | taps at `(540,460)`, `(860,460)`, then screenshots/XML dumps | PASS | A6,A7,A8 |
| S4 | top/middle/bottom scroll coverage | Attribution `LazyColumn` | ADB swipes `540,1800 -> 540,650` and reverse; fresh screenshots/XML dumps | PASS | A9,A10,A11,A12,A13 |
| S5 | factor expand/detail | Contribution factor rows | Scroll to factor rows; tap a visible factor row; capture expanded state | PASS | A14,A15 |
| S6 | honest empty states | Summary, PIAS, activity, plan | Inspect fresh XML/screenshot for no-history state | PASS | A4,A10,A11,A14 |
| S7 | CJK layout/readability | Rendered attribution cards | Open fresh PNGs and inspect text bounds/line wrapping at 1080x2400 | PASS | A9,A10,A11,A16 |

### adversarialCases

| scenario id | criterion reference | adversarial class | expected behavior | verdict | artifactRefs |
|---|---|---|---|---|---|
| ADV1 | migration/no-crash | upgrade without data clear | Existing app data remains usable after `-r`; no process crash | PASS | A1,A2,A3 |
| ADV2 | honest empty states | insufficient history / missing factors | Show `--`, `待生成`, `待记录`, `未提供` and explain prerequisites; never fabricate risk/plan | PASS | A4,A10,A11,A14 |
| ADV3 | factor detail | repeated expand/collapse interaction | Tapping a factor changes `详情` to `收起` and shows source/explanation text | PASS | A14,A15 |
| ADV4 | failure/retry | remote/network failure path | A visible retryable error should appear if attribution remote call is exercised | not_applicable | A17 — preserved emulator has zero attribution history, so this screen never invokes the remote PIAS call; no safe failure injection was available |
| ADV5 | PIAS status/animation | PIAS ready/animated forecast | Show real PIAS status and motion when confirmed history exists | not_applicable | A18 — no confirmed risk-history rows exist on the preserved emulator, so PIAS remains honestly `待生成` and no forecast animation is triggered |

### artifactRefs

| id | kind | description | path |
|---|---|---|---|
| A1 | log | Independent Gradle gate, first environment blocker (`JAVA_HOME` absent) | `D:/rehealthAI/.omo/evidence/restore-demo-attribution-live-data-executor/independent-qa/gradle-independent.log` |
| A2 | log | Independent successful `testDebugUnitTest lintDebug assembleDebug` with explicit Temurin JDK | `D:/rehealthAI/.omo/evidence/restore-demo-attribution-live-data-executor/independent-qa/gradle-independent-with-jdk.log` |
| A3 | log | Device/API31, `adb install -r Success`, launch focus, app PID, runtime scan | `D:/rehealthAI/.omo/evidence/restore-demo-attribution-live-data-executor/independent-qa/install-independent.txt` |
| A4 | screenshot | Fresh attribution rendered capture (PNG validated 1080x2400) | `D:/rehealthAI/.omo/evidence/restore-demo-attribution-live-data-executor/independent-qa/fresh-attribution-top.png` |
| A5 | XML | Attribution tab hierarchy with `健康归因`, selectors, PIAS/activity cards | `D:/rehealthAI/.omo/evidence/restore-demo-attribution-live-data-executor/independent-qa/attr-top.xml` |
| A6 | screenshot | Fresh 30-day selector capture | `D:/rehealthAI/.omo/evidence/restore-demo-attribution-live-data-executor/independent-qa/fresh-period-30.png` |
| A7 | screenshot | Fresh 90-day selector capture | `D:/rehealthAI/.omo/evidence/restore-demo-attribution-live-data-executor/independent-qa/fresh-period-90.png` |
| A8 | XML | Selector hierarchy for 30/90 state changes | `D:/rehealthAI/.omo/evidence/restore-demo-attribution-live-data-executor/independent-qa/period-90.xml` |
| A9 | screenshot | Fresh middle-scroll capture showing factor cards/detail state | `D:/rehealthAI/.omo/evidence/restore-demo-attribution-live-data-executor/independent-qa/fresh-scroll-middle.png` |
| A10 | screenshot | Fresh bottom-scroll capture showing plan/disclaimer | `D:/rehealthAI/.omo/evidence/restore-demo-attribution-live-data-executor/independent-qa/fresh-scroll-bottom.png` |
| A11 | XML | Middle-scroll hierarchy including `贡献因素`, `16 项`, factor sections | `D:/rehealthAI/.omo/evidence/restore-demo-attribution-live-data-executor/independent-qa/scroll-middle.xml` |
| A12 | XML | Bottom-scroll hierarchy including `基础体征` and plan | `D:/rehealthAI/.omo/evidence/restore-demo-attribution-live-data-executor/independent-qa/scroll-bottom.xml` |
| A13 | XML | Return-to-top hierarchy after reverse swipes | `D:/rehealthAI/.omo/evidence/restore-demo-attribution-live-data-executor/independent-qa/scroll-top-return.xml` |
| A14 | screenshot | Fresh valid 1080x2400 expanded-factor/detail capture | `D:/rehealthAI/.omo/evidence/restore-demo-attribution-live-data-executor/independent-qa/fresh-factor-expanded.png` |
| A15 | XML | Expanded factor has `收起` plus model/source explanation text | `D:/rehealthAI/.omo/evidence/restore-demo-attribution-live-data-executor/independent-qa/factor-expand-attempt.xml` |
| A16 | log | Fresh PNG signatures/dimensions and final resumed activity/runtime scan | `D:/rehealthAI/.omo/evidence/restore-demo-attribution-live-data-executor/independent-qa/fresh-png-validation.txt` |
| A17 | log | Refresh tap immediate/settled captures; no remote call because history empty | `D:/rehealthAI/.omo/evidence/restore-demo-attribution-live-data-executor/independent-qa/refresh-settled.xml` |
| A18 | XML | PIAS honest `待生成` state and prerequisite copy | `D:/rehealthAI/.omo/evidence/restore-demo-attribution-live-data-executor/independent-qa/attr-top.xml` |

## Overall verdict

**PASS for the exercised surface.** The current APK builds, installs over preserved data, survives migration/launch, renders the attribution tab and all requested honest empty states, supports 7/30/90 selection, scroll, factor expansion, and CJK layout without an app crash. Remote failure/retry and PIAS ready-animation were not applicable because the preserved emulator contains no confirmed risk-history rows; those states require seeded history or a safe backend fault-injection harness.
