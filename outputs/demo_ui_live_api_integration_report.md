# Demo UI Live API Integration Report

更新时间：2026-07-22

## 1. 最终结论

已在 `D:\rehealth_demo\Android-apk` 的 `codex/real-device` 分支上完成整合，保留该分支的 Demo Compose 页面编排、五页底部导航、第二页仪表盘结构和归因页卡片/展开交互；未用真实能力来源仓库的重写 UI 覆盖它们。

已接入并用于正式运行路径的能力：

- MRD BLE 扫描、GATT 连接、服务发现、通知订阅、命令写入和 Room 落库。
- Android 12+ `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT` 权限申请、永久拒绝跳系统设置，以及权限撤销竞态保护。
- Room 中真实测量、睡眠、活动历史数据的 1/7/30/90 天聚合展示。
- `POST /rehealth/mobile/features/evaluate` 风险评估请求、`X-Access-Token` 鉴权、snake/camel 响应解析和网络/401/403/5xx/空结果状态。
- 归因页真实风险分和真实特征贡献映射。预测曲线、饮食记录、干预计划在后端未返回时显示空状态，不再展示固定 Demo 数据。

尚未完成或无法在当前环境确认：实体 MRD 戒指的真实 GATT 服务发现、通知数据解析和多轮测量；真实后端联调需要用户提供 `REHEALTH_API_BASE_URL` 和 `REHEALTH_API_TOKEN`；健康采访仍使用明确命名的 `MockHealthInterviewModel`。

APK 构建成功：

`D:\rehealth_demo\Android-apk\app\build\outputs\apk\debug\app-debug.apk`

## 2. 分支信息

- 最终工作区：`D:\rehealth_demo\Android-apk`
- 基础分支：`codex/real-device`
- 当前分支：`codex/real-device`，按用户要求未新建整合分支
- 真实能力参考仓库：`D:\rehealthAI\Android-apk`，其 remote 为 `wjmlong/rehealth_tongbu.git`
- Demo UI 来源仓库：`D:\rehealth_demo\Android-apk`，其 remote 为 `RehealthAI/Android-apk.git`
- 当前提交前 HEAD：`93c1a728d4d65d3f8504fdd6d6c0f307d724de89`
- 最终提交哈希：`48bf5af` (`feat(android): integrate live data into demo UI`)
- 本轮 MRD/PIAS 验证提交：`b666b76` (`feat(android): validate MRD replay and PIAS flow`)

比对报告中曾将本地真实能力工作树称为另一个 `work/D3...` 分支；实际本次按 Git 和文件内容核对后，最终工作区是 `D:\rehealth_demo\Android-apk` 的 `codex/real-device`，真实能力参考为 `D:\rehealthAI\Android-apk`。两者 UI 不能混用。

## 3. 修改文件清单

修改：

- `app/build.gradle.kts`：增加 API Base URL/Token 的 `local.properties`、环境变量和 BuildConfig 配置；增加 Retrofit/Moshi/OkHttp 测试依赖。
- `app/src/main/AndroidManifest.xml`：补齐 Android 12+ BLE 权限声明，并为扫描权限声明 `neverForLocation`。
- `app/src/main/java/com/rehealth/genie/ring/RingViewModel.kt`：消费 Room 历史流；阻止未连接设备时自动采集；补充风险请求异常状态和用户提示。
- `app/src/main/java/com/rehealth/genie/ring/data/RingDataDao.kt`：增加近期测量、睡眠、活动历史流查询。
- `app/src/main/java/com/rehealth/genie/ring/mrd/MrdBleRingRepository.kt`：扫描/连接/GATT/通知/写入/关闭前权限检查；移除生产日志中的设备地址和原始健康 payload。
- `app/src/main/java/com/rehealth/genie/ui/ReHealthApp.kt`：恢复并保留完整 Demo Compose UI；第二页改为 Room 时间窗口聚合；归因页和模型页接入真实风险状态、空状态和刷新；移除固定风险曲线、午餐、干预和个人统计展示；设备页补全权限状态机。

新增：

- `app/src/main/java/com/rehealth/genie/live/LiveRiskState.kt`
- `app/src/main/java/com/rehealth/genie/live/LiveRiskRepository.kt`
- `app/src/main/java/com/rehealth/genie/network/LiveRiskApi.kt`
- `app/src/main/java/com/rehealth/genie/ring/RingBleGuards.kt`

删除：无。

## 4. 能力迁移清单

