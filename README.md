# ReHealth AI / 睿禾健康

ReHealth 是面向可穿戴设备和健康干预场景的软硬件一体化系统。当前 MVP
正式客户端以 HBand MT116 蓝牙设备和云米云端手表为用户可选接入，完成从设备采集、本地持久化、离线上传、
云端时序存储，到 CVD 风险评分、干预建议和用户反馈的闭环。

本文档是仓库级项目入口和结构说明。具体接口、数据契约、部署细节和当前
验收状态，以“文档索引”中列出的专项文档为准。

## 1. 项目目标

当前主链路：

```text
登录与健康访谈
  -> 按 productCode 绑定单一可穿戴设备
  -> BLE 前台/后台采集
  -> Room 本地持久化
  -> 本地上传队列
  -> Gateway / Device Service
  -> TimescaleDB + Transactional Outbox
  -> CVD 16 维特征与模型评分
  -> 干预建议
  -> 用户反馈与风险趋势
```

云米 S8/S9/GS20/GS17/A67/K9L 使用独立云端拉取链路：App 以 IMEI 绑定，JeecgBoot
持有厂商凭据并先将心率、血氧、血压持久化到硬件库，再返回 App 写入 Room。首次绑定
回填最多 31 天，后续增量同步；Room v15 隔离测量，Room v16 将相同的登录用户/设备归属扩展到睡眠、活动和信号/ECG，Room v17 增加按登录用户隔离的 PIAS 展示缓存；旧的无归属行保留但不向其他账号展示。

