# D3 Android Auth + Typed Feedback Integration

## Overview

D3 implements authentication-aware upload queuing and typed intervention feedback to replace the legacy `submitCheckIn` pattern.

## Key Features

### 1. 401 Detection and Queue Pause

`AuthenticatedApiClient` wraps all backend API calls and detects HTTP 401 responses:

- When 401 occurs → sets `authState = Unauthorized`
- Returns `ApiResult.Unauthorized` to caller
- `SyncRepository` pauses queue via `pauseQueue()`
- No further upload attempts until re-login

### 2. Typed Intervention Feedback

Replaces legacy `ReHealthBackendClient.submitCheckIn()` with proper intervention-scoped feedback:

```kotlin
InterventionFeedbackRepository.submitFeedback(
    interventionId = "intervention-uuid",
    status = "completed",  // or "partially_completed", "skipped", "not_applicable"
    note = "User note here"
)
```

Each feedback:
- References specific intervention ID (not generic item ID)
- Queued locally first (never lost)
- Uploaded asynchronously with exponential backoff
- Pauses on 401, resumes after re-login

### 3. Upload Queue Infrastructure

**UploadQueueEntity**: Generic queue for all upload types
- `kind`: "telemetry_batch" | "feature_evaluate" | "intervention_feedback"
- `status`: "pending" | "uploading" | "done" | "failed"
- Exponential backoff: 30s → 60s → 120s → ... (max 30 min)

**InterventionFeedbackEntity**: Dedicated feedback queue
- `interventionId`: UUID reference
- `status`: completion status
- `uploadStatus`: separate from feedback status
- `uploadAttempts`: retry counter with backoff

### 4. Auth State Management

```kotlin
// After login
authenticatedApiClient.onLoginSuccess(token)
syncRepository.resumeQueue()

// After logout or 401
authenticatedApiClient.onLogout()
syncRepository.pauseQueue()
```

## E1.2 Contract Compliance

Based on frozen backend auth contract (E1.2):

- **Token**: `X-Access-Token` header (JeecgBoot JWT)
- **Validity**: 15 days (JWT), Redis session 30 days initial TTL
- **No refresh**: 401 requires full re-login via `POST /jeecg-boot/sys/mLogin`
- **403**: Cross-user feedback → permanent failure
- **Ownership**: Backend uses `LoginUser.id`, clients cannot override

## Database Schema (Migration 2→3)

### sync_upload_queue
```sql
CREATE TABLE sync_upload_queue (
    id TEXT PRIMARY KEY,
    kind TEXT NOT NULL,
    payload_json TEXT NOT NULL,
    status TEXT NOT NULL,
    attempts INTEGER DEFAULT 0,
    last_error TEXT,
    created_at INTEGER NOT NULL,
    next_retry_at INTEGER NOT NULL
);
CREATE INDEX index_sync_upload_queue_status_retry ON sync_upload_queue(status, next_retry_at);
```

### intervention_feedback_queue
```sql
CREATE TABLE intervention_feedback_queue (
    id TEXT PRIMARY KEY,
    intervention_id TEXT NOT NULL,
    status TEXT NOT NULL,
    note TEXT,
    checked_at INTEGER NOT NULL,
    created_at INTEGER NOT NULL,
    upload_status TEXT DEFAULT 'pending',
    upload_attempts INTEGER DEFAULT 0,
    last_error TEXT,
    next_retry_at INTEGER DEFAULT 0
);
CREATE INDEX index_intervention_feedback_intervention_id ON intervention_feedback_queue(intervention_id);
CREATE INDEX index_intervention_feedback_upload_status ON intervention_feedback_queue(upload_status, next_retry_at);
```

## API Result Types

`ApiResult<T>` replaces generic `RemotePhmOutcome` for auth-aware paths:

- `Success<T>` - operation succeeded
- `Unauthorized` - 401, pause queue, require re-login
- `Forbidden` - 403, permanent failure
- `InvalidRequest` - 400, permanent failure
- `InvalidResponse` - malformed response, permanent failure
- `ServiceUnavailable` - 503, retry with backoff
- `NetworkError` - timeout/connection, retry with backoff

