from __future__ import annotations

import pytest

from app.runtime_config import (
    AttributionMode,
    RuntimeConfig,
    RuntimeConfigurationError,
    RuntimeMode,
    load_runtime_config,
    validate_model_runtime,
)


def test_loads_explicit_development_configuration() -> None:
    config = load_runtime_config(
        {
            "REHEALTH_RUNTIME_MODE": "development",
            "REHEALTH_ATTRIBUTION_MODE": "pias",
        }
    )

    assert config.runtime_mode is RuntimeMode.DEVELOPMENT
    assert config.attribution_mode is AttributionMode.PIAS
    assert config.mock_attribution_enabled is False


@pytest.mark.parametrize("runtime_mode", ["production", "staging"])
def test_loads_protected_configuration_with_secure_service_boundary(runtime_mode: str) -> None:
    config = load_runtime_config(
        {
            "REHEALTH_RUNTIME_MODE": runtime_mode,
            "REHEALTH_ATTRIBUTION_MODE": "pias",
            "REHEALTH_MODEL_SERVICE_BASE_URL": "https://model.internal.example",
            "REHEALTH_PROVIDER_CREDENTIAL_FILE": "/run/secrets/provider_credential",
        }
    )

    assert config.service_base_url == "https://model.internal.example"
    assert config.provider_credential_file == "/run/secrets/provider_credential"


def test_loads_explicit_demo_configuration_with_visible_provenance() -> None:
    config = load_runtime_config(
        {
            "REHEALTH_RUNTIME_MODE": "demo",
            "REHEALTH_DEMO_ENABLED": "true",
            "REHEALTH_ATTRIBUTION_MODE": "demo_mock",
            "REHEALTH_ATTRIBUTION_PROVENANCE": "demo_mock",
        }
    )

    assert config.runtime_mode is RuntimeMode.DEMO
    assert config.mock_attribution_enabled is True
    assert config.provenance == "demo_mock"


@pytest.mark.parametrize("runtime_mode", [RuntimeMode.PRODUCTION, RuntimeMode.STAGING])
def test_rejects_mock_model_in_protected_runtime(runtime_mode: RuntimeMode) -> None:
    config = RuntimeConfig(runtime_mode=runtime_mode)

    with pytest.raises(RuntimeConfigurationError, match="REAL_MODEL_REQUIRED"):
        validate_model_runtime(config, scorer_mode="mock")


def test_rejects_demo_mock_in_production() -> None:
    with pytest.raises(RuntimeConfigurationError, match="ATTRIBUTION_MODE_UNSAFE"):
        load_runtime_config(
            {
                "REHEALTH_RUNTIME_MODE": "production",
                "REHEALTH_DEMO_ENABLED": "true",
                "REHEALTH_ATTRIBUTION_MODE": "demo_mock",
                "REHEALTH_ATTRIBUTION_PROVENANCE": "demo_mock",
            }
        )


def test_rejects_demo_mock_without_explicit_flag() -> None:
    with pytest.raises(RuntimeConfigurationError, match="DEMO_FLAG_REQUIRED"):
        load_runtime_config(
            {
                "REHEALTH_RUNTIME_MODE": "development",
                "REHEALTH_ATTRIBUTION_MODE": "demo_mock",
                "REHEALTH_ATTRIBUTION_PROVENANCE": "demo_mock",
            }
        )


def test_rejects_demo_mock_without_visible_provenance() -> None:
    with pytest.raises(RuntimeConfigurationError, match="DEMO_PROVENANCE_REQUIRED"):
        load_runtime_config(
            {
                "REHEALTH_RUNTIME_MODE": "demo",
                "REHEALTH_DEMO_ENABLED": "true",
                "REHEALTH_ATTRIBUTION_MODE": "demo_mock",
            }
        )


def test_rejects_invalid_boolean_value() -> None:
    with pytest.raises(RuntimeConfigurationError, match="INVALID_BOOLEAN"):
        load_runtime_config(
            {
                "REHEALTH_RUNTIME_MODE": "demo",
                "REHEALTH_DEMO_ENABLED": "sometimes",
            }
        )


def test_rejects_insecure_model_service_url_in_production() -> None:
    with pytest.raises(RuntimeConfigurationError, match="SECURE_URL_REQUIRED"):
        load_runtime_config(
            {
                "REHEALTH_RUNTIME_MODE": "production",
                "REHEALTH_MODEL_SERVICE_BASE_URL": "http://models.example.com",
                "REHEALTH_PROVIDER_CREDENTIAL_FILE": "/run/secrets/provider_credential",
            }
        )


def test_rejects_embedded_provider_secret_in_production() -> None:
    with pytest.raises(RuntimeConfigurationError, match="EMBEDDED_SECRET_FORBIDDEN"):
        load_runtime_config(
            {
                "REHEALTH_RUNTIME_MODE": "production",
                "REHEALTH_MODEL_SERVICE_BASE_URL": "https://model.internal.example",
                "REHEALTH_PROVIDER_SECRET": "do-not-ship",
            }
        )


def test_rejects_embedded_agent_token_in_production() -> None:
    with pytest.raises(RuntimeConfigurationError, match="EMBEDDED_AGENT_TOKEN_FORBIDDEN"):
        load_runtime_config(
            {
                "REHEALTH_RUNTIME_MODE": "production",
                "REHEALTH_MODEL_SERVICE_BASE_URL": "https://model.internal.example",
                "REHEALTH_PROVIDER_CREDENTIAL_FILE": "/run/secrets/provider_credential",
                "REHEALTH_AGENT_PROVIDER_ENABLED": "true",
                "REHEALTH_AGENT_INTERNAL_TOKEN": "do-not-ship",
            }
        )
