# D Android Network Sync Prompt

Read AGENTS.md, ENGINEERING.md, and CODEX_ORCHESTRATION.md.

Workstream: D_android_network_sync.

Goal:
Add backend API client and offline upload queue.

Tasks:
1. Add Retrofit/OkHttp/Moshi if needed.
2. Define ReHealthApi DTOs.
3. Implement UploadQueueEntity/Dao and MeasurementSyncWorker.
4. Ensure all uploads are local-first and retryable.
5. Add token interceptor.
6. Do not place network calls inside BLE collection.
7. Add tests or manual validation notes.
