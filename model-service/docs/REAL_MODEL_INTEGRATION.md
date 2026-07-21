# Real Model Integration

Status: real CVD-16 scoring is locally validated with the reviewed artifact `cvd-core16-catboost-20260710T173543Z`.

## Training Integrity

The deployment candidate is produced by `rehealth-algorithms/train/train_cvd_retrain.py` with these controls:

- Stratified 71.2% final train, 16% calibration validation, and 12.8% final lockbox partitions.
- Imputation is embedded in the sklearn pipeline and fitted on the training partition only.
- CatBoost configuration and the logistic sanity baseline are evaluated on validation data.
- Logistic regression uses an imputer and `StandardScaler` in the same pipeline for fit and prediction.
- Platt sigmoid calibration is fitted on validation sample-level binary outcomes and frozen CatBoost probabilities.
- The final lockbox was isolated from the prior train-only pool and accessed once after the model and calibration method were frozen.
- The earlier seed-42 test used during pipeline repair is excluded from final evaluation and reused only as final training data.
- Calibration failure is represented by a null value and reason; ideal values are never substituted.

The deployment choice remains `core16_catboost`. Research feature sets do not participate because they would change the Android/backend CVD-16 contract.

## Runtime Dependencies

Real artifact loading requires:

```text
catboost>=1.2.8,<1.3
joblib==1.5.1
scikit-learn==1.7.1
```

CatBoost pulls its numeric runtime dependencies. SHAP is intentionally optional and is not required for real prediction.

## Loader Gate

`app.risk_scorer.load_risk_scorer()` enables `is_mock=false` only after all checks pass:

1. A supported model artifact exists under the local artifact root.
2. A supported feature-order artifact exists under the same root.
3. The order exactly matches Android C1 `cvd-16-v1`.
4. The reviewed local artifact loads through `joblib`.
5. It exposes `predict_proba` or `predict`.
6. Prediction succeeds and yields a finite probability in `[0, 1]`.

Any startup validation failure keeps `MockRiskScorer` active with `scorer_mode=real_unavailable` and `is_mock=true`. A failure after startup raises the existing model-unavailable API error; it is not relabeled as real scoring.

## Runtime Artifact Names

Preferred names:

```text
models/rehealth_cvd_catboost.pkl
models/feature_cols.pkl
models/model_meta_v2.json
```

Historical aliases remain supported:

```text
models/rehealth_v2_final.pkl
models/feature_cols_v2.pkl
models/cvd_features.json
models/model_metadata.json
```

Environment overrides remain constrained to the local artifact root:

```text
REHEALTH_CVD_ARTIFACT_ROOT=models
REHEALTH_CVD_MODEL_PATH=models/rehealth_cvd_catboost.pkl
REHEALTH_CVD_FEATURE_ORDER_PATH=models/feature_cols.pkl
REHEALTH_CVD_MODEL_META_PATH=models/model_meta_v2.json
```

## Current Runtime Result

With the reviewed local files present, `/health` reports:

```json
{
  "model_version": "cvd-core16-catboost-20260710T173543Z",
  "feature_schema_version": "cvd-16-v1",
  "scorer_mode": "real_available",
  "model_available": true,
  "loaded_artifact_name": "rehealth_cvd_catboost.pkl"
}
```

A valid CVD-16 request, including nullable lab values, returns a finite score and `is_mock=false`.

## Contributions And Safety

SHAP is not included in this candidate. Real prediction is active, while `feature_contributions` remains the existing deterministic zero fallback with `contribution_method=deterministic_zero_fallback`. This limitation is explicit and must not be described as an explanation of individual clinical risk.

The model uses self-reported prevalent CVD outcomes from NHANES and has not been clinically validated for diagnosis or for transfer to Chinese populations. Android continues to call backend, and backend calls model-service; the model is not packaged into the APK.
