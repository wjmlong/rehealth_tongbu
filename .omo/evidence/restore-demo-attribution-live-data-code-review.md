# Code Quality Review — restore-demo-attribution-live-data

## Verdict

- **codeQualityStatus:** BLOCK
- **recommendation:** REQUEST_CHANGES
- **review verdict:** FAIL
- **blockers:** Two HIGH correctness findings remain. Do not approve until mock-derived profile values are excluded and the Android PIAS accumulation model is aligned with the deployed real endpoint and covered by a truthful contract test.

## Scope Reviewed

- Goal: restore the historical attribution-tab anatomy using current real Jeecg/PIAS/MR11/Room/server-intervention data, with honest empty states and no mock fallback presented as real.
- Committed range: `5b44b0deefab7cb8f0828f0a2cb2168c35eb518f..273efe2624bd158d766db9557538c904c7a04b15`, restricted to Android attribution/PIAS/domain/navigation/tests.
- Current uncommitted scope: `AppDatabase.kt`, `RiskHistoryMigrationSql.kt`, `RiskHistoryMigrationTest.kt`, `PiasApiClientTest.kt`; late shared-tree UI token edits were also noted but were not the basis of the correctness verdict.
- Full neighboring sources consulted: `RingViewModel`, `RiskHistoryRepository`/DAO/entity, `PiasApiClient`, Core16 feature vector/DTO mapper, feedback ViewModel, generated Room schema, and `rehealth-algorithms` PIAS router/attributor.
- ULW status command: `omo ulw-loop status --json` was unavailable (`omo` not found), so this report uses the required fallback path.

## Skill-Perspective Check

- **Ran:** `rehealth-android-mvp`, `omo:programming` shared type/boundary/test criteria, and the complete `omo:remove-ai-slops` review criteria were consulted before maintainability/test judgment.
- **Programming perspective:** VIOLATED. The UI trusts a provenance-ambiguous `PatientMvpPayload`, and the PIAS contract test does not represent the checked-in/deployed boundary response. Network/Room orchestration also remains inside the composable rather than a ViewModel.
- **Remove-AI-slops perspective:** VIOLATED. `PiasApiClientTest` mirrors invented response fields instead of the real producer contract; `RiskHistoryMigrationTest` asserts generated SQL strings rather than executing observable migration behavior; the 963-line screen remains an oversized mixed-responsibility production file.

## CRITICAL

- None.

## HIGH

### 1. Mock-derived patient facts are displayed as real attribution factor values

- `RingViewModel.kt:36` constructs `initialFallbackMvp`; `RingViewModel.kt:53` installs it into every initial `RingUiState`.
- The fallback builder converts nullable/absent local values to negative facts via comparisons such as `vector.smoking == 1`, `vector.diabetesHistory == 1`, and `vector.familyHistory == 1` at `RingViewModel.kt:529-533`. For the neutral default vector, all five values become `false`, not unknown.
- `AttributionScreen.kt:995-1006` then renders those booleans as concrete `否`/`无` Core16 values. Unlike interventions, factor values are not gated on `risk.mode == "local_heuristic"` (`AttributionScreen.kt:180`).
- Observable effect: before a real patient profile arrives, and after backend failure when no prior real payload exists, the attribution screen asserts that the user does not smoke/drink and has no diabetes, hypertension, or family history. This violates the explicit no-mock/no-invented-data success criterion and can materially misrepresent medical risk context.
- Required correction: carry explicit provenance/unknown state to the attribution mapper and omit fallback-derived profile fields, or keep nullable booleans nullable in the fallback. Add a regression test proving that an initial/backend-failure `local_heuristic` payload produces missing factor values rather than `否`/`无`.

### 2. The green PIAS contract test fabricates metadata absent from the real service, producing an inaccurate accumulation state

- The checked-in real endpoint response at `rehealth-algorithms/api/routers/pias_jeecg.py:303-335` emits `status`, current state, forecast, intervention-effect values, charts, animations, and reports, but not `history_days`, `min_history_days`, `att_available`, `att_unavailable_reason`, `intervention_days`, or `intervention_data_sufficient`.
- Fresh empirical request against the healthy `rehealth-pias:dev` container on `localhost:8200/api/pias/v2/attribute/individual` returned `status: "accumulating"` for one history row and omitted those metadata fields.
- Android maps the absent fields to `null` (`RemotePhmService.kt:178-194`), then defaults the accumulation state to `historyDays = 0` and `minHistoryDays = 14` (`AttributionUiMapper.kt:103-107`). The user-visible card reports those values at `AttributionScreen.kt:420-421`, so one recorded day is shown as zero days.
- `PiasApiClientTest.kt:48-63` builds an envelope containing exactly the non-existent metadata and only proves Gson can deserialize the fixture. It gives false confidence instead of locking compatibility with the producer.
- Required correction: align the producer and consumer contract. Either return the metadata from the PIAS router, or derive the actual submitted history count in the Android domain and avoid asserting unsupported availability fields. Replace/add a contract fixture captured from the real endpoint, including an accumulating response and a ready/ATT-insufficient response.

