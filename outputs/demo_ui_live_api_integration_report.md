# Demo UI Live API Integration Report

更新时间：2026-07-22

## 1. 最终结论

已在 `D:\rehealth_demo\Android-apk` 的 `codex/real-device` 分支上完成整合，保留该分支的 Demo Compose 页面编排、五页底部导航、第二页仪表盘结构和归因页卡片/展开交互；未用真实能力来源仓库的重写 UI 覆盖它们。

已接入并用于正式运行路径的能力：

- MRD BLE 扫描、GATT 连接、服务发现、通知订阅、命令写入和 Room 落库。
- Android 12+ `BLUETOOTH_SCAN` / `BLUETOOTH_CONNECT` 权限申请、永久拒绝跳系统设置，以及权限撤销竞态保护。
- Room 中真实测量、睡眠、活动历史数据的 1/7/30/90 天聚合展示。
- `POST /rehealth/mobile/features/evaluate` 风险评估请求、`X-Access-Token` 鉴权、snake/camel 响应解析和网络/401/403/5xx/空结果状态。
- 归因页真实风险分和真实特征贡献映射；PIAS 可用时展示其真实返回的预测曲线，饮食记录和干预计划在后端未返回时保持空状态，不再展示固定 Demo 数据。

尚未完成或无法在当前环境确认：实体 MRD 戒指的真实 GATT 服务发现、通知数据解析和多轮测量；生产登录仍需将 Demo 手机验证码流程接入 Jeecg 会话，而不是构建时 Token；健康采访仍使用明确命名的 `MockHealthInterviewModel`，其自然语言回答尚未结构化映射到 CVD16。

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
- 本轮上线前链路修复提交：`c370b12` (`fix(android): validate live risk and animate PIAS curves`)

比对报告中曾将本地真实能力工作树称为另一个 `work/D3...` 分支；实际本次按 Git 和文件内容核对后，最终工作区是 `D:\rehealth_demo\Android-apk` 的 `codex/real-device`，真实能力参考为 `D:\rehealthAI\Android-apk`。两者 UI 不能混用。

## 3. 修改文件清单

修改：

- `app/build.gradle.kts`：增加 API Base URL/Token 的 `local.properties`、环境变量和 BuildConfig 配置；增加 Retrofit/Moshi/OkHttp 测试依赖。
- `app/src/main/AndroidManifest.xml`：补齐 Android 12+ BLE 权限声明，并为扫描权限声明 `neverForLocation`。
- `app/src/main/java/com/rehealth/genie/ring/RingViewModel.kt`：消费 Room 历史流；阻止未连接设备时自动采集；补充风险请求异常状态和用户提示。
- `app/src/main/java/com/rehealth/genie/ring/data/RingDataDao.kt`：增加近期测量、睡眠、活动历史流查询。
- `app/src/main/java/com/rehealth/genie/ring/mrd/MrdBleRingRepository.kt`：扫描/连接/GATT/通知/写入/关闭前权限检查；移除生产日志中的设备地址和原始健康 payload。
- `app/src/main/java/com/rehealth/genie/ui/ReHealthApp.kt`：恢复并保留完整 Demo Compose UI；第二页改为 Room 时间窗口聚合；归因页和模型页接入真实风险状态、空状态和刷新；移除固定风险曲线、午餐、干预和个人统计展示；设备页补全权限状态机。

2026-07-22 上线前复核补充修改：

