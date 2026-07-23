# Todo 8 Independent Gate Review

recommendation: APPROVE

blockers: []

## originalIntent

Port authenticated Device Service telemetry ingestion to PostgreSQL 17 plus
TimescaleDB so one durable transaction owns the batch receipt, normalized
measurement/session rows, quality and rejection records, reconciliation state,
and versioned outbox events. Replays and concurrent duplicates must resolve to
one logical batch and original receipt. Database failures must produce a stable
retryable 503 without partial rows or false success. Kafka must not participate
in the ingestion transaction.

## desiredOutcome

An authenticated mixed telemetry upload commits atomically to TimescaleDB and
returns success only after commit. Replays are idempotent and owner-scoped.
Concurrent duplicates are deadlock-free and exact-count. Failed persistence is
atomic and retryable. A pending, versioned outbox records the future Kafka work.

## userOutcomeReview

The shipped implementation satisfies Todo 8 on the current worktree.

- `TransactionTemplate.execute` encloses advisory locking, replay resolution,
  batch/reconciliation inserts, normalized measurement/sleep/activity inserts,
  quality/rejection inserts, batch/reconciliation state transitions, and both
  versioned outbox inserts. The controller cannot construct HTTP 200 until the
  synchronous service/store call returns after the transaction commits.
- A first upload and replay return the same receipt. SQL assertions reproduce
  exact counts: batch 1, measurement 1, sleep 1, activity 1, quality 2,
  reconciliation 1, outbox 2. The persisted and quality event types each occur
  once at event version 1.
- The owner key includes tenant, authenticated user, device, and batch ID.
  A same-batch-ID upload by a second user creates a separate receipt and row.
- Two simultaneous duplicate writes completed without deadlock, produced one
  receipt, one duplicate response, and the exact single logical row/event set.
- A constraint violation in a later measurement rolled back every table to
  zero rows and raised `HARDWARE_PERSISTENCE_UNAVAILABLE` with status 503.
  An unreachable database produced the same stable 503 HTTP envelope.
- No Kafka dependency, producer, client, or call occurs in the store or module
  dependency list. Outbox rows remain `PENDING`; publication is a later task.

## Reproduced gates

Pinned database:

- image: `timescale/timescaledb:2.21.1-pg17`
- digest/image ID:
  `sha256:c17f60ac41a9b5c529af918e8827156bb02faeddb3fab1c961b4eebe52b25d83`
- TimescaleDB: `2.21.1`
- PostgreSQL: `17.5`

Native `mvn` was unavailable on PATH, so Maven 3.9.11 was invoked by its
absolute path with the exact T8 Maven arguments. Testcontainers' Windows
Docker discovery returned a false HTTP 400 even while Docker CLI was healthy;
therefore the same pinned image was launched directly and supplied through
the test's supported `TIMESCALE_TEST_JDBC_URL`.

- Happy T8: exit 0; 6 discovered, 0 failures, 0 errors, 3 intentionally
  skipped failure cases; all 3 happy cases executed.
- Failure T8 with
  `-Dcases=concurrent_duplicate,mid_batch_failure,db_down`: exit 0; 6
  discovered, 0 failures, 0 errors, 3 intentionally skipped happy cases; all
  3 requested adversarial cases executed.

Machine-readable evidence:

- `t8-happy-surefire.xml`
- `t8-failure-surefire.xml`

## Direct programming and remove-ai-slops pass

The tests are behavior-bearing rather than deletion-only, tautological, or
request-removal assertions. Real Timescale migrations and SQL row counts are
used; no persistence mock mirrors the implementation. Separate fixtures make
owner and fallback values observably distinct. The failure cases exercise real
constraint rollback, concurrent connections, and the HTTP error boundary.

No success-criterion-blocking slop was found. One non-blocking maintenance note:
`TimescaleTelemetryStore.java` is 489 pure LOC and
`TelemetryIngestionIT.java` is 366 pure LOC, above the programming skill's
250-LOC review threshold. This is a NOTE because Todo 8 specifies behavior,
atomicity, and evidence, not a module-size ceiling; changing it during a
read-only gate would add risk and scope.

The task evidence directory contains no executor code-review report or notepad.
This is an exact evidence gap, but not a stated Todo 8 acceptance criterion.
The direct gate pass above independently covers programming, test-overfit, and
AI-slop criteria.

## Advisory-lock audit

The transaction lock key hashes the length-independent tuple
`tenantId + U+001F + userId + U+001F + deviceId + U+001F + batchId` to a
PostgreSQL signed 64-bit advisory key. All duplicate contenders take exactly
one transaction-scoped lock before replay lookup, so there is no multi-lock
ordering cycle. A rare hash collision only serializes unrelated uploads; it
does not merge them because replay lookup and the database unique constraint
use all four original columns. This is collision-safe for correctness.

The separator is a control character and is not accepted by normal IDs. Even
if malformed components made tuple strings ambiguous, the full-column unique
constraint and replay predicate still prevent cross-owner receipt reuse.

## checkedArtifactPaths

- `.omo/plans/complete-rehealth-backend-model-service.md` (T8 and Todo 8)
- `AGENTS.md`
- `ENGINEERING.md`
- `CODEX_ORCHESTRATION.md`
- `ACCEPTANCE_REVIEW_2026-07-16.md`
- `.omo/evidence/complete-rehealth-backend-model-service/task-8/`
- `backend/device-service/README.md`
- `backend/device-service/pom.xml`
- `backend/device-service/src/main/java/com/rehealth/device/adapter/TimescaleTelemetryStore.java`
- `backend/device-service/src/main/java/com/rehealth/device/api/DeviceTelemetryController.java`
- `backend/device-service/src/main/java/com/rehealth/device/application/DeviceTelemetryService.java`
- `backend/device-service/src/main/java/com/rehealth/device/application/DeviceRequestException.java`
- `backend/device-service/src/main/java/com/rehealth/device/config/TimescaleMigrationConfiguration.java`
- `backend/device-service/src/main/resources/db/migration/timescale/V2__create_hardware_schema.sql`
- `backend/device-service/src/main/resources/db/migration/timescale/V3__create_hypertables_and_lifecycle_policies.sql`
- `backend/device-service/src/test/java/com/rehealth/device/TelemetryIngestionIT.java`
- `backend/device-service/src/test/java/com/rehealth/device/TimescaleTestDatabase.java`

## exactEvidenceGaps

- No Todo 8 executor code-review report was present.
- No Todo 8 notepad path/artifact was present.
- The happy path validates the store and committed SQL state directly rather
  than driving a successful HTTP request; synchronous controller/service/store
  control flow proves HTTP 200 is constructed only after the committed store
  call returns. The DB-down HTTP path is exercised through MockMvc.
- The concurrent test uses unbounded `Future.get()`. It completed in 1.615 s
  in this reproduction, proving the current implementation did not deadlock,
  but a future regression could hang the test rather than fail on a deadline.
  This is not a failure of the stated current-behavior criterion.

