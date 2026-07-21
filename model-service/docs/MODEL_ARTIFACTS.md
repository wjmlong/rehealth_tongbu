# Model Artifacts

Status: a reviewed local CVD-16 CatBoost artifact is available as of 2026-07-11. The binary remains local and is excluded from git.

## Current Deployment Candidate

Source output:

```text
D:\rehealthAI\rehealth-algorithms\outputs\cvd_retrain_20260710_173543
```

The output timestamp is UTC. The reviewed candidate was produced on 2026-07-11 Asia/Shanghai time.

| Artifact | Local destination | Status |
| --- | --- | --- |
| `rehealth_cvd_catboost.pkl` | `model-service/models/rehealth_cvd_catboost.pkl` | Present locally; gitignored |
| `feature_cols.pkl` | `model-service/models/feature_cols.pkl` | Present locally; gitignored |
| `model_meta_v2.json` | `model-service/models/model_meta_v2.json` | Present locally; gitignored |

Reviewed metadata:

| Field | Value |
| --- | --- |
| Model version | `cvd-core16-catboost-20260710T173543Z` |
| Feature schema | `cvd-16-v1` |
| Final lockbox AUC | `0.849594` |
| Calibration | Platt sigmoid on validation sample-level binary outcomes |
| Final lockbox calibration slope | `0.964810` |
| Final lockbox calibration intercept | `-0.079902` |
| Model SHA-256 | `01c99d209da22387583cea6c23a84b4d92a80471a5eff08a67b9112a2722e497` |
| Feature-order SHA-256 | `8cafcca881b3cfb34d3cf8e43c77558dab1c3987be83bcc8d1d6f1ceb7b126c7` |

These metrics describe this training evaluation only. They are not clinical validation, diagnostic performance claims, or evidence of transfer to a Chinese population.

## Canonical Feature Order

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

The order exactly matches Android C1 and backend E1. Optional lab values remain nullable; the deployed sklearn pipeline contains its train-only median imputer.

## Artifact Policy

- Canonical deployment names are `rehealth_cvd_catboost.pkl`, `feature_cols.pkl`, and `model_meta_v2.json`.
- Historical aliases `rehealth_v2_final.pkl`, `feature_cols_v2.pkl`, and `model_metadata.json` remain loader-compatible.
- Model binaries and serialized feature-order files must not be committed without explicit approval.
- Artifacts must come from the reviewed training pipeline and stay under the configured local artifact root.
- Joblib/pickle-compatible loading can execute code. Never load an untrusted artifact.

The current `.gitignore` policy is:

```text
models/*
!models/.gitkeep
```

## Optional Artifacts

| File | Current status | Runtime behavior |
| --- | --- | --- |
| `shap_explainer.pkl` | Not produced | Real prediction remains available; contributions use documented zero fallback |
| `calibration.json` | Not needed | Calibration is embedded in the serialized sklearn pipeline |
| `feature_schema.json` | Not produced | Canonical order is supplied by `feature_cols.pkl` and metadata |

## Reproduce And Validate

From `D:\rehealthAI\rehealth-algorithms`:

```powershell
$PY = "C:\Users\kiki\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe"
& $PY train\train_cvd_retrain.py
& $PY train\validate_deploy_candidate.py --candidate outputs\<new-timestamp>\deploy_candidate
```

Copy only a reviewed candidate:

```powershell
$src = "D:\rehealthAI\rehealth-algorithms\outputs\<reviewed-timestamp>\deploy_candidate"
$dst = "D:\rehealthAI\model-service\models"
Copy-Item "$src\rehealth_cvd_catboost.pkl" "$dst\rehealth_cvd_catboost.pkl" -Force
Copy-Item "$src\feature_cols.pkl" "$dst\feature_cols.pkl" -Force
Copy-Item "$src\model_meta_v2.json" "$dst\model_meta_v2.json" -Force
```

From `D:\rehealthAI\model-service`:

```powershell
$PY = "C:\Users\kiki\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe"
& $PY -m pip install -r requirements.txt
& $PY -m pytest
& $PY -m compileall app
& $PY -m uvicorn app.main:app
```

The real-model gate is closed only when `/health` returns `scorer_mode=real_available` and `model_available=true`, and a valid risk request returns `is_mock=false` after prediction succeeds.
