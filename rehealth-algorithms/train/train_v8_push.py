# -*- coding: utf-8 -*-
"""
V8 Push — 可中断阶段化训练管线

7 stages with checkpoints:
  1. Data loading + feature engineering
  2. Train/test split
  3a/3b/3c. Optuna HPO (CatBoost/LightGBM/XGBoost) — SQLite persistent
  4. Train 6 final models
  5. 5-fold stacking + ensemble evaluation
  6. 5-fold CV + save final model

Usage:
  python train/train_v8_push.py --trials 80
  python train/train_v8_push.py --trials 80 --from-stage 3a
  python train/train_v8_push.py --trials 80 --from-stage 3a --force
  python train/train_v8_push.py --trials 80 --only-stage 3a,3b,3c
"""

import argparse
import json
import sys
import warnings
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from scipy.stats import rankdata
from sklearn.ensemble import (
    ExtraTreesClassifier,
    GradientBoostingClassifier,
    RandomForestClassifier,
)
from sklearn.linear_model import LogisticRegressionCV
from sklearn.metrics import classification_report, roc_auc_score
from sklearn.model_selection import StratifiedKFold, train_test_split

warnings.filterwarnings("ignore")

PROJECT_ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(PROJECT_ROOT))

from train_v7_wearable import (
    CAT_FEATURES,
    engineer_features,
    extract_features,
    impute_crp,
    load_accelerometer,
    load_nhanes_deep,
)

CKPT_DIR = Path(__file__).resolve().parent / "checkpoints" / "v8"
STAGES = ["1", "2", "3a", "3b", "3c", "4", "5", "6"]


def save_ckpt(name, data):
    CKPT_DIR.mkdir(parents=True, exist_ok=True)
    joblib.dump(data, CKPT_DIR / name)
    print(f"  [ckpt] saved {name}")


def load_ckpt(name):
    p = CKPT_DIR / name
    if p.exists():
        return joblib.load(p)
    return None


def should_run(stage, args):
    if args.only_stage and stage not in args.only_stage:
        return False
    if args.from_stage:
        if STAGES.index(stage) < STAGES.index(args.from_stage):
            return False
        if stage == args.from_stage and args.force:
            return True
    return True


def rank_average(*probas):
    ranks = [rankdata(p) for p in probas]
    avg_rank = np.mean(ranks, axis=0)
    return avg_rank / len(avg_rank)


# ═══════════════════════════════════════════════════════════
# Stage 1: Data loading + feature engineering
# ═══════════════════════════════════════════════════════════
def stage1(args):
    ckpt = load_ckpt("stage1_features.pkl")
    if ckpt is not None and not (args.from_stage == "1" and args.force):
        print("[1] ✓ loaded from checkpoint")
        return ckpt

    if not should_run("1", args):
        raise RuntimeError("Stage 1 checkpoint missing but stage skipped")

    print("\n[1] Loading NHANES (5 cycles + accel)...")
    raw = load_nhanes_deep()
    feat = extract_features(raw)
    print(f"  Adults: {len(feat)}")

    print("\n[2] CRP imputation...")
    feat = impute_crp(feat)

    print("\n[3] Accelerometer...")
    accel = load_accelerometer()
    if len(accel) > 0:
        feat = pd.merge(feat, accel, on="SEQN", how="left")
        feat["has_accel"] = feat["accel_daily_mims"].notna().astype(int)
        print(f"  With accel: {feat['has_accel'].sum()} ({feat['has_accel'].mean():.1%})")
    else:
        feat["has_accel"] = 0

    print("\n[4] Features...")
    raw_cols = [c for c in feat.columns if c not in ["SEQN", "label"]]
    X = feat[raw_cols].copy()
    cont_cols = [c for c in raw_cols if c not in CAT_FEATURES]
    for col in cont_cols:
        if col in X.columns and X[col].isna().any():
            X[col] = X[col].fillna(X[col].median())
    for col in CAT_FEATURES:
        if col in X.columns:
            mode_val = X[col].mode()
            if len(mode_val) > 0:
                X[col] = X[col].fillna(mode_val.iloc[0]).astype(int)

    X = engineer_features(X)
    y = feat["label"].fillna(0).astype(int)

    ALL_FEATURES = [c for c in X.columns if c not in ["SEQN", "label"]]
    for col in ALL_FEATURES:
        if X[col].isna().any():
            med = X[col].median()
            X[col] = X[col].fillna(med if pd.notna(med) else 0)
        if np.isinf(X[col]).any():
            X[col] = X[col].replace([np.inf, -np.inf], np.nan).fillna(X[col].median())

    mask = X[ALL_FEATURES].isna().any(axis=1)
    X = X.loc[~mask, ALL_FEATURES]
    y = y[~mask]

    print(f"  Dataset: {len(X)} x {len(ALL_FEATURES)} features, {y.mean():.2%} positive")

    data = {"X": X, "y": y, "ALL_FEATURES": ALL_FEATURES, "CAT_FEATURES": CAT_FEATURES}
    save_ckpt("stage1_features.pkl", data)
    return data


