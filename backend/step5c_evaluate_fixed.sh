#!/bin/bash
set -e

BASE="http://localhost:8080/jeecg-boot"
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6IjEzNTA3MDA3OTg0IiwiY2xpZW50VHlwZSI6IkFQUCIsImV4cCI6MTc4NTg1Njc2Nn0.q4M7zJqgJnWcdyzIz59vAsJzYaVmITdl1lMyfoH-xJQ"

echo "=== 1. /rehealth/mobile/features/evaluate with VALID status ==="
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
        "age":{"status":"VALID","source":"ring"},
        "gender":{"status":"VALID","source":"ring"},
        "bmi":{"status":"VALID","source":"ring"},
        "sbp":{"status":"VALID","source":"ring"},
        "dbp":{"status":"VALID","source":"ring"},
        "fasting_glucose":{"status":"VALID","source":"ring"},
        "total_cholesterol":{"status":"VALID","source":"ring"},
        "ldl":{"status":"VALID","source":"ring"},
        "hdl":{"status":"VALID","source":"ring"},
        "triglycerides":{"status":"VALID","source":"ring"},
        "exercise_days":{"status":"VALID","source":"ring"},
        "smoking":{"status":"VALID","source":"user"},
        "drinking":{"status":"VALID","source":"user"},
        "diabetes_history":{"status":"VALID","source":"user"},
        "hypertension_history":{"status":"VALID","source":"user"},
        "family_history":{"status":"VALID","source":"user"}
      }
    }
  }' 2>&1

echo ""
echo ""
echo "=== 2. Also try direct model-service /v1/cvd/risk/evaluate ==="
curl -s --max-time 15 -X POST "http://localhost:8000/v1/cvd/risk/evaluate" \
  -H "Content-Type: application/json" \
  -d '{
    "feature_vector":{
      "age":52,"gender":1,"bmi":27.5,"sbp":145,"dbp":90,
      "fasting_glucose":6.2,"total_cholesterol":5.8,"ldl":3.9,"hdl":1.1,
      "triglycerides":2.1,"exercise_days":2,"smoking":1,"drinking":0,
      "diabetes_history":0,"hypertension_history":1,"family_history":1
    }
  }' 2>&1 | python3 -m json.tool 2>/dev/null

echo ""
echo "=== 3. Try direct model-service with flat fields ==="
curl -s --max-time 15 -X POST "http://localhost:8000/v1/cvd/risk/evaluate" \
  -H "Content-Type: application/json" \
  -d '{
    "age":52,"gender":1,"bmi":27.5,"sbp":145,"dbp":90,
    "fasting_glucose":6.2,"total_cholesterol":5.8,"ldl":3.9,"hdl":1.1,
    "triglycerides":2.1,"exercise_days":2,"smoking":1,"drinking":0,
    "diabetes_history":0,"hypertension_history":1,"family_history":1
  }' 2>&1 | python3 -m json.tool 2>/dev/null
