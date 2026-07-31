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
  capability-gated manual measurement and body-composition history follow. Direct HBand App
  HRV/MET measurement uses the vendor's exact double gates: `isSupportHRV &&
  isSupportHrvAppDetect` and `isSupportMet && isSupportMetAppDetect`. When the relevant pair
  is true, `RH-HB-E01` prefers the dedicated SDK API; otherwise HRV/stress retain package-4
  mini-checkup or history fallback and MET retains manual-measurement history fallback. The
  purchased MT116's 2026-07-30 `unknown action` evidence covers the older generic commands.
  This prevents the SDK's unsupported-feature toast without inventing an instant MET value;
  only positive real SDK results are persisted. Completed
  reads are retained if a later optional SDK operation fails. Unsupported,
  zero, and invalid readings remain absent; raw ECG samples remain local only.
  HBand ECG uses the matching four-ABI JNI runtime and Room v5: new records store
  calibrated `FLOAT32_LE` mV plus structured lead/sample/duration/contact metadata,
  while migrated legacy `INT32_LE` rows remain relative-only. Neither representation
  is included in telemetry uploads.
- Data-card visibility uses App-measurement capability separately from history capability:
  HRV/stress/MET are hidden when only historical sync is available. Dedicated HRV/MET or
  mini-checkup HRV/stress remain visible and measurable.
- Room v8 adds nullable `total_sleep_minutes` through a non-destructive v7→v8 migration.
  HBand persists the SDK-authoritative `allSleepTime` there and period aggregation uses it
  before actual sleep stages (`deep + light + REM`) and finally elapsed session time. Awake
  minutes and the `sleepDown`/`sleepUp` clock span are not counted as HBand sleep duration.
  Queries still use `ended_at`, so cross-midnight sessions ending today remain included.
- The Data-screen action is a connected-only daily sync for sleep, steps, and activity. It never
  auto-connects from the UI, and the in-process automatic cycle skips while disconnected. Explicit
  Foreground Service recovery retains bound-device reconnect behavior. For HBand, existing recent
  Room sleep/activity rows select a two-day-or-greater overlap window; origin history is skipped
  when activity has no gap, while first sync or a gap retains origin-history recovery. Vendor sleep
  and origin callbacks feed monotonic target progress, and Compose smooths toward that target without
  delaying persistence or upload completion.

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
