# ReHealth 当前状态

> 最后核对：2026-08-20。本文档是仓库唯一的当前状态入口；历史验收记录只保存在
> `docs/archive/acceptance/`，不得作为当前实现或发布状态的依据。

当前待发布 Android 版本为 `1.0.0 (versionCode 1)`；该版本包含 HBand/云米连接方式选择，
显示版本和内部版本号均按产品要求固定为 `1.0.0 (1)`。
2026-08-21 已修复风险特征和 HBand 增量窗口的账号/设备作用域：风险输入仅查询当前登录用户，
HBand 近期窗口还必须匹配当前绑定设备与 `hband_wearable` 来源；后端资料失败不再生成本地启发式
风险或固定干预。设备绑定页已增加 HBand 后台自动采集启停入口，Android 13+ 启用前申请通知权限，
云米保持云端同步且不展示该入口。相关软件测试已补齐，物理锁屏、重启、功耗与准确性门禁仍未解除。
Release Lint 保留全部既有门禁，但临时禁用会因 AGP 8.10.1/Compose lint 类加载缺失而直接崩溃的
`MutableCollectionMutableState` 单项检测；升级整套 Android 工具链后必须恢复该检测。

## 2026-08-12 保险业务闭环（本地 MVP）

- 官网保险页面统一使用保险角色守卫；FastAPI 会话把 Jeecg Token 和当前租户保存在服务端，不接受浏览器伪造租户作为访问范围。保险机构成员管理已复用 Jeecg 租户成员、部门、角色和细粒度权限，支持租户内新建账号、邀请、部门/角色调整及启停；新建账号原子绑定当前租户和白名单业务角色，仅一次性返回临时密码，待接受邀请不能由管理员绕过确认直接启用。
- JeecgBoot/MySQL 已完成投保人、保单、理赔的幂等导入，风险工作台与风险池读取真实业务汇总；没有非 Mock 风险结果时评分和评估时间保持空值。
- 干预改善工作台已接入真实 JeecgBoot 聚合接口：看板、列表和详情统一执行“当前保险租户 + 已授权主体 + 当前员工负责关系”，详情组合 CVD 风险/Factor16、RHI 日趋势、独立 RDI 日趋势与结构化贡献、计划、APP 反馈、人工行动和 PIAS 归因；人群队列与机构概览的依从性优先按版本化机构计划近 28 日到期任务及每个任务最新执行事实加权计算，仅无版本化计划和任务时回退旧绑定事件。“应该采取什么行动”优先逐项展示当前生效的机构发布版本及当天任务/APP 反馈状态，人工行动作为独立补充记录展示待执行、执行中、已完成或已取消且不覆盖计划版本，无生效机构计划时才回退旧个人计划绑定。人工行动进度不等同于用户完成或健康改善；改善图限制为最近 14 个 RHI 观察点并在证据不足时使用警示色。保险详情不接收原始 `latest_measurements`，而是按已授权被保人的 `user_id` 从硬件库读取最近遥测并输出不含设备/记录标识的 `health_metrics` 白名单聚合值；RDI 结构化贡献可补足步数、睡眠或活动证据。仍缺少值时显示即将上线，不在真实模式生成伪造健康数据；本地 QA 种子值明确标记为合成数据。只有工作流已改善且归因为非 Mock、数据充分时才展示改善结论。机构管理员、部门经理和运营员可按 `rehealth:insurance:intervention:manage` 创建/更新人工行动并写审计，其他保险角色只读。
- JeecgBoot 已增加通用机构计划版本核心和保险适配 API：草稿可编辑，发布版本内容冻结，再次修改必须克隆新草稿；计划级 `lock_version` 防止并发覆盖，发布/撤回会维护生效区间并取消旧版本未来任务实例。查看、草稿编辑和发布分别使用 `rehealth:insurance:care-plan:view/manage/publish`，保险计划禁止诊断、用药和治疗类项目。迁移 `V20260819_1` 建立 5 张带注释的版本表，`V20260819_2` 增加不可变执行事实；App 聚合接口按生效版本展开 `daily`、`weekly`、`once` 任务并返回 28 日到期任务依从性。本地 `LOCAL_VERSIONED_CARE_PLAN_QA` 脚本可复用 36 个保险服务对象验证计划、版本、项目、任务实例及审计；医疗机构适配器仍待接入。
- 投保人风险分层已支持在 JeecgBoot 当前租户/负责人范围内按保单渠道和档案年龄过滤，官网可按相同条件导出最多 10000 条 CSV；页面已取消批量激励的多选、按钮和弹窗。服务端批量行动接口仍保留给受控流程，并继续执行管理权限、负责人范围、单事务回滚和审计约束。
- 已建立细分角色模板：查看员、分析员、运营员和审计员；本地 `admin` 仅用于验收授权，不替代正式账号配置。
- 已实现研究项目、不可变快照、持久化异步任务、PSM 结果审核、RWE 报告审核/Word 导出，以及不可变结算包的提交、审批、退回和重算状态机。
- RWE 默认读取 `docs/ReHealth_PSM_RWE_Report_Draft_V0.1.docx`；FastAPI 不直连数据库，PSM 输入和结果均通过 JeecgBoot API 持久化。
- App 已有保险计划绑定兼容链路和版本化机构计划聚合链路；归因页“个性化干预计划”优先展示当前机构、版本、28 日依从性与今日任务，并提供完成、部分完成、稍后和不适用反馈。反馈先写入 Room v19 durable queue，再按稳定 `occurrence_id` 幂等上传；界面监听具体队列记录，成功显示“已同步”，临时失败自动退避重试，永久拒绝或 10 次重试耗尽显示失败而非长期“正在同步”。服务端按到期任务权重计算，完成 100%、部分 50%、跳过 0%、不适用排除。绑定/撤回授权 UI 仍待完成。
- JeecgBoot 已提供认证的 `POST /rehealth/mobile/rhi/daily-snapshot` 和 `POST /rehealth/mobile/rdi/daily-snapshot`，均校验请求用户与登录用户一致并按用户/日期幂等保存日级聚合结果；RDI 另保存不含本地化证据文本的结构化贡献。APP 通用干预反馈只保留在个人计划链路，保险机构反馈必须按具体 `bindingId + planItemId` 上传，避免同一 APP 用户的反馈被复制给所有服务机构。
- 保险租户隔离已补齐多机构共享 APP 用户场景：成员部门调整只删除当前租户拥有的部门关系；租户专用角色优先于平台角色；APP 用户不进入后台机构成员目录，计划绑定、当前计划和反馈实时复核服务关系、租户、保单和授权状态，并按 `tenant_id` 隔离读取；任一机构关系失效都只阻断该机构绑定。
- 本地 `LOCAL_MULTI_INSURER_QA` 已提供 3 个合成保险租户、各 2 个业务部门、分层保险角色、待接受邀请和跨 3 租户共享审计账号的幂等种子，用于验证官网成员管理及 Jeecg 租户隔离；默认 `admin` 会加入这些 QA 租户、关联各机构根部门但保留原默认登录租户，需在 Jeecg 中切换到 `9101`–`9103` 后查看对应部门；种子会拒绝任何测试机构成员缺少当前租户有效部门的结果，固定测试编号不得用于 staging/production。
- 本地 `LOCAL_MULTI_INSURER_APP_QA` 已在上述 3 家机构上提供 14 个全局 APP 账号、36 条保险服务关系和 120 条员工负责关系；每家机构工作台有 12 位负责对象，四种状态各 3 位，每位详情至少包含 30 个风险点、7 个 RHI 点、7 个明确标记为 Mock 的 RDI 点、3 个 RDI 贡献、4 个 Factor16、3 个计划项、3 条 APP 反馈和 3 条人工行动，每条关系另有 118 天测量、睡眠、活动和饮食全链路数据。Factor16 和干预计划按血压、血脂、睡眠运动、代谢四类画像生成不同因素、实测值和行动标题，避免所有测试对象展示同一套字段。跨机构服务用户复用同一全局 APP 身份，业务关系、计划、反馈和员工范围仍按租户隔离；所有数据幂等、合成、非临床，密码统一为 `123456`。为覆盖本地 UI 的风险分布与改善状态，风险及指定改善组归因使用显式 `local_qa_fixture` 非 Mock 口径；机构、人员、产品、计划等业务展示字段使用自然业务文案，不附加“测试”“合成”或 `[LOCAL QA]`，但内部仍保留非模型制品名、`source_system`、`synthetic=true` 和 `clinicalUseAllowed=false`，禁止进入预发布或生产。APP 用户不加入 `sys_user_tenant`，风险看板、列表和详情从 `rehealth_insurance_subject` 识别服务用户，并对机构管理员、部门经理、分析员、运营员、查看员和审计员统一执行“保险角色 + 负责关系”SQL 范围过滤。
- 保险机构员工与平台 APP 用户的匹配、多机构服务、后台角色、负责人范围及团体客户员工场景已形成持续分析文档；目标模型明确投保人无需匿名，机构员工只使用 WEB 后台，所有角色均可读取其负责用户的全部业务数据、只在操作权限上区分。保险阶段确认优先复用 `sys_tenant/sys_user_tenant/sys_user_role`、`rehealth_insurance_subject`、`rehealth_insurance_subject_manager` 和现有审计表，不预建通用服务或跨领域负责人表。保险移动接口已从 `sys_user_tenant` 后台成员校验切换为有效服务关系校验；后台负责用户详情与负责人目标角色继续沿用现有保险工作台权限链路。
- 本地真实 MySQL 已验证幂等重放、错误密码、伪造租户头和跨租户 `403`。正式发布前仍需确定真实理赔来源，并以两个正式租户、最小权限业务账号和生产级 PSM 数据量复验。

