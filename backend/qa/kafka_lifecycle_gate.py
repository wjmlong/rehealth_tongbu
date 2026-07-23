from __future__ import annotations

import json
import os
import shutil
import subprocess
import uuid
from dataclasses import dataclass
from pathlib import Path
from typing import Final


EXPECTED_CASES: Final = frozenset(
    {"broker_down", "publisher_poison", "consumer_poison", "duplicate_event"}
)
MAVEN_IMAGE: Final = "maven:3.9.11-eclipse-temurin-17"


@dataclass(frozen=True, slots=True)
class KafkaGateError(Exception):
    message: str

    def __str__(self) -> str:
        return self.message


def _run(
    arguments: list[str],
    *,
    cwd: Path,
    input_bytes: bytes | None = None,
    check: bool = True,
) -> subprocess.CompletedProcess[bytes]:
    completed = subprocess.run(
        arguments,
        cwd=cwd,
        input=input_bytes,
        capture_output=True,
        check=False,
    )
    if check and completed.returncode != 0:
        stderr = completed.stderr.decode("utf-8", errors="replace").strip()
        stdout = completed.stdout.decode("utf-8", errors="replace").strip()
        raise KafkaGateError(
            f"command failed ({completed.returncode}): {' '.join(arguments)}\n"
            f"{stderr or stdout}"
        )
    return completed


def _compose(project: str, compose: Path, *arguments: str) -> list[str]:
    return [
        "docker",
        "compose",
        "-p",
        project,
        "-f",
        str(compose),
        *arguments,
    ]


def _exec(project: str, compose: Path, service: str, *arguments: str) -> list[str]:
    return _compose(project, compose, "exec", "-T", service, *arguments)


def _container_path(repository: Path, path: Path) -> str:
    absolute = path.resolve()
    try:
        relative = absolute.relative_to(repository)
    except ValueError as exception:
        raise KafkaGateError(
            f"gate artifact must be inside the repository: {absolute}"
        ) from exception
    return "/workspace/" + relative.as_posix()


def _assert_cleanup(repository: Path, project: str) -> None:
    remaining = _run(
        [
            "docker",
            "ps",
            "-aq",
            "--filter",
            f"label=com.docker.compose.project={project}",
        ],
        cwd=repository,
    ).stdout.decode("utf-8", errors="replace").strip()
    if remaining:
        raise KafkaGateError(f"Kafka gate resources were not removed: {remaining}")


def run_kafka_gate(
    fixture: Path,
    report: Path,
    selected_cases: list[str],
) -> int:
    unknown = sorted(set(selected_cases) - EXPECTED_CASES)
    if unknown:
        raise KafkaGateError(f"unknown Kafka failure cases: {unknown}")
    if shutil.which("docker") is None:
        raise KafkaGateError("docker executable is not available")

    repository = Path(__file__).resolve().parents[2]
    fixture = fixture.resolve()
    report = report.resolve()
    if not fixture.is_file():
        raise KafkaGateError(f"fixture does not exist: {fixture}")
    json.loads(fixture.read_text(encoding="utf-8"))

    compose = repository / "backend" / "qa" / "kafka" / "docker-compose.yml"
    docker_repository = os.environ.get(
        "REHEALTH_GATE_DOCKER_REPOSITORY", str(repository)
    )
    project = f"rehealth-t9-{uuid.uuid4().hex[:10]}"
    integration_succeeded = False
    try:
        _run(_compose(project, compose, "up", "-d", "--wait"), cwd=repository)
        provision = (
            repository
            / "backend"
            / "deploy"
            / "rehealth"
            / "kafka"
            / "provision-topics.sh"
        )
        portable_script = provision.read_bytes().replace(b"\r\n", b"\n")
        _run(
            _exec(project, compose, "kafka", "/bin/sh", "-s"),
            cwd=repository,
            input_bytes=portable_script,
        )

        maven = [
            "docker",
            "run",
            "--rm",
            "--network",
            f"{project}_default",
            "-v",
            "rehealth-m2:/root/.m2",
            "-v",
            f"{docker_repository}:/workspace",
            "-w",
            "/workspace",
            MAVEN_IMAGE,
            "mvn",
            "-f",
            "backend/device-service/pom.xml",
            "-Dtest=KafkaLifecycleIT",
            f"-Dkafka.bootstrap=kafka:9092",
            "-Dtimescale.url=jdbc:postgresql://timescale:5432/rehealth_hardware",
            "-Dtimescale.username=rehealth",
            "-Dtimescale.password=synthetic_gate_only",
            "-Dmysql.url=jdbc:mysql://software-db:3306/rehealth_software"
            "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
            "-Dmysql.username=rehealth",
            "-Dmysql.password=synthetic_gate_only",
            f"-Dgate.fixture={_container_path(repository, fixture)}",
            f"-Dgate.report={_container_path(repository, report)}",
            f"-Dgate.cases={','.join(selected_cases)}",
            "test",
        ]
        completed = _run(maven, cwd=repository)
        if completed.stdout:
            print(completed.stdout.decode("utf-8", errors="replace"), end="")
        integration_succeeded = True
    finally:
        _run(
            _compose(project, compose, "down", "-v", "--remove-orphans"),
            cwd=repository,
            check=False,
        )
        _assert_cleanup(repository, project)

    if not integration_succeeded or not report.is_file():
        raise KafkaGateError("production lifecycle integration did not create its report")
    payload = json.loads(report.read_text(encoding="utf-8"))
    if payload.get("passed") is not True or payload.get("runtime_verified") is not True:
        raise KafkaGateError("production lifecycle integration report did not pass")
    payload["cleanup"] = "containers_and_volumes_removed"
    report.parent.mkdir(parents=True, exist_ok=True)
    report.write_text(
        json.dumps(payload, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
        newline="\n",
    )
    print(json.dumps(payload, sort_keys=True))
    return 0