Android 按 `productCode` 选择单一有效 Provider，Release 只注册 HBand 和 Viomi Cloud；
MRD/RWFit 仅保留在 Debug 工程测试目录。HBand 已完成
隔离 Provider 并进入真机联调，已按设备能力接入心率、步数/活动、睡眠、血氧、HRV、血压、
血糖、压力、MET、ECG、血液成分和身体成分的测量或历史同步，并接入血糖校准与经期设置；
固定 SDK 仍保留 HRV/MET 专用 API 及其双能力位识别，但 `RH-HB-E01` 产品流程按 MT116
真机证据将 HRV/压力优先路由到一键体检或真实历史，MET 只读取真实设备历史。三项只有
真实 Provider 返回有效正值时才写 Room 并显示；MET 不提供实时测量按钮，空值、零值、越界值
和模拟来源直接隐藏。MT116 三项专用命令返回全零 `unknown action` 的证据作为该策略的回归基线；
HBand 体温因当前真机验证不通过，已从 `RH-HB-E01` 商品能力和数据页移除；MT116 已加入
该商品的扫描优先提示，能力判定合并新版分包报告，并由 2 号能力包优先确认 ECG；
采购设备的完整连接、能力与数据验收仍待完成。当前不支持多设备同时连接或数据融合。
HBand ECG 已补齐与固定 SDK 匹配的四 ABI 原生库，实时 ADC 按设备增益换算为 mV；Android Room v5
保存导联、采样/绘制频率、时长、校准方式、平均心率和接触质量，并提供实时及本机历史单导联波形详情。
ECG 和身体成分在下发设备测量命令前先展示电极接触与稳定姿势说明，用户确认后才开始测量。
SDK 疾病风险不作为诊断展示，界面明确标注仅供健康参考、不能替代医疗诊断。
Debug 与 Release 的用户选择器都只展示“HBand”和“云米（IMEI 云端）”；Mock 与旧 MRD/RWFit
工程测试入口只存在于 Debug。两种构建首次安装都默认 HBand，覆盖安装时旧 MRD/RWFit 选择会迁移到 HBand。
后台恢复只重连加密保存的当前绑定；HBand 所需四项真实画像使用按用户哈希隔离的
加密缓存，不使 BLE 采集依赖网络。
Android 在重新登录和进入个人页时按当前用户读取类型化个人资料及最近健康问答；这些读取与
风险/干预服务解耦，退出后清除上一用户的内存资料。健康问答用户消息先写按用户隔离的
Room v8 本地库（v7 会话/消息表及可空的设备睡眠总时长），再调用服务端并由 `software_db` 保存完整会话；问答中明确自述的姓名、性别、
年龄、身高和体重由 JeecgBoot 合并写入类型化个人档案，并在同轮回复中确认更新字段；麦克风语音入口按需解释并申请权限，应用不保存录音。
新注册账号的健康初始问答状态按账号隔离，既有账号登录不重复提示；每次认证创建新的活动健康
对话，历史仍按账号保留。数据页默认今日，跨午夜睡眠按结束日统计；HBand 优先展示 SDK 的
`allSleepTime`，不会把起止跨度或清醒时长计为实际睡眠；同一晚的累积回调按结束日只取最终
有效记录，数据页与“我的”页共用相同的夜间选择规则，周期睡眠只平均每天最终值。步数按本地
自然日保留手表累计值的最大值，避免把本地采集和登录云端回填的同一天记录重复相加。周期健康指数仅聚合已确认的
每日真实风险结果。
注册短信在本地可通过 `JEECG_SMS_DEV_MODE=true` 使用固定测试码；生产由 JeecgBoot 独立调用阿里云
号码认证服务 `Dypnsapi`：`SendSmsVerifyCode` 使用赠送登录/注册模板 `100001`、
`{"code":"##code##","min":"5"}` 让云端生成 6 位验证码，注册时再以
`CheckSmsVerifyCode` 的 `VerifyResult=PASS` 为唯一通过条件。RAM AccessKey 仅挂载在服务端，
Redis 只保存 5 分钟发送会话、60 秒冷却、集群频控和注册锁，不保存生产验证码明文；配置缺失时
失败关闭，也不复用 OSS 凭据或标准短信服务 `Dysmsapi` 的签名模板。具体部署入口见
`backend/deploy/rehealth/README.md`。
公司官网本地联调使用独立 `POST /sys/webLogin`：复用现有 Jeecg 用户、租户、角色、失败锁定和
JWT 校验，但以 `WEB` 客户端类型签发令牌，单点登录缓存与管理 PC 端、Android APP 分离。
官网 FastAPI BFF 在服务端 Redis 保存 Jeecg Token，并只向浏览器设置 HttpOnly 会话 Cookie；
浏览器中的兼容 Token 仅用于 CSRF 校验。该本地阶段保留官网滑块交互，邮箱验证码和机构自主注册
尚未接入统一认证，不能作为生产人机校验或生产开户注册方案。
健康问答默认由 JeecgBoot Java LangChain4j 执行；身份类问题通过只绑定当前认证账号、且不接受
`userId` 参数的服务端资料工具读取最新昵称与基本资料，`model-service` 对话接口仅保留为显式回滚。
首页拍照记录使用系统相机的应用私有临时文件，经客户端方向校正、缩放和重编码后上传到 JeecgBoot；
Java LangChain4j 调用独立的服务端视觉模型进行食物分析/OCR，仅把当前用户的结构化结果保存到
`software_db` 并展示在今日行为记录；有效 FOOD 结果同时以服务端行为 ID 幂等写入当前用户的
Room 今日餐食并进入既有离线上传队列，OCR/OTHER 不进入餐食。服务端按设备本地日查询，客户端
再次按本地自然日边界过滤；登录或页面刷新拉回的营养完整 FOOD 也会幂等回填餐食，避免拍照回调
中断后只剩行为记录。原始照片和模型凭据均不进入数据库或 APK。退出登录会清空用户健康
页面的内存状态，餐食、RHI/RDI 与问答 ViewModel 均按当前账号隔离。
数据页的睡眠/步数/活动按钮只在设备已连接时执行日常增量同步；HBand 对近期数据使用两天重叠窗口并
跳过无缺口时的长原始历史读取，首次或发现活动缺口时仍补读设备历史。后台前台服务保留显式恢复重连。

## 2. 系统架构

```mermaid
flowchart LR
    Ring["当前 productCode 对应的单一设备"] --> Android["Provider 路由 / Android BLE"]
    Android --> Room["Room 本地数据与上传队列"]
    Room --> Edge["Edge / Gateway"]

    Edge --> Device["Device Service"]
    Edge --> Jeecg["JeecgBoot ReHealth"]

    Device --> Timescale["TimescaleDB 硬件时序数据"]
    Device --> Outbox["Transactional Outbox"]
    Outbox --> Kafka["Kafka 遥测事件"]
    Kafka --> Projection["Jeecg 运营与质量投影"]

    Jeecg --> SoftwareDb["MySQL software_db"]
    Jeecg --> Model["model-service"]
    Jeecg --> Agent["LangChain4j 健康问答"]
    Model --> Risk["CatBoost / SHAP"]
    Jeecg --> PIAS["PIAS 归因服务"]
```