- `app/src/main/java/com/rehealth/genie/live/LiveRiskRepository.kt`：缺失特征的 `FeatureSource` 从协议不支持的 `MISSING` 修正为 `UNKNOWN`；有有效步数观察但未达阈值时发送真实派生的 `exercise_days=0`。
- `app/src/main/java/com/rehealth/genie/ring/RingViewModel.kt`：PIAS 抓包验证历史末点锚定本次真实后端风险分，同时保留 7 天对照/7 天干预与“验证历史重放”标识。
- `app/src/main/java/com/rehealth/genie/ui/ReHealthApp.kt`：仅在既有 `MiniChart` 内增加 1 秒逐段描线动画；不改变归因页布局、卡片、导航或交互。
- `app/src/test/java/com/rehealth/genie/live/LiveRiskFeatureMapperTest.kt`：锁定真实抓包血压、步数和 model-service 质量来源契约。
- `app/src/test/java/com/rehealth/genie/ring/ReplayPiasHistoryTest.kt`：锁定 14 天验证历史的真实风险末点与干预/对照分组。

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
- 后端地址和 Token 需要通过未提交的 `local.properties`、环境变量或后续正式登录会话配置；不得提交密钥。
- 真实干预计划和饮食记录 API 尚未接入；风险预测曲线已接真实 PIAS，页面保留原 Demo 编排。
- `HealthInterviewFlow` 仍使用 `MockHealthInterviewModel`，需要后续接入真实健康档案服务。
- 旧的 `MockRingRepository`、`MockPhmService` 文件保留给 Preview/测试隔离，正式 `ReHealthApplication` 使用 `MrdBleRingRepository`。

## 8. 2026-07-22 MRD SDK、PIAS 与既有后端能力复核

### 本轮结论

- 目标仓库仍为 `D:\rehealth_demo\Android-apk`，分支仍为 `codex/real-device`；没有创建新分支，也没有将其他分支的 Compose UI 覆盖进来。
- Debug 构建可通过显式开关使用 `CapturedMrdReplayRepository`；Release 构建固定装配 `MrdBleRingRepository`，抓包重放代码不进入正式运行路径。
- API 31 模拟器已完成：访客 onboarding、BLE 权限、搜索重放设备、连接、自动采集、Room 写入、第二页展示和归因页 PIAS 展示。
- 重放设备显示名称为 `MR11 真实抓包重放（非实时测量）`，来源为 `mrd_capture_replay`；页面明确提示这不是实时人体测量。
- 第二页保留原 Demo 编排，显示厂商 SDK 解析值：心率 70 bpm、HRV 34 ms、血氧 99.5%（UI 取整为 99%）、血压 107/69 mmHg、步数 165；没有温度抓包，因此体温保持空状态；空睡眠包不会伪造睡眠时长。
- 归因页保留原卡片、周期选择和交互；本轮配置一次性 Jeecg 测试会话后，真实风险主卡返回 2.38%，PIAS 返回 `ready`、当前风险约 2.4% 以及 30 天两条预测曲线，没有用 Mock 风险补位。

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

### 2026-07-22 上线前端到端实测

- 使用 `POST /sys/mLogin` 的本地测试账户取得一次性 JWT；Token 只注入 Gradle 进程和模拟器联调 APK，不写入源码、报告或 `local.properties`。
- API 31 模拟器完整走通：Demo 登录/8 步健康初识 → BLE 权限 → 搜索并连接 MR11 抓包重放设备 → 厂商 SDK 解析 → Room 写入 6 条 → 第二页展示 → Jeecg `/features/evaluate` → 真实 CatBoost/SHAP → 归因页 → 真实 PIAS。
- 后端返回 HTTP 200：`riskScore=0.0238`、`riskLevel=low`、16 项 `featureContributions`、`isMock=false`，模型版本 `cvd-core16-catboost-20260710T173543Z`，贡献方法 `shap_via_catboost`。
- 第二页实测：心率 70、血氧 99、血压 107/69；体温保持 `--`；SDK 验证还覆盖 HRV 34、步数 165；空睡眠包不伪造记录。
- PIAS 返回 `ready`；14 天显式验证历史的末点锚定真实后端风险，页面报告当前风险 2.4%、30 天计划差异 0.1%，并持续显示“真实 MR11 抓包 + 验证历史重放，不是实时人体测量”。
- 归因页显示真实 CatBoost 风险、16 项 SHAP 贡献；两条 PIAS 预测曲线来自服务端 `no_action`/`with_plan` 数组，由原 `MiniChart` 在 1 秒内逐段描线。
- 动画证据：`D:\rehealthAI\outputs\attribution-animation-start.png`、`attribution-animation-mid.png`、`attribution-animation-end.png`，以及 `pias-animation-start.png`、`pias-animation-mid.png`、`pias-animation-end.png`。

### 最终构建门禁

