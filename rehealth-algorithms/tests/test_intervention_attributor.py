"""Tests for InterventionAttributor (counterfactual decomposition)."""

import pytest
import numpy as np
from unittest.mock import MagicMock

from bodyup_cloud.engine.intervention_attributor import (
    InterventionAttributor,
    InterventionReport,
    MODIFIABLE_FEATURES,
    NON_MODIFIABLE_FEATURES,
)


SAMPLE_T0 = {
    "age": 60, "gender": 1, "bmi": 30.0, "sbp": 155, "dbp": 95,
    "fasting_glucose": 7.5, "total_cholesterol": 6.2, "ldl": 4.0,
    "hdl": 1.0, "triglycerides": 2.5, "exercise_days": 1, "smoking": 1,
    "drinking": 1, "diabetes_history": 1, "hypertension_history": 1,
    "family_history": 0,
}

SAMPLE_T1 = {
    "age": 60, "gender": 1, "bmi": 27.0, "sbp": 130, "dbp": 82,
    "fasting_glucose": 6.0, "total_cholesterol": 5.0, "ldl": 3.0,
    "hdl": 1.3, "triglycerides": 1.6, "exercise_days": 1, "smoking": 1,
    "drinking": 1, "diabetes_history": 1, "hypertension_history": 1,
    "family_history": 0,
}


def _make_mock_model(risk_map=None):
    """Create a mock model where predict_proba returns based on BMI thresholds."""
    model = MagicMock()

    call_count = [0]
    probas = risk_map or [0.5, 0.3, 0.5]  # T0, T1, counterfactual

    def fake_predict_proba(df):
        idx = min(call_count[0], len(probas) - 1)
        p = probas[idx]
        call_count[0] += 1
        return np.array([[1 - p, p]])

    model.predict_proba = MagicMock(side_effect=fake_predict_proba)
    return model


class TestInterventionAttributor:
    def test_attribute_returns_report(self):
        model = _make_mock_model([0.5, 0.3, 0.5])
        attr = InterventionAttributor(model)
        report = attr.attribute(SAMPLE_T0, SAMPLE_T1)

        assert isinstance(report, InterventionReport)
        assert report.risk_before == pytest.approx(0.5)
        assert report.risk_after == pytest.approx(0.3)
        assert report.risk_change == pytest.approx(-0.2)

    def test_counterfactual_decomposition(self):
        # T0=0.5, T1=0.3, counterfactual=0.5 (no non-modifiable change)
        model = _make_mock_model([0.5, 0.3, 0.5])
        attr = InterventionAttributor(model)
        report = attr.attribute(SAMPLE_T0, SAMPLE_T1)

        assert report.counterfactual_risk == pytest.approx(0.5)
        assert report.intervention_effect == pytest.approx(0.2)
        assert report.natural_change == pytest.approx(0.0)
        assert report.attribution_ratio == pytest.approx(1.0)

    def test_mixed_attribution(self):
        # T0=0.5, T1=0.3, counterfactual=0.45 (some natural improvement)
        # intervention = 0.45 - 0.3 = 0.15
        # natural = 0.45 - 0.5 = -0.05
        t1_with_age = dict(SAMPLE_T1)
        t1_with_age["age"] = 61
        model = _make_mock_model([0.5, 0.3, 0.45] + [0.3] * 20)
        attr = InterventionAttributor(model)
        report = attr.attribute(SAMPLE_T0, t1_with_age)

        assert report.intervention_effect == pytest.approx(0.15)
        assert report.natural_change == pytest.approx(-0.05)

    def test_no_change_scenario(self):
        model = _make_mock_model([0.4, 0.4, 0.4] + [0.4] * 20)
        attr = InterventionAttributor(model)
        report = attr.attribute(SAMPLE_T0, SAMPLE_T0)

        assert report.risk_change == pytest.approx(0.0)
        assert report.intervention_effect == pytest.approx(0.0)
        for f in report.feature_details:
            assert f.direction == "unchanged"

    def test_feature_details_cover_all_features(self):
        model = _make_mock_model([0.5, 0.3, 0.5] + [0.3] * 20)
        attr = InterventionAttributor(model)
        report = attr.attribute(SAMPLE_T0, SAMPLE_T1)

        feature_names = {f.feature for f in report.feature_details}
        expected = set(InterventionAttributor.FEATURE_COLS)
        assert feature_names == expected

    def test_modifiable_classification(self):
        model = _make_mock_model([0.5, 0.3, 0.5] + [0.3] * 20)
        attr = InterventionAttributor(model)
        report = attr.attribute(SAMPLE_T0, SAMPLE_T1)

        for f in report.feature_details:
            if f.feature in MODIFIABLE_FEATURES:
                assert f.is_modifiable is True
            else:
                assert f.is_modifiable is False

    def test_to_dict_has_required_keys(self):
        model = _make_mock_model([0.5, 0.3, 0.5] + [0.3] * 20)
        attr = InterventionAttributor(model)
        report = attr.attribute(SAMPLE_T0, SAMPLE_T1)
        d = report.to_dict()

        required = [
            "risk_before", "risk_after", "risk_change",
            "risk_level_before", "risk_level_after",
            "counterfactual_risk", "intervention_effect",
            "natural_change", "attribution_ratio",
            "feature_details", "summary",
        ]
        for key in required:
            assert key in d

    def test_grade_levels(self):
        assert InterventionAttributor._grade(0.1) == "low"
        assert InterventionAttributor._grade(0.35) == "moderate"
        assert InterventionAttributor._grade(0.55) == "high"
        assert InterventionAttributor._grade(0.85) == "very_high"
