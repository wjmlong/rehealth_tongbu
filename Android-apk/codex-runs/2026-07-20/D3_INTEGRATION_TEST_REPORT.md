# D3 Android Auth + Typed Feedback — Integration Test Report

- **Branch:** `work/D3_android_auth_typed_feedback`
- **Date:** 2026-07-20
- **Scope:** Manual execution of the D3 integration guide (Tasks 1–5). Task 6 (device QA) is **not executable** in this environment — see §6.
- **Verification method:** Static source verification (read/grep cross-checks of every call site and dependency contract). **The Gradle build was NOT run** — see §5.

---

## 1. Tasks completed

| Task | Description | Status |
|------|-------------|--------|
| 1 | Login flow integration — real JeecgBoot login + D3 auth hooks | ✅ Code complete, statically verified |
| 2 | Logout flow integration — cancel worker, pause queue, clear session | ✅ Code complete, statically verified |
| 3 | Replace `submitCheckIn` with typed feedback (completed / not_applicable / skipped) | ✅ Code complete, statically verified |
| 4 | Add `QueueStatusBanner` to `ReHealthApp` | ✅ Wired (component already existed in D3 infra) |
| 5 | Worker init in `Application.onCreate` | ✅ Code complete, statically verified |
| 6 | Device testing / manual QA checklist | ⚠️ Not executable here (no device, no build env) |

---

## 2. Static verification results

