# DoneClaim: Todos 2–5 verifier fixes

## Outcome

All four updated exact gates pass. Android/Compose files were not touched.

## Success-criterion evidence

| Criterion | Scenario and invocation | Binary observable | Artifact |
| --- | --- | --- | --- |
| T2 portable topology | Windows venv Python and WSL `python3 backend/qa/rehealth_stack_gate.py topology --compose backend/deploy/rehealth/docker-compose.yml --profiles staging,production ...` | both exit 0; only `edge` is published; `runtime_verified=false` | `green-t2-windows-final.log`, `green-t2-wsl-final.log`, `task-2-complete-rehealth-backend-model-service/topology.json`, `topology-wsl-final.json` |
| T2 runtime negative | `... rehealth_stack_gate.py topology-failures --cases timescale_down,auth_down,kafka_down,bad_model_hash` | exit 0; each temporary TCP dependency probes available before injection and unavailable after; Timescale/auth make ingest unavailable, Kafka leaves ingest ready with publisher degraded, bad artifact makes model unready; every case has `runtime_verified=true` | `green-t2-failures-windows.log` |
| T2 spoof stripping | topology route validation plus the same passing topology commands | both Gateway routes contain removal filters; edge clears all three incoming ReHealth identity headers | `final-gate-pytest.log`, `task-2-complete-rehealth-backend-model-service/topology.json` |
| T3 legacy compatibility | `mvn -f backend/contracts/telemetry/pom.xml test -Dfixtures=src/test/resources/legacy-valid` | exit 0; `Tests run: 7, Failures: 0, Errors: 0` | `green-t3.log` |
| T3 raw rejection | `mvn -f backend/contracts/telemetry/pom.xml test -Dtest=TelemetryContractRejectionTest` | exit 0; `Tests run: 5, Failures: 0, Errors: 0` | `green-t3-rejection.log` |
| T4 happy identity/binding | updated reactor command with `-am -Dsurefire.failIfNoSpecifiedTests=false` | exit 0; 8 authorization cases run, 0 failures | `green-t4-happy.log` |
| T4 selected adversarial matrix | same reactor command with `-Dcases=revoked,cross_device,unbound,spoofed_header,auth_unavailable` | exit 0; exactly 5 selected cases execute and 3 are skipped; 0 failures/errors | `green-t4-negative.log` |
| T5 valid/invalid matrix | `... rehealth_stack_gate.py config-matrix --valid production,staging,development,demo` and `--invalid production_demo,disabled_software_db,http_external,embedded_secret` on Windows and WSL | all exit 0; unsafe cases return the four required rejection codes | `task-5-complete-rehealth-backend-model-service/valid.log`, `invalid.log`, `green-t5-wsl-valid.log`, `green-t5-wsl-invalid.log` |
| T5 actual model boundary | `model-service/.venv/Scripts/python.exe -m pytest -q tests/test_runtime_config.py tests/test_api.py` | exit 0; 21 passed; protected mode rejects HTTP URL and embedded provider secret | `final-model-pytest.log` |

## Red-to-green evidence

- T3 red: the same seven-test command failed the captured legacy fixture with
  `raw_signal.disabled` at `quality`; see `red-t3.log`.
- T4 red: the prior leaf-only command failed dependency resolution before tests;
  see `red-t4.log`.
- T5 red: the command returned `unsupported subcommand: config-matrix`; see
  `red-t5.log`.
- T2 red: the previous output had no runtime probe transition; the green
  artifact records live before/after probes for all four injected outages.

## Cleanup

No gate, Maven, or temporary dependency process remains. `git diff --check`
passes. Existing `.idea` and verifier artifacts were preserved. During the
shared-worktree run, external sync commit `426d411` committed the code/test
changes and pushed them; this worker did not run commit or push. The remaining
working-tree changes are the deployment/runtime documentation and provider
secret wiring plus this evidence.
