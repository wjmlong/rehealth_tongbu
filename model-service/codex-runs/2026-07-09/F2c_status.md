# F2c Model Artifact Recovery And Validation Status - 2026-07-10

## Scope

Workstream: `F2c_model_artifact_recovery_and_validation`

Goal: find, validate, and prepare real CatBoost model artifacts for model-service without training a new model, faking real scoring, or enabling `is_mock=false` without validated artifacts.

## Files Read

- `model-service/docs/REAL_MODEL_INTEGRATION.md`
- `model-service/docs/API_CONTRACT.md`
- `model-service/app/risk_scorer.py`
- `model-service/app/schemas.py`
- `model-service/README.md`
- `model-service/codex-runs/2026-07-09/F2b_status.md`
- `Android-apk/docs/FEATURE_EXTRACTOR.md`
- `backend/docs/CANONICAL_BACKEND_RISK_PATH.md`
- `rehealth-android/bodyup_cloud/engine/risk_scorer.py`
- `rehealth-android/bodyup_cloud/config/cvd_features.json`
- `rehealth-android/bodyup_edge_sdk/config/cvd_features.json`
- `rehealth-android/train/train_v2_final.py`
- `rehealth-android/bodyup_cloud/config/settings.py`

## Local Artifact Search

Searched roots:

- `D:\rehealthAI`
- `D:\rehealthAI\rehealth-android`
- `D:\rehealthAI\model-service`
- `D:\rehealthAI\outputs`
- `D:\rehealthAI\tools`

Patterns:

- `*.pkl`
- `*.cbm`
- `*.joblib`
- `*catboost*`
- `*feature_cols*`
- `*feature_columns*`
- `*model*`
- `*shap*`
- `*calibration*`

Result:

- No `.pkl`, `.cbm`, or `.joblib` deployable model artifact was found.
- No `feature_cols.pkl`, `feature_cols_v2.pkl`, `feature_columns*`, `shap_explainer.pkl`, `calibration.json`, or `feature_schema.json` artifact was found.
- Relevant non-deployable matches were found and inventoried in `docs/MODEL_ARTIFACTS.md`.
- Many broad `*model*` matches were unrelated Android/backend/tooling source files or compiled classes.

## Candidate Inventory Summary

| Path | Size | Modified | Classification | Copy to `model-service/models`? |
| --- | ---: | --- | --- | --- |
| `rehealth-android/catboost_info/catboost_training.json` | 29,939 bytes | 2026-07-08 20:58:44 | Training metadata/log, not deployable model | No |
| `rehealth-android/healthagent/pias/china_calibration.py` | 12,120 bytes | 2026-07-08 20:58:44 | Research/source calibration helper, not artifact | No |
| `tools/apache-maven-3.9.11/lib/maven-repository-metadata-3.9.11.jar` | 28,530 bytes | 2025-07-12 18:30:34 | Unrelated Maven tooling dependency | No |
| `rehealth-android/bodyup_cloud/config/cvd_features.json` | 1,312 bytes | 2026-07-08 20:58:44 | Feature-order reference JSON | Reference only |
| `rehealth-android/bodyup_edge_sdk/config/cvd_features.json` | 1,312 bytes | 2026-07-08 20:58:44 | Feature-order reference JSON | Reference only |
| `rehealth-android/train/train_v2_final.py` | 19,042 bytes | 2026-07-08 20:58:44 | Canonical training script | No |
| `rehealth-android/bodyup_cloud/engine/risk_scorer.py` | 3,542 bytes | 2026-07-08 20:58:44 | Reference real-scoring implementation | No |

## Feature Order Validation

No serialized `feature_cols.pkl` exists to load.

Validated available JSON equivalents:

- `rehealth-android/bodyup_cloud/config/cvd_features.json`
- `rehealth-android/bodyup_edge_sdk/config/cvd_features.json`

Both match model-service `FEATURE_FIELDS` exactly:

```text
age
gender
bmi
sbp
dbp
fasting_glucose
total_cholesterol
ldl
hdl
triglycerides
exercise_days
smoking
drinking
diabetes_history
hypertension_history
family_history
```

Comparison result:

