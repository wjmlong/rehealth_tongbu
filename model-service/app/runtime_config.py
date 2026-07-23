from __future__ import annotations

import os
from collections.abc import Mapping
from dataclasses import dataclass
from enum import StrEnum
from typing import assert_never
from urllib.parse import urlparse
from pathlib import Path
import re

from pydantic import BaseModel, ConfigDict, Field


class RuntimeMode(StrEnum):
    PRODUCTION = "production"
    STAGING = "staging"
    DEVELOPMENT = "development"
    DEMO = "demo"


class AttributionMode(StrEnum):
    PIAS = "pias"
    DEMO_MOCK = "demo_mock"


@dataclass(frozen=True, slots=True)
class RuntimeConfigurationError(RuntimeError):
    code: str
    detail: str

    def __str__(self) -> str:
        return f"{self.code}: {self.detail}"


class RuntimeConfig(BaseModel):
    model_config = ConfigDict(frozen=True)

    runtime_mode: RuntimeMode = RuntimeMode.DEVELOPMENT
    attribution_mode: AttributionMode = AttributionMode.PIAS
    demo_enabled: bool = False
    mock_attribution_enabled: bool = False
    provenance: str = "pias"
    service_base_url: str = ""
    provider_credential_file: str = ""
    embedded_provider_secret: str = ""
    model_verification_file: str = ""
    model_evaluation_timeout_seconds: float = Field(default=5.0, gt=0)
    model_circuit_failure_threshold: int = Field(default=3, ge=1)
    model_circuit_reset_seconds: float = Field(default=30.0, gt=0)


class RuntimeStatus(BaseModel):
    model_config = ConfigDict(frozen=True)

    runtime_mode: RuntimeMode
    attribution_mode: AttributionMode
    demo_enabled: bool
    mock_attribution_enabled: bool
    provenance: str


def load_runtime_config(environ: Mapping[str, str] | None = None) -> RuntimeConfig:
    source = os.environ if environ is None else environ
    runtime_mode = _parse_runtime_mode(source.get("REHEALTH_RUNTIME_MODE", "development"))
    attribution_mode = _parse_attribution_mode(source.get("REHEALTH_ATTRIBUTION_MODE", "pias"))
    demo_enabled = _parse_bool(source.get("REHEALTH_DEMO_ENABLED", "false"), "REHEALTH_DEMO_ENABLED")
    provenance = source.get("REHEALTH_ATTRIBUTION_PROVENANCE", "pias").strip()
    service_base_url = source.get("REHEALTH_MODEL_SERVICE_BASE_URL", "").strip()
    provider_credential_file = source.get("REHEALTH_PROVIDER_CREDENTIAL_FILE", "").strip()
    embedded_provider_secret = source.get("REHEALTH_PROVIDER_SECRET", "").strip()
    model_verification_file = source.get("REHEALTH_MODEL_VERIFICATION_FILE", "").strip()
    config = RuntimeConfig(
        runtime_mode=runtime_mode,
        attribution_mode=attribution_mode,
        demo_enabled=demo_enabled,
        mock_attribution_enabled=attribution_mode is AttributionMode.DEMO_MOCK,
        provenance=provenance,
        service_base_url=service_base_url,
        provider_credential_file=provider_credential_file,
        embedded_provider_secret=embedded_provider_secret,
        model_verification_file=model_verification_file,
        model_evaluation_timeout_seconds=float(
            source.get("REHEALTH_MODEL_EVALUATION_TIMEOUT_SECONDS", "5")
        ),
        model_circuit_failure_threshold=int(
            source.get("REHEALTH_MODEL_CIRCUIT_FAILURE_THRESHOLD", "3")
        ),
        model_circuit_reset_seconds=float(
            source.get("REHEALTH_MODEL_CIRCUIT_RESET_SECONDS", "30")
        ),
    )
    validate_runtime_config(config)
    return config


def validate_runtime_config(config: RuntimeConfig) -> None:
    _validate_attribution_mode(config)
    _validate_protected_boundary(config)


