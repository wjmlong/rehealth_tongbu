"""
PIAS end-to-end walkthrough client for ReHealth model-service.

Mimics what the Android app would send after HealthFeatureExtractor derives the
16-dim CvdFeatureVector from:
  - user profile  -> age, gender, bmi, smoking, drinking, *history, family_history
  - REAL_DEVICE ring (blood pressure) -> sbp, dbp
  - DERIVED from 7-day step/activity history -> exercise_days
  - clinical/lab report -> fasting_glucose, total_cholesterol, ldl, hdl, triglycerides

Then walks the real PIAS surface:
  P  POST /v1/cvd/risk/evaluate          (real CatBoost model)
  I  POST /v1/cvd/intervention/generate  (ConservativePrescriptionGenerator)
  A  POST /v1/cvd/attribution/individual (trend over an event series)
Plus an in-process MockRiskScorer call to show the rule-based per-feature
contributions the attribution screen renders (the real model path currently
returns deterministic_zero_fallback for contributions).
"""
from __future__ import annotations

import json
import sys
from pathlib import Path

# Make the repo importable so we can reuse the real schemas + scorers.
ROOT = Path(__file__).resolve().parent.parent / "model-service"
sys.path.insert(0, str(ROOT))

import httpx  # noqa: E402
from app.risk_scorer import MockRiskScorer  # noqa: E402
from app.schemas import CvdFeatureVector, RiskEvaluateResponse  # noqa: E402

BASE = "http://127.0.0.1:8000"

# ---- 1. Build the 16-dim feature vector exactly as the app would -----------------
# A representative 54yo male, overweight, elevated ring BP, borderline labs,
# moderate exercise, one history flag + family history. Sources mirror the app.
F = {
    "age": 54,
    "gender": 1,                       # 1 = male
    "bmi": 27.3,
    "sbp": 142.0,                      # REAL_DEVICE ring average
    "dbp": 91.0,                       # REAL_DEVICE ring average
    "fasting_glucose": 6.4,            # CLINICAL_REPORT (impaired fasting glucose)
    "total_cholesterol": 5.8,          # CLINICAL_REPORT
    "ldl": 3.6,
    "hdl": 1.05,
    "triglycerides": 1.9,
    "exercise_days": 3,                # DERIVED from 7-day steps/activity
    "smoking": 1,                      # USER_REPORTED
    "drinking": 0,
    "diabetes_history": 0,
    "hypertension_history": 1,
    "family_history": 1,
}

# featureQuality must cover ALL 16 fields.
QUALITY = {
    "age": ("VALID", "USER_REPORTED", "profile"),
    "gender": ("VALID", "USER_REPORTED", "profile"),
    "bmi": ("VALID", "USER_REPORTED", "profile"),
    "sbp": ("VALID", "REAL_DEVICE", "ring cuffless estimate"),
    "dbp": ("VALID", "REAL_DEVICE", "ring cuffless estimate"),
    "fasting_glucose": ("VALID", "CLINICAL_REPORT", "lab panel"),
    "total_cholesterol": ("VALID", "CLINICAL_REPORT", "lab panel"),
    "ldl": ("VALID", "CLINICAL_REPORT", "lab panel"),
    "hdl": ("VALID", "CLINICAL_REPORT", "lab panel"),
    "triglycerides": ("VALID", "CLINICAL_REPORT", "lab panel"),
    "exercise_days": ("VALID", "DERIVED", "7-day step/activity aggregation"),
    "smoking": ("VALID", "USER_REPORTED", "intake"),
    "drinking": ("VALID", "USER_REPORTED", "intake"),
    "diabetes_history": ("VALID", "USER_REPORTED", "intake"),
    "hypertension_history": ("VALID", "USER_REPORTED", "intake"),
    "family_history": ("VALID", "USER_REPORTED", "intake"),
}
feature_quality = {
    k: {"status": s, "source": src, "reason": r} for k, (s, src, r) in QUALITY.items()
}

vector = CvdFeatureVector(**F, feature_quality=feature_quality)
print("=== Built CvdFeatureVector (16 dims) ===")
print(vector.model_dump())


def section(title: str) -> None:
    print("\n" + "=" * 72)
    print(title)
    print("=" * 72)


# ---- 2. P — Predict via the real CatBoost model ----------------------------------
section("P (Predict)  POST /v1/cvd/risk/evaluate  [real CatBoost model]")
req_body = {
    "featureVector": vector.model_dump(by_alias=True, mode="json"),
    "requestId": "demo-ring-0001",
}
with httpx.Client(base_url=BASE, timeout=10.0) as client:
    r = client.post("/v1/cvd/risk/evaluate", json=req_body)
    print("HTTP", r.status_code)
    risk_json = r.json()
    print(json.dumps(risk_json, indent=2, ensure_ascii=False))

risk_response = RiskEvaluateResponse(**risk_json)

# ---- 3. I — Intervene -----------------------------------------------------------
section("I (Intervene)  POST /v1/cvd/intervention/generate")
# Send the risk result back (exactly what the backend forwards to the app).
interv_req = {
    "riskResult": risk_json,
    "featureVector": vector.model_dump(by_alias=True, mode="json"),
    "patientContext": {"demo": True},
}
with httpx.Client(base_url=BASE, timeout=10.0) as client:
    r = client.post("/v1/cvd/intervention/generate", json=interv_req)
    print("HTTP", r.status_code)
    print(json.dumps(r.json(), indent=2, ensure_ascii=False))

# ---- 4. A — Individual attribution (trend over an event series) -----------------
section("A (Attribute)  POST /v1/cvd/attribution/individual")
events = [
    {"date": "2026-07-14", "risk_score": 0.42, "adherence": 0.6},
    {"date": "2026-07-16", "risk_score": 0.38, "adherence": 0.7},
    {"date": "2026-07-18", "risk_score": 0.35, "adherence": 0.8},
]
attr_req = {"events": events, "baselineRiskScore": 0.42}
with httpx.Client(base_url=BASE, timeout=10.0) as client:
    r = client.post("/v1/cvd/attribution/individual", json=attr_req)
    print("HTTP", r.status_code)
    print(json.dumps(r.json(), indent=2, ensure_ascii=False))

# ---- 5. Contribution contrast: in-process MockRiskScorer -----------------------
section("Contrast: rule-based per-feature contributions (MockRiskScorer)")
print("The REAL model path returns contribution_method='deterministic_zero_fallback'")
print("(all feature_contributions = 0.0). The attribution screen instead renders the")
print("deterministic rule-based breakdown below, which is what the mock fallback gives:")
mock = MockRiskScorer(scorer_mode="mock")
mock_resp = mock.evaluate(vector)
contrib = mock_resp.feature_contributions
ordered = sorted(contrib.items(), key=lambda kv: abs(kv[1]), reverse=True)
for field, val in ordered:
    tag = "risk" if val > 0 else ("protective" if val < 0 else "neutral")
    print(f"  {field:18s} {val:+.4f}  {tag}")
print(f"  -> risk_score (mock) = {mock_resp.risk_score}  level = {mock_resp.risk_level}")

print("\nDONE. Values above are what the backend returns to the frontend.")
