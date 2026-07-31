# ReHealth 当前状态

> 最后核对：2026-07-31。本文档是仓库唯一的当前状态入口；历史验收记录只保存在
> `docs/archive/acceptance/`，不得作为当前实现或发布状态的依据。

## 发布结论

当前 MVP 发布状态：**BLOCKED**。

主要阻塞项：

1. 物理 MRD/RWFit 戒指及 HBand 手表/手环与 Android 13+ 真机的扫描、重连、锁屏长时间采集、功耗和准确性 QA 尚未完成；HBand 已开始首次真机联调，完整重装后的连接验证仍待完成。
2. Android 运行时端到端证据仍需覆盖登录、采集、离线队列、遥测上传、风险评估和反馈回传。
3. 签名 Release APK 的运行时 logcat、权限、隐私和真实 HTTPS 环境仍需验收。

## 已实现能力

| 范围 | 当前实现 |
| --- | --- |
| Android | 单一有效设备 Provider 路由（Release 注册 MRD/RWFit/HBand）、真实 SDK/BLE、Room、本地优先、Foreground Service、WorkManager、CVD 16 特征、认证感知上传队列、风险/干预/反馈 UI；健康初识仅对按账号标记的新注册用户展示，设备绑定保留在“我的”；Room v7 按登录用户隔离健康问答会话与消息，每次认证创建新的活动会话，首页支持完整消息滚动、滚动收起问候、历史切换和确认删除/清空，用户消息先落本机再请求服务端；数据页默认今日，跨午夜睡眠按结束日纳入且优先使用阶段分钟，周期健康指数只平均已确认非 Mock 的每日风险；“我的”头像按用户私有存储并在进入页面时重载，健康档案不重复基本资料与“健康问答”前缀；HBand App HRV/MET 专用 API 仅在双能力位成立时直测，HRV/压力可走一键体检，只有历史能力时隐藏 HRV/压力/MET App 测量卡片；所有失败路径均不生成占位值；固定 SDK 对应四 ABI JNI 已打包，Room v5 保存校准 mV/导联/采样元数据并提供实时及历史单导联波形详情；体温因真机验证不通过已从 HBand 商品能力和数据页移除，其他指标及 ECG 真机准确性仍待验收 |
| Device Service | 遥测校验、TimescaleDB 持久化、幂等批次、Transactional Outbox、Kafka 发布 |
| JeecgBoot | 登录与权限、用户/设备绑定、结构化档案/访谈/干预业务数据、风险/干预/反馈编排、LangChain4j 健康问答、安全策略、用户/租户隔离会话历史和 software_db；健康问答可把明确自述的姓名、性别、年龄、身高和体重合并入结构化档案，并让同轮提示词读取新值；模型证据继续保留版本化 JSON 快照 |
| model-service | CVD 风险评分、模型制品校验、干预生成；旧健康助手接口保留为可配置回退 |
| PIAS | 独立服务提供个体归因；数据页今日风险优先展示远程 PIAS 返回的当前风险，Android 不执行生产归因 |
| 部署 | Gateway、MySQL、TimescaleDB、Kafka、Redis、Nacos、Prometheus、Grafana 的 Compose 拓扑 |
| 真机联调通道 | `https://rehealth.youngjimmy.store`（SSH 反向隧道 + ECS nginx，Let's Encrypt SAN 证书，2026-07-29 端到端 200；备用 `rehealth.47.80.30.228.sslip.io`），Debug/Release 均可联调；见 `tools/dev-tunnel/README.md` |

## 已验证边界

