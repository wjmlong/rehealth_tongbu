#!/bin/bash

# Android模拟器启动脚本
# 用于在WSL2中启动Android Studio模拟器

echo "🚀 睿禾精灵 - Android模拟器启动脚本"
echo "================================================"

# 1. 检查Android SDK环境
ANDROID_HOME="/mnt/d/Android_Studio/sdk"
EMULATOR_PATH="$ANDROID_HOME/emulator/emulator"

if [ ! -f "$EMULATOR_PATH" ]; then
    echo "❌ Android SDK未找到，请检查路径：$ANDROID_HOME"
    echo ""
    echo "📝 请在Windows中手动启动模拟器："
    echo "1. 打开Android Studio"
    echo "2. 点击 Tools → Device Manager"
    echo "3. 启动任意模拟器"
    echo "4. 返回WSL2运行: adb devices"
    exit 1
fi

# 2. 列出可用模拟器
echo "📱 检查可用模拟器..."
$EMULATOR_PATH -list-avds

# 3. 启动默认模拟器
AVD_NAME=$($EMULATOR_PATH -list-avds | head -1)

if [ -z "$AVD_NAME" ]; then
    echo "❌ 未找到模拟器，请先创建AVD"
    echo ""
    echo "📝 创建模拟器步骤："
    echo "1. 打开Android Studio"
    echo "2. Tools → Device Manager"
    echo "3. Create Device"
    echo "4. 选择 Pixel 5 + Android 13 (API 33)"
    exit 1
fi

echo "✅ 找到模拟器: $AVD_NAME"
echo "🚀 正在启动模拟器..."

# 4. 启动模拟器
$EMULATOR_PATH -avd "$AVD_NAME" -no-snapshot-load &

echo "⏳ 等待模拟器启动..."
sleep 10

# 5. 检查ADB连接
echo "🔗 检查ADB连接..."
adb devices

# 6. 安装APK
APK_PATH="./app/build/outputs/apk/debug/app-debug.apk"

if [ -f "$APK_PATH" ]; then
    echo "📦 安装睿禾精灵APK..."
    adb install -r "$APK_PATH"
    echo "✅ 安装完成！"
    echo ""
    echo "🎉 现在可以在模拟器中打开睿禾精灵App了！"
else
    echo "⚠️ APK文件未找到: $APK_PATH"
    echo "请先编译项目: ./gradlew assembleDebug"
fi

echo ""
echo "================================================"
echo "💡 常用命令："
echo "  adb devices              - 查看连接的设备"
echo "  adb logcat              - 查看日志"
echo "  adb shell am start ...  - 启动App"
echo "================================================"
