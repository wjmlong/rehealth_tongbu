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
| Android | 单一有效设备 Provider 路由（Release 注册 MRD/RWFit/HBand）、真实 SDK/BLE、Room、本地优先、Foreground Service、WorkManager、CVD 16 特征、认证感知上传队列、风险/干预/反馈 UI；健康初识完成后直接进入首页，设备绑定保留在“我的”；Room v7 按登录用户隔离健康问答会话与消息，6→7 显式迁移保留消息并生成会话，首页支持本机会话列表、新建、切换和确认删除/清空，用户消息先落本机再请求服务端，页面重建/重登后恢复本机及云端最新会话；首页麦克风使用系统语音转文字并回填待确认输入，AI 回复使用不执行 HTML、远程资源或自动链接的受限 Markdown；模型页采用固定的端侧学习视觉稿，仅调整展示层，不改变实际云端风险评分链路；“我的”每日步数优先聚合 Room 当地自然日活动，头像经系统照片选择、本机重编码后按用户隔离保存且不上云；健康初识会把可识别的年龄、身高、体重作为结构化 profile 一并排队同步；HBand 心率、步数/活动、睡眠、血氧、HRV、血压、血糖、压力、MET、ECG、血液/身体成分以及血糖校准、经期设置按设备能力接入；MT116 能力判定合并新版分包报告，ECG 以 2 号能力包优先；2026-07-30 真机日志证实固件虽然声明 HRV/压力/MET 独立能力，三项专用命令仍返回全 0 `unknown action`，现已改为 HRV/压力优先走 4 号能力包一键体检、MET 优先获取设备历史，避免 SDK 弹出不支持提示且不生成占位值；固定 SDK 对应四 ABI JNI 已打包，Room v5 保存校准 mV/导联/采样元数据并提供实时及历史单导联波形详情；ECG 与身体成分在用户确认电极接触和稳定姿势说明后才下发测量命令；体温因真机验证不通过已从 HBand 商品能力和数据页移除，其他指标及 ECG 真机准确性仍待验收 |
| Device Service | 遥测校验、TimescaleDB 持久化、幂等批次、Transactional Outbox、Kafka 发布 |
| JeecgBoot | 登录与权限、用户/设备绑定、结构化档案/访谈/干预业务数据、风险/干预/反馈编排、LangChain4j 健康问答、安全策略、用户/租户隔离会话历史和 software_db；健康问答可把明确自述的姓名、性别、年龄、身高和体重合并入结构化档案，并让同轮提示词读取新值；模型证据继续保留版本化 JSON 快照 |
| model-service | CVD 风险评分、模型制品校验、干预生成；新增隔离的 `/v2/rhi/evaluate` research preview，提供 32 维确定性 RHI、五域、动量和可信度，明确不生成临床概率；旧健康助手接口保留为可配置回退 |
| PIAS | 独立服务提供个体归因；Android 不执行生产归因 |
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
- Android 重新登录和进入个人页会刷新当前用户的类型化个人资料与最近健康问答，且不再受风险/干预接口失败影响；健康初识完成前先持久化 Room 队列，麦克风入口具备用途说明、运行时授权和拒绝后的设置引导。
- 健康问答 Java 纵向链路已实现：可在 `model-service` 与 `langchain4j` 间配置切换，每轮装配类型化画像/访谈/风险/干预，问答中明确自述的五项基本资料先合并入库再装配同轮画像，MySQL 会话与消息按用户+租户隔离，Android Room v7 本地先写并管理会话；后端仍只有最新会话恢复，没有列表/删除契约，生产数据库迁移、真实 Provider 和跨设备手工 QA 仍待执行。
- RHI v2 已完成研究规划、32 维 typed schema、确定性预览引擎、验证工具和 Android 未接线 DTO/迁移映射；Android 本地 `rhi-deterministic-preview-2.1.0-android-lite` 已接入 Room 可穿戴数据、可信个人资料及“我的 > 健康档案”手填指标。Room v9/v10 以显式 8→9→10 迁移保存久坐、腰围、正式 VO₂max、HbA1c、eGFR、确认袖带血压和带日期医院血检；空白值不补正常值，无袖带戒指血压不进入 RHI。当前仍没有 JeecgBoot 公共路由或云端日快照表，不能作为经验证临床能力，生产风险仍走 CVD-16。
- Android 已保留独立本地 `RDI rdi-rule-1.0.0` 算法骨架：Room v8 通过显式
  7→8 迁移保存每日快照与贡献证据，但它不再驱动归因页。“健康改善得分”现由
  Android RHI Lite 透明引擎计算 RHI-100：7 日取当前近 7 日有效数据，30/90 日
  取有效日 RHI 稳健中位数，同卡片折线显示 RHI 历史。右侧当前风险和下方 PIAS
  个人风险趋势使用既有 RDI-16 已确认风险历史作为实线，并将 PIAS 返回的
  维持现状/完成计划序列画成灰色/绿色虚线、置信区间画成淡色区域；页面明确
  情景模拟不代表未来疾病发生概率。模型页采用固定端侧学习视觉稿，
  不参与实际风险计算，也未把 CVD 16 项迁入。RDI 骨架不覆盖 CVD-16 风险历史，不把无袖带血压、缺失血检或跨设备
  HRV 当正式贡献；医院报告 OCR 与饮食识别仍未实现。
- Android 数据页已移除硬编码健康指数 `87`：风险卡标为 RDI-16，复用既有
  16 特征评估接口且只展示真实、有限、范围有效、非 Mock 的云端结果，不改变
  原特征提取规则；健康指数圆环展示 RHI-100；今日/7 日取当前 RHI，
  30/90 日取有效日稳健中位数，缺少数据时保持积累状态。
- 归因页 16 项已与 RDI16 解耦：model-service 额外返回
  `factor16-rule-v1.0.0` 规则贡献和血压/血检 80/20 分量，Android 展示同次
  送评估向量的真实字段值；经确认的 3–7 日上臂袖带均值和带日期医院报告
  由 Room v10 入口提供，无袖带设备血压及未确认血检保持缺失。控制支持趋势
  尚无可验证输入时 20% 明确为 0，不伪造临床值。每项展开区只显示字段解释和
  保守固定建议，不展示来源、规则版本、规则贡献说明或 80/20 技术分量；
  建议不改变规则贡献，也不作为服务端干预计划。

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
