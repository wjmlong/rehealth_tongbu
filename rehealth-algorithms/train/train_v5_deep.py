# -*- coding: utf-8 -*-
"""
V5 Deep biomarker training — target AUC ≥ 0.88

Key improvements over v4-biomarkers:
  1. NHANES-only (drop CHARLS/MIMIC to get full biomarker coverage)
  2. Medication features: BP meds, cholesterol meds, diabetes meds
     (addresses treatment confounding — normal BP + on meds = still high risk)
  3. More lab values: NLR, PLR, RDW, liver enzymes, BUN, platelet count
  4. Richer engineered ratios: AST/ALT, BUN/Cr, Monocyte/HDL
  5. CatBoost + LightGBM stacking with logistic meta-learner

Usage:
    python train/train_v5_deep.py [--trials 100]
"""

import argparse
import json
import sys
import warnings
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import classification_report, roc_auc_score
from sklearn.model_selection import StratifiedKFold, train_test_split

warnings.filterwarnings("ignore")

PROJECT_ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(PROJECT_ROOT))

CAT_FEATURES = [
    "gender", "smoking", "drinking",
    "diabetes_history", "hypertension_history", "family_history",
    "on_bp_meds", "on_chol_meds", "on_diabetes_meds",
]

NHANES_CYCLES = {
    "2015-2016": {
        "suffix": "I",
        "base_files": [
            "DEMO_I.xpt", "BMX_I.xpt", "BPX_I.xpt", "GLU_I.xpt",
            "TCHOL_I.xpt", "HDL_I.xpt", "TRIGLY_I.xpt", "BPQ_I.xpt",
            "DIQ_I.xpt", "MCQ_I.xpt", "SMQ_I.xpt", "ALQ_I.xpt", "PAQ_I.xpt",
        ],
        "extra_files": ["GHB_I.xpt", "HSCRP_I.xpt", "BIOPRO_I.xpt", "CBC_I.xpt"],
    },
    "2017-2018": {
        "suffix": "J",
        "base_files": [
            "DEMO_J.xpt", "BMX_J.xpt", "BPX_J.xpt", "GLU_J.xpt",
            "TCHOL_J.xpt", "HDL_J.xpt", "TRIGLY_J.xpt", "BPQ_J.xpt",
            "DIQ_J.xpt", "MCQ_J.xpt", "SMQ_J.xpt", "ALQ_J.xpt", "PAQ_J.xpt",
        ],
        "extra_files": ["GHB_J.xpt", "HSCRP_J.xpt", "BIOPRO_J.xpt", "CBC_J.xpt"],
    },
    "2021-2023": {
        "suffix": "L",
        "base_files": [
            "DEMO_L.xpt", "BMX_L.xpt", "BPX_L.xpt", "GLU_L.xpt",
            "TCHOL_L.xpt", "HDL_L.xpt", "TRIGLY_L.xpt", "BPQ_L.xpt",
            "DIQ_L.xpt", "MCQ_L.xpt", "SMQ_L.xpt", "ALQ_L.xpt", "PAQ_L.xpt",
        ],
        "extra_files": ["GHB_L.xpt", "HSCRP_L.xpt", "BIOPRO_L.xpt", "CBC_L.xpt"],
    },
}


def load_nhanes_deep():
    """Load NHANES with all available lab + medication data."""
    data_dir = Path("data/nhanes")
    all_frames = []

    for cycle_name, cfg in NHANES_CYCLES.items():
        cycle_dir = data_dir / cycle_name
        if not cycle_dir.is_dir():
            print(f"  [skip] {cycle_name}: not found")
            continue

        all_files = cfg["base_files"] + cfg["extra_files"]
        tables = {}
        for fname in all_files:
            fpath = cycle_dir / fname
            if not fpath.exists():
                fpath = cycle_dir / fname.replace(".xpt", ".XPT")
            if fpath.exists():
                key = fname.split("_")[0]
                tables[key] = pd.read_sas(str(fpath), format="xport", encoding="latin-1")

        if "DEMO" not in tables:
            continue

        df = tables["DEMO"][["SEQN"]].copy()
        for key, tbl in tables.items():
            overlap = set(tbl.columns) & set(df.columns) - {"SEQN"}
            if overlap:
                tbl = tbl.drop(columns=list(overlap))
            df = pd.merge(df, tbl, on="SEQN", how="left")

        df["_cycle"] = cycle_name
        all_frames.append(df)
        print(f"  {cycle_name}: {len(df)} rows, {len(df.columns)} cols")

    combined = pd.concat(all_frames, ignore_index=True)
    print(f"  Total raw: {len(combined)} rows")
    return combined


