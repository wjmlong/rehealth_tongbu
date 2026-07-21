"""Tests for HealthDesensitizer (edge-side data desensitization)."""

from bodyup_edge_sdk.core.desensitizer import HealthDesensitizer


SAMPLE_MEMORY = {
    "device_id": "dev_test_001",
    "disease_type": "CVD",
    "static_baseline": {
        "age": 55,
        "bmi": 26.5,
    },
    "dynamic_memory_vectors": {
        "sbp_mean": 138.0,
        "sbp_std": 12.3,
        "night_bp_pattern": "non_dipper",
        "active_calories_daily_mean": 210.0,
        "sleep_hours_mean": 6.5,
        "resting_hr_mean": 75.0,
        "hrv_rmssd_mean": 38.0,
    },
    "intervention_compliance": {
        "diet_compliance_rate": 0.72,
    },
    "risk_score_history": [
        {"date": "2026-05-01", "Y": 0.52},
        {"date": "2026-05-02", "Y": 0.50},
    ],
}


class TestDesensitizer:
    def test_output_structure(self):
        d = HealthDesensitizer()
        result = d.desensitize(SAMPLE_MEMORY)
        assert result["device_id"] == "dev_test_001"
        assert result["disease_type"] == "CVD"
        assert "memory_snapshot" in result
        assert "attribution_timeseries" in result

    def test_age_bucketed(self):
        d = HealthDesensitizer()
        result = d.desensitize(SAMPLE_MEMORY)
        assert result["memory_snapshot"]["age_bracket"] == 4  # 50-60

    def test_bmi_bucketed(self):
        d = HealthDesensitizer()
        result = d.desensitize(SAMPLE_MEMORY)
        assert result["memory_snapshot"]["bmi_level"] == "overweight"  # 24-28

    def test_bp_grade(self):
        d = HealthDesensitizer()
        result = d.desensitize(SAMPLE_MEMORY)
        assert result["memory_snapshot"]["bp_baseline_grade"] == "stage1"  # 130-140

    def test_activity_level(self):
        d = HealthDesensitizer()
        result = d.desensitize(SAMPLE_MEMORY)
        assert result["memory_snapshot"]["activity_level"] == "moderate"  # 200-350

    def test_no_raw_age_or_bmi(self):
        d = HealthDesensitizer()
        result = d.desensitize(SAMPLE_MEMORY)
        snap = result["memory_snapshot"]
        assert 55 not in snap.values()
        assert 26.5 not in snap.values()

    def test_timeseries_preserved(self):
        d = HealthDesensitizer()
        result = d.desensitize(SAMPLE_MEMORY)
        assert len(result["attribution_timeseries"]) == 2

    def test_empty_memory(self):
        d = HealthDesensitizer()
        result = d.desensitize({})
        assert result["device_id"] is None
        assert result["memory_snapshot"]["age_bracket"] == 0
        assert result["memory_snapshot"]["bmi_level"] == "unknown"

    def test_age_brackets_coverage(self):
        d = HealthDesensitizer()
        cases = [(25, 1), (35, 2), (45, 3), (55, 4), (65, 5), (75, 6)]
        for age, expected in cases:
            mem = {"static_baseline": {"age": age}}
            result = d.desensitize(mem)
            assert result["memory_snapshot"]["age_bracket"] == expected, f"age={age}"
