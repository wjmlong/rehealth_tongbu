# restore-demo-attribution-live-data - Work Plan

## TL;DR (For humans)
<!-- Fill this LAST, after the detailed plan below is written, so it summarizes the REAL plan. -->
<!-- Plain English for a non-engineer: NO file paths, NO todo numbers, NO wave/agent/tool names. -->

**What you'll get:** The Demo attribution screen shown in screenshots 4-6 restored in the canonical Android app, while its score, trends, PIAS forecasts, behavior, contribution factors and interventions come from current real data sources or honest same-layout empty states.

**Why this approach:** The target UI already exists in historical Compose code and the real MR11/Room/Jeecg/PIAS stack already exists in the canonical repository. A typed adapter between them is the smallest change that preserves both.

**What it will NOT do:** It will not restore fixed demo values, copy the inferior real-branch UI, redesign unrelated pages, or commit credentials/generated APKs.

**Effort:** Large
**Risk:** High - real API/state integration and screenshot-level Compose fidelity must pass together.
**Decisions to sanity-check:** Historical layout is structural reference only; unavailable data remains empty within the same target card.

Your next move: Execute this already-approved plan.

---

> TL;DR (machine): Restore target Compose anatomy via pure live-data mappers, test/build/emulator-compare, preserve dirty work, then commit/push codex/real-device.

## Scope
### Must have
- Work only in canonical Git root `D:/rehealthAI`; preserve existing dirty root files.
- Restore target attribution structure/interactions from `7008f2f`: period selector, improvement/current-risk card, mini chart, dual PIAS chart/legend/metrics, behavior card, grouped expandable factors, intervention plan.
- Map current canonical Jeecg feature evaluation, risk history, PIAS, Room activity/MR11 and intervention data; no mock fallback.
- Define improvement as `(oldest selected-period confirmed risk - newest confirmed risk) * 100` percentage points, signed and rounded to one decimal; if fewer than two confirmed points exist, show `--` and an inline accumulation message.
- Define 7/30/90-day selector as a filter for persisted confirmed risk history and improvement baseline only. PIAS forecast remains explicitly a 30-day forecast and does not pretend to change horizon with the selector.
- Render the historical behavior-card anatomy as a real activity card using Room steps/duration/calories/distance; never label ring activity as a meal or invent macronutrients.
- Supported actions are: period switch; contribution factor expand/collapse; intervention plan expand/collapse; existing server-backed feedback where an intervention ID exists. No fake add-to-plan success state.
- The bottom-tab `AttributionScreen` remains navigation owner and consumes a single `AttributionUiState`; `AttributionReportScreen` remains a standalone detailed PIAS surface and is not substituted into the tab.
- The screen-level owner loads confirmed risk history and PIAS in a keyed `LaunchedEffect`/cancellable coroutine, ignores stale results after key change, exposes retry from the attribution tab, and preserves the last successful content only with a visible refreshing/error status.
- Keep loading/empty/error/retry inside target card anatomy and prevent large blank cards.
- Add focused tests and fresh emulator screenshots/motion/UI hierarchy evidence.
- Run Gradle tests/lint/build, review, commit legitimate dirty project code and push `codex/real-device`.
### Must NOT have (guardrails, anti-slop, scope boundaries)
- No fixed demo score/meal/calorie/factor/plan values in production.
- No unrelated UI/backend/schema refactor; no inferior UI imports.
- No JWT/password/token/API key/local.properties/APK/raw health data in Git.
- No overwrite/revert of pre-existing dirty files.

## Verification strategy
> Zero human intervention - all verification is agent-executed.
- Test decision: TDD for pure state mappers; tests-after plus fresh emulator visual QA for Compose.
- Evidence: `.omo/qa/restore-demo-attribution-live-data/task-<N>/`, redacted and untracked.

## Execution strategy
### Parallel execution waves
> Target 5-8 todos per wave. Fewer than 3 (except the final) means you under-split.
- Wave 1: mapper/state characterization and implementation.
- Wave 2: Compose target restoration and focused tests.
- Wave 3: Gradle build plus emulator visual/runtime QA.
- Wave 4: review, report, secret/scope audit, commit and push.

