# ReHealth AI Android

睿禾精灵 Android 客户端，负责 MRD/RWFit 戒指与 HBand 手表/手环采集、本地持久化、轻量健康
特征提取、离线上传和用户交互。CatBoost、SHAP、LLM 和生产归因均位于云端，
不进入 Android APK。

仓库级架构、数据边界和文档同步规则见根目录 `README.md`。

## 当前能力

- Compose 登录、注册、健康访谈、设备绑定、主页、数据、风险、干预、反馈、归因和健康助手页面。
- MRD SDK/协议适配，以及固定版本 RWFit、HBand 官方 SDK Provider。
- 基于 `productCode` 的单一有效设备路由；Release 注册 MRD/RWFit/HBand，Debug 另可
  注册 Mock 或通过 Gradle 属性生成指定厂商真机测试 APK。
- 心率、血氧、血压、体温、睡眠、步数和活动等本地记录。
- Room 本地优先持久化及显式数据库迁移。
- Foreground Service 后台低频采集与 WorkManager 恢复任务。
- 认证感知的 durable upload queue；401 时暂停，重新登录后恢复。
- 遥测批量上传、设备绑定、访谈、CVD 16 特征评估和 typed intervention feedback。
- Debug 环境可连接本机 JeecgBoot，Release 环境强制 HTTPS 后端地址。

## 主要目录

```text
app/src/main/java/com/rehealth/genie/
├─ ring/            可穿戴领域、Repository、BLE 守卫与 MRD/RWFit/HBand 适配
├─ ring/provider/   单一有效绑定、商品目录、Provider 懒加载与路由
├─ ring/data/       Room 遥测实体和 DAO
├─ service/         RingForegroundService
├─ work/            采集恢复和上传 WorkManager
├─ data/sync/       上传队列、云端映射和反馈同步
├─ features/        CVD 16 维特征与质量信息
├─ network/         会话、认证客户端、API 和 DTO
├─ phm/             风险/干预远程服务抽象与显式失败状态
└─ ui/              Compose UI
```

厂商 SDK 位于：

```text
app/libs/sdk_mrd2026_1.3.0.aar
app/libs/blesdk-rwfit-release_v2_260724.aar
app/libs/vpbluetooth-1.20.aar
app/libs/vpprotocol-2.3.73.15.aar
app/libs/jl_bt_ota_V1.10.0_10931-release.aar
app/libs/jl_rcsp_V0.7.2_527-release.aar
app/libs/JL_Watch_V1.13.1_11214-release.aar
```

三个 JieLi AAR 仅满足 HBand 核心 SDK 的连接/认证及管理器初始化依赖；应用不提供 OTA、
表盘或消息控制入口。
HBand SDK 还会在 BLE 连接回调中初始化 Nordic OTA 适配器，因此固定引入官方要求的
`mcumgr-core:2.7.4`、`mcumgr-ble:2.7.4` 和 `scanner:1.4.2`；应用仍不提供 OTA 入口。

## 核心数据流

```text
productCode -> ActiveRingRepository -> MRD BLE / RWFit SDK / HBand SDK
  -> RingRepository
  -> Room
  -> UploadQueue
  -> MeasurementSyncWorker
  -> JeecgBoot / Device Service
```

采集必须先写 Room，网络请求不得阻塞 BLE。遥测上传不直接触发模型评分；
CVD 评估通过独立的 feature-evaluate 路径完成。

正式 Android/Backend 契约：

- `docs/REHEALTH_INTEGRATION_CONTRACT.md`
- `docs/D2_TELEMETRY_SYNC_PLAN.md`
- `docs/FEATURE_EXTRACTOR.md`
- `docs/wearable/SDK_BASELINE.md`（厂商 SDK、采购型号与能力证据基线）
- `docs/wearable/RWFIT_DEVICE_QA.md`（RWFit 真机测试步骤与证据清单）
- `docs/wearable/HBAND_DEVICE_QA.md`（HBand 待设备真机测试步骤与证据清单）

## 配置

Debug 默认后端：

```text
http://10.0.2.2:8080/jeecg-boot/
```

可在未跟踪的 `local.properties` 中配置：

```properties
rehealth.api.base.url=http://10.0.2.2:8080/jeecg-boot/
rehealth.release.api.base.url=https://api.example.com/jeecg-boot/
```

Release 的后端地址必须使用 HTTPS。模型 Provider 凭据、内部服务 token 和生产
secret 禁止进入 `local.properties`、BuildConfig 或 APK。

