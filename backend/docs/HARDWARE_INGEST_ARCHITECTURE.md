# ReHealth 硬件遥测接入架构

日期：2026-07-31
当前模块：`device-service`

## 当前 Device Service 流程

```text
已认证的 Android 批次（telemetry-v2）
  -> Gateway 遥测权威路由
  -> Device Service 身份/设备授权
  -> 共享 TelemetryContractValidator
  -> 一个 TimescaleDB 事务
       -> 上传收据 + 测量 + 睡眠 + 活动 + 饮食
       -> 对账 + Transactional Outbox
  -> 持久化 ACCEPTED_PERSISTED / 幂等 ACCEPTED_DUPLICATE
```

为向后兼容，`dietRecords` 是可选字段。每条记录都必须包含稳定 ID、`consumedAt`、`breakfast|lunch|dinner|snack` 之一以及有界描述；提供的营养值必须是有限且非负的数值。原始餐食图片不属于该契约。Android 必须先在本地持久化捕获的餐食，再将其加入可重试的遥测批次。

个性化干预生成是独立的读取路径，绝不在接入流程中运行。Jeecg 调用受凭据保护的 Device Service 端点 `/rehealth/internal/v1/operations/users/{userId}/intervention-context`；该端点只读取当前认证租户/用户的当前自然日及有界近期趋势。因此，上传成功不依赖 LLM、`software_db` 或干预服务是否可用。

## 旧版 Jeecg/MySQL E2.1 参考

## 范围

E2.1 用独立 `hardware` 数据源/Schema 中的同步事务型 MVP 写入，替换 `POST /rehealth/mobile/measurements/batch` 背后仅供开发使用的内存接受路径。

该路径不向 `software_db` 写入遥测，不调用 model-service，不运行 CatBoost/SHAP，也不执行干预、归因或结算工作。

## 运行时流程

```text
已认证的 Android 请求
  -> ReHealthMobileController 用 LoginUser.id 覆盖请求体 userId
  -> HardwareTelemetryIngestionService
  -> TelemetryBatchValidator
       -> 拒绝空请求、超限请求和原始信号输入
  -> JdbcHardwareTelemetryWriter
       -> 解析名为 hardware 的物理动态数据源
       -> 一个硬件库本地事务
       -> 幂等查询/唯一约束
       -> 批次 + 测量 + 睡眠 + 活动
       -> 提交
  -> ACCEPTED_PERSISTED 或 ACCEPTED_DUPLICATE
```

只有硬件库本地事务提交完成，或查到此前已提交的批次后，才返回 `accepted=true` 和 `persisted=true`。

## 所有权与隔离

- JSON `userId` 字段为兼容 Android D2 DTO 而保留。
- 控制器使用当前 Jeecg `LoginUser.id` 覆盖该字段。
- 幂等范围为 `(authenticated user_id, device_id, batch_id)`。
- 客户端无法通过修改 `userId` 选中其他用户的硬件记录。
- 写入器通过 `DynamicRoutingDataSource.getDataSources().get("hardware")` 获取物理数据源，不使用默认 `master` 路由。
- 事务管理器仅针对该物理数据源创建，不存在软件库/硬件库分布式事务。

使用持久化 `software_db` 绑定校验设备归属仍是 E1.1 依赖。在该能力完成前，认证可以保护用户归属，但无法证明提交的 `deviceId` 已绑定到该用户。

## 幂等性

`hardware_upload_batch` 对 `(user_id, device_id, batch_id)` 建立唯一键。普通重试返回原始收据。并发插入竞争通过同一数据库约束和回滚后的查询解决，不会再次插入子记录。

状态：

| 状态 | 含义 |
| --- | --- |
| `ACCEPTED_PERSISTED` | 新批次和规范化记录已提交。 |
| `ACCEPTED_DUPLICATE` | 同一所有者、设备和批次此前已提交。 |
| `REJECTED_INVALID` | 请求在持久化前被校验拒绝。 |

数据源被禁用、缺失或事务失败时，端点返回 `code=503` 的失败 Jeecg 信封。Android 必须保留本地批次，并使用同一 `batchId` 重试。

## 原始信号策略

默认拒绝原始信号分块、`payload_base64`、原始载荷字段、PPG/RRI/波形键及其嵌套等价字段。V1 Schema 仅包含一张未来使用的元数据表，不包含原始载荷列。即使启用原始信号上传，仍必须另行批准用户同意、保留期限、加密和对象存储方案；E2.1 不实现该路径。

## PIAS 边界

硬件遥测是后续后端编排使用的认证事实来源。患者客户端不提交用于归因的风险历史，也不调用群体归因或结算。后续 E1.1/E1.2 后端流程必须从已持久化的风险、干预、反馈和遥测派生摘要组装个体归因输入。结算仍是仅管理员可用的证据流程，绝不在遥测接入中运行。

## 云米适配器（主动上报回调）

认证移动 API 也支持所需的按需拉取流程：

```text
Android -> POST /rehealth/mobile/viomi/bind（IMEI + productCode）
Android -> POST /rehealth/mobile/viomi/sync（IMEI + Epoch 毫秒时间窗 + 指标）
后端 -> 云米令牌/设备/历史 OpenAPI -> HardwareIngestionPort -> hardware_db
后端 -> 已规范化并持久化的测量 -> Android Room
```

`AppId`、`AppKey` 和缓存的 `AccessToken` 仅保留在服务端。绑定流程验证 IMEI 对已配置云米账号可见，只在 `software_db` 保存哈希设备身份，并将绑定限定到当前认证用户。设备列表请求使用云米令牌响应返回的 `UserId`；只有该字段缺失时，才使用配置项 `REHEALTH_VIOMI_USER_ID` 作为兼容回退。

