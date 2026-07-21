#!/bin/bash
set -e

BASE="http://localhost:8080/jeecg-boot"
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6IjEzNTA3MDA3OTg0IiwiY2xpZW50VHlwZSI6IkFQUCIsImV4cCI6MTc4NTg1Njc2Nn0.q4M7zJqgJnWcdyzIz59vAsJzYaVmITdl1lMyfoH-xJQ"

echo "=== 1. /rehealth/mobile/features/evaluate ==="
curl -s --max-time 30 -X POST "$BASE/rehealth/mobile/features/evaluate" \
  -H "Content-Type: application/json" \
  -H "X-Access-Token: $TOKEN" \
  -d '{"featureVector":{"age":52,"gender":1,"bmi":27.5,"sbp":145,"dbp":90,"fasting_glucose":6.2,"total_cholesterol":5.8,"ldl":3.9,"hdl":1.1,"triglycerides":2.1,"exercise_days":2,"smoking":1,"drinking":0,"diabetes_history":0,"hypertension_history":1,"family_history":1,"featureQuality":{}}}' | python3 -m json.tool 2>/dev/null

echo ""
echo "=== 2. /rehealth/mobile/risk/latest ==="
curl -s --max-time 10 "$BASE/rehealth/mobile/risk/latest" \
  -H "X-Access-Token: $TOKEN" | python3 -m json.tool 2>/dev/null

echo ""
echo "=== 3. /rehealth/mobile/interventions/today ==="
curl -s --max-time 10 "$BASE/rehealth/mobile/interventions/today" \
  -H "X-Access-Token: $TOKEN" | python3 -m json.tool 2>/dev/null

echo ""
echo "=== 4. Direct model-service PIAS predict ==="
curl -s --max-time 15 -X POST "http://localhost:8000/api/pias/v2/predict" \
  -H "Content-Type: application/json" \
  -d '{"age":52,"gender":1,"bmi":27.5,"sbp":145,"dbp":90,"fasting_glucose":6.2,"total_cholesterol":5.8,"ldl":3.9,"hdl":1.1,"triglycerides":2.1,"exercise_days":2,"smoking":1,"drinking":0,"diabetes_history":0,"hypertension_history":1,"family_history":1}' | python3 -m json.tool 2>/dev/null
