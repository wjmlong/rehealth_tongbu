#!/usr/bin/env bash
set -u
MYSQL_C="rehealth-staging-mysql-1"
REDIS_C="rehealth-staging-redis-1"

echo "=== MySQL: list databases ==="
docker exec "$MYSQL_C" sh -c 'mysql -uroot -proot -e "SHOW DATABASES;" 2>/dev/null' 2>&1 | head -20

echo "=== MySQL: find jeecg db + sys_user table ==="
for db in $(docker exec "$MYSQL_C" sh -c 'mysql -uroot -proot -e "SHOW DATABASES;" 2>/dev/null' 2>/dev/null | grep -vE "Database|information|performance|mysql"); do
  has_user=$(docker exec "$MYSQL_C" sh -c "mysql -uroot -proot $db -e \"SHOW TABLES LIKE 'sys_user';\" 2>/dev/null" 2>/dev/null | grep -c sys_user || true)
  if [ "$has_user" -gt 0 ]; then
    echo "FOUND sys_user in db: $db"
    docker exec "$MYSQL_C" sh -c "mysql -uroot -proot $db -e \"DESCRIBE sys_user;\" 2>/dev/null" 2>&1 | head -30
    echo "--- existing users (phone/username) ---"
    docker exec "$MYSQL_C" sh -c "mysql -uroot -proot $db -e \"SELECT id,username,phone,telephone,status,del_flag FROM sys_user LIMIT 10;\" 2>/dev/null" 2>&1
  fi
done

echo "=== Redis: check password / scan keys ==="
docker exec "$REDIS_C" sh -c 'redis-cli KEYS "*" 2>/dev/null | head -20' 2>&1
echo "--- try without auth ---"
docker exec "$REDIS_C" sh -c 'redis-cli PING 2>/dev/null' 2>&1
