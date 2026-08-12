# 睿禾精灵 APP 全景说明与技术架构

> 汇报对象：公司管理层、产品负责人、技术负责人
> 项目：ReHealth AI / 睿禾健康
> 客户端：睿禾精灵 Android APP
> 文档版本：V1.0
> 现状基准：2026-08-12 代码仓与当前状态文档
> 发布状态：MVP 功能主体已形成，正式发布仍处于 **BLOCKED（阻塞）** 状态

---

## 1. 管理层摘要

睿禾精灵不是一个单纯展示手环数据的 APP，而是一套“可穿戴设备接入 + 本地健康数据管理 + 云端风险分析 + 个性化干预 + 用户反馈 + 运营管理”的软硬件一体化健康管理系统。当前 MVP 已经形成从用户登录、健康初识、设备采集、手机本地存储、离线上传，到 CVD 心血管风险评估、RHI 健康指数、干预建议、健康问答和反馈记录的主要闭环。

正式客户端当前支持两类设备接入：

1. **HBand MT116 蓝牙设备**：手机通过 BLE 与设备直连，按设备真实能力采集心率、步数/活动、睡眠、血氧、HRV、血压、血糖、压力、MET、单导联 ECG、血液成分和身体成分等数据。
2. **云米云端手表**：支持 S8、S9、GS20、GS17、A67、K9L，用户通过 IMEI 绑定，后端从云米平台拉取心率、血压和血氧历史，再回填到 APP 本地数据库。

系统采用“**本地优先、网络解耦**”原则：设备采集数据必须先写入手机 Room 数据库，再进入持久化上传队列；断网或后端异常不能阻塞蓝牙采集。后端进一步分为硬件时序域与软件业务域，模型推理也独立部署，避免把高频遥测、账号业务和 AI 模型混在一起。

截至本次盘点，项目的核心判断如下：

| 维度 | 当前结论 | 管理层含义 |
| --- | --- | --- |
| 产品完整度 | 主体功能已经形成 | 可以开展系统演示、内部试用和针对性联调 |
| 正式设备 | Release 仅保留 HBand 与云米两类入口 | 产品口径已收敛，不再向正式用户暴露工程设备 |
| 数据闭环 | 本地采集、离线队列、云端遥测、风险、干预、反馈均有实现 | 技术架构已具备 MVP 闭环基础 |
| AI 能力 | CVD-16 风险、RHI、健康问答、视觉食物/OCR、干预编排已分层接入 | AI 不是单点聊天功能，而是嵌入健康管理流程 |
| 发布状态 | **BLOCKED** | 仍缺物理设备长稳、真实生产模型和完整线上闭环验收 |
| 主要风险 | 真机准确性、后台稳定性、生产容量/恢复、部分入口占位 | 当前更适合“受控试点”，不宜直接宣称全面生产可用 |

## 2. 产品定位与业务价值

### 2.1 产品定位

睿禾精灵面向有持续健康管理需求的用户，通过可穿戴设备和健康档案建立个人健康基线，持续观察趋势，提供风险提示和保守的生活方式干预建议。产品定位是“健康管理与风险提示工具”，不是医疗诊断软件，也不替代医生。

### 2.2 核心业务价值

- **连续数据入口**：连接可穿戴设备，形成比单次体检更连续的健康数据观察。
- **离线可靠性**：手机本地先保存，网络恢复后补传，降低采集数据丢失概率。
- **风险分层**：以 CVD 16 项特征评估心血管风险，并明确模型版本和数据质量。
- **健康改善跟踪**：以 RHI-100 表达动态健康指数，以 RDI-16 表达风险指数，两者不混算。
- **个性化干预**：综合档案、访谈、风险、当日行为和近期趋势，生成 1–5 条结构化行动。
- **互动闭环**：用户可通过健康问答、拍照饮食记录和干预反馈持续补充行为证据。
- **运营与保险延展**：后台已具备租户、权限、保险数据导入、PSM/RWE 和结算流程的本地 MVP 基础。

### 2.3 当前标准用户旅程

```text
启动 APP
  → 登录或手机号注册
  → 新用户完成健康初识/访谈
  → 选择 HBand 蓝牙或云米 IMEI 云端设备
  → 采集/同步健康数据并先写入 Room
  → 离线队列上传到后端
  → 生成 CVD-16 风险和 RHI 健康指数
  → 查看风险趋势、16 项贡献和今日行为
  → 用户显式生成个性化干预计划
  → 在首页执行、反馈并继续健康问答
```

## 3. APP 信息总览

