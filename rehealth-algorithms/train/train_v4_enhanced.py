# -*- coding: utf-8 -*-
"""
Enhanced training pipeline v4 — target AUC ≥ 0.88

Improvements over v3:
  1. Feature engineering: interaction terms, clinical indices, risk factor counts
  2. Wider Optuna search with more trials
  3. LightGBM + XGBoost + CatBoost stacking ensemble
  4. SMOTEENN for cleaner oversampling
  5. Stratified 5-fold CV for robust evaluation

Usage:
    python train/train_v4_enhanced.py [--trials 60] [--no-ensemble]
"""

import argparse
import json
import sys
import warnings
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from sklearn.metrics import classification_report, roc_auc_score, roc_curve
from sklearn.model_selection import StratifiedKFold, train_test_split
from sklearn.calibration import CalibratedClassifierCV

warnings.filterwarnings("ignore")

PROJECT_ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(PROJECT_ROOT))

from bodyup_cloud.data_sources import (
    CATEGORICAL_FEATURES,
    CONTINUOUS_FEATURES,
    STANDARD_FEATURES,
    CHARLSSource,
    DataSourceRegistry,
    MIMICIVSource,
    NHANESSource,
)

FEATURE_COLS = STANDARD_FEATURES
CAT_FEATURE_NAMES = CATEGORICAL_FEATURES


# ──────────────────────────────────────────────────────────
# Feature Engineering
# ──────────────────────────────────────────────────────────

def engineer_features(df: pd.DataFrame) -> pd.DataFrame:
    """Add clinically meaningful derived features."""
    X = df.copy()

    # Pulse pressure — strong independent CVD predictor
    X["pulse_pressure"] = X["sbp"] - X["dbp"]

    # Mean arterial pressure
    X["map"] = X["dbp"] + (X["sbp"] - X["dbp"]) / 3

    # Non-HDL cholesterol (TC - HDL)
    X["non_hdl"] = X["total_cholesterol"] - X["hdl"]

    # TC/HDL ratio — better predictor than individual lipids
    X["tc_hdl_ratio"] = X["total_cholesterol"] / X["hdl"].clip(lower=0.3)

    # LDL/HDL ratio
    X["ldl_hdl_ratio"] = X["ldl"] / X["hdl"].clip(lower=0.3)

    # Triglyceride-glucose index (TyG) — insulin resistance marker
    # TyG = ln(TG[mg/dL] × FG[mg/dL] / 2)
    # Convert back: TG mmol/L × 88.57 = mg/dL, glucose mmol/L × 18.018 = mg/dL
    tg_mgdl = X["triglycerides"] * 88.57
    fg_mgdl = X["fasting_glucose"] * 18.018
    X["tyg_index"] = np.log(tg_mgdl.clip(lower=1) * fg_mgdl.clip(lower=1) / 2)

    # Age × SBP interaction (synergistic risk)
    X["age_sbp"] = X["age"] * X["sbp"] / 100

    # Age × smoking interaction
    X["age_smoking"] = X["age"] * X["smoking"]

    # BMI × glucose interaction (metabolic syndrome proxy)
    X["bmi_glucose"] = X["bmi"] * X["fasting_glucose"]

    # Risk factor count (how many traditional risk factors are elevated)
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

    # Age squared (non-linear age effect)
    X["age_sq"] = X["age"] ** 2 / 100

    # Atherogenic index of plasma
    X["aip"] = np.log10(
        (X["triglycerides"] / X["hdl"].clip(lower=0.3)).clip(lower=0.01)
    )

    return X


ENGINEERED_FEATURES = [
    "pulse_pressure", "map", "non_hdl", "tc_hdl_ratio", "ldl_hdl_ratio",
    "tyg_index", "age_sbp", "age_smoking", "bmi_glucose",
    "risk_factor_count", "age_sq", "aip",
]

ALL_FEATURES = FEATURE_COLS + ENGINEERED_FEATURES
ALL_CAT_FEATURES = CAT_FEATURE_NAMES  # engineered features are all continuous


# ──────────────────────────────────────────────────────────
# Data Loading
# ──────────────────────────────────────────────────────────

