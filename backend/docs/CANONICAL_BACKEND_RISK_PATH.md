# 标准后端风险路径 P0c

日期：2026-07-09
状态：P0c 后端旧路径退役。

## 标准流程

```text
Android
  -> POST /rehealth/mobile/features/evaluate
  -> ReHealthMobileController.evaluateFeatures
  -> ReHealthMobileService.evaluateFeatures
  -> ModelServiceClient.evaluateRisk
  -> POST model-service /v1/cvd/risk/evaluate
  -> 后端 Result<RiskEvaluateResponseDto>
  -> Android UI
```

`/rehealth/mobile/features/evaluate` 是移动端 CVD 风险评分唯一的生产型后端风险评估入口。Java 后端负责编排请求和响应，不实现 CatBoost、SHAP、LLM、因果归因或 Java 侧降级评分。

## Model Service 权威边界

`ModelServiceClient` 是后端调用算法的边界。

生产风险所需目标：

- `GET /health`
- `POST /v1/cvd/risk/evaluate`

`POST /v1/cvd/intervention/generate` 仅作为兼容目标保留。标准移动端个性化计划端点现在由 Jeecg 组装最新授权上下文并使用 LangChain4j。生产归因由 PIAS 而非 model-service 负责。

开发环境配置：

```yaml
rehealth:
  model-service:
    base-url: http://127.0.0.1:8000
    timeout-seconds: 10
```

旧的 `rehealth-algorithms` PIAS API 不是生产评分服务。后端生产路径不得调用 `rehealth-algorithms` 的 `/api/pias/predict` 或 `/api/pias/v2/predict`。

## 已退役的旧路径

以下路径不是生产后端风险或干预路径：

| 旧路径 | P0c 状态 |
| --- | --- |
| `POST /rehealth/mobile/ring/snapshots` | 已从后端生产风险评分中退役。`jeecg-module-rehealth` 中不存在该路径；Android 的旧版或 Debug 引用不得视为生产风险上传。 |
| `GET /rehealth/mobile/patient/risk-score` | 已退役。`jeecg-module-rehealth` 中不存在该路径；评估应使用 `POST /features/evaluate`，只有读取已持久化的最新结果时才使用 `GET /risk/latest`。 |
| `GET /rehealth/mobile/patient/intervention-plan` | 已退役。`jeecg-module-rehealth` 中不存在该路径；使用已认证的 `POST /interventions/generate`，基于最新服务端上下文通过 LangChain4j 生成计划；只有读取已持久化的最新计划时才使用 `GET /interventions/today`。 |
| `POST /api/pias/predict` | 已停止用于后端生产。它是 `rehealth-algorithms` 的原型路径，不符合 model-service API 契约。 |
| `POST /api/pias/v2/predict` | 已停止用于后端生产。后端必须改用 `ModelServiceClient` 和 model-service 的 `/v1/cvd/**` 契约。 |

历史文档可以提及这些字符串，以说明其移除原因。新的生产代码不得将其暴露为有效的移动评分端点。

## 遥测分离

`POST /rehealth/mobile/measurements/batch` 属于 E2 遥测接入，与 P0c 风险评估分离，不得同步触发生产风险评分。当前后端路由通过 `HardwareIngestionPort` 以及显式的开发队列/写入器边界校验并接受遥测，不调用 `ModelServiceClient`。

## 认证边界

只有 `GET /rehealth/mobile/health` 有意标记为 `@IgnoreAuth`。包括 `/features/evaluate` 在内的生产型 ReHealth 移动端点必须使用 JeecgBoot 的常规认证流程。

## 验证快照

P0c 源码检查确认：

- `/rehealth/mobile/features/evaluate` 存在于 `jeecg-module-rehealth`。
- `HttpModelServiceClient` 将风险评估发送至 `/v1/cvd/risk/evaluate`。
- `HttpModelServiceClient` 仅为兼容保留 `/v1/cvd/intervention/generate`；移动端生成服务不使用客户端健康上下文，也不使用该兼容调用。
- 未使用的 `jeecg-module-demo` 运行时已移除；ReHealth 路由仅存在于 `jeecg-module-rehealth`。
- `application-dev.yml` 使用 `rehealth.model-service.base-url`，而不是旧算法地址。
- 旧路由字符串仅存在于历史记录或文档中，不是有效的生产后端路径。
