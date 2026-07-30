import pytest
import json
from pathlib import Path

from rhi.validation import (
    Direction,
    device_fairness,
    monotonicity,
    responsiveness,
    stability,
)


def test_stability_reports_daily_variation():
    report = stability([70.0, 70.5, 69.8, 70.2])

    assert report.observations == 4
    assert report.maximum_day_change == pytest.approx(0.7)
    assert report.standard_deviation < 0.3


def test_responsiveness_checks_expected_direction():
    report = responsiveness(
        baseline_scores=[62, 63, 64],
        followup_scores=[68, 69, 70],
        expected_direction=Direction.HIGHER_IS_BETTER,
    )

    assert report.signed_change == 6
    assert report.directionally_correct


def test_monotonicity_detects_blood_pressure_score_violation():
    report = monotonicity(
        [(120, 90), (140, 60), (160, 65)],
        expected_direction=Direction.LOWER_IS_BETTER,
    )

    assert report.violations == 1
    assert report.violation_rate == 0.5


def test_device_fairness_reports_maximum_mean_gap():
    report = device_fairness(
        {
            "device-a": [70, 72, 71],
            "device-b": [66, 68, 67],
        },
    )

    assert report.device_count == 2
    assert report.maximum_gap == 4


def test_rhi_preview_manifest_has_32_unique_features_and_normalized_domains():
    manifest_path = Path(__file__).parents[1] / "config" / "rhi_v2_preview.json"
    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    features = manifest["feature_registry"]

    assert len(features) == 32
    assert len({feature["name"] for feature in features}) == 32
    assert sum(manifest["domains"].values()) == pytest.approx(1.0)
    assert manifest["algorithm_status"] == "research_preview_not_clinically_validated"
