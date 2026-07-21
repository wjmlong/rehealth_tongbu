#!/usr/bin/env bash
set -u
REDIS_C="rehealth-staging-redis-1"
echo "=== check redis env vars (password) ==="
docker inspect "$REDIS_C" --format '{{range .Config.Env}}{{println .}}{{end}}' 2>/dev/null | grep -iE "redis|pass|auth"
echo "=== check compose for redis password ==="
COMPOSE="/home/wjmlong/rehealthAI/backend/deploy/staging/compose.yml"
grep -A5 -i redis "$COMPOSE" 2>/dev/null | head -20
echo "=== check .env file ==="
cat /home/wjmlong/rehealthAI/backend/deploy/staging/.env 2>/dev/null | head -20
echo "=== try redis without auth ==="
docker exec "$REDIS_C" redis-cli PING 2>&1
echo "=== try redis with common passwords ==="
for pw in "" "root" "redis" "rehealth" "staging" "123456"; do
  result=$(docker exec "$REDIS_C" redis-cli -a "$pw" PING 2>/dev/null | head -1)
  echo "password='$pw' -> $result"
done
