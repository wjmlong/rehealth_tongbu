from __future__ import annotations

import json
import os
from abc import ABC, abstractmethod
from dataclasses import dataclass
import logging
from math import isfinite, sigmoid
from pathlib import Path
from typing import Any, Sequence

import joblib
from catboost import Pool

logger = logging.getLogger(__name__)

from app.model_registry import ModelRegistry
from app.schemas import (
    FEATURE_FIELDS,
    CvdFeatureVector,
    FeatureQualityStatus,
    RiskEvaluateResponse,
)


MOCK_MODEL_VERSION = "cvd-mock-rules-v1"
REAL_MODEL_VERSION_FALLBACK = "cvd-catboost-v2"
LOCAL_ARTIFACT_ROOT = Path("models")
REAL_CONTRIBUTION_FALLBACK = "deterministic_zero_fallback"
REAL_CONTRIBUTION_SHAP = "shap_via_catboost"
MODEL_ARTIFACT_CANDIDATES = (
    Path("models/rehealth_cvd_catboost.pkl"),
    Path("models/rehealth_v2_final.pkl"),
)
FEATURE_ORDER_ARTIFACT_CANDIDATES = (
    Path("models/feature_cols.pkl"),
    Path("models/feature_cols_v2.pkl"),
    Path("models/cvd_features.json"),
)
MODEL_METADATA_CANDIDATES = (
    Path("models/model_meta_v2.json"),
    Path("models/model_metadata.json"),
)
DEFAULT_MODEL_PATH = MODEL_ARTIFACT_CANDIDATES[0]
DEFAULT_FEATURE_ORDER_PATH = FEATURE_ORDER_ARTIFACT_CANDIDATES[0]
DEFAULT_MODEL_META_PATH = MODEL_METADATA_CANDIDATES[0]


class ModelUnavailableError(RuntimeError):
    """Raised when a real model path was selected but cannot safely score."""


@dataclass(frozen=True)
class RealModelArtifacts:
    artifact_root: Path = LOCAL_ARTIFACT_ROOT
    model_path: Path | None = None
    feature_order_path: Path | None = None
    metadata_path: Path | None = None
    model_paths: Sequence[Path] = MODEL_ARTIFACT_CANDIDATES
    feature_order_paths: Sequence[Path] = FEATURE_ORDER_ARTIFACT_CANDIDATES
    metadata_paths: Sequence[Path] = MODEL_METADATA_CANDIDATES

    def model_candidates(self) -> list[Path]:
        return [self.model_path] if self.model_path is not None else list(self.model_paths)

    def feature_order_candidates(self) -> list[Path]:
        if self.feature_order_path is not None:
            return [self.feature_order_path]
        return list(self.feature_order_paths)

    def metadata_candidates(self) -> list[Path]:
        return [self.metadata_path] if self.metadata_path is not None else list(self.metadata_paths)


@dataclass(frozen=True)
class ArtifactValidationResult:
    available: bool
    reason: str | None
    feature_order: list[str] | None = None
    model_version: str | None = None
    model_path: Path | None = None
    feature_order_path: Path | None = None
    metadata_path: Path | None = None
    loaded_artifact_name: str | None = None
    model_auc: float | None = None
    expected_model_artifacts: list[str] | None = None
    supported_model_artifact_aliases: list[str] | None = None
    expected_feature_order_artifacts: list[str] | None = None


class RiskScorer(ABC):
    @property
    @abstractmethod
    def model_version(self) -> str:
        raise NotImplementedError

    @property
    @abstractmethod
    def is_mock(self) -> bool:
        raise NotImplementedError

    @property
    @abstractmethod
    def scorer_mode(self) -> str:
        raise NotImplementedError

    @property
    def model_unavailable_reason(self) -> str | None:
        return None

    @property
    def expected_model_artifacts(self) -> list[str]:
        return _path_labels(MODEL_ARTIFACT_CANDIDATES)

    @property
    def supported_model_artifact_aliases(self) -> list[str]:
        return _path_labels(MODEL_ARTIFACT_CANDIDATES)

    @property
    def expected_feature_order_artifacts(self) -> list[str]:
        return _path_labels(FEATURE_ORDER_ARTIFACT_CANDIDATES)

    @property
    def loaded_artifact_name(self) -> str | None:
        return None

    @property
    def model_auc(self) -> float | None:
        return None

    @abstractmethod
    def evaluate(self, vector: CvdFeatureVector) -> RiskEvaluateResponse:
        raise NotImplementedError


