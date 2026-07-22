# Attribution restoration baseline

Captured before Android product edits on 2026-07-22.

## Historical target anatomy

Source: `D:/rehealth_demo/Android-apk` commit `7008f2f`, plus the three supplied
reference frames.

1. `7 天 / 30 天 / 90 天` segmented period selector.
2. Improvement summary with signed improvement, current risk, risk level/trend,
   and a compact history chart.
3. Individual risk trend card with two PIAS forecast lines, confidence legend,
   and three 30-day forecast metrics.
4. Behavior card in the same visual position as the demo meal card.
5. Four grouped CVD factor sections containing all 16 canonical fields, with
   per-row expansion.
6. Expandable personalized intervention plan.
7. Existing attribution bottom tab remains the navigation owner.

## Canonical data binding

| Target region | Canonical source | Honest unavailable state |
| --- | --- | --- |
| Current risk | confirmed Jeecg feature evaluation / persisted `RiskHistoryEntity` | `--` plus accumulation text |
| Improvement/history | persisted confirmed risk history only | fewer than two points shows `--` |
| PIAS forecast/ATT | `/api/pias/v2/attribute/individual`, fixed 30-day forecast | accumulating, ATT unavailable, failed, or retry status inside the card |
| Behavior | latest Room `RingActivityEntity` written by MR11 | no activity recorded; debug replay is labeled |
| Factors | Jeecg `featureContributions` in exact Core16 order | retained row marked not supplied |
| Intervention | server `PatientInterventionPayload` with a real nonblank ID | no server plan; heuristic fallback excluded |
| Feedback | existing durable feedback repository/worker for a real ID | no action when no real ID exists |

## Current regression captured

`before-navigation.png` and `before-navigation.xml` are fresh ADB captures from
`emulator-5554` running `com.rehealth.genie` version 1.0. The attribution tab was
already selected and scrolled near its bottom. The installed screen shows a flat
16-row contribution list and an honest ungenerated-plan card, but no period
selector, persisted improvement baseline, PIAS forecast card, Room activity card,
or grouped/expandable target anatomy.

ADB invocation:

```powershell
D:\Android_SDK\platform-tools\adb.exe -s emulator-5554 shell uiautomator dump --compressed /sdcard/rehealth-before.xml
D:\Android_SDK\platform-tools\adb.exe -s emulator-5554 pull /sdcard/rehealth-before.xml D:\rehealthAI\.omo\qa\restore-demo-attribution-live-data\task-1\before-navigation.xml
D:\Android_SDK\platform-tools\adb.exe -s emulator-5554 shell screencap -p /sdcard/rehealth-before.png
D:\Android_SDK\platform-tools\adb.exe -s emulator-5554 pull /sdcard/rehealth-before.png D:\rehealthAI\.omo\qa\restore-demo-attribution-live-data\task-1\before-navigation.png
```