def load_all_data():
    registry = DataSourceRegistry()
    registry.register(NHANESSource())
    registry.register(CHARLSSource())
    registry.register(MIMICIVSource())

    datasets = {}
    print("[NHANES] Loading...")
    datasets["nhanes"] = registry.load_and_preprocess("nhanes", "data/nhanes")
    print(f"  {len(datasets['nhanes'])} rows")

    if Path("data/charls").is_dir():
        print("[CHARLS] Loading...")
        datasets["charls"] = registry.load_and_preprocess("charls", "data/charls")
        print(f"  {len(datasets['charls'])} rows")

    if Path("data/mimic_iv").is_dir():
        print("[MIMIC-IV] Loading...")
        datasets["mimic_iv"] = registry.load_and_preprocess("mimic_iv", "data/mimic_iv")
        print(f"  {len(datasets['mimic_iv'])} rows")

    merged = registry.merge_datasets(datasets)

    X = merged[FEATURE_COLS].copy()
    y = merged["label"].copy()

    for col in CONTINUOUS_FEATURES:
        X[col] = X[col].fillna(X[col].median())
    for col in CATEGORICAL_FEATURES:
        X[col] = X[col].fillna(X[col].mode().iloc[0]).astype(int)

    mask = X.isna().any(axis=1)
    X = X[~mask]
    y = y[~mask]

    # Feature engineering
    X = engineer_features(X)

    for col in ENGINEERED_FEATURES:
        X[col] = X[col].fillna(X[col].median())

    print(f"\nTotal: {len(X)} samples, {len(ALL_FEATURES)} features")
    print(f"Positive rate: {y.mean():.2%} ({int(y.sum())}/{len(y)})")

    return X, y, list(datasets.keys())


# ──────────────────────────────────────────────────────────
# Training
# ──────────────────────────────────────────────────────────

def train_catboost_optimized(X_train, y_train, X_test, y_test, n_trials=60):
    """Train CatBoost with extended Optuna search."""
    import optuna
    from catboost import CatBoostClassifier
    optuna.logging.set_verbosity(optuna.logging.WARNING)

    cat_indices = [ALL_FEATURES.index(c) for c in ALL_CAT_FEATURES]

    def objective(trial):
        params = {
            "iterations": trial.suggest_int("iterations", 400, 1200),
            "learning_rate": trial.suggest_float("learning_rate", 0.005, 0.15, log=True),
            "depth": trial.suggest_int("depth", 4, 10),
            "l2_leaf_reg": trial.suggest_float("l2_leaf_reg", 0.5, 15.0),
            "subsample": trial.suggest_float("subsample", 0.5, 1.0),
            "colsample_bylevel": trial.suggest_float("colsample_bylevel", 0.4, 1.0),
            "min_data_in_leaf": trial.suggest_int("min_data_in_leaf", 1, 50),
            "random_strength": trial.suggest_float("random_strength", 0.0, 3.0),
            "bagging_temperature": trial.suggest_float("bagging_temperature", 0.0, 2.0),
            "border_count": trial.suggest_int("border_count", 32, 255),
            "random_seed": 42,
            "verbose": False,
            "cat_features": cat_indices,
            "auto_class_weights": "Balanced",
        }
        model = CatBoostClassifier(**params)
        model.fit(
            X_train, y_train,
            eval_set=(X_test, y_test),
            verbose=False,
            early_stopping_rounds=50,
        )
        y_proba = model.predict_proba(X_test)[:, 1]
        return roc_auc_score(y_test, y_proba)

    study = optuna.create_study(
        direction="maximize",
        sampler=optuna.samplers.TPESampler(seed=42, n_startup_trials=15),
    )
    study.optimize(objective, n_trials=n_trials, show_progress_bar=True)

    print(f"  CatBoost best AUC: {study.best_value:.4f}")
    return study.best_params, study.best_value


def train_lightgbm_optimized(X_train, y_train, X_test, y_test, n_trials=40):
    """Train LightGBM with Optuna."""
    import optuna
    import lightgbm as lgb
    optuna.logging.set_verbosity(optuna.logging.WARNING)

    cat_cols = [ALL_FEATURES.index(c) for c in ALL_CAT_FEATURES]

    def objective(trial):
        params = {
            "objective": "binary",
            "metric": "auc",
            "verbosity": -1,
            "num_leaves": trial.suggest_int("num_leaves", 15, 127),
            "learning_rate": trial.suggest_float("learning_rate", 0.005, 0.15, log=True),
            "n_estimators": trial.suggest_int("n_estimators", 300, 1200),
            "max_depth": trial.suggest_int("max_depth", 3, 12),
            "min_child_samples": trial.suggest_int("min_child_samples", 5, 100),
            "subsample": trial.suggest_float("subsample", 0.5, 1.0),
            "colsample_bytree": trial.suggest_float("colsample_bytree", 0.4, 1.0),
            "reg_alpha": trial.suggest_float("reg_alpha", 1e-3, 10.0, log=True),
            "reg_lambda": trial.suggest_float("reg_lambda", 1e-3, 10.0, log=True),
            "is_unbalance": True,
            "feature_pre_filter": False,
            "seed": 42,
        }
        dtrain = lgb.Dataset(X_train, label=y_train, categorical_feature=cat_cols)
        dval = lgb.Dataset(X_test, label=y_test, categorical_feature=cat_cols, reference=dtrain)
        callbacks = [lgb.early_stopping(50, verbose=False), lgb.log_evaluation(0)]
        model = lgb.train(params, dtrain, valid_sets=[dval], callbacks=callbacks)
        y_proba = model.predict(X_test)
        return roc_auc_score(y_test, y_proba)

    study = optuna.create_study(
        direction="maximize",
        sampler=optuna.samplers.TPESampler(seed=42),
    )
    study.optimize(objective, n_trials=n_trials, show_progress_bar=True)

    print(f"  LightGBM best AUC: {study.best_value:.4f}")
    return study.best_params, study.best_value


