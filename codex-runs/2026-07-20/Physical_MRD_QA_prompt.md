# Physical MRD QA Prompt (2026-07-20)

## Context

B1 (BLE background collection) has been implemented in code, but physical hardware validation is pending. This is a **P0 release blocker** that requires real Android 13+ device and MRD ring hardware.

**B1 Implementation Status**:
- Commit: Historical B1 commits in Android-apk
- Branch: `work/B1_fixup_release_blockers`
- Code status: ✅ Implemented
- Build status: ✅ Pass
- Physical QA: ❌ **NOT COMPLETED** (this task)

**B1 Release Blockers** (per ACCEPTANCE_REVIEW_2026-07-10.md):
1. Telemetry reliability - verify data collection continuity
2. Battery drain - measure power consumption over 24h
3. User consent - verify permission prompts and user control

---

## Task: Physical MRD Ring QA on Real Hardware

Validate BLE background collection, battery drain, and user consent on real Android 13+ device with MRD ring hardware.

---

## Prerequisites

### Hardware Requirements

1. **Android Device**:
   - Android 13 or higher (required for BLE background permissions)
   - Battery capacity ≥3000mAh (for meaningful drain measurement)
   - Not rooted (production-like environment)
   - Factory reset or clean test profile (avoid interference from other apps)

2. **MRD Ring**:
   - Fully charged (≥80%)
   - Known MAC address (for binding verification)
   - Known firmware version (for issue triage)

3. **Test Environment**:
   - Stable indoor environment (avoid BLE interference)
   - Charger available (for baseline power measurement)
   - Stopwatch or timer (for duration measurement)

### Software Requirements

1. **ReHealth APK**:
   - Build from latest `work/B1_fixup_release_blockers` branch
   - Debug build acceptable (easier to inspect logs)
   - ProGuard disabled (for readable stack traces)

2. **ADB Tools**:
   - Android SDK platform-tools installed
   - USB debugging enabled on test device
   - `adb devices` shows test device

3. **Battery Monitoring**:
   - Battery Historian (recommended): https://github.com/google/battery-historian
   - Or built-in Android battery stats: `adb shell dumpsys batterystats`

---

## Working Directory

```
D:\rehealthAI\Android-apk
```

---

## Phase 1: Pre-Test Setup

### Step 1.1: Build and Install APK

```powershell
# Clean build
.\gradlew.bat clean

# Build debug APK
.\gradlew.bat assembleDebug

# Install on device
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

**Validation**:
- APK installed successfully
- App launches without crash
- Permissions request shown on first launch

### Step 1.2: Baseline Battery Measurement

**Before starting test**, measure baseline battery drain without app activity:

```powershell
# Reset battery stats
adb shell dumpsys batterystats --reset

# Wait 1 hour (device idle, screen off)
# ... wait ...

# Check baseline drain
adb shell dumpsys battery
```

**Record**:
- Baseline drain rate: ___ % per hour
- Battery temperature: ___ °C
- Expected: <2% per hour idle drain

### Step 1.3: Grant Permissions

Launch app and grant all required permissions:
- ✅ Location (required for BLE scan)
- ✅ Bluetooth
- ✅ Notifications
- ✅ Background location (Android 10+)
- ✅ Nearby devices (Android 12+)

**Validation**:
- All permissions granted
- No permission denial errors in logcat

---

## Phase 2: MRD Ring Binding Test

### Step 2.1: Ring Pairing

1. Launch app
2. Navigate to "Ring Settings" or "Device Binding"
3. Tap "Scan for Ring"
4. Select MRD ring from scan results
5. Tap "Bind"

**Expected Result**:
- Ring appears in scan results within 10 seconds
- Binding succeeds
- Ring MAC address saved to Room database

**Validation**:
```powershell
# Check Room database
adb shell run-as com.rehealth.genie
sqlite3 /data/data/com.rehealth.genie/databases/rehealth_db
SELECT * FROM bound_devices;
.quit
```

**Record**:
- Ring MAC: _______________
- Binding time: ___ seconds
- Success: ✅ / ❌

### Step 2.2: Connection Stability

After binding, verify connection remains stable:

1. Keep app in foreground for 5 minutes
2. Monitor logcat for BLE connection events

```powershell
adb logcat -s ReHealthApp:D MrdSdk:D BluetoothGatt:D | findstr "connect"
```

**Expected Result**:
- Connection established within 10 seconds
- No "disconnected" events during 5-minute window
- GATT characteristic discovery succeeds

**Validation**:
- ✅ Connection stable for 5 minutes
- ✅ No reconnection attempts
- ✅ Characteristic discovery successful

**Record**:
- Connection time: ___ seconds
- Disconnections: ___ times
- Success: ✅ / ❌

---

## Phase 3: Data Collection Continuity Test

### Step 3.1: Foreground Collection (Baseline)

With app in foreground:

1. Monitor logcat for MRD data packets
2. Collect for 5 minutes
3. Verify data persisted to Room

```powershell
# Monitor data packets
adb logcat -s ReHealthApp:D | findstr "MRD\|heart\|spo2"

