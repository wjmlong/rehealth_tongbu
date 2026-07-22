# Measurement Upload Consumer DoneClaim

Date: 2026-07-22

## Delivered

- `ReHealthApi` / `ReHealthMobileApi` post the existing typed telemetry DTO to authenticated `POST /rehealth/mobile/measurements/batch`.
- `AuthenticatedApiClient` exposes the measurement upload through the existing token and 401 state handling while preserving the concurrent authenticated attribution method.
- `SyncRepository` consumes due `telemetry_batch` Room rows and persists binary outcomes:
  - durable `ACCEPTED_*` + `accepted=true` + `persisted=true` -> `done`;
  - 401 -> queue `Paused`, row retained;
  - network / 503 -> `failed` with exponential `next_retry_at` and the same stored `batchId`;
  - malformed or permanent-invalid payload -> `dead_letter` with category-only safe error text.
- `MeasurementSyncWorker` drains measurement rows before feedback, asks WorkManager to retry transient failures, and stops successfully while authentication is paused.
- No raw health payload, token, phone, or identifier was added to production logs.

## Validation

Scenario: authenticated durable measurement consumer behavior.

Invocation:

`D:\rehealthAI\Android-apk\gradlew.bat testDebugUnitTest --tests com.rehealth.genie.data.sync.SyncRepositoryMeasurementTest --tests com.rehealth.genie.network.ReHealthMobileApiMeasurementTest --tests com.rehealth.genie.work.MeasurementSyncWorkerPolicyTest`

Binary observable: `BUILD SUCCESSFUL in 1m 10s`.

Captured artifact: `D:\rehealthAI\.omo\evidence\measurement-upload\focused-tests.txt`.

Red-first artifact: `D:\rehealthAI\.omo\evidence\measurement-upload\red-focused-tests.txt` (expected unresolved production seams before implementation).

## Explicit Remaining Work

- Producer is not delivered in this scoped patch: real MRD Room persistence does not yet enqueue `telemetry_batch` rows. The consumer works for existing rows, but BLE -> queue -> worker is not an end-to-end DoneClaim.
- Replace the retired direct `/rehealth/mobile/ring/snapshots` producer path with local-first queue insertion in a follow-up; enqueue failure must not roll back Room health-data persistence and raw signal bytes must remain excluded.
- Backend runtime must configure the named `hardware` datasource and apply the hardware schema migration. With `rehealth.ingest.hardware-db-enabled=false` or no `hardware` datasource, Jeecg intentionally returns code 503 and Android retains/retries the batch.
- Physical MRD + authenticated Jeecg + real MySQL hardware datasource QA remains required.
