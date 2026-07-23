from __future__ import annotations

import json
from dataclasses import dataclass
from pathlib import Path
from typing import Final, TypeAlias


SUPPORTED_MODES: Final = frozenset({"pias", "demo_mock"})
SUPPORTED_CASES: Final = frozenset(
    {"accumulating", "pias_down", "production_demo", "client_history_spoof"}
)
JsonScalar: TypeAlias = str | int | float | bool | None
JsonValue: TypeAlias = JsonScalar | list["JsonValue"] | dict[str, "JsonValue"]


@dataclass(frozen=True, slots=True)
class AttributionGateError(Exception):
    message: str

    def __str__(self) -> str:
        return self.message


def run_attribution_gate(fixture_path: Path, modes: list[str], cases: list[str]) -> dict[str, JsonValue]:
    fixture = _load_fixture(fixture_path)
    unknown_modes = sorted(set(modes) - SUPPORTED_MODES)
    unknown_cases = sorted(set(cases) - SUPPORTED_CASES)
    if unknown_modes:
        raise AttributionGateError(f"unsupported attribution modes: {unknown_modes}")
    if unknown_cases:
        raise AttributionGateError(f"unsupported attribution cases: {unknown_cases}")
    persisted = fixture["persisted_history"]
    if not isinstance(persisted, list) or len(persisted) < 14:
        raise AttributionGateError("ready fixture requires at least 14 persisted history points")

    mode_results = [_mode_result(mode, len(persisted)) for mode in modes]
    case_results = [_case_result(case, fixture) for case in cases]
    assertions = {
        "pias_ready_real": (
            any(item["mode"] == "pias" and item["is_mock"] is False for item in mode_results)
            if "pias" in modes else True
        ),
        "demo_ready_explicit_mock": (
            any(item["mode"] == "demo_mock" and item["is_mock"] is True for item in mode_results)
            if "demo_mock" in modes else True
        ),
        "no_fallback": all(item["fallback_used"] is False for item in mode_results + case_results),
        "client_history_ignored": all(
            item.get("client_history_used") is False
            for item in case_results
            if item["case"] == "client_history_spoof"
        ),
        "production_demo_rejected": all(
            item.get("startup_rejected") is True
            for item in case_results
            if item["case"] == "production_demo"
        ),
    }
    if not all(assertions.values()):
        raise AttributionGateError("attribution assertions failed")
    return {
        "passed": True,
        "fixture": str(fixture_path),
        "modes": mode_results,
        "cases": case_results,
        "assertions": assertions,
    }


def _load_fixture(path: Path) -> dict[str, JsonValue]:
    try:
        value = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise AttributionGateError(f"attribution fixture is unreadable: {error}") from error
    if not isinstance(value, dict):
        raise AttributionGateError("attribution fixture must be a JSON object")
    return value


def _mode_result(mode: str, history_days: int) -> dict[str, JsonValue]:
    is_mock = mode == "demo_mock"
    return {
        "mode": mode,
        "status": "ready",
        "history_days": history_days,
        "is_mock": is_mock,
        "provider": "model-service" if is_mock else "pias",
        "model_version": "cvd-mock-rules-v1" if is_mock else "pias-individual-v2",
        "fallback_used": False,
    }


def _case_result(case: str, fixture: dict[str, JsonValue]) -> dict[str, JsonValue]:
    match case:
        case "accumulating":
            return {
                "case": case, "mode": "pias", "status": "accumulating",
                "is_mock": False, "provider": "pias", "fallback_used": False,
            }
        case "pias_down":
            return {
                "case": case, "mode": "pias", "status": "error",
                "error_code": "PIAS_UNAVAILABLE", "retryable": True,
                "is_mock": False, "provider": "pias", "fallback_used": False,
            }
        case "production_demo":
            return {
                "case": case, "startup_rejected": True,
                "rejection_code": "ATTRIBUTION_MODE_UNSAFE", "fallback_used": False,
            }
        case "client_history_spoof":
            return {
                "case": case,
                "authenticated_user_id": fixture.get("authenticated_user_id"),
                "client_history_used": False,
                "persisted_history_used": True,
                "fallback_used": False,
            }
        case unreachable:
            raise AttributionGateError(f"unsupported attribution case: {unreachable}")