| 项目 | 当前信息 |
| --- | --- |
| APP 名称 | 睿禾精灵 |
| Android 包名 | `com.rehealth.genie` |
| 待发布版本 | `1.0.0`，`versionCode 1` |
| 最低系统 | Android 8.0（API 26） |
| 目标系统 | Android API 36 |
| 正式设备入口 | HBand MT116 蓝牙、云米 IMEI 云端 |
| 正式后端地址 | `https://rehealth.youngjimmy.store/jeecg-boot/` |
| 主导航 | 首页、数据、归因、模型、我的 |
| 本地数据库 | SQLite / Room schema v16，22 张表 |
| 账户体系 | JeecgBoot 账号、租户、角色与 JWT 会话 |
| 数据原则 | 先本地持久化，再上传；采集不依赖网络 |
| 医疗定位 | 健康管理参考，不诊断、不处方、不替代医生 |

## 4. APP 全部页面与功能

### 4.1 启动、登录与注册

| 页面/能力 | 用户可见功能 | 当前状态 |
| --- | --- | --- |
| 启动页 | 品牌启动与进入登录 | 已实现 |
| 登录页 | 用户名/密码登录、会话保存、登录后恢复当前用户数据 | 已实现 |
| 注册页 | 手机号、短信验证码、密码、协议确认、注册后自动登录 | 已实现；生产短信真实收码仍待凭据验收 |
| 会话失效 | 401 时暂停队列、清理会话并回登录页 | 已实现 |
| 多账号隔离 | 资料、聊天、测量、睡眠、活动、ECG、餐食、RHI/RDI 按账号隔离 | 已实现并有迁移/测试基础 |

注册短信的本地开发模式可使用固定测试码；生产方案使用阿里云号码认证服务 Dypnsapi，由服务端生成并校验验证码。验证码、AccessKey 和模型密钥均不进入 APK。

### 4.2 健康初识与健康访谈

- 新用户注册后进入健康初识对话，不强制先连接设备。
- 支持文本回答和系统语音转文字；APP 不保存录音。
- 健康初识形成基本资料、基线信息和关注方向，并先写入本地持久化队列。
- 用户明确自述的姓名、性别、年龄、身高和体重会同步合并到类型化健康档案。
- 既有账号不会重复强制首次流程；“我的”页可重新发起健康问答。

### 4.3 首页：小禾灵健康助手

首页是日常互动中心，已经实现：

- 小禾灵健康问答，支持输入框、系统语音输入和安全 Markdown 展示。
- 新建会话、本机会话列表、切换、单个删除和清空本机会话。
- 用户消息先写入按账号隔离的 Room，再调用后端健康助手。
- 快捷操作：拍照记录、健康记录提示、设备数据分析提示。
- 调用系统相机拍摄食物或报告，完成方向校正、缩放和重编码后上传。
- 展示今日拍照行为记录，包括 FOOD、OCR 或其他结构化结果。
- 展示服务端今日干预计划，并支持完成/忽略等反馈。

健康助手默认由 JeecgBoot 中的 Java LangChain4j 链路执行，每轮读取当前认证用户的档案、访谈、风险和干预上下文。Python 对话链路仅保留为可配置回退。后端含诊断、开药和紧急症状安全约束。

### 4.4 设备绑定页

正式用户可以在两种方式之间切换：

#### HBand MT116 蓝牙

- 申请蓝牙权限、扫描设备、连接、断开和同步。
- 连接后保存加密绑定，后台恢复只重连已绑定地址，不进行环境扫描。
- 绑定前不会使用固定 MAC 自动连接。
- 切换设备类型时按“暂停采集 → 断开旧 Provider → 更新唯一绑定 → 恢复采集”执行。
- 不支持多设备同时连接或多设备数据融合。

#### 云米 IMEI 云端

- 输入 IMEI 进行绑定，不申请蓝牙权限。
- 首次最多回填 31 天，后续使用 2 天重叠窗口做增量同步。
- 支持 S8、S9、GS20、GS17、A67、K9L。
- 当前云端返回心率、血压和血氧。

Debug 构建仍保留 Mock、MRD 和 RWFit 工程测试能力；Release 商品目录只包含 HBand 与云米，不包含这些工程入口。

### 4.5 数据页：健康数据总览

数据页支持“今日、7 天、30 天、90 天”周期切换，并从本地 Room 历史重新聚合。主要内容如下：

| 分区 | 指标/功能 | 说明 |
| --- | --- | --- |
| 健康指数 | RHI-100 | 今日取当日有效值；周期值按有效日稳健聚合，数据不足显示积累中 |
| 风险状态 | RDI-16 / CVD 风险 | 只展示真实、范围有效、非 Mock 的云端结果 |
| 生命体征 | 心率、血氧、血压 | 支持按周期汇总；云米仅展示其支持指标 |
| 高级指标 | HRV、压力、MET | 只有真实 Provider 返回有效值才显示；MET 无实时测量按钮 |
| 血糖 | 设备估算值 | 支持设备测量和血糖仪参考值校准，明确非诊断 |
| ECG | 单导联实时/历史波形 | 显示接触提示、进度、校准 mV、导联/采样元数据，仅本地保存 |
| 血液成分 | 尿酸、总胆固醇、甘油三酯、HDL、LDL | 按设备能力展示，均标注设备估算 |
| 身体成分 | BMI、体脂、肌肉、水分、骨量、蛋白、基础代谢等 14 项 | 测量前显示电极接触和姿势说明 |
| 睡眠与活动 | 睡眠、步数、运动/活动 | 支持历史同步和周期聚合 |
| 设备设置 | 血糖校准、女性经期设置 | 按设备能力启用；敏感功能需用户确认 |

