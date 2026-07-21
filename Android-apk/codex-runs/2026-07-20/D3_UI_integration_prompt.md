# D3 UI Integration Prompt

Date: 2026-07-20  
Workstream: D3_android_auth_typed_feedback  
Phase: UI Integration  
Dependencies: D3 core infrastructure (commit 67f77df)

---

## Context

D3 core infrastructure is complete:
- ✅ AuthenticatedApiClient with 401 detection
- ✅ InterventionFeedbackRepository with typed feedback queue
- ✅ SyncRepository with pauseQueue()/resumeQueue()
- ✅ Database migration 2→3 (sync tables)

Now we need to **integrate with the UI** to replace legacy `submitCheckIn` and expose queue state to users.

---

## Goals

1. **Wire SessionStore to login/logout flows** - Call lifecycle hooks
2. **Remove legacy submitCheckIn** - Replace with typed feedback
3. **Add queue state UI** - Show "Syncing..." / "Paused" banner
4. **Wire dependencies in Application** - Provide repositories to ViewModels
5. **Add typed feedback buttons** - Helpful/Not Helpful/Dismiss

---

## Tasks

### Task A: Create QueueStatusBanner.kt

Create a Composable banner that observes queue state and shows:
- **Active + uploading**: "Syncing feedback..." (blue banner)
- **Paused**: "Session expired, please login to sync" (yellow banner, clickable to login)
- **Active + no pending**: Hide banner

File: `app/src/main/java/com/rehealth/genie/ui/components/QueueStatusBanner.kt`

```kotlin
@Composable
fun QueueStatusBanner(
    queueState: QueueState,
    pendingCount: Int,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier,
)
```

### Task B: Wire SessionStore.login() to LoginScreen

Update login flow to call:
1. `sessionStore.token = response.token`
2. `sessionStore.userId = response.userId`
3. `authenticatedApiClient.onLoginSuccess(response.token)`
4. `syncRepository.resumeQueue()`

File: `app/src/main/java/com/rehealth/genie/ui/LoginScreen.kt` (or ViewModel)

### Task C: Wire SessionStore.logout() to ProfileScreen

Update logout flow to call:
1. `authenticatedApiClient.onLogout()`
2. `syncRepository.pauseQueue()`
3. Navigate to login screen

File: `app/src/main/java/com/rehealth/genie/ui/ProfileScreen.kt` (or similar)

### Task D: Remove Legacy submitCheckIn

Remove all calls to:
- `ReHealthBackendClient.submitCheckIn()`
- `RingViewModel.submitCheckIn()`

Files to update:
- `app/src/main/java/com/rehealth/genie/ring/RingViewModel.kt`
- `app/src/main/java/com/rehealth/genie/ui/ReHealthApp.kt`

### Task E: Add Typed Feedback Buttons

Replace generic "打卡" (check-in) button with typed feedback:
- "✓ Helpful" → status = "completed"
- "✗ Not Helpful" → status = "not_applicable"
- "Later" → status = "skipped"

Each button calls:
```kotlin
interventionFeedbackRepo.submitFeedback(
    interventionId = intervention.id,
    status = "completed", // or "not_applicable", "skipped"
    note = null
)
```

File: `app/src/main/java/com/rehealth/genie/ui/ReHealthApp.kt` (intervention section)

### Task F: Observe queueState in ReHealthApp

Add to ReHealthApp composable:
```kotlin
val queueState by syncRepository.queueState.collectAsState()
val pendingFeedback by interventionFeedbackRepo.observePendingFeedback().collectAsState(initial = emptyList())

Column {
    QueueStatusBanner(
        queueState = queueState,
        pendingCount = pendingFeedback.size,
        onLoginClick = { navController.navigate("login") }
    )
    // ... rest of UI
}
```

File: `app/src/main/java/com/rehealth/genie/ui/ReHealthApp.kt`

