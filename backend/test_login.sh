#!/bin/bash
set -e

REDIS_PASS="ae3860fbb510eedc9d8bf43e49b60c2f21e5cb74c1919172"
MYSQL_PASS="dfe4d17b82b7e7756fc1409b49019359454099b34572f59b"
BASE="http://localhost:8080/jeecg-boot"

echo "=== 1. Verify password hash in MySQL ==="
docker exec rehealth-staging-mysql-1 mysql -uroot -p"$MYSQL_PASS" jeecg-boot \
  -e "SELECT id,username,phone,LEFT(password,32) as pw,salt FROM sys_user WHERE phone='13507007984';" 2>/dev/null

echo ""
echo "=== 2. Inject captcha into Redis (with auth) ==="
docker exec rehealth-staging-redis-1 redis-cli -a "$REDIS_PASS" SET "captcha:cap001" "1234" EX 600 2>/dev/null

echo ""
echo "=== 3. Try /sys/login with JSON body ==="
curl -sv --max-time 10 -X POST "$BASE/sys/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"13507007984","password":"123456","checkKey":"cap001","captcha":"1234"}' 2>&1 | tail -20

echo ""
echo "=== 4. Check Redis keys after captcha inject ==="
docker exec rehealth-staging-redis-1 redis-cli -a "$REDIS_PASS" KEYS "*cap*" 2>/dev/null

echo ""
echo "=== 5. Check what SysLoginModel fields look like ==="
# Look at JeecgBoot source for the field names
docker exec rehealth-staging-backend-1 find / -name "SysLoginModel*" -type f 2>/dev/null | head -5

echo ""
echo "=== 6. Try mLogin with SysLoginModel fields ==="
curl -s --max-time 10 -X POST "$BASE/sys/mLogin" \
  -H "Content-Type: application/json" \
  -d '{"mobile":"13507007984","captcha":"1234","checkKey":"cap001"}' 2>&1

echo ""
echo "=== 7. Try /sys/login with form data ==="
curl -s --max-time 10 -X POST "$BASE/sys/login" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d 'username=13507007984&password=123456&checkKey=cap001&captcha=1234' 2>&1
