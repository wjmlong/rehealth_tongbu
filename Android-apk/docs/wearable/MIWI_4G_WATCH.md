# Miwi 4G 云平台手表（S8）接入说明

> 更新：2026-07-29（分支 `4Gwatch`）。适用于云米/MiwiTracker 云平台的 4G 手表
> （API 文档覆盖 S8、S9、GS20、GS17、A67、K9L；当前接入目标为 S8）。
> 契约细节见 `Android-apk/docs/REHEALTH_INTEGRATION_CONTRACT.md` 的
> "Miwi 4G Cloud Watch (S8)" 一节。

## 0. 方案决策（来自厂家资料 + 架构评审）

| 维度 | S8 厂家云 API 拉取（**首发**） | L16 直连 TCP（二期） |
| --- | --- | --- |
| 链路 | 手表→厂家云→**睿禾后端定时拉取**→统一库→App | 手表→睿禾 TCP 网关（独立服务） |
| 实时性 | 分钟级（轮询周期） | 秒级（适合 SOS/跌倒报警） |
| 后端复杂度 | 中：Token/轮询/游标/去重/限流 | 高：长连接/粘包/心跳/ACK/重连/会话 |
| ECG/原始波形 | 当前无 ECG、无血糖接口 | 有 APHD 分包 ECG、RRI 协议 |
| 安全 | HTTPS+Token，易控 | 协议无 TLS/设备签名，需额外保护 |
| 适合 | 8 月初快速上线、普通健康数据同步 | 实时报警、原始波形、降低厂家云依赖 |

**首发选 S8 云拉取**；L16 列为二期，做成独立 `l16-device-gateway`，与 S8、BLE 一样
统一进入同一条遥测入库管线，**不在 JeecgBoot 内直接开 Socket 写业务表**。

## 1. 架构与边界

```text
S8 手表 --4G/SIM--> 云米云平台
                        │
            ┌───────────┴────────────┐
            │ 厂商云 OpenAPI（拉取，首发主路径）│  厂商云主动推送（实时补充通道）
            │ 后端定时轮询 bytime 接口      │  POST /rehealth/miwi/push?token=
            ▼                            ▼
   S8PollingService              MiwiPushService
            │                            │
            └─────────► HardwareIngestionPort ◄── (与手机 BLE 同一条入库管线)
                              │
                              ▼
                   hardware_measurement（统一表，按 deviceId/source 区分）

App：仅负责 IMEI 绑定（/rehealth/mobile/devices/bind）与结果展示，不参与数据采集。
```

- 该设备**不走手机蓝牙**。App 端 `Miwi4gCloudRingRepository` 不做 BLE 扫描；
  `syncAll()` 不产生本地记录（数据由后端拉取后落入统一表，App 通过现有
  `/rehealth/mobile/measurements/recent` 读取）。
- deviceId 规则与 BLE 设备一致：`miwi4g-` + SHA-256(IMEI) 前 24 位十六进制；
  IMEI 原文不上传、不写日志（IMEI 仅在后端调用厂商 API 时作为查询参数使用）。
- `source` 标记：`S8_CLOUD_PULL`（拉取）/ `MIWI_4G_CLOUD`（推送）。provider=S8，
  transport=VENDOR_CLOUD_PULL。
- **不融合**：两只手表（蓝牙 ECG + S8）测到同一指标时，原始层全部保存（不同
  deviceId/source），由查询层/特征层按设备角色与质量选择主来源，入库时不做平均/覆盖。

## 2. 后端组件（jeecg-module-rehealth）

### 2.1 拉取主路径（新增 `org.jeccg.modules.rehealth.miwi.pull`）