class MockRiskScorer(RiskScorer):
    """Deterministic fallback used only until a validated model artifact is supplied."""

    def __init__(
        self,
        scorer_mode: str = "mock",
        unavailable_reason: str | None = None,
        validation: ArtifactValidationResult | None = None,
    ) -> None:
        self._scorer_mode = scorer_mode
        self._unavailable_reason = unavailable_reason
        self._validation = validation

    @property
    def model_version(self) -> str:
        return MOCK_MODEL_VERSION

    @property
    def is_mock(self) -> bool:
        return True

    @property
    def scorer_mode(self) -> str:
        return self._scorer_mode

    @property
    def model_unavailable_reason(self) -> str | None:
        return self._unavailable_reason

    @property
    def expected_model_artifacts(self) -> list[str]:
        if self._validation and self._validation.expected_model_artifacts:
            return self._validation.expected_model_artifacts
        return super().expected_model_artifacts

    @property
    def supported_model_artifact_aliases(self) -> list[str]:
        if self._validation and self._validation.supported_model_artifact_aliases:
            return self._validation.supported_model_artifact_aliases
        return super().supported_model_artifact_aliases

    @property
    def expected_feature_order_artifacts(self) -> list[str]:
        if self._validation and self._validation.expected_feature_order_artifacts:
            return self._validation.expected_feature_order_artifacts
        return super().expected_feature_order_artifacts

    def evaluate(self, vector: CvdFeatureVector) -> RiskEvaluateResponse:
        values = vector.model_dump()
        score = 0.08
        contributions = {field: 0.0 for field in FEATURE_FIELDS}

        score = self._add(score, contributions, "age", self._age_contribution(values.get("age")))
        score = self._add(score, contributions, "bmi", self._bmi_contribution(values.get("bmi")))
        score = self._add(score, contributions, "sbp", self._sbp_contribution(values.get("sbp")))
        score = self._add(score, contributions, "dbp", self._dbp_contribution(values.get("dbp")))
        score = self._add(
            score,
            contributions,
            "fasting_glucose",
            self._range_contribution(values.get("fasting_glucose"), warning_at=6.1, high_at=7.0, weight=0.08),
        )
        score = self._add(
            score,
            contributions,
            "total_cholesterol",
            self._range_contribution(values.get("total_cholesterol"), warning_at=5.2, high_at=6.2, weight=0.06),
        )
        score = self._add(score, contributions, "ldl", self._range_contribution(values.get("ldl"), 3.4, 4.1, 0.06))
        score = self._add(score, contributions, "hdl", self._hdl_contribution(values.get("hdl")))
        score = self._add(
            score,
            contributions,
            "triglycerides",
            self._range_contribution(values.get("triglycerides"), 1.7, 2.3, 0.05),
        )
        score = self._add(score, contributions, "exercise_days", self._exercise_contribution(values.get("exercise_days")))

        for field, weight in [
            ("smoking", 0.08),
            ("drinking", 0.03),
            ("diabetes_history", 0.08),
            ("hypertension_history", 0.08),
            ("family_history", 0.04),
        ]:
            if values.get(field) == 1:
                score = self._add(score, contributions, field, weight)

        missing = vector.missing_fields()
        score = min(max(score + min(len(missing) * 0.01, 0.08), 0.01), 0.95)
        warnings = self._quality_warnings(vector)

        return RiskEvaluateResponse(
            risk_score=round(score, 4),
            risk_level=self._risk_level(score),
            feature_contributions={key: round(value, 4) for key, value in contributions.items()},
            model_version=self.model_version,
            is_mock=self.is_mock,
            missing_fields=missing,
            quality_warnings=warnings,
            summary=self._summary(score, missing, warnings),
            model_trace=ModelRegistry(self).active_trace(),
        )

    def _add(self, score: float, contributions: dict[str, float], field: str, value: float) -> float:
        contributions[field] = value
        return score + value

    def _age_contribution(self, age: int | None) -> float:
        if age is None:
            return 0.0
        if age >= 70:
            return 0.20
        if age >= 60:
            return 0.14
        if age >= 50:
            return 0.09
        if age >= 40:
            return 0.04
        return 0.0

    def _bmi_contribution(self, bmi: float | None) -> float:
        if not self._valid_number(bmi):
            return 0.0
        if bmi >= 32:
            return 0.10
        if bmi >= 28:
            return 0.07
        if bmi >= 24:
            return 0.03
        return 0.0

    def _sbp_contribution(self, sbp: float | None) -> float:
        if not self._valid_number(sbp):
            return 0.0
        if sbp >= 160:
            return 0.18
        if sbp >= 140:
            return 0.12
        if sbp >= 130:
            return 0.07
        return 0.0

    def _dbp_contribution(self, dbp: float | None) -> float:
        if not self._valid_number(dbp):
            return 0.0
        if dbp >= 100:
            return 0.10
        if dbp >= 90:
            return 0.06
        if dbp >= 85:
            return 0.03
        return 0.0

    def _range_contribution(self, value: float | None, warning_at: float, high_at: float, weight: float) -> float:
        if not self._valid_number(value):
            return 0.0
        if value >= high_at:
            return weight
        if value >= warning_at:
            return weight * 0.5
        return 0.0

    def _hdl_contribution(self, hdl: float | None) -> float:
        if not self._valid_number(hdl):
            return 0.0
        if hdl < 1.0:
            return 0.05
        if hdl < 1.2:
            return 0.025
        return -0.02

    def _exercise_contribution(self, exercise_days: int | None) -> float:
        if exercise_days is None:
            return 0.0
        if exercise_days >= 5:
            return -0.05
        if exercise_days >= 3:
            return -0.025
        return 0.04

    def _quality_warnings(self, vector: CvdFeatureVector) -> list[str]:
        warnings = []
        for field, quality in vector.feature_quality.items():
            if quality.status in {FeatureQualityStatus.STALE, FeatureQualityStatus.LOW_CONFIDENCE}:
                warnings.append(f"{field}:{quality.status.value}")
        return warnings

    def _summary(self, score: float, missing: list[str], warnings: list[str]) -> str:
        level = self._risk_level(score)
        quality_note = " Some fields are missing or lower confidence; complete clinical inputs before making care decisions."
        if missing or warnings:
            return f"Baseline CVD risk estimate is {level}.{quality_note}"
        return f"Baseline CVD risk estimate is {level}. This is not a diagnosis."

    def _risk_level(self, score: float) -> str:
        if score < 0.25:
            return "low"
        if score < 0.50:
            return "moderate"
        if score < 0.75:
            return "high"
        return "very_high"

    def _valid_number(self, value: float | None) -> bool:
        return value is not None and isfinite(value)


