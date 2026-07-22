"""
Enhanced Prediction Example

Demonstrates how to use the enhanced PIAS engine with feature engineering
and knowledge distillation for improved CVD risk prediction.
"""

import sys
sys.path.insert(0, ".")

from healthagent.pias import (
    FeatureEngineer,
    EnhancedCVDRiskScorer,
    EnhancedModelRegistry,
)


def example_feature_engineering():
    """Show how feature engineering expands 16 features to 40+."""
    import pandas as pd

    print("=" * 60)
    print("Feature Engineering Example")
    print("=" * 60)

    # Sample patient data (16 base features)
    patient = {
        "age": 52,
        "gender": 1,  # male
        "bmi": 27.5,
        "sbp": 145,
        "dbp": 90,
        "fasting_glucose": 6.2,
        "total_cholesterol": 5.8,
        "ldl": 3.9,
        "hdl": 1.1,
        "triglycerides": 2.1,
        "exercise_days": 2,
        "smoking": 1,
        "drinking": 0,
        "diabetes_history": 0,
        "hypertension_history": 1,
        "family_history": 1,
    }

    print("\n[Input] 16 Base Features:")
    for k, v in patient.items():
        print(f"  {k}: {v}")

    # Apply feature engineering
    fe = FeatureEngineer()
    df = pd.DataFrame([patient])
    df_engineered = fe.transform(df)

    print(f"\n[Output] {len(df_engineered.columns)} Engineered Features:")
    print("\n--- New Derived Features ---")
    for col in df_engineered.columns:
        if col not in patient:
            val = df_engineered[col].iloc[0]
            desc = _feature_description(col)
            print(f"  {col}: {val:.3f}  ({desc})")

    print(f"\nTotal features: {len(fe.get_feature_names())}")
    return df_engineered


def example_enhanced_prediction():
    """Show enhanced prediction with engineered features."""
    print("\n" + "=" * 60)
    print("Enhanced Prediction Example")
    print("=" * 60)

    # This would use the actual distilled model
    # For demo, we show the interface

    print("\n[Note] To use the enhanced model, you need:")
    print("  1. Train a distilled model using knowledge_distillation.py")
    print("  2. Place the model in models/distilled/")
    print("  3. Load with EnhancedCVDRiskScorer")

    print("\nExample code:")
    print("""
    from healthagent.pias import EnhancedCVDRiskScorer

    # Load distilled model
    scorer = EnhancedCVDRiskScorer(
        model_path="models/distilled/rehealth_v2_distilled_catboost.pkl",
        model_auc=0.81,
    )

    # Predict with same 16 features
    patient = {
        "age": 52, "gender": 1, "bmi": 27.5,
        "sbp": 145, "dbp": 90,
        "fasting_glucose": 6.2, "total_cholesterol": 5.8,
        "ldl": 3.9, "hdl": 1.1, "triglycerides": 2.1,
        "exercise_days": 2, "smoking": 1, "drinking": 0,
        "diabetes_history": 0, "hypertension_history": 1,
        "family_history": 1,
    }

    result = scorer.predict(patient)

    print(f"Risk Score: {result['risk_score']:.1%}")
    print(f"Risk Level: {result['risk_level']}")
    print(f"Model: {result['model_version']}")
    print(f"Features: {result['feature_engineering']['engineered_features']}")

    print("\\nTop Risk Factors:")
    for factor in result['top_contributors']:
        print(f"  - {factor['description']}: {factor['contribution']:+.3f}")
    """)


def example_distillation_training():
    """Show how to run knowledge distillation."""
    print("\n" + "=" * 60)
    print("Knowledge Distillation Training Example")
    print("=" * 60)

    print("""
    # Prepare your data as CSV with columns:
    # - 16 base features (age, gender, bmi, sbp, dbp, etc.)
    # - label (0 or 1 for CVD outcome)

    # Run distillation pipeline:
    python -m healthagent.pias.knowledge_distillation \\
        --teacher models/rehealth_v8_final.pkl \\
        --data data/training_data.csv \\
        --output models/distilled \\
        --student-type catboost

    # This will:
    # 1. Load V8 teacher model (97 features, AUC 0.86)
    # 2. Apply feature engineering (16 → 40+ features)
    # 3. Generate soft labels from teacher
    # 4. Train student model with soft labels
    # 5. Save distilled model

    # Expected output:
    # Train AUC: 0.85+
    # Val AUC: 0.80-0.82
    # Test AUC: 0.80-0.82
    """)


def example_model_comparison():
    """Compare original V2 vs enhanced V2."""
    print("\n" + "=" * 60)
    print("Model Comparison: Original V2 vs Enhanced V2")
    print("=" * 60)

    print("""
    ┌─────────────────┬─────────────┬─────────────────┐
    │     Metric      │  Original   │     Enhanced    │
    │                 │     V2      │   V2-distilled  │
    ├─────────────────┼─────────────┼─────────────────┤
    │ Features        │     16      │     40+         │
    │ AUC             │    0.767    │    0.80-0.82    │
    │ User Input      │   16 items  │   16 items      │
    │ Extra Data      │    None     │    None         │
    │ Model Size      │   Small     │   Medium        │
    │ Inference Time  │    Fast     │   Fast          │
    └─────────────────┴─────────────┴─────────────────┘

    Key Insight:
    - Same user input (16 features)
    - Better accuracy through feature engineering
    - Knowledge from V8 teacher model
    - No additional data collection needed
    """)


def _feature_description(feature: str) -> str:
    """Get feature description."""
    descriptions = {
        "pulse_pressure": "脉压 = 收缩压 - 舒张压",
        "mean_arterial_pressure": "平均动脉压",
        "bp_ratio": "血压比 = 收缩压 / 舒张压",
        "bp_category": "血压分级 (0-3)",
        "non_hdl_cholesterol": "非HDL胆固醇 = 总胆固醇 - HDL",
        "tc_hdl_ratio": "总胆固醇/HDL比值",
        "ldl_hdl_ratio": "LDL/HDL比值",
        "trig_hdl_ratio": "甘油三酯/HDL比值",
        "atherogenic_index": "致动脉粥样硬化指数 = log(TG/HDL)",
        "metabolic_score": "代谢综合征评分 (0-5)",
        "glucose_category": "血糖分级 (0=正常, 1=前期, 2=糖尿病)",
        "bmi_category": "BMI分级 (0-3)",
        "bmi_age_interaction": "BMI × 年龄",
        "smoking_age_interaction": "吸烟 × 年龄",
        "smoking_bp_interaction": "吸烟 × 收缩压",
        "diabetes_glucose_interaction": "糖尿病史 × 血糖",
        "framingham_risk_factors": "Framingham风险评分因子",
        "ascvd_risk_factors": "ASCVD风险评分因子",
        "risk_factor_count": "风险因子总数",
        "lifestyle_score": "生活方式评分 (0-100, 越高越好)",
        "age_squared": "年龄²",
        "age_risk_interaction": "年龄 × 风险因子数",
        "gender_age_interaction": "性别 × 年龄",
        "gender_bp_interaction": "性别 × 收缩压",
    }
    return descriptions.get(feature, feature)


if __name__ == "__main__":
    print("PIAS Enhanced Prediction Examples")
    print("=" * 60)

    # Run examples
    example_feature_engineering()
    example_enhanced_prediction()
    example_distillation_training()
    example_model_comparison()

    print("\n" + "=" * 60)
    print("For more information, see docs/PIAS_INTEGRATION.md")
    print("=" * 60)
