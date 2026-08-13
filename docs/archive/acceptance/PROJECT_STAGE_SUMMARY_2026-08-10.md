# ReHealth AI 项目阶段总结（2026-08-10）

> 文档性质：阶段汇报快照。本文记录 2026-08-10 对代码、配置、提交和本机运行状态的核查结果；后续状态以根目录 `STATUS.md` 为准。
>
> 评估基线：分支 `codex/real-device`，提交 `1b0c0951 feat(auth): integrate Aliyun SMS verification`。评估时本地分支较远端领先 1 个提交；云米厂商资料目录存在未跟踪文件，未纳入本次代码结论或提交。
>
> 结论口径：只有代码、契约、迁移、测试或运行证据可验证的能力标记为“已实现”；只有接口声明或局部代码、但闭环未通的能力标记为“部分实现”；没有实现证据的能力标记为“未实现”。

# 一、项目基本信息

- 项目名称：ReHealth AI / 睿禾健康
- 项目目标：面向可穿戴设备与健康干预场景，完成“账号与健康访谈 → HBand/云米设备接入 → 本地优先采集与持久化 → 离线同步 → CVD 16 维风险评分 → 个性化干预 → 用户反馈与趋势”的 MVP 闭环。
- 技术架构：Android Kotlin/Jetpack Compose + Room/WorkManager/Foreground Service；JeecgBoot/Spring Boot + MySQL；独立 Device Service/Spring Boot + TimescaleDB + Transactional Outbox + Kafka；FastAPI model-service + CatBoost/SHAP；LangChain4j 健康问答与视觉/干预；Vue3/Ant Design Vue 管理前端；Docker Compose + Nginx/Gateway + Nacos + Redis + Prometheus/Grafana。
- 当前开发阶段：功能开发后期、系统集成与发布验收阶段。核心代码骨架和多数业务功能已形成，但真实设备、真实模型、完整遥测链路和发布级端到端证据尚未闭合。
- 当前版本状态：待发布 Android 版本为 `1.0.0 (versionCode 1)`；仓库 `STATUS.md` 明确标记为 **BLOCKED**。签名 APK/AAB 已有历史构建证据，但不等同于本次发布门禁完成。

## 评估范围与信息完整性

本次实际检查了：

- 根目录 `README.md`、`ENGINEERING.md`、`STATUS.md`、`QA_TEST_PLAN.md`、`RELEASE_CHECKLIST.md`；
- Android 源码、Room v16 Schema、Compose 页面、Retrofit API、厂商 SDK 与测试；
- Jeecg ReHealth 模块 Controller、Service、Repository、MySQL 迁移和短信实现；
- Device Service API、TimescaleDB V1–V4 迁移、Outbox/Kafka 和测试；
- model-service API、模型门禁、测试和本机健康状态；
- Vue3 管理端源码、Docker Compose、环境变量样例、当前进程/端口/容器、Prometheus targets；
- 最近 8 条提交记录和当前 Git 工作区。

仍缺少以下信息或外部证据：

- 经审核 CVD 模型制品、校准报告、外部验证标记和生产哈希；
- 生产数据库容量、备份恢复、灾备、故障切换与压测报告；

# 二、已完成内容

## 1. 用户与权限模块

- 已完成功能：JeecgBoot 账号登录、注册、Token 认证、当前用户资料读写、健康访谈、用户/租户隔离、设备绑定归属、401 会话清理；内部服务通过独立身份授权接口校验设备与用户关系。
- 核心实现方式：移动端只提交业务数据，不允许通过请求体声明数据所有者；后端从认证上下文解析用户/租户。Room v15/v16 将测量、睡眠、活动、信号数据扩展为用户和设备作用域；退出登录清理内存态并停止设备采集。
- 短信注册：开发模式可使用固定测试码；生产实现已切换为阿里云号码认证 `Dypnsapi` 的 `SendSmsVerifyCode`/`CheckSmsVerifyCode`。Redis 只保存发送会话、冷却、频控和注册锁，不保存生产验证码明文；配置缺失时失败关闭。
- 涉及技术：JeecgBoot 权限体系、JWT/Token、Redis、Spring Boot、Android 加密首选项、Room 用户隔离。
- 当前完成度：**部分完成**。代码与 10 个短信专项单测通过；并发注册、生产签名/凭据和移动端安全签名方案仍需环境验收。

## 2. 业务功能模块

### 2.1 设备选择与绑定

- 功能描述：Release 面向用户提供 HBand蓝牙设备与云米云端手表两类接入；Debug 保留 MRD/RWFit/Mock 工程入口。
- 实现流程：`productCode` 选择单一 Provider → 加密保存唯一绑定 → 切换时暂停采集并断开旧 Provider → 恢复当前 Provider。云米由 App 输入 IMEI，后端验证并保存哈希绑定。
- 后端接口：`POST /rehealth/mobile/devices/bind`、`POST /rehealth/mobile/viomi/bind`。
- 数据表设计：MySQL `rehealth_device_binding`；Android 加密绑定首选项；Room 遥测表保存 `owner_user_id`、`device_id`。
- 前端页面：Compose `DeviceBindingScreen`；正式设备选择仅展示 HBand 与云米。
- 完成状态：**已实现**。

