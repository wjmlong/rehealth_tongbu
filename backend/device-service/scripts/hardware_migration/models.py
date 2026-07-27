from __future__ import annotations

from contextlib import AbstractContextManager
from dataclasses import dataclass
from datetime import datetime
from decimal import Decimal
from typing import Callable, Mapping, Protocol, Sequence, TypeAlias
from uuid import UUID


Scalar: TypeAlias = str | int | float | bool | Decimal | datetime | UUID | None
Value: TypeAlias = Scalar | list["Value"] | dict[str, "Value"]
Row: TypeAlias = Mapping[str, Value]
MappedRow: TypeAlias = dict[str, Value]
Mapper: TypeAlias = Callable[[Row, str], MappedRow]


class DbCursor(Protocol):
    def execute(self, statement: str, parameters: Sequence[Value] = ()) -> None: ...
    def fetchone(self) -> tuple[Value, ...] | None: ...
    def fetchall(self) -> list[Row] | list[tuple[Value, ...]]: ...
    def __enter__(self) -> "DbCursor": ...
    def __exit__(self, *error: Value) -> None: ...


class DbConnection(Protocol):
    def cursor(
        self,
        *,
        dictionary: bool = False,
        row_factory: Callable[..., Row] | None = None,
    ) -> DbCursor: ...
    def commit(self) -> None: ...
    def close(self) -> None: ...
    def transaction(self) -> AbstractContextManager[None]: ...


@dataclass(frozen=True, slots=True)
class TableSpec:
    source: str
    target: str
    time_column: str
    target_time_column: str
    columns: tuple[str, ...]
    conflict_columns: tuple[str, ...]
    mapper: Mapper


@dataclass(frozen=True, slots=True)
class MigrationConfig:
    source_dsn: str
    target_dsn: str
    tenant_id: str
    page_size: int
    source_schema_version: str
    target_schema_version: str


@dataclass(frozen=True, slots=True)
class PagePosition:
    time_value: str
    row_id: str


@dataclass(frozen=True, slots=True)
class MigrationResult:
    pages: int
    rows: int
    complete: bool


class MigrationInterruptedError(Exception):
    __slots__ = ("pages",)

    def __init__(self, pages: int) -> None:
        self.pages = pages

    def __str__(self) -> str:
        return f"migration intentionally interrupted after {self.pages} committed pages"


class ConfigurationError(Exception):
    __slots__ = ("message",)

    def __init__(self, message: str) -> None:
        self.message = message

    def __str__(self) -> str:
        return self.message


class MappingError(Exception):
    __slots__ = ("message",)

    def __init__(self, message: str) -> None:
        self.message = message

    def __str__(self) -> str:
        return self.message


class DriftInjectionError(Exception):
    __slots__ = ()

    def __str__(self) -> str:
        return "target drift injection requires at least one migrated row"
