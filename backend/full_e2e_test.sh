#!/bin/bash
set -e

REDIS_PASS="ae3860fbb510eedc9d8bf43e49b60c2f21e5cb74c1919172"
MYSQL_PASS="dfe4d17b82b7e7756fc1409b49019359454099b34572f59b"
BASE="http://localhost:8080/jeecg-boot"

echo "=== 1. Update password hash in MySQL ==="
docker exec rehealth-staging-mysql-1 mysql -uroot -p"$MYSQL_PASS" jeecg-boot \
  -e "UPDATE sys_user SET password='302aa544675f1992' WHERE phone='13507007984';" 2>/dev/null

echo "=== Verify ==="
docker exec rehealth-staging-mysql-1 mysql -uroot -p"$MYSQL_PASS" jeecg-boot \
  -e "SELECT id,username,phone,LEFT(password,32) as pw,salt FROM sys_user WHERE phone='13507007984';" 2>/dev/null

echo ""
echo "=== 2. Clear any login fail counters ==="
docker exec rehealth-staging-redis-1 redis-cli -a "$REDIS_PASS" DEL "sys.login.failcount.13507007984" 2>/dev/null
docker exec rehealth-staging-redis-1 redis-cli -a "$REDIS_PASS" DEL "LOGIN_FAIL_13507007984" 2>/dev/null

echo ""
echo "=== 3. Login via /sys/mLogin ==="
RESP=$(curl -s --max-time 10 -X POST "$BASE/sys/mLogin" \
  -H "Content-Type: application/json" \
  -d '{"username":"13507007984","password":"123456"}')
echo "$RESP" | python3 -m json.tool 2>/dev/null || echo "$RESP"

# Extract token
TOKEN=$(echo "$RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); t=d.get('result',{}).get('token','') if d.get('result') else ''; print(t)" 2>/dev/null)
echo ""
echo "Token: ${TOKEN:0:80}..."

if [ -z "$TOKEN" ]; then
  echo ""
  echo "=== LOGIN FAILED. Checking all Redis keys for fail count ==="
  docker exec rehealth-staging-redis-1 redis-cli -a "$REDIS_PASS" KEYS "*fail*" 2>/dev/null
  docker exec rehealth-staging-redis-1 redis-cli -a "$REDIS_PASS" KEYS "*13507*" 2>/dev/null
  exit 1
fi

echo ""
echo "=== 4. Call /rehealth/mobile/features/evaluate ==="
curl -s --max-time 15 -X POST "$BASE/rehealth/mobile/features/evaluate" \
  -H "Content-Type: application/json" \
  -H "X-Access-Token: $TOKEN" \
  -d '{"featureVector":{"age":52,"gender":1,"bmi":27.5,"sbp":145,"dbp":90,"fasting_glucose":6.2,"total_cholesterol":5.8,"ldl":3.9,"hdl":1.1,"triglycerides":2.1,"exercise_days":2,"smoking":1,"drinking":0,"diabetes_history":0,"hypertension_history":1,"family_history":1,"featureQuality":{}}}' | python3 -m json.tool 2>/dev/null

echo ""
echo "=== 5. Call /rehealth/mobile/risk/latest ==="
curl -s --max-time 10 "$BASE/rehealth/mobile/risk/latest" \
  -H "X-Access-Token: $TOKEN" | python3 -m json.tool 2>/dev/null

echo ""
echo "=== 6. Call /rehealth/mobile/interventions/today ==="
curl -s --max-time 10 "$BASE/rehealth/mobile/interventions/today" \
  -H "X-Access-Token: $TOKEN" | python3 -m json.tool 2>/dev/null
