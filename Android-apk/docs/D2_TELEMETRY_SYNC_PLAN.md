# D2 Telemetry Sync Status and Remaining QA

Status: implemented software path; updated 2026-08-21.

## Implemented

### Viomi cloud pull path (2026-08-05)

- `VIOMI_CLOUD` is a non-BLE `RingRepository` provider for S8/S9/GS20/GS17/A67/K9L.
- Binding and history sync use authenticated backend endpoints; vendor credentials never enter the APK.
- A successful bind triggers the first sync automatically. The first pull requests up to 31 days;
  later pulls start two days before the latest scoped local record and remain capped at 31 days.
- Backend persistence is the authority. Only a persisted response is imported to Room.
- Imported `viomi_cloud` records set `RingSyncResult.requiresUpload=false`, preventing an upload echo loop.
- Room v15 adds nullable measurement owner/device columns and a composite lookup index. Room v16
  applies nullable owner/device scope to sleep, activity, and signal/ECG rows so account switching
  cannot expose another user's cached health telemetry. Viomi
  observations are read by authenticated user + hashed backend device + `viomi_cloud` source,
  while migration 14→15 preserves legacy rows with null scope.
- The cloud Data screen exposes only heart rate, blood oxygen and blood pressure. It uses real
  samples for trends, daily-balanced period means, and period-minimum SpO₂; unsupported sections
  and synthetic mini-charts are hidden.

- MRD/RWFit/HBand collection writes to Room before any network operation.
- Successful manual/automatic sync creates a durable `telemetry_batch` queue item.
  上传队列 `kind` 全集为：`telemetry_batch`、`health_interview`、`rhi_daily_snapshot`、
  `rdi_daily_snapshot`、`rhi_manual_health_input`（`SyncRepository.kt` 私有 companion
  常量）；批次 `schemaVersion` 为 `telemetry-v2`。干预反馈不走该通用队列，使用独立的
  `intervention_feedback_queue` 实体。
- WorkManager uploads through the authenticated Jeecg mobile client.
- `401` pauses the queue for re-login; transient failures retry the same batch.
- Institution care-plan feedback observes its exact owner-scoped Room queue row after submission.
  Successful upload changes the plan message to “已同步”; transient failures use bounded backoff,
  while permanent rejection or ten exhausted attempts becomes `dead_letter` and is displayed as
  a failure instead of remaining indefinitely in “正在同步”.
- A batch is complete only after backend confirms durable hardware-db persistence.
- Raw signal bytes and entity `rawPayload` fields are excluded.
- Device addresses are SHA-256 hashed before cloud binding/upload.
- Synthetic QA provenance is labelled `synthetic_qa`.
- Room v11 adds user-scoped `diet_records`. Manual meal entry persists locally
  before creating a stable `telemetry-v2 dietRecords` queue item. When no real
  wearable identity is bound, the row remains local and is queued after binding;
  network availability never blocks entry.
- Collection is routed through one `productCode`-selected Provider. The Release
  registry contains only HBand and Viomi Cloud; the user chooses MT116 Bluetooth or
  Viomi IMEI cloud binding. MRD/RWFit remain Debug-only engineering providers.
- Cloud binding and batch provenance derive from the active domain vendor:
  `mrd-*`/`mrd_room`, `rwfit-*`/`rwfit_room`, or `hband-*`/`hband_room`. The latest snapshot excludes
  records whose entity source belongs to another vendor.
- MRD background reconnect uses only the encrypted active binding address. With
  no successful foreground binding, it writes no record and retries later; it
  does not use a fixed address or synthesize missing metrics.
- HBand synchronization attempts ECG history first, then reads live daily sport and
  explicitly awaits `readSleepData` before starting `readOriginData`, because physical-device
  validation found firmware that returned origin records but omitted sleep from `readAllHealthData`. Five-minute
  step, distance, and calorie records are aggregated per day before Room persistence;
  capability-gated manual measurement and body-composition history follow. The SDK layer retains
  the vendor's exact direct HRV/MET double gates (`isSupportHRV && isSupportHrvAppDetect` and
  `isSupportMet && isSupportMetAppDetect`) for compatibility and diagnostics. The `RH-HB-E01`
  product flow does not trust those flags as proof that the purchased firmware accepts the command:
  HRV/stress prefer package-4 mini-checkup or real history, and MET is history-only with no real-time
  action. The purchased MT116's 2026-07-30 all-zero `unknown action` evidence is the regression basis.
  This prevents the SDK's unsupported-feature toast without inventing an instant result;
  only positive real SDK results are persisted. Completed
  reads are retained if a later optional SDK operation fails. Unsupported,
  zero, and invalid readings remain absent; raw ECG samples remain local only.
  HBand ECG uses the matching four-ABI JNI runtime and Room v5: new records store
  calibrated `FLOAT32_LE` mV plus structured lead/sample/duration/contact metadata,
  while migrated legacy `INT32_LE` rows remain relative-only. Neither representation
  is included in telemetry uploads.
- HRV/stress/MET card visibility is value-gated rather than capability-gated. A real Provider Room
  record must contain HRV/MET `> 0` or stress `1..100`; mock, synthetic, missing, zero, non-finite,
  or out-of-range values hide the card. HRV/stress show a measure action only when mini-checkup is
  available; a history-only value has no action. MET never exposes a real-time measure action.