| 命令 | 结果 |
|---|---|
| `.\gradlew.bat lintDebug testDebugUnitTest assembleDebug assembleRelease connectedDebugAndroidTest` | 成功；133 tasks，API 31 仪器测试 1/1 |
| `git diff --check` | 成功；只有工作区 LF/CRLF 提示，无 whitespace error |
| Release BuildConfig 审计 | `REHEALTH_CAPTURE_REPLAY=false`，`REHEALTH_API_TOKEN=""` |

- Debug APK：`D:\rehealth_demo\Android-apk\app\build\outputs\apk\debug\app-debug.apk`，29,667,305 字节。
- Release APK：`D:\rehealth_demo\Android-apk\app\build\outputs\apk\release\app-release-unsigned.apk`，13,271,604 字节；未签名，不能作为商店发布包。

### 尚未验证 / 发布阻塞

- 没有实体 MRD 戒指，因此真实射频扫描、GATT 服务发现、通知时序、断线重连和人体测量仍需真机验证。
- 本轮是厂商 SDK + 真实 MR11 抓包的软件重放验证，不是当前用户的生理测量。
- 物理 MR11 戒指的射频、GATT、通知时序、断线重连、长时间采集和人体读数仍未验证；抓包重放不能替代这些真机门禁。
- 当前 Demo 手机号/验证码登录仍未换成正式 Jeecg 登录/401 重新登录流程；本轮一次性构建 Token 只能用于本机联调，不能上线。
- 健康初识回答只保存自然语言摘要，尚未可靠结构化到年龄、BMI、病史等 CVD16 字段；真实模型本轮只有 SBP/DBP/exercise_days 来自抓包，其余字段由模型缺失值管线处理，不能解释为完整个人健康画像。
- PIAS 需要至少 14 天风险与干预/对照历史；本轮只有末点来自真实 MR11→CatBoost，之前 13 天是明确标注的验证历史，因此不能作为真实因果干预结论。

## 9. 2026-07-23 健康档案与访谈云同步补齐

- 当前权威仓库为 `D:\rehealthAI`，分支为 `codex/real-device`；本节覆盖并纠正报告中仍指向 `D:\rehealth_demo\Android-apk` 的历史路径说明。
- 后端新增认证用户隔离的 `GET/PUT /rehealth/mobile/profile`、`POST /rehealth/mobile/interviews`、`GET /rehealth/mobile/interviews/latest`，复用现有 `rehealth_patient_profile` 与 `rehealth_health_interview` 表。
- Android 健康初识页面的布局、动画、问题编排和导航未改变；完成访谈时先保存本地摘要，再把类型化回答和基线写入 Room 上传队列，由 WorkManager 在有网且登录有效时同步。
- software_db 未启用时，后端返回可重试的 `503` 业务响应，Android 不会把未持久化访谈误标为成功。
- H2 聚焦测试通过：4 项，覆盖档案 upsert、访谈持久化、用户隔离和路由契约。
- Android 聚焦单测通过：访谈 DTO 路由、队列上传、基线映射。
- Android 完整门禁通过：`testDebugUnitTest`、`lintDebug`、`assembleDebug`、`assembleRelease`；Debug APK 为 `D:\rehealthAI\Android-apk\app\build\outputs\apk\debug\app-debug.apk`（21,420,227 字节，SHA-256 `fea219584d1f0b5c8a19cca4d2e5fff28399466c2d94c4b18da87d0f088a9da0`）。
- ReHealth 后端模块完整测试通过：22 项，0 失败、0 错误。
- 官方 AVD 已按用户要求移除，等待用户安装网易 MuMu；因此本节未执行新的模拟器 UI 验证，也未声称完成真机戒指验证。

## 10. 2026-07-23 模型审计与硬件近期数据查询

