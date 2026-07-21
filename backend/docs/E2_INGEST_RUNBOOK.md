# E2.1 Durable Hardware Ingest Runbook

Date: 2026-07-13
Endpoint: `POST /rehealth/mobile/measurements/batch`

## Prerequisites

1. Create a separate MySQL schema named `rehealth_hardware`.
2. Apply the add-only V1 migration:

```powershell
$migration = "D:\rehealthAI\backend\jeecg-boot\jeecg-boot-module\jeecg-module-rehealth\src\main\resources\db\hardware\mysql\V1__create_hardware_telemetry_tables.sql"
Get-Content -Raw $migration | & mysql -h 127.0.0.1 -u root -p rehealth_hardware
```

JeecgBoot disables Flyway auto-configuration, so this migration is an explicit
deployment step. Do not enable the writer before the V1 SQL succeeds.

## Configuration

```powershell
$env:REHEALTH_HARDWARE_DB_ENABLED = "true"
$env:REHEALTH_HARDWARE_DB_URL = "jdbc:mysql://127.0.0.1:3306/rehealth_hardware?characterEncoding=UTF-8&useUnicode=true&useSSL=false&serverTimezone=Asia/Shanghai"
$env:REHEALTH_HARDWARE_DB_USERNAME = "root"
$env:REHEALTH_HARDWARE_DB_PASSWORD = "<local-password>"
```

The corresponding config keys are:

```yaml
rehealth.ingest.mode: durable-direct
rehealth.ingest.queue.type: direct-hardware-db
rehealth.hardware-db.enabled: true
rehealth.raw-signal-upload.enabled: false
spring.datasource.dynamic.datasource.hardware: <separate hardware_db connection>
```

If `rehealth.hardware-db.enabled=false`, valid telemetry is not accepted into a
fallback memory queue. The endpoint returns `code=503` and the Android local
queue must retry later.

## Start Backend

From `D:\rehealthAI\backend\jeecg-boot`:

```powershell
$env:JAVA_HOME = "D:\Android_Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
D:\rehealthAI\tools\apache-maven-3.9.11\bin\mvn.cmd -pl jeecg-module-system/jeecg-system-start -am spring-boot:run -Dspring-boot.run.profiles=dev
```

Use a valid Jeecg token. Only `/rehealth/mobile/health` is unauthenticated.

## Manual QA

Submit this D2-compatible body twice with the same token and `batchId`:

```json
{
  "batchId": "manual-e2-1-001",
  "userId": "client-value-is-ignored",
  "deviceId": "ring-001",
  "collectedFrom": 1720000000000,
  "collectedTo": 1720000300000,
  "source": "ANDROID_ROOM",
  "measurements": [{
    "id": "measurement-001",
    "metricType": "HEART_RATE",
    "measuredAt": 1720000010000,
    "primaryValue": 72.0,
    "unit": "bpm",
    "source": "MRD"
  }],
  "sleepSessions": [],
  "activitySessions": [],
  "signalChunks": [],
  "quality": {"schemaVersion": "d2-v1"}
}
```

Expected first result: `ACCEPTED_PERSISTED`, `accepted=true`,
`persisted=true`, `queued=false`, `ingestStage=HARDWARE_DB_COMMITTED`.

Expected retry result: `ACCEPTED_DUPLICATE`, the same `receiptId`, and no new
rows. Verify with:

```sql
SELECT COUNT(*) FROM hardware_upload_batch WHERE batch_id = 'manual-e2-1-001';
SELECT COUNT(*) FROM hardware_measurement m
JOIN hardware_upload_batch b ON b.id = m.upload_batch_id
WHERE b.batch_id = 'manual-e2-1-001';
```

Both counts should be `1`. Restart backend, submit the same body again, and
confirm `ACCEPTED_DUPLICATE` with both counts still `1`.

Add a `signalChunks` item or a nested `ppgPayload`/`rawPayload` key and confirm
`REJECTED_INVALID`, `accepted=false`, and no new database rows.

## Automated Validation

```powershell
D:\rehealthAI\tools\apache-maven-3.9.11\bin\mvn.cmd -pl jeecg-boot-module/jeecg-module-rehealth -am '-Dtest=TelemetryBatchValidatorTest,HardwareTelemetryIngestionServiceTest,JdbcHardwareTelemetryWriterTest' test
D:\rehealthAI\tools\apache-maven-3.9.11\bin\mvn.cmd -pl jeecg-boot-module/jeecg-module-rehealth -am package
git diff --check
git status --short --branch
```

The H2 MySQL-mode tests cover committed normalized writes, process-restart-style
idempotency, full rollback after a partial row failure, raw rejection, and API
response semantics. They do not replace the MySQL manual QA above.

## Operational Risks

- Direct synchronous JDBC throughput is bounded by the hardware connection pool.
- No durable MQ, dead-letter queue, partition rotation, or load-test evidence exists yet.
- Device ownership validation awaits durable `software_db` device binding.
- Retention values are policy/config documentation; purge jobs are not implemented.