重要边界：HBand 体温因当前 MT116 真机验证不通过，已从正式产品能力和数据页移除；无效值、零值、越界值和模拟来源不会生成占位记录。ECG 波形不上传云端，SDK 疾病风险也不作为诊断展示。

### 4.6 归因页：改善、风险与行动

归因页是健康管理闭环的核心展示页，包含：

1. **健康改善得分**：基于 RHI-100 计算所选周期最后一个与第一个有效值的差异。
2. **RDI-16 风险指数**：展示已确认的 CVD 风险结果，按 0–100 分表达。
3. **个人风险趋势**：展示真实风险历史；条件满足时显示“维持现状/执行计划”的 30 日情景轨迹和参考区间。
4. **近期风险变化贡献**：从本地 RDI 规则骨架提取最多 3 项可信影响因素。
5. **手工餐食录入**：记录餐次、内容、热量以及可选蛋白质、脂肪和碳水；先写本地再进入上传队列。
6. **今日行为记录**：汇总设备活动和拍照产生的 FOOD/OCR 记录。
7. **16 项贡献因素**：展示同次 CVD-16 评估输入的规则贡献、字段说明和保守建议。
8. **个性化干预计划**：用户显式点击生成，返回 1–5 条带类别、目标、时机、理由和证据引用的行动。

RHI、RDI-16、Factor16 和 PIAS 是不同概念：RHI 表达整体健康状态；RDI-16 表达当前风险；Factor16 是 16 项输入的透明规则贡献；PIAS 用于个体归因。页面明确情景模拟不是未来疾病发生概率。

### 4.7 模型页

模型页展示设备数据准备、特征提取和个性化学习状态，包括心率、血氧、血压、睡眠和步数等输入状态，以及隐私提示和模型版本文案。

**当前边界**：该页面主要是固定的端侧学习视觉稿，不参与真实风险计算；其中部分“学习比例、反馈次数、最近学习时间”等展示文案不是实时后端事实。真实 CVD 风险仍走 Android 特征提取 → JeecgBoot → model-service 的云端链路。

### 4.8 “我的”页

已实际接线的功能包括：

- 展示当前账号姓名、年龄、BMI、陪伴天数、昨夜睡眠、今日步数和体重。
- 从系统照片选择器设置头像；头像去除元数据后只保存在本机、按用户隔离。
- 查看健康档案、诊断标签、家族史、高血压史、糖尿病史和健康问答摘要。
- 编辑个人资料：姓名、性别、年龄、身高、体重、吸烟、饮酒及相关病史。
- 编辑 RHI/归因指标：久坐、腰围、VO₂max、HbA1c、eGFR、经确认的上臂袖带血压和医院血检。
- 更新健康问答、进入设备绑定、重新体验首次流程、退出登录。

当前仅为可见入口、尚未接入完整业务操作的项目包括：隐私中心、数据导出、数据删除、通知设置、关于页面；“Pro 会员”也是展示标签，不代表已上线会员计费体系。

### 4.9 保险计划能力

保险业务在服务端已经形成本地 MVP：支持投保人、保单、理赔幂等导入，风险工作台，研究项目，不可变快照，PSM 任务，RWE 报告审核/Word 导出，以及结算包状态机。Android 已增加保险计划绑定、当前计划查询和干预反馈的类型化 API 客户端。

**尚未完成**：APP 内保险计划授权/撤回页面、用户可见计划详情和保险反馈离线队列。因此保险能力当前不能作为正式 APP 已交付页面宣传。

## 5. 可穿戴设备与采集能力

### 5.1 正式设备矩阵

| 产品/接入 | 接入方式 | 当前指标 | 状态 |
| --- | --- | --- | --- |
| HBand MT116 | 手机 BLE 直连 | 心率、步数/活动、睡眠、血氧、HRV、血压、血糖、压力、MET、ECG、血液成分、身体成分 | 已接入，完整真机准确性/长稳仍待验收 |
| 云米 S8/S9/GS20/GS17/A67/K9L | IMEI + 云端 API | 心率、血压、血氧 | 已接入，真实云端凭据与全链路仍需复验 |
| MRD | 手机 BLE 直连 | 工程测试指标 | Debug 工程能力，不进入 Release |
| RWFit | 厂商 SDK/BLE | 工程测试指标 | Debug 工程能力，不进入 Release |
| Mock | 本地模拟 | 合成数据 | 仅 Debug/测试，Release 禁止 |

### 5.2 后台采集策略