### Dependency matrix
| Todo | Depends on | Blocks | Can parallelize with |
| --- | --- | --- | --- |
| 1 | None | 2 | None |
| 2 | 1 | 3 | None |
| 3 | 2 | 4 | None |
| 4 | 3 | 5 | None |
| 5 | 4 | F1-F4 | None |

## Todos
> Implementation + Test = ONE todo. Never separate.
<!-- APPEND TASK BATCHES BELOW THIS LINE WITH edit/apply_patch - never rewrite the headers above. -->
- [ ] 1. Characterize target, current states and regression
  What to do / Must NOT do: Pin historical target component/order/interactions, current canonical risk/PIAS/history/activity/intervention state, and failing current emulator UI. No product edits or real secrets.
  Parallelization: Wave 1 | Blocked by: none | Blocks: 2
  References (executor has NO interview context - be exhaustive): `D:/rehealth_demo/Android-apk` commit `7008f2f`; `Android-apk/app/src/main/java/com/rehealth/genie/ui/ReHealthApp.kt:1313`; user screenshots; current DTO/repository/state files.
  Acceptance criteria (agent-executable): Evidence names every target visible element and its real data source or honest empty state; current failing screenshot/UI XML captured.
  QA scenarios (name exact tool + invocation): ADB `emulator-5554` screenshot/uiautomator current attribution; source diff against `7008f2f`. Evidence `.omo/qa/restore-demo-attribution-live-data/task-1/`.
  Commit: N

- [ ] 2. Add typed real-data attribution view state and tests
  What to do / Must NOT do: Introduce the smallest pure Kotlin `AttributionUiState` mapper for canonical feature evaluation, confirmed `RiskHistoryEntity` rows, PIAS forecast, Room activity and real server interventions. The canonical 16-key order is `age, gender, bmi, sbp, dbp, fasting_glucose, total_cholesterol, ldl, hdl, triglycerides, exercise_days, smoking, drinking, diabetes_history, hypertension_history, family_history`; normalize camel/snake aliases, group into the four historical sections, render absent contributions as missing/not supplied instead of dropping/truncating them. Extend PIAS DTOs with `status`, `history_days`, `min_history_days` and ATT-availability fields that the service actually returns. Treat `accumulating`, `ready`, ATT-unavailable, failed, refreshing and empty distinctly. Exclude `initialFallbackMvp`/heuristic interventions from attribution unless a payload has a real server intervention ID; replay data is allowed only in debug builds and must remain visibly labeled. Never invent absent data.
  Parallelization: Wave 1 | Blocked by: 1 | Blocks: 3
  References: current feature evaluation DTO/status, `PiasApiClient.kt`, risk-history repository/DAO, `RingUiState`, intervention DTOs, historical target models.
  Acceptance criteria: Unit tests fail first, then pass for 2.19% risk; signed percentage-point improvement from oldest/newest confirmed points for 7/30/90 windows; fewer-than-two history rows; unequal/empty PIAS arrays; PIAS accumulating/ready/ATT-unavailable; exact 16 factor order and missing rows; camel/snake aliases; activity absent/present; debug replay label; fallback intervention exclusion; stale refresh result suppression and retry states.
  QA scenarios: `.\gradlew.bat testDebugUnitTest --tests '*Attribution*' --tests '*Pias*'`; malformed/empty fixtures do not throw. Evidence `.omo/qa/restore-demo-attribution-live-data/task-2/`.
  Commit: N

- [ ] 3. Restore target Compose attribution anatomy with real state
  What to do / Must NOT do: Surgically restore historical card order/layout/interaction helpers in the bottom-tab `AttributionScreen` and bind only `AttributionUiState`. Keep navigation/theme/other screens and standalone `AttributionReportScreen` untouched. Use period switch, factor expand/collapse and plan expand/collapse; invoke existing feedback only for real intervention IDs. Use Room activity values in the historical behavior-card anatomy. No hardcoded production demo values and no new Compose test dependency; emulator ADB hierarchy/screenshot/motion capture is the explicit rendered-surface acceptance mechanism.
  Parallelization: Wave 2 | Blocked by: 2 | Blocks: 4
  References: historical `AttributionScreen`, `RiskForecastChart`, `AttributionFactorRow`; current `ReHealthApp.kt` and mapper.
  Acceptance criteria: Screenshots 4-6 structure exists; ready state has no blank card; period/factor/plan actions work; missing/error states retain card shells; CJK fits.
  QA scenarios: mapper/unit tests plus debug APK install; SDK-local ADB hierarchy, matching-scroll screenshots and screenrecord/start-mid-end captures. Exact target references: `C:/Users/wjmlong/AppData/Local/Temp/codex-clipboard-5225098f-c281-4643-a359-18534546938d.png`, `codex-clipboard-fd52493f-fa6c-4bd6-a38c-711b8548048f.png`, `codex-clipboard-59460fe8-0d16-4a6f-9e07-71294dab2db6.png`. Evidence `.omo/qa/restore-demo-attribution-live-data/task-3/`.
  Commit: N

