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
- 本地 RDI 引擎（设计 6 章：总公式 50+ΣC、领域上限、平滑、个人基线、可信度收缩）。
  Room v8 每日快照/逐因素证据；v12 增加 `is_mock` 列；v13 新增 `rdi_baselines`（锚定基线）、
  `rdi_confirmed_labs`、`rdi_confirmed_meals` 三张表。
  已接入「健康归因」页（分数/状态/周期趋势/周期 Top 3/固定免责声明）；数据质量只展示
  “依据充分/一般/有限”，不把输入完整性系数表述为统计可信度百分比。
  状态枚举按有效日数判定：NO_DATA / BASELINE_BUILDING / PRELIMINARY / CONFIRMED / STALE / INVALID / DEBUG_MOCK。
  六大领域全部实现：C_activity / C_sleep / C_recovery / C_bp_weight（仅已确认上臂袖带血压计分，未验证手表血压不计分）
  / C_lab（±10，80%实测+20%控制支持）/ C_diet（±5，单餐 -2~+2）。
  个人基线锚定化：首个连续 14 有效日建立稳健中位数+MAD，冻结 90 天，重建保留旧版本（SUPERSEDED）。
  静息心率因子、睡眠相对基线改善项已补全；绝对状态修正改为累加而非取下界。
  C_diet 已改为**离线优先本地估算**：直接读取 Room v11 `diet_records` 当日餐食，按保守营养区间
  （热量 1600–2400kcal 中线、钠 ≤2300mg、蛋白 ≥50g、脂肪供能 ≤35%）分摊每餐影响（单餐 ±2，domain ±5），
  不依赖网络或 model-service 回填；若后端经 `rdi_confirmed_meals` 回填精确 `meal_impact` 则优先采用。
  后台 `rdi_confirmed_meals` 表保留为可选叠加通道，不作为归因前置条件。
- Room v9/v10 通过显式迁移保存 RHI 手填健康指标、经确认上臂袖带 7 日血压和医院血检；Room v11 保存按用户隔离的手工餐食并接入 durable queue，不依赖破坏性迁移。
- Android RHI Lite 透明计算：使用 Room 可穿戴数据、经核对健康档案和当前用户资料生成 RHI-100；空白字段保持 `NULL`/中性并降低可信度，不补正常值。
- 数据页风险卡把既有 16 特征评估接口作为 RDI-16 数据源，只展示真实非 Mock 结果；健康指数圆环读取 RHI-100，不再使用硬编码分数。
- Foreground Service 后台低频采集与 WorkManager 恢复任务。
- 数据页已连接时执行睡眠/步数/活动日常增量同步；断连时按钮禁用且自动采集跳过，不触发静默重连。
  HBand 首次/缺口同步补读原始历史，近期重复同步使用两天重叠窗口并跳过无缺口的长历史命令。
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
├─ rhi/             本地 RHI Lite 特征、透明曲线与周期聚合
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
app/libs/BmpConvert_V1.6.0_10604-release.aar
app/libs/abpartool-release.aar
app/src/main/jniLibs/{arm64-v8a,armeabi-v7a,x86,x86_64}/libnative-lib.so
```

五个 JieLi/Bluechip 配套 AAR 满足 HBand 核心 SDK 的连接、认证、管理器初始化和断连释放依赖；
其中 `BmpConvert` 必须存在，否则 SDK 的 `releaseJLSDK()` 会在断连回调中崩溃。应用不提供 OTA、
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

Debug 默认后端（已提交到 `gradle.properties`，仅用于真机联调）：

```text
https://rehealth.youngjimmy.store/jeecg-boot/
```

可在未跟踪的 `local.properties` 中覆盖（优先级高于 `gradle.properties` 与环境变量）：

```properties
# Android 模拟器访问宿主机
rehealth.api.base.url=http://10.0.2.2:8080/jeecg-boot/

# USB 真机：先执行 adb reverse tcp:8080 tcp:8080
rehealth.api.base.url=http://127.0.0.1:8080/jeecg-boot/

# 仅在明确切换到公网 Release 环境时配置；必须是 HTTPS
rehealth.release.api.base.url=https://rehealth.example.com/jeecg-boot/
```

也可分别使用 `REHEALTH_API_BASE_URL` 和 `REHEALTH_RELEASE_API_BASE_URL` 环境变量。
未显式配置公网 Release 地址时，提交的 `https://api.rehealth.invalid/` 占位地址会
失败关闭，避免构建意外连接公网联调环境。

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

