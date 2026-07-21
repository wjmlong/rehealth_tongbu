# 比对报告（实时版）：`codex/real-device` ↔ 当前本地工程

> 生成时间：2026-07-20 23:34
> 比对对象：私有分支 `codex/real-device`（**实时拉取**） ↔ 本地工作树（分支 `work/D3_android_auth_typed_feedback`，含未提交改动）

## 一、数据来源（本次为权威实时结果）

通过 WSL2 内 `gh`（账号 **wjmlong**，scope 含 `repo`）实时拉取私有分支：
1. `gh api` 取分支 tree（递归，共 **285** 条目）；
2. 逐文件 `gh api contents` 拉取 **132** 个源码/文本文件（排除 `.aar`/图片/二进制等无需比对的产物），落地到 `/tmp/real_branch`；
3. 用 `diff -rq --strip-trailing-cr` 与本地工作树逐文件比对（忽略 build/、`.ref`、outputs、二进制、本地验证产物等噪声）。

**此结论取代此前基于 `.ref/real-device` 快照的估算**——该快照仅含 15 个 Kotlin 文件，漏掉了 `AndroidManifest.xml` 与 `ring/data/RingDataDao.kt` 两处真实差异。现在是完整、实时的结果。

## 二、总体结论（一句话）

- `codex/real-device` = **"能采、能存、本地演示"** 的设备采集早期快照：真机 MRD 戒指 BLE 采集 + 本地 Room 存储，**后端 / 账号体系 / 云端上传 / 模型推理均尚未接入，登录为本地演示流程**。
- 当前本地 = 在其上**叠加了一整层后端集成**：真实鉴权（账号密码 + 注册）、类型化移动 API（Retrofit/Moshi）、离线上传队列 + WorkManager、干预反馈闭环、远程 PHM（风险/干预/归因）、前台采集服务、富 dashboard。

## 三、文件级结果（实时）

| 维度 | 数量 | 说明 |
|---|---|---|
| **内容有差异** | **14** | 双方都有、但内容不同（见下表） |
| **仅本地新增** | **46** | 分支中不存在、本地新增的目录/文件（见第四节） |
| **仅分支独有** | **2** | `Ring/`（厂商 SDK 文档目录）、`docs/DEEPSEEK_LOCAL_CONFIG.md` |

### 14 个差异文件

| 文件 | 主题 | 核心变化 |
|---|---|---|
| `app/build.gradle.kts` | 构建/依赖 | 新增后端 BuildConfig、buildTypes 开关、OkHttp/Retrofit/Moshi/WorkManager/security-crypto |
| `app/src/main/AndroidManifest.xml` | **清单（快照漏掉）** | 新增 POST_NOTIFICATIONS / FOREGROUND_SERVICE / FOREGROUND_SERVICE_CONNECTED_DEVICE 权限；BLUETOOTH_SCAN 加 `neverForLocation`；移除 RECORD_AUDIO；去掉 icon/logo；加 `usesCleartextTraffic`；**新增 `RingForegroundService` 声明** |
| `app/src/main/java/.../ReHealthApplication.kt` | 应用装配 | 新增 SessionStore、BackendClient、MobileApi、AuthClient、Sync/Feedback 仓库、RemotePhmService、Worker 调度、fake-ring 条件分支 |
| `app/src/main/java/.../chat/DeepSeekClient.kt` | 聊天 | 默认从 BuildConfig 读 key；加错误日志 |
| `app/src/main/java/.../data/AppDatabase.kt` | 数据库 | version 2→3；新增上传队列表 + 干预反馈队列表 + Migration2To3 |
| `app/src/main/java/.../phm/MockPhmService.kt` | PHM | 实现扩展后的完整 `PhmService` 契约（全部 mock 方法） |
| `app/src/main/java/.../ring/MockRingRepository.kt` | 戒指 Mock | 种子化 6 天基线、可复现随机值、quality/rawPayload、autoConnect/sendCommand |
| `app/src/main/java/.../ring/RingRepository.kt` | 接口 | 新增 `autoConnect()`、`sendCommand()` |
| `app/src/main/java/.../ring/RingViewModel.kt` | 采集+云 | 每次同步后 `uploadLatestSnapshot()` 上传云端并回刷患者 MVP/风险；前台采集；默认 mock MVP |
| `app/src/main/java/.../ring/data/RingDataDao.kt` | **DAO（快照漏掉）** | 新增 `getMeasurementsSince` / `getLatestMeasurement` / `getActivitiesSince` 查询（供上传与后台恢复取数） |
| `app/src/main/java/.../ring/mrd/MrdBleRingRepository.kt` | BLE | `KNOWN_RING_ADDRESS` 常量替代 SharedPreferences；新增 autoConnect/sendCommand；权限抽到 `RingBleGuards` |
| `app/src/main/java/.../ring/mrd/MrdProtocolAdapter.kt` | 协议 | 新增一批设备设置命令桩（返回空包，待接真 vendor 命令） |
| `app/src/main/java/.../ui/LoginScreen.kt` | 登录 | 手机号+验证码演示 → 账号+密码真实登录；注册入口；错误/loading |
| `app/src/main/java/.../ui/ReHealthApp.kt` | 导航/UI | 近重写：新增 Splash/Interview/Home、富 dashboard、干预反馈卡片、RemoteFeatureEvaluate、logout 等 |

