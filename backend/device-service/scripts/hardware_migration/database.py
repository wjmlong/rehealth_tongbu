from __future__ import annotations

import json
from contextlib import contextmanager
from datetime import UTC, datetime
from pathlib import Path
from typing import Iterator
from urllib.parse import unquote, urlsplit
from uuid import NAMESPACE_URL, uuid5

from .models import (
    DbConnection,
    DriftInjectionError,
    MappedRow,
    PagePosition,
    Row,
    TableSpec,
    Value,
)


def mysql_connect(dsn: str) -> DbConnection:
    import mysql.connector

    parsed = urlsplit(dsn)
    return mysql.connector.connect(
        host=parsed.hostname,
        port=parsed.port or 3306,
        user=unquote(parsed.username or ""),
        password=unquote(parsed.password or ""),
        database=parsed.path.lstrip("/"),
        charset="utf8mb4",
        use_pure=True,
    )


def postgres_connect(dsn: str) -> DbConnection:
    import psycopg

    return psycopg.connect(dsn, autocommit=True)


@contextmanager
def connections(
    source_dsn: str,
    target_dsn: str,
) -> Iterator[tuple[DbConnection, DbConnection]]:
    source = mysql_connect(source_dsn)
    target = postgres_connect(target_dsn)
    try:
        with source.cursor() as cursor:
            cursor.execute("SET time_zone = '+00:00'")
        yield source, target
    finally:
        target.close()
        source.close()


def source_table_exists(source: DbConnection, table: str) -> bool:
    with source.cursor() as cursor:
        cursor.execute(
            """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = DATABASE() AND table_name = %s
            """,
            (table,),
        )
        row = cursor.fetchone()
    return bool(row and row[0] == 1)


def apply_source_fixture(
    source: DbConnection,
    schema_path: Path,
    fixture_path: Path,
) -> None:
    if not source_table_exists(source, "hardware_upload_batch"):
        execute_mysql_script(source, schema_path.read_text(encoding="utf-8"))
    execute_mysql_script(source, fixture_path.read_text(encoding="utf-8"))


def execute_mysql_script(source: DbConnection, sql: str) -> None:
    statements = [statement.strip() for statement in sql.split(";") if statement.strip()]
    with source.cursor() as cursor:
        for statement in statements:
            cursor.execute(statement)
    source.commit()


def read_checkpoint(target: DbConnection, table: str) -> PagePosition | None:
    with target.cursor() as cursor:
        cursor.execute(
            """
            SELECT source_position, status
            FROM hardware_migration_checkpoint
            WHERE source_name = %s AND checkpoint_key = 'mysql-to-timescale-v1'
            """,
            (table,),
        )
        row = cursor.fetchone()
    if row is None or row[1] == "VERIFIED":
        return None
    payload = json.loads(row[0])
    return PagePosition(time_value=str(payload["time"]), row_id=str(payload["id"]))


def checkpoint_row_count(target: DbConnection, table: str) -> int:
    with target.cursor() as cursor:
        cursor.execute(
            """
            SELECT row_count, status
            FROM hardware_migration_checkpoint
            WHERE source_name = %s AND checkpoint_key = 'mysql-to-timescale-v1'
            """,
            (table,),
        )
        row = cursor.fetchone()
    return 0 if row is None or row[1] == "VERIFIED" else int(row[0])


def source_page(
    source: DbConnection,
    spec: TableSpec,
    position: PagePosition | None,
    page_size: int,
) -> list[Row]:
    query = f"SELECT * FROM {spec.source}"
    parameters: tuple[Value, ...]
    if position is None:
        parameters = (page_size,)
    else:
        query += (
            f" WHERE ({spec.time_column} > %s)"
            f" OR ({spec.time_column} = %s AND id > %s)"
        )
        time_value = datetime.fromisoformat(position.time_value.replace("Z", "+00:00"))
        naive_utc = time_value.astimezone(UTC).replace(tzinfo=None)
        parameters = (naive_utc, naive_utc, position.row_id, page_size)
    query += f" ORDER BY {spec.time_column}, id LIMIT %s"
    with source.cursor(dictionary=True) as cursor:
        cursor.execute(query, parameters)
        return list(cursor.fetchall())


