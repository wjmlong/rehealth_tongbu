#!/usr/bin/env bash
set -u
REDIS_C="rehealth-staging-redis-1"
REDIS_PW="ae3860fbb510eedc9d8bf43e49b60c2f21e5cb74c1919172"
BASE="http://localhost:8080/jeecg-boot"

echo "=== 1) request fresh captcha with known key ==="
KEY="testkey$(date +%s)"
echo "key=$KEY"
curl -s --max-time 8 "$BASE/sys/randomImage/$KEY" > /dev/null 2>&1

echo "=== 2) scan ALL redis keys after captcha request ==="
docker exec "$REDIS_C" redis-cli -a "$REDIS_PW" --no-auth-warning KEYS "*" 2>/dev/null

echo "=== 3) try to find captcha key pattern ==="
docker exec "$REDIS_C" redis-cli -a "$REDIS_PW" --no-auth-warning KEYS "*captcha*" 2>/dev/null
docker exec "$REDIS_C" redis-cli -a "$REDIS_PW" --no-auth-warning KEYS "*randomImage*" 2>/dev/null
docker exec "$REDIS_C" redis-cli -a "$REDIS_PW" --no-auth-warning KEYS "*code*" 2>/dev/null
docker exec "$REDIS_C" redis-cli -a "$REDIS_PW" --no-auth-warning KEYS "*$KEY*" 2>/dev/null
docker exec "$REDIS_C" redis-cli -a "$REDIS_PW" --no-auth-warning KEYS "*sys*" 2>/dev/null

echo "=== 4) compute password hash with python ==="
docker exec "$REDIS_C" sh -c "python3 -c \"import hashlib; h1=hashlib.md5('123456'.encode()).hexdigest(); h2=hashlib.md5((h1+'APDzGLuO').encode()).hexdigest(); print(h2)\" 2>/dev/null || echo 'no python3 in redis'" 2>&1

echo "=== 5) try in mysql container ==="
MYSQL_C="rehealth-staging-mysql-1"
MYSQL_PW="dfe4d17b82b7e7756fc1409b49019359454099b34572f59b"
DB="jeecg-boot"
docker exec "$MYSQL_C" sh -c "echo 'SELECT MD5(CONCAT(MD5(\"123456\"), \"APDzGLuO\"));' | mysql -uroot -p'$MYSQL_PW' $DB -N 2>/dev/null" 2>&1
echo "---"
# Try alternative: use SELECT directly
docker exec "$MYSQL_C" sh -c "mysql -uroot -p'$MYSQL_PW' $DB -N -e \"SELECT MD5(CONCAT(MD5('123456'), 'APDzGLuO'));\" 2>/dev/null" 2>&1
