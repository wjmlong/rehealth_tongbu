from __future__ import annotations

import subprocess
from collections import defaultdict
from datetime import UTC, datetime, timedelta
from pathlib import Path

from .canonical import canonical_hash, json_value
from .database import reconcile_checkpoint, target_rows
from .mapping import TABLES
from .migration import mapped_source_rows
from .models import DbConnection, MappingError, MigrationConfig, Value
from .report import dsn_fingerprint


ScopeKey = tuple[str, str, str, str]


def _row_time(row: dict[str, Value], column: str) -> datetime:
    value = row[column]
    if not isinstance(value, datetime):
        raise MappingError(f"{column} must be datetime")
    return value.replace(tzinfo=UTC) if value.tzinfo is None else value.astimezone(UTC)


def _scope(row: dict[str, Value], time_column: str) -> ScopeKey:
    return (
        str(row["tenant_id"]),
        str(row["user_id"]),
        str(row["device_id"]),
        _row_time(row, time_column).date().isoformat(),
    )


def _group(
    rows: list[dict[str, Value]],
    time_column: str,
) -> dict[ScopeKey, list[dict[str, Value]]]:
    groups: dict[ScopeKey, list[dict[str, Value]]] = defaultdict(list)
    for row in rows:
        groups[_scope(row, time_column)].append(row)
    return groups


def _sample(rows: list[dict[str, Value]]) -> list[dict[str, Value]]:
    ordered = sorted(rows, key=lambda row: str(row.get("id")))
    return [json_value(row) for row in ordered[:3]]


def _scope_payload(
    key: ScopeKey,
    source_rows: list[dict[str, Value]],
    target_scope_rows: list[dict[str, Value]],
) -> dict[str, Value]:
    source_hash = canonical_hash(source_rows)
    target_hash = canonical_hash(target_scope_rows)
    return {
        "scope": {
            "tenant_id": key[0],
            "user_id": key[1],
            "device_id": key[2],
            "utc_day": key[3],
        },
        "source_count": len(source_rows),
        "target_count": len(target_scope_rows),
        "source_hash": source_hash,
        "target_hash": target_hash,
        "source_sample": _sample(source_rows),
        "target_sample": _sample(target_scope_rows),
        "matched": len(source_rows) == len(target_scope_rows) and source_hash == target_hash,
    }


def _shadow_reads(
    source_tables: dict[str, list[dict[str, Value]]],
    target_tables: dict[str, list[dict[str, Value]]],
) -> list[dict[str, Value]]:
    reports: list[dict[str, Value]] = []
    public_tables = {
        "hardware_measurement": "observed_at",
        "hardware_sleep_session": "started_at",
        "hardware_activity": "started_at",
    }
    owners = {
        (str(row["tenant_id"]), str(row["user_id"]), str(row["device_id"]))
        for table in public_tables
        for row in source_tables[table]
    }
    for owner in sorted(owners):
        for table, time_column in public_tables.items():
            source = [
                row
                for row in source_tables[table]
                if (str(row["tenant_id"]), str(row["user_id"]), str(row["device_id"])) == owner
            ]
            target = [
                row
                for row in target_tables[table]
                if (str(row["tenant_id"]), str(row["user_id"]), str(row["device_id"])) == owner
            ]
            source = sorted(source, key=lambda row: _row_time(row, time_column), reverse=True)[:50]
            target = sorted(target, key=lambda row: _row_time(row, time_column), reverse=True)[:50]
            source_hash = canonical_hash(source)
            target_hash = canonical_hash(target)
            reports.append(
                {
                    "table": table,
                    "tenant_id": owner[0],
                    "user_id": owner[1],
                    "device_id": owner[2],
                    "limit": 50,
                    "source_count": len(source),
                    "target_count": len(target),
                    "source_hash": source_hash,
                    "target_hash": target_hash,
                    "matched": len(source) == len(target) and source_hash == target_hash,
                }
            )
    return reports


def _git_sha(repository_root: Path) -> str:
    completed = subprocess.run(
        ["git", "rev-parse", "HEAD"],
        cwd=repository_root,
        check=True,
        capture_output=True,
        text=True,
    )
    return completed.stdout.strip()


def build_reconciliation(
    source: DbConnection,
    target: DbConnection,
    config: MigrationConfig,
    repository_root: Path,
    created_at: datetime | None = None,
) -> dict[str, Value]:
    source_tables = mapped_source_rows(source, config)
    all_times = [
        _row_time(row, spec.target_time_column)
        for spec in TABLES
        for row in source_tables[spec.source]
    ]
    window_from = min(all_times) if all_times else None
    window_to = max(all_times) if all_times else None
    target_tables = {
        spec.target: target_rows(
            target, spec, config.tenant_id, window_from, window_to
        )
        for spec in TABLES
    }
    table_reports: list[dict[str, Value]] = []
    mismatches: list[dict[str, Value]] = []
    for spec in TABLES:
        source_rows = source_tables[spec.source]
        migrated_rows = target_tables[spec.target]
        source_groups = _group(source_rows, spec.target_time_column)
        target_groups = _group(migrated_rows, spec.target_time_column)
        scopes = []
        for key in sorted(set(source_groups) | set(target_groups)):
            scope = _scope_payload(key, source_groups.get(key, []), target_groups.get(key, []))
            scopes.append(scope)
            if scope["matched"] is not True:
                mismatches.append({"table": spec.target, **scope})
        source_hash = canonical_hash(source_rows)
        target_hash = canonical_hash(migrated_rows)
        matched = len(source_rows) == len(migrated_rows) and source_hash == target_hash
        reconcile_checkpoint(target, spec.source, source_hash, target_hash, matched)
        table_reports.append(
            {
                "table": spec.target,
                "source_count": len(source_rows),
                "target_count": len(migrated_rows),
                "source_hash": source_hash,
                "target_hash": target_hash,
                "matched": matched,
                "scopes": scopes,
            }
        )
    shadows = _shadow_reads(source_tables, target_tables)
    generated = (created_at or datetime.now(UTC)).astimezone(UTC).replace(microsecond=0)
    return {
        "schema_version": "rehealth.hardware-reconciliation.v1",
        "source": {
            "dsn_fingerprint": dsn_fingerprint(config.source_dsn),
            "schema_version": config.source_schema_version,
        },
        "target": {
            "dsn_fingerprint": dsn_fingerprint(config.target_dsn),
            "schema_version": config.target_schema_version,
        },
        "window": {
            "from_utc": _format_time(window_from),
            "to_utc": _format_time(window_to),
        },
        "tables": table_reports,
        "shadow_reads": shadows,
        "mismatches": mismatches,
        "mismatch_count": len(mismatches),
        "eligible": not mismatches and all(report["matched"] is True for report in shadows),
        "git_sha": _git_sha(repository_root),
        "created_at": _format_time(generated),
        "expires_at": _format_time(generated + timedelta(hours=24)),
    }


def _format_time(value: datetime | None) -> str | None:
    if value is None:
        return None
    return value.astimezone(UTC).isoformat(timespec="seconds").replace("+00:00", "Z")