Room v7 新增 `health_chat_conversations`，从 v6 消息无损生成会话标题、更新时间和当前会话；
Room v8 为睡眠会话新增可空的 `total_sleep_minutes`，v7→v8 迁移保留已有健康数据。
首页支持本机会话列表、新建、切换以及经确认的删除/清空；删除使用本地墓碑阻止“最新会话”刷新
立即恢复，但只影响本机缓存。当前后端只提供最新会话读取，没有列表/删除契约，因此云端
`software_db` 完整历史不会随本机删除而删除。
每次登录或注册成功后都会为当前账号创建新的活动对话；首页展示活动对话的完整可滚动消息，
滚动时收起大图与问候。新注册账号按用户记录待完成的健康初始问答，既有账号默认直接进入首页，
不会因另一账号的完成状态被重复引导。AI 授权上下文包含后端个人档案中的姓名/昵称。

首页“拍照记录”使用系统相机和应用私有 `FileProvider` 临时文件。客户端校正照片方向、限制长边并
重新编码后，通过认证 multipart 接口上传给 JeecgBoot；模型密钥不进入 APK。服务端只保存验证后的
FOOD/OCR 结构化结果，首页与数据页的“今日行为记录”按当前账号显示，原始照片在请求结束后删除。
相机返回后客户端会等待私有文件大小稳定，再通过文件路径解码；这兼容返回结果早于文件刷盘完成的
MIUI 相机，并避免重复打开 `content://` URI。读取失败日志只记录异常类型和文件大小，不记录图片内容。
照片识别使用独立于普通 API 的长请求超时，以覆盖视觉模型处理时间；服务端单次最多等待 75 秒且不做
自动重复推理。超过该时限时界面明确提示“图片识别超时”，不会误报为普通网络断开。
营养值为图片估算，不能替代专业营养或医疗评估。

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

Room v9 新增按用户隔离的 `rhi_manual_health_inputs`，保存日均久坐、腰围、
正式 VO₂max、HbA1c 和 eGFR；Room v10 增加经用户确认的上臂袖带 7 日血压、
医院血检及报告日期。`8→9` 和 `9→10` 都是显式迁移，空字段保存为 `NULL`，
不会写入正常默认值。医院血检按报告日期衰减可信度；当前真实录入口统一要求
使用 `mmol/L`，不对未知单位做静默换算。

Room v11 新增按用户隔离的 `diet_records`。归因页可录入餐次、餐食内容、热量和
可选三大营养素；记录先落 Room，再以稳定批次 ID 加入 `telemetry-v2 dietRecords`
上传队列。离线或尚未取得真实设备绑定时保留本地记录，绑定恢复后补排队。

