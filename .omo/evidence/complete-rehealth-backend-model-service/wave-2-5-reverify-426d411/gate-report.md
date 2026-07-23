# Wave 2-5 fixed re-verification — `426d411`

## recommendation

APPROVE

## blockers

None.

## originalIntent

Deliver Todos 2-5 as executable platform foundations: portable/private deployment topology with truthful dependency-outage behavior; backward-compatible telemetry contracts; fail-closed service identity and device ownership; and explicit safe runtime modes with no embedded provider credentials.

## desiredOutcome

The T2-T5 gates must execute on the pinned product state, exercise the named positive and adversarial cases, and prove that topology/header rules, legacy telemetry, authorization denials, and production configuration boundaries behave as specified.

## userOutcomeReview

The prior blockers are fixed on HEAD `426d411520da28b25a35d658c3d5d876678f1ade`. All executable T2-T5 gates passed. The three uncommitted product files contain documentation and external secret-file wiring only; no credential value was found. No Android/Compose product file was modified.

## per-task verdicts

### Todo 2 — CONFIRMED

- Portable topology gate passed with Windows venv Python and WSL Python.
- Topology artifacts report only the edge as published and validate both Gateway route header-removal filters.
- `topology-failures` passed all four bounded live transitions. Every case records `probe_before.available=true`, `probe_after.available=false`, and `runtime_verified=true`.
- Timescale/auth outages make ingest unready; Kafka leaves ingest ready with publisher degraded; bad model hash makes model readiness false.
- Evidence: `t2-topology-windows.json`, `t2-topology-wsl.json`, `t2-failures-windows.log`, `exits.txt`.

### Todo 3 — CONFIRMED

- Legacy suite: 7 tests, 0 failures/errors.
- Focused rejection suite: 5 tests, 0 failures/errors.
- Evidence: `t3-legacy.log`, `t3-rejection.log`, `exits.txt`.

### Todo 4 — CONFIRMED

- Reactor-safe happy command built the four-module reactor and ran 8/8 authorization tests successfully.
- `-Dcases=revoked,cross_device,unbound,spoofed_header,auth_unavailable` ran exactly five selected cases (8 discovered, 3 skipped), with zero failures/errors.
- Direct source inspection confirms the five selected tests assert 401/403/503 as applicable, stable denial codes, and unchanged device-binding row count. The selector rejects unknown names rather than silently ignoring them.
- Evidence: `t4-happy.log`, `t4-negative.log`, `exits.txt`; source `InternalIdentityAndDeviceAuthorizationIT.java`.

### Todo 5 — CONFIRMED

- Valid matrix passed for production, staging, development, and demo on Windows and WSL.
- Invalid matrix rejected all four named cases with exact codes: `ATTRIBUTION_MODE_UNSAFE`, `SOFTWARE_DB_REQUIRED`, `SECURE_URL_REQUIRED`, and `EMBEDDED_SECRET_FORBIDDEN`.
- Actual model-service boundary tests passed: 21 tests, including protected-mode HTTPS URL, credential-file, embedded-secret, and startup checks.
- The first root-directory pytest attempt exited 2 because the suite expects the model-service working directory; the corrected documented invocation from `model-service/` passed and is the substantive result.
- Evidence: `t5-valid-windows.log`, `t5-invalid-windows.log`, `t5-valid-wsl.log`, `t5-invalid-wsl.log`, `t5-model-pytest-model-cwd.log`, `exits.txt`.

## provider-secret and modified-file audit

- `backend/deploy/rehealth/docker-compose.yml` mounts `provider_credential` from `./secrets/provider_credential` and supplies only the file path through `REHEALTH_PROVIDER_CREDENTIAL_FILE`.
- The protected model-service URL default is HTTPS and contains no embedded credentials.
- `backend/deploy/rehealth/README.md` truthfully scopes `runtime_verified` to the bounded dependency harness.
- `model-service/README.md` documents the protected-mode HTTPS/credential-file requirements and embedded-secret rejection.
- Diff secret-value scan: no matches.
- Only tracked path under `backend/deploy/rehealth/secrets/` is `README.md`; no provider credential file is tracked.
- Evidence: `three-product-files.diff`, `secret-value-scan.txt`, `tracked-secret-paths.txt`.

## direct remove-ai-slops / programming pass

- The former static/tautological T2 failure gate has been replaced by observable socket lifecycle transitions; it no longer merely restates expected JSON.
- T3 tests exercise deserialization/validation behavior rather than requested deletion or prose.
- T4 selectors materially alter execution (five run, three skipped), and each denial test asserts externally relevant status/code plus no binding mutation.
- T5 model tests exercise the actual runtime configuration boundary. The matrix output is not used alone to claim the Python boundary.
- No excessive extraction, deletion-only test, implementation-mirroring expected-value derivation, embedded secret, or scope-expanding production abstraction was found that violates a Todo 2-5 criterion.

## checked artifact paths

- `.omo/plans/complete-rehealth-backend-model-service.md`
- `.omo/evidence/complete-rehealth-backend-model-service/wave-2-5-verifier-b788ed8/gate-report.md`
- `.omo/evidence/complete-rehealth-backend-model-service/wave-2-5-fix-b788ed8/DONE_CLAIM.md`
- `backend/qa/rehealth_stack_gate.py`
- `backend/deploy/rehealth/README.md`
- `backend/deploy/rehealth/docker-compose.yml`
- `backend/deploy/rehealth/gateway/rehealth-routes.json`
- `backend/contracts/telemetry/**`
- `backend/jeecg-boot/jeecg-boot-module/jeecg-module-rehealth/src/test/java/org/jeecg/modules/rehealth/auth/InternalIdentityAndDeviceAuthorizationIT.java`
- `model-service/app/runtime_config.py`
- `model-service/tests/test_runtime_config.py`
- `model-service/tests/test_api.py`
- All logs and machine-readable artifacts in this report directory.

## exact evidence gaps

None for the requested fixed Todo 2-5 re-verification. T2 topology happy remains explicitly static (`runtime_verified=false`); this is truthful and was not used as full-stack deployment proof. The requested bounded outage transition is separately runtime-verified.

## cleanup

- Final HEAD: `426d411520da28b25a35d658c3d5d876678f1ade`.
- No task-owned WSL/Maven/Python/socket failure-harness process remained.
- Product files were not edited by this reviewer; no commit or push was performed.
