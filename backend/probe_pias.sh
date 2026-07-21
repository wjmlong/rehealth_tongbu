#!/usr/bin/env bash
set -u
echo "=== model-service (staging) on 8000: root & docs ==="
curl -s -o /dev/null -w "root http=%{http_code}\n" --max-time 8 http://localhost:8000/ 2>&1
curl -s -w "\nhttp=%{http_code}\n" --max-time 8 http://localhost:8000/api/pias/v2 2>&1 | head -c 500
echo ""
curl -s -w "\nhttp=%{http_code}\n" --max-time 8 http://localhost:8000/docs 2>&1 | head -c 200
echo ""
echo "=== redis: scan for sms/captcha keys ==="
REDIS_C=$(docker ps --filter "name=redis" --format '{{.Names}}' | head -1)
echo "redis container: $REDIS_C"
if [ -n "$REDIS_C" ]; then
  docker exec "$REDIS_C" sh -c 'redis-cli --no-auth-warning KEYS "*sms*" 2>/dev/null; echo "---"; redis-cli --no-auth-warning KEYS "*captcha*" 2>/dev/null; echo "---"; redis-cli --no-auth-warning KEYS "*13507007984*" 2>/dev/null' 2>&1
fi