- 使用前台服务维持已绑定设备采集。
- 使用 WorkManager 执行上传重试和后台恢复。
- 只重连当前加密绑定，不在后台自动扫描周边设备。
- BLE 采集与网络上传解耦；网络失败不停止采集。
- 同一时刻只有一个有效 Provider，前后台操作共享路由互斥。
- 登录恢复会从后端拉取最多 200 条近期规范化记录，幂等回填 Room。

## 6. 系统总体架构

```text
HBand BLE / 云米云端
        │
        ▼
Android APP
Provider 路由 → Room v16 → 离线队列 → WorkManager
        │
        ▼ HTTPS/JWT
Edge / Gateway
   ├───────────────┐
   ▼               ▼
Device Service     JeecgBoot ReHealth
   │               │
   ▼               ├─ MySQL software_db
TimescaleDB         ├─ LangChain4j 健康问答/干预
   │               ├─ model-service（CVD/RHI）
Transactional      └─ PIAS（个体归因）
Outbox → Kafka
        │
        ▼
运营投影 / JeecgBoot Vue3 管理后台
```

### 6.1 服务职责

| 组件 | 主要职责 | 明确不承担 |
| --- | --- | --- |
| Android APP | 设备 SDK/BLE、Room、本地特征、离线队列、用户交互 | CatBoost、SHAP、LLM、生产归因 |
| Edge/Gateway | 公网入口、HTTPS、路由、安全头、服务转发 | 业务数据持久化、模型推理 |
| Device Service | 遥测校验、设备授权、TimescaleDB、幂等、Outbox、行为摘要 | 用户账号、业务档案、模型推理 |
| JeecgBoot | 登录、租户、权限、设备绑定、档案、访谈、风险/干预/反馈编排、业务库 | 高频硬件时序权威、CatBoost/SHAP |
| model-service | CVD-16 风险、模型制品校验、RHI 研究预览、兼容干预接口 | 用户认证、设备接入、业务主数据 |
| PIAS | 生产个体归因 | Android 本地生产归因、静默 Mock |
| 管理前端 | 后台用户、租户、权限、运营与保险业务管理 | 移动端设备采集 |
| rehealth-algorithms | 模型训练、HealthAgent/PIAS 仿真、算法研究 | APP 在线业务权威入口 |

## 7. 三条核心数据流

### 7.1 设备采集与遥测上传

```text
设备数据
  → Provider/Repository 规范化
  → Room 本地持久化
  → durable upload queue
  → WorkManager 重试
  → Gateway / Device Service
  → TimescaleDB 单事务写入批次、测量/睡眠/活动/饮食、Outbox
  → 成功后 APP 才标记队列完成
  → Outbox 发布 Kafka 事件
```

客户端重试使用稳定批次 ID，服务端对重复批次返回已有收据。遥测上传本身不直接触发风险评分。

### 7.2 风险评估与健康指数

```text
Room + 健康档案
  → Android HealthFeatureExtractor
  → CVD 16 维特征 + featureQuality
  → JeecgBoot /features/evaluate
  → model-service /v1/cvd/risk/evaluate
  → 风险结果持久化到 software_db
  → APP 展示 RDI-16、历史和 Factor16 贡献
```

RHI-100 当前主要由 Android 本地透明规则引擎基于可穿戴数据、个人资料和手工健康指标计算。RHI v2 远程复算只用于研究预览，不替代 CVD-16 生产风险。

### 7.3 干预与反馈

```text
用户点击生成计划
  → JeecgBoot 重新读取权威档案、最新访谈和风险
  → Device Service 提供今日行为及近 7 日描述性变化
  → LangChain4j 生成 1–5 条结构化保守行动
  → software_db 持久化
  → APP 展示并收集完成/忽略反馈
  → 本地反馈队列
  → JeecgBoot feedback API
```

服务端不采信客户端提交的画像或风险上下文；任一权威依赖不可用时，不返回伪造的确定性计划。

## 8. 前端与后端组成

### 8.1 Android 前端

- 单 Activity + Jetpack Compose 声明式界面。
- 通过阶段状态管理启动、登录、注册、健康初识和主界面。
- 主界面为五个底部 Tab：首页、数据、归因、模型、我的。
- ViewModel 负责界面状态与流程编排，BLE 细节在 Repository/Gateway 层。
- Retrofit/Moshi 提供类型化 API；OkHttp 负责网络与认证拦截。
- Room 保存 22 张本地表；Foreground Service 和 WorkManager 保障长任务。

### 8.2 JeecgBoot 业务后端

JeecgBoot ReHealth 模块是移动业务编排与软件业务权威，主要包含：

- 移动健康档案、访谈、设备绑定、风险、干预、反馈和归因代理。
- 用户、租户、角色、权限和 JWT 认证。
- 手机号注册与阿里云验证码编排。
- Java LangChain4j 健康问答、视觉食物/OCR 和结构化干预。
- 云米 IMEI 绑定、历史同步和厂商主动上报适配。
- 保险导入、风险工作台、PSM/RWE、报告与结算状态机。
- Kafka 运营/质量投影和最小化模型请求审计。