- Room v8 adds nullable `total_sleep_minutes` through a non-destructive v7→v8 migration.
  (实施修正：该列实际由 `9→10` 迁移显式添加，`13→14` 另设带存在性守卫的补列迁移，
  兼容 v11–v13 时代新建的库；此前文档声称 v7→v8 添加与代码不符，测试与说明已修正。)
  HBand persists the SDK-authoritative `allSleepTime` there and period aggregation uses it
  before actual sleep stages (`deep + light + REM`) and finally elapsed session time. Awake
  minutes and the `sleepDown`/`sleepUp` clock span are not counted as HBand sleep duration.
  Queries still use `ended_at`, so cross-midnight sessions ending today remain included.
  When one night has several increasing HBand cumulative snapshots, aggregation keeps the
  preferred final duration for that local wake-up day and averages those daily finals across
  the selected period; Data and Profile use the same selection rule and never treat cloud/local
  copies or intermediate callbacks as separate nights. Activity rows are cumulative day totals;
  presentation keeps the maximum per local day instead of adding overlapping local/cloud copies.
- The Data-screen action is a daily sync for sleep, steps, and activity. When the active Bluetooth
  device is disconnected, it first retries the encrypted bound-device connection with bounded
  backoff; it never scans or connects an unbound device. The in-process automatic cycle uses the
  same reconnect path instead of silently skipping a disconnected device. Explicit
  Foreground Service recovery retains bound-device reconnect behavior. For HBand, only recent Room
  sleep/activity rows owned by the authenticated user and matching the active device plus
  `hband_wearable` source select a two-day-or-greater overlap window; origin history is skipped
  when activity has no gap, while first sync or a gap retains origin-history recovery. Vendor sleep
  and origin callbacks feed monotonic target progress, and Compose smooths toward that target without
  delaying persistence or upload completion.
- For an active Bluetooth binding, the Foreground Service remains the continuous collection path
  once explicitly enabled through “My → Device binding → Background automatic collection”. Android
  13+ requests notification permission before starting it. Re-entering the Main stage proactively
  reconnects only the encrypted last binding through `autoConnect()`; it does not scan, start the
  service, or trigger an immediate ring collection. This restores the device connection without
  repeating the long measurement step every time an older user reopens the app. The same device
  page can explicitly stop collection, and logout still stops it.
- 2026-08-23 隔离与队列强化：Room v20 为通用上传队列 `sync_upload_queue` 增加
  `owner_user_id` 与 in-flight `claim_time` 抢占（Worker 先原子认领再上传，崩溃遗留行 10 分钟
  租约后回收；周期任务与“立即上传”不再并发重发同一批次），所有入队按当前登录用户盖章、所有
  pending 读取按 owner 过滤；通用队列 transient 重试改为 10 次封顶后进入 `dead_letter`；
  设备绑定地址与后台采集设置（开关/间隔/冷却）全部按账号哈希隔离，旧值一次性认领迁移；
  退出登录只停止本地前台采集，不再改动云米服务端计划；遥测 batchId 加入 owner 哈希、source
  与 schemaVersion 使其崩溃重入幂等；测量/上传档位预设补齐契约区间（3–60 分钟与 30–1440
  分钟）；HBand 命令队列锁等待有 30 秒上界；RDI mock 快照不再进入上传队列，血检贡献只接受
  model-service 标准化点（原始化验值不再直接当作贡献分），血压 7 日均值作为单点锚定基线
  可正常建立。

## 主动测量与可配置上传节奏（已实现，真机 QA 待完成）

> 本节在 2026-08-22 已完成代码落地：HBand 使用 `connectedDevice` 前台服务串行主动测量，
> 云米使用 JeecgBoot 持久计划和服务端调度，HBand 遥测由独立 Worker 按配置上传。代码已经
> 通过 Android 单元测试/Debug APK 构建及 JeecgBoot 编译/云米同步测试；物理设备功耗、后台
> 存活、命令码与准确性仍属于发布前门禁。

实现取舍：本次保持 Room v19，不新增原计划中的 checkpoint 表。HBand 每轮真实结果由现有仓库
先写 Room，随后立即创建独立、耐久的 `telemetry_batch` 队列项，但不立即联网；
`TelemetryUploadWorker` 到用户配置的上传时间才发送。每轮批次以现有稳定记录 ID 保持幂等，
既保存 3–5 分钟中间样本，又避免改变已有 Room 迁移链。若未来改为跨轮合并 1,000 条时间窗
批次，再实施本文保留的 Room v20 checkpoint 设计。

### 1. 目标与范围

目标能力：

- 用户可以在 App 中开启或关闭自动测量。
- HBand 通过已绑定 BLE 设备主动执行受支持的测量命令；测量轮次串行执行，完成后等待
  配置间隔再开始下一轮，禁止两个轮次重叠。
- 云米由 ReHealth 服务端向在线手表下发主动测量命令，命令受理后查询真实新记录并持久化；
  App 不持有云米 `AppKey`、`AccessToken` 或完整命令码配置。
- 测量间隔提供 `3 分钟`、`5 分钟`、`10 分钟`、`15 分钟`、`30 分钟`档位，默认
  `5 分钟`；自定义范围建议限制为 `3–60 分钟`的整数分钟。
- HBand 上传间隔提供 `30 分钟`、`1 小时`、`2 小时`、`4 小时`档位，默认
  `2 小时`；自定义范围建议限制为 `30 分钟–24 小时`。
- 30 分钟档位表示最快的常规批量上传节奏，不取消离线队列、幂等、退避、401 暂停和
  durable acknowledgement 规则。
- 用户可看到当前设备、启用状态、测量档位、上传档位、最近一次测量、最近一次成功上传、
  下次计划时间和最近错误的安全摘要；不得在日志或通知中显示健康数值、IMEI、MAC、Token。

不在本阶段范围：

- 不把 ECG、身体成分、血液成分等需要用户姿势确认或电极接触的项目放入无人值守轮次。
- 不把 HBand 体温重新加入 `RH-HB-E01`；当前真机验收仍不支持该指标。
- 不上传原始 ECG/PPG/RRI 波形，不改变 CVD/RHI/RDI 的模型边界。
- 不承诺 Android/OEM 在 App 被用户强制停止、后台权限被系统限制或设备离线时仍能精确到秒执行。

