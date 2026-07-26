# 修改计划：补齐安卓 ↔ 后端缺口（ReHealth AI）

> 配套分析：`outputs/android_backend_gap_analysis.md`
> 范围：让安卓 MVP 全链路（登录→设备绑定→遥测→风险→干预→访谈→归因）在现有后端上真正跑通。
> 已现场核对的结论：
> - `features/evaluate` **已落库**（`ReHealthMobileServiceImpl:176` `saveRiskResult`），归因读 `findAttributionHistory` 组装 —— **P0 落库项已满足**，仅需确认 `software_db` 启用。
> - `device-service` 的 tenant/device 头为 `required=false`，主链路只发 `X-Access-Token` 够用 —— 无需补头。
> - 后端**没有任何调度器**调 `interventions/generate` —— 这是唯一的后端功能缺口。

---

## 执行总原则（来自 AGENTS.md）
- 最小安全改动；真实实现优先于 mock。
- BLE 采集与上传解耦；本地先持久化再上传；后端不可用不阻断采集。
- 不把模型推理塞进安卓/JeecgBoot 业务层（已合规，保持）。
- 不记原始健康数据/令牌/手机号到日志。
- 医疗建议保守，不宣称诊断。

---

## 阶段 0 — 契约对齐（不写业务代码，先消除歧义）
**目标**：避免后面改完发现字段对不上。

| # | 动作 | 文件 | 产出 |
|---|---|---|---|
| 0.1 | 比对 `MobileConfigResponse`（安卓）与后端 `/config` 返回字段（apiVersion/endpoints/modelContract/limitations） | `Android-apk/.../network/dto/FeatureEvaluationDtos.kt`、`backend/.../controller/ReHealthMobileController.java:56`、`docs/MOBILE_API.md` | 字段差异清单 |
| 0.2 | 比对 `FeatureEvaluateRequest`/`RiskResultDto`（安卓）与 model-service `/v1/cvd/risk/evaluate` 契约 | `network/dto/FeatureEvaluationDtos.kt`、`backend/.../model/impl/HttpModelServiceClient.java` | 字段差异清单 |
| 0.3 | 确认 `software_db` 在部署中已启用（否则 risk/attribution 持久化全为 null） | `deploy/rehealth/*.env`、`application.yml` | 启用状态确认 |

> 阶段 0 不阻塞后续，可与阶段 1 并行；差异随改随修。

---

## 阶段 1 — P0 后端：今日干预计划调度器（唯一的功能缺口）

**现状**：`POST /interventions/generate` 存在且会落库（`saveInterventionPlan`，`:207`），但无人定时调用；安卓只 `GET /interventions/today` → 永远空。

### 1.1 新增调度器
- **新建** `backend/.../rehealth/scheduler/InterventionPlanScheduler.java`
  - 类上加 `@Component`；模块主配置加 `@EnableScheduling`（若 JeecgBoot 已全局开启可省）。
  - 方法 `@Scheduled(cron = "${rehealth.intervention.schedule.cron:0 0 2 * * *}")`（默认每日 02:00）。
  - 逻辑：
    1. 查「近 `rehealth.intervention.eligible-days`（默认 30）天内有持久化风险结果、且今天还没有干预计划的用户」→ 需新增仓储方法 `findUserIdsEligibleForDailyPlan(today)`。
    2. 对每个用户构造 `InterventionGenerateRequestDto`（后端自行从 profile/risk 历史组装，app 不传）。
    3. 调 `mobileService.generateIntervention(userId, request)`（复用现有落库逻辑，幂等：已存在今日计划则跳过）。
    4. 分批 + 限流（如每批 20、批间 `Thread.sleep` 或队列），避免打爆 model-service；失败记入 `rehealth_model_request_log`，不中断整体。
