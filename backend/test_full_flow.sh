#!/bin/bash
set -e
BASE="http://localhost:8080/jeecg-boot"

echo "=== 1. Login via /sys/mLogin ==="
LOGIN_RESP=$(curl -s --max-time 10 -X POST "$BASE/sys/mLogin" \
  -H "Content-Type: application/json" \
  -d '{"mobile":"13507007984","password":"123456"}')
echo "$LOGIN_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print('success:', d.get('success')); print('msg:', d.get('message')); print('token:', d.get('result',{}).get('token','N/A')[:60]+'...' if d.get('result') else 'N/A')" 2>/dev/null || echo "$LOGIN_RESP"

TOKEN=$(echo "$LOGIN_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('result',{}).get('token','') if d.get('result') else '')" 2>/dev/null)

if [ -z "$TOKEN" ]; then
  echo "=== Login failed, trying /sys/login with captcha ==="
  # Get captcha key
  CAPTCHA_RESP=$(curl -s --max-time 5 "$BASE/sys/randomImage/testkey123")
  echo "Captcha image response length: ${#CAPTCHA_RESP}"
  
  # Inject captcha code into Redis
  docker exec rehealth-staging-redis-1 redis-cli SET "captcha:testkey123" "1234" EX 300 2>/dev/null
  echo "Injected captcha: testkey123 = 1234"
  
  # Login with username (not mobile)
  LOGIN_RESP=$(curl -s --max-time 10 -X POST "$BASE/sys/login" \
    -H "Content-Type: application/json" \
    -d '{"username":"13507007984","password":"123456","checkKey":"testkey123","captcha":"1234"}')
  echo "$LOGIN_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print('success:', d.get('success')); print('msg:', d.get('message')); t=d.get('result',{}).get('token','') if d.get('result') else ''; print('token:', t[:60]+'...' if t else 'NONE')" 2>/dev/null || echo "$LOGIN_RESP"
  TOKEN=$(echo "$LOGIN_RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); print(d.get('result',{}).get('token','') if d.get('result') else '')" 2>/dev/null)
fi

if [ -z "$TOKEN" ]; then
  echo "=== FAILED: No token obtained ==="
  exit 1
fi

echo ""
echo "=== 2. Call /rehealth/mobile/features/evaluate ==="
curl -s --max-time 15 -X POST "$BASE/rehealth/mobile/features/evaluate" \
  -H "Content-Type: application/json" \
  -H "X-Access-Token: $TOKEN" \
  -d '{"featureVector":{"age":52,"gender":1,"bmi":27.5,"sbp":145,"dbp":90,"fasting_glucose":6.2,"total_cholesterol":5.8,"ldl":3.9,"hdl":1.1,"triglycerides":2.1,"exercise_days":2,"smoking":1,"drinking":0,"diabetes_history":0,"hypertension_history":1,"family_history":1,"featureQuality":{}}}' 2>&1 | python3 -m json.tool 2>/dev/null || echo "Raw output above"

echo ""
echo "=== 3. Call /rehealth/mobile/risk/latest ==="
curl -s --max-time 10 "$BASE/rehealth/mobile/risk/latest" \
  -H "X-Access-Token: $TOKEN" 2>&1 | python3 -m json.tool 2>/dev/null || echo "Raw output above"

echo ""
echo "=== 4. Call /rehealth/mobile/interventions/today ==="
curl -s --max-time 10 "$BASE/rehealth/mobile/interventions/today" \
  -H "X-Access-Token: $TOKEN" 2>&1 | python3 -m json.tool 2>/dev/null || echo "Raw output above"
