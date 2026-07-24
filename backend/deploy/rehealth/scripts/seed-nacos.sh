#!/usr/bin/env bash
# Seed Nacos config for the rehealth local stack.
#
# Nacos runs inside Docker (service "nacos") with auth DISABLED in dev.
# It is NOT port-exposed to the host, so we copy the seed files into the
# nacos container and POST them via `docker exec` (the nacos image ships curl,
# used by its own healthcheck).
#
# Target: namespace "springboot3", group "DEFAULT_GROUP" (the baked values in
# the jeecg-boot jars' default `dev` maven profile).
#
# Idempotent: re-publishing identical content does not disturb running services.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SEED_DIR="$(cd "$SCRIPT_DIR/../.local-runtime/nacos-seed" && pwd)"
NAMESPACE="springboot3"
GROUP="DEFAULT_GROUP"

NACOS_CONTAINER="$(docker ps -q --filter "label=com.docker.compose.service=nacos" --filter "status=running" | head -n1)"
if [ -z "$NACOS_CONTAINER" ]; then
  echo "ERROR: nacos container not found or not running. Start the stack first." >&2
  exit 1
fi
echo "Using nacos container: $NACOS_CONTAINER"

# Wait for Nacos to be ready (actuator health lives on 8848 inside the container).
echo "Waiting for Nacos actuator health..."
for i in $(seq 1 60); do
  if docker exec "$NACOS_CONTAINER" curl -fsS "http://localhost:8848/nacos/actuator/health" >/dev/null 2>&1; then
    echo "Nacos is up."
    break
  fi
  if [ "$i" -eq 60 ]; then
    echo "ERROR: Nacos did not become healthy in time." >&2
    exit 1
  fi
  sleep 2
done

publish() {
  local data_id="$1" file="$2" type="$3"
  if [ ! -f "$file" ]; then
    echo "WARN: seed file missing, skipping $data_id: $file" >&2
    return
  fi
  echo "Publishing $data_id (type=$type)..."
  # Pipe the file straight into the container's curl via stdin (content@-).
  # Avoids `docker cp` host-path conversion issues under Git Bash on Windows.
  if docker exec -i "$NACOS_CONTAINER" curl -fsS -X POST \
      "http://localhost:8848/nacos/v1/cs/configs" \
      --data-urlencode "dataId=$data_id" \
      --data-urlencode "group=$GROUP" \
      --data-urlencode "tenant=$NAMESPACE" \
      --data-urlencode "type=$type" \
      --data-urlencode "content@-" < "$file"; then
    echo "  -> $data_id published OK"
  else
    echo "  -> ERROR publishing $data_id" >&2
    exit 1
  fi
}

publish "jeecg.yaml"          "$SEED_DIR/jeecg.yaml"          "yaml"
publish "jeecg-dev.yaml"      "$SEED_DIR/jeecg-dev.yaml"      "yaml"
publish "gateway-router.json" "$SEED_DIR/gateway-router.json" "json"

echo "Nacos seeding complete."
