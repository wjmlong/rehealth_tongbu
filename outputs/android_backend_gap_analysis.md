# 安卓端所需的后端接口 & 后端缺口分析

> 分析对象：
> - 安卓端：`D:\rehealthAI\Android-apk`
> - 后端：`D:\rehealthAI\backend`（JeecgBoot 单体 `jeecg-module-rehealth` + 独立 `device-service` + Python `model-service`）
>
> 目的：把安卓「实际会发起的网络调用」与后端「实际已实现的端点」逐条比对，找出缺口（双向），并列出还需要补齐的**功能逻辑**。

---

## 0. 结论速览（TL;DR）

| 类别 | 数量 | 说明 |
|---|---|---|
| ✅ 安卓调用 ↔ 后端已实现（匹配） | 13 | 主链路已通 |
| 🔴 安卓调用 ↔ 后端**缺失**（真缺口） | 3 | `ring/snapshots`、`patient/mvp`、`patient/checkins` |
| 🟡 后端已实现 ↔ 安卓**未消费**（反向缺口） | 4 | `profile`、`measurements/recent`、`interventions/generate`、`agent/messages` |
| 🔥 **关键功能缺口（逻辑层）** | 1 | 「今日干预计划」无人生成（无调度器） |
| ⚠️ 设备绑定 | 1 | 后端已就绪，安卓未接通 `bindDevice` |

**一句话结论**：接口层面的「形状」基本对齐，瓶颈不在端点数量，而在两处——
（1）安卓旧 L2 客户端仍在调 3 个后端已**退役**的聚合/快照端点；
（2）后端缺一个**把「今日干预计划」预先算出来**的调度逻辑，否则 app 拉到的永远是空。

---

## 1. 安卓 → 后端 接口映射总表

> 路径均以 `{BASE}/jeecg-boot` 为前缀（`BASE` 默认 `http://10.0.2.2:8080`）。
> 鉴权统一走 `X-Access-Token`（来自 `POST /sys/mLogin`）。

| 功能域 | 安卓调用路径 | 方法 | 后端对应端点 | 后端文件 | 状态 |
|---|---|---|---|---|---|
| 登录 | `/sys/mLogin` | POST | ✅ `/sys/mLogin`（移动端） | `LoginController` | 匹配 |
| 短信注册 | `/sys/sms` | POST | ✅ `/sys/sms`（带 `X-Sign`） | `LoginController` | 匹配 |
| 注册 | `/sys/user/register` | POST | ✅ `/sys/user/register` | `SysUserController:1173` | 匹配 |
| 健康探针 | `/rehealth/mobile/health` | GET | ✅ `@IgnoreAuth` | `ReHealthMobileController:50` | 匹配 |
| 客户端配置 | `/rehealth/mobile/config` | GET | ✅ | `:56` | 匹配 |
| 遥测批量上报 | `/rehealth/mobile/measurements/batch` | POST | ✅（网关→`device-service`） | `DeviceTelemetryController:25` | 匹配 |
| 风险特征评估 | `/rehealth/mobile/features/evaluate` | POST | ✅（代理 model-service） | `:135` | 匹配 |
| 最新风险 | `/rehealth/mobile/risk/latest` | GET | ✅ | `:145` | 匹配 |
| 今日干预 | `/rehealth/mobile/interventions/today` | GET | ✅（但可能为空，见 §4） | `:161` | 匹配⚠️ |
| 干预反馈 | `/rehealth/mobile/interventions/{id}/feedback` | POST | ✅ | `:167` | 匹配 |
| 健康访谈提交 | `/rehealth/mobile/interviews` | POST | ✅ | `:78` | 匹配 |
| 最新访谈 | `/rehealth/mobile/interviews/latest` | GET | ✅ | `:90` | 匹配 |
| 个体归因 | `/rehealth/mobile/attribution/events` | POST | ✅（代理 PIAS） | `:176` | 匹配 |
| **戒指快照** | `/rehealth/mobile/ring/snapshots` | POST | 🔴 **已退役，后端无** | — | **真缺口** |
| **患者聚合** | `/rehealth/mobile/patient/mvp` | GET | 🔴 **后端无** | — | **真缺口** |
| **患者打卡** | `/rehealth/mobile/patient/checkins` | POST | 🔴 **后端无（遗留）** | — | **真缺口** |
| 健康档案 | `/rehealth/mobile/profile` | GET/PUT | 🟡 后端有，安卓未调 | `:62/68` | 反向缺口 |
| 遥测历史 | `/rehealth/mobile/measurements/recent` | GET | 🟡 后端有，安卓未调 | `:115` | 反向缺口 |
| 干预生成 | `/rehealth/mobile/interventions/generate` | POST | 🟡 后端有，安卓未调 | `:151` | 反向缺口 |
| 健康助手 | `/rehealth/mobile/agent/messages` | POST | 🟡 后端有，安卓未调（app 直连 DeepSeek） | `HealthAgentController` | 反向缺口 |
| 设备绑定 | `/rehealth/mobile/devices/bind` | POST | 🟢 后端有；安卓**未接通** | `:96` | 未接通 |

