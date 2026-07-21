# D3 UI Integration Prompt (2026-07-20)

## Context

D3 core infrastructure has been implemented:
- **Commit**: `67f77df` feat(android): D3 auth-aware upload queue and typed feedback
- **Branch**: `work/D3_android_auth_typed_feedback`
- **Status**: ⚠️ Core infrastructure complete, UI integration pending

**Core Components Ready**:
- `AuthenticatedApiClient` - 401/403 detection ✅
- `SyncRepository` - Queue pause/resume with `queueState` Flow ✅
- `InterventionFeedbackRepository` - Local-first feedback submission ✅
- `SessionStore` - Token storage and auth lifecycle ✅
- Room migrations 2→3 - `sync_upload_queue` and `intervention_feedback_queue` tables ✅

**Missing**:
- UI integration (replace legacy `submitCheckIn`, observe `queueState`, re-login flow)
- Worker implementation (background upload)
- Automated tests

This prompt addresses **UI integration only**. Worker and tests are separate follow-up tasks.

---

## Task: D3 UI Integration - Auth-Aware UI and Typed Feedback

Replace legacy patient check-in with typed intervention feedback and implement auth-aware UI that responds to queue pause/resume states.

---

## Working Directory

```
D:\rehealthAI\Android-apk
```

---

## Prerequisites

Before starting, verify:

1. **Current branch**:
   ```powershell
   git branch --show-current
   ```
   Expected: `work/D3_android_auth_typed_feedback`

2. **Core infrastructure present**:
   ```powershell
   git log --oneline --decorate -3
   ```
   Expected: `67f77df feat(android): D3 auth-aware upload queue and typed feedback`

3. **Required files exist**:
   ```powershell
   ls app/src/main/java/com/rehealth/genie/network/AuthenticatedApiClient.kt
   ls app/src/main/java/com/rehealth/genie/data/sync/SyncRepository.kt
   ls app/src/main/java/com/rehealth/genie/data/sync/InterventionFeedbackRepository.kt
   ls app/src/main/java/com/rehealth/genie/network/SessionStore.kt
   ```

---

## Implementation Plan

### Phase 1: Inspect Existing UI

#### Step 1.1: Locate Legacy Check-In

```powershell
grep -r "submitCheckIn" app/src/main/java/com/rehealth/genie/ui/
```

Expected result: `ReHealthApp.kt:onCheckIn = ringViewModel::submitCheckIn`

#### Step 1.2: Read Current UI Structure

Read the following files to understand current UI architecture:
- `app/src/main/java/com/rehealth/genie/ui/ReHealthApp.kt`
- Find where risk/intervention is displayed (likely `ModelScreen.kt` or `RiskScreen.kt`)
- Find login screen (search for "LoginScreen" or "login")
- Find profile/settings screen (search for "ProfileScreen" or "logout")

#### Step 1.3: Inspect SessionStore

Read `app/src/main/java/com/rehealth/genie/network/SessionStore.kt` to understand:
- How `login(username, password, token)` should be called
- How `logout()` should be called
- How `isLoggedIn` and `token` are exposed

#### Step 1.4: Inspect SyncRepository

Read `app/src/main/java/com/rehealth/genie/data/sync/SyncRepository.kt` to understand:
- How `queueState: StateFlow<QueueState>` is exposed
- How `resumeQueue()` should be called after login

---

### Phase 2: Create QueueStatusBanner Component

#### Step 2.1: Create Banner Composable

Create `app/src/main/java/com/rehealth/genie/ui/QueueStatusBanner.kt`:

```kotlin
package com.rehealth.genie.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.rehealth.genie.data.sync.QueueState

/**
 * D3 queue status banner shown when uploads are paused due to 401.
 */
@Composable
fun QueueStatusBanner(
    queueState: QueueState,
    onReLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (queueState == QueueState.Paused) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Row(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Warning",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Uploads Paused",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "Your session has expired. Please log in again to resume uploads.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onReLoginClick,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Re-login")
                }
            }
        }
    }
}
```

**Validation**:
- File created
- No compilation errors
- Banner shows when `queueState == QueueState.Paused`
- "Re-login" button calls `onReLoginClick` callback

---

### Phase 3: Wire SessionStore to LoginScreen

#### Step 3.1: Locate or Create LoginScreen

If `LoginScreen.kt` exists:
```powershell
ls app/src/main/java/com/rehealth/genie/ui/LoginScreen.kt
```

If not found, search for login UI:
```powershell
grep -r "password" app/src/main/java/com/rehealth/genie/ui/ | grep -i "text"
```

#### Step 3.2: Wire SessionStore.login()

Modify `LoginScreen.kt` (or create if missing):

**Key changes**:
1. Accept `sessionStore: SessionStore` parameter
2. On successful backend login response:
   ```kotlin
   sessionStore.login(username = username, password = password, token = response.token)
   ```
