# -*- coding: utf-8 -*-
"""
群体归因端到端验证 — 模拟保险投保人群

模拟场景:
  - 200 名投保人，随机分为使用产品组(treated)和对照组(control)
  - T0: 基线体检数据（从真实分布采样）
  - T1: 3个月后随访（treated 组指标改善，control 组自然变化）
  - 用 CatBoost 模型计算每人的 T0→T1 风险变化(delta_Y)
  - 用 GroupAttributionEngine 做 PSM+DRE+Bootstrap → ATT + CI
  - 输出保险结算报告：群体风险降幅、保费节省估算

Usage:
    python train/test_group_attribution_e2e.py
"""

import json
import sys
from pathlib import Path

import numpy as np
import pandas as pd

PROJECT_ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(PROJECT_ROOT))

import joblib
from bodyup_cloud.engine.intervention_attributor import InterventionAttributor
from bodyup_cloud.engine.group_attribution import GroupAttributionEngine


def generate_cohort(model, n=200, seed=42):
    """Generate a simulated cohort with treated/control groups."""
    rng = np.random.default_rng(seed)
    attributor = InterventionAttributor(model)

    records = []
    detail_rows = []

    for i in range(n):
        is_treated = int(rng.random() > 0.5)

        # T0: sample from realistic distributions
        t0 = {
            "age": int(rng.integers(40, 75)),
            "gender": int(rng.choice([0, 1])),
            "bmi": round(float(rng.normal(27, 4)), 1),
            "sbp": round(float(rng.normal(140, 18)), 0),
            "dbp": round(float(rng.normal(88, 10)), 0),
            "fasting_glucose": round(float(rng.normal(6.5, 1.5)), 1),
            "total_cholesterol": round(float(rng.normal(5.5, 1.0)), 1),
            "ldl": round(float(rng.normal(3.5, 0.8)), 1),
            "hdl": round(float(rng.normal(1.2, 0.3)), 1),
            "triglycerides": round(float(rng.normal(2.0, 0.8)), 1),
            "exercise_days": int(rng.choice([0, 1, 2, 3])),
            "smoking": int(rng.choice([0, 1], p=[0.7, 0.3])),
            "drinking": int(rng.choice([0, 1], p=[0.6, 0.4])),
            "diabetes_history": int(rng.choice([0, 1], p=[0.8, 0.2])),
            "hypertension_history": int(rng.choice([0, 1], p=[0.6, 0.4])),
            "family_history": int(rng.choice([0, 1], p=[0.7, 0.3])),
        }

        # Clamp values to valid ranges
        t0["bmi"] = max(16, min(45, t0["bmi"]))
        t0["sbp"] = max(80, min(200, t0["sbp"]))
        t0["dbp"] = max(50, min(130, t0["dbp"]))
        t0["fasting_glucose"] = max(3.0, min(15.0, t0["fasting_glucose"]))
        t0["total_cholesterol"] = max(2.0, min(10.0, t0["total_cholesterol"]))
        t0["ldl"] = max(0.5, min(7.0, t0["ldl"]))
        t0["hdl"] = max(0.4, min(3.0, t0["hdl"]))
        t0["triglycerides"] = max(0.3, min(8.0, t0["triglycerides"]))

        # T1: treated group improves continuous metrics, control has natural drift
        t1 = dict(t0)
        if is_treated:
            t1["sbp"] = round(t0["sbp"] - rng.normal(12, 5), 0)
            t1["dbp"] = round(t0["dbp"] - rng.normal(7, 3), 0)
            t1["bmi"] = round(t0["bmi"] - rng.normal(1.5, 0.8), 1)
            t1["fasting_glucose"] = round(t0["fasting_glucose"] - rng.normal(0.8, 0.4), 1)
            t1["total_cholesterol"] = round(t0["total_cholesterol"] - rng.normal(0.6, 0.3), 1)
            t1["ldl"] = round(t0["ldl"] - rng.normal(0.5, 0.3), 1)
            t1["hdl"] = round(t0["hdl"] + rng.normal(0.15, 0.08), 1)
            t1["triglycerides"] = round(t0["triglycerides"] - rng.normal(0.4, 0.2), 1)
        else:
            t1["sbp"] = round(t0["sbp"] + rng.normal(2, 3), 0)
            t1["dbp"] = round(t0["dbp"] + rng.normal(1, 2), 0)
            t1["bmi"] = round(t0["bmi"] + rng.normal(0.3, 0.5), 1)
            t1["fasting_glucose"] = round(t0["fasting_glucose"] + rng.normal(0.1, 0.3), 1)

        # Clamp T1 values
        t1["bmi"] = max(16, min(45, t1["bmi"]))
        t1["sbp"] = max(80, min(200, t1["sbp"]))
        t1["dbp"] = max(50, min(130, t1["dbp"]))
        t1["fasting_glucose"] = max(3.0, min(15.0, t1["fasting_glucose"]))
        t1["total_cholesterol"] = max(2.0, min(10.0, t1["total_cholesterol"]))
        t1["ldl"] = max(0.5, min(7.0, t1["ldl"]))
        t1["hdl"] = max(0.4, min(3.0, t1["hdl"]))
        t1["triglycerides"] = max(0.3, min(8.0, t1["triglycerides"]))

        risk_t0 = attributor._score(t0)
        risk_t1 = attributor._score(t1)
        delta_y = risk_t1 - risk_t0  # negative = risk reduced

        # Encode features for PSM matching
        age_bracket = min(5, max(1, (t0["age"] - 30) // 10))
        bmi_level = 0 if t0["bmi"] < 18.5 else (1 if t0["bmi"] < 25 else (2 if t0["bmi"] < 30 else 3))
        bp_grade = 0 if t0["sbp"] < 120 else (1 if t0["sbp"] < 140 else 2)
        activity_level = min(3, t0["exercise_days"])

        month = rng.integers(1, 13)
        season_sin = float(np.sin(2 * np.pi * month / 12))
        season_cos = float(np.cos(2 * np.pi * month / 12))

        records.append({
            "device_id": f"user_{i:04d}",
            "Z": is_treated,
            "delta_Y": float(delta_y),
            "features": {
                "age_bracket": age_bracket,
                "bmi_level_encoded": bmi_level,
                "bp_baseline_grade_encoded": bp_grade,
                "activity_level_encoded": activity_level,
                "gender_encoded": t0["gender"],
                "season_sin": season_sin,
                "season_cos": season_cos,
            },
        })

        detail_rows.append({
            "user_id": f"user_{i:04d}",
            "group": "treated" if is_treated else "control",
            "risk_t0": risk_t0,
            "risk_t1": risk_t1,
            "delta_y": delta_y,
        })

    return records, pd.DataFrame(detail_rows)


def main():
    print("=" * 60)
    print("群体归因端到端验证 — 保险投保人群模拟")
    print("=" * 60)

    model_path = PROJECT_ROOT / "train" / "rehealth_v3_unified.pkl"
    if not model_path.exists():
        model_path = PROJECT_ROOT / "train" / "rehealth_v2_final.pkl"
    if not model_path.exists():
        print("ERROR: No model found!")
        sys.exit(1)

    print(f"Model: {model_path.name}")
    model = joblib.load(str(model_path))

    # Step 1: Generate simulated cohort
    print("\n[Step 1] 生成模拟投保人群 (200人)...")
    records, details = generate_cohort(model, n=200)

    treated = details[details["group"] == "treated"]
    control = details[details["group"] == "control"]
    print(f"  Treated: {len(treated)} 人, Control: {len(control)} 人")
    print(f"  Treated 风险变化: {treated['delta_y'].mean():+.4f} (mean), {treated['delta_y'].median():+.4f} (median)")
    print(f"  Control 风险变化: {control['delta_y'].mean():+.4f} (mean), {control['delta_y'].median():+.4f} (median)")

    # Step 2: Run PSM + DRE + Bootstrap
    print("\n[Step 2] PSM + 双重稳健估计 + Bootstrap...")
    engine = GroupAttributionEngine({
        "caliper": 0.2,
        "n_bootstrap": 500,
        "confidence_level": 0.95,
        "min_group_size": 30,
    })
    result = engine.estimate(records)

    if result["status"] != "success":
        print(f"  ERROR: {result}")
        sys.exit(1)

    print(f"  Status: {result['status']}")
    print(f"  ATT (Average Treatment effect on Treated): {result['att']:+.4f}")
    print(f"  95% CI: [{result['ci_lower']:+.4f}, {result['ci_upper']:+.4f}]")
    print(f"  Significant: {result['is_significant']} (CI entirely below 0)")
    print(f"  Matched pairs: {result['n_matched_pairs']}")

    print(f"\n  Matching balance:")
    for feat, info in result["matching_balance"].items():
        status = "OK" if info["balanced"] else "WARN"
        print(f"    {feat:30s} SMD={info['smd']:.4f}  [{status}]")

    # Step 3: Insurance settlement report
    print("\n" + "=" * 60)
    print("保险结算报告")
    print("=" * 60)

    avg_premium = 5000  # 年均保费 ¥5000
    avg_claim = 50000   # 平均理赔 ¥50000
    n_treated_total = result["n_treated"]

    risk_reduction = abs(result["att"]) if result["att"] < 0 else 0
    estimated_claims_avoided = risk_reduction * n_treated_total
    estimated_savings = estimated_claims_avoided * avg_claim

    settlement = {
        "report_type": "group_attribution_settlement",
        "period": "2026-Q1",
        "cohort_size": result["n_total_users"],
        "treated_users": result["n_treated"],
        "control_users": result["n_control"],
        "matched_pairs": result["n_matched_pairs"],
        "att_risk_reduction": round(result["att"], 4),
        "ci_95": [round(result["ci_lower"], 4), round(result["ci_upper"], 4)],
        "statistically_significant": result["is_significant"],
        "estimated_claims_avoided": round(estimated_claims_avoided, 2),
        "estimated_savings_rmb": round(estimated_savings, 2),
        "per_user_value_rmb": round(estimated_savings / n_treated_total, 2) if n_treated_total > 0 else 0,
        "methodology": "PSM + Doubly Robust Estimation + Bootstrap CI",
    }
    print(json.dumps(settlement, indent=2, ensure_ascii=False))

    print("\n" + "=" * 60)
    print("群体归因验证完成!")
    print("=" * 60)


if __name__ == "__main__":
    main()
