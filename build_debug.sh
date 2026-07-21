#!/usr/bin/env bash
set -e
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
cd /home/wjmlong/rehealthAI-android
# Kill any stale Gradle daemon that may cache the wrong SDK path
pkill -f GradleDaemon 2>/dev/null || true
sleep 2
echo "=== gradle version ==="
./gradlew --version 2>&1 | head -20
echo "=== assembleDebug ==="
./gradlew assembleDebug --no-daemon 2>&1 | tail -80
echo "BUILD_EXIT=${PIPESTATUS[0]}"
