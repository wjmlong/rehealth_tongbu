#!/bin/bash
set -e

TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6IjEzNTA3MDA3OTg0IiwiY2xpZW50VHlwZSI6IkFQUCIsImV4cCI6MTc4NTg1Njc2Nn0.q4M7zJqgJnWcdyzIz59vAsJzYaVmITdl1lMyfoH-xJQ"
BASE="http://localhost:8080/jeecg-boot"

# First verify token works
echo "=== Verify token ==="
curl -s --max-time 10 "$BASE/rehealth/mobile/health" -H "X-Access-Token: $TOKEN" 2>&1 | head -100

echo ""
echo "=== Evaluate with feature_quality ==="
curl -s --max-time 30 -X POST "$BASE/rehealth/mobile/features/evaluate" \
  -H "Content-Type: application/json" \
  -H "X-Access-Token: $TOKEN" \
  -d @/mnt/d/rehealthAI/backend/evaluate_body.json 2>&1 | python3 -m json.tool 2>/dev/null

echo ""
echo "=== Direct model-service with feature_quality ==="
curl -s --max-time 30 -X POST "http://localhost:8000/v1/cvd/risk/evaluate" \
  -H "Content-Type: application/json" \
  -d @/mnt/d/rehealthAI/backend/evaluate_body.json 2>&1 | python3 -m json.tool 2>/dev/null
