# Canonical Risk Path

Status: P0b Android UI wiring.

## Flow

```text
Android local Room/ring/profile data
  -> HealthMemorySnapshot
  -> HealthFeatureExtractor
  -> CvdFeatureVector
  -> CvdFeatureVectorDtoMapper
  -> backend POST /rehealth/mobile/features/evaluate
  -> backend ModelServiceClient
  -> model-service POST /v1/cvd/risk/evaluate
  -> backend Result<RiskResultDto>
  -> Android UI risk score / risk level / contributions / model version / mock flag
```

Android does not call model-service directly. Android sends the local CVD feature vector
to the backend mobile API, and the backend remains the only boundary that calls
model-service risk evaluation.

## Endpoints Used In P0b

- `POST /rehealth/mobile/features/evaluate`: canonical risk evaluation path.

The Retrofit base URL is configured by `REHEALTH_API_BASE_URL`, defaulting in debug to:

```text
http://10.0.2.2:8080/jeecg-boot
```

## Endpoints Explicitly Not Used In P0b

- `POST /rehealth/mobile/measurements/batch`
- `POST /rehealth/mobile/ring/snapshots`
- `GET /rehealth/mobile/patient/risk-score`
- `GET /rehealth/mobile/patient/intervention-plan`
- Direct Android calls to `POST /v1/cvd/risk/evaluate`
- Raw PPG upload
- Raw RRI upload
- `ring_signal_chunks` upload

## Legacy Paths Retired From Primary UI

`ReHealthBackendClient.uploadRingSnapshot()` and
`/rehealth/mobile/ring/snapshots` remain in the codebase as legacy snapshot/debug
behavior owned outside P0b. They are no longer the primary source for the Data/Model risk
cards.

`RingViewModel` still contains legacy cloud snapshot state (`cloudRiskScore`,
`cloudRiskLevel`, `cloudRiskMode`, `cloudRiskSummary`) for existing sync behavior, but
P0b risk UI reads from the canonical feature-evaluate status instead.

## Mock And Fallback Behavior

`RemotePhmService` is the primary risk service. It evaluates a `CvdFeatureVector`
through the backend `/features/evaluate` endpoint.

`MockPhmService` remains available only as an explicit fallback when the backend,
model-service through backend, or local DTO mapping is unavailable. The UI labels fallback
as `local mock fallback` and labels backend mock responses separately as backend/cloud mock
responses. Mock values must not be presented as production risk results.

The UI preserves and displays, when available:

- risk score
- risk level
- feature contributions
- model version
- mock/fallback flag
- request id

## E2 Telemetry Note

E2 added a backend ingest boundary for `/rehealth/mobile/measurements/batch`, but it is
not durable and remains outside P0b. P0b does not integrate telemetry sync, raw signal
upload, WorkManager telemetry upload, MQ, or hardware_db persistence.

## Known Remaining Work

- F2: real CatBoost/SHAP model-service scoring and validated non-mock model versions.
- D2: durable Android telemetry upload queue and retry strategy.
- B1: release blockers for background collection remain outside P0b.
- Production auth/token handling and refresh if not already completed.
- Backend persistence for latest risk/intervention retrieval, if required by later UI
  screens.
- Canonical intervention generation/display if the backend path is promoted for Android
  UI use.
