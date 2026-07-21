# E2.1 Backend Durable Telemetry Ingestion Status

Date: 2026-07-13
Branch: `work/E2.1_backend_durable_telemetry_ingestion`
Status: implementation complete; MySQL manual QA pending local database availability

## Implemented

- Replaced the endpoint's dev in-memory queue and pending writer with a direct,
  transactional `hardware_db` JDBC writer.
- Added an explicit physical datasource boundary named `hardware`; missing
  registration cannot fall back to the software `master` datasource.
- Added add-only MySQL V1 migration for upload batches, measurements, sleep,
  activity, signal metadata, and quality events.
- Added database-enforced idempotency on `(user_id, device_id, batch_id)`.
- Added committed and duplicate success semantics. `persisted=true` is emitted
  only after commit or for an existing committed receipt.
- Added a controlled `code=503` response when durable persistence is unavailable.
- Preserved Android D2 request/response DTO fields and camelCase record mapping.
- Bound persisted `user_id` to Jeecg authenticated `LoginUser.id`; body `userId`
  is compatibility-only.
- Kept raw signal payload/chunks disabled and recursively rejected by default.
- Kept telemetry ingestion independent from model-service and PIAS scoring.

## Automated Validation

Focused Maven tests:

```text
TelemetryBatchValidatorTest
HardwareTelemetryIngestionServiceTest
JdbcHardwareTelemetryWriterTest
```

Result: `BUILD SUCCESS`; 13 tests, 0 failures, 0 errors, 0 skipped.

Coverage includes normalized durable writes, duplicate replay across a new
writer instance, rollback after a partial child write, raw rejection, empty
batch rejection, and truthful response fields.

Additional validation:

- ReHealth module `package`: `BUILD SUCCESS`; 13 tests passed during package.
- `jeecg-system-start -am package -DskipTests`: `BUILD SUCCESS` across 11 reactor modules.
- The writer tests execute the delivered MySQL V1 migration directly in H2
  MySQL mode; migration and writer remained aligned.
- `git diff --cached --check`: passed.
- Android-apk and model-service had no working-tree changes. rehealth-android
  had pre-existing untracked NHANES files; E2.1 did not touch them.

## Manual QA

Not run. This machine has no `mysql` client on `PATH`, and TCP
`127.0.0.1:3306` is not listening. Docker is also not installed, so an isolated
MySQL/Redis container fallback cannot be started. Follow
`docs/E2_INGEST_RUNBOOK.md` on an environment with MySQL: submit one
authenticated D2 batch twice, verify one batch/child set, restart backend,
retry, and verify raw signal rejection.

## Known Risks

- Direct JDBC is an MVP pilot path; no durable MQ/stream or load-test evidence.
- Device ownership cannot be checked against software binding until E1.1 adds
  durable `software_db` device records.
- Flyway auto-configuration is disabled by JeecgBoot, so V1 is a documented
  manual deployment step.
- Retention purge/rollup jobs are not implemented.

## PIAS Follow-up Boundary

E2.1 only establishes authenticated telemetry facts. Recommended next sequence:
contract freeze, E1.1 software persistence/user isolation, E1.2 backend PIAS
orchestration/admin RBAC, D3 Android typed patient P/I/individual-A calls, then
full G4 acceptance. Settlement stays admin-only evidence generation.

## Repository Identity Follow-up

After E2.1 validation, the algorithm/research repository was renamed from
`rehealth-android` to `rehealth-algorithms` locally and on GitHub to avoid
confusion with the patient Android app. Active backend, Android, model-service,
and algorithm documentation was updated and pushed. Historical status and
acceptance snapshots retain the old name as evidence of their original state.
