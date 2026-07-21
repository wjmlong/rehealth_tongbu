#!/usr/bin/env bash
set -e
# Kill any stale Gradle daemon that may cache the wrong SDK path
pkill -f GradleDaemon || true
SRC=/mnt/d/rehealthAI/Android-apk
DST=/home/wjmlong/rehealthAI-android
echo "==> copying changed source files into WSL clone"
mkdir -p "$DST/app/src/main/java/com/rehealth/genie/ui" \
         "$DST/app/src/main/java/com/rehealth/genie/ring/data"
cp "$SRC/app/src/main/java/com/rehealth/genie/ui/ReHealthApp.kt" \
   "$DST/app/src/main/java/com/rehealth/genie/ui/ReHealthApp.kt"
cp "$SRC/app/src/main/java/com/rehealth/genie/ring/RingViewModel.kt" \
   "$DST/app/src/main/java/com/rehealth/genie/ring/RingViewModel.kt"
cp "$SRC/app/src/main/java/com/rehealth/genie/ring/data/RingDataDao.kt" \
   "$DST/app/src/main/java/com/rehealth/genie/ring/data/RingDataDao.kt"
cd "$DST"
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH="$JAVA_HOME/bin:$PATH"
echo "==> JAVA_HOME=$JAVA_HOME"
echo "==> running assembleDebug"
./gradlew assembleDebug --no-daemon