### 2.2 可穿戴采集与本地持久化

- 功能描述：HBand 接入心率、步数/活动、睡眠、血氧、血压、血糖、ECG、血液/身体成分及部分设备设置；云米回填心率、血压、血氧。
- 实现流程：Provider/SDK → Repository 标准化 → Room 先写入 → 创建上传任务；后台采集使用 Foreground Service，恢复与同步使用 WorkManager。BLE 采集不依赖网络。
- 数据表设计：Room v16 包含 `ring_measurements`、`ring_sleep_sessions`、`ring_activities`、`ring_signal_chunks`、`sync_upload_queue` 等，并提供 6→16 的显式迁移及导出 Schema。
- 前端页面：`HomeScreen`、`DataScreen`、`EcgDetailScreen`、`ProfileScreen`、设备绑定页。
- 完成状态：**已完成**。本地优先架构、迁移和单测较完整。ECG 仅本地保存、不上传原始波形，是合理的隐私边界。

### 2.3 云米历史同步

- 功能描述：首次绑定最多回填 31 天，后续使用 2 天重叠窗口做增量同步；仅接受有效的心率、血压、血氧。
- 实现流程：Android 请求后端 → JeecgBoot 使用服务端云米凭据拉取 → 先走硬件入库端口持久化 → 返回标准化记录 → Android 幂等写 Room；云端来源不再次进入本地上传队列。
- 后端接口：`POST /rehealth/mobile/viomi/sync`、厂商主动回调 `POST /rehealth/viomi/report`。
- 数据表设计：硬件遥测表/TimescaleDB 目标表；Room `ring_measurements` 用户/设备作用域。
- 前端页面：设备绑定页、数据页。
- 完成状态：**已完成**。

### 2.4 遥测上传、对账与事件发布

- 功能描述：批量上传测量、睡眠、活动、饮食，支持幂等收据、事务落库、质量事件、对账和 Outbox/Kafka 发布。
- 实现流程：Room 队列 → WorkManager → Gateway → Device Service → TimescaleDB 事务写入批次/遥测/对账/Outbox → Kafka → Jeecg 运营投影。
- 后端接口：`POST /rehealth/mobile/measurements/batch`、`GET /rehealth/mobile/measurements/recent`，以及 Device Service 内部运营上下文接口。
- 数据表设计：TimescaleDB `hardware_upload_batch`、`hardware_measurement`、`hardware_sleep_session`、`hardware_activity`、`hardware_data_quality_event`、`hardware_reconciliation`、`hardware_outbox`、`hardware_diet_record` 等。测量类数据默认 7 天后压缩、默认保留 730 天；信号元数据 90 天、运营历史 1095 天、已发布 Outbox 30 天。
- 前端页面：队列状态横幅、数据页、餐食录入与反馈状态。
- 完成状态：**已完成**。

### 2.5 CVD 16 维风险评分

- 功能描述：Android 从资料、可穿戴及确认输入提取 CVD 16 维特征，经 JeecgBoot 调用 model-service，返回风险、模型版本、质量警告和贡献证据。
- 实现流程：Room/档案 → `HealthFeatureExtractor` → `POST /rehealth/mobile/features/evaluate` → model-service `/v1/cvd/risk/evaluate` → MySQL 持久化 → Android 风险/模型页面。
- 后端接口：`POST /rehealth/mobile/features/evaluate`、`GET /rehealth/mobile/risk/latest`。
- 数据表设计：MySQL `rehealth_cvd_feature_vector`、`rehealth_cvd_risk_result`、`rehealth_model_request_log`；Room 风险历史。
- 前端页面：`DataScreen`、`ModelScreen`、`AttributionScreen`。
- 完成状态：**已完成**。

### 2.6 RHI/RDI 健康指数与趋势

- 功能描述：Android 本地 RHI Lite 生成 RHI-100 及日快照；保留独立 RDI 透明规则、周期聚合、趋势和情景模拟骨架；缺失值不补造正常值。
- 实现流程：Room 可穿戴数据 + 用户资料 + 经确认临床输入 → 本地确定性引擎 → Room 四类 RHI 日快照/RDI 快照 → UI 周期聚合 → 离线队列上传 RHI 日快照。
- 后端接口：已实现 `POST /rehealth/mobile/rhi/evaluate-series`、GET/PUT 手填指标；Android 已声明 `POST /rehealth/mobile/rhi/daily-snapshot`。
- 数据表设计：Room `rhi_daily_health_index`、`rhi_daily_domain_score`、`rhi_daily_feature_snapshot`、`rhi_data_quality_snapshot`、`rdi_daily_snapshots`、`rdi_contribution_records`、`rhi_manual_health_inputs` 等；MySQL 仅有手填指标表，未找到 RHI 日快照服务端表/实现。
- 前端页面：数据页、归因页、健康档案页。
- 完成状态：**部分完成**。本地计算、持久化和队列已实现；JeecgBoot 缺少 `daily-snapshot` Controller/落库，当前请求会 404 并进入死信队列。RHI 的研究预览属性与正式用户展示口径也需产品/医疗治理复核。

