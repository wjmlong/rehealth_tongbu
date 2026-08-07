# ReHealth 当前状态

> 最后核对：2026-08-07。本文档是仓库唯一的当前状态入口；历史验收记录只保存在
> `docs/archive/acceptance/`，不得作为当前实现或发布状态的依据。

当前待发布 Android 版本为 `1.0.0 (versionCode 1)`；该版本包含 HBand/云米连接方式选择，
显示版本和内部版本号均按产品要求固定为 `1.0.0 (1)`。
Release Lint 保留全部既有门禁，但临时禁用会因 AGP 8.10.1/Compose lint 类加载缺失而直接崩溃的
`MutableCollectionMutableState` 单项检测；升级整套 Android 工具链后必须恢复该检测。

## 2026-08-05 云米云端手表接入

- 后端已增加 `/rehealth/mobile/viomi/bind` 与 `/viomi/sync`，支持 S8、S9、GS20、GS17、A67、K9L 共用的 IMEI 验证和历史拉取流程。
- 心率、血压、血氧先经硬件入库端口持久化，再返回 Android 写入 Room；云端来源不会被 App 重复上传。
- App 已增加 `VIOMI_CLOUD` provider、产品目录与 IMEI 绑定 UI；生产包允许选择真实设备产品。
- Debug 与 Release 的正式连接选择统一收敛为“HBand”和“云米（IMEI 云端）”；
  两种构建默认 HBand，旧 MRD/RWFit 保存选择升级后迁移到 HBand，已有云米绑定保持不变。
- 绑定成功自动执行首次 31 天回填，后续按设备最新记录以 2 天重叠窗口增量同步；数据页仅展示已支持的心率、血氧、血压。
- Room v15 为测量增加用户与设备作用域；云米数据查询按用户、设备、`viomi_cloud` 来源隔离，14→15 迁移保留旧记录。
- 真实联调仍需注入 `REHEALTH_VIOMI_APP_ID`、`REHEALTH_VIOMI_APP_KEY` 和 `REHEALTH_VIOMI_USER_ID`。

## 发布结论

当前 MVP 发布状态：**BLOCKED**。

主要阻塞项：

1. 物理 MRD/RWFit 戒指及 HBand 手表/手环与 Android 13+ 真机的扫描、重连、锁屏长时间采集、功耗和准确性 QA 尚未完成；HBand 已开始首次真机联调，完整重装后的连接验证仍待完成。
2. Android 运行时端到端证据仍需覆盖登录、采集、离线队列、遥测上传、风险评估和反馈回传。
3. 签名 Release APK 的真实设备运行时 logcat、权限、隐私和真实 HTTPS 全链路仍需验收。

2026-08-04 发布整理已完成 Release 源集门禁：`testDebugUnitTest`、R8、Lint Vital 和
`assembleRelease` 在显式 HTTPS 联调地址下通过；Mock 商品资源、设备演练 UI、
`synthetic_qa`、Debug Factor16 版本、占位域名和 API Key 样式值均未进入 Release APK，
Manifest 禁止 cleartext，所有模拟开关为 `false`。完整 `lintRelease` 发现并移除了未使用的
`QUERY_ALL_PACKAGES` 与尚未接线的 Health Connect 写权限，修正后已通过。该阶段的 unsigned
产物仅用于边界审计，已由下述正式签名产物取代。

2026-08-06 已按产品要求重新完成 `1.0.0 (versionCode 1)` 正式签名构建。
`testDebugUnitTest`、`verifyPublishConfiguration`、`lintRelease`、R8、`bundleRelease` 与
`assembleRelease` 均通过。签名 APK SHA-256 为
`5EC3E54093FBB5F842ECF34ACC8111E33F3589C42747FFAF9DCCD1C945E40E40`，签名 AAB SHA-256 为
`6137E651B7925941B2F7ADB1AE86E3A725F6E6EB0A15D31E7E7F9BDD0ABBF869`；APK 使用既有批准证书的
v2 签名。Release APK 内仅包含 HBand 与 Viomi Cloud 商品目录，不包含 Debug 商品目录。
MuMu（API 35）中的 `versionCode 2` 应用及本地数据已先卸载，再全新安装该 APK；安装成功，
包信息显示 `versionName=1.0.0`、`versionCode=1`，并已成功启动账号登录页。设备选择页和
物理 MT116 连接仍需登录后的人工真机验收。

