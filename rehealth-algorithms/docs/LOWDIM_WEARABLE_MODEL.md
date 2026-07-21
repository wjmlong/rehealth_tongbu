# Low-Dimensional Wearable-First CVD Model

## Current Product Requirement

The current research goal is not simply to maximize AUC. It is to find the smallest practical input set that retains useful discrimination when the user has an ordinary commercial fitness band plus a small amount of profile and questionnaire data.

The production architecture remains:

```text
Android App -> backend -> model-service
```

The Android app collects and persists data, extracts a stable feature payload, and calls a cloud API. CatBoost, calibration, SHAP, and PIAS inference stay cloud-side. A future Android client module may wrap these API calls, but it must not embed the Python model or PIAS engine.

## Cross-Repository Evidence

The V1 engineering document in both `RehealthAI/rehealth-algorithms` and `RehealthAI/Rehealth_AI` defines cloud CatBoost plus the PIAS Predict -> Intervene -> Attribute -> Settle chain. The legacy `Rehealth_AI` documentation also records two important caveats:

- `docs/API_DOCUMENTATION.md` describes prediction from 16 clinical features, not a pure fitness-band sensor model.
- `docs/FEATURE_ENGINEERING_DISTILLATION.md` proposes improving the 16-field model without adding user inputs by deriving additional features; derived dimensions do not reduce acquisition burden.
- Historical notes conflict between V2 AUC values around 0.767 and 0.847, while higher values belong to much wider research schemas. These figures are trace evidence, not acceptance evidence for the current low-dimensional model.

For that reason, this work uses newly generated validation and temporal-holdout results instead of inheriting a historical AUC claim.

## What Can Be Tested Today

The corrected search compares these acquisition profiles:

| Candidate | Fields | Highest tier | Interpretation |
| --- | ---: | --- | --- |
| `band_profile_6` | 6 | Tier 1 | profile/questionnaire plus activity frequency |
| `band_profile_8` | 8 | Tier 1 | adds known diabetes and family history |
| `bp_assisted_10` | 10 | Tier 2 | adds validated SBP/DBP |
| `home_measurement_12` | 12 | Tier 2 | adds fasting glucose and drinking status |
| `canonical_core16` | 16 | Tier 3 | current Android/backend contract including lipid labs |

Tier definitions:

- Tier 0: profile or questionnaire.
- Tier 1: activity frequency available from an ordinary band or user record.
- Tier 2: validated blood-pressure or home measurement. Ordinary fitness bands must not be assumed to provide medically reliable SBP/DBP.
- Tier 3: clinical laboratory input.

The current CVD-16 dataset does not expose direct resting heart rate, steps, SpO2, or sleep fields through the production contract. In the NHANES training source, `exercise_days` is primarily a questionnaire-derived activity proxy rather than a synchronized commercial-band sensor stream. Therefore the 6/8-field candidates are low-burden wearable-assisted models, not pure sensor-only wristband models.

## Latest Corrected Result

Run output: `outputs/cvd_lowdim_pareto_20260713_034326`.

| Candidate | Validation AUC | Validation AUC 95% CI | Requirement |
| --- | ---: | --- | --- |
| `band_profile_6` | 0.837524 | [0.821365, 0.852871] | Tier 0/1 |
| `band_profile_8` | 0.846071 | [0.829438, 0.860566] | Tier 0/1 |
| `bp_assisted_10` | 0.844132 | [0.829180, 0.860703] | validated BP |
| `home_measurement_12` | 0.846754 | [0.832079, 0.861530] | BP plus fasting glucose |
| `canonical_core16` | 0.857228 | [0.844000, 0.870514] | labs |

The predeclared rule selected `band_profile_8`. Its 2021-2023 temporal holdout AUC was 0.821258 (95% bootstrap CI [0.808570, 0.833690]). This is evidence that a low-burden model can retain substantial discrimination in this dataset, but the temporal drop means 0.846 must not be presented as guaranteed production or clinical performance.

## Corrected Search Protocol

Run:

```powershell
$PY = "C:\Users\kiki\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe"
& $PY train\train_cvd_lowdim_pareto.py
```

The script:

1. Loads the same NHANES feature source used by the corrected Core16 pipeline.
2. Uses pre-2021 cycles for training and validation.
3. Fits imputation only inside each training pipeline.
4. Selects the Tier 0/1 champion from validation results using a predeclared rule.
5. Scores the 2021-2023 temporal holdout only after champion selection.
6. Writes a new timestamped directory without overwriting prior outputs.

Outputs include `leaderboard.csv`, `pareto_front.csv`, `feature_sets.json`, `split_manifest.json`, `REPORT.md`, `CHAMPION_SELECTION.md`, and a research-only reviewed candidate.

## Deployment Boundary

The generated low-dimensional artifact is deliberately marked `research_only_not_deployable_to_cvd_16_api`. It must not replace `model-service/models/rehealth_cvd_catboost.pkl`.

A production low-dimensional model requires a separately approved versioned contract across Android, backend, and model-service. Until then, `cvd-16-v1` remains stable and the reviewed Core16 artifact remains the production model.

## PIAS Completion Status

PIAS research components exist in `healthagent/pias`, but the full Predict -> Intervene -> Attribute -> Settle chain is not yet a production App-callable service. Current production completion is limited to real CVD prediction, conservative intervention generation, and a basic individual attribution endpoint in model-service.

The next PIAS phase should migrate reviewed capabilities behind typed model-service APIs, then expose them through backend orchestration and an Android network client. Fixed mock PIAS routes must not be treated as production behavior.