### 2.7 个性化干预与反馈

- 功能描述：后端重新读取权威画像、最新访谈/风险及 Device Service 今日/近 7 日行为数据，经 LangChain4j 生成 1–5 条结构化保守行动；用户反馈先本地排队再同步。
- 实现流程：显式点击生成 → JeecgBoot 装配服务端上下文 → LangChain4j → MySQL 保存计划 → Android 展示 → 反馈队列 → 后端持久化。
- 后端接口：`POST /rehealth/mobile/interventions/generate`、`GET /interventions/today`、`POST /interventions/{id}/feedback`。
- 数据表设计：`rehealth_intervention_plan`、`rehealth_intervention_contraindication`、`rehealth_intervention_feedback`；Room `intervention_feedback_queue`。
- 前端页面：`AttributionScreen`、干预计划卡、反馈 ViewModel。
- 完成状态：**部分完成**。结构化链路和安全边界已实现；当前 Device Service 未运行、真实模型未就绪，完整上下文生成与反馈 E2E 未验证。

### 2.8 健康问答与个人资料更新

- 功能描述：Android 用户消息先写 Room，再调用服务端；JeecgBoot 默认使用 Java LangChain4j，按当前认证用户装配有界历史、资料、访谈、风险和干预，并可从明确自述中更新姓名、性别、年龄、身高、体重。
- 后端接口：`POST /rehealth/mobile/agent/messages`、`GET /agent/conversations/latest`。
- 数据表设计：Room `health_chat_conversations`、`health_chat_messages`；MySQL `rehealth_ai_conversation`、`rehealth_ai_message`。
- 前端页面：`HealthChatScreen`、首次健康访谈、首页语音转文字入口。
- 完成状态：**已完成**。

### 2.9 拍照识别与餐食记录

- 功能描述：系统相机拍摄后在应用私有缓存中等待写稳、纠正方向、缩放和重编码，上传至 JeecgBoot 进行食物分析/OCR；只持久化结构化结果，不保存原图。有效 FOOD 结果幂等写入 Room 餐食并进入遥测队列。
- 后端接口：`POST /rehealth/mobile/behavior-records/analyze-photo`、`GET /behavior-records/today`。
- 数据表设计：MySQL `rehealth_behavior_record`；Room `diet_records`；TimescaleDB `hardware_diet_record`。
- 前端页面：`HomeScreen`、`BehaviorRecordViewModel`、`DietEntryCard`、归因页餐食录入。
- 完成状态：**已完成**。

### 2.10 管理端/医生端

- 功能描述：仓库保留 JeecgBoot Vue3 通用管理前端，可作为账号、权限和通用后台基础。
- 后端接口：Device Service 已提供内部运营状态、用户健康摘要和干预上下文；Kafka 投影表已设计。
- 数据表设计：`rehealth_telemetry_event_projection`、`rehealth_telemetry_quality_case` 等。
- 前端页面：**未找到 ReHealth 专属设备质量、RHI、风险、干预、医生随访页面源码证据**。
- 完成状态：**未实现/证据不足**。现阶段不能把通用 Jeecg 管理框架等同于业务运营工作台。

## 3. 技术能力建设

### 缓存设计

- Redis：短信发送会话、60 秒冷却、手机号/IP 频控、注册锁；健康问答限流；Jeecg 基础缓存。
- Android：加密首选项保存绑定与必要画像缓存；Room 是本地权威存储，不应被当作可丢弃缓存。
- 当前不足：没有发现统一的业务查询缓存策略，现阶段数据规模下不是 P0；后续应优先保证一致性和监控，再评估热点缓存。

### 数据库设计

- Android Room v16：显式迁移、用户/设备作用域、队列、风险/RHI/RDI/餐食/聊天等本地表，整体符合离线优先要求。
- software_db/MySQL 8：账号体系之外，保存档案、访谈、设备绑定、风险、干预、反馈、归因、AI 会话、行为记录、手填健康指标和运营投影。
- hardware_db/TimescaleDB：规范化遥测、质量、对账、Outbox、餐食 hypertable，具备压缩和保留策略。
- 当前不足：Jeecg 中仍保留旧 MySQL hardware 表及直接写路径，目标 TimescaleDB 权威路径尚未完成切流和迁移验收。

### 消息队列

- 已实现 Transactional Outbox、Kafka 发布器、主事件/质量/DLQ Schema、消费者投影与重试设计。
- 当前不足：本次 Kafka 生命周期测试缺 `kafka.bootstrap` 参数；Prometheus 未监控到应用容器；完整投递、重试、DLQ 和消费者幂等门禁未通过。

### AI 能力接入