架构边界：

| 组件 | 主要职责 | 不应承担的职责 |
| --- | --- | --- |
| Android | BLE/厂商 SDK、Room、本地轻量特征、离线队列、用户交互 | CatBoost、SHAP、LLM、云端归因 |
| Gateway | 统一公网入口、路由、安全头处理、未来限流 | 业务数据持久化、模型推理 |
| Device Service | 硬件遥测与饮食行为校验、设备授权、TimescaleDB、Outbox、按租户/用户/自然日提供有界行为摘要 | 用户业务档案、模型推理 |
| JeecgBoot | 账号、租户、设备绑定、业务编排、LangChain4j 健康问答与结构化生活方式干预、后台权限、software_db | 直接拥有硬件时序库、运行 CatBoost/SHAP/归因模型 |
| model-service | CVD 风险、SHAP；保留旧干预与健康助手接口用于兼容/灰度回退 | 用户认证、设备接入、业务主数据、权威聊天历史 |
| PIAS | 生产个体归因 | Android 端计算、静默 Mock 回退 |
| rehealth-algorithms | 训练、仿真、算法研究及独立 PIAS 实现 | 患者移动端业务入口 |

## 3. 仓库结构

```text
rehealth_tongbu/
├─ Android-apk/                 Android Compose 客户端
│  ├─ app/src/main/java/com/rehealth/genie/
│  │  ├─ ring/                  可穿戴领域、BLE 仓库、MRD/RWFit/HBand 适配
│  │  ├─ ring/provider/         单一有效绑定、商品目录与 Provider 路由
│  │  ├─ ring/data/             Room 遥测实体与 DAO
│  │  ├─ service/               前台采集服务
│  │  ├─ work/                  WorkManager 上传与恢复任务
│  │  ├─ data/sync/             本地上传队列与反馈同步
│  │  ├─ features/              CVD 16 维特征提取
│  │  ├─ rdi/                   本地 RDI 透明规则、每日快照与证据
│  │  ├─ rhi/                   本地 RHI Lite 透明评分与周期聚合
│  │  ├─ network/               认证客户端、API 与 DTO
│  │  ├─ phm/                   风险/干预服务抽象
│  │  └─ ui/                    Compose 页面
│  └─ docs/                     Android 契约、同步计划和 QA 文档
│
├─ backend/
│  ├─ contracts/                公共 OpenAPI、遥测 Java 契约、事件 Schema、ADR
│  ├─ device-service/           独立硬件遥测接入服务
│  ├─ jeecg-boot/               JeecgBoot Java 后端
│  │  └─ jeecg-boot-module/
│  │     └─ jeecg-module-rehealth/  ReHealth 业务模块
│  ├─ jeecgboot-vue3/           JeecgBoot 管理前端
│  ├─ deploy/rehealth/          Compose、Gateway、Kafka、TimescaleDB、监控
│  └─ qa/                       拓扑、迁移和发布门禁
│
├─ model-service/               FastAPI 模型与健康助手服务
│  ├─ app/                      API、模型加载、执行保护和安全策略
│  ├─ models/                   本地模型制品挂载位置
│  ├─ tests/                    Python 自动化测试
│  └─ docs/                     模型契约、制品和注册表文档
│
├─ rehealth-algorithms/         HealthAgent、PIAS、训练和算法研究
├─ docs/archive/                历史验收与 QA 快照（只读参考）
├─ tools/                       仓库级源码辅助工具（不存放下载的工具链）
├─ STATUS.md                    当前实现与发布状态唯一入口
├─ ENGINEERING.md               MVP 工程实施总纲
├─ QA_TEST_PLAN.md              测试计划
└─ RELEASE_CHECKLIST.md         发布检查清单
```

## 4. 核心数据流

### 4.1 设备采集与本地优先

```text
productCode / 单一有效绑定
  -> ActiveRingRepository
  -> HBand SDK 或 Viomi Cloud（当前 productCode 对应的唯一 Provider）
  -> RingRepository
  -> 规范化测量、睡眠、活动记录
  -> Room
  -> durable upload queue
  -> WorkManager
```

