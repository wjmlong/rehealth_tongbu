#!/usr/bin/env bash
set -u
MYSQL_C="rehealth-staging-mysql-1"
MYSQL_PW="dfe4d17b82b7e7756fc1409b49019359454099b34572f59b"
DB="jeecg-boot"
REDIS_C="rehealth-staging-redis-1"
REDIS_PW="ae3860fbb510eedc9d8bf43e49b60c2f21e5cb74c1919172"

# JeecgBoot password: md5(md5(plain) + salt)
# Set password "123456" for user 13507007984 (salt=APDzGLuO)
# Compute in the mysql container
echo "=== compute md5 hash for password 123456 ==="
HASH=$(docker exec "$MYSQL_C" sh -c "echo -n 'SELECT MD5(CONCAT(MD5('123456'), 'APDzGLuO'));' | mysql -uroot -p'$MYSQL_PW' $DB -N 2>/dev/null" 2>&1 | tr -d '[:space:]')
echo "hash=$HASH"

echo "=== update password for 13507007984 ==="
docker exec "$MYSQL_C" sh -c "mysql -uroot -p'$MYSQL_PW' $DB -e \"UPDATE sys_user SET password='$HASH', salt='APDzGLuO' WHERE phone='13507007984';\" 2>/dev/null" 2>&1

echo "=== verify update ==="
docker exec "$MYSQL_C" sh -c "mysql -uroot -p'$MYSQL_PW' $DB -e \"SELECT id,username,phone,LEFT(password,20) as pw_prefix,salt FROM sys_user WHERE phone='13507007984';\" 2>/dev/null" 2>&1