| 类 | 职责 |
| --- | --- |
| `S8Metric` | 可拉取指标枚举 + 各自 bytime 端点、轮询间隔、字段别名、合理区间 |
| `S8OpenApiClient` | 复用 `get_token`（MD5）；`post(endpoint, body)` 带 `Authorization: <AccessToken>` 头 |
| `S8Normalizer` | 容错解析 bytime 响应 → ReHealth 测量 Map（与推送共用 `MiwiHealthDataMapper`） |
| `S8DeviceRegistry` / `JdbcS8DeviceRegistry` | 运营商导入的 S8 设备清单（IMEI/model/role），计算 deviceId |
| `S8SyncCursorRepository` / `JdbcS8SyncCursorRepository` | **每 (device, metric) 独立游标**，失败不阻塞其他指标 |
| `S8PollingService` | 编排：列设备→查绑定用户→读游标→拉取→归一→`HardwareIngestionPort`→推进游标 |
| `S8PollingScheduler` | `@Scheduled` 每指标独立频率；`rehealth.miwi.pull.enabled=true` 时启用 |
| `MiwiAdminController` | 运营商导入 S8 设备（`POST /rehealth/miwi/admin/s8-devices`，需登录） |

### 2.2 推送补充通道（既有）

| 类 | 职责 |
| --- | --- |
| `MiwiProperties` | `rehealth.miwi.*` 配置（回调默认 `enabled=false`，拉取默认 `pull.enabled=false`） |
| `MiwiCallbackController` | `POST /rehealth/miwi/push?token=...`，`@IgnoreAuth` + 预共享 token 校验 |
| `MiwiPushService` | 解包双层 JSON、IMEI→deviceId→用户、送入 `HardwareIngestionPort` |
| `MiwiHealthDataMapper` | 字段映射与 UTC 时间归一（支持秒/毫秒/本地时间字符串，本地时间按 UTC+8） |

## 3. 真实厂商 OpenAPI（V1.6.5，已核对 docx）

- Base URL：`https://openapi.miwitracker.com`
- 鉴权：`POST [json]`，请求头 `Authorization: <AccessToken>`；OpenAPI 成功码 `Code == 0`
- Token：`POST /api/token/get_token`，body `{ AppId, Timestamp, Password=MD5(AppKey+AppId+Timestamp) }`
- bytime 查询（body `{ imei, startTime, endTime }`）：
  - `/api/heartrate/get_heartrate_bytime`
  - `/api/bloodpressure/get_bloodpressure_bytime`
  - `/api/bloodoxygen/get_bloodoxygen_bytime`
  - `/api/temperature/get_temperature_bytime`
  - `/api/steps/get_steps_bytime`（另有 `/api/steps/get_steps_forday`）
  - 位置/轨迹：`/api/location/get_location_info`、`/api/track/get_track_info`
  - 指令：`/api/command/sendcommand`、`/api/geofence/create_geofence`
- 时间格式以 `rehealth.miwi.pull.time-format` 配置（默认 `epoch_seconds`，
  亦支持 `epoch_millis` 或 `yyyy-MM-dd HH:mm:ss`）；**联调前须按 V1.6.5 实测确认**。
- 字段名在 `S8Metric` 中以别名列表给出，按真实响应调整无需改动其他代码。

## 4. 去重与一致性（关键）

- 每个 (device, metric) **独立游标**（`rehealth_s8_sync_cursor`）。某指标失败只重试该指标，
  其余照常推进；游标回退 `rehealth.miwi.pull.backfill-minutes`（默认 10 分钟）接住厂家晚到数据。
- 测量行幂等：每条 S8 测量生成确定性 `client_record_id`
  （`s8-` + SHA-256(deviceId|source|metricType|measuredAt|values)），
  `hardware_measurement` 增加 `UNIQUE(client_record_id)`，写入用
  `INSERT ... ON DUPLICATE KEY UPDATE id=id`，重叠窗口重拉不产生重复行。
- 绑定解析：拉取仅对"已在 `rehealth_s8_device` 注册 **且** 已在 `rehealth_device_binding`
  绑定到用户"的设备产生数据；否则跳过（debug 日志）。

## 5. App 端组件

