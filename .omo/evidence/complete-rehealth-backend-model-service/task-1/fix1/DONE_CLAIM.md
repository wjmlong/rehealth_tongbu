# Todo 1 verifier fix DoneClaim

## Changed files

- `backend/contracts/scripts/validate_contracts.py`
- `backend/contracts/README.md`
- `backend/contracts/tests/test_validate_contracts.py`
- `backend/contracts/fixtures/forbidden/unknown_event_type.json`
- `backend/contracts/fixtures/forbidden/event_type_escape.json`
- `backend/contracts/fixtures/forbidden/schema_ref_escape.json`
- `backend/contracts/fixtures/forbidden/metrics.json`
- `backend/contracts/fixtures/forbidden/heart_rate.json`
- `backend/contracts/fixtures/forbidden/spo2.json`
- `backend/contracts/fixtures/forbidden/raw_ppg.json`

## Exact scenarios and evidence

| Scenario | Invocation | Binary observable | Artifact |
| --- | --- | --- | --- |
| Failing-first focused tests | Project interpreter `-m pytest -q backend/contracts/tests/test_validate_contracts.py` before validator fix | Exit 1; seven intended assertion failures | `red-tests.xml`, `red-tests.stdout.txt` |
| Failing-first forbidden suite | Validator against `backend/contracts/fixtures/forbidden` before fix | Exit 1 in expected-rejection mode; two unsafe event types accepted | `red-forbidden.json` |
| Focused regression tests | Project interpreter `-m pytest -q backend/contracts/tests/test_validate_contracts.py` | Exit 0; 7 tests, 0 failures/errors | `final-tests.xml`, `final-tests.stdout.txt` |
| T1 happy | Project interpreter `backend/contracts/scripts/validate_contracts.py --all --fixtures backend/contracts/fixtures/valid --report .../contracts.json` | Exit 0; 48 accepted, 0 rejected | `contracts.json` |
| T1 forbidden | Project interpreter `backend/contracts/scripts/validate_contracts.py --fixtures backend/contracts/fixtures/forbidden --expect-rejected token,raw_signal,client_owner` | Exit 0; 0 accepted, 10 rejected; required and new policy reasons present | `forbidden.json` |
| Malformed JSON | Validator against `task-1/malformed-fixture` | Expected exit 1; one rejection with `malformed_json` | `malformed.json` |
| Unknown/traversal/ref escape | Validator against `escape-fixtures` with expected reasons | Exit 0; 0 accepted, 3 rejected; `schema:unknown_event_type` and `schema_ref` | `escape.json` |
| Raw/normalized metrics | Validator against `metric-fixtures` with expected reasons | Exit 0; 0 accepted, 4 rejected; `metric_value` and `raw_signal` | `metrics.json` |
| Python quality | `py_compile` plus `check-no-excuse-rules.py` for validator and tests | Both exit 0; bytecode removed afterward | `no-excuse.stdout.txt`, `exits.json` |
| Owned diff | `git diff --check -- backend/contracts backend/docs`, then isolated temporary-index `git diff --cached --check` because the owned tree is untracked | Both exit 0; temporary index removed without touching the shared index | `exits.json`, `owned-diff-check.json` |

`verification.json` independently parses JUnit and every machine report, checks exact counts/reasons/exit codes, confirms non-empty hashed artifacts, and confirms bytecode cleanup.

## Manual QA and cleanup

The CLI was exercised through its real process surface using `D:\rehealthAI\model-service\.venv\Scripts\python.exe`. The allowlist maps only the three approved event types to fixed local filenames; no fixture value is transformed into a path. `README.md` and the script run block now use the discoverable project interpreter because bare `python` resolves to WindowsApps and exits 9009 here.

No servers or containers were started. Only generated `__pycache__` directories were removed; evidence was retained. No commit or push was performed.

## Residual risk

The policy intentionally operates on field names and closed schemas, not arbitrary string values. Future event versions must be explicitly added to both `EVENT_SCHEMAS` and a closed schema before fixtures can validate.