class RealCatBoostRiskScorer(RiskScorer):
    """Local artifact-backed scorer.

    Artifacts are loaded only from the configured local `models/` directory. This uses Python
    pickle-compatible loading, which can execute payloads; only load artifacts produced by the
    approved training pipeline and reviewed for deployment.
    """

    def __init__(
        self,
        model: Any,
        feature_order: Sequence[str],
        model_version: str = REAL_MODEL_VERSION_FALLBACK,
        loaded_artifact_name: str | None = None,
        model_auc: float | None = None,
        expected_model_artifacts: Sequence[str] | None = None,
        supported_model_artifact_aliases: Sequence[str] | None = None,
        expected_feature_order_artifacts: Sequence[str] | None = None,
        contribution_method: str = REAL_CONTRIBUTION_SHAP,
    ) -> None:
        self._model = model
        self._feature_order = list(feature_order)
        self._model_version = model_version
        self._loaded_artifact_name = loaded_artifact_name
        self._model_auc = model_auc
        self._expected_model_artifacts = list(expected_model_artifacts or _path_labels(MODEL_ARTIFACT_CANDIDATES))
        self._supported_model_artifact_aliases = list(
            supported_model_artifact_aliases or _path_labels(MODEL_ARTIFACT_CANDIDATES)
        )
        self._expected_feature_order_artifacts = list(
            expected_feature_order_artifacts or _path_labels(FEATURE_ORDER_ARTIFACT_CANDIDATES)
        )
        self._contribution_method = contribution_method
        self._base_value: float | None = None
        self._validate_feature_order(self._feature_order)

    @property
    def model_version(self) -> str:
        return self._model_version

    @property
    def is_mock(self) -> bool:
        return False

    @property
    def scorer_mode(self) -> str:
        return "real_available"

    @property
    def expected_model_artifacts(self) -> list[str]:
        return self._expected_model_artifacts

    @property
    def supported_model_artifact_aliases(self) -> list[str]:
        return self._supported_model_artifact_aliases

    @property
    def expected_feature_order_artifacts(self) -> list[str]:
        return self._expected_feature_order_artifacts

    @property
    def loaded_artifact_name(self) -> str | None:
        return self._loaded_artifact_name

    @property
    def model_auc(self) -> float | None:
        return self._model_auc

    def evaluate(self, vector: CvdFeatureVector) -> RiskEvaluateResponse:
        probability = self._predict_probability(vector)
        contributions, base_value = self._shap_feature_contributions(vector)
        self._base_value = base_value
        return RiskEvaluateResponse(
            risk_score=round(probability, 4),
            risk_level=self._risk_level(probability),
            feature_contributions=contributions,
            model_version=self.model_version,
            is_mock=self.is_mock,
            missing_fields=vector.missing_fields(),
            quality_warnings=[],
            summary="CatBoost CVD risk estimate with SHAP attribution. This is not a diagnosis.",
            contribution_method=self._contribution_method,
            base_value=base_value,
            model_trace=ModelRegistry(self).active_trace(),
        )

    def _predict_probability(self, vector: CvdFeatureVector) -> float:
        row = [[self._model_value(getattr(vector, field)) for field in self._feature_order]]
        if hasattr(self._model, "predict_proba"):
            probabilities = self._model.predict_proba(row)
            probability = probabilities[0][1]
        elif hasattr(self._model, "predict"):
            predictions = self._model.predict(row)
            probability = predictions[0]
        else:
            raise ModelUnavailableError("model artifact must expose predict_proba or predict")
        return min(max(float(probability), 0.0), 1.0)

    def _shap_feature_contributions(self, vector: CvdFeatureVector) -> tuple[dict[str, float], float | None]:
        """Real per-feature attribution via CatBoost SHAP values.

        Returns probability-space contributions (one per FEATURE_FIELDS) that sum to
        risk_score - baseline_probability, plus the population baseline probability.
        Falls back to zero contributions if SHAP cannot be computed.
        """
        row = [[self._model_value(getattr(vector, field)) for field in self._feature_order]]
        try:
            pool = Pool(row)
            shap = self._model.get_feature_importance(type="ShapValues", data=pool)
        except Exception as exc:  # pragma: no cover - defensive
            logger.warning("SHAP attribution failed; returning zero contributions: %s", exc)
            return {field: 0.0 for field in FEATURE_FIELDS}, None

        # shap shape: (n_samples, n_features + 1); last column is the base value (log-odds).
        phi = [float(v) for v in shap[0, :-1]]
        base_logodds = float(shap[0, -1])

        contributions: dict[str, float] = {}
        running = base_logodds
        prev_prob = sigmoid(running)
        for field, value in zip(self._feature_order, phi):
            running += value
            new_prob = sigmoid(running)
            contributions[field] = round(new_prob - prev_prob, 4)
            prev_prob = new_prob
        return contributions, round(sigmoid(base_logodds), 4)

    def _model_value(self, value: float | int | None) -> float | int:
        if value is None:
            return float("nan")
        return value

    def _validate_feature_order(self, feature_order: Sequence[str]) -> None:
        if list(feature_order) != FEATURE_FIELDS:
            raise ModelUnavailableError(
                "feature order mismatch; expected Android C1 CVD 16 fields in canonical order"
            )

    def _risk_level(self, score: float) -> str:
        if score < 0.25:
            return "low"
        if score < 0.50:
            return "moderate"
        if score < 0.75:
            return "high"
        return "very_high"


