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

The Retrofit base URL is configured by `REHEALTH_API_BASE_URL`, defaulting in debug to
(the committed `gradle.properties` value for internal testing):

```text
https://rehealth.youngjimmy.store/jeecg-boot/
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

`ReHealthBackendClient.uploadRingSnapshot()` 与 `/rehealth/mobile/ring/snapshots` 已从
Android 代码删除；它们不再出现在任何生产或 Debug 路径中，仅存在于历史文档。

`RingViewModel` 仍保留旧版云端快照状态字段（`cloudSnapshotId`、`cloudRiskScore`、
`cloudRiskLevel`、`cloudRiskMode`、`cloudRiskSummary`、`patientMvp` 等，
见 `ring/RingViewModel.kt` 的 `RingUiState`），属于未清理的遗留状态；
P0b 风险 UI 只读取 canonical feature-evaluate 状态，不消费这些字段。

## Mock And Failure Behavior

`RemotePhmService` is the primary risk service. It evaluates a `CvdFeatureVector`
through the backend `/features/evaluate` endpoint.

Backend, model-service, and local DTO mapping failures produce an explicit unavailable
state and no local risk score. A backend response with `is_mock=true` remains visible as
cloud mock output and is excluded from confirmed risk history and attribution input.

The UI preserves and displays, when available:

- risk score
- risk level
- feature contributions
- model version
- backend mock flag or explicit unavailable state
- request id

## E2 Telemetry Note

E2/E2.1 已实现 durable 遥测接入：`/rehealth/mobile/measurements/batch` 经 Gateway 路由到
Device Service/TimescaleDB，事务提交后返回 `ACCEPTED_PERSISTED`；该路径与 P0b 风险路径
保持分离，接入不触发评分。详见 `D2_TELEMETRY_SYNC_PLAN.md`。

## Known Remaining Work

- F2: real CatBoost/SHAP model-service scoring and validated non-mock model versions.
  **Done (2026-07-30 smoke test):** `POST /v1/cvd/risk/evaluate` returns
  `is_mock=false`, `scorer_mode=real_available`,
  `model_version=cvd-core16-catboost-20260710T173543Z`, real SHAP contributions
  (`contribution_method=shap_via_catboost`). The mobile `features/evaluate` path
  reaches this via JeecgBoot `model-service.base-url=http://127.0.0.1:8000`
  (application-dev.yml).
- D2: durable Android telemetry upload queue and retry strategy — **已实现**（见
  `D2_TELEMETRY_SYNC_PLAN.md`）。
- B1: background collection 已实现；物理锁屏、重启、功耗与准确性门禁仍未解除
  （见 `STATUS.md` 与 `BLE_BACKGROUND_QA.md`）。
- 生产认证/token 处理：无 refresh-token 流程，401 暂停队列并要求重新登录（已实现）。
- 风险/干预结果的 software_db 持久化与读取已实现（`/risk/latest`、
  `/risk/history`、`/interventions/today`）。
- 干预生成已上线：`POST /rehealth/mobile/interventions/generate` 由 JeecgBoot
  LangChain4j 以服务端权威上下文生成结构化计划。