- 本轮未修改 Android Compose UI。
- software_db 的 `rehealth_model_request_log` 已接入风险评估、干预生成和归因调用，只保存请求 ID、操作、模型版本、结果与时间；不保存请求体、Token、手机号或原始健康数据。
- 新增认证接口 `GET /rehealth/mobile/measurements/recent?limit=50`，从独立 hardware_db 返回当前登录用户的近期标准化测量、睡眠和活动记录；限制范围为 1–200，不返回 PPG/RRI 或其他原始信号载荷。
- hardware_db 未启用时返回可重试 `503`，不会回退为 Mock；查询测试验证了跨用户隔离和按时间倒序限量。
- 聚焦 Maven 测试 6 项通过，覆盖审计落库、近期数据查询和控制器接口契约；ReHealth 后端完整测试共 25 项通过，0 失败、0 错误。
- Android 回归门禁 `testDebugUnitTest lintDebug assembleDebug assembleRelease` 通过；本轮没有 Android 源码或 UI 修改。
- 仍待完成：网易 MuMu UI 回归、物理 MR11 长时间采集，以及 Jeecg 低代码管理页。低代码生成还需要确定可连接的 Jeecg 数据库/API 与前端工程目标，不能靠猜测生成到错误环境。

## 11. 2026-07-23 G3 发布隐私与日志门禁

- Android Release 已启用 R8，产物不可调试、禁止明文流量和备份；本机密钥与模拟器后端 URL 未进入 APK。
- Release 不再携带 DeepSeek provider key 或 Jeecg 客户端签名密钥；真实后端地址必须通过 `REHEALTH_RELEASE_API_BASE_URL` 配置且只能使用 HTTPS。
- 移除了 MRD BLE 原始帧与反馈记录标识日志；Release 字节码中的对应日志调用为零。
- JeecgBoot `application-prod.yml` 的 18 个密码、Token、API Key 和 OAuth Secret 字段已改为环境变量或空默认值；打包 JAR 内硬编码凭据字段为零。
- Android `testDebugUnitTest lintDebug assembleDebug assembleRelease` 与 JeecgBoot 11 模块 Reactor package 均成功。
- 详细证据：`codex-runs/2026-07-20/G3_privacy_audit_report.md`。
- 仍待：配置真实 HTTPS 域名、签名 Release APK、MuMu 运行时 logcat、物理 MR11 验证；静态门禁通过不等同于生产部署完成。

## 12. 2026-07-23 MySQL 8 双库与模型链路实测

- staging 使用 MySQL `8.0.42`，加法升级脚本 `V20260723_2__upgrade_legacy_software_schema.sql` 在旧 software_db schema 上重复执行验证通过，并在真实 staging 应用；未做破坏性重建。
- 修复 Dynamic Routing DataSource 与独立 hardware JdbcTemplate 的 Bean 冲突；software repository 明确使用主路由 JdbcTemplate，hardware writer/query 使用名为 `hardware` 的子数据源。
- 修复 Java `HttpClient` 默认 h2c 升级导致 Uvicorn POST 返回 422：模型客户端固定使用 HTTP/1.1。回归测试确认旧实现发送 `Upgrade: h2c`，修复后不再发送。
- 修复旧干预记录的 snake_case JSON 回读，`plan_id`、`model_version`、`is_mock` 等字段可恢复为当前 DTO，同时保持当前移动 API 的 camelCase 输出不变。
- 使用真实 `POST /sys/mLogin` 会话和 `X-Access-Token` 完成：profile、interview、device bind、MR11 规范化抓包重放批次、recent telemetry、真实 CatBoost 风险、干预、反馈与真实 PIAS 归因。
- hardware_db 核验：同一 batch 重试返回相同 receipt；落库为 1 batch、2 measurements、1 sleep、1 activity；重启后再次提交仍不重复。请求中伪造的 `userId` 落库数为 0，所有权来自认证主体。
- software_db 核验：profile/interview/device/feature/risk/feedback/attribution/model audit 均真实落库；真实 CatBoost 返回 `isMock=false`；后端重启后 latest risk、today intervention、profile、interview 均可回读。
- PIAS 以明确标记的 30 天验证历史返回 `ready` 和可用 ATT；这是软件链路验证数据，不是当前用户的因果医学结论。
- 完整 ReHealth Maven 测试：30 项通过，0 失败、0 错误；测试后已删除 QA 双库数据、临时数据库备份和本机会话凭证，不写入 Git。
- 本轮没有修改 Android Compose UI。网易 MuMu 页面回归和物理 MR11 BLE/GATT/锁屏长时间采集仍未验证。