同步支持 `HEART_RATE`、`BLOOD_PRESSURE` 和 `BLOOD_OXYGEN`，将没有显式偏移量的厂商时间戳按 `Asia/Shanghai` 解释，并将单次请求限制为最多 31 天。同步会拒绝非有限值或生理范围无效的样本：心率 20–250 bpm、SpO₂ 50–100%、收缩压 50–260 mmHg、舒张压 30–180 mmHg，且收缩压必须高于舒张压。只有硬件接入持久化成功后，记录才会返回 Android。`NO_NEW_DATA` 是成功的空操作。

`POST /rehealth/viomi/report` 允许云米（miwitracker）平台向本后端推送可穿戴遥测。手表不会直接调用 `measurements/batch`；云米云端收到手表数据后调用我们的回调。

### 流程

```text
云米云端（使用共享 AppKey 的 JWT HS256 签名）
  -> POST /rehealth/viomi/report（@IgnoreAuth；无 Jeecg 会话）
  -> ViomiReportController 写入精确的 {"code":1,"msg":"操作成功"} 确认响应
  -> ViomiReportService 校验 JWT、映射载荷并调用 HardwareIngestionPort
  -> HardwareTelemetryIngestionService
  -> TelemetryBatchValidator
  -> JdbcHardwareTelemetryWriter（复用同一 hardware 数据源和幂等机制）
```

### 认证

- 令牌通过 `Authorization: Bearer <jwt>` 请求头或请求体字段 `AccessToken` 传递。
- 使用 `rehealth.viomi.app-key` 通过 HMAC-SHA256（JWT HS256）验证。
- 从 JWT 载荷读取 `appId`/`imei` 声明；缺失时回退到请求体 `Imei` 字段和已配置的 `rehealth.viomi.app-id`。
- 当 `rehealth.viomi.require-auth=true`（默认）且验证失败时，端点返回 `{"code":0,"msg":"操作失败"}`，使云米执行重试。

### 字段映射（云米 -> ReHealth `metricType`）

| 云米 `DataType` | 云米字段 | ReHealth `metricType` | 单位 |
| --- | --- | --- | --- |
| `Health` | `heartRate` | `HEART_RATE` | bpm |
| `Health` | `bloodOxygen` | `SPO2` | % |
| `Health` | `bloodPressureMax`/`bloodPressureMin` | `BLOOD_PRESSURE`（主值=收缩压，次值=舒张压） | mmHg |
| `Health` | `steps` | `STEPS` | 步 |
| `Health` | `distance` | `DISTANCE` | m |
| `Health` | `calorie` | `CALORIE` | kcal |
| `Health` | `deepSleep`/`lighSleep`/`totalSleep`/`sleepTime` | `hardware_sleep_session`（深睡/浅睡分钟） | - |
| `StepRoll`/`StepRolls` | `step`/`roll`/`distance`/`calorie` | `STEPS` / `ROLL` / `DISTANCE` / `CALORIE` | 步 / 次 / m / kcal |
| `Temperature` | `temperature` | `BODY_TEMPERATURE` | °C |
| `Location` | `battery` | `DEVICE_BATTERY` | % |

空白云米值会被跳过，因此部分载荷仍可持久化其中有效的指标。`ResultData` 是 JSON 字符串，按 `DataType` 解析。

### 所有权与幂等性

- `deviceId` = 云米 `imei`。
- `userId` = 已配置的 `rehealth.viomi.user-id`（默认 `viomi-gateway`），即平台网关账号，因为回调没有 Jeecg 用户会话。通过 `software_db` 设备绑定将每个 IMEI 映射到真实用户是后续工作，依赖 E1.1。
- `batchId` = `viomi-{imei}-{dataType}-{reqId|hash}`，以同一 `(user_id, device_id, batch_id)` 唯一键保证幂等。

### 响应契约

成功时：

```json
{ "code": 1, "msg": "操作成功" }
```

认证失败、校验拒绝或持久化错误时：

```json
{ "code": 0, "msg": "操作失败" }
```

传输层始终返回 HTTP 200，云米根据 `code` 判断上报是否送达。

### 配置

```yaml
rehealth:
  viomi:
    enabled: ${REHEALTH_VIOMI_ENABLED:true}
    app-id: ${REHEALTH_VIOMI_APP_ID:}
    app-key: ${REHEALTH_VIOMI_APP_KEY:}
    require-auth: ${REHEALTH_VIOMI_REQUIRE_AUTH:true}
    user-id: ${REHEALTH_VIOMI_USER_ID:viomi-gateway}
    source: ${REHEALTH_VIOMI_SOURCE:viomi}
```

`app-id`/`app-key` 由云米在接入期间签发，并通过 `REHEALTH_VIOMI_APP_ID`/`REHEALTH_VIOMI_APP_KEY` 环境变量注入，源码中不保存密钥。`require-auth` 默认为 `true`；在云米签发真实凭据前进行本地集成时，可设置 `REHEALTH_VIOMI_REQUIRE_AUTH=false` 让上报通过，但数据仍会持久化到配置的 `user-id` 下。

## 生产后续工作

直接 JDBC 事务适合 MVP 试点，并非最终高并发拓扑。后续任务应在不改变 Android 批次契约的前提下，增加一套持久化队列/流、消费者批量写入器、压力测试、可观测性、分区/保留策略和死信处理。