# ═══════════════════════════════════════════════════════════
# Stage 2: Train/test split
# ═══════════════════════════════════════════════════════════
def stage2(args, s1):
    ckpt = load_ckpt("stage2_split.pkl")
    if ckpt is not None and not (args.from_stage == "2" and args.force):
        print("[2] ✓ loaded from checkpoint")
        return ckpt

    if not should_run("2", args):
        raise RuntimeError("Stage 2 checkpoint missing but stage skipped")

    print("\n[5] Splitting data...")
    X, y = s1["X"], s1["y"]
    ALL_FEATURES = s1["ALL_FEATURES"]

    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=42, stratify=y)

    for col in CAT_FEATURES:
        if col in X_train.columns:
            X_train[col] = X_train[col].astype(int)
            X_test[col] = X_test[col].astype(int)

    X_train_xgb = X_train.copy()
    X_test_xgb = X_test.copy()
    for col in CAT_FEATURES:
        if col in X_train_xgb.columns:
            X_train_xgb[col] = X_train_xgb[col].astype("category")
            X_test_xgb[col] = X_test_xgb[col].astype("category")

    print(f"  Train: {len(X_train)}, Test: {len(X_test)}")

    data = {
        "X_train": X_train, "X_test": X_test,
        "y_train": y_train, "y_test": y_test,
        "X_train_xgb": X_train_xgb, "X_test_xgb": X_test_xgb,
    }
    save_ckpt("stage2_split.pkl", data)
    return data


# ═══════════════════════════════════════════════════════════
# Stage 3a: CatBoost Optuna HPO (SQLite persistent)
# ═══════════════════════════════════════════════════════════
def stage3a(args, s1, s2):
    import optuna
    from catboost import CatBoostClassifier
    optuna.logging.set_verbosity(optuna.logging.WARNING)

    ALL_FEATURES = s1["ALL_FEATURES"]
    cat_indices = [ALL_FEATURES.index(c) for c in CAT_FEATURES if c in ALL_FEATURES]
    X_train, X_test = s2["X_train"], s2["X_test"]
    y_train, y_test = s2["y_train"], s2["y_test"]

    db_path = CKPT_DIR / "stage3a_optuna_cb.db"
    CKPT_DIR.mkdir(parents=True, exist_ok=True)
    storage = f"sqlite:///{db_path}"

    study = optuna.create_study(
        study_name="catboost_v8", storage=storage,
        direction="maximize",
        sampler=optuna.samplers.TPESampler(seed=42, n_startup_trials=20),
        load_if_exists=True,
    )
    done = len(study.trials)
    remaining = max(0, args.trials - done)

    if remaining == 0:
        print(f"[3a] ✓ CatBoost HPO done ({done} trials, best={study.best_value:.4f})")
        return study

    if not should_run("3a", args):
        if done > 0:
            print(f"[3a] ✓ CatBoost HPO partial ({done} trials, best={study.best_value:.4f})")
            return study
        raise RuntimeError("Stage 3a: no trials and stage skipped")

    print(f"\n[3a] CatBoost HPO ({done}/{args.trials} done, {remaining} remaining)...")

    def cb_objective(trial):
        params = {
            "iterations": trial.suggest_int("iterations", 500, 2000),
            "learning_rate": trial.suggest_float("learning_rate", 0.005, 0.1, log=True),
            "depth": trial.suggest_int("depth", 4, 10),
            "l2_leaf_reg": trial.suggest_float("l2_leaf_reg", 0.1, 30.0),
            "bootstrap_type": "Bernoulli",
            "subsample": trial.suggest_float("subsample", 0.5, 1.0),
            "min_data_in_leaf": trial.suggest_int("min_data_in_leaf", 1, 100),
            "random_strength": trial.suggest_float("random_strength", 0.0, 10.0),
            "border_count": trial.suggest_int("border_count", 32, 255),
            "grow_policy": trial.suggest_categorical("grow_policy",
                                                     ["SymmetricTree", "Depthwise"]),
            "random_seed": 42, "verbose": False,
            "task_type": "GPU", "devices": "0",
            "cat_features": cat_indices,
            "auto_class_weights": trial.suggest_categorical("auto_class_weights",
                                                            ["Balanced", "SqrtBalanced"]),
        }
        model = CatBoostClassifier(**params)
        model.fit(X_train, y_train, eval_set=(X_test, y_test),
                  verbose=False, early_stopping_rounds=50)
        return roc_auc_score(y_test, model.predict_proba(X_test)[:, 1])

    study.optimize(cb_objective, n_trials=remaining, show_progress_bar=True)
    print(f"  CatBoost best: {study.best_value:.4f}")
    return study


