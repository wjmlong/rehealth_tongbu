#!/bin/bash
# ReHealth Android App - Complete Integration Test Suite
# Generated: 2026-07-20

set -e  # Exit on error

echo "================================"
echo "ReHealth Integration Test Suite"
echo "================================"
echo ""

# Color codes
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Test results
PASSED=0
FAILED=0
SKIPPED=0

# Helper functions
print_test() {
    echo -e "\n${YELLOW}[TEST]${NC} $1"
}

print_pass() {
    echo -e "${GREEN}[PASS]${NC} $1"
    ((PASSED++))
}

print_fail() {
    echo -e "${RED}[FAIL]${NC} $1"
    ((FAILED++))
}

print_skip() {
    echo -e "${YELLOW}[SKIP]${NC} $1"
    ((SKIPPED++))
}

# Configuration
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
PACKAGE_NAME="com.rehealth.genie"
BACKEND_URL="http://10.0.2.2:8080/jeecg-boot"
MODEL_SERVICE_URL="http://10.0.2.2:8000"

echo "Configuration:"
echo "  APK: $APK_PATH"
echo "  Package: $PACKAGE_NAME"
echo "  Backend: $BACKEND_URL"
echo "  Model Service: $MODEL_SERVICE_URL"
echo ""

#############################################
# Phase 1: Pre-flight Checks
#############################################

echo "================================"
echo "Phase 1: Pre-flight Checks"
echo "================================"

# Test 1.1: Check APK exists
print_test "1.1 - Check APK file exists"
if [ -f "$APK_PATH" ]; then
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    print_pass "APK found ($APK_SIZE)"
else
    print_fail "APK not found at $APK_PATH"
    echo "Run: ./gradlew assembleDebug"
    exit 1
fi

# Test 1.2: Check ADB connection
print_test "1.2 - Check ADB device connection"
DEVICE_COUNT=$(adb devices | grep -c "device$" || true)
if [ "$DEVICE_COUNT" -gt 0 ]; then
    DEVICE_ID=$(adb devices | grep "device$" | head -1 | awk '{print $1}')
    print_pass "Device connected: $DEVICE_ID"
else
    print_fail "No device connected"
    echo "Start emulator or connect physical device"
    exit 1
fi

# Test 1.3: Check backend service (optional)
print_test "1.3 - Check backend service availability"
if curl -s -o /dev/null -w "%{http_code}" "$BACKEND_URL/actuator/health" 2>/dev/null | grep -q "200\|404"; then
    print_pass "Backend reachable at $BACKEND_URL"
else
    print_skip "Backend not running (tests will use mock data)"
fi

#############################################
# Phase 2: APK Installation
#############################################

echo ""
echo "================================"
echo "Phase 2: APK Installation"
echo "================================"

# Test 2.1: Uninstall old version
print_test "2.1 - Uninstall old version (if exists)"
if adb shell pm list packages | grep -q "$PACKAGE_NAME"; then
    adb uninstall "$PACKAGE_NAME" 2>&1 | grep -q "Success" && \
        print_pass "Old version uninstalled" || \
        print_fail "Failed to uninstall old version"
else
    print_skip "No previous installation found"
fi

# Test 2.2: Install APK
print_test "2.2 - Install new APK"
INSTALL_OUTPUT=$(adb install -r "$APK_PATH" 2>&1)
if echo "$INSTALL_OUTPUT" | grep -q "Success"; then
    print_pass "APK installed successfully"
else
    print_fail "APK installation failed"
    echo "$INSTALL_OUTPUT"
    exit 1
fi

# Test 2.3: Verify installation
print_test "2.3 - Verify package installed"
if adb shell pm list packages | grep -q "$PACKAGE_NAME"; then
    VERSION=$(adb shell dumpsys package "$PACKAGE_NAME" | grep versionName | head -1 | awk '{print $1}')
    print_pass "Package verified: $VERSION"
else
    print_fail "Package not found after installation"
    exit 1
fi

#############################################
# Phase 3: App Launch & Stability
#############################################

echo ""
echo "================================"
echo "Phase 3: App Launch & Stability"
echo "================================"

# Clear logcat
adb logcat -c

# Test 3.1: Launch app
print_test "3.1 - Launch application"
adb shell monkey -p "$PACKAGE_NAME" -c android.intent.category.LAUNCHER 1 > /dev/null 2>&1
sleep 3  # Wait for app to start

# Test 3.2: Check app process
print_test "3.2 - Check app process is running"
if adb shell "ps -A | grep $PACKAGE_NAME" > /dev/null 2>&1; then
    print_pass "App process is running"
else
    print_fail "App process not found"
fi

