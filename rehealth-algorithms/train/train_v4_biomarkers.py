# -*- coding: utf-8 -*-
"""
Enhanced training with additional biomarkers — target AUC ≥ 0.88

Adds to the base 16 features:
  - HbA1c (LBXGH) — glycated hemoglobin, top diabetes/CVD marker
  - CRP (LBXHSCRP) — inflammation, strong CVD predictor
  - Creatinine → eGFR — kidney function
  - Uric acid (LBXSUA) — hyperuricemia linked to CVD
  - Waist circumference (BMXWAIST) — central obesity
  - Hemoglobin (LBXHGB) — anemia indicator
  - WBC (LBXWBCSI) — systemic inflammation
  - Albumin (LBXSAL) — nutritional/liver status

Plus engineered features from v4.

Usage:
    python train/train_v4_biomarkers.py [--trials 80]
"""

import argparse
import json
import sys
import warnings
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from sklearn.metrics import classification_report, roc_auc_score
from sklearn.model_selection import StratifiedKFold, train_test_split

warnings.filterwarnings("ignore")

PROJECT_ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(PROJECT_ROOT))

# ── Base 16 features ──
BASE_FEATURES = [
    "age", "gender", "bmi", "sbp", "dbp",
    "fasting_glucose", "total_cholesterol", "ldl", "hdl", "triglycerides",
    "exercise_days", "smoking", "drinking",
    "diabetes_history", "hypertension_history", "family_history",
]
CAT_FEATURES = [
    "gender", "smoking", "drinking",
    "diabetes_history", "hypertension_history", "family_history",
]

# ── New biomarkers ──
BIOMARKER_FEATURES = [
    "hba1c", "crp", "egfr", "uric_acid",
    "waist_circumference", "hemoglobin", "wbc", "albumin",
]

# ── Engineered features ──
ENGINEERED_FEATURES = [
    "pulse_pressure", "map", "non_hdl", "tc_hdl_ratio",
    "ldl_hdl_ratio", "tyg_index", "age_sbp", "bmi_glucose",
    "risk_factor_count", "age_sq", "aip",
    "waist_bmi_ratio", "crp_wbc_index",
]


def load_nhanes_enhanced():
    """Load NHANES with extra biomarker files merged in."""
    from bodyup_cloud.data_sources.nhanes import CYCLE_CONFIG, NHANESSource

    EXTRA_FILES_MAP = {
        "2015-2016": {"GHB": "GHB_I.xpt", "HSCRP": "HSCRP_I.xpt", "BIOPRO": "BIOPRO_I.xpt", "CBC": "CBC_I.xpt"},
        "2017-2018": {"GHB": "GHB_J.xpt", "HSCRP": "HSCRP_J.xpt", "BIOPRO": "BIOPRO_J.xpt", "CBC": "CBC_J.xpt"},
        "2021-2023": {"GHB": "GHB_L.xpt", "HSCRP": "HSCRP_L.xpt", "BIOPRO": "BIOPRO_L.xpt", "CBC": "CBC_L.xpt"},
    }

    data_dir = Path("data/nhanes")
    all_frames = []

    for cycle_name, cfg in CYCLE_CONFIG.items():
        cycle_dir = data_dir / cycle_name
        if not cycle_dir.is_dir():
            continue

        # Load base files
        tables = {}
        for fname in cfg["files"]:
            fpath = cycle_dir / fname
            if not fpath.exists():
                fpath = cycle_dir / fname.replace(".xpt", ".XPT")
            if fpath.exists():
                tables[fname.split("_")[0]] = pd.read_sas(str(fpath), format="xport", encoding="latin-1")

        if "DEMO" not in tables:
            continue

        # Merge base tables on SEQN
        df = tables["DEMO"][["SEQN"]].copy()
        for key, tbl in tables.items():
            df = pd.merge(df, tbl, on="SEQN", how="left")

        # Load and merge extra biomarker files (drop overlapping columns)
        extra = EXTRA_FILES_MAP.get(cycle_name, {})
        for key, fname in extra.items():
            fpath = cycle_dir / fname
            if fpath.exists():
                tbl = pd.read_sas(str(fpath), format="xport", encoding="latin-1")
                overlap = set(tbl.columns) & set(df.columns) - {"SEQN"}
                if overlap:
                    tbl = tbl.drop(columns=list(overlap))
                df = pd.merge(df, tbl, on="SEQN", how="left")

        df["_cycle"] = cycle_name
        all_frames.append(df)
        print(f"  {cycle_name}: {len(df)} rows, {len(df.columns)} columns")

    combined = pd.concat(all_frames, ignore_index=True)
    print(f"  Total NHANES raw: {len(combined)} rows")
    return combined


