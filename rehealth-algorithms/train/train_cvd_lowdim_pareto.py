"""Run a leakage-controlled, wearable-first CVD feature Pareto search.

This script is research-only. It does not replace the deployed CVD-16 model or
produce a model-service artifact. Candidate selection uses an older-cycle
training/validation pool, while the newest available NHANES cycle is evaluated
only after the low-burden champion has been frozen.
"""

from __future__ import annotations

import argparse
import json
from datetime import datetime, timezone
from pathlib import Path

import joblib
import numpy as np
import pandas as pd
from sklearn.calibration import CalibratedClassifierCV
from sklearn.frozen import FrozenEstimator
from sklearn.metrics import roc_auc_score
from sklearn.model_selection import train_test_split

try:
    from .train_cvd_retrain import (
        CORE16,
        RANDOM_SEED,
        catboost_pipeline,
        evaluate,
        load_core16_data,
        write_json,
    )
except ImportError:
    from train_cvd_retrain import (
        CORE16,
        RANDOM_SEED,
        catboost_pipeline,
        evaluate,
        load_core16_data,
        write_json,
    )


PROJECT_ROOT = Path(__file__).resolve().parent.parent
LATEST_CYCLE = "2021-2023"
RESEARCH_SCHEMA_VERSION = "cvd-lowdim-wearable-research-v1"

FEATURE_TIERS = {
    "age": 0,
    "gender": 0,
    "bmi": 0,
    "smoking": 0,
    "drinking": 0,
    "diabetes_history": 0,
    "hypertension_history": 0,
    "family_history": 0,
    "exercise_days": 1,
    "sbp": 2,
    "dbp": 2,
    "fasting_glucose": 2,
    "total_cholesterol": 3,
    "ldl": 3,
    "hdl": 3,
    "triglycerides": 3,
}

TIER_LABELS = {
    0: "profile_or_questionnaire",
    1: "ordinary_band_activity",
    2: "validated_bp_or_home_measurement",
    3: "clinical_lab",
}

CANDIDATE_FEATURES = {
    "band_profile_6": [
        "age", "gender", "bmi", "exercise_days", "smoking", "hypertension_history",
    ],
    "band_profile_8": [
        "age", "gender", "bmi", "exercise_days", "smoking", "diabetes_history",
        "hypertension_history", "family_history",
    ],
    "bp_assisted_10": [
        "age", "gender", "bmi", "sbp", "dbp", "exercise_days", "smoking",
        "diabetes_history", "hypertension_history", "family_history",
    ],
    "home_measurement_12": [
        "age", "gender", "bmi", "sbp", "dbp", "fasting_glucose", "exercise_days",
        "smoking", "drinking", "diabetes_history", "hypertension_history", "family_history",
    ],
    "canonical_core16": list(CORE16),
}


def canonicalize_feature_sets() -> dict[str, list[str]]:
    """Return every candidate in the stable CVD-16 relative order."""
    canonical = {}
    for name, fields in CANDIDATE_FEATURES.items():
        unknown = set(fields) - set(CORE16)
        if unknown:
            raise ValueError(f"{name} contains fields outside CVD-16: {sorted(unknown)}")
        canonical[name] = [field for field in CORE16 if field in fields]
    return canonical


def temporal_split_indices(
    source_features: pd.DataFrame,
    target: pd.Series,
) -> tuple[np.ndarray, np.ndarray, np.ndarray]:
    cycles = source_features["_cycle"].astype(str)
    available_cycles = sorted(cycles.dropna().unique().tolist())
    if LATEST_CYCLE not in available_cycles:
        raise ValueError(
            f"required temporal holdout cycle {LATEST_CYCLE!r} is unavailable: {available_cycles}"
        )

    temporal_holdout = np.flatnonzero((cycles == LATEST_CYCLE).to_numpy())
    development = np.flatnonzero((cycles != LATEST_CYCLE).to_numpy())
    train_indices, validation_indices = train_test_split(
        development,
        test_size=0.20,
        random_state=RANDOM_SEED,
        stratify=target.iloc[development],
    )
    return np.asarray(train_indices), np.asarray(validation_indices), temporal_holdout


def bootstrap_auc_interval(
    target: pd.Series,
    probabilities: np.ndarray,
    samples: int,
    seed: int = RANDOM_SEED,
) -> list[float | None]:
    values = np.asarray(target, dtype=int)
    scores = np.asarray(probabilities, dtype=float)
    rng = np.random.default_rng(seed)
    aucs: list[float] = []
    for _ in range(samples):
        indices = rng.integers(0, len(values), len(values))
        if np.unique(values[indices]).size != 2:
            continue
        aucs.append(float(roc_auc_score(values[indices], scores[indices])))
    if not aucs:
        return [None, None]
    return [
        round(float(np.quantile(aucs, 0.025)), 6),
        round(float(np.quantile(aucs, 0.975)), 6),
    ]


