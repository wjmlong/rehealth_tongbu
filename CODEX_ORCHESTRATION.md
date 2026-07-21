# CODEX_ORCHESTRATION.md

> How to run Codex for the ReHealth Android MVP.

## 1. Recommended workflow

Use a hybrid structure:

```text
One root/orchestrator conversation:
  - planning
  - repo understanding
  - spawning read-only exploration/review subagents
  - collecting status

Separate implementation conversations:
  - one per workstream
  - one branch per workstream
  - one PR/commit chain per workstream
```

Do not ask seven implementation subagents to edit the same repository in the same turn unless the work is read-only or extremely well-isolated. Parallel analysis is good. Parallel code edits to overlapping Android files are conflict-prone.

## 1.1 Minimal Change Principle

**All workstreams and subagents must follow the minimal change rule**:

- **Update existing files** instead of creating duplicates
- **Reuse existing naming patterns** (e.g., `ACCEPTANCE_REVIEW_2026-07-XX.md`, not `NEW_ACCEPTANCE_REVIEW.md`)
- **Update existing status files** instead of creating new ones
- **Append to existing docs** instead of creating parallel documentation
- Only create **new files when necessary** for genuinely new content

**Examples**:
- ✅ Update `ACCEPTANCE_REVIEW_2026-07-16.md` with new appendix
- ❌ Create `ACCEPTANCE_REVIEW_2026-07-16_v2.md`
- ✅ Update `codex-runs/2026-07-09/E1_status.md` with completion notes
- ❌ Create `codex-runs/2026-07-16/E1_final_status.md`
- ✅ Append progress to `ORCHESTRATOR_SESSION_2026-07-16.md`
- ❌ Create `ORCHESTRATOR_SESSION_2026-07-16_EVENING.md`

**Rationale**: Minimize file proliferation, maintain clear audit trail, prevent duplicate documentation.

## 2. Recommended 7 workstreams

```text
A_android_build
B_android_ble_background
C_android_feature_extractor
D_android_network_sync
E_backend_mobile_api
F_model_service
G_qa_release
```

## 3. Folder layout for prompts and logs

Suggested local folder:

```text
.codex/
  prompts/
    00_orchestrator.md
    A_android_build.md
    B_android_ble_background.md
    C_android_feature_extractor.md
    D_android_network_sync.md
    E_backend_mobile_api.md
    F_model_service.md
    G_qa_release.md
  agents/
    android-builder.toml
    android-ble-service.toml
    android-feature-extractor.toml
    android-sync.toml
    backend-mobile-api.toml
    model-service.toml
    qa-release.toml
codex-runs/
  2026-xx-xx/
    A_status.md
    B_status.md
    ...
```

## 4. Master orchestration prompt

Use this in the root Codex conversation:

```text
Read AGENTS.md, ENGINEERING.md, and CODEX_ORCHESTRATION.md.

I want to run the ReHealth Android MVP work as coordinated parallel workstreams.

First, do not edit code. Inspect the repository and produce:
1. current repo map;
2. build/test commands;
3. files likely touched by each workstream;
4. conflict risks between workstreams;
5. recommended branch order.

Then spawn read-only subagents, one per workstream:
A Android build health
B BLE/background collection
C feature extractor
D network/offline sync
E backend mobile API
F model-service
G QA/release

Each subagent must return:
- current state
- target files
- implementation plan
- risks
- validation commands
- estimated order

After all subagents return, consolidate into a single implementation queue.
Do not edit files until I approve the queue.
```

## 5. Implementation prompt template

Use this for each separate implementation conversation:

```text
You are working on workstream: <WORKSTREAM_NAME>.

Read AGENTS.md, ENGINEERING.md, and CODEX_ORCHESTRATION.md.
Only work on this workstream.

Constraints:
- Do not solve unrelated tasks.
- Avoid overlapping files listed as owned by other workstreams unless necessary.
- If you must touch shared files, explain why before editing.
- Implement the smallest useful vertical slice.
- Add/update tests where practical.
- Run relevant build/tests.
- Commit your changes if possible.
- Update codex-runs/<date>/<WORKSTREAM>_status.md with:
  - what changed
  - tests run
  - known risks
  - next recommended task

Definition of done:
1. changed files listed
2. build/test result shown
3. manual QA checklist included
4. known risks listed
5. git status checked
```

## 6. Workstream ownership

### A: Android build health

Owns:

```text
settings.gradle.kts
build.gradle.kts
app/build.gradle.kts
BUILD_NOTES.md
```

Avoids:
- BLE logic
- feature extraction
- backend networking

### B: Android BLE/background collection

Owns:

```text
app/src/main/AndroidManifest.xml
app/src/main/java/com/rehealth/genie/service/
app/src/main/java/com/rehealth/genie/ring/
```

May touch:
- ReHealthApplication
- RingViewModel

Avoids:
- network upload
- model scoring

### C: Android feature extractor

Owns:

```text
app/src/main/java/com/rehealth/genie/features/
app/src/test/
```

May touch:
- DAO read methods
- health interview persistence

Avoids:
- BLE internals
- remote API implementation

### D: Android network sync

Owns:

```text
app/src/main/java/com/rehealth/genie/network/
app/src/main/java/com/rehealth/genie/sync/
```

May touch:
- app/build.gradle.kts
- AppDatabase

Avoids:
- BLE command protocol
- feature math except DTO mapping

### E: Backend mobile API

Owns:

```text
backend ReHealth mobile controller/service/entity/mapper
OpenAPI/Swagger docs
```

Avoids:
- Python scoring implementation

### F: model-service

Owns:

```text
model-service/
```

Avoids:
- Android code
- JeecgBoot internals except API contract docs

### G: QA/release

Owns:

```text
QA_TEST_PLAN.md
RELEASE_CHECKLIST.md
MANUAL_TESTING.md
```

May inspect everything.
Should avoid code edits unless fixing obvious doc/build references.

## 7.验收机制

Each PR/branch must answer:

```text
Can I install it?
Can I collect real data?
Can I inspect stored data?
Can I generate features?
Can I upload or queue upload?
Can I get risk score?
Can I show intervention?
Can I record feedback?
What fails when network/Bluetooth/permission is broken?
```

## 8. When to use subagents

Use subagents for:

- codebase exploration
- security review
- QA review
- comparing approaches
- independent workstreams with no shared files

Avoid subagents for:

- one fragile migration
- one UI refactor touching many shared files
- BLE bug requiring sequential device testing
- tasks where every agent will edit MainActivity/ReHealthApp/AppDatabase simultaneously

