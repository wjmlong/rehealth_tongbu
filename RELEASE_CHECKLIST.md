# ReHealth MVP Release Checklist

Date: 2026-07-09
Gate: G1 acceptance audit, not final release approval.

## Build Status

- Android `testDebugUnitTest`: PASS on 2026-07-09 with `JAVA_HOME=D:\Android_Studio\jbr`.
- Android `assembleDebug`: PASS on 2026-07-09 with `JAVA_HOME=D:\Android_Studio\jbr`.
- Android `git diff --check`: PASS, with CRLF normalization warnings only.
- Backend `jeecg-module-rehealth -am package -DskipTests`: PASS.
- Backend `jeecg-system-start -am package -DskipTests`: PASS.
- Model-service `python -m pytest`: normal `python` command failed due to environment stub; bundled Python PASS with 12 tests.
- Model-service `python -m compileall app`: normal `python` command failed due to environment stub; bundled Python PASS.

## Tests

- Android unit tests: PASS from current dirty Android worktree.
- Backend tests: skipped by requested Maven `-DskipTests`; package compile passed.
- Model-service tests: 12 passed.
- Physical MRD ring QA: BLOCKED, no locked-screen/background real-device evidence recorded in this audit.

## Branches And Push Status

| Repo | Branch | State | Push status |
| --- | --- | --- | --- |
| `Android-apk` | `work/C_android_feature_extractor` | Clean; D1 is committed as `5528e22` | Should be pushed or have upstream set if this work branch is intended for review |
| `backend` | `work/E_backend_mobile_api` | Clean | Should be pushed or have upstream set if not already on remote |
| `model-service` | `main...origin/main` | Clean | No push needed |
| `rehealth-android` | `main...origin/main` | Clean | No push needed |

## Known Blockers

- Android D1 source is now committed, but `Android-apk/codex-runs/2026-07-09/D_status.md` is still missing even though a root `codex-runs/2026-07-09/D_status.md` exists.
- B1 has no physical MRD ring evidence for background collection under lock screen, killed app, Bluetooth off, and permission denied cases.
- B1 exposes service/ViewModel APIs but has no production UI toggle or Android 13 notification permission UX.
- Current MRD BLE code logs raw packet hex / parsed JSON in `MrdBleRingRepository`; this is not production-release safe for health data.
- Legacy `ReHealthBackendClient` still posts `/rehealth/mobile/ring/snapshots` from `RingViewModel`; it is not the D1 typed feature-evaluate path and should be retired or explicitly dev-gated before release.
- `/measurements/batch` is E2-pending and not durable telemetry sync.
- Backend E1 has no software_db tables/mappers and no hardware_db/MQ writer.
- Model-service uses `MockRiskScorer` with `is_mock=true`; no production clinical model is validated.

## Medical Safety Wording

- Required: all risk/intervention text must state conservative wellness support only.
- Required: no diagnosis, treatment guarantee, or replacement of clinician review.
- Current model-service contract includes `medical_disclaimer`.
- Release blocker: Android UI must not present `MockRiskScorer` output as production medical scoring.

## Privacy And Security Checks

- No raw health data, access tokens, phone numbers, or identifiers in production logs.
- No raw PPG/RRI or raw BLE packet upload by default.
- No secrets committed in new G1 docs.
- Android BuildConfig token default is empty in documented D1 flow.
- Backend E1 uses normal auth for production-style mobile endpoints; only `/rehealth/mobile/health` is `@IgnoreAuth`.
- Release blocker: raw BLE packet logging must be removed or build-gated before production.

## Raw Signal Upload Policy

- D1 typed `ReHealthApi` intentionally excludes `POST /rehealth/mobile/measurements/batch`.
- D1 typed feature evaluation does not upload raw signal chunks.
- Legacy snapshot client sends signal metadata, not signal payload bytes, but it still uses an obsolete `/ring/snapshots` path and must not be treated as production telemetry sync.

## Mock Model Policy

- `MockRiskScorer` is acceptable for MVP integration testing only.
- Any UI, backend, or release note must preserve `is_mock=true`.
- No pilot or production claim may be based on mock scoring.

## Release Decision

Current gate result: BLOCKED FOR FINAL RELEASE.

Allowed next work: engineering cleanup and E2 preparation. Do not approve final release until Android D status is restored, B1 real-device QA evidence exists, raw logging is removed/gated, and E2 durable ingestion is implemented or explicitly deferred from scope.
