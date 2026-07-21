#!/bin/bash
set -e

BASE="http://localhost:8080/jeecg-boot"
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6IjEzNTA3MDA3OTg0IiwiY2xpZW50VHlwZSI6IkFQUCIsImV4cCI6MTc4NTg1Njc2Nn0.q4M7zJqgJnWcdyzIz59vAsJzYaVmITdl1lMyfoH-xJQ"

Q='{\"status\":\"VALID\",\"source\":\"REAL_DEVICE\",\"reason\":\"ring device\"}'
Q_USER='{\"status\":\"VALID\",\"source\":\"USER_REPORTED\",\"reason\":\"user self-report\"}'

echo "=== /rehealth/mobile/features/evaluate with correct feature_quality ==="
curl -s --max-time 30 -X POST "$BASE/rehealth/mobile/features/evaluate" \
  -H "Content-Type: application/json" \
  -H "X-Access-Token: $TOKEN" \
  -d "{
    \"featureVector\":{
      \"age\":52,\"gender\":1,\"bmi\":27.5,\"sbp\":145,\"dbp\":90,
      \"fasting_glucose\":6.2,\"total_cholesterol\":5.8,\"ldl\":3.9,\"hdl\":1.1,
      \"triglycerides\":2.1,\"exercise_days\":2,\"smoking\":1,\"drinking\":0,
      \"diabetes_history\":0,\"hypertension_history\":1,\"family_history\":1,
      \"featureQuality\":{
        \"age\":$Q,
        \"gender\":$Q,
        \"bmi\":$Q,
        \"sbp\":$Q,
        \"dbp\":$Q,
        \"fasting_glucose\":$Q,
        \"total_cholesterol\":$Q,
        \"ldl\":$Q,
        \"hdl\":$Q,
        \"triglycerides\":$Q,
        \"exercise_days\":$Q,
        \"smoking\":$Q_USER,
        \"drinking\":$Q_USER,
        \"diabetes_history\":$Q_USER,
        \"hypertension_history\":$Q_USER,
        \"family_history\":$Q_USER
      }
    }
  }" | python3 -m json.tool 2>/dev/null
