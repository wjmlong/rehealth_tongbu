#!/bin/bash

echo "🚀 睿禾精灵 - 自动安装脚本"
echo "================================"
echo ""

# 1. 尝试通过Windows启动Android Studio
echo "步骤1: 尝试启动Android Studio..."
cmd.exe /c start "" "D:\Android_Studio\bin\studio64.exe" 2>/dev/null && echo "✅ Android Studio已启动" || echo "⏳ 请手动启动Android Studio"

echo ""
echo "步骤2: 等待30秒，让你启动模拟器..."
echo "请在Android Studio中："
echo "1. Tools → Device Manager"
echo "2. 点击启动模拟器 ▶️"
echo ""

for i in {30..1}; do
    echo -ne "等待中... $i 秒\r"
    sleep 1
done

echo ""
echo ""
echo "步骤3: 检查设备连接..."

# 等待设备
timeout=60
for i in $(seq 1 $timeout); do
    devices=$(adb devices 2>/dev/null | grep -v "List" | grep "device$" | wc -l)
    if [ $devices -gt 0 ]; then
        echo "✅ 发现设备！"
        adb devices
        break
    fi
    echo -ne "等待设备连接... $i/$timeout 秒\r"
    sleep 1
done

echo ""
echo ""
echo "步骤4: 安装APK..."
APK="app/build/outputs/apk/debug/app-debug.apk"

if [ -f "$APK" ]; then
    adb install -r "$APK"
    echo ""
    echo "✅ 安装完成！"
    echo ""
    echo "🎉 现在可以在模拟器中打开'睿禾精灵'App了！"
else
    echo "❌ APK文件未找到"
fi

echo ""
echo "================================"
echo "完成！"