---

## 2. 🔴 真缺口：安卓在调，但后端没有（需要决定谁来改）

这三个端点都来自安卓的 **L2 原生 OkHttp 客户端**（`ReHealthBackendClient.kt`），不在主 Retrofit 接口（`ReHealthApi.kt`）里。后端在 `docs/MOBILE_API.md` 的「Retired Legacy Paths」中明确把它们列为**已退役**。

### 2.1 `POST /rehealth/mobile/ring/snapshots`（`ReHealthBackendClient.kt:88`）
- **安卓期望**：请求体含设备元信息 + 测量 + 睡眠 + 活动 + 信号；响应**内联**返回 `snapshotId / riskScore / riskLevel / mode / summary / modelVersion`（`ReHealthBackendClient.kt:121-126`）。
- **后端现状**：已退役。规范主路径是「`measurements/batch`（仅入库）→ `features/evaluate`（拿风险）→ `risk/latest`（回读）」三段式。
- **建议（二选一）**：
  - **推荐**：安卓把 `RingViewModel` 的快照上传拆成标准三步（先 `measurements/batch` 落库，再 `features/evaluate` 求风险，最后 `risk/latest` 读取），**删除** `ring/snapshots` 调用。后端无需新增端点。
  - 备选：若业务上确实要「一次上传即返回风险」，后端重开一个 `/ring/snapshots` 组合端点（但不推荐，违反「遥测入库与风险评估分离」的设计）。

### 2.2 `GET /rehealth/mobile/patient/mvp`（`ReHealthBackendClient.kt:94`）
- **安卓期望**：一次聚合返回 `profile + risk + interventionPlan + recentCheckins`（`PatientMvpPayload`）。
- **后端现状**：无该聚合端点。但所有子资源端点都已存在（`profile`、`risk/latest`、`interventions/today`）。
- **建议**：安卓直接在 UI 层并发调三个独立端点拼装即可，**无需后端新增**；若想减少请求数，后端可新增一个只读聚合 `GET /rehealth/mobile/patient/mvp`，但属于 Nice-to-have。

### 2.3 `POST /rehealth/mobile/patient/checkins`（`ReHealthBackendClient.kt:112`）
- **安卓现状**：遗留打卡，已无当前调用方（被「干预反馈」取代）。
- **后端现状**：无。
- **建议**：**直接删除**安卓侧该调用（连同 `PatientCheckInPayload`）。不需要后端做任何事。

---

## 3. 🟢 设备绑定：后端就绪，安卓未接通

- 后端 `POST /rehealth/mobile/devices/bind`（`ReHealthMobileController:96`）**已实现**，且有 `rehealth_device_binding` 表 + 内部身份授权链。
- 安卓侧：`DeviceBindingScreen` UI 与领域模型 `DeviceBindRequest/Result` 都在，但**激活的** `ReHealthApi.kt` 已无 `bindDevice` 方法——定义只残留在 `ReHealthApi.kt.backup:44`。
- **行动**：把 `bindDevice` 从 `.backup` 接回 `ReHealthApi.kt`（或新建等效方法），让设备绑定流程真正可触发。后端零改动。

---

## 4. 🔥 关键功能缺口（逻辑层，最该先解决）

### 4.1 「今日干预计划」无人生成（最高优先级）
- 安卓只做 `GET /rehealth/mobile/interventions/today`，**从不**调 `POST .../interventions/generate`。
- 后端 `interventions/generate` 存在，但**全代码库没有任何调度器/cron/xxl-job 去调用它**（已 grep 确认，rehealth 模块无 `@Scheduled`、无定时任务引用）。
- 结果：`interventions/today` 永远返回空 → app 干预页永远空白。
- **必需的补漏（二选一，推荐前者）**：
  - **后端加调度**：用 Spring `@Scheduled` 或 xxl-job，每天为「近 N 天有持久化风险结果的活跃用户」调一次 `generateIntervention`，把结果写进 `rehealth_intervention_plan`，使 `today` 有数据。
  - **或 app 兜底**：在打开干预页且无今日计划时，由 app 主动调一次 `generate`（需注意耗时与降级）。

### 4.2 归因历史依赖「持久化的风险结果」
- 后端 `attribution/events` 会**忽略 app 上送的 `risk_history`**，改为从 `software_db` 里该用户**已持久化的** `rehealth_cvd_risk_result` 自行组装（`ReHealthMobileServiceImpl`）。
- 这意味着：`features/evaluate` 必须**把风险结果落库**（`rehealth_cvd_risk_result`），否则归因永远 `status=accumulating`。
- 已知设计：用户需 ≥14 天持久化历史才会出因果效应。需确认 `features/evaluate` 的持久化分支已启用（尤其在 `software_db` 启用时）。

