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
| Android | 单一有效设备 Provider 路由（Release 注册 MRD/RWFit/HBand）、真实 SDK/BLE、Room、本地优先、Foreground Service、WorkManager、CVD 16 特征、认证感知上传队列、风险/干预/反馈 UI；健康初识完成后直接进入首页，设备绑定保留在“我的”；Room v7 按登录用户隔离健康问答会话与消息，6→7 显式迁移保留消息并生成会话，首页支持本机会话列表、新建、切换和确认删除/清空，用户消息先落本机再请求服务端，页面重建/重登后恢复本机及云端最新会话；首页麦克风使用系统语音转文字并回填待确认输入，AI 回复使用不执行 HTML、远程资源或自动链接的受限 Markdown；首页拍照记录调用系统相机，以私有 FileProvider 临时文件上传食物/OCR 照片，并在首页和数据页展示当前用户的今日结构化行为记录；模型页采用固定的端侧学习视觉稿，仅调整展示层，不改变实际云端风险评分链路；“我的”每日步数优先聚合 Room 当地自然日活动，头像经系统照片选择、本机重编码后按用户隔离保存且不上云；健康初识会把可识别的年龄、身高、体重作为结构化 profile 一并排队同步；HBand 心率、步数/活动、睡眠、血氧、HRV、血压、血糖、压力、MET、ECG、血液/身体成分以及血糖校准、经期设置按设备能力接入；MT116 能力判定合并新版分包报告，ECG 以 2 号能力包优先；2026-07-30 真机日志证实固件虽然声明 HRV/压力/MET 独立能力，三项专用命令仍返回全 0 `unknown action`，现已改为 HRV/压力优先走 4 号能力包一键体检、MET 优先获取设备历史，避免 SDK 弹出不支持提示且不生成占位值；固定 SDK 对应四 ABI JNI 已打包，Room v5 保存校准 mV/导联/采样元数据并提供实时及历史单导联波形详情；ECG 与身体成分在用户确认电极接触和稳定姿势说明后才下发测量命令；体温因真机验证不通过已从 HBand 商品能力和数据页移除，其他指标及 ECG 真机准确性仍待验收 |
| Device Service | 遥测校验、TimescaleDB 持久化、幂等批次、Transactional Outbox、Kafka 发布；`telemetry-v2` 新增饮食行为记录，并向受信 Jeecg 调用提供租户/用户/自然日隔离的今日行为与近 7 日描述性变化 |
| JeecgBoot | 登录与权限、用户/设备绑定、结构化档案/访谈/干预/行为业务数据、风险/干预/反馈编排、LangChain4j 健康问答、视觉食物/OCR 和结构化生活方式干预、安全策略、用户/租户隔离会话历史和 software_db；每次生成干预都重新读取权威画像、最新访谈/风险和 Device Service 行为上下文，不采信客户端画像/风险；拍照分析只持久化验证后的结构化结果，不保存原图；模型证据继续保留版本化 JSON 快照 |
| model-service | CVD 风险评分、模型制品校验；旧干预生成仍保留作兼容路径；新增隔离的 `/v2/rhi/evaluate` research preview，提供 32 维确定性 RHI、五域、动量和可信度，明确不生成临床概率；旧健康助手接口保留为可配置回退 |
| PIAS | 独立服务提供个体归因；Android 不执行生产归因 |
| 部署 | Gateway、MySQL、TimescaleDB、Kafka、Redis、Nacos、Prometheus、Grafana 的 Compose 拓扑 |
| 真机联调通道 | `https://rehealth.youngjimmy.store`（SSH 反向隧道 + ECS nginx，Let's Encrypt SAN 证书，2026-07-29 端到端 200；备用 `rehealth.47.80.30.228.sslip.io`），Debug/Release 均可联调；见 `tools/dev-tunnel/README.md` |

## 已验证边界