CatBoostRiskScorer = RealCatBoostRiskScorer


def load_risk_scorer(artifacts: RealModelArtifacts | None = None) -> RiskScorer:
    artifact_config = artifacts or artifacts_from_environment()
    validation = validate_real_model_artifacts(artifact_config)
    if not validation.available:
        return MockRiskScorer(
            scorer_mode="real_unavailable",
            unavailable_reason=validation.reason,
            validation=validation,
        )

    try:
        if validation.model_path is None:
            raise ModelUnavailableError("validated model path is missing")
        model = _load_local_serialized_artifact(validation.model_path, artifact_config.artifact_root)
        return RealCatBoostRiskScorer(
            model=model,
            feature_order=validation.feature_order or [],
            model_version=validation.model_version or REAL_MODEL_VERSION_FALLBACK,
            loaded_artifact_name=validation.loaded_artifact_name,
            model_auc=validation.model_auc,
            expected_model_artifacts=validation.expected_model_artifacts,
            supported_model_artifact_aliases=validation.supported_model_artifact_aliases,
            expected_feature_order_artifacts=validation.expected_feature_order_artifacts,
        )
    except Exception as exc:
        reason = f"real model failed to load: {exc}"
        return MockRiskScorer(scorer_mode="real_unavailable", unavailable_reason=reason, validation=validation)


