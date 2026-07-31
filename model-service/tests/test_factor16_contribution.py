import pytest

from app.factor16_contribution import FACTOR16_RULE_VERSION, Factor16ContributionEngine
from app.schemas import CvdFeatureVector, FeatureQuality, FeatureQualityStatus, FeatureSource


def quality(
    source: FeatureSource,
    *,
    reason: str = "test",
    observed_at: int | None = None,
) -> FeatureQuality:
    return FeatureQuality(
        status=FeatureQualityStatus.VALID,
        source=source,
        reason=reason,
        observedAt=observed_at,
    )


def vector(**overrides) -> CvdFeatureVector:
    values = {
        "age": 55,
        "gender": 1,
        "bmi": 29.0,
        "sbp": 145.0,
        "dbp": 90.0,
        "fasting_glucose": 6.6,
        "total_cholesterol": 5.7,
        "ldl": 3.9,
        "hdl": 0.8,
        "triglycerides": 2.2,
        "exercise_days": 3,
        "smoking": 0,
        "drinking": 0,
        "diabetes_history": 0,
        "hypertension_history": 1,
        "family_history": 1,
    }
    values.update(overrides)
    qualities = {
        field: quality(FeatureSource.USER_REPORTED)
        for field in values
    }
    for field in {"sbp", "dbp"}:
        qualities[field] = quality(
            FeatureSource.CLINICAL_REPORT,
            reason="Validated upper-arm cuff 7-day mean from 5 valid days.",
        )
    for field in {"fasting_glucose", "total_cholesterol", "ldl", "hdl", "triglycerides"}:
        qualities[field] = quality(FeatureSource.CLINICAL_REPORT, observed_at=1_700_000_000_000)
    qualities["exercise_days"] = quality(FeatureSource.REAL_DEVICE)
    values["featureQuality"] = qualities
    return CvdFeatureVector.model_validate(values)


def test_engine_uses_factor16_version_and_all_available_fields():
    result = Factor16ContributionEngine(now_millis=1_700_000_000_000).evaluate(vector())

    assert result.rule_version == FACTOR16_RULE_VERSION
    assert set(result.contributions) == {
        "age",
        "gender",
        "bmi",
        "sbp",
        "dbp",
        "fasting_glucose",
        "total_cholesterol",
        "ldl",
        "hdl",
        "triglycerides",
        "exercise_days",
        "smoking",
        "drinking",
        "diabetes_history",
        "hypertension_history",
        "family_history",
    }


def test_clinical_cards_apply_80_percent_measured_and_zero_unverified_support():
    result = Factor16ContributionEngine(now_millis=1_700_000_000_000).evaluate(vector())

    assert result.control_support_components["ldl"] == 0.0
    assert result.contributions["ldl"] == result.measured_components["ldl"]
    assert result.contributions["ldl"] == pytest.approx(0.8 * 1.2)


def test_hypoglycemia_is_not_rewarded_with_protective_points():
    result = Factor16ContributionEngine(now_millis=1_700_000_000_000).evaluate(
        vector(fasting_glucose=3.5),
    )

    assert result.contributions["fasting_glucose"] == 0.0


def test_unvalidated_wearable_blood_pressure_is_omitted():
    candidate = vector()
    candidate.feature_quality["sbp"] = quality(
        FeatureSource.REAL_DEVICE,
        reason="Most recent plausible ring blood pressure measurement.",
    )
    candidate.feature_quality["dbp"] = candidate.feature_quality["sbp"]

    result = Factor16ContributionEngine(now_millis=1_700_000_000_000).evaluate(candidate)

    assert "sbp" not in result.contributions
    assert "dbp" not in result.contributions


def test_missing_lab_is_not_treated_as_zero_or_normal():
    result = Factor16ContributionEngine(now_millis=1_700_000_000_000).evaluate(
        vector(ldl=None),
    )

    assert "ldl" not in result.contributions
    assert "ldl" not in result.measured_components
