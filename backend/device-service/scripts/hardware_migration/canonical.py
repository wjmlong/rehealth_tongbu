from __future__ import annotations

import hashlib
import json
from datetime import UTC, date, datetime
from decimal import Decimal
from typing import TypeAlias, assert_never
from uuid import UUID

from .models import Value


JsonScalar: TypeAlias = str | int | float | bool | None
JsonValue: TypeAlias = JsonScalar | list["JsonValue"] | dict[str, "JsonValue"]


def json_value(value: Value) -> JsonValue:
    match value:
        case None | str() | int() | bool():
            return value
        case float():
            return format(Decimal(str(value)), "f")
        case Decimal():
            return format(value, "f")
        case UUID():
            return str(value)
        case datetime():
            normalized = value.replace(tzinfo=UTC) if value.tzinfo is None else value.astimezone(UTC)
            return normalized.isoformat(timespec="milliseconds").replace("+00:00", "Z")
        case date():
            return value.isoformat()
        case list():
            return [json_value(item) for item in value]
        case dict():
            return {key: json_value(item) for key, item in value.items()}
        case unreachable:
            assert_never(unreachable)


def canonical_json_bytes(value: Value) -> bytes:
    encoded = json.dumps(
        json_value(value),
        ensure_ascii=False,
        separators=(",", ":"),
        sort_keys=True,
    )
    return (encoded + "\n").encode("utf-8")


def canonical_hash(rows: list[dict[str, Value]]) -> str:
    ordered = sorted(rows, key=lambda row: canonical_json_bytes(row))
    digest = hashlib.sha256()
    for row in ordered:
        digest.update(canonical_json_bytes(row))
    return f"sha256:{digest.hexdigest()}"
