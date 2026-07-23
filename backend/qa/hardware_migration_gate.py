#!/usr/bin/env -S uv run --script
# /// script
# requires-python = ">=3.11"
# dependencies = [
#   "typer==0.16.1",
# ]
# ///

# ─── How to run ───
# 1. Generate Todo 10 reconciliation.json with migrate_hardware.py.
# 2. Validate it without changing any route:
#      uv run backend/qa/hardware_migration_gate.py reconciliation --report reconciliation.json
# 3. Add --signature and --verify-key-env to require cosign verification.
# ──────────────────

from __future__ import annotations

import hashlib
import json
import re
import sys
from datetime import datetime
from pathlib import Path

import typer


REPOSITORY_ROOT = Path(__file__).resolve().parents[2]
MIGRATION_SCRIPTS = REPOSITORY_ROOT / "backend" / "device-service" / "scripts"
sys.path.insert(0, str(MIGRATION_SCRIPTS))

from hardware_migration.report import validate_reconciliation
from hardware_migration.signing import verify_report
from hardware_migration.models import Value


app = typer.Typer(no_args_is_help=True, add_completion=False)
SHA256 = re.compile(r"^sha256:[0-9a-f]{64}$")
GIT_SHA = re.compile(r"^[0-9a-f]{40}$")


@app.callback()
def main() -> None:
    pass


def _require_hash(value: Value, field: str) -> None:
    if not isinstance(value, str) or SHA256.fullmatch(value) is None:
        raise typer.BadParameter(f"{field} must be a credential-free sha256 fingerprint")


def validate_structure(payload: dict[str, Value]) -> None:
    required = {
        "schema_version",
        "source",
        "target",
        "window",
        "tables",
        "shadow_reads",
        "mismatches",
        "mismatch_count",
        "eligible",
        "git_sha",
        "created_at",
        "expires_at",
    }
    if set(payload) != required:
        raise typer.BadParameter(
            f"reconciliation fields differ: missing={sorted(required - set(payload))} "
            f"extra={sorted(set(payload) - required)}"
        )
    for side in ("source", "target"):
        descriptor = payload[side]
        if not isinstance(descriptor, dict):
            raise typer.BadParameter(f"{side} descriptor must be an object")
        if set(descriptor) != {"dsn_fingerprint", "schema_version"}:
            raise typer.BadParameter(f"{side} descriptor fields differ")
        _require_hash(descriptor["dsn_fingerprint"], f"{side}.dsn_fingerprint")
    tables = payload["tables"]
    mismatches = payload["mismatches"]
    shadows = payload["shadow_reads"]
    if not isinstance(tables, list) or len(tables) != 6:
        raise typer.BadParameter("exactly six legacy hardware table reports are required")
    if not isinstance(mismatches, list) or payload["mismatch_count"] != len(mismatches):
        raise typer.BadParameter("mismatch_count must equal mismatches length")
    if not isinstance(shadows, list):
        raise typer.BadParameter("shadow_reads must be an array")
    if not isinstance(payload["git_sha"], str) or GIT_SHA.fullmatch(payload["git_sha"]) is None:
        raise typer.BadParameter("git_sha must be a full 40-character SHA")
    for field in ("created_at", "expires_at"):
        value = payload[field]
        if not isinstance(value, str):
            raise typer.BadParameter(f"{field} must be UTC")
        datetime.fromisoformat(value.replace("Z", "+00:00"))


@app.command()
def reconciliation(
    report: Path = typer.Option(...),
    signature: Path | None = typer.Option(None),
    verify_key_env: str = typer.Option("REHEALTH_CUTOVER_VERIFY_KEY"),
    expect_blocked: bool = typer.Option(False),
    output: Path | None = typer.Option(None),
) -> None:
    report_bytes = report.resolve().read_bytes()
    payload = json.loads(report_bytes)
    if not isinstance(payload, dict):
        raise typer.BadParameter("reconciliation report must be an object")
    validate_structure(payload)
    errors = validate_reconciliation(payload)
    blocked = bool(errors)
    if expect_blocked != blocked:
        raise typer.BadParameter(
            f"unexpected reconciliation decision: blocked={blocked} errors={errors}"
        )
    if signature is not None:
        if blocked:
            raise typer.BadParameter("signature verification is forbidden for blocked reports")
        verify_report(report.resolve(), signature.resolve(), verify_key_env)
    result = {
        "passed": True,
        "blocked": blocked,
        "errors": list(errors),
        "reconciliation_sha256": hashlib.sha256(report_bytes).hexdigest(),
        "signature_verified": signature is not None,
        "route_changed": False,
    }
    encoded = json.dumps(result, separators=(",", ":"), sort_keys=True) + "\n"
    if output is not None:
        output.resolve().parent.mkdir(parents=True, exist_ok=True)
        output.resolve().write_text(encoded, encoding="utf-8")
    typer.echo(encoded, nl=False)


if __name__ == "__main__":
    app()
