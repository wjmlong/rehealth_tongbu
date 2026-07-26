# D2 Telemetry Sync Status and Remaining QA

Status: implemented software path; updated 2026-07-26.

## Implemented

- MRD collection writes to Room before any network operation.
- Successful manual/automatic sync creates a durable `telemetry_batch` queue item.
- WorkManager uploads through the authenticated Jeecg mobile client.
- `401` pauses the queue for re-login; transient failures retry the same batch.
- A batch is complete only after backend confirms durable hardware-db persistence.
- Raw signal bytes and entity `rawPayload` fields are excluded.
- Device addresses are SHA-256 hashed before cloud binding/upload.
- Synthetic QA provenance is labelled `synthetic_qa`.

## Software-Only Validation

- DTO/route contract tests with MockWebServer.
- Room-to-telemetry mapping tests, including stable batch identity and raw-data exclusion.
- Queue retry, durable acknowledgement, malformed payload, and 401 policy tests.
- Debug Kotlin compilation, JVM unit tests, and debug APK assembly.

## HARDWARE_QA_PENDING

The following cannot be accepted without a physical MRD ring and Android 13+ test
device:

- BLE scan/connect/reconnect and permission behavior.
- MR11 SDK measurement commands and timestamp/quality accuracy.
- Foreground collection across screen-off, process restart, and network loss.
- Long-duration duplicate/loss rate and upload latency.
- Battery consumption and thermal behavior.
- Raw-signal capability and consent gate, if enabled in a later release.

No synthetic record may be presented as evidence for these hardware gates.
