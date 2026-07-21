#!/usr/bin/env python3
"""
Knowledge Distillation Training Script

Train a distilled V2 model using V4 teacher on CHARLS data.

Teacher: rehealth_v4_biomarkers.pkl (37 features, better than V2)
Student: Enhanced V2 with 40+ engineered features

Expected results:
- Original V2: AUC 0.767 (16 features)
- Distilled V2: AUC 0.80-0.82 (40 features, same user input)
"""

import sys
import os
import pandas as pd
import numpy as np
from pathlib import Path
from sklearn.model_selection import train_test_split

# Add project to path
sys.path.insert(0, ".")

from healthagent.pias import (
    FeatureEngineer,
    KnowledgeDistiller,
    EnhancedCVDRiskScorer,
)


def main():
    # ═══════════════════════════════════════════════════════════
    # Configuration
    # ═══════════════════════════════════════════════════════════

    # Paths - Use V4 teacher (37 features, compatible with CHARLS)
    TEACHER_MODEL_PATH = "/mnt/e/DISK_D/Project/Rehealth_AI/Project/train/rehealth_v4_biomarkers.pkl"
    DATA_PATH = "/mnt/e/DISK_D/Project/Rehealth_AI/Project/data/charls/CHARLS.csv"
    OUTPUT_DIR = "models/distilled"

    # Distillation parameters
    TEMPERATURE = 3.0      # Soften probability distribution
    ALPHA = 0.7            # Weight: soft labels vs hard labels
    STUDENT_TYPE = "catboost"  # catboost / lightgbm / xgboost / sklearn

    # ═══════════════════════════════════════════════════════════
    # Step 1: Load and prepare data
    # ═══════════════════════════════════════════════════════════

    print("=" * 60)
    print("PIAS Knowledge Distillation Training")
    print("=" * 60)

    print(f"\n[1/7] Loading data from {DATA_PATH}...")
    df = pd.read_csv(DATA_PATH, encoding='gbk')
    print(f"  Loaded {len(df)} samples, {len(df.columns)} columns")

    # Label column
    label_col = "hearte"  # CVD outcome
    df["label"] = df[label_col]
    print(f"  Label: {label_col} (CVD)")
    print(f"  Label distribution:\n{df[label_col].value_counts()}")

    # ═══════════════════════════════════════════════════════════
    # Step 2: Feature mapping (CHARLS -> our format)
    # ═══════════════════════════════════════════════════════════

    print("\n[2/7] Mapping features...")

    # Mapping for V4 teacher (37 features)
    v4_feature_mapping = {
        # Base 16 features
        "age": "age",
        "gender": "gender",
        "bmi": "bmi",
        "sbp": "systo",
        "dbp": "diasto",
        "fasting_glucose": "bl_glu",
        "total_cholesterol": "bl_cho",
        "ldl": "bl_ldl",
        "hdl": "bl_hdl",
        "triglycerides": "bl_tg",
        "smoking": "smokev",
        "drinking": "drinkev",
        "diabetes_history": "diabe",
        "hypertension_history": "hibpe",
        "family_history": None,  # Not in CHARLS
        "exercise_days": "totmet",
        # Extended features for V4
        "hba1c": "bl_hbalc",
        "crp": "bl_crp",
        "uric_acid": "bl_ua",
        "waist_circumference": "mwaist",
        "hemoglobin": "bl_hgb",
        "wbc": "bl_wbc",
    }

    # Apply mapping
    for our_feat, charls_feat in v4_feature_mapping.items():
        if charls_feat and charls_feat in df.columns:
            df[our_feat] = df[charls_feat]
        elif our_feat not in df.columns:
            df[our_feat] = 0

    # Fix exercise_days (convert totmet to days)
    if "totmet" in df.columns:
        df["exercise_days"] = (df["totmet"] / 30).clip(0, 7).astype(int)

    print(f"  Mapped {len([f for f, c in v4_feature_mapping.items() if c and c in df.columns])} features")

    # ═══════════════════════════════════════════════════════════
    # Step 3: Prepare teacher features (V4 format)
    # ═══════════════════════════════════════════════════════════

    print("\n[3/7] Preparing teacher features...")

    # V4 expects these features
    v4_features_list = [
        'age', 'gender', 'bmi', 'sbp', 'dbp', 'fasting_glucose',
        'total_cholesterol', 'ldl', 'hdl', 'triglycerides',
        'exercise_days', 'smoking', 'drinking',
        'diabetes_history', 'hypertension_history', 'family_history',
        'hba1c', 'crp', 'egfr', 'uric_acid', 'waist_circumference',
        'hemoglobin', 'wbc', 'albumin'
    ]

    # Fill missing features with 0
    for feat in v4_features_list:
        if feat not in df.columns:
            df[feat] = 0
            print(f"  Warning: {feat} not in data, filled with 0")

    # Generate derived features for V4
    df["pulse_pressure"] = df["sbp"] - df["dbp"]
    df["map"] = df["dbp"] + (df["sbp"] - df["dbp"]) / 3
    df["non_hdl"] = df["total_cholesterol"] - df["hdl"]
    df["tc_hdl_ratio"] = df["total_cholesterol"] / df["hdl"].clip(lower=0.1)
    df["ldl_hdl_ratio"] = df["ldl"] / df["hdl"].clip(lower=0.1)
    df["tyg_index"] = np.log(df["triglycerides"].clip(lower=0.1) * df["fasting_glucose"].clip(lower=0.1) / 2)
    df["age_sbp"] = df["age"] * df["sbp"]
    df["bmi_glucose"] = df["bmi"] * df["fasting_glucose"]
    df["risk_factor_count"] = (
        (df["smoking"] > 0).astype(int) +
        (df["bmi"] > 25).astype(int) +
        (df["sbp"] > 140).astype(int) +
        (df["fasting_glucose"] > 7.0).astype(int) +
        (df["total_cholesterol"] > 6.2).astype(int)
    )
    df["age_sq"] = df["age"] ** 2
    df["aip"] = np.log10(df["triglycerides"].clip(lower=0.1) / df["hdl"].clip(lower=0.1))
    df["waist_bmi_ratio"] = df["waist_circumference"] / df["bmi"].clip(lower=1)
    df["crp_wbc_index"] = df["crp"] * df["wbc"]

    # V4 full feature list (37 features)
    v4_full_features = [
        'age', 'gender', 'bmi', 'sbp', 'dbp', 'fasting_glucose',
        'total_cholesterol', 'ldl', 'hdl', 'triglycerides',
        'exercise_days', 'smoking', 'drinking',
        'diabetes_history', 'hypertension_history', 'family_history',
        'hba1c', 'crp', 'egfr', 'uric_acid', 'waist_circumference',
        'hemoglobin', 'wbc', 'albumin',
        'pulse_pressure', 'map', 'non_hdl', 'tc_hdl_ratio', 'ldl_hdl_ratio',
        'tyg_index', 'age_sbp', 'bmi_glucose', 'risk_factor_count',
        'age_sq', 'aip', 'waist_bmi_ratio', 'crp_wbc_index'
    ]

    X_teacher = df[v4_full_features].copy()
    X_teacher = X_teacher.fillna(X_teacher.median())
    print(f"  Teacher features: {len(v4_full_features)}")

    # ═══════════════════════════════════════════════════════════
    # Step 4: Feature engineering for student (16 -> 40+)
    # ═══════════════════════════════════════════════════════════

    print("\n[4/7] Applying feature engineering for student...")
    fe = FeatureEngineer()

    # Student uses 16 base features + engineered
    student_base = [
        "age", "gender", "bmi", "sbp", "dbp",
        "fasting_glucose", "total_cholesterol", "ldl", "hdl", "triglycerides",
        "exercise_days", "smoking", "drinking",
        "diabetes_history", "hypertension_history", "family_history",
    ]

    X_student_base = df[student_base].copy()
    X_student_base = X_student_base.fillna(X_student_base.median())
    X_student = fe.transform(X_student_base)

    y = df["label"].values

    print(f"  Student features: {X_student.shape[1]}")

    # ═══════════════════════════════════════════════════════════
    # Step 5: Split data
    # ═══════════════════════════════════════════════════════════

    print("\n[5/7] Splitting data...")
    indices = np.arange(len(df))
    idx_train, idx_test = train_test_split(indices, test_size=0.2, random_state=42, stratify=y)
    idx_train, idx_val = train_test_split(idx_train, test_size=0.15, random_state=42, stratify=y[idx_train])

    X_teacher_train = X_teacher.iloc[idx_train]
    X_teacher_test = X_teacher.iloc[idx_test]
    X_student_train = X_student.iloc[idx_train]
    X_student_val = X_student.iloc[idx_val]
    X_student_test = X_student.iloc[idx_test]
    y_train = y[idx_train]
    y_val = y[idx_val]
    y_test = y[idx_test]

    print(f"  Train: {len(idx_train)}, Val: {len(idx_val)}, Test: {len(idx_test)}")

    # ═══════════════════════════════════════════════════════════
    # Step 6: Load teacher and generate soft labels
    # ═══════════════════════════════════════════════════════════

    print(f"\n[6/7] Loading V4 teacher model...")
    distiller = KnowledgeDistiller(
        teacher_model_path=TEACHER_MODEL_PATH,
        student_model_type=STUDENT_TYPE,
        temperature=TEMPERATURE,
        alpha=ALPHA,
    )
    distiller.load_teacher()

    print("  Generating soft labels from teacher...")
    soft_labels_train = distiller.generate_soft_labels(X_teacher_train)
    print(f"  Soft labels range: [{soft_labels_train.min():.3f}, {soft_labels_train.max():.3f}]")
    print(f"  Soft labels mean: {soft_labels_train.mean():.3f}")

    # ═══════════════════════════════════════════════════════════
    # Step 7: Train student model
    # ═══════════════════════════════════════════════════════════

    print(f"\n[7/7] Training {STUDENT_TYPE} student model...")
    distiller.train_student(
        X_student=X_student_train,
        y_hard=y_train,
        soft_labels=soft_labels_train,
        eval_set=(X_student_val, y_val),
    )

    # ═══════════════════════════════════════════════════════════
    # Evaluate
    # ═══════════════════════════════════════════════════════════

    print("\n" + "=" * 60)
    print("Evaluation Results")
    print("=" * 60)

    train_results = distiller.evaluate(X_student_train, y_train, "Train")
    val_results = distiller.evaluate(X_student_val, y_val, "Validation")
    test_results = distiller.evaluate(X_student_test, y_test, "Test")

    # ═══════════════════════════════════════════════════════════
    # Save model
    # ═══════════════════════════════════════════════════════════

    output_path = Path(OUTPUT_DIR)
    output_path.mkdir(parents=True, exist_ok=True)

    model_path = output_path / f"rehealth_v2_distilled_{STUDENT_TYPE}.pkl"
    distiller.save_student(str(model_path))

    # Save feature list
    feature_path = output_path / "distilled_features.txt"
    with open(feature_path, "w") as f:
        for feat in X_student_train.columns:
            f.write(f"{feat}\n")

    # Save training config
    config_path = output_path / "training_config.txt"
    with open(config_path, "w") as f:
        f.write(f"teacher_model: {TEACHER_MODEL_PATH}\n")
        f.write(f"student_type: {STUDENT_TYPE}\n")
        f.write(f"temperature: {TEMPERATURE}\n")
        f.write(f"alpha: {ALPHA}\n")
        f.write(f"train_auc: {train_results['auc']:.4f}\n")
        f.write(f"val_auc: {val_results['auc']:.4f}\n")
        f.write(f"test_auc: {test_results['auc']:.4f}\n")
        f.write(f"features: {len(X_student_train.columns)}\n")

    # ═══════════════════════════════════════════════════════════
    # Summary
    # ═══════════════════════════════════════════════════════════

    improvement = test_results['auc'] - 0.767

    print("\n" + "=" * 60)
    print("Training Complete!")
    print("=" * 60)
    print(f"\nModel saved to: {model_path}")
    print(f"Features saved to: {feature_path}")

    print(f"\n┌─────────────────────────────────────┐")
    print("│  Results Summary                    │")
    print("├─────────────────────────────────────┤")
    print(f"│  Original V2 AUC:    0.767          │")
    print(f"│  Distilled V2 AUC:   {test_results['auc']:.3f}          │")
    print(f"│  Features:           {len(X_student_train.columns):<14}│")
    print(f"│  Improvement:        {improvement:+.3f}          │")
    print("└─────────────────────────────────────┘")

    print("\nNext steps:")
    print("1. Test the model with enhanced_prediction_example.py")
    print("2. Update PIAS_MODEL_PATH to point to the distilled model")
    print("3. Push changes to GitHub")


if __name__ == "__main__":
    main()