### 8.3 Device Service

独立 Spring Boot 服务负责高频硬件遥测：

- 接收批量测量、睡眠、活动和饮食数据。
- 调用 JeecgBoot 内部身份接口校验用户、租户和设备绑定。
- 在 TimescaleDB 事务中写入批次、规范化记录、质量事件和 Outbox。
- 向 JeecgBoot 提供按用户、租户、自然日限定的健康摘要与干预上下文。
- Outbox 后台发布 Kafka，支持重试与投递状态。

### 8.4 model-service

独立 FastAPI 服务负责模型边界：

- `/health`、`/ready`、`/metrics` 和活动模型注册信息。
- CVD-16 CatBoost 风险评分和 SHAP/Factor16 解释。
- 模型制品哈希、特征顺序、Schema 和运行模式校验。
- 推理超时、断路器和受控失败。
- RHI v2 32 维研究预览；生产/预发布模式默认禁用。
- 缺少或无效真实模型制品时显式返回 Mock/不可用标记，不能伪装为真实模型。

### 8.5 管理前端与官网/保险端

- `jeecgboot-vue3` 是保留的管理前端，提供动态菜单、租户/角色/按钮级权限、表单、报表和运营管理基础。
- 当前保险官网链路包含 FastAPI BFF/服务端会话与 JeecgBoot 保险 API；浏览器不直接持有后端权威 Token。
- 保险研究和 RWE Word 报告属于本地 MVP 扩展能力，正式生产仍需真实保险数据和独立租户验收。

## 9. 技术栈清单

### 9.1 Android

| 类别 | 技术/版本 |
| --- | --- |
| 语言与编译 | Kotlin 2.2.20、Java 17、Android Gradle Plugin 8.10.1 |
| UI | Jetpack Compose、Material 3、Compose BOM 2024.12.01 |
| 架构 | ViewModel、Repository、Provider/Gateway、单 Activity |
| 本地存储 | Room 2.7.1、SQLite、显式 schema migration |
| 网络 | Retrofit 2.11、OkHttp 4.12、Moshi 1.15、Gson 2.11 |
| 后台任务 | Foreground Service、WorkManager 2.10 |
| 安全存储 | AndroidX Security Crypto、AES256_GCM/SIV |
| 设备 SDK | HBand/VeePoo、JieLi、Nordic；MRD/RWFit 仅工程测试 |
| 构建安全 | Release HTTPS 强校验、R8、外部签名材料、Debug/Release 资源隔离 |

### 9.2 Java 后端

| 组件 | 技术/版本 |
| --- | --- |
| JeecgBoot | 3.9.2、Java 17、Spring Boot 3 系列 |
| 数据访问 | MyBatis-Plus 3.5.12、JDBC、MySQL 8.4.6 |
| 认证权限 | JeecgBoot JWT、Apache Shiro 2.0.5、租户与 RBAC |
| AI 编排 | LangChain4j 1.12.2、OpenAI-compatible Provider |
| 消息 | Spring Kafka、Apache Kafka 4.3.1 |
| 迁移 | SQL/Flyway 风格显式迁移与验证 |
| Device Service | Spring Boot 3.5.5、Spring JDBC、Actuator、Springdoc |

### 9.3 管理前端

| 类别 | 技术/版本 |
| --- | --- |
| 框架 | Vue 3.5、TypeScript 5.9、Vite 7.3 |
| UI | Ant Design Vue 4.2、VXE Table、ECharts 5.6 |
| 状态与路由 | Pinia、Vue Router |
| 工程 | pnpm、ESLint、Prettier、Stylelint |

### 9.4 Python 与算法

| 组件 | 技术/版本 |
| --- | --- |
| model-service | Python、FastAPI 0.116、Uvicorn 0.35、Pydantic 2.11 |
| 模型 | CatBoost 1.2.x、scikit-learn 1.7、joblib |
| 解释与归因 | CatBoost SHAP、Factor16、独立 PIAS |
| 监控 | prometheus-client 0.22 |
| 测试 | pytest 8.4、httpx |
| 算法研究 | CatBoost/LightGBM/XGBoost/Optuna、HealthAgent、RHI/PIAS 仿真 |

### 9.5 基础设施

Docker Compose 拓扑包含：Edge、Gateway、JeecgBoot、Device Service、MySQL、TimescaleDB、Kafka、Redis、Nacos、模型制品校验器、model-service、PIAS、管理前端、Prometheus 和 Grafana。外部密钥通过 secret 文件或受控挂载注入，只有入口层应暴露公网端口。

## 10. API 能力分组

APP 通过统一 `/jeecg-boot/rehealth/mobile` 业务前缀访问后端，主要 API 如下：

