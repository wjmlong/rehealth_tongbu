# -*- coding: utf-8 -*-
"""
统一多源训练管道 — NHANES + CHARLS + MIMIC-IV → CatBoost

Usage:
    python train/train_unified.py [options]

Options:
    --nhanes-dir    data/nhanes      NHANES XPT 文件目录
    --charls-dir    data/charls      CHARLS 数据目录 (可选)
    --mimic-dir     data/mimic_iv    MIMIC-IV 数据目录 (可选)
    --output        train/rehealth_v3_unified.pkl  输出模型路径
    --trials        20               Optuna 调参轮数
    --no-smote                       不使用 SMOTE 过采样

数据源优先级:
  - NHANES: 必须 (主训练源)
  - CHARLS: 可选 (中国人群校准)
  - MIMIC-IV: 可选 (高频时序补充)

缺少 CHARLS/MIMIC-IV 时自动降级为仅 NHANES 训练。
"""

import argparse
import json
import sys
import warnings
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from catboost import CatBoostClassifier
from imblearn.over_sampling import SMOTE
from sklearn.metrics import classification_report, roc_auc_score, roc_curve
from sklearn.model_selection import train_test_split

warnings.filterwarnings("ignore")

# Add project root to path
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


def load_sources(nhanes_dir: str, charls_dir: str | None, mimic_dir: str | None) -> dict[str, pd.DataFrame]:
    """Load all available data sources and return standardized DataFrames."""
    registry = DataSourceRegistry()
    registry.register(NHANESSource())
    registry.register(CHARLSSource())
    registry.register(MIMICIVSource())

    datasets: dict[str, pd.DataFrame] = {}

    # NHANES — required
    nhanes_path = Path(nhanes_dir)
    if not nhanes_path.is_dir():
        print(f"[ERROR] NHANES directory not found: {nhanes_path}")
        print("Run: python train/download_nhanes.py")
        sys.exit(1)
    print("[NHANES] Loading...")
    df = registry.load_and_preprocess("nhanes", str(nhanes_path))
    report = registry.validate_source("nhanes", df)
    print(f"  rows={report['row_count']}, label_positive={df['label'].sum() if 'label' in df.columns else 'N/A'}")
    datasets["nhanes"] = df

    # CHARLS — optional
    if charls_dir and Path(charls_dir).is_dir():
        print("[CHARLS] Loading...")
        try:
            df = registry.load_and_preprocess("charls", charls_dir)
            report = registry.validate_source("charls", df)
            print(f"  rows={report['row_count']}")
            datasets["charls"] = df
        except NotImplementedError as e:
            print(f"  [skip] CHARLS not yet implemented: {e.args[0].splitlines()[0]}")
    else:
        print("[CHARLS] Not available — skipping")

    # MIMIC-IV — optional
    if mimic_dir and Path(mimic_dir).is_dir():
        print("[MIMIC-IV] Loading...")
        try:
            df = registry.load_and_preprocess("mimic_iv", mimic_dir)
            report = registry.validate_source("mimic_iv", df)
            print(f"  rows={report['row_count']}")
            datasets["mimic_iv"] = df
        except NotImplementedError as e:
            print(f"  [skip] MIMIC-IV not yet implemented: {e.args[0].splitlines()[0]}")
    else:
        print("[MIMIC-IV] Not available — skipping")

    return datasets


def merge_and_prepare(datasets: dict[str, pd.DataFrame]) -> tuple[pd.DataFrame, pd.Series]:
    """Merge all source DataFrames and split into features/labels."""
    registry = DataSourceRegistry()
    for name in datasets:
        # Register dummy sources for merge validation
        if name == "nhanes":
            registry.register(NHANESSource())
        elif name == "charls":
            registry.register(CHARLSSource())
        elif name == "mimic_iv":
            registry.register(MIMICIVSource())

    merged = registry.merge_datasets(datasets)

    source_counts = merged["source"].value_counts()
    print(f"\nMerged dataset: {len(merged)} rows")
    for src, cnt in source_counts.items():
        print(f"  {src}: {cnt} rows ({cnt/len(merged)*100:.1f}%)")

    X = merged[FEATURE_COLS].copy()
    y = merged["label"].copy() if "label" in merged.columns else pd.Series(0, index=merged.index)

    # Final NaN cleanup — median for continuous, mode for categorical
    for col in CONTINUOUS_FEATURES:
        if X[col].isna().any():
            X[col] = X[col].fillna(X[col].median())
    for col in CATEGORICAL_FEATURES:
        if X[col].isna().any():
            X[col] = X[col].fillna(X[col].mode().iloc[0]).astype(int)

    remaining_nan = X.isna().sum().sum()
    if remaining_nan > 0:
        print(f"  Warning: {remaining_nan} NaN values remaining, dropping rows")
        mask = X.isna().any(axis=1)
        X = X[~mask]
        y = y[~mask]

    print(f"Positive rate: {y.mean():.2%} ({int(y.sum())}/{len(y)})")
    return X, y


