#!/usr/bin/env bash
set -u
echo "=== staging mysql container ==="
docker ps --format 'table {{.Names}}\t{{.Image}}\t{{.Status}}' 2>/dev/null | grep -i mysql
echo "=== try to exec into mysql and list jeecg databases ==="
MYSQL_C=$(docker ps --filter "name=mysql" --format '{{.Names}}' | head -1)
echo "mysql container: $MYSQL_C"
if [ -n "$MYSQL_C" ]; then
  docker exec "$MYSQL_C" sh -c 'mysql -uroot -proot -e "SHOW DATABASES;" 2>&1 | head -30' 2>&1
fi