### 相比快照估算，本次新确认的两处差异
1. **`AndroidManifest.xml`**：本地新增前台服务与通知权限（支撑后台 BLE 采集）、`usesCleartextTraffic`（开发期直连 http 后端）、移除 RECORD_AUDIO、BLUETOOTH_SCAN 加 `neverForLocation`。这是"本地已支持后台常驻采集"的直接证据。
2. **`ring/data/RingDataDao.kt`**：本地新增 3 个查询方法，供云端上传与后台采集恢复时按时间窗取数。

### 仅存在于分支（2，非功能性）
- `Ring/`：`Android-SDK-MRD-1.3.0/` 厂商 SDK 文档 + README。本地以 `app/libs/*.aar` 方式集成同一 SDK，等价差异。
- `docs/DEEPSEEK_LOCAL_CONFIG.md`：DeepSeek 本地配置说明文档，本地无对应文档。

## 四、本地新增（46 个目录/文件，按包归类）

- **鉴权**：`network/{SessionStore, AuthenticatedApiClient, ReHealthMobileApi, BackendConfig, SignInterceptor}`、`network/dto/{AuthDto, LoginDto, MobileDto}`、`ui/LoginViewModel`、`ui/RegisterScreen(-ViewModel)`
- **网络 / PHM**：`network/{ReHealthBackendClient, ReHealthApi, HealthChatService, RemotePhmError}`、`phm/{RemotePhmService, PhmService, PhmModels, Mappers}`、`network/dto/{FeatureEvaluationDtos, AttributionDto}`
- **同步 / 反馈**：`data/sync/{SyncRepository, UploadQueueDao, UploadQueueEntity, InterventionFeedbackRepository, InterventionFeedbackDao, InterventionFeedbackEntity}`、`work/{MeasurementSyncWorker, RingBackgroundRecoveryWorker}`、`ui/InterventionFeedbackViewModel`、`ui/components/QueueStatusBanner`
- **特征提取**：`features/{CvdFeatureVector, CvdFeatureVectorDtoMapper, FeatureQuality, HealthFeatureExtractor, HealthMemorySnapshot}`
- **设备 / 后台 / 通知**：`ring/device/{DeviceSettings, DeviceSettingsViewModel}`、`ring/{RingBackgroundCollectionPolicy, RingBackgroundCollectionSettings, RingBleGuards, RingModels, SignalEncoding}`、`ring/vendor/VendorRingRepository`、`service/RingForegroundService`、`notification/RingNotificationChannels`
- **数据与 UI 屏幕**：`data/{RiskCacheDao, RiskCacheEntity}`、`ring/data/{RingDataDao, RingEntities}`、`ui/{AttributionReportScreen, HealthChatScreen}`、`ui/AttributionDetailScreen.kt.disabled`、`ui/DeviceSettingsScreen.kt.disabled`
- **测试**：`src/test/**` 共 4 个（`CvdFeatureVectorDtoMapperTest`、`HealthFeatureExtractorTest`、`RemotePhmServiceRemoteFailureTest`、`RingBackgroundCollectionPolicyTest`）
- **文档 / 脚本**（根与 docs）：`docs/{BLE_BACKGROUND_QA, CANONICAL_RISK_PATH, D3_AUTH_TYPED_FEEDBACK, FEATURE_EXTRACTOR, MOBILE_INTEGRATION_OPTIMIZATION_PLAN, NETWORK_FEATURE_EVALUATE, REHEALTH_INTEGRATION_CONTRACT, WSL2_CONNECTIVITY}.md`、`install-adb-wsl.sh`、`install-apk-wsl.sh`、`quick-install.sh`、`start-emulator.{bat,sh}`、`TEST_REPORT.md`、`codex-runs/`
- **本地保护文件**：`app/libs/`（SDK aar）、`app/proguard-rules.pro`、`app/src/main/res/drawable-nodpi`

