#!/bin/bash
set -e

REDIS_PASS="ae3860fbb510eedc9d8bf43e49b60c2f21e5cb74c1919172"
MYSQL_PASS="dfe4d17b82b7e7756fc1409b49019359454099b34572f59b"
BASE="http://localhost:8080/jeecg-boot"

echo "=== 1. Check if old token is in Redis ==="
docker exec rehealth-staging-redis-1 redis-cli -a "$REDIS_PASS" KEYS "prefix_user_token:eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6IjEzNTA3MDA3OTg0IiwiY2xpZW50VHlwZSI6IkFQUCIsImV4cCI6MTc4NTg1Njc2Nn0*" 2>/dev/null

echo ""
echo "=== 2. Find ALL tokens for user13507007984 ==="
docker exec rehealth-staging-redis-1 redis-cli -a "$REDIS_PASS" KEYS "prefix_user_token:*" 2>/dev/null | head -20

echo ""
echo "=== 3. Restore original password hash ==="
docker exec rehealth-staging-mysql-1 mysql -uroot -p"$MYSQL_PASS" jeecg-boot \
  -e "UPDATE sys_user SET password='b0ab66ed07063343d9764abc148811c2' WHERE phone='13507007984';" 2>/dev/null
echo "Restored original hash."

echo ""
echo "=== 4. Try old token ==="
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6IjEzNTA3MDA3OTg0IiwiY2xpZW50VHlwZSI6IkFQUCIsImV4cCI6MTc4NTg1Njc2Nn0.q4M7zJqgJnWcdyzIz59vAsJzYaVmITdl1lMyfoH-xJQ"
curl -s --max-time 10 "$BASE/rehealth/mobile/health" -H "X-Access-Token: $TOKEN" 2>&1

echo ""
echo ""
echo "=== 5. Compute PBEWithMD5AndDES with 1000 iterations ==="
# The JDK PBEWithMD5AndDESCipher uses PBKDF1-like with iteration count
# dk1 = MD5(password + salt), dk2 = MD5(dk1), ..., dk1000 = MD5(dk999)
# Take first 16 bytes of dk1 for key+iv (since len(MD5)=16 >= 16 needed)
# BUT: Java might do something different. Let me try with the PyJWT approach to generate a token directly.
docker exec rehealth-staging-model-service-1 python3 -c "
from Crypto.Cipher import DES
from Crypto.Util.Padding import pad
import hashlib

def md5(data):
    return hashlib.md5(data).digest()

# Java PBKDF1 with iterations
pwd = b'13507007984'
salt = b'APDzGLuO'
plaintext = b'123456'

# Method 1: Just first MD5 (no iterations on key derivation)
dk = md5(pwd + salt)
key1, iv1 = dk[:8], dk[8:16]
cipher = DES.new(key1, DES.MODE_CBC, iv1)
enc1 = cipher.encrypt(pad(plaintext, 8, style='pkcs7'))
print('HASH1=' + enc1.hex())

# Method 2: 1000 iterations of MD5 on the dk
dk = md5(pwd + salt)
for i in range(999):
    dk = md5(dk)
key2, iv2 = dk[:8], dk[8:16]
cipher = DES.new(key2, DES.MODE_CBC, iv2)
enc2 = cipher.encrypt(pad(plaintext, 8, style='pkcs7'))
print('HASH2=' + enc2.hex())

# Method 3: PKCS#5 v1.5 PBKDF1 - T1=MD5(P+S), T2=MD5(T1), concatenate, take first 16
T1 = md5(pwd + salt)
T2 = md5(T1)
dk3 = (T1 + T2)[:16]
key3, iv3 = dk3[:8], dk3[8:16]
cipher = DES.new(key3, DES.MODE_CBC, iv3)
enc3 = cipher.encrypt(pad(plaintext, 8, style='pkcs7'))
print('HASH3=' + enc3.hex())

# Method 4: MD5(MD5(pwd+salt) for 1000 times, accumulate
dk_accum = b''
dk = md5(pwd + salt)
for i in range(1000):
    dk_accum += dk
    dk = md5(dk)
dk4 = dk_accum[:16]
key4, iv4 = dk4[:8], dk4[8:16]
cipher = DES.new(key4, DES.MODE_CBC, iv4)
enc4 = cipher.encrypt(pad(plaintext, 8, style='pkcs7'))
print('HASH4=' + enc4.hex())
" 2>&1
