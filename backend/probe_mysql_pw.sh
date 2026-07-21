#!/usr/bin/env bash
set -u
MYSQL_C="rehealth-staging-mysql-1"
echo "=== try different mysql passwords ==="
for pw in "" "root" "password" "123456" "rehealth" "staging"; do
  result=$(docker exec "$MYSQL_C" sh -c "mysql -uroot -p'$pw' -e 'SELECT 1;' 2>&1" | head -1)
  echo "password='$pw' -> $result"
done
echo "=== check mysql env vars ==="
docker inspect "$MYSQL_C" --format '{{range .Config.Env}}{{println .}}{{end}}' 2>/dev/null | grep -i mysql
