# F_model_service Status - 2026-07-09

## F1b Audit Result

- Required F1 files exist:
  - `app/main.py`
  - `app/schemas.py`
  - `app/risk_scorer.py`
  - `app/prescription_generator.py`
  - `app/attribution.py`
  - `tests/test_api.py`
  - `tests/test_risk_scorer.py`
  - `README.md`
  - `requirements.txt`
  - `Dockerfile`
  - `codex-runs/2026-07-09/F_status.md`
- Existing essential engineering files:
  - `.gitignore`
  - `app/__init__.py`
- Added missing contract freeze document:
  - `docs/API_CONTRACT.md`
- `tests/__init__.py` was not added because pytest imports work without it.

## API Contract Reconciliation Result

- Python schemas include all Android C1 CVD 16 fields.
- Optional lab values remain nullable: `fasting_glucose`, `total_cholesterol`, `ldl`, `hdl`, `triglycerides`.
- Binary MVP fields are validated as `0` or `1` when present.
- `featureQuality` is accepted from Android and normalized to `feature_quality` internally; snake_case quality keys are required for every CVD 16 field.
- `FeatureQuality.status` matches Android C1: `VALID`, `MISSING`, `STALE`, `LOW_CONFIDENCE`.
- `FeatureQuality.source` matches Android C1: `REAL_DEVICE`, `USER_REPORTED`, `CLINICAL_REPORT`, `DERIVED`, `UNKNOWN`.
- `risk_score` is constrained and documented as `0.0` to `1.0`.
- `risk_level` is constrained and documented as `low`, `moderate`, `high`, or `very_high`.
- `model_version` and `is_mock` are present in risk and intervention responses.
- Response field names are covered by tests to keep backend E1 and Android D1 mappings stable.

## What Changed

- Added `docs/API_CONTRACT.md` with endpoint examples, stable field names, error behavior, mock scorer behavior, artifact location, and compatibility notes.
- Tightened response schemas with `RiskLevel`, `risk_score` range validation, and intervention `confidence` range validation.
- Added API tests that assert exact stable response field names.
- Updated `README.md` to point to the frozen API contract.

## Validation Commands And Results

- `python --version` failed because PATH resolves to the Windows Store Python stub.
- `C:\Users\kiki\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe --version` passed: Python 3.12.13.
- `C:\Users\kiki\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe -m pip install -r requirements.txt` passed; requirements were already installed.
- `C:\Users\kiki\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe -m pytest` passed: 12 tests.
- `C:\Users\kiki\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe -m compileall app` passed.
- `C:\Users\kiki\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe -m uvicorn app.main:app --reload --host 127.0.0.1 --port 8000` started successfully.
- Uvicorn smoke `GET /health` passed and returned `cvd-mock-rules-v1`.
- Uvicorn smoke `POST /v1/cvd/risk/evaluate` passed and returned a moderate mock risk result.

## GitHub Versioning

- GitHub repository: `https://github.com/RehealthAI/model-service.git`
- Branch: `main`
- Initial implementation commit hash: `7359e5281e08949b57c6f9fb1de72d93d277a9aa`
- Initial push result: succeeded; `main` now tracks `origin/main`.
- Status-note follow-up commit: created after this file was updated.
- Final git status: clean after status-note follow-up commit and push.

## Known Risks

- `MockRiskScorer` is deterministic and explicitly returns `is_mock=true`; it is not a real clinical model.
- `CatBoostRiskScorer` loads a reserved future artifact path, but real preprocessing, calibration, feature contribution extraction, and validation remain future work.
- Interventions are conservative wellness support only and must not be presented as diagnosis or treatment.

## Exact Files Changed

- `.gitignore`
- `Dockerfile`
- `README.md`
- `app/__init__.py`
- `app/attribution.py`
- `app/main.py`
- `app/prescription_generator.py`
- `app/risk_scorer.py`
- `app/schemas.py`
- `codex-runs/2026-07-09/F_status.md`
- `docs/API_CONTRACT.md`
- `requirements.txt`
- `tests/test_api.py`
- `tests/test_risk_scorer.py`

## Next Recommended Task

Use backend E1 to call `/v1/cvd/risk/evaluate` and `/v1/cvd/intervention/generate`, preserving Android local persistence and offline upload queue behavior.