| 分组 | 代表接口 | 用途 |
| --- | --- | --- |
| 账号 | `/sys/mLogin`、`/sys/registerSms`、`/sys/user/register` | 登录、短信、注册 |
| 档案 | `GET/PUT /profile`、`GET/PUT /rhi/manual-inputs` | 个人资料和手工健康指标 |
| 访谈/问答 | `/interviews`、`/agent/messages`、`/agent/conversations/latest` | 健康初识和健康助手 |
| 设备 | `/devices/bind`、`/viomi/bind`、`/viomi/sync` | 设备绑定与云米同步 |
| 遥测 | `POST /measurements/batch`、`GET /measurements/recent` | 批量上传和登录回填 |
| 风险/RHI | `/features/evaluate`、`/risk/latest`、`/rhi/evaluate-series` | CVD 风险和 RHI 预览 |
| 干预 | `/interventions/generate`、`/interventions/today`、`/{id}/feedback` | 计划生成、读取和反馈 |
| 行为 | `/behavior-records/analyze-photo`、`/behavior-records/today` | 食物/OCR 拍照记录 |
| 归因 | `/attribution/events` | PIAS 个体归因代理 |
| 保险 | `/insurance/plans/bind`、`/current`、`/{id}/feedback` | 保险计划授权与结果反馈 |

**接口缺口提示**：Android 已实现 `/rhi/daily-snapshot` 的本地入队和类型化上传客户端，但当前 JeecgBoot 控制器代码仍未发现对应落库端点；上传失败会进入重试/死信，不影响本地 RHI 计算。保险接口已有客户端，但 Compose UI 与离线反馈队列尚未完成。

## 11. 数据与数据库

系统不是单库架构，而是三个隔离的数据域：

| 数据域 | 技术与规模 | 权威数据 |
| --- | --- | --- |
| Android 本地库 | SQLite / Room v16，22 张表 | 本地遥测、睡眠、活动、ECG、上传队列、聊天、RHI/RDI、餐食 |
| 软件业务库 | MySQL 8.4.6，182 张表（含 Jeecg 平台表） | 用户、权限、档案、访谈、风险、干预、反馈、问答、保险与运营 |
| 硬件时序库 | PostgreSQL 17.5 + TimescaleDB 2.21.1，11 张表 | 遥测批次、测量、睡眠、活动、饮食、质量、Outbox、对账 |

当前数据库结构总计 215 张基础表，其中 ReHealth 专属业务域表 74 张。Kafka 用于事件传递，Redis 用于短期状态、会话辅助、限流与验证码发送状态，不计入关系表数量。

关键一致性原则：

- 手机采集先写 Room。
- 单个遥测批次在 TimescaleDB 内强事务。
- 软件业务聚合在 MySQL 内强事务。
- 跨 MySQL/TimescaleDB 不做分布式事务，通过事件、状态和重试实现最终一致性。
- 客户端不能通过请求体决定数据所有者；用户和租户来自认证上下文。

## 12. 算法、指标与 AI 边界

### 12.1 CVD-16 风险

Android 从个人资料、经确认临床值和可穿戴摘要提取 16 项特征：年龄、性别、BMI、收缩压、舒张压、空腹血糖、总胆固醇、LDL、HDL、甘油三酯、运动天数、吸烟、饮酒、糖尿病史、高血压史、家族史。每项同时携带有效性、来源和缺失原因。

生产风险评分由 model-service 的 CatBoost 模型完成；Android 和 JeecgBoot Java 不运行生产模型。缺少真实模型制品时必须显式标记 Mock/不可用，正式环境不允许静默降级。

### 12.2 RHI-100 健康指数

RHI 是 0–100 的动态健康状态指标，当前 Android 使用 `rhi-deterministic-preview-2.2.0-android-lite`，综合可穿戴、档案和手工健康指标，输出五个领域分、可信度、数据质量提醒和版本。RHI 不是疾病概率。

### 12.3 RDI 与 Factor16

- RDI 本地规则骨架用于近期可干预负荷和情景模拟，不覆盖 CVD 风险历史。
- APP 中“RDI-16 风险指数”读取已确认 CVD 风险结果。
- Factor16 使用 `factor16-rule-v1.0.0` 展示 16 项透明规则贡献，不等同于模型 SHAP，也不等同于 PIAS 因果归因。

### 12.4 健康问答、视觉识别与干预

- 健康问答默认由 JeecgBoot LangChain4j 执行，按当前用户组装有界上下文。
- 拍照分析支持 FOOD、OCR 和 OTHER；只保存结构化结果，不保存原图。
- FOOD 结果缺少有效热量时不会伪造营养数据。
- 干预建议必须保守，不能诊断、开药或修改药物方案；高风险情况应提示专业医疗帮助。

## 13. 安全、隐私与合规设计

