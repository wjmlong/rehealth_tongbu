# B Android BLE Background Prompt

Read AGENTS.md, ENGINEERING.md, and CODEX_ORCHESTRATION.md.

Workstream: B_android_ble_background.

Goal:
Make real MRD ring data collection survive app background/lock screen as much as Android allows.

Tasks:
1. Inspect MrdBleRingRepository, RingViewModel, ReHealthApplication, AndroidManifest.
2. Implement RingForegroundService with persistent notification.
3. Add WorkManager fallback for periodic sync.
4. Handle Android 12+ Bluetooth permissions.
5. Add reconnect/retry without blocking UI.
6. Ensure all data continues to write Room first.
7. Add manual real-device checklist.

Do not implement backend sync or model scoring.