### 2. 实施前基线差距（已用于本次改造）

| 范围 | 当前实现 | 与目标的差距 |
| --- | --- | --- |
| HBand 后台采集 | `RingForegroundService` 固定每 15 分钟调用 `repository.syncAll()` | 主要读取历史/日汇总，不是 3–5 分钟主动测量轮次；间隔不可配置 |
| 页面内自动采集 | `RingViewModel.startAutoCollection()` 内有固定 15 分钟循环，会依次 `measure()` 多个指标 | 当前无正常启动入口，依赖 ViewModel 生命周期，离开页面后不可靠，并会在每轮后立即尝试上传 |
| HBand 命令 | `HBandRingRepository.measure()` 已复用能力判断、命令队列、45 秒超时和 Room 持久化 | 尚无无人值守指标模板、轮次截止时间、每指标冷却和动态调度 |
| 云米采集 | `ViomiCloudRingRepository` 只调用 `/viomi/sync` 拉取已存在历史 | 后端 `ViomiOpenApiGateway` 尚无 `sendcommand`，App 的 `manuallyMeasurableMetrics` 为空 |
| 云米供应商能力 | Postman 资料记录 `9012`、`9510`、`9511` 等主动测量命令 | 命令码存在型号/固件差异；响应只代表受理，不能当作测量值 |
| 上传调度 | `MeasurementSyncWorker` 固定 30 分钟并排空遥测、访谈、RHI/RDI、手填项和反馈 | 不能设置默认 2 小时；改变其周期会错误影响非遥测业务队列 |
| 遥测批次 | `RingCloudRepository.enqueueLatestTelemetry()` 只投影各指标最新值并立即触发 Worker | 3–5 分钟采样的中间真实记录可能不进入批次，也无法按时间窗聚合上传 |
| 配置持久化 | `RingBackgroundCollectionSettings` 仅保存启用状态和最近尝试/成功时间 | 没有按用户、设备和 Vendor 隔离的测量/上传计划，也没有版本和迁移策略 |

### 3. 总体架构

```text
App 设置页
  -> DeviceMeasurementPlan（按登录用户 + productCode + 设备绑定隔离）
     -> HBand：Android connectedDevice Foreground Service
        -> 能力过滤 -> 串行主动测量 -> Room
        -> Telemetry window builder -> durable upload_queue
        -> 可配置 TelemetryUploadWorker -> Device Service -> TimescaleDB
     -> Viomi：JeecgBoot 保存计划
        -> 服务端 due-plan scheduler
        -> Viomi sendcommand（服务端凭据）
        -> 5/15/30/60 秒回查历史或接收 report callback
        -> HardwareIngestionPort -> Device Service/TimescaleDB
        -> App 通过 recent/sync 幂等回填 Room
```

采用 Vendor 分治，而不是强行把两类设备塞进同一个本地定时器：

- HBand 是本机 BLE 链路。3–5 分钟任务必须由用户显式开启的 `connectedDevice`
  Foreground Service 执行；WorkManager 的周期任务最短 15 分钟且执行时间不精确，只能承担
  恢复和上传，不能承担 3–5 分钟 BLE 测量。
- 云米是云端链路。主动命令必须由持有厂商凭据的 JeecgBoot 调度，即使 App 退到后台也不依赖
  手机进程；测量结果一旦由回查或回调取得，应立即进入硬件库，不能为了模拟“2 小时上传”而
  延迟持久化。
- App 的“上传频率”只控制 HBand 本地 Room 到 ReHealth 服务端的批量上传。云米卡片显示
  “测量完成后云端即时入库”，上传档位禁用，避免给用户错误承诺。

Android 官方约束依据：