必须先完成 Room 写入，再创建上传任务。网络或后端不可用不得阻塞 BLE 采集。

### 4.2 硬件遥测上传

公共路径保持稳定：

```text
POST /jeecg-boot/rehealth/mobile/measurements/batch
POST /jeecg-boot/rehealth/mobile/viomi/bind  (云米 IMEI 验证与用户绑定)
POST /jeecg-boot/rehealth/mobile/viomi/sync  (按时间窗拉取心率/血压/血氧并入库)
GET  /jeecg-boot/rehealth/mobile/measurements/recent
POST /jeecg-boot/rehealth/viomi/report        (云米/viomi 平台主动上报回调；JWT HS256 验签)
```

Android 在每次登录令牌建立后（以及持有有效会话的进程重启后）优先调用一次
`measurements/recent?limit=200`，校验返回用户归属并按稳定记录 ID 幂等回填测量、睡眠和活动到
Room；同一客户端睡眠/活动记录只保留最终累计结果，再刷新档案。回填成功会触发本地 RHI 重算；网络或硬件库查询失败不会阻塞登录、BLE
采集或后续重试。App 不直接连接数据库，数据库访问始终经过认证 API。

`/rehealth/viomi/report` 是手表厂商（云米 miwitracker）主动上报的回调端点：云米平台
先把数据发给自己的云，再按我们提供的回调地址（AppId/AppKey 鉴权）推送到本端点，
复用与手机 batch 同一条 `hardware` 落库链路。端点契约与字段映射见
`backend/docs/HARDWARE_INGEST_ARCHITECTURE.md` 的"Viomi Adapter"一节。

Gateway 在完成切换审批后把这两个路径路由到 Device Service。Device Service
校验批次、通过 JeecgBoot 确认用户/租户/设备绑定，并在一个 TimescaleDB
事务中写入：

1. 上传批次和幂等收据；
2. 规范化测量、睡眠、活动和质量事件；
3. 对账状态；
4. 待发布的 Outbox 事件。

只有数据库事务成功后，Android 才能把本地队列项标记为完成。

### 4.3 风险、干预与反馈

遥测上传不直接触发模型评分。当前标准路径是：

```text
Android Room
  -> HealthFeatureExtractor
  -> CVD 16 feature vector
  -> JeecgBoot /features/evaluate
  -> model-service /v1/cvd/risk/evaluate
  -> software_db 持久化风险结果
  -> POST JeecgBoot /interventions/generate
  -> 每次重新读取 software_db 画像/访谈/最新风险
  -> Device Service 读取 TimescaleDB 今日活动/睡眠/测量/饮食及近 7 日变化
  -> JeecgBoot LangChain4j 生成 1–5 条结构化保守行动
  -> software_db 持久化计划
  -> Android 本地反馈队列
  -> JeecgBoot feedback API
```

饮食记录自 `telemetry-v2` 起作为 `dietRecords` 随本地持久化后的上传批次进入
TimescaleDB `hardware_diet_record`。干预生成忽略客户端提交的画像/风险上下文，
只信任当前认证用户的服务端数据，并将今日行为置于历史趋势之前；趋势仅作为描述性
证据，不解释为诊断或因果改善。

Android 进入归因页时只读取今日已持久化计划，不再在资料刷新中静默生成；用户通过
“生成个性化干预计划”明确触发服务端生成，并可看到加载与失败状态。客户端同时兼容
规范 snake_case 与既有 camelCase 干预响应。已有计划在归因页默认展开为 01–05 编号行动
清单，显示围绕 16 项健康输入的说明、展开状态和整宽收起/展开按钮；空计划仍保留显式
生成入口。健康档案核心字段更新后覆盖旧归因展示值，
档案或经确认的血压/血检保存会触发 CVD-16 重新评估。退出登录与 401 暂停会话由根导航
统一清理并直接返回登录页，不在主界面停留为“会话已过期”横幅。

