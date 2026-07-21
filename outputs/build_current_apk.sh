#!/usr/bin/env bash
# Build the CURRENT Android-apk HEAD (includes D3 auth/register/ring-skip + real-device
# attribution/data UI) via WSL2 using the established recipe (Linux SDK).
set -e

cd ~/rehealthAI-android

echo "[build] fetching latest from Windows repo origin..."
git fetch origin

echo "[build] resetting to origin/work/D3_android_auth_typed_feedback (HEAD = 2439c77)..."
git reset --hard origin/work/D3_android_auth_typed_feedback

# Ensure local.properties points at the Linux SDK (Windows SDK can't build from WSL).
cat > local.properties <<'EOF'
sdk.dir=/home/wjmlong/Android/Sdk
EOF

echo "[build] killing any stale Gradle daemons (they can cache the wrong SDK path)..."
pkill -f GradleDaemon || true
sleep 1

echo "[build] assembling debug (no daemon)..."
./gradlew assembleDebug --no-daemon

mkdir -p /mnt/d/rehealthAI/outputs
cp app/build/outputs/apk/debug/app-debug.apk /mnt/d/rehealthAI/outputs/current_debug.apk
echo "[build] DONE -> $(ls -la /mnt/d/rehealthAI/outputs/current_debug.apk)"
