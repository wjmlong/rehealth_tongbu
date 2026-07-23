# ReHealth Model Service API Contract

Status: frozen for F1b MVP integration, extended by F2b model-loader health metadata and M1 model governance trace metadata.

This contract is the boundary between Android D1, backend E1, and the Python model-service. It is compatible with the Android C1 feature extractor contract documented in `Android-apk/docs/FEATURE_EXTRACTOR.md`, `CvdFeatureVector.kt`, `FeatureQuality.kt`, and `HealthMemorySnapshot.kt`.

## Compatibility Notes

- Android C1 sends the canonical 16 CVD fields listed below.
- Field keys inside `featureQuality` must use snake_case names, matching `CvdFeatureFields.ALL`.
- The service accepts either a flat feature-vector body or a wrapped body: `{ "featureVector": { ... } }`.
- The service accepts Android camelCase field names at the top feature-vector level and normalizes them to snake_case.
- Backend E1 should treat response field names as stable and forward/store them without renaming unless its own DTO contract explicitly maps them.
- Backend E1 may pass `requestId` or `X-Request-ID`; model-service returns the
  selected correlation ID in the response header, `request_id`, and
  `model_trace.request_id`.
- M1 adds `model_trace` as a stable optional governance block. Backend and Android clients may ignore it initially, but should store it when audit history is required.
- Medical guidance returned by this service is conservative support only. It must not be presented as diagnosis or a replacement for clinician review.

## Canonical CVD 16 Fields

| Field | Type | Nullable | Notes |
| --- | --- | --- | --- |
| `age` | integer | yes | Adult range 18 to 120 when present. |
| `gender` | integer | yes | Binary MVP encoding, valid values `0` or `1` when present. |
| `bmi` | number | yes | Range 10 to 80 when present. |
| `sbp` | number | yes | Systolic blood pressure, 70 to 250 when present. |
| `dbp` | number | yes | Diastolic blood pressure, 40 to 150 when present; must be lower than `sbp` when both exist. |
| `fasting_glucose` | number | yes | Optional lab value; finite positive number up to 1000 when present. |
| `total_cholesterol` | number | yes | Optional lab value; finite positive number up to 1000 when present. |
| `ldl` | number | yes | Optional lab value; finite positive number up to 1000 when present. |
| `hdl` | number | yes | Optional lab value; finite positive number up to 1000 when present. |
| `triglycerides` | number | yes | Optional lab value; finite positive number up to 1000 when present. |
| `exercise_days` | integer | yes | Distinct active days in the last 7 days, 0 to 7 when present. |
| `smoking` | integer | yes | Binary MVP encoding, valid values `0` or `1` when present. |
| `drinking` | integer | yes | Binary MVP encoding, valid values `0` or `1` when present. |
| `diabetes_history` | integer | yes | Binary MVP encoding, valid values `0` or `1` when present. |
| `hypertension_history` | integer | yes | Binary MVP encoding, valid values `0` or `1` when present. |
| `family_history` | integer | yes | Binary MVP encoding, valid values `0` or `1` when present. |

Every canonical field must have a `featureQuality` entry keyed by the snake_case field name.

## Feature Quality

```json
{
  "status": "VALID",
  "source": "USER_REPORTED",
  "observedAt": 1783540800000,
  "reason": "profile"
}
```

`status` enum:

- `VALID`
- `MISSING`
- `STALE`
- `LOW_CONFIDENCE`

`source` enum:

- `REAL_DEVICE`
- `USER_REPORTED`
- `CLINICAL_REPORT`
- `DERIVED`
- `UNKNOWN`

`observedAt` is optional Unix epoch milliseconds. The Python schema also accepts `observed_at` for server-side callers, but Android and backend integrations should prefer `observedAt`.

## GET /health

Returns process liveness and safe active model metadata. Model unavailability
does not change this endpoint from HTTP 200.

Response 200:

```json
{
  "status": "ok",
  "service": "model-service",
  "model_version": "cvd-mock-rules-v1",
  "model_registry_version": "model-registry-v1",
  "feature_schema_version": "cvd-16-v1",
  "scorer_mode": "real_unavailable",
  "model_available": false,
  "model_unavailable_reason": "reviewed model is unavailable",
  "expected_model_artifacts": [
    "models/rehealth_cvd_catboost.pkl",
    "models/rehealth_v2_final.pkl"
  ],
  "supported_model_artifact_aliases": [
    "models/rehealth_cvd_catboost.pkl",
    "models/rehealth_v2_final.pkl"
  ],
  "expected_feature_order_artifacts": [
    "models/feature_cols.pkl",
    "models/feature_cols_v2.pkl",
    "models/cvd_features.json"
  ]
}
```

