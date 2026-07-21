#!/usr/bin/env bash
set -u
MYSQL_C=$(docker ps --filter "name=mysql" --format '{{.Names}}' | head -1)
echo "mysql container: $MYSQL_C"
if [ -z "$MYSQL_C" ]; then echo "NO MYSQL CONTAINER"; exit 0; fi
echo "=== databases ==="
docker exec "$MYSQL_C" sh -c 'mysql -uroot -proot -e "SHOW DATABASES;" 2>/dev/null' 2>&1 | head -20
echo "=== search user 13507007984 across likely dbs ==="
for db in $(docker exec "$MYSQL_C" sh -c 'mysql -uroot -proot -e "SHOW DATABASES;" 2>/dev/null' 2>/dev/null | grep -iE "jeecg|rehealth|sys"); do
  echo "--- db: $db ---"
  docker exec "$MYSQL_C" sh -c "mysql -uroot -proot $db -e \"SELECT id,username,phone,telephone,status,del_flag FROM sys_user WHERE phone='13507007984' OR telephone='13507007984' OR username='13507007984' LIMIT 5;\" 2>/dev/null" 2>&1
done