- model-service：FastAPI 类型化 Schema、CVD 风险、Factor16、模型注册表、超时/断路器、Prometheus 指标、RHI v2 research preview。
- JeecgBoot：LangChain4j 健康问答、结构化干预、视觉识别，Provider 凭据仅在服务端。
- PIAS：独立个体归因边界，Android 不执行生产归因。
- 当前不足：真实 LLM/视觉 Provider 的稳定性和成本未形成验收报告。

### 文件处理

- 手机照片使用应用私有临时文件，纠正方向、限制长边、重编码后上传；服务端不保存或记录原图，仅保存验证后的结构化结果。
- 模型制品通过只读挂载、哈希和外部验证文件管理，不提交 Git。
- 当前不足：医院报告 OCR 自动写入健康档案未实现；通用文件存储/生命周期不是当前 MVP 能力。

### 第三方接口

- HBand 固定 SDK、JieLi/Nordic/JNI 依赖；Debug MRD/RWFit SDK。
- 云米 OpenAPI 与主动上报回调。
- 阿里云号码认证 Dypnsapi。
- OpenAI-compatible/DeepSeek 等 LLM 与视觉 Provider，通过配置和 secret file 接入。
- Let's Encrypt HTTPS 联调域名及 SSH 反向隧道。
- 风险：厂商 SDK、云端 API、签名模板和外部模型均是关键依赖，需要版本锁定、SLA、降级和合同/合规证据。

### 部署能力

- Compose 定义 Edge、Gateway、JeecgBoot、Device Service、MySQL、TimescaleDB、Kafka、Redis、Nacos、model-service、PIAS、Admin Web、Prometheus、Grafana。
- 配置已大量改为环境变量与外部 secret 文件，正式环境禁止将凭据写入 APK、YAML、`.env` 或镜像层。
- 当前不足：本机实际只以容器运行基础设施，Jeecg/model-service 运行在宿主机，Device Service/PIAS 未运行；Compose 全拓扑和监控网络没有形成一致运行态。

# 三、当前项目架构分析

## 系统架构

目标生产数据流：

```text
Android Compose
  → Room 本地持久化 / durable queue
  → Edge / Gateway
  ├─→ JeecgBoot ReHealth
  │    ├─→ software_db / MySQL
  │    ├─→ model-service / CatBoost + SHAP
  │    ├─→ LangChain4j / LLM + Vision Provider
  │    └─→ PIAS 归因服务
  └─→ Device Service
       ├─→ TimescaleDB
       └─→ Transactional Outbox → Kafka → Jeecg 运营投影

Redis：短信会话、频控、注册锁和平台缓存
Nacos：服务配置/注册
Prometheus + Grafana：应用与基础设施监控
```

当前本机实际运行流：

```text
宿主机 JeecgBoot :8080 → software_db / hardware direct-write 配置
宿主机 model-service :8000 → liveness 200、readiness 503
基础设施容器 → MySQL / TimescaleDB / Kafka / Redis / Nacos / Prometheus / Grafana
Device Service :8081 → 未运行
PIAS → 未发现运行实例
Prometheus 应用 targets → 全部 down
```

## 技术选型分析

### 为什么选择这些技术

- Kotlin + Compose：适合 Android 新 UI 和状态驱动开发，便于快速迭代设备/健康页面。
- Room + WorkManager + Foreground Service：契合“先落本地、断网可用、后台采集、恢复重试”的设备场景。
- JeecgBoot + Vue3：复用成熟账号、权限、配置和通用管理能力，降低后台基础建设成本。
- 独立 Device Service + TimescaleDB：将高频硬件时序数据与业务主数据分离，TimescaleDB 更适合时间分区、压缩和保留策略。
- Transactional Outbox + Kafka：避免“数据库已写、事件未发”的双写不一致，支撑异步运营投影和后续扩展。
- FastAPI + Python 模型服务：贴近 CatBoost/SHAP 生态，便于模型版本治理和独立扩缩容。
- Redis/Nacos/Prometheus/Grafana：分别承担状态协调、配置/注册和可观测性。

### 当前方案优点

- 服务边界清晰：Android 不承载 CatBoost/SHAP/LLM/生产归因，JeecgBoot 不直接执行模型推理。
- 离线优先和数据安全原则正确：采集先持久化，网络故障不阻塞 BLE。
- 数据分层合理：软件业务 MySQL、硬件时序 TimescaleDB、本地 Room。
- Mock 边界和模型 trace 较完善，能够避免把缺失模型伪装为真实生产能力。
- API、Schema、迁移、QA 和发布文档数量较完整，关键链路已有自动化测试。

### 存在不足

- 目标架构与当前运行架构不一致，Device Service/Timescale 权威切流尚未完成。
- 模型服务“存活但不就绪”，真实核心 AI 能力仍受制品阻塞。
- 管理端主要停留在通用框架，运营/医生业务闭环明显弱于移动端。
- Compose、厂商 SDK Gateway、ViewModel 和 Repository 出现超大文件，后续迭代与回归成本上升。
- 依赖厂商 SDK、云米、短信、LLM/视觉等多类外部服务，环境矩阵和故障模式复杂。

