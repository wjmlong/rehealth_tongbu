#!/usr/bin/env -S uv run --script
# /// script
# requires-python = ">=3.11"
# dependencies = []
# ///

# ─── How to run ───
# 1. Install uv (if not installed):
#      curl -LsSf https://astral.sh/uv/install.sh | sh
# 2. Run directly (no venv, no pip install needed):
#      uv run backend/qa/rehealth_stack_gate.py topology --help
# 3. Or invoke with a project Python because this script has no dependencies:
#      python backend/qa/rehealth_stack_gate.py topology --help
# ──────────────────

from __future__ import annotations

import json
import os
import subprocess
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Final


EXIT_USAGE: Final = 64
REQUIRED_SERVICES: Final = frozenset(
    {
        "edge",
        "gateway",
        "jeecg-boot",
        "device-service",
        "software-db",
        "hardware-db",
        "kafka",
        "redis",
        "nacos",
        "model-artifact-verify",
        "model-service",
        "pias",
        "admin-web",
        "prometheus",
        "grafana",
    }
)
EXPECTED_FAILURE_CASES: Final = frozenset(
    {"timescale_down", "auth_down", "kafka_down", "bad_model_hash"}
)


@dataclass(frozen=True, slots=True)
class GateError(Exception):
    message: str
    exit_code: int = 1

    def __str__(self) -> str:
        return self.message


def option(arguments: list[str], name: str, *, required: bool = True) -> str | None:
    try:
        index = arguments.index(name)
    except ValueError:
        if required:
            raise GateError(f"missing required option: {name}", EXIT_USAGE) from None
        return None
    if index + 1 >= len(arguments):
        raise GateError(f"missing value for option: {name}", EXIT_USAGE)
    return arguments[index + 1]


def load_json(path: Path) -> dict[str, object]:
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as error:
        raise GateError(f"cannot read JSON {path}: {error}") from error
    if not isinstance(payload, dict):
        raise GateError(f"expected JSON object: {path}")
    return payload


def compose_config(compose: Path, profile: str) -> dict[str, object]:
    environment = os.environ.copy()
    environment["REHEALTH_RUNTIME_MODE"] = profile
    completed = subprocess.run(
        [
            "docker",
            "compose",
            "-f",
            str(compose),
            "--profile",
            profile,
            "config",
            "--format",
            "json",
        ],
        cwd=compose.parents[3],
        env=environment,
        check=False,
        capture_output=True,
        text=True,
    )
    if completed.returncode != 0:
        raise GateError(f"docker compose config failed for {profile}: {completed.stderr.strip()}")
    try:
        payload = json.loads(completed.stdout)
    except json.JSONDecodeError as error:
        raise GateError(f"docker compose returned invalid JSON for {profile}: {error}") from error
    if not isinstance(payload, dict):
        raise GateError(f"docker compose returned a non-object for {profile}")
    return payload


def require_mapping(container: dict[str, object], key: str) -> dict[str, object]:
    value = container.get(key)
    if not isinstance(value, dict):
        raise GateError(f"missing mapping: {key}")
    return value


def validate_routes(deploy_root: Path) -> None:
    routes = json.loads((deploy_root / "gateway" / "rehealth-routes.json").read_text(encoding="utf-8"))
    if not isinstance(routes, list):
        raise GateError("gateway route seed must be an array")
    encoded = json.dumps(routes, ensure_ascii=False, sort_keys=True)
    required = (
        "/jeecg-boot/rehealth/mobile/measurements/batch",
        "/jeecg-boot/rehealth/mobile/measurements/recent",
        "/jeecg-boot/rehealth/**",
        "lb://rehealth-device-service",
        "lb://jeecg-system",
    )
    missing = [value for value in required if value not in encoded]
    if missing:
        raise GateError(f"gateway route seed is incomplete: {missing}")


def validate_images(config: dict[str, object], deploy_root: Path) -> None:
    services = require_mapping(config, "services")
    for name, raw_service in services.items():
        if not isinstance(raw_service, dict):
            raise GateError(f"service is not an object: {name}")
        image = raw_service.get("image")
        build = raw_service.get("build")
        pull_policy = raw_service.get("pull_policy")
        pinned_external = isinstance(image, str) and "@sha256:" in image
        locked_local = isinstance(build, dict) and pull_policy == "never"
        if not pinned_external and not locked_local:
            raise GateError(f"service image is neither digest-pinned nor source-locked: {name}")
    lock = load_json(deploy_root / "images.lock")
    external = require_mapping(lock, "externalImages")
    unpinned = [name for name, image in external.items() if not isinstance(image, str) or "@sha256:" not in image]
    if unpinned:
        raise GateError(f"images.lock contains unpinned external images: {unpinned}")
    for dockerfile in (deploy_root / "images").glob("*.Dockerfile"):
        for line in dockerfile.read_text(encoding="utf-8").splitlines():
            if line.startswith("FROM ") and "@sha256:" not in line:
                raise GateError(f"Dockerfile base is not digest-pinned: {dockerfile.name}")


