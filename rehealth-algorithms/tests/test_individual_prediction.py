"""Tests for IndividualRiskPredictor (Level 1 attribution)."""

import numpy as np
from bodyup_cloud.engine.individual_prediction import IndividualRiskPredictor


def _make_history(n_days: int, base: float = 0.5, slope: float = -0.003):
    rng = np.random.default_rng(42)
    return [
        {
            "date": f"2026-05-{i + 1:02d}",
            "Y": float(np.clip(base + slope * i + rng.normal(0, 0.01), 0, 1)),
            "Z": int(i % 3 == 0),
        }
        for i in range(n_days)
    ]


class TestIndividualRiskPredictor:
    def test_accumulating_when_short(self):
        predictor = IndividualRiskPredictor({"min_history_days": 14})
        result = predictor.predict(_make_history(5))
        assert result["status"] == "accumulating"
        assert result["days_available"] == 5
        assert result["days_needed"] == 14

    def test_ready_with_sufficient_data(self):
        predictor = IndividualRiskPredictor({"min_history_days": 14, "forecast_days": 30})
        result = predictor.predict(_make_history(30))
        assert result["status"] == "ready"
        assert result["history_days"] == 30
        assert len(result["forecast_status_quo"]) == 30
        assert len(result["forecast_with_plan"]) == 30

    def test_forecast_values_bounded(self):
        predictor = IndividualRiskPredictor()
        result = predictor.predict(_make_history(30, base=0.5, slope=-0.003))
        for v in result["forecast_status_quo"]:
            assert 0.0 <= v <= 1.0
        for v in result["forecast_with_plan"]:
            assert 0.0 <= v <= 1.0

    def test_declining_trend_has_negative_slope(self):
        predictor = IndividualRiskPredictor()
        result = predictor.predict(_make_history(60, base=0.6, slope=-0.005))
        assert result["trend_slope_overall"] < 0

    def test_rising_trend_has_positive_slope(self):
        predictor = IndividualRiskPredictor()
        result = predictor.predict(_make_history(60, base=0.3, slope=0.004))
        assert result["trend_slope_overall"] > 0

    def test_retrospective_att_present_when_enough_data(self):
        history = _make_history(60)
        n_int = sum(1 for r in history if r["Z"] == 1)
        n_ctrl = sum(1 for r in history if r["Z"] == 0)
        predictor = IndividualRiskPredictor()
        result = predictor.predict(history)
        if n_int >= 7 and n_ctrl >= 7:
            assert result["retrospective_att"] is not None
        else:
            assert result["retrospective_att"] is None

    def test_confidence_interval_present(self):
        predictor = IndividualRiskPredictor()
        result = predictor.predict(_make_history(30))
        assert result["status"] == "ready"
        assert "forecast_ci_upper" in result
        assert "forecast_ci_lower" in result
        assert len(result["forecast_ci_upper"]) == 30
        assert len(result["forecast_ci_lower"]) == 30
        for lo, mid, hi in zip(
            result["forecast_ci_lower"],
            result["forecast_status_quo"],
            result["forecast_ci_upper"],
        ):
            assert lo <= mid + 1e-9
            assert mid <= hi + 1e-9

    def test_insufficient_intervention_flag(self):
        history = [
            {"date": f"2026-05-{i+1:02d}", "Y": 0.5, "Z": 0}
            for i in range(20)
        ]
        history[0]["Z"] = 1
        history[1]["Z"] = 1
        predictor = IndividualRiskPredictor()
        result = predictor.predict(history)
        assert result["status"] == "ready"
        assert result["intervention_data_sufficient"] is False

    def test_report_text_present(self):
        predictor = IndividualRiskPredictor()
        result = predictor.predict(_make_history(30))
        assert "report_text" in result
        report = result["report_text"]
        assert "headline" in report
        assert "body" in report
        assert "advice" in report
        assert "metrics" in report
        assert "confidence_band_width" in report["metrics"]
