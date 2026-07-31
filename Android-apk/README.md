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
- 心率、HRV、血氧、血压、血糖、压力、MET、ECG、睡眠、步数、活动、血液成分和身体成分等本地记录与数据卡片；能力门控的血糖校准与经期设置。
- Room 本地优先持久化及显式数据库迁移。
- Room v8 本地 RDI 每日快照、逐因素证据、可信度收缩和平滑算法；归因页“健康改善得分”读取该结果。
- Foreground Service 后台低频采集与 WorkManager 恢复任务。
- 认证感知的 durable upload queue；401 时暂停，重新登录后恢复。
- 遥测批量上传、设备绑定、访谈、CVD 16 特征评估和 typed intervention feedback。
- 已加入隔离的 RHI v2 32 维 DTO 与 CVD-16 保守迁移映射，便于后续接入；
  当前未声明后端路由、未切换首页，研究预览不会替代生产风险结果。
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
├─ features/        CVD 16 特征、质量信息与 RHI v2 草稿迁移
├─ rdi/             本地 RDI 透明规则、Room 快照与贡献证据
├─ network/         会话、认证客户端、API、v1 DTO 与未接线的 v2 DTO
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
app/src/main/jniLibs/{arm64-v8a,armeabi-v7a,x86,x86_64}/libnative-lib.so
```

三个 JieLi AAR 仅满足 HBand 核心 SDK 的连接/认证及管理器初始化依赖；应用不提供 OTA、
表盘或消息控制入口。
HBand SDK 还会在 BLE 连接回调中初始化 Nordic OTA 适配器，因此固定引入官方要求的
`mcumgr-core:2.7.4`、`mcumgr-ble:2.7.4` 和 `scanner:1.4.2`；应用仍不提供 OTA 入口。
HBand ECG 算法还会通过 JNI 加载 `libnative-lib.so`；四个 ABI 的文件均来自与
`vpprotocol-2.3.73.15.aar` 相同的官方固定提交，不能与其他 SDK 版本混用。

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

Debug 默认后端（已提交到 `gradle.properties`，内测环境）：

```text
https://rehealth.youngjimmy.store/jeecg-boot/
```

可在未跟踪的 `local.properties` 中覆盖（优先级高于 `gradle.properties` 与环境变量）：

```properties
rehealth.api.base.url=https://rehealth.youngjimmy.store/jeecg-boot/
rehealth.release.api.base.url=https://rehealth.youngjimmy.store/jeecg-boot/
```

无蓝牙的真机 QA（模拟器 / MuMu）可用 fake-ring 通道替掉 BLE 采集：

```bash
./gradlew.bat assembleDebug -Prehealth.debug.use.fake.ring=true
```

该开关默认关闭，不影响真机 BLE 采集 QA；仅 `MockRingRepository` 合成数据走上传→
`rehealth/mobile/features/evaluate` 链路，后端与 model-service 仍是真实调用。

Release 的后端地址必须使用 HTTPS。模型 Provider 凭据、内部服务 token 和生产
secret 禁止进入 `local.properties`、BuildConfig 或 APK。

Debug 注册请求会使用 JeecgBoot 的开发签名默认值为 `/sys/sms` 增加 `X-Sign` 和
`X-Timestamp`；可通过 `local.properties` 的 `JEECG_SIGNATURE_SECRET` 或同名环境变量
覆盖。仅当后端使用 `JEECG_SMS_DEV_MODE=true` 时，验证码接口保存固定测试码 `123456`，
Android 在请求成功后自动填入该值。Release 的签名字段和测试码均为空，生产环境继续
由后端随机生成验证码并调用真实短信 Provider。

进入主界面和打开“我的”页时，客户端会按当前登录用户重新读取
`GET /rehealth/mobile/profile` 与 `GET /rehealth/mobile/interviews/latest`。个人资料、
最近健康问答画像和关注方向的读取不依赖风险模型或干预接口可用；退出登录会立即清除
内存中的上一位用户资料。健康问答点击完成后，必须先成功写入 Room durable queue 才能离开页面，
随后直接进入首页，再由 WorkManager 写入 `software_db` 的类型化访谈表；设备绑定不再阻塞
首次使用流程，可从“我的 > 设备绑定”按需进入。不再另存一份无人读取的偏好摘要。

健康问答语音入口声明并按需申请 `RECORD_AUDIO`。点击麦克风时先解释用途和“不保存录音”，
用户确认后才显示系统授权；拒绝后可转到应用设置，也可继续使用文字回答。

首页健康助手与健康问答页复用同一个 `HealthChatViewModel` 和按用户隔离的 Room 会话流，
不再维护临时单轮回复。首页麦克风调用系统语音识别服务并只把识别结果回填输入框，用户确认后
才发送，不再跳转健康初识。服务端 AI 回复使用受限 Markdown 子集渲染；原始 HTML 不执行，
远程图片不加载，链接目标不自动打开。

Room v7 新增 `health_chat_conversations`，从 v6 消息无损生成会话标题、更新时间和当前会话。
首页支持本机会话列表、新建、切换以及经确认的删除/清空；删除使用本地墓碑阻止“最新会话”刷新
立即恢复，但只影响本机缓存。当前后端只提供最新会话读取，没有列表/删除契约，因此云端
`software_db` 完整历史不会随本机删除而删除。

Room v8 新增 `rdi_daily_snapshots` 与 `rdi_contribution_records`。`7→8`
显式迁移只建表和索引，不删除既有健康、设备、队列、风险或健康问答数据。
`rdi-rule-1.0.0` 以 50 为中性值，从近 7/28 日活动、睡眠和满足同设备门槛的
HRV 生成本地近期可干预负荷；每项贡献乘数据可信度，展示值按 `0.30/0.70`
平滑并限制普通单日最多变化 3 分。数据不足时向中性收缩或保持上一展示值，
不会把缺失当正常，也不会纳入消费级无袖带血压或伪造血检值。
其中 6000 步/150 分钟和成人 7–9 小时睡眠只作为产品目标锚点，分别参考
[《中国居民膳食指南（2022）》公开解读](https://www.sport.gov.cn/n20001280/n20001265/n20066978/c24291669/content.html)
与 [AHA Life's Essential 8](https://www.heart.org/en/healthy-living/healthy-lifestyle/lifes-essential-8)；
`0.35 分/1000 步` 等仍是待纵向验证的 V1 产品参数，不是临床效应量。

归因页与模型页保持 `fc1f6d5` 的既有样式和主要交互。归因页“健康改善得分”
不再使用风险百分点差：7 日显示由近 7 日有效 Room 数据计算的当前 RDI，
30/90 日显示窗口内有效日 RDI 的稳健中位数；同卡片折线也使用 RDI 历史。
右侧当前风险、PIAS、活动、16 项因素和干预计划仍走原有链路，模型页不新增
RDI 或展开的 CVD 16 项卡片。
模型页仍不显示接口路径、请求 ID、内部贡献值或体温输入，也不再声称云端模型在端侧运行。“我的”中的每日步数优先使用
Room `ring_activities` 按设备当地自然日聚合的真实活动记录，活动缺失时才兼容旧 `STEPS` 测量。

“我的”头像使用 Android 系统照片选择器。所选图片在本机缩放并重新编码为 JPEG（同时去除原图
元数据），按登录用户 SHA-256 摘要隔离保存到应用私有目录；仅本机预览和持久化，不新增媒体权限，
也不调用后端上传接口。

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
心率、步数/活动、睡眠、血氧、HRV、血压、血糖、压力、MET、ECG、血液成分和身体成分；运行时仍与设备的
SDK 能力报告取交集。新版 `DeviceFunctionPackage1..5` 对相应字段优先，旧版
`FunctionDeviceSupportData` 仅作为兼容回退；应用等待能力回调稳定后再判定，避免 MT116
因旧回调首次返回的字段尚未初始化而误报不支持 ECG。血糖校准和经期设置也只在设备报告相应能力时启用。
HBand 将独立测量能力与历史协议能力分开处理，避免把 `hrvType`/`metType` 误当成独立测量
开关而触发 SDK 的“不支持”提示。2026-07-30 的 MT116 真机日志进一步确认：固件虽然声明
HRV、压力、MET 独立能力，三项 `manual_detect_de` 命令仍全部返回 `unknown action`。因此
HRV、压力在设备报告 `miniCheckup` 时优先走一键体检真实结果，MET 的“获取”按钮优先读取
设备最新 MET 历史；只有调用方明确禁用真实数据兜底时才允许尝试专用直测接口。连接成功的
`RH-HB-E01` 也允许对 HRV、压力、MET 发起受控历史读取；失败或无有效值时不写入占位数据。
`ECG` 是 `RH-HB-E01` 的必需能力；设备未上报 ECG 时连接会明确失败，避免把不兼容型号当作已支持商品。
不支持的能力在数据页保留禁用入口或静态空卡片，但不会触发测量、写入 0 或生成模拟数据。
计步、睡眠、活动属于同步数据，不提供即时测量按钮；数据页“睡眠与活动”区域提供手动同步按钮，
点击后执行完整设备历史同步，同步期间按钮禁用并显示进度。生命体征和高级指标无记录时显示 `--`。
血液成分拆分为尿酸、总胆固醇、甘油三酯、HDL、LDL 独立记录，单位读取设备个性化设置；
身体成分拆分为 14 项独立记录。ECG 和身体成分在下发测量命令前会显示操作说明并等待用户确认，
明确要求另一只手持续接触金属电极片、保持姿势稳定；取消说明不会启动 SDK 测量。
血糖校准与女性功能是设备设置，不写入测量表；当前女性功能
只接入经期模式，备孕、孕期和妈妈模式尚未开放。
同步按 SDK 的串行限制先用 `readSleepData` 完整读取睡眠，再用 `readOriginData` 读取五分钟原始数据；
这样兼容合并读取只返回原始数据、不返回睡眠的设备固件。原始步数、距离和热量按天聚合，
并用实时计步补齐当天结果；同时读取设备声明支持的手动测量、ECG 和身体成分历史。ECG 测量同时处理
正常结束状态和异常诊断结果，即使设备不返回曲线但返回平均心率也会保存摘要。血糖保留设备单位，
压力只保存正数 `1..100 score`，代谢当量保存为 `MET`；一键体检历史中的 HRV/压力也会按相同
规则规范化。ECG 波形只写入本地 Room，
不会进入遥测上传批次；实时回调的 ADC 采样按对应增益通过官方 `EcgUtil` 换算为 mV，
Room v5 同时保存采样率、绘制频率、时长、导联、ECG 类型、校准方式、平均心率和接触质量。
旧版 `INT32_LE` 记录通过 v4→v5 非破坏迁移保留，在详情页只按相对幅值展示；新记录使用
`FLOAT32_LE` 保存校准后的 mV。数据页可进入单导联 ECG 详情查看实时和最近 10 条本机历史波形；
导联仅在 SDK 明确返回 `leadOffType` 时标记为 I 或 V1，否则显示待设备确认。
血压与 ECG 结果仅用于健康记录，SDK 疾病风险不作为诊断展示，页面固定提示
“仅供健康参考，不能替代医疗诊断”。
HBand 体温在当前采购设备上验证不通过，已从 `RH-HB-E01` 商品能力和数据页移除。
若 HBand 只返回总睡眠时长而没有深睡/浅睡拆分，应用会保存阶段未知的睡眠会话并展示总时长，
不会把未知时长伪造为深睡、浅睡或 REM。

Debug APK：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 当前限制

- 已有 MRD/RWFit/HBand 单一有效设备路由；RWFit 真机型号/固件、HRV 单位、数据准确性
  和后台稳定性仍待验证；HBand 已开始真机联调，连接及 ECG 所需的 JieLi/Nordic/JNI 运行时依赖已补齐，
  已实现能力门控的心率、步数/活动、睡眠、血氧、HRV、血压、血糖、压力、MET、ECG、血液/身体成分、
  血糖校准和经期设置，仍需使用完整重装 APK
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