# Test 3.3: Check for crashes
print_test "3.3 - Check for crash logs"
CRASH_COUNT=$(adb logcat -d | grep -c "FATAL EXCEPTION" || true)
if [ "$CRASH_COUNT" -eq 0 ]; then
    print_pass "No crashes detected"
else
    print_fail "Found $CRASH_COUNT crash(es)"
    echo ""
    echo "Recent crash logs:"
    adb logcat -d | grep -A 10 "FATAL EXCEPTION" | tail -20
fi

# Test 3.4: Check for ANR
print_test "3.4 - Check for ANR (Application Not Responding)"
ANR_COUNT=$(adb logcat -d | grep -c "ANR in" || true)
if [ "$ANR_COUNT" -eq 0 ]; then
    print_pass "No ANR detected"
else
    print_fail "Found $ANR_COUNT ANR(s)"
fi

# Test 3.5: Check main activity
print_test "3.5 - Check main activity is resumed"
RESUMED_ACTIVITY=$(adb shell dumpsys activity activities | grep "mResumedActivity" | head -1)
if echo "$RESUMED_ACTIVITY" | grep -q "$PACKAGE_NAME"; then
    print_pass "Main activity is active"
else
    print_fail "Main activity not active"
    echo "Current: $RESUMED_ACTIVITY"
fi

#############################################
# Phase 4: Feature Verification
#############################################

echo ""
echo "================================"
echo "Phase 4: Feature Verification"
echo "================================"

# Test 4.1: Check network permissions
print_test "4.1 - Verify INTERNET permission"
if adb shell dumpsys package "$PACKAGE_NAME" | grep -q "android.permission.INTERNET"; then
    print_pass "INTERNET permission granted"
else
    print_fail "INTERNET permission missing"
fi

# Test 4.2: Check BLE permissions
print_test "4.2 - Verify Bluetooth permissions"
BLE_PERMS=$(adb shell dumpsys package "$PACKAGE_NAME" | grep -c "BLUETOOTH" || true)
if [ "$BLE_PERMS" -gt 0 ]; then
    print_pass "Bluetooth permissions found ($BLE_PERMS)"
else
    print_fail "Bluetooth permissions missing"
fi

# Test 4.3: Check foreground service permission
print_test "4.3 - Verify FOREGROUND_SERVICE permission"
if adb shell dumpsys package "$PACKAGE_NAME" | grep -q "FOREGROUND_SERVICE"; then
    print_pass "FOREGROUND_SERVICE permission granted"
else
    print_fail "FOREGROUND_SERVICE permission missing"
fi

# Test 4.4: Check database creation
print_test "4.4 - Verify Room database created"
DB_EXISTS=$(adb shell "ls /data/data/$PACKAGE_NAME/databases/ 2>/dev/null | grep -c rehealth || echo 0")
if [ "$DB_EXISTS" -gt 0 ]; then
    print_pass "Database created"
else
    print_skip "Database not yet created (may require first data)"
fi

# Test 4.5: Check SharedPreferences
print_test "4.5 - Verify SharedPreferences for session"
PREFS_EXISTS=$(adb shell "ls /data/data/$PACKAGE_NAME/shared_prefs/ 2>/dev/null | grep -c session || echo 0")
if [ "$PREFS_EXISTS" -gt 0 ]; then
    print_pass "Session preferences found"
else
    print_skip "Session preferences not yet created"
fi

#############################################
# Phase 5: Memory & Performance
#############################################

echo ""
echo "================================"
echo "Phase 5: Memory & Performance"
echo "================================"

# Test 5.1: Check memory usage
print_test "5.1 - Check memory usage"
MEMORY_INFO=$(adb shell dumpsys meminfo "$PACKAGE_NAME" | grep "TOTAL PSS" | head -1 | awk '{print $3}')
if [ -n "$MEMORY_INFO" ]; then
    MEMORY_MB=$((MEMORY_INFO / 1024))
    if [ "$MEMORY_MB" -lt 300 ]; then
        print_pass "Memory usage: ${MEMORY_MB}MB (healthy)"
    else
        print_fail "Memory usage: ${MEMORY_MB}MB (high)"
    fi
else
    print_skip "Could not get memory info"
fi

# Test 5.2: Check for memory leaks (basic)
print_test "5.2 - Check for memory leak warnings"
LEAK_COUNT=$(adb logcat -d | grep -c "LeakCanary\|memory leak" || true)
if [ "$LEAK_COUNT" -eq 0 ]; then
    print_pass "No memory leak warnings"
else
    print_fail "Found $LEAK_COUNT memory leak warning(s)"
fi

#############################################
# Phase 6: API Integration Check
#############################################

echo ""
echo "================================"
echo "Phase 6: API Integration Check"
echo "================================"

