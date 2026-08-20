# D2 Telemetry Sync Status and Remaining QA

Status: implemented software path; updated 2026-08-20.

## Implemented

### Viomi cloud pull path (2026-08-05)

- `VIOMI_CLOUD` is a non-BLE `RingRepository` provider for S8/S9/GS20/GS17/A67/K9L.
- Binding and history sync use authenticated backend endpoints; vendor credentials never enter the APK.
- A successful bind triggers the first sync automatically. The first pull requests up to 31 days;
  later pulls start two days before the latest scoped local record and remain capped at 31 days.
- Backend persistence is the authority. Only a persisted response is imported to Room.
- Imported `viomi_cloud` records set `RingSyncResult.requiresUpload=false`, preventing an upload echo loop.
- Room v15 adds nullable measurement owner/device columns and a composite lookup index. Room v16
  applies nullable owner/device scope to sleep, activity, and signal/ECG rows so account switching
  cannot expose another user's cached health telemetry. Viomi
  observations are read by authenticated user + hashed backend device + `viomi_cloud` source,
  while migration 14→15 preserves legacy rows with null scope.
- The cloud Data screen exposes only heart rate, blood oxygen and blood pressure. It uses real
  samples for trends, daily-balanced period means, and period-minimum SpO₂; unsupported sections
  and synthetic mini-charts are hidden.

- MRD/RWFit/HBand collection writes to Room before any network operation.
- Successful manual/automatic sync creates a durable `telemetry_batch` queue item.
- WorkManager uploads through the authenticated Jeecg mobile client.
- `401` pauses the queue for re-login; transient failures retry the same batch.
- Institution care-plan feedback observes its exact owner-scoped Room queue row after submission.
  Successful upload changes the plan message to “已同步”; transient failures use bounded backoff,
  while permanent rejection or ten exhausted attempts becomes `dead_letter` and is displayed as
  a failure instead of remaining indefinitely in “正在同步”.
- A batch is complete only after backend confirms durable hardware-db persistence.
- Raw signal bytes and entity `rawPayload` fields are excluded.
- Device addresses are SHA-256 hashed before cloud binding/upload.
- Synthetic QA provenance is labelled `synthetic_qa`.
- Room v11 adds user-scoped `diet_records`. Manual meal entry persists locally
  before creating a stable `telemetry-v2 dietRecords` queue item. When no real
  wearable identity is bound, the row remains local and is queued after binding;
  network availability never blocks entry.
- Collection is routed through one `productCode`-selected Provider. The Release
  registry contains only HBand and Viomi Cloud; the user chooses MT116 Bluetooth or
  Viomi IMEI cloud binding. MRD/RWFit remain Debug-only engineering providers.
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
  capability-gated manual measurement and body-composition history follow. The SDK layer retains
  the vendor's exact direct HRV/MET double gates (`isSupportHRV && isSupportHrvAppDetect` and
  `isSupportMet && isSupportMetAppDetect`) for compatibility and diagnostics. The `RH-HB-E01`
  product flow does not trust those flags as proof that the purchased firmware accepts the command:
  HRV/stress prefer package-4 mini-checkup or real history, and MET is history-only with no real-time
  action. The purchased MT116's 2026-07-30 all-zero `unknown action` evidence is the regression basis.
  This prevents the SDK's unsupported-feature toast without inventing an instant result;
  only positive real SDK results are persisted. Completed
  reads are retained if a later optional SDK operation fails. Unsupported,
  zero, and invalid readings remain absent; raw ECG samples remain local only.
  HBand ECG uses the matching four-ABI JNI runtime and Room v5: new records store
  calibrated `FLOAT32_LE` mV plus structured lead/sample/duration/contact metadata,
  while migrated legacy `INT32_LE` rows remain relative-only. Neither representation
  is included in telemetry uploads.
- HRV/stress/MET card visibility is value-gated rather than capability-gated. A real Provider Room
  record must contain HRV/MET `> 0` or stress `1..100`; mock, synthetic, missing, zero, non-finite,
  or out-of-range values hide the card. HRV/stress show a measure action only when mini-checkup is
  available; a history-only value has no action. MET never exposes a real-time measure action.
- Room v8 adds nullable `total_sleep_minutes` through a non-destructive v7→v8 migration.
  HBand persists the SDK-authoritative `allSleepTime` there and period aggregation uses it
  before actual sleep stages (`deep + light + REM`) and finally elapsed session time. Awake
  minutes and the `sleepDown`/`sleepUp` clock span are not counted as HBand sleep duration.
  Queries still use `ended_at`, so cross-midnight sessions ending today remain included.
  When one night has several increasing HBand cumulative snapshots, aggregation keeps the
  preferred final duration for that local wake-up day and averages those daily finals across
  the selected period; Data and Profile use the same selection rule and never treat cloud/local
  copies or intermediate callbacks as separate nights. Activity rows are cumulative day totals;
  presentation keeps the maximum per local day instead of adding overlapping local/cloud copies.
- The Data-screen action is a daily sync for sleep, steps, and activity. When the active Bluetooth
  device is disconnected, it first retries the encrypted bound-device connection with bounded
  backoff; it never scans or connects an unbound device. The in-process automatic cycle uses the
  same reconnect path instead of silently skipping a disconnected device. Explicit
  Foreground Service recovery retains bound-device reconnect behavior. For HBand, existing recent
  Room sleep/activity rows select a two-day-or-greater overlap window; origin history is skipped
  when activity has no gap, while first sync or a gap retains origin-history recovery. Vendor sleep
  and origin callbacks feed monotonic target progress, and Compose smooths toward that target without
  delaying persistence or upload completion.
- For an active Bluetooth binding, the Foreground Service remains the continuous collection path
  once explicitly enabled through the service/ViewModel API. Re-entering the Main stage no longer
  starts the service or triggers an immediate ring collection; this avoids a long reconnect and
  measurement step every time an older user reopens the app. Logout still stops the service, and a
  future settings/action flow can explicitly enable background collection again.

## Software-Only Validation

- DTO/route contract tests with MockWebServer.
- Room-to-telemetry mapping tests, including stable batch identity and raw-data exclusion.
- Queue retry, durable acknowledgement, malformed payload, and 401 policy tests.
- Intervention-feedback tests cover synced/retrying/dead-letter presentation and retry exhaustion.
- Diet repository tests cover local-first save, structured batch mapping and
  single enqueue after a delayed device binding; migration 10→11 has an
  instrumentation migration test.
- Debug Kotlin compilation, JVM unit tests, and debug APK assembly.
- Viomi mapping/range/scope unit tests, Room 14→15 migration-test compilation, and backend
  Shanghai-time/range validation tests.

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