3. Call `syncRepository.resumeQueue()` after login

**Example**:
```kotlin
@Composable
fun LoginScreen(
    sessionStore: SessionStore,
    syncRepository: SyncRepository,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Username field
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        // Password field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Login button
        Button(
            onClick = {
                scope.launch {
                    isLoading = true
                    errorMessage = null
                    try {
                        // Call backend /sys/mLogin
                        val response = reHealthApi.login(username, password)
                        if (response.success) {
                            // Store token
                            sessionStore.login(username, password, response.result.token)
                            // Resume queue
                            syncRepository.resumeQueue()
                            // Navigate away
                            onLoginSuccess()
                        } else {
                            errorMessage = response.message ?: "Login failed"
                        }
                    } catch (e: Exception) {
                        errorMessage = e.message ?: "Network error"
                    } finally {
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading && username.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp))
            } else {
                Text("Login")
            }
        }
        
        // Error message
        errorMessage?.let {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
```

**Validation**:
- User can enter username/password
- Login button calls backend `/sys/mLogin`
- On success: `sessionStore.login()` called with token
- On success: `syncRepository.resumeQueue()` called
- On success: Navigate away from login screen

---

### Phase 4: Wire SessionStore.logout() to ProfileScreen

#### Step 4.1: Locate ProfileScreen

```powershell
grep -r "Profile" app/src/main/java/com/rehealth/genie/ui/ | grep "Screen"
```

or

```powershell
grep -r "logout" app/src/main/java/com/rehealth/genie/ui/
```

#### Step 4.2: Add Logout Button

Modify `ProfileScreen.kt` (or create if missing):

**Key changes**:
1. Accept `sessionStore: SessionStore` parameter
2. Add "Logout" button
3. On click:
   ```kotlin
   sessionStore.logout()
   syncRepository.pauseQueue() // Optional: pause queue on manual logout
   // Navigate to login screen
   ```

**Example**:
```kotlin
@Composable
fun ProfileScreen(
    sessionStore: SessionStore,
    syncRepository: SyncRepository,
    onLogoutSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        Text("Profile", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(24.dp))
        
        // User info
        Text("Logged in as: ${sessionStore.username ?: "Unknown"}")
        Spacer(modifier = Modifier.height(16.dp))
        
        // Logout button
        Button(
            onClick = {
                sessionStore.logout()
                syncRepository.pauseQueue()
                onLogoutSuccess()
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Logout")
        }
    }
}
```

**Validation**:
- Logout button visible
- On click: `sessionStore.logout()` called
- On click: Navigate to login screen
- Token cleared from SessionStore

---

### Phase 5: Replace Legacy submitCheckIn with Typed Feedback

#### Step 5.1: Remove Legacy submitCheckIn from ReHealthApp

Open `app/src/main/java/com/rehealth/genie/ui/ReHealthApp.kt`.

Find:
```kotlin
onCheckIn = ringViewModel::submitCheckIn,
```

Remove this line and the corresponding parameter from the composable that receives it.

#### Step 5.2: Create Typed Feedback UI in Intervention Screen

Find where interventions are displayed (search for "intervention" or "Intervention"):
```powershell
grep -r "intervention" app/src/main/java/com/rehealth/genie/ui/ -i
```

Modify the intervention display composable to add feedback buttons:

**Example** (pseudo-code, adapt to actual UI structure):
```kotlin
@Composable
fun InterventionCard(
    intervention: Intervention,
    feedbackRepository: InterventionFeedbackRepository,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var feedbackSubmitted by remember { mutableStateOf(false) }

    Card(modifier = modifier.fillMaxWidth().padding(16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(intervention.title, style = MaterialTheme.typography.titleMedium)
            Text(intervention.description, style = MaterialTheme.typography.bodyMedium)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (!feedbackSubmitted) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Helpful button
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                feedbackRepository.submitFeedback(
                                    interventionId = intervention.id,
                                    feedbackType = "helpful"
                                )
                                feedbackSubmitted = true
                            }
                        }
                    ) {
                        Text("👍 Helpful")
                    }
                    
                    // Not helpful button
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                feedbackRepository.submitFeedback(
                                    interventionId = intervention.id,
                                    feedbackType = "not_helpful"
                                )
                                feedbackSubmitted = true
                            }
                        }
                    ) {
                        Text("👎 Not Helpful")
                    }
                    
                    // Dismiss button
                    TextButton(
                        onClick = {
                            scope.launch {
                                feedbackRepository.submitFeedback(
                                    interventionId = intervention.id,
                                    feedbackType = "dismissed"
                                )
                                feedbackSubmitted = true
                            }
                        }
                    ) {
                        Text("Dismiss")
                    }
                }
            } else {
                Text(
                    "Thank you for your feedback!",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
```

