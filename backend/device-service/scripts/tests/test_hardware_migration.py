from __future__ import annotations

import hashlib
import json
import sys
from datetime import UTC, datetime
from pathlib import Path

import pytest


SCRIPTS = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(SCRIPTS))

from hardware_migration.canonical import canonical_hash, canonical_json_bytes
from hardware_migration.mapping import (
    legacy_source_record_id,
    map_upload_batch,
    mysql_datetime_to_utc,
)
from hardware_migration.report import dsn_fingerprint, validate_reconciliation


def test_dsn_fingerprint_excludes_credentials_and_is_stable() -> None:
    first = dsn_fingerprint(
        "mysql://first-user:first-secret@db.example:3306/hardware?charset=utf8mb4"
    )
    second = dsn_fingerprint(
        "mysql://second-user:second-secret@db.example:3306/hardware?charset=utf8mb4"
    )

    assert first == second
    assert "secret" not in first
    assert "user" not in first


def test_mysql_datetime_is_interpreted_as_utc_without_local_timezone_shift() -> None:
    legacy = datetime(2026, 7, 23, 23, 59, 59, 123000)

    observed = mysql_datetime_to_utc(legacy)

    assert observed == datetime(2026, 7, 23, 23, 59, 59, 123000, tzinfo=UTC)


def test_null_client_id_maps_to_deterministic_source_id() -> None:
    first = legacy_source_record_id("hardware_measurement", "row-1", None)
    second = legacy_source_record_id("hardware_measurement", "row-1", None)

    assert first == second
    assert first == "legacy:hardware_measurement:row-1"


def test_upload_batch_maps_status_null_quality_and_tenant() -> None:
    row = {
        "id": "1f4b55bf-acde-4a7c-8bc5-6cc2c6937001",
        "receipt_id": "1f4b55bf-acde-4a7c-8bc5-6cc2c6937002",
        "batch_id": "batch-1",
        "user_id": "user-1",
        "device_id": "device-1",
        "source": None,
        "collected_from": None,
        "collected_to": None,
        "received_at": datetime(2026, 7, 23, 1, 2, 3),
        "committed_at": datetime(2026, 7, 23, 1, 2, 4),
        "status": "COMMITTED",
        "record_count": 0,
        "measurement_count": 0,
        "sleep_session_count": 0,
        "activity_count": 0,
        "signal_chunk_count": 0,
        "quality_json": None,
    }

    mapped = map_upload_batch(row, "tenant-legacy")

    assert mapped["tenant_id"] == "tenant-legacy"
    assert mapped["status"] == "PERSISTED"
    assert mapped["quality_summary"] == {}
    assert mapped["collected_from"] is None


def test_canonical_hash_is_order_independent_but_value_sensitive() -> None:
    rows = [{"id": "b", "value": "2.000"}, {"id": "a", "value": None}]
    reordered = list(reversed(rows))

    assert canonical_hash(rows) == canonical_hash(reordered)
    assert canonical_hash(rows) != canonical_hash(
        [{"id": "b", "value": "2.001"}, {"id": "a", "value": None}]
    )


def test_reconciliation_validation_rejects_pinpointed_mismatch() -> None:
    payload = {
        "schema_version": "rehealth.hardware-reconciliation.v1",
        "source": {
            "dsn_fingerprint": "sha256:source",
            "schema_version": "mysql-hardware-v1",
        },
        "target": {
            "dsn_fingerprint": "sha256:target",
            "schema_version": "timescale-hardware-v3",
        },
        "window": {
            "from_utc": "2026-07-23T00:00:00Z",
            "to_utc": "2026-07-24T00:00:00Z",
        },
        "tables": [],
        "shadow_reads": [],
        "mismatches": [
            {
                "table": "hardware_measurement",
                "scope": {
                    "tenant_id": "tenant-legacy",
                    "user_id": "user-1",
                    "device_id": "device-1",
                    "utc_day": "2026-07-23",
                },
                "source_count": 1,
                "target_count": 0,
                "source_hash": "sha256:source-row",
                "target_hash": "sha256:empty",
                "source_sample": [{"id": "row-1"}],
                "target_sample": [],
            }
        ],
        "mismatch_count": 1,
        "eligible": False,
        "git_sha": "1450d4351e8fa07220ec061ec28d5ef4c7f2876e",
        "created_at": "2026-07-23T00:00:00Z",
        "expires_at": "2026-07-24T00:00:00Z",
    }

    errors = validate_reconciliation(payload)

    assert errors == ("RECONCILIATION_NOT_ELIGIBLE", "RECONCILIATION_MISMATCH")


def test_canonical_json_bytes_are_exact_and_end_with_newline() -> None:
    payload = {"z": 1, "a": {"b": True}}

    encoded = canonical_json_bytes(payload)

    assert encoded == b'{"a":{"b":true},"z":1}\n'
    assert hashlib.sha256(encoded).hexdigest() == (
        "c7cc588b95acc0da0c9b8229178f9d35a58371fdfe04f9075f7fb1ff03013e75"
    )
