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


def test_loads_development_health_agent_from_local_yaml(tmp_path) -> None:
    config_path = tmp_path / "ai-chat.local.yml"
    config_path.write_text(
        """
runtime:
  mode: development
health-agent:
  internal-token: local-agent-token
  provider:
    base-url: https://api.deepseek.com
    model: deepseek-v4-flash
    api-key: local-deepseek-key
    timeout-seconds: 20
""".strip(),
        encoding="utf-8",
    )

    config = load_runtime_config({"REHEALTH_LOCAL_CONFIG_FILE": str(config_path)})

    assert config.runtime_mode is RuntimeMode.DEVELOPMENT
    assert config.agent_provider_enabled is True
    assert config.agent_provider_base_url == "https://api.deepseek.com"
    assert config.agent_provider_model == "deepseek-v4-flash"
    assert config.embedded_provider_secret == "local-deepseek-key"
    assert config.agent_provider_timeout_seconds == 20
    assert config.agent_internal_token == "local-agent-token"


def test_environment_overrides_local_yaml(tmp_path) -> None:
    config_path = tmp_path / "ai-chat.local.yml"
    config_path.write_text(
        """
health-agent:
  provider:
    model: deepseek-v4-flash
    api-key: local-deepseek-key
""".strip(),
        encoding="utf-8",
    )

    config = load_runtime_config(
        {
            "REHEALTH_LOCAL_CONFIG_FILE": str(config_path),
            "REHEALTH_AGENT_PROVIDER_MODEL": "environment-model",
            "REHEALTH_PROVIDER_SECRET": "environment-key",
        }
    )

    assert config.agent_provider_model == "environment-model"
    assert config.embedded_provider_secret == "environment-key"


def test_rejects_embedded_local_yaml_key_in_protected_runtime(tmp_path) -> None:
    config_path = tmp_path / "ai-chat.local.yml"
    config_path.write_text(
        """
runtime:
  mode: production
health-agent:
  provider:
    api-key: must-not-be-accepted
""".strip(),
        encoding="utf-8",
    )

    with pytest.raises(RuntimeConfigurationError, match="EMBEDDED_SECRET_FORBIDDEN"):
        load_runtime_config({"REHEALTH_LOCAL_CONFIG_FILE": str(config_path)})


def test_rejects_malformed_local_yaml_without_exposing_content(tmp_path) -> None:
    config_path = tmp_path / "ai-chat.local.yml"
    config_path.write_text("health-agent: [secret-value", encoding="utf-8")

    with pytest.raises(RuntimeConfigurationError, match="LOCAL_CONFIG_INVALID") as failure:
        load_runtime_config({"REHEALTH_LOCAL_CONFIG_FILE": str(config_path)})

    assert "secret-value" not in str(failure.value)


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