# 四、当前存在的问题

## 1. 功能问题

### P0-1 真实设备发布验收未完成

- 问题：HBand 在 Android 13+ 的扫描、首次绑定、锁屏长时间采集、重连、准确性、功耗、温升和完整 ECG 运行时仍缺验收证据。
- 影响：核心数据源可靠性无法证明，签名 APK 不能进入正式试点；错误或缺失健康数据会继续影响风险与干预。
- 原因分析：厂商 SDK/JNI/固件组合复杂，自动化无法替代物理设备测试，当前开发重心先完成了代码路径和页面。
- 解决方案：按 `HBAND_DEVICE_QA.md` 固定型号/固件/SDK/APK/commit，执行 8–24 小时锁屏、断网、重连、重复同步、功耗和参考设备对比；将原始证据与发布包哈希绑定。
- 优先级：**P0**。

### P1-1 RHI 日快照上传后端未实现

- 问题：Android 已声明并排队 `POST /rehealth/mobile/rhi/daily-snapshot`，契约也描述了落库行为，但 JeecgBoot Controller 和服务端表中未找到实现。
- 影响：RHI 队列项会 404/重试/进入死信，管理端无法获得日快照，产生持续无效流量和“客户端看似完成、云端实际缺失”的假闭环。
- 原因分析：客户端、契约和后端交付不同步，Definition of Done 未在接口实现层闭合。
- 解决方案：在 JeecgBoot 新增认证端点、DTO 校验、按 `(user_id, scored_on)` 幂等 upsert、MySQL 迁移、契约测试和 Android 回放测试；上线前提供旧死信重放方案。
- 优先级：**P1**，若管理端 RHI 是本期目标则提升为 P0。

### P1-2 ReHealth 专属管理/医生页面未实现

- 问题：存在通用 Jeecg Vue3 管理端，但未找到 ReHealth 设备质量、用户 RHI、风险、干预、反馈、会话和异常队列页面。
- 影响：运营无法发现数据断流、死信、模型降级和高风险用户；试点高度依赖开发人员查库。
- 原因分析：开发资源集中在 Android 与后端纵向链路，管理端业务需求未形成明确页面/权限/接口清单。
- 解决方案：先实现最小运营台：设备在线/最后同步、队列与死信、模型就绪/版本、用户风险/RHI趋势、干预/反馈、数据质量告警；按医生/运营/管理员做字段脱敏和权限隔离。
- 优先级：**P1**。

### P1-3 健康问答会话管理不完整

- 问题：后端只有“最新会话”恢复，缺少会话列表、删除/保留策略；本机会话列表、新建、切换、墓碑删除未完成验收。
- 影响：用户无法有效管理历史，对隐私删除预期和跨设备一致性说明不足。
- 原因分析：当前只实现最短纵向会话闭环，生命周期与数据治理未纳入首轮 API。
- 解决方案：定义服务端会话列表、归档/删除/保留契约；明确本地删除是否影响云端；增加跨账号、跨设备和墓碑同步测试。
- 优先级：**P1/P2**。

### P2-1 医院报告 OCR 自动入档未实现

- 问题：拍照 OCR 能返回结构化行为结果，但医院报告 OCR 自动写入经确认健康档案未实现。
- 影响：血脂/HbA1c/eGFR 等高价值字段仍依赖手工录入，RHI/CVD 特征完整度有限。
- 原因分析：医疗报告需要单位、日期、置信度和用户确认，不能直接复用食品识别链路自动落库。
- 解决方案：设计“识别草稿 → 字段/单位/日期校验 → 用户逐项确认 → 保存”流程，禁止 OCR 结果未经确认直接参与风险计算。
- 优先级：**P2**。

## 2. 技术问题

### P1-4 集成测试环境不可用

- 问题：本次 Device Service 22 个测试中 19 通过、3 个错误；Timescale/Testcontainers 无法获得有效 Docker API，Kafka 生命周期缺 `kafka.bootstrap` 参数。
- 影响：V4 饮食 hypertable、混合批次事务回滚、真实 Kafka 生命周期无法作为本次发布证据。
- 原因分析：Docker Desktop CLI 能列出已有容器，但 Testcontainers 通过 npipe 获取到不完整 daemon 信息；测试命令未注入 Kafka QA 参数。
- 解决方案：修复 Docker Desktop/Testcontainers npipe 或在 Linux CI 运行；使用仓库 Kafka QA Compose 与 gate 脚本提供 bootstrap/凭据；将集成测试与纯单测分 profile，失败原因保持显式。
- 优先级：**P1**。

### P1-5 可观测性拓扑与实际运行方式脱节