- Android 采集先写 Room，再创建上传任务；网络不可阻塞 BLE。
- 遥测上传不直接触发风险评分。
- 硬件时序数据归 Device Service/TimescaleDB，业务数据归 JeecgBoot/software_db。
- TimescaleDB V4 增加 `hardware_diet_record` hypertable；批次、饮食及 Outbox 在同一事务提交。结构化干预每次生成前重新读取基本健康信息、最近变化和今日行为，返回 1–5 条带类别、目标、时机、依据引用和优先级的保守行动。
- Android Room v11 增加用户隔离的 `diet_records` 和显式 10→11 迁移；归因页支持餐次、内容、热量及可选三大营养素录入，先本地保存再写 durable queue。没有真实绑定或网络时记录不会丢失，取得绑定后以稳定 `telemetry-v2 dietRecords` 批次补同步。
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
- 拍照行为记录已完成系统相机、私有临时 URI、方向校正/缩放重编码、认证上传、Java LangChain4j 视觉分析、用户/租户隔离幂等落库及首页/数据页今日展示；本地 MySQL 迁移、Provider 模型目录、后端测试和 Debug APK 构建已验证，真实手机拍摄的食物/OCR 准确性与失败恢复仍待手工 QA。
- MIUI 相机返回早于私有文件完全写稳时，拍照读取现会等待文件大小稳定并直接从受控缓存路径解码；Android 14 真机仪器测试已覆盖延迟写入和 2400×1800 JPEG 的 1600 边界压缩，真实食物拍摄仍需用户手工确认。
- 拍照识别现使用独立于普通 API 的长超时：Android 最多等待 110 秒，JeecgBoot 单次视觉调用默认 75 秒且不自动重试；Provider 超时会显示“图片识别超时”而非误报网络断开。真实食物图片在修复后的端到端结果仍需真机复测。
- RHI v2 已完成研究规划、32 维 typed schema、确定性预览引擎、验证工具和 Android 未接线 DTO/迁移映射；Android 本地 `rhi-deterministic-preview-2.2.0-android-lite` 已接入 Room 可穿戴数据、可信个人资料及“我的 > 健康档案”手填指标。Room v9/v10 以显式 8→9→10 迁移保存久坐、腰围、正式 VO₂max、HbA1c、eGFR、确认袖带血压和带日期医院血检；空白值不补正常值，无袖带戒指血压不进入 RHI。当前仍没有 JeecgBoot 公共路由或云端日快照表，不能作为经验证临床能力，生产风险仍走 CVD-16。
- RHI 2.2.0 修正四处计算缺陷并落地日度持久化，未改动 UI：按
  LITE/STANDARD/CLINICAL 分级判定可信度分母（分级由实际提取到的证据决定，
  不区分手填或设备同步），消除 `total_cholesterol` 重复计数与纯可穿戴用户被
  化验项虚高分母压制的问题；MVPA 个人基线改用 7 日滚动总量对齐量纲；
  `steps_7d_mean` 恒除以 7，未佩戴日按零暴露计入；新增四类质量提醒
  （`activity_duration_missing` / `wear_time_incomplete` /
  `blood_pressure_unavailable` / `steps_all_zero`），仅解释可信度、不改分数。
  Room v14 以纯新增迁移 13→14 拆出 `rhi_daily_health_index` /
  `rhi_daily_domain_score` / `rhi_daily_feature_snapshot` /
  `rhi_data_quality_snapshot`，按 `(user_id, scored_on)` 重算即覆盖，未计分域存
  `NULL` 而非中性 50，持久化失败不阻断评分；`delta_7d` / `delta_28d` 改为固定
  回看窗。已通过 `:app:testDebugUnitTest`（含 21 项 RHI 用例）与
  `assembleDebug`；`:app:connectedDebugAndroidTest` 已在 MuMu（API 35 / x86_64）
  跑通全部 10 项仪器化用例，其中 13→14 由 `runMigrationsAndValidate` 校验建表
  SQL 与 Room 期望 schema 一致，`RhiSnapshotPersistenceTest` 以真实
  `RhiRepository` + 真实 Room 验证四张表确实被写入、重算覆盖而非追加、质量提醒
  落库、未登录时仍出分但不落库。覆盖安装后设备端实测 `user_version=14`、四表
  与 8 个索引就绪且无 SQLite 异常。云端日快照表与 JeecgBoot 公共路由仍未提供。