- Android 采集先写 Room，再创建上传任务；网络不可阻塞 BLE。
- 遥测上传不直接触发风险评分。
- 硬件时序数据归 Device Service/TimescaleDB，业务数据归 JeecgBoot/software_db。
- CatBoost、SHAP、LLM 和生产归因不进入 Android APK。
- 生产和 staging 不允许把 Mock 结果伪装成真实模型结果。
- Android Release 源集不包含模拟戒指实现；远程风险失败不会生成本地模拟结果。
- `productCode` 只选择一个懒加载 Provider；绑定存于加密偏好且不迁移 Room，
  未绑定地址时后台采集不会使用固定 MAC 自动连接；RWFit SDK 类型不进入 UI、
  ViewModel 或 Room Entity；HBand SDK 类型同样被限制在 Gateway 文件内，未支持的指标不生成占位记录，
  ECG 波形只写本地 Room 且不进入云端上传；HBand SDK 疾病风险不作为诊断展示，
  新记录保存校准 mV 和结构化导联/采样元数据，旧整数记录保留并仅按相对幅值展示。
- Debug 设备页可在明确确认后暂停采集、断开旧 Provider 并切换本地 `productCode`；
  Release 隐藏该入口，切换不会删除历史 `ring_*` 数据。
- 三个真实 Provider 的后台同步只重连已绑定地址且不做环境扫描；前后台操作共享
  路由互斥锁。HBand 恢复画像使用按用户哈希隔离的加密缓存，外部协程取消会断开 SDK。
- MySQL 8 staging 已有迁移、用户隔离、幂等和重启回读证据；生产容量与恢复仍待验证。
- Android 重新登录和进入个人页会刷新当前用户的类型化个人资料、最近健康问答和本地头像，且不再受风险/干预接口失败影响；健康初识按用户隔离且完成前先持久化 Room 队列，麦克风入口具备用途说明、运行时授权和拒绝后的设置引导。
- 健康问答默认使用 JeecgBoot Java LangChain4j，`model-service` 对话仅保留为显式回滚；每轮装配类型化画像/访谈/风险/干预，“我是谁/我叫什么”通过不接收用户选择参数的当前认证用户资料工具读取最新基本资料。问答中明确自述的五项基本资料先合并入库再装配同轮画像，MySQL 会话与消息按用户+租户隔离，Android Room v7 本地先写并管理会话；后端仍只有最新会话恢复，没有列表/删除契约，生产数据库迁移、真实 Provider 和跨设备手工 QA 仍待执行。
- 数据页睡眠/步数/活动改为已连接设备的日常增量同步，断连时按钮禁用且进程内自动采集跳过；HBand 根据 Room 最近日期选择重叠窗口，无活动缺口时跳过长原始历史，首次/缺口仍补全。SDK 回调驱动目标进度，界面平滑推进且不延迟 Room 落库。显式 Foreground Service 恢复仍可重连当前加密绑定。

## 当前仓库治理决定

- `backend/jeecgboot-vue3` 保留，作为 JeecgBoot 管理前端。
- `jeecg-boot-module-airag` 保留，不在本轮清理范围。
- `jeecg-module-demo`、`jeecg-demo-cloud-start` 与 `jeecg-cloud-test` 已确认不含 ReHealth 用途并移除。
- Git 历史不重写；已删除内容仍可从历史 commit 恢复。
- 本地代理状态、构建产物、虚拟环境、APK、截图和下载工具不进入 Git。

## 下一验收顺序

1. Docker 引擎恢复后补跑 Device Service 的 TimescaleDB/Testcontainers 集成测试。
2. 在发布环境挂载已审核模型制品并复核真实模型门禁。
3. 使用包含完整 JieLi/Nordic/JNI 依赖的 APK 完成 HBand 连接与 ECG 实时/历史波形复测，再完成 MRD/RWFit/HBand 与 Android 运行时端到端 QA。
4. 完成签名 Release APK 和真实部署环境验收。

## 历史证据

- 阶段验收快照：`docs/archive/acceptance/`
- G3 静态隐私审计：`docs/archive/qa/G3_PRIVACY_AUDIT_2026-07-20.md`
- 当前接口与行为以根 `README.md` 的文档索引所列契约为准。
