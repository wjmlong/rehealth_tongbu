# ReHealth Device Service

Independent Spring Boot 3.5.5 / Java 17 boundary for authenticated wearable
telemetry. It reuses the versioned sources from `backend/contracts/telemetry`
and exposes:

- `POST /rehealth/mobile/measurements/batch`
- `GET /rehealth/mobile/measurements/recent`
- `GET /rehealth/internal/v1/operations/status`
- `/actuator/health` and `/actuator/health/readiness`
- `/v3/api-docs`

The scaffold fails closed for identity and persistence until concrete adapters
are configured. Todo 7 provides the PostgreSQL 17 / TimescaleDB schema and
Flyway lifecycle. Todo 8 adds the telemetry-port adapter: each accepted batch,
normalized telemetry, quality/rejection records, reconciliation state, and
versioned Outbox events commit in one Timescale transaction. Duplicate replays
return the original receipt without adding rows. Kafka publication remains a
separate concern. This module does not copy Jeecg repositories or model
responsibilities.

Readiness is `OUT_OF_SERVICE` until both identity authorization and telemetry
storage report ready. The HTTP identity adapter requires
`REHEALTH_IDENTITY_BASE_URL`, `REHEALTH_IDENTITY_READINESS_URL`, an internal
service credential, and `REHEALTH_IDENTITY_ENABLED=true`. Kafka is deliberately
not a readiness dependency.

Set `REHEALTH_HARDWARE_DB_ENABLED=true` with
`REHEALTH_HARDWARE_DB_URL`, `REHEALTH_HARDWARE_DB_USERNAME`, and either
`REHEALTH_HARDWARE_DB_PASSWORD` or `REHEALTH_HARDWARE_DB_PASSWORD_FILE` to run
the Timescale migrations at startup. Retention defaults are 730 days for
normalized measurement/session telemetry, 90 days for signal metadata, 1,095
days for operational history, and 30 days for published outbox rows. Failed or
unresolved outbox work is never removed by the lifecycle job.

Run:

```bash
mvn -f backend/device-service/pom.xml test
```

## Legacy MySQL hardware migration

`scripts/migrate_hardware.py` dark-migrates all six legacy hardware tables
without changing Gateway routes or deleting MySQL rows. Configure
`REHEALTH_HARDWARE_MYSQL_DSN`, `REHEALTH_TIMESCALE_DSN`, and the source
database tenant through `REHEALTH_MIGRATION_TENANT_ID`, then run:

```bash
python backend/device-service/scripts/migrate_hardware.py migrate
python backend/device-service/scripts/migrate_hardware.py reconcile \
  --output /approved-evidence/reconciliation.json
```

Pages are ordered by each table's legacy UTC time column and source ID. Each
target upsert and `hardware_migration_checkpoint` advance commit in one
Timescale transaction. An interrupted `PENDING` checkpoint resumes after the
last committed time/ID; a completed `VERIFIED` checkpoint is rescanned on the
next run so late-arriving legacy rows are included without duplicating target
rows.

Legacy MySQL `DATETIME(3)` values are interpreted as UTC. Existing UUIDs are
preserved; non-UUID IDs use deterministic UUIDv5 mapping. Nullable client
record IDs become `legacy:<table>:<source-id>`. Legacy `COMMITTED` batches map
to `PERSISTED`, null quality JSON maps to `{}`, raw `payload_ref` is never
migrated, and legacy quality messages are represented only by a SHA-256 detail
code.

The reconciliation artifact contains only credential-free DSN fingerprints,
schema versions, the UTC window, per-table and per-tenant/user/device/day
counts, canonical hashes and samples, recent-query shadow comparisons,
mismatches, eligibility, the full Git SHA, and creation/expiry times. Its JSON
is canonicalized and written once so cosign signs the exact bytes:

```bash
python backend/device-service/scripts/migrate_hardware.py sign \
  --reconciliation reconciliation.json \
  --signature reconciliation.sig
python backend/device-service/scripts/migrate_hardware.py verify \
  --reconciliation reconciliation.json \
  --signature reconciliation.sig
python backend/qa/hardware_migration_gate.py reconciliation \
  --report reconciliation.json \
  --signature reconciliation.sig \
  --verify-key-env REHEALTH_CUTOVER_VERIFY_KEY
```

The private key is accepted only through
`REHEALTH_CUTOVER_SIGNING_KEY`; the CLI never writes it. The QA gate validates
the report and signature but always records `route_changed=false`. Gateway
cutover remains a separate task.
