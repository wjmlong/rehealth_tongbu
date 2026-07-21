#!/usr/bin/env bash
set -u
LOG=/mnt/d/rehealthAI/backend/startup.log
: > "$LOG"
echo "[$(date)] backend startup begin" | tee -a "$LOG"

cd /mnt/d/rehealthAI/backend/jeecg-boot || { echo "cd failed" | tee -a "$LOG"; exit 1; }

# Ensure docker running
if ! docker info >/dev/null 2>&1; then
  echo "[$(date)] docker not running; attempting service start" | tee -a "$LOG"
  sudo service docker start >/dev/null 2>&1 || true
  sleep 5
fi
docker info >/dev/null 2>&1 && echo "[$(date)] docker OK" | tee -a "$LOG" || echo "[$(date)] docker STILL DOWN" | tee -a "$LOG"

echo "[$(date)] docker compose up -d (this may pull images)" | tee -a "$LOG"
docker compose up -d >> "$LOG" 2>&1
echo "[$(date)] compose exit=$?" | tee -a "$LOG"

# Poll for app health on 8080
for i in $(seq 1 60); do
  code=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/jeecg-boot/ 2>/dev/null || echo "000")
  echo "[$(date)] probe $i -> HTTP $code" | tee -a "$LOG"
  if [ "$code" != "000" ] && [ "$code" != "404" ]; then
    echo "[$(date)] BACKEND UP (HTTP $code)" | tee -a "$LOG"
    break
  fi
  sleep 10
done

echo "[$(date)] final container status:" | tee -a "$LOG"
docker compose ps >> "$LOG" 2>&1
echo "[$(date)] startup script done" | tee -a "$LOG"
