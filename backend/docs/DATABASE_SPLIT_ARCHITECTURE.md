# ReHealth 后端数据库拆分架构

评审项：E0.5_backend_module_selection_and_database_split
日期：2026-07-09
范围：E1 前的软件业务数据库与硬件遥测数据库拆分。

低代码管理页面和手写执行服务也必须遵循 [`REHEALTH_LOW_CODE_BOUNDARY.md`](./REHEALTH_LOW_CODE_BOUNDARY.md)。

## 决策

ReHealth 后端需要两个数据库所有权域：

1. `software_db`：用户、业务和应用记录。
2. `hardware_db`：高并发可穿戴/戒指遥测和接入记录。

Android 绝不直接写入任一数据库。Android 先在本地持久化，再通过后端 API 上传。model-service 绝不拥有 JeecgBoot 数据库。

当前实现更新（2026-07-31）：`software_db` 仍使用 Jeecg/MySQL，而 `hardware_db` 权威已完成向 Device Service/TimescaleDB 的迁移。下文早期 Jeecg 双数据源/MySQL 内容是历史 MVP 设计背景，不是当前有效写入路径。TimescaleDB V4 新增 `hardware_diet_record` Hypertable；其他服务通过 Device Service API 使用按租户/用户限定的摘要，不直接执行 SQL 或跨库关联。

## 数据库职责

| 数据库 | 拥有的数据 | 不拥有的数据 |
| --- | --- | --- |
| `software_db` | 用户、角色、权限、设备绑定、用户档案、健康访谈、CVD 特征向量、风险结果、干预计划、干预反馈、上传状态、管理/业务记录、模型请求元数据。 | 高频原始遥测样本、PPG/RRI 分块、运营日志之外的原始上传载荷、时序保留数据。 |
| `hardware_db` | 可穿戴遥测批次、心率、血氧、血压、体温、睡眠、步数/活动、结构化饮食行为、HRV、在法律/临床允许时的 RRI/PPG 元数据、原始上传批次日志、数据质量标记、接入事件和幂等记录。 | 用户账号权威、角色/权限、干预计划、模型风险输出、管理/医生工作流。 |

## `software_db` 设计

| 维度 | 设计 |
| --- | --- |
| 所有服务 | Jeecg 系统服务拥有系统表。ReHealth 移动端、风险和管理服务拥有 `rehealth_*` 业务表。 |
| 数据表类别 | 系统用户/角色/权限、ReHealth 患者档案、设备绑定、健康访谈、CVD 特征向量、风险结果、干预计划、干预反馈、模型请求日志、上传批次状态、管理/业务工作流记录。 |
| 读写模式 | 中等写入量；频繁读取最新档案、风险和干预；按用户/设备/日期执行管理查询；在单个业务聚合内进行事务更新。 |
| 预期并发 | MVP 阶段为低到中等。登录和读取最新风险的并发较高，但远低于遥测接入量。 |
| 保留策略 | 账号/档案/绑定在账号有效期内及合规策略要求期限内保留。特征向量、风险、干预和反馈需保留足够时间以支持纵向趋势和审计。模型请求元数据应脱敏并最小化载荷后保留。 |
| 事务一致性 | 软件业务聚合内部使用强一致性，例如档案更新、特征向量与风险结果、干预与反馈。避免与硬件遥测建立跨库事务。 |
| 存储适配 | 关系型数据库。JeecgBoot 默认的 MySQL 兼容路径适用。依赖也支持 PostgreSQL，但 JeecgBoot 脚本和默认配置偏向 MySQL。 |
| MVP 实现选择 | MySQL 数据库/Schema；使用 JeecgBoot 动态数据源主库 `software`，或将现有 `master` 映射为仅供软件数据使用。 |
| 生产实现选择 | 根据部署标准选用 MySQL/PostgreSQL/TiDB。必要时为管理/报表读取配置只读副本。必须显式配置 PHI/PII 加密、审计、备份和访问控制。 |

建议的 `software_db` 数据表：

