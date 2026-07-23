from __future__ import annotations

import hashlib
import json
import os
import shutil
import stat
import subprocess
import tempfile
from datetime import UTC, datetime
from pathlib import Path

from cutover_types import GateError, SignatureVerifier


COSIGN_IMAGE = (
    "ghcr.io/sigstore/cosign/cosign:v2.4.3@"
    "sha256:c77247c92f4dfea851c70555738226498393e34e2f9ca83cb959e51c230e4ad7"
)


class CosignVerifier:
    def verify(self, reconciliation: bytes, signature: bytes, public_key: bytes) -> None:
        with tempfile.TemporaryDirectory(prefix="rehealth-cutover-") as directory:
            root = Path(directory)
            (root / "reconciliation.json").write_bytes(reconciliation)
            (root / "reconciliation.sig").write_bytes(signature)
            (root / "verification.pub").write_bytes(public_key)
            cosign = shutil.which("cosign")
            if cosign is not None:
                command = [
                    cosign,
                    "verify-blob",
                    "--insecure-ignore-tlog",
                    "--key",
                    str(root / "verification.pub"),
                    "--signature",
                    str(root / "reconciliation.sig"),
                    str(root / "reconciliation.json"),
                ]
            else:
                docker = shutil.which("docker")
                if docker is None:
                    raise GateError("COSIGN_UNAVAILABLE")
                command = [
                    docker,
                    "run",
                    "--rm",
                    "--network",
                    "none",
                    "-v",
                    f"{root}:/work:ro",
                    COSIGN_IMAGE,
                    "verify-blob",
                    "--insecure-ignore-tlog",
                    "--key",
                    "/work/verification.pub",
                    "--signature",
                    "/work/reconciliation.sig",
                    "/work/reconciliation.json",
                ]
            completed = subprocess.run(command, check=False, capture_output=True, text=True)
            if completed.returncode != 0:
                raise GateError("SIGNATURE_INVALID")


def exact_read(path: Path) -> bytes:
    metadata = path.lstat()
    if stat.S_ISLNK(metadata.st_mode) or not stat.S_ISREG(metadata.st_mode):
        raise GateError("ARTIFACT_DESCRIPTOR_INVALID")
    descriptor = os.open(path, os.O_RDONLY | getattr(os, "O_BINARY", 0))
    try:
        opened = os.fstat(descriptor)
        content = b""
        while chunk := os.read(descriptor, 65_536):
            content += chunk
        closed = os.fstat(descriptor)
    finally:
        os.close(descriptor)
    if (opened.st_size, opened.st_mtime_ns) != (closed.st_size, closed.st_mtime_ns):
        raise GateError("ARTIFACT_CHANGED_DURING_READ")
    return content


def json_object(content: bytes, code: str) -> dict[str, object]:
    try:
        value = json.loads(content)
    except (UnicodeDecodeError, json.JSONDecodeError) as error:
        raise GateError(code) from error
    if not isinstance(value, dict):
        raise GateError(code)
    return value


def mapping(value: object, code: str) -> dict[str, object]:
    if not isinstance(value, dict):
        raise GateError(code)
    return value


def text(container: dict[str, object], key: str, code: str) -> str:
    value = container.get(key)
    if not isinstance(value, str) or not value:
        raise GateError(code)
    return value


def sha256(content: bytes) -> str:
    return hashlib.sha256(content).hexdigest()


def timestamp(value: object) -> datetime:
    if not isinstance(value, str):
        raise GateError("REPORT_WINDOW_INVALID")
    try:
        parsed = datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError as error:
        raise GateError("REPORT_WINDOW_INVALID") from error
    if parsed.tzinfo is None:
        raise GateError("REPORT_WINDOW_INVALID")
    return parsed.astimezone(UTC)


def validate_artifact(
    approval: dict[str, object],
    reconciliation_bytes: bytes,
    signature: bytes,
    public_key: bytes,
    verifier: SignatureVerifier,
    now: datetime,
) -> dict[str, object]:
    report = json_object(reconciliation_bytes, "RECONCILIATION_INVALID")
    if approval.get("approvalStatus") != "approved" or approval.get("artifactState") != "clean":
        raise GateError("ARTIFACT_NOT_APPROVED")
    if report.get("eligible") is not True or report.get("mismatch_count") != 0:
        raise GateError("RECONCILIATION_INELIGIBLE")
    if report.get("mismatches") != []:
        raise GateError("RECONCILIATION_DIRTY")
    if now < timestamp(report.get("created_at")) or now >= timestamp(report.get("expires_at")):
        raise GateError("REPORT_EXPIRED")
    if report.get("git_sha") != approval.get("expectedGitSha"):
        raise GateError("GIT_SHA_STALE")
    for side in ("source", "target"):
        observed = mapping(report.get(side), "RECONCILIATION_INVALID")
        expected = mapping(approval.get(side), "APPROVAL_INVALID")
        if observed.get("dsn_fingerprint") != expected.get("dsnFingerprint"):
            raise GateError("DSN_FINGERPRINT_MISMATCH")
        if observed.get("schema_version") != expected.get("schemaVersion"):
            raise GateError("SCHEMA_FINGERPRINT_MISMATCH")
    if sha256(reconciliation_bytes) != approval.get("reconciliationSha256"):
        raise GateError("RECONCILIATION_BYTES_DIRTY")
    if sha256(signature) != approval.get("signatureSha256"):
        raise GateError("SIGNATURE_BYTES_DIRTY")
    if sha256(public_key) != approval.get("publicKeySha256"):
        raise GateError("PUBLIC_KEY_MISMATCH")
    verifier.verify(reconciliation_bytes, signature, public_key)
    return report
