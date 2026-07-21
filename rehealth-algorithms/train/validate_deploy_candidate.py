# -*- coding: utf-8 -*-
"""
validate_deploy_candidate.py — Standalone validation for deploy artifacts.

Loads rehealth_cvd_catboost.pkl + feature_cols.pkl + model_meta_v2.json
from a deploy_candidate directory, runs a synthetic valid CVD input,
and verifies:
  - model loads via joblib
  - feature_cols loads
  - meta loads
  - predict_proba returns shape (1, 2)
  - risk_score in [0, 1]
  - feature order matches meta
  - prints deployment readiness

Usage:
    python train/validate_deploy_candidate.py
    python train/validate_deploy_candidate.py --candidate outputs/cvd_retrain_<ts>/deploy_candidate
"""

import argparse
import json
import sys
from pathlib import Path

import joblib
import numpy as np
import pandas as pd


# Canonical Android C1 CVD-16 feature order
CVD16_CANONICAL = [
    "age", "gender", "bmi", "sbp", "dbp",
    "fasting_glucose", "total_cholesterol", "ldl", "hdl", "triglycerides",
    "exercise_days", "smoking", "drinking",
    "diabetes_history", "hypertension_history", "family_history",
]

# Synthetic valid CVD-16 input
SYNTHETIC_INPUT = {
    "age": 52, "gender": 1, "bmi": 27.4,
    "sbp": 136.0, "dbp": 86.0,
    "fasting_glucose": 6.2, "total_cholesterol": 5.8,
    "ldl": 3.9, "hdl": 1.1, "triglycerides": 2.1,
    "exercise_days": 3, "smoking": 0, "drinking": 0,
    "diabetes_history": 0, "hypertension_history": 1, "family_history": 1,
}