## 2026-08-05 云米云端手表接入

- 后端已增加 `/rehealth/mobile/viomi/bind` 与 `/viomi/sync`，支持 S8、S9、GS20、GS17、A67、K9L 共用的 IMEI 验证和历史拉取流程。
- 心率、血压、血氧先经硬件入库端口持久化，再返回 Android 写入 Room；云端来源不会被 App 重复上传。
- App 已增加 `VIOMI_CLOUD` provider、产品目录与 IMEI 绑定 UI；生产包允许选择真实设备产品。
- Debug 与 Release 的正式连接选择统一收敛为“HBand”和“云米（IMEI 云端）”；
  两种构建默认 HBand，旧 MRD/RWFit 保存选择升级后迁移到 HBand，已有云米绑定保持不变。
- 绑定成功自动执行首次 31 天回填，后续按设备最新记录以 2 天重叠窗口增量同步；数据页仅展示已支持的心率、血氧、血压。
- Room v15 为测量增加用户与设备作用域；Room v16 将相同作用域扩展到睡眠、活动和信号/ECG；Room v17 增加按登录用户隔离、带 `is_mock` 与模型版本的 PIAS 展示缓存。HBand/MRD/RWFit 新采集及认证云端回填均写当前用户，UI、RHI/RDI、历史和上传按用户读取；14→15、15→16 与 16→17 迁移均保留既有数据。
- 真实联调仍需注入 `REHEALTH_VIOMI_APP_ID`、`REHEALTH_VIOMI_APP_KEY` 和 `REHEALTH_VIOMI_USER_ID`。

