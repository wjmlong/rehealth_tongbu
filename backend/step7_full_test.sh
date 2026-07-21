#!/bin/bash
set -e

REDIS_PASS="ae3860fbb510eedc9d8bf43e49b60c2f21e5cb74c1919172"
MYSQL_PASS="dfe4d17b82b7e7756fc1409b49019359454099b34572f59b"
BASE="http://localhost:8080/jeecg-boot"
TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJ1c2VybmFtZSI6IjEzNTA3MDA3OTg0IiwiY2xpZW50VHlwZSI6IkFQUCIsImV4cCI6MTc4NTg1Njc2Nn0.q4M7zJqgJnWcdyzIz59vAsJzYaVmITdl1lMyfoH-xJQ"

echo "=== 1. Verify MySQL hash ==="
docker exec rehealth-staging-mysql-1 mysql -uroot -p"$MYSQL_PASS" jeecg-boot \
  -e "SELECT id,username,phone,LEFT(password,32) as pw,salt FROM sys_user WHERE phone='13507007984';" 2>/dev/null

echo ""
echo "=== 2. Verify token in Redis ==="
docker exec rehealth-staging-redis-1 redis-cli -a "$REDIS_PASS" EXISTS "prefix_user_token:$TOKEN" 2>/dev/null

echo ""
echo "=== 3. Health check with token ==="
curl -s --max-time 10 "$BASE/rehealth/mobile/health" -H "X-Access-Token: $TOKEN" 2>&1

echo ""
echo ""
echo "=== 4. Evaluate with feature_quality (from file) ==="
curl -s --max-time 30 -X POST "$BASE/rehealth/mobile/features/evaluate" \
  -H "Content-Type: application/json" \
  -H "X-Access-Token: $TOKEN" \
  -d @/mnt/d/rehealthAI/backend/evaluate_body.json 2>&1

echo ""
echo ""
echo "=== 5. Direct model-service /v1/cvd/risk/evaluate ==="
curl -s --max-time 15 -X POST "http://localhost:8000/v1/cvd/risk/evaluate" \
  -H "Content-Type: application/json" \
  -d @/mnt/d/rehealthAI/backend/evaluate_body.json 2>&1