RHI v2 当前以可回滚的 preview 链路接入，不替代既有临床风险链路。Android
默认在本地用 32 维可用子集计算 RHI；正式数据页和归因页不向用户暴露计算源切换。
开发验收仍可通过 JeecgBoot 远程复算：
`POST /rehealth/mobile/rhi/evaluate-series` 由 JeecgBoot 逐日调用
`model-service /v2/rhi/evaluate`，Android 不直连 model-service。该 preview
不把 RHI 当作疾病概率，也不建立云端 RHI 权威缓存；后续仍需由独立 Feature
Pipeline 生成并持久化版本化日快照。算法规划见
`rehealth-algorithms/docs/RHI_V2_ALGORITHM_PLAN.md`。

Android 当前保留独立的本地 `RDI rdi-rule-1.0.0` 算法与持久化骨架，但它
不再驱动“健康改善得分”。归因页顶部改由 `RHI Lite
rhi-deterministic-preview-2.1.0-android-lite` 从 Room、经核对的健康档案
和当前用户资料计算 RHI-100；阈值曲线、适用域权重、可信度收缩和
`0.25/0.75` 平滑与 RHI v2 预览规范一致。Room v10 通过显式 8→9→10
迁移保存日均久坐、腰围、正式 VO₂max、HbA1c、eGFR、经确认上臂袖带
7 日血压和医院血检。无法安全推导的字段保持缺失/中性而不填正常值，
戒指无袖带血压不进入 RHI。登录用户保存这些字段时先写 Room，再以稳定队列项调用
`PUT /rehealth/mobile/rhi/manual-inputs` 写入 MySQL；重新登录或打开健康档案时按
`updatedAt` 合并云端较新副本。
模型页采用固定的端侧学习视觉稿，仅调整展示层，不改变实际云端风险评分链路。
原 RDI 骨架可从 Room 的近
7/28 日活动、睡眠及满足同设备/有效天数要求的 HRV 生成近期可干预负荷，
将可信度直接乘入贡献，并对展示值做 `0.30/0.70` 平滑和普通日 `±3` 限幅。
数据不足时结果向 50 收缩或冻结上一有效展示值。它不调用新接口、不覆盖
CVD-16 风险历史，也不从消费设备反推验证血压、LDL 或 HbA1c。Room v8
通过显式 7→8 迁移保存每日快照和逐因素证据；Room v9/v10 保存用户手填
RHI 指标和经核对的临床血压/血检值，所有空白字段仍为 `NULL`。
归因页“健康改善得分”显示最近有效 RHI 与 90 天窗口最早有效个人基准的
差值；覆盖不足 90 天时明确标为“最早有效基准”，不会伪装成 90 天数据。
同卡折线按 7/30/90 日选择展示真实 RHI 历史。右侧“风险指数”只读取已确认
RDI-16 返回的 `risk_score`，按 `risk_score × 100` 显示为 `x/100`，不读取
情景模拟返回作为当前风险。下方个人风险趋势以已落库 RDI-16 历史为深色
实线，维持现状/执行计划为珊瑚色/绿色虚线；接口返回有效上下界时再显示
淡灰色 95% 参考区间；不与 RHI 混算，并明确情景模拟不代表未来疾病发生概率。
个人风险趋势之后展示本地 Room RDI 快照生成的“近期风险影响分”和最多 3 项
可信贡献；该卡不替代上方 RDI-16 当前风险，也不改写情景模拟数据。
归因页下方 16 项贡献现使用独立的 `factor16-rule-v1.0.0` 服务端规则结果，
不是 RDI16，也不替代 CVD 风险概率或 PIAS 因果归因。风险模型原始
`feature_contributions` 仍保留用于模型审计；用户卡片读取
`factor_contributions`。血压与代谢卡执行 80% 经确认实测贡献 + 20%
控制支持趋势的展示拆分；尚无可验证纵向趋势时 20% 为 0，不补造血压或血检值。
每项展开区只显示基于同次送评估值的字段解释和保守固定建议；建议不参与贡献计算，
也不冒充服务端干预计划。展开区不展示来源、规则版本、规则贡献说明或 80/20 技术分量。
仅在 Debug 明确选择 Mock 设备、App 已尝试真实评估请求但服务端尚未返回完整
Factor16 字段或暂不可用时，App 使用同版透明规则补齐 16 项并标记
`factor16-rule-v1.0.0-debug-mock`；Release 实现固定禁用，完整服务端结果始终优先。
该 Debug 回放仍经本机加密档案、Room 临床输入与 Room 活动数据入口提取，
不会在贡献卡 UI 中直接硬编码字段。
数据页同样拆分为两条真实链路：风险卡以 RDI-16 名义展示既有 16 特征
评估接口的非 Mock 返回值（不另改特征提取规则），该值按风险百分比显示，
不得写成 0–100 的动态影响“分”，
健康指数圆环展示 RHI-100；“今日”读取当前自然日有效值，7/30/90 日分别读取所选自然日窗口的稳健中位数，最少需要 3/7/14 个有效日。