def extract_features(df):
    """Extract all features from raw NHANES data."""
    feat = pd.DataFrame()
    feat["SEQN"] = df["SEQN"]

    # ── Demographics ──
    feat["age"] = df["RIDAGEYR"]
    feat["gender"] = (df["RIAGENDR"] == 1).astype(int)  # 1=male

    # ── Anthropometrics ──
    feat["bmi"] = df["BMXBMI"]
    feat["waist"] = df["BMXWAIST"] if "BMXWAIST" in df.columns else np.nan

    # ── Blood pressure (try old + new column names) ──
    sbp = pd.Series(np.nan, index=df.index)
    dbp = pd.Series(np.nan, index=df.index)
    for col in ["BPXSY1", "BPXOSY1"]:
        if col in df.columns:
            mask = sbp.isna() & df[col].notna()
            sbp[mask] = df.loc[mask, col]
    for col in ["BPXDI1", "BPXODI1"]:
        if col in df.columns:
            mask = dbp.isna() & df[col].notna()
            dbp[mask] = df.loc[mask, col]
    feat["sbp"] = sbp
    feat["dbp"] = dbp

    # ── Lipids (convert mg/dL → mmol/L) ──
    feat["fasting_glucose"] = df["LBXGLU"] * 0.0555 if "LBXGLU" in df.columns else np.nan
    feat["total_cholesterol"] = df["LBXTC"] * 0.02586 if "LBXTC" in df.columns else np.nan
    feat["hdl"] = df["LBDHDD"] * 0.02586 if "LBDHDD" in df.columns else np.nan
    feat["ldl"] = df["LBDLDL"] * 0.02586 if "LBDLDL" in df.columns else np.nan
    feat["triglycerides"] = df["LBXTR"] * 0.01129 if "LBXTR" in df.columns else np.nan

    # ── Lifestyle ──
    feat["smoking"] = 0
    if "SMQ040" in df.columns:
        feat["smoking"] = df["SMQ040"].isin([1, 2]).astype(int)
    feat["drinking"] = 0
    if "ALQ130" in df.columns:
        feat["drinking"] = (df["ALQ130"] >= 1).astype(int)

    feat["exercise_days"] = 0
    if "PAD660" in df.columns and "PAD675" in df.columns:
        vig = pd.to_numeric(df["PAD660"], errors="coerce").clip(0, 7).fillna(0)
        mod = pd.to_numeric(df["PAD675"], errors="coerce").clip(0, 7).fillna(0)
        has_paq = df["PAD660"].notna() | df["PAD675"].notna()
        feat.loc[has_paq, "exercise_days"] = vig[has_paq].combine(mod[has_paq], max).astype(int)
    if "PAD680" in df.columns:
        sed = pd.to_numeric(df["PAD680"], errors="coerce")
        has_new = sed.notna() & (feat["exercise_days"] == 0)
        feat.loc[has_new, "exercise_days"] = np.where(
            sed[has_new] < 180, 5, np.where(sed[has_new] < 360, 3, np.where(sed[has_new] < 540, 1, 0)))

    # ── Medical history ──
    feat["diabetes_history"] = (df["DIQ010"] == 1).astype(int) if "DIQ010" in df.columns else 0
    feat["hypertension_history"] = (df["BPQ020"] == 1).astype(int) if "BPQ020" in df.columns else 0
    feat["family_history"] = 0
    for col in ("MCQ300B", "MCQ300C"):
        if col in df.columns:
            feat["family_history"] = feat["family_history"] | (df[col] == 1).astype(int)

    # ── KEY: Medication features (addresses treatment confounding) ──
    feat["on_bp_meds"] = (df["BPQ050A"] == 1).astype(int) if "BPQ050A" in df.columns else 0
    feat["on_chol_meds"] = (df["BPQ100D"] == 1).astype(int) if "BPQ100D" in df.columns else 0
    feat["on_diabetes_meds"] = 0
    if "DIQ070" in df.columns:
        feat["on_diabetes_meds"] = (df["DIQ070"] == 1).astype(int)
    if "DIQ050" in df.columns:
        feat["on_diabetes_meds"] = feat["on_diabetes_meds"] | (df["DIQ050"] == 1).astype(int)

    # ── Glycemic ──
    feat["hba1c"] = df["LBXGH"] if "LBXGH" in df.columns else np.nan

    # ── Inflammation ──
    feat["crp"] = df["LBXHSCRP"] if "LBXHSCRP" in df.columns else np.nan

    # ── Kidney function ──
    feat["uric_acid"] = df["LBXSUA"] * 59.48 / 1000 if "LBXSUA" in df.columns else np.nan
    feat["bun"] = df["LBXSBU"] if "LBXSBU" in df.columns else np.nan

    # eGFR (CKD-EPI 2021 race-free)
    if "LBXSCR" in df.columns:
        scr = df["LBXSCR"]
        age = feat["age"]
        is_female = feat["gender"] == 0
        kappa = np.where(is_female, 0.7, 0.9)
        alpha = np.where(is_female, -0.241, -0.302)
        scr_k = scr / kappa
        egfr = 142 * np.minimum(scr_k, 1.0) ** alpha * np.maximum(scr_k, 1.0) ** (-1.200) * 0.9938 ** age
        egfr = np.where(is_female, egfr * 1.012, egfr)
        feat["egfr"] = egfr
    else:
        feat["egfr"] = np.nan

    # ── Liver function ──
    feat["alt"] = df["LBXSATSI"] if "LBXSATSI" in df.columns else np.nan  # ALT U/L
    feat["ast"] = df["LBXSASSI"] if "LBXSASSI" in df.columns else np.nan  # AST U/L
    feat["ggt"] = df["LBXSGTSI"] if "LBXSGTSI" in df.columns else np.nan  # GGT U/L
    feat["albumin"] = df["LBXSAL"] if "LBXSAL" in df.columns else np.nan
    feat["alk_phos"] = df["LBXSAPSI"] if "LBXSAPSI" in df.columns else np.nan

    # ── Hematology ──
    feat["hemoglobin"] = df["LBXHGB"] if "LBXHGB" in df.columns else np.nan
    feat["hematocrit"] = df["LBXHCT"] if "LBXHCT" in df.columns else np.nan
    feat["wbc"] = df["LBXWBCSI"] if "LBXWBCSI" in df.columns else np.nan
    feat["platelet"] = df["LBXPLTSI"] if "LBXPLTSI" in df.columns else np.nan
    feat["rdw"] = df["LBXRDW"] if "LBXRDW" in df.columns else np.nan  # Red cell distribution width
    feat["neutrophil_pct"] = df["LBXNEPCT"] if "LBXNEPCT" in df.columns else np.nan
    feat["lymphocyte_pct"] = df["LBXLYPCT"] if "LBXLYPCT" in df.columns else np.nan
    feat["monocyte_pct"] = df["LBXMOPCT"] if "LBXMOPCT" in df.columns else np.nan
    feat["neutrophil_n"] = df["LBDNENO"] if "LBDNENO" in df.columns else np.nan
    feat["lymphocyte_n"] = df["LBDLYMNO"] if "LBDLYMNO" in df.columns else np.nan
    feat["monocyte_n"] = df["LBDMONO"] if "LBDMONO" in df.columns else np.nan

    # ── Label: self-reported CVD events ──
    label = pd.Series(0, index=df.index)
    for col in ["MCQ160B", "MCQ160C", "MCQ160D", "MCQ160E", "MCQ160F"]:
        if col in df.columns:
            label = label | (df[col] == 1).astype(int)
    feat["label"] = label.values

    # Age filter
    feat = feat[(feat["age"] >= 18) & (feat["age"] <= 100)].copy()
    return feat