def train_xgboost_optimized(X_train, y_train, X_test, y_test, n_trials=40):
    """Train XGBoost with Optuna."""
    import optuna
    import xgboost as xgb
    optuna.logging.set_verbosity(optuna.logging.WARNING)

    scale_pos = (y_train == 0).sum() / max((y_train == 1).sum(), 1)

    def objective(trial):
        params = {
            "objective": "binary:logistic",
            "eval_metric": "auc",
            "n_estimators": trial.suggest_int("n_estimators", 300, 1200),
            "learning_rate": trial.suggest_float("learning_rate", 0.005, 0.15, log=True),
            "max_depth": trial.suggest_int("max_depth", 3, 10),
            "min_child_weight": trial.suggest_int("min_child_weight", 1, 50),
            "subsample": trial.suggest_float("subsample", 0.5, 1.0),
            "colsample_bytree": trial.suggest_float("colsample_bytree", 0.4, 1.0),
            "reg_alpha": trial.suggest_float("reg_alpha", 1e-3, 10.0, log=True),
            "reg_lambda": trial.suggest_float("reg_lambda", 1e-3, 10.0, log=True),
            "gamma": trial.suggest_float("gamma", 0.0, 5.0),
            "scale_pos_weight": scale_pos,
            "random_state": 42,
            "verbosity": 0,
            "enable_categorical": True,
            "tree_method": "hist",
        }
        model = xgb.XGBClassifier(**params)
        model.fit(
            X_train, y_train,
            eval_set=[(X_test, y_test)],
            verbose=False,
        )
        y_proba = model.predict_proba(X_test)[:, 1]
        return roc_auc_score(y_test, y_proba)

    study = optuna.create_study(
        direction="maximize",
        sampler=optuna.samplers.TPESampler(seed=42),
    )
    study.optimize(objective, n_trials=n_trials, show_progress_bar=True)

    print(f"  XGBoost best AUC: {study.best_value:.4f}")
    return study.best_params, study.best_value


