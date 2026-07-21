"""Tests for GroupAttributionEngine (Level 2 PSM + DRE + Bootstrap)."""

import numpy as np
from bodyup_cloud.engine.group_attribution import GroupAttributionEngine


def _make_cohort(n: int = 80, treatment_effect: float = -0.08):
    rng = np.random.default_rng(42)
    records = []
    for i in range(n):
        Z = int(rng.random() > 0.5)
        base_delta = rng.normal(0.0, 0.05)
        delta_Y = base_delta + (treatment_effect if Z == 1 else 0)
        records.append(
            {
                "device_id": f"dev_{i:04d}",
                "Z": Z,
                "delta_Y": float(delta_Y),
                "features": {
                    "age_bracket": int(rng.choice([1, 2, 3, 4, 5])),
                    "bmi_level_encoded": int(rng.choice([0, 1, 2, 3])),
                    "bp_baseline_grade_encoded": int(rng.choice([0, 1, 2])),
                    "activity_level_encoded": int(rng.choice([0, 1, 2, 3])),
                    "gender_encoded": int(rng.choice([0, 1])),
                    "season_sin": float(rng.normal(0, 0.5)),
                    "season_cos": float(rng.normal(0, 0.5)),
                },
            }
        )
    return records


class TestGroupAttributionEngine:
    def test_insufficient_data(self):
        engine = GroupAttributionEngine({"min_group_size": 30})
        result = engine.estimate(_make_cohort(15))
        assert result["status"] == "insufficient_data"

    def test_insufficient_groups(self):
        records = _make_cohort(40)
        for r in records:
            r["Z"] = 1
        records[0]["Z"] = 0
        engine = GroupAttributionEngine()
        result = engine.estimate(records)
        assert result["status"] == "insufficient_groups"

    def test_success_structure(self):
        engine = GroupAttributionEngine({"n_bootstrap": 50, "min_group_size": 30})
        result = engine.estimate(_make_cohort(100, treatment_effect=-0.10))
        assert result["status"] == "success"
        assert "att" in result
        assert "ci_lower" in result
        assert "ci_upper" in result
        assert result["ci_lower"] <= result["att"] <= result["ci_upper"]
        assert result["n_total_users"] == 100
        assert "matching_balance" in result

    def test_att_direction_with_strong_effect(self):
        engine = GroupAttributionEngine({"n_bootstrap": 50})
        result = engine.estimate(_make_cohort(200, treatment_effect=-0.15))
        if result["status"] == "success":
            assert result["att"] < 0

    def test_balance_diagnostics(self):
        engine = GroupAttributionEngine({"n_bootstrap": 50})
        result = engine.estimate(_make_cohort(120))
        if result["status"] == "success":
            balance = result["matching_balance"]
            for feat, info in balance.items():
                assert "smd" in info
                assert "balanced" in info

    def test_gamma_sensitivity_present(self):
        engine = GroupAttributionEngine({"n_bootstrap": 50})
        result = engine.estimate(_make_cohort(100, treatment_effect=-0.10))
        if result["status"] == "success":
            assert "gamma_sensitivity" in result
            assert isinstance(result["gamma_sensitivity"], float)
            assert result["gamma_sensitivity"] >= 1.0
            assert "sensitivity_interpretation" in result

    def test_settlement_report_has_gamma(self):
        engine = GroupAttributionEngine({"n_bootstrap": 50})
        result = engine.estimate(_make_cohort(100, treatment_effect=-0.10))
        if result["status"] == "success":
            report = result["settlement_report"]
            assert "gamma_sensitivity" in report["metrics"]
            assert "Rosenbaum" in report["method"]

    def test_adaptive_caliper(self):
        engine = GroupAttributionEngine({"caliper": "auto", "n_bootstrap": 50})
        result = engine.estimate(_make_cohort(100, treatment_effect=-0.10))
        assert result["status"] in ("success", "insufficient_matches")
