#!/usr/bin/env bash
set -u
MYSQL_C="rehealth-staging-mysql-1"
MYSQL_PW="dfe4d17b82b7e7756fc1409b49019359454099b34572f59b"
DB="jeecg-boot"

echo "=== list tables in jeecg-boot ==="
docker exec "$MYSQL_C" sh -c "mysql -uroot -p'$MYSQL_PW' $DB -e 'SHOW TABLES;' 2>/dev/null" 2>&1 | grep -i "sys_user\|sys_role\|sys_permission" | head -10

echo "=== sys_user schema ==="
docker exec "$MYSQL_C" sh -c "mysql -uroot -p'$MYSQL_PW' $DB -e 'DESCRIBE sys_user;' 2>/dev/null" 2>&1 | head -30

echo "=== existing users ==="
docker exec "$MYSQL_C" sh -c "mysql -uroot -p'$MYSQL_PW' $DB -e \"SELECT id,username,phone,telephone,status,del_flag FROM sys_user LIMIT 10;\" 2>/dev/null" 2>&1

echo "=== try insert test user 13507007984 ==="
# JeecgBoot password is MD5+salt. Use a known hash for '123456' with Jeecg default salt.
# Jeecg default: password=MD5(password+salt), salt='jeecg-boot'
# For simplicity, try a direct insert with a known JeecgBoot compatible password.
# Jeecg uses: md5(md5(password) + salt)
# For password "123456", salt "jeecg-boot": md5(md5("123456")+"jeecg-boot")
# md5("123456") = e10adc3949ba59abbe56e057f20f883e
# md5("e10adc3949ba59abbe56e057f20f883ejeecg-boot") = we need to compute this

# Actually let's just check if we can use the register endpoint instead
echo "=== check sys_user columns for password ==="
docker exec "$MYSQL_C" sh -c "mysql -uroot -p'$MYSQL_PW' $DB -e \"SELECT COLUMN_NAME,DATA_TYPE,COLUMN_DEFAULT FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA='$DB' AND TABLE_NAME='sys_user';\" 2>/dev/null" 2>&1 | grep -iE "password|salt|phone|username|status|del"