# After 5 minutes, check Room
adb shell run-as com.rehealth.genie
sqlite3 /data/data/com.rehealth.genie/databases/rehealth_db
SELECT COUNT(*) FROM ring_measurements WHERE timestamp > datetime('now', '-5 minutes');
.quit
```

**Expected Result**:
- Data packets received every 1-5 seconds (ring-dependent)
- ≥50 measurements in 5 minutes
- Room database updated in real-time

**Validation**:
- ✅ Data packets received continuously
- ✅ Room records: ≥50 in 5 minutes
- ✅ No "BLE read failed" errors

**Record**:
- Packets received: ___ in 5 minutes
- Room records: ___
- Success: ✅ / ❌

### Step 3.2: Background Collection (Critical Test)

**This is the key B1 blocker**: Verify data collection continues when app is backgrounded.

1. **Start collection** (app in foreground)
2. **Press Home button** (app goes to background)
3. **Wait 30 minutes**
4. **Check data continuity**

```powershell
# Monitor background data (run in separate terminal)
adb logcat -s ReHealthApp:D ForegroundService:D WorkManager:D | findstr "collect\|measure"

# After 30 minutes, check Room
adb shell run-as com.rehealth.genie
sqlite3 /data/data/com.rehealth.genie/databases/rehealth_db
SELECT COUNT(*) FROM ring_measurements WHERE timestamp > datetime('now', '-30 minutes');
SELECT MIN(timestamp), MAX(timestamp) FROM ring_measurements WHERE timestamp > datetime('now', '-30 minutes');
.quit
```

**Expected Result**:
- Foreground service notification visible
- Data packets continue in logcat (may be less frequent)
- ≥300 measurements in 30 minutes (1 per 6 seconds acceptable)
- No gaps >2 minutes in timestamp sequence

**Validation**:
- ✅ Foreground service running
- ✅ Room records: ≥300 in 30 minutes
- ✅ No gaps >2 minutes
- ✅ BLE connection maintained

**Record**:
- Packets received: ___ in 30 minutes
- Room records: ___
- Largest gap: ___ seconds
- Success: ✅ / ❌

### Step 3.3: App Killed Recovery Test

Verify collection resumes after Android kills the app:

1. **Start collection**
2. **Kill app** via:
   ```powershell
   adb shell am force-stop com.rehealth.genie
   ```
3. **Wait 5 minutes** (WorkManager should restart collection)
4. **Check collection resumed**

```powershell
# Check if app restarted
adb logcat -s ReHealthApp:D | findstr "onCreate\|WorkManager\|collect"

# Check Room data after 5 minutes
adb shell run-as com.rehealth.genie
sqlite3 /data/data/com.rehealth.genie/databases/rehealth_db
SELECT COUNT(*) FROM ring_measurements WHERE timestamp > datetime('now', '-5 minutes');
.quit
```

**Expected Result**:
- WorkManager restarts collection within 15 minutes (Android-dependent)
- Foreground service relaunched
- Data collection resumes

**Validation**:
- ✅ App restarted by WorkManager
- ✅ Collection resumed
- ✅ Room records continue after restart

**Record**:
- Restart time: ___ minutes after kill
- Collection resumed: ✅ / ❌
- Success: ✅ / ❌

---

## Phase 4: Battery Drain Test (24-Hour)

### Step 4.1: Full Discharge Test

**This is a long-running test** - requires 24 hours:

1. **Start conditions**:
   - Battery: 100% (fully charged)
   - App: Background collection running
   - Screen: Off (wake only for checks)
   - Network: WiFi connected (for realistic usage)
   - Other apps: Minimal (production-like)

2. **Reset battery stats**:
   ```powershell
   adb shell dumpsys batterystats --reset
   ```

3. **Run for 24 hours**

4. **Check battery stats**:
   ```powershell
   # Export battery stats
   adb bugreport bugreport.zip

   # Or check inline
   adb shell dumpsys batterystats | findstr "ReHealth\|Estimated"
   ```

**Expected Result**:
- Battery drain: <10% per 24 hours from ReHealth app alone
- No "excessive wake locks" in battery stats
- No ANR (Application Not Responding) events

**Validation**:
- ✅ Total drain: ___% in 24h
- ✅ ReHealth contribution: ___% (should be <10%)
- ✅ No excessive wake locks
- ✅ No ANR events

**Record**:
- Total battery drain: ___% in 24h
- ReHealth drain: ___%
- Wake locks: ___ minutes
- ANR events: ___
- Success: ✅ / ❌

### Step 4.2: Battery Historian Analysis (Optional but Recommended)

Upload `bugreport.zip` to Battery Historian:

1. Open: https://bathist.ef.lc/
2. Upload `bugreport.zip`
3. Find "com.rehealth.genie" in process list
4. Check:
   - Wake lock duration
   - Foreground service time
   - BLE scan frequency
   - Network activity

**Look for red flags**:
- ❌ Continuous wake locks (should be minimal)
- ❌ BLE scan every <1 minute (too frequent)
- ❌ CPU usage spikes when screen off

**Record findings**:
- Wake lock time: ___ minutes / 24h
- BLE scan frequency: ___ per hour
- CPU usage: ___ % average
- Red flags: _______________

---

## Phase 5: User Consent and Control Test

### Step 5.1: Permission Prompts

Uninstall and reinstall app, verify permission UX:

```powershell
adb uninstall com.rehealth.genie
adb install app\build\outputs\apk\debug\app-debug.apk
```

Launch app and verify:
1. ✅ Permission rationale shown **before** requesting permission
2. ✅ "Allow" / "Deny" choice clear
3. ✅ App handles denial gracefully (no crash)
4. ✅ "Background location" explained (why needed for BLE)

**Record**:
- Rationale shown: ✅ / ❌
- Denial handled: ✅ / ❌
- Success: ✅ / ❌

### Step 5.2: User Stop Control

Verify user can stop collection:

1. Navigate to "Ring Settings"
2. Tap "Stop Collection" or "Unbind Ring"
3. Verify:
   - ✅ Foreground service stops
   - ✅ BLE connection closes
   - ✅ No more data in Room
   - ✅ User confirmation dialog shown

```powershell
# Check foreground service stopped
adb shell dumpsys activity services | findstr "ReHealth"
# Should show no foreground service