| 类别 | 数据表 |
| --- | --- |
| 档案/访谈 | `rehealth_patient_profile`、`rehealth_health_interview` |
| 设备/业务 | `rehealth_device_binding`、`rehealth_upload_batch` |
| 特征/风险 | `rehealth_cvd_feature_vector`、`rehealth_cvd_risk_result`、`rehealth_model_request_log` |
| 干预/反馈 | `rehealth_intervention_plan`、`rehealth_intervention_feedback`、`rehealth_attribution_event` |
| 管理/业务 | 后续按需增加 `rehealth_admin_case`、`rehealth_doctor_note`、`rehealth_operation_record` |

## `hardware_db` 设计

| 维度 | 设计 |
| --- | --- |
| 所有服务 | ReHealth 接入服务/模块。其他服务通过服务 API 或已物化的软件记录读取摘要，不进行临时关联查询。 |
| 数据表类别 | 原始上传批次日志、规范化测量行、睡眠会话、活动/步数会话、HRV 行、允许时的 RRI/PPG 元数据/分块、设备遥测质量标记、接入事件和幂等键。 |
| 读写模式 | 高写入量，以追加和批量接入为主。读取主要按用户/设备/时间范围进行摘要、最新缓存刷新、QA、重放和特征提取。 |
| 预期并发 | MVP 阶段为中等；试点/生产阶段为高并发，因为每台可穿戴设备都可能反复上传批次。离线后会出现突发上传。 |
| 保留策略 | 对高容量原始数据分层保留。近期详细遥测保持热存储，将旧数据汇总为摘要，并按同意/合规策略归档或删除原始分块。原始 PPG/RRI 应采用更严格的保留期限和显式许可。 |
| 事务一致性 | 必须保证批次接受幂等。单批次写入适合使用强一致性。与 `software_db` 的跨库一致性应采用状态、事件和重试实现最终一致性，而不是分布式事务。 |
| 存储适配 | 生产环境适合追加型时序或分析存储。MVP 可使用具备批次表、索引和保留上限的关系型 MySQL。 |
| MVP 实现选择 | 独立 MySQL 数据库/Schema `rehealth_hardware`，包含规范化批次、测量和会话表，并设置严格的载荷大小与幂等限制。 |
| 生产实现选择 | 对高容量遥测使用时序数据库或 ClickHouse；若需要关系/时序混合，也可使用 PostgreSQL/TimescaleDB。若分析存储独立，接入元数据仍可保存在 MySQL/PostgreSQL。 |

建议的 `hardware_db` 数据表：

| 类别 | 数据表 |
| --- | --- |
| 设备/接入 | `rehealth_hw_device`、`rehealth_hw_measurement_batch`、`rehealth_hw_ingestion_event` |
| 测量 | `rehealth_hw_measurement`，可选按日期/类型分区 |
| 睡眠/活动 | `rehealth_hw_sleep_session`、`rehealth_hw_activity_session`、`rehealth_hw_step_summary` |
| 信号 | `rehealth_hw_hrv`，以及允许时的 `rehealth_hw_rri_metadata`、`rehealth_hw_ppg_chunk` |
| 质量 | `rehealth_hw_quality_flag`、`rehealth_hw_batch_rejection` |

高容量数据表要求：

- 使用内部 `user_id`/患者引用，绝不使用手机号。
- 包含 `device_id`。
- 包含客户端 `batch_id` 或幂等键。
- 包含 `measured_at` 或 `started_at`/`ended_at`。
- 包含 `received_at`。
- 包含指标类型、单位、来源和质量/状态。
- 对 `(user_id, device_id, batch_id)` 或等效幂等键设置唯一约束。
- 对 `(user_id, measured_at)`、`(device_id, measured_at)` 和 `(batch_id)` 建立索引。
- 在达到试点数据规模前确定分区/TTL 策略。

## JeecgBoot 数据源策略

JeecgBoot 基础核心已包含 `dynamic-datasource-spring-boot3-starter`。E1 应使用显式数据源名称，避免将硬件遥测混入默认 `master` 数据库。

推荐命名：

```yaml
spring:
  datasource:
    dynamic:
      primary: software
      datasource:
        software:
          url: jdbc:mysql://.../rehealth_software
        hardware:
          url: jdbc:mysql://.../rehealth_hardware
```

本地 MVP 选项：

