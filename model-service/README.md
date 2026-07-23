# ReHealth Model Service

Standalone FastAPI service for the ReHealth Android MVP model boundary.

## Scope

This service owns cloud-side CVD risk scoring and conservative intervention generation. It accepts the Android C1 feature contract from:

- `Android-apk/docs/FEATURE_EXTRACTOR.md`
- `Android-apk/app/src/main/java/com/rehealth/genie/features/CvdFeatureVector.kt`
- `Android-apk/app/src/main/java/com/rehealth/genie/features/FeatureQuality.kt`
- `Android-apk/app/src/main/java/com/rehealth/genie/features/HealthMemorySnapshot.kt`

The runtime loads a reviewed local CVD-16 CatBoost artifact when canonical files are present. When artifacts are absent or invalid, it falls back to `cvd-mock-rules-v1`, a deterministic mock scorer. Responses include `is_mock` so clients cannot mistake fallback scoring for real model output.

## API

- `GET /health`
- `POST /v1/cvd/risk/evaluate`
- `POST /v1/cvd/intervention/generate`

`/v1/cvd/risk/evaluate` accepts either a flat Android feature vector body or `{ "featureVector": { ... } }`. The service accepts Android camelCase names and snake_case names for the CVD fields.

The frozen MVP API contract is documented in `docs/API_CONTRACT.md`.
Real model artifact requirements, F2 safety gates, historical artifact traces, and M1 model registry behavior are documented in `docs/REAL_MODEL_INTEGRATION.md`, `docs/MODEL_ARTIFACTS.md`, `docs/REHEALTH_ALGORITHMS_MODEL_TRACE.md`, and `docs/MODEL_REGISTRY.md`.

## Run

```powershell
cd D:\rehealthAI\model-service
python -m pip install -r requirements.txt
python -m uvicorn app.main:app --reload
```

## Test

```powershell
cd D:\rehealthAI\model-service
python -m pytest
```

## Docker

```powershell
cd D:\rehealthAI\model-service
docker build -t rehealth-model-service .
docker run --rm -p 8000:8000 rehealth-model-service
```

## Curl Examples

Health:

```bash
curl http://127.0.0.1:8000/health
```

Risk evaluation:

```bash
curl -X POST http://127.0.0.1:8000/v1/cvd/risk/evaluate \
  -H "Content-Type: application/json" \
  -d '{
    "featureVector": {
      "age": 52,
      "gender": 1,
      "bmi": 27.4,
      "sbp": 136.0,
      "dbp": 86.0,
      "fasting_glucose": null,
      "total_cholesterol": null,
      "ldl": null,
      "hdl": null,
      "triglycerides": null,
      "exercise_days": 3,
      "smoking": 0,
      "drinking": 0,
      "diabetes_history": 0,
      "hypertension_history": 1,
      "family_history": 1,
      "featureQuality": {
        "age": {"status": "VALID", "source": "USER_REPORTED", "reason": "profile"},
        "gender": {"status": "VALID", "source": "USER_REPORTED", "reason": "profile"},
        "bmi": {"status": "VALID", "source": "USER_REPORTED", "reason": "profile"},
        "sbp": {"status": "VALID", "source": "REAL_DEVICE", "reason": "latest ring blood pressure"},
        "dbp": {"status": "VALID", "source": "REAL_DEVICE", "reason": "latest ring blood pressure"},
        "fasting_glucose": {"status": "MISSING", "source": "UNKNOWN", "reason": "not provided"},
        "total_cholesterol": {"status": "MISSING", "source": "UNKNOWN", "reason": "not provided"},
        "ldl": {"status": "MISSING", "source": "UNKNOWN", "reason": "not provided"},
        "hdl": {"status": "MISSING", "source": "UNKNOWN", "reason": "not provided"},
        "triglycerides": {"status": "MISSING", "source": "UNKNOWN", "reason": "not provided"},
        "exercise_days": {"status": "VALID", "source": "DERIVED", "reason": "7-day activity summary"},
        "smoking": {"status": "VALID", "source": "USER_REPORTED", "reason": "profile"},
        "drinking": {"status": "VALID", "source": "USER_REPORTED", "reason": "profile"},
        "diabetes_history": {"status": "VALID", "source": "USER_REPORTED", "reason": "profile"},
        "hypertension_history": {"status": "VALID", "source": "USER_REPORTED", "reason": "profile"},
        "family_history": {"status": "VALID", "source": "USER_REPORTED", "reason": "profile"}
      }
    }
  }'
```

Intervention generation:

```bash
curl -X POST http://127.0.0.1:8000/v1/cvd/intervention/generate \
  -H "Content-Type: application/json" \
  -d '{
    "riskResult": {
      "risk_score": 0.34,
      "risk_level": "moderate",
      "feature_contributions": {"sbp": 0.07},
      "model_version": "cvd-mock-rules-v1",
      "is_mock": true,
      "missing_fields": ["fasting_glucose"],
      "quality_warnings": [],
      "summary": "Baseline CVD risk estimate is moderate."
    }
  }'
```

## Model Artifact Placeholder

The current reviewed local candidate is `cvd-core16-catboost-20260710T173543Z` with canonical artifact name `rehealth_cvd_catboost.pkl`. Local artifacts are excluded from git; see `docs/MODEL_ARTIFACTS.md` for provenance, hashes, calibration, and reproduction commands.

`app.risk_scorer.load_risk_scorer()` falls back to `MockRiskScorer` when a real model is unavailable. A real scorer can be enabled only when a local model artifact and a local feature-order artifact exist under the local `models/` directory and validate.

Protected runtime modes also require
`REHEALTH_MODEL_SERVICE_BASE_URL` to be an HTTPS URL without embedded
credentials and `REHEALTH_PROVIDER_CREDENTIAL_FILE` to name the externally
mounted provider credential. `REHEALTH_PROVIDER_SECRET` is rejected in
production and staging; provider values must not be embedded in environment
configuration.

Model artifact search order:

```text
models/rehealth_cvd_catboost.pkl
models/rehealth_v2_final.pkl
```

Feature-order artifact search order:

```text
models/feature_cols.pkl
models/feature_cols_v2.pkl
models/cvd_features.json
```

`feature_cols.pkl`, `feature_cols_v2.pkl`, or `cvd_features.json` must exactly match the Android C1 CVD 16 feature order. `models/model_meta_v2.json` or `models/model_metadata.json` is optional but recommended for `model_version`. The service does not hardcode old AUC values such as `0.847`; AUC is omitted unless verified metadata provides it.

`GET /health` reports `scorer_mode` as `real_unavailable`, `real_available`, or `mock`. The service must not return `is_mock=false` unless the real model artifact loads and scores successfully.

## Model Governance

The service keeps real model governance in the cloud boundary, not in the Android APK. Risk responses include a `model_trace` block for audit and version routing:

```json
{
  "feature_schema_version": "cvd-16-v1",
  "model_version": "cvd-mock-rules-v1",
  "scorer_mode": "real_unavailable",
  "fallback_reason": "model artifact missing"
}
```

When a real artifact is validated and predicts successfully, `model_trace.artifact_name` identifies the loaded artifact filename. Android and backend integrations can keep calling the stable CVD 16 API while model-service owns model versioning, fallback state, and future schema routing.

## Privacy and Safety

- Request payloads are not logged.
- Raw PII fields are not part of the model-service contract.
- Intervention text is conservative wellness support and includes a medical disclaimer.