# Check BLE disconnected
adb logcat -s BluetoothGatt:D | findstr "disconnect"
```

**Record**:
- Service stopped: ✅ / ❌
- BLE disconnected: ✅ / ❌
- User control working: ✅ / ❌

### Step 5.3: Data Privacy

Verify no PII or raw health data in logs:

```powershell
# Capture 5 minutes of logs
adb logcat -d > test_logs.txt

# Search for potential PII
findstr /i "password token username email phone" test_logs.txt
findstr /i "heart_rate spo2 blood_pressure" test_logs.txt
```

**Expected Result**:
- ❌ No passwords/tokens in logs
- ❌ No raw health measurements in logs (only aggregated/anonymized)
- ✅ Only device IDs and timestamps (acceptable)

**Record**:
- PII found: ✅ / ❌ (describe if found)
- Raw health data logged: ✅ / ❌ (describe if found)
- Privacy compliant: ✅ / ❌

---

## Phase 6: Edge Cases and Failure Modes

### Test 6.1: Ring Battery Dead

Simulate ring battery depletion:

1. Let ring battery drain to <5%
2. Observe app behavior

**Expected Result**:
- ✅ App detects ring disconnection
- ✅ User notified: "Ring battery low, please charge"
- ✅ Collection stops gracefully
- ✅ No crash or ANR

**Record**:
- Low battery detected: ✅ / ❌
- User notification: ✅ / ❌
- Success: ✅ / ❌

### Test 6.2: Bluetooth Disabled

Turn off Bluetooth on phone:

1. Swipe down → disable Bluetooth
2. Observe app behavior

**Expected Result**:
- ✅ App detects Bluetooth off
- ✅ User notified: "Bluetooth required for ring"
- ✅ Collection paused (not crashed)
- ✅ When Bluetooth re-enabled, collection resumes

**Record**:
- Bluetooth off detected: ✅ / ❌
- User notification: ✅ / ❌
- Auto-resume: ✅ / ❌
- Success: ✅ / ❌

### Test 6.3: Location Permission Revoked

Revoke location permission while app running:

```powershell
adb shell pm revoke com.rehealth.genie android.permission.ACCESS_FINE_LOCATION
```

**Expected Result**:
- ✅ App detects permission loss
- ✅ Collection stops
- ✅ User prompted to re-grant
- ✅ No crash

**Record**:
- Permission loss detected: ✅ / ❌
- User re-prompt: ✅ / ❌
- Success: ✅ / ❌

### Test 6.4: Ring Out of Range

Walk >10 meters away from ring (beyond BLE range):

**Expected Result**:
- ✅ App detects connection loss within 30 seconds
- ✅ Attempts reconnection (backoff: 10s, 30s, 60s)
- ✅ User notified after 3 failed attempts
- ✅ When back in range, auto-reconnects

**Record**:
- Connection loss detected: ✅ / ❌
- Reconnection attempts: ___
- Auto-reconnect: ✅ / ❌
- Success: ✅ / ❌

---

## Phase 7: Results Documentation

### Step 7.1: Create QA Evidence File

Create `Android-apk/codex-runs/2026-07-20/B1_physical_qa_evidence.md`:

```markdown
# B1 Physical MRD QA Evidence