归因页与模型页保持 `fc1f6d5` 的既有样式和主要交互。归因页“健康改善得分”
使用本地 RHI Lite `rhi-deterministic-preview-2.2.0-android-lite`，按当前选择的
7/30/90 日窗口显示最近有效 RHI 与该窗口最早有效个人基准的带符号差值；
同卡片折线展示相同窗口的真实 RHI 历史。2.2.0 按
LITE/STANDARD/CLINICAL 分级判定可信度分母，
仅按用户实际具备的证据计分，不再让纯可穿戴用户被化验项拉低可信度，
`total_cholesterol` 也不再被重复计数；MVPA 个人基线改用 7 日滚动总量以对齐量纲；
`steps_7d_mean` 恒除以 7，未佩戴日按零暴露计入；新增
`activity_duration_missing`（有步数但运动时长为 0）、`wear_time_incomplete`、
`blood_pressure_unavailable`、`steps_all_zero` 四类质量提醒，仅解释可信度、不改分数。
Room v14 将 RHI 日度持久化拆为
`rhi_daily_health_index` / `rhi_daily_domain_score` / `rhi_daily_feature_snapshot` /
`rhi_data_quality_snapshot` 四张表，13→14 为纯新增迁移，不改动或删除既有表；
按 `(user_id, scored_on)` 主键重算即覆盖，未参与计分的域存 `NULL` 而非中性 50；
`delta_7d` / `delta_28d` 按固定回看窗计算，7 日与 90 日视图给出一致的 7 日变化。
“我的 > 健康档案”可录入日均久坐、腰围、正式
VO₂max、HbA1c、eGFR、经确认的上臂袖带血压和医院血检。RHI 越高越健康；
空白值不填正常值，戒指无袖带血压只展示而不进入 RHI。
归因页“健康改善得分”来自 RHI-100，按所选 7/30/90 日窗口计算最后一个有效日
与第一个有效日的差值，因此表示该窗口内的累计改善，不再用同一个 90 日基线减去
不同周期的中位数。
归因页右侧“RDI-16 风险指数”读取 Android 本地透明规则引擎的原生 0–100 分值，
按 7/30/90 日窗口聚合并显示为 `x/100`，不会把 CVD 风险概率乘 100，也不会读取
PIAS 当前值或预测值。下方个人风险趋势以相同窗口的已落库 RDI-16 历史绘制蓝色实线。
当至少有 7 个活动、睡眠和 HRV 有效日且存在可识别的活动或睡眠计划时，Android
以所选 7/30/90 日个人模式分别构造“维持现状”和“执行计划”的 30 日瞬时输入，并逐日调用同一
RDI-16 引擎；所选周期 RDI 分作为水平的维持现状参考线，两臂逐日原生 RDI 差值叠加到
该参考线形成计划轨迹，第 30 日差值作为预计降低。95% 情景区间使用近期个人波动的确定性
正态敏感性扰动生成，表示输入情景范围而非疾病概率置信区间。所有未来输入和分值仅用于
页面模拟，不写入 Room 观测历史；条件不足时对应字段仍明确显示暂不可用，且绝不以 PIAS
填充。所选窗口内的因素变化贡献统一按“周期末原生 RDI 因素贡献 − 周期起点贡献”计算；
页面随 7/30/90 日切换展示本周/本月/本季度风险变化贡献 Top 3，而不是把周期平均风险负担
误写成改善贡献。负值明确标为“降低风险”，正值明确标为“增加风险”，避免加减分歧义。
该模块使用浅色内层卡片、分隔线和简短因素名称，并以定性数据依据等级替代“可信度 100%”。
趋势图将蓝色实际 RDI、维持现状、执行计划和灰色 95% 情景区间统一锚定到所选
周期最左端的第一个真实 RDI 点，并在同一整段横轴上绘制；情景线与实际线允许重合。
该对齐只影响图表表达，不改变底部原生 RDI 数值。
干预计划仍走原有 CVD-16/PIAS 链路。贡献因素卡片改读服务端独立的
`factor16-rule-v1.0.0` 结果，不再把模型 SHAP 或 RDI16 当作这 16 项的展示贡献。
卡片同时显示送入评估的真实 16 项值和来源入口。血压只接受已确认的上臂袖带
7 日均值，代谢项只接受
带报告日期的医院血检；两类项目按 80% 测量贡献 + 20% 已验证控制支持展示，
尚无纵向证据时支持项明确为 0，不补造数值。模型页不新增 RHI 或展开的
CVD 16 项卡片。
数据页使用相同的双轨语义：风险卡通过 RDI-16 接口展示既有 CVD-16 已确认风险百分比，不能把
`risk_score × 100` 标成动态影响“分”；Mock、网络失败或
未返回有效分数时显示不可用；健康指数来自 RHI-100，今日/7 日显示当前 RHI，
30/90 日显示有效日稳健中位数，数据不足时显示积累状态。
模型页仍不显示接口路径、请求 ID、内部贡献值或体温输入，也不再声称云端模型在端侧运行。“我的”中的每日步数优先使用
Room `ring_activities` 按设备当地自然日聚合的真实活动记录，活动缺失时才兼容旧 `STEPS` 测量。

“我的”头像使用 Android 系统照片选择器。所选图片在本机缩放并重新编码为 JPEG（同时去除原图
元数据），按登录用户 SHA-256 摘要隔离保存到应用私有目录；仅本机预览和持久化，不新增媒体权限，
也不调用后端上传接口；每次进入“我的”都会从当前账号的私有文件重新加载。

模拟戒指只存在于 `app/src/debug`，由 Debug 专用工厂和
`USE_FAKE_RING`/`SEED_FAKE_HEALTH_DATA` 控制。`app/src/release` 的工厂只构造
真实 MRD/RWFit/HBand Provider；远程风险评估失败时显示不可用，不生成本地模拟风险。

