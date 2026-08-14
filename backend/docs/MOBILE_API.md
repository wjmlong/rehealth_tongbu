# ReHealth 移动 API E1

状态：E1 实现契约。
模块：`jeecg-boot/jeecg-boot-module/jeecg-module-rehealth`。

## 边界

ReHealth 生产后端代码位于 `jeecg-module-rehealth`；本仓库运行时不包含上游 Demo 模块。

Java 后端负责 API、持久化边界和编排。Python `model-service` 仍是 CVD 风险/SHAP 的权威服务，PIAS 仍是归因权威服务。认证后的个性化健康计划路径是 Jeecg 中明确的 LangChain4j 用例：它组装有界的权威上下文并生成结构化、保守的行动，但不实现 CatBoost、SHAP、诊断、用药调整或因果归因。

## 标准风险路径

标准后端风险路径：

```text
Android
  -> POST /rehealth/mobile/features/evaluate
  -> ReHealthMobileService
  -> ModelServiceClient
  -> POST model-service /v1/cvd/risk/evaluate
  -> 后端响应
  -> Android UI
```

`POST /rehealth/mobile/features/evaluate` 是唯一的生产型移动风险评估入口。后端 Java 不会调用 `rehealth-algorithms` 的 `/api/pias/predict` 或 `/api/pias/v2/predict` 进行生产评分。

## 基础路径

单体开发环境 URL：

```text
http://localhost:8080/jeecg-boot/rehealth/mobile
```

控制器映射：

```text
/rehealth/mobile
```

只有 `GET /rehealth/mobile/health` 标记了 `@IgnoreAuth`。所有生产型移动端点都使用 JeecgBoot 常规认证流程。

保险计划接口同样只接受当前 Jeecg 登录身份。`tenantId` 仅用于声明要加入的保险计划，服务端会重新校验当前用户是该租户成员、投保人映射存在、保单有效且授权版本有效。App 只回传计划标识、授权证据引用、完成率、依从性和有界结果摘要；原始心率、血压、睡眠、ECG、手机号和身份证号不得进入保险接口。Android 当前已接入类型化网络客户端，Compose 授权/撤回页面与离线反馈队列尚未完成。

## 端点

