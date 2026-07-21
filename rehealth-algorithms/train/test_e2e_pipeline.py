# -*- coding: utf-8 -*-
"""
端到端流程验证: 新模型 Risk Score → MiMo LLM 处方生成 → 归因

Usage:
    python train/test_e2e_pipeline.py
"""

import json
import sys
from pathlib import Path

PROJECT_ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(PROJECT_ROOT))

from bodyup_cloud.config.settings import settings
from bodyup_cloud.engine.risk_scorer import CVDRiskScorer
from bodyup_cloud.engine.llm_provider import create_provider
from bodyup_cloud.engine.prescription_generator import PrescriptionGenerator
from bodyup_cloud.engine.intervention_attributor import InterventionAttributor

# Test patient profile
TEST_PATIENT = {
    "age": 55,
    "gender": 1,
    "bmi": 27.5,
    "sbp": 145,
    "dbp": 92,
    "fasting_glucose": 6.8,
    "total_cholesterol": 5.9,
    "ldl": 3.8,
    "hdl": 1.1,
    "triglycerides": 2.1,
    "exercise_days": 1,
    "smoking": 1,
    "drinking": 1,
    "diabetes_history": 0,
    "hypertension_history": 1,
    "family_history": 1,
}

MEMORY_SNAPSHOT = {
    "age_bracket": "50-59",
    "bp_variability": "偏高",
    "night_bp_pattern": "non-dipper",
    "activity_level": "久坐",
    "sleep_quality_index": 0.45,
    "intervention_compliance": 0.3,
}


def main():
    print("=" * 60)
    print("PIAS 端到端流程验证")
    print("=" * 60)

    # ── Step 1: Predict ──────────────────────────────
    print("\n[Step 1] Predict — CatBoost 风险评分")

    # Try v3 first, fallback to v2
    model_path = PROJECT_ROOT / "train" / "rehealth_v3_unified.pkl"
    if not model_path.exists():
        model_path = PROJECT_ROOT / "train" / "rehealth_v2_final.pkl"
    if not model_path.exists():
        print("  ERROR: No model found!")
        sys.exit(1)

    print(f"  Model: {model_path.name}")
    scorer = CVDRiskScorer(str(model_path))
    risk_result = scorer.predict(TEST_PATIENT)

    print(f"  Risk score: {risk_result['risk_score']:.4f}")
    print(f"  Risk level: {risk_result['risk_level']}")
    print(f"  Top contributors:")
    sorted_fc = sorted(risk_result["feature_contributions"].items(), key=lambda x: abs(x[1]), reverse=True)
    for k, v in sorted_fc[:5]:
        print(f"    {k:25s} {v:+.4f}")

    # ── Step 2: Intervene — LLM 处方生成 ─────────────
    print("\n[Step 2] Intervene — MiMo LLM 处方生成")

    api_key = settings.mimo_api_key
    if not api_key:
        print("  SKIP: MIMO_API_KEY not set in .env")
        print("  (Set MIMO_API_KEY to run LLM prescription generation)")
        return

    print(f"  Provider: mimo ({settings.mimo_model})")
    print(f"  Base URL: {settings.mimo_base_url}")

    provider = create_provider(
        "mimo",
        api_key=api_key,
        model=settings.mimo_model,
        base_url=settings.mimo_base_url,
    )
    gen = PrescriptionGenerator(provider)

    print("  Generating prescription...")
    prescription = gen.generate(risk_result, MEMORY_SNAPSHOT)

    print(f"\n  Raw LLM response ({len(prescription)} chars):")
    print("  " + "-" * 50)
    for line in prescription.split("\n"):
        print(f"  {line}")
    print("  " + "-" * 50)

    # Try to parse as JSON
    try:
        parsed = json.loads(prescription)
        print("\n  Parsed JSON keys:", list(parsed.keys()))
    except json.JSONDecodeError:
        # Try extracting JSON from markdown code block
        import re
        m = re.search(r"```(?:json)?\s*\n?(.*?)\n?```", prescription, re.DOTALL)
        if m:
            try:
                parsed = json.loads(m.group(1))
                print("\n  Parsed JSON keys (from code block):", list(parsed.keys()))
            except json.JSONDecodeError:
                print("\n  Note: LLM response is not strict JSON (OK for prototype)")
        else:
            print("\n  Note: LLM response is not strict JSON (OK for prototype)")

    # ── Step 3: Attribute — 干预效果归因 ────────────────
    print("\n[Step 3] Attribute — 干预效果归因 (counterfactual)")

    T1_PATIENT = {
        "age": 55, "gender": 1, "bmi": 25.8, "sbp": 130, "dbp": 82,
        "fasting_glucose": 6.0, "total_cholesterol": 5.2, "ldl": 3.2,
        "hdl": 1.3, "triglycerides": 1.7, "exercise_days": 1,
        "smoking": 1, "drinking": 1, "diabetes_history": 0,
        "hypertension_history": 1, "family_history": 1,
    }

    attributor = InterventionAttributor(scorer.model)
    attr_report = attributor.attribute(TEST_PATIENT, T1_PATIENT)
    attr_dict = attr_report.to_dict()

    print(f"  Risk: {attr_dict['risk_before']:.4f} → {attr_dict['risk_after']:.4f} "
          f"(change: {attr_dict['risk_change']:+.4f})")
    print(f"  Intervention effect: {attr_dict['intervention_effect']:+.4f}")
    print(f"  Attribution ratio: {attr_dict['attribution_ratio']:.0%}")
    print(f"  Summary: {attr_dict['summary']}")
    improvements = [f for f in attr_dict["feature_details"]
                    if f["is_modifiable"] and f["direction"] == "improved"]
    if improvements:
        print(f"  Top improvements:")
        for f in improvements[:5]:
            print(f"    {f['feature']:20s} {f['t0_value']:.1f}→{f['t1_value']:.1f}  "
                  f"risk avoided: {f['marginal_effect']:+.4f}")

    # ── Step 4: Settle — 签名 ────────────────────────
    print("\n[Step 4] Settle — Ed25519 签名")
    key_path = PROJECT_ROOT / settings.ed25519_private_key_path
    if key_path.exists():
        from bodyup_cloud.engine.report_signer import ReportSigner
        from cryptography.hazmat.primitives.serialization import load_pem_private_key

        with open(key_path, "rb") as f:
            private_key = load_pem_private_key(f.read(), password=None)
        signer = ReportSigner(private_key)
        report = {
            "patient": TEST_PATIENT,
            "risk_result": {
                "risk_score": risk_result["risk_score"],
                "risk_level": risk_result["risk_level"],
            },
        }
        signed = signer.sign(report)
        print(f"  Signature: {signed['signature'][:60]}...")
        print(f"  Verified: {signer.verify(signed)}")
    else:
        print("  SKIP: Ed25519 key not found (will be auto-generated on server start)")

    print("\n" + "=" * 60)
    print("PIAS pipeline complete!")
    print("=" * 60)


if __name__ == "__main__":
    main()