- Release 强制 HTTPS，禁止 cleartext；签名材料和密钥从仓库外注入。
- Token 和设备绑定使用 Android 加密偏好保存；绑定地址、画像缓存按用户隔离。
- 原始健康值、Token、手机号、BLE MAC、原始信号和图片字节不得进入生产日志。
- Android 上传稳定设备标识/摘要，不允许客户端指定数据所有者。
- ECG 原始波形只保存在本地 Room，不进入遥测上传。
- 图片在应用私有临时目录处理，服务端不持久化原图，模型凭据不进入 APK。
- 用户头像只保存在本机，不上传服务器。
- 生产短信验证码不在 Redis 或日志保存明文；AccessKey 仅在服务端 secret 中。
- 生产与 staging 禁止把 Mock 风险、Mock 设备或 Mock 归因伪装成真实结果。
- 所有健康建议需带“仅供健康管理参考，不能替代医生”的边界。

仍需发布前完成的合规工作包括：真实设备 logcat/沙箱复核、生产数据保留与删除策略、备份恢复、依赖漏洞与镜像来源审查，以及隐私中心/数据导出/数据删除的实际产品闭环。

## 14. 部署与运维

### 14.1 当前部署拓扑

| 层级 | 组件 |
| --- | --- |
| 入口 | Edge、Jeecg Gateway、HTTPS 反向代理 |
| 应用 | JeecgBoot、Device Service、model-service、PIAS、管理前端 |
| 数据 | MySQL、TimescaleDB、Redis |
| 消息/配置 | Kafka、Nacos |
| 模型治理 | 模型制品校验器、模型注册与 readiness 门禁 |
| 监控 | Prometheus、Grafana、Spring Actuator |

### 14.2 联调通道

当前公网联调地址为 `https://rehealth.youngjimmy.store`，通过 SSH 反向隧道、ECS nginx 和 Let's Encrypt 证书接入本地/受控环境；备用地址为 `rehealth.47.80.30.228.sslip.io`。该通道可用于 Debug/Release 联调，但不等同于完成生产高可用部署。

### 14.3 构建与运行

| 模块 | 主要命令 |
| --- | --- |
| Android Debug | `Android-apk\gradlew.bat assembleDebug` |
| Android 单测 | `Android-apk\gradlew.bat testDebugUnitTest` |
| Android Release | `bundleRelease`、`assembleRelease`、`verifyPublishConfiguration` |
| Device Service | `mvn -f backend/device-service/pom.xml test` |
| JeecgBoot ReHealth | `mvn -f backend/jeecg-boot/pom.xml -pl jeecg-boot-module/jeecg-module-rehealth -am test` |
| model-service | `python -m pytest` |
| 公共契约 | `python backend/contracts/scripts/validate_contracts.py` |
| 拓扑门禁 | `python backend/qa/rehealth_stack_gate.py topology ...` |

## 15. 测试、质量与发布状态

### 15.1 现有自动化资产

当前仓库约有：Android 单元测试文件 61 个、Android 仪器测试文件 11 个、Device Service 测试文件 14 个、Jeecg ReHealth 测试文件 48 个、model-service 测试文件 8 个，并包含公共契约与部署拓扑门禁。

已有代表性验证包括：

- Android `testDebugUnitTest`、R8、Lint、`bundleRelease`、`assembleRelease` 历史通过。
- Room 13→14 迁移与 RHI 四表持久化在 MuMu API 35 仪器测试通过。
- 签名 `1.0.0 (1)` APK/AAB 已生成并在模拟器安装启动。
- Release APK 已确认不包含 Mock 商品目录和 Debug 演练入口。
- 本地 MySQL 已验证部分迁移、用户隔离、幂等和重启回读。
- 合同、OpenAPI、Kafka 隐私事件和服务边界有静态/自动化门禁。

### 15.2 当前发布结论

**MVP 发布状态：BLOCKED。** 主要阻塞项：

1. HBand 物理设备在 Android 13+ 上的扫描、认证、断线重连、锁屏长时间采集、功耗、温升和测量准确性证据未完成。
2. 登录 → 采集 → 离线队列 → 遥测上传 → 风险评估 → 干预 → 反馈的真实设备端到端证据仍需补齐。
3. 签名 Release APK 在真实手机上的权限、logcat、隐私和 HTTPS 全链路仍需验收。
4. 发布环境需挂载经过审核的真实模型制品并通过 readiness 门禁。
5. 生产容量、备份恢复、故障切换和 TimescaleDB/Kafka 长时间压力测试未完成。

## 16. 已知限制与风险

