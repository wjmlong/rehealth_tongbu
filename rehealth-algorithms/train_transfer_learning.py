#!/usr/bin/env python3
"""
Transfer Learning: NHANES → CHARLS

Transfer V2 model trained on NHANES (US) to CHARLS (China).
Goal: Improve CVD prediction for Chinese users.

Strategy:
1. Load V2 pre-trained on NHANES (AUC 0.9264)
2. Fine-tune on CHARLS data with lower learning rate
3. Use progressive unfreezing or full fine-tuning
"""

import sys
sys.path.insert(0, ".")

import pandas as pd
import numpy as np
from sklearn.model_selection import train_test_split, StratifiedKFold
from sklearn.metrics import roc_auc_score, classification_report
import joblib
from pathlib import Path
from catboost import CatBoostClassifier, Pool
import warnings
warnings.filterwarnings('ignore')


def load_nhanes_v2_features():
    """Load NHANES 2015-2016 data with V2 feature extraction."""
    DATA_DIR = "/mnt/e/DISK_D/Project/Rehealth_AI/Project/data/nhanes/2015-2016"

    print("Loading NHANES 2015-2016...")
    demo = pd.read_sas(f'{DATA_DIR}/DEMO_I.xpt', format='xport', encoding='latin-1')
    bmx = pd.read_sas(f'{DATA_DIR}/BMX_I.xpt', format='xport', encoding='latin-1')
    bpx = pd.read_sas(f'{DATA_DIR}/BPX_I.xpt', format='xport', encoding='latin-1')
    bpq = pd.read_sas(f'{DATA_DIR}/BPQ_I.xpt', format='xport', encoding='latin-1')
    glu = pd.read_sas(f'{DATA_DIR}/GLU_I.xpt', format='xport', encoding='latin-1')
    tchol = pd.read_sas(f'{DATA_DIR}/TCHOL_I.xpt', format='xport', encoding='latin-1')
    hdl_df = pd.read_sas(f'{DATA_DIR}/HDL_I.xpt', format='xport', encoding='latin-1')
    trigly = pd.read_sas(f'{DATA_DIR}/TRIGLY_I.xpt', format='xport', encoding='latin-1')
    smq = pd.read_sas(f'{DATA_DIR}/SMQ_I.xpt', format='xport', encoding='latin-1')
    alq = pd.read_sas(f'{DATA_DIR}/ALQ_I.xpt', format='xport', encoding='latin-1')
    diq = pd.read_sas(f'{DATA_DIR}/DIQ_I.xpt', format='xport', encoding='latin-1')
    paq = pd.read_sas(f'{DATA_DIR}/PAQ_I.xpt', format='xport', encoding='latin-1')
    mcq = pd.read_sas(f'{DATA_DIR}/MCQ_I.xpt', format='xport', encoding='latin-1')

    df = demo[['SEQN']].copy()
    for other in [bmx, bpx, bpq, glu, tchol, hdl_df, trigly, smq, alq, diq, paq, mcq]:
        df = pd.merge(df, other, on='SEQN', how='left')

    feat = pd.DataFrame()
    feat['age'] = demo['RIDAGEYR']
    feat['gender'] = (demo['RIAGENDR'] == 1).astype(int)
    feat['bmi'] = df['BMXBMI']
    feat['sbp'] = df['BPXSY1']
    feat['dbp'] = df['BPXDI1']
    feat['fasting_glucose'] = df['LBXGLU'] * 0.0555
    feat['total_cholesterol'] = df['LBXTC'] * 0.02586
    feat['hdl'] = df['LBDHDD'] * 0.02586
    feat['ldl'] = df['LBDLDL'] * 0.02586
    feat['triglycerides'] = df['LBXTR'] * 0.01129

    feat['smoking'] = 0
    if 'SMQ040' in df.columns:
        feat['smoking'] = df['SMQ040'].isin([1, 2]).astype(int)

    feat['drinking'] = 0
    if 'ALQ130' in df.columns:
        feat['drinking'] = (df['ALQ130'] >= 1).astype(int)

    feat['exercise_days'] = 0
    if 'PAD660' in df.columns and 'PAD675' in df.columns:
        vig = pd.to_numeric(df['PAD660'], errors='coerce').clip(0, 7).fillna(0)
        mod = pd.to_numeric(df['PAD675'], errors='coerce').clip(0, 7).fillna(0)
        feat['exercise_days'] = vig.combine(mod, max).astype(int)

    feat['diabetes_history'] = 0
    if 'DIQ010' in df.columns:
        feat['diabetes_history'] = (df['DIQ010'] == 1).astype(int)

    feat['hypertension_history'] = 0
    if 'BPQ020' in df.columns:
        feat['hypertension_history'] = (df['BPQ020'] == 1).astype(int)

    feat['family_history'] = 0
    for col in ['MCQ300B', 'MCQ300C']:
        if col in df.columns:
            feat['family_history'] = feat['family_history'] | (df[col] == 1).astype(int)

    label = pd.Series(0, index=df.index)
    for col in ['MCQ160B', 'MCQ160C', 'MCQ160D', 'MCQ160E', 'MCQ160F']:
        if col in df.columns:
            label = label | (df[col] == 1).astype(int)
    feat['label'] = label.values

    feat = feat[(feat['age'] >= 18) & (feat['age'] <= 100)].copy()
    for col in feat.columns:
        if col != 'label':
            feat[col] = feat[col].fillna(feat[col].median())

    return feat