- [ ] 4. Build and independently validate emulator fidelity/runtime
  What to do / Must NOT do: Run unit/lint/debug/release/instrumentation as available; install exact APK on API31 emulator, capture every scroll region/state and compare to target; test backend failure/retry without altering UI.
  Parallelization: Wave 3 | Blocked by: 3 | Blocks: 5
  References: Gradle wrapper, target screenshots, `D:/Android_SDK/platform-tools/adb.exe`, existing Android tests.
  Acceptance criteria: Build/test gates pass; fresh captures show target anatomy and real/replay provenance; animation progresses; no crash/blank/clipped text; independent visual reviewers approve.
  QA scenarios: `.\gradlew.bat --no-daemon testDebugUnitTest lintDebug assembleDebug assembleRelease`; ADB install/input/uiautomator/screencap/screenrecord. Evidence `.omo/qa/restore-demo-attribution-live-data/task-4/`.
  Commit: N

- [ ] 5. Review all dirty code, document, commit and push
  What to do / Must NOT do: Preserve/review existing dirty root changes, update integration report, run secret/scope/diff checks, then use two commit allowlists. Feature commit: only Android attribution state/DTO/repository/UI/tests and integration-report edits produced/verified by this task. Separate synchronization commit: pre-existing `.gitattributes`, `.gitignore`, `sync_to_github.sh`, `_lfs_*.sh`, and output screenshots only after independent inspection proves they contain no secrets, unsafe paths, generated archives or broken commands. Never stage `.omo`, `Android-apk/build/`, APKs, `local.properties`, `sync_to_github.log`, raw QA/health/API data, or any unreviewed archive. Push current branch.
  Parallelization: Wave 4 | Blocked by: 4 | Blocks: F1-F4
  References: root `git status`; `.gitignore`; `outputs/demo_ui_live_api_integration_report.md`; sync/LFS scripts; origin remote.
  Acceptance criteria: No secret/generated APK committed; intended dirty code accounted for; commit SHA recorded; `git push origin codex/real-device` succeeds; remote equals local; status clean except intentional ignored artifacts.
  QA scenarios: `git diff --check`, secret scan, staged diff review, `git push`, fetch/rev-parse comparison. Evidence `.omo/qa/restore-demo-attribution-live-data/task-5/`.
  Commit: Y | `feat(android): restore live data in demo attribution UI`

## Final verification wave
> Runs in parallel after ALL todos. ALL must APPROVE. Surface results and wait for the user's explicit okay before declaring complete.
- [ ] F1. Plan compliance audit
  Independent reviewer checks user screenshots, real-data constraint, dirty-file preservation and exact commit.
- [ ] F2. Code quality review
  Independent reviewer checks mapper/state/Compose/error/security quality.
- [ ] F3. Real manual QA
  Independent QA repeats full fresh emulator visual/runtime scenario.
- [ ] F4. Scope fidelity
  Independent reviewer proves no unrelated UI, mock-as-real, credential, or omitted legitimate dirty code.

## Commit strategy
- One or more atomic conventional commits on existing `codex/real-device`; do not amend/rebase/reset.
- Push after all verification/review gates.
- Exclude secrets, APKs, local.properties, build outputs and raw health/QA data.

## Success criteria
- Target Demo attribution UI is restored in canonical app with real Jeecg/PIAS/MR11/Room data adapters.
- All target regions/interactions/states render correctly on emulator; no blank ready card or CJK clipping.
- Automated Android gates pass or exact environmental blockers are documented.
- All legitimate dirty project changes are committed and pushed; sensitive/generated artifacts remain excluded.