- **新增仓储方法**：`ReHealthBusinessRepository.findUserIdsEligibleForDailyPlan(LocalDate)` + `JdbcSoftwareDbReHealthBusinessRepository` 的 SQL（按 `rehealth_cvd_risk_result` 最近日期 + `LEFT JOIN` 今日 `rehealth_intervention_plan` 判空）。
- **配置项**（写 `application.yml` / env）：
  - `rehealth.intervention.schedule.cron`
  - `rehealth.intervention.eligible-days`
  - `rehealth.intervention.batch-size`

### 1.2 兜底（可选，P0 保险）
- 在 `ReHealthMobileController.interventionsToday` 中：若返回 null 且当前用户在「已评估且今天尚未生成」状态，同步触发一次 `generateIntervention`（带超时与降级，避免首屏卡顿）。—— 推荐加上，保证即使调度器未跑也有数据。

### 1.3 验证
- 启动后端（Docker `up -d` 或本地 `assembleDebug` 等价后端）。
- `mLogin` → `features/evaluate`（落库）→ 手动触发调度（临时把 cron 改为近一分钟，或暴露一个 `@PostConstruct` 测试入口/actuator）→ `GET /interventions/today` 应返回非空计划。
- 检查 `rehealth_intervention_plan` 表有行、`rehealth_model_request_log` 有调用记录。

---

## 阶段 2 — P0 安卓：接通设备绑定

**现状**：后端 `devices/bind` 已就绪；安卓激活的 `ReHealthApi.kt` 没有 `bindDevice`（定义残留在 `ReHealthApi.kt.backup:44`）。

### 2.1 接回接口
- 把 `.backup:44` 的 `bindDevice` 方法搬回 `ReHealthApi.kt`（用 `FeatureEvaluationDtos.kt` 同级的 DTO 或现有 `DeviceBindRequest/Result`，注意 `.backup` 里用的是旧 `DeviceBindRequestDto`/`ApiResult` 信封 —— 需对齐到激活层用的 `JeecgResult` 信封与 `DeviceBindRequest/Result` 领域模型）。

### 2.2 接线 UI/ViewModel
- `DeviceBindingScreen.kt` + 对应 ViewModel 当前走 `PhmService`/`MockPhmService`；改为调用 `AuthenticatedApiClient.bindDevice(...)`（与登录同令牌链路）。
- 绑定成功后本地保存 `deviceId`（供 `measurements/batch` 的 `deviceId` 字段、以及后续 `X-ReHealth-Device-Id` 使用）。

### 2.3 验证
- `./gradlew assembleDebug` 通过；MuMu 模拟器跑通「扫描/选设备 → 绑定 → 后端 `rehealth_device_binding` 出现行」。

---

## 阶段 3 — P1 安卓：清理旧 L2 调用 + 契约对齐 + AI 问答归一

### 3.1 拆除 `ring/snapshots` / `patient/mvp` / `patient/checkins`
- 这三个来自 `ReHealthBackendClient.kt`（L2），后端已退役。
- `RingViewModel.kt:419` 的 `uploadRingSnapshot`（期望响应内联 `riskScore` 等）→ **重构为标准三段式**：
  1. 调已队列化的 `SyncRepository.uploadMeasurement`（→ `measurements/batch` 落库）；
  2. 调 `features/evaluate` 求风险；
  3. 调 `risk/latest` 回读展示。
- 删除 `ReHealthBackendClient.kt` 中 `ring/snapshots`、`patient/mvp`、`patient/checkins` 三处调用及 `PatientMvpPayload`/`PatientCheckInPayload`（无引用后）。
- 保留 `ReHealthBackendClient` 仅作清理对象或整文件标记 `.disabled`（确认无其他调用方）。

### 3.2 `/config` 字段对齐 + 模型错误码透传
- 后端 `/config` 返回字段对齐 `MobileConfigResponse`（阶段 0.1）。
- 后端 `features/evaluate`/`attribution/events` 调 model-service 失败时，把 `55001~55004` 写进 `JeecgResult.code` 透传（核对 `HttpModelServiceClient` 的异常处理是否已映射；若未映射则补 `try/catch` → 对应 code）。
- 安卓侧 `RemotePhmError.kt` 已定义这些码，确认解析路径能拿到 `result.code`。

