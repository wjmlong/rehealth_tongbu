"""Train the reviewed CVD-16 CatBoost deployment candidate.

The pipeline keeps a fixed held-out test split sealed until the CatBoost
configuration has been selected on validation data. All imputation statistics
are fit on training data only during selection and on train+validation data
only for the final deployable pipeline.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import sys
from datetime import datetime, timezone
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from catboost import CatBoostClassifier
from sklearn.calibration import CalibratedClassifierCV
from sklearn.frozen import FrozenEstimator
from sklearn.impute import SimpleImputer
from sklearn.linear_model import LogisticRegression
from sklearn.metrics import (
    average_precision_score,
    brier_score_loss,
    confusion_matrix,
    f1_score,
    roc_auc_score,
    roc_curve,
)
from sklearn.model_selection import train_test_split
from sklearn.pipeline import Pipeline
from sklearn.preprocessing import StandardScaler


PROJECT_ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(PROJECT_ROOT / "train"))

from train_v7_wearable import extract_features, load_nhanes_deep  # noqa: E402


RANDOM_SEED = 42
CANONICAL_ARTIFACT_NAME = "rehealth_cvd_catboost.pkl"
CORE16 = [
    "age",
    "gender",
    "bmi",
    "sbp",
    "dbp",
    "fasting_glucose",
    "total_cholesterol",
    "ldl",
    "hdl",
    "triglycerides",
    "exercise_days",
    "smoking",
    "drinking",
    "diabetes_history",
    "hypertension_history",
    "family_history",
]
VALID_RANGES = {
    "bmi": (15.0, 60.0),
    "sbp": (60.0, 250.0),
    "dbp": (40.0, 150.0),
    "fasting_glucose": (2.0, 35.0),
    "total_cholesterol": (2.0, 20.0),
    "ldl": (0.5, 10.0),
    "hdl": (0.3, 5.0),
    "triglycerides": (0.2, 20.0),
    "exercise_days": (0.0, 7.0),
}


def write_json(path: Path, value: object) -> None:
    path.write_text(json.dumps(value, indent=2, ensure_ascii=False), encoding="utf-8")


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as artifact_file:
        for chunk in iter(lambda: artifact_file.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def load_core16_data() -> tuple[pd.DataFrame, pd.Series, pd.DataFrame]:
    raw = load_nhanes_deep()
    features = extract_features(raw)
    features = features.merge(
        raw[["SEQN", "_cycle"]].drop_duplicates(subset=["SEQN"]),
        on="SEQN",
        how="left",
    )
    matrix = features[CORE16].copy()
    for field, (minimum, maximum) in VALID_RANGES.items():
        matrix[field] = matrix[field].where(matrix[field].between(minimum, maximum))
    target = features["label"].astype(int)
    return matrix, target, features


def split_indices(target: pd.Series) -> tuple[np.ndarray, np.ndarray, np.ndarray, np.ndarray]:
    all_indices = np.arange(len(target))
    historical_development, previously_exposed_test = train_test_split(
        all_indices,
        test_size=0.20,
        random_state=RANDOM_SEED,
        stratify=target,
    )
    historical_train, validation_indices = train_test_split(
        historical_development,
        test_size=0.20,
        random_state=RANDOM_SEED,
        stratify=target.iloc[historical_development],
    )
    retained_train, final_lockbox = train_test_split(
        historical_train,
        test_size=0.20,
        random_state=20260711,
        stratify=target.iloc[historical_train],
    )
    train_indices = np.concatenate([retained_train, previously_exposed_test])
    return train_indices, validation_indices, final_lockbox, previously_exposed_test


def catboost_estimator(iterations: int) -> CatBoostClassifier:
    return CatBoostClassifier(
        iterations=iterations,
        learning_rate=0.04,
        depth=6,
        l2_leaf_reg=5.0,
        random_seed=RANDOM_SEED,
        verbose=False,
        auto_class_weights="Balanced",
        bootstrap_type="Bayesian",
        allow_writing_files=False,
    )


def catboost_pipeline(iterations: int) -> Pipeline:
    return Pipeline(
        steps=[
            ("imputer", SimpleImputer(strategy="median")),
            ("model", catboost_estimator(iterations)),
        ]
    )


def calibration_assessment(target: pd.Series, probabilities: np.ndarray) -> dict[str, object]:
    clipped = np.clip(np.asarray(probabilities, dtype=float), 1e-6, 1 - 1e-6)
    logits = np.log(clipped / (1.0 - clipped)).reshape(-1, 1)
    binary_target = np.asarray(target, dtype=int)
    if np.unique(binary_target).size != 2 or np.unique(logits).size < 2:
        return {
            "method": "sample_level_logistic_on_prediction_logit",
            "slope": None,
            "intercept": None,
            "status": "not_computable",
            "reason": "calibration requires both outcome classes and non-constant probabilities",
        }
    try:
        calibration_model = LogisticRegression(
            penalty=None,
            solver="lbfgs",
            max_iter=2000,
            random_state=RANDOM_SEED,
        )
        calibration_model.fit(logits, binary_target)
        return {
            "method": "sample_level_logistic_on_prediction_logit",
            "slope": round(float(calibration_model.coef_[0, 0]), 6),
            "intercept": round(float(calibration_model.intercept_[0]), 6),
            "status": "computed",
            "reason": None,
        }
    except (TypeError, ValueError) as error:
        return {
            "method": "sample_level_logistic_on_prediction_logit",
            "slope": None,
            "intercept": None,
            "status": "failed",
            "reason": str(error),
        }


def evaluate(target: pd.Series, probabilities: np.ndarray) -> dict[str, object]:
    binary_target = np.asarray(target, dtype=int)
    clipped = np.clip(np.asarray(probabilities, dtype=float), 1e-6, 1 - 1e-6)
    false_positive_rate, true_positive_rate, thresholds = roc_curve(binary_target, clipped)
    threshold = float(thresholds[int(np.argmax(true_positive_rate - false_positive_rate))])
    predicted = (clipped >= threshold).astype(int)
    true_negative, false_positive, false_negative, true_positive = confusion_matrix(
        binary_target, predicted, labels=[0, 1]
    ).ravel()
    calibration = calibration_assessment(target, clipped)
    return {
        "auc": round(float(roc_auc_score(binary_target, clipped)), 6),
        "auprc": round(float(average_precision_score(binary_target, clipped)), 6),
        "brier": round(float(brier_score_loss(binary_target, clipped)), 6),
        "calibration": calibration,
        "youden_threshold": round(threshold, 6),
        "threshold_metrics": {
            "sensitivity": round(true_positive / max(1, true_positive + false_negative), 6),
            "specificity": round(true_negative / max(1, true_negative + false_positive), 6),
            "f1": round(float(f1_score(binary_target, predicted)), 6),
            "tp": int(true_positive),
            "fp": int(false_positive),
            "tn": int(true_negative),
            "fn": int(false_negative),
        },
    }


def train_logistic_baseline(
    train_features: pd.DataFrame,
    train_target: pd.Series,
    validation_features: pd.DataFrame,
) -> np.ndarray:
    baseline = Pipeline(
        steps=[
            ("imputer", SimpleImputer(strategy="median")),
            ("scaler", StandardScaler()),
            (
                "model",
                LogisticRegression(
                    max_iter=2000,
                    class_weight="balanced",
                    random_state=RANDOM_SEED,
                ),
            ),
        ]
    )
    baseline.fit(train_features, train_target)
    return baseline.predict_proba(validation_features)[:, 1]


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument(
        "--out",
        help="New output directory relative to the repository root",
    )
    args = parser.parse_args()

    timestamp = datetime.now(timezone.utc)
    output_name = args.out or f"outputs/cvd_retrain_{timestamp.strftime('%Y%m%d_%H%M%S')}"
    output_root = (PROJECT_ROOT / output_name).resolve()
    outputs_root = (PROJECT_ROOT / "outputs").resolve()
    if outputs_root not in output_root.parents or output_root.exists():
        raise ValueError("--out must be a new directory under outputs/")
    deploy_directory = output_root / "deploy_candidate"
    deploy_directory.mkdir(parents=True)

    features, target, source_features = load_core16_data()
    train_indices, validation_indices, test_indices, previously_exposed_test = split_indices(target)
    train_features = features.iloc[train_indices]
    validation_features = features.iloc[validation_indices]
    test_features = features.iloc[test_indices]
    train_target = target.iloc[train_indices]
    validation_target = target.iloc[validation_indices]
    test_target = target.iloc[test_indices]

    candidate = catboost_pipeline(iterations=700)
    candidate.fit(train_features.to_numpy(), train_target)
    validation_probabilities = candidate.predict_proba(validation_features.to_numpy())[:, 1]
    validation_metrics = evaluate(validation_target, validation_probabilities)

    baseline_probabilities = train_logistic_baseline(
        train_features,
        train_target,
        validation_features,
    )
    baseline_metrics = evaluate(validation_target, baseline_probabilities)

    final_model = CalibratedClassifierCV(
        FrozenEstimator(candidate),
        method="sigmoid",
    )
    final_model.fit(validation_features.to_numpy(), validation_target)

    # The held-out test set is first touched here, after model family and parameters are frozen.
    test_probabilities = final_model.predict_proba(test_features.to_numpy())[:, 1]
    test_metrics = evaluate(test_target, test_probabilities)

    artifact_path = deploy_directory / CANONICAL_ARTIFACT_NAME
    feature_order_path = deploy_directory / "feature_cols.pkl"
    metadata_path = deploy_directory / "model_meta_v2.json"
    joblib.dump(final_model, artifact_path)
    joblib.dump(CORE16, feature_order_path)

    trained_at = timestamp.strftime("%Y-%m-%dT%H:%M:%SZ")
    model_version = f"cvd-core16-catboost-{timestamp.strftime('%Y%m%dT%H%M%SZ')}"
    calibration = test_metrics["calibration"]
    metadata = {
        "model_id": "rehealth-cvd-core16",
        "model_version": model_version,
        "model_auc": test_metrics["auc"],
        "artifact_name": CANONICAL_ARTIFACT_NAME,
        "feature_schema_version": "cvd-16-v1",
        "feature_names_ordered": CORE16,
        "feature_order": CORE16,
        "trained_at": trained_at,
        "source_output_directory": str(output_root),
        "training_script_sha256": sha256_file(Path(__file__)),
        "selection_policy": "core16_catboost fixed before held-out test; logistic regression is a validation-only sanity baseline",
        "split_policy": "71.2% final train / 16% calibration validation / 12.8% final lockbox; stratified",
        "test_access_policy": "final lockbox was isolated from the prior train-only pool and evaluated once after the base model and calibration were frozen",
        "prior_exposed_test_policy": "the prior seed-42 test was excluded from final evaluation and reused only as final training data",
        "imputation_strategy": "sklearn SimpleImputer median inside deployable pipeline; fit on training split only",
        "calibration_method": "Platt sigmoid fit on validation sample-level binary outcomes and frozen CatBoost probabilities",
        "calibration_status": "applied" if calibration["status"] == "computed" else calibration["status"],
        "calibration_slope": calibration["slope"],
        "calibration_intercept": calibration["intercept"],
        "calibration_failure_reason": calibration["reason"],
        "metrics": {
            "validation": validation_metrics,
            "validation_logistic_baseline": baseline_metrics,
            "held_out_test": test_metrics,
        },
        "artifact_sha256": sha256_file(artifact_path),
        "feature_order_sha256": sha256_file(feature_order_path),
        "data_sources": [
            "NHANES 2011-2012",
            "NHANES 2013-2014",
            "NHANES 2015-2016",
            "NHANES 2017-2018",
            "NHANES 2021-2023",
        ],
        "known_limitations": [
            "The outcome is self-reported prevalent CVD, not a prospective clinical endpoint.",
            "NHANES is a US population sample; transfer to Chinese populations is not validated.",
            "This model is for wellness screening support and is not a diagnosis or medical device.",
        ],
    }
    write_json(metadata_path, metadata)
    write_json(output_root / "validation_metrics.json", validation_metrics)
    write_json(output_root / "logistic_baseline_validation_metrics.json", baseline_metrics)
    write_json(output_root / "held_out_test_metrics.json", test_metrics)
    write_json(
        output_root / "split_manifest.json",
        {
            "random_seed": RANDOM_SEED,
            "total": len(target),
            "train": len(train_indices),
            "validation": len(validation_indices),
            "test": len(test_indices),
            "previously_exposed_test_reused_for_training": len(previously_exposed_test),
            "train_positive_rate": round(float(train_target.mean()), 6),
            "validation_positive_rate": round(float(validation_target.mean()), 6),
            "test_positive_rate": round(float(test_target.mean()), 6),
            "missingness_core16": {
                field: round(float(features[field].isna().mean()), 6) for field in CORE16
            },
            "cycle_counts": source_features["_cycle"].value_counts().to_dict(),
            "split_method": {
                "historical_exposed_test": "20% seed-42 split; reused only for final training",
                "calibration_validation": "16% seed-42 split from historical development pool",
                "final_lockbox": "12.8% total, isolated with seed 20260711 from the prior train-only pool",
            },
        },
    )
    (deploy_directory / "model_card_v2.md").write_text(
        "\n".join(
            [
                "# ReHealth CVD-16 CatBoost Candidate",
                "",
                f"- Model version: `{model_version}`",
                f"- Held-out test AUC: `{test_metrics['auc']}`",
                f"- Calibration status: `{calibration['status']}`",
                "- Feature schema: `cvd-16-v1`",
                "- Intended use: wellness screening support for adults; not a diagnosis.",
                "",
                "The held-out test was evaluated once after the CVD-16 CatBoost configuration was fixed.",
            ]
        ),
        encoding="utf-8",
    )

    print(json.dumps({
        "output_directory": str(output_root),
        "model_version": model_version,
        "validation_auc": validation_metrics["auc"],
        "logistic_baseline_validation_auc": baseline_metrics["auc"],
        "held_out_test_auc": test_metrics["auc"],
        "calibration": calibration,
        "artifact": str(artifact_path),
    }, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