- 问题：Prometheus 的 Device Service、JeecBoot、model-service、PIAS 四个 targets 全部 down，错误为容器 DNS 名不存在；而 JeecBoot/model-service 实际运行在宿主机。
- 影响：没有应用指标、错误率、延迟、模型降级和队列堆积告警，故障只能人工发现。
- 原因分析：只启动了 local-infra 容器，Prometheus 仍使用全容器拓扑的服务名。
- 解决方案：统一选择“全 Compose”或“宿主机应用 + host.docker.internal”开发拓扑；为两种模式分别生成 Prometheus 配置并增加 topology gate。
- 优先级：**P1**。

### P1-6 RHI 研究属性与正式展示口径存在治理冲突

- 问题：工程总纲要求 `research_preview_not_clinically_validated` 不进入生产 UI，但 Android 当前正式数据页展示本地 RHI-100 健康指数。
- 影响：产品文案、医学含义、用户预期和合规边界可能不一致；后续模型升级也可能造成指标不可比。
- 原因分析：本地透明 RHI Lite 与云端 RHI v2 research preview 的命名、版本和发布等级没有完全拆清。
- 解决方案：由产品、算法、医学与合规共同确认：若仅为生活方式指数，单独命名、版本化并展示非临床说明；若沿用 RHI v2，则必须完成验证和 production 状态门禁。
- 优先级：**P1**。

### P2-2 超大类与遗留骨架增加维护风险

- 问题：`RealHBandSdkGateway.kt` 约 1744 行、`AttributionScreen.kt` 约 1521 行、`RhiLiteEngine.kt` 约 1126 行、`RingViewModel.kt` 约 1104 行、`DataScreen.kt` 约 1069 行、`JdbcSoftwareDbReHealthBusinessRepository.java` 约 1054 行。Room 还保留仅定义未使用的 `HealthRecordEntity`/`AttributionLogEntity`，Jeec 保留 pending repository 骨架。
- 影响：厂商协议、业务规则、状态编排和 UI 修改容易相互影响，代码评审和回归成本高；遗留类型会误导维护者。
- 原因分析：纵向 MVP 快速迭代导致职责持续堆叠，兼容历史原型时缺少清理窗口。
- 解决方案：在功能冻结后按“SDK command/mapper/history/ECG”“screen sections + state holder”“RHI domains”“repository per aggregate”拆分；用引用扫描和迁移策略删除未使用骨架。
- 优先级：**P2**。

### P2-3 外部依赖和二进制 SDK 风险集中

- 问题：正式 APK 依赖 HBand 本地 AAR、JieLi/Nordic/JNI；业务还依赖云米、短信、LLM/视觉和证书/隧道。
- 影响：任一厂商变更都可能造成类加载、ABI、协议、配额、合规或可用性问题。
- 原因分析：软硬件一体化业务天然依赖多供应商，目前缺统一依赖清单、SLA 和兼容矩阵证据。
- 解决方案：建立 SBOM、SDK/固件/ABI 矩阵、供应商变更流程、生产配额与故障降级策略；每次发版执行 Release APK 内容审计。
- 优先级：**P2**。

## 3. 工程问题

### P1-7 缺少持续集成流水线

- 问题：仓库未发现 `.github/workflows`、GitLab CI、Jenkinsfile 或其他主 CI 配置。
- 影响：Android、Java、Python、契约、迁移和拓扑门禁依赖人工执行，容易出现“某模块通过、整体未验证”的状态漂移。
- 原因分析：项目仍处于本地集成阶段，门禁脚本和文档已有，但未接入统一执行平台。
- 解决方案：建立分层 CI：PR 快速单测/静态检查；主干契约与构建；夜间 Testcontainers/Kafka/Android emulator；发布流水线生成签名包、SBOM、哈希和验收清单。
- 优先级：**P1**。

### P1-8 发布检查表未形成可追溯关闭记录

- 问题：`RELEASE_CHECKLIST.md` 仍保留大量未勾选项，历史构建通过记录与当前 commit、环境和制品没有统一关联。
- 影响：无法一眼判断某个发布候选是否完成全部门禁，签名包、代码、配置和测试可能不是同一版本。
- 原因分析：状态文档记录了阶段结果，但缺面向单次 Release Candidate 的自动化证据包。
- 解决方案：每个 RC 生成不可变 evidence manifest，记录 Git SHA、APK/AAB 哈希、证书指纹、配置摘要、自动化/真机结果和例外审批；清单只对该 RC 勾选。
- 优先级：**P1**。

### P2-4 配置矩阵复杂且本地/容器模式容易漂移

- 问题：存在多套 Jeec profiles、多个 Compose、宿主机启动脚本和大量 `REHEALTH_*`/`JEECG_*` 环境变量；本次已出现运行方式与 Prometheus 配置不匹配。
- 影响：环境问题可能被误判为代码问题，生产 secret/URL/开关配置错误的概率上升。
- 原因分析：单体/微服务、本地/容器、Debug/Release、Mock/真实 Provider 多维组合并存。
- 解决方案：收敛为明确的 local-infra、local-full、staging、production 四套 profile；用 schema 校验 `.env`；启动前运行 `verifyPublishConfiguration` 与 topology gate，并输出不含秘密的配置摘要。
- 优先级：**P2**。

