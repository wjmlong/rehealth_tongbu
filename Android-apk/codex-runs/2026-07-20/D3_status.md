# D3 Android Auth + Typed Feedback Status

Date: 2026-07-20

## Scope

Workstream: `D3_android_auth_typed_feedback`

This implements:
1. **401 detection and queue pause/resume** - Handle token expiry gracefully
2. **Typed intervention feedback** - Replace legacy `submitCheckIn` with proper feedback queue
3. **Auth-aware API client** - Detect unauthorized state and pause uploads
4. **Local-first feedback queue** - Never lose user feedback, upload asynchronously

## Changed Files

### New Files Created

1. **app/src/main/java/com/rehealth/genie/network/AuthenticatedApiClient.kt**
   - Wraps `ReHealthMobileApi` with 401 detection
   - Returns typed `ApiResult<T>` with auth states (Unauthorized, Forbidden)
   - Tracks `AuthState` (Authorized/Unauthorized)
   - Provides `onLoginSuccess()` and `onLogout()` lifecycle hooks

2. **app/src/main/java/com/rehealth/genie/data/sync/InterventionFeedbackEntity.kt**
   - Room entity for typed intervention feedback
   - Fields: interventionId, status, note, checkedAt, uploadStatus, uploadAttempts
   - Status values: "completed", "partially_completed", "skipped", "not_applicable"

3. **app/src/main/java/com/rehealth/genie/data/sync/InterventionFeedbackDao.kt**
   - DAO for intervention feedback queue
   - Methods: pendingUploads(), getLatestForIntervention(), observePendingFeedback()
   - Supports retry with exponential backoff

4. **app/src/main/java/com/rehealth/genie/data/sync/InterventionFeedbackRepository.kt**
   - High-level API for submitting and uploading feedback
   - `submitFeedback()` - always succeeds locally, queues for upload
   - `uploadFeedback()` - attempts upload, returns null on 401 (queue paused)
   - Exponential backoff: 30s → 60s → 120s → ... (max 30 min)

5. **docs/D3_AUTH_TYPED_FEEDBACK.md**
   - Complete D3 documentation
   - Migration guide from legacy `submitCheckIn` to typed feedback
   - E1.2 contract compliance notes
   - Database schema (migration 2→3)
   - Testing strategy and manual QA steps

### Modified Files

1. **app/src/main/java/com/rehealth/genie/data/sync/SyncRepository.kt**
   - Added queue state tracking (Active/Paused)
   - Added `canUpload()` - checks auth state and queue state
   - Added `pauseQueue()` / `resumeQueue()` methods
   - Added `handleResult()` - processes ApiResult and pauses on 401
   - Now requires `AuthenticatedApiClient` dependency

2. **app/src/main/java/com/rehealth/genie/data/sync/UploadQueueDao.kt**
   - Added `getById(id)` query
   - Added `getPendingByKind(kind)` query for filtering by upload type

3. **app/src/main/java/com/rehealth/genie/data/AppDatabase.kt**
   - Added `UploadQueueEntity` and `InterventionFeedbackEntity` to entities list
   - Added `uploadQueueDao()` and `interventionFeedbackDao()` abstract methods
   - Bumped version from 2 to 3
   - Added Migration2To3:
     - Creates `sync_upload_queue` table with indices
     - Creates `intervention_feedback_queue` table with indices

## E1.2 Contract Compliance

Based on the frozen backend mobile auth contract (E1.2):

✅ **Token Header**: Uses `X-Access-Token` (via existing `AuthInterceptor`)
✅ **No Refresh Token**: 401 requires full re-login, no refresh endpoint exists
✅ **401 Handling**: Detected by `AuthenticatedApiClient`, pauses queue
✅ **403 Handling**: Cross-user feedback marked as permanent failure
✅ **Ownership**: Backend enforces `LoginUser.id`, clients cannot override

## Implementation Details

### Auth Flow

