# D3 Worker Implementation - Complete

Date: 2026-07-20  
File: `app/src/main/java/com/rehealth/genie/work/MeasurementSyncWorker.kt`

---

## Overview

`MeasurementSyncWorker` is a periodic WorkManager worker that drains the intervention feedback queue asynchronously.

## Features

### Periodic Sync
- Runs every **30 minutes**
- Constraints:
  - Network connected
  - Battery not low
- Exponential backoff on failure

### Auth-Aware
- Checks `syncRepository.canUpload()` before running
- Exits early if queue is paused or unauthorized
- Resumes automatically after re-login

### Feedback Upload Flow
1. Check if queue can upload (not paused, authorized)
2. Fetch pending feedback via `feedbackRepo.getPendingUploads()`
3. For each item:
   - Call `feedbackRepo.uploadFeedback(item)`
   - If returns null → 401 detected, stop worker
   - If returns updated item → save status (done/failed/retry)
4. Prune old completed items (7+ days old)
5. Log results: uploaded count, failed count, paused flag

### Public API

```kotlin
// Schedule periodic worker (call after login or app startup)
MeasurementSyncWorker.schedule(context)

// Cancel worker (call after logout)
MeasurementSyncWorker.cancel(context)

// Trigger immediate sync (call after feedback submission)
MeasurementSyncWorker.triggerImmediate(context)
```

---

## Integration Points

### 1. App Startup
Add to `ReHealthApplication.onCreate()`:
```kotlin
override fun onCreate() {
    super.onCreate()
    // ... existing code
    if (sessionStore.isLoggedIn) {
        MeasurementSyncWorker.schedule(this)
    }
}
```

### 2. Login Flow
After successful login:
```kotlin
sessionStore.token = response.token
authenticatedApiClient.onLoginSuccess(response.token)
syncRepository.resumeQueue()
MeasurementSyncWorker.schedule(context)  // Schedule worker
MeasurementSyncWorker.triggerImmediate(context)  // Immediate sync
```

### 3. Logout Flow
Before logout:
```kotlin
MeasurementSyncWorker.cancel(context)  // Cancel worker
authenticatedApiClient.onLogout()
syncRepository.pauseQueue()
```

### 4. Feedback Submission
After user submits feedback:
```kotlin
interventionFeedbackRepo.submitFeedback(interventionId, status, note)
MeasurementSyncWorker.triggerImmediate(context)  // Trigger immediate upload
```

---

## Behavior

### Normal Flow
```
1. Worker starts every 30 minutes
2. Check canUpload() → true
3. Fetch pending items → [item1, item2, item3]
4. Upload item1 → success → mark as done
5. Upload item2 → network error → retry with backoff
6. Upload item3 → success → mark as done
7. Prune old items
8. Worker completes
```

### 401 Detection Flow
```
1. Worker starts
2. Check canUpload() → true
3. Fetch pending items → [item1, item2]
4. Upload item1 → 401 detected
5. InterventionFeedbackRepository returns null
6. Worker logs "401 detected, queue paused"
7. Worker exits early (doesn't process item2)
8. SyncRepository.pauseQueue() already called
9. Worker won't run again until re-login calls resumeQueue()
```

### Queue Paused Flow
```
1. Worker starts
2. Check canUpload() → false (queue paused)
3. Worker logs "Queue paused, skipping sync"
4. Worker exits immediately
5. No API calls made
```

---

## Configuration

### Periodic Schedule
- **Interval**: 30 minutes
- **Policy**: KEEP (don't replace existing work)
- **Constraints**: Network + Battery not low

### Retry Policy
- **Backoff**: Exponential
- **Min backoff**: 10 seconds (WorkRequest.MIN_BACKOFF_MILLIS)
- **Max attempts**: 10 (permanent failure after)

### Pruning
- Completed items older than **7 days** are deleted
- Runs after every sync

---

## Testing

### Unit Tests (To Be Added)
- Worker runs when authorized
- Worker skips when paused
- Worker stops on 401 detection
- Worker retries on network error
- Worker prunes old items

### Manual QA
1. Submit feedback while online → verify immediate upload
2. Submit feedback while offline → go online → verify upload within 30 min
3. Force 401 → verify worker stops and logs "queue paused"
4. Re-login → verify worker resumes and uploads pending items
5. Submit 10 feedback items → verify all uploaded
6. Check logs for "uploaded=N, failed=N, paused=false"

---

## Dependencies

### Required
- WorkManager (already in dependencies)
- InterventionFeedbackRepository (D3 core)
- SyncRepository (D3 core)
- ReHealthApplication (updated with D3 dependencies)

### Not Required
- UI changes (worker is background-only)
- Login/logout integration (can be added separately)

---

## Known Limitations

1. **Fixed 30-minute interval** - Could be made configurable
2. **No UI notification** - Worker runs silently, user doesn't see progress
3. **No batch upload** - Uploads one item at a time (could be optimized)
4. **No priority queue** - FIFO order only
5. **Manual integration needed** - schedule()/cancel() must be called by app

---

## Next Steps

### Immediate (Required for Full D3)
1. Add `MeasurementSyncWorker.schedule()` to app startup
2. Add `MeasurementSyncWorker.triggerImmediate()` to feedback submission
3. Add worker calls to login/logout flows (when implemented)

### Optional (Nice to Have)
4. Add notification when sync completes (N items uploaded)
5. Add exponential interval (5 min → 15 min → 30 min if no pending items)
6. Add batch upload support
7. Add priority levels (urgent feedback uploaded first)
8. Add unit tests

---

## Files Modified

```
A  app/src/main/java/com/rehealth/genie/work/MeasurementSyncWorker.kt
A  codex-runs/2026-07-20/D3_WORKER_COMPLETE.md (this file)
```

---

## Summary

✅ **MeasurementSyncWorker COMPLETE**

The worker:
- Runs every 30 minutes with network/battery constraints
- Drains intervention feedback queue asynchronously
- Pauses on 401 detection (resumes after re-login)
- Retries transient failures with exponential backoff
- Prunes old completed items
- Provides schedule/cancel/triggerImmediate API

**Integration**: Requires manual wiring to app startup, login/logout, and feedback submission points.

**Testing**: Fully testable independently of UI (no UI changes needed).

**Status**: Ready for integration and testing.