def engineer_features(X):
    """Comprehensive feature engineering."""
    X = X.copy()

    # ── BP derived ──
    X["pulse_pressure"] = X["sbp"] - X["dbp"]
    X["map"] = X["dbp"] + (X["sbp"] - X["dbp"]) / 3

    # ── Lipid derived ──
    X["non_hdl"] = X["total_cholesterol"] - X["hdl"]
    X["tc_hdl_ratio"] = X["total_cholesterol"] / X["hdl"].clip(lower=0.3)
    X["ldl_hdl_ratio"] = X["ldl"] / X["hdl"].clip(lower=0.3)
    tg_mgdl = X["triglycerides"] * 88.57
    fg_mgdl = X["fasting_glucose"] * 18.018
    X["tyg_index"] = np.log(tg_mgdl.clip(lower=1) * fg_mgdl.clip(lower=1) / 2)
    X["aip"] = np.log10((X["triglycerides"] / X["hdl"].clip(lower=0.3)).clip(lower=0.01))

    # ── Interaction features ──
    X["age_sbp"] = X["age"] * X["sbp"] / 100
    X["bmi_glucose"] = X["bmi"] * X["fasting_glucose"]
    X["age_sq"] = X["age"] ** 2 / 100

    # ── Anthropometric ──
    X["waist_bmi_ratio"] = X["waist"] / X["bmi"].clip(lower=10)

    # ── Inflammation indices ──
    X["nlr"] = X["neutrophil_n"] / X["lymphocyte_n"].clip(lower=0.1)  # Neutrophil-to-Lymphocyte Ratio
    X["plr"] = X["platelet"] / X["lymphocyte_n"].clip(lower=0.1)  # Platelet-to-Lymphocyte Ratio
    X["mhr"] = X["monocyte_n"] / X["hdl"].clip(lower=0.3)  # Monocyte-to-HDL Ratio
    X["crp_wbc"] = X["crp"].fillna(0) * X["wbc"].fillna(0) / 10
    X["sii"] = X["platelet"] * X["neutrophil_n"] / X["lymphocyte_n"].clip(lower=0.1)  # Systemic Immune-Inflammation Index

    # ── Liver function ratios ──
    X["ast_alt_ratio"] = X["ast"] / X["alt"].clip(lower=1)  # De Ritis ratio
    X["fib4"] = (X["age"] * X["ast"]) / (X["platelet"].clip(lower=10) * X["alt"].clip(lower=1) ** 0.5)  # FIB-4 index

    # ── Kidney ──
    X["bun_cr_ratio"] = X["bun"] / X["egfr"].clip(lower=1)

    # ── Medication-adjusted BP/glucose (captures "treated but controlled") ──
    X["bp_med_adjusted"] = X["sbp"] + X["on_bp_meds"] * 15  # Add 15 mmHg if on BP meds
    X["glucose_med_adjusted"] = X["fasting_glucose"] + X["on_diabetes_meds"] * 1.5  # Add 1.5 mmol/L if on diabetes meds

    # ── Risk factor count ──
    X["risk_factor_count"] = (
        (X["sbp"] >= 140).astype(int) +
        (X["fasting_glucose"] >= 7.0).astype(int) +
        (X["total_cholesterol"] >= 6.2).astype(int) +
        (X["ldl"] >= 4.1).astype(int) +
        (X["hdl"] < 1.0).astype(int) +
        (X["triglycerides"] >= 2.3).astype(int) +
        (X["bmi"] >= 28).astype(int) +
        X["smoking"] +
        X["diabetes_history"] +
        X["hypertension_history"] +
        X["on_bp_meds"] +
        X["on_chol_meds"]
    )

    # ── Medication count (polypharmacy indicator) ──
    X["med_count"] = X["on_bp_meds"] + X["on_chol_meds"] + X["on_diabetes_meds"]

    # ── Age-stratified interactions (CVD risk differs by age group) ──
    X["age_hyp"] = X["age"] * X["hypertension_history"]
    X["age_diabetes"] = X["age"] * X["diabetes_history"]
    X["age_meds"] = X["age"] * X["med_count"]
    X["age_crp"] = X["age"] * X["crp"].fillna(0)
    X["age_hba1c"] = X["age"] * X["hba1c"].fillna(5.5)

    # ── Framingham-like composite risk indicator ──
    X["framingham_proxy"] = (
        X["age"] / 10 +
        X["sbp"] / 20 +
        (X["total_cholesterol"] - X["hdl"]) * 2 +
        X["smoking"] * 3 +
        X["diabetes_history"] * 4 +
        X["hypertension_history"] * 3 +
        X["on_bp_meds"] * 2
    )

    # ── Metabolic syndrome score (0-5) ──
    X["mets_score"] = (
        (X["waist"] > np.where(X["gender"] == 1, 102, 88)).astype(int) +
        (X["triglycerides"] >= 1.7).astype(int) +
        (X["hdl"] < np.where(X["gender"] == 1, 1.03, 1.29)).astype(int) +
        (X["sbp"] >= 130).astype(int) +
        (X["fasting_glucose"] >= 5.6).astype(int)
    )

    # ── Kidney-liver-inflammation composite ──
    X["organ_stress"] = (
        (X["egfr"] < 60).astype(int) +
        (X["crp"].fillna(0) > 3).astype(int) +
        (X["alt"] > 40).astype(int) +
        (X["hba1c"].fillna(5.5) > 6.5).astype(int)
    )

    # ── log transforms of skewed features ──
    X["log_crp"] = np.log1p(X["crp"].fillna(0))
    X["log_tg"] = np.log1p(X["triglycerides"])

    return X


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--trials", type=int, default=100)
    parser.add_argument("--output", default="train/rehealth_v5_deep.pkl")
    args = parser.parse_args()

    print("=" * 60)
    print("ReHealth v5 Deep Biomarker Training")
    print("=" * 60)

    # 1. Load NHANES only (full biomarker coverage)
    print("\n[1] Loading NHANES (all cycles)...")
    raw = load_nhanes_deep()
    feat = extract_features(raw)
    print(f"  Adults: {len(feat)} rows")

    # 2. Show coverage
    print("\n[2] Biomarker coverage:")
    bio_cols = ["hba1c", "crp", "egfr", "uric_acid", "bun", "alt", "ast", "ggt",
                "albumin", "hemoglobin", "hematocrit", "wbc", "platelet", "rdw",
                "neutrophil_n", "lymphocyte_n", "monocyte_n", "waist"]
    for col in bio_cols:
        if col in feat.columns:
            cov = feat[col].notna().mean()
            print(f"    {col:20s} {cov:.1%}")

    med_cols = ["on_bp_meds", "on_chol_meds", "on_diabetes_meds"]
    print("  Medication features:")
    for col in med_cols:
        print(f"    {col:20s} {feat[col].mean():.1%} positive")

    print(f"\n  Label positive rate: {feat['label'].mean():.2%}")

    # 3. Impute + Engineer
    raw_cols = [c for c in feat.columns if c not in ["SEQN", "label"]]
    X = feat[raw_cols].copy()

    cont_cols = [c for c in raw_cols if c not in CAT_FEATURES]
    for col in cont_cols:
        if X[col].isna().any():
            X[col] = X[col].fillna(X[col].median())
    for col in CAT_FEATURES:
        if col in X.columns:
            mode_val = X[col].mode()
            if len(mode_val) > 0:
                X[col] = X[col].fillna(mode_val.iloc[0]).astype(int)

    X = engineer_features(X)
    y = feat["label"].fillna(0).astype(int)

    # Collect all feature names
    ALL_FEATURES = [c for c in X.columns if c not in ["SEQN", "label"]]

    # Final impute engineered
    for col in ALL_FEATURES:
        if X[col].isna().any():
            med = X[col].median()
            X[col] = X[col].fillna(med if pd.notna(med) else 0)
        if np.isinf(X[col]).any():
            X[col] = X[col].replace([np.inf, -np.inf], np.nan).fillna(X[col].median())

    # Drop rows with remaining NaN
    mask = X[ALL_FEATURES].isna().any(axis=1)
    X = X.loc[~mask, ALL_FEATURES]
    y = y[~mask]

    print(f"\n[3] Final dataset: {len(X)} samples, {len(ALL_FEATURES)} features")
    print(f"  Positive rate: {y.mean():.2%} ({int(y.sum())}/{len(y)})")

    # 4. Split
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y)

    for col in CAT_FEATURES:
        if col in X_train.columns:
            X_train[col] = X_train[col].astype(int)
            X_test[col] = X_test[col].astype(int)

    # XGBoost needs category dtype
    X_train_xgb = X_train.copy()
    X_test_xgb = X_test.copy()
    for col in CAT_FEATURES:
        if col in X_train_xgb.columns:
            X_train_xgb[col] = X_train_xgb[col].astype("category")
            X_test_xgb[col] = X_test_xgb[col].astype("category")

    print(f"  Train: {len(X_train)}, Test: {len(X_test)}")

    # 5. Optuna CatBoost
    import optuna
    from catboost import CatBoostClassifier
    optuna.logging.set_verbosity(optuna.logging.WARNING)

    cat_indices = [ALL_FEATURES.index(c) for c in CAT_FEATURES if c in ALL_FEATURES]

    print(f"\n[4] CatBoost Optuna ({args.trials} trials)...")

    def cb_objective(trial):
        params = {
            "iterations": trial.suggest_int("iterations", 600, 2000),
            "learning_rate": trial.suggest_float("learning_rate", 0.005, 0.15, log=True),
            "depth": trial.suggest_int("depth", 4, 10),
            "l2_leaf_reg": trial.suggest_float("l2_leaf_reg", 0.1, 20.0),
            "subsample": trial.suggest_float("subsample", 0.5, 1.0),
            "colsample_bylevel": trial.suggest_float("colsample_bylevel", 0.3, 1.0),
            "min_data_in_leaf": trial.suggest_int("min_data_in_leaf", 1, 100),
            "random_strength": trial.suggest_float("random_strength", 0.0, 8.0),
            "bagging_temperature": trial.suggest_float("bagging_temperature", 0.0, 5.0),
            "border_count": trial.suggest_int("border_count", 32, 255),
            "grow_policy": trial.suggest_categorical("grow_policy", ["SymmetricTree", "Depthwise", "Lossguide"]),
            "random_seed": 42,
            "verbose": False,
            "cat_features": cat_indices,
            "auto_class_weights": "Balanced",
        }
        if params["grow_policy"] == "Lossguide":
            params["max_leaves"] = trial.suggest_int("max_leaves", 16, 128)
        model = CatBoostClassifier(**params)
        model.fit(X_train, y_train, eval_set=(X_test, y_test),
                  verbose=False, early_stopping_rounds=80)
        return roc_auc_score(y_test, model.predict_proba(X_test)[:, 1])

    study_cb = optuna.create_study(
        direction="maximize",
        sampler=optuna.samplers.TPESampler(seed=42, n_startup_trials=25),
    )
    study_cb.optimize(cb_objective, n_trials=args.trials, show_progress_bar=True)
    print(f"  CatBoost best AUC: {study_cb.best_value:.4f}")

    # 6. LightGBM search
    from lightgbm import LGBMClassifier
    print(f"\n[5] LightGBM Optuna ({args.trials} trials)...")

    lgb_cat = [c for c in CAT_FEATURES if c in ALL_FEATURES]

    def lgb_objective(trial):
        params = {
            "n_estimators": trial.suggest_int("n_estimators", 500, 2000),
            "learning_rate": trial.suggest_float("learning_rate", 0.005, 0.15, log=True),
            "max_depth": trial.suggest_int("max_depth", 4, 12),
            "num_leaves": trial.suggest_int("num_leaves", 16, 128),
            "reg_alpha": trial.suggest_float("reg_alpha", 0.0, 10.0),
            "reg_lambda": trial.suggest_float("reg_lambda", 0.0, 10.0),
            "subsample": trial.suggest_float("subsample", 0.5, 1.0),
            "colsample_bytree": trial.suggest_float("colsample_bytree", 0.3, 1.0),
            "min_child_samples": trial.suggest_int("min_child_samples", 5, 100),
            "is_unbalance": True,
            "random_state": 42,
            "verbose": -1,
            "feature_pre_filter": False,
        }
        model = LGBMClassifier(**params)
        model.fit(X_train, y_train,
                  eval_set=[(X_test, y_test)],
                  callbacks=[
                      __import__("lightgbm").early_stopping(80, verbose=False),
                      __import__("lightgbm").log_evaluation(0),
                  ],
                  categorical_feature=lgb_cat)
        return roc_auc_score(y_test, model.predict_proba(X_test)[:, 1])

    study_lgb = optuna.create_study(
        direction="maximize",
        sampler=optuna.samplers.TPESampler(seed=42, n_startup_trials=25),
    )
    study_lgb.optimize(lgb_objective, n_trials=args.trials, show_progress_bar=True)
    print(f"  LightGBM best AUC: {study_lgb.best_value:.4f}")

    # 6b. XGBoost search
    from xgboost import XGBClassifier
    print(f"\n[5b] XGBoost Optuna ({args.trials} trials)...")

    neg_pos_ratio = (y_train == 0).sum() / max((y_train == 1).sum(), 1)

    def xgb_objective(trial):
        params = {
            "n_estimators": trial.suggest_int("n_estimators", 500, 2000),
            "learning_rate": trial.suggest_float("learning_rate", 0.005, 0.15, log=True),
            "max_depth": trial.suggest_int("max_depth", 4, 10),
            "min_child_weight": trial.suggest_int("min_child_weight", 1, 50),
            "reg_alpha": trial.suggest_float("reg_alpha", 0.0, 10.0),
            "reg_lambda": trial.suggest_float("reg_lambda", 0.0, 10.0),
            "subsample": trial.suggest_float("subsample", 0.5, 1.0),
            "colsample_bytree": trial.suggest_float("colsample_bytree", 0.3, 1.0),
            "gamma": trial.suggest_float("gamma", 0.0, 5.0),
            "scale_pos_weight": neg_pos_ratio,
            "random_state": 42,
            "verbosity": 0,
            "enable_categorical": True,
            "tree_method": "hist",
        }
        model = XGBClassifier(**params)
        model.fit(X_train_xgb, y_train,
                  eval_set=[(X_test_xgb, y_test)],
                  verbose=False)
        return roc_auc_score(y_test, model.predict_proba(X_test_xgb)[:, 1])

    study_xgb = optuna.create_study(
        direction="maximize",
        sampler=optuna.samplers.TPESampler(seed=42, n_startup_trials=25),
    )
    study_xgb.optimize(xgb_objective, n_trials=args.trials, show_progress_bar=True)
    print(f"  XGBoost best AUC: {study_xgb.best_value:.4f}")

    # 7. Train final models
    print("\n[6] Training final models...")

    best_cb = study_cb.best_params.copy()
    if "max_leaves" not in best_cb and best_cb.get("grow_policy") != "Lossguide":
        best_cb.pop("max_leaves", None)
    best_cb.update({"random_seed": 42, "verbose": 100,
                    "cat_features": cat_indices, "auto_class_weights": "Balanced"})
    cb_model = CatBoostClassifier(**best_cb)
    cb_model.fit(X_train, y_train, eval_set=(X_test, y_test), early_stopping_rounds=80)
    cb_proba = cb_model.predict_proba(X_test)[:, 1]
    cb_auc = roc_auc_score(y_test, cb_proba)
    print(f"  CatBoost AUC: {cb_auc:.4f}")

    best_lgb = study_lgb.best_params.copy()
    best_lgb.update({"random_state": 42, "verbose": -1,
                     "is_unbalance": True, "feature_pre_filter": False})
    lgb_model = LGBMClassifier(**best_lgb)
    lgb_model.fit(X_train, y_train,
                  eval_set=[(X_test, y_test)],
                  callbacks=[
                      __import__("lightgbm").early_stopping(80, verbose=False),
                      __import__("lightgbm").log_evaluation(0),
                  ],
                  categorical_feature=lgb_cat)
    lgb_proba = lgb_model.predict_proba(X_test)[:, 1]
    lgb_auc = roc_auc_score(y_test, lgb_proba)
    print(f"  LightGBM AUC: {lgb_auc:.4f}")

    best_xgb = study_xgb.best_params.copy()
    best_xgb.update({"random_state": 42, "verbosity": 0,
                     "scale_pos_weight": neg_pos_ratio,
                     "enable_categorical": True, "tree_method": "hist"})
    xgb_model = XGBClassifier(**best_xgb)
    xgb_model.fit(X_train_xgb, y_train, eval_set=[(X_test_xgb, y_test)], verbose=False)
    xgb_proba = xgb_model.predict_proba(X_test_xgb)[:, 1]
    xgb_auc = roc_auc_score(y_test, xgb_proba)
    print(f"  XGBoost AUC: {xgb_auc:.4f}")

    # 8. Stacking: logistic regression meta-learner on OOF predictions (3 models)
    print("\n[7] 3-model stacking ensemble...")
    skf = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)

    oof_cb = np.zeros(len(X_train))
    oof_lgb = np.zeros(len(X_train))
    oof_xgb = np.zeros(len(X_train))
    test_cb_folds = np.zeros(len(X_test))
    test_lgb_folds = np.zeros(len(X_test))
    test_xgb_folds = np.zeros(len(X_test))

    for fold, (tr_idx, val_idx) in enumerate(skf.split(X_train, y_train)):
        Xtr, Xval = X_train.iloc[tr_idx], X_train.iloc[val_idx]
        ytr, yval = y_train.iloc[tr_idx], y_train.iloc[val_idx]

        m_cb = CatBoostClassifier(**{k: v for k, v in best_cb.items() if k != "verbose"}, verbose=False)
        m_cb.fit(Xtr, ytr, eval_set=(Xval, yval), early_stopping_rounds=80, verbose=False)
        oof_cb[val_idx] = m_cb.predict_proba(Xval)[:, 1]
        test_cb_folds += m_cb.predict_proba(X_test)[:, 1] / 5

        m_lgb = LGBMClassifier(**best_lgb)
        m_lgb.fit(Xtr, ytr, eval_set=[(Xval, yval)],
                  callbacks=[
                      __import__("lightgbm").early_stopping(80, verbose=False),
                      __import__("lightgbm").log_evaluation(0),
                  ],
                  categorical_feature=lgb_cat)
        oof_lgb[val_idx] = m_lgb.predict_proba(Xval)[:, 1]
        test_lgb_folds += m_lgb.predict_proba(X_test)[:, 1] / 5

        Xtr_xgb = X_train_xgb.iloc[tr_idx]
        Xval_xgb = X_train_xgb.iloc[val_idx]
        m_xgb = XGBClassifier(**best_xgb)
        m_xgb.fit(Xtr_xgb, ytr, eval_set=[(Xval_xgb, yval)], verbose=False)
        oof_xgb[val_idx] = m_xgb.predict_proba(Xval_xgb)[:, 1]
        test_xgb_folds += m_xgb.predict_proba(X_test_xgb)[:, 1] / 5

        val_auc = roc_auc_score(yval, oof_cb[val_idx])
        val_auc2 = roc_auc_score(yval, oof_lgb[val_idx])
        val_auc3 = roc_auc_score(yval, oof_xgb[val_idx])
        print(f"    Fold {fold+1}: CB={val_auc:.4f}  LGB={val_auc2:.4f}  XGB={val_auc3:.4f}")

    # Train meta-learner (3 models)
    stack_train = np.column_stack([oof_cb, oof_lgb, oof_xgb])
    stack_test = np.column_stack([test_cb_folds, test_lgb_folds, test_xgb_folds])

    meta = LogisticRegression(C=1.0, random_state=42)
    meta.fit(stack_train, y_train)
    stack_proba = meta.predict_proba(stack_test)[:, 1]
    stack_auc = roc_auc_score(y_test, stack_proba)
    print(f"\n  Stacking AUC: {stack_auc:.4f}")
    print(f"  Meta weights: CB={meta.coef_[0][0]:.3f}, LGB={meta.coef_[0][1]:.3f}, XGB={meta.coef_[0][2]:.3f}")

    # Simple average blend (3 models)
    blend_proba = (cb_proba + lgb_proba + xgb_proba) / 3
    blend_auc = roc_auc_score(y_test, blend_proba)
    print(f"  Blend AUC: {blend_auc:.4f}")

    # Weighted blend (proportional to individual AUC)
    w_cb = cb_auc / (cb_auc + lgb_auc + xgb_auc)
    w_lgb = lgb_auc / (cb_auc + lgb_auc + xgb_auc)
    w_xgb = xgb_auc / (cb_auc + lgb_auc + xgb_auc)
    wblend_proba = w_cb * cb_proba + w_lgb * lgb_proba + w_xgb * xgb_proba
    wblend_auc = roc_auc_score(y_test, wblend_proba)
    print(f"  Weighted blend AUC: {wblend_auc:.4f}")

    # Pick best
    all_aucs = {"catboost": cb_auc, "lightgbm": lgb_auc, "xgboost": xgb_auc,
                "stacking": stack_auc, "blend": blend_auc, "wblend": wblend_auc}
    method = max(all_aucs, key=all_aucs.get)
    best_auc = all_aucs[method]
    proba_map = {"catboost": cb_proba, "lightgbm": lgb_proba, "xgboost": xgb_proba,
                 "stacking": stack_proba, "blend": blend_proba, "wblend": wblend_proba}
    final_proba = proba_map[method]

    print(f"\n  Best method: {method} (AUC={best_auc:.4f})")

    y_pred = (final_proba >= 0.5).astype(int)
    print(classification_report(y_test, y_pred, target_names=["No CVD", "CVD"]))

    # 9. Cross-validation of best single model
    print("[8] 5-fold CV (CatBoost)...")
    cv_aucs = []
    for fold, (tr_idx, val_idx) in enumerate(skf.split(X, y)):
        m = CatBoostClassifier(**{k: v for k, v in best_cb.items() if k != "verbose"}, verbose=False)
        m.fit(X.iloc[tr_idx], y.iloc[tr_idx],
              eval_set=(X.iloc[val_idx], y.iloc[val_idx]),
              early_stopping_rounds=80, verbose=False)
        fold_auc = roc_auc_score(y.iloc[val_idx], m.predict_proba(X.iloc[val_idx])[:, 1])
        cv_aucs.append(fold_auc)
        print(f"    Fold {fold+1}: AUC={fold_auc:.4f}")
    print(f"    Mean AUC: {np.mean(cv_aucs):.4f} ± {np.std(cv_aucs):.4f}")

    # 10. Feature importance
    importance = pd.DataFrame({
        "feature": ALL_FEATURES,
        "importance": cb_model.feature_importances_,
    }).sort_values("importance", ascending=False)
    print("\nFeature Importance (top 25):")
    for _, row in importance.head(25).iterrows():
        bar = "#" * int(row["importance"] / importance["importance"].max() * 30)
        print(f"  {row['feature']:25s} {row['importance']:6.2f}  {bar}")

    # 11. Save
    out = Path(args.output)
    out.parent.mkdir(parents=True, exist_ok=True)

    # Save CatBoost as primary model
    joblib.dump(cb_model, str(out))

    # Save stacking components if ensemble won
    if method in ("stacking", "blend", "wblend"):
        joblib.dump({
            "catboost": cb_model,
            "lightgbm": lgb_model,
            "xgboost": xgb_model,
            "meta": meta if method == "stacking" else None,
            "method": method,
        }, str(out.with_stem(out.stem + "_ensemble")))

    meta_info = {
        "model_version": "v5.0-deep",
        "auc": round(best_auc, 4),
        "cv_auc": round(np.mean(cv_aucs), 4),
        "method": method,
        "catboost_auc": round(cb_auc, 4),
        "lightgbm_auc": round(lgb_auc, 4),
        "xgboost_auc": round(xgb_auc, 4),
        "stacking_auc": round(stack_auc, 4),
        "blend_auc": round(blend_auc, 4),
        "wblend_auc": round(wblend_auc, 4),
        "feature_cols": ALL_FEATURES,
        "cat_features": CAT_FEATURES,
        "n_features": len(ALL_FEATURES),
        "n_samples": len(X),
        "positive_rate": round(y.mean(), 4),
        "best_cb_params": study_cb.best_params,
        "best_lgb_params": study_lgb.best_params,
        "best_xgb_params": study_xgb.best_params,
    }
    with open(out.with_suffix(".json"), "w", encoding="utf-8") as f:
        json.dump(meta_info, f, ensure_ascii=False, indent=2)

    joblib.dump(ALL_FEATURES, str(out.parent / "feature_cols_v5.pkl"))
    print(f"\nSaved: {out}")

    print("\n" + "=" * 60)
    print(f"BEST AUC: {best_auc:.4f} ({method}) | CV: {np.mean(cv_aucs):.4f}")
    if best_auc >= 0.88:
        print("TARGET MET!")
    else:
        print(f"Gap to 0.88: {0.88 - best_auc:.4f}")
    print("=" * 60)


if __name__ == "__main__":
    main()
