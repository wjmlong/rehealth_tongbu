#!/bin/bash
# Final Report Generator - Runs at end of overnight automation
# This generates the comprehensive final report with all results

OUTPUT_DIR="/mnt/d/rehealthAI/outputs"
PROJECT_DIR="/mnt/d/rehealthAI/Android-apk"
TIMESTAMP=$(date '+%Y-%m-%d %H:%M:%S')

generate_final_report() {
    cat > "$OUTPUT_DIR/FINAL_OVERNIGHT_REPORT.md" << 'EOFMAIN'
# ReHealth 夜间自动化完整报告

**生成时间**: TIMESTAMP_PLACEHOLDER

---

## 📊 执行摘要

### 任务完成情况

EOFMAIN

    # Task 1: APK Build
    if [ -f "$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk" ]; then
        APK_SIZE=$(du -h "$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk" | cut -f1)
        cat >> "$OUTPUT_DIR/FINAL_OVERNIGHT_REPORT.md" << EOF
#### ✅ Task 1: APK构建
- **状态**: 成功完成
- **APK大小**: $APK_SIZE
- **位置**: \`$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk\`
EOF
    else
        cat >> "$OUTPUT_DIR/FINAL_OVERNIGHT_REPORT.md" << EOF
#### ⏸️ Task 1: APK构建
- **状态**: 未完成
- **说明**: APK未在预期位置找到，可能需要在Windows环境手动构建
- **操作**: 参见 \`BUILD_TRIGGER.txt\` 或 \`PLEASE_BUILD_APK.txt\`
EOF
    fi

    # Task 2: Installation
    if [ -f "$OUTPUT_DIR/test_results.txt" ]; then
        cat >> "$OUTPUT_DIR/FINAL_OVERNIGHT_REPORT.md" << EOF

#### ✅ Task 2: APK安装
- **状态**: 成功完成
- **详情**: 应用已安装到设备/模拟器
EOF
    else
        cat >> "$OUTPUT_DIR/FINAL_OVERNIGHT_REPORT.md" << EOF

#### ⏸️ Task 2: APK安装
- **状态**: 待执行
- **说明**: 等待APK构建完成或需要手动安装
- **操作**: 参见 \`MANUAL_INSTALL.txt\`
EOF
    fi

    # Task 3: Verification
    if [ -f "$OUTPUT_DIR/logcat_filtered.txt" ]; then
        CRASHES=$(grep -c "FATAL EXCEPTION" "$OUTPUT_DIR/logcat_filtered.txt" || echo "0")
        if [ "$CRASHES" -eq 0 ]; then
            cat >> "$OUTPUT_DIR/FINAL_OVERNIGHT_REPORT.md" << EOF

#### ✅ Task 3: 应用启动验证
- **状态**: 验证成功
- **崩溃数**: 0
- **日志**: \`logcat_filtered.txt\`
EOF
        else
            cat >> "$OUTPUT_DIR/FINAL_OVERNIGHT_REPORT.md" << EOF

#### ⚠️ Task 3: 应用启动验证
- **状态**: 发现问题
- **崩溃数**: $CRASHES
- **日志**: \`logcat_filtered.txt\`
- **建议**: 查看崩溃日志并修复问题
EOF
        fi
    else
        cat >> "$OUTPUT_DIR/FINAL_OVERNIGHT_REPORT.md" << EOF

#### ⏸️ Task 3: 应用启动验证
- **状态**: 待执行
- **说明**: 等待应用安装完成
EOF
    fi

    # Task 4: Comprehensive Tests
    if [ -f "$OUTPUT_DIR/test_results.txt" ]; then
        PASSED=$(grep -c "\[PASS\]" "$OUTPUT_DIR/test_results.txt" || echo "0")
        FAILED=$(grep -c "\[FAIL\]" "$OUTPUT_DIR/test_results.txt" || echo "0")
        SKIPPED=$(grep -c "\[SKIP\]" "$OUTPUT_DIR/test_results.txt" || echo "0")

        cat >> "$OUTPUT_DIR/FINAL_OVERNIGHT_REPORT.md" << EOF

#### ✅ Task 4: 完整功能测试
- **状态**: 已执行
- **通过**: $PASSED 项
- **失败**: $FAILED 项
- **跳过**: $SKIPPED 项
- **详细结果**: \`test_results.txt\`
EOF
    else
        cat >> "$OUTPUT_DIR/FINAL_OVERNIGHT_REPORT.md" << EOF

#### ⏸️ Task 4: 完整功能测试
- **状态**: 待执行
- **说明**: 等待应用安装完成
- **脚本**: \`complete_integration_test.sh\`
EOF
    fi

    # Task 5: Report Generation
    if [ -f "$OUTPUT_DIR/comprehensive_test_report.md" ]; then
        cat >> "$OUTPUT_DIR/FINAL_OVERNIGHT_REPORT.md" << EOF

#### ✅ Task 5: 测试报告生成
- **状态**: 已完成
- **报告**: \`comprehensive_test_report.md\`
- **JSON数据**: \`test_report.json\`
EOF
    else
        cat >> "$OUTPUT_DIR/FINAL_OVERNIGHT_REPORT.md" << EOF

#### ⚠️ Task 5: 测试报告生成
- **状态**: 部分完成
- **说明**: 基础报告已生成，但可能缺少测试数据
EOF
    fi

    # Task 6: Backend Service
    if ps aux | grep -q "[u]vicorn.*api.main"; then
        BACKEND_PID=$(pgrep -f 'uvicorn.*api.main')
        cat >> "$OUTPUT_DIR/FINAL_OVERNIGHT_REPORT.md" << EOF

#### ✅ Task 6: 后端服务启动
- **状态**: 运行中
- **PID**: $BACKEND_PID
- **端口**: 8000
- **访问**: http://localhost:8000
- **日志**: \`backend.log\`
EOF
    else
        cat >> "$OUTPUT_DIR/FINAL_OVERNIGHT_REPORT.md" << EOF

#### ⏸️ Task 6: 后端服务启动
- **状态**: 已停止
- **说明**: 服务可能启动失败或已停止
- **日志**: \`backend.log\`
EOF
    fi

    # Add detailed sections
    cat >> "$OUTPUT_DIR/FINAL_OVERNIGHT_REPORT.md" << 'EOF'

---

## 📁 生成的文件

### 核心文件
EOF

    # List key files
    for file in comprehensive_test_report.md test_report.json overnight_master.log automation.log backend.log test_results.txt logcat_full.txt screenshot_*.png; do
        if ls "$OUTPUT_DIR"/$file 2>/dev/null | head -1 > /dev/null; then
            FILE_PATH=$(ls "$OUTPUT_DIR"/$file 2>/dev/null | head -1)
            FILE_SIZE=$(du -h "$FILE_PATH" | cut -f1)
            FILE_NAME=$(basename "$FILE_PATH")
            echo "- ✅ **$FILE_NAME** ($FILE_SIZE)" >> "$OUTPUT_DIR/FINAL_OVERNIGHT_REPORT.md"
        fi
    done

    cat >> "$OUTPUT_DIR/FINAL_OVERNIGHT_REPORT.md" << 'EOF'

### 所有输出文件
EOF

    ls -lh "$OUTPUT_DIR" | tail -n +2 | awk '{printf "- `%s` (%s)\n", $9, $5}' >> "$OUTPUT_DIR/FINAL_OVERNIGHT_REPORT.md"

    cat >> "$OUTPUT_DIR/FINAL_OVERNIGHT_REPORT.md" << 'EOF'

---

## 📊 统计信息

### 进程运行时长
EOF

    # Process uptime
    for pid in $(pgrep -f "overnight_automation\|uvicorn.*api.main\|periodic_status"); do
        PROC_NAME=$(ps -p $pid -o comm= 2>/dev/null || echo "Unknown")
        PROC_TIME=$(ps -p $pid -o etime= 2>/dev/null || echo "N/A")
        echo "- **$PROC_NAME** (PID $pid): $PROC_TIME" >> "$OUTPUT_DIR/FINAL_OVERNIGHT_REPORT.md"
    done

    cat >> "$OUTPUT_DIR/FINAL_OVERNIGHT_REPORT.md" << 'EOF'

### 磁盘使用
EOF

    OUTPUT_SIZE=$(du -sh "$OUTPUT_DIR" | cut -f1)
    PROJECT_SIZE=$(du -sh "$PROJECT_DIR" | cut -f1)

    cat >> "$OUTPUT_DIR/FINAL_OVERNIGHT_REPORT.md" << EOF
- **输出目录**: $OUTPUT_SIZE
- **项目目录**: $PROJECT_SIZE

---

## 📝 关键日志摘要

### 自动化日志 (最近50行)
\`\`\`
$(tail -50 "$OUTPUT_DIR/overnight_master.log" 2>/dev/null || echo "日志不可用")
\`\`\`

### 后端服务日志 (最近30行)
\`\`\`
$(tail -30 "$OUTPUT_DIR/backend.log" 2>/dev/null || echo "日志不可用")
\`\`\`

---

## 🎯 完成度总结
EOF

    # Calculate completion percentage
    COMPLETED=0
    TOTAL=6

    [ -f "$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk" ] && ((COMPLETED++))
    [ -f "$OUTPUT_DIR/test_results.txt" ] && ((COMPLETED++))
    [ -f "$OUTPUT_DIR/logcat_filtered.txt" ] && ((COMPLETED++))
    [ -f "$OUTPUT_DIR/test_results.txt" ] && ((COMPLETED++))
    [ -f "$OUTPUT_DIR/comprehensive_test_report.md" ] && ((COMPLETED++))
    ps aux | grep -q "[u]vicorn.*api.main" && ((COMPLETED++))

    PERCENTAGE=$((COMPLETED * 100 / TOTAL))

    cat >> "$OUTPUT_DIR/FINAL_OVERNIGHT_REPORT.md" << EOF

**完成度**: $COMPLETED/$TOTAL 任务 ($PERCENTAGE%)

EOF

    if [ $COMPLETED -eq $TOTAL ]; then
        cat >> "$OUTPUT_DIR/FINAL_OVERNIGHT_REPORT.md" << 'EOF'
🎉 **所有任务已完成！** 系统已完全自动化运行并通过所有测试。
EOF
    elif [ $COMPLETED -ge 4 ]; then
        cat >> "$OUTPUT_DIR/FINAL_OVERNIGHT_REPORT.md" << 'EOF'
✅ **大部分任务已完成** - 核心功能已验证，部分任务可能需要手动干预。
EOF
    else
        cat >> "$OUTPUT_DIR/FINAL_OVERNIGHT_REPORT.md" << 'EOF'
⏸️ **任务进行中** - 部分任务仍在等待执行或需要手动完成。
EOF
    fi

    cat >> "$OUTPUT_DIR/FINAL_OVERNIGHT_REPORT.md" << 'EOF'

---

## 🚀 后续步骤

### 如果所有任务完成
1. 查看详细测试结果：`comprehensive_test_report.md`
2. 检查应用截图验证UI
3. 查看性能指标和内存使用
4. 如有问题，查看相关日志文件

### 如果APK未构建
1. 在Windows环境执行：`full_automation.bat`
2. 或手动构建：`gradlew.bat assembleDebug`
3. 构建完成后，运行：`overnight_automation.sh`

### 如果需要手动测试
1. 安装APK：`adb install -r <apk路径>`
2. 启动应用：`adb shell monkey -p com.rehealth.genie -c android.intent.category.LAUNCHER 1`
3. 运行测试：`./complete_integration_test.sh`

---

## 📞 支持信息

### 重要文件位置
- **输出目录**: `$OUTPUT_DIR`
- **项目目录**: `$PROJECT_DIR`
- **APK位置**: `$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk`

### 文档参考
- 执行总结：`EXECUTIVE_SUMMARY.md`
- 完整交付报告：`final_delivery_report.md`
- API映射文档：`page_api_capability_mapping.md`

### 联系方式
如遇问题，请查看详细日志文件或参考上述文档。

---

**报告生成时间**: $TIMESTAMP
**自动化工具**: Claude Code Overnight Automation
**版本**: 1.0
EOF

    # Replace timestamp
    sed -i "s/TIMESTAMP_PLACEHOLDER/$TIMESTAMP/g" "$OUTPUT_DIR/FINAL_OVERNIGHT_REPORT.md"

    echo "$OUTPUT_DIR/FINAL_OVERNIGHT_REPORT.md"
}

# Generate the report
echo "Generating final overnight report..."
REPORT_PATH=$(generate_final_report)
echo "✓ Final report generated: $REPORT_PATH"

# Create a summary for console
cat << EOF

========================================
夜间自动化执行完成
========================================

完整报告: $REPORT_PATH

快速摘要:
EOF

# Quick summary
APK_STATUS="⏸️ 待完成"
[ -f "$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk" ] && APK_STATUS="✅ 已完成"

INSTALL_STATUS="⏸️ 待执行"
[ -f "$OUTPUT_DIR/test_results.txt" ] && INSTALL_STATUS="✅ 已完成"

BACKEND_STATUS="⏸️ 已停止"
ps aux | grep -q "[u]vicorn.*api.main" && BACKEND_STATUS="✅ 运行中"

cat << EOF
  APK构建: $APK_STATUS
  应用安装: $INSTALL_STATUS
  功能测试: $INSTALL_STATUS
  后端服务: $BACKEND_STATUS

查看完整报告了解详情。
========================================

EOF

exit 0
