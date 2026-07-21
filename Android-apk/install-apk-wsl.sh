#!/bin/bash

echo "========================================="
echo "睿禾精灵 - WSL Android模拟器启动脚本"
echo "========================================="
echo ""

# 检查是否有adb
if ! command -v adb &> /dev/null; then
    echo "❌ 未找到adb命令"
    echo ""
    echo "请手动启动Android Studio模拟器："
    echo "1. 在Windows中打开Android Studio"
    echo "2. Tools → Device Manager"
    echo "3. 启动任意模拟器"
    echo "4. 返回此窗口，等待模拟器启动完成"
    echo ""
    read -p "模拟器启动后，按Enter继续..."
else
    echo "✅ 找到adb命令"
fi

echo ""
echo "等待模拟器连接..."
echo ""

# 等待设备连接
timeout=60
elapsed=0
while [ $elapsed -lt $timeout ]; do
    devices=$(adb devices | grep -v "List" | grep "device$" | wc -l)
    if [ $devices -gt 0 ]; then
        echo "✅ 发现 $devices 个设备"
        break
    fi
    echo "等待中... ($elapsed/$timeout 秒)"
    sleep 3
    elapsed=$((elapsed + 3))
done

# 显示连接的设备
echo ""
echo "当前连接的设备："
adb devices
echo ""

# 安装APK
APK="app/build/outputs/apk/debug/app-debug.apk"

if [ -f "$APK" ]; then
    echo "📦 正在安装睿禾精灵APK..."
    adb install -r "$APK"
    echo ""
    echo "✅ 安装完成！"
    echo ""
    echo "🎉 现在可以在模拟器中打开睿禾精灵App了！"
else
    echo "❌ APK文件未找到: $APK"
    echo ""
    echo "请先编译项目："
    echo "  export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64"
    echo "  ./gradlew assembleDebug"
fi

echo ""
echo "========================================="
echo "完成！"
echo "========================================="