def load_charls_features():
    """Load CHARLS data with V2 feature extraction."""
    print("Loading CHARLS...")
    df = pd.read_csv("/mnt/e/DISK_D/Project/Rehealth_AI/Project/data/charls/CHARLS.csv", encoding='gbk')

    feature_mapping = {
        "age": "age", "gender": "gender", "bmi": "bmi",
        "sbp": "systo", "dbp": "diasto",
        "fasting_glucose": "bl_glu", "total_cholesterol": "bl_cho",
        "ldl": "bl_ldl", "hdl": "bl_hdl", "triglycerides": "bl_tg",
        "smoking": "smokev", "drinking": "drinkev",
        "diabetes_history": "diabe", "hypertension_history": "hibpe",
        "family_history": None, "exercise_days": "totmet",
    }

    feat = pd.DataFrame()
    for our_feat, charls_feat in feature_mapping.items():
        if charls_feat and charls_feat in df.columns:
            feat[our_feat] = df[charls_feat]
        else:
            feat[our_feat] = 0

    feat['exercise_days'] = (feat['exercise_days'] / 30).clip(0, 7).astype(int)
    feat['label'] = df['hearte'].values

    feat = feat.copy()
    for col in feat.columns:
        if col != 'label':
            feat[col] = feat[col].fillna(feat[col].median())

    return feat