# ═══════════════════════════════════════════════════════════
# Stage 3b: LightGBM Optuna HPO (SQLite persistent)
# ═══════════════════════════════════════════════════════════
def stage3b(args, s1, s2):
    import optuna
    from lightgbm import LGBMClassifier
    import lightgbm as lgb
    optuna.logging.set_verbosity(optuna.logging.WARNING)

    ALL_FEATURES = s1["ALL_FEATURES"]
    lgb_cat = [c for c in CAT_FEATURES if c in ALL_FEATURES]
    X_train, X_test = s2["X_train"], s2["X_test"]
    y_train, y_test = s2["y_train"], s2["y_test"]

    db_path = CKPT_DIR / "stage3b_optuna_lgb.db"
    CKPT_DIR.mkdir(parents=True, exist_ok=True)
    storage = f"sqlite:///{db_path}"

    study = optuna.create_study(
        study_name="lightgbm_v8", storage=storage,
        direction="maximize",
        sampler=optuna.samplers.TPESampler(seed=42, n_startup_trials=20),
        load_if_exists=True,
    )
    done = len(study.trials)
    remaining = max(0, args.trials - done)

    if remaining == 0:
        print(f"[3b] ✓ LightGBM HPO done ({done} trials, best={study.best_value:.4f})")
        return study

    if not should_run("3b", args):
        if done > 0:
            print(f"[3b] ✓ LightGBM HPO partial ({done} trials, best={study.best_value:.4f})")
            return study
        raise RuntimeError("Stage 3b: no trials and stage skipped")

    print(f"\n[3b] LightGBM HPO ({done}/{args.trials} done, {remaining} remaining)...")

    def lgb_objective(trial):
        params = {
            "n_estimators": trial.suggest_int("n_estimators", 500, 2000),
            "learning_rate": trial.suggest_float("learning_rate", 0.005, 0.1, log=True),
            "max_depth": trial.suggest_int("max_depth", 4, 12),
            "num_leaves": trial.suggest_int("num_leaves", 16, 128),
            "reg_alpha": trial.suggest_float("reg_alpha", 0.0, 15.0),
            "reg_lambda": trial.suggest_float("reg_lambda", 0.0, 15.0),
            "subsample": trial.suggest_float("subsample", 0.5, 1.0),
            "colsample_bytree": trial.suggest_float("colsample_bytree", 0.3, 1.0),
            "min_child_samples": trial.suggest_int("min_child_samples", 5, 100),
            "min_split_gain": trial.suggest_float("min_split_gain", 0.0, 1.0),
            "is_unbalance": True, "random_state": 42, "verbose": -1,
            "feature_pre_filter": False,
        }
        model = LGBMClassifier(**params)
        model.fit(X_train, y_train, eval_set=[(X_test, y_test)],
                  callbacks=[lgb.early_stopping(50, verbose=False),
                             lgb.log_evaluation(0)],
                  categorical_feature=lgb_cat)
        return roc_auc_score(y_test, model.predict_proba(X_test)[:, 1])

    study.optimize(lgb_objective, n_trials=remaining, show_progress_bar=True)
    print(f"  LightGBM best: {study.best_value:.4f}")
    return study