### Task G: Wire Dependencies in ReHealthApplication

Update `ReHealthApplication.kt` to provide:
```kotlin
class ReHealthApplication : Application() {
    lateinit var sessionStore: SessionStore
    lateinit var authenticatedApiClient: AuthenticatedApiClient
    lateinit var syncRepository: SyncRepository
    lateinit var interventionFeedbackRepo: InterventionFeedbackRepository
    
    override fun onCreate() {
        super.onCreate()
        val db = AppDatabase.create(this)
        sessionStore = SessionStore(this)
        
        val httpClient = OkHttpClient.Builder().build()
        authenticatedApiClient = AuthenticatedApiClient(
            baseUrl = "http://10.0.2.2:8080/jeecg-boot",
            httpClient = httpClient,
            sessionStore = sessionStore
        )
        
        syncRepository = SyncRepository(
            dao = db.uploadQueueDao(),
            apiClient = authenticatedApiClient
        )
        
        interventionFeedbackRepo = InterventionFeedbackRepository(
            dao = db.interventionFeedbackDao(),
            apiClient = authenticatedApiClient
        )
    }
}
```

File: `app/src/main/java/com/rehealth/genie/ReHealthApplication.kt`

---

## Implementation Order

1. **Task G first** - Wire dependencies in Application
2. **Task A** - Create QueueStatusBanner
3. **Task B & C** - Wire login/logout
4. **Task D** - Remove legacy submitCheckIn
5. **Task E** - Add typed feedback buttons
6. **Task F** - Observe queue state

---

## Validation Checklist

After implementation:
- [ ] App compiles: `./gradlew assembleDebug`
- [ ] No legacy `submitCheckIn` references remain
- [ ] Login flow calls `onLoginSuccess()` and `resumeQueue()`
- [ ] Logout flow calls `onLogout()` and `pauseQueue()`
- [ ] Queue banner shows when feedback is pending
- [ ] Queue banner shows "Session expired" when paused
- [ ] Typed feedback buttons enqueue feedback locally
- [ ] Manual test: Submit feedback while offline → verify queued
- [ ] Manual test: Force 401 → verify banner shows "Session expired"

---

## Known Edge Cases

1. **No intervention ID available** - If backend doesn't return intervention IDs yet, use a placeholder UUID and document as limitation
2. **Multiple interventions** - Each intervention should have its own feedback buttons
3. **Already submitted feedback** - Disable buttons if feedback already exists for this intervention
4. **Queue banner positioning** - Should be at top of screen, above navigation

---

## Files to Modify

1. `app/src/main/java/com/rehealth/genie/ReHealthApplication.kt` - Wire dependencies
2. `app/src/main/java/com/rehealth/genie/ui/components/QueueStatusBanner.kt` - New file
3. `app/src/main/java/com/rehealth/genie/ui/LoginScreen.kt` - Wire login hooks
4. `app/src/main/java/com/rehealth/genie/ui/ProfileScreen.kt` - Wire logout hooks
5. `app/src/main/java/com/rehealth/genie/ring/RingViewModel.kt` - Remove submitCheckIn
6. `app/src/main/java/com/rehealth/genie/ui/ReHealthApp.kt` - Add banner, typed feedback buttons

---

## Success Criteria

✅ All legacy `submitCheckIn` calls removed  
✅ Login/logout flows call auth lifecycle hooks  
✅ Queue status banner visible and functional  
✅ Typed feedback buttons enqueue feedback locally  
✅ App compiles without errors  
✅ Git diff clean (only D3 UI changes)

---

## Next Steps After UI Integration

1. Implement `MeasurementSyncWorker` to drain feedback queue
2. Add unit tests for D3 components
3. Manual QA on device
4. Push to origin
5. Cross-service E2E QA

---

**Estimated Time**: 3-4 hours  
**Priority**: P0 (blocks E2E QA)  
**Dependencies**: D3 core (commit 67f77df)