Date: [YYYY-MM-DD]
Tester: [Name]
Device: [Model], Android [Version]
Ring: MRD [Model], Firmware [Version]

## Summary

- Binding: ✅ / ❌
- Foreground collection: ✅ / ❌
- Background collection (30min): ✅ / ❌
- App killed recovery: ✅ / ❌
- 24h battery drain: ✅ / ❌ ([X]% total, ReHealth [Y]%)
- User consent: ✅ / ❌
- Privacy: ✅ / ❌
- Edge cases: [N]/4 passed

## Detailed Results

[Paste all "Record" sections from phases above]

## Issues Found

[List any failures, with severity: Critical/High/Medium/Low]

## Screenshots

[Attach screenshots of key UX flows]

## Logs

[Attach relevant logcat excerpts or bugreport.zip]

## Recommendation

✅ PASS - B1 ready for release
⚠️ PASS WITH ISSUES - [list must-fix items]
❌ FAIL - [blocking issues]
```

### Step 7.2: Update Acceptance Review

Update `ACCEPTANCE_REVIEW_2026-07-20.md`:

Find **Section 2. Release Blockers**:

```markdown
4. **Physical MRD QA** ✅ **RESOLVED** / ⚠️ **PASS WITH ISSUES** / ❌ **BLOCKED**
   - Evidence: `Android-apk/codex-runs/2026-07-20/B1_physical_qa_evidence.md`
   - Binding: ✅
   - Background collection: ✅
   - Battery drain: [X]% / 24h
   - Issues: [list if any]
```

### Step 7.3: Commit Evidence

```powershell
git add codex-runs/2026-07-20/B1_physical_qa_evidence.md
git commit -m "test(android): B1 physical MRD QA evidence

Validated BLE background collection on real Android [Version] + MRD ring.

Results:
- Binding: ✅
- Background collection 30min: ✅
- App killed recovery: ✅
- 24h battery drain: [X]% (ReHealth [Y]%)
- User consent: ✅
- Privacy: ✅
- Edge cases: [N]/4 passed

[PASS/PASS WITH ISSUES/FAIL]

Device: [Model]
Ring: MRD [Model], FW [Version]
Ref: ACCEPTANCE_REVIEW_2026-07-20.md Physical MRD QA blocker
"
```

---

## Definition of Done

- [ ] Hardware acquired (Android 13+ device, MRD ring)
- [ ] APK installed and permissions granted
- [ ] Ring binding succeeds
- [ ] Foreground collection verified (5 min)
- [ ] Background collection verified (30 min, ≥300 records)
- [ ] App killed recovery verified
- [ ] 24h battery drain measured (<10% from app)
- [ ] User consent flows verified
- [ ] Privacy verified (no PII in logs)
- [ ] 4 edge cases tested (ring battery, BT off, permission revoked, out of range)
- [ ] QA evidence file created
- [ ] Acceptance review updated
- [ ] Git commit with evidence

---

## Success Criteria

**PASS** (minimum requirements):
- ✅ Background collection: ≥300 records in 30 minutes
- ✅ App killed recovery: Auto-resumes within 15 minutes
- ✅ 24h battery drain: <10% from ReHealth app
- ✅ User consent: Rationale shown, denial handled
- ✅ Privacy: No PII/raw health data in logs
- ✅ Edge cases: 3/4 pass

**PASS WITH ISSUES** (acceptable for MVP):
- ⚠️ Background collection: ≥200 records in 30 minutes (gaps <5 min acceptable)
- ⚠️ Battery drain: 10-15% (documented, optimization deferred)
- ⚠️ Edge cases: 2/4 pass (remaining documented as known issues)

**FAIL** (blocking release):
- ❌ Background collection: <200 records or gaps >5 minutes
- ❌ Battery drain: >15%
- ❌ Crashes on permission denial or BT disable
- ❌ PII/raw health data found in logs

---

## Troubleshooting

### Issue: Ring not found in scan

**Check**:
- Ring battery >20%
- Ring not paired to another device
- Location permission granted
- Bluetooth enabled

**Solution**:
```powershell
# Check BLE scan logs
adb logcat -s BluetoothAdapter:D | findstr "scan"
```

### Issue: Background collection stops after 10 minutes

**Likely cause**: Android battery optimization killing app

**Solution**:
```powershell
# Disable battery optimization for ReHealth app
adb shell dumpsys deviceidle whitelist +com.rehealth.genie
```

Or manually: Settings → Apps → ReHealth → Battery → Unrestricted

### Issue: Cannot access Room database via adb

**Solution**:
- Debug build required (run-as only works on debuggable apps)
- Or use Android Studio Database Inspector

---

**End of Physical MRD QA Prompt**