# 五、代码质量分析

## 目录结构是否合理

总体合理。Android、Device Service、JeecgBoot、契约、模型服务、算法研究和部署目录边界明确，根 README 能解释核心数据流。需要继续避免把研究算法直接带入 Android/Jeecg 生产路径。

## 模块划分是否合理

宏观模块划分正确，尤其是硬件时序与业务库分离、模型独立服务、Android 本地优先。当前主要问题不是顶层拆分，而是模块内部超大文件与目标架构/现运行态并存。

## 是否符合开发规范

优点包括：显式 Room/Flyway 迁移、类型化 DTO、认证上下文确定所有权、模型版本/Mock 标记、原始健康数据和凭据不入日志、保守医疗文案、Release Mock 门禁。缺口是 CI、集成门禁、真机证据和单次 RC 追溯。

## 是否存在重复代码

未进行全仓复制粘贴相似度扫描，因此不能断言不存在重复实现。已确认的“概念重复”包括：Jeec 直接硬件写入与 Device Service/Timescale 目标路径并存、旧 MySQL hardware Schema 与新 Timescale Schema 并存、RDI/RHI/Factor16/CVD 多套指标需持续防止语义混用。

## 是否存在潜在 Bug

- 已确认：RHI `daily-snapshot` 客户端/契约存在但后端缺实现，会产生 404/死信。
- 已确认：模型 liveness 200 但 readiness 503；只看 `/health` 会误判可发布。
- 已确认：Prometheus targets 全部 down，应用异常不会进入现有监控。
- 风险项：旧 `HealthRecordEntity.source` 默认值为 `mock` 且实体仅定义未使用，容易误导后续代码；应清理而非复用。
- 风险项：超大 SDK Gateway/ViewModel/Composable 增加状态竞态、生命周期和回归遗漏概率，但本次未发现新的确定性业务断言失败。

## 是否方便后续扩展

接口和服务边界使新增设备 Provider、模型版本和事件消费者具备扩展基础；但在完成 Device Service 切流、模块内部拆分、管理端和 CI 之前，继续增加设备/指标会显著放大联调矩阵与维护成本。

# 六、测试情况分析

## 本次实际执行（2026-08-10）

|范围|命令/方式|结果|
|-|-|-|
|model-service|`.venv\Scripts\python.exe -m pytest -q`|**71 passed, 1 skipped**|
|Android JVM 单测|`gradlew.bat :app:testDebugUnitTest`|**通过**|
|短信专项 Java 单测|Maven 指定 `AliyunSmsVerification*`、`RegistrationSmsStateTest`|**10 passed**|
|Device Service|`mvn -q -DskipITs test`|**22 项：19 通过、3 环境错误**；2 项 Testcontainers 无有效 Docker，1 项缺 `kafka.bootstrap`|
|运行探针|curl、Docker Compose ps、Prometheus API|model `/health` 200、`/ready` 503；Jeec ReHealth health 200；Device Service 端口未监听；基础设施容器运行；应用监控 targets 全 down|

说明：Device Service 的 `-DskipITs` 未排除以 Surefire 执行的 `*IT`，因此本次仍触发集成测试。3 个错误均为环境/参数问题，不是断言失败，但仍表示发布级集成验证未通过。

## 历史已有测试证据

- `STATUS.md` 记录 Android `testDebugUnitTest`、`assembleDebug`、R8、`lintRelease`、`bundleRelease`、`assembleRelease` 曾通过。
- MuMu API 35 曾完成 Room 迁移、RHI 持久化和相机处理等仪器测试，并安装启动签名 APK。
- MySQL 8 staging 曾有迁移、用户隔离、幂等和重启回读证据。

这些记录是历史证据，不能替代本次 commit 的完整 RC 验证。

## 已发现 Bug/异常

- RHI 日快照后端缺口；
- Kafka 集成测试缺参数；
- Prometheus 四个应用 targets 全 down。

## 未覆盖测试点

- HBand/云米真实设备与云端账号；
- Android 13+ 长稳、锁屏、重连、功耗、温升、准确性和 ECG 全链路；
- 断网采集 → 恢复网络 → Timescale durable write → Outbox/Kafka → 风险 → 干预 → 反馈的完整 E2E；
- 真实短信收码、并发重复注册和频控；
- 真实 LLM/视觉 Provider 的超时、限流、费用和内容安全；
- 生产数据库压测、备份恢复、容灾与保留策略实际执行；
- 管理端权限、脱敏和运营流程；
- 依赖漏洞、SBOM、镜像与签名供应链审计；
- Play Console 内测和生产 HTTPS/代理头最终验收。

# 七、当前项目完成度评估

> 百分比为负责人视角的“可交付完成度”，综合代码、测试、环境和发布证据，不等同于代码量。

