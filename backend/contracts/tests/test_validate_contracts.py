from __future__ import annotations

import json
import subprocess
import sys
from pathlib import Path
from typing import Mapping

import pytest

VALIDATOR = Path(__file__).parents[1] / "scripts" / "validate_contracts.py"


def run_fixture(tmp_path: Path, payload: Mapping[str, str | int | Mapping[str, int]]) -> subprocess.CompletedProcess[str]:
    fixture_dir = tmp_path / "fixtures"
    fixture_dir.mkdir()
    (fixture_dir / "payload.json").write_text(json.dumps(payload), encoding="utf-8")
    return subprocess.run(
        [sys.executable, str(VALIDATOR), "--fixtures", str(fixture_dir)],
        check=False,
        capture_output=True,
        text=True,
    )


def test_unknown_event_type_is_rejected(tmp_path: Path) -> None:
    result = run_fixture(tmp_path, {"event_type": "rehealth.telemetry.persistedd.v1"})
    report = json.loads(result.stdout)
    assert result.returncode == 1
    assert "schema:unknown_event_type" in report["rejected_reasons"]


def test_path_like_event_type_is_rejected(tmp_path: Path) -> None:
    result = run_fixture(tmp_path, {"event_type": "../../outside"})
    report = json.loads(result.stdout)
    assert result.returncode == 1
    assert "schema:unknown_event_type" in report["rejected_reasons"]


def test_schema_ref_escape_is_rejected(tmp_path: Path) -> None:
    result = run_fixture(tmp_path, {"event_type": "rehealth.telemetry.persisted.v1", "$ref": "../../outside"})
    report = json.loads(result.stdout)
    assert result.returncode == 1
    assert "schema_ref" in report["rejected_reasons"]


@pytest.mark.parametrize(
    ("field", "value", "reason"),
    [
        ("metrics", {"sample": 72}, "metric_value"),
        ("heart_rate", 72, "metric_value"),
        ("spo2", 98, "metric_value"),
        ("raw_ppg", "AAECAw==", "raw_signal"),
    ],
)
def test_sensitive_metric_fields_are_rejected(
    tmp_path: Path,
    field: str,
    value: str | int | Mapping[str, int],
    reason: str,
) -> None:
    result = run_fixture(tmp_path, {"event_type": "rehealth.telemetry.persisted.v1", field: value})
    report = json.loads(result.stdout)
    assert result.returncode == 1
    assert reason in report["rejected_reasons"]