Debug 版仅在“我的 → 设备绑定”提供一次需二次确认的 50 岁男性全链路演练入口；
Release 对应工厂固定不可用。演练数据统一标记 `synthetic_qa`，先写 Room，再真实
执行设备绑定、`telemetry-v2` 持久化确认、RHI 本地/远程复算、30 日 RDI-16
评估和 PIAS 30 点情景预测。它不会在其他页面静默生成模拟数据。

未来若需要持续云端分析，应增加独立 Feature Pipeline 消费遥测持久化事件，
按事件中的批次/设备引用读取授权范围内的 TimescaleDB 数据，再生成版本化特征；
不要把原始健康值直接放入 Kafka 事件。

## 5. 数据存储边界

官网管理后台通过 JeecgBoot 只读聚合 API 接入现有 App 数据，不新增上传或存储链路：
`GET /rehealth/admin/v1/patients` 提供最大 100 条的租户成员分页、关键词与
`high|medium|low` 风险筛选；`GET /rehealth/admin/v1/patients/{patientId}` 在再次确认
目标用户属于同一活动租户后，最多调用一次 Device Service 获取遥测摘要。由于档案和风险表
尚无租户列，具有其他活动租户成员关系的用户会被 fail-closed 排除。两个端点均要求
标准 `X-Access-Token`、`X-Tenant-Id` 和 `rehealth:admin:patient:view` 权限，不返回手机号、
邮箱或账号名。遥测详情携带服务端生成的 `provenance` / `isSynthetic`；只有非空且全部命中
真实设备来源白名单的详情才标记 `verified_real`，空、混合未知或未识别来源保持 `unknown`，
合成及 unknown 数据均不返回 `latestRisk`。生产/预发布需挂载 `REHEALTH_DEVICE_SERVICE_INTERNAL_TOKEN_FILE`，缺失
凭据或 Device Service 不可用时详情返回 503；Compose 实际运行的 Cloud JAR 内置并启用权限
Flyway 迁移，但不授予任何默认角色。
列表为避免逐用户调用 Device Service，逐条返回 `provenanceStatus=unknown`；官网统计不得将
unknown 记录计入临床风险。
旧 `/rehealth/admin/v1/users` 已禁用并返回 410 业务码。

| 数据 | 权威存储 | 说明 |
| --- | --- | --- |
| Android 本地遥测和待上传任务 | Room | 本地优先、离线可用 |
| Android 每日 RDI 与贡献证据 | Room v8 | 本地产品趋势；不作为临床概率或云端权威风险 |
| Android RHI 手填指标与已确认临床输入 | Room v9/v10 + MySQL `rehealth_rhi_manual_health_input` | 本地先写、按用户隔离、稳定队列重试；空白保存为 `NULL`，按客户端 `updatedAt` 防止旧副本覆盖新值 |
| Android 手工饮食记录 | Room v11 | 先本地持久化，再通过 durable queue 以 `telemetry-v2 dietRecords` 上传；显式 10→11 迁移保留既有健康数据 |
| Android PIAS 展示缓存 | Room v17 | 按登录用户保存最新响应 JSON、模型版本与 `is_mock`；Mock 行只允许显式全量 Mock Debug 读取，不是云端权威归因结果 |
| 规范化硬件时序数据 | TimescaleDB | Device Service 独占写入和读取 |
| 用户、档案、绑定、风险、干预、反馈、健康问答历史 | MySQL `software_db` | JeecgBoot 业务权威；聊天按用户+租户隔离 |
| 遥测持久化/质量事件 | Kafka | 事件通知，不存原始健康值 |
| 模型制品 | 只读制品挂载 | model-service 校验签名/哈希后加载 |
| 原始 PPG/RRI | 当前禁止云端上传 | 未来必须经过同意、加密和保留策略评审 |