def artifacts_from_environment() -> RealModelArtifacts:
    model_path = os.environ.get("REHEALTH_CVD_MODEL_PATH")
    feature_order_path = os.environ.get("REHEALTH_CVD_FEATURE_ORDER_PATH")
    metadata_path = os.environ.get("REHEALTH_CVD_MODEL_META_PATH")
    return RealModelArtifacts(
        artifact_root=Path(os.environ.get("REHEALTH_CVD_ARTIFACT_ROOT", LOCAL_ARTIFACT_ROOT)),
        model_path=Path(model_path) if model_path else None,
        feature_order_path=Path(feature_order_path) if feature_order_path else None,
        metadata_path=Path(metadata_path) if metadata_path else None,
    )


def validate_real_model_artifacts(artifacts: RealModelArtifacts) -> ArtifactValidationResult:
    expected_model_artifacts = _path_labels(artifacts.model_candidates())
    expected_feature_order_artifacts = _path_labels(artifacts.feature_order_candidates())
    supported_model_artifact_aliases = _path_labels(MODEL_ARTIFACT_CANDIDATES)
    try:
        resolved_model_paths = [
            _resolve_local_artifact_path(path, artifacts.artifact_root) for path in artifacts.model_candidates()
        ]
        resolved_feature_order_paths = [
            _resolve_local_artifact_path(path, artifacts.artifact_root) for path in artifacts.feature_order_candidates()
        ]
        resolved_metadata_paths = [
            _resolve_local_artifact_path(path, artifacts.artifact_root) for path in artifacts.metadata_candidates()
        ]
    except ModelUnavailableError as exc:
        return ArtifactValidationResult(
            available=False,
            reason=str(exc),
            feature_order=FEATURE_FIELDS,
            expected_model_artifacts=expected_model_artifacts,
            supported_model_artifact_aliases=supported_model_artifact_aliases,
            expected_feature_order_artifacts=expected_feature_order_artifacts,
        )

    model_path = _first_existing_path(resolved_model_paths)
    feature_order_path = _first_existing_path(resolved_feature_order_paths)
    metadata_path = _first_existing_path(resolved_metadata_paths)

    if model_path is None:
        return ArtifactValidationResult(
            available=False,
            reason=f"model artifact missing: {_join_path_labels(expected_model_artifacts)}",
            feature_order=FEATURE_FIELDS,
            expected_model_artifacts=expected_model_artifacts,
            supported_model_artifact_aliases=supported_model_artifact_aliases,
            expected_feature_order_artifacts=expected_feature_order_artifacts,
        )
    if feature_order_path is None:
        return ArtifactValidationResult(
            available=False,
            reason=f"feature order artifact missing: {_join_path_labels(expected_feature_order_artifacts)}",
            feature_order=FEATURE_FIELDS,
            model_path=model_path,
            loaded_artifact_name=model_path.name,
            expected_model_artifacts=expected_model_artifacts,
            supported_model_artifact_aliases=supported_model_artifact_aliases,
            expected_feature_order_artifacts=expected_feature_order_artifacts,
        )

    try:
        feature_order = _load_feature_order(feature_order_path)
        if feature_order != FEATURE_FIELDS:
            return ArtifactValidationResult(
                available=False,
                reason="feature order mismatch; real scorer requires Android C1 CVD 16 order",
                feature_order=feature_order,
                model_path=model_path,
                feature_order_path=feature_order_path,
                loaded_artifact_name=model_path.name,
                expected_model_artifacts=expected_model_artifacts,
                supported_model_artifact_aliases=supported_model_artifact_aliases,
                expected_feature_order_artifacts=expected_feature_order_artifacts,
            )
        model_metadata = _load_model_metadata(metadata_path)
        return ArtifactValidationResult(
            available=True,
            reason=None,
            feature_order=feature_order,
            model_version=model_metadata["model_version"],
            model_path=model_path,
            feature_order_path=feature_order_path,
            metadata_path=metadata_path,
            loaded_artifact_name=model_path.name,
            model_auc=model_metadata["model_auc"],
            expected_model_artifacts=expected_model_artifacts,
            supported_model_artifact_aliases=supported_model_artifact_aliases,
            expected_feature_order_artifacts=expected_feature_order_artifacts,
        )
    except Exception as exc:
        return ArtifactValidationResult(
            available=False,
            reason=f"artifact validation failed: {exc}",
            model_path=model_path,
            feature_order_path=feature_order_path,
            loaded_artifact_name=model_path.name,
            expected_model_artifacts=expected_model_artifacts,
            supported_model_artifact_aliases=supported_model_artifact_aliases,
            expected_feature_order_artifacts=expected_feature_order_artifacts,
        )