| 方法 | 路径 | E1 行为 |
| --- | --- | --- |
| `GET` | `/rehealth/mobile/config` | 返回 API 版本、端点列表、模型契约和 E1 限制。 |
| `GET` | `/rehealth/mobile/profile` | 读取当前认证用户已持久化的健康档案。 |
| `PUT` | `/rehealth/mobile/profile` | 新增或更新类型化档案字段，在服务端计算 BMI 并返回 `version`；版本过期时返回 `409`，`software_db` 禁用时返回可重试的 `503`。 |
| `GET` | `/rehealth/mobile/rhi/manual-inputs` | 从 `software_db` 读取当前认证用户可空的 RHI 手工健康档案值；没有记录时返回 `null`。 |
| `PUT` | `/rehealth/mobile/rhi/manual-inputs` | 校验并新增或更新久坐时长、腰围、正式 VO₂max、HbA1c、eGFR、经确认的袖带均值和带日期化验值。数据归属只来自认证；较旧的 `updatedAt` 会返回较新的已存副本而不覆盖它。 |
| `POST` | `/rehealth/mobile/interviews` | 在当前认证用户下持久化类型化回答和基线摘要。 |
| `GET` | `/rehealth/mobile/interviews/latest` | 读取当前认证用户最新的已持久化访谈。 |
| `POST` | `/rehealth/mobile/devices/bind` | 当 `software_db` 启用时持久化当前认证用户的绑定。 |
| `POST` | `/rehealth/mobile/insurance/plans/bind` | 在当前用户、租户、有效保单和授权记录全部匹配时绑定保险健康计划。 |
| `GET` | `/rehealth/mobile/insurance/plans/current` | 返回当前认证用户的有效保险计划绑定；没有绑定时返回 `null`。 |
| `POST` | `/rehealth/mobile/insurance/plans/{bindingId}/feedback` | 按 `sourceRecordId` 幂等回传干预完成率、依从性和有界结果摘要。 |
| `POST` | `/rehealth/mobile/viomi/bind` | 使用服务端云米账号验证 IMEI，并持久化哈希后的 ReHealth 绑定。 |
| `POST` | `/rehealth/mobile/viomi/sync` | 在最长 31 天的 Epoch 毫秒时间窗内拉取心率、血压和血氧历史；将无偏移量的厂商时间戳按 Asia/Shanghai 解释，拒绝无效生理值，通过硬件接入完成持久化后返回规范化记录。 |
| `POST` | `/rehealth/mobile/measurements/batch` | 经 Gateway 路由的 Device Service 权威端校验 `telemetry-v2`，并在事务中将测量、睡眠、活动和饮食记录写入 TimescaleDB；重复重试返回现有收据。 |
| `GET` | `/rehealth/mobile/measurements/recent?limit=50` | 只读取当前认证用户最新的规范化测量、睡眠和活动记录；`limit` 被限制在 1–200，且绝不返回原始信号载荷。规范化记录包含稳定的来源记录 `id`，使 Android 登录后能幂等恢复到 Room。 |
| `POST` | `/rehealth/mobile/features/evaluate` | 调用 model-service 的 `POST /v1/cvd/risk/evaluate`；不可用时返回受控错误；将 M1 引入的治理跟踪块 `model_trace` 从 model-service 透传到 Android 客户端，该字段可空；详见 `model-service/docs/MODEL_REGISTRY.md`。 |
| `POST` | `/rehealth/mobile/rhi/evaluate-series` | 认证的 RHI 预览。接受 1–120 个有序的 RHI v2 每日请求，依次调用 `model-service POST /v2/rhi/evaluate`，并返回数量相同且顺序一致的评估结果。它不持久化权威 RHI 快照。 |
| `POST` | `/rehealth/mobile/rhi/daily-snapshot` | 接收 Android App 本地计算并供管理平台使用的 RHI 每日快照。请求体为 `RhiDailySnapshotBatchDto`：一个 `userId` 和一个 `RhiDailyIndexDto` 列表；每项包含当日总分、领域分数、特征快照和数据质量快照。后端按 `(user_id, scored_on)` 新增或更新，并返回 `{accepted, persisted, status}`。这是为管理端 RHI 视图提供数据的权威上传路径；`rhi/evaluate-series` 仍是独立的远程复算通道，不写入该存储。 |
| `POST` | `/rehealth/mobile/rdi/daily-snapshot` | 接收认证 Android App 在 Room 落库后排队上传的 RDI 每日快照。请求体包含 `userId`、日级总分、置信度、状态、Mock 标记、算法版本和结构化贡献项；后端从认证上下文确认数据归属，按 `(user_id, scored_on)` 幂等更新快照并原子替换贡献项。只接收管理端展示所需的聚合值，不接收原始遥测或自由文本证据。 |
| `GET` | `/rehealth/mobile/risk/latest` | 读取当前认证用户最新的已持久化风险。 |
| `POST` | `/rehealth/mobile/interventions/generate` | 忽略客户端拥有的健康上下文，重新加载档案、访谈、最新风险及按租户限定的 Device Service 遥测上下文，通过 LangChain4j 生成结构化行动，再持久化版本化 JSON 计划。 |
| `GET` | `/rehealth/mobile/interventions/today` | 只读取当前认证用户在 `rehealth.mobile.time-zone` 当前自然日内生成的结构化计划。 |
| `POST` | `/rehealth/mobile/interventions/{id}/feedback` | 在当前认证用户下持久化反馈，并返回类型化持久确认。 |
| `POST` | `/rehealth/mobile/behavior-records/analyze-photo` | 接受一张认证的 Multipart 相机图片和稳定 `requestId`，在服务端完成食物/OCR 分析，持久化结构化结果，并返回当前用户的记录。 |
| `GET` | `/rehealth/mobile/behavior-records/today` | 只读取当前认证用户在请求本地日期和时区偏移量下的行为记录。 |

其他已实现的 E1 支持端点：

| 方法 | 路径 | E1 行为 |
| --- | --- | --- |
| `GET` | `/rehealth/mobile/health` | ReHealth 模块的开发健康检查。 |
| `POST` | `/rehealth/mobile/attribution/events` | 通过认证代理到 PIAS `POST /api/pias/v2/attribute/individual`；`software_db` 启用时，在当前用户下持久化请求和结果。 |
| `POST` | `/rehealth/mobile/agent/messages` | 认证的健康助手代理；后端组装已持久化的用户上下文、执行限流，并默认使用 Java LangChain4j。Provider 凭据绝不进入 APK。 |

