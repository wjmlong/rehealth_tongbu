# Attribution Clone Fidelity Review

## Decision

- **VERDICT:** REVISE
- **recommendation:** REQUEST_CHANGES
- **confidence:** HIGH
- **reviewed revision:** `273efe2624bd158d766db9557538c904c7a04b15`
- **goal:** Restore the historical Android attribution anatomy while rendering only live data or explicit, honest unavailable states.

The implementation is a real Compose surface backed by typed state; it is not a screenshot, raster substitute, or mock-only composition. It also materially restores the historical card order and protects state honesty. Approval is still blocked because the implementation is not a rigorous token-driven design system, the factor-row anatomy diverges from the exact historical target, the medical disclaimer contains a visible CJK semantic split, and current-build evidence does not cover the target's data-populated state.

## Findings

### CRITICAL

None.

### HIGH

1. **[product] [design system] Styling is predominantly one-off rather than token-driven.**
   - The shared theme defines only six named colors and a Material color scheme; it defines no typography scale, spacing scale, radii, component dimensions, or semantic chart/status colors (`Android-apk/app/src/main/java/com/rehealth/genie/ui/theme/Theme.kt:8`, `Android-apk/app/src/main/java/com/rehealth/genie/ui/theme/Theme.kt:15`).
   - `AttributionScreen.kt` embeds 13 distinct raw colors, 111 `dp` literals, and 51 `sp` literals. Representative one-off definitions include the screen rhythm and heading (`Android-apk/app/src/main/java/com/rehealth/genie/ui/AttributionScreen.kt:190`), period control (`Android-apk/app/src/main/java/com/rehealth/genie/ui/AttributionScreen.kt:253`), summary metrics (`Android-apk/app/src/main/java/com/rehealth/genie/ui/AttributionScreen.kt:307`), factor rows (`Android-apk/app/src/main/java/com/rehealth/genie/ui/AttributionScreen.kt:557`), and the card primitive itself (`Android-apk/app/src/main/java/com/rehealth/genie/ui/AttributionScreen.kt:747`).
   - File-private primitives such as `AttributionCard`, `AttributionCompactMessage`, and metric/legend helpers provide some reuse, but their geometry and type values are still hardcoded locally. This does not meet the required standard of reusable tokens driving color, spacing, typography, and component anatomy.

2. **[product] [reference anatomy] Factor rows do not match the historical target's component hierarchy.**
   - The exact historical row contains a numbered circular rank badge, indented label/value content, a signed score, a separate contribution-share line, and an indented dual-tone progress bar. This is visible in the supplied third reference screenshot and pinned historical source `D:/rehealth_demo/Android-apk`, commit `7008f2f`, `ReHealthApp.kt:1510`.
   - The current row omits the rank badge and contribution-share label, exposes the raw model value such as `+0.123` instead of the target score/share presentation, and draws a full-width progress bar (`Android-apk/app/src/main/java/com/rehealth/genie/ui/AttributionScreen.kt:578`, `Android-apk/app/src/main/java/com/rehealth/genie/ui/AttributionScreen.kt:583`, `Android-apk/app/src/main/java/com/rehealth/genie/ui/AttributionScreen.kt:593`).
   - The fresh middle/bottom captures confirm the divergence: factor rows begin directly with the label and end with `未提供 / 详情`, without the target rank hierarchy. Grouping and disclosure behavior are restored, but the target row anatomy is not.

3. **[product] [CJK/safety copy] The rendered disclaimer splits the medical term `医学诊断` across lines.**
   - In `absolute-final/bottom.png`, the first line ends with `医学诊` and the next begins with `断，也不能替代医生建议。` This is a semantic-phrase split in safety-critical copy, not harmless punctuation wrapping.
   - The source places the disclaimer in a weighted row with a fixed start padding and no CJK phrase-preservation strategy (`Android-apk/app/src/main/java/com/rehealth/genie/ui/AttributionScreen.kt:230`, `Android-apk/app/src/main/java/com/rehealth/genie/ui/AttributionScreen.kt:236`).
   - The XML proves the complete string exists and is not truncated, so this is a product layout defect rather than corrupt evidence.

