"""Tests for CVDRiskScorer and ModelRegistry (no real model file needed)."""

import pytest
import numpy as np
from unittest.mock import MagicMock, patch

from bodyup_cloud.engine.risk_scorer import CVDRiskScorer, ModelRegistry


SAMPLE_FEATURES = {
    "age": 55,
    "gender": 1,
    "bmi": 27.3,
    "sbp": 140,
    "dbp": 88,
    "fasting_glucose": 5.8,
    "total_cholesterol": 5.2,
    "ldl": 3.4,
    "hdl": 1.1,
    "triglycerides": 1.9,
    "exercise_days": 2,
    "smoking": 1,
    "drinking": 0,
    "diabetes_history": 0,
    "hypertension_history": 1,
    "family_history": 0,
}


def _make_mock_model():
    model = MagicMock()
    model.predict_proba.return_value = np.array([[0.35, 0.65]])
    return model


def _make_mock_explainer(n_features=16):
    explainer = MagicMock()
    sv = np.random.default_rng(0).normal(0, 0.05, n_features)
    explainer.shap_values.return_value = [
        np.array([sv * -1]),
        np.array([sv]),
    ]
    return explainer


class TestCVDRiskScorer:
    @patch("bodyup_cloud.engine.risk_scorer.shap.TreeExplainer")
    @patch("bodyup_cloud.engine.risk_scorer.joblib.load")
    def test_predict_returns_expected_keys(self, mock_load, mock_tree_exp):
        mock_load.return_value = _make_mock_model()
        mock_tree_exp.return_value = _make_mock_explainer()

        scorer = CVDRiskScorer("fake_path.pkl")
        result = scorer.predict(SAMPLE_FEATURES)

        assert "risk_score" in result
        assert "risk_level" in result
        assert "feature_contributions" in result
        assert "model_version" in result
        assert "model_auc" in result
        assert 0.0 <= result["risk_score"] <= 1.0
        assert len(result["feature_contributions"]) == 16

    @patch("bodyup_cloud.engine.risk_scorer.shap.TreeExplainer")
    @patch("bodyup_cloud.engine.risk_scorer.joblib.load")
    def test_risk_level_grading(self, mock_load, mock_tree_exp):
        mock_model = _make_mock_model()
        mock_load.return_value = mock_model
        mock_tree_exp.return_value = _make_mock_explainer()

        scorer = CVDRiskScorer("fake.pkl")

        mock_model.predict_proba.return_value = np.array([[0.85, 0.15]])
        assert scorer.predict(SAMPLE_FEATURES)["risk_level"] == "low"

        mock_model.predict_proba.return_value = np.array([[0.60, 0.40]])
        assert scorer.predict(SAMPLE_FEATURES)["risk_level"] == "moderate"

        mock_model.predict_proba.return_value = np.array([[0.40, 0.60]])
        assert scorer.predict(SAMPLE_FEATURES)["risk_level"] == "high"

        mock_model.predict_proba.return_value = np.array([[0.15, 0.85]])
        assert scorer.predict(SAMPLE_FEATURES)["risk_level"] == "very_high"


class TestModelRegistry:
    def test_register_and_list(self):
        registry = ModelRegistry()
        assert registry.list_models() == []

    def test_get_missing_raises(self):
        registry = ModelRegistry()
        with pytest.raises(KeyError, match="default"):
            registry.get("default")
