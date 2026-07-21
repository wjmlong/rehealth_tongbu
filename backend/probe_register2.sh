#!/usr/bin/env bash
set -u
BASE="http://localhost:8080/jeecg-boot"
echo "=== /sys/user/register body (200 earlier with {}) ==="
curl -s -w "\nhttp=%{http_code}\n" --max-time 8 -X POST "$BASE/sys/user/register" \
  -H "Content-Type: application/json" \
  -d '{"mobile":"13800000000","password":"test123456","username":"testuser","captcha":"1234"}' 2>&1
echo "=== /sys/user/register minimal ==="
curl -s -w "\nhttp=%{http_code}\n" --max-time 8 -X POST "$BASE/sys/user/register" \
  -H "Content-Type: application/json" \
  -d '{"username":"testuser","password":"test123456","mobile":"13800000000"}' 2>&1
echo "=== probe captcha image endpoint to understand mLogin captcha ==="
curl -s -w "\nhttp=%{http_code}\n" --max-time 8 "$BASE/sys/randomImage/$(date +%s)" 2>&1 | head -c 400
echo ""
echo "=== try mLogin after register attempt (same mobile) ==="
curl -s -w "\nhttp=%{http_code}\n" --max-time 8 -X POST "$BASE/sys/mLogin" \
  -H "Content-Type: application/json" \
  -d '{"mobile":"13800000000","captcha":"1234"}' 2>&1
