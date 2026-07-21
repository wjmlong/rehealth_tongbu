#!/usr/bin/env bash
set -u
BASE="http://localhost:8080/jeecg-boot"
echo "=== 1) GET /jeecg-boot/ (expect 302/login redirect) ==="
curl -s -o /dev/null -w "http=%{http_code}\n" --max-time 8 "$BASE/" 2>&1
echo "=== 2) POST /sys/mLogin with mobile+captcha ==="
curl -s -w "\nhttp=%{http_code}\n" --max-time 8 -X POST "$BASE/sys/mLogin" \
  -H "Content-Type: application/json" \
  -d '{"mobile":"13800000000","captcha":"1234"}' 2>&1
echo "=== 3) POST /sys/login with username+password (standard jeecg) ==="
curl -s -w "\nhttp=%{http_code}\n" --max-time 8 -X POST "$BASE/sys/login" \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}' 2>&1
echo "=== 4) GET /sys/randomImage/{key} (captcha endpoint probe) ==="
curl -s -o /dev/null -w "http=%{http_code}\n" --max-time 8 "$BASE/sys/randomImage/key123" 2>&1
