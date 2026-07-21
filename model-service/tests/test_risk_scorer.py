import pytest
import pickle
from pydantic import ValidationError

from app.risk_scorer import (
    FEATURE_FIELDS,
    FEATURE_ORDER_ARTIFACT_CANDIDATES,
    MOCK_MODEL_VERSION,
    MODEL_ARTIFACT_CANDIDATES,
    ModelUnavailableError,
    MockRiskScorer,
    RealCatBoostRiskScorer,
    RealModelArtifacts,
    load_risk_scorer,
    validate_real_model_artifacts,
)
from app.schemas import CvdFeatureVector, FeatureQuality, FeatureQualityStatus, FeatureSource


class FakeProbabilityModel:
    def predict_proba(self, rows):
        return [[0.21, 0.79] for _ in rows]


def quality(status=FeatureQualityStatus.VALID, source=FeatureSource.USER_REPORTED):
    return FeatureQuality(status=status, source=source, reason="test")


def complete_vector(**overrides):
    data = {
        "age": 55,
        "gender": 1,
        "bmi": 29.0,
        "sbp": 146.0,
        "dbp": 92.0,
        "fasting_glucose": 6.4,
        "total_cholesterol": 5.4,
        "ldl": 3.6,
        "hdl": 1.0,
        "triglycerides": 1.9,
        "exercise_days": 1,
        "smoking": 1,
        "drinking": 0,
        "diabetes_history": 0,
        "hypertension_history": 1,
        "family_history": 1,
    }
    data.update(overrides)
    data["featureQuality"] = {key: quality() for key in data if key != "featureQuality"}
    return CvdFeatureVector.model_validate(data)


def artifact_alias_config(artifact_root):
    return RealModelArtifacts(
        artifact_root=artifact_root,
        model_paths=(
            artifact_root / "rehealth_cvd_catboost.pkl",
            artifact_root / "rehealth_v2_final.pkl",
        ),
        feature_order_paths=(
            artifact_root / "feature_cols.pkl",
            artifact_root / "feature_cols_v2.pkl",
            artifact_root / "cvd_features.json",
        ),
        metadata_paths=(
            artifact_root / "model_meta_v2.json",
            artifact_root / "model_metadata.json",
        ),
    )


def test_artifact_alias_constants_include_normalized_and_historical_names():
    aliases = [path.as_posix() for path in MODEL_ARTIFACT_CANDIDATES]

    assert aliases == [
        "models/rehealth_cvd_catboost.pkl",
        "models/rehealth_v2_final.pkl",
    ]


def test_feature_order_alias_constants_include_pickle_and_json_names():
    aliases = [path.as_posix() for path in FEATURE_ORDER_ARTIFACT_CANDIDATES]

    assert aliases == [
        "models/feature_cols.pkl",
        "models/feature_cols_v2.pkl",
        "models/cvd_features.json",
    ]


def test_risk_response_contains_model_version_and_all_contributions():
    result = MockRiskScorer().evaluate(complete_vector())

    assert result.model_version == MOCK_MODEL_VERSION
    assert result.is_mock is True
    assert 0.0 <= result.risk_score <= 1.0
    assert result.risk_level in {"low", "moderate", "high", "very_high"}
    assert "sbp" in result.feature_contributions
    assert len(result.feature_contributions) == 16
    assert result.model_trace is not None
    assert result.model_trace.feature_schema_version == "cvd-16-v1"
    assert result.model_trace.scorer_mode == "mock"
    assert result.model_trace.fallback_reason == "mock scorer active"


def test_missing_fields_are_reported_without_rejecting_payload():
    vector = complete_vector(fasting_glucose=None, total_cholesterol=None)
    result = MockRiskScorer().evaluate(vector)

    assert "fasting_glucose" in result.missing_fields
    assert "total_cholesterol" in result.missing_fields
    assert result.model_version == MOCK_MODEL_VERSION


def test_quality_warnings_include_stale_and_low_confidence_fields():
    vector = complete_vector()
    vector.feature_quality["sbp"] = quality(FeatureQualityStatus.STALE, FeatureSource.REAL_DEVICE)
    vector.feature_quality["ldl"] = quality(FeatureQualityStatus.LOW_CONFIDENCE, FeatureSource.CLINICAL_REPORT)

    result = MockRiskScorer().evaluate(vector)

    assert "sbp:STALE" in result.quality_warnings
    assert "ldl:LOW_CONFIDENCE" in result.quality_warnings


