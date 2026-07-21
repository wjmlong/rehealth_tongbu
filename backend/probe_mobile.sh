#!/usr/bin/env bash
set -u
BASE="http://localhost:8080/jeecg-boot"
echo "=== GET /rehealth/mobile/health (no token) ==="
curl -s -w "\nhttp=%{http_code}\n" --max-time 8 "$BASE/rehealth/mobile/health" 2>&1 | head -c 600
echo ""
echo "=== POST /rehealth/mobile/features/evaluate (no token, sample vector) ==="
curl -s -w "\nhttp=%{http_code}\n" --max-time 10 -X POST "$BASE/rehealth/mobile/features/evaluate" \
  -H "Content-Type: application/json" \
  -d '{"featureVector":{"age":45,"gender":1,"bmi":24.5,"sbp":128,"dbp":82,"fasting_glucose":5.6,"featureQuality":{}},"requestId":"probe-1"}' 2>&1 | head -c 800
echo ""
echo "=== GET /rehealth/mobile/config (no token) ==="
curl -s -w "\nhttp=%{http_code}\n" --max-time 8 "$BASE/rehealth/mobile/config" 2>&1 | head -c 600
echo ""