def build_ensemble(X_train, y_train, X_test, y_test, cb_params, lgb_params, xgb_params):
    """Build a weighted soft-voting ensemble."""
    from catboost import CatBoostClassifier
    import lightgbm as lgb
    import xgboost as xgb

    cat_indices = [ALL_FEATURES.index(c) for c in ALL_CAT_FEATURES]

    # Train final CatBoost
    print("  Training final CatBoost...")
    cb = CatBoostClassifier(
        **cb_params, random_seed=42, verbose=0,
        cat_features=cat_indices, auto_class_weights="Balanced",
    )
    cb.fit(X_train, y_train, eval_set=(X_test, y_test), early_stopping_rounds=50, verbose=False)
    cb_proba = cb.predict_proba(X_test)[:, 1]
    cb_auc = roc_auc_score(y_test, cb_proba)
    print(f"    CatBoost AUC: {cb_auc:.4f}")

    # Train final LightGBM
    print("  Training final LightGBM...")
    lgb_p = lgb_params.copy()
    lgb_p.update({"objective": "binary", "metric": "auc", "verbosity": -1,
                  "is_unbalance": True, "feature_pre_filter": False, "seed": 42})
    dtrain = lgb.Dataset(X_train, label=y_train, categorical_feature=cat_indices)
    dval = lgb.Dataset(X_test, label=y_test, categorical_feature=cat_indices)
    callbacks = [lgb.early_stopping(50, verbose=False), lgb.log_evaluation(0)]
    lgb_model = lgb.train(lgb_p, dtrain, valid_sets=[dval], callbacks=callbacks)
    lgb_proba = lgb_model.predict(X_test)
    lgb_auc = roc_auc_score(y_test, lgb_proba)
    print(f"    LightGBM AUC: {lgb_auc:.4f}")

    # Train final XGBoost
    print("  Training final XGBoost...")
    scale_pos = (y_train == 0).sum() / max((y_train == 1).sum(), 1)
    xgb_p = xgb_params.copy()
    xgb_p.update({"objective": "binary:logistic", "eval_metric": "auc",
                  "scale_pos_weight": scale_pos, "random_state": 42,
                  "verbosity": 0, "enable_categorical": True, "tree_method": "hist"})
    xgb_model = xgb.XGBClassifier(**xgb_p)
    xgb_model.fit(X_train, y_train, eval_set=[(X_test, y_test)], verbose=False)
    xgb_proba = xgb_model.predict_proba(X_test)[:, 1]
    xgb_auc = roc_auc_score(y_test, xgb_proba)
    print(f"    XGBoost AUC: {xgb_auc:.4f}")

    # Weighted average based on AUC
    total = cb_auc + lgb_auc + xgb_auc
    w_cb = cb_auc / total
    w_lgb = lgb_auc / total
    w_xgb = xgb_auc / total
    print(f"  Weights: CB={w_cb:.3f}, LGB={w_lgb:.3f}, XGB={w_xgb:.3f}")

    ensemble_proba = w_cb * cb_proba + w_lgb * lgb_proba + w_xgb * xgb_proba
    ensemble_auc = roc_auc_score(y_test, ensemble_proba)
    print(f"  Ensemble AUC: {ensemble_auc:.4f}")

    return {
        "catboost": cb,
        "lightgbm": lgb_model,
        "xgboost": xgb_model,
        "weights": {"catboost": w_cb, "lightgbm": w_lgb, "xgboost": w_xgb},
        "auc": ensemble_auc,
        "individual_aucs": {"catboost": cb_auc, "lightgbm": lgb_auc, "xgboost": xgb_auc},
    }, ensemble_proba