def upsert_page(
    target: DbConnection,
    spec: TableSpec,
    rows: list[MappedRow],
    position: PagePosition,
    total_rows: int,
) -> None:
    from psycopg.types.json import Jsonb

    columns = ", ".join(spec.columns)
    placeholders = ", ".join(["%s"] * len(spec.columns))
    conflict = ", ".join(spec.conflict_columns)
    updates = ", ".join(
        f"{column} = EXCLUDED.{column}"
        for column in spec.columns
        if column not in spec.conflict_columns
    )
    statement = (
        f"INSERT INTO {spec.target} ({columns}) VALUES ({placeholders}) "
        f"ON CONFLICT ({conflict}) DO UPDATE SET {updates}"
    )
    checkpoint_id = uuid5(NAMESPACE_URL, f"rehealth:migration:{spec.source}")
    position_json = json.dumps(
        {"id": position.row_id, "time": position.time_value},
        separators=(",", ":"),
        sort_keys=True,
    )
    with target.transaction():
        with target.cursor() as cursor:
            for row in rows:
                values = tuple(
                    Jsonb(row[column]) if column == "quality_summary" else row[column]
                    for column in spec.columns
                )
                cursor.execute(statement, values)
            cursor.execute(
                """
                INSERT INTO hardware_migration_checkpoint (
                    id, source_name, checkpoint_key, source_position,
                    row_count, status, checked_at
                ) VALUES (%s, %s, 'mysql-to-timescale-v1', %s, %s, 'PENDING', now())
                ON CONFLICT (source_name, checkpoint_key) DO UPDATE SET
                    source_position = EXCLUDED.source_position,
                    row_count = EXCLUDED.row_count,
                    status = 'PENDING',
                    checked_at = now()
                """,
                (checkpoint_id, spec.source, position_json, total_rows),
            )


def mark_checkpoint(
    target: DbConnection,
    table: str,
    status: str,
    row_count: int,
) -> None:
    checkpoint_id = uuid5(NAMESPACE_URL, f"rehealth:migration:{table}")
    with target.cursor() as cursor:
        cursor.execute(
            """
            INSERT INTO hardware_migration_checkpoint (
                id, source_name, checkpoint_key, source_position,
                row_count, status, checked_at
            ) VALUES (
                %s, %s, 'mysql-to-timescale-v1',
                '{"id":"","time":"1970-01-01T00:00:00.000Z"}',
                %s, %s, now()
            )
            ON CONFLICT (source_name, checkpoint_key) DO UPDATE SET
                row_count = EXCLUDED.row_count,
                status = EXCLUDED.status,
                checked_at = now()
            """,
            (checkpoint_id, table, row_count, status),
        )


def target_rows(
    target: DbConnection,
    spec: TableSpec,
    tenant_id: str,
    window_from: datetime | None,
    window_to: datetime | None,
) -> list[MappedRow]:
    from psycopg.rows import dict_row

    columns = ", ".join(spec.columns)
    where = "WHERE tenant_id = %s"
    parameters: list[Value] = [tenant_id]
    if window_from is not None and window_to is not None:
        where += f" AND {spec.target_time_column} >= %s AND {spec.target_time_column} <= %s"
        parameters.extend((window_from, window_to))
    with target.cursor(row_factory=dict_row) as cursor:
        cursor.execute(
            f"SELECT {columns} FROM {spec.target} {where} "
            f"ORDER BY {spec.target_time_column}, id",
            tuple(parameters),
        )
        return [dict(row) for row in cursor.fetchall()]


def reconcile_checkpoint(
    target: DbConnection,
    table: str,
    source_hash: str,
    target_hash: str,
    matched: bool,
) -> None:
    with target.cursor() as cursor:
        cursor.execute(
            """
            UPDATE hardware_migration_checkpoint
            SET source_hash = %s,
                target_hash = %s,
                status = %s,
                checked_at = now()
            WHERE source_name = %s AND checkpoint_key = 'mysql-to-timescale-v1'
            """,
            (source_hash, target_hash, "VERIFIED" if matched else "DRIFTED", table),
        )


def delete_one_target_row(target: DbConnection) -> tuple[str, str]:
    for table, time_column in (
        ("hardware_measurement", "observed_at"),
        ("hardware_sleep_session", "started_at"),
        ("hardware_activity", "started_at"),
        ("hardware_signal_chunk_metadata", "started_at"),
        ("hardware_data_quality_event", "event_at"),
        ("hardware_upload_batch", "received_at"),
    ):
        with target.cursor() as cursor:
            cursor.execute(f"SELECT id::text FROM {table} ORDER BY {time_column}, id LIMIT 1")
            row = cursor.fetchone()
            if row is not None:
                cursor.execute(f"DELETE FROM {table} WHERE id = %s", (row[0],))
                return table, str(row[0])
    raise DriftInjectionError
