#!/bin/bash

echo "╔════════════════════════════════════════════════╗"
echo "║  在WSL2中安装Android工具并测试睿禾精灵        ║"
echo "╚════════════════════════════════════════════════╝"
echo ""

# 1. 安装adb
echo "步骤1: 安装adb工具..."
sudo apt update
sudo apt install -y adb

echo ""
echo "✅ adb安装完成"
echo ""

# 2. 检查版本
adb version

echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "下一步操作："
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "【如果有Android手机】"
echo "1. 手机开启开发者选项："
echo "   设置 → 关于手机 → 连续点击'版本号'7次"
echo ""
echo "2. 开启USB调试："
echo "   设置 → 开发者选项 → USB调试（开启）"
echo ""
echo "3. 用USB线连接手机到电脑"
echo ""
echo "4. 运行：adb devices"
echo "   （手机上会弹出授权提示，点击'允许'）"
echo ""
echo "5. 安装APK："
echo "   adb install -r app/build/outputs/apk/debug/app-debug.apk"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "【如果没有Android手机】"
echo "需要在Windows上安装Android Studio来使用模拟器"
echo ""
echo "或者可以使用在线Android模拟器："
echo "- https://appetize.io/"
echo "- https://www.browserstack.com/"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "adb工具已安装完成！"