def mask_for_robustness(features: pd.DataFrame, seed: int = RANDOM_SEED) -> pd.DataFrame:
    """Mask 15% of non-identity values to exercise the train-fitted imputer."""
    masked = features.astype(float).copy()
    maskable = [field for field in masked.columns if field not in {"age", "gender"}]
    rng = np.random.default_rng(seed)
    mask = rng.random((len(masked), len(maskable))) < 0.15
    masked.loc[:, maskable] = masked[maskable].mask(mask)
    return masked


def candidate_cost(fields: list[str]) -> dict[str, object]:
    tiers = [FEATURE_TIERS[field] for field in fields]
    return {
        "feature_count": len(fields),
        "max_tier": max(tiers),
        "max_tier_label": TIER_LABELS[max(tiers)],
        "acquisition_cost": round(float(sum(0.25 if tier == 0 else tier for tier in tiers)), 2),
        "ordinary_band_eligible": max(tiers) <= 1,
    }


def pareto_front(rows: list[dict[str, object]]) -> list[dict[str, object]]:
    front = []
    for row in rows:
        dominated = False
        for other in rows:
            if other is row:
                continue
            no_worse = (
                int(other["feature_count"]) <= int(row["feature_count"])
                and float(other["acquisition_cost"]) <= float(row["acquisition_cost"])
                and float(other["validation_auc"]) >= float(row["validation_auc"])
                and float(other["validation_brier"]) <= float(row["validation_brier"])
            )
            strictly_better = (
                int(other["feature_count"]) < int(row["feature_count"])
                or float(other["acquisition_cost"]) < float(row["acquisition_cost"])
                or float(other["validation_auc"]) > float(row["validation_auc"])
                or float(other["validation_brier"]) < float(row["validation_brier"])
            )
            if no_worse and strictly_better:
                dominated = True
                break
        if not dominated:
            front.append(row)
    return front