4. **[evidence] [state coverage] Fresh current-build screenshots cover only the honest unavailable/empty state, not the target's data-populated anatomy.**
   - `absolute-final/top.png`, `middle.png`, and `bottom.png` cover the whole scroll surface, but show no confirmed score/history chart, no PIAS lines/legend/metrics, no Room activity metrics, no non-missing factor contribution, and no server intervention expansion/feedback state.
   - Source review shows live state variants exist (`Android-apk/app/src/main/java/com/rehealth/genie/ui/AttributionScreen.kt:348`, `Android-apk/app/src/main/java/com/rehealth/genie/ui/AttributionScreen.kt:390`, `Android-apk/app/src/main/java/com/rehealth/genie/ui/AttributionScreen.kt:417`, `Android-apk/app/src/main/java/com/rehealth/genie/ui/AttributionScreen.kt:485`, `Android-apk/app/src/main/java/com/rehealth/genie/ui/AttributionScreen.kt:639`), but code presence cannot establish rendered fidelity, wrapping, chart bounds, or interaction geometry for those states.
   - Approval requires a fresh same-revision, honest data-populated capture set or an explicit acceptance that only empty-state fidelity is in scope.

### MEDIUM

1. **[evidence] [interaction motion] The disclosure sequence does not provide a distinct mid-transition frame.**
   - `factor-transition-start.png` and `factor-transition-mid.png` are byte-identical (`SHA-256 FC20CBE9...4034D`); only the end frame differs. The artifacts prove the settled expanded state but not transition behavior.
   - The source toggles expanded content immediately and animates only the contribution bar value (`Android-apk/app/src/main/java/com/rehealth/genie/ui/AttributionScreen.kt:548`, `Android-apk/app/src/main/java/com/rehealth/genie/ui/AttributionScreen.kt:565`). This is not slop animation, but the submitted start/mid/end evidence should not be described as proof of a rendered expansion transition.

2. **[product] [semantics] Custom clickable controls expose weak accessibility semantics.**
   - Period choices are clipped `Box` elements with `clickable`, rather than tab/selectable semantics with selected state (`Android-apk/app/src/main/java/com/rehealth/genie/ui/AttributionScreen.kt:258`).
   - Entire factor rows are clickable, while the visible `详情` text is a non-clickable child in the hierarchy (`Android-apk/app/src/main/java/com/rehealth/genie/ui/AttributionScreen.kt:575`). The touch target works, but assistive technology receives neither disclosure role nor expanded/collapsed state.

### LOW

1. **[product] [copy fidelity] The summary heading is shortened from the target's `健康改善得分` to `健康改善`.**
   - Current: `Android-apk/app/src/main/java/com/rehealth/genie/ui/AttributionScreen.kt:311`.
   - Target: supplied first reference screenshot and historical source commit `7008f2f`, `ReHealthApp.kt:1298`.

2. **[evidence] [artifact provenance] The final screenshots have install/launch/build receipts but no cryptographic binding to a post-install APK pull.**
   - The final build log passes, install succeeds, source matches reviewed `HEAD`, and screenshots follow the install chronologically.
   - The provided `installed-apk/installed.apk` predates the absolute-final capture/install and differs from the later workspace APK, so it should not be cited as the exact binary captured without a fresh post-install hash receipt.

## What Is Good

- **Real component tree:** `LazyColumn`, Material `Card`, `Text`, `Button`, `Canvas`, and reusable Compose helpers render the surface. No `Image`, bitmap, raster, or background-image substitutes are used (`Android-apk/app/src/main/java/com/rehealth/genie/ui/AttributionScreen.kt:183`).
- **Honest state mapping:** only confirmed risk evaluations produce contributions; missing Core16 fields remain present and explicitly unavailable (`Android-apk/app/src/main/java/com/rehealth/genie/ui/AttributionUiMapper.kt:162`).
- **Honest provenance:** release UI excludes replay activity, debug replay is visibly labeled, and local heuristic/idless interventions are excluded (`Android-apk/app/src/main/java/com/rehealth/genie/ui/AttributionUiMapper.kt:184`, `Android-apk/app/src/main/java/com/rehealth/genie/ui/AttributionUiMapper.kt:207`).
- **State integrity:** typed empty/loading/refreshing/error/ready states exist; stale request completion is ignored and last successful data is retained on refresh failure (`Android-apk/app/src/main/java/com/rehealth/genie/ui/AttributionUiModels.kt:108`, `Android-apk/app/src/main/java/com/rehealth/genie/ui/AttributionUiModels.kt:163`).
- **Target hierarchy:** the fresh captures preserve the required top-to-bottom order: period selector, improvement, PIAS trend, behavior, grouped factors, intervention plan, and medical disclaimer.
- **Interactions:** the 30-day capture proves selector state changes; the factor end-state capture and XML prove row disclosure works. Refresh/retry and server-backed feedback are wired in source.
- **Runtime evidence:** `gradle-absolute-final.log` records `testDebugUnitTest`, `lintDebug`, and `assembleDebug` success; install/launch receipts succeed and `crash-check.txt` is empty.
- **CJK generally:** no tofu, clipping, baseline loss, or one-character orphaning is visible outside the disclaimer defect.