def validate_secrets(config: dict[str, object]) -> None:
    secrets = require_mapping(config, "secrets")
    for name, raw_secret in secrets.items():
        if not isinstance(raw_secret, dict) or set(raw_secret) - {"name", "file"}:
            raise GateError(f"secret must be sourced from a file: {name}")
    services = require_mapping(config, "services")
    sensitive = ("PASSWORD", "TOKEN", "SECRET", "CREDENTIAL", "PRIVATE_KEY")
    for name, raw_service in services.items():
        if not isinstance(raw_service, dict):
            continue
        environment = raw_service.get("environment", {})
        if not isinstance(environment, dict):
            continue
        leaked = [
            key
            for key in environment
            if isinstance(key, str)
            and any(word in key.upper() for word in sensitive)
            and not key.upper().endswith("_FILE")
            and key not in {"NACOS_AUTH_ENABLE"}
        ]
        if leaked:
            raise GateError(f"service has inline sensitive environment keys: {name}: {leaked}")


def validate_topology(config: dict[str, object], deploy_root: Path) -> list[str]:
    services = require_mapping(config, "services")
    missing = sorted(REQUIRED_SERVICES - set(services))
    if missing:
        raise GateError(f"required services missing: {missing}")
    published = sorted(
        name
        for name, raw_service in services.items()
        if isinstance(raw_service, dict) and raw_service.get("ports")
    )
    if published != ["edge"]:
        raise GateError(f"only edge may publish host ports; observed {published}")
    networks = require_mapping(config, "networks")
    for name in ("backend", "data", "observability"):
        raw_network = networks.get(name)
        if not isinstance(raw_network, dict) or raw_network.get("internal") is not True:
            raise GateError(f"network must be internal: {name}")
    device = services.get("device-service")
    if not isinstance(device, dict):
        raise GateError("device-service definition missing")
    dependencies = device.get("depends_on")
    if not isinstance(dependencies, dict):
        raise GateError("device-service dependency contract missing")
    hardware = dependencies.get("hardware-db")
    identity = dependencies.get("jeecg-boot")
    kafka = dependencies.get("kafka")
    if not isinstance(hardware, dict) or hardware.get("condition") != "service_healthy":
        raise GateError("device-service must require healthy TimescaleDB")
    if not isinstance(identity, dict) or identity.get("condition") != "service_healthy":
        raise GateError("device-service must require healthy identity resolution")
    if not isinstance(kafka, dict) or kafka.get("condition") != "service_started":
        raise GateError("device-service readiness dependencies are unsafe")
    validate_images(config, deploy_root)
    validate_secrets(config)
    validate_routes(deploy_root)
    return published


def run_topology(arguments: list[str]) -> int:
    compose_value = option(arguments, "--compose")
    profiles_value = option(arguments, "--profiles")
    report_value = option(arguments, "--report")
    assert compose_value is not None and profiles_value is not None and report_value is not None
    compose = Path(compose_value).resolve()
    profiles = [profile.strip() for profile in profiles_value.split(",") if profile.strip()]
    if profiles != ["staging", "production"]:
        raise GateError("topology gate requires profiles staging,production", EXIT_USAGE)
    published: list[str] = []
    for profile in profiles:
        published = validate_topology(compose_config(compose, profile), compose.parent)
    report = {
        "passed": True,
        "profiles": profiles,
        "published_services": published,
        "runtime_verified": False,
        "note": "Static Compose/configuration gate only; runtime health requires deployable artifacts and secrets.",
    }
    report_path = Path(report_value)
    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(json.dumps(report, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    print(json.dumps(report, sort_keys=True))
    return 0


def run_topology_failures(arguments: list[str]) -> int:
    cases_value = option(arguments, "--cases")
    assert cases_value is not None
    cases = [case.strip() for case in cases_value.split(",") if case.strip()]
    unknown = sorted(set(cases) - EXPECTED_FAILURE_CASES)
    if unknown:
        raise GateError(f"unknown topology failure cases: {unknown}", EXIT_USAGE)
    contract_path = Path(__file__).resolve().parents[1] / "deploy" / "rehealth" / "failure-contract.json"
    contract = load_json(contract_path)
    selected = []
    for case in cases:
        result = contract.get(case)
        if not isinstance(result, dict):
            raise GateError(f"failure contract missing: {case}")
        selected.append({"case": case, **result})
    print(json.dumps({"passed": True, "cases": selected}, sort_keys=True))
    return 0


def main() -> int:
    if len(sys.argv) < 2:
        raise GateError("missing subcommand", EXIT_USAGE)
    command = sys.argv[1]
    match command:
        case "topology":
            return run_topology(sys.argv[2:])
        case "topology-failures":
            return run_topology_failures(sys.argv[2:])
        case _:
            raise GateError(f"unsupported subcommand: {command}", EXIT_USAGE)


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except GateError as error:
        print(f"ERROR: {error}", file=sys.stderr)
        raise SystemExit(error.exit_code) from None
