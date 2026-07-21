# F2e Status - Docs-Driven Artifact Alias Patch

Date: 2026-07-10

## Summary

F2e patched the model-service real-model boundary using evidence from `rehealth-android` docs. The service now supports the normalized model artifact name and the historical V2 artifact alias while preserving the safe mock fallback when deployable artifacts are absent.

No model binaries were trained, copied, or committed. No Android, backend, or rehealth-android files were modified.

## Changed Files

- `README.md`
- `app/main.py`
- `app/risk_scorer.py`
- `app/schemas.py`
- `docs/MODEL_ARTIFACTS.md`
- `docs/REAL_MODEL_INTEGRATION.md`
- `docs/REHEALTH_ANDROID_MODEL_TRACE.md`
- `tests/test_api.py`
- `tests/test_risk_scorer.py`
- `codex-runs/2026-07-09/F2e_status.md`

## Docs Evidence Summary

Reviewed `rehealth-android` documentation traces:

- `工程日记_CVD模型训练.md`
- `V1_工程实施版_端云协同系统开发框架说明书.md`
- `CTO技术决策对话记录_20260525.md`

Evidence reconciled:

- Historical artifact name: `rehealth_v2_final.pkl`
- Normalized model-service artifact name: `rehealth_cvd_catboost.pkl`
- Current service feature contract: Android C1 CVD 16 fields
- V2 16-feature MVP trace: AUC `~0.767`
- Older docs mention AUC `0.847`, but later notes identify hardcoded `model_auc: 0.847` as outdated behavior
- V8 AUC `0.8615` is a 97-feature research trace and not the current C1 16-field model-service contract

## Artifact Alias Policy

Model artifact search order:

```text
models/rehealth_cvd_catboost.pkl
models/rehealth_v2_final.pkl
```

Policy:

- Prefer `rehealth_cvd_catboost.pkl`
- Accept `rehealth_v2_final.pkl` as a historical alias
- Do not set `is_mock=false` unless a real local artifact loads and a prediction succeeds

## Feature Order Alias Policy

Feature-order artifact search order:

```text
models/feature_cols.pkl
models/feature_cols_v2.pkl
models/cvd_features.json
```

If no feature-order artifact exists, the loader reports the built-in Android C1 16-field order for diagnostics only. Real scoring remains unavailable until a local feature-order artifact validates exactly.

## Metadata Policy

Metadata search order:

```text
models/model_meta_v2.json
models/model_metadata.json
```

If metadata is absent and a real model loads, the fallback model version is `cvd-catboost-v2`. AUC remains omitted/`None` unless verified metadata provides it. The service does not hardcode `model_auc=0.847`.

## Health Endpoint Result

`GET /health` now reports:

- `scorer_mode`
- `model_available`
- `model_unavailable_reason`
- `expected_model_artifacts`
- `supported_model_artifact_aliases`
- `expected_feature_order_artifacts`
- `loaded_artifact_name` when a real artifact is loaded

Current no-artifact mode:

```text
scorer_mode=real_unavailable
model_available=False
model_version=cvd-mock-rules-v1
model_unavailable_reason=model artifact missing: models/rehealth_cvd_catboost.pkl; models/rehealth_v2_final.pkl
```

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
- Health smoke via `TestClient`: passed, returned mock fallback plus alias lists
- Sibling repo status checks: `Android-apk`, `backend`, and `rehealth-android` had no modified files

## Known Risks

- No real model artifact is present in this checkout, so runtime remains `real_unavailable` with `is_mock=true`.
- Pickle-compatible model loading is still limited to reviewed local artifacts under the configured artifact root.
- Real SHAP attribution, calibration, and clinical validation remain future work.
- Local pytest can emit a cache warning because `.pytest_cache` cannot be written in this environment; tests still pass.

## Commit

Planned commit message:

```text
fix(model-service): align real model artifact discovery with rehealth docs
```

## Final Git Status

Expected after the F2e commit:

```text
## main...origin/main [ahead 3]
```

No uncommitted files.