All call sites and dependency contracts were cross-checked against the actual codebase (not the guide's line numbers, which assumed a different architecture).

| Check | Result |
|-------|--------|
| `ReHealthApi.mobileLogin` endpoint added (`POST /jeecg-boot/sys/mLogin`) | ✅ |
| `ReHealthMobileApi.mobileLogin` delegation (`unwrap` → `RemotePhmOutcome`) | ✅ |
| `AuthenticatedApiClient.mobileLogin` + `onLoginSuccess`/`onLogout` rebuild `mobileApi` with/without token | ✅ |
| `LoginViewModel` drives session store, auth client, queue resume, worker schedule/trigger | ✅ |
| `LoginScreen(onLoginSuccess)` wired; loading + error states | ✅ |
| `SessionStore` `token`/`userId`/`username`/`isLoggedIn`/`clear` match usage | ✅ |
| `SyncRepository.queueState`/`resumeQueue`/`pauseQueue`/`canUpload` match usage | ✅ |
| `InterventionFeedbackRepository.submitFeedback`/`observePendingFeedback` match usage | ✅ |
| `MeasurementSyncWorker.schedule`/`cancel`/`triggerImmediate` (companion) exist | ✅ |
| `QueueStatusBanner` composable defined (`ui/components/QueueStatusBanner.kt`) + imported | ✅ |
| `MainShell` banner + content layout (no overflow) | ✅ |
| `PatientPlanRow` → 3 `FeedbackButton`s → `InterventionFeedbackViewModel` | ✅ |
| `ProfileScreen` logout dialog → `performLogout` → `onGoToLogin` | ✅ |
| `RingViewModel.submitCheckIn` / `addLocalCheckIn` removed; no dangling refs | ✅ |
| No duplicate DTO classes (`LoginDto` vs `AuthDto`/`MobileDto`) | ✅ |
| `ReHealthApp.kt` imports (`AlertDialog`, `TextButton`, `ExitToApp`, `QueueStatusBanner`, `MeasurementSyncWorker`) | ✅ |

---

## 3. Bugs fixed during integration

### 3.1 `AuthenticatedApiClient` constructor visibility (compile error)
`baseUrl` and `httpClient` were plain constructor parameters, but `onLoginSuccess`/`onLogout`
rebuild `mobileApi` using them inside **member functions**. In Kotlin a non-`val`/`var`
constructor param is only visible in init blocks / property initializers, so this would not
compile ("Unresolved reference"). Promoted both to `private val`.

### 3.2 `ApiResult` redeclaration collision (build-breaking)
Seven untracked orphan files (`ApiClient.kt`, `ApiException.kt`, `ApiResult.kt`,
`AuthInterceptor.kt`, `DeepSeekApiClient.kt`, `LunaApiClient.kt`, `PiasApi.kt`) formed a
parallel network refactor that declared a Gson-based `ApiResult` colliding with D3's sealed
`ApiResult` (same package `com.rehealth.genie.network`). Renamed to `.disabled` (repo
convention — reversible, no git history lost). Verified orphaned: nothing in committed D3
code references them.

---

## 4. Files changed (by task)

**Task 1 — Login**
- `network/dto/LoginDto.kt` *(new)* — `MobileLoginRequest`/`MobileLoginResponse`/`LoginUserInfo`
- `network/ReHealthApi.kt` — `mobileLogin` endpoint
- `network/ReHealthMobileApi.kt` — `mobileLogin` delegation
- `network/AuthenticatedApiClient.kt` — `mobileLogin` + `onLoginSuccess`/`onLogout` token rebuild
- `ui/LoginViewModel.kt` *(new)*
- `ui/LoginScreen.kt` — `onLoginSuccess` contract

**Task 2 — Logout** (logic lives in `AuthenticatedApiClient.onLogout`; UI in Task 3's file)
- `ui/ReHealthApp.kt` — `ProfileScreen` logout dialog → `performLogout` (cancel worker, clear session, pause queue)

**Task 3 — Typed feedback**
- `ui/InterventionFeedbackViewModel.kt` *(new)*
- `ring/RingViewModel.kt` — removed `submitCheckIn` / `addLocalCheckIn`
- `ui/ReHealthApp.kt` — `PatientPlanRow` 3-button feedback; `AttributionScreen` creates `InterventionFeedbackViewModel`

**Task 4 — QueueStatusBanner**
- `ui/components/QueueStatusBanner.kt` (already in D3 infra) + `ui/ReHealthApp.kt` wiring

**Task 5 — Worker init**
- `ReHealthApplication.kt` — `MeasurementSyncWorker.schedule(this)` on startup if `sessionStore.isLoggedIn`

**Enhancement (verification finding)**
- `ui/ReHealthApp.kt` — hoisted `onboardingComplete`; re-login now returns to `Main` instead of
  re-running the interview/device-setup flow.

---

## 5. Build limitation (important)

This Windows host has **no JDK installed** (`JAVA_HOME` unset, `java`/`gradlew` launcher exits 49).
The project is normally built from **WSL2** (`local.properties` points at `/mnt/d/Android_SDK`).
Therefore:

- **No `assembleDebug` was run.** All verification above is static.
- Compile correctness rests on the call-site/contract cross-checks in §2, plus the two bug
  fixes in §3. There is residual risk of an unverified Kotlin/Compose compile error.
- **Action required:** run `./gradlew assembleDebug` from WSL2 before merging.

---

## 6. Deviations from the guide

1. **Login DTO field names are unconfirmed.** `MobileLoginRequest(mobile, captcha)` follows the
   D3 contract, but JeecgBoot's `/sys/mLogin` commonly expects `username`/`password` (±`captcha`).
   Adjust the property names in `LoginDto.kt` and the call in `LoginViewModel.login` once the
   live endpoint is confirmed.
2. **Seven orphaned network-refactor files disabled** (`.disabled`) to resolve the `ApiResult`
   collision — see §3.2.
3. **Logout UI is committed together with banner/worker** (Task 4/5) because `ReHealthApp.kt` is
   shared across Tasks 1–4. The guide's 5-commit split is approximated as 5 commits where the
   logout/UI-integration slice is one commit.
4. **Re-login routing** improved beyond the guide (§4 enhancement).

---

## 7. Task 6 — device testing status (Emulator QA executed 2026-07-20 ~12:39–12:48)

### Environment
- **Emulator:** `emulator-5554` (Android 12 / API 31, Google APIs)
- **Build:** WSL2 Gradle (`~/rehealthAI-android`, Linux SDK) → `BUILD SUCCESSFUL in 32s`
- **APK:** `app-debug.apk` (21.3 MB, includes login fix + DeepSeek integration)
- **Backend:** Host `127.0.0.1:8080/jeecg-boot`, reachable from emulator via `10.0.2.2:8080`
- **Login credentials tested:** `admin` / `123456`

### Prerequisite: Login field correction
Before QA execution, the login DTO was corrected to match the real backend contract:
- **Before:** `MobileLoginRequest(mobile, captcha)` — matched `/sys/phoneLogin`
- **After:** `MobileLoginRequest(username, password)` — matches `/sys/mLogin` (verified by reading `LoginController.java` source + `curl` test)
- Files changed: `LoginDto.kt`, `AuthenticatedApiClient.kt`, `LoginViewModel.kt`, `LoginScreen.kt`

### QA Checklist Results

| # | Test case | Status | Evidence |
|---|-----------|--------|----------|
| ① | Real JeecgBoot login returns token; `SessionStore` persists; banner clears | ✅ **PASS** | `admin/123456` → HTTP 200 + token returned; persisted to `EncryptedSharedPreferences` (`rehealth_session.xml`); navigated through 8-step onboarding → main dashboard; no `QueueStatusBanner` error visible |
| ② | 401 mid-session → queue pauses; `QueueStatusBanner` shows "session expired"; re-login resumes | ⚠️ **NOT TESTED** | Requires token invalidation or mock 401 response; current session valid |
| ③ | Typed feedback (completed / not_applicable / skipped) persists locally, uploads via worker | ⚠️ **NEEDS REAL DEVICE** | Emulator has no BLE ring → no health data → cannot trigger feedback submission flow |
| ④ | Logout cancels `MeasurementSyncWorker`, pauses queue, clears session | ⚠️ **CODE VERIFIED** | `ProfileScreen` → logout dialog → `performLogout()` path exists in `ReHealthApp.kt:1663–1688`; UI accessible via bottom-nav "我的" tab (not reached in current onboarding sub-flow) |
| ⑤ | App restart with stored session auto-schedules the sync worker | ⚠️ **NEEDS RESTART TEST** | `Application.onCreate` calls `MeasurementSyncWorker.schedule()`; untested after cold start |

### Screenshots collected
| File | Content |
|------|---------|
| `qa_01_login.png` | Login screen — "账号登录" title, username/password fields confirmed |
| `qa_02_filled.png` | Fields filled: admin + password dots |
| `qa_03_login_click.png` | After login click — navigated to onboarding step 1/8 |
| `qa_13_home_final.png` | Onboarding complete — "健康初识已完成" summary |
| `qa_15_dashboard.png` | Main dashboard — device connection page (ring not connected) |

### Summary
The **login-to-dashboard critical path is fully functional** on emulator against real JeecgBoot backend.
Remaining items (②④⑤) need either session manipulation, navigation to Profile tab, or app restart — all feasible in follow-up testing.
Item ③ requires a physical MRD ring (or BLE simulator).

---

## 8. Risks & next steps

- ~~**Highest risk:** unverified compilation (no JDK).~~ → **RESOLVED:** `BUILD SUCCESSFUL in 32s` (WSL2 Gradle).
- ~~**Confirm the real `/sys/mLogin` request shape.~~ → **RESOLVED:** Verified against `LoginController.java` source; DTO corrected to `username`/`password`.
- **Remaining QA:** Items ②(401), ③(BLE feedback), ④(logout UI), ⑤(restart) need follow-up testing.
- Consider committing the login field correction + DeepSeek integration changes.
- The DeepSeek client compiles safely with empty key (graceful degradation to local fallback).
