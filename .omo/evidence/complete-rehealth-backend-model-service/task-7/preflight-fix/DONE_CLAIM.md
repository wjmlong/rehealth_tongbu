# Todo 7 pre-Flyway prerequisite fix

## Outcome

`TimescaleMigrationConfiguration` now resolves and validates database
credentials, runs `TimescalePrerequisiteValidator`, and only then constructs
the Flyway instance. The validator performs read-only checks for PostgreSQL 17+
and an installed TimescaleDB 2.18+ extension. Migration V1 remains unchanged as
defense in depth.

## Success-criterion evidence

### Supported TimescaleDB migrates

- Scenario: clean and upgraded migrations on pinned
  `timescale/timescaledb:2.21.1-pg17`.
- Invocation:
  `mvn -f backend/device-service/pom.xml -Dtest=TimescaleMigrationIT test`
  (executed with Maven 3.9.11 / Temurin 17 in a container against the live
  TimescaleDB container).
- Binary observable: exit `0`; 4 tests, 0 failures, 0 errors, 3 skipped;
  `BUILD SUCCESS`.
- Artifacts: `t7-happy.exit`, `t7-happy.log`,
  `supported-timescale-migrated.log`.
- Direct database observable: Flyway version `3`, `success=true`, and four
  hardware hypertables.

### Unsupported extension fails before every schema write

- Scenario: the actual `TimescaleMigrationConfiguration.hardwareDatabaseFlyway`
  factory is invoked against plain `postgres:17`.
- Invocation:
  `mvn -f backend/device-service/pom.xml -Dtest=TimescaleMigrationIT
  -Dcases=duplicate_source,unsupported_extension,timezone_roundtrip test`
  (executed with Maven 3.9.11 / Temurin 17 in a container against live
  PostgreSQL and TimescaleDB containers).
- Binary observable: exit `0`; 4 tests, 0 failures, 0 errors, 1 skipped;
  `BUILD SUCCESS`.
- Artifacts: `t7-failure.exit`, `t7-failure.log`,
  `plain-postgres-zero-writes.log`.
- Direct database observable for every plain-PostgreSQL test database:
  `public_table_count=0`, `flyway_history=ABSENT`, and
  `hardware_upload_batch=ABSENT`.

### Source hygiene and cleanup

- Scenario: scoped whitespace validation and worktree inspection.
- Invocation: `git diff --check -- <T7 preflight source/test paths>` and
  `git status --short -- <T7 paths>`.
- Binary observable: `git diff --check` exit `0`; scoped status contains only
  the intended shared-worktree changes and evidence.
- Artifact: `git-status-scoped.log`.
- Cleanup observable: all containers named `rehealth-t7-preflight-*` were
  removed; the matching `docker ps -a` query returned no rows.