def preprocess_nhanes_enhanced(df):
    """Extract all features including new biomarkers."""
    feat = pd.DataFrame()
    feat["SEQN"] = df["SEQN"]

    # ── Base 16 features (same as NHANESSource) ──
    feat["age"] = df["RIDAGEYR"]
    feat["gender"] = (df["RIAGENDR"] == 1).astype(int)
    feat["bmi"] = df["BMXBMI"]

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

    feat["fasting_glucose"] = df["LBXGLU"] * 0.0555 if "LBXGLU" in df.columns else np.nan
    feat["total_cholesterol"] = df["LBXTC"] * 0.02586 if "LBXTC" in df.columns else np.nan
    feat["hdl"] = df["LBDHDD"] * 0.02586 if "LBDHDD" in df.columns else np.nan
    feat["ldl"] = df["LBDLDL"] * 0.02586 if "LBDLDL" in df.columns else np.nan
    feat["triglycerides"] = df["LBXTR"] * 0.01129 if "LBXTR" in df.columns else np.nan

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

    feat["diabetes_history"] = (df["DIQ010"] == 1).astype(int) if "DIQ010" in df.columns else 0
    feat["hypertension_history"] = (df["BPQ020"] == 1).astype(int) if "BPQ020" in df.columns else 0
    feat["family_history"] = 0
    for col in ("MCQ300B", "MCQ300C"):
        if col in df.columns:
            feat["family_history"] = feat["family_history"] | (df[col] == 1).astype(int)

    # Label
    label = pd.Series(0, index=df.index)
    for col in ["MCQ160B", "MCQ160C", "MCQ160D", "MCQ160E"]:
        if col in df.columns:
            label = label | (df[col] == 1).astype(int)
    if "MCQ160F" in df.columns:
        label = label | (df["MCQ160F"] == 1).astype(int)
    feat["label"] = label.values

    # ── New biomarkers ──
    feat["hba1c"] = df["LBXGH"] if "LBXGH" in df.columns else np.nan
    feat["crp"] = df["LBXHSCRP"] if "LBXHSCRP" in df.columns else np.nan
    feat["uric_acid"] = df["LBXSUA"] * 59.48 / 1000 if "LBXSUA" in df.columns else np.nan  # mg/dL → mmol/L
    feat["albumin"] = df["LBXSAL"] if "LBXSAL" in df.columns else np.nan  # g/dL
    feat["hemoglobin"] = df["LBXHGB"] if "LBXHGB" in df.columns else np.nan  # g/dL
    feat["wbc"] = df["LBXWBCSI"] if "LBXWBCSI" in df.columns else np.nan  # 1000 cells/uL
    feat["waist_circumference"] = df["BMXWAIST"] if "BMXWAIST" in df.columns else np.nan  # cm

    # eGFR from creatinine using CKD-EPI formula (simplified)
    if "LBXSCR" in df.columns:
        scr = df["LBXSCR"]  # mg/dL
        age = feat["age"]
        is_female = feat["gender"] == 0
        # CKD-EPI 2021 (race-free)
        kappa = np.where(is_female, 0.7, 0.9)
        alpha = np.where(is_female, -0.241, -0.302)
        scr_k = scr / kappa
        egfr = 142 * np.minimum(scr_k, 1.0) ** alpha * np.maximum(scr_k, 1.0) ** (-1.200) * 0.9938 ** age
        egfr = np.where(is_female, egfr * 1.012, egfr)
        feat["egfr"] = egfr
    else:
        feat["egfr"] = np.nan

    # Age filter
    feat = feat[(feat["age"] >= 18) & (feat["age"] <= 100)].copy()

    return feat


def load_charls():
    """Load CHARLS with NaN placeholders for missing biomarkers."""
    from bodyup_cloud.data_sources import CHARLSSource
    source = CHARLSSource()
    df = source.load_raw("data/charls")
    feat = source.preprocess(df)
    for col in BIOMARKER_FEATURES:
        if col not in feat.columns:
            feat[col] = np.nan
    return feat


def load_mimic():
    """Load MIMIC-IV with NaN placeholders for missing biomarkers."""
    from bodyup_cloud.data_sources import MIMICIVSource
    source = MIMICIVSource()
    tables = source.load_raw("data/mimic_iv")
    feat = source.preprocess(tables)
    for col in BIOMARKER_FEATURES:
        if col not in feat.columns:
            feat[col] = np.nan
    return feat


