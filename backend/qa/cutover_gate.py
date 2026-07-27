from __future__ import annotations

import json
import os
from datetime import UTC, datetime, timedelta
from pathlib import Path

from cutover_routes import TELEMETRY_PATHS, TELEMETRY_ROUTE_ID, device_routes, parse_routes
from cutover_types import CutoverRequest, GateError, SignatureVerifier
from cutover_verification import (
    CosignVerifier,
    exact_read,
    json_object,
    mapping,
    sha256,
    text,
    timestamp,
    validate_artifact,
)

__all__ = [
    "CosignVerifier",
    "CutoverRequest",
    "GateError",
    "execute_cutover",
]


def _atomic_write(path: Path, content: bytes) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_name(f".{path.name}.{os.getpid()}.tmp")
    try:
        temporary.write_bytes(content)
        os.replace(temporary, path)
    finally:
        temporary.unlink(missing_ok=True)


def _failure_cases(
    request: CutoverRequest,
    verifier: SignatureVerifier,
    now: datetime,
) -> dict[str, object]:
    supported = {
        "bad_signature",
        "expired_report",
        "dirty_reconciliation",
        "stale_git_sha",
        "dsn_mismatch",
        "route_collision",
    }
    if set(request.cases) - supported:
        raise GateError("UNKNOWN_CUTOVER_CASE")
    approval_bytes = exact_read(request.approval)
    reconciliation = exact_read(request.reconciliation)
    signature = exact_read(request.signature)
    public_key = exact_read(request.public_key)
    routes_bytes = exact_read(request.routes)
    results: list[dict[str, object]] = []
    for case in request.cases:
        approval = json_object(approval_bytes, "APPROVAL_INVALID")
        candidate_report = reconciliation
        candidate_signature = signature
        candidate_now = now
        candidate_routes = routes_bytes
        match case:
            case "bad_signature":
                candidate_signature += b"x"
            case "expired_report":
                report = json_object(reconciliation, "RECONCILIATION_INVALID")
                candidate_now = timestamp(report.get("expires_at")) + timedelta(seconds=1)
            case "dirty_reconciliation":
                candidate_report += b" "
            case "stale_git_sha":
                approval["expectedGitSha"] = "0" * 40
            case "dsn_mismatch":
                target = mapping(approval.get("target"), "APPROVAL_INVALID")
                target["dsnFingerprint"] = "sha256:" + "0" * 64
            case "route_collision":
                routes = parse_routes(routes_bytes)
                routes.append(
                    {
                        "id": "collision",
                        "order": -200,
                        "predicates": [
                            {"name": "Path", "args": {"_genkey_0": TELEMETRY_PATHS[0]}}
                        ],
                        "uri": "lb://jeecg-system",
                    }
                )
                candidate_routes = json.dumps(routes).encode()
            case unreachable:
                raise GateError(f"UNKNOWN_CUTOVER_CASE:{unreachable}")
        try:
            validate_artifact(
                approval,
                candidate_report,
                candidate_signature,
                public_key,
                verifier,
                candidate_now,
            )
            device_routes(parse_routes(candidate_routes))
        except GateError as error:
            results.append(
                {
                    "case": case,
                    "denied": True,
                    "reason": str(error),
                    "route_unchanged": request.routes.read_bytes() == routes_bytes,
                }
            )
        else:
            raise GateError(f"UNSAFE_CASE_ACCEPTED:{case}")
    return {"passed": True, "cases": results}


def execute_cutover(
    request: CutoverRequest,
    verifier: SignatureVerifier,
    now: datetime,
) -> dict[str, object]:
    if request.cases:
        return _failure_cases(request, verifier, now)
    routes_bytes = exact_read(request.routes)
    routes = parse_routes(routes_bytes)
    has_timescale_authority = any(route.get("id") == TELEMETRY_ROUTE_ID for route in routes)
    if request.action == "rollback":
        status = "timescale-retained" if has_timescale_authority else "mysql-retained"
        return {
            "passed": True,
            "status": status,
            "authority": "timescale" if has_timescale_authority else "mysql",
        }
    if request.action != "apply":
        raise GateError("CUTOVER_ACTION_INVALID")
    approval = json_object(exact_read(request.approval), "APPROVAL_INVALID")
    reconciliation_bytes = exact_read(request.reconciliation)
    signature = exact_read(request.signature)
    public_key = exact_read(request.public_key)
    report = validate_artifact(
        approval,
        reconciliation_bytes,
        signature,
        public_key,
        verifier,
        now,
    )
    updated = device_routes(routes)
    route_content = (json.dumps(updated, indent=2, sort_keys=True) + "\n").encode()
    audit = {
        "schemaVersion": "rehealth.gateway-cutover-audit.v1",
        "recordedAt": now.astimezone(UTC).isoformat().replace("+00:00", "Z"),
        "action": "telemetry-authority-cutover",
        "status": "applied",
        "authority": "timescale",
        "routeOwner": "rehealth-device-service",
        "reconciliationSha256": sha256(reconciliation_bytes),
        "signatureSha256": sha256(signature),
        "publicKeySha256": sha256(public_key),
        "ciSigner": text(approval, "ciSigner", "APPROVAL_INVALID"),
        "gitSha": report.get("git_sha"),
    }
    _atomic_write(request.routes, route_content)
    _atomic_write(request.audit, (json.dumps(audit, indent=2, sort_keys=True) + "\n").encode())
    return {
        "passed": True,
        "status": "applied",
        "authority": "timescale",
        "reconciliation_sha256": audit["reconciliationSha256"],
        "audit": str(request.audit),
    }