拍照分析接受 JPEG、PNG 或 WebP，最大为配置的 4 MB。Android 将图片捕获到私有 `FileProvider` 缓存项中，校正方向、限制长边并重新编码后上传。JeecgBoot 将图片发送给配置的视觉 Provider，不持久化或记录原始字节；它校验结构化食物/OCR 结果，并只把该结果保存到 `software_db`。营养值是估算值，不是临床测量。复用同一所有者范围内的 `requestId` 会返回现有记录，不会重复调用 Provider。

## 已退役的旧风险路径

后端生产模块不暴露以下原型路径：

- `POST /rehealth/mobile/ring/snapshots`
- `GET /rehealth/mobile/patient/risk-score`
- `GET /rehealth/mobile/patient/intervention-plan`
- `POST /api/pias/predict`
- `POST /api/pias/v2/predict`

历史文档或状态说明中出现这些字符串时，只是在引用已退役的原型行为。不得将它们用作生产风险或干预路径。

## Model Service 配置

`application-dev.yml` 中的开发默认值：

```yaml
rehealth:
  model-service:
    base-url: http://127.0.0.1:8000
    timeout-seconds: 10
```

`ModelServiceClient` 目标：

- `GET /health`
- `POST /v1/cvd/risk/evaluate`
- `POST /v1/cvd/intervention/generate`
- 通过独立配置的 `rehealth.attribution-service.base-url` 调用 `POST /api/pias/v2/attribute/individual`

保留的 model-service 干预端点是兼容路径；移动端 `POST /interventions/generate` 实现使用 Jeecg LangChain4j，不向该兼容端点转发客户端档案或风险字段。

## 个性化干预上下文

每次生成都按以下顺序执行最新的失败关闭读取：

1. 已认证的 `software_db` 档案；
2. 最新健康访谈；
3. 最新已持久化 CVD 风险；
4. Device Service 当前本地自然日的活动、睡眠、测量和饮食，以及有界的近期/前一 7 日描述性变化。

Device Service 请求包含已解析租户、当前认证用户和 `rehealth.mobile.time-zone`，并要求内部服务凭据。客户端 `riskResult`、`featureVector` 和 `patientContext` 仅为传输兼容继续接受，不作为证据。响应保留旧摘要字段，并新增 `summary`、`focus_date`、上下文新鲜度字段，以及 1–5 个 `items`；每项包含类别、标题、行动、理由、目标、时机、优先级和证据引用。允许的类别为饮食、运动、睡眠、血压、代谢和随访。

当 Device Service、Provider 或软件持久化不可用时，不返回确定性 Mock 计划。

本地 DeepSeek v4 Provider 在该结构化 JSON 操作中使用非思考模式，因为默认启用的思考模式可能在生成 `content` 前耗尽输出额度。JSON、Schema、证据或安全校验失败时，只执行一次有界重试；失败或不安全的响应不会持久化。

归因请求结构：

```json
{
  "risk_history": [
    {"date": "2026-07-22", "Y": 0.219, "Z": 1}
  ],
  "forecast_days": 30,
  "language": "zh"
}
```

`risk_history` 当前来自已认证 Android 的本地 Room 历史。响应保留 PIAS 的 `status`、`current_state`、`forecast`、`intervention_effect` 和用户报告字段；缺失的预测值或 ATT 值不会被合成。

model-service 不可用或配置错误时，E1 返回受控的 `Result.error` 响应，不会静默返回伪造的生产结果。

## `software_db` 配置

在 Jeecg 主数据源上运行 `db/software/mysql/V1__create_rehealth_software_tables.sql`，并配置：

```yaml
rehealth:
  software-db:
    enabled: true
```

