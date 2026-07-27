# ReHealth AI Android

睿禾精灵 Android 客户端，负责 MRD 戒指 BLE 采集、本地持久化、轻量健康
特征提取、离线上传和用户交互。CatBoost、SHAP、LLM 和生产归因均位于云端，
不进入 Android APK。

仓库级架构、数据边界和文档同步规则见根目录 `README.md`。

## 当前能力

- Compose 登录、注册、健康访谈、设备绑定、主页、数据、风险、干预、反馈、归因和健康助手页面。
- MRD SDK `sdk_mrd2026_1.3.0.aar` 与 BLE 协议适配。
- 心率、血氧、血压、体温、睡眠、步数和活动等本地记录。
- Room 本地优先持久化及显式数据库迁移。
- Foreground Service 后台低频采集与 WorkManager 恢复任务。
- 认证感知的 durable upload queue；401 时暂停，重新登录后恢复。
- 遥测批量上传、设备绑定、访谈、CVD 16 特征评估和 typed intervention feedback。
- Debug 环境可连接本机 JeecgBoot，Release 环境强制 HTTPS 后端地址。

## 主要目录

```text
app/src/main/java/com/rehealth/genie/
├─ ring/            戒指领域、Repository、BLE 守卫与 MRD/vendor 适配
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
```

## 核心数据流

```text
MRD SDK / BLE
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
`MrdBleRingRepository`；远程风险评估失败时显示不可用，不生成本地模拟风险。

## 构建与测试

需要 JDK 17、Android SDK 36 和 Build Tools 36.0.0。

```powershell
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
.\gradlew.bat assembleRelease
```

Debug APK：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 当前限制

- 多设备领域仍以 `RingRepository` 为中心，尚未完成通用 Device Adapter 插件化。
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