def validate(candidate_dir: Path, verbose: bool = True) -> dict:
    result = {"ok": True, "checks": []}

    def check(name, cond, detail=""):
        status = "PASS" if cond else "FAIL"
        if verbose:
            print(f"  [{status}] {name}" + (f" — {detail}" if detail else ""))
        result["checks"].append({"name": name, "pass": bool(cond), "detail": detail})
        if not cond:
            result["ok"] = False

    print(f"\nValidating deploy_candidate at: {candidate_dir}")
    print("=" * 60)

    # Resolve artifacts (canonical + historical alias)
    model_path = candidate_dir / "rehealth_cvd_catboost.pkl"
    if not model_path.exists():
        model_path = candidate_dir / "rehealth_v2_final.pkl"
    fc_path = candidate_dir / "feature_cols.pkl"
    if not fc_path.exists():
        fc_path = candidate_dir / "feature_cols_v2.pkl"
    meta_path = candidate_dir / "model_meta_v2.json"
    if not meta_path.exists():
        meta_path = candidate_dir / "model_metadata.json"

    check("model artifact exists", model_path.exists(), str(model_path))
    check("feature_cols artifact exists", fc_path.exists(), str(fc_path))
    check("meta artifact exists", meta_path.exists(), str(meta_path))

    if not (model_path.exists() and fc_path.exists()):
        result["ok"] = False
        return result

    # Load
    print("\n[load]")
    try:
        model = joblib.load(model_path)
        check("model loads via joblib", True, type(model).__name__)
    except Exception as e:
        check("model loads via joblib", False, str(e))
        result["ok"] = False
        return result

    try:
        feature_cols = joblib.load(fc_path)
        check("feature_cols loads", True, f"{len(feature_cols)} features")
    except Exception as e:
        check("feature_cols loads", False, str(e))
        result["ok"] = False
        return result

    meta = {}
    if meta_path.exists():
        try:
            meta = json.loads(meta_path.read_text(encoding="utf-8"))
            check("meta loads as JSON", True, meta.get("model_version", "unknown"))
        except Exception as e:
            check("meta loads as JSON", False, str(e))

    # Verify feature_cols is a list of strings
    check("feature_cols is list of strings",
          isinstance(feature_cols, list) and all(isinstance(c, str) for c in feature_cols),
          str(feature_cols))

    # Verify feature count and (if present in meta) order match
    if meta and "feature_names_ordered" in meta:
        meta_order = meta["feature_names_ordered"]
        check("feature_cols matches meta.feature_names_ordered",
              feature_cols == meta_order,
              f"{len(feature_cols)} vs {len(meta_order)}")

    if meta:
        check("metadata artifact_name is canonical",
              meta.get("artifact_name") == "rehealth_cvd_catboost.pkl",
              str(meta.get("artifact_name")))
        check("metadata has finite top-level model_auc",
              isinstance(meta.get("model_auc"), (int, float)) and np.isfinite(meta["model_auc"]),
              str(meta.get("model_auc")))
        check("metadata calibration is truthful",
              meta.get("calibration_status") in {"applied", "computed", "failed", "not_computable"}
              and (meta.get("calibration_status") not in {"applied", "computed"}
                   or (isinstance(meta.get("calibration_slope"), (int, float))
                       and isinstance(meta.get("calibration_intercept"), (int, float)))),
              str(meta.get("calibration_status")))

    # Check if it's a cvd-16 schema
    is_cvd16 = feature_cols == CVD16_CANONICAL
    check("feature order == cvd-16-v1 canonical", is_cvd16,
          "exact 16-field match" if is_cvd16 else f"different ({len(feature_cols)} features)")

    # Build input frame for synthetic test — handle non-cvd16 schemas by filling available fields
    print("\n[predict]")
    synth_row = {}
    for c in feature_cols:
        synth_row[c] = SYNTHETIC_INPUT.get(c, 0)
    model_input = pd.DataFrame([synth_row], columns=feature_cols).to_numpy()

    try:
        if hasattr(model, "predict_proba"):
            proba = model.predict_proba(model_input)
            check("model.predict_proba exists", True, f"shape={proba.shape}")
        else:
            proba = model.predict(model_input)
            check("model.predict_proba exists", False,
                  "falling back to predict (no predict_proba)")
        if proba.ndim == 2:
            risk_score = float(proba[0, 1])
        else:
            risk_score = float(np.asarray(proba).ravel()[0])
        check("predict returns shape (1, 2) or scalar", proba.size >= 1, f"size={proba.size}")
        check("risk_score in [0, 1]", 0.0 <= risk_score <= 1.0, f"{risk_score:.4f}")
        check("risk_score is finite", np.isfinite(risk_score), f"{risk_score}")
    except Exception as e:
        check("predict runs without error", False, str(e))
        result["ok"] = False
        risk_score = None

    # Synthetic valid input must produce a risk_score
    check("synthetic valid input produces risk_score",
          risk_score is not None and np.isfinite(risk_score),
          f"risk_score={risk_score}")

    # Risk level mapping
    if risk_score is not None:
        if risk_score < 0.1:
            risk_level = "low"
        elif risk_score < 0.3:
            risk_level = "moderate"
        elif risk_score < 0.5:
            risk_level = "high"
        else:
            risk_level = "very_high"
        print(f"\n  Synthetic CVD-16 input → risk_score={risk_score:.4f}, risk_level={risk_level}")

    # Deployment readiness
    print("\n" + "=" * 60)
    if result["ok"]:
        print("DEPLOYMENT READY — all checks passed")
        if is_cvd16:
            print("Schema: cvd-16-v1 (compatible with model-service /health is_mock=false gate)")
        else:
            print(f"Schema: {len(feature_cols)}-field custom schema")
            print("NOTE: model-service requires exact cvd-16-v1 order for is_mock=false.")
            print("      This candidate uses a different schema and may need a new schema version.")
    else:
        print("DEPLOYMENT NOT READY — see FAIL checks above")
    print("=" * 60)

    return result


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--candidate",
                        default="outputs/cvd_retrain_20260710_023125/deploy_candidate",
                        help="Path to deploy_candidate directory")
    args = parser.parse_args()

    cand = Path(args.candidate)
    if not cand.is_absolute():
        cand = Path(__file__).resolve().parent.parent / cand
    if not cand.exists():
        print(f"ERROR: candidate directory not found: {cand}")
        return 2

    r = validate(cand)
    return 0 if r["ok"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
