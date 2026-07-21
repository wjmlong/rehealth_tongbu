# F Workstream Closure Status

Date: 2026-07-11 (Asia/Shanghai)

## Result

F is closed locally end-to-end. The corrected training pipeline produced a reviewed CVD-16 CatBoost candidate, model-service loaded it, `/health` reported real availability, and risk evaluation returned `is_mock=false` after an actual prediction.

## Training Fixes

- Split data before fitting imputation or model preprocessing statistics.
- Use train-only imputation during model selection through a serialized sklearn pipeline.
- Reserve validation for configuration assessment and Platt sigmoid calibration on sample-level binary outcomes.
- Access the held-out test once after the CatBoost configuration and calibration method are frozen.
- Use the same scaled sklearn pipeline for logistic baseline fit and validation prediction.
- Record failed calibration as null with a reason; never substitute slope `1` or intercept `0`.
- Write top-level `model_auc`, canonical `artifact_name`, exact feature order, calibration fields, timestamp, and source output directory.

## Reviewed Artifact

Source directory:

```text
D:\rehealthAI\rehealth-android\outputs\cvd_retrain_20260710_173543
```

Local model-service files, intentionally not committed:

```text
models/rehealth_cvd_catboost.pkl
models/feature_cols.pkl
models/model_meta_v2.json
```

Evidence:

- Model version: `cvd-core16-catboost-20260710T173543Z`
- Final lockbox AUC: `0.849594`
- Final lockbox Brier score: `0.078440`
- Calibration status: `applied`
- Final lockbox calibration slope: `0.964810`
- Final lockbox calibration intercept: `-0.079902`
- Model SHA-256: `01c99d209da22387583cea6c23a84b4d92a80471a5eff08a67b9112a2722e497`
- Feature-order SHA-256: `8cafcca881b3cfb34d3cf8e43c77558dab1c3987be83bcc8d1d6f1ceb7b126c7`

Earlier outputs `cvd_retrain_20260710_172441`, `cvd_retrain_20260710_172603`, and `cvd_retrain_20260710_172749` are retained for audit and were not deployed. The final lockbox in `173543` was isolated from the prior train-only pool; the prior seed-42 test was reused only for final training.

## Commands And Results

```powershell
$PY = "C:\Users\kiki\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe"

# rehealth-android
& $PY train\train_cvd_retrain.py
& $PY train\validate_deploy_candidate.py --candidate outputs\cvd_retrain_20260710_173543\deploy_candidate

# model-service
& $PY -m pip install -r requirements.txt
& $PY -m pytest
& $PY -m compileall app
git diff --check
git status --short --branch
```

Results:

- Retraining: passed; 31,978 adults split into 22,768 final train, 5,117 calibration validation, and 4,093 final lockbox samples.
- Candidate validation: all artifact, metadata, feature-order, and synthetic prediction checks passed.
- Nullable-lab prediction: passed without warnings.
- `pytest`: 23 passed; one existing `.pytest_cache` permission warning.
- `compileall app`: passed.
- TestClient `/health`: `scorer_mode=real_available`, `model_available=true`.
- TestClient risk smoke: real prediction returned `is_mock=false` with a non-empty model version.
- Uvicorn HTTP smoke on `127.0.0.1:8011`: passed with the same real availability and risk result; process stopped after validation.

## Changed Files

`rehealth-android`:

- `train/train_cvd_retrain.py`
- `train/validate_deploy_candidate.py`

`model-service`:

- `app/risk_scorer.py`
- `requirements.txt`
- `tests/test_api.py`
- `tests/test_risk_scorer.py`
- `README.md`
- `docs/MODEL_ARTIFACTS.md`
- `docs/REAL_MODEL_INTEGRATION.md`
- `codex-runs/2026-07-11/F_close_status.md`

## Git And Artifact Policy

- Model binaries are local and ignored by `models/*`; they were not staged or committed.
- Existing untracked NHANES data and downloader files in `rehealth-android` were not modified or staged.
- Training pipeline commit: `7153cca fix(research): harden CVD retraining pipeline`.
- Model-service implementation commit: `bdf50ea fix(model-service): deploy reviewed CVD artifact runtime`.
- `model-service`: `main...origin/main [ahead 1]` before this status-only follow-up; no tracked implementation changes remained.
- `rehealth-android`: `main...origin/main [ahead 1]`; pre-existing untracked `data/nhanes/` and `download_all_nhanes.py` remain intentionally untouched.
- `Android-apk`: no modified files; branch was already ahead of its remote by one commit.
- `backend`: clean and aligned with its remote branch.

## Residual Risks

- The outcome is self-reported prevalent CVD, not a prospective clinical endpoint.
- NHANES transfer to the intended Chinese population is not clinically validated.
- SHAP is not packaged; contribution fields remain an explicit deterministic zero fallback.
- Artifact distribution still needs a signed registry or secure release process before multi-environment deployment.