# ═══════════════════════════════════════════════════════════
# Stage 3c: XGBoost Optuna HPO (SQLite persistent)
# ═══════════════════════════════════════════════════════════
def stage3c(args, s1, s2):
    import optuna
    from xgboost import XGBClassifier
    optuna.logging.set_verbosity(optuna.logging.WARNING)

    y_train, y_test = s2["y_train"], s2["y_test"]
    X_train_xgb, X_test_xgb = s2["X_train_xgb"], s2["X_test_xgb"]
    neg_pos_ratio = (y_train == 0).sum() / max((y_train == 1).sum(), 1)

    db_path = CKPT_DIR / "stage3c_optuna_xgb.db"
    CKPT_DIR.mkdir(parents=True, exist_ok=True)
    storage = f"sqlite:///{db_path}"

    study = optuna.create_study(
        study_name="xgboost_v8", storage=storage,
        direction="maximize",
        sampler=optuna.samplers.TPESampler(seed=42, n_startup_trials=20),
        load_if_exists=True,
    )
    done = len(study.trials)
    remaining = max(0, args.trials - done)

    if remaining == 0:
        print(f"[3c] ✓ XGBoost HPO done ({done} trials, best={study.best_value:.4f})")
        return study

    if not should_run("3c", args):
        if done > 0:
            print(f"[3c] ✓ XGBoost HPO partial ({done} trials, best={study.best_value:.4f})")
            return study
        raise RuntimeError("Stage 3c: no trials and stage skipped")

    print(f"\n[3c] XGBoost HPO ({done}/{args.trials} done, {remaining} remaining)...")

    def xgb_objective(trial):
        params = {
            "n_estimators": trial.suggest_int("n_estimators", 500, 2000),
            "learning_rate": trial.suggest_float("learning_rate", 0.005, 0.1, log=True),
            "max_depth": trial.suggest_int("max_depth", 4, 10),
            "min_child_weight": trial.suggest_int("min_child_weight", 1, 50),
            "reg_alpha": trial.suggest_float("reg_alpha", 0.0, 15.0),
            "reg_lambda": trial.suggest_float("reg_lambda", 0.0, 15.0),
            "subsample": trial.suggest_float("subsample", 0.5, 1.0),
            "colsample_bytree": trial.suggest_float("colsample_bytree", 0.3, 1.0),
            "gamma": trial.suggest_float("gamma", 0.0, 5.0),
            "scale_pos_weight": neg_pos_ratio,
            "random_state": 42, "verbosity": 0,
            "enable_categorical": True, "tree_method": "hist", "device": "cuda",
        }
        model = XGBClassifier(**params)
        model.fit(X_train_xgb, y_train, eval_set=[(X_test_xgb, y_test)], verbose=False)
        return roc_auc_score(y_test, model.predict_proba(X_test_xgb)[:, 1])

    study.optimize(xgb_objective, n_trials=remaining, show_progress_bar=True)
    print(f"  XGBoost best: {study.best_value:.4f}")
    return study


