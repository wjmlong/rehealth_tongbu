---
slug: restore-demo-attribution-live-data
status: approved
intent: clear
review_required: false
pending-action: write .omo/plans/restore-demo-attribution-live-data.md
approach: Use commit 7008f2f from D:/rehealth_demo/Android-apk as the visual contract, preserve the current D:/rehealthAI Android real MR11/Room/Jeecg/PIAS architecture, add pure UI mapping/state in Android-apk, and restore the target attribution card anatomy/interactions without hardcoded demo production values; verify in emulator, commit all legitimate dirty code, and push codex/real-device.
---

# Draft: restore-demo-attribution-live-data

## Components (topology ledger)
<!-- Lock the SHAPE before depth. One row per top-level component that can succeed or fail independently. -->
<!-- id | outcome (one line) | status: active|deferred | evidence path -->

## Open assumptions (announced defaults)
<!-- Record any default you adopt instead of asking, so the user can veto it at the gate. -->
<!-- assumption | adopted default | rationale | reversible? -->

## Findings (cited - path:lines)
Correct Git root is `D:/rehealthAI`, branch `codex/real-device`, initially dirty in root sync/report/LFS artifacts but Android product source clean.
Current attribution is `Android-apk/app/src/main/java/com/rehealth/genie/ui/ReHealthApp.kt:1313` and consumes canonical remote feature evaluation but not full target layout.
Current repository already contains `PiasApiClient.kt`, risk history persistence, authenticated Jeecg APIs, interventions API, MR11/Room state, and existing tests.
Historical `D:/rehealth_demo/Android-apk` commit `7008f2f` is the exact target UI source; do not cherry-pick its mock data.

## Decisions (with rationale)
Edit the canonical D:/rehealthAI repository only.
Treat existing uncommitted root files as user work and never overwrite/revert them.
Restore layout via mapper/state layer, not via backend duplication or wholesale file copy.
Use live/real/replay provenance labels and honest empty states.

## Scope IN
Android attribution mapper/state/UI/tests/docs, emulator QA, audit and Git push of all legitimate non-secret changes.

## Scope OUT (Must NOT have)
No other UI redesign, no hardcoded demo production data, no backend duplication, no JWT/APK/local.properties in Git, no destructive Git commands.

## Open questions
None; user explicitly requested implementation and push.

## Approval gate
status: approved
approved-by: explicit corrective implementation and push request
next-action: write and execute plan
<!-- When exploration is exhausted and unknowns are answered, set status: awaiting-approval. -->
<!-- That durable record is the loop guard: on a later turn read it and resume at the gate instead of re-running exploration. -->
target-ui | recover period, score, forecast, behavior, factors, and plan component anatomy | active | screenshots 4-6 / commit 7008f2f
real-data | map current canonical risk, risk history, PIAS and intervention/activity states | active | Android-apk current sources
states | retain target card shells for loading/empty/error/retry | active | Compose attribution screen
qa | build/test/install/capture all target regions and animation | active | emulator-5554
delivery | preserve existing dirty work, audit, commit and push legitimate code | active | D:/rehealthAI codex/real-device
target | screenshots 4-6 and 7008f2f are exact structural reference | explicitly supplied | reversible
mock policy | no historical fixed values return to production path | repository rule | reversible
dirty work | preserve and later review/commit all legitimate current changes | user requested all code synced | reversible
