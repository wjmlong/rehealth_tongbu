# BLE Background Collection QA

Last updated: 2026-07-09

## Scope

B1 adds a local-first foreground service for low-frequency MRD ring collection and a WorkManager recovery job. The service only calls the existing `RingRepository.syncAll()` path, which persists parsed measurements, sleep, activity, and signal chunks through Room. It does not call backend APIs, model-service, `/measurements/batch`, or raw PPG/RRI upload.

The production UI toggle is not part of B1. The app-facing APIs are:

- `RingForegroundService.start(context)`
- `RingForegroundService.stop(context)`
- `RingViewModel.startBackgroundCollection(context)`
- `RingViewModel.stopBackgroundCollection(context)`

## Manual QA Checklist

1. Fresh install the debug APK.
2. Leave Bluetooth off and start background collection from a debug call path; verify the foreground notification appears and reports Bluetooth is off.
3. Deny BLE permissions, start background collection, and verify collection is paused without crashing.
4. Grant `BLUETOOTH_SCAN` and `BLUETOOTH_CONNECT` on Android 12+ or location permission on Android 11 and below.
5. Scan for the MRD ring from the existing device-binding screen.
6. Connect the ring from the existing device-binding screen.
7. Confirm manual heart-rate measurement still works and the latest row appears in Room.
8. Confirm manual SpO2 measurement still works and the latest row appears in Room.
9. Confirm manual BP measurement still works if the ring firmware supports it.
10. Start background collection using the service/ViewModel API.
11. Put the app in the background.
12. Lock the screen.
13. Wait for one conservative interval, currently 15 minutes.
14. Confirm Room receives new local data in `ring_measurements`, `ring_sleep_sessions`, `ring_activities`, or `ring_signal_chunks`.
15. Stop the service and confirm the foreground notification disappears.
16. Disconnect the ring and restart background collection; verify it retries later without a fast loop.
17. Reconnect the ring and verify the next interval can persist local records.
18. Kill the app process while collection is active.
19. Reopen the app; verify WorkManager recovery is scheduled and no duplicate aggressive loops appear.
20. Search logs/network inspector and verify B1 performs no backend upload, model-service call, `/measurements/batch` call, or raw PPG/RRI upload.

## Expected Behavior

- Collection interval is at least 15 minutes.
- If foreground manual sync is already in progress, the background service skips that cycle.
- Missing permission, unsupported Bluetooth, and Bluetooth-off states are reported in the notification instead of crashing.
- The service uses a persistent low-importance notification with a Stop action.
- WorkManager is recovery-only and does not collect BLE data directly.

## Known Follow-Ups

- Add an in-app background collection toggle in a UI-owned workstream.
- Add device-specific QA evidence from a real locked-screen run.
- Consider a user-visible notification permission prompt on Android 13+ before enabling background collection.
