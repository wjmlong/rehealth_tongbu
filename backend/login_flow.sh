#!/usr/bin/env bash
set -u
BASE="http://localhost:8080/jeecg-boot"
REDIS_C="rehealth-staging-redis-1"
REDIS_PW="ae3860fbb510eedc9d8bf43e49b60c2f21e5cb74c1919172"

echo "=== Step1: get captcha image key ==="
KEY="captcha_$(date +%s)"
echo "captcha key: $KEY"

echo "=== Step2: get captcha image ==="
CAPTCHA_RESPONSE=$(curl -s --max-time 8 "$BASE/sys/randomImage/$KEY" 2>&1)
echo "captcha response length: ${#CAPTCHA_RESPONSE}"
echo "$CAPTCHA_RESPONSE" | head -c 200
echo ""

echo "=== Step3: inject captcha '1234' into Redis ==="
docker exec "$REDIS_C" redis-cli -a "$REDIS_PW" --no-auth-warning SET "captcha:$KEY" "1234" EX 300 2>/dev/null
echo "SET captcha:$KEY = 1234"

echo "=== Step4: login with username=13507007984 password=123456 captcha=1234 ==="
LOGIN_RESULT=$(curl -s -w "\nHTTP=%{http_code}" --max-time 10 -X POST "$BASE/sys/login" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"13507007984\",\"password\":\"123456\",\"checkKey\":\"$KEY\",\"captcha\":\"1234\"}" 2>&1)
echo "$LOGIN_RESULT" | head -c 500

echo ""
echo "=== Step5: if login failed, try mLogin with phone+captcha ==="
MLOGIN_RESULT=$(curl -s -w "\nHTTP=%{http_code}" --max-time 10 -X POST "$BASE/sys/mLogin" \
  -H "Content-Type: application/json" \
  -d "{\"mobile\":\"13507007984\",\"captcha\":\"1234\"}" 2>&1)
echo "$MLOGIN_RESULT" | head -c 500
