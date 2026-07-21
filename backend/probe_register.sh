#!/usr/bin/env bash
set -u
BASE="http://localhost:8080/jeecg-boot"
echo "=== register endpoint probe (common jeecg paths) ==="
for p in "/sys/user/register" "/sys/register" "/rehealth/mobile/register" "/rehealth/mobile/user/register"; do
  code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 6 -X POST "$BASE$p" -H "Content-Type: application/json" -d '{}' 2>/dev/null || echo 000)
  echo "$p -> $code"
done
echo "=== check if admin login works via /sys/login with captcha disabled? try /sys/login with checkKey ==="
curl -s -w "\nhttp=%{http_code}\n" --max-time 8 -X POST "$BASE/sys/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123456","remember_me":true,"captcha":"","checkKey":""}' 2>&1
echo "=== mobile register via mLogin-like? try /rehealth/mobile/user/register with mobile ==="
curl -s -w "\nhttp=%{http_code}\n" --max-time 8 -X POST "$BASE/rehealth/mobile/user/register" \
  -H "Content-Type: application/json" \
  -d '{"mobile":"13800000000","password":"test123456","captcha":"1234"}' 2>&1