def validate_model_runtime(config: RuntimeConfig, scorer_mode: str) -> None:
    match config.runtime_mode:
        case RuntimeMode.PRODUCTION | RuntimeMode.STAGING:
            if scorer_mode != "real_available":
                raise RuntimeConfigurationError(
                    code="REAL_MODEL_REQUIRED",
                    detail=f"{config.runtime_mode.value} requires an available real model",
                )
        case RuntimeMode.DEVELOPMENT | RuntimeMode.DEMO:
            return
        case unreachable:
            assert_never(unreachable)


def runtime_status(config: RuntimeConfig) -> RuntimeStatus:
    return RuntimeStatus(
        runtime_mode=config.runtime_mode,
        attribution_mode=config.attribution_mode,
        demo_enabled=config.demo_enabled,
        mock_attribution_enabled=config.mock_attribution_enabled,
        provenance=config.provenance,
    )


def artifact_verification_available(config: RuntimeConfig) -> bool:
    if config.runtime_mode not in {RuntimeMode.PRODUCTION, RuntimeMode.STAGING}:
        return True
    if not config.model_verification_file:
        return False
    verification_path = Path(config.model_verification_file)
    try:
        digest = verification_path.read_text(encoding="utf-8").strip()
    except OSError:
        return False
    return re.fullmatch(r"[a-f0-9]{64}", digest) is not None


def _validate_attribution_mode(config: RuntimeConfig) -> None:
    match config.attribution_mode:
        case AttributionMode.PIAS:
            return
        case AttributionMode.DEMO_MOCK:
            if config.runtime_mode in {RuntimeMode.PRODUCTION, RuntimeMode.STAGING}:
                raise RuntimeConfigurationError(
                    code="ATTRIBUTION_MODE_UNSAFE",
                    detail="demo_mock attribution is forbidden in production and staging",
                )
            if not config.demo_enabled:
                raise RuntimeConfigurationError(
                    code="DEMO_FLAG_REQUIRED",
                    detail="demo_mock attribution requires REHEALTH_DEMO_ENABLED=true",
                )
            if config.provenance != "demo_mock":
                raise RuntimeConfigurationError(
                    code="DEMO_PROVENANCE_REQUIRED",
                    detail="demo_mock attribution must report demo_mock provenance",
                )
        case unreachable:
            assert_never(unreachable)


def _validate_protected_boundary(config: RuntimeConfig) -> None:
    if config.runtime_mode not in {RuntimeMode.PRODUCTION, RuntimeMode.STAGING}:
        return
    if config.embedded_provider_secret:
        raise RuntimeConfigurationError(
            code="EMBEDDED_SECRET_FORBIDDEN",
            detail="provider credentials must come from an external secret file",
        )
    parsed = urlparse(config.service_base_url)
    if (
        parsed.scheme != "https"
        or parsed.hostname is None
        or parsed.username is not None
        or parsed.password is not None
    ):
        raise RuntimeConfigurationError(
            code="SECURE_URL_REQUIRED",
            detail="protected runtime requires an HTTPS model-service URL without embedded credentials",
        )
    if not config.provider_credential_file:
        raise RuntimeConfigurationError(
            code="PROVIDER_CREDENTIAL_REQUIRED",
            detail="protected runtime requires REHEALTH_PROVIDER_CREDENTIAL_FILE",
        )


def _parse_runtime_mode(value: str) -> RuntimeMode:
    try:
        return RuntimeMode(value.strip().lower())
    except ValueError as error:
        raise RuntimeConfigurationError(
            code="INVALID_RUNTIME_MODE",
            detail="runtime mode must be production, staging, development, or demo",
        ) from error


def _parse_attribution_mode(value: str) -> AttributionMode:
    try:
        return AttributionMode(value.strip().lower())
    except ValueError as error:
        raise RuntimeConfigurationError(
            code="INVALID_ATTRIBUTION_MODE",
            detail="attribution mode must be pias or demo_mock",
        ) from error


def _parse_bool(value: str, name: str) -> bool:
    match value.strip().lower():
        case "true" | "1" | "yes" | "on":
            return True
        case "false" | "0" | "no" | "off" | "":
            return False
        case invalid:
            raise RuntimeConfigurationError(
                code="INVALID_BOOLEAN",
                detail=f"{name} has unsupported boolean value {invalid!r}",
            )