### 4.3 健康档案 `profile` 的初始化
- 后端有 `GET/PUT /rehealth/mobile/profile` 与 `rehealth_patient_profile` 表，但 app 从不调用。
- 风险/访谈数据目前是独立表，未归并到 profile。建议：用户首登或首次 `features/evaluate` 时，后端**自动建/补** `patient_profile` 行，避免后续 `profile` 始终为空。

### 4.4 `/config` 契约字段对齐
- app 期望 `MobileConfigResponse` 含 `apiVersion / endpoints / modelContract / limitations`。
- 需核对后端 `/config` 实际返回的字段与 app 解析字段**完全同名同义**，否则 app 自我校验会失败。

### 4.5 模型类错误码透传
- app 定义 `55001~55004`（模型服务配置/不可用/响应非法/契约违规）。
- 后端在 `features/evaluate` / `attribution/events` 调 model-service 失败时，需把这些码**透传**进 `JeecgResult.code`，app 才能给出正确文案与降级。

---

## 5. 🟡 反向缺口：后端已实现，安卓未消费（按需取舍）

| 端点 | 后端能力 | 安卓现状 | 建议 |
|---|---|---|---|
| `GET/PUT /rehealth/mobile/profile` | 读写健康档案 | 未使用 | 若要做「个人中心/档案页」应接通；否则可暂不消费 |
| `GET /rehealth/mobile/measurements/recent` | 拉取历史遥测（已脱敏） | 未使用 | 做「健康趋势图/历史」时接通；当前 MVP 可暂缓 |
| `POST /rehealth/mobile/interventions/generate` | 按需生成计划 | 未使用（靠调度器） | 由 §4.1 调度器消费；app 不必直调 |
| `POST /rehealth/mobile/agent/messages` | 后端代理健康助手（密钥在服务端） | app 直连外部 DeepSeek（`DeepSeekClient`） | **建议统一**：把 app 的 AI 问答改走后端 `agent/messages`，密钥不落端、便于审计与限流；或反过来下线后端端点 |

---

## 6. 已验证可放心的点（不是缺口）

- **遥测上报鉴权**：`device-service` 的 `X-Tenant-Id` / `X-ReHealth-Device-Id` / `X-Access-Token` 均为 `required=false`，身份由「内部身份端点」凭 token 重算。所以 app 主链路 `measurements/batch` 只发 `X-Access-Token` 是**够用的**，无需补发设备头。
- **模型服务不直连**：app 所有风险/归因都经 `/rehealth/mobile/...` 由后端代理，符合 AGENTS.md「模型推理不入安卓/不入 JeecgBoot 业务层」的设计。
- **注册链路**：`/sys/user/register` 后端存在且会校验短信码，app 注册流程可闭环。

---

## 7. 建议的优先级行动清单

### P0（阻断 MVP 可用，必须做）
1. **补「今日干预计划」调度器**（§4.1）—— 否则干预功能全空。
2. **接通设备绑定** `bindDevice`（§3）—— 否则戒指数据无法与用户关联。
3. **确认 `features/evaluate` 落库风险结果**（§4.2）—— 否则归因永远 accumulating。

### P1（影响体验/一致性）
4. **拆除 `ring/snapshots` / `patient/mvp` / `patient/checkins` 旧 L2 调用**（§2），改走标准三段式 + 独立端点。
5. **`/config` 字段对齐 + 模型错误码 55001–55004 透传**（§4.4/§4.5）。
6. **AI 问答归一到后端 `agent/messages`**（§5），密钥不下端。

### P2（增强，可排期）
7. 接通 `profile` 与 `measurements/recent`，做个人中心与健康趋势页（§5）。
8. 用户首登自动初始化 `patient_profile`（§4.3）。
9. （未来）医生/运营后台、群组归因、推送提醒——当前无对应端点，按需再开。

---

## 附：关键文件索引
- 安卓主接口：`Android-apk/app/src/main/java/com/rehealth/genie/network/ReHealthApi.kt`
- 安卓 L2 旧客户端：`Android-apk/app/src/main/java/com/rehealth/genie/network/ReHealthBackendClient.kt`
- 安卓旧定义备份：`Android-apk/app/src/main/java/com/rehealth/genie/network/ReHealthApi.kt.backup`
- 后端主控制器：`backend/jeecg-boot/jeecg-boot-module/jeccg-module-rehealth/src/main/java/org/jeecg/modules/rehealth/mobile/controller/ReHealthMobileController.java`
- 后端遥测服务：`backend/device-service/src/main/java/com/rehealth/device/api/DeviceTelemetryController.java`
- 后端业务实现：`backend/jeecg-boot/jeccg-boot-module/jeccg-module-rehealth/src/main/java/org/jeccg/modules/rehealth/service/impl/ReHealthMobileServiceImpl.java`
- 端点到端点契约文档：`backend/docs/MOBILE_API.md`