**Validation**:
- Three buttons visible: "Helpful", "Not Helpful", "Dismiss"
- On click: `feedbackRepository.submitFeedback()` called
- Feedback stored in `intervention_feedback_queue` Room table
- UI shows "Thank you" message after submission

---

### Phase 6: Integrate QueueStatusBanner into ReHealthApp

#### Step 6.1: Modify ReHealthApp Scaffold

Open `app/src/main/java/com/rehealth/genie/ui/ReHealthApp.kt`.

Add:
1. `syncRepository` parameter
2. `queueState` collection
3. `QueueStatusBanner` above main content

**Example**:
```kotlin
@Composable
fun ReHealthApp(
    syncRepository: SyncRepository,
    sessionStore: SessionStore,
    navController: NavHostController = rememberNavController()
) {
    val queueState by syncRepository.queueState.collectAsState()
    
    Scaffold(
        topBar = { /* ... */ },
        bottomBar = { /* ... */ }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // D3 queue status banner
            QueueStatusBanner(
                queueState = queueState,
                onReLoginClick = {
                    navController.navigate("login")
                }
            )
            
            // Main content
            NavHost(
                navController = navController,
                startDestination = if (sessionStore.isLoggedIn) "home" else "login"
            ) {
                composable("login") {
                    LoginScreen(
                        sessionStore = sessionStore,
                        syncRepository = syncRepository,
                        onLoginSuccess = { navController.navigate("home") }
                    )
                }
                composable("home") { /* ... */ }
                composable("profile") {
                    ProfileScreen(
                        sessionStore = sessionStore,
                        syncRepository = syncRepository,
                        onLogoutSuccess = { navController.navigate("login") }
                    )
                }
                // ... other routes
            }
        }
    }
}
```

**Validation**:
- Banner hidden when `queueState == QueueState.Active`
- Banner visible when `queueState == QueueState.Paused`
- "Re-login" button navigates to login screen
- After login, banner disappears (queue resumed)

---

### Phase 7: Wire Dependencies in Application

#### Step 7.1: Modify ReHealthApplication

Open `app/src/main/java/com/rehealth/genie/ReHealthApplication.kt`.

Ensure:
1. `SessionStore` is created (singleton or injected)
2. `SyncRepository` is created with `AuthenticatedApiClient` and `SessionStore`
3. `InterventionFeedbackRepository` is created

**Example** (pseudo-code, adapt to actual DI setup):
```kotlin
class ReHealthApplication : Application() {
    
    lateinit var sessionStore: SessionStore
    lateinit var syncRepository: SyncRepository
    lateinit var feedbackRepository: InterventionFeedbackRepository
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize SessionStore
        sessionStore = SessionStore(
            prefs = getSharedPreferences("session", MODE_PRIVATE)
        )
        
        // Initialize AuthenticatedApiClient
        val apiClient = AuthenticatedApiClient(
            baseUrl = "https://your-backend.com",
            httpClient = OkHttpClient(),
            sessionStore = sessionStore
        )
        
        // Initialize SyncRepository
        val database = Room.databaseBuilder(
            this,
            AppDatabase::class.java,
            "rehealth_db"
        ).build()
        
        syncRepository = SyncRepository(
            dao = database.uploadQueueDao(),
            apiClient = apiClient
        )
        
        // Initialize InterventionFeedbackRepository
        feedbackRepository = InterventionFeedbackRepository(
            dao = database.interventionFeedbackDao(),
            apiClient = apiClient
        )
    }
}
```

#### Step 7.2: Pass Dependencies to ReHealthApp

In `MainActivity.kt`, pass repositories to `ReHealthApp`:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val app = application as ReHealthApplication
        
        setContent {
            ReHealthTheme {
                ReHealthApp(
                    syncRepository = app.syncRepository,
                    sessionStore = app.sessionStore,
                    feedbackRepository = app.feedbackRepository
                )
            }
        }
    }
}
```

**Validation**:
- Application compiles
- `ReHealthApp` receives all required dependencies
- No null pointer exceptions on app launch

---

## Validation

### Step 1: Build

```powershell
.\gradlew.bat assembleDebug
```

Expected: `BUILD SUCCESSFUL`

### Step 2: Manual Testing Checklist

1. **Login Flow**:
   - [ ] Launch app → Login screen shown (if not logged in)
   - [ ] Enter username/password → Login button enabled
   - [ ] Click login → Backend `/sys/mLogin` called
   - [ ] Success → Token stored in `SessionStore`
   - [ ] Success → Navigate to home screen
   - [ ] Banner not visible (queue active)

2. **401 Simulation**:
   - [ ] Manually clear token from SessionStore (or wait for expiry)
   - [ ] Trigger API call (e.g., feature evaluate)
   - [ ] 401 detected → Queue pauses
   - [ ] Banner appears: "Uploads Paused"
   - [ ] Click "Re-login" → Navigate to login screen
   - [ ] Login again → Token stored, queue resumes
   - [ ] Banner disappears

3. **Typed Feedback**:
   - [ ] Navigate to intervention screen
   - [ ] See "Helpful", "Not Helpful", "Dismiss" buttons
   - [ ] Click "Helpful" → Feedback stored in `intervention_feedback_queue` table
   - [ ] UI shows "Thank you" message
   - [ ] Verify Room table: `SELECT * FROM intervention_feedback_queue;`

4. **Logout**:
   - [ ] Navigate to profile screen
   - [ ] Click "Logout" → Token cleared
   - [ ] Navigate to login screen

5. **Legacy submitCheckIn Removed**:
   - [ ] Search codebase: `grep -r "submitCheckIn" app/src/main/java/`
   - [ ] Expected: No results (or only in legacy tests/comments)

### Step 3: Database Verification

```powershell
# Use Android Studio Database Inspector or adb
adb shell
run-as com.rehealth.genie
sqlite3 /data/data/com.rehealth.genie/databases/rehealth_db