```text
Login → sessionStore.token = jwt → authenticatedApiClient.onLoginSuccess(jwt) → syncRepository.resumeQueue()
Logout → authenticatedApiClient.onLogout() → syncRepository.pauseQueue() → sessionStore.clear()
401 Detected → authState = Unauthorized → pauseQueue() → return ApiResult.Unauthorized
```

### Feedback Flow

```text
User submits feedback → InterventionFeedbackRepository.submitFeedback()
  → Save to intervention_feedback_queue (uploadStatus = "pending")
  → Worker calls uploadFeedback() → POST /rehealth/mobile/interventions/{id}/feedback
    → Success → uploadStatus = "done"
    → 401 → return null, pause queue
    → Network error → retry with exponential backoff
```

### Queue Pause/Resume

```kotlin
// On 401
syncRepository.pauseQueue()  // Sets queueState = Paused
syncRepository.canUpload()   // Returns false

// After re-login
authenticatedApiClient.onLoginSuccess(token)
syncRepository.resumeQueue()  // Sets queueState = Active (if authorized)
syncRepository.canUpload()    // Returns true
```

## Database Schema (Migration 2→3)

### sync_upload_queue
- Generic queue for all upload types
- Used for future telemetry batch, feature evaluate, attribution uploads

### intervention_feedback_queue
- Dedicated feedback queue
- References intervention ID (not legacy item ID)
- Separate upload status from feedback completion status

## Testing

### Build Verification (Pending)
- ❌ `./gradlew testDebugUnitTest` - not run yet (requires build environment)
- ❌ `./gradlew assembleDebug` - not run yet (requires build environment)

### Unit Tests (To Be Added)
- `AuthenticatedApiClient` 401/403 detection
- `SyncRepository.pauseQueue()` / `resumeQueue()` state transitions
- `InterventionFeedbackRepository.uploadFeedback()` retry logic
- Exponential backoff calculation

### Integration Tests (To Be Added)
- Queue pause on 401, resume after re-login
- Feedback persists locally when offline
- No data loss during network failures

### Manual QA Checklist
1. [ ] Submit feedback while offline → verify queued locally
2. [ ] Go online → verify auto-upload
3. [ ] Force 401 (expire token) → verify queue pauses
4. [ ] Re-login → verify queue resumes and flushes pending
5. [ ] Submit feedback with wrong intervention ID → verify 403 handling
6. [ ] Network failure → verify exponential backoff

## Known Limitations

1. **Worker not implemented** - `MeasurementSyncWorker` to drain queue is deferred
2. **UI not updated** - Legacy `submitCheckIn` calls still exist in RingViewModel/ReHealthApp
3. **No queue state UI** - User doesn't see "Syncing..." or "Paused" indicator
4. **No unit tests** - Test coverage deferred to separate task
5. **Build not verified** - Cannot run gradle commands in this environment

## Next Steps (Priority Order)

1. **Immediate (P0)**: Update UI to use `InterventionFeedbackRepository` instead of `submitCheckIn`
2. **Immediate (P0)**: Implement `MeasurementSyncWorker` to drain feedback queue
3. **High (P1)**: Add login/logout hooks in UI to call `onLoginSuccess()` / `onLogout()`
4. **High (P1)**: Add queue state observer to show sync status in UI
5. **Medium (P2)**: Add unit tests for all D3 components
6. **Medium (P2)**: Remove legacy `ReHealthBackendClient.submitCheckIn()` completely
7. **Low (P3)**: Add WorkManager constraints (network, battery)

## Dependencies

### Requires (Already Exists)
- `SessionStore` - encrypted token storage
- `AuthInterceptor` - injects `X-Access-Token` header
- `ReHealthMobileApi` - typed Retrofit interface
- `RemotePhmError` - error types from D1
- E1.2 frozen auth contract

### Provides (For Other Components)
- `AuthenticatedApiClient` - 401-aware API wrapper
- `InterventionFeedbackRepository` - typed feedback API
- `SyncRepository.pauseQueue()` / `resumeQueue()` - queue control
- Database migration 2→3 - sync tables

## Validation Results

