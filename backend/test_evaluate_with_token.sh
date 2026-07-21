#!/usr/bin/env bash
set -u
BASE="http://localhost:8080/jeecg-boot"
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6IjEzNTA3MDA3OTg0IiwiY2xpZW50VHlwZSI6IkFQUCIsImV4cCI6MTc4NTg1Njc2Nn0.q4M7zJqgJnWcdyzIz59vAsJzYaVmITdl1lMyfoH-xJQ"
BODY='{"featureVector":{"age":52,"gender":1,"bmi":27.5,"sbp":145,"dbp":90,"fasting_glucose":6.2,"total_cholesterol":5.8,"ldl":3.9,"hdl":1.1,"triglycerides":2.1,"exercise_days":2,"smoking":1,"drinking":0,"diabetes_history":0,"hypertension_history":1,"family_history":1,"featureQuality":{}}}'

echo "=== call /rehealth/mobile/features/evaluate with EXISTING token ==="
curl -s -w "\nHTTP=%{http_code}" --max-time 15 -X POST "$BASE/rehealth/mobile/features/evaluate" \
  -H "Content-Type: application/json" \
  -H "X-Access-Token: $TOKEN" \
  -d "$BODY" 2>&1 | head -c 2000

echo ""
echo ""
echo "=== call /rehealth/mobile/risk/latest ==="
curl -s -w "\nHTTP=%{http_code}" --max-time 10 "$BASE/rehealth/mobile/risk/latest" \
  -H "X-Access-Token: $TOKEN" 2>&1 | head -c 1000

echo ""
echo ""
echo "=== call /rehealth/mobile/interventions/today ==="
curl -s -w "\nHTTP=%{http_code}" --max-time 10 "$BASE/rehealth/mobile/interventions/today" \
  -H "X-Access-Token: $TOKEN" 2>&1 | head -c 1000
