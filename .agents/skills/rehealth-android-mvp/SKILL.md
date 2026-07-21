---
name: rehealth-android-mvp
description: Use when implementing or reviewing the ReHealth Android MVP: real MRD ring collection, Room persistence, CVD 16-feature extraction, backend upload, model-service risk scoring, intervention feedback, and QA release gates. Do not use for unrelated Android apps.
---

# ReHealth Android MVP Skill

## Purpose

Help Codex reliably execute the ReHealth Android MVP workflow.

The target vertical slice is:

```text
Onboarding → MRD ring binding → real data collection → Room persistence
→ CVD 16-feature vector → upload queue → backend API → model-service risk score
→ intervention recommendation → user feedback → trend/QA report
```

## Required context

Before acting, read:

1. `AGENTS.md`
2. `ENGINEERING.md`
3. `CODEX_ORCHESTRATION.md`
4. Relevant repository README files
5. Current build files and touched modules

## Golden architecture

```text
Android-apk:
  BLE / MRD SDK / Room / Compose / local features / upload queue

backend:
  JeecgBoot account, device, mobile API, admin/doctor operations

model-service:
  Python FastAPI, CatBoost/SHAP/LLM/attribution

rehealth-algorithms:
  HealthAgent simulation/research only
```

## Hard rules

- Never block BLE collection on network.
- Never use mock output as if it were real production output.
- Never log raw health data or tokens.
- Keep medical advice conservative.
- Add or update QA docs when user-visible behavior changes.
- Run build/tests when possible.

## Workflow

### Step 1: Inspect

Summarize existing files and state.

### Step 2: Plan

Write a short plan with target files and risks.

### Step 3: Implement

Make the smallest useful vertical slice.

### Step 4: Validate

Run relevant commands:

```bash
./gradlew assembleDebug
pytest
mvn test
```

Use the command relevant to the current repo. If not possible, explain exactly why.

### Step 5: Report

Final response must include:

```text
Changed files
What changed
Tests/builds run
Manual QA
Risks
Next recommended task
```

## MVP acceptance checklist

- App builds.
- User can bind ring.
- Real data is persisted in Room.
- Background collection works.
- Feature vector can be generated.
- Offline upload queue works.
- Backend receives batch.
- model-service returns risk score.
- App displays risk and intervention.
- User feedback is saved and synced.