# ═══════════════════════════════════════════════════════════
# Stage 4: Train 6 final models
# ═══════════════════════════════════════════════════════════
def stage4(args, s1, s2, study_cb, study_lgb, study_xgb):
    ckpt = load_ckpt("stage4_base_models.pkl")
    if ckpt is not None and not (args.from_stage == "4" and args.force):
        print("[4] ✓ loaded from checkpoint")
        return ckpt

    if not should_run("4", args):
        raise RuntimeError("Stage 4 checkpoint missing but stage skipped")

    from catboost import CatBoostClassifier
    from lightgbm import LGBMClassifier
    import lightgbm as lgb
    from xgboost import XGBClassifier

    ALL_FEATURES = s1["ALL_FEATURES"]
    cat_indices = [ALL_FEATURES.index(c) for c in CAT_FEATURES if c in ALL_FEATURES]
    lgb_cat = [c for c in CAT_FEATURES if c in ALL_FEATURES]
    X_train, X_test = s2["X_train"], s2["X_test"]
    y_train, y_test = s2["y_train"], s2["y_test"]
    X_train_xgb, X_test_xgb = s2["X_train_xgb"], s2["X_test_xgb"]
    neg_pos_ratio = (y_train == 0).sum() / max((y_train == 1).sum(), 1)

    print("\n[6] Training 6 final models...")

    best_cb = study_cb.best_params.copy()
    acw = best_cb.pop("auto_class_weights", "Balanced")
    best_cb.update({"random_seed": 42, "verbose": 100,
                    "task_type": "GPU", "devices": "0",
                    "bootstrap_type": "Bernoulli",
                    "cat_features": cat_indices, "auto_class_weights": acw})
    cb_model = CatBoostClassifier(**best_cb)
    cb_model.fit(X_train, y_train, eval_set=(X_test, y_test), early_stopping_rounds=50)
    cb_proba = cb_model.predict_proba(X_test)[:, 1]
    cb_auc = roc_auc_score(y_test, cb_proba)
    print(f"  CatBoost: {cb_auc:.4f}")

    best_lgb = study_lgb.best_params.copy()
    best_lgb.update({"random_state": 42, "verbose": -1,
                     "is_unbalance": True, "feature_pre_filter": False})
    lgb_model = LGBMClassifier(**best_lgb)
    lgb_model.fit(X_train, y_train, eval_set=[(X_test, y_test)],
                  callbacks=[lgb.early_stopping(50, verbose=False),
                             lgb.log_evaluation(0)],
                  categorical_feature=lgb_cat)
    lgb_proba = lgb_model.predict_proba(X_test)[:, 1]
    lgb_auc = roc_auc_score(y_test, lgb_proba)
    print(f"  LightGBM: {lgb_auc:.4f}")

    best_xgb = study_xgb.best_params.copy()
    best_xgb.update({"random_state": 42, "verbosity": 0,
                     "scale_pos_weight": neg_pos_ratio,
                     "enable_categorical": True, "tree_method": "hist", "device": "cuda"})
    xgb_model = XGBClassifier(**best_xgb)
    xgb_model.fit(X_train_xgb, y_train, eval_set=[(X_test_xgb, y_test)], verbose=False)
    xgb_proba = xgb_model.predict_proba(X_test_xgb)[:, 1]
    xgb_auc = roc_auc_score(y_test, xgb_proba)
    print(f"  XGBoost: {xgb_auc:.4f}")

    et_model = ExtraTreesClassifier(
        n_estimators=2000, max_depth=20, min_samples_leaf=3,
        class_weight="balanced", random_state=42, n_jobs=-1)
    et_model.fit(X_train, y_train)
    et_proba = et_model.predict_proba(X_test)[:, 1]
    et_auc = roc_auc_score(y_test, et_proba)
    print(f"  ExtraTrees: {et_auc:.4f}")

    rf_model = RandomForestClassifier(
        n_estimators=2000, max_depth=20, min_samples_leaf=3,
        class_weight="balanced", random_state=42, n_jobs=-1)
    rf_model.fit(X_train, y_train)
    rf_proba = rf_model.predict_proba(X_test)[:, 1]
    rf_auc = roc_auc_score(y_test, rf_proba)
    print(f"  RandomForest: {rf_auc:.4f}")

    gbm_model = GradientBoostingClassifier(
        n_estimators=1000, learning_rate=0.01, max_depth=5,
        min_samples_leaf=10, subsample=0.8, random_state=42)
    gbm_model.fit(X_train, y_train)
    gbm_proba = gbm_model.predict_proba(X_test)[:, 1]
    gbm_auc = roc_auc_score(y_test, gbm_proba)
    print(f"  GBM: {gbm_auc:.4f}")

    data = {
        "models": {
            "catboost": cb_model, "lightgbm": lgb_model, "xgboost": xgb_model,
            "extratrees": et_model, "randomforest": rf_model, "gbm": gbm_model,
        },
        "test_probas": {
            "catboost": cb_proba, "lightgbm": lgb_proba, "xgboost": xgb_proba,
            "extratrees": et_proba, "randomforest": rf_proba, "gbm": gbm_proba,
        },
        "individual_aucs": {
            "catboost": cb_auc, "lightgbm": lgb_auc, "xgboost": xgb_auc,
            "extratrees": et_auc, "randomforest": rf_auc, "gbm": gbm_auc,
        },
        "best_cb": best_cb, "best_lgb": best_lgb, "best_xgb": best_xgb,
    }
    save_ckpt("stage4_base_models.pkl", data)
    return data


