#!/bin/bash
# ReHealth Autonomous Daemon - Fully Automated Task Executor
# This script runs continuously and completes all tasks autonomously

OUTPUT_DIR="/mnt/d/rehealthAI/outputs"
PROJECT_DIR="/mnt/d/rehealthAI/Android-apk"
BACKEND_DIR="/mnt/d/rehealthAI/rehealth-algorithms"
DAEMON_LOG="$OUTPUT_DIR/autonomous_daemon.log"

log() {
    echo "[$(date '+%Y-%m-%d %H:%M:%S')] $1" | tee -a "$DAEMON_LOG"
}

log "========================================"
log "ReHealth 自主守护进程启动"
log "========================================"

# Task tracking
declare -A TASK_STATUS
TASK_STATUS[1_apk_build]="pending"
TASK_STATUS[2_apk_install]="pending"
TASK_STATUS[3_app_verify]="pending"
TASK_STATUS[4_full_test]="pending"
TASK_STATUS[5_generate_report]="pending"
TASK_STATUS[6_backend_service]="pending"

# Function: Start Backend Service
start_backend() {
    log "[Task 6] 启动后端服务..."

    cd "$BACKEND_DIR"

    # Ensure venv exists
    if [ ! -d "venv" ]; then
        log "创建虚拟环境..."
        python3 -m venv venv
    fi

    # Activate and install dependencies
    source venv/bin/activate

    # Install dependencies with retry
    MAX_RETRIES=3
    for i in $(seq 1 $MAX_RETRIES); do
        log "安装依赖 (尝试 $i/$MAX_RETRIES)..."

        if [ -f "api/requirements.txt" ]; then
            pip install -r api/requirements.txt --quiet --no-cache-dir && break
        elif [ -f "requirements.txt" ]; then
            pip install -r requirements.txt --quiet --no-cache-dir && break
        else
            pip install uvicorn fastapi --quiet --no-cache-dir && break
        fi

        [ $i -lt $MAX_RETRIES ] && sleep 10
    done

    # Start service
    log "启动 uvicorn 服务..."
    nohup venv/bin/uvicorn api.main:app --host 0.0.0.0 --port 8000 > "$OUTPUT_DIR/backend_daemon.log" 2>&1 &
    BACKEND_PID=$!
    echo $BACKEND_PID > "$OUTPUT_DIR/backend_daemon.pid"

    sleep 5

    # Verify
    if curl -s http://localhost:8000/docs > /dev/null 2>&1; then
        log "✅ [Task 6] 后端服务启动成功 (PID: $BACKEND_PID)"
        TASK_STATUS[6_backend_service]="completed"
        return 0
    else
        log "⚠️ [Task 6] 后端服务启动失败，将重试"
        return 1
    fi
}

# Function: Monitor APK Build
monitor_apk_build() {
    APK_PATH="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"

    if [ -f "$APK_PATH" ]; then
        APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
        log "✅ [Task 1] APK已构建 (大小: $APK_SIZE)"
        TASK_STATUS[1_apk_build]="completed"
        return 0
    else
        return 1
    fi
}

# Function: Install APK
install_apk() {
    log "[Task 2] 尝试安装APK..."

    APK_PATH="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"

    # Try to find adb
    ADB=""
    for adb_path in "/mnt/d/Android_SDK/platform-tools/adb.exe" "/mnt/c/Android/Sdk/platform-tools/adb.exe" "adb"; do
        if command -v "$adb_path" &> /dev/null; then
            ADB="$adb_path"
            break
        fi
    done

    if [ -z "$ADB" ]; then
        log "⏸️ [Task 2] ADB未找到，创建手动安装指南"
        cat > "$OUTPUT_DIR/INSTALL_APK_MANUALLY.txt" << EOF
APK位置: $APK_PATH

请手动执行:
1. 连接设备/模拟器
2. adb install -r "$APK_PATH"
3. adb shell monkey -p com.rehealth.genie -c android.intent.category.LAUNCHER 1
EOF
        return 1
    fi

    # Check devices
    DEVICES=$("$ADB" devices 2>/dev/null | grep -v "List" | grep "device$" | wc -l)

    if [ $DEVICES -eq 0 ]; then
        log "⏸️ [Task 2] 无设备连接"
        return 1
    fi

    log "发现 $DEVICES 个设备，开始安装..."

    if "$ADB" install -r "$APK_PATH" >> "$DAEMON_LOG" 2>&1; then
        log "✅ [Task 2] APK安装成功"
        TASK_STATUS[2_apk_install]="completed"

        # Launch app
        "$ADB" shell monkey -p com.rehealth.genie -c android.intent.category.LAUNCHER 1
        sleep 3

        return 0
    else
        log "⚠️ [Task 2] APK安装失败"
        return 1
    fi
}

# Function: Verify App
verify_app() {
    log "[Task 3] 验证应用启动..."

    # Find adb
    ADB=""
    for adb_path in "/mnt/d/Android_SDK/platform-tools/adb.exe" "/mnt/c/Android/Sdk/platform-tools/adb.exe" "adb"; do
        if command -v "$adb_path" &> /dev/null; then
            ADB="$adb_path"
            break
        fi
    done

    [ -z "$ADB" ] && return 1

    # Collect logs
    "$ADB" logcat -d > "$OUTPUT_DIR/logcat_full_daemon.txt" 2>&1
    "$ADB" logcat -d | grep -E "ReHealth|FATAL" > "$OUTPUT_DIR/logcat_filtered_daemon.txt" 2>&1

    # Check crashes
    CRASHES=$("$ADB" logcat -d | grep -c "FATAL EXCEPTION" || echo "0")

    if [ "$CRASHES" -eq 0 ]; then
        log "✅ [Task 3] 应用验证通过，无崩溃"
        TASK_STATUS[3_app_verify]="completed"

        # Take screenshot
        "$ADB" shell screencap -p /sdcard/daemon_test.png 2>/dev/null
        "$ADB" pull /sdcard/daemon_test.png "$OUTPUT_DIR/screenshot_daemon_$(date +%Y%m%d_%H%M%S).png" 2>/dev/null

        return 0
    else
        log "⚠️ [Task 3] 发现 $CRASHES 个崩溃"
        return 1
    fi
}

