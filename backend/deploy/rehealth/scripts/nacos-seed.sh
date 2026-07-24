#!/bin/bash
# Container-native Nacos seeder (runs as a one-shot init container inside the
# compose network). Reaches Nacos by service name and pushes the local seed
# files. Idempotent: re-publishing identical content does not disturb services.
#
# Mounts:
#   /seed        -> .local-runtime/nacos-seed  (jeecg.yaml, jeecg-dev.yaml, gateway-router.json)
set -euo pipefail

NACOS="http://nacos:8848"
NS="springboot3"
GROUP="DEFAULT_GROUP"
SEED_DIR="/seed"

echo "[nacos-seed] waiting for Nacos at $NACOS ..."
i=0
while [ "$i" -lt 60 ]; do
  if curl -fsS "$NACOS/nacos/actuator/health" >/dev/null 2>&1; then
    echo "[nacos-seed] Nacos is up."
    break
  fi
  i=$((i + 1))
  sleep 2
done

seed() {
  local did="$1" file="$2" type="$3"
  if [ ! -f "$file" ]; then
    echo "[nacos-seed] WARN: missing $file, skip $did" >&2
    return
  fi
  echo "[nacos-seed] publishing $did ($type)"
  curl -fsS -X POST "$NACOS/nacos/v1/cs/configs" \
    --data-urlencode "dataId=$did" \
    --data-urlencode "group=$GROUP" \
    --data-urlencode "tenant=$NS" \
    --data-urlencode "type=$type" \
    --data-urlencode "content@$file"
  echo "[nacos-seed] $did OK"
}

seed "jeecg.yaml"          "$SEED_DIR/jeecg.yaml"          "yaml"
seed "jeecg-dev.yaml"      "$SEED_DIR/jeecg-dev.yaml"      "yaml"
seed "gateway-router.json" "$SEED_DIR/gateway-router.json" "json"

echo "[nacos-seed] complete."