- 真实 API：Retrofit `LiveRiskApi.evaluate` 调用 `/rehealth/mobile/features/evaluate`。
- 网络层：Moshi DTO、OkHttp Token interceptor、默认模拟器地址 `http://10.0.2.2:8080/jeecg-boot`；私有 Token 不写入源码。
- 数据模型：`FeatureVectorPayload`、`FeatureQualityPayload`、`RiskResponse`，同时兼容 camelCase/snake_case 响应字段。
- Repository：`LiveRiskRepository` 从 Room 记录生成已验证的 SBP/DBP/运动天数特征，并返回 UI-safe `LiveRiskState`。
- 设备能力：`MrdBleRingRepository` 负责 BLE 扫描、GATT、服务发现、通知订阅、MRD 协议命令、解析和 Room 写入。
- 鉴权：使用 `X-Access-Token`；Token 缺失时明确阻止真实风险请求。
- 状态管理：`RingViewModel` 管理连接、采集、历史数据、风险加载/成功/空/失败和取消范围。
- 错误处理：网络不可用、超时、解析失败、401、403、5xx、权限不足、蓝牙关闭、扫描为空和无数据均转成用户可读消息。
- Mock 清理：正式数据页、归因页、模型页和“我的”统计不再读取 Mock；`MockRingRepository`、`MockPhmService`、`MockHealthInterviewModel` 仍保留用于开发/采访遗留路径，未作为正式戒指仓库装配。

## 5. 页面映射

```text
第二页数据
→ RingViewModel.uiState
→ RingDataDao.observeRecentMeasurements/observeRecentSleepSessions/observeRecentActivities
→ MRD BLE Repository → Room
→ RingMeasurementEntity/RingSleepSessionEntity/RingActivityEntity
→ DataScreen 的 1/7/30/90 天聚合 UI 模型

归因页
→ RingViewModel.refreshLiveRisk
→ LiveRiskRepository
→ LiveRiskApi POST /rehealth/mobile/features/evaluate
→ RiskResponse
→ LiveRiskState.Ready
→ AttributionScreen 的真实风险分和 featureContributions 展开卡片

模型页
→ RingViewModel.uiState + LiveRiskState
→ Room 本地数据 + LiveRiskRepository
→ ModelScreen 的真实输入可用数、风险状态和刷新交互

设备绑定页
→ RingViewModel.scan/connect/syncAll/measure
→ MrdBleRingRepository
→ Android BluetoothAdapter/BluetoothGatt + MRD 协议
→ Room
→ DeviceBindingScreen 权限、连接、空扫描和失败状态
```

## 6. 验证结果

在 Windows 主机使用 JDK `D:\Android_Studio\jbr`、SDK `D:\Android_SDK` 执行：

| 命令 | 结果 |
|---|---|
| `.\gradlew.bat testDebugUnitTest --no-daemon --max-workers=1` | 成功；当前工作区该任务为 `NO-SOURCE` |
| `.\gradlew.bat lintDebug --no-daemon --max-workers=1` | 成功；无 MissingPermission 阻塞 |
| `.\gradlew.bat assembleDebug --no-daemon --max-workers=1` | 成功；生成 debug APK |

模拟器：`emulator-5554`，`ReHealth_Huawei_API31`，API 31。

- APK 安装成功，`com.rehealth.genie` 进程存活，`MainActivity` resumed。
- 五个底部导航项可见：首页、数据、归因、模型、我的。
- 第二页显示 `近 7 天身体状态总览`、`所选周期暂无真实戒指数据`、`--` 和 `待评估`，没有固定生命体征。
- 归因页显示真实评估等待/失败空状态、真实贡献因素容器和无固定午餐/干预数据。
- 设备页权限流程可打开；API 31 上 `BLUETOOTH_SCAN` 和 `BLUETOOTH_CONNECT` 均验证为 `granted=true`。
- 模拟器的 Bluetooth HAL/`svc bluetooth enable` 曾发生系统组件崩溃，因此没有把虚拟机扫描结果或 GATT 连接结果声称为实体戒指验证。

## 7. 遗留问题

- 必须用实体 MRD 戒指在 Android 12+ 真机验证 GATT 服务 UUID、通知订阅、协议包解析、测量时长、断线重连和 Room 落库。
- 当前风险请求只从戒指数据生成 SBP/DBP、运动天数，其余 CVD 字段标记为 `MISSING`；应接入健康档案和临床报告输入后再进行完整模型评估。
- 后端地址和 Token 需要通过未提交的 `local.properties` 或环境变量配置；不得提交密钥。
- 真实干预计划、风险历史曲线、饮食记录 API 尚未接入，页面保留原 Demo 编排但只展示空状态。
- `HealthInterviewFlow` 仍使用 `MockHealthInterviewModel`，需要后续接入真实健康档案服务。
- 旧的 `MockRingRepository`、`MockPhmService` 文件保留给 Preview/测试隔离，正式 `ReHealthApplication` 使用 `MrdBleRingRepository`。

## 8. 2026-07-22 MRD SDK、PIAS 与既有后端能力复核

### 本轮结论

