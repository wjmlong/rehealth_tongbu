#!/usr/bin/env bash
LOCAL=/mnt/d/rehealthAI/Android-apk
REMOTE=/tmp/real_branch
OUT=/mnt/d/rehealthAI/outputs/realdevice_live_content.diff
: > "$OUT"
FILES=(
  "app/build.gradle.kts"
  "app/src/main/AndroidManifest.xml"
  "app/src/main/java/com/rehealth/genie/ReHealthApplication.kt"
  "app/src/main/java/com/rehealth/genie/chat/DeepSeekClient.kt"
  "app/src/main/java/com/rehealth/genie/data/AppDatabase.kt"
  "app/src/main/java/com/rehealth/genie/phm/MockPhmService.kt"
  "app/src/main/java/com/rehealth/genie/ring/MockRingRepository.kt"
  "app/src/main/java/com/rehealth/genie/ring/RingRepository.kt"
  "app/src/main/java/com/rehealth/genie/ring/RingViewModel.kt"
  "app/src/main/java/com/rehealth/genie/ring/data/RingDataDao.kt"
  "app/src/main/java/com/rehealth/genie/ring/mrd/MrdBleRingRepository.kt"
  "app/src/main/java/com/rehealth/genie/ring/mrd/MrdProtocolAdapter.kt"
  "app/src/main/java/com/rehealth/genie/ui/LoginScreen.kt"
  "app/src/main/java/com/rehealth/genie/ui/ReHealthApp.kt"
)
for rel in "${FILES[@]}"; do
  echo "########## DIFF: $rel ##########" >> "$OUT"
  diff -u --strip-trailing-cr "$REMOTE/$rel" "$LOCAL/$rel" >> "$OUT" 2>&1
  echo "" >> "$OUT"
done
echo "written: $OUT  ($(wc -l < "$OUT") lines)"