# Function: Run Full Tests
run_full_tests() {
    log "[Task 4] 执行完整测试..."

    if [ ! -f "$PROJECT_DIR/complete_integration_test.sh" ]; then
        log "⏸️ [Task 4] 测试脚本未找到"
        return 1
    fi

    cd "$PROJECT_DIR"
    bash complete_integration_test.sh > "$OUTPUT_DIR/test_results_daemon.txt" 2>&1

    if [ -f "$OUTPUT_DIR/test_results_daemon.txt" ]; then
        PASSED=$(grep -c "\[PASS\]" "$OUTPUT_DIR/test_results_daemon.txt" || echo "0")
        FAILED=$(grep -c "\[FAIL\]" "$OUTPUT_DIR/test_results_daemon.txt" || echo "0")

        log "✅ [Task 4] 测试完成 - 通过: $PASSED, 失败: $FAILED"
        TASK_STATUS[4_full_test]="completed"
        return 0
    else
        log "⚠️ [Task 4] 测试执行失败"
        return 1
    fi
}

# Function: Generate Reports
generate_reports() {
    log "[Task 5] 生成测试报告..."

    cd "$PROJECT_DIR"

    # Generate comprehensive report
    python3 generate_test_report.py >> "$DAEMON_LOG" 2>&1

    # Generate final report
    bash generate_final_report.sh >> "$DAEMON_LOG" 2>&1

    if [ -f "$OUTPUT_DIR/comprehensive_test_report.md" ] && [ -f "$OUTPUT_DIR/FINAL_OVERNIGHT_REPORT.md" ]; then
        log "✅ [Task 5] 报告生成完成"
        TASK_STATUS[5_generate_report]="completed"
        return 0
    else
        log "⚠️ [Task 5] 报告生成部分失败"
        return 1
    fi
}

# Main Loop
ITERATION=0
MAX_ITERATIONS=720  # 720 * 60s = 12 hours

while [ $ITERATION -lt $MAX_ITERATIONS ]; do
    ((ITERATION++))

    log "========== 迭代 $ITERATION/$MAX_ITERATIONS =========="

    # Task 6: Backend Service (priority)
    if [ "${TASK_STATUS[6_backend_service]}" != "completed" ]; then
        if ps aux | grep -q "[u]vicorn.*api.main"; then
            log "✅ [Task 6] 后端服务已运行"
            TASK_STATUS[6_backend_service]="completed"
        else
            start_backend
        fi
    fi

    # Task 1: Monitor APK Build
    if [ "${TASK_STATUS[1_apk_build]}" != "completed" ]; then
        monitor_apk_build
    fi

    # Task 2-4: Install, Verify, Test (sequential, requires APK)
    if [ "${TASK_STATUS[1_apk_build]}" == "completed" ]; then
        if [ "${TASK_STATUS[2_apk_install]}" != "completed" ]; then
            install_apk && TASK_STATUS[2_apk_install]="completed"
        fi

        if [ "${TASK_STATUS[2_apk_install]}" == "completed" ] && [ "${TASK_STATUS[3_app_verify]}" != "completed" ]; then
            verify_app && TASK_STATUS[3_app_verify]="completed"
        fi

        if [ "${TASK_STATUS[3_app_verify]}" == "completed" ] && [ "${TASK_STATUS[4_full_test]}" != "completed" ]; then
            run_full_tests && TASK_STATUS[4_full_test]="completed"
        fi
    fi

    # Task 5: Generate Reports (can run anytime)
    if [ "${TASK_STATUS[5_generate_report]}" != "completed" ]; then
        generate_reports
    fi

    # Check if all tasks completed
    ALL_COMPLETE=true
    for task in "${!TASK_STATUS[@]}"; do
        if [ "${TASK_STATUS[$task]}" != "completed" ]; then
            ALL_COMPLETE=false
            break
        fi
    done

    if [ "$ALL_COMPLETE" = true ]; then
        log "========================================"
        log "🎉 所有任务已完成！"
        log "========================================"

        # Generate final summary
        bash "$PROJECT_DIR/overnight_status_summary.sh" >> "$DAEMON_LOG"
        bash "$PROJECT_DIR/generate_final_report.sh" >> "$DAEMON_LOG"

        log "完整报告: $OUTPUT_DIR/FINAL_OVERNIGHT_REPORT.md"
        log "守护进程正常退出"

        exit 0
    fi

    # Status update every 10 iterations (10 minutes)
    if [ $((ITERATION % 10)) -eq 0 ]; then
        COMPLETED=0
        for task in "${!TASK_STATUS[@]}"; do
            [ "${TASK_STATUS[$task]}" == "completed" ] && ((COMPLETED++))
        done

        log "进度: $COMPLETED/6 任务完成"
        log "状态: ${TASK_STATUS[*]}"

        # Generate interim report
        python3 "$PROJECT_DIR/generate_test_report.py" > /dev/null 2>&1
    fi

    # Wait 60 seconds before next iteration
    sleep 60
done

log "========================================"
log "⏸️ 守护进程达到最大迭代次数"
log "部分任务可能未完成，请检查日志"
log "========================================"

# Generate final report anyway
bash "$PROJECT_DIR/generate_final_report.sh" >> "$DAEMON_LOG"

exit 1
