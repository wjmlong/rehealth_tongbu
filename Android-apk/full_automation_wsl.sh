#!/bin/bash
# ReHealth Full Automation - WSL Native Approach
# This script works without Windows interop by creating trigger files

set -e

LOG_DIR="/mnt/d/rehealthAI/outputs"
PROJECT_DIR="/mnt/d/rehealthAI/Android-apk"
BACKEND_DIR="/mnt/d/rehealthAI/rehealth-algorithms"

mkdir -p "$LOG_DIR"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$LOG_DIR/automation.log"
}

log "========================================"
log "ReHealth Full Automation Started"
log "========================================"

# Step 1: Create build trigger file for Windows
log "Step 1: Creating build trigger..."
cat > "$PROJECT_DIR/BUILD_TRIGGER.txt" << EOF
请在Windows环境执行以下命令构建APK:

cd D:\rehealthAI\Android-apk
gradlew.bat assembleDebug

或者运行: full_automation.bat

构建完成后，删除此文件表示完成。
EOF

log "Build trigger created at: $PROJECT_DIR/BUILD_TRIGGER.txt"

# Step 2: Backend service (already started, verify)
log "Step 2: Verifying backend service..."
if ps aux | grep -q "[u]vicorn.*api.main"; then
    log "✓ Backend service is running (PID: $(pgrep -f 'uvicorn.*api.main'))"
else
    log "✗ Backend service not running, starting..."
    cd "$BACKEND_DIR"

    # Check if venv exists
    if [ -d "venv" ]; then
        source venv/bin/activate
    elif [ -d ".venv" ]; then
        source .venv/bin/activate
    else
        log "Creating virtual environment..."
        python3 -m venv venv
        source venv/bin/activate

        # Install dependencies
        if [ -f "requirements.txt" ]; then
            log "Installing dependencies..."
            pip install -q -r requirements.txt
        fi
    fi

    # Start backend
    log "Starting backend service..."
    nohup uvicorn api.main:app --host 0.0.0.0 --port 8000 > "$LOG_DIR/backend.log" 2>&1 &
    BACKEND_PID=$!
    echo $BACKEND_PID > "$LOG_DIR/backend.pid"

    sleep 5

    # Verify startup
    if curl -s http://localhost:8000/docs > /dev/null 2>&1; then
        log "✓ Backend started successfully (PID: $BACKEND_PID)"
    else
        log "✗ Backend failed to start, check $LOG_DIR/backend.log"
    fi
fi

# Step 3: Wait for APK to be built (monitor loop)
log "Step 3: Monitoring for APK..."
APK_PATH="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
BUILD_TIMEOUT=3600  # 1 hour
BUILD_START=$(date +%s)

while true; do
    ELAPSED=$(($(date +%s) - BUILD_START))

    if [ -f "$APK_PATH" ]; then
        APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
        log "✓ APK found! Size: $APK_SIZE"
        rm -f "$PROJECT_DIR/BUILD_TRIGGER.txt"  # Remove trigger
        break
    fi

    if [ $ELAPSED -gt $BUILD_TIMEOUT ]; then
        log "✗ APK build timeout (waited ${ELAPSED}s)"
        log "Please check BUILD_TRIGGER.txt and build manually"
        break
    fi

    if [ $((ELAPSED % 60)) -eq 0 ]; then
        log "Waiting for APK... (${ELAPSED}s elapsed)"
    fi

    sleep 10
done

# Step 4: Create installation guide if APK exists
if [ -f "$APK_PATH" ]; then
    log "Step 4: Creating installation guide..."

    cat > "$LOG_DIR/INSTALL_GUIDE.txt" << EOF
APK已构建成功！

APK位置: $APK_PATH
大小: $(du -h "$APK_PATH" | cut -f1)

请执行以下步骤安装测试:

1. 检查设备连接:
   adb devices

2. 安装APK:
   adb install -r "$APK_PATH"

3. 启动应用:
   adb shell monkey -p com.rehealth.genie -c android.intent.category.LAUNCHER 1

4. 查看日志:
   adb logcat | grep ReHealth

5. 截图:
   adb shell screencap -p /sdcard/test.png
   adb pull /sdcard/test.png $LOG_DIR/screenshot.png

6. 运行完整测试:
   cd $PROJECT_DIR
   ./complete_integration_test.sh
EOF

    log "Installation guide created: $LOG_DIR/INSTALL_GUIDE.txt"
