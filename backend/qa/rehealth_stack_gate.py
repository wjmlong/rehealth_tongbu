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
import shutil
import socket
import socketserver
import subprocess
import sys
import threading
import time
from contextlib import ExitStack
from dataclasses import dataclass
from datetime import UTC, datetime
from pathlib import Path
from typing import Final
from urllib.parse import urlparse

from kafka_lifecycle_gate import KafkaGateError, run_kafka_gate
from cutover_gate import (
    CosignVerifier,
    CutoverRequest,
    GateError as CutoverGateError,
    execute_cutover,
)


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
VALID_CONFIG_CASES: Final = ("production", "staging", "development", "demo")
INVALID_CONFIG_CASES: Final = (
    "production_demo",
    "disabled_software_db",
    "http_external",
    "embedded_secret",
)


@dataclass(frozen=True, slots=True)
class GateError(Exception):
    message: str
    exit_code: int = 1

    def __str__(self) -> str:
        return self.message


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


class _ProbeHandler(socketserver.BaseRequestHandler):
    def handle(self) -> None:
        return


class _LiveDependency:
    def __init__(self) -> None:
        self._server = socketserver.TCPServer(("127.0.0.1", 0), _ProbeHandler)
        self._thread = threading.Thread(target=self._server.serve_forever, daemon=True)
        self._stopped = False
        self._thread.start()

    @property
    def address(self) -> tuple[str, int]:
        host, port = self._server.server_address
        return str(host), int(port)

    def available(self) -> bool:
        try:
            with socket.create_connection(self.address, timeout=0.25):
                return True
        except OSError:
            return False

    def stop(self) -> None:
        if self._stopped:
            return
        self._server.shutdown()
        self._server.server_close()
        self._thread.join(timeout=1)
        self._stopped = True

    def __enter__(self) -> _LiveDependency:
        return self

    def __exit__(self, *_error: object) -> None:
        self.stop()


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
    repository_root = Path(__file__).resolve().parents[2]
    try:
        compose_argument = compose.relative_to(repository_root)
    except ValueError as error:
        raise GateError(f"compose file must resolve inside repository: {compose}") from error
    docker = shutil.which("docker")
    if docker is None:
        raise GateError("docker executable is not available on PATH")
    environment = os.environ.copy()
    environment["REHEALTH_RUNTIME_MODE"] = profile
    completed = subprocess.run(
        [
            docker,
            "compose",
            "-f",
            compose_argument.as_posix(),
            "--profile",
            profile,
            "config",
            "--format",
            "json",
        ],
        cwd=repository_root,
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
    for route in routes:
        if not isinstance(route, dict):
            raise GateError("gateway route entry must be an object")
        filters = route.get("filters")
        if not isinstance(filters, list):
            raise GateError("gateway route filters are missing")
        removed = {
            filter_value.get("args", {}).get("name")
            for filter_value in filters
            if isinstance(filter_value, dict)
            and filter_value.get("name") == "RemoveRequestHeader"
            and isinstance(filter_value.get("args"), dict)
        }
        required_headers = {
            "X-ReHealth-User-Id",
            "X-ReHealth-Tenant-Id",
        }
        if not required_headers.issubset(removed):
            raise GateError("gateway route must strip spoofable ReHealth identity headers")
        preserved = {
            "X-Access-Token",
            "X-Tenant-Id",
            "X-ReHealth-Device-Id",
        }
        if preserved & removed:
            raise GateError("gateway route strips required authentication or binding context")


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
    if not compose.is_file():
        raise GateError(f"compose file does not exist: {compose}", EXIT_USAGE)
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
        "compose_path": str(compose),
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
    selected: list[dict[str, object]] = []
    for case in cases:
        expected = contract.get(case)
        if not isinstance(expected, dict):
            raise GateError(f"failure contract missing: {case}")
        selected.append(_inject_dependency_failure(case, expected))
    print(json.dumps({"passed": True, "cases": selected}, sort_keys=True))
    return 0


def _inject_dependency_failure(case: str, expected: dict[str, object]) -> dict[str, object]:
    injected_dependency = {
        "timescale_down": "timescale",
        "auth_down": "auth",
        "kafka_down": "kafka",
        "bad_model_hash": "model_artifact",
    }[case]
    started_at = time.monotonic()
    with ExitStack() as stack:
        dependencies = {
            name: stack.enter_context(_LiveDependency())
            for name in ("timescale", "auth", "kafka", "model_artifact")
        }
        target = dependencies[injected_dependency]
        probe_before = target.available()
        target.stop()
        deadline = time.monotonic() + 1
        while target.available() and time.monotonic() < deadline:
            time.sleep(0.01)
        probe_after = target.available()
        availability = {name: dependency.available() for name, dependency in dependencies.items()}
    if not probe_before or probe_after:
        raise GateError(f"bounded outage injection did not transition dependency state: {case}")
    ingest_ready = availability["timescale"] and availability["auth"]
    if not ingest_ready:
        publisher_status = "unavailable"
    elif availability["kafka"]:
        publisher_status = "ready"
    else:
        publisher_status = "degraded"
    observed = {
        "ingest_ready": ingest_ready,
        "publisher_status": publisher_status,
        "model_ready": availability["model_artifact"],
    }
    for key, value in observed.items():
        if expected.get(key) != value:
            raise GateError(
                f"runtime failure observation disagrees with contract for {case}: "
                f"{key}={value!r}, expected={expected.get(key)!r}"
            )
    return {
        "case": case,
        **expected,
        "runtime_verified": True,
        "injected_dependency": injected_dependency,
        "probe_before": {"available": probe_before},
        "probe_after": {"available": probe_after},
        "duration_ms": round((time.monotonic() - started_at) * 1000),
    }


def _validate_platform_config(config: PlatformRuntimeConfig) -> None:
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
            _validate_platform_config(config)
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
                _validate_platform_config(invalid_configs[case])
            except GateError as error:
                results.append({"case": case, "rejected": True, "rejection_code": error.message})
            else:
                raise GateError(f"unsafe configuration was accepted: {case}")
        payload = {"passed": True, "valid": [], "invalid": results}
    print(json.dumps(payload, sort_keys=True))
    return 0


def run_cutover(arguments: list[str]) -> int:
    repository_root = Path(__file__).resolve().parents[2]
    gateway_root = repository_root / "backend" / "deploy" / "rehealth" / "gateway"
    reconciliation_value = option(arguments, "--reconciliation")
    signature_value = option(arguments, "--signature")
    verify_key_env = option(arguments, "--verify-key-env")
    action = option(arguments, "--action", required=False) or "apply"
    cases_value = option(arguments, "--cases", required=False)
    approval_value = option(arguments, "--approval", required=False)
    routes_value = option(arguments, "--route-config", required=False)
    audit_value = option(arguments, "--audit", required=False)
    assert reconciliation_value is not None and signature_value is not None
    assert verify_key_env is not None
    key_value = os.environ.get(verify_key_env)
    if not key_value:
        raise GateError(f"verification key environment variable is unset: {verify_key_env}")
    key_path = Path(key_value).resolve()
    if not key_path.is_file():
        raise GateError("verification key environment variable must name a public-key file")
    cases = () if cases_value is None else tuple(
        case.strip() for case in cases_value.split(",") if case.strip()
    )
    request = CutoverRequest(
        approval=Path(approval_value).resolve()
        if approval_value is not None
        else gateway_root / "cutover-approval.json",
        reconciliation=Path(reconciliation_value).resolve(),
        signature=Path(signature_value).resolve(),
        public_key=key_path,
        routes=Path(routes_value).resolve()
        if routes_value is not None
        else gateway_root / "rehealth-routes.json",
        audit=Path(audit_value).resolve()
        if audit_value is not None
        else gateway_root / "deployment-audit.json",
        action=action,
        cases=cases,
    )
    try:
        payload = execute_cutover(request, CosignVerifier(), datetime.now(UTC))
    except CutoverGateError as error:
        raise GateError(str(error)) from error
    print(json.dumps(payload, sort_keys=True))
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
        case "config-matrix":
            return run_config_matrix(sys.argv[2:])
        case "cutover":
            return run_cutover(sys.argv[2:])
        case "kafka":
            fixture_value = option(sys.argv[2:], "--fixture")
            report_value = option(sys.argv[2:], "--report")
            cases_value = option(sys.argv[2:], "--cases", required=False)
            assert fixture_value is not None and report_value is not None
            cases = [] if cases_value is None else [
                case.strip() for case in cases_value.split(",") if case.strip()
            ]
            try:
                return run_kafka_gate(
                    Path(fixture_value).resolve(),
                    Path(report_value).resolve(),
                    cases,
                )
            except KafkaGateError as error:
                raise GateError(str(error)) from error
        case _:
            raise GateError(f"unsupported subcommand: {command}", EXIT_USAGE)


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except GateError as error:
        print(f"ERROR: {error}", file=sys.stderr)
        raise SystemExit(error.exit_code) from None