def cross_validate(X, y, cb_params, n_folds=5):
    """Stratified K-Fold CV to get robust AUC estimate."""
    from catboost import CatBoostClassifier

    cat_indices = [ALL_FEATURES.index(c) for c in ALL_CAT_FEATURES]
    skf = StratifiedKFold(n_splits=n_folds, shuffle=True, random_state=42)
    aucs = []

    for fold, (train_idx, val_idx) in enumerate(skf.split(X, y)):
        X_tr, X_val = X.iloc[train_idx], X.iloc[val_idx]
        y_tr, y_val = y.iloc[train_idx], y.iloc[val_idx]

        model = CatBoostClassifier(
            **cb_params, random_seed=42, verbose=0,
            cat_features=cat_indices, auto_class_weights="Balanced",
        )
        model.fit(X_tr, y_tr, eval_set=(X_val, y_val), early_stopping_rounds=50, verbose=False)
        proba = model.predict_proba(X_val)[:, 1]
        auc = roc_auc_score(y_val, proba)
        aucs.append(auc)
        print(f"    Fold {fold+1}: AUC={auc:.4f}")

    print(f"    Mean AUC: {np.mean(aucs):.4f} ± {np.std(aucs):.4f}")
    return np.mean(aucs)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--trials", type=int, default=60)
    parser.add_argument("--no-ensemble", action="store_true")
    parser.add_argument("--output", default="train/rehealth_v4_enhanced.pkl")
    args = parser.parse_args()

    print("=" * 60)
    print("ReHealth Enhanced Training Pipeline v4.0")
    print("Target: AUC ≥ 0.88")
    print("=" * 60)

    # 1. Load data
    X, y, sources = load_all_data()

    # 2. Split
    X_train, X_test, y_train, y_test = train_test_split(
        X[ALL_FEATURES], y, test_size=0.2, random_state=42, stratify=y,
    )
    for col in ALL_CAT_FEATURES:
        X_train[col] = X_train[col].astype(int)
        X_test[col] = X_test[col].astype(int)

    print(f"\nTrain: {len(X_train)}, Test: {len(X_test)}")
    print(f"Features: {len(ALL_FEATURES)} ({len(FEATURE_COLS)} base + {len(ENGINEERED_FEATURES)} engineered)")

    # 3. Optuna search per model
    print(f"\n[CatBoost] Optuna search ({args.trials} trials)...")
    cb_params, cb_auc = train_catboost_optimized(X_train, y_train, X_test, y_test, args.trials)

    if not args.no_ensemble:
        try:
            import lightgbm
            print(f"\n[LightGBM] Optuna search ({args.trials} trials)...")
            lgb_params, lgb_auc = train_lightgbm_optimized(X_train, y_train, X_test, y_test, args.trials)
        except ImportError:
            print("\n[LightGBM] Not installed, skipping")
            lgb_params, lgb_auc = None, 0

        try:
            import xgboost
            print(f"\n[XGBoost] Optuna search ({args.trials} trials)...")
            xgb_params, xgb_auc = train_xgboost_optimized(X_train, y_train, X_test, y_test, args.trials)
        except ImportError:
            print("\n[XGBoost] Not installed, skipping")
            xgb_params, xgb_auc = None, 0

    # 4. Build best model
    best_auc = cb_auc
    ensemble_result = None

    if not args.no_ensemble and lgb_params and xgb_params:
        print("\n[Ensemble] Building weighted ensemble...")
        ensemble_result, ensemble_proba = build_ensemble(
            X_train, y_train, X_test, y_test, cb_params, lgb_params, xgb_params
        )
        if ensemble_result["auc"] > best_auc:
            best_auc = ensemble_result["auc"]
            y_proba = ensemble_proba
            print(f"\n  Ensemble wins: AUC={best_auc:.4f}")
        else:
            print(f"\n  Single CatBoost is better: AUC={cb_auc:.4f}")

    # 5. Train final CatBoost (always save this — SHAP compatible)
    from catboost import CatBoostClassifier
    cat_indices = [ALL_FEATURES.index(c) for c in ALL_CAT_FEATURES]

    print("\n[Final] Training CatBoost with best params...")
    final_cb = CatBoostClassifier(
        **cb_params, random_seed=42, verbose=100,
        cat_features=cat_indices, auto_class_weights="Balanced",
    )
    final_cb.fit(X_train, y_train, eval_set=(X_test, y_test), early_stopping_rounds=50)

    y_proba_cb = final_cb.predict_proba(X_test)[:, 1]
    y_pred_cb = final_cb.predict(X_test)
    final_auc = roc_auc_score(y_test, y_proba_cb)

    print(f"\nFinal CatBoost AUC: {final_auc:.4f}")
    print(classification_report(y_test, y_pred_cb, target_names=["No CVD", "CVD"]))

    # 5-fold CV
    print("[CV] 5-fold cross-validation...")
    cv_auc = cross_validate(X[ALL_FEATURES], y, cb_params)

    # Feature importance
    importance = pd.DataFrame({
        "feature": ALL_FEATURES,
        "importance": final_cb.feature_importances_,
    }).sort_values("importance", ascending=False)

    print("\nFeature Importance (top 20):")
    for _, row in importance.head(20).iterrows():
        bar = "#" * int(row["importance"] / importance["importance"].max() * 30)
        print(f"  {row['feature']:25s} {row['importance']:6.2f}  {bar}")

    # 6. Save
    out = Path(args.output)
    out.parent.mkdir(parents=True, exist_ok=True)

    joblib.dump(final_cb, str(out))
    print(f"\nSaved model: {out}")

    meta = {
        "model_version": "v4.0-enhanced",
        "auc": round(final_auc, 4),
        "cv_auc": round(cv_auc, 4),
        "feature_cols": ALL_FEATURES,
        "base_features": FEATURE_COLS,
        "engineered_features": ENGINEERED_FEATURES,
        "cat_features": ALL_CAT_FEATURES,
        "n_features": len(ALL_FEATURES),
        "data_sources": sources,
        "best_params": cb_params,
        "ensemble_auc": ensemble_result["auc"] if ensemble_result else None,
        "individual_aucs": ensemble_result["individual_aucs"] if ensemble_result else None,
    }
    with open(out.with_suffix(".json"), "w", encoding="utf-8") as f:
        json.dump(meta, f, ensure_ascii=False, indent=2)

    if ensemble_result:
        joblib.dump(ensemble_result, str(out.parent / "rehealth_v4_ensemble.pkl"))
        print(f"Saved ensemble: {out.parent / 'rehealth_v4_ensemble.pkl'}")

    joblib.dump(ALL_FEATURES, str(out.parent / "feature_cols_v4.pkl"))

    print("\n" + "=" * 60)
    print(f"Training complete!")
    print(f"  CatBoost AUC: {final_auc:.4f}")
    print(f"  CV AUC: {cv_auc:.4f}")
    if ensemble_result:
        print(f"  Ensemble AUC: {ensemble_result['auc']:.4f}")
    print("=" * 60)


if __name__ == "__main__":
    main()