## Blockers

1. Replace screen-local style literals with a documented, reusable attribution/app token system covering typography, spacing, radii, surfaces, chart/status colors, and component states.
2. Restore the historical factor-row hierarchy: rank, separate score/share semantics, aligned bar anatomy, and matching expanded-content indentation while retaining honest missing data.
3. Prevent the disclaimer from splitting `医学诊断` in the supported emulator width and re-capture the bottom region.
4. Capture the complete data-populated attribution state on the same reviewed revision, including charts, activity metrics, factor contributions/expanded detail, and server intervention/feedback states, or explicitly narrow acceptance to empty-state fidelity.

## Evidence Inspected

### Reference targets

- `C:/Users/wjmlong/AppData/Local/Temp/codex-clipboard-5225098f-c281-4643-a359-18534546938d.png`
- `C:/Users/wjmlong/AppData/Local/Temp/codex-clipboard-fd52493f-fa6c-4bd6-a38c-711b8548048f.png`
- `C:/Users/wjmlong/AppData/Local/Temp/codex-clipboard-59460fe8-0d16-4a6f-9e07-71294dab2db6.png`
- `D:/rehealth_demo/Android-apk`, commit `7008f2f`, historical `ReHealthApp.kt`

### Fresh actual surface

- `.omo/qa/restore-demo-attribution-live-data/task-4/absolute-final/top.png`
- `.omo/qa/restore-demo-attribution-live-data/task-4/absolute-final/top.xml`
- `.omo/qa/restore-demo-attribution-live-data/task-4/absolute-final/middle.png`
- `.omo/qa/restore-demo-attribution-live-data/task-4/absolute-final/middle.xml`
- `.omo/qa/restore-demo-attribution-live-data/task-4/absolute-final/bottom.png`
- `.omo/qa/restore-demo-attribution-live-data/task-4/absolute-final/bottom.xml`
- `.omo/qa/restore-demo-attribution-live-data/task-4/attribution-30-day.png`
- `.omo/qa/restore-demo-attribution-live-data/task-4/factor-transition-start.png`
- `.omo/qa/restore-demo-attribution-live-data/task-4/factor-transition-mid.png`
- `.omo/qa/restore-demo-attribution-live-data/task-4/factor-transition-end.png`

### Runtime/provenance

- `.omo/qa/restore-demo-attribution-live-data/task-4/gradle-absolute-final.log`
- `.omo/qa/restore-demo-attribution-live-data/task-4/absolute-final/install.txt`
- `.omo/qa/restore-demo-attribution-live-data/task-4/absolute-final/launch.txt`
- `.omo/qa/restore-demo-attribution-live-data/task-4/absolute-final/crash-check.txt`
- `.omo/qa/restore-demo-attribution-live-data/task-1/target-and-baseline.md`
- `.omo/plans/restore-demo-attribution-live-data.md`
- Git commit/diff `273efe2624bd158d766db9557538c904c7a04b15`

### Source/state/tests

- `Android-apk/app/src/main/java/com/rehealth/genie/ui/AttributionScreen.kt`
- `Android-apk/app/src/main/java/com/rehealth/genie/ui/AttributionUiMapper.kt`
- `Android-apk/app/src/main/java/com/rehealth/genie/ui/AttributionUiModels.kt`
- `Android-apk/app/src/main/java/com/rehealth/genie/ui/ReHealthApp.kt`
- `Android-apk/app/src/main/java/com/rehealth/genie/ui/theme/Theme.kt`
- `Android-apk/app/src/test/java/com/rehealth/genie/ui/AttributionUiStateTest.kt`

