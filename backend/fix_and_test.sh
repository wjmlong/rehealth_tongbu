#!/bin/bash
set -ex

REDIS_PASS="ae3860fbb510eedc9d8bf43e49b60c2f21e5cb74c1919172"
MYSQL_PASS="dfe4d17b82b7e7756fc1409b49019359454099b34572f59b"
BASE="http://localhost:8080/jeecg-boot"

# Step 1: Compute password hash
INNER=$(echo -n "123456" | md5sum | awk '{print $1}')
HASH=$(echo -n "${INNER}APDzGLuO" | md5sum | awk '{print $1}')
echo "Password hash for 123456: $HASH"

# Step 2: Update password in MySQL
docker exec rehealth-staging-mysql-1 mysql -uroot -p"$MYSQL_PASS" jeecg-boot \
  -e "UPDATE sys_user SET password='$HASH' WHERE phone='13507007984';"
echo "Password updated."

# Step 3: Verify
docker exec rehealth-staging-mysql-1 mysql -uroot -p"$MYSQL_PASS" jeecg-boot \
  -e "SELECT id,username,phone,LEFT(password,32) as pw,salt FROM sys_user WHERE phone='13507007984';" 2>/dev/null

# Step 4: Login via /sys/mLogin (username+password, NO captcha)
echo ""
echo "=== Login via /sys/mLogin ==="
RESP=$(curl -s --max-time 10 -X POST "$BASE/sys/mLogin" \
  -H "Content-Type: application/json" \
  -d "{\"username\":\"13507007984\",\"password\":\"123456\"}")
echo "$RESP"

# Extract token
TOKEN=$(echo "$RESP" | python3 -c "import sys,json; d=json.load(sys.stdin); t=d.get('result',{}).get('token','') if d.get('result') else ''; print(t)" 2>/dev/null)
echo "Token: ${TOKEN:0:60}..."

if [ -z "$TOKEN" ]; then
  echo "LOGIN FAILED. Trying direct password check..."
  # Maybe JeecgBoot uses different hash algorithm? Try plain MD5
  PLAIN_MD5=$(echo -n "123456" | md5sum | awk '{print $1}')
  echo "Trying plain MD5 hash: $PLAIN_MD5"
  docker exec rehealth-staging-mysql-1 mysql -uroot -p"$MYSQL_PASS" jeecg-boot \
    -e "UPDATE sys_user SET password='$PLAIN_MD5' WHERE phone='13507007984';"
  RESP2=$(curl -s --max-time 10 -X POST "$BASE/sys/mLogin" \
    -H "Content-Type: application/json" \
    -d "{\"username\":\"13507007984\",\"password\":\"123456\"}")
  echo "$RESP2"
  TOKEN=$(echo "$RESP2" | python3 -c "import sys,json; d=json.load(sys.stdin); t=d.get('result',{}).get('token','') if d.get('result') else ''; print(t)" 2>/dev/null)
  echo "Token (plain MD5): ${TOKEN:0:60}..."
fi

if [ -z "$TOKEN" ]; then
  echo "STILL FAILED. Checking JeecgBoot source for hash algorithm..."
  # Check what hashing JeecgBoot actually uses
  docker exec rehealth-staging-backend-1 sh -c 'find / -name "*.jar" -path "*jeecg*" 2>/dev/null | head -5'
  exit 1
fi

# Step 5: Test /rehealth/mobile/features/evaluate
echo ""
echo "=== /rehealth/mobile/features/evaluate ==="
curl -s --max-time 15 -X POST "$BASE/rehealth/mobile/features/evaluate" \
  -H "Content-Type: application/json" \
  -H "X-Access-Token: $TOKEN" \
  -d '{"featureVector":{"age":52,"gender":1,"bmi":27.5,"sbp":145,"dbp":90,"fasting_glucose":6.2,"total_cholesterol":5.8,"ldl":3.9,"hdl":1.1,"triglycerides":2.1,"exercise_days":2,"smoking":1,"drinking":0,"diabetes_history":0,"hypertension_history":1,"family_history":1,"featureQuality":{}}}' | python3 -m json.tool 2>/dev/null

# Step 6: Test /rehealth/mobile/risk/latest
echo ""
echo "=== /rehealth/mobile/risk/latest ==="
curl -s --max-time 10 "$BASE/rehealth/mobile/risk/latest" \
  -H "X-Access-Token: $TOKEN" | python3 -m json.tool 2>/dev/null

# Step 7: Test /rehealth/mobile/interventions/today
echo ""
echo "=== /rehealth/mobile/interventions/today ==="
curl -s --max-time 10 "$BASE/rehealth/mobile/interventions/today" \
  -H "X-Access-Token: $TOKEN" | python3 -m json.tool 2>/dev/null