def choose_wearable_champion(
    rows: list[dict[str, object]],
    auc_tolerance: float = 0.005,
) -> dict[str, object]:
    eligible = [row for row in rows if bool(row["ordinary_band_eligible"])]
    if not eligible:
        raise ValueError("no Tier 0/1 candidate is available")
    best_auc = max(float(row["validation_auc"]) for row in eligible)
    near_best = [
        row for row in eligible if best_auc - float(row["validation_auc"]) <= auc_tolerance
    ]
    return min(
        near_best,
        key=lambda row: (int(row["feature_count"]), float(row["validation_brier"])),
    )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--out", help="New output directory under outputs/")
    parser.add_argument("--iterations", type=int, default=700)
    parser.add_argument("--bootstrap-samples", type=int, default=500)
    args = parser.parse_args()

    timestamp = datetime.now(timezone.utc)
    output_name = args.out or f"outputs/cvd_lowdim_pareto_{timestamp.strftime('%Y%m%d_%H%M%S')}"
    output_root = (PROJECT_ROOT / output_name).resolve()
    outputs_root = (PROJECT_ROOT / "outputs").resolve()
    if outputs_root not in output_root.parents or output_root.exists():
        raise ValueError("--out must be a new directory under outputs/")
    output_root.mkdir(parents=True)

    features, target, source_features = load_core16_data()
    train_indices, validation_indices, holdout_indices = temporal_split_indices(
        source_features, target
    )
    feature_sets = canonicalize_feature_sets()
    rows: list[dict[str, object]] = []
    fitted_models = {}

    for offset, (name, fields) in enumerate(feature_sets.items()):
        train_features = features.iloc[train_indices][fields]
        validation_features = features.iloc[validation_indices][fields]
        model = catboost_pipeline(iterations=args.iterations)
        model.fit(train_features, target.iloc[train_indices])
        probabilities = model.predict_proba(validation_features)[:, 1]
        metrics = evaluate(target.iloc[validation_indices], probabilities)
        robustness_probabilities = model.predict_proba(
            mask_for_robustness(validation_features, RANDOM_SEED + offset)
        )[:, 1]
        auc_interval = bootstrap_auc_interval(
            target.iloc[validation_indices],
            probabilities,
            args.bootstrap_samples,
            RANDOM_SEED + offset,
        )
        row = {
            "candidate": name,
            **candidate_cost(fields),
            "feature_order": fields,
            "validation_auc": metrics["auc"],
            "validation_auc_ci_low": auc_interval[0],
            "validation_auc_ci_high": auc_interval[1],
            "validation_auprc": metrics["auprc"],
            "validation_brier": metrics["brier"],
            "validation_calibration_slope": metrics["calibration"]["slope"],
            "validation_calibration_intercept": metrics["calibration"]["intercept"],
            "masked_15pct_auc": round(
                float(
                    roc_auc_score(
                        target.iloc[validation_indices], robustness_probabilities
                    )
                ),
                6,
            ),
        }
        rows.append(row)
        fitted_models[name] = model

    rows.sort(key=lambda row: (-float(row["validation_auc"]), int(row["feature_count"])))
    champion = choose_wearable_champion(rows)
    champion_name = str(champion["candidate"])
    champion_fields = feature_sets[champion_name]
    champion_model = fitted_models[champion_name]

    calibrated_model = CalibratedClassifierCV(FrozenEstimator(champion_model), method="sigmoid")
    calibrated_model.fit(
        features.iloc[validation_indices][champion_fields], target.iloc[validation_indices]
    )
    holdout_probabilities = calibrated_model.predict_proba(
        features.iloc[holdout_indices][champion_fields]
    )[:, 1]
    holdout_metrics = evaluate(target.iloc[holdout_indices], holdout_probabilities)
    holdout_metrics["auc_ci"] = bootstrap_auc_interval(
        target.iloc[holdout_indices],
        holdout_probabilities,
        args.bootstrap_samples,
        20260713,
    )

    review_directory = output_root / "review_candidate"
    review_directory.mkdir()
    joblib.dump(calibrated_model, review_directory / f"{champion_name}.pkl")
    joblib.dump(champion_fields, review_directory / "feature_cols.pkl")
    write_json(
        review_directory / "model_metadata.json",
        {
            "status": "research_only_not_deployable_to_cvd_16_api",
            "model_version": f"{champion_name}-{timestamp.strftime('%Y%m%dT%H%M%SZ')}",
            "feature_schema_version": RESEARCH_SCHEMA_VERSION,
            "feature_order": champion_fields,
            "selection_policy": "best Tier 0/1 candidate; smallest feature count within 0.005 validation AUC of the Tier 0/1 leader",
            "temporal_holdout_cycle": LATEST_CYCLE,
            "temporal_holdout_metrics": holdout_metrics,
            "not_for_diagnosis": True,
        },
    )

    pd.DataFrame(rows).drop(columns=["feature_order"]).to_csv(
        output_root / "leaderboard.csv", index=False
    )
    pd.DataFrame(pareto_front(rows)).drop(columns=["feature_order"]).to_csv(
        output_root / "pareto_front.csv", index=False
    )
    write_json(output_root / "feature_sets.json", feature_sets)
    write_json(
        output_root / "split_manifest.json",
        {
            "train": len(train_indices),
            "validation": len(validation_indices),
            "temporal_holdout": len(holdout_indices),
            "temporal_holdout_cycle": LATEST_CYCLE,
            "cycle_counts": source_features["_cycle"].value_counts().to_dict(),
            "historical_exposure_note": "NHANES cycles were used in earlier repository experiments; this holdout is sealed only within this corrected run, not globally pristine.",
        },
    )

    report_lines = [
        "# Corrected Low-Dimensional Wearable-First CVD Search",
        "",
        f"- Selected research champion: `{champion_name}` ({len(champion_fields)} fields)",
        f"- Validation AUC: `{champion['validation_auc']}`",
        f"- Temporal holdout ({LATEST_CYCLE}) AUC: `{holdout_metrics['auc']}`",
        f"- Temporal holdout AUC 95% bootstrap CI: `{holdout_metrics['auc_ci']}`",
        "- Selection used only Tier 0/1 candidates and did not inspect the temporal holdout.",
        "- The result is research-only and does not replace the deployed `cvd-16-v1` artifact.",
        "",
        "## Product Interpretation",
        "",
        "`band_profile_6` and `band_profile_8` use profile/questionnaire fields plus activity frequency. The current dataset and Android CVD-16 contract do not contain direct resting-heart-rate, step-count, SpO2, or sleep features, so this run must not be described as a pure sensor-only wristband model.",
        "",
        "`bp_assisted_10` requires validated blood-pressure input. `home_measurement_12` additionally requires fasting glucose. `canonical_core16` requires lipid labs.",
        "",
        "## Method",
        "",
        "- Median imputation stays inside each sklearn pipeline and is fitted on training data only.",
        "- Candidates are compared on a validation split from pre-2021 cycles.",
        "- The ordinary-band champion is frozen before the 2021-2023 temporal holdout is scored.",
        "- Calibration uses validation sample-level outcomes and predictions; failed calibration is reported as null by the shared corrected evaluator.",
    ]
    (output_root / "REPORT.md").write_text(
        "\n".join(report_lines) + "\n", encoding="utf-8"
    )
    (output_root / "CHAMPION_SELECTION.md").write_text(
        "\n".join(
            [
                "# Champion Selection",
                "",
                f"Research champion: `{champion_name}`.",
                "",
                "The champion is the smallest Tier 0/1 candidate within 0.005 validation AUC of the best Tier 0/1 candidate. This rule is fixed before temporal holdout evaluation.",
                "",
                "It is not deployment-compatible with `cvd-16-v1` because its feature schema is intentionally smaller. Keep the current Core16 model in production until a versioned low-dimensional API contract is approved across Android, backend, and model-service.",
            ]
        )
        + "\n",
        encoding="utf-8",
    )

    print(
        json.dumps(
            {
                "output_directory": str(output_root),
                "champion": champion_name,
                "feature_count": len(champion_fields),
                "validation_auc": champion["validation_auc"],
                "temporal_holdout_auc": holdout_metrics["auc"],
                "research_only": True,
            },
            indent=2,
        )
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
