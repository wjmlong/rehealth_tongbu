# Model Registry And Contract Versioning

Status: M1 model governance skeleton.

This document freezes the model-service direction:

```text
Android App -> backend -> model-service
```

The Android app owns collection, local persistence, CVD 16 feature extraction, display, and local fallback. The model-service owns real model loading, model version metadata, artifact identity, fallback state, and future model routing.

## Current Registry

Current registry version:

```text
model-registry-v1
```

Current feature schema version:

```text
cvd-16-v1
```

Current active scorer modes:

| Mode | Meaning |
| --- | --- |
| `mock` | Explicit direct mock scorer. Used in local tests or intentionally configured mock paths. |
| `real_unavailable` | Real artifact boundary is configured, but model artifacts are missing or invalid, so mock fallback is active. |
| `real_available` | A local reviewed model artifact and feature-order artifact loaded successfully. Risk responses may return `is_mock=false` only after prediction succeeds. |

## Risk Response Trace

Risk responses include a `model_trace` block:

```json
{
  "feature_schema_version": "cvd-16-v1",
  "model_version": "cvd-mock-rules-v1",
  "artifact_name": null,
  "scorer_mode": "real_unavailable",
  "fallback_reason": "model artifact missing: models/rehealth_cvd_catboost.pkl; models/rehealth_v2_final.pkl",
  "request_id": "optional-client-request-id"
}
```

Field policy:

- `feature_schema_version` is the feature contract version used for scoring.
- `model_version` is the active scorer/model identifier.
- `artifact_name` is present only when a real artifact is loaded.
- `scorer_mode` describes the active scorer path.
- `fallback_reason` is present for mock/fallback paths and omitted for real available paths.
- `request_id` mirrors top-level `request_id` when backend passes `requestId`.

Top-level response fields remain available for simple clients:

```text
risk_score
risk_level
feature_contributions
model_version
is_mock
missing_fields
quality_warnings
summary
request_id
contribution_method
model_trace
```

## Artifact Manifest

Do not commit model binaries to git. A future artifact handoff may provide a local manifest next to the reviewed artifacts:

```text
models/model_manifest.json
```

Proposed manifest shape:

```json
{
  "registry_version": "model-registry-v1",
  "models": [
    {
      "model_version": "cvd-catboost-v2",
      "feature_schema_version": "cvd-16-v1",
      "artifact_name": "rehealth_cvd_catboost.pkl",
      "feature_order_artifact": "feature_cols.pkl",
      "metadata_artifact": "model_meta_v2.json",
      "status": "candidate",
      "notes": "Reviewed 16-feature CatBoost artifact."
    }
  ]
}
```

The manifest must not be treated as proof that a model is real or safe. Runtime still needs to validate:

- Artifact path stays under the local `models/` root.
- Feature order exactly matches the declared `feature_schema_version`.
- The model artifact loads from a reviewed local file.
- The model exposes `predict_proba` or `predict`.
- Prediction succeeds and returns a finite score that can be clamped to `0.0` through `1.0`.

## Feature Schema Versioning

The current Android C1 CVD 16 contract is:

```text
cvd-16-v1
```

Fields:

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

Future V8/97-feature work must not mutate `cvd-16-v1` in place. It should introduce a new schema version, for example:

```text
cvd-97-v2
```

The backend and Android app can continue using `/v1/cvd/risk/evaluate`; model-service should route by `feature_schema_version` once multi-schema requests are explicitly introduced.

## Gray Release Policy

M1 does not enable gray routing yet. Future gray routing should be driven by backend/model-service configuration and recorded in `model_trace`.

Minimum future fields:

```text
route_key
cohort
model_version
feature_schema_version
artifact_name
```

Do not implement random client-side model selection in Android.

## Android And Backend Pass-through Policy（2026-07-10 model_trace_contract）

