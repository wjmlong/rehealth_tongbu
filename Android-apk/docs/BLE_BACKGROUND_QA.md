# BLE Background Collection QA

Last updated: 2026-08-24

## Scope

B1 adds a local-first foreground service for low-frequency wearable collection and a WorkManager recovery job. The service only calls the routed `RingRepository.syncAll()` path, which selects one active MRD, RWFit, or HBand Provider and persists parsed measurements, sleep, activity, and signal chunks through Room. It does not call backend APIs, model-service, `/measurements/batch`, or raw PPG/RRI upload.

For a bound Bluetooth device, the production app does not start this service merely because the
Main stage is entered. Background collection must be explicitly enabled through the service or
ViewModel API; logout stops it. Users enable or stop it from “我的 → 设备绑定 → 后台自动采集”.
Android 13+ requests notification permission before the enable action reaches the service. The
app-facing APIs remain:

- `RingForegroundService.start(context)`
- `RingForegroundService.stop(context)`
- `RingViewModel.startBackgroundCollection(context)`
- `RingViewModel.stopBackgroundCollection(context)`

## Manual QA Checklist

1. Fresh install the debug APK.
2. Leave Bluetooth off and start background collection from a debug call path; verify the foreground notification appears and reports Bluetooth is off.
3. Deny BLE permissions, start background collection, and verify collection is paused without crashing.
4. Grant `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT` on Android 12+ or location permission on Android 11 and below.
5. Scan for the device selected by the current MRD/RWFit `productCode` from the existing device-binding screen.
6. Connect the ring from the existing device-binding screen.
7. Restart the app and confirm the same encrypted active binding is proactively reconnected without
   scanning or starting an immediate ring collection; logs and cloud payloads must not expose the
   raw address.
8. Confirm manual heart-rate measurement still works and the latest row appears in Room.
9. Confirm manual SpO2 measurement still works and the latest row appears in Room.
10. For MRD, confirm manual BP measurement if firmware supports it. For RWFit,
    confirm HRV only when the capability flag is present; BP/temperature/stress
    are outside the current RWFit Provider.
11. Open “我的 → 设备绑定 → 后台自动采集” and enable it. On Android 13+, grant the notification
    permission and verify the page changes to “已启用”.
12. Put the app in the background.
13. Lock the screen.
14. Wait for one conservative interval, currently 15 minutes.
15. Confirm Room receives new local data in `ring_measurements`, `ring_sleep_sessions`, `ring_activities`, or `ring_signal_chunks`.
16. Stop the service and confirm the foreground notification disappears.
17. Disconnect the ring and restart background collection; verify it retries later without a fast loop.
18. Reconnect the ring and verify the next interval can persist local records.
19. Kill the app process while collection is active.
20. Reopen the app; verify Main entry does not start an immediate collection or duplicate loop.
    If the service was explicitly enabled before process death, verify WorkManager recovery remains
    scheduled and resumes only on its normal interval.
21. Search logs/network inspector and verify B1 performs no backend upload, model-service call, `/measurements/batch` call, or raw PPG/RRI upload.
22. After clearing app data, start collection before scanning/binding. Confirm no
    hardcoded-device connection or automatic scan occurs and no zero/simulated
    health row is inserted.

## Expected Behavior

- Collection interval is at least 15 minutes.
- If foreground manual sync is already in progress, the background service skips that cycle.
- Missing permission, unsupported Bluetooth, and Bluetooth-off states are reported in the notification instead of crashing.
- The service uses a persistent low-importance notification with a Stop action.
- WorkManager is recovery-only and does not collect BLE data directly.
- `syncAll()` reconnects only an existing encrypted binding and never scans the
  surrounding environment when no address is bound.
- Foreground and UI operations pass through the same `ActiveRingRepository`
  mutex. HBand additionally serializes SDK commands and disconnects on coroutine
  cancellation because its history API has no callback-removal operation.
- HBand process recovery reads only the four real demographics required by
  `syncPersonInfo` from encrypted, user-hash-scoped preferences; it never waits
  for network profile access and never substitutes Demo values.
- Only the active Provider may collect. A missing active binding address causes
  an empty retryable cycle, not a fixed-address connection or fabricated data.
- Android 12+ BLE platform calls re-check `BLUETOOTH_SCAN` and
  `BLUETOOTH_CONNECT` immediately before use. Revoking permission during a scan
  or connection attempt must return an empty/error state without crashing.
- Changing the measurement interval while collection is enabled must NOT
  restart the foreground service. The running loop re-reads the interval every
  round, so the new preset applies on the next cycle. Rapidly tapping interval
  presets must not crash: a previous stop/start design left a
  `startForegroundService()` request pending while `stopSelf()` tore the
  service down, and the system killed the process after 5 seconds with
  `RemoteServiceException$ForegroundServiceDidNotStartInTimeException`
  (logcat marker: "Bringing down service while still waiting for start
  foreground"). `startForeground()` is also called first in
  `onStartCommand(ACTION_START)` so the system watchdog is always satisfied
  before any other start work.

## Emulator Regression (API 31+)

1. Install the debug APK on an API 31 or newer emulator.
2. Revoke `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT` with `pm revoke`.
3. Launch the app and open the device-binding page.
4. Trigger scan without granting the permission prompt; verify the app remains
   alive and exposes the permission-required state.
5. Grant both permissions with `pm grant`, relaunch, and trigger scan again.
6. An emulator may return no wearable devices, but the scan path must complete without
   `SecurityException`, fatal exception, or ANR.

## Known Follow-Ups

- Add device-specific QA evidence from a real locked-screen run.
