#!/bin/bash
# ReHealth Overnight Automation - Master Status Summary
# This script provides a complete overview of all automated tasks

OUTPUT_DIR="/mnt/d/rehealthAI/outputs"
PROJECT_DIR="/mnt/d/rehealthAI/Android-apk"

echo "========================================"
echo "ReHealth 自动化状态摘要"
echo "时间: $(date '+%Y-%m-%d %H:%M:%S')"
echo "========================================"
echo ""

# Check each component
echo "🔍 检查各组件状态..."
echo ""

# 1. APK Build Status
echo "1️⃣ APK构建状态"
APK_PATH="$PROJECT_DIR/app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    echo "   ✅ 已完成 (大小: $APK_SIZE)"
else
    echo "   ⏸️ 待完成 - 需要在Windows环境构建"
    echo "   提示: 查看 BUILD_TRIGGER.txt 或运行 full_automation.bat"
fi
echo ""

# 2. Backend Service Status
echo "2️⃣ 后端服务状态"
if ps aux | grep -q "[u]vicorn.*api.main"; then
    BACKEND_PID=$(pgrep -f 'uvicorn.*api.main')
    echo "   ✅ 运行中 (PID: $BACKEND_PID)"
    echo "   URL: http://localhost:8000"
else
    echo "   ⏸️ 未运行"
    if [ -f "$OUTPUT_DIR/backend.log" ]; then
        LAST_ERROR=$(tail -5 "$OUTPUT_DIR/backend.log" | grep -i "error" | head -1)
        [ -n "$LAST_ERROR" ] && echo "   错误: $LAST_ERROR"
    fi
fi
echo ""

# 3. Automation Processes
echo "3️⃣ 自动化进程状态"
AUTOMATION_COUNT=0
for proc in "overnight_automation" "periodic_status"; do
    if pgrep -f "$proc" > /dev/null; then
        PID=$(pgrep -f "$proc")
        UPTIME=$(ps -p $PID -o etime= 2>/dev/null | tr -d ' ')
        echo "   ✅ $proc (PID: $PID, 运行时长: $UPTIME)"
        ((AUTOMATION_COUNT++))
    fi
done
[ $AUTOMATION_COUNT -eq 0 ] && echo "   ⏸️ 无自动化进程运行"
echo ""

# 4. Generated Files
echo "4️⃣ 生成的文件"
FILE_COUNT=$(ls "$OUTPUT_DIR" 2>/dev/null | wc -l)
echo "   📁 输出目录: $FILE_COUNT 个文件"
KEY_FILES=("FINAL_OVERNIGHT_REPORT.md" "comprehensive_test_report.md" "test_report.json" "automation.log")
for file in "${KEY_FILES[@]}"; do
    if [ -f "$OUTPUT_DIR/$file" ]; then
        FILE_SIZE=$(du -h "$OUTPUT_DIR/$file" | cut -f1)
        echo "   ✅ $file ($FILE_SIZE)"
    fi
done
echo ""

# 5. Task Completion Summary
echo "5️⃣ 任务完成情况"
TASKS=(
    "APK构建"
    "APK安装"
    "应用验证"
    "功能测试"
    "报告生成"
    "后端服务"
)

COMPLETED=0
TOTAL=6

# Check completion
[ -f "$APK_PATH" ] && ((COMPLETED++)) && TASK1="✅" || TASK1="⏸️"
[ -f "$OUTPUT_DIR/test_results.txt" ] && ((COMPLETED++)) && TASK2="✅" || TASK2="⏸️"
[ -f "$OUTPUT_DIR/logcat_filtered.txt" ] && ((COMPLETED++)) && TASK3="✅" || TASK3="⏸️"
[ -f "$OUTPUT_DIR/test_results.txt" ] && ((COMPLETED++)) && TASK4="✅" || TASK4="⏸️"
[ -f "$OUTPUT_DIR/comprehensive_test_report.md" ] && ((COMPLETED++)) && TASK5="✅" || TASK5="⏸️"
ps aux | grep -q "[u]vicorn.*api.main" && ((COMPLETED++)) && TASK6="✅" || TASK6="⏸️"

PERCENTAGE=$((COMPLETED * 100 / TOTAL))

echo "   $TASK1 ${TASKS[0]}"
echo "   $TASK2 ${TASKS[1]}"
echo "   $TASK3 ${TASKS[2]}"
echo "   $TASK4 ${TASKS[3]}"
echo "   $TASK5 ${TASKS[4]}"
echo "   $TASK6 ${TASKS[5]}"
echo ""
echo "   完成度: $COMPLETED/$TOTAL ($PERCENTAGE%)"
echo ""

# 6. Next Actions
echo "6️⃣ 后续操作建议"
if [ $COMPLETED -eq $TOTAL ]; then
    echo "   🎉 所有任务完成！查看报告了解详情。"
elif [ $COMPLETED -ge 4 ]; then
    echo "   ✅ 大部分任务完成，检查未完成项。"
    [ ! -f "$APK_PATH" ] && echo "   → 需要构建APK (Windows环境)"
    ! ps aux | grep -q "[u]vicorn.*api.main" && echo "   → 需要启动后端服务"
else
    echo "   ⏸️ 多个任务待完成："
    [ ! -f "$APK_PATH" ] && echo "   → 构建APK (关键)"
    ! ps aux | grep -q "[u]vicorn.*api.main" && echo "   → 启动后端服务"
    [ ! -f "$OUTPUT_DIR/test_results.txt" ] && echo "   → 等待APK构建后自动执行测试"
fi
echo ""

# 7. Key File Locations
echo "7️⃣ 关键文件位置"
echo "   📊 完整报告: $OUTPUT_DIR/FINAL_OVERNIGHT_REPORT.md"
echo "   📝 详细日志: $OUTPUT_DIR/overnight_master.log"
echo "   🔧 项目目录: $PROJECT_DIR"
echo "   📦 APK位置: $APK_PATH"
echo ""

# 8. System Resources
echo "8️⃣ 系统资源"
if [ -d "$OUTPUT_DIR" ]; then
    OUTPUT_SIZE=$(du -sh "$OUTPUT_DIR" 2>/dev/null | cut -f1)
    echo "   💾 输出目录: $OUTPUT_SIZE"
fi
LOAD=$(uptime | awk -F'load average:' '{print $2}' | cut -d',' -f1 | tr -d ' ')
echo "   💻 系统负载: $LOAD"
echo ""

echo "========================================"
echo "查看完整报告: cat $OUTPUT_DIR/FINAL_OVERNIGHT_REPORT.md"
echo "更新此摘要: ./overnight_status_summary.sh"
echo "========================================"