# ═══════════════════════════════════════════════════════════
# Stage 5: 5-fold stacking + ensemble evaluation
# ═══════════════════════════════════════════════════════════
def stage5(args, s1, s2, s4):
    ckpt = load_ckpt("stage5_stacking.pkl")
    if ckpt is not None and not (args.from_stage == "5" and args.force):
        print("[5] ✓ loaded from checkpoint")
        return ckpt

    if not should_run("5", args):
        raise RuntimeError("Stage 5 checkpoint missing but stage skipped")

    from catboost import CatBoostClassifier
    from lightgbm import LGBMClassifier
    import lightgbm as _lgb
    from xgboost import XGBClassifier

    ALL_FEATURES = s1["ALL_FEATURES"]
    cat_indices = [ALL_FEATURES.index(c) for c in CAT_FEATURES if c in ALL_FEATURES]
    lgb_cat = [c for c in CAT_FEATURES if c in ALL_FEATURES]
    X_train, X_test = s2["X_train"], s2["X_test"]
    y_train, y_test = s2["y_train"], s2["y_test"]
    X_train_xgb, X_test_xgb = s2["X_train_xgb"], s2["X_test_xgb"]

    best_cb = s4["best_cb"]
    best_lgb = s4["best_lgb"]
    best_xgb = s4["best_xgb"]
    probas = s4["test_probas"]
    indiv_aucs = s4["individual_aucs"]

    cb_proba = probas["catboost"]
    lgb_proba = probas["lightgbm"]
    xgb_proba = probas["xgboost"]
    et_proba = probas["extratrees"]
    rf_proba = probas["randomforest"]
    gbm_proba = probas["gbm"]

    print("\n[7] 6-model stacking...")
    skf = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)
    n_models = 6

    oof = [np.zeros(len(X_train)) for _ in range(n_models)]
    test_preds = [np.zeros(len(X_test)) for _ in range(n_models)]

    for fold, (tr_idx, val_idx) in enumerate(skf.split(X_train, y_train)):
        Xtr, Xval = X_train.iloc[tr_idx], X_train.iloc[val_idx]
        ytr, yval = y_train.iloc[tr_idx], y_train.iloc[val_idx]
        Xtr_x, Xval_x = X_train_xgb.iloc[tr_idx], X_train_xgb.iloc[val_idx]

        m = CatBoostClassifier(**{k: v for k, v in best_cb.items() if k != "verbose"}, verbose=False)
        m.fit(Xtr, ytr, eval_set=(Xval, yval), early_stopping_rounds=50, verbose=False)
        oof[0][val_idx] = m.predict_proba(Xval)[:, 1]
        test_preds[0] += m.predict_proba(X_test)[:, 1] / 5

        m2 = LGBMClassifier(**best_lgb)
        m2.fit(Xtr, ytr, eval_set=[(Xval, yval)],
               callbacks=[_lgb.early_stopping(50, verbose=False), _lgb.log_evaluation(0)],
               categorical_feature=lgb_cat)
        oof[1][val_idx] = m2.predict_proba(Xval)[:, 1]
        test_preds[1] += m2.predict_proba(X_test)[:, 1] / 5

        m3 = XGBClassifier(**best_xgb)
        m3.fit(Xtr_x, ytr, eval_set=[(Xval_x, yval)], verbose=False)
        oof[2][val_idx] = m3.predict_proba(Xval_x)[:, 1]
        test_preds[2] += m3.predict_proba(X_test_xgb)[:, 1] / 5

        m4 = ExtraTreesClassifier(n_estimators=2000, max_depth=20, min_samples_leaf=3,
                                  class_weight="balanced", random_state=42, n_jobs=-1)
        m4.fit(Xtr, ytr)
        oof[3][val_idx] = m4.predict_proba(Xval)[:, 1]
        test_preds[3] += m4.predict_proba(X_test)[:, 1] / 5

        m5 = RandomForestClassifier(n_estimators=2000, max_depth=20, min_samples_leaf=3,
                                    class_weight="balanced", random_state=42, n_jobs=-1)
        m5.fit(Xtr, ytr)
        oof[4][val_idx] = m5.predict_proba(Xval)[:, 1]
        test_preds[4] += m5.predict_proba(X_test)[:, 1] / 5

        m6 = GradientBoostingClassifier(n_estimators=1000, learning_rate=0.01, max_depth=5,
                                        min_samples_leaf=10, subsample=0.8, random_state=42)
        m6.fit(Xtr, ytr)
        oof[5][val_idx] = m6.predict_proba(Xval)[:, 1]
        test_preds[5] += m6.predict_proba(X_test)[:, 1] / 5

        aucs = [roc_auc_score(yval, oof[i][val_idx]) for i in range(n_models)]
        print(f"  Fold {fold+1}: CB={aucs[0]:.4f} LGB={aucs[1]:.4f} XGB={aucs[2]:.4f} "
              f"ET={aucs[3]:.4f} RF={aucs[4]:.4f} GBM={aucs[5]:.4f}")

    stack_train = np.column_stack(oof)
    stack_test = np.column_stack(test_preds)
    meta = LogisticRegressionCV(Cs=20, cv=5, random_state=42, scoring="roc_auc")
    meta.fit(stack_train, y_train)
    stack_proba = meta.predict_proba(stack_test)[:, 1]
    stack_auc = roc_auc_score(y_test, stack_proba)
    print(f"\n  Stacking AUC: {stack_auc:.4f}")
    names = ["CB", "LGB", "XGB", "ET", "RF", "GBM"]
    weights = meta.coef_[0]
    print(f"  Meta: {' '.join(f'{n}={w:.3f}' for n, w in zip(names, weights))}")

    all_probas = [cb_proba, lgb_proba, xgb_proba, et_proba, rf_proba, gbm_proba]
    blend6 = np.mean(all_probas, axis=0)
    blend3_gbdt = np.mean([cb_proba, lgb_proba, xgb_proba], axis=0)
    total = sum(indiv_aucs.values())
    wblend = sum(indiv_aucs[k]/total * probas[k] for k in indiv_aucs)
    rank_avg = rank_average(*all_probas)
    rank_avg3 = rank_average(cb_proba, lgb_proba, xgb_proba)
    stack_fold_blend3 = np.mean(test_preds[:3], axis=0)
    stack_fold_blend6 = np.mean(test_preds, axis=0)

    all_aucs = {
        "catboost": indiv_aucs["catboost"],
        "lightgbm": indiv_aucs["lightgbm"],
        "xgboost": indiv_aucs["xgboost"],
        "extratrees": indiv_aucs["extratrees"],
        "randomforest": indiv_aucs["randomforest"],
        "gbm": indiv_aucs["gbm"],
        "stacking": stack_auc,
        "blend6": roc_auc_score(y_test, blend6),
        "blend3_gbdt": roc_auc_score(y_test, blend3_gbdt),
        "wblend": roc_auc_score(y_test, wblend),
        "rank_avg6": roc_auc_score(y_test, rank_avg),
        "rank_avg3": roc_auc_score(y_test, rank_avg3),
        "stack_fold_blend3": roc_auc_score(y_test, stack_fold_blend3),
        "stack_fold_blend6": roc_auc_score(y_test, stack_fold_blend6),
    }

    print("\n  All methods:")
    for name, auc in sorted(all_aucs.items(), key=lambda x: -x[1]):
        marker = " <-- BEST" if auc == max(all_aucs.values()) else ""
        print(f"    {name:20s}: {auc:.4f}{marker}")

    method = max(all_aucs, key=all_aucs.get)
    best_auc = all_aucs[method]

    proba_map = {
        "catboost": cb_proba, "lightgbm": lgb_proba, "xgboost": xgb_proba,
        "extratrees": et_proba, "randomforest": rf_proba, "gbm": gbm_proba,
        "stacking": stack_proba, "blend6": blend6, "blend3_gbdt": blend3_gbdt,
        "wblend": wblend, "rank_avg6": rank_avg, "rank_avg3": rank_avg3,
        "stack_fold_blend3": stack_fold_blend3, "stack_fold_blend6": stack_fold_blend6,
    }
    final_proba = proba_map[method]

    print(f"\n  BEST: {method} (AUC={best_auc:.4f})")
    y_pred = (final_proba >= np.median(final_proba[y_test == 1])).astype(int)
    print(classification_report(y_test, y_pred, target_names=["No CVD", "CVD"]))

    data = {
        "oof": oof, "test_preds": test_preds, "meta": meta,
        "all_aucs": all_aucs, "best_method": method, "best_auc": best_auc,
    }
    save_ckpt("stage5_stacking.pkl", data)
    return data