def _load_feature_order(path: Path) -> list[str]:
    if path.suffix.lower() == ".json":
        data = json.loads(path.read_text(encoding="utf-8"))
        if isinstance(data, list):
            return [str(item) for item in data]
        if isinstance(data, dict) and isinstance(data.get("feature_cols"), list):
            return [str(item) for item in data["feature_cols"]]
        raise ValueError("JSON feature order must be a list or contain feature_cols")

    loaded = _load_serialized_file(path)
    if not isinstance(loaded, list):
        raise ValueError("feature order artifact must contain a list")
    return [str(item) for item in loaded]


def _load_model_metadata(path: Path | None) -> dict[str, str | float | None]:
    if path is None or not path.exists():
        return {"model_version": REAL_MODEL_VERSION_FALLBACK, "model_auc": None}
    data = json.loads(path.read_text(encoding="utf-8"))
    version = data.get("model_version")
    if not isinstance(version, str) or not version:
        version = REAL_MODEL_VERSION_FALLBACK
    auc = data.get("model_auc")
    if auc is None:
        auc = data.get("auc")
    model_auc = float(auc) if isinstance(auc, (int, float)) and isfinite(float(auc)) else None
    return {"model_version": version, "model_auc": model_auc}


def _load_local_serialized_artifact(path: Path, artifact_root: Path) -> Any:
    resolved_path = _resolve_local_artifact_path(path, artifact_root)
    return _load_serialized_file(resolved_path)


def _load_serialized_file(path: Path) -> Any:
    return joblib.load(path)


def _resolve_local_artifact_path(path: Path, artifact_root: Path) -> Path:
    root = artifact_root.resolve()
    candidate = path if path.is_absolute() else Path.cwd() / path
    resolved = candidate.resolve()
    if root != resolved and root not in resolved.parents:
        raise ModelUnavailableError(f"artifact path must stay under local artifact root {artifact_root}: {path}")
    return resolved


def _first_existing_path(paths: Sequence[Path]) -> Path | None:
    return next((path for path in paths if path.exists()), None)


def _path_labels(paths: Sequence[Path]) -> list[str]:
    return [path.as_posix() for path in paths]


def _join_path_labels(paths: Sequence[str]) -> str:
    return "; ".join(paths)