档案、RHI 手工健康输入与每日快照、RDI 每日快照、访谈、设备绑定、特征/风险结果、干预、反馈和归因结果均使用已认证的 `LoginUser.id` 限定范围。`POST /rhi/daily-snapshot` 与 `POST /rdi/daily-snapshot` 都会拒绝请求体 `userId` 与当前登录用户不一致的载荷；RDI 路径只保存日级聚合分数、置信度、状态和结构化贡献项，不保存原始遥测或自由文本证据。Android 先在本地保存已完成的数据，再将类型化载荷入队并通过 WorkManager 重试；访客计算只保留在本机，禁用的 `software_db` 绝不会产生虚假的持久成功。现有数据库必须应用 `V20260805_1__add_rhi_manual_health_input.sql`、`V20260814_3__create_rhi_daily_snapshot.sql` 和 `V20260814_4__create_rdi_daily_snapshot.sql`。

`PatientProfileDto.version` 是乐观锁令牌。客户端应先通过 GET 获取档案，在编辑期间保留返回的版本，并随 PUT 发送。服务端忽略请求中的 `patientId`，从认证主体派生归属、校验数值范围，并根据身高和体重计算 BMI。运营档案和访谈字段保存在类型化列/子表中；模型证据快照按设计保留为 JSON。

当持久化禁用时，所有依赖 `software_db` 的读写都返回 `code=503` 的失败 Jeecg 信封。风险、干预和归因模型调用也必须先完成 `software_db` 持久化才能返回成功，因此数据库写入跳过时，绝不能将模型响应报告为已持久化。

当日干预窗口在 `rehealth.mobile.time-zone`（默认 `Asia/Shanghai`）中计算，范围为 `generated_at >= startOfDay` 且 `< startOfNextDay`。

反馈成功响应：

```json
{
  "interventionId": "plan-id",
  "status": "completed",
  "persisted": true,
  "persistenceStage": "software_db"
}
```

风险、干预和归因模型调用还会向 `rehealth_model_request_log` 写入最小审计元数据：请求 ID、操作、模型版本、结果和时间戳。不记录请求体、遥测值、令牌、手机号或其他健康载荷。

## D1 说明

只有响应包含 `accepted=true`、`persisted=true` 和 `ACCEPTED_*` 状态时，Android 才能将批次标记为完成。E2.1 提供该持久化 Device Service/TimescaleDB 契约。`code=503` 的失败信封表示本地队列必须使用同一 `batchId` 重试。

## E2.1 遥测分离

`POST /rehealth/mobile/measurements/batch` 只负责遥测接入，不触发风险评分，也不调用 model-service。为兼容 Android，DTO 中保留请求 `userId`，但后端会使用当前 Jeecg `LoginUser.id` 覆盖它；客户端无法选择数据行归属。

`telemetry-v2` 新增可选的 `dietRecords` 并返回 `dietRecordCount`；旧 `d2-v1` 和 `telemetry-v1` 批次仍被接受。饮食行只包含结构化餐食文本和营养值，不含原始图片，并与批次共享事务、幂等收据和回滚语义。

新批次成功响应：

```json
{
  "status": "ACCEPTED_PERSISTED",
  "accepted": true,
  "persisted": true,
  "queued": false,
  "durableQueue": false,
  "queueType": "direct-hardware-db",
  "ingestStage": "HARDWARE_DB_COMMITTED"
}
```

幂等重试返回 `ACCEPTED_DUPLICATE`、`persisted=true` 和原始 `receiptId`。默认仍拒绝原始信号分块和类似原始载荷的字段。

当前直接 JDBC 路径是持久化 MVP。消息队列/流式工作器和高并发压力测试属于生产后续工作。

`GET /measurements/recent` 使用同一独立 `hardware` 数据源和认证归属边界。硬件持久化禁用时，端点返回可重试的 `503` 信封，不回退到 Mock 或跨用户数据。

患者移动 API 仅涵盖 P、I 和后续的个体 A。群体归因和结算证据需要独立的后端管理 RBAC。个体 A 最终必须由后端从已持久化记录组装，而不是使用客户端提供的风险历史。

## Android 客户端契约

当前 Android 代码只使用类型化且已认证的客户端。已移除退役的 `/ring/snapshots`、`/patient/mvp`、`/patient/*` 和旧打卡客户端。设备绑定发送稳定的哈希硬件身份；遥测从 Room 入队且不包含原始信号字节。健康助手请求由后端代理，Android 构建配置中不包含模型 Provider 密钥。
