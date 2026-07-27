# AGENTS.md

> Repository-wide instructions for Codex and other coding agents.

## Project identity

This repository is part of ReHealth AI / 睿禾健康.

Current engineering goal: convert the Android demo into a usable MVP that supports real device data collection, local persistence, CVD feature extraction, backend upload, cloud risk scoring, intervention generation, and feedback loop.

Read `README.md` first for the repository map, current service boundaries, core data flows, development commands, and documentation index.
Read `ENGINEERING.md` before making any non-trivial change.
Read `STATUS.md` for the current implementation and release checkpoint. Historical acceptance snapshots are archived under `docs/archive/acceptance/` and must not be treated as current state.
For Android-rule reference docs, see the canonical paths listed in `Android-apk/docs/REHEALTH_INTEGRATION_CONTRACT.md` (rewritten 2026-07-10).
For telemetry status and remaining QA, read `Android-apk/docs/D2_TELEMETRY_SYNC_PLAN.md`. D2, E2.1, and E1.1 are implemented; do not reopen them as unstarted work.

## Current architecture

Preferred architecture:

```text
Android-apk              Android app: BLE/MRD ring, Room, Compose UI, local feature extraction, upload queue
backend/device-service   Hardware telemetry ingress, TimescaleDB, transactional Outbox, Kafka publication
backend/jeecg-boot       JeecgBoot user/device/account/admin/doctor/operations APIs and software_db
backend/contracts        Versioned mobile/telemetry/event contracts and architecture decisions
model-service            Python FastAPI: CatBoost/SHAP/LLM/attribution
rehealth-algorithms      Model training, HealthAgent/PIAS simulation, and algorithm research
```

Do not put CatBoost, SHAP, LLM, or causal attribution directly into the Android app.
Do not put model inference directly into JeecgBoot Java unless explicitly requested.

## Working rules

1. Make the smallest safe change that moves the MVP forward.
2. Prefer real implementation over mock. If mock is unavoidable, name it clearly as mock.
3. Keep BLE collection independent from network upload.
4. Always persist collected health data locally before uploading.
5. Do not block BLE collection on backend availability.
6. Do not add production dependencies without explaining why.
7. Do not log raw health data, access tokens, phone numbers, or identifiers in production logs.
8. Medical advice must be conservative and must not claim to diagnose or replace doctors.
9. For Android changes, preserve minSdk/targetSdk/Compose compatibility unless necessary.
10. For database schema changes, add migrations or explicitly explain why destructive migration is acceptable only in local dev.

## Mandatory first steps per task

Before coding:

1. Inspect relevant files.
2. Summarize the existing implementation.
3. Write a short implementation plan.
4. Identify expected tests/build commands.
5. Then edit files.

## Mandatory finish steps per task

Before final response:

1. Run relevant build/tests when possible.
2. If a command cannot run, explain why.
3. Check `git status`.
4. Commit changes if the task produced code and the environment allows commits.
5. Leave clear notes in a relevant markdown file when requested.
6. Perform the documentation impact check in `README.md` and update every affected project, contract, deployment, QA, or release document. If no documentation change is needed, state why in the final response.

## Documentation maintenance

`README.md` is the repository-level project entry point. Keep it concise and current;
put detailed contracts and decisions in their canonical module documents and link them
from the root README instead of duplicating them.

Documentation updates are mandatory when a change affects any of the following:

- module/service ownership, repository structure, or the end-to-end data flow;
- API paths, authentication, DTO fields, completion semantics, or event schemas;
- Room, MySQL, TimescaleDB, queue, or retention schemas and migrations;
- supported devices, BLE/vendor protocols, metrics, collection, or sync behavior;
- Kafka topics, message guarantees, consumers, or broker replacement decisions;
- model inputs/outputs, model versions, Mock behavior, attribution, or health-agent safety;
- ports, environment variables, secrets, containers, deployment topology, or build/test commands;
- user-visible behavior, permissions, privacy posture, QA steps, or release gates.

Use the documentation mapping table in `README.md` to select the canonical files.
Do not create a parallel status or architecture document when an existing canonical
document can be updated. Documentation-only changes still require link/format checks
and a final `git status` review.

## Android commands

From `Android-apk` root:

```powershell
.\gradlew.bat assembleDebug
```

or on Unix shell:

```bash
./gradlew assembleDebug
```

Debug APK path:

```text
app/build/outputs/apk/debug/app-debug.apk
```

## Android coding expectations

- Kotlin first.
- Compose UI should keep business logic outside composables.
- BLE operations should stay in repository/service layer.
- ViewModel should orchestrate UI state, not own low-level Bluetooth details.
- Room writes must be explicit and resilient.
- Long-running collection should use Foreground Service and WorkManager.
- Network sync must use a queue and retry strategy.

## Backend expectations

- JeecgBoot remains the admin/account/business orchestration layer.
- New ReHealth mobile APIs should be isolated in a `rehealth` module/package.
- Reuse existing auth/permission mechanisms where possible.
- Keep model-service communication behind a client abstraction.

## model-service expectations

- Python FastAPI.
- Typed request/response schemas.
- Unit tests for feature validation and scoring response shape.
- Model version included in every response.
- No raw PII in logs.
- Health check endpoint required.

## Definition of Done

A task is done only when:

- Code compiles or the failure is documented.
- Tests/build commands are run or skipped with reason.
- New behavior has at least basic validation.
- Any changed API is documented.
- Any changed schema has migration strategy.
- Final response includes changed files, tests, risks, and next step.