Decision summary: pass-through store. M1 已经把 `model_trace` 加入 model-service risk response，作为顶级可选（nullable）的 snake_case JSON 字段。Android 与 backend 对该字段的处理策略统一为：保留 + 透传 + 不阻塞 + 不删除 + 不强依赖。本章节记录三端的字段对应清单与处理纪律，供各端 subagent 对齐契约。

### Three-side field mapping

| 端 | 字段 | 类型 | 必填 | 持久化 | 当前 UI 显示 |
| --- | --- | --- | --- | --- | --- |
| model-service | `response.model_trace` | `ModelTrace` (pydantic) / nullable | 否（M1 已加；mock 路径也返回） | 不持久化（M1 决定） | 不显示 |
| backend (P0c-E2 followup) | `RiskEvaluateResponseDto.modelTrace` | `ModelTraceDto` / nullable | 否 | 不持久化（M1 followup，不写 software_db） | 不显示 |
| Android | `RiskResultDto.model_trace` / `modelTrace` / `normalizedModelTrace` | `ModelTraceDto` / nullable | 否 | 不持久化（与 client-side 状态绑定，不写 Room） | 不在 P0b UI 高亮（保留作后续 track） |

### Handling discipline

- 句柄说明：backend FastJSON 自动递归解析嵌套 JSON 对象，调用方无需手写 mapping。即 model-service 返回 snake_case 嵌套对象时，后端 DTO 只需声明 `ModelTraceDto modelTrace`，反序列化由框架完成，无需在 controller/service 层写字段拷贝。
- Android Moshi codegen 自动生成 adapter：客户端 DTO 声明 `@Json(name = "model_trace") val modelTrace: ModelTraceDto?` 后，Moshi codegen 生成对应 adapter；客户端可在 ViewModel 中读取 `normalizedModelTrace`（未来在 ModelScreen 显示 `model_version` / `scorer_mode` 时使用，P0b 不强制显示）。
- 不阻塞：当 backend / model-service 因任何原因不返回 `model_trace` 时，所有端都把该字段当 null；旧客户端在新增 `model_trace` 之前已部署且工作正常，新增字段不得破坏旧客户端反序列化与渲染。
- 不强依赖：UI 不依赖 `model_trace` 字段才能渲染分数；后端不依赖 `model_trace` 才能反序列化成功；任何一端缺字段都视为 nullable 降级，不抛 required 错误。
- 不持久化：M1 决定 `model_trace` 仅是 per-request 的 governance 信息，不写任何 database 表（避免每次评分都写 software_db）。如未来要持久化趋势对比，应另起 track（如 D2 / M2），并在此章节补充迁移与表设计说明。
- 不删除：客户端依赖 snake_case key `model_trace`，backend 输出层必须保留 `model_trace` 而非 `modelTrace`（详见下方 Safety Rules 同条约束）。

### Mock path early value

生成实测的 mock 路径：`MockRiskScorer.evaluate` 也返回 `model_trace`（M1 已实现）。在 `GET /health` 与 `POST /v1/cvd/risk/evaluate` 路径下，可在 `model_trace` 块看到 `scorer_mode=real_unavailable`、`model_version=cvd-mock-rules-v1`、`fallback_reason = model artifact missing: models/rehealth_cvd_catboost.pkl; models/rehealth_v2_final.pkl`。这意味 Android 现在就能在 mock 模式下从后端获取完整 trace 信息——这是 M1 存在的早期价值，让前端/后端在真实 artifact 移交前即可验证 pass-through 链路正确性。

## Safety Rules

- Do not train models in model-service unless explicitly approved.
- Do not copy or commit model binaries without explicit approval.
- Do not return `is_mock=false` unless a real artifact loads and predicts.
- Do not hardcode historical AUC values such as `0.847`.
- Keep medical text conservative and non-diagnostic.
- Keep Android on stable APIs; real model changes happen in model-service.
- 不修改或删除 `model_trace` 顶级 snake_case JSON 字段 key 名称；客户端依赖 snake_case key `model_trace`（backend 输出 `model_trace` 而非 `modelTrace`）。
