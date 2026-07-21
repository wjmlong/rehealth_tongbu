#!/usr/bin/env bash
set -u
MYSQL_C="rehealth-staging-mysql-1"
MYSQL_PW="dfe4d17b82b7e7756fc1409b49019359454099b34572f59b"
DB="jeecg-boot"

echo "=== check existing user's password/salt ==="
docker exec "$MYSQL_C" sh -c "mysql -uroot -p'$MYSQL_PW' $DB -e \"SELECT id,username,phone,password,salt FROM sys_user WHERE phone='13507007984';\" 2>/dev/null" 2>&1

echo "=== check ceshi user password format as reference ==="
docker exec "$MYSQL_C" sh -c "mysql -uroot -p'$MYSQL_PW' $DB -e \"SELECT id,username,phone,password,salt FROM sys_user WHERE username='ceshi';\" 2>/dev/null" 2>&1

echo "=== check another user for reference ==="
docker exec "$MYSQL_C" sh -c "mysql -uroot -p'$MYSQL_PW' $DB -e \"SELECT id,username,phone,LEFT(password,30) as pw_prefix,salt FROM sys_user WHERE password IS NOT NULL AND password != '' LIMIT 3;\" 2>/dev/null" 2>&1