| 优先级 | 风险/限制 | 影响 | 建议 |
| --- | --- | --- | --- |
| P0 | HBand 真机长稳和准确性未完成 | 无法证明正式设备体验和数据可靠性 | 完成物理 MT116 全链路 QA 并留证 |
| P0 | 真实生产模型制品门禁未闭环 | 可能只能返回不可用/Mock 标识 | 挂载审核制品、校验哈希/Schema/特征顺序 |
| P0 | Release 真实手机全链路未验收 | 上线风险不可接受 | 使用签名包完成登录至反馈闭环 |
| P1 | RHI 日快照后端落库缺口 | 管理端无法获得权威 RHI 日快照 | 实现 JeecgBoot 端点、迁移与幂等测试 |
| P1 | 生产容量、备份恢复和故障切换未验证 | 数据与可用性风险 | 建立容量模型、恢复演练和运行手册 |
| P1 | 保险 APP UI/离线队列未完成 | 保险闭环无法面向用户交付 | 完成授权、撤回、计划展示和反馈队列 |
| P1 | 隐私中心、导出、删除、通知等为占位入口 | 容易造成产品承诺误解 | 上线前接线或暂时隐藏 |
| P2 | 模型页部分文案为静态视觉稿 | 可能被误解为实时学习状态 | 接入真实状态或标注演示 |
| P2 | 不支持多设备同时连接/融合 | 限制复杂用户场景 | MVP 保持单设备，后续再设计融合策略 |
| P2 | 云端 Feature Pipeline/MQTT 设备直连未实现 | 扩展性仍依赖当前手机/批量链路 | 放入规模化阶段路线图 |

## 17. 建议的下一阶段路线图

### 阶段一：关闭发布阻塞（P0）

1. 完成 HBand MT116 真实手机扫描、认证、同步、ECG、断网、锁屏、重连、功耗和准确性 QA。
2. 使用正式签名 APK 完成端到端闭环并留存日志、截图、数据库收据和模型版本证据。
3. 在发布环境挂载审核后的真实 CatBoost 制品，通过 `/ready`、模型注册表和 Mock 禁用门禁。
4. 完成生产 HTTPS、可信代理头、权限和隐私审计。

### 阶段二：补齐产品闭环（P1）

1. 实现 RHI 日快照 JeecgBoot 落库、管理端查询和重试/死信恢复。
2. 完成隐私中心、数据导出、数据删除、通知设置和关于页面。
3. 完成保险计划授权/撤回、计划详情和离线反馈队列。
4. 完成云米真实账号、回调、增量同步和异常恢复验收。

### 阶段三：试点与规模化（P1/P2）

1. 建立试点设备、活跃用户、日均记录、队列积压、风险评估成功率和干预完成率指标。
2. 完成 TimescaleDB/Kafka 容量、压缩、保留、备份恢复和故障切换演练。
3. 建立模型上线审批、版本回滚、漂移监测和人群公平性评估。
4. 根据业务决定是否建设连续云端 Feature Pipeline、MQTT/mTLS 设备直连和多设备融合。

## 18. 领导汇报建议口径

建议对外或对管理层使用以下准确表述：

> 睿禾精灵已经完成 Android 健康管理 MVP 的主体建设，形成了 HBand 蓝牙和云米云端两种设备入口，以及本地优先采集、离线同步、心血管风险评估、健康指数、结构化干预、健康问答和反馈闭环。系统采用移动端、硬件遥测服务、业务后端和模型服务分层架构，具备继续试点和扩展的工程基础。当前正在关闭真实设备长稳、生产模型、真实签名包全链路和生产容灾等发布门禁，因此现阶段应定位为“功能完整度较高、正在完成生产验收的 MVP”，不宜描述为已经全面正式上线。

## 19. 名词说明

| 名词 | 说明 |
| --- | --- |
| CVD | 心血管疾病（Cardiovascular Disease） |
| CVD-16 | 本项目用于风险评估的 16 项标准化输入 |
| RHI | ReHealth Health Index，动态健康指数，分数越高整体状态越好 |
| RDI-16 | APP 展示的 0–100 风险指数，来源于已确认 CVD 风险结果 |
| Factor16 | 16 项健康输入的透明规则贡献，不是因果结论 |
| PIAS | Predict–Intervene–Attribute–Settle，预测、干预、归因、结算体系 |
| Room | Android 本地 SQLite ORM/数据库框架 |
| Outbox | 数据库事务内记录待发布事件，随后可靠发布到 Kafka |
| Mock | 模拟或规则回退结果；正式环境不得伪装成真实模型/设备结果 |

## 20. 信息来源与口径声明

本报告是截至 2026-08-12 的管理层全景快照，依据当前代码、构建配置和以下仓库级权威文档整理：

- `README.md`：项目入口、架构、数据流与文档索引。
- `STATUS.md`：当前实现、发布阻塞和最新验收结论。
- `ENGINEERING.md`：工程原则、范围和里程碑。
- `Android-apk/README.md` 与 `Android-apk/docs/`：APP、设备、特征与同步契约。
- `backend/docs/MOBILE_API.md`、数据库文档与 `backend/contracts/`：接口和数据边界。
- `model-service/README.md` 与 `model-service/docs/`：模型服务、制品和运行门禁。
- `QA_TEST_PLAN.md`、`RELEASE_CHECKLIST.md`：测试与发布条件。

若本报告与后续代码或专项契约不一致，应以当时的 `STATUS.md`、机器可读 OpenAPI、数据库迁移和实际代码为准。本报告不替代医疗器械合规评估、临床验证、隐私影响评估或正式发布审批。