- 若没有独立 `rehealth_software` 数据库，可将 `software` 映射到现有 Jeecg 数据库。
- 仍需为遥测创建独立的 `hardware` 数据库/Schema。
- 若第二数据源不可用，E1 必须记录该限制，不得将硬件遥测伪装为生产就绪。

Mapper/服务约定：

- 软件/业务 Mapper 使用主数据源或 `@DS("software")`。
- 硬件 Mapper 使用 `@DS("hardware")`。
- 不使用跨库事务。
- 若一个流程必须触及两个域，应先写入已接受的遥测，再写软件侧状态/事件；必要时异步重试第二次写入。

## 数据流

### Android 测量上传流程

```text
MRD 戒指/BLE
  -> Android repository/service
  -> Room 本地持久化
  -> Android 上传队列
  -> POST /rehealth/mobile/telemetry/batches
  -> 后端认证/设备归属校验
  -> 幂等校验
  -> 持久化批次写入
  -> 已接受/已拒绝/可重试响应
```

规则：

- Android 按批次上传，而不是每个样本发送一个 HTTP 请求。
- Android 采集不得因后端或 model-service 不可用而阻塞。
- 后端必须先接受并持久化有效遥测，再触发模型评估。
- 后端不得记录原始遥测载荷、令牌、手机号或标识符。

### 硬件数据接入流程

E1 MVP 同步路径：

```text
Mobile API
  -> ReHealthIngestService.validate()
  -> hardware_db.batch + hardware_db.measurements/sessions
  -> software_db.upload_batch 状态
  -> 向 Android 返回响应
```

高并发路径：

```text
Gateway
  -> rehealth-ingest-service
  -> 小型接入收据/状态写入
  -> 消息队列 Topic/队列
  -> 遥测消费者工作器
  -> hardware_db 规范化写入
  -> 失败时进入死信/重试
  -> software_db 上传状态/物化摘要
```

消息队列建议：

- 除非明确要求，否则 E1 不引入消息队列。
- 经过运维评审后，生产环境只选择一种消息队列：RabbitMQ 适合较简单的可靠队列，RocketMQ 适合高吞吐有序/事件流工作负载。
- MVP 不得同时启用 RabbitMQ 和 RocketMQ。

### 特征提取和风险评估流程

推荐的 MVP 流程：

```text
Android CVD 特征提取器
  -> POST /rehealth/mobile/features/evaluate
  -> 后端校验 CVD 16 + featureQuality
  -> software_db.rehealth_cvd_feature_vector
  -> ModelServiceClient POST /v1/cvd/risk/evaluate
  -> software_db.rehealth_cvd_risk_result
  -> 重新加载 software_db 档案/访谈/最新风险
  -> Device Service 按租户/用户限定的 TimescaleDB 干预上下文
  -> Jeecg LangChain4j 生成结构化健康计划
  -> software_db.rehealth_intervention_plan
  -> 向 Android 返回响应
```

后端以后也可以根据 `hardware_db` 计算服务端摘要，但 E1 应遵守 Android C1 特征提取器契约和 model-service API 契约。

### 最新数据缓存流程

目的：无需扫描原始遥测即可保持移动端最新数据页面的响应速度。

MVP 选项：

```text
已接受的遥测批次
  -> 更新最新测量摘要记录/缓存
  -> GET /rehealth/mobile/risk/latest 从 software_db 读取最新风险
  -> GET /rehealth/mobile/interventions/today 从 software_db 读取当前计划
```

生产选项：

```text
hardware_db 追加
  -> 流/工作器计算每位用户的最新摘要
  -> Redis 最新缓存 + software_db 物化摘要
  -> 移动端读取最新数据端点
```

缓存规则：

- 缓存保存最新派生摘要，不保存原始 PPG/RRI 载荷。
- 缓存未命中时回退到已持久化的最新记录。
- 缓存失效依据已接受批次的时间范围，以及风险/干预生成事件。

### Model Service 调用流程

健康检查：

```text
后端启动/周期检查
  -> GET model-service /health
  -> 在 /rehealth/mobile/config 中暴露状态摘要
```

风险：