`scorer_mode` values:

- `mock`: explicit mock-only scorer mode.
- `real_unavailable`: real artifacts are missing or invalid; risk evaluation
  returns HTTP 503 and never executes mock scoring.
- `real_available`: real artifacts loaded successfully and model-service can return `is_mock=false` after prediction.

## GET /ready

Returns HTTP 200 only when the active mode is deployable. Production and
staging require both a real available model and the external artifact verifier
marker. Missing, invalid, or wrong-order artifacts return HTTP 503:

```json
{
  "status": "unavailable",
  "code": "model_unavailable",
  "model_version": "cvd-mock-rules-v1",
  "feature_schema_version": "cvd-16-v1",
  "scorer_mode": "real_unavailable",
  "is_mock": true
}
```

Stable readiness codes are `ready`, `model_unavailable`,
`artifact_not_verified`, `mock_forbidden`, and `demo_mock_active`.

## GET /v1/models/active

Returns safe registry/version/schema metadata, artifact basename, and readiness
state. Configured paths and raw artifact validation errors are excluded.

## GET /metrics

Returns Prometheus text format. Labels are limited to fixed `operation` and
`outcome` values; correlation IDs, authorization values, and feature fields are
not labels.

## POST /v1/cvd/risk/evaluate

Evaluates a C1 feature vector and returns a structured risk result.

Request example:

```json
{
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
  },
  "requestId": "optional-client-request-id"
}
```

Response 200:

```json
{
  "risk_score": 0.34,
  "risk_level": "moderate",
  "feature_contributions": {
    "age": 0.09,
    "gender": 0.0,
    "bmi": 0.03,
    "sbp": 0.07,
    "dbp": 0.03,
    "fasting_glucose": 0.0,
    "total_cholesterol": 0.0,
    "ldl": 0.0,
    "hdl": 0.0,
    "triglycerides": 0.0,
    "exercise_days": -0.025,
    "smoking": 0.0,
    "drinking": 0.0,
    "diabetes_history": 0.0,
    "hypertension_history": 0.08,
    "family_history": 0.04
  },
  "model_version": "cvd-mock-rules-v1",
  "is_mock": true,
  "missing_fields": [
    "fasting_glucose",
    "total_cholesterol",
    "ldl",
    "hdl",
    "triglycerides"
  ],
  "quality_warnings": [],
  "summary": "Baseline CVD risk estimate is moderate. Some fields are missing or lower confidence; complete clinical inputs before making care decisions.",
  "model_trace": {
    "feature_schema_version": "cvd-16-v1",
    "model_version": "cvd-mock-rules-v1",
    "scorer_mode": "real_unavailable",
    "fallback_reason": "model artifact missing: models/rehealth_cvd_catboost.pkl; models/rehealth_v2_final.pkl"
  }
}
```

Stable response fields:

- `risk_score`: number in the inclusive range `0.0` to `1.0`.
- `risk_level`: enum, one of `low`, `moderate`, `high`, `very_high`.
- `feature_contributions`: object keyed by CVD 16 snake_case field names.
- `model_version`: active scorer/model identifier.
- `is_mock`: boolean; `true` when deterministic mock fallback is used.
- `missing_fields`: CVD 16 field names whose value is `null`.
- `quality_warnings`: entries formatted as `<field>:<status>` for stale or low-confidence inputs.
- `summary`: conservative explanatory text, not a diagnosis.
- `request_id`: optional; returned only when the request includes `requestId`.
- `contribution_method`: optional; returned by real scorer paths when SHAP is not used or another contribution method is selected.
- `model_trace`: optional governance block for audit/version routing metadata.

`model_trace` fields:

- `feature_schema_version`: current feature contract version. The Android C1 contract is `cvd-16-v1`.
- `model_version`: same active scorer/model identifier as the top-level `model_version`.
- `artifact_name`: loaded model artifact filename when a real artifact is available; omitted in mock/unavailable paths.
- `scorer_mode`: one of `mock`, `real_unavailable`, or `real_available`.
- `fallback_reason`: why fallback/mock mode is active; omitted for real available responses.
- `request_id`: optional; mirrors top-level `request_id` when the request includes `requestId`.

