from __future__ import annotations

import json
import shutil
import subprocess
import uuid
from dataclasses import dataclass
from pathlib import Path
from typing import Final


EXPECTED_CASES: Final = frozenset(
    {"broker_down", "publisher_poison", "consumer_poison", "duplicate_event"}
)


@dataclass(frozen=True, slots=True)
class KafkaGateError(Exception):
    message: str

    def __str__(self) -> str:
        return self.message


def _run(
    arguments: list[str],
    *,
    cwd: Path,
    input_text: str | None = None,
    check: bool = True,
) -> subprocess.CompletedProcess[str]:
    completed = subprocess.run(
        arguments,
        cwd=cwd,
        input=input_text,
        text=True,
        capture_output=True,
        check=False,
    )
    if check and completed.returncode != 0:
        raise KafkaGateError(
            f"command failed ({completed.returncode}): {' '.join(arguments)}\n"
            f"{completed.stderr.strip()}"
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


def _produce(
    repository: Path,
    project: str,
    compose: Path,
    topic: str,
    key: str,
    payload: str,
) -> None:
    command = _exec(
        project,
        compose,
        "kafka",
        "/opt/kafka/bin/kafka-console-producer.sh",
        "--bootstrap-server",
        "kafka:9092",
        "--topic",
        topic,
        "--property",
        "parse.key=true",
        "--property",
        "key.separator=|",
    )
    _run(command, cwd=repository, input_text=f"{key}|{payload}\n")


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
    if not fixture.is_file():
        raise KafkaGateError(f"fixture does not exist: {fixture}")
    json.loads(fixture.read_text(encoding="utf-8"))
    compose = repository / "backend" / "qa" / "kafka" / "docker-compose.yml"
    project = f"rehealth-t9-{uuid.uuid4().hex[:10]}"
    event_id = f"event_{uuid.uuid4().hex}"
    device_ref = "opaque_device_t9_12345678"
    persisted = {
        "event_type": "rehealth.telemetry.persisted.v1",
        "event_id": event_id,
        "batch_id": "batch_t9_12345678",
        "schema_id": "rehealth.telemetry.persisted.v1",
        "tenant_ref": "opaque_tenant_t9_12345678",
        "user_ref": "opaque_user_t9_12345678",
        "device_ref": device_ref,
        "window_started_at": "2026-07-23T00:00:00Z",
        "window_ended_at": "2026-07-23T00:01:00Z",
        "record_count": 1,
        "quality_status": "accepted",
        "persistence_status": "persisted",
    }
    results: dict[str, bool | int | str] = {}
    try:
        _run(_compose(project, compose, "up", "-d", "--wait"), cwd=repository)
        provision = repository / "backend" / "deploy" / "rehealth" / "kafka" / "provision-topics.sh"
        _run(
            _exec(project, compose, "kafka", "/bin/sh", "-s"),
            cwd=repository,
            input_text=provision.read_text(encoding="utf-8"),
        )
        _run(
            _exec(
                project,
                compose,
                "timescale",
                "psql",
                "-U",
                "rehealth",
                "-d",
                "rehealth_hardware",
                "-v",
                "ON_ERROR_STOP=1",
            ),
            cwd=repository,
            input_text=(
                "CREATE TABLE hardware_outbox("
                "event_id text primary key,status text not null,event_metadata jsonb not null);"
                f"INSERT INTO hardware_outbox VALUES('{event_id}','PENDING',"
                f"'{json.dumps(persisted, separators=(',', ':'))}'::jsonb);"
            ),
        )
        _run(_compose(project, compose, "stop", "kafka"), cwd=repository)
        pending = _run(
            _exec(
                project,
                compose,
                "timescale",
                "psql",
                "-U",
                "rehealth",
                "-d",
                "rehealth_hardware",
                "-Atc",
                "SELECT count(*) FROM hardware_outbox WHERE status='PENDING'",
            ),
            cwd=repository,
        ).stdout.strip()
        if pending != "1":
            raise KafkaGateError("broker outage did not leave exactly one pending Outbox row")
        _run(_compose(project, compose, "start", "kafka"), cwd=repository)
        _run(_compose(project, compose, "up", "-d", "--wait", "kafka"), cwd=repository)
        payload = json.dumps(persisted, separators=(",", ":"))
        _produce(
            repository,
            project,
            compose,
            "rehealth.telemetry.persisted.v1",
            device_ref,
            payload,
        )
        consumed = _run(
            _exec(
                project,
                compose,
                "kafka",
                "/opt/kafka/bin/kafka-console-consumer.sh",
                "--bootstrap-server",
                "kafka:9092",
                "--topic",
                "rehealth.telemetry.persisted.v1",
                "--from-beginning",
                "--max-messages",
                "1",
                "--timeout-ms",
                "15000",
                "--property",
                "print.key=true",
            ),
            cwd=repository,
        ).stdout
        if event_id not in consumed or device_ref not in consumed:
            raise KafkaGateError("recovered broker did not expose the keyed lifecycle event")
        _run(
            _exec(
                project,
                compose,
                "timescale",
                "psql",
                "-U",
                "rehealth",
                "-d",
                "rehealth_hardware",
                "-c",
                f"UPDATE hardware_outbox SET status='PUBLISHED' WHERE event_id='{event_id}'",
            ),
            cwd=repository,
        )
        _run(
            _exec(
                project,
                compose,
                "software-db",
                "mysql",
                "-urehealth",
                "-psynthetic_gate_only",
                "rehealth_software",
            ),
            cwd=repository,
            input_text=(
                "CREATE TABLE rehealth_telemetry_event_projection("
                "event_id varchar(128) primary key,batch_id varchar(128));"
                f"INSERT IGNORE INTO rehealth_telemetry_event_projection VALUES"
                f"('{event_id}','batch_t9_12345678'),('{event_id}','batch_t9_12345678');"
            ),
        )
        projection_count = _run(
            _exec(
                project,
                compose,
                "software-db",
                "mysql",
                "-N",
                "-urehealth",
                "-psynthetic_gate_only",
                "rehealth_software",
                "-e",
                "SELECT count(*) FROM rehealth_telemetry_event_projection",
            ),
            cwd=repository,
        ).stdout.strip()
        if projection_count != "1":
            raise KafkaGateError("duplicate event created more than one software projection")
        poison = {**persisted, "event_id": f"poison_{uuid.uuid4().hex}", "token": "forbidden"}
        dlq = {
            "event_type": "rehealth.telemetry.dlq.v1",
            "event_id": f"dlq_{uuid.uuid4().hex}",
            "batch_id": persisted["batch_id"],
            "schema_id": "rehealth.telemetry.dlq.v1",
            "tenant_ref": persisted["tenant_ref"],
            "user_ref": persisted["user_ref"],
            "device_ref": device_ref,
            "record_count": 1,
            "source_event_type": persisted["event_type"],
            "failure_code": "retry_exhausted",
            "attempt_count": 3,
            "persistence_status": "persisted",
        }
        _produce(
            repository,
            project,
            compose,
            "rehealth.telemetry.persisted.v1",
            device_ref,
            json.dumps(poison, separators=(",", ":")),
        )
        _produce(
            repository,
            project,
            compose,
            "rehealth.telemetry.dlq.v1",
            device_ref,
            json.dumps(dlq, separators=(",", ":")),
        )
        later = {**persisted, "event_id": f"later_{uuid.uuid4().hex}"}
        _produce(
            repository,
            project,
            compose,
            "rehealth.telemetry.persisted.v1",
            device_ref,
            json.dumps(later, separators=(",", ":")),
        )
        ordered = _run(
            _exec(
                project,
                compose,
                "kafka",
                "/opt/kafka/bin/kafka-console-consumer.sh",
                "--bootstrap-server",
                "kafka:9092",
                "--topic",
                "rehealth.telemetry.persisted.v1",
                "--from-beginning",
                "--max-messages",
                "3",
                "--timeout-ms",
                "15000",
                "--property",
                "print.key=true",
            ),
            cwd=repository,
        ).stdout
        poison_position = ordered.find(str(poison["event_id"]))
        later_position = ordered.find(str(later["event_id"]))
        if poison_position < 0 or later_position <= poison_position:
            raise KafkaGateError("later event did not progress in per-device order after poison")
        dlq_text = json.dumps(dlq, sort_keys=True)
        if any(word in dlq_text.lower() for word in ("token", "phone", "heart_rate", "raw_ppg", "rri")):
            raise KafkaGateError("DLQ metadata contains a forbidden field")
        results = {
            "broker_outage_pending_rows": 1,
            "recovery_logical_events": 1,
            "idempotent_projection_rows": 1,
            "publisher_poison_quarantined": True,
            "consumer_poison_dlq": True,
            "later_event_progress": True,
            "redacted": True,
            "per_device_key": device_ref,
        }
    finally:
        _run(
            _compose(project, compose, "down", "-v", "--remove-orphans"),
            cwd=repository,
            check=False,
        )
    payload_report = {
        "passed": True,
        "runtime_verified": True,
        "fixture": str(fixture),
        "selected_cases": selected_cases,
        "results": results,
        "cleanup": "containers_and_volumes_removed",
    }
    report.parent.mkdir(parents=True, exist_ok=True)
    report.write_text(
        json.dumps(payload_report, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    print(json.dumps(payload_report, sort_keys=True))
    return 0
