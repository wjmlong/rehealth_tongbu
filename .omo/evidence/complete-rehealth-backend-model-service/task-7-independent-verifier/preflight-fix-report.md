# Todo 7 Preflight Fix Reverification

## recommendation

CONFIRMED

## priorBlocker

The earlier implementation invoked Flyway directly on unsupported plain
PostgreSQL, leaving `public.flyway_schema_history` behind despite the
fail-before-write criterion.

## currentImplementation

`TimescaleMigrationConfiguration.hardwareDatabaseFlyway` now resolves the
actual configured URL/username/password and calls
`TimescalePrerequisiteValidator.validate` before constructing Flyway. The
validator performs read-only PostgreSQL-version and installed-TimescaleDB
queries.

`TimescaleMigrationIT` invokes this actual Device Service configuration for
both the supported clean migration and unsupported-extension case.

## reproducedResults

- Fresh `postgres:17` without TimescaleDB:
  - actual `TimescaleMigrationConfiguration.hardwareDatabaseFlyway` throws
    `IllegalStateException`;
  - `public_table_count = 0`;
  - `to_regclass('public.flyway_schema_history') IS NULL`;
  - `to_regclass('public.hardware_upload_batch') IS NULL`.
- Fresh pinned `timescale/timescaledb:2.21.1-pg17`:
  - migrations V1, V2, and V3 all applied successfully;
  - Flyway validation succeeded;
  - the second migration ran zero migrations;
  - the V1+V2 to V3 upgrade succeeded and validated;
  - live catalog contains 9 hardware tables and 4 hypertables.
- Combined focused Maven execution:
  - tests run: 4;
  - failures: 0;
  - errors: 0;
  - skipped: 2 unselected cases;
  - exit code: 0.

## evidence

- `preflight-fix-maven.log`
- `preflight-fix-maven.exit`
- `preflight-fix-catalog.log`
- `backend/device-service/src/main/java/com/rehealth/device/config/TimescalePrerequisiteValidator.java`
- `backend/device-service/src/main/java/com/rehealth/device/config/TimescaleMigrationConfiguration.java`
- `backend/device-service/src/test/java/com/rehealth/device/TimescaleMigrationIT.java`

## scope

No product code was edited, committed, or pushed by this verifier. Only this
evidence report and the focused runtime logs were created.