模拟戒指只存在于 `app/src/debug`，由 Debug 专用工厂和
`USE_FAKE_RING`/`SEED_FAKE_HEALTH_DATA` 控制。`app/src/release` 的工厂只构造
真实 MRD/RWFit/HBand Provider；远程风险评估失败时显示不可用，不生成本地模拟风险。

当前有效设备绑定保存在 `EncryptedSharedPreferences`，不进入 Room。设备首次
扫描连接成功后才保存绑定地址；没有绑定地址时，后台采集不会使用固定地址或
自动扫描连接。
HBand 恢复连接所需的真实性别、年龄、身高和体重也只保存在该加密存储中，键按
登录 `userId` 的 SHA-256 前缀隔离；不保存到 Room、不记录日志、不上传给新增后端。

## 构建与测试

需要 JDK 17、Android SDK 36、Build Tools 36.0.0、Gradle 8.11.1、AGP 8.10.1
和 Kotlin 2.2.20。Kotlin/KSP/R8 版本与 HBand 固定的 Nordic MCU Manager 2.7.4
元数据保持兼容。

Gradle 会优先从 Maven 本地仓库解析插件和项目依赖，再回退到 Google Maven、
Maven Central 和 Gradle Plugin Portal。未覆盖 Maven 配置时，本地仓库路径为
`%USERPROFILE%\.m2\repository`；未设置 `GRADLE_USER_HOME` 时，Gradle 用户目录为
`%USERPROFILE%\.gradle`，下载的依赖缓存位于其 `caches\modules-2\files-2.1` 子目录。
本地仓库中与远程仓库同坐标的制品会被优先使用，发布或排查依赖问题时应确认其来源和版本。

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```

生成强制选择 RWFit、并保留重启后绑定重连能力的真机测试 APK：

```powershell
.\gradlew.bat "-Prehealth.debug.wearable.product.code=RH-RW-P01" testDebugUnitTest assembleDebug
```

使用 Android Studio 的 Run 按钮进行 RWFit 真机调试时，在不提交版本库的
`local.properties` 中加入：

```properties
rehealth.debug.wearable.product.code=RH-RW-P01
```

命令行 `-Prehealth.debug.wearable.product.code=...` 会覆盖本地配置；两者都未设置时
Debug 默认使用 MRD。切换配置后需重新构建并安装应用。

Debug 的“设备绑定”页也可在确认对话框后切换本地商品目录中的 `productCode`。
切换会暂停采集、断开旧 Provider、清空旧绑定并保留全部 Room 历史，再恢复原先
启用的采集任务。Release 不显示该入口，套餐仍由受信任的产品配置决定。

HBand 真机联调可生成强制选择 `RH-HB-E01` 的专用 APK：

```powershell
.\gradlew.bat "-Prehealth.debug.wearable.product.code=RH-HB-E01" testDebugUnitTest assembleDebug
```

连接前必须从真实用户档案取得性别、年龄、身高和体重。当前 HBand 商品能力开放
心率、步数/活动、睡眠、血压和 ECG；运行时仍与设备的 `FunctionDeviceSupportData`
取交集，不支持的能力不会显示、调用或生成占位数据。ECG 波形只写入本地 Room，
不会进入遥测上传批次；血压与 ECG 结果仅用于健康记录，不作诊断解释。

Debug APK：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 当前限制

- 已有 MRD/RWFit/HBand 单一有效设备路由；RWFit 真机型号/固件、HRV 单位、数据准确性
  和后台稳定性仍待验证；HBand 已开始真机联调，连接所需的 JieLi/Nordic 运行时依赖已补齐，
  已实现能力门控的心率、步数/活动、睡眠、血压和 ECG 接入，仍需使用完整重装 APK
  验证采购设备实际能力、测量准确性、扫描、认证、画像同步、历史读取与后台稳定性；
  不支持多设备同时连接或数据融合。
- 本地遥测和上传队列仍需进一步按登录用户和设备维度隔离。
- 遥测上传仍需从“最新快照”演进到按本地游标处理全部未上传记录。
- MRD 扫描、重连、锁屏长时间采集、功耗和测量准确性仍需物理设备 QA。
- 原始信号云端上传默认关闭；后续启用必须增加用户同意、加密和保留策略。

## 文档同步

以下变化必须同步本 README 及对应专项文档：

- 新设备、厂商 SDK、BLE 协议、指标或采集行为；
- Room Schema、上传队列、重试和持久化完成语义；
- API、认证、DTO、BuildConfig、权限或 Release 地址；
- 用户可见流程、硬件 QA 步骤或隐私规则。