## Migration Path

### Before (Legacy)
```kotlin
// RingViewModel.submitCheckIn()
client.submitCheckIn(itemId = "item-123", mood = "stable")
```

### After (D3)
```kotlin
// InterventionFeedbackRepository.submitFeedback()
feedbackRepo.submitFeedback(
    interventionId = "intervention-uuid",
    status = "completed",
    note = "Walked 30 minutes"
)
```

## Worker/Service Integration

Sync worker (to be implemented separately):

1. Check `syncRepository.canUpload()` → false if paused or unauthorized
2. Fetch `feedbackRepo.getPendingUploads()`
3. For each feedback:
   ```kotlin
   val updated = feedbackRepo.uploadFeedback(feedback)
   if (updated != null) {
       feedbackRepo.saveFeedback(updated)
   } else {
       // 401 detected, queue paused, stop worker
       break
   }
   ```
4. Prune done items: `feedbackRepo.pruneDone()`

## UI Integration

### Login Flow
```kotlin
// After successful POST /jeecg-boot/sys/mLogin
sessionStore.token = response.token
sessionStore.userId = response.userId
authenticatedApiClient.onLoginSuccess(response.token)
syncRepository.resumeQueue()
// Trigger worker to flush pending uploads
```

### Logout Flow
```kotlin
authenticatedApiClient.onLogout()
syncRepository.pauseQueue()
// Navigate to login screen
```

### 401 Detection
```kotlin
when (val result = apiClient.evaluateFeatures(request)) {
    is ApiResult.Unauthorized -> {
        // Show "Session expired, please login" dialog
        // Navigate to login screen
    }
    is ApiResult.Success -> { /* handle success */ }
    // ...
}
```

## Testing Strategy

### Unit Tests
- `AuthenticatedApiClient` 401/403 handling
- `SyncRepository.pauseQueue() / resumeQueue()`
- `InterventionFeedbackRepository.uploadFeedback()` retry logic
- Exponential backoff calculation

### Integration Tests
- Queue pause on 401, resume after re-login
- Feedback persists locally even if backend unreachable
- No data loss during network failures

### Manual QA
1. Submit feedback while offline → verify queued
2. Go online → verify auto-upload
3. Force 401 (expire token) → verify queue pauses
4. Re-login → verify queue resumes and uploads pending items
5. Submit feedback for wrong intervention ID → verify 403 handling

## Known Limitations

- No refresh token (E1.2 contract), 401 requires full re-login
- No batch feedback upload (one-at-a-time)
- Worker implementation deferred to separate task
- No UI notification when queue pauses (to be added)

## Related Files

### Core Implementation
- `AuthenticatedApiClient.kt` - 401 detection wrapper
- `SyncRepository.kt` - queue pause/resume
- `InterventionFeedbackRepository.kt` - typed feedback queue
- `InterventionFeedbackEntity.kt` - feedback schema
- `InterventionFeedbackDao.kt` - feedback DAO
- `UploadQueueEntity.kt` - generic upload queue schema
- `UploadQueueDao.kt` - generic upload DAO
- `AppDatabase.kt` - migration 2→3
- `SessionStore.kt` - encrypted token storage (existing)
- `AuthInterceptor.kt` - token header injection (existing)

### DTO Layer
- `FeatureEvaluationDtos.kt` - request/response types (existing)
- `ApiResult.kt` - typed result with auth states (new)

### Legacy (To Be Deprecated)
- `ReHealthBackendClient.submitCheckIn()` - replaced by `InterventionFeedbackRepository`
- `CheckInPayload` / `PatientCheckInPayload` - replaced by `InterventionFeedbackRequest`

## Next Steps (Post-D3)

1. Implement `MeasurementSyncWorker` to drain feedback queue
2. Update UI to use `InterventionFeedbackRepository` instead of `submitCheckIn`
3. Add queue state observer to show "Syncing..." / "Paused" indicator
4. Remove legacy `ReHealthBackendClient.submitCheckIn()` calls
5. Add unit tests for all D3 components
6. Cross-service E2E QA: Android → backend → model-service
