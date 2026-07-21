#!/usr/bin/env bash
set -u
BASE="http://localhost:8080/jeecg-boot"
BODY='{"featureVector":{"age":52,"gender":1,"bmi":27.5,"sbp":145,"dbp":90,"fasting_glucose":6.2,"total_cholesterol":5.8,"ldl":3.9,"hdl":1.1,"triglycerides":2.1,"exercise_days":2,"smoking":1,"drinking":0,"diabetes_history":0,"hypertension_history":1,"family_history":1,"featureQuality":{}}}'

echo "=== POST /rehealth/mobile/features/evaluate (16 snake_case fields, NO token) ==="
curl -s -w "\nHTTP_CODE=%{http_code}\n" --max-time 15 -X POST "$BASE/rehealth/mobile/features/evaluate" \
  -H "Content-Type: application/json" \
  -d "$BODY" 2>&1

echo ""
echo "=== POST /rehealth/mobile/features/evaluate (with fake token to see if auth works) ==="
curl -s -w "\nHTTP_CODE=%{http_code}\n" --max-time 15 -X POST "$BASE/rehealth/mobile/features/evaluate" \
  -H "Content-Type: application/json" \
  -H "X-Access-Token: fake-token" \
  -d "$BODY" 2>&1