## MEDIUM

### 3. Migration tests mirror SQL strings instead of proving a lossless 3→4 Room upgrade

- `RiskHistoryMigrationTest.kt:8-69` asserts statement counts and substrings. It never opens SQLite, runs `Migration3To4`, validates Room `TableInfo`, counts copied risk/queue/feedback rows, or proves rollback for an unknown schema.
- The real migration has already had two runtime-only failures documented in the supplied artifacts: missing `user_id` index creation and queue-index schema mismatch. That history demonstrates why string assertions are insufficient.
- The manual fresh-v3 launch/crash check is useful evidence for one known database shape, and the current SQL is plausibly atomic/lossless for that shape, but the automated suite still allows row-copy, affinity, PK, index, or rollback regressions to remain green.
- Required correction: enable exported schemas plus `room-testing` `MigrationTestHelper`, or add an equivalent instrumentation/SQLite migration test with seeded legacy risk, upload-queue, and feedback rows plus an adversarial unsupported schema. Assert data, exact schema, and fail-closed rollback observables.

### 4. Attribution orchestration and the 963-line screen remain maintainability/regression risks

- `AttributionScreen.kt:82-180` owns repository access, network calls, retry sequencing, error conversion, patient/profile adaptation, feature values, intervention filtering inputs, and rendering state inside a composable. The screen now contains roughly 930 nonblank/noncomment lines and more than twenty composables/helpers.
- The reducer test proves stale event IDs, but Compose `LaunchedEffect` already cancels the prior coroutine. No test exercises real effect cancellation/navigation, repository failure after cancellation, or state survival across recreation. State is plain `remember`, not a screen ViewModel or `rememberSaveable`.
- This is not a correctness blocker for the observed happy path, but it conflicts with repository guidance that ViewModels orchestrate UI state and materially raises future regression cost.
- Required correction: move load/retry/reducer ownership and boundary error handling to a dedicated attribution ViewModel/use case, split the screen by card responsibility, and retain pure mapper coverage. Do not add pass-through abstractions.

## LOW

### 5. Risk-history schema recognition validates names only

- `RiskHistoryMigrationSql.kt:4-20` chooses branches from a `Set<String>` of column names. A table with canonical names but wrong affinity/nullability/PK shape gets only index creation and fails later during Room validation, rather than being rejected at schema classification.
- The current branch is fail-closed rather than destructive, so severity is LOW. A future migration helper should inspect `PRAGMA table_info` metadata, not just names.

### 6. Migration fallback remains globally destructive for unregistered paths

- `AppDatabase.kt:205-209` registers 1→2→3→4 migrations but retains `fallbackToDestructiveMigration()`. The reviewed 3→4 path is registered and fail-closed; however, any future missing path can erase locally persisted health/queue data.
- Track removal or strict scoping before production release.

## Positive Findings

- Confirmed/non-mock risk persistence is correctly fail-closed: `RiskHistoryRepository.recordConfirmedRemoteRisk` requires an authenticated user, a score, and `normalizedIsMock == false`.
- PIAS invocation does not substitute a local/mock attribution result, and cancellation is rethrown instead of swallowed.
- Core16 canonical key order matches the feature contract; camel/snake alias normalization and missing contribution rows are covered.
- Local heuristic/idless interventions are excluded, and feedback remains local-first through the existing queue ViewModel.
- The known legacy risk-table transformation preserves rows under a non-user sentinel and refuses unknown column-name sets instead of deleting them.

## Validation Evidence

- `git diff --check 5b44b0d..HEAD -- Android-apk/app/src/main Android-apk/app/src/test`: PASS.
- Focused command: `gradlew.bat --no-daemon testDebugUnitTest --tests ...AttributionUiStateTest --tests ...PiasApiClientTest --tests ...RiskHistoryMigrationTest --tests ...RiskHistoryRepositoryTest --tests ...CvdFeatureVectorDtoMapperTest`: PASS, 26 tests / 0 failures.
- Full command: `gradlew.bat --no-daemon testDebugUnitTest lintDebug assembleDebug`: PASS, 47 tests / 0 failures, lint PASS, debug APK assembled.
- Important qualification: a shared-tree worker modified `AttributionScreen.kt` and added `AttributionTokens.kt` after the full gate began; the two HIGH findings predate and survive those edits, but that late refactor requires its own stable rerun before final approval.
- Live PIAS probe: healthy `rehealth-pias:dev` container, POST to `http://localhost:8200/api/pias/v2/attribute/individual`: success/accumulating response observed with no history/min-history/ATT-availability metadata.
- Static/security scan: N/A for this scoped review; no new health-data/token logging was found in the scoped production code.

## Blockers Before Approval

1. Prevent `local_heuristic`/fallback profile booleans from appearing as real `否`/`无` Core16 facts, with regression coverage of initial and backend-failure states.
2. Align Android PIAS accumulation/ATT metadata with the actual producer response and replace the implementation-mirroring fixture with truthful boundary tests.
3. Re-run focused tests and `testDebugUnitTest lintDebug assembleDebug` on a stable tree after the current late UI-token edits settle.