def test_feature_vector_rejects_invalid_c1_values():
    with pytest.raises(ValidationError):
        complete_vector(age=17)

    with pytest.raises(ValidationError):
        complete_vector(sbp=80, dbp=90)

    with pytest.raises(ValidationError):
        complete_vector(exercise_days=8)


def test_feature_vector_requires_quality_for_all_canonical_fields():
    data = complete_vector().model_dump()
    data["feature_quality"].pop("age")

    with pytest.raises(ValidationError):
        CvdFeatureVector.model_validate(data)


def test_missing_artifacts_do_not_enable_real_scoring(tmp_path):
    artifact_root = tmp_path / "models"
    artifact_root.mkdir()
    artifacts = RealModelArtifacts(
        model_path=artifact_root / "rehealth_cvd_catboost.pkl",
        feature_order_path=artifact_root / "feature_cols.pkl",
        metadata_path=artifact_root / "model_meta_v2.json",
        artifact_root=artifact_root,
    )

    scorer = load_risk_scorer(artifacts)

    assert isinstance(scorer, MockRiskScorer)
    assert scorer.is_mock is True
    assert scorer.scorer_mode == "real_unavailable"
    assert "model artifact missing" in (scorer.model_unavailable_reason or "")


def test_feature_order_mismatch_marks_real_model_unavailable(tmp_path):
    artifact_root = tmp_path / "models"
    artifact_root.mkdir()
    model_path = artifact_root / "rehealth_cvd_catboost.pkl"
    feature_order_path = artifact_root / "feature_cols.pkl"
    model_path.write_bytes(pickle.dumps(FakeProbabilityModel()))
    feature_order_path.write_bytes(pickle.dumps(["age", "gender"]))
    artifacts = RealModelArtifacts(
        model_path=model_path,
        feature_order_path=feature_order_path,
        metadata_path=artifact_root / "model_meta_v2.json",
        artifact_root=artifact_root,
    )

    validation = validate_real_model_artifacts(artifacts)
    scorer = load_risk_scorer(artifacts)

    assert validation.available is False
    assert "feature order mismatch" in (validation.reason or "")
    assert scorer.is_mock is True
    assert scorer.scorer_mode == "real_unavailable"


def test_real_scorer_loads_only_when_artifacts_validate(tmp_path):
    artifact_root = tmp_path / "models"
    artifact_root.mkdir()
    model_path = artifact_root / "rehealth_cvd_catboost.pkl"
    feature_order_path = artifact_root / "feature_cols.pkl"
    metadata_path = artifact_root / "model_meta_v2.json"
    model_path.write_bytes(pickle.dumps(FakeProbabilityModel()))
    feature_order_path.write_bytes(pickle.dumps(FEATURE_FIELDS))
    metadata_path.write_text('{"model_version":"test-real-v1"}', encoding="utf-8")
    artifacts = RealModelArtifacts(
        model_path=model_path,
        feature_order_path=feature_order_path,
        metadata_path=metadata_path,
        artifact_root=artifact_root,
    )

    scorer = load_risk_scorer(artifacts)
    result = scorer.evaluate(complete_vector())

    assert isinstance(scorer, RealCatBoostRiskScorer)
    assert scorer.scorer_mode == "real_available"
    assert result.is_mock is False
    assert result.risk_score == 0.79
    assert result.model_version == "test-real-v1"
    assert result.contribution_method == "deterministic_zero_fallback"
    assert result.model_trace is not None
    assert result.model_trace.model_version == "test-real-v1"
    assert result.model_trace.feature_schema_version == "cvd-16-v1"
    assert result.model_trace.artifact_name == "rehealth_cvd_catboost.pkl"
    assert result.model_trace.scorer_mode == "real_available"
    assert result.model_trace.fallback_reason is None


