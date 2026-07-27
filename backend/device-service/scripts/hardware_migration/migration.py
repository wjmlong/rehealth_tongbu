from __future__ import annotations

from datetime import UTC, datetime

from .canonical import canonical_hash
from .database import (
    checkpoint_row_count,
    mark_checkpoint,
    read_checkpoint,
    source_page,
    upsert_page,
)
from .mapping import TABLES
from .models import (
    DbConnection,
    MigrationConfig,
    MigrationInterruptedError,
    MigrationResult,
    MappingError,
    PagePosition,
    Value,
)


def run_migration(
    source: DbConnection,
    target: DbConnection,
    config: MigrationConfig,
    stop_after_pages: int | None = None,
) -> MigrationResult:
    committed_pages = 0
    migrated_rows = 0
    for spec in TABLES:
        position = read_checkpoint(target, spec.source)
        total_rows = checkpoint_row_count(target, spec.source)
        while True:
            page = source_page(source, spec, position, config.page_size)
            if not page:
                mark_checkpoint(target, spec.source, "VERIFIED", total_rows)
                break
            mapped = [spec.mapper(row, config.tenant_id) for row in page]
            final_source = page[-1]
            final_time = final_source[spec.time_column]
            if not isinstance(final_time, datetime):
                raise MappingError(f"{spec.source}.{spec.time_column} must be datetime")
            utc_time = (
                final_time.replace(tzinfo=UTC)
                if final_time.tzinfo is None
                else final_time.astimezone(UTC)
            )
            position = PagePosition(
                time_value=utc_time.isoformat(timespec="milliseconds").replace("+00:00", "Z"),
                row_id=str(final_source["id"]),
            )
            total_rows += len(mapped)
            upsert_page(target, spec, mapped, position, total_rows)
            committed_pages += 1
            migrated_rows += len(mapped)
            if stop_after_pages is not None and committed_pages >= stop_after_pages:
                raise MigrationInterruptedError(committed_pages)
    return MigrationResult(pages=committed_pages, rows=migrated_rows, complete=True)


def mapped_source_rows(
    source: DbConnection,
    config: MigrationConfig,
) -> dict[str, list[dict[str, Value]]]:
    mapped_tables: dict[str, list[dict[str, Value]]] = {}
    for spec in TABLES:
        position: PagePosition | None = None
        mapped: list[dict[str, Value]] = []
        while True:
            page = source_page(source, spec, position, config.page_size)
            if not page:
                break
            mapped.extend(spec.mapper(row, config.tenant_id) for row in page)
            final = page[-1]
            final_time = final[spec.time_column]
            if not isinstance(final_time, datetime):
                raise MappingError(f"{spec.source}.{spec.time_column} must be datetime")
            utc_time = (
                final_time.replace(tzinfo=UTC)
                if final_time.tzinfo is None
                else final_time.astimezone(UTC)
            )
            position = PagePosition(
                utc_time.isoformat(timespec="milliseconds").replace("+00:00", "Z"),
                str(final["id"]),
            )
        mapped_tables[spec.source] = mapped
    return mapped_tables


def checkpoint_hash(rows: list[dict[str, Value]]) -> str:
    return canonical_hash(rows)
