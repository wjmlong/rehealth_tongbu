from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path

import pytest


REPO_ROOT = Path(__file__).resolve().parents[3]
GATE = REPO_ROOT / "backend" / "qa" / "rehealth_stack_gate.py"
COMPOSE = REPO_ROOT / "backend" / "deploy" / "rehealth" / "docker-compose.yml"


def run_gate(*arguments: str) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        [sys.executable, str(GATE), *arguments],
        cwd=REPO_ROOT,
        check=False,
        capture_output=True,
        text=True,
    )


def test_topology_writes_a_passing_report(tmp_path: Path) -> None:
    report = tmp_path / "topology.json"

    result = run_gate(
        "topology",
        "--compose",
        str(COMPOSE),
        "--profiles",
        "staging,production",
        "--report",
        str(report),
    )

    assert result.returncode == 0, result.stderr
    payload = json.loads(report.read_text(encoding="utf-8"))
    assert payload["passed"] is True
    assert payload["published_services"] == ["edge"]
    assert payload["profiles"] == ["staging", "production"]
    assert payload["compose_path"] == str(COMPOSE.resolve())
    assert payload["runtime_verified"] is False


@pytest.mark.parametrize(
    ("case", "ingest_ready", "publisher_status"),
    [
        ("timescale_down", False, "unavailable"),
        ("auth_down", False, "unavailable"),
        ("kafka_down", True, "degraded"),
        ("bad_model_hash", True, "ready"),
    ],
)
def test_failure_injection_is_fail_closed_by_capability(
    case: str,
    ingest_ready: bool,
    publisher_status: str,
) -> None:
    result = run_gate("topology-failures", "--cases", case)

    assert result.returncode == 0, result.stderr
    payload = json.loads(result.stdout)
    observed = payload["cases"][0]
    assert observed["runtime_verified"] is True
    assert observed["probe_before"]["available"] is True
    assert observed["probe_after"]["available"] is False
    assert observed["ingest_ready"] is ingest_ready
    assert observed["publisher_status"] == publisher_status
    if case == "bad_model_hash":
        assert observed["model_ready"] is False


def test_config_matrix_executes_valid_runtime_modes() -> None:
    result = run_gate("config-matrix", "--valid", "production,staging,development,demo")

    assert result.returncode == 0, result.stderr
    payload = json.loads(result.stdout)
    assert payload["passed"] is True
    assert [case["mode"] for case in payload["valid"]] == [
        "production",
        "staging",
        "development",
        "demo",
    ]


def test_config_matrix_executes_invalid_runtime_cases() -> None:
    result = run_gate(
        "config-matrix",
        "--invalid",
        "production_demo,disabled_software_db,http_external,embedded_secret",
    )

    assert result.returncode == 0, result.stderr
    payload = json.loads(result.stdout)
    assert [case["rejection_code"] for case in payload["invalid"]] == [
        "ATTRIBUTION_MODE_UNSAFE",
        "SOFTWARE_DB_REQUIRED",
        "SECURE_URL_REQUIRED",
        "EMBEDDED_SECRET_FORBIDDEN",
    ]


def test_unknown_subcommand_is_explicitly_unsupported() -> None:
    result = run_gate("cutover")

    assert result.returncode == 64
    assert "unsupported" in result.stderr.lower()