```text
后端
  -> POST model-service /v1/cvd/risk/evaluate
  -> 持久化 risk_score、risk_level、contributions、model_version、is_mock、missing_fields、quality_warnings、summary
```

干预：

```text
后端
  -> 从 software_db 重新加载认证用户的档案、最新访谈和风险
  -> GET Device Service /rehealth/internal/v1/operations/users/{userId}/intervention-context
  -> Jeecg LangChain4j 校验 1–5 条带证据引用的行动
  -> 持久化 plan_id、结构化 JSON items、上下文新鲜度、免责声明和模型版本
```

后续归因：

```text
后端
  -> 根据风险、干预和反馈记录构建事件历史
  -> POST model-service /v1/cvd/attribution/individual
  -> 持久化或返回归因摘要
```

失败行为：

- model-service 不可用不得拒绝已经接受的遥测。
- 特征向量持久化后，特征/风险评估可以返回可重试的服务错误。
- 除非明确标为仅开发使用且排除在生产行为之外，否则不得提供 Java 临床评分回退。

## 服务所有权矩阵

| 服务/模块 | 写入 | 读取 | 说明 |
| --- | --- | --- | --- |
| Jeecg 系统服务 | `software_db` 系统表 | 系统、用户、角色、权限 | 保持现有所有权。 |
| `rehealth-mobile-service` | 档案、设备绑定、上传状态、反馈 | 最新风险/干预/档案/配置 | 面向 Android 的 API。 |
| `rehealth-ingest-service` | `hardware_db` 遥测和接入记录 | 通过服务/软件查询读取设备绑定 | 必须独立于 model-service 可用性。 |
| `rehealth-risk-orchestration-service` | 特征向量、风险结果、干预计划 | 特征向量、最新档案、模型状态 | 调用 Python model-service。 |
| `rehealth-admin-service` | 管理/业务记录 | 默认读取软件摘要，不读取原始遥测 | 后续实现。 |
| Python `model-service` | 不写 Jeecg 数据库 | 不直接读取 | 只接收 HTTP 请求。 |

## 一致性边界

以下场景使用强事务：

- `software_db` 中的设备绑定更新。
- `software_db` 中的档案/访谈更新。
- `hardware_db` 中的单个遥测批次写入。
- 当特征向量和风险结果在同一工作流生成时，将两者写入 `software_db`。
- `software_db` 中的干预反馈写入。

以下场景使用最终一致性：

- `hardware_db` 遥测接受与 `software_db` 上传状态之间的同步。
- 遥测摘要/缓存更新。
- 消息队列消费者写入。
- model-service 可用性和重试。
- 管理/报表物化。

避免：

- 在 E1 使用 Seata/分布式事务。
- 在应用查询中执行跨库关联。
- 在 Java 侧执行模型推理。
- 在日志中保存原始健康载荷。

## 修订后的 E1 数据库范围

若获批准，E1 当前应实现：

- `software_db` MVP 表：设备绑定、档案/访谈、特征向量、风险结果、干预计划、反馈和上传批次状态。
- `hardware_db` MVP 表：测量批次、规范化测量行、睡眠/活动会话和接入事件/拒绝记录。
- 遥测批次的幂等行为。
- 明确的数据源注解或服务边界。
- 数据库 Schema 变化时提供 Schema 文档和迁移。

E1 应将以下内容移至 E2：

- 消息队列写入/消费路径。
- ClickHouse/时序数据库迁移。
- 除非明确允许，否则不存储元数据以外的原始 PPG/RRI。
- 分区/分片实现。
- 保留数据清理任务。
- 管理分析/报表。

## D1 就绪门禁

E1 提供以下内容后，Android D1 才能安全集成：

- 稳定的 `/rehealth/mobile/**` 端点路径。
- 遥测上传、特征评估、最新风险/干预和反馈的请求/响应 DTO。
- 上传批次幂等键行为。
- 已接受/已拒绝/可重试错误结构。
- 确认遥测是否已持久化到 `hardware_db`；若未持久化，则提供准确记录的开发限制。
- 确认与 `model-service/docs/API_CONTRACT.md` 的映射。

在此之前，Android 不应硬编码 CVD 特征/模型契约之外的后端行为。