Debug 版“我的 → 设备绑定”另提供唯一的全链路演练入口，且必须在警告对话框中
再次确认。它为当前测试账号写入 90 天、来源为 `synthetic_qa` 的 50 岁男性正常
纵向数据，然后依次真实执行 Room 入库、设备绑定、遥测上传及持久化确认、本地和
JeecgBoot 远程 RHI、30 日 RDI-16 与 PIAS；Release source set 的实现固定
`available=false`。遥测中的 `rawSignalExcluded=true` 是“未上传原始信号”的
控制元数据，不得被服务端误判为 PPG/RRI 波形内容。

RHI 默认在 Android 本地计算；数据页和归因页不展示计算源切换或“本地即时”来源
标签。开发验收使用的远程复算能力仍调用认证路由
`POST /rehealth/mobile/rhi/evaluate-series`，JeecgBoot 再调用
`model-service /v2/rhi/evaluate`；APK 不保存 model-service 地址，也不直接访问它。

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
HBand 将独立测量能力与历史协议能力分开处理。底层仍识别 HRV 直测所需的
`isSupportHRV && isSupportHrvAppDetect` 和 MET 直测所需的
`isSupportMet && isSupportMetAppDetect`，并保留 SDK 的 `startDetectHrv`、`startDetectMet`
接口供后续兼容与诊断。当前固定的
`vpprotocol-2.3.73.15.aar` 已包含 2026-04-23 HRV 与 2026-07-02 MET API，因此本次不引入
仅包含后续 JH58 变更的 SDK 升级。2026-07-30 的 MT116 真机证据表明固件虽然声明
HRV、压力、MET 专用能力，三项专用命令仍返回全零 `unknown action`。因此
`RH-HB-E01` 产品流程将 HRV、压力优先路由到 package-4 一键体检或真实历史，MET 只读取
设备真实历史；失败或无有效值时不写入占位数据，也不显示伪结果。
`ECG` 是 `RH-HB-E01` 的必需能力；设备未上报 ECG 时连接会明确失败，避免把不兼容型号当作已支持商品。
HRV、压力、MET 卡片不由能力位直接决定，而由已落库的真实有效值决定：HRV/MET 必须大于 0，
压力必须在 `1..100`，且来源不能是 Mock/模拟数据。HRV、压力通过一键体检可测时才显示测量按钮，
只有历史值时显示卡片但不显示按钮；MET 永不显示实时测量按钮。无有效值或来源未验证时直接隐藏卡片。
不支持的其他能力保留禁用入口或静态空卡片，但不会触发测量、写入 0 或生成模拟数据。
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
HBand 睡眠把 SDK `allSleepTime` 原值保存到 Room v8 的 `total_sleep_minutes` 并优先用于展示；
`sleepDown/sleepUp` 只保留实际入睡/起床时刻，不再用二者跨度替代睡眠总时长。SDK 未提供清醒
分钟数时不会用总时长与深睡/浅睡之差伪造清醒阶段。其他 Provider 没有设备总时长时按
深睡+浅睡+REM 计算，阶段也缺失才回退会话跨度；“今日”查询按结束时间包含昨夜跨午夜、今日醒来的会话。
HBand 同一晚可能回调多条递增的累积睡眠快照，今日及周期统计先按本地结束日选择当天最大（最终）
总时长，再对每天的最终值取平均，不会把一次睡眠的中间快照当成多晚数据。

数据页默认打开“今日”。每个自然日最多保存一条已确认、非 Mock 风险结果；健康指数按
`(1 - riskScore) * 100` 计算；今日优先展示已确认 RDI-16 返回的当前风险，7 天/30 天等周期
只对已落库的 RDI-16 真实风险日取平均，并展示有效日数。PIAS 不作为该卡片的风险来源。

Debug APK：

```text
app/build/outputs/apk/debug/app-debug.apk
```

### Debug RHI/RDI attribution fixture (2026-08-01)

The Debug mock ring seeds 118 Room days: 90 visible days plus 28 warm-up
days. Selecting 7/30/90 days therefore uses 7/30/90 real local RDI daily
calculations instead of reusing a 30-day flat series. The attribution risk
number is the native Android RDI-16 index on a 0-100 scale; CVD probability and
PIAS output are never substituted. RDI `rdi-rule-1.0.1` aligns the activity
baseline with the current seven-day-minute unit and removes the invalid
clock-minute-as-sleep-variability baseline. The RHI improvement number is the
governed RHI-100 difference between the last and first valid day inside the
selected 7/30/90-day window. Guest RHI daily calculations are persisted under
the local-device Room key but are never queued for authenticated upload. These
synthetic records are Debug-only and marked as mock in Room.

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