- Missing fields: none.
- Extra fields: none.
- Order mismatch: none.
- Android C1 contract match: yes.

## Model Availability Validation

No real model artifact exists at:

```text
model-service/models/rehealth_cvd_catboost.pkl
```

No alternate `.pkl`, `.cbm`, or `.joblib` model artifact exists in the searched roots.

Fallback validation with a synthetic valid CVD 16 vector:

```text
scorer_class= MockRiskScorer
scorer_mode= real_unavailable
model_available= False
model_unavailable_reason= model artifact missing: models\rehealth_cvd_catboost.pkl
risk_score= 0.405
risk_level= RiskLevel.MODERATE
model_version= cvd-mock-rules-v1
is_mock= True
```

Conclusion:

- Real scoring is unavailable.
- `is_mock=false` was not enabled.
- Current scorer mode remains `real_unavailable`.

## Artifact Request List

Required:

- `rehealth_cvd_catboost.pkl`
  - Destination: `model-service/models/rehealth_cvd_catboost.pkl`
  - Expected source: `rehealth-android/rehealth_v2_final.pkl`, produced by `rehealth-android/train/train_v2_final.py`
  - Expected interface: CatBoost-compatible object exposing `predict_proba` or `predict`

- `feature_cols.pkl`
  - Destination: `model-service/models/feature_cols.pkl`
  - Expected source: `rehealth-android/feature_cols_v2.pkl`, produced by `rehealth-android/train/train_v2_final.py`
  - Expected content: Python `list[str]` exactly matching Android C1 feature order

Recommended:

- `model_meta_v2.json`
  - Destination: `model-service/models/model_meta_v2.json`
  - Expected source: `rehealth-android/model_meta_v2.json`, produced by `rehealth-android/train/train_v2_final.py`
  - Expected content: JSON object with `model_version`

Optional future:

- `cat_cols_v2.pkl`
- `shap_explainer.pkl`
- `calibration.json`
- `feature_schema.json`

## Documentation Updates

Created or updated:

- `docs/MODEL_ARTIFACTS.md`
- `codex-runs/2026-07-09/F2c_status.md`

No model-service API contract changes were needed.

## Validation Commands

Python on PATH remains unavailable:

```text
python --version
```

Used bundled Python:

```text
C:\Users\kiki\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe
```

Validation results:

```powershell
C:\Users\kiki\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe -m pytest
```

First run result:

- 14 tests passed.
- 3 setup errors occurred because pytest attempted to create temp directories under `C:\Users\kiki\AppData\Local\Temp\pytest-of-kiki`, which was not accessible in this sandboxed session.
- This was an environment temp-directory permission issue, not a model-service test assertion failure.

Rerun with local workspace temp:

```powershell
$env:TMP = (Resolve-Path .\.tmp).Path
$env:TEMP = (Resolve-Path .\.tmp).Path
C:\Users\kiki\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe -m pytest
```

Result:

- Passed: 17 tests.

Compile validation:

```powershell
C:\Users\kiki\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe -m compileall app
```

Result:

- Passed.

Focused test validation:

```powershell
C:\Users\kiki\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe -m pytest tests/test_api.py tests/test_risk_scorer.py
```

Result:

- Passed: 17 tests.

Final checks:

```powershell
git diff --check
git status --short --branch
```

Results before commit:

- `git diff --check`: passed.
- `git status --short --branch`: `## main...origin/main [ahead 1]` with only:
  - `?? codex-runs/2026-07-09/F2c_status.md`
  - `?? docs/MODEL_ARTIFACTS.md`

Expected status after F2c docs/status commit:

- Working tree clean.
- Branch ahead of `origin/main` by the existing F2b commit plus the F2c docs/status commit.

## Known Risks

- Required model and feature-order artifacts are missing.
- Real CatBoost unpickling may require `catboost`, `joblib`, `numpy`, and possibly `pandas`; these are intentionally not added until a real artifact is available and reviewed.
- Pickle-compatible artifact loading can execute code; only reviewed local artifacts from the approved pipeline should be loaded.
- SHAP, calibration, and real contribution semantics are not validated.
- No new model training was performed.
- No fake real scoring path was created.