## 发布结论

当前 MVP 发布状态：**BLOCKED**。

主要阻塞项：

1. 物理 MRD/RWFit 戒指及 HBand 手表/手环与 Android 13+ 真机的扫描、重连、锁屏长时间采集、功耗和准确性 QA 尚未完成；HBand 已开始首次真机联调，完整重装后的连接验证仍待完成。
2. Android 运行时端到端证据仍需覆盖登录、采集、离线队列、遥测上传、风险评估和反馈回传。
3. 签名 Release APK 的真实设备运行时 logcat、权限、隐私和真实 HTTPS 全链路仍需验收。

2026-08-11 已将当前 `1.0.0 (versionCode 1)` 签名 Release APK 发布到睿禾健康官网，
首页行动按钮已由“了解小禾灵”改为“下载小禾灵”，公开下载地址为
`https://ruihehealth.cn/downloads/xiaoheling-1.0.0.apk`。公网响应已验证为 HTTP 200，文件大小为
`20661106` 字节，SHA-256 为
`DD3883823310575720C6AC9E468C04FDBE4E638E99B4323F248C4949A83A7138`。
官网桌面端下载卡片同时展示“手机扫码下载”二维码，二维码内容为上述 HTTPS APK 直链；
移动端保留直接下载按钮。公网二维码 PNG 已完成反向解码校验。
该记录仅表示分发入口可用，不解除上述物理设备、运行时和隐私验收阻塞。

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

