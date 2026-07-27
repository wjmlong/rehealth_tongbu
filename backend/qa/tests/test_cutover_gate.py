from __future__ import annotations

import hashlib
import json
import subprocess
from datetime import UTC, datetime, timedelta
from pathlib import Path

import pytest

from cutover_gate import CosignVerifier, CutoverRequest, GateError, execute_cutover
from cutover_verification import exact_read


GIT_SHA = "6f0ded1b21fc46fba74c9d14b40a05be41423664"
SOURCE_FP = "sha256:" + "1" * 64
TARGET_FP = "sha256:" + "2" * 64
PUBLIC_KEY = b"synthetic-cosign-public-key"
SIGNATURE = b"synthetic-cosign-signature"
TELEMETRY_PATHS = (
    "/jeecg-boot/rehealth/mobile/measurements/batch",
    "/jeecg-boot/rehealth/mobile/measurements/recent",
)


class ExactVerifier:
    def verify(self, reconciliation: bytes, signature: bytes, public_key: bytes) -> None:
        assert reconciliation
        if signature != SIGNATURE or public_key != PUBLIC_KEY:
            raise GateError("SIGNATURE_INVALID")


def test_cosign_verifier_is_offline_for_public_key_blob(
    monkeypatch: pytest.MonkeyPatch,
) -> None:
    observed: list[str] = []

    def fake_run(command: list[str], **_options: object) -> subprocess.CompletedProcess[str]:
        observed.extend(command)
        return subprocess.CompletedProcess(command, 0, "Verified OK", "")

    monkeypatch.setattr("cutover_verification.shutil.which", lambda name: name)
    monkeypatch.setattr("cutover_verification.subprocess.run", fake_run)

    CosignVerifier().verify(b"report", b"signature", b"public-key")

    assert "--insecure-ignore-tlog" in observed


def test_exact_read_preserves_route_descriptor_bytes_on_windows(tmp_path: Path) -> None:
    descriptor = tmp_path / "routes.json"
    expected = b"[\r\n  {\r\n    \"id\": \"rehealth-business\"\r\n  }\r\n]\r\n"
    descriptor.write_bytes(expected)

    assert exact_read(descriptor) == expected


def exact_hash(value: bytes) -> str:
    return hashlib.sha256(value).hexdigest()


def write_json(path: Path, payload: dict[str, object]) -> bytes:
    encoded = (json.dumps(payload, sort_keys=True, separators=(",", ":")) + "\n").encode()
    path.write_bytes(encoded)
    return encoded


def fixture(tmp_path: Path) -> tuple[CutoverRequest, datetime]:
    now = datetime(2026, 7, 23, 12, tzinfo=UTC)
    reconciliation = tmp_path / "reconciliation.json"
    reconciliation_bytes = write_json(
        reconciliation,
        {
            "schema_version": "rehealth.hardware-reconciliation.v1",
            "created_at": (now - timedelta(minutes=10)).isoformat().replace("+00:00", "Z"),
            "expires_at": (now + timedelta(hours=1)).isoformat().replace("+00:00", "Z"),
            "git_sha": GIT_SHA,
            "eligible": True,
            "mismatch_count": 0,
            "mismatches": [],
            "source": {
                "dsn_fingerprint": SOURCE_FP,
                "schema_version": "mysql-hardware-v1",
            },
            "target": {
                "dsn_fingerprint": TARGET_FP,
                "schema_version": "timescale-hardware-v3",
            },
        },
    )
    signature = tmp_path / "reconciliation.sig"
    signature.write_bytes(SIGNATURE)
    public_key = tmp_path / "cutover.pub"
    public_key.write_bytes(PUBLIC_KEY)
    approval = tmp_path / "approval.json"
    write_json(
        approval,
        {
            "schemaVersion": "rehealth.telemetry-cutover-approval.v1",
            "approvalStatus": "approved",
            "artifactState": "clean",
            "ciSigner": "todo10-ci",
            "expectedGitSha": GIT_SHA,
            "reconciliationSha256": exact_hash(reconciliation_bytes),
            "signatureSha256": exact_hash(SIGNATURE),
            "publicKeySha256": exact_hash(PUBLIC_KEY),
            "source": {
                "dsnFingerprint": SOURCE_FP,
                "schemaVersion": "mysql-hardware-v1",
            },
            "target": {
                "dsnFingerprint": TARGET_FP,
                "schemaVersion": "timescale-hardware-v3",
            },
        },
    )
    routes = tmp_path / "routes.json"
    write_json(
        routes,
        [
            {
                "id": "rehealth-business",
                "order": -90,
                "predicates": [
                    {
                        "name": "Path",
                        "args": {"_genkey_0": "/jeecg-boot/rehealth/**"},
                    }
                ],
                "filters": [{"name": "StripPrefix", "args": {"parts": "1"}}],
                "uri": "lb://jeecg-system",
            }
        ],
    )
    request = CutoverRequest(
        approval=approval,
        reconciliation=reconciliation,
        signature=signature,
        public_key=public_key,
        routes=routes,
        audit=tmp_path / "audit.jsonl",
        action="apply",
        cases=(),
    )
    return request, now