`software_db` 的核心可查询业务字段使用类型化列或明细表；完整 JSON 仅用于模型证据快照、版本化扩展和可重放队列载荷，不作为个人档案或访谈的唯一权威表示。

## 6. 主要开发与验证命令

### Android

```powershell
cd Android-apk
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
$env:REHEALTH_RELEASE_API_BASE_URL="https://api.example.com/jeecg-boot/"
.\gradlew.bat verifyReleaseConfiguration lintRelease assembleRelease
.\gradlew.bat verifyPublishConfiguration
```

Debug APK：`Android-apk/app/build/outputs/apk/debug/app-debug.apk`

Release 默认使用已批准的 `https://rehealth.youngjimmy.store/jeecg-boot/`，版本号可由本机或
CI 覆盖，签名材料必须从仓库外注入且不得进入 Git。Release 合并资源只包含
HBand 与 Viomi Cloud 两个正式 Provider，非生产遥测会被拒绝上传；
环境覆盖仍由 `verifyReleaseConfiguration` 校验。

### Device Service 与遥测契约

```powershell
mvn -f backend/contracts/telemetry/pom.xml test
mvn -f backend/device-service/pom.xml test
```

Device Service 的 TimescaleDB/Kafka 集成测试需要 Docker 和对应测试配置。

### JeecgBoot ReHealth 模块

```powershell
mvn -f backend/jeecg-boot/pom.xml -pl jeecg-boot-module/jeecg-module-rehealth -am -DskipTests=false test
```

### model-service

```powershell
cd model-service
python -m pip install -r requirements.txt
python -m pytest
```

### 契约与部署门禁

```powershell
python backend/contracts/scripts/validate_contracts.py
python backend/qa/rehealth_stack_gate.py topology `
  --compose backend/deploy/rehealth/docker-compose.yml `
  --profiles staging,production `
  --report topology.json