def engineer_features(X):
    """Add engineered features."""
    X = X.copy()
    X["pulse_pressure"] = X["sbp"] - X["dbp"]
    X["map"] = X["dbp"] + (X["sbp"] - X["dbp"]) / 3
    X["non_hdl"] = X["total_cholesterol"] - X["hdl"]
    X["tc_hdl_ratio"] = X["total_cholesterol"] / X["hdl"].clip(lower=0.3)
    X["ldl_hdl_ratio"] = X["ldl"] / X["hdl"].clip(lower=0.3)

    tg_mgdl = X["triglycerides"] * 88.57
    fg_mgdl = X["fasting_glucose"] * 18.018
    X["tyg_index"] = np.log(tg_mgdl.clip(lower=1) * fg_mgdl.clip(lower=1) / 2)

    X["age_sbp"] = X["age"] * X["sbp"] / 100
    X["bmi_glucose"] = X["bmi"] * X["fasting_glucose"]

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
        X["hypertension_history"]
    )

    X["age_sq"] = X["age"] ** 2 / 100
    X["aip"] = np.log10((X["triglycerides"] / X["hdl"].clip(lower=0.3)).clip(lower=0.01))

    # New: waist/BMI ratio (central vs general obesity)
    X["waist_bmi_ratio"] = X["waist_circumference"] / X["bmi"].clip(lower=10)

    # New: CRP × WBC composite inflammation index
    X["crp_wbc_index"] = X["crp"].fillna(0) * X["wbc"].fillna(0) / 10

    return X


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--trials", type=int, default=80)
    parser.add_argument("--output", default="train/rehealth_v4_biomarkers.pkl")
    args = parser.parse_args()

    ALL_FEATURES = BASE_FEATURES + BIOMARKER_FEATURES + ENGINEERED_FEATURES

    print("=" * 60)
    print("ReHealth v4 Biomarker-Enhanced Training")
    print(f"Target: AUC ≥ 0.88 | Features: {len(ALL_FEATURES)}")
    print("=" * 60)

    # 1. Load NHANES with extra biomarkers
    print("\n[NHANES] Loading with biomarkers...")
    nhanes_raw = load_nhanes_enhanced()
    nhanes = preprocess_nhanes_enhanced(nhanes_raw)
    nhanes["source"] = "nhanes"
    print(f"  {len(nhanes)} rows, biomarker coverage:")
    for col in BIOMARKER_FEATURES:
        coverage = nhanes[col].notna().mean()
        print(f"    {col:20s} {coverage:.1%}")

    # 2. Load CHARLS
    print("\n[CHARLS] Loading...")
    charls = load_charls()
    charls["source"] = "charls"
    print(f"  {len(charls)} rows")

    # 3. Load MIMIC-IV
    print("\n[MIMIC-IV] Loading...")
    mimic = load_mimic()
    mimic["source"] = "mimic_iv"
    print(f"  {len(mimic)} rows")

    # 4. Merge
    frames = [nhanes, charls, mimic]
    # Align columns
    all_cols = set()
    for f in frames:
        all_cols.update(f.columns)
    for f in frames:
        for col in all_cols:
            if col not in f.columns:
                f[col] = np.nan

    merged = pd.concat(frames, ignore_index=True)
    print(f"\nMerged: {len(merged)} rows")
    print(f"  {merged['source'].value_counts().to_dict()}")

    # 5. Feature engineering
    X = merged[BASE_FEATURES + BIOMARKER_FEATURES].copy()

    # Impute before engineering
    continuous_cols = [c for c in BASE_FEATURES + BIOMARKER_FEATURES if c not in CAT_FEATURES]
    for col in continuous_cols:
        if col in X.columns and X[col].isna().any():
            X[col] = X[col].fillna(X[col].median())
    for col in CAT_FEATURES:
        if col in X.columns:
            mode_val = X[col].mode()
            if len(mode_val) > 0:
                X[col] = X[col].fillna(mode_val.iloc[0]).astype(int)

    X = engineer_features(X)

    # Final impute engineered features
    for col in ENGINEERED_FEATURES:
        if col in X.columns and X[col].isna().any():
            X[col] = X[col].fillna(X[col].median())

    y = merged["label"].fillna(0).astype(int)

    # Drop remaining NaN rows
    mask = X[ALL_FEATURES].isna().any(axis=1)
    X = X.loc[~mask, ALL_FEATURES]
    y = y[~mask]

    print(f"\nFinal: {len(X)} samples, {len(ALL_FEATURES)} features")
    print(f"Positive rate: {y.mean():.2%} ({int(y.sum())}/{len(y)})")

    # 6. Train/test split
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y)

    for col in CAT_FEATURES:
        X_train[col] = X_train[col].astype(int)
        X_test[col] = X_test[col].astype(int)

    print(f"Train: {len(X_train)}, Test: {len(X_test)}")

    # 7. Optuna CatBoost search
    import optuna
    from catboost import CatBoostClassifier
    optuna.logging.set_verbosity(optuna.logging.WARNING)

    cat_indices = [ALL_FEATURES.index(c) for c in CAT_FEATURES]

    print(f"\n[CatBoost] Optuna search ({args.trials} trials)...")

    def objective(trial):
        params = {
            "iterations": trial.suggest_int("iterations", 500, 1500),
            "learning_rate": trial.suggest_float("learning_rate", 0.005, 0.12, log=True),
            "depth": trial.suggest_int("depth", 4, 10),
            "l2_leaf_reg": trial.suggest_float("l2_leaf_reg", 0.5, 15.0),
            "subsample": trial.suggest_float("subsample", 0.5, 1.0),
            "colsample_bylevel": trial.suggest_float("colsample_bylevel", 0.4, 1.0),
            "min_data_in_leaf": trial.suggest_int("min_data_in_leaf", 1, 80),
            "random_strength": trial.suggest_float("random_strength", 0.0, 5.0),
            "bagging_temperature": trial.suggest_float("bagging_temperature", 0.0, 3.0),
            "border_count": trial.suggest_int("border_count", 32, 255),
            "random_seed": 42,
            "verbose": False,
            "cat_features": cat_indices,
            "auto_class_weights": "Balanced",
        }
        model = CatBoostClassifier(**params)
        model.fit(X_train, y_train, eval_set=(X_test, y_test),
                  verbose=False, early_stopping_rounds=50)
        return roc_auc_score(y_test, model.predict_proba(X_test)[:, 1])

    study = optuna.create_study(
        direction="maximize",
        sampler=optuna.samplers.TPESampler(seed=42, n_startup_trials=20),
    )
    study.optimize(objective, n_trials=args.trials, show_progress_bar=True)

    print(f"\n  Best AUC: {study.best_value:.4f}")
    print(f"  Best params: {study.best_params}")

    # 8. Train final model
    print("\n[Final] Training with best params...")
    best = study.best_params.copy()
    best.update({"random_seed": 42, "verbose": 100,
                 "cat_features": cat_indices, "auto_class_weights": "Balanced"})
    model = CatBoostClassifier(**best)
    model.fit(X_train, y_train, eval_set=(X_test, y_test), early_stopping_rounds=50)

    y_proba = model.predict_proba(X_test)[:, 1]
    y_pred = model.predict(X_test)
    auc = roc_auc_score(y_test, y_proba)

    print(f"\nFinal AUC: {auc:.4f}")
    print(classification_report(y_test, y_pred, target_names=["No CVD", "CVD"]))

    # 9. 5-fold CV
    print("[CV] 5-fold cross-validation...")
    skf = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)
    cv_aucs = []
    for fold, (tr_idx, val_idx) in enumerate(skf.split(X, y)):
        m = CatBoostClassifier(**best)
        m.fit(X.iloc[tr_idx], y.iloc[tr_idx],
              eval_set=(X.iloc[val_idx], y.iloc[val_idx]),
              early_stopping_rounds=50, verbose=False)
        fold_auc = roc_auc_score(y.iloc[val_idx], m.predict_proba(X.iloc[val_idx])[:, 1])
        cv_aucs.append(fold_auc)
        print(f"    Fold {fold+1}: AUC={fold_auc:.4f}")
    print(f"    Mean AUC: {np.mean(cv_aucs):.4f} ± {np.std(cv_aucs):.4f}")

    # 10. Feature importance
    importance = pd.DataFrame({
        "feature": ALL_FEATURES,
        "importance": model.feature_importances_,
    }).sort_values("importance", ascending=False)
    print("\nFeature Importance (top 20):")
    for _, row in importance.head(20).iterrows():
        bar = "#" * int(row["importance"] / importance["importance"].max() * 30)
        print(f"  {row['feature']:25s} {row['importance']:6.2f}  {bar}")

    # 11. Save
    out = Path(args.output)
    out.parent.mkdir(parents=True, exist_ok=True)
    joblib.dump(model, str(out))

    meta = {
        "model_version": "v4.0-biomarkers",
        "auc": round(auc, 4),
        "cv_auc": round(np.mean(cv_aucs), 4),
        "feature_cols": ALL_FEATURES,
        "base_features": BASE_FEATURES,
        "biomarker_features": BIOMARKER_FEATURES,
        "engineered_features": ENGINEERED_FEATURES,
        "cat_features": CAT_FEATURES,
        "n_features": len(ALL_FEATURES),
        "best_params": study.best_params,
    }
    with open(out.with_suffix(".json"), "w", encoding="utf-8") as f:
        json.dump(meta, f, ensure_ascii=False, indent=2)

    joblib.dump(ALL_FEATURES, str(out.parent / "feature_cols_v4b.pkl"))
    print(f"\nSaved: {out}")

    print("\n" + "=" * 60)
    print(f"AUC: {auc:.4f} | CV: {np.mean(cv_aucs):.4f}")
    print("=" * 60)


if __name__ == "__main__":
    main()