# Check feedback queue
SELECT * FROM intervention_feedback_queue;

# Expected columns: id, interventionId, feedbackType, timestamp, uploaded, uploadedAt
```

### Step 4: Git Status

```powershell
git status --short
```

Expected: Only UI files modified, no untracked critical files.

---

## Definition of Done

- [ ] `QueueStatusBanner.kt` created
- [ ] `LoginScreen.kt` wires `SessionStore.login()` and `syncRepository.resumeQueue()`
- [ ] `ProfileScreen.kt` wires `SessionStore.logout()`
- [ ] Legacy `submitCheckIn` removed from `ReHealthApp.kt`
- [ ] Intervention UI has "Helpful/Not Helpful/Dismiss" buttons
- [ ] `ReHealthApp.kt` observes `queueState` and shows banner when paused
- [ ] `ReHealthApplication.kt` creates and wires `SessionStore`, `SyncRepository`, `InterventionFeedbackRepository`
- [ ] `.\gradlew.bat assembleDebug` succeeds
- [ ] Manual testing checklist completed (8/8 items)
- [ ] `intervention_feedback_queue` populated with test feedback
- [ ] No `grep -r "submitCheckIn"` results in UI code

---

## Commit Message

```
feat(android): D3 UI integration - auth-aware UI and typed feedback

Completes D3 user-facing auth flow and typed intervention feedback.

Changed:
- Created QueueStatusBanner.kt - shows "Uploads paused" when 401 detected
- Wired SessionStore.login() to LoginScreen
- Wired SessionStore.logout() to ProfileScreen
- Replaced legacy submitCheckIn with typed feedback buttons (helpful/not_helpful/dismissed)
- ReHealthApp now observes SyncRepository.queueState and prompts re-login on pause
- ReHealthApplication wires SessionStore, SyncRepository, InterventionFeedbackRepository

User flow:
1. Login → token stored → queue active
2. 401 detected → queue pauses → banner shows "Please re-login"
3. Re-login → queue resumes → banner disappears
4. Intervention feedback → local queue → background upload (worker pending)

Tests: Manual QA checklist passed (login, 401 recovery, typed feedback, logout)

Integration work: Worker implementation and automated tests deferred to follow-up commits.

Ref: ACCEPTANCE_REVIEW_2026-07-20.md D3 UI integration
Ref: D3_status.md Section 6
```

---

## Next Steps (After This Task)

1. **D3 Worker Implementation** - Background upload of queued items
2. **D3 Automated Tests** - Unit/integration tests for auth-aware sync
3. **D3 Remote Push** - Push branch to origin for review
4. **Cross-service E2E QA** - Validate full auth flow end-to-end

---

## Troubleshooting

### Issue: SessionStore not found

**Solution**: Verify `SessionStore.kt` exists:
```powershell
ls app/src/main/java/com/rehealth/genie/network/SessionStore.kt
```

If missing, read D3_status.md Section 5 for SessionStore specification.

### Issue: SyncRepository.queueState not found

**Solution**: Verify `SyncRepository.kt` exposes `queueState: StateFlow<QueueState>`:
```powershell
grep "queueState" app/src/main/java/com/rehealth/genie/data/sync/SyncRepository.kt
```

### Issue: InterventionFeedbackRepository not found

**Solution**: Verify it exists:
```powershell
ls app/src/main/java/com/rehealth/genie/data/sync/InterventionFeedbackRepository.kt
```

### Issue: Room migration 2→3 not applied

**Solution**: Uninstall app and reinstall to force fresh database:
```powershell
adb uninstall com.rehealth.genie
.\gradlew.bat installDebug
```

---

**End of D3 UI Integration Prompt**
