#!/usr/bin/env -S uv run --script
# /// script
# requires-python = ">=3.11"
# dependencies = [
#   "mysql-connector-python==9.4.0",
#   "psycopg[binary]==3.2.10",
#   "typer==0.16.1",
# ]
# ///

# ─── How to run ───
# 1. Export REHEALTH_HARDWARE_MYSQL_DSN and REHEALTH_TIMESCALE_DSN.
# 2. Run with uv:
#      uv run backend/device-service/scripts/migrate_hardware.py reconcile --output reconciliation.json
# 3. Or install the declared dependencies and invoke with Python:
#      python backend/device-service/scripts/migrate_hardware.py reconcile --output reconciliation.json
# ──────────────────

from __future__ import annotations

import os
from pathlib import Path

import typer

from hardware_migration.database import (
    apply_source_fixture,
    connections,
    delete_one_target_row,
)
from hardware_migration.migration import run_migration
from hardware_migration.models import (
    ConfigurationError,
    DbConnection,
    MigrationConfig,
    MigrationInterruptedError,
)
from hardware_migration.reconciliation import build_reconciliation
from hardware_migration.report import write_exact_report
from hardware_migration.signing import SigningError, sign_report, verify_report


app = typer.Typer(no_args_is_help=True, add_completion=False)
REPOSITORY_ROOT = Path(__file__).resolve().parents[3]
LEGACY_SCHEMA = (
    REPOSITORY_ROOT
    / "backend"
    / "jeecg-boot"
    / "jeecg-boot-module"
    / "jeecg-module-rehealth"
    / "src"
    / "main"
    / "resources"
    / "db"
    / "hardware"
    / "mysql"
    / "V1__create_hardware_telemetry_tables.sql"
)


def migration_config(
    source_dsn: str | None,
    target_dsn: str | None,
    tenant_id: str,
    page_size: int,
) -> MigrationConfig:
    source = source_dsn or os.environ.get("REHEALTH_HARDWARE_MYSQL_DSN")
    target = target_dsn or os.environ.get("REHEALTH_TIMESCALE_DSN")
    if not source or not target:
        raise ConfigurationError(
            "REHEALTH_HARDWARE_MYSQL_DSN and REHEALTH_TIMESCALE_DSN are required"
        )
    if page_size < 1 or page_size > 10_000:
        raise ConfigurationError("page size must be between 1 and 10000")
    if not tenant_id.strip():
        raise ConfigurationError("tenant id is required")
    return MigrationConfig(
        source_dsn=source,
        target_dsn=target,
        tenant_id=tenant_id,
        page_size=page_size,
        source_schema_version=os.environ.get(
            "REHEALTH_HARDWARE_MYSQL_SCHEMA_VERSION", "mysql-hardware-v1"
        ),
        target_schema_version=os.environ.get(
            "REHEALTH_TIMESCALE_SCHEMA_VERSION", "timescale-hardware-v3"
        ),
    )


def prepare_fixture(source: DbConnection, fixture: Path | None) -> None:
    if fixture is not None:
        apply_source_fixture(source, LEGACY_SCHEMA, fixture.resolve())


@app.command()
def migrate(
    source_dsn: str | None = typer.Option(None, envvar="REHEALTH_HARDWARE_MYSQL_DSN"),
    target_dsn: str | None = typer.Option(None, envvar="REHEALTH_TIMESCALE_DSN"),
    tenant_id: str = typer.Option("legacy", envvar="REHEALTH_MIGRATION_TENANT_ID"),
    page_size: int = typer.Option(1000),
    source_fixture: Path | None = typer.Option(None),
    stop_after_pages: int | None = typer.Option(None, hidden=True),
) -> None:
    config = migration_config(source_dsn, target_dsn, tenant_id, page_size)
    with connections(config.source_dsn, config.target_dsn) as (source, target):
        prepare_fixture(source, source_fixture)
        result = run_migration(source, target, config, stop_after_pages)
    typer.echo(
        f"migration complete: pages={result.pages} rows={result.rows} complete={result.complete}"
    )


@app.command()
def reconcile(
    output: Path = typer.Option(...),
    source_dsn: str | None = typer.Option(None, envvar="REHEALTH_HARDWARE_MYSQL_DSN"),
    target_dsn: str | None = typer.Option(None, envvar="REHEALTH_TIMESCALE_DSN"),
    tenant_id: str = typer.Option("legacy", envvar="REHEALTH_MIGRATION_TENANT_ID"),
    page_size: int = typer.Option(1000),
    source_fixture: Path | None = typer.Option(None),
    inject_target_drift: str | None = typer.Option(None),
    expect_blocked: bool = typer.Option(False),
) -> None:
    config = migration_config(source_dsn, target_dsn, tenant_id, page_size)
    with connections(config.source_dsn, config.target_dsn) as (source, target):
        prepare_fixture(source, source_fixture)
        run_migration(source, target, config)
        if inject_target_drift is not None:
            if inject_target_drift != "drop_one_row":
                raise ConfigurationError("only drop_one_row drift injection is supported")
            if source_fixture is None:
                raise ConfigurationError("target drift injection requires --source-fixture")
            deleted_table, deleted_id = delete_one_target_row(target)
            typer.echo(f"injected target drift: table={deleted_table} id={deleted_id}")
        payload = build_reconciliation(source, target, config, REPOSITORY_ROOT)
    digest = write_exact_report(output.resolve(), payload)
    eligible = payload["eligible"] is True
    typer.echo(f"reconciliation: eligible={eligible} sha256={digest}")
    if expect_blocked and eligible:
        raise typer.Exit(2)
    if not expect_blocked and not eligible:
        raise typer.Exit(2)


@app.command()
def sign(
    reconciliation: Path = typer.Option(...),
    signature: Path = typer.Option(...),
    key_env: str = typer.Option("REHEALTH_CUTOVER_SIGNING_KEY"),
) -> None:
    sign_report(reconciliation.resolve(), signature.resolve(), key_env)
    typer.echo(f"signed exact bytes: {reconciliation.resolve()}")


@app.command()
def verify(
    reconciliation: Path = typer.Option(...),
    signature: Path = typer.Option(...),
    key_env: str = typer.Option("REHEALTH_CUTOVER_VERIFY_KEY"),
) -> None:
    verify_report(reconciliation.resolve(), signature.resolve(), key_env)
    typer.echo(f"verified exact bytes: {reconciliation.resolve()}")


def main() -> None:
    try:
        app()
    except MigrationInterruptedError as error:
        typer.echo(str(error), err=True)
        raise SystemExit(75) from None
    except (ConfigurationError, SigningError) as error:
        typer.echo(f"ERROR: {error}", err=True)
        raise SystemExit(64) from None


if __name__ == "__main__":
    main()