- `WearableVendor.MIWI4G`、产品 `RH-S8-4G01`（`wearable_products.json`）。
- `Miwi4gCloudRingRepository`：IMEI 即"设备地址"；`connect()` 校验 10-17 位数字并
  写入 `ActiveWearableBindingStore`，随后由 `RingViewModel` 触发云端 bind。
- 设备页（`DeviceBindingScreen`）在激活 S8 产品时显示 IMEI 输入卡片，隐藏蓝牙扫描；
  文案已说明"数据由后端定时从厂商云拉取，来源标记为 4G 云同步"。
- **App 不持有 AppKey/Token/原始 IMEI**；这些仅存在于后端运行环境与配置中。
- 产品切换入口目前仅 Debug 构建开放（`ALLOW_WEARABLE_PRODUCT_SWITCH`），Release
  默认产品策略不变——正式发布前需确定 Release 产品选择方案。

## 6. 已知厂商 API 限制（V1.6.5 / V1.6.7）

- 无 ECG 波形/R 波/房颤/QT 等任何 ECG 接口；无血糖、血脂、尿酸接口。
- PPG 原始数据仅 ZIP 导出（约 20Hz），且未确认 S8/MT116 固件是否支持 HEALTHPPG 上报。
- 回调无签名（"校验规则：无"）；本项目以私有 callback-token + 建议 IP 白名单兜底。
- 返回码不统一（推送 code=1 成功，OpenAPI Code=0 成功）；时区/时间格式混杂，
  入库前已统一为 UTC epoch millis。
- 健康数据缺质量字段（佩戴状态、测量方式、信号质量、固件版本等）。
- 厂商 API 调用非无限：1000 台 × 5 接口 × 5 分钟一次 ≈ 172.8 万次/天，
  上线前须确认 QPS/账号限额/单设备时间范围/是否支持批量设备查询。

## 7. 待厂家书面确认清单（发给厂家）

1. S8（以及 MT116，如适用）固件是否已接入贵司云平台，并支持 Health 数据？
2. 请提供测试环境：AppId/AppKey、api-base-url、样机 IMEI、回调联调支持。
3. 回调安全：能否增加 `X-App-Id / X-Timestamp / X-Nonce / X-Signature`
   （HMAC-SHA256(AppSecret, timestamp+nonce+body)）与 IP 白名单？现阶段请在回调
   URL 中携带我方分配的 token 参数。
4. 哪些型号支持 HEALTHPPG 原始数据上报？PPG 实际采样率是否高于 CSV 中的 20Hz？
5. 是否有 ECG 波形（采样率、导联、R 波时间点、报告）上传或导出的任何计划/私有接口？
6. 血糖是否仅设备本地显示？是否存在未写入本文档的血糖上报通道与校准接口？
7. 健康数据能否附带质量字段：佩戴状态、手动/自动测量、信号质量、固件版本、算法版本？
8. bytime 接口的时间字段权威定义（时区/单位/格式），以及单次查询最大时间范围、是否分页。
9. Token 有效期与刷新规则；QPS、并发、单账号、单设备调用限制。
10. 同一条历史数据是否可能被修正？是否有稳定记录 ID 可用于幂等？
11. SOS、跌倒是否仅存在于实时推送而不进入历史查询？
12. 厂家云故障或终止合作后，能否完整导出用户历史数据？
13. 开通推送的费用、回调签名、重试机制与 SLA（如需秒级报警）。

## 8. QA 状态

- 软件侧：`MiwiPushServiceTest`、`Miwi4gCloudRingRepositoryTest` 已通过；
  Android `:app:compileDebugKotlin` / `:app:compileReleaseKotlin` 与 miwi 单测均通过。
- `HARDWARE_QA_PENDING`：真机 S8 + 厂商测试环境未打通前，端到端拉取、Token 获取、
  时区实测、绑定归属正确性、去重幂等均未验收。
- 后端模块本机未运行 Maven 编译（开发机无 Maven/JDK 配置），合入主干前需
  在 CI 或后端开发机执行 `mvn -pl jeecg-boot-module/jeccg-module-rehealth -am test`。
- L16 直连为二期，未在本分支实现。
