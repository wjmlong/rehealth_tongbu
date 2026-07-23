# Wave 2–5 adversarial verification — b788ed80006e796a7802d52302b1ef51ff329deb

## Recommendation

REJECT

## Original intent

Ship Todos 2–5 as usable platform foundations: a reproducible and private deployment topology with exercised readiness failures; a reusable backward-compatible telemetry contract; fail-closed internal identity/device ownership; and explicit safe runtime modes in both Jeecg and model-service.

## Desired outcome

Each exact T2–T5 command exits zero at the pinned SHA, its negative command exercises the named failure rather than restating a fixture, and production configuration cannot expose internal services, accept spoofed ownership, silently use Demo/Mock, or start without required real dependencies.

## Per-task verdicts

### Todo 2 — REJECT

- **Violated criterion T2.AC1 / T2.QA:** The happy gate did not complete. The exact Windows command cannot execute because `python` is a nonfunctional Windows Store alias (`t2-happy.exit.txt`, exit 9009). A supplemental WSL `python3` run also fails because `compose_config()` resolves `/mnt/d/...` and passes a malformed `D:\mnt\d\...` path to Windows Docker (`t2-happy-python3.txt`).
- **Violated criterion T2.AC2 / T2.QA-failure:** `topology-failures` does not kill or probe Timescale, auth, Kafka, or the model verifier. It reads `backend/deploy/rehealth/failure-contract.json` and echoes the selected declarations as `"passed": true` (`t2-failure-python3.txt`; `backend/qa/rehealth_stack_gate.py`, `run_topology_failures`).
- **Violated criterion T2.QA-happy:** Even a successful static topology run would explicitly report `"runtime_verified": false`; it cannot prove health/readiness or that the admin SPA is served through the edge.
- **Violated criterion T2 gateway spoofing dependency:** Edge config uses `proxy_hide_header`, which removes response headers, not spoofable incoming request headers; the gateway route seed has no request-header removal filter (`backend/deploy/rehealth/edge/nginx.conf`, `backend/deploy/rehealth/gateway/rehealth-routes.json`).

### Todo 3 — REJECT

- **Violated criterion T3.AC1 / T3.QA-happy:** The exact Maven happy gate reproduced in WSL runs 7 tests and fails `TelemetryLegacyCompatibilityTest.preservesLegacyAndroidShapeWhenFixtureRoundTrips`. The captured Android fixture is rejected with `raw_signal.disabled` at path `quality` (`t3-happy-wsl.txt`, lines containing the assertion failure).
- The focused rejection suite passes 5/5 (`t3-failure-wsl.txt`), but rejection-only coverage does not compensate for broken legacy compatibility.
- Exact Windows invocation is unavailable because Maven/Java are absent from PATH (`t3-happy.txt`, `t3-failure.txt`); the WSL reproduction is the substantive product result.

### Todo 4 — REJECT

- **Violated criterion T4.AC / T4.QA:** Both exact Maven commands reproduced in WSL fail before test execution because the command builds only the leaf module and cannot resolve reactor sibling `jeecg-boot-base-core:3.9.2` (`t4-happy-wsl.txt`, `t4-failure-wsl.txt`). There is therefore no passing integration evidence for valid, revoked, cross-device, unbound, spoofed-header, or auth-unavailable behavior at this SHA.
- **Violated criterion T4 “Gateway strips spoofable internal headers”:** No route filter removes `X-ReHealth-User-Id`, `X-ReHealth-Tenant-Id`, or `X-ReHealth-Device-Id`; edge `proxy_hide_header` only affects responses. The controller rejects those headers, which is useful defense, but it does not satisfy the explicit gateway-strip criterion.
- **Evidence gap:** The named `-Dcases=...` property is not consumed by the test class; the second command appears to rerun the same static test set rather than selecting the required adversarial matrix.

### Todo 5 — REJECT

- **Violated criterion T5.QA / T5.AC matrix:** `backend/qa/rehealth_stack_gate.py` implements only `topology` and `topology-failures`. Both exact `config-matrix` forms return `unsupported subcommand` under runnable WSL Python (`t5-happy-python3.txt`, `t5-failure-python3.txt`); Windows exact commands exit 9009 because Python is unavailable.
- **Violated criterion T5 model-service configuration:** `model-service/app/runtime_config.py` validates runtime/attribution/Demo provenance and real scorer availability, but has no typed DB/service URL or provider-credential fields and cannot reject the requested HTTP external URL or embedded provider key. The Jeecg validator covers some of these checks, but the task explicitly requires fail-closed runtime configuration in Jeecg and model-service.
- Focused model-service pytest was interrupted after it stopped producing output; no success is claimed (`t5-model-runtime-pytest.txt`).

## Security and external-sync audit

- The sync commit includes large `.omo/evidence/**`, `.omo/drafts/**`, and local helper artifacts despite the plan's explicit “Never commit `.omo` evidence” rule. This is unsafe repository noise and leaks internal execution transcripts, although it is not itself a Todo 2–5 acceptance blocker.
- No newly introduced private key, `.env` value, JWT, or provider secret was found in the Todo 2–5 product paths.
- The current pinned repository does contain hard-coded Google API keys in tracked archived Gemini web assets under `rehealth-algorithms/gemini_conversation/**`. Diff inspection did not show those files as newly added by `b788ed8`; treat this as a pre-existing repository security issue, not a new Todo 2–5 secret.

## Direct remove-ai-slops / programming pass

- `backend/qa/rehealth_stack_gate.py` creates false confidence: a failure “test” mirrors a hand-authored expected JSON contract and never exercises runtime behavior.
- T4's second command supplies an unused selector, so it can pass without proving the requested adversarial classes.
- The T3 happy suite is meaningful and correctly exposes a real compatibility regression; the rejection tests are behavioral rather than deletion-only or tautological.
- Runtime validators are reasonably small and typed, but the Python configuration model is incomplete relative to its stated boundary, and the topology runner's cross-platform path handling is not robust.
- No blocker is based solely on style, module size, or architecture taste; every blocker above maps to a stated T2–T5 criterion.

## Checked artifacts

- `.omo/plans/complete-rehealth-backend-model-service.md`
- `AGENTS.md`, `ENGINEERING.md`, `ACCEPTANCE_REVIEW_2026-07-16.md`, `CODEX_ORCHESTRATION.md`
- `backend/qa/rehealth_stack_gate.py`
- `backend/deploy/rehealth/docker-compose.yml`, `failure-contract.json`, gateway and edge configs
- `backend/contracts/telemetry/**`
- Jeecg internal identity/auth controller, service, credential verifier and integration test
- Jeecg runtime safety validator and profile YAML
- `model-service/app/runtime_config.py`, `main.py`, and runtime tests
- All command transcripts in this directory

## Exact evidence gaps

- No successful runtime deployment/readiness/admin-SPA proof for T2.
- No injected outage proof for any T2 failure case.
- No passing legacy fixture round-trip for T3.
- No executed T4 integration test due the non-reactor-safe exact command.
- No implemented T5 `config-matrix` command.
- No model-service URL/provider-secret configuration matrix.