2026-08-09 已补齐官网管理后台与 App 后端的最小安全只读连接：JeecgBoot 新增
`/rehealth/admin/v1/patients` 分页/搜索/风险等级筛选和患者详情，使用标准登录令牌、
`rehealth:admin:patient:view` 权限并校验当前管理员的活动租户成员关系。列表只读取当前租户内
存在类型化患者档案的用户，机构员工不会作为患者返回；同时批量读取 `software_db` 的最新 CVD
风险和干预计划，不逐用户访问硬件库；详情在目标成员校验
后仅调用一次 Device Service。由于软件档案/风险表暂未携带租户列，多活动租户用户会被
fail-closed 排除。Device Service 健康摘要的每条 TimescaleDB 查询均同时限定 `tenant_id`
与 `user_id`，并返回 `provenance` / `isSynthetic`；`LOCAL_TEST_SEED`、`ring_sim`、mock、demo、
sample 和 synthetic 来源仅在风险、RHI/RDI、Factor16 和干预计划分别明确标记为 Mock 时作为
测试预览展示；只有非空且全部属于已登记真实设备来源的详情才是 `verified_real`，空或未知来源
保持 `unknown` 并清除风险、指数和干预结果。测试预览不得进入临床统计或作为医疗建议。
凭据缺失或 Device Service 不可用时详情返回 503。列表逐条标记 `provenanceStatus=unknown`，BFF
不得将 unknown 记录计入临床风险统计。Compose 使用的 `jeecg-system-cloud-start` JAR 已包含并
启用权限 Flyway 迁移，但未授权任何默认角色。返回 DTO 不含手机号、邮箱和账号名，旧全库
`/rehealth/admin/v1/users` 已禁用。RHI 云端日快照端点仍未实现，本次未改变该状态。

| 范围 | 当前实现 |
| --- | --- |
| Android | 单一有效设备 Provider 路由；Release 只注册 HBand/Viomi Cloud，正式选择只展示 HBand MT116 蓝牙和云米 IMEI 云端，默认 HBand 并迁移旧 MRD/RWFit 选择；Debug 保留 MRD/RWFit/Mock 工程入口。已接入真实 SDK/BLE、云米 IMEI 绑定、Room、本地优先、Foreground Service、WorkManager、CVD 16 特征、认证感知上传队列及风险/干预/反馈 UI。HBand 已按能力接入心率、步数/活动、睡眠、血氧、HRV、血压、血糖、压力、MET、ECG、血液/身体成分与设备设置；MT116 的 HRV/压力优先走一键体检或真实历史，MET 只读真实历史，体温已移除，完整真机准确性仍待验收。 |
| Device Service | 遥测校验、TimescaleDB 持久化、幂等批次、Transactional Outbox、Kafka 发布；`telemetry-v2` 新增饮食行为记录，并向受信 Jeecg 调用提供租户/用户/自然日隔离的今日行为与近 7 日描述性变化 |
| JeecgBoot | 登录与权限、用户/设备绑定、结构化档案/访谈/干预/行为业务数据、风险/干预/反馈编排、LangChain4j 健康问答、视觉食物/OCR 和结构化生活方式干预、安全策略、用户/租户隔离会话历史和 software_db；公司官网本地联调已增加独立 `WEB` 登录客户端，复用现有用户/租户/角色并与 PC/APP 单点会话隔离，官网 BFF 只在服务端持有 Jeecg Token；注册短信已切换到独立阿里云号码认证服务 `Dypnsapi`，使用赠送登录/注册模板 `100001`，由 `SendSmsVerifyCode` 生成 6 位/5 分钟验证码并由 `CheckSmsVerifyCode` 云端核验，Redis 只保存发送会话、60 秒冷却、集群频控和注册锁，缺配置失败关闭；真实发送仍待 RAM 凭据和赠送签名到位后验收；每次生成干预都重新读取权威画像、最新访谈/风险和 Device Service 行为上下文，不采信客户端画像/风险；拍照分析只持久化验证后的结构化结果，不保存原图；模型证据继续保留版本化 JSON 快照 |
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
- Android 重新登录和进入个人页会刷新当前用户的类型化个人资料与最近健康问答，且不再受风险/干预接口失败影响；每个新登录令牌（以及有效会话的进程重启）会先通过认证 `GET /measurements/recent?limit=200` 幂等恢复当前用户的测量、睡眠和活动到 Room，同一客户端记录选最终累计值并触发本地 RHI 重算，失败不阻塞登录或 BLE；步数展示按本地自然日取累计最大值，睡眠数据页/个人页共用最终夜间记录口径；健康初识完成前先持久化 Room 队列，麦克风入口具备用途说明、运行时授权和拒绝后的设置引导。
- Android 归因页只读取今日已持久化干预，空计划由用户按钮显式生成并展示加载和失败状态；已有计划默认按 01–05 编号行动清单展开，显示 16 项健康输入说明、展开状态和整宽收起/展开按钮，不重复显示重新生成按钮。客户端兼容 snake_case/camelCase 计划响应。类型化档案字段保存后同步覆盖贡献因素展示并触发新一轮特征评估，经确认血压/血检保存同样触发重算。退出登录或未授权暂停会话由根导航直接返回登录页。
- 健康问答 Java 纵向链路已实现：可在 `model-service` 与 `langchain4j` 间配置切换，每轮装配类型化画像/访谈/风险/干预，问答中明确自述的五项基本资料先合并入库再装配同轮画像，MySQL 会话与消息按用户+租户隔离，Android Room v7 本地先写并管理会话；后端仍只有最新会话恢复，没有列表/删除契约，生产数据库迁移、真实 Provider 和跨设备手工 QA 仍待执行。
- 拍照行为记录已完成系统相机、私有临时 URI、方向校正/缩放重编码、认证上传、Java LangChain4j 视觉分析、用户/租户隔离幂等落库及首页/数据页今日展示；有效 FOOD 结果会在拍照成功或今日行为刷新时以服务端行为 ID 幂等写入当前用户 Room 今日餐食并复用 `telemetry-v2 dietRecords` 离线队列，缺少有效热量时不伪造营养数据；餐食/RHI/RDI ViewModel 按账号分 key，退出登录清空测量、睡眠、活动、ECG、风险和档案内存状态。服务端按设备本地日查询，Android 再按本地自然日边界过滤；真实手机拍摄的食物/OCR 准确性与失败恢复仍待手工 QA。
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
- 全量 Mock Debug 构建显式启用 `USE_FAKE_RING` 与 `SEED_FAKE_HEALTH_DATA` 时，
  118 天模拟戒指历史按当前登录账号幂等写入，当前日补齐血糖、MET、ECG、血液成分和
  身体成分；数据页仅在该构建中允许展示带模拟来源的高级指标。归因页明确标识为
  “Debug 模拟”的 PIAS 图表预览先写入 Room v17 `pias_attribution_cache` 再读取展示；
  普通 Debug 与 Release 不读取 `is_mock=1` 缓存，仍不把模拟数据冒充真实结果。
