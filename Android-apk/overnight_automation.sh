#!/bin/bash
# ReHealth Overnight Automation Orchestrator
# This script runs all tasks autonomously overnight

set -e

LOG_DIR="/mnt/d/rehealthAI/outputs"
PROJECT_DIR="/mnt/d/rehealthAI/Android-apk"
APK_PATH="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"

mkdir -p "$LOG_DIR"
LOG_FILE="$LOG_DIR/overnight_automation.log"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_FILE"
}

log "========================================"
log "ReHealth Overnight Automation Started"
log "========================================"

# Task 1: Monitor APK build (already running in background via Windows)
log "Task 1: Monitoring APK build..."
BUILD_TIMEOUT=7200  # 2 hours
BUILD_START=$(date +%s)
APK_FOUND=false

while true; do
    ELAPSED=$(($(date +%s) - BUILD_START))

    if [ -f "$APK_PATH" ]; then
        APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
        log "✓ Task 1 Complete: APK found! Size: $APK_SIZE"
        APK_FOUND=true
        break
    fi

    if [ $ELAPSED -gt $BUILD_TIMEOUT ]; then
        log "⏸ Task 1 Timeout: APK not built after ${BUILD_TIMEOUT}s"
        log "Creating Windows build instructions..."

        cat > "$PROJECT_DIR/PLEASE_BUILD_APK.txt" << EOF
请在Windows环境执行以下命令构建APK:

方法1 (推荐):
  cd D:\rehealthAI\Android-apk
  full_automation.bat

方法2 (仅构建):
  cd D:\rehealthAI\Android-apk
  gradlew.bat assembleDebug

构建完成后，自动化流程将继续执行后续任务。
EOF
        break
    fi

    if [ $((ELAPSED % 300)) -eq 0 ]; then
        log "Still waiting for APK... (${ELAPSED}s elapsed)"
    fi

    sleep 30
done

# Task 2: Install APK to emulator (if APK exists)
if [ "$APK_FOUND" = true ]; then
    log "Task 2: Attempting APK installation..."

    # Check if ADB is available via Windows path
    if command -v /mnt/d/Android_SDK/platform-tools/adb.exe &> /dev/null; then
        ADB="/mnt/d/Android_SDK/platform-tools/adb.exe"
        log "Found ADB at: $ADB"

        # Check devices
        DEVICES=$($ADB devices | grep -v "List" | grep "device$" | wc -l)

        if [ $DEVICES -gt 0 ]; then
            log "Found $DEVICES device(s), installing APK..."

            if $ADB install -r "$APK_PATH" 2>&1 | tee -a "$LOG_FILE"; then
                log "✓ Task 2 Complete: APK installed successfully"

                # Launch app
                log "Launching application..."
                $ADB shell monkey -p com.rehealth.genie -c android.intent.category.LAUNCHER 1
                sleep 5

                # Check if app is running
                if $ADB shell "ps -A | grep rehealth" > /dev/null 2>&1; then
                    log "✓ Application started successfully"
                else
                    log "⚠ Application may not have started correctly"
                fi

                # Task 3: Verify application startup
                log "Task 3: Verifying application startup..."

                # Collect logs
                $ADB logcat -d > "$LOG_DIR/logcat_full.txt"
                $ADB logcat -d | grep "ReHealth\|FATAL" > "$LOG_DIR/logcat_filtered.txt"

                # Check for crashes
                CRASHES=$($ADB logcat -d | grep -c "FATAL EXCEPTION" || true)
                if [ $CRASHES -eq 0 ]; then
                    log "✓ Task 3 Complete: No crashes detected"
                else
                    log "✗ Task 3 Failed: Found $CRASHES crash(es)"
                fi

                # Take screenshot
                $ADB shell screencap -p /sdcard/overnight_test.png
                $ADB pull /sdcard/overnight_test.png "$LOG_DIR/screenshot_$(date +%Y%m%d_%H%M%S).png"
                log "Screenshot saved"

                # Task 4: Run comprehensive tests
                log "Task 4: Running comprehensive tests..."

                if [ -f "$PROJECT_DIR/complete_integration_test.sh" ]; then
                    log "Executing test suite..."
                    cd "$PROJECT_DIR"
                    bash complete_integration_test.sh > "$LOG_DIR/test_results.txt" 2>&1 || true
                    log "✓ Task 4 Complete: Test suite executed"
                else
                    log "⏸ Task 4 Skipped: Test script not found"
                fi

            else
                log "✗ Task 2 Failed: APK installation failed"
            fi
        else
            log "⏸ Task 2 Skipped: No devices connected"
            log "To install manually: adb install -r $APK_PATH"
        fi
    else
        log "⏸ Task 2 Skipped: ADB not found"
        cat > "$LOG_DIR/MANUAL_INSTALL.txt" << EOF
APK已构建，请手动安装:

APK位置: $APK_PATH

安装命令:
  adb install -r $APK_PATH

启动命令:
  adb shell monkey -p com.rehealth.genie -c android.intent.category.LAUNCHER 1

测试命令:
  cd $PROJECT_DIR
  ./complete_integration_test.sh
EOF
    fi
else
    log "⏸ Tasks 2-4 Skipped: APK not available"
fi

# Task 5: Generate comprehensive test report
log "Task 5: Generating comprehensive test report..."

cd "$PROJECT_DIR"
python3 generate_test_report.py 2>&1 | tee -a "$LOG_FILE"

log "✓ Task 5 Complete: Test report generated"

# Final Summary
log "========================================"
log "Overnight Automation Summary"
log "========================================"

if [ "$APK_FOUND" = true ]; then
    log "✓ APK Build: Success"
else
    log "⏸ APK Build: Pending/Manual"
fi

if [ -f "$LOG_DIR/test_results.txt" ]; then
    log "✓ Installation & Testing: Complete"
    PASSED=$(grep -c "PASS" "$LOG_DIR/test_results.txt" || echo "0")
    FAILED=$(grep -c "FAIL" "$LOG_DIR/test_results.txt" || echo "0")
    log "  Test Results: $PASSED passed, $FAILED failed"
else
    log "⏸ Installation & Testing: Skipped (see logs)"
fi

if [ -f "$LOG_DIR/comprehensive_test_report.md" ]; then
    log "✓ Report Generation: Complete"
else
    log "⏸ Report Generation: Incomplete"
fi

# Check backend service
if ps aux | grep -q "[u]vicorn.*api.main"; then
    log "✓ Backend Service: Running"
else
    log "⏸ Backend Service: Stopped"
fi

log "========================================"
log "All Results: $LOG_DIR"
log "Main Report: $LOG_DIR/comprehensive_test_report.md"
log "Full Log: $LOG_FILE"
log "========================================"
log "Automation Complete - $(date)"

exit 0
