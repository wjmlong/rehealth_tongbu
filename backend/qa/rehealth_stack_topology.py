from __future__ import annotations

import json
import os
import shutil
import subprocess
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
