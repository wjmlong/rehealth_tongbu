#!/bin/bash
set -e

BASE="http://localhost:8080/jeecg-boot"
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6IjEzNTA3MDA3OTg0IiwiY2xpZW50VHlwZSI6IkFQUCIsImV4cCI6MTc4NTg1Njc2Nn0.q4M7zJqgJnWcdyzIz59vAsJzYaVmITdl1lMyfoH-xJQ"

echo "=== 1. /rehealth/mobile/features/evaluate with feature_quality ==="
curl -s --max-time 30 -X POST "$BASE/rehealth/mobile/features/evaluate" \
  -H "Content-Type: application/json" \
  -H "X-Access-Token: $TOKEN" \
  -d '{
    "featureVector":{
      "age":52,"gender":1,"bmi":27.5,"sbp":145,"dbp":90,
      "fasting_glucose":6.2,"total_cholesterol":5.8,"ldl":3.9,"hdl":1.1,
      "triglycerides":2.1,"exercise_days":2,"smoking":1,"drinking":0,
      "diabetes_history":0,"hypertension_history":1,"family_history":1,
      "featureQuality":{
        "age":{"status":"REAL_DEVICE","source":"ring","observedAt":1784600000},
        "gender":{"status":"REAL_DEVICE","source":"ring","observedAt":1784600000},
        "bmi":{"status":"REAL_DEVICE","source":"ring","observedAt":1784600000},
        "sbp":{"status":"REAL_DEVICE","source":"ring","observedAt":1784600000},
        "dbp":{"status":"REAL_DEVICE","source":"ring","observedAt":1784600000},
        "fasting_glucose":{"status":"REAL_DEVICE","source":"ring","observedAt":1784600000},
        "total_cholesterol":{"status":"REAL_DEVICE","source":"ring","observedAt":1784600000},
        "ldl":{"status":"REAL_DEVICE","source":"ring","observedAt":1784600000},
        "hdl":{"status":"REAL_DEVICE","source":"ring","observedAt":1784600000},
        "triglycerides":{"status":"REAL_DEVICE","source":"ring","observedAt":1784600000},
        "exercise_days":{"status":"REAL_DEVICE","source":"ring","observedAt":1784600000},
        "smoking":{"status":"USER_REPORTED","source":"user","observedAt":1784600000},
        "drinking":{"status":"USER_REPORTED","source":"user","observedAt":1784600000},
        "diabetes_history":{"status":"USER_REPORTED","source":"user","observedAt":1784600000},
        "hypertension_history":{"status":"USER_REPORTED","source":"user","observedAt":1784600000},
        "family_history":{"status":"USER_REPORTED","source":"user","observedAt":1784600000}
      }
    }
  }' | python3 -m json.tool 2>/dev/null

echo ""
echo "=== 2. Check model-service routes ==="
curl -s --max-time 5 "http://localhost:8000/openapi.json" 2>&1 | python3 -c "
import sys, json
data = json.load(sys.stdin)
paths = data.get('paths', {})
for path in sorted(paths.keys()):
    methods = list(paths[path].keys())
    print(f'{path}: {methods}')
" 2>/dev/null || echo "No openapi.json, trying docs..."
curl -s --max-time 5 "http://localhost:8000/docs" 2>&1 | head -5