def train_transfer_model():
    """
    Transfer learning: NHANES → CHARLS

    Strategy:
    1. Pre-train on NHANES (larger, cleaner data)
    2. Fine-tune on CHARLS (target domain)
    """
    print("=" * 60)
    print("Transfer Learning: NHANES → CHARLS")
    print("=" * 60)

    OUTPUT_DIR = Path("models/transfer")
    OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

    V2_FEATURES = [
        "age", "gender", "bmi", "sbp", "dbp",
        "fasting_glucose", "total_cholesterol", "ldl", "hdl", "triglycerides",
        "exercise_days", "smoking", "drinking",
        "diabetes_history", "hypertension_history", "family_history",
    ]

    # ═══════════════════════════════════════════════════════════
    # Step 1: Load data
    # ═══════════════════════════════════════════════════════════

    print("\n[1/5] Loading datasets...")
    nhanes = load_nhanes_v2_features()
    charls = load_charls_features()

    print(f"  NHANES: {len(nhanes)} samples, CVD rate: {nhanes['label'].mean():.2%}")
    print(f"  CHARLS: {len(charls)} samples, CVD rate: {charls['label'].mean():.2%}")

    # ═══════════════════════════════════════════════════════════
    # Step 2: Split CHARLS data (target domain)
    # ═══════════════════════════════════════════════════════════

    print("\n[2/5] Splitting CHARLS data...")
    X_charls = charls[V2_FEATURES]
    y_charls = charls['label'].values

    X_train, X_test, y_train, y_test = train_test_split(
        X_charls, y_charls, test_size=0.2, random_state=42, stratify=y_charls
    )
    X_train, X_val, y_train, y_val = train_test_split(
        X_train, y_train, test_size=0.15, random_state=42, stratify=y_train
    )

    print(f"  Train: {len(X_train)}, Val: {len(X_val)}, Test: {len(X_test)}")

    # ═══════════════════════════════════════════════════════════
    # Step 3: Baseline - Train only on CHARLS
    # ═══════════════════════════════════════════════════════════

    print("\n[3/5] Baseline: Train on CHARLS only...")
    baseline_model = CatBoostClassifier(
        iterations=500,
        learning_rate=0.05,
        depth=6,
        l2_leaf_reg=3,
        random_seed=42,
        verbose=100,
    )
    baseline_model.fit(X_train, y_train, eval_set=(X_val, y_val), early_stopping_rounds=50)

    baseline_pred = baseline_model.predict_proba(X_test)[:, 1]
    baseline_auc = roc_auc_score(y_test, baseline_pred)
    print(f"  Baseline AUC: {baseline_auc:.4f}")

    # ═══════════════════════════════════════════════════════════
    # Step 4: Transfer Learning - Pre-train on NHANES, fine-tune on CHARLS
    # ═══════════════════════════════════════════════════════════

    print("\n[4/5] Transfer Learning: NHANES pre-training + CHARLS fine-tuning...")

    # Step 4a: Pre-train on NHANES
    X_nhanes = nhanes[V2_FEATURES]
    y_nhanes = nhanes['label'].values

    print("  Step 4a: Pre-training on NHANES...")
    pretrained_model = CatBoostClassifier(
        iterations=500,
        learning_rate=0.05,
        depth=6,
        l2_leaf_reg=3,
        random_seed=42,
        verbose=100,
    )
    pretrained_model.fit(X_nhanes, y_nhanes)

    # Evaluate pre-trained model on NHANES
    nhanes_pred = pretrained_model.predict_proba(X_nhanes)[:, 1]
    nhanes_auc = roc_auc_score(y_nhanes, nhanes_pred)
    print(f"  Pre-trained NHANES AUC: {nhanes_auc:.4f}")

    # Step 4b: Fine-tune on CHARLS with lower learning rate
    print("  Step 4b: Fine-tuning on CHARLS...")
    transfer_model = CatBoostClassifier(
        iterations=300,
        learning_rate=0.01,  # Lower learning rate for fine-tuning
        depth=6,
        l2_leaf_reg=3,
        random_seed=42,
        verbose=100,
    )
    # Use pre-trained model as initialization via fit() parameter
    transfer_model.fit(
        X_train, y_train,
        eval_set=(X_val, y_val),
        early_stopping_rounds=50,
        init_model=pretrained_model
    )

    transfer_pred = transfer_model.predict_proba(X_test)[:, 1]
    transfer_auc = roc_auc_score(y_test, transfer_pred)
    print(f"  Transfer AUC: {transfer_auc:.4f}")

    # ═══════════════════════════════════════════════════════════
    # Step 5: Evaluation and comparison
    # ═══════════════════════════════════════════════════════════

    print("\n[5/5] Evaluation...")
    print("\n" + "=" * 60)
    print("Results Comparison")
    print("=" * 60)

    print(f"\n┌─────────────────────────────────────────────────────┐")
    print(f"│  Model Comparison                                   │")
    print(f"├─────────────────────────────────────────────────────┤")
    print(f"│  Baseline (CHARLS only):     AUC = {baseline_auc:.4f}          │")
    print(f"│  Transfer (NHANES→CHARLS):   AUC = {transfer_auc:.4f}          │")
    print(f"│  Improvement:                {transfer_auc - baseline_auc:+.4f}          │")
    print(f"└─────────────────────────────────────────────────────┘")

    # Classification report for transfer model
    print("\nTransfer Model Classification Report:")
    transfer_binary = (transfer_pred > 0.5).astype(int)
    print(classification_report(y_test, transfer_binary))

    # ═══════════════════════════════════════════════════════════
    # Step 6: Save models
    # ═══════════════════════════════════════════════════════════

    print("\n[6/6] Saving models...")

    # Save transfer model
    transfer_path = OUTPUT_DIR / "rehealth_v2_transfer_charls.pkl"
    joblib.dump(transfer_model, transfer_path)
    print(f"  Transfer model: {transfer_path}")

    # Save baseline for comparison
    baseline_path = OUTPUT_DIR / "rehealth_v2_baseline_charls.pkl"
    joblib.dump(baseline_model, baseline_path)
    print(f"  Baseline model: {baseline_path}")

    # Save config
    config_path = OUTPUT_DIR / "transfer_config.txt"
    with open(config_path, "w") as f:
        f.write(f"source_domain: NHANES 2015-2016\n")
        f.write(f"target_domain: CHARLS\n")
        f.write(f"features: {len(V2_FEATURES)}\n")
        f.write(f"baseline_auc: {baseline_auc:.4f}\n")
        f.write(f"transfer_auc: {transfer_auc:.4f}\n")
        f.write(f"improvement: {transfer_auc - baseline_auc:+.4f}\n")

    print(f"  Config: {config_path}")

    # ═══════════════════════════════════════════════════════════
    # Additional: Cross-validation on CHARLS
    # ═══════════════════════════════════════════════════════════

    print("\n" + "=" * 60)
    print("Cross-validation on CHARLS (5-fold)")
    print("=" * 60)

    cv = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)
    baseline_aucs = []
    transfer_aucs = []

    for fold, (train_idx, val_idx) in enumerate(cv.split(X_charls, y_charls), 1):
        X_fold_train = X_charls.iloc[train_idx]
        y_fold_train = y_charls[train_idx]
        X_fold_val = X_charls.iloc[val_idx]
        y_fold_val = y_charls[val_idx]

        # Baseline
        m1 = CatBoostClassifier(iterations=300, learning_rate=0.05, depth=6, verbose=0, random_seed=42)
        m1.fit(X_fold_train, y_fold_train)
        p1 = m1.predict_proba(X_fold_val)[:, 1]
        baseline_aucs.append(roc_auc_score(y_fold_val, p1))

        # Transfer
        m2 = CatBoostClassifier(
            iterations=300, learning_rate=0.01, depth=6, verbose=0, random_seed=42,
        )
        m2.fit(X_fold_train, y_fold_train, init_model=pretrained_model)
        p2 = m2.predict_proba(X_fold_val)[:, 1]
        transfer_aucs.append(roc_auc_score(y_fold_val, p2))

        print(f"  Fold {fold}: Baseline={baseline_aucs[-1]:.4f}, Transfer={transfer_aucs[-1]:.4f}")

    print(f"\n  Mean Baseline AUC: {np.mean(baseline_aucs):.4f} ± {np.std(baseline_aucs):.4f}")
    print(f"  Mean Transfer AUC: {np.mean(transfer_aucs):.4f} ± {np.std(transfer_aucs):.4f}")
    print(f"  Mean Improvement:  {np.mean(transfer_aucs) - np.mean(baseline_aucs):+.4f}")

    return {
        "baseline_auc": baseline_auc,
        "transfer_auc": transfer_auc,
        "cv_baseline_auc": np.mean(baseline_aucs),
        "cv_transfer_auc": np.mean(transfer_aucs),
        "model_path": str(transfer_path),
    }


if __name__ == "__main__":
    results = train_transfer_model()

    print("\n" + "=" * 60)
    print("Transfer Learning Complete!")
    print("=" * 60)
    print(f"\nBest model saved to: {results['model_path']}")
    print("\nNext steps:")
    print("1. Use transfer model for Chinese users")
    print("2. Update PIAS_MODEL_PATH to transfer model")
    print("3. Push to GitHub")