- 目标仓库仍为 `D:\rehealth_demo\Android-apk`，分支仍为 `codex/real-device`；没有创建新分支，也没有将其他分支的 Compose UI 覆盖进来。
- Debug 构建可通过显式开关使用 `CapturedMrdReplayRepository`；Release 构建固定装配 `MrdBleRingRepository`，抓包重放代码不进入正式运行路径。
- API 31 模拟器已完成：访客 onboarding、BLE 权限、搜索重放设备、连接、自动采集、Room 写入、第二页展示和归因页 PIAS 展示。
- 重放设备显示名称为 `MR11 真实抓包重放（非实时测量）`，来源为 `mrd_capture_replay`；页面明确提示这不是实时人体测量。
- 第二页保留原 Demo 编排，显示厂商 SDK 解析值：心率 70 bpm、HRV 34 ms、血氧 99.5%（UI 取整为 99%）、血压 107/69 mmHg、步数 165；没有温度抓包，因此体温保持空状态；空睡眠包不会伪造睡眠时长。
- 归因页保留原卡片、周期选择和交互，PIAS 返回 `ready`、当前风险 0.501、趋势与 30 天两条预测曲线。风险主卡因未配置 Jeecg Token 正确显示未登录状态，没有用 Mock 风险补位。

### 厂商 SDK 文档复核

复核文件：`Ring/Android-SDK-MRD-1.3.0/曼瑞德戒指SDK文档(Android)_zh_en.pdf`，50 页，版本 1.3.0。

- 第 2 页：初始化与 I/O 顺序为 `Manridy.init(context)`、`Manridy.getMrdSend().xxx(...).getDatas()`、`Manridy.getMrdRead().read(cmd)`；当前实现一致。
- 第 30-37 页：解析模型及字段与当前 Mapper 一致，包括 `HeartModel.heartRate`、`HRVModel.hrv`、`BoModel.boRate`、`BpModel.bpHp/bpLp/bpHr`、`SleepModel`、`TempModel.userTemp`、`stepNum/stepMileage/stepCalorie/stepTime`。
- 厂商 `SportBean.stepDate` 的日期格式不能由默认 Gson 稳定解析，当前实现使用仅包含文档字段的内部 payload 映射，避免已识别的 `Step_realTime` 丢失。
- 仪器测试将真实 MR11 抓包逐包送入厂商 `Manridy.getMrdRead().read(...)`，验证模型类型、数值与 Room 映射，而不是手工伪造 SDK 返回对象。

### 既有真实后端能力审查

只读审查了 `D:\rehealthAI\Android-apk` 当前代码和以下既有分支能力：

- `origin/work/D_android_network_feature_evaluate`
- `origin/work/P0b_android_canonical_risk_ui_path`
- `origin/agent/rehealth-patient-mvp`
- 当前代码树中的 D3 auth/typed feedback/WorkManager 实现

确认来源树已有 `SessionStore`、`AuthenticatedApiClient`、`ReHealthMobileApi`、CVD16 Mapper、风险历史、离线队列、typed intervention feedback 和 `MeasurementSyncWorker`。来源树 `testDebugUnitTest` 在临时指定 `D:\Android_SDK` 后通过。

本轮没有整包迁移这些类，原因不是重复实现，而是它们依赖真实 `POST /sys/mLogin` 会话、用户 ID、Room v4 队列和反馈 UI 入口；当前 Demo 登录页是手机号/验证码视觉流程，强行接线会要求修改 UI 或把本地登录冒充真实后端登录。当前分支继续复用已经可验证的真实 `/rehealth/mobile/features/evaluate` 与 PIAS 客户端；登录/反馈队列只在现有 UI 有正确契约入口后再接入。

未迁移的来源风险：来源树未提交 BLE diff 仍有设备地址/广播内容日志；D3 Worker 会记录 feedback/intervention 标识；`RemotePhmService` 仍支持 Mock fallback。这些不符合当前生产日志与 Mock 隔离要求。

### 服务与构建证据

- JeecgBoot `http://localhost:8080/jeecg-boot/`：HTTP 200。
- model-service `http://localhost:8090/health`：`scorer_mode=real_available`，`model_available=true`，模型版本 `cvd-core16-catboost-20260710T173543Z`，工件 `rehealth_cvd_catboost.pkl`。
- PIAS `http://localhost:8200/health`：`healthy`；Android 使用 `http://10.0.2.2:8200/api/pias/v2` 访问。
- `lintDebug testDebugUnitTest assembleDebug assembleRelease connectedDebugAndroidTest`：成功，耗时 1 分 40 秒。
- 单元测试：PIAS 请求字段与 ready 响应解析通过。
- 仪器测试：API 31 AVD 上 1/1 通过。
- APK：`D:\rehealth_demo\Android-apk\app\build\outputs\apk\debug\app-debug.apk`，29,667,305 字节。

### 尚未验证

- 没有实体 MRD 戒指，因此真实射频扫描、GATT 服务发现、通知时序、断线重连和人体测量仍需真机验证。
- 本轮是厂商 SDK + 真实 MR11 抓包的软件重放验证，不是当前用户的生理测量。
- 未配置有效 `REHEALTH_API_TOKEN`，所以本轮没有在模拟器内执行受保护的 Jeecg 风险评估；后端、model-service 和 PIAS 服务健康已分别验证。
