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
# ───────────────────

from __future__ import annotations

import json
import os
import sys
from datetime import UTC, datetime
from pathlib import Path

from cutover_gate import (
    CosignVerifier,
    CutoverRequest,
    GateError as CutoverGateError,
    execute_cutover,
)
from kafka_lifecycle_gate import KafkaGateError, run_kafka_gate
from attribution_gate import AttributionGateError, run_attribution_gate
from rehealth_stack_config import run_config_matrix
from rehealth_stack_failures import run_topology_failures
from rehealth_stack_topology import GateError, option, run_topology


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


def run_kafka(arguments: list[str]) -> int:
    fixture_value = option(arguments, "--fixture")
    report_value = option(arguments, "--report")
    cases_value = option(arguments, "--cases", required=False)
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


def run_attribution(arguments: list[str]) -> int:
    fixture_value = option(arguments, "--fixture")
    modes_value = option(arguments, "--modes", required=False)
    cases_value = option(arguments, "--cases", required=False)
    assert fixture_value is not None
    modes = [] if modes_value is None else [
        mode.strip() for mode in modes_value.split(",") if mode.strip()
    ]
    cases = [] if cases_value is None else [
        case.strip() for case in cases_value.split(",") if case.strip()
    ]
    try:
        payload = run_attribution_gate(Path(fixture_value).resolve(), modes, cases)
    except AttributionGateError as error:
        raise GateError(str(error)) from error
    print(json.dumps(payload, sort_keys=True))
    return 0


def main() -> int:
    if len(sys.argv) < 2:
        raise GateError("missing subcommand", 64)
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
            return run_kafka(sys.argv[2:])
        case "attribution":
            return run_attribution(sys.argv[2:])
        case _:
            raise GateError(f"unsupported subcommand: {command}", 64)


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except GateError as error:
        print(f"ERROR: {error}", file=sys.stderr)
        raise SystemExit(error.exit_code) from None
