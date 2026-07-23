# Todo 7 Independent Gate Review

## recommendation

REJECT

## originalIntent

Deliver an additive PostgreSQL 17 + TimescaleDB hardware schema whose Flyway
migrations are safe on clean and V1+V2-upgraded databases, create exactly four
UTC hypertables with the specified lifecycle policies, enforce scoped
idempotency keys, retain only approved normalized metadata, and fail an
unsupported database prerequisite before writing anything.

## desiredOutcome

The Device Service can start against the pinned TimescaleDB 2.21.1/PostgreSQL
17 runtime, migrate and validate deterministically, serve scoped recent-query
indexes, reject duplicate source events, preserve unresolved Outbox work, and
leave an unsupported target untouched.

## userOutcomeReview

Most schema behavior is reproduced successfully on the exact pinned
TimescaleDB image. Clean migration, validation, a second zero-migration run,
V1+V2 to V3 upgrade, hypertables, chunk intervals, compression, retention,
ordinary cleanup, duplicate rejection, UTC legacy interpretation, scoped
indexes, and raw-payload absence were confirmed.

The unsupported plain PostgreSQL case does not satisfy the explicit
fail-before-write outcome. Flyway creates `public.flyway_schema_history` before
V1 fails on `CREATE EXTENSION timescaledb`. The product tables are absent, but
the target database is not untouched.

## blockers

1. `violatedCriterion`: **T7 failure / unsupported prerequisite fails before writes**
   - Observation: after the expected Flyway failure on plain `postgres:17`,
     `public.flyway_schema_history` remains present.
   - `evidencePointer`:
     `.omo/evidence/complete-rehealth-backend-model-service/task-7-independent-verifier/direct-db-audit.log`
     under `unsupported plain postgres post-failure public tables`.
   - Required correction: perform a read-only prerequisite probe before Flyway
     initializes its schema-history table, or explicitly revise the criterion
     to permit Flyway bookkeeping while forbidding domain-schema writes.

## reproducedEvidence

- Exact happy gate in an external Maven 3.9.11/JDK 17 container against
  `timescale/timescaledb:2.21.1-pg17`:
  `t7-happy.exit` = 0 and `t7-happy.log` records 4 tests, 0 failures, 3
  intentionally skipped cases.
- Exact failure gate against the same Timescale container plus real
  `postgres:17`: `t7-failure.exit` = 0 and `t7-failure.log` records 4 tests,
  0 failures, 1 skipped happy case.
- Direct catalog probe confirmed:
  - `hardware_measurement` uses 1-day chunks.
  - sleep, activity, and quality use 7-day chunks.
  - all four compression policies use 7 days.
  - normalized telemetry/session retention is 730 days; quality is 1095 days.
  - ordinary job config is signal metadata 90 days, operational rows 1095
    days, and published Outbox 30 days.
- Direct ordinary-retention execution confirmed old published Outbox and its
  terminal batch/reconciliation are removed while old `FAILED` and `PENDING`
  Outbox rows and their unresolved reconciliation/batches survive:
  `ordinary-retention-probe.log`.
- Catalog inspection found no raw/payload/PPG/RRI column.
- Scoped recent-query indexes and source/outbox unique keys exist in the
  migration SQL and live catalog.
- The failure gate reproduced SQLSTATE `23505` duplicate rejection for batch,
  measurement, and quality source keys.
- With session timezone `Asia/Shanghai`, the legacy helper maps the
  timezone-less epoch-derived value `2024-07-03 09:46:40.123` to
  `2024-07-03T09:46:40.123Z`, matching the plan's explicit rule that these
  legacy values are UTC values originally derived from epoch milliseconds.

## removeAiSlopsAndProgrammingPass

- Direct review found no deletion-only, removal-marker, prose-pinning,
  tautological, or implementation-mirroring test.
- The test uses real databases and observable SQL/catalog outcomes, not mocks.
- The current happy test only counts policies/hypertables and checks one index;
  it does not assert exact chunk intervals, policy configs, ordinary cleanup,
  or raw-payload absence. This creates maintenance burden and false confidence,
  but the independent direct probes cover the current artifact. It is a NOTE,
  not a second blocker.
- The unsupported test asserts only absence of
  `hardware_upload_batch`; this is too narrow for the stated
  fail-before-write criterion and allowed the residual Flyway table to pass.
- The hard-coded `rehealth_test` database password is a synthetic test-only
  credential, not a repository secret.
- No unnecessary production parsing, normalization, extraction, or speculative
  abstraction was identified in the migration scope.

## checkedArtifactPaths

- `.omo/plans/complete-rehealth-backend-model-service.md`
- `AGENTS.md`
- `ENGINEERING.md`
- `CODEX_ORCHESTRATION.md`
- `ACCEPTANCE_REVIEW_2026-07-16.md`
- `backend/device-service/pom.xml`
- `backend/device-service/src/main/resources/application.yml`
- `backend/device-service/src/main/java/com/rehealth/device/config/TimescaleDatabaseProperties.java`
- `backend/device-service/src/main/java/com/rehealth/device/config/TimescaleMigrationConfiguration.java`
- `backend/device-service/src/main/resources/db/migration/timescale/V1__verify_timescale_prerequisites.sql`
- `backend/device-service/src/main/resources/db/migration/timescale/V2__create_hardware_schema.sql`
- `backend/device-service/src/main/resources/db/migration/timescale/V3__create_hypertables_and_lifecycle_policies.sql`
- `backend/device-service/src/test/java/com/rehealth/device/TimescaleMigrationIT.java`
- `backend/device-service/src/test/java/com/rehealth/device/TimescaleTestDatabase.java`
- `.omo/evidence/complete-rehealth-backend-model-service/task-7/`
- `.omo/evidence/complete-rehealth-backend-model-service/task-7-independent-verifier/t7-happy.log`
- `.omo/evidence/complete-rehealth-backend-model-service/task-7-independent-verifier/t7-failure.log`
- `.omo/evidence/complete-rehealth-backend-model-service/task-7-independent-verifier/direct-db-audit.log`
- `.omo/evidence/complete-rehealth-backend-model-service/task-7-independent-verifier/ordinary-retention-probe.log`

## exactEvidenceGaps

- No Todo 7 executor summary, code-review report, manual-QA matrix, or notepad
  path was present under the supplied Todo 7 evidence directory.
- Existing Todo 7 evidence contained exit files and image-pull logs, but no
  assertion-bearing JUnit/SQL output. This verifier supplied fresh exact-gate
  logs and direct SQL snapshots.
- No separate code-review artifact demonstrates the required programming and
  overfit/slop perspectives. The direct pass above supports all completion
  areas except the cited fail-before-write violation.
- `omo ulw-loop status --json` could not be executed because `omo` is not on
  PATH, so the explicitly requested verifier directory was used.

## scopeAndSecrets

- Product code was not edited, committed, or pushed.
- Only this independent verifier evidence directory was created.
- Sensitive-term review found only the synthetic `rehealth_test` test password.
- The worktree already contained unrelated tracked and untracked changes; they
  were preserved.

