# M1 Status - Model Registry And Contract Versioning

Date: 2026-07-10

## Summary

Added a model governance skeleton to model-service without changing the Android C1 CVD 16 feature semantics and without enabling fake real scoring.

The Android app remains a stable API client. Real model versioning, artifact identity, scorer mode, and fallback state are now represented by model-service metadata.

## Changed Files

- `README.md`
- `app/main.py`
- `app/model_registry.py`
- `app/risk_scorer.py`
- `app/schemas.py`
- `docs/API_CONTRACT.md`
- `docs/MODEL_REGISTRY.md`
- `tests/test_api.py`
- `tests/test_risk_scorer.py`
- `codex-runs/2026-07-10/M1_status.md`

## Implemented

- `ModelRegistry` abstraction with registry version `model-registry-v1`.
- Stable feature schema version `cvd-16-v1`.
- `model_trace` risk-response block with:
  - `feature_schema_version`
  - `model_version`
  - `artifact_name`
  - `scorer_mode`
  - `fallback_reason`
  - `request_id`
- `/health` now reports:
  - `model_registry_version`
  - `feature_schema_version`
  - existing scorer/artifact availability fields
- Mock fallback remains explicit:
  - `is_mock=true`
  - `scorer_mode=real_unavailable` in the default missing-artifact path
  - `fallback_reason` populated in `model_trace`
- Real scorer trace includes `artifact_name` only after artifact validation and prediction path setup.
- API and registry docs now document future artifact manifest shape and feature schema versioning policy.

## Not Implemented

- No real model artifact was added.
- No model training was performed.
- No gray routing or multi-model routing was enabled.
- No Android, backend, or rehealth-android files were changed.

## Validation Results

Commands run from `D:\rehealthAI\model-service` using bundled Python:

```powershell
$env:TMP='D:\rehealthAI\model-service\.tmp'
$env:TEMP='D:\rehealthAI\model-service\.tmp'
C:\Users\kiki\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe -m pytest
C:\Users\kiki\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe -m compileall app
git diff --check
git status --short --branch
```

Results:

- `pytest`: 22 passed, 1 cache warning from `.pytest_cache` permission
- `compileall app`: passed
- `git diff --check`: passed
- Health smoke: passed, returned `model_registry_version=model-registry-v1` and `feature_schema_version=cvd-16-v1`
- Risk smoke: passed, returned `model_trace.request_id=smoke-m1` and `scorer_mode=real_unavailable`
- Sibling repo status checks: `Android-apk`, `backend`, and `rehealth-android` had no modified files

## Known Risks

- No real model artifact is present, so model-service still runs the mock fallback path.
- `model_trace` is a new response field; backend and Android clients should store or ignore it intentionally.
- Future V8/97-feature work must use a new schema version instead of changing `cvd-16-v1`.

## Commit

Planned commit message:

```text
feat(model-service): add model registry trace metadata
```

Expected final status after commit:

```text
## main...origin/main [ahead 4]
```