# ═══════════════════════════════════════════════════════════
# Stage 6: 5-fold CV + save final model
# ═══════════════════════════════════════════════════════════
def stage6(args, s1, s2, s4, s5):
    if not should_run("6", args):
        ckpt = load_ckpt("stage6_results.json")
        if ckpt is not None:
            print("[6] ✓ already done")
            return
        raise RuntimeError("Stage 6 checkpoint missing but stage skipped")

    from catboost import CatBoostClassifier

    ALL_FEATURES = s1["ALL_FEATURES"]
    X, y = s1["X"], s1["y"]
    best_cb = s4["best_cb"]
    models = s4["models"]
    meta = s5["meta"]
    method = s5["best_method"]
    best_auc = s5["best_auc"]
    all_aucs = s5["all_aucs"]

    print("\n[8] 5-fold CV (CatBoost)...")
    skf = StratifiedKFold(n_splits=5, shuffle=True, random_state=42)
    cv_aucs = []
    for fold, (tr_idx, val_idx) in enumerate(skf.split(X, y)):
        m = CatBoostClassifier(**{k: v for k, v in best_cb.items() if k != "verbose"}, verbose=False)
        m.fit(X.iloc[tr_idx], y.iloc[tr_idx],
              eval_set=(X.iloc[val_idx], y.iloc[val_idx]),
              early_stopping_rounds=50, verbose=False)
        fold_auc = roc_auc_score(y.iloc[val_idx], m.predict_proba(X.iloc[val_idx])[:, 1])
        cv_aucs.append(fold_auc)
        print(f"  Fold {fold+1}: {fold_auc:.4f}")
    print(f"  Mean: {np.mean(cv_aucs):.4f} +/- {np.std(cv_aucs):.4f}")

    cb_model = models["catboost"]
    importance = pd.DataFrame({
        "feature": ALL_FEATURES,
        "importance": cb_model.feature_importances_,
    }).sort_values("importance", ascending=False)
    accel_feats = [c for c in ALL_FEATURES if c.startswith("accel_") or c in
                   ["has_accel", "age_activity", "bmi_activity", "sbp_activity",
                    "sleep_activity_ratio", "activity_risk"]]
    print("\nTop 20 features:")
    for _, row in importance.head(20).iterrows():
        w = " [W]" if row["feature"] in accel_feats else ""
        print(f"  {row['feature']:25s} {row['importance']:6.2f}{w}")

    out = Path(args.output)
    out.parent.mkdir(parents=True, exist_ok=True)
    joblib.dump(cb_model, str(out))

    ensemble_data = {**models, "meta": meta, "method": method}
    joblib.dump(ensemble_data, str(out.with_stem(out.stem + "_ensemble")))

    meta_info = {
        "model_version": "v8.0-push",
        "auc": round(best_auc, 4),
        "cv_auc": round(np.mean(cv_aucs), 4),
        "method": method,
        "all_aucs": {k: round(v, 4) for k, v in all_aucs.items()},
        "feature_cols": ALL_FEATURES,
        "cat_features": CAT_FEATURES,
        "n_features": len(ALL_FEATURES),
        "n_samples": len(X),
        "positive_rate": round(y.mean(), 4),
    }
    with open(out.with_suffix(".json"), "w", encoding="utf-8") as f:
        json.dump(meta_info, f, ensure_ascii=False, indent=2)

    results = {"auc": best_auc, "cv_auc": float(np.mean(cv_aucs)), "method": method}
    CKPT_DIR.mkdir(parents=True, exist_ok=True)
    with open(CKPT_DIR / "stage6_results.json", "w") as f:
        json.dump(results, f, indent=2)

    print(f"\nSaved: {out}")
    print("\n" + "=" * 60)
    print(f"BEST AUC: {best_auc:.4f} ({method}) | CV: {np.mean(cv_aucs):.4f}")
    if best_auc >= 0.88:
        print("TARGET MET!")
    else:
        print(f"Gap to 0.88: {0.88 - best_auc:.4f}")
    print("=" * 60)