### Git Status (Before Commit)
```
On branch work/D3_android_auth_typed_feedback
Untracked files:
  app/src/main/java/com/rehealth/genie/network/AuthenticatedApiClient.kt
  app/src/main/java/com/rehealth/genie/data/sync/InterventionFeedbackEntity.kt
  app/src/main/java/com/rehealth/genie/data/sync/InterventionFeedbackDao.kt
  app/src/main/java/com/rehealth/genie/data/sync/InterventionFeedbackRepository.kt
  docs/D3_AUTH_TYPED_FEEDBACK.md

Changes not staged:
  app/src/main/java/com/rehealth/genie/data/sync/SyncRepository.kt
  app/src/main/java/com/rehealth/genie/data/sync/UploadQueueDao.kt
  app/src/main/java/com/rehealth/genie/data/AppDatabase.kt
```

### Build Commands (Not Run)
Cannot execute build commands in WSL2 environment without Android SDK/JDK.
Build verification deferred to user's Windows environment:
```powershell
cd D:\rehealthAI\Android-apk
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
```

### Code Review Checklist
- [x] No PII logged (token only via encrypted SessionStore)
- [x] No raw exceptions swallowed (all converted to ApiResult types)
- [x] Database migration preserves existing data (adds new tables only)
- [x] Exponential backoff prevents thundering herd
- [x] 401 pauses queue (doesn't retry forever)
- [x] 403 marked as permanent failure (doesn't retry)
- [x] Local-first (feedback never lost)
- [x] E1.2 contract compliant (no refresh token assumed)

## Remaining Work for D3 Completion

### Core Implementation (This Status)
✅ AuthenticatedApiClient with 401 detection
✅ InterventionFeedbackRepository with typed feedback
✅ SyncRepository queue pause/resume
✅ Database migration 2→3
✅ Documentation (D3_AUTH_TYPED_FEEDBACK.md)

### Integration Work (Separate Commits)
❌ Update RingViewModel to use InterventionFeedbackRepository
❌ Update ReHealthApp UI to remove legacy submitCheckIn
❌ Implement MeasurementSyncWorker to drain queue
❌ Add login/logout lifecycle hooks
❌ Add queue state UI indicator

### Testing (Separate Commits)
❌ Unit tests for AuthenticatedApiClient
❌ Unit tests for InterventionFeedbackRepository
❌ Unit tests for SyncRepository
❌ Integration tests for queue pause/resume
❌ Manual QA on physical device

## Release Blocker Status

**D3 Core Implementation**: ✅ **COMPLETED** (this commit)

**D3 Full Integration**: ❌ **PENDING** (requires UI updates + worker + tests)

D3 unblocks:
- Cross-service E2E QA (after UI integration)
- Physical MRD QA (can run in parallel)

## Commit Message

```
feat(android): D3 auth-aware upload queue and typed feedback

Implements 401 detection, queue pause/resume, and typed intervention
feedback to replace legacy submitCheckIn pattern.

Core D3 features:
- AuthenticatedApiClient wraps ReHealthMobileApi, detects 401/403
- InterventionFeedbackRepository provides typed feedback queue
- SyncRepository supports pauseQueue()/resumeQueue() on 401
- Database migration 2→3 adds sync_upload_queue and intervention_feedback_queue
- E1.2 contract compliant (no refresh token, 401 requires re-login)

Integration work (UI updates, worker, tests) deferred to follow-up commits.

Ref: ACCEPTANCE_REVIEW_2026-07-16.md D3 release blocker
Ref: backend E1.2 frozen auth contract
Ref: docs/D3_AUTH_TYPED_FEEDBACK.md
```

## Notes

This is the **core infrastructure commit** for D3. It provides all the building blocks for auth-aware sync, but doesn't yet integrate with the UI or implement the worker.

The separation allows:
1. Core logic to be reviewed/tested independently
2. UI changes to be staged separately (smaller PRs)
3. Worker implementation to be optimized/tested separately

**Definition of Done for D3 Core**: This commit completes the infrastructure. D3 will be fully **RESOLVED** after UI integration + worker + tests in follow-up commits.
