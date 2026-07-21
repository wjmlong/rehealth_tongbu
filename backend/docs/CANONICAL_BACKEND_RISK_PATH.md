# Canonical Backend Risk Path P0c

Date: 2026-07-09
Status: P0c backend legacy path retirement.

## Canonical Flow

```text
Android
  -> POST /rehealth/mobile/features/evaluate
  -> ReHealthMobileController.evaluateFeatures
  -> ReHealthMobileService.evaluateFeatures
  -> ModelServiceClient.evaluateRisk
  -> POST model-service /v1/cvd/risk/evaluate
  -> backend Result<RiskEvaluateResponseDto>
  -> Android UI
```

`/rehealth/mobile/features/evaluate` is the only production-style backend risk
evaluation entry point for mobile CVD risk scoring. The Java backend orchestrates
the request and response; it does not implement CatBoost, SHAP, LLM, causal
attribution, or Java-side fallback scoring.

## Model-Service Authority

`ModelServiceClient` is the backend boundary for algorithm calls.

Required model-service targets:

- `GET /health`
- `POST /v1/cvd/risk/evaluate`
- `POST /v1/cvd/intervention/generate`
- `POST /v1/cvd/attribution/individual`

Dev configuration uses:

```yaml
rehealth:
  model-service:
    base-url: http://127.0.0.1:8000
    timeout-seconds: 10
```

The old `rehealth-algorithms` PIAS API is not a production scoring service. Backend
production paths must not call `rehealth-algorithms` `/api/pias/predict` or
`/api/pias/v2/predict`.

## Retired Legacy Paths

The following paths are not production backend risk or intervention paths:

| Legacy path | P0c status |
| --- | --- |
| `POST /rehealth/mobile/ring/snapshots` | Retired from backend production risk scoring. It does not exist in `jeecg-module-rehealth`; Android legacy/debug references must not be treated as production risk upload. |
| `GET /rehealth/mobile/patient/risk-score` | Retired. It does not exist in `jeecg-module-rehealth`; use `POST /features/evaluate` for evaluation and `GET /risk/latest` only for persisted latest result reads. |
| `GET /rehealth/mobile/patient/intervention-plan` | Retired. It does not exist in `jeecg-module-rehealth`; use `POST /interventions/generate` for model-service generation and `GET /interventions/today` only for persisted latest plan reads. |
| `POST /api/pias/predict` | Retired from backend production use. This was a prototype `rehealth-algorithms` path and does not match the model-service API contract. |
| `POST /api/pias/v2/predict` | Retired from backend production use. Backend must use `ModelServiceClient` and the model-service `/v1/cvd/**` contract instead. |

Historical docs may mention these strings to explain why they were removed. New
production code must not expose them as active mobile scoring endpoints.

## Telemetry Separation

`POST /rehealth/mobile/measurements/batch` belongs to E2 telemetry ingest. It is
separate from P0c risk evaluation and must not trigger synchronous production
risk scoring. The current backend route validates and accepts telemetry through
`HardwareIngestionPort` and an explicit dev queue/writer boundary; it does not
call `ModelServiceClient`.

## Auth Boundary

Only `GET /rehealth/mobile/health` is intentionally marked `@IgnoreAuth`.
Production-style ReHealth mobile endpoints, including `/features/evaluate`,
must use the normal JeecgBoot authentication flow.

## Verification Snapshot

P0c source inspection confirmed:

- `/rehealth/mobile/features/evaluate` exists in `jeecg-module-rehealth`.
- `HttpModelServiceClient` posts risk evaluation to `/v1/cvd/risk/evaluate`.
- `HttpModelServiceClient` posts intervention generation to `/v1/cvd/intervention/generate`.
- `jeecg-module-demo` contains no ReHealth Java mobile routes.
- `application-dev.yml` uses `rehealth.model-service.base-url`, not the old algorithm URL.
- Legacy route strings are historical/docs-only and are not active production backend paths.