def test_cutover_applies_exact_route_and_audits_signed_artifact(tmp_path: Path) -> None:
    request, now = fixture(tmp_path)

    result = execute_cutover(request, ExactVerifier(), now)

    assert result["status"] == "applied"
    routes = json.loads(request.routes.read_text(encoding="utf-8"))
    telemetry = next(route for route in routes if route["id"] == "rehealth-device-telemetry")
    assert telemetry["order"] < next(
        route["order"] for route in routes if route["id"] == "rehealth-business"
    )
    assert set(telemetry["predicates"][0]["args"].values()) == set(TELEMETRY_PATHS)
    removed = {
        item["args"]["name"]
        for item in telemetry["filters"]
        if item["name"] == "RemoveRequestHeader"
    }
    assert removed == {"X-ReHealth-User-Id", "X-ReHealth-Tenant-Id"}
    audit = json.loads(request.audit.read_text(encoding="utf-8"))
    assert audit["reconciliationSha256"] == result["reconciliation_sha256"]
    assert audit["ciSigner"] == "todo10-ci"
    assert audit["gitSha"] == GIT_SHA
    assert audit["authority"] == "timescale"


@pytest.mark.parametrize(
    "case",
    [
        "bad_signature",
        "expired_report",
        "dirty_reconciliation",
        "stale_git_sha",
        "dsn_mismatch",
        "route_collision",
    ],
)
def test_invalid_cutover_case_leaves_route_and_audit_unchanged(
    tmp_path: Path,
    case: str,
) -> None:
    request, now = fixture(tmp_path)
    request = CutoverRequest(
        approval=request.approval,
        reconciliation=request.reconciliation,
        signature=request.signature,
        public_key=request.public_key,
        routes=request.routes,
        audit=request.audit,
        action="apply",
        cases=(case,),
    )
    route_before = request.routes.read_bytes()

    result = execute_cutover(request, ExactVerifier(), now)

    assert result["passed"] is True
    assert result["cases"][0]["denied"] is True
    assert result["cases"][0]["route_unchanged"] is True
    assert request.routes.read_bytes() == route_before
    assert not request.audit.exists()


def test_pre_authority_rollback_keeps_mysql_route(tmp_path: Path) -> None:
    request, now = fixture(tmp_path)
    request = CutoverRequest(
        approval=request.approval,
        reconciliation=request.reconciliation,
        signature=request.signature,
        public_key=request.public_key,
        routes=request.routes,
        audit=request.audit,
        action="rollback",
        cases=(),
    )

    result = execute_cutover(request, ExactVerifier(), now)

    assert result["status"] == "mysql-retained"
    assert "rehealth-device-telemetry" not in request.routes.read_text(encoding="utf-8")


def test_post_authority_application_rollback_keeps_timescale(tmp_path: Path) -> None:
    request, now = fixture(tmp_path)
    execute_cutover(request, ExactVerifier(), now)
    request = CutoverRequest(
        approval=request.approval,
        reconciliation=request.reconciliation,
        signature=request.signature,
        public_key=request.public_key,
        routes=request.routes,
        audit=request.audit,
        action="rollback",
        cases=(),
    )

    result = execute_cutover(request, ExactVerifier(), now)

    assert result["status"] == "timescale-retained"
    assert "rehealth-device-telemetry" in request.routes.read_text(encoding="utf-8")
