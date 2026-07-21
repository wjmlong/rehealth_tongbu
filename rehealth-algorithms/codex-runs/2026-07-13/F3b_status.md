# F3b Low-Dimensional Wearable Pareto Status

Date: 2026-07-13 (Asia/Shanghai)

## Result

The corrected low-dimensional research search is complete. The broader product goal is only partially complete: a low-burden eight-field candidate has been validated in NHANES, while a pure commercial-band sensor model and the full App-callable PIAS chain remain unimplemented.

## Requirement Reconciliation

The V1 engineering document places CatBoost and PIAS inference in the cloud and keeps the device responsible for data collection and local feature preparation. The current matching requirement is therefore:

1. Minimize practical acquisition burden while preserving useful CVD discrimination.
2. Keep `cvd-16-v1` stable until a new versioned schema is approved.
3. Keep PIAS inference behind backend and model-service APIs instead of embedding Python or model binaries in Android.

The `RehealthAI/Rehealth_AI` GitHub repository was also inspected at HEAD `8807fbc90db33d340fb4b49cabb6725869e72a03`. Its API documentation remains 16-clinical-feature oriented, its feature-distillation plan explicitly targets improvement without extra user inputs, and its historical notes contain the known 0.767/0.847 V2 AUC conflict. Those claims were treated as context only; the corrected run below is the acceptance evidence.

## Changed Files

- `train/train_cvd_lowdim_pareto.py`
- `tests/test_cvd_lowdim_pareto.py`
- `docs/LOWDIM_WEARABLE_MODEL.md`
- `docs/PIAS_INTEGRATION.md`
- `codex-runs/2026-07-13/F3b_status.md`

No Android-apk, backend, or model-service runtime files were changed.

## Training Output

Successful output:

```text
outputs/cvd_lowdim_pareto_20260713_034326
```

The first attempted output stopped during missingness masking because pandas rejected `NaN` assignment to integer columns. It was preserved under `outputs/` for audit. The robustness copy was corrected to floating point and covered by a regression test before retraining.

Selected research champion:

- Candidate: `band_profile_8`
- Fields: age, gender, bmi, exercise_days, smoking, diabetes_history, hypertension_history, family_history
- Validation AUC: 0.846071
- Validation AUC 95% bootstrap CI: [0.829438, 0.860566]
- 2021-2023 temporal holdout AUC: 0.821258
- Temporal holdout AUC 95% bootstrap CI: [0.808570, 0.833690]
- Temporal holdout AUPRC: 0.354464
- Temporal holdout Brier: 0.089510
- Calibration slope: 0.947586
- Calibration intercept: -0.038871

The artifact is marked `research_only_not_deployable_to_cvd_16_api` and was not copied to model-service.

## Validation Commands

```powershell
$PY = "C:\Users\kiki\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe"

& $PY -m pytest tests\test_cvd_lowdim_pareto.py -q
& $PY train\train_cvd_lowdim_pareto.py
& $PY -m compileall train healthagent\pias api\routers

cd D:\rehealthAI\model-service
& $PY -m pytest
& $PY -m compileall app
```

Results:

- Focused low-dimensional tests: 4 passed.
- Corrected Pareto training: passed and wrote a new timestamped output.
- rehealth-android compileall: passed.
- model-service tests: 23 passed with the existing pytest-cache permission warning.
- model-service compileall: passed.
- model-service TestClient smoke: `/health` returned `scorer_mode=real_available` and `model_available=true`; a complete CVD-16 request returned HTTP 200, `risk_score=0.0277`, `model_version=cvd-core16-catboost-20260710T173543Z`, and `is_mock=false`.
- Full rehealth-android pytest collection is blocked by missing environment packages `python-jose` and `shap`; this is not caused by the new training code. Focused tests pass.

## Product Completion Assessment

- Reviewed real Core16 scoring in model-service: complete locally.
- Corrected 6/8/10/12/16 low-dimensional comparison: complete for the available CVD-16/NHANES fields.
- Pure ordinary-band sensor model using steps, resting heart rate, SpO2, and sleep: not complete because these are not in the current production feature contract and training alignment.
- PIAS research implementation: present.
- Full typed PIAS production APIs through model-service, backend, and Android client: not complete.

## Risks

- `exercise_days` is primarily questionnaire-derived in this training source, not direct commercial-band telemetry.
- The outcome is self-reported prevalent CVD, not a prospective clinical endpoint.
- NHANES transfer to the intended Chinese population is not externally validated.
- The latest-cycle holdout was sealed within this run, but repository researchers had used these NHANES cycles in older experiments, so it is not globally pristine.
- A lower-dimensional production contract would require explicit versioning and coordinated Android/backend/model-service changes.