- [PeriodicWorkRequest 最短周期为 15 分钟](https://developer.android.com/reference/androidx/work/PeriodicWorkRequest)。
- [Android 12+ 后台启动 Foreground Service 存在限制](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)。
- [蓝牙手表持续通信应使用 connectedDevice 前台服务或 Companion Device Manager](https://developer.android.com/develop/background-work/background-tasks/data-transfer-options)。

### 4. 配置模型与档位

建议统一领域模型：

```kotlin
data class DeviceMeasurementPlan(
    val enabled: Boolean,
    val vendor: WearableVendor,
    val productCode: String,
    val bindingKey: String,
    val measurementIntervalMinutes: Int = 5,
    val uploadIntervalMinutes: Int = 120,
    val enabledMetrics: Set<RingMetricType>,
    val quietHoursStartMinutes: Int? = null,
    val quietHoursEndMinutes: Int? = null,
    val updatedAt: Long,
    val version: Long,
)
```

约束：

- `measurementIntervalMinutes`：`3..60`；预设 `3/5/10/15/30`，默认 `5`。
- `uploadIntervalMinutes`：`30..1440`；预设 `30/60/120/240`，默认 `120`。
- 配置键必须由登录用户哈希、`productCode` 和不可逆设备绑定标识组成；不得把原始 userId、
  MAC 或 IMEI 写入日志。
- 首次升级从旧 `RingBackgroundCollectionSettings.active=true` 迁移为：自动测量开启、
  测量 5 分钟、上传 120 分钟；旧的 `last_attempt_at` 只作为迁移参考，不作为新计划游标。
- 修改计划采用完整校验后原子替换；正在运行的 HBand 服务通过 `ACTION_APPLY_SCHEDULE`
  重新读取配置，不并发启动第二个循环。
- 自定义间隔必须显示预计电量影响。3 分钟标记为“高频”，5 分钟为“推荐”，10 分钟以上为
  “省电”。高频模式不绕过设备低电量、未佩戴、BUSY 或系统后台限制。

### 5. HBand 主动测量设计

#### 5.1 无人值守指标模板

不是所有 `manuallyMeasurableMetrics` 都适合后台执行。建议新增
`HBandScheduledMetricPolicy`：

| 指标 | 默认后台策略 | 最小建议间隔 | 说明 |
| --- | --- | --- | --- |
| 心率 | 每轮 | 3 分钟 | 复用 `measureHeartRate()`；只有真实正值写 Room |
| 血氧 | 每轮 | 5 分钟 | 设备支持且佩戴状态有效时执行 |
| 血压 | 默认关闭，可选 | 30 分钟 | 消费级设备结果仅作参考；避免每 3 分钟高频加压和错误姿势数据 |
| HRV / 压力 | 默认关闭，可选 | 15 分钟 | 只走已验证 mini-checkup 或真实历史回退；不触发 MT116 的 `unknown action` 路径 |
| 步数 / 活动 / 睡眠 | 历史同步 | 30 分钟或日常同步 | 不属于主动测量命令；与高频轮次解耦 |
| MET | 只读历史 | 不主动测量 | 保持现有产品限制 |
| ECG / 身体成分 / 血液成分 | 仅手动 | 不调度 | 需要姿势、电极或用户确认 |
| 体温 | 不支持 | 不调度 | `RH-HB-E01` 当前真机验收未通过 |

“一轮”定义为当前模板中到期指标的串行集合，不是无条件调用全部 SDK 能力。若用户选择 3 分钟，
心率可以每轮执行，血氧按自身 5 分钟冷却到期后执行，血压仍至少等待 30 分钟。这样既满足
3–5 分钟持续观察，又避免一轮包含多个 45 秒命令导致调度永久积压。

#### 5.2 轮次状态机

```text
IDLE
  -> WAITING_FOR_DUE_TIME
  -> CHECK_PERMISSION_AND_BINDING
  -> RECONNECT_BOUND_DEVICE（仅加密保存的绑定；禁止后台扫描）
  -> SELECT_DUE_METRICS
  -> MEASURE_METRIC_1 -> PERSIST_ROOM
  -> MEASURE_METRIC_N -> PERSIST_ROOM
  -> ENQUEUE_UPLOAD_WINDOW_IF_DUE
  -> RECORD_SAFE_STATUS
  -> WAIT interval after completion
```

实现规则：

- 轮次完成后再计算下一次时间，即 `nextDueAt = completedAt + interval`，避免长命令结束后立即
  补跑造成设备连续工作；UI 文案明确为“每轮完成后等待 N 分钟”。
- 整轮只允许一个 Job；配置更新、手动测量、页面同步和 Provider 切换继续通过
  `ActiveRingRepository` 路由互斥锁及 `HBandCommandQueue` 串行化。
- 测量间隔更新不得通过 stop/start 前台服务生效：运行中的循环每轮重读间隔配置，
  新档位下一轮生效。stop→start 会在快速点击时把 `startForegroundService()` 的 5 秒
  看门狗挂在已 `stopSelf()` 的服务上，系统随后以
  `RemoteServiceException$ForegroundServiceDidNotStartInTimeException` 杀死进程；
  `onStartCommand(ACTION_START)` 必须首先调用 `startForeground()`（2026-08-24 真机
  crash buffer 实证，两台复现均为此异常）。
- 单指标沿用 45 秒测量超时；建议增加整轮最大 120 秒截止时间。到达截止时间时保留此前已写入
  Room 的成功记录，其余指标标记为跳过，禁止回滚真实结果。
- 断连时只对已绑定地址执行现有 `autoConnect()` 有界重连；失败后结束本轮，不做环境扫描。
- `LOW_POWER`、`WEAR_OFF`、`BUSY`、不支持、超时、无有效值均不生成占位测量；只保存安全状态码
  和下次重试时间，不记录健康数值。
- 服务通知显示“自动测量运行中 / 下次约 HH:mm / 已暂停：蓝牙关闭”等状态，不显示具体指标值。
- `RingViewModel.startAutoCollection()` 的页面级循环应在新服务通过验收后删除，或收敛为调用
  Foreground Service 的单一入口，避免两套调度器重复测量。

#### 5.3 Android 后台与恢复

- 继续使用 `RingForegroundService` 的 `connectedDevice` 类型，由用户在可见页面点击开启。
- 在服务内部使用可取消的单一协程和动态计划，不使用 3/5 分钟 `PeriodicWorkRequest`。
- `RingBackgroundRecoveryWorker` 保留 15 分钟以上的尽力恢复职责；它不是精确定时器。
- 增加 `BOOT_COMPLETED` 接收器前必须完成 Android 12–15 真机验证。接收器只在用户此前明确开启、
  有有效绑定且系统允许时恢复；捕获 `ForegroundServiceStartNotAllowedException` 后保持计划启用但
  标为“等待用户打开 App 恢复”，不得循环崩溃。
- 可在后续阶段接入 Companion Device Manager presence API，提高已配对手表在后台恢复服务的合规性；
  不把申请忽略电池优化作为默认强制步骤。

### 6. HBand Room 批量上传设计

#### 6.1 拆分采集节奏和上传节奏

测量成功只负责 Room 持久化，不立即发网络请求。上传到期后再从 Room 构造时间窗批次：

```text
3/5 分钟测量 -> ring_measurements（每条真实记录均保存）
30/60/120/240 分钟到期
  -> 查询上次已入队游标之后的当前用户 + 当前设备 + 当前 Vendor 记录
  -> 构建 telemetry-v2 批次
  -> 同一 Room 事务写 upload_queue + 推进 checkpoint
  -> TelemetryUploadWorker 在有网络时上传
```

当前 `enqueueLatestTelemetry()` 只发送每类最新值，不足以承载 3–5 分钟时序。建议新增：

- `TelemetryUploadCheckpointEntity`（Room v20）：按 `owner_user_id + device_id + source`
  保存 `last_enqueued_measured_at`、`last_batch_id`、`updated_at`。
- DAO 时间窗查询：测量、睡眠、活动均限定当前登录用户、当前绑定设备、Vendor source 和
  `(checkpoint, cutoff]`；允许 1–2 分钟重叠并依赖稳定记录 ID 去重。
- `RingCloudRepository.enqueueTelemetryWindow(...)`：发送时间窗内全部规范化测量，而不是每类
  只取最新一条。
- 稳定 `batchId`：由不可逆 owner key、deviceId、source、windowStart、windowEnd 和 schemaVersion
  计算；崩溃后重复构造仍得到同一个批次。
- 单个客户端批次建议最多 1,000 条，超过时按时间切片；服务端现有生产上限为 5,000 条。
- ECG/PPG/RRI 和 `rawPayload` 继续排除。

Room v20 迁移必须是非破坏性的；新增表和索引，不修改或删除 v19 健康记录。Android Room 不支持
数据库 `COMMENT` 语法，表/字段语义需在 Entity、迁移测试和本文件中说明；MySQL/TimescaleDB
如新增表则必须在迁移 SQL 中为表和字段添加注释。

#### 6.2 独立遥测上传 Worker

不要直接把现有 `MeasurementSyncWorker` 从 30 分钟改成 2 小时，因为它还承担健康访谈、
RHI/RDI、手填健康项和干预反馈。建议拆分：

- `TelemetryUploadWorker`：只处理 `kind=telemetry_batch`，周期由上传档位决定；默认 120 分钟。
- `MeasurementSyncWorker`：保留非遥测业务队列的现有 30 分钟兜底和用户操作后的即时触发。
- 用户修改上传档位时，以 `ExistingPeriodicWorkPolicy.UPDATE` 更新唯一周期任务，不能继续使用
  `KEEP`，否则旧周期不会生效。
- 用户点击“立即上传”时可提交一次 `OneTimeWorkRequest`；自动测量入队不得每轮立即触发网络。
- 网络不可用、401、服务不可用或 durable ack 缺失时继续使用现有 retry/dead-letter 策略；
  checkpoint 代表“已可靠入队”，不代表“服务端已成功持久化”，队列项必须保留到 ack 成功。

### 7. 云米主动测量设计

#### 7.1 为什么必须服务端执行

云米 `sendcommand` 需要供应商 `AccessToken`，其获取依赖 `AppId/AppKey`；这些凭据已经按现有
架构只保存在 JeecgBoot。云米手表也不是手机 BLE 连接，使用 Android 3–5 分钟 Foreground
Service 只会增加耗电和失败率。因此：

- App 只保存用户计划并调用 ReHealth 认证 API。
- JeecgBoot 校验当前用户与云米设备的有效绑定后保存计划。
- 服务端集群调度器挑选到期计划并调用云米 OpenAPI。
- 任何 API 都不接受客户端传入 `userId`/`tenantId`，身份来自认证上下文。

#### 7.2 供应商命令与能力门禁

现有供应商资料提供以下候选命令，但不能直接写死为所有设备通用：

| 指标 | 候选命令码 | 当前结论 |
| --- | --- | --- |
| 心率/健康数据 | `9012` | 可进入受控试点，仍需按型号/固件确认 |
| 血压 | `9510` | 必须取得型号/固件书面确认后启用 |
| 血氧 | `9511` | 文档与 `9726` 定义冲突；禁止自动尝试 `9726` |

命令码应放在服务端配置或受审计的产品能力表中，以 `productCode + vendorModel + firmwareRange`
匹配；没有确认映射时 API 返回“不支持”，不得猜测或轮询尝试多个命令码。

#### 7.3 命令任务状态机

```text
DUE
  -> VALIDATE_BINDING_AND_CONSENT
  -> READ_BASELINE_LATEST_TIMESTAMP
  -> SEND_COMMAND(reqId)
  -> ACCEPTED（仅接受 Code 0/1/1803 的端点级规则）
  -> POLL 5s -> 15s -> 30s -> 60s / WAIT_CALLBACK
  -> NEW_REAL_RECORD_FOUND
  -> HardwareIngestionPort durable persistence
  -> SUCCEEDED

失败分支：OFFLINE(1800) / SEND_TIMEOUT(1801) / REJECTED(1802) /
VENDOR_ERROR / RESULT_TIMEOUT / UNBOUND / DISABLED
```

完成判定必须同时满足：

1. 新记录的测量时间晚于发送前基线；
2. 指标类型与命令一致；
3. 数值通过现有生理范围校验；
4. 当前用户仍拥有该绑定；
5. HardwareIngestionPort 返回 durable persisted acknowledgement。

超过 60 秒没有新记录时任务记为 `RESULT_TIMEOUT`，下一计划轮次可继续，但不生成假数据。
厂商回调 `/rehealth/viomi/report` 与主动回查必须使用同一稳定记录 ID/幂等键，先到者完成任务，
后到者作为重复记录安全接受。

#### 7.4 后端表与调度

建议在 `software_db` 新增两张表；创建迁移时必须包含表注释和全部字段注释：

实际 MVP 将计划和最近执行状态合并到一张带完整注释的
`rehealth_viomi_measurement_plan` 表；当前任务同步完成且不需要单独长期查询，因此没有预建
job 表。若后续增加异步单指标任务、供应商高并发或独立任务查询，再按下述设计增加 job 表。

1. `rehealth_device_measurement_plan`
   - `id`、`tenant_id`、`user_id`、`device_id`、`vendor`、`product_code`
   - `enabled`、`measurement_interval_seconds`、`upload_interval_seconds`
   - `metric_set_json`、`quiet_hours_start`、`quiet_hours_end`
   - `next_measure_at`、`last_measure_at`、`last_success_at`、`last_error_code`
   - `lock_version`、`created_at`、`updated_at`
   - 唯一键：`tenant_id + user_id + device_id + vendor`

2. `rehealth_viomi_measurement_job`
   - `id`、`plan_id`、`request_id`、`tenant_id`、`user_id`、`device_id`
   - `metric_type`、`vendor_command_code`、`baseline_measured_at`
   - `status`、`vendor_code`、`sent_at`、`deadline_at`、`next_poll_at`、`poll_attempts`
   - `result_record_id`、`error_code`、`created_at`、`updated_at`
   - 唯一键：`request_id`；索引：`status + next_poll_at`

集群调度器建议每 30 秒扫描到期计划和任务，使用数据库行锁/乐观锁抢占，不能为每个用户创建
一个 JVM Timer。一次只推进有限数量任务，设置 Vendor 全局并发和每设备单命令锁；命令失败采用
有界退避，不能无限重试或在日志中输出 IMEI、Token 和健康值。

### 8. API 契约

建议新增认证移动端契约：

实际 MVP 已冻结为 `PUT /rehealth/mobile/viomi/measurement-plan`，只接收当前绑定 IMEI、
启用状态、3–60 分钟间隔和指标白名单；用户身份始终取认证会话。对应 Android/Java DTO 和
`rehealth-mobile-v1.openapi.json` 1.6.0 已同步。GET/立即测量/job 查询留到产品需要时再增加。

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/rehealth/mobile/device-measurement-plan` | 获取当前认证用户、当前有效绑定的计划和最近安全状态 |
| `PUT` | `/rehealth/mobile/device-measurement-plan` | 校验并保存启用状态、档位、指标模板和 quiet hours；使用 `version` 乐观锁 |
| `POST` | `/rehealth/mobile/device-measurement-plan/measure-now` | 用户可见页面触发单轮测试；HBand 仍由本地执行，云米由后端创建一次性任务 |
| `GET` | `/rehealth/mobile/device-measurement-plan/jobs/{requestId}` | 查询云米一次性任务状态，不返回厂商 Token、IMEI 或原始响应 |

DTO 要求：

- 不接收 `userId`、`tenantId`、原始 MAC、完整 IMEI 或厂商命令码。
- `measurementIntervalMinutes` 只接受 `3..60`；`uploadIntervalMinutes` 只接受
  `30..1440`。
- `enabledMetrics` 先与当前绑定产品能力求交集；返回 `acceptedMetrics` 和
  `unsupportedMetrics`，不能静默伪装成功。
- 返回 `effectiveAt`、`nextMeasureAt`、`lastMeasurementAt`、`lastUploadAt`、
  `status`、`version` 和安全错误码。
- OpenAPI、Android DTO、控制器 DTO、鉴权和契约校验脚本必须同一提交更新。

### 9. App 交互方案

在“我的 → 设备绑定”现有后台采集卡片扩展为“自动测量与数据上传”：

1. 总开关：默认关闭，必须由用户显式开启。
2. 测量频率：`3 分钟（高频）`、`5 分钟（推荐）`、`10 分钟`、`15 分钟`、
   `30 分钟`、`自定义`。
3. 上传频率（仅 HBand）：`30 分钟`、`1 小时`、`2 小时（默认）`、`4 小时`、
   `自定义`；云米显示“云端测量完成后即时安全入库”。
4. 测量内容：展示当前产品实际支持且允许无人值守的指标；危险或需接触确认的指标不可勾选。
5. 时间范围：可选全天或自定义 quiet hours；夜间仍允许睡眠/活动历史同步，但主动测量按用户设置暂停。
6. 状态区：最近测量、下次计划、最近上传、待上传批次数、最近安全错误；不显示敏感标识。
7. 操作：`保存设置`、`立即测量一轮`、`立即上传`、`关闭自动测量`。

开启前检查：

- 已登录且当前绑定属于当前用户；
- HBand 已完成真实画像、BLE/通知权限和设备绑定；
- 云米绑定仍有效且后端声明当前型号支持主动命令；
- 3 分钟模式展示手机与手表耗电提示；
- 用户切换 Vendor 时先停止旧计划，保存新绑定后再要求确认是否启用新计划，不能自动继承高频模式。

### 10. 失败、隐私与安全策略

- 所有 HBand 结果必须先写当前用户/设备隔离的 Room；写库失败时不得推进上传 checkpoint。
- 云米命令受理不等于测量成功；只有真实新记录入硬件库后才显示成功。
- 网络失败不得停止 HBand 本地测量；待上传窗口继续积累，恢复后按稳定批次补传。
- 401 暂停上传但不删除本地记录；HBand 在无有效登录用户时停止新轮次，避免无法确定 owner。
- 云米计划属于账号级服务端配置。退出登录不应悄悄改变计划；App 必须在设置页明确提示，解绑设备
  或关闭开关才撤销服务端计划。此产品决定需在实现前由产品/隐私负责人确认并写入隐私说明。
- 用户删除账号、解绑设备或撤回计划时，服务端立即禁用后续命令；已落库数据按现有保留/删除政策处理。
- 生产日志只记录计划 ID、哈希设备 ID、状态码、耗时和计数；不记录原始健康数值、Token、IMEI、
  MAC、手机号或完整用户 ID。
- 所有消费级测量继续标注“仅供健康管理参考，不能替代医疗诊断”。

### 11. 分阶段实施任务

#### 阶段 A：契约冻结与供应商确认（1–2 人日，不含供应商等待）

- [ ] 产品确认“轮次完成后等待 N 分钟”的语义、默认 5 分钟测量和默认 2 小时上传。
- [ ] 确认 HBand 每个无人值守指标的设备耗电、佩戴错误码和最小安全间隔。
- [ ] 向云米按 S8/S9/GS20/GS17/A67/K9L 型号与固件取得 `9012/9510/9511` 书面映射、
  成功码、频控和每天命令上限；在确认前只允许测试环境 feature flag。
- [x] 冻结 API、DTO 和 MySQL 计划表；Room v20 由“每轮立即耐久入队”方案替代。

#### 阶段 B：Android 配置与 HBand 主动轮次（3–5 人日）

- [x] 已实现 3–60 分钟测量配置、前台服务主动轮次、串行命令、自动重连和 30 分钟血压冷却。

- [ ] 新增 `DeviceMeasurementPlan`、用户/设备隔离的计划 Store 和旧设置迁移。
- [x] 将 `RingBackgroundCollectionPolicy` 改为动态间隔并使用轮次完成时间计算下一次 due time。
- [x] 在 `RingForegroundService` 实现单 Job 循环、血氧/血压冷却和安全通知状态；整轮沿用各指标 45 秒上限。
- [x] 复用 `HBandRingRepository.measure()`、`HBandCommandQueue`、能力判断和 Room 持久化。
- [x] 移除/收敛 `RingViewModel` 的重复自动循环。（已删除页面级 15 分钟循环与每轮立即上传，
  前台服务成为唯一无人值守调度器；`stopAutoCollection` 保留为生命周期钩子。）
- [x] 扩展 `DeviceBindingScreen` 预设、自定义设置和输入范围校验。

#### 阶段 C：Room 时间窗批次与可配置上传（3–4 人日）

- [x] 采用每轮 Room 落库后立即创建耐久批次，不升级 Room schema。
- [ ] 增加按 owner/device/source/time-window 查询和最多 1,000 条切片。
- [x] 复用稳定 batchId 和记录 ID，每轮入队；上传 Worker 与业务 Worker已拆分。
- [x] 拆出 `TelemetryUploadWorker`，支持 30/60/120/240/自定义周期与 `UPDATE` 重调度。
- [x] 保持业务队列即时触发和 30 分钟兜底，不让 2 小时遥测档位延迟干预反馈。

#### 阶段 D：云米服务端主动测量（5–8 人日，不含供应商联调）

- [x] 已实现计划 API、加密 IMEI、到期抢占、命令白名单、5/10/15/30 秒轮询和硬件入库。
- [ ] 供应商必须按具体型号/固件确认命令码后，通过环境变量启用；默认 `0` 会拒绝下发。

- [x] 扩展 `ViomiOpenApiGateway`：命令下发、最新时间基线和端点级状态码策略。
- [x] 新增带表/字段注释的 MySQL 迁移、计划服务和执行状态。
- [x] 新增数据库条件抢占的 due-plan scheduler；多实例不会重复领取同一到期计划。
- [x] 复用 `ViomiPullService.normalize()` 和 `HardwareIngestionPort` 持久化路径。
- [x] 新增移动 API、OpenAPI、Android DTO/UI；计划回读暂用按登录用户哈希隔离的成功响应缓存。

#### 阶段 E：真机 QA、灰度和发布（3–5 人日）

- [ ] HBand 与云米物理设备的锁屏、重启、离线、功耗、命令码及准确性门禁仍待执行。

- [ ] HBand 物理设备连续 6/12/24 小时测试 3 分钟与 5 分钟档位。
- [ ] 云米每个准入型号完成命令受理、离线、超时、回调/回查竞态测试。
- [ ] 断网、401、进程杀死、重启、切换账号、切换 Vendor、低电量、未佩戴、蓝牙关闭测试。
- [ ] 先对白名单账号启用；监控稳定后再扩大范围，保留服务端/客户端 kill switch。

预计总工作量约 `15–24 人日`，不包含云米书面确认和真实设备排队时间。最不确定部分是云米
型号命令映射、供应商频控，以及 HBand 在 3 分钟连续主动测量下的设备续航与固件稳定性。

### 12. 目标文件清单

Android 预计修改：

- `ring/RingBackgroundCollectionSettings.kt` 或新增 `ring/DeviceMeasurementPlanStore.kt`
- `ring/RingBackgroundCollectionPolicy.kt`
- `service/RingForegroundService.kt`
- `ring/RingViewModel.kt`
- `ui/DeviceBindingScreen.kt`
- `ring/hband/HBandRingRepository.kt`（只增加调度策略入口，不复制 SDK 命令）
- `data/sync/RingCloudRepository.kt`
- `data/sync/SyncRepository.kt`
- `work/TelemetryUploadWorker.kt`、`work/MeasurementSyncWorker.kt`
- `data/AppDatabase.kt`、`ring/data` 与 `data/sync` 下的 DAO、Room v20 Entity/Migration
- `network/ReHealthApi.kt`、`AuthenticatedApiClient.kt`、计划 DTO

后端预计修改：

- `viomi/ViomiOpenApiGateway.java`、`ViomiOpenApiClient.java`
- `viomi/ViomiPullService.java` 及新增计划/任务服务
- `mobile/controller/ReHealthMobileController.java` 或独立 measurement-plan controller
- `mobile/dto/*MeasurementPlan*.java`
- `repository/ReHealthBusinessRepository.java` 及 MyBatis 实现
- `src/main/resources/db/software/mysql/V...__add_device_measurement_plan.sql`
- `backend/contracts/openapi/rehealth-mobile-v1.openapi.json`
- `backend/contracts/scripts/validate_contracts.py`

文档预计同步：

- 根 `README.md` 的采集/上传数据流与文档索引
- `Android-apk/README.md`
- `Android-apk/docs/REHEALTH_INTEGRATION_CONTRACT.md`
- 本文件和 `BLE_BACKGROUND_QA.md`、`HBAND_DEVICE_QA.md`
- `backend/docs/MOBILE_API.md`、`REHEALTH_BACKEND_API.md`、数据库 schema 文档
- `STATUS.md` 与发布检查清单（只有代码和 QA 证据完成后才标记 implemented）

### 13. 测试计划与验收标准

#### 自动化测试

- 计划校验：预设/自定义边界、旧配置迁移、用户/设备/Vendor 隔离、乐观锁冲突。
- 调度策略：首次立即、轮次完成后等待、时间回拨、配置更新、quiet hours、长轮次不重叠。
- HBand：能力过滤、每指标冷却、45 秒超时、120 秒整轮截止、BUSY/LOW_POWER/WEAR_OFF、
  成功记录仍写 Room、失败不生成假值。
- Room v20：19→20 migration、window query、1–2 分钟重叠、稳定 batchId、崩溃重入、
  queue/checkpoint 原子性、1,000 条切片。
- Worker：默认 120 分钟、切到 30 分钟后 `UPDATE` 生效、网络约束、401 暂停、durable ack、
  遥测与业务队列互不延迟。
- 云米：命令码能力表、0/1/1803 受理、1800/1801/1802、Token 刷新、5/15/30/60 秒回查、
  超时无假值、callback/poll 幂等、跨租户/跨用户拒绝。
- 契约：Android DTO、Java DTO、OpenAPI、控制器路由和数据库迁移门禁。

建议命令：

```powershell
cd Android-apk
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug :app:lintDebug

cd ..\backend\jeecg-boot
mvn -pl jeecg-boot-module/jeecg-module-rehealth -am test

cd ..\..
python backend/contracts/scripts/validate_contracts.py
```

#### 真机验收

1. HBand 选择 3 分钟档位运行 6 小时：轮次不重叠，实际间隔按“完成后等待”计算，成功记录全部
   带当前 owner/device/source 写入 Room。
2. HBand 断网 6 小时：测量继续，本地记录不丢；恢复网络后按稳定批次上传且服务端无重复。
3. 默认 2 小时上传：在允许 WorkManager 浮动的窗口内形成并持久化批次；切换 30 分钟档位后旧
   periodic work 被更新，不同时存在两套任务。
4. 低电量、未佩戴、蓝牙关闭、进程被回收、手机重启后不崩溃、不扫描陌生设备、不生成零值。
5. 云米命令返回受理但无新记录时显示超时；只有真实晚于基线的新记录才能显示成功。
6. 两个账号绑定不同设备时，计划、Room、队列、服务端任务和状态完全隔离。
7. 24 小时测试记录手机耗电、HBand/云米设备耗电、成功轮次率、P50/P95 测量耗时、待上传队列
   深度、上传延迟和重复率；没有真实证据不得解除发布门禁。

#### 验收阈值建议

| 指标 | 建议门槛 |
| --- | --- |
| HBand 成功轮次率 | 已连接、已佩戴且非低电量窗口内 ≥ 95% |
| 轮次重叠 | 0 |
| Room 持久化丢失 | 0 |
| 服务端重复业务记录 | 0（允许幂等重复请求，不允许重复落库） |
| 默认上传延迟 | 2 小时档位下 P95 ≤ 2 小时 30 分钟（考虑 WorkManager 浮动与网络） |
| 30 分钟上传延迟 | P95 ≤ 45 分钟（网络可用、无系统限制） |
| 云米假成功 | 0；无新记录必须是 timeout/no-data |
| 跨用户/跨租户泄露 | 0 |
| 原始健康值/凭据日志 | 0 |

### 14. 实施前必须确认的产品决策

在进入代码实现前，产品/设备/隐私负责人需确认：

1. 3 分钟和 5 分钟是“上一轮完成后的等待时间”，还是固定的 start-to-start 周期；本方案选择前者。
2. 默认无人值守指标是否仅心率 + 血氧；血压是否允许用户显式开启且最短 30 分钟。
3. HBand 高低电量阈值、3 分钟模式最长持续时间和是否需要充电中才能长期开启。
4. 云米每个型号/固件允许的命令码、频控、每天上限和厂商责任边界。
5. 用户退出登录后云米服务端计划是否继续；本方案建议计划属于账号设置，只有关闭或解绑才停止，
   但必须在 UI 和隐私政策中明确。
6. 自定义 quiet hours 是否为首版必需；若工期受限，可先交付档位和全天模式，quiet hours 放第二阶段。

## Software-Only Validation

- DTO/route contract tests with MockWebServer.
- Room-to-telemetry mapping tests, including stable batch identity and raw-data exclusion.
- Queue retry, durable acknowledgement, malformed payload, and 401 policy tests.
- Intervention-feedback tests cover synced/retrying/dead-letter presentation and retry exhaustion.
- Diet repository tests cover local-first save, structured batch mapping and
  single enqueue after a delayed device binding; migration 10→11 has an
  instrumentation migration test.
- Debug Kotlin compilation, JVM unit tests, and debug APK assembly.
- Viomi mapping/range/scope unit tests, Room 14→15 migration-test compilation, and backend
  Shanghai-time/range validation tests.
- Risk feature queries and HBand incremental-window reads are owner scoped; HBand tests reject
  recent rows from another user or device. Background-entry policy tests cover binding, Bluetooth,
  Android 13+ notification permission, and the absence of a synthesized local risk/plan default.

## HARDWARE_QA_PENDING

The following cannot be accepted without the applicable physical MRD/RWFit ring or HBand wearable
and Android 13+ test device:

- BLE scan/connect/reconnect and permission behavior.
- First-bind address persistence and restart/background reconnect using that
  binding, including the no-binding no-connect case.
- MR11/RWFit/HBand SDK commands, timestamp/unit mapping, and measurement accuracy.
- Foreground collection across screen-off, process restart, and network loss.
- Long-duration duplicate/loss rate and upload latency.
- Battery consumption and thermal behavior.
- Raw-signal capability and consent gate, if enabled in a later release.

No synthetic record may be presented as evidence for these hardware gates.