- RHI 上传侧（Android）已实现：本地算好并落库后，`RhiRepository.persist` 把当天
  快照投影为 `RhiDailySnapshotBatchDto`（`RhiSnapshotMapper.toUploadDto`，只含聚合
  输出、不含原始可穿戴序列），通过现有离线上传队列 `SyncRepository` 的
  `rhi_daily_snapshot` kind 入队，复用其 401 暂停、指数退避与死信逻辑；新增
  `RhiSnapshotUploadClient` 接口与 `AuthenticatedApiClient.uploadRhiSnapshot`
  实现，Retrofit 端点在 `ReHealthApi` / `ReHealthMobileApi` 声明为
  `POST /rehealth/mobile/rhi/daily-snapshot`（契约见 `backend/docs/MOBILE_API.md`）。
  端点尚未在 JeecgBoot 落库实现，故上传会收到 404/失败并进入死信，但不会崩溃，
  也不影响本地评分与持久化；后端实现后即可即通。未改动任何 UI。新增
  `RhiSnapshotPersistenceTest.refreshPeriod_enqueuesRhiSnapshotForUpload` 在 MuMu
  上以真实 `RhiRepository` + 真实 `SyncRepository` + 真实 Room 验证入队行为，5 项
  仪器化用例全过；`:app:testDebugUnitTest` 与 `assembleDebug` 均通过。
- Android 已保留独立本地 `RDI rdi-rule-1.0.0` 算法骨架：Room v8 通过显式
  7→8 迁移保存每日快照与贡献证据，但它不再驱动归因页。“健康改善得分”现由
  Android RHI Lite 透明引擎按所选 7/30/90 日窗口计算最后与第一个有效 RHI 的差值，
  因而展示本周期累计改善，折线展示同一窗口的 RHI 历史。右侧“RDI-16 风险指数”
  聚合相同窗口内已确认、已落库的 RDI-16 返回并显示为 `x/100`。下方个人风险趋势
  只以相同窗口的 RDI-16 历史绘制蓝色实线，不再以 PIAS 填充当前值或趋势。
  Android 已增加 RDI 原生 30 日反事实预测：在活动、睡眠、HRV 各至少 7 个有效日且
  存在明确活动/睡眠计划时，按所选 7/30/90 日个人模式构造“维持现状”和“执行计划”输入，逐日
  调用同一 RDI 引擎；所选周期 RDI 分作为水平的维持现状参考线，原生两臂差值形成计划轨迹，
  以第 30 日差值显示预计降低；95% 情景区间由近期个人波动的确定性
  敏感性扰动生成，不表示疾病概率置信区间。未来输入和分值不写入 Room，条件不足时仍显示
  暂不可用，且不以 PIAS 填充。趋势卡按所选 7/30/90 日窗口计算周期末与周期起点的原生
  RDI 因素贡献差，分别展示本周/本月/本季度风险变化贡献 Top 3；不再把周期平均风险负担
  误写成改善贡献，负值明确标为降低风险、正值明确标为增加风险，也不再
  把输入完整性系数显示成“可信度 100%”，改用充分/一般/有限的定性数据依据等级。
  图表中的蓝色实际线、两条情景线和灰色 95% 情景区间从所选周期最左端的第一个真实
  RDI 点开始，共用整段横轴，允许实际线与情景线重合；底部数值不受图表对齐影响。
  页面明确情景模拟不代表未来疾病发生概率。
  模型页采用固定端侧学习视觉稿，不参与实际风险计算，也未把 CVD 16 项迁入。
  RDI 骨架不覆盖 CVD-16 风险历史，不把无袖带血压、缺失血检或跨设备
  HRV 当正式贡献；手工餐食录入和拍照行为记录已实现；医院报告 OCR 自动入档与饮食结构化遥测仍未实现，拍照识别结果只进入今日行为记录。
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

1. Docker 引擎恢复后补跑 Device Service 的 TimescaleDB/Testcontainers 集成测试，重点验证 V4 饮食 hypertable、压缩/保留策略和混合批次事务回滚。
2. 在发布环境挂载已审核模型制品并复核真实模型门禁。
3. 使用包含完整 JieLi/Nordic/JNI 依赖的 APK 完成 HBand 连接与 ECG 实时/历史波形复测，再完成 MRD/RWFit/HBand 与 Android 运行时端到端 QA。
4. 完成签名 Release APK 和真实部署环境验收。

## 历史证据

- 阶段验收快照：`docs/archive/acceptance/`
- G3 静态隐私审计：`docs/archive/qa/G3_PRIVACY_AUDIT_2026-07-20.md`
- 当前接口与行为以根 `README.md` 的文档索引所列契约为准。
