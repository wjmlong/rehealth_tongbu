from __future__ import annotations

import json
import os
import shutil
import subprocess
from pathlib import Path

from .report import validate_reconciliation


class SigningError(Exception):
    __slots__ = ("message",)

    def __init__(self, message: str) -> None:
        self.message = message

    def __str__(self) -> str:
        return self.message


def _cosign() -> str:
    configured = os.environ.get("REHEALTH_COSIGN_BIN")
    executable = configured or shutil.which("cosign")
    if not executable:
        raise SigningError("cosign executable is not available")
    return executable


def sign_report(report: Path, signature: Path, key_env: str) -> None:
    payload = json.loads(report.read_text(encoding="utf-8"))
    errors = validate_reconciliation(payload)
    if errors:
        raise SigningError(f"signing forbidden: {','.join(errors)}")
    if not os.environ.get(key_env):
        raise SigningError(f"signing key environment variable is empty: {key_env}")
    signature.parent.mkdir(parents=True, exist_ok=True)
    completed = subprocess.run(
        [
            _cosign(),
            "sign-blob",
            "--yes",
            "--key",
            f"env://{key_env}",
            "--output-signature",
            str(signature),
            str(report),
        ],
        check=False,
        capture_output=True,
        text=True,
    )
    if completed.returncode != 0:
        raise SigningError(f"cosign sign-blob failed: {completed.stderr.strip()}")


def verify_report(report: Path, signature: Path, key_env: str) -> None:
    if not os.environ.get(key_env):
        raise SigningError(f"verification key environment variable is empty: {key_env}")
    completed = subprocess.run(
        [
            _cosign(),
            "verify-blob",
            "--key",
            f"env://{key_env}",
            "--signature",
            str(signature),
            str(report),
        ],
        check=False,
        capture_output=True,
        text=True,
    )
    if completed.returncode != 0:
        raise SigningError(f"cosign verify-blob failed: {completed.stderr.strip()}")
