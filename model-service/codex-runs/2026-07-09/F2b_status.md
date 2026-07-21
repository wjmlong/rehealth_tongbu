# F2b Model Service Real Algorithm Integration Status - 2026-07-09

## Mandatory Preflight Summary

Canonical real-scoring source candidate:

- `D:\rehealthAI\rehealth-android\bodyup_cloud\engine\risk_scorer.py`
- Corroborating trainer: `D:\rehealthAI\rehealth-android\train\train_v2_final.py`

Artifact files found:

- `D:\rehealthAI\rehealth-android\bodyup_cloud\config\cvd_features.json`
- `D:\rehealthAI\rehealth-android\bodyup_edge_sdk\config\cvd_features.json`
- `D:\rehealthAI\rehealth-android\train\rehealth_v3_unified.json`
- `D:\rehealthAI\rehealth-android\train\rehealth_v4_biomarkers.json`
- CatBoost training metadata under `D:\rehealthAI\rehealth-android\catboost_info\`

Required artifact files missing:

- `D:\rehealthAI\model-service\models\rehealth_cvd_catboost.pkl`
- `D:\rehealthAI\model-service\models\feature_cols.pkl`
- `D:\rehealthAI\model-service\models\model_meta_v2.json`
- `D:\rehealthAI\rehealth-android\rehealth_v2_final.pkl`
- `D:\rehealthAI\rehealth-android\feature_cols_v2.pkl`
- `D:\rehealthAI\rehealth-android\cat_cols_v2.pkl`

Expected feature order:

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

Feature order reconciliation:

- Matches Android C1 `CvdFeatureFields.ALL`.
- Matches model-service `FEATURE_FIELDS`.
- Matches `rehealth-android/bodyup_cloud/config/cvd_features.json`.
- Matches `rehealth-android/train/train_v2_final.py`.

Required dependencies:

- Current safe boundary: no new dependency; Python stdlib pickle-compatible loading is used for local artifact validation.
- Future real CatBoost artifact runtime likely requires `joblib`, `catboost`, `pandas`, `numpy`.
- SHAP contribution support requires `shap` and explicit performance/semantics validation.

Can F2b implement true `is_mock=false` scoring now?

- No. Required serialized model and feature-order artifacts are missing from `model-service\models`.

Safe partial implementation:

- Added a real-scorer loader boundary.
- Added local artifact validation and feature-order validation.
- Added explicit `real_unavailable` health mode.
- Kept mock scoring explicit with `is_mock=true`.
- Added tests proving missing or mismatched artifacts do not pretend real scoring.

## What Changed

- Added `RealModelArtifacts`, `ArtifactValidationResult`, `ModelUnavailableError`, and `RealCatBoostRiskScorer`.
- Added local-only artifact path checks under `models/`.
- Added feature-order loading from `feature_cols.pkl` or JSON with `feature_cols`.
- Added optional `request_id` and `contribution_method` response fields with `response_model_exclude_none=True`.
- Updated `/health` to report `scorer_mode`, `model_available`, and `model_unavailable_reason`.
- Added `docs/REAL_MODEL_INTEGRATION.md`.
- Updated `docs/API_CONTRACT.md` and `README.md`.
- Added `models/.gitkeep` and ignored model binaries.

## Validation Results

- `python --version` remains unavailable because PATH resolves to the Windows Store Python stub.
- Used `C:\Users\kiki\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe`.
- Cycle 1: `pip install -r requirements.txt` failed after adding `joblib==1.5.1` because PyPI SSL fetches failed. F2b does not need a hard dependency while artifacts are missing, so the dependency was removed and the boundary uses stdlib pickle-compatible local loading.
- Cycle 2: `python -m pip install -r requirements.txt` passed.
- Cycle 2: `python -m compileall app` passed.
- Cycle 2: `python -m pytest` passed: 17 tests.
- Cycle 2: `python -m uvicorn app.main:app --reload --host 127.0.0.1 --port 8000` started successfully.
- Uvicorn smoke `GET /health` passed with `scorer_mode=real_unavailable`, `model_available=false`, and missing model reason.
- Uvicorn smoke `POST /v1/cvd/risk/evaluate` passed with `is_mock=true` and preserved `request_id=f2b-smoke-001`.
- `git diff --check` passed; Git reported line-ending warnings only.
- `git status --short --branch` before commit showed only intended model-service changes.

## Known Risks

- No real model artifact is present, so runtime remains mock fallback with `is_mock=true`.
- Pickle-compatible artifact loading can execute payloads; artifacts must stay local and reviewed before deployment.
- SHAP is not enabled in F2b.
- Real model calibration and clinical validation remain future work.

## Files Changed

- `.gitignore`
- `README.md`
- `app/main.py`
- `app/risk_scorer.py`
- `app/schemas.py`
- `codex-runs/2026-07-09/F2b_status.md`
- `docs/API_CONTRACT.md`
- `docs/REAL_MODEL_INTEGRATION.md`
- `models/.gitkeep`
- `requirements.txt`
- `tests/test_api.py`
- `tests/test_risk_scorer.py`