### 3.3 AI 问答改走后端 `agent/messages`
- 现状：app 直连外部 DeepSeek（`DeepSeekClient`），密钥在端上（`BuildConfig.DEEPSEEK_API_KEY`）。
- 推荐：新增 `HealthAgentController` 代理路径调用（`HealthChatService` 优先走后端 `/agent/messages`，失败或无后端时回退本地规则/直连）。
- 决策点（需你确认）：是「完全改用后端代理、下掉端上 DeepSeek key」，还是「保留直连作为后端不可用时的兜底」。后者更稳但密钥仍留端。

### 3.4 验证
- `./gradlew assembleDebug` 通过；模拟器跑全链路：登录→绑定→遥测上传（队列+重试）→风险→干预(今日有数据)→访谈→归因；观察 401 触发重登、离线队列 dead-letter。

---

## 阶段 4 — P2（按需，不阻塞 MVP）
| # | 动作 | 说明 |
|---|---|---|
| 4.1 | 接通 `GET/PUT /profile` | 做「个人中心/档案页」；首登自动初始化 `patient_profile`（阶段 0.3 启用后，`mLogin` 或首次 `features/evaluate` 时 `savePatientProfile` 兜底建行）。 |
| 4.2 | 接通 `GET /measurements/recent` | 做「健康趋势/历史」图表页（数据已脱敏）。 |
| 4.3 | （未来）医生/运营后台、群组归因、推送提醒 | 当前后端无对应端点，需另开 `rehealth` 管理控制器与调度，排期处理。 |

---

## 阶段 5 — 联调与回归
1. 后端 Docker 全量 `up -d`（`edge:8080` → `gateway` → `jeecg-system`/`device-service`/`model-service`）。
2. 安卓 `local.properties` 设 `rehealth.api.base.url=http://10.0.2.2:8080/jeecg-boot/`，MuMu 模拟器安装 `app-debug.apk`。
3. 端到端走查 + 检查：
   - 401 处理、上传队列指数退避与 dead-letter；
   - model-service 不可用时的 `5500x` 文案与降级；
   - 归因在 <14 天历史时返回 `accumulating` 属预期。
4. `git status` → 提交各阶段改动（后端 / 安卓分仓库或同仓分提交），留变更清单。

---

## 风险与注意事项
- **`software_db` 必须启用**：否则 risk/attribution/profile 全 null，阶段 1/2/4 都白做（先确认 0.3）。
- **调度器打爆 model-service**：务必限流 + 幂等 + 失败隔离；建议先在预发用短 cron 验证。
- **`bindDevice` 信封对齐**：`.backup` 用的是旧 `ApiResult` 信封，激活层用 `JeecgResult`，搬回时务必统一，否则解析失败。
- **`ring/snapshots` 重构**：务必保留「先落库再评估」的语义，不要为图省事让 app 直接等内联风险（那正是后端退役它的原因）。
- **AI 问答密钥**：若决定完全走后端，需同步下线 `BuildConfig.DEEPSEEK_API_KEY` 注入，避免密钥残留端上。

---

## 建议执行顺序（依赖关系）
```
阶段0 (契约对齐) ──┬─> 阶段1 (后端调度器) ──> 阶段5 联调
                   ├─> 阶段2 (安卓绑定) ──────┤
                   └─> 阶段3 (清理+对齐) ─────┘
阶段4 (P2) 可随时插入，不阻塞主线
```
阶段 1 与阶段 2/3 相互独立，可并行；阶段 5 依赖前三者完成。

---

## 交付物清单（每阶段完成即更新）
- 后端：新增 `InterventionPlanScheduler.java` + 仓储方法 + 配置项；`/config` 与错误码透传微调。
- 安卓：接回 `bindDevice` + ViewModel 接线；重构 `RingViewModel` 三段式；清理 `ReHealthBackendClient` 旧调用；`HealthChatService` 接后端 agent。
- 文档：`outputs/android_backend_gap_analysis.md`（已出）、本计划、各阶段 commit 说明。