2026-08-05 已完成 `1.0.1 (versionCode 2)` 正式签名构建：Debug/Release 连接选择仅展示
HBand MT116 蓝牙与云米 IMEI 云端，Release 默认 HBand，旧 MRD/RWFit 保存选择迁移到 HBand。
`testDebugUnitTest`、`verifyPublishConfiguration`、`lintRelease`、R8、`bundleRelease` 与
`assembleRelease` 均通过。签名 APK SHA-256 为
`6D9F4C28BAED5F3614D3745CEBAC770FC240A1EC16A8DB41EDE1A0AB62B0728D`，签名 AAB SHA-256 为
`3437A437BCDB5A86978D8D3DABC352855DF74A924A2D11555BB22F0B617C1EFC`；APK 仅使用批准上传证书的
v2 签名，证书 SHA-256 与既有批准指纹一致。Release APK 内商品目录只含 HBand 与 Viomi Cloud，
不含 Debug 增量目录。
MuMu（API 35）已从同证书 `1.0.0 (1)` 覆盖安装到 `1.0.1 (2)` 并成功启动登录页；
设备选择页仍需登录后的人工点击验收，物理 MT116 连接继续保持发布阻塞。

2026-08-04 已在仓库外创建 RSA 4096 位 PKCS12 Android Upload Key，并把仓库默认 Release
地址确认为 `https://rehealth.youngjimmy.store/jeecg-boot/`。上传证书 SHA-256 为
`84:56:D2:47:A4:9E:A4:71:9B:95:A0:9D:AD:AB:7C:83:0F:1E:1C:74:D8:E3:22:A0:6D:BB:53:D6:A2:BA:C9:75`；
keystore 和 DPAPI 凭据不进入 Git。`testDebugUnitTest`、`verifyPublishConfiguration`、
`lintRelease`、R8、`bundleRelease` 与 `assembleRelease` 已通过；签名 APK SHA-256 为
`295CE48A64D6FB37FF95B40D3D0E09374B20E000E79EB6C11501ACECAD060845`，签名 AAB SHA-256 为
`F02CE90E0713585C52FA9EEE4DE568565CA34D664BB86E4A6B0FB4ACEC1C9A9E`。MuMu 已强制卸载旧包、
安装并启动 `1.0.0 (versionCode 1)` 正式签名 APK，系统未标记 debuggable且可见页面无 Debug
入口。生产 API 路径保持 HTTPS 并返回预期 200/401；站点根路径的文档跳转仍生成 HTTP URL，
虽不影响 App API 调用，仍应在网关补齐可信 `X-Forwarded-Proto`/HTTPS 重定向后再完成公网验收。
Play Console 内测、物理设备与完整线上闭环验收仍保持发布阻塞。

## 已实现能力

| 范围 | 当前实现 |
| --- | --- |
| Android | 单一有效设备 Provider 路由；Release 只注册 HBand/Viomi Cloud，正式选择只展示 HBand MT116 蓝牙和云米 IMEI 云端，默认 HBand 并迁移旧 MRD/RWFit 选择；Debug 保留 MRD/RWFit/Mock 工程入口。已接入真实 SDK/BLE、云米 IMEI 绑定、Room、本地优先、Foreground Service、WorkManager、CVD 16 特征、认证感知上传队列及风险/干预/反馈 UI。HBand 已按能力接入心率、步数/活动、睡眠、血氧、HRV、血压、血糖、压力、MET、ECG、血液/身体成分与设备设置；MT116 的 HRV/压力优先走一键体检或真实历史，MET 只读真实历史，体温已移除，完整真机准确性仍待验收。 |
| Device Service | 遥测校验、TimescaleDB 持久化、幂等批次、Transactional Outbox、Kafka 发布；`telemetry-v2` 新增饮食行为记录，并向受信 Jeecg 调用提供租户/用户/自然日隔离的今日行为与近 7 日描述性变化 |
| JeecgBoot | 登录与权限、用户/设备绑定、结构化档案/访谈/干预/行为业务数据、风险/干预/反馈编排、LangChain4j 健康问答、视觉食物/OCR 和结构化生活方式干预、安全策略、用户/租户隔离会话历史和 software_db；注册短信已支持独立阿里云短信 RAM 凭据、签名与模板配置，缺配置失败关闭，真实发送仍待凭据/签名/模板到位后验收；每次生成干预都重新读取权威画像、最新访谈/风险和 Device Service 行为上下文，不采信客户端画像/风险；拍照分析只持久化验证后的结构化结果，不保存原图；模型证据继续保留版本化 JSON 快照 |
| model-service | CVD 风险评分、模型制品校验；旧干预生成仍保留作兼容路径；新增隔离的 `/v2/rhi/evaluate` research preview，提供 32 维确定性 RHI、五域、动量和可信度，明确不生成临床概率；旧健康助手接口保留为可配置回退 |
| PIAS | 独立服务提供个体归因；Android 不执行生产归因 |
| 部署 | Gateway、MySQL、TimescaleDB、Kafka、Redis、Nacos、Prometheus、Grafana 的 Compose 拓扑 |
| 真机联调通道 | `https://rehealth.youngjimmy.store`（SSH 反向隧道 + ECS nginx，Let's Encrypt SAN 证书，2026-07-29 端到端 200；备用 `rehealth.47.80.30.228.sslip.io`），Debug/Release 均可联调；见 `tools/dev-tunnel/README.md` |

