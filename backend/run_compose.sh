#!/usr/bin/env bash
set -u
COMPOSE_DIR="/mnt/d/rehealthai/backend/jeecg-boot"
LOG="/mnt/d/rehealthai/backend/startup.log"
mkdir -p "$(dirname "$LOG")"
: > "$LOG"
echo "[$(date)] starting docker compose up -d" | tee -a "$LOG"
cd "$COMPOSE_DIR" || { echo "cd failed" | tee -a "$LOG"; exit 1; }
docker compose up -d >> "$LOG" 2>&1
echo "[$(date)] compose up returned $?" | tee -a "$LOG"
# probe loop: wait for backend HTTP on 8080
for i in $(seq 1 90); do
  code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 http://localhost:8080/jeecg-boot/ 2>/dev/null || echo "000")
  echo "[$(date)] probe $i -> $code" | tee -a "$LOG"
  if [ "$code" != "000" ] && [ "$code" != "404" ]; then
    echo "[$(date)] BACKEND_UP http=$code" | tee -a "$LOG"
    break
  fi
  sleep 10
done
echo "[$(date)] final docker compose ps:" | tee -a "$LOG"
docker compose ps | tee -a "$LOG"
