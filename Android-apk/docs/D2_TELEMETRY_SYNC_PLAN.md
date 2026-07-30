# D2 Telemetry Sync Status and Remaining QA

Status: implemented software path; updated 2026-07-29.

## Implemented

- MRD/RWFit/HBand collection writes to Room before any network operation.
- Successful manual/automatic sync creates a durable `telemetry_batch` queue item.
- WorkManager uploads through the authenticated Jeecg mobile client.
- `401` pauses the queue for re-login; transient failures retry the same batch.
- A batch is complete only after backend confirms durable hardware-db persistence.
- Raw signal bytes and entity `rawPayload` fields are excluded.
- Device addresses are SHA-256 hashed before cloud binding/upload.
- Synthetic QA provenance is labelled `synthetic_qa`.
- Collection is routed through one `productCode`-selected Provider. The Release
  registry contains MRD/RWFit/HBand and all keep the existing Room batch path.
- Cloud binding and batch provenance derive from the active domain vendor:
  `mrd-*`/`mrd_room`, `rwfit-*`/`rwfit_room`, or `hband-*`/`hband_room`. The latest snapshot excludes
  records whose entity source belongs to another vendor.
- MRD background reconnect uses only the encrypted active binding address. With
  no successful foreground binding, it writes no record and retries later; it
  does not use a fixed address or synthesize missing metrics.
- HBand synchronization attempts ECG history first, then reads live daily sport and
  explicitly awaits `readSleepData` before starting `readOriginData`, because physical-device
  validation found firmware that returned origin records but omitted sleep from `readAllHealthData`. Five-minute
  step, distance, and calorie records are aggregated per day before Room persistence;
  capability-gated manual measurement and body-composition history follow. HBand HRV
  capability merges the app-detection, device-feature, and protocol-type signals; MET
  accepts either its feature flag or non-zero protocol type. Connected `RH-HB-E01` devices
  additionally expose scoped HRV/MET compatibility commands for legacy firmware that leaves
  those signals unset; only positive real SDK results are persisted. Completed
  reads are retained if a later optional SDK operation fails. Unsupported,
  zero, and invalid readings remain absent; raw ECG samples remain local only.
  HBand ECG uses the matching four-ABI JNI runtime and Room v5: new records store
  calibrated `FLOAT32_LE` mV plus structured lead/sample/duration/contact metadata,
  while migrated legacy `INT32_LE` rows remain relative-only. Neither representation
  is included in telemetry uploads.

## Software-Only Validation

- DTO/route contract tests with MockWebServer.
- Room-to-telemetry mapping tests, including stable batch identity and raw-data exclusion.
- Queue retry, durable acknowledgement, malformed payload, and 401 policy tests.
- Debug Kotlin compilation, JVM unit tests, and debug APK assembly.

## HARDWARE_QA_PENDING

The following cannot be accepted without the applicable physical MRD/RWFit ring or HBand wearable
and Android 13+ test device:

- BLE scan/connect/reconnect and permission behavior.
- First-bind address persistence and restart/background reconnect using that
  binding, including the no-binding no-connect case.
- MR11/RWFit/HBand SDK commands, timestamp/unit mapping, and measurement accuracy.
- Foreground collection across screen-off, process restart, and network loss.
- Long-duration duplicate/loss rate and upload latency.
- Battery consumption and thermal behavior.
- Raw-signal capability and consent gate, if enabled in a later release.

No synthetic record may be presented as evidence for these hardware gates.