def train_catboost(
    X: pd.DataFrame,
    y: pd.Series,
    n_trials: int = 20,
    use_smote: bool = True,
) -> tuple[CatBoostClassifier, float, dict]:
    """Train CatBoost with Optuna hyperparameter search."""
    import optuna
    optuna.logging.set_verbosity(optuna.logging.WARNING)

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y,
    )
    print(f"\nTrain: {len(X_train)}, Test: {len(X_test)}")

    if use_smote:
        print("Applying SMOTE...")
        smote = SMOTE(random_state=42, k_neighbors=5)
        X_train_res, y_train_res = smote.fit_resample(X_train, y_train)
        print(f"After SMOTE: {len(X_train_res)} samples, positive rate: {y_train_res.mean():.2%}")
    else:
        X_train_res, y_train_res = X_train, y_train

    for col in CAT_FEATURE_NAMES:
        X_train_res[col] = X_train_res[col].astype(int)
        X_test[col] = X_test[col].astype(int)

    # Optuna search
    print(f"\nOptuna search ({n_trials} trials)...")

    def objective(trial):
        params = {
            "iterations": trial.suggest_int("iterations", 300, 700),
            "learning_rate": trial.suggest_float("learning_rate", 0.01, 0.15, log=True),
            "depth": trial.suggest_int("depth", 4, 8),
            "l2_leaf_reg": trial.suggest_int("l2_leaf_reg", 1, 10),
            "subsample": trial.suggest_float("subsample", 0.6, 1.0),
            "colsample_bylevel": trial.suggest_float("colsample_bylevel", 0.6, 1.0),
            "random_seed": 42,
            "verbose": False,
            "cat_features": CAT_FEATURE_NAMES,
        }
        model = CatBoostClassifier(**params)
        model.fit(
            X_train_res, y_train_res,
            eval_set=(X_test, y_test),
            verbose=False,
            early_stopping_rounds=30,
        )
        y_proba = model.predict_proba(X_test)[:, 1]
        return roc_auc_score(y_test, y_proba)

    study = optuna.create_study(
        direction="maximize",
        sampler=optuna.samplers.TPESampler(seed=42),
    )
    study.optimize(objective, n_trials=n_trials, show_progress_bar=True)

    print(f"\nBest AUC: {study.best_value:.4f}")
    print(f"Best params: {study.best_params}")

    # Train final model
    print("\nTraining final model...")
    best_params = study.best_params.copy()
    best_params.update({
        "random_seed": 42,
        "verbose": 100,
        "cat_features": CAT_FEATURE_NAMES,
    })

    final_model = CatBoostClassifier(**best_params)
    final_model.fit(
        X_train_res, y_train_res,
        eval_set=(X_test, y_test),
        early_stopping_rounds=50,
    )

    y_proba = final_model.predict_proba(X_test)[:, 1]
    y_pred = final_model.predict(X_test)
    auc = roc_auc_score(y_test, y_proba)

    print(f"\nFinal AUC: {auc:.4f}")
    print(classification_report(y_test, y_pred, target_names=["No CVD", "CVD"]))

    importance = pd.DataFrame({
        "feature": FEATURE_COLS,
        "importance": final_model.feature_importances_,
    }).sort_values("importance", ascending=False)

    print("Feature Importance:")
    for _, row in importance.iterrows():
        bar = "#" * int(row["importance"] / importance["importance"].max() * 30)
        print(f"  {row['feature']:25s} {row['importance']:6.2f}  {bar}")

    return final_model, auc, study.best_params


def save_model(model: CatBoostClassifier, auc: float, best_params: dict, output_path: str, sources: list[str]):
    """Save model, metadata, and feature info."""
    out = Path(output_path)
    out.parent.mkdir(parents=True, exist_ok=True)

    joblib.dump(model, str(out))
    print(f"\nSaved model: {out}")

    meta_path = out.with_suffix(".json")
    meta = {
        "model_version": "v3.0-unified",
        "auc": round(auc, 4),
        "feature_cols": FEATURE_COLS,
        "cat_features": CAT_FEATURE_NAMES,
        "n_features": len(FEATURE_COLS),
        "data_sources": sources,
        "best_params": best_params,
        "unit_note": "glucose/lipids in mmol/L, BP in mmHg, BMI in kg/m2",
    }
    with open(meta_path, "w", encoding="utf-8") as f:
        json.dump(meta, f, ensure_ascii=False, indent=2)
    print(f"Saved metadata: {meta_path}")

    joblib.dump(FEATURE_COLS, str(out.parent / "feature_cols_v3.pkl"))
    joblib.dump(CAT_FEATURE_NAMES, str(out.parent / "cat_cols_v3.pkl"))


def main():
    parser = argparse.ArgumentParser(description="Unified multi-source CatBoost training")
    parser.add_argument("--nhanes-dir", default="data/nhanes", help="NHANES data directory")
    parser.add_argument("--charls-dir", default="data/charls", help="CHARLS data directory")
    parser.add_argument("--mimic-dir", default="data/mimic_iv", help="MIMIC-IV data directory")
    parser.add_argument("--output", default="train/rehealth_v3_unified.pkl", help="Output model path")
    parser.add_argument("--trials", type=int, default=20, help="Optuna trials")
    parser.add_argument("--no-smote", action="store_true", help="Disable SMOTE")
    args = parser.parse_args()

    print("=" * 60)
    print("ReHealth Unified Training Pipeline v3.0")
    print("Sources: NHANES + CHARLS + MIMIC-IV → CatBoost")
    print("=" * 60)

    # 1. Load
    datasets = load_sources(args.nhanes_dir, args.charls_dir, args.mimic_dir)

    # 2. Merge
    X, y = merge_and_prepare(datasets)

    # 3. Train
    model, auc, best_params = train_catboost(X, y, n_trials=args.trials, use_smote=not args.no_smote)

    # 4. Save
    save_model(model, auc, best_params, args.output, list(datasets.keys()))

    print("\n" + "=" * 60)
    print(f"Training complete! AUC={auc:.4f}")
    print(f"Model: {args.output}")
    print("=" * 60)


if __name__ == "__main__":
    main()
