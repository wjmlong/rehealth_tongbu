# ReHealth Hardware Ingest Architecture E2.1

Date: 2026-07-13
Module: `jeecg-boot/jeecg-boot-module/jeecg-module-rehealth`

## Scope

E2.1 replaces the dev-only in-memory acceptance path behind
`POST /rehealth/mobile/measurements/batch` with a synchronous, transactional
MVP write to the separate `hardware` datasource/schema.

This path does not write telemetry to `software_db`, call model-service, run
CatBoost/SHAP, or perform intervention, attribution, or settlement work.

## Runtime Flow

```text
authenticated Android request
  -> ReHealthMobileController replaces body userId with LoginUser.id
  -> HardwareTelemetryIngestionService
  -> TelemetryBatchValidator
       -> reject empty/oversized/raw-signal input
  -> JdbcHardwareTelemetryWriter
       -> resolve physical dynamic datasource named hardware
       -> one hardware-local transaction
       -> idempotency lookup/unique constraint
       -> batch + measurements + sleep + activity
       -> commit
  -> ACCEPTED_PERSISTED or ACCEPTED_DUPLICATE
```

`accepted=true` and `persisted=true` are returned only after the hardware-local
transaction commits, or when a previously committed batch is found.

## Ownership And Isolation

- The JSON `userId` field remains for Android D2 DTO compatibility.
- The controller overwrites it with the current Jeecg `LoginUser.id`.
- Idempotency is scoped by `(authenticated user_id, device_id, batch_id)`.
- A client cannot select another user's hardware rows by changing `userId`.
- The writer obtains the physical datasource from
  `DynamicRoutingDataSource.getDataSources().get("hardware")`; it does not use
  the default `master` route.
- The transaction manager is created for that physical datasource only. There
  is no software/hardware distributed transaction.

Device ownership validation against a durable `software_db` binding remains an
E1.1 dependency. Until that exists, authentication protects user ownership but
does not prove that the submitted `deviceId` is bound to that user.

## Idempotency

`hardware_upload_batch` has a unique key on
`(user_id, device_id, batch_id)`. A normal retry returns the original receipt.
Concurrent insert races are resolved by the same database constraint and a
post-rollback lookup. Child rows are not inserted again.

Statuses:

| Status | Meaning |
| --- | --- |
| `ACCEPTED_PERSISTED` | New batch and normalized rows committed. |
| `ACCEPTED_DUPLICATE` | The same owner/device/batch was already committed. |
| `REJECTED_INVALID` | Validation rejected the request before persistence. |

When the datasource is disabled, missing, or a transaction fails, the endpoint
returns a failed Jeecg envelope with `code=503`. Android must retain the local
batch and retry with the same `batchId`.

## Raw Signal Policy

Raw signal chunks, `payload_base64`, raw payload fields, PPG/RRI/waveform keys,
and nested equivalents are rejected by default. The V1 schema contains only a
future metadata table and no raw payload column. Enabling raw signal upload
still requires separate consent, retention, encryption, and object-storage
approval; E2.1 does not implement that path.

## PIAS Boundary

Hardware telemetry is an authenticated fact source for later backend
orchestration. Patient clients do not submit risk history for attribution and
do not call group attribution or settlement. A later E1.1/E1.2 backend flow
must build individual attribution inputs from persisted risk, intervention,
feedback, and telemetry-derived summaries. Settlement remains an admin-only
evidence workflow and never runs in telemetry ingestion.

## Production Follow-up

The direct JDBC transaction is appropriate for an MVP pilot, not the final
high-concurrency topology. A later task should add one durable queue/stream,
consumer batch writers, pressure tests, observability, partitioning/retention,
and dead-letter handling without changing the Android batch contract.
