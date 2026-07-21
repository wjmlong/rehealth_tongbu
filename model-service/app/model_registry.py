from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from app.schemas import ModelTrace


FEATURE_SCHEMA_VERSION = "cvd-16-v1"
MODEL_REGISTRY_VERSION = "model-registry-v1"


@dataclass(frozen=True)
class ModelRegistryEntry:
    model_version: str
    feature_schema_version: str
    artifact_name: str | None
    scorer_mode: str
    is_mock: bool
    fallback_reason: str | None


class ModelRegistry:
    """Small governance layer around the active scorer.

    It records model identity and fallback state without changing CVD 16 feature semantics.
    """

    registry_version = MODEL_REGISTRY_VERSION

    def __init__(self, scorer: Any, feature_schema_version: str = FEATURE_SCHEMA_VERSION) -> None:
        self._scorer = scorer
        self._feature_schema_version = feature_schema_version

    @property
    def feature_schema_version(self) -> str:
        return self._feature_schema_version

    def active_entry(self) -> ModelRegistryEntry:
        return ModelRegistryEntry(
            model_version=self._scorer.model_version,
            feature_schema_version=self._feature_schema_version,
            artifact_name=self._scorer.loaded_artifact_name,
            scorer_mode=self._scorer.scorer_mode,
            is_mock=self._scorer.is_mock,
            fallback_reason=self._fallback_reason(),
        )

    def active_trace(self, request_id: str | None = None) -> ModelTrace:
        entry = self.active_entry()
        return ModelTrace(
            feature_schema_version=entry.feature_schema_version,
            model_version=entry.model_version,
            artifact_name=entry.artifact_name,
            scorer_mode=entry.scorer_mode,
            fallback_reason=entry.fallback_reason,
            request_id=request_id,
        )

    def _fallback_reason(self) -> str | None:
        reason = self._scorer.model_unavailable_reason
        if reason:
            return reason
        if self._scorer.is_mock:
            return "mock scorer active"
        return None