> 注：根目录 `current-my.xml` 两边一致（未列入差异）。

## 五、逐能力差异（要点，含实时新增项）

1. **鉴权（全新）**：手机号+验证码演示 → 账号+密码真实登录，接 `LoginViewModel`/`SessionStore`/`AuthenticatedApiClient`/`ReHealthMobileApi`/`SignInterceptor` + 注册页。
2. **云端上传（全新）**：`RingViewModel.uploadLatestSnapshot()` 每次采集后上传；`SyncRepository` + Room 队列表 + `MeasurementSyncWorker`(WorkManager) 离线重试；401 暂停队列。
3. **后台常驻采集（全新，清单证实）**：`AndroidManifest.xml` 新增 `RingForegroundService`（`foregroundServiceType="connectedDevice"`）+ 通知/前台服务权限；`ReHealthApplication.onCreate` 调度 `RingBackgroundRecoveryWorker`；`RingDataDao` 新增按时间窗取数供恢复/上传。
4. **远程 PHM（全新+改造）**：`PhmService` 扩为完整类型化契约；新增 `RemotePhmService`（mock 兜底）+ `Mappers` + `PhmModels`。
5. **干预反馈闭环（全新）**：`InterventionFeedbackRepository` + 队列表 + `FeedbackButton`/`InterventionCard` UI。
6. **特征提取（全新）**：`features/` 把原始戒指数据提成特征向量，对接 `/features/evaluate`。
7. **UI / 导航**：`ReHealthApp.kt` 近重写（删 234 / 增 1515 行），新增 Splash/Interview/Home、富 dashboard（风险/健康分/图表）、Attribution、logout 等。
8. **BLE**：`KNOWN_RING_ADDRESS` 常量替代 SharedPreferences；`autoConnect`/`sendCommand`；权限抽到 `RingBleGuards`；`MockRingRepository` 种子化基线。

## 六、需要注意的点

1. **README 未同步**：本地 `README.md` 仍是 real-device 描述（"未接入云端"），与代码现状不符，建议更新。
2. **BLE 设备名乱码（确属本地）**：`MrdBleRingRepository.kt` 第 1341 行（syncAll 重连处）为 `MR11 鏅鸿兘鎴掓寚`（应为"MR11 智能戒指"）；其余处已修正。属编码问题，建议修正。
3. **比对基准**：本次为 `codex/real-device` 实时内容；本地为 `work/D3_android_auth_typed_feedback` 工作树（含未提交改动）。若远端分支此后继续演进，需重跑。

---
- 文件级差异明细：`outputs/realdevice_live_diff.txt`
- 14 个差异文件的逐行 diff：`outputs/realdevice_live_content.diff`
