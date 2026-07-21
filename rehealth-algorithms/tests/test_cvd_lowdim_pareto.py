from train.train_cvd_lowdim_pareto import (
    CORE16,
    canonicalize_feature_sets,
    choose_wearable_champion,
    mask_for_robustness,
    pareto_front,
)

import pandas as pd


def test_feature_sets_preserve_canonical_relative_order():
    positions = {field: index for index, field in enumerate(CORE16)}
    for fields in canonicalize_feature_sets().values():
        assert fields == sorted(fields, key=positions.__getitem__)


def test_wearable_champion_excludes_bp_and_lab_candidates():
    rows = [
        {
            "candidate": "band_profile_6",
            "ordinary_band_eligible": True,
            "feature_count": 6,
            "validation_auc": 0.830,
            "validation_brier": 0.10,
        },
        {
            "candidate": "band_profile_8",
            "ordinary_band_eligible": True,
            "feature_count": 8,
            "validation_auc": 0.840,
            "validation_brier": 0.09,
        },
        {
            "candidate": "bp_assisted_10",
            "ordinary_band_eligible": False,
            "feature_count": 10,
            "validation_auc": 0.860,
            "validation_brier": 0.08,
        },
    ]
    assert choose_wearable_champion(rows)["candidate"] == "band_profile_8"


def test_pareto_front_removes_strictly_dominated_candidate():
    strong = {
        "candidate": "strong",
        "feature_count": 6,
        "acquisition_cost": 2.0,
        "validation_auc": 0.84,
        "validation_brier": 0.09,
    }
    weak = {
        "candidate": "weak",
        "feature_count": 8,
        "acquisition_cost": 3.0,
        "validation_auc": 0.83,
        "validation_brier": 0.10,
    }
    assert pareto_front([strong, weak]) == [strong]


def test_missingness_robustness_accepts_integer_feature_columns():
    features = pd.DataFrame(
        {
            "age": [40, 50],
            "gender": [0, 1],
            "exercise_days": [3, 5],
            "smoking": [0, 1],
        }
    )
    masked = mask_for_robustness(features, seed=7)
    assert all(dtype.kind == "f" for dtype in masked.dtypes)
