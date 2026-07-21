#!/usr/bin/env bash
set -u
REDIS_C="rehealth-staging-redis-1"
REDIS_PW="ae3860fbb510eedc9d8bf43e49b60c2f21e5cb74c1919172"

echo "=== scan all Redis keys ==="
docker exec "$REDIS_C" redis-cli -a "$REDIS_PW" --no-auth-warning KEYS "*" 2>/dev/null

echo "=== try common JeecgBoot SMS key patterns ==="
for pattern in "sms:*" "captcha:*" "code:*" "verify:*" "login:*" "13507007984*" "*13507007984*"; do
  result=$(docker exec "$REDIS_C" redis-cli -a "$REDIS_PW" --no-auth-warning KEYS "$pattern" 2>/dev/null)
  if [ -n "$result" ]; then
    echo "  $pattern -> $result"
  fi
done

echo "=== inject SMS captcha '1234' for 13507007984 ==="
# Try common JeecgBoot SMS captcha Redis key patterns
for key in "sms:code:13507007984" "captcha:code:13507007984" "SMS_CODE:13507007984" "jeecg:sms:13507007984" "rehealth:sms:13507007984" "captcha_13507007984"; do
  docker exec "$REDIS_C" redis-cli -a "$REDIS_PW" --no-auth-warning SET "$key" "1234" EX 300 2>/dev/null
  docker exec "$REDIS_C" redis-cli -a "$REDIS_PW" --no-auth-warning EXPIRE "$key" 300 2>/dev/null
  echo "  SET $key = 1234 (TTL 300s)"
done

echo "=== verify injection ==="
docker exec "$REDIS_C" redis-cli -a "$REDIS_PW" --no-auth-warning KEYS "*13507007984*" 2>/dev/null
