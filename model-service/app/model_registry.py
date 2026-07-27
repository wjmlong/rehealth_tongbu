from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path
from typing import Protocol

from app.runtime_config import RuntimeConfig, RuntimeMode, artifact_verification_available
from app.schemas import ModelTrace


FEATURE_SCHEMA_VERSION = "cvd-16-v1"
MODEL_REGISTRY_VERSION = "model-registry-v1"


class RegistryScorer(Protocol):
    @property
    def model_version(self) -> str: ...

    @property
    def loaded_artifact_name(self) -> str | None: ...

    @property
    def scorer_mode(self) -> str: ...

    @property
    def is_mock(self) -> bool: ...


@dataclass(frozen=True, slots=True)
class ModelRegistryEntry:
    model_version: str
    feature_schema_version: str
    artifact_name: str | None
    scorer_mode: str
    is_mock: bool
    readiness_code: str


class ModelRegistry:
    """Small governance layer around the active scorer.

    It records model identity and fallback state without changing CVD 16 feature semantics.
    """

    registry_version = MODEL_REGISTRY_VERSION

    def __init__(
        self,
        scorer: RegistryScorer,
        feature_schema_version: str = FEATURE_SCHEMA_VERSION,
    ) -> None:
        self._scorer = scorer
        self._feature_schema_version = feature_schema_version

    @property
    def feature_schema_version(self) -> str:
        return self._feature_schema_version

    def active_entry(self) -> ModelRegistryEntry:
        _, readiness_code = self.readiness()
        return ModelRegistryEntry(
            model_version=self._scorer.model_version,
            feature_schema_version=self._feature_schema_version,
            artifact_name=self._safe_artifact_name(),
            scorer_mode=self._scorer.scorer_mode,
            is_mock=self._scorer.is_mock,
            readiness_code=readiness_code,
        )

    def active_trace(self, request_id: str | None = None) -> ModelTrace:
        entry = self.active_entry()
        return ModelTrace(
            feature_schema_version=entry.feature_schema_version,
            model_version=entry.model_version,
            artifact_name=entry.artifact_name,
            scorer_mode=entry.scorer_mode,
            fallback_reason=self._fallback_reason(entry.readiness_code),
            request_id=request_id,
        )

    def readiness(self, config: RuntimeConfig | None = None) -> tuple[bool, str]:
        if self._scorer.scorer_mode == "real_available" and not self._scorer.is_mock:
            if config is not None and not artifact_verification_available(config):
                return False, "artifact_not_verified"
            return True, "ready"
        if self._scorer.scorer_mode == "mock":
            if config is not None and config.runtime_mode in {
                RuntimeMode.PRODUCTION,
                RuntimeMode.STAGING,
            }:
                return False, "mock_forbidden"
            return True, "demo_mock_active"
        return False, "model_unavailable"

    def _safe_artifact_name(self) -> str | None:
        artifact_name = self._scorer.loaded_artifact_name
        return None if artifact_name is None else Path(artifact_name).name

    def _fallback_reason(self, readiness_code: str) -> str | None:
        if readiness_code == "ready":
            return None
        if readiness_code == "demo_mock_active":
            return "mock scorer active"
        return readiness_code