- Android 已保留独立本地 `RDI rdi-rule-1.0.1` 算法骨架：Room v8 通过显式
  7→8 迁移保存每日快照与贡献证据，但它不再驱动归因页。“健康改善得分”现由
  Android RHI Lite 透明引擎按所选 1/7/30/90 日窗口计算最后与第一个有效 RHI 的差值，
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
  HRV 当正式贡献；手工餐食录入和拍照行为记录已实现，有效 FOOD 拍照结果会进入用户隔离的今日餐食及结构化遥测队列；医院报告 OCR 自动入档仍未实现。
- Android 数据页已移除硬编码健康指数 `87`：风险卡标为 RDI-16，复用既有
  16 特征评估接口且只展示真实、有限、范围有效、非 Mock 的云端结果，不改变
  原特征提取规则；健康指数圆环展示 RHI-100；今日取当前自然日有效 RHI，7 日取近 7 个自然日有效 RHI 稳健中位数（至少 3 个有效日），
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

1. 确定真实保单/理赔数据来源与字段字典，用两个独立保险租户和四类最小权限账号复跑导入、PSM、RWE、结算及越权验收。
2. Docker 引擎恢复后补跑 Device Service 的 TimescaleDB/Testcontainers 集成测试，重点验证 V4 饮食 hypertable、压缩/保留策略和混合批次事务回滚。
3. 在发布环境挂载已审核模型制品并复核真实模型门禁。
4. 使用包含完整 JieLi/Nordic/JNI 依赖的 APK 完成 HBand 连接与 ECG 实时/历史波形复测，再完成 MRD/RWFit/HBand 与 Android 运行时端到端 QA。
5. 完成签名 Release APK 和真实部署环境验收。

## 历史证据

- 阶段验收快照：`docs/archive/acceptance/`
- G3 静态隐私审计：`docs/archive/qa/G3_PRIVACY_AUDIT_2026-07-20.md`
- 当前接口与行为以根 `README.md` 的文档索引所列契约为准。