```

## 7. 安全与隐私原则

- 不在生产日志记录原始健康值、原始信号、token、手机号、BLE MAC 或直接标识符。
- Android 上传 SHA-256 地址摘要和稳定设备 ID，不上传原始 BLE MAC。
- 客户端不能通过请求体声明数据所有者；用户和租户来自可信认证上下文。
- 模型和健康助手凭据只存在于后端/model-service 运行时 secret 中。
- 医疗建议必须保守，不能声称诊断、处方或替代医生。
- Mock 输出必须显式携带 Mock 标记，生产和 staging 不允许静默回退。

## 8. 当前已知边界

- 真实 MRD 扫描、长时间重连、锁屏采集、功耗和测量准确性仍需要物理设备 QA。
- HBand 已开始真机联调；管理器和连接回调所需的 JieLi/Nordic 运行时依赖已补齐，
  心率、步数/活动、睡眠、血氧、HRV、血压、血糖、压力、MET、ECG、
  血液/身体成分及设备设置均受能力门控；
  ECG 波形只保存在本地；新 HBand 波形以校准 mV 和结构化导联/采样元数据保存，升级前旧记录保留为相对幅值，
  完整重装后的连接、认证、画像、能力、准确性与后台数据 QA 仍待完成。
- Android 已有 MRD/RWFit/HBand 单一有效 Provider 路由；RWFit 的具体型号、固件、HRV
  单位和长时间采集仍待真机确认；HBand 的扫描、认证、画像同步、数据准确性和后台
  稳定性仍待完整真机验证；不支持多设备同时连接或数据融合。
- 本地遥测与上传队列仍需进一步按用户和设备隔离，并改成完整增量同步。
- Kafka 当前主要承载持久化/质量事件和运营投影，云端连续 Feature Pipeline 尚未实现。
- 独立 IoT 设备直连所需的 MQTT、mTLS、设备证书、激活和吊销体系尚未实现。
- 生产容量、故障转移、备份恢复和长时间硬件压力测试仍需专项验收。

## 9. 文档索引

| 文档 | 用途 | 何时更新 |
| --- | --- | --- |
| `README.md` | 项目入口、结构、整体架构和开发命令 | 模块、主链路、基础设施、关键命令变化时 |
| `STATUS.md` | 当前实现、阻塞项和清理决策 | 验收结论、发布阻塞或关键清理决策变化时 |
| `ENGINEERING.md` | MVP 原则、范围和工程路线 | 目标、边界、里程碑变化时 |
| `Android-apk/docs/REHEALTH_INTEGRATION_CONTRACT.md` | Android/Backend 正式接口契约 | 路径、认证、DTO、完成语义变化时 |
| `Android-apk/docs/D2_TELEMETRY_SYNC_PLAN.md` | 遥测同步状态和剩余 QA | 队列、重试、持久化确认、硬件 QA 变化时 |
| `Android-apk/docs/wearable/SDK_BASELINE.md` | 厂商 SDK、采购型号与能力证据基线 | SDK、型号、能力或厂商 Demo 证据变化时 |
| `Android-apk/docs/wearable/RWFIT_DEVICE_QA.md` | RWFit 真机安装、采集与证据清单 | RWFit 构建开关、指标映射或真机结果变化时 |
| `Android-apk/docs/wearable/HBAND_DEVICE_QA.md` | HBand 真机安装、认证、采集与证据清单 | HBand 构建开关、指标映射或真机结果变化时 |
| `backend/contracts/openapi/rehealth-mobile-v1.openapi.json` | 公共移动 API 机器可读契约 | 公共 API 字段或路径变化时 |
| `backend/contracts/openapi/rehealth-admin-v1.openapi.json` | 官网管理后台患者只读 API 契约 | 后台路径、权限、租户隔离、筛选或返回字段变化时 |
| `backend/contracts/adrs/` | 跨服务架构决策 | 权威边界、消息系统、数据库或信任模型变化时 |
| `backend/docs/REHEALTH_DB_SCHEMA.md` | Room、MySQL 与 TimescaleDB 数据库结构总览及 211 张表逐表附录入口 | Room/MySQL/TimescaleDB Schema、索引、关系或字段语义变化时 |
| `backend/deploy/rehealth/README.md` | 部署拓扑和运行方式 | 服务、端口、环境变量、secret、容器变化时 |
| `model-service/docs/API_CONTRACT.md` | 模型服务接口 | 模型请求/响应、版本或就绪语义变化时 |
| `rehealth-algorithms/docs/RHI_V2_ALGORITHM_PLAN.md` | RHI v2 双轨模型、32 维协议、迁移与验证门禁 | RHI 字段、评分、证据或上线阶段变化时 |
| `tools/dev-tunnel/README.md` | 真机联调公网通道（SSH 反向隧道 + ECS nginx） | 隧道链路、域名、ECS 侧配置或自启方式变化时 |
| `QA_TEST_PLAN.md` | QA 范围 | 用户行为、硬件能力和发布门禁变化时 |
| `RELEASE_CHECKLIST.md` | 发布条件 | 新权限、新数据类型、新依赖或新运行时风险出现时 |
| `docs/archive/acceptance/` | 历史阶段验收快照 | 仅归档已失效快照；当前结论写入 `STATUS.md` |

## 10. 文档同步规则

任何实现变更在完成前必须执行一次文档影响检查：

| 变更类型 | 必须同步的文档 |
| --- | --- |
| 新增/删除/重命名模块或服务 | 根 `README.md`、部署 README、相关 ADR |
| 修改 API 路径、认证、DTO 或完成语义 | 集成契约、OpenAPI、模块 README、相关测试 |
| 修改 Room/MySQL/TimescaleDB Schema | 数据说明、迁移文件、相关 QA 文档 |
| 修改 BLE 协议、指标、设备能力或采集策略 | Android README、集成契约、遥测同步计划、QA 计划 |
| 修改 Kafka Topic、事件 Schema 或消费语义 | ADR-002、事件 Schema、部署 README |
| 修改模型输入、输出、版本或 Mock 策略 | model-service 契约、模型治理文档、集成契约 |
| 修改环境变量、端口、secret 或部署拓扑 | 部署 README、`.env.example`、根 README |
| 修改用户可见行为 | QA 测试计划、发布检查清单 |

如果一次变更不需要更新文档，任务或 PR 的验证说明中应明确写出“文档无影响”及
原因。禁止只修改实现而让根 README、接口契约和部署说明长期失真。
