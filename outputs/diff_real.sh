#!/usr/bin/env bash
LOCAL=/mnt/d/rehealthAI/Android-apk
REMOTE=/tmp/real_branch
OUT=/mnt/d/rehealthAI/outputs/realdevice_live_diff.txt
: > "$OUT"
echo "===== FILE-LEVEL DIFF: codex/real-device (fetched) vs LOCAL working tree =====" >> "$OUT"
echo "generated: $(date)" >> "$OUT"
echo "" >> "$OUT"
diff -rq --strip-trailing-cr "$REMOTE" "$LOCAL" \
  --exclude=.git --exclude=build --exclude=.gradle --exclude=.idea --exclude=.kotlin \
  --exclude=.ref --exclude=outputs --exclude=.omo --exclude=.codegraph \
  --exclude='*.iml' --exclude=local.properties --exclude=BUILD_NOTES.md --exclude=build.log \
  --exclude='*.aar' --exclude='*.jar' --exclude='*.bin' --exclude='*.apk' --exclude='*.so' \
  --exclude='*.png' --exclude='*.jpg' --exclude='*.jpeg' --exclude='*.gif' --exclude='*.ico' \
  --exclude='*.ttf' --exclude='*.woff' --exclude='*.woff2' --exclude='*.zip' --exclude='*.keystore' \
  --exclude='*.mp4' --exclude='*.wav' --exclude='*.len' --exclude='*.log' \
  --exclude='rehealth-*' --exclude='current-my.xml' --exclude='.ref' \
  >> "$OUT" 2>&1
echo "" >> "$OUT"
echo "===== SUMMARY COUNTS =====" >> "$OUT"
echo "Only in branch (deleted locally): $(grep -c '^Only in '"$REMOTE" "$OUT")" >> "$OUT"
echo "Only in local (new locally):      $(grep -c '^Only in '"$LOCAL" "$OUT")" >> "$OUT"
echo "Differ (both exist, changed):     $(grep -c '^Files ' "$OUT")" >> "$OUT"
echo "DONE" >> "$OUT"
cat "$OUT"
