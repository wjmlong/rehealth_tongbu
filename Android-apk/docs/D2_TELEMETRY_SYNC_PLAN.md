# D2 Telemetry Sync Status and Remaining QA

Status: implemented software path; updated 2026-07-31.

## Implemented

- MRD/RWFit/HBand collection writes to Room before any network operation.
- Successful manual/automatic sync creates a durable `telemetry_batch` queue item.
- WorkManager uploads through the authenticated Jeecg mobile client.
- `401` pauses the queue for re-login; transient failures retry the same batch.
- A batch is complete only after backend confirms durable hardware-db persistence.
- Raw signal bytes and entity `rawPayload` fields are excluded.
- Device addresses are SHA-256 hashed before cloud binding/upload.
- Synthetic QA provenance is labelled `synthetic_qa`.
- Room v11 adds user-scoped `diet_records`. Manual meal entry persists locally
  before creating a stable `telemetry-v2 dietRecords` queue item. When no real
  wearable identity is bound, the row remains local and is queued after binding;
  network availability never blocks entry.
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
  capability-gated manual measurement and body-composition history follow. Direct HBand
  HRV/MET commands require their dedicated SDK flags, but the purchased MT116 still rejects
  the advertised HRV/stress/MET direct commands with `unknown action`. `RH-HB-E01` therefore
  prioritizes the package-4 mini-checkup result for HRV/stress and device history for MET;
  HRV/stress/MET can all read scoped device manual history as the final real-data fallback.
  This prevents the SDK's unsupported-feature toast without inventing an instant MET value;
  only positive real SDK results are persisted. Completed
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
- Diet repository tests cover local-first save, structured batch mapping and
  single enqueue after a delayed device binding; migration 10→11 has an
  instrumentation migration test.
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