## POST /v1/cvd/intervention/generate

Generates one conservative intervention plan from a risk result. The endpoint does not claim diagnosis, treatment, or guaranteed clinical outcome.

Request example:

```json
{
  "riskResult": {
    "risk_score": 0.34,
    "risk_level": "moderate",
    "feature_contributions": {"sbp": 0.07},
    "model_version": "cvd-mock-rules-v1",
    "is_mock": true,
    "missing_fields": ["fasting_glucose"],
    "quality_warnings": [],
    "summary": "Baseline CVD risk estimate is moderate.",
    "model_trace": {
      "feature_schema_version": "cvd-16-v1",
      "model_version": "cvd-mock-rules-v1",
      "scorer_mode": "real_unavailable",
      "fallback_reason": "model artifact missing"
    }
  },
  "patientContext": {}
}
```

Response 200:

```json
{
  "plan_id": "plan-6e6e7b93-5161-5d9e-b0bb-66f8dc9925fd",
  "generated_at": "2026-07-09T04:04:09.383992Z",
  "priority_intervention": "Record blood pressure at consistent morning and evening times for 3 days.",
  "rationale": "Blood pressure contributed to the estimate, and repeated measurements can reduce uncertainty.",
  "expected_impact": "May clarify whether readings are persistent or one-off variation.",
  "contraindications": [
    "chest pain",
    "dizziness",
    "unusual shortness of breath",
    "clinician-advised activity restriction"
  ],
  "confidence": 0.58,
  "model_version": "cvd-mock-rules-v1",
  "is_mock": true,
  "medical_disclaimer": "This guidance is conservative wellness support and does not diagnose disease or replace a clinician."
}
```

Stable response fields:

- `plan_id`: deterministic plan identifier for the scorer/version/risk-level/action combination.
- `generated_at`: ISO-8601 UTC timestamp.
- `priority_intervention`: primary conservative action.
- `rationale`: why the action was selected.
- `expected_impact`: cautious non-guaranteed impact statement.
- `contraindications`: safety conditions that should stop or redirect the action.
- `confidence`: number in the inclusive range `0.0` to `1.0`.
- `model_version`: copied from the risk result.
- `is_mock`: copied from the risk result.
- `medical_disclaimer`: conservative safety disclaimer.

## Error Behavior

FastAPI/Pydantic returns HTTP 422 for schema validation errors, including:

- Missing `featureQuality` entries for any CVD 16 field.
- Unknown enum values for `FeatureQuality.status`, `FeatureQuality.source`, or `risk_level`.
- Values outside documented ranges.
- `sbp` less than or equal to `dbp` when both are present.
- Non-finite or non-positive lab values.
- `risk_score` or `confidence` outside `0.0` to `1.0`.

The service does not log raw request payloads.

Runtime real-model failures return HTTP 503 with:

```json
{
  "detail": {
    "code": "model_unavailable",
    "message": "model artifact must expose predict_proba or predict"
  }
}
```

## Mock Scorer Behavior

When no real model artifact exists, `load_risk_scorer()` returns `MockRiskScorer`.

- `model_version`: `cvd-mock-rules-v1`
- `is_mock`: `true`
- `scorer_mode`: `real_unavailable` when the configured real artifacts are missing or invalid.
- Deterministic output for the same feature vector.
- Conservative text only; no clinical claims.
- Missing fields are accepted and reported in `missing_fields`.

## Future Real Model Artifact

The reserved artifact path is:

```text
models/rehealth_cvd_catboost.pkl
models/rehealth_v2_final.pkl
models/feature_cols.pkl
models/feature_cols_v2.pkl
models/cvd_features.json
models/model_meta_v2.json
models/model_metadata.json
```

If a model artifact and feature-order artifact exist, `load_risk_scorer()` validates that the feature-order artifact exactly matches the Android C1 CVD 16 order before creating `RealCatBoostRiskScorer`. Artifact paths must stay under the local `models/` root because pickle-compatible artifacts can execute code during load. Production responses with `is_mock=false` must not be enabled until the artifact, preprocessing pipeline, probability calibration, contribution extraction, and model version naming are validated and documented.

Future feature expansion must use a new `feature_schema_version` such as `cvd-97-v2` instead of changing the `cvd-16-v1` semantics in place.