fi

# Step 5: Generate status report
log "Step 5: Generating status report..."

cat > "$LOG_DIR/automation_status.md" << EOF
# ReHealth Automation Status Report

**Generated**: $(date '+%Y-%m-%d %H:%M:%S')

---

## Status Summary

### Backend Service
$(if ps aux | grep -q "[u]vicorn.*api.main"; then
    echo "- ✅ Running (PID: $(pgrep -f 'uvicorn.*api.main'))"
    echo "- URL: http://localhost:8000"
    echo "- Logs: $LOG_DIR/backend.log"
else
    echo "- ⏸️ Not running"
fi)

### APK Build
$(if [ -f "$APK_PATH" ]; then
    echo "- ✅ Completed"
    echo "- Size: $(du -h "$APK_PATH" | cut -f1)"
    echo "- Location: $APK_PATH"
else
    echo "- ⏸️ Pending"
    echo "- Trigger: $PROJECT_DIR/BUILD_TRIGGER.txt"
    echo "- Please build in Windows environment"
fi)

### Installation
- ⏸️ Requires manual execution (see INSTALL_GUIDE.txt)
- Script ready: complete_integration_test.sh

### Testing
- ⏸️ Awaiting installation completion

---

## Next Steps

$(if [ -f "$APK_PATH" ]; then
    echo "1. Follow INSTALL_GUIDE.txt to install APK"
    echo "2. Run ./complete_integration_test.sh for full tests"
    echo "3. Review test results in outputs directory"
else
    echo "1. Build APK in Windows: gradlew.bat assembleDebug"
    echo "2. Or run: full_automation.bat"
    echo "3. Return to this automation once APK is built"
fi)

---

## Configuration

- **Backend Port**: 8000
- **Backend URL**: http://localhost:8000 (http://10.0.2.2:8000 from emulator)
- **API Base URL**: http://10.0.2.2:8080/jeecg-boot/
- **Package Name**: com.rehealth.genie

---

## Available Scripts

1. **complete_integration_test.sh** - 9-phase full test suite
2. **start_backend_services.sh** - Backend service management
3. **full_automation.bat** - Windows full automation (build+install+test)

---

## Log Files

- Automation log: $LOG_DIR/automation.log
- Backend log: $LOG_DIR/backend.log
- Test results: (generated after running tests)

---

**Automation Controller**: Running in background
**Monitoring**: Active
**Ready for next phase**: $([ -f "$APK_PATH" ] && echo "Yes" || echo "Waiting for APK")
EOF

log "Status report generated: $LOG_DIR/automation_status.md"

# Step 6: Setup continuous monitoring
log "Step 6: Setting up continuous monitoring..."

# Monitor APK appearance
if [ ! -f "$APK_PATH" ]; then
    log "Starting APK monitor (will check every 60s)..."

    (
        while true; do
            if [ -f "$APK_PATH" ]; then
                log "✓ APK detected! Updating status..."

                # Update status report
                bash "$0" --update-status

                # Create notification
                echo "APK构建完成！" > "$LOG_DIR/APK_READY.txt"
                echo "时间: $(date)" >> "$LOG_DIR/APK_READY.txt"
                echo "大小: $(du -h "$APK_PATH" | cut -f1)" >> "$LOG_DIR/APK_READY.txt"
                echo "请执行安装测试: 参见 INSTALL_GUIDE.txt" >> "$LOG_DIR/APK_READY.txt"

                log "✓ APK ready notification created"
                break
            fi
            sleep 60
        done
    ) &

    MONITOR_PID=$!
    echo $MONITOR_PID > "$LOG_DIR/apk_monitor.pid"
    log "APK monitor started (PID: $MONITOR_PID)"
fi

# Summary
log "========================================"
log "Automation Setup Complete"
log "========================================"
log ""
log "Backend Service: $(ps aux | grep -q "[u]vicorn.*api.main" && echo "Running ✓" || echo "Stopped ✗")"
log "APK Status: $([ -f "$APK_PATH" ] && echo "Ready ✓" || echo "Pending ⏸️")"
log "Monitoring: Active ✓"
log ""
log "Output directory: $LOG_DIR"
log "Key files:"
log "  - automation_status.md (current status)"
log "  - automation.log (detailed log)"
log "  - INSTALL_GUIDE.txt (next steps)"
log ""
log "For manual steps, see: $LOG_DIR/automation_status.md"
log "========================================"

exit 0
