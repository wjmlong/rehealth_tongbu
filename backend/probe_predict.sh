#!/usr/bin/env bash
set -u
BASE="http://localhost:8000/api/pias/v2"
echo "=== POST /api/pias/v2/predict (16 features from ring-sim extractable dims) ==="
curl -s -w "\nhttp=%{http_code}\n" --max-time 15 -X POST "$BASE/predict" \
  -H "Content-Type: application/json" \
  -d '{
    "age": 52, "gender": 1, "bmi": 27.5, "sbp": 145, "dbp": 90,
    "fasting_glucose": 6.2, "total_cholesterol": 5.8, "ldl": 3.9, "hdl": 1.1,
    "triglycerides": 2.1, "exercise_days": 2, "smoking": 1, "drinking": 0,
    "diabetes_history": 0, "hypertension_history": 1, "family_history": 1
  }' 2>&1 | head -c 1500
echo ""
echo "=== POST /api/pias/v2/attribute/individual (needs risk_history) ==="
curl -s -w "\nhttp=%{http_code}\n" --max-time 15 -X POST "$BASE/attribute/individual" \
  -H "Content-Type: application/json" \
  -d '{"risk_history":[{"date":"2026-05-01","Y":0.52,"Z":1},{"date":"2026-05-02","Y":0.50,"Z":1},{"date":"2026-05-03","Y":0.48,"Z":0}],"forecast_days":30,"language":"zh"}' 2>&1 | head -c 400
