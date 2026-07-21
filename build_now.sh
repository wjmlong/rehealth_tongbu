#!/usr/bin/env bash
set -e
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
# Sync Windows source into WSL, preserving WSL's local.properties (sdk + 10.0.2.2:8080)
rsync -a --delete --exclude='.git' --exclude='local.properties' --exclude='build' --exclude='.gradle' /mnt/d/rehealthAI/Android-apk/ /home/wjmlong/rehealthAI-android/
cd /home/wjmlong/rehealthAI-android
pkill -f GradleDaemon 2>/dev/null || true
sleep 2
./gradlew assembleDebug --no-daemon > /home/wjmlong/bd_now.log 2>&1
CODE=$?
echo "BUILD_EXIT=$CODE"
# Surface the tail for quick inspection
tail -25 /home/wjmlong/bd_now.log
# Copy artifacts back to Windows for inspection
cp /home/wjmlong/bd_now.log /mnt/d/rehealthAI/outputs/bd_now.log 2>/dev/null || true
if [ "$CODE" -eq 0 ]; then
  cp /home/wjmlong/rehealthAI-android/app/build/outputs/apk/debug/app-debug.apk /mnt/d/rehealthAI/outputs/rehealth_liandong_debug.apk
  echo "APK_COPIED=YES"
fi
