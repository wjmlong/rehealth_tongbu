from __future__ import annotations

import json
from dataclasses import dataclass
from typing import Final
from urllib.parse import urlparse

from rehealth_stack_topology import EXIT_USAGE, GateError, option


VALID_CONFIG_CASES: Final = ("production", "staging", "development", "demo")
INVALID_CONFIG_CASES: Final = (
    "production_demo",
    "disabled_software_db",
    "http_external",
    "embedded_secret",
)


@dataclass(frozen=True, slots=True)
class PlatformRuntimeConfig:
    runtime_mode: str
    attribution_mode: str = "pias"
    demo_enabled: bool = False
    software_db_enabled: bool = True
    device_service_enabled: bool = True
    timescale_enabled: bool = True
    real_model_required: bool = True
    model_service_url: str = "https://model.internal.example"
    device_service_url: str = "https://device.internal.example"
    attribution_service_url: str = "https://pias.internal.example"
    embedded_provider_secret: str = ""


def validate_platform_config(config: PlatformRuntimeConfig) -> None:
    protected = config.runtime_mode in {"production", "staging"}
    if protected and not config.software_db_enabled:
        raise GateError("SOFTWARE_DB_REQUIRED")
    if protected and (not config.device_service_enabled or not config.timescale_enabled):
        raise GateError("REQUIRED_SERVICE_DISABLED")
    if protected and not config.real_model_required:
        raise GateError("REAL_MODEL_REQUIRED")
    if protected and config.attribution_mode != "pias":
        raise GateError("ATTRIBUTION_MODE_UNSAFE")
    if protected:
        for value in (
            config.model_service_url,
            config.device_service_url,
            config.attribution_service_url,
        ):
            parsed = urlparse(value)
            if parsed.scheme != "https" or not parsed.hostname or parsed.username or parsed.password:
                raise GateError("SECURE_URL_REQUIRED")
        if config.embedded_provider_secret:
            raise GateError("EMBEDDED_SECRET_FORBIDDEN")
    if config.runtime_mode == "demo" and not config.demo_enabled:
        raise GateError("DEMO_FLAG_REQUIRED")


def run_config_matrix(arguments: list[str]) -> int:
    valid_value = option(arguments, "--valid", required=False)
    invalid_value = option(arguments, "--invalid", required=False)
    if (valid_value is None) == (invalid_value is None):
        raise GateError("config-matrix requires exactly one of --valid or --invalid", EXIT_USAGE)
    if valid_value is not None:
        cases = [case.strip() for case in valid_value.split(",") if case.strip()]
        unknown = sorted(set(cases) - set(VALID_CONFIG_CASES))
        if unknown:
            raise GateError(f"unknown valid config cases: {unknown}", EXIT_USAGE)
        results = []
        for case in cases:
            config = PlatformRuntimeConfig(
                runtime_mode=case,
                attribution_mode="demo_mock" if case == "demo" else "pias",
                demo_enabled=case == "demo",
            )
            validate_platform_config(config)
            results.append({"mode": case, "accepted": True})
        payload = {"passed": True, "valid": results, "invalid": []}
    else:
        assert invalid_value is not None
        cases = [case.strip() for case in invalid_value.split(",") if case.strip()]
        unknown = sorted(set(cases) - set(INVALID_CONFIG_CASES))
        if unknown:
            raise GateError(f"unknown invalid config cases: {unknown}", EXIT_USAGE)
        invalid_configs = {
            "production_demo": PlatformRuntimeConfig(
                runtime_mode="production",
                attribution_mode="demo_mock",
                demo_enabled=True,
            ),
            "disabled_software_db": PlatformRuntimeConfig(
                runtime_mode="production",
                software_db_enabled=False,
            ),
            "http_external": PlatformRuntimeConfig(
                runtime_mode="production",
                model_service_url="http://models.example.com",
            ),
            "embedded_secret": PlatformRuntimeConfig(
                runtime_mode="production",
                embedded_provider_secret="synthetic-invalid-secret",
            ),
        }
        results = []
        for case in cases:
            try:
                validate_platform_config(invalid_configs[case])
            except GateError as error:
                results.append({"case": case, "rejected": True, "rejection_code": error.message})
            else:
                raise GateError(f"unsafe configuration was accepted: {case}")
        payload = {"passed": True, "valid": [], "invalid": results}
    print(json.dumps(payload, sort_keys=True))
    return 0
