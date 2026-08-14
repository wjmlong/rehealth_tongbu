"""End-to-end tests for the standardized PSM/RWE Draft engine."""

from datetime import date

import numpy as np

from healthagent.pias.attribution.psm import CVD_BASELINE_FEATURES, PSMEngine
from healthagent.pias.insurance.settlement_engine import SettlementEngine


def _cohort(n=80):
    rng = np.random.default_rng(7)
    rows = []
    for i in range(n):
        z = int(rng.random() > 0.5)
        age = 45 + (i % 12)
        features = {
            "age": age,
            "gender": i % 2,
            "bmi": 24.0 + (i % 5) * 0.2,
            "sbp": 125.0 + (i % 7),
            "dbp": 78.0 + (i % 4),
            "fasting_glucose": 5.2 + (i % 4) * 0.1,
            "total_cholesterol": 4.6 + (i % 5) * 0.1,
            "ldl": 2.5 + (i % 5) * 0.1,
            "hdl": 1.3 + (i % 3) * 0.05,
            "triglycerides": 1.4 + (i % 4) * 0.1,
            "exercise_days": i % 5,
            "smoking": i % 3 == 0,
            "drinking": i % 4 == 0,
            "diabetes_history": i % 11 == 0,
            "hypertension_history": i % 9 == 0,
            "family_history": i % 6 == 0,
        }
        outcome = rng.normal(0, 0.02) - (0.10 if z else 0)
        rows.append({"member_id": f"m-{i}", "Z": z, "outcome": float(outcome), "features": features})
    return rows


def test_standard_engine_reports_before_and_after_balance():
    result = PSMEngine({"n_bootstrap": 200, "min_matched_pairs": 20}).run(_cohort())

    assert result["status"] == "success"
    assert result["engine_version"].startswith("psm-rwe-")
    assert result["caliper_scale"] == "logit_propensity"
    assert result["p_value_method"] == "paired t-test on matched differences"
    assert result["att"] < 0
    assert all("smd_before" in value and "smd_after" in value for value in result["balance"].values())
    assert len(result["snapshot_hash"]) == 64


def test_settlement_requires_explicit_financial_terms():
    psm = PSMEngine({"n_bootstrap": 200, "min_matched_pairs": 20}).run(_cohort())
    report = SettlementEngine().generate_settlement_report(
        psm,
        {"insurer_name": "Demo Insurer"},
        {"start": date(2026, 1, 1), "end": date(2026, 12, 31)},
    )

    assert report.report_status == "draft"
    assert report.financial_impact.settlement_amount == 0
    assert report.quality_gates["financial_terms_complete"] is False
    assert "不得直接用于正式结算" in report.to_markdown()


def test_settlement_formula_uses_effective_units_and_unit_value():
    psm = PSMEngine({"n_bootstrap": 200, "min_matched_pairs": 20}).run(_cohort())
    report = SettlementEngine().generate_settlement_report(
        psm,
        {"insurer_name": "Demo Insurer"},
        financial_terms={
            "outcome_unit": "rmb_per_member",
            "effective_treated_units": 100,
            "unit_value": 50000,
            "service_cost": 1000,
            "sharing_ratio": 0.5,
        },
    )

    expected_gross = (-psm["att"]) * 100 * 50000
    assert np.isclose(report.financial_impact.gross_savings, expected_gross)
    assert np.isclose(
        report.financial_impact.settlement_amount,
        max(expected_gross - 1000, 0) * 0.5,
    )
    assert report.quality_gates["financial_terms_complete"] is True


def test_invalid_financial_terms_do_not_enable_settlement():
    psm = PSMEngine({"n_bootstrap": 200, "min_matched_pairs": 20}).run(_cohort())
    report = SettlementEngine().generate_settlement_report(
        psm,
        {"insurer_name": "Demo Insurer"},
        financial_terms={
            "outcome_unit": "rmb_per_member",
            "effective_treated_units": 100,
            "unit_value": -1,
            "sharing_ratio": 1.5,
        },
    )

    assert report.financial_impact.settlement_amount == 0
    assert report.quality_gates["financial_terms_complete"] is True
    assert report.quality_gates["financial_terms_valid"] is False
    assert report.quality_gates["settlement_ready"] is False