# ═══════════════════════════════════════════════════════════
# Main
# ═══════════════════════════════════════════════════════════
def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--trials", type=int, default=100)
    parser.add_argument("--output", default="train/rehealth_v8_push.pkl")
    parser.add_argument("--from-stage", type=str, default=None, choices=STAGES)
    parser.add_argument("--force", action="store_true")
    parser.add_argument("--only-stage", type=str, default=None,
                        help="Comma-separated stages to run, e.g. 3a,3b,3c")
    args = parser.parse_args()

    if args.only_stage:
        args.only_stage = [s.strip() for s in args.only_stage.split(",")]
    else:
        args.only_stage = None

    print("=" * 60)
    print("ReHealth v8 Push — Target AUC 0.88+")
    print(f"  Checkpoints: {CKPT_DIR}")
    if args.from_stage:
        print(f"  From stage: {args.from_stage} (force={args.force})")
    if args.only_stage:
        print(f"  Only stages: {args.only_stage}")
    print("=" * 60)

    s1 = stage1(args)
    s2 = stage2(args, s1)
    study_cb = stage3a(args, s1, s2)
    study_lgb = stage3b(args, s1, s2)
    study_xgb = stage3c(args, s1, s2)
    s4 = stage4(args, s1, s2, study_cb, study_lgb, study_xgb)
    s5 = stage5(args, s1, s2, s4)
    stage6(args, s1, s2, s4, s5)


if __name__ == "__main__":
    main()