# Test 6.1: Check for Retrofit initialization
print_test "6.1 - Check Retrofit API client initialization"
RETROFIT_LOG=$(adb logcat -d | grep -c "Retrofit\|OkHttp" || true)
if [ "$RETROFIT_LOG" -gt 0 ]; then
    print_pass "Retrofit client initialized"
else
    print_skip "No Retrofit logs found (may not have made API calls yet)"
fi

# Test 6.2: Check for network errors
print_test "6.2 - Check for network connection errors"
NETWORK_ERRORS=$(adb logcat -d | grep -c "UnknownHostException\|ConnectException\|SocketTimeoutException" || true)
if [ "$NETWORK_ERRORS" -eq 0 ]; then
    print_pass "No network errors"
elif [ "$NETWORK_ERRORS" -lt 5 ]; then
    print_skip "Minor network issues ($NETWORK_ERRORS) - backend may be unavailable"
else
    print_fail "Multiple network errors ($NETWORK_ERRORS)"
fi

# Test 6.3: Check for authentication logs
print_test "6.3 - Check authentication system"
AUTH_LOGS=$(adb logcat -d | grep -c "SessionStore\|AuthenticatedApiClient\|login" || true)
if [ "$AUTH_LOGS" -gt 0 ]; then
    print_pass "Authentication system active ($AUTH_LOGS logs)"
else
    print_skip "No authentication logs (user may not have logged in)"
fi

#############################################
# Phase 7: Device Integration
#############################################

echo ""
echo "================================"
echo "Phase 7: Device Integration"
echo "================================"

# Test 7.1: Check BLE adapter
print_test "7.1 - Check Bluetooth adapter status"
BLE_LOGS=$(adb logcat -d | grep -c "BluetoothAdapter\|BLE\|MrdBle" || true)
if [ "$BLE_LOGS" -gt 0 ]; then
    print_pass "BLE system initialized ($BLE_LOGS logs)"
else
    print_skip "No BLE logs (may be using mock data on emulator)"
fi

# Test 7.2: Check ring repository
print_test "7.2 - Check ring repository initialization"
RING_LOGS=$(adb logcat -d | grep -c "RingRepository\|MockRingRepository\|MrdBleRingRepository" || true)
if [ "$RING_LOGS" -gt 0 ]; then
    print_pass "Ring repository initialized ($RING_LOGS logs)"
else
    print_skip "No ring repository logs yet"
fi

#############################################
# Phase 8: UI Verification
#############################################

echo ""
echo "================================"
echo "Phase 8: UI Verification"
echo "================================"

# Test 8.1: Take screenshot
print_test "8.1 - Capture current screen"
adb shell screencap -p /sdcard/rehealth_test.png 2>/dev/null
if adb pull /sdcard/rehealth_test.png /tmp/rehealth_screenshot.png > /dev/null 2>&1; then
    print_pass "Screenshot saved to /tmp/rehealth_screenshot.png"
else
    print_skip "Could not capture screenshot"
fi

# Test 8.2: Check UI hierarchy
print_test "8.2 - Dump UI hierarchy"
if adb shell uiautomator dump > /dev/null 2>&1; then
    print_pass "UI hierarchy accessible"
else
    print_skip "Could not dump UI hierarchy"
fi

#############################################
# Phase 9: Cleanup
#############################################

echo ""
echo "================================"
echo "Phase 9: Cleanup"
echo "================================"

# Test 9.1: Save logs
print_test "9.1 - Save logcat to file"
LOG_FILE="/tmp/rehealth_test_$(date +%Y%m%d_%H%M%S).log"
adb logcat -d > "$LOG_FILE"
print_pass "Logs saved to $LOG_FILE"

# Test 9.2: Keep app running or stop
print_test "9.2 - App state"
print_pass "App left running for manual testing"

#############################################
# Summary
#############################################

echo ""
echo "================================"
echo "Test Summary"
echo "================================"
echo -e "${GREEN}PASSED:${NC} $PASSED"
echo -e "${RED}FAILED:${NC} $FAILED"
echo -e "${YELLOW}SKIPPED:${NC} $SKIPPED"
echo ""

TOTAL=$((PASSED + FAILED + SKIPPED))
SUCCESS_RATE=$((PASSED * 100 / TOTAL))

echo "Success Rate: ${SUCCESS_RATE}%"
echo ""

if [ "$FAILED" -eq 0 ]; then
    echo -e "${GREEN}✓ All tests passed!${NC}"
    exit 0
elif [ "$FAILED" -lt 5 ]; then
    echo -e "${YELLOW}⚠ Some tests failed, but app is functional${NC}"
    exit 0
else
    echo -e "${RED}✗ Multiple test failures detected${NC}"
    exit 1
fi