|模块| 完成度 | 状态     | 说明                                                           |
|-|--------|----------|----------------------------------------------------------------|
|用户系统与权限| 90%    | 基本完成 | 认证、资料、隔离、短信代码完成 |
|Android 页面与导航| 85%    | 部分完成 | 主要用户页面齐全；会话管理、部分真实状态 QA 未闭合             |
|HBand/云米设备接入| 90%    | 基本完成 | Provider、SDK、IMEI 流程已实现；真实设备/云端验收不足          |
|Room 本地持久化与迁移| 90%    | 基本完成 | v16、显式迁移、用户隔离、队列较完整；仍有遗留实体待清理        |
|离线上传与恢复| 80%    | 部分完成 | 认证感知、退避、401、死信已有；完整云端闭环未验证              |
|Device Service/TimescaleDB| 70%    | 部分完成 | 代码、迁移、Outbox 完成度较高；当前未运行且集成测试未闭合      |
|Kafka 事件与运营投影| 65%    | 部分完成 | Schema/Outbox/消费者存在；真实生命周期门禁未通过               |
|CVD 16 风险评分| 55%    | 基本完成 | 接口、Schema、UI 完成                                          |
|RHI/RDI 指数与趋势| 70%    | 部分完成 | 本地算法、持久化和 UI 已实现；日快照后端缺失且治理口径待确认   |
|个性化干预与反馈| 70%    | 部分完成 | 结构化 LangChain4j 和反馈队列已实现；权威上下文 E2E 未验证     |
|健康问答| 75%    | 部分完成 | 本地先写、服务端上下文和最新会话恢复已实现；会话生命周期不完整 |
|拍照/餐食/OCR| 90%    | 基本完成 | 结构化 FOOD 闭环已实现        |

# 八、项目风险预测

## 技术风险

- 设备协议/SDK 风险：固件、ABI、本地 AAR、JieLi/Nordic/JNI 组合变化可能导致扫描、通知、类加载或 ECG 崩溃。应固定兼容矩阵和回归设备池。
- 数据一致性风险：直接硬件写入与 Device Service/Timescale 目标路径并存时，可能产生双写、漏写和回读差异。应完成 shadow-read、对账和一次性切流。
- 模型治理风险：模型制品缺失或 RHI/CVD/RDI/Factor16 语义混用，可能把研究指标或 Mock 误当临床风险。应通过版本、状态门禁、文案和 UI 数据源审计隔离。
- 安全隐私风险：健康数据、IMEI、手机号、照片、聊天上下文属于敏感数据；需持续执行日志审计、最小权限、保留/删除策略和供应商合规评估。

## 性能风险

- 高频遥测、31 天云米回填、历史恢复和多指标聚合可能造成 Room 主线程压力、批次过大、数据库热点和移动端耗电。
- 结构化 LLM/视觉调用的 75–110 秒超时会占用连接和线程，峰值时容易形成队列堆积。
- Timescale 压缩/保留策略虽已设计，但尚无目标规模压测；Outbox、Kafka consumer lag 和死信没有可用监控。
- 应建立每用户/设备日数据量基线、上传批次上限、服务 P95/P99、数据库容量模型和端侧功耗指标。

## 业务风险

- 真实设备准确性和稳定性若不达标，风险、干预和用户信任会同时受损。
- “健康指数/风险/情景模拟”如果文案边界不清，可能被用户理解为诊断或未来疾病概率。
- 云米、短信、LLM/视觉供应商的账号、配额、审核、价格和 SLA 可能影响注册、同步与核心体验。
- 管理端缺失会使试点无法规模化运营，问题发现依赖研发人工查库。

## 维护风险

- 超大 Kotlin/Java 文件和多套运行 profile 会增加新人上手、评审和回归成本。
- 没有 CI 时，文档、契约、DTO、迁移和实现容易再次失步。
- 研究算法与生产代码同仓，需要持续维护明确的发布边界，防止研究依赖或 Mock 被误打包。
- 供应商二进制 SDK 缺源码，长期升级、安全审计和问题定位能力受限。

# 九、总结

## 当前项目整体评价

ReHealth 已从“Android 演示”推进到具备真实设备 Provider、离线优先 Room、版本化契约、双数据库边界、模型服务、结构化干预与反馈的完整 MVP 工程骨架。架构方向总体正确，隐私、Mock、模型版本和医疗安全边界意识较强，Android/模型服务单测基础也较扎实。

但当前仍属于“代码主链路基本形成、真实生产闭环未验收”的阶段，不能按可发布产品评价。最关键的不是继续扩展更多指标或页面，而是把真实设备、真实模型、Device Service 权威路径、监控和发布证据收口。

## 当前最需要解决的 5 个问题

1. 完成 HBand/云米真实设备、长稳、准确性、功耗和签名 Release E2E 验收。
2. 挂载并验证真实 CVD 模型制品，使 model-service `/ready` 通过且全链路 `is_mock=false`。
3. 启动 Device Service，完成 MySQL→Timescale 对账、Gateway 切流、Outbox/Kafka 和回滚门禁。
4. 实现 JeecBoot RHI `daily-snapshot` 端点/迁移并重放死信，消除明确的 404 断点。
