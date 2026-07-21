"""Tests for HealthFeatureExtractor (edge-side feature extraction)."""

import numpy as np
from datetime import datetime
from bodyup_edge_sdk.core.feature_extractor import HealthFeatureExtractor


def _make_bp_series(n_days: int = 14, base_sbp: float = 130.0, base_dbp: float = 80.0):
    rng = np.random.default_rng(0)
    hours = n_days * 24
    sbp = base_sbp + rng.normal(0, 5, hours)
    dbp = base_dbp + rng.normal(0, 3, hours)
    return sbp.tolist(), dbp.tolist()


class TestBPFeatures:
    def test_returns_expected_keys(self):
        sbp, dbp = _make_bp_series()
        ext = HealthFeatureExtractor(window_days=14)
        result = ext.extract_bp_features(sbp, dbp)
        assert "sbp_mean" in result
        assert "sbp_std" in result
        assert "sbp_slope" in result
        assert "dbp_mean" in result
        assert "night_bp_pattern" in result

    def test_empty_input(self):
        ext = HealthFeatureExtractor()
        result = ext.extract_bp_features([], [])
        assert result == {}

    def test_short_input(self):
        ext = HealthFeatureExtractor()
        result = ext.extract_bp_features([120.0] * 10, [80.0] * 10)
        assert result == {}

    def test_night_bp_pattern_valid(self):
        sbp, dbp = _make_bp_series()
        ext = HealthFeatureExtractor()
        result = ext.extract_bp_features(sbp, dbp)
        assert result["night_bp_pattern"] in (
            "extreme_dipper", "dipper", "non_dipper", "reverse_dipper"
        )


class TestCardiacFeatures:
    def test_basic(self):
        ext = HealthFeatureExtractor(window_days=7)
        hr = [72.0] * (7 * 24)
        hrv = [45.0] * 7
        result = ext.extract_cardiac_features(hr, hrv)
        assert abs(result["resting_hr_mean"] - 72.0) < 0.1
        assert abs(result["hrv_rmssd_mean"] - 45.0) < 0.1


class TestActivityFeatures:
    def test_basic(self):
        ext = HealthFeatureExtractor(window_days=7)
        calories = [250.0] * 7
        sports = [{"type": "run"}, {"type": "walk"}]
        sleep = [7.5] * 7
        result = ext.extract_activity_features(calories, sports, sleep)
        assert abs(result["active_calories_daily_mean"] - 250.0) < 0.1
        assert result["sport_freq_weekly"] == 2.0
        assert abs(result["sleep_hours_mean"] - 7.5) < 0.1


class TestTimeConfounders:
    def test_output_keys(self):
        result = HealthFeatureExtractor.encode_time_confounders(datetime(2026, 6, 21))
        assert "season_sin" in result
        assert "season_cos" in result
        assert "day_of_week" in result
        assert "is_weekend" in result

    def test_summer_sin_positive(self):
        result = HealthFeatureExtractor.encode_time_confounders(datetime(2026, 7, 1))
        assert result["season_sin"] > 0

    def test_weekend_flag(self):
        result = HealthFeatureExtractor.encode_time_confounders(datetime(2026, 5, 24))  # Sunday
        assert result["is_weekend"] == 1
        result2 = HealthFeatureExtractor.encode_time_confounders(datetime(2026, 5, 25))  # Monday
        assert result2["is_weekend"] == 0


class TestExtractAll:
    def test_combined(self):
        ext = HealthFeatureExtractor(window_days=14)
        sbp, dbp = _make_bp_series()
        hr = [70.0] * (14 * 24)
        hrv = [42.0] * 14
        cal = [300.0] * 14
        sports = [{"type": "run"}] * 5
        sleep = [7.0] * 14
        result = ext.extract_all(sbp, dbp, hr, hrv, cal, sports, sleep)
        assert "sbp_mean" in result
        assert "resting_hr_mean" in result
        assert "active_calories_daily_mean" in result
        assert "season_sin" in result
