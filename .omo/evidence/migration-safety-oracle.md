# Migration safety oracle — Room 3→4

Reviewed 2026-07-22, read-only. Scope: `AppDatabase.kt` and `RiskHistoryMigrationSql.kt`.

## Scenarios and observables

1. Existing v3 database with the v2→3 queue indices.
   - Invocation/artifact: decoded `.omo/qa/restore-demo-attribution-live-data/task-4/schema-reconcile-logcat.txt` with `iconv -f UTF-16LE -t UTF-8 ...`.
   - Binary observable: app crashed with `Migration didn't properly handle: sync_upload_queue`; Room expected no indices and found `index_sync_upload_queue_status_retry`.
   - Current-source assessment: the later `VersionThreeSchemaSql` table-copy rebuild removes undeclared indices while preserving queue rows. Room invokes registered migrations in its upgrade transaction, so rename/create/copy/drop is atomic.

2. Legacy risk-history data preservation.
   - Invocation/artifact: inspected `.omo/qa/restore-demo-attribution-live-data/task-4/legacy-risk-history-schema.txt` and current copy SQL.
   - Binary observable: the legacy table has `PRIMARY KEY(evaluated_on)`; copying every row to the constant `__legacy_unscoped__` therefore cannot collide on the new `(user_id, evaluated_on)` key.
   - Caveat: `RiskHistoryDao.latestForUser` only queries the authenticated user, so these rows are retained on disk but intentionally unreachable unless a later explicit user-claim strategy is introduced.

3. Canonical Room v4 schema shape.
   - Invocation/artifact: inspected generated `Android-apk/app/build/generated/ksp/debug/kotlin/com/rehealth/genie/data/AppDatabase_Impl.kt`.
   - Binary observable: expected risk table has five exact columns, composite PK `(user_id, evaluated_on)`, and `index_cvd_risk_history_user_day`; queue entities expect no indices and no SQL defaults.

## Findings / minimal corrections

- **High — unknown risk schema is destructively dropped.** `RiskHistoryMigrationSql.forColumns` drops any nonempty table that is neither the three-column legacy shape nor merely contains `user_id`. This violates health-history preservation. Minimal correction: throw for unknown shapes so Room rolls the migration transaction back, or add an explicit lossless copy mapping for each known shape; never silently `DROP TABLE`.
- **Medium — `user_id` alone is not canonical validation.** A table with `user_id` but a missing/wrong column, affinity, nullability, or primary key receives only an index and then fails Room validation. Minimal correction: recognize the complete canonical column set and rebuild it into the exact v4 table (copying values), or verify `PRAGMA table_info` including type/not-null/PK positions before the index-only branch.
- **Medium — tests verify strings, not SQLite/Room migration.** The JVM test cannot prove transaction rollback, copied row counts, defaults, indices, or Room `TableInfo` validation. Minimal correction: keep a fresh installed-v3 → v4 ADB upgrade scenario that checks process survival and row/schema observables; longer-term enable exported schemas plus `room-testing` `MigrationTestHelper`.
- **Product decision — legacy rows are orphaned by design.** Do not automatically attach `__legacy_unscoped__` history to whichever account logs in, because that can cross account boundaries. Either document exclusion or claim only with a trustworthy legacy account binding.

No files under `Android-apk` were modified by this review.