def test_reviewed_calibrated_model_returns_native_shap_contributions():
    scorer = load_risk_scorer()

    result = scorer.evaluate(complete_vector())

    assert isinstance(scorer, RealCatBoostRiskScorer)
    assert result.contribution_method == "shap_via_catboost"
    assert result.base_value is not None
    assert set(result.feature_contributions) == set(FEATURE_FIELDS)
    assert any(value != 0.0 for value in result.feature_contributions.values())


def test_historical_model_alias_loads_when_feature_cols_v2_validates(tmp_path):
    artifact_root = tmp_path / "models"
    artifact_root.mkdir()
    historical_model_path = artifact_root / "rehealth_v2_final.pkl"
    feature_order_path = artifact_root / "feature_cols_v2.pkl"
    historical_model_path.write_bytes(pickle.dumps(FakeProbabilityModel()))
    feature_order_path.write_bytes(pickle.dumps(FEATURE_FIELDS))

    scorer = load_risk_scorer(artifact_alias_config(artifact_root))
    result = scorer.evaluate(complete_vector())

    assert isinstance(scorer, RealCatBoostRiskScorer)
    assert scorer.loaded_artifact_name == "rehealth_v2_final.pkl"
    assert scorer.model_version == "cvd-catboost-v2"
    assert scorer.model_auc is None
    assert result.is_mock is False
    assert result.risk_score == 0.79
    assert result.model_trace is not None
    assert result.model_trace.artifact_name == "rehealth_v2_final.pkl"


def test_cvd_features_json_alias_validates_feature_order(tmp_path):
    artifact_root = tmp_path / "models"
    artifact_root.mkdir()
    model_path = artifact_root / "rehealth_cvd_catboost.pkl"
    feature_order_path = artifact_root / "cvd_features.json"
    model_path.write_bytes(pickle.dumps(FakeProbabilityModel()))
    feature_order_path.write_text('{"feature_cols": ' + str(FEATURE_FIELDS).replace("'", '"') + "}", encoding="utf-8")

    validation = validate_real_model_artifacts(artifact_alias_config(artifact_root))
    scorer = load_risk_scorer(artifact_alias_config(artifact_root))

    assert validation.available is True
    assert validation.feature_order_path == feature_order_path
    assert isinstance(scorer, RealCatBoostRiskScorer)


def test_model_metadata_alias_can_supply_version_without_default_auc(tmp_path):
    artifact_root = tmp_path / "models"
    artifact_root.mkdir()
    model_path = artifact_root / "rehealth_cvd_catboost.pkl"
    feature_order_path = artifact_root / "feature_cols.pkl"
    metadata_path = artifact_root / "model_metadata.json"
    model_path.write_bytes(pickle.dumps(FakeProbabilityModel()))
    feature_order_path.write_bytes(pickle.dumps(FEATURE_FIELDS))
    metadata_path.write_text('{"model_version":"metadata-alias-v1"}', encoding="utf-8")

    scorer = load_risk_scorer(artifact_alias_config(artifact_root))

    assert isinstance(scorer, RealCatBoostRiskScorer)
    assert scorer.model_version == "metadata-alias-v1"
    assert scorer.model_auc is None


def test_reviewed_metadata_shape_supplies_top_level_model_auc(tmp_path):
    artifact_root = tmp_path / "models"
    artifact_root.mkdir()
    model_path = artifact_root / "rehealth_cvd_catboost.pkl"
    feature_order_path = artifact_root / "feature_cols.pkl"
    metadata_path = artifact_root / "model_meta_v2.json"
    model_path.write_bytes(pickle.dumps(FakeProbabilityModel()))
    feature_order_path.write_bytes(pickle.dumps(FEATURE_FIELDS))
    metadata_path.write_text(
        '{"model_version":"reviewed-core16-v1","model_auc":0.84,'
        '"artifact_name":"rehealth_cvd_catboost.pkl"}',
        encoding="utf-8",
    )

    scorer = load_risk_scorer(artifact_alias_config(artifact_root))

    assert isinstance(scorer, RealCatBoostRiskScorer)
    assert scorer.model_version == "reviewed-core16-v1"
    assert scorer.model_auc == 0.84


def test_real_scorer_rejects_bad_feature_order_directly():
    with pytest.raises(ModelUnavailableError):
        RealCatBoostRiskScorer(
            model=FakeProbabilityModel(),
            feature_order=["age", "gender"],
            model_version="bad-order",
        )
