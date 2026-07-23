from __future__ import annotations

import hashlib
from datetime import UTC, datetime
from pathlib import Path
from typing import Mapping
from urllib.parse import parse_qsl, urlencode, urlsplit, urlunsplit

from .canonical import canonical_json_bytes
from .models import Value


SENSITIVE_QUERY_KEYS = frozenset(
    {"password", "passwd", "pwd", "token", "secret", "sslkey", "sslpassword"}
)


def dsn_fingerprint(dsn: str) -> str:
    parsed = urlsplit(dsn)
    host = parsed.hostname or ""
    port = parsed.port
    netloc = host if port is None else f"{host}:{port}"
    safe_query = urlencode(
        sorted(
            (key, value)
            for key, value in parse_qsl(parsed.query, keep_blank_values=True)
            if key.lower() not in SENSITIVE_QUERY_KEYS
        )
    )
    normalized = urlunsplit((parsed.scheme.lower(), netloc.lower(), parsed.path, safe_query, ""))
    return f"sha256:{hashlib.sha256(normalized.encode('utf-8')).hexdigest()}"


def validate_reconciliation(payload: Mapping[str, Value]) -> tuple[str, ...]:
    errors: list[str] = []
    if payload.get("schema_version") != "rehealth.hardware-reconciliation.v1":
        errors.append("RECONCILIATION_SCHEMA_UNSUPPORTED")
    mismatch_count = payload.get("mismatch_count")
    if payload.get("eligible") is not True:
        errors.append("RECONCILIATION_NOT_ELIGIBLE")
    if mismatch_count != 0:
        errors.append("RECONCILIATION_MISMATCH")
    expires_at = payload.get("expires_at")
    if isinstance(expires_at, str):
        expiry = datetime.fromisoformat(expires_at.replace("Z", "+00:00"))
        if expiry <= datetime.now(UTC):
            errors.append("RECONCILIATION_EXPIRED")
    else:
        errors.append("RECONCILIATION_EXPIRY_MISSING")
    return tuple(errors)


def write_exact_report(path: Path, payload: Mapping[str, Value]) -> str:
    report_path = Path(path)
    report_path.parent.mkdir(parents=True, exist_ok=True)
    encoded = canonical_json_bytes(dict(payload))
    temporary = report_path.with_suffix(report_path.suffix + ".tmp")
    temporary.write_bytes(encoded)
    temporary.replace(report_path)
    return f"sha256:{hashlib.sha256(encoded).hexdigest()}"