HBand 的 HRV、压力、MET 页面策略已经按 MT116 实测收紧：HRV/压力仅通过一键体检或真实历史取得有效值，MET 仅通过真实设备历史取得有效值；HRV/MET 专用 SDK 能力仍保留在底层以便兼容与诊断，但产品页不触发 MET 实时命令。三项只有真实 Provider 的 HRV/MET 正值或 `1..100` 压力值才写入 Room 并显示，无有效值、越界值或模拟来源时隐藏卡片。

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
- Debug 与 Release 设备页均只向用户提供 HBand MT116 蓝牙与云米 IMEI 云端两种连接方式；Debug 另保留 Mock、MRD/RWFit 工程测试能力与演练入口。
  切换会先暂停采集、断开旧 Provider，且不会删除历史 `ring_*` 数据。
- 三个真实 Provider 的后台同步只重连已绑定地址且不做环境扫描；前后台操作共享
  路由互斥锁。HBand 恢复画像使用按用户哈希隔离的加密缓存，外部协程取消会断开 SDK。
- MySQL 8 staging 已有迁移、用户隔离、幂等和重启回读证据；生产容量与恢复仍待验证。
- Android 重新登录和进入个人页会刷新当前用户的类型化个人资料与最近健康问答，且不再受风险/干预接口失败影响；每个新登录令牌（以及有效会话的进程重启）会先通过认证 `GET /measurements/recent?limit=200` 幂等恢复当前用户的测量、睡眠和活动到 Room 并触发本地 RHI 重算，失败不阻塞登录或 BLE；健康初识完成前先持久化 Room 队列，麦克风入口具备用途说明、运行时授权和拒绝后的设置引导。
- Android 归因页只读取今日已持久化干预，空计划由用户按钮显式生成并展示加载和失败状态；已有计划默认按 01–05 编号行动清单展开，显示 16 项健康输入说明、展开状态和整宽收起/展开按钮，不重复显示重新生成按钮。客户端兼容 snake_case/camelCase 计划响应。类型化档案字段保存后同步覆盖贡献因素展示并触发新一轮特征评估，经确认血压/血检保存同样触发重算。退出登录或未授权暂停会话由根导航直接返回登录页。
- 健康问答 Java 纵向链路已实现：可在 `model-service` 与 `langchain4j` 间配置切换，每轮装配类型化画像/访谈/风险/干预，问答中明确自述的五项基本资料先合并入库再装配同轮画像，MySQL 会话与消息按用户+租户隔离，Android Room v7 本地先写并管理会话；后端仍只有最新会话恢复，没有列表/删除契约，生产数据库迁移、真实 Provider 和跨设备手工 QA 仍待执行。
- 拍照行为记录已完成系统相机、私有临时 URI、方向校正/缩放重编码、认证上传、Java LangChain4j 视觉分析、用户/租户隔离幂等落库及首页/数据页今日展示；服务端按设备本地日查询，Android 再按本地自然日边界过滤；本地 MySQL 迁移、Provider 模型目录、后端测试和 Debug APK 构建已验证，真实手机拍摄的食物/OCR 准确性与失败恢复仍待手工 QA。
- MIUI 相机返回早于私有文件完全写稳时，拍照读取现会等待文件大小稳定并直接从受控缓存路径解码；Android 14 真机仪器测试已覆盖延迟写入和 2400×1800 JPEG 的 1600 边界压缩，真实食物拍摄仍需用户手工确认。
- 拍照识别现使用独立于普通 API 的长超时：Android 最多等待 110 秒，JeecgBoot 单次视觉调用默认 75 秒且不自动重试；Provider 超时会显示“图片识别超时”而非误报网络断开。真实食物图片在修复后的端到端结果仍需真机复测。
- RHI v2 已完成研究规划、32 维 typed schema、确定性预览引擎、验证工具和 Android DTO/迁移映射；Android 本地 `rhi-deterministic-preview-2.2.0-android-lite` 已接入 Room 可穿戴数据、可信个人资料及“我的 > 健康档案”手填指标。Room v9/v10 以显式 8→9→10 迁移保存久坐、腰围、正式 VO₂max、HbA1c、eGFR、确认袖带血压和带日期医院血检；空白值不补正常值，无袖带戒指血压不进入 RHI。手填健康档案现为 Room-first，并通过稳定队列同步到 MySQL `rehealth_rhi_manual_health_input`，GET/PUT 按认证用户和 `updatedAt` 合并；这不改变 RHI 的研究预览属性，生产风险仍走 CVD-16。
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
