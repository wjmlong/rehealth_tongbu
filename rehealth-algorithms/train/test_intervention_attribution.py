# -*- coding: utf-8 -*-
"""
Test intervention attribution engine with a realistic scenario:
  T0: high-risk patient (elevated BP, glucose, lipids, overweight)
  T1: same patient after 3 months of following advice (metrics improved)

Usage:
    python train/test_intervention_attribution.py
"""

import json
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(PROJECT_ROOT))

from bodyup_cloud.engine.intervention_attributor import InterventionAttributor

import joblib

# T0: Baseline — high-risk patient before intervention
T0_PATIENT = {
    "age": 62,
    "gender": 1,
    "bmi": 30.0,
    "sbp": 160,
    "dbp": 100,
    "fasting_glucose": 8.5,
    "total_cholesterol": 6.8,
    "ldl": 4.5,
    "hdl": 0.9,
    "triglycerides": 3.0,
    "exercise_days": 0,
    "smoking": 1,
    "drinking": 1,
    "diabetes_history": 1,
    "hypertension_history": 1,
    "family_history": 1,
}

# T1: 3 months later — patient followed diet/exercise/medication advice
# Continuous metrics improved, lifestyle flags unchanged for this demo
T1_PATIENT = {
    "age": 62,
    "gender": 1,
    "bmi": 27.0,         # weight loss (30 → 27)
    "sbp": 135,          # BP improved (160 → 135)
    "dbp": 85,           # BP improved (100 → 85)
    "fasting_glucose": 6.5,  # glucose controlled (8.5 → 6.5)
    "total_cholesterol": 5.5,  # cholesterol down (6.8 → 5.5)
    "ldl": 3.2,          # LDL down (4.5 → 3.2)
    "hdl": 1.2,          # HDL up (0.9 → 1.2)
    "triglycerides": 1.8,  # TG down (3.0 → 1.8)
    "exercise_days": 0,  # unchanged (model has confounding here)
    "smoking": 1,        # unchanged
    "drinking": 1,       # unchanged
    "diabetes_history": 1,
    "hypertension_history": 1,
    "family_history": 1,
}


def main():
    print("=" * 60)
    print("Intervention Attribution Engine Test")
    print("=" * 60)

    model_path = PROJECT_ROOT / "train" / "rehealth_v3_unified.pkl"
    if not model_path.exists():
        model_path = PROJECT_ROOT / "train" / "rehealth_v2_final.pkl"
    if not model_path.exists():
        print("ERROR: No model found!")
        sys.exit(1)

    print(f"Model: {model_path.name}")
    model = joblib.load(str(model_path))

    attributor = InterventionAttributor(model)

    print("\n[T0] Baseline patient (before intervention)")
    print(f"  BMI={T0_PATIENT['bmi']}, SBP={T0_PATIENT['sbp']}/{T0_PATIENT['dbp']}, "
          f"Glucose={T0_PATIENT['fasting_glucose']}, TC={T0_PATIENT['total_cholesterol']}")

    print("\n[T1] Follow-up patient (3 months after intervention)")
    print(f"  BMI={T1_PATIENT['bmi']}, SBP={T1_PATIENT['sbp']}/{T1_PATIENT['dbp']}, "
          f"Glucose={T1_PATIENT['fasting_glucose']}, TC={T1_PATIENT['total_cholesterol']}")

    print("\n[Attribution] Counterfactual decomposition...")
    report = attributor.attribute(T0_PATIENT, T1_PATIENT)
    result = report.to_dict()

    print(f"\n  Risk T0 (baseline):      {result['risk_before']:.4f} ({result['risk_level_before']})")
    print(f"  Risk T1 (follow-up):     {result['risk_after']:.4f} ({result['risk_level_after']})")
    print(f"  Risk counterfactual:     {result['counterfactual_risk']:.4f}")
    print(f"    (= T1 if modifiable features stayed at T0 values)")
    print(f"  Risk change (T1-T0):     {result['risk_change']:+.4f}")
    print(f"\n  Intervention effect:     {result['intervention_effect']:+.4f}")
    print(f"    (= risk AVOIDED by following advice)")
    print(f"  Natural change:          {result['natural_change']:+.4f}")
    print(f"    (= change from non-modifiable factors)")
    print(f"  Attribution ratio:       {result['attribution_ratio']:.1%}")

    print(f"\n  Summary: {result['summary']}")

    print("\n  Per-feature marginal effects:")
    print(f"  {'Feature':25s} {'T0→T1':>14s} {'Risk avoided':>12s} {'Type':>10s} {'Direction':>10s}")
    print("  " + "-" * 75)
    for f in result["feature_details"]:
        if not f["value_changed"]:
            val_str = f"{f['t0_value']:.1f} (same)"
        else:
            val_str = f"{f['t0_value']:.1f}→{f['t1_value']:.1f}"
        mod_str = "modifiable" if f["is_modifiable"] else "fixed"
        print(f"  {f['feature']:25s} {val_str:>14s} {f['marginal_effect']:>+12.4f} {mod_str:>10s} {f['direction']:>10s}")

    # Settlement-ready output
    print("\n" + "=" * 60)
    print("Settlement Report (for insurance)")
    print("=" * 60)
    settlement = {
        "patient_id": "demo-001",
        "period": "2026-03 to 2026-06",
        "risk_before": result["risk_before"],
        "risk_after": result["risk_after"],
        "total_risk_reduction": abs(result["risk_change"]) if result["risk_change"] < 0 else 0,
        "counterfactual_risk": result["counterfactual_risk"],
        "intervention_value": max(0, result["intervention_effect"]),
        "attribution_ratio": result["attribution_ratio"],
        "improvements": [
            {"feature": f["feature"],
             "from": f["t0_value"], "to": f["t1_value"],
             "risk_avoided": f["marginal_effect"]}
            for f in result["feature_details"]
            if f["is_modifiable"] and f["direction"] == "improved"
        ],
        "summary": result["summary"],
    }
    print(json.dumps(settlement, indent=2, ensure_ascii=False))

    print("\n" + "=" * 60)
    print("Attribution engine verified!")
    print("=" * 60)


if __name__ == "__main__":
    main()
