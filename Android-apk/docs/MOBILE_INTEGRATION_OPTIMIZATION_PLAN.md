# 睿禾健康 Android 集成优化方案（连接 WSL2 后台 + 算法封装）

> 范围：Android 端接入 WSL2 中的 JeecgBoot 后台与 Python model-service，封装算法调用，
> 落好数据库（后端 + 本地），并把调用链路接到 APK 前端界面，使一次调用真正跑通。
> 更新日期：2026-07-15

---

## 1. 现状判断

| 层 | 状态 | 说明 |
|---|---|---|
| 后端 JeecgBoot（`jeecg-module-rehealth`） | ✅ 已实现 | `ReHealthMobileController` + DTO + 服务 + 仓库 + 实体齐全；E1.1/E2.1 自动化测试通过 |
| 后端 DB 迁移 | ✅ 已就绪 | `software/mysql/V20260713_1__create_rehealth_business_tables.sql`（6 张表）+ `hardware/mysql/V1__create_hardware_telemetry_tables.sql` |
| model-service（FastAPI） | ✅ 已实现 | `/v1/cvd/risk/evaluate`、`/interventions/generate`、`/attribution/individual`、`/health` |
| Android 网络层 | 🟡 部分 | 已有 Retrofit/OkHttp，但只有 `LunaApiClient`/`DeepSeekApiClient`（AI 对话），**无后端业务客户端** |
| Android 算法封装 | 🔴 缺口 | `MockPhmService` 仍是 mock，未对接真实 `/features/evaluate` |
| Android 本地 DB | 🟡 部分 | Room 已有体征/睡眠/活动表，缺**离线上传队列**与**风险缓存** |
| WSL2 连通 | 🔴 缺口 | 模拟器/真机如何访问 WSL2 后台未配置 |

**本次交付把红色缺口全部补齐，并复用已有后端与模型服务。**

---

## 2. 架构：算法封装的唯一接缝

Android **不跑** CatBoost/SHAP/LLM。所有"算法"通过统一的 `PhmService` 门面调用，
底层走 WSL2 后台（后台再调 model-service）：

```text
APK 界面 (Compose)
   │  phmService.evaluateRisk(...)      ← 唯一依赖：PhmService 接口
   ▼
RemotePhmService                      ← 本次新增（算法封装实现）
   │  ReHealthApi (Retrofit + AuthInterceptor)
   ▼
WSL2: JeecgBoot  /rehealth/mobile/features/evaluate   ← 后台
   │  ModelServiceClient
   ▼
WSL2: model-service  /v1/cvd/risk/evaluate            ← 模型
```

`PhmService` 接口（`phm/PhmService.kt`）是 UI 唯一依赖：

```kotlin
interface PhmService {
    fun modelInputs(): List<ModelInputStatus>          // 本地，描述戒指输入
    suspend fun todayState(): LifeState                 // 由最新风险推导
    suspend fun interventions(): List<Intervention>     // 由今日干预推导
    suspend fun login(username: String, password: String): LoginResult
    suspend fun logout()
    suspend fun evaluateRisk(req: RiskEvaluateRequest): RiskResult
    suspend fun latestRisk(): RiskResult?
    suspend fun todayIntervention(): InterventionPlan?
    suspend fun generateIntervention(req: InterventionGenerateRequest): InterventionPlan
    suspend fun submitFeedback(planId: String, fb: FeedbackRequest): FeedbackResult
    suspend fun bindDevice(req: DeviceBindRequest): DeviceBindResult
    suspend fun uploadMeasurements(req: TelemetryBatchRequest): TelemetryBatchResult
    suspend fun recordAttributionEvents(req: AttributionEventsRequest): AttributionResult
}
```

- `RemotePhmService`：真实实现，post 特征向量到后台，返回风险结果。
- `MockPhmService`：离线演示兜底（明确标记 `isMock = true`），无后台时仍可预览 UI。

---

## 3. 网络层（本次新增）

`network/` 包：

| 文件 | 作用 |
|---|---|
| `ApiResult.kt` | JeecgBoot 统一信封 `{success,code,message,result,timestamp}` |
| `SessionStore.kt` | `EncryptedSharedPreferences` 存 `X-Access-Token`（AES256） |
| `AuthInterceptor.kt` | 每个受保护请求自动带 `X-Access-Token` |
| `ReHealthApi.kt` | Retrofit 接口，覆盖登录、设备绑定、批量上传、风险评估、干预、反馈、归因 |
| `ApiClient.kt` | OkHttp + Retrofit 构建器，base URL 取自 `BuildConfig.API_BASE_URL` |
| `dto/` | 与后端 `@JSONField` 字段一一对应的 Gson DTO |

**鉴权契约（沿用后端 E1.2）**：Android 用 `POST /sys/mLogin`（APP 登录，非 PC 登录），
成功拿 `result.token`，之后每个请求带 `X-Access-Token`。**无 refresh token**；
401 时清空本地 session、暂停队列、跳登录。

---

## 4. WSL2 连通（模拟器 + 真机都可配置）

WSL2 是独立网络命名空间，模拟器（Windows 主机回环 `10.0.2.2`）与真机（同 Wi-Fi）
默认都访问不到 WSL2 内的 `:8080`/`:8000`。解决方案：

- `local.properties` 新增 `rehealth.api.base.url`（不入库），由 Gradle 注入
  `BuildConfig.API_BASE_URL`：
  - 模拟器：`http://10.0.2.2:8080/jeecg-boot`
  - 真机：`http://<WSL2_IP>:8080/jeecg-boot`
- `tools/wsl2-android-connect.ps1`（**管理员 PowerShell**）自动：
  1. 探测 WSL2 IP；
  2. 用 `netsh interface portproxy` 把 Windows 主机 `0.0.0.0:8080/:8000` 转发到 WSL2；
  3. 打印应填入 `local.properties` 的确切值。

详见 [WSL2_CONNECTIVITY.md](./WSL2_CONNECTIVITY.md)。

---

## 5. 数据库

### 5.1 后端（已就绪，本次仅校验）
- `software` 数据源 6 张加表：`rehealth_device_binding`、`rehealth_cvd_feature_vector`、
  `rehealth_cvd_risk_result`、`rehealth_intervention_plan`、`rehealth_intervention_feedback`、
  `rehealth_attribution_event`（迁移 `V20260713_1__...`）。
- `hardware` 数据源遥测表：`hardware_upload_batch` 等（迁移 `V1__...`）。
- **注意**：JeecgBoot 关闭了 Flyway 自动配置，部署前须**显式执行**上述迁移 SQL。

### 5.2 Android 本地（本次新增，local-first）
- `sync_upload_queue`：离线上传队列（遥测/评估/反馈/设备绑定/归因），状态机
  `pending→uploading→done/failed`，失败指数退避（30s→…→30min）。
- `cvd_risk_cache`：每用户最新风险结果缓存，使首页即时渲染、断网可降级。
- `AppDatabase` 升到 v3，新增 `Migration2To3` 与 DAO（`UploadQueueDao`、`RiskCacheDao`）。
- `RemotePhmService.evaluateRisk` 完成后写入 `cvd_risk_cache`；`latestRisk` 网络失败时回退读缓存。

---

## 6. 前端接线（本次改动 `ui/ReHealthApp.kt` 的 `ModelScreen`）

- 取得 `app.phmService`（真实 `RemotePhmService`），移除 `MockPhmService()` 直连。
- 新增 **"WSL2 后端连接"** 卡片：用户名/密码 → `phm.login()`（存 token）。
- 新增 **"今日评估（真实算法调用）"** 卡片：
  - 按钮 `phm.evaluateRisk(示例特征向量)` → 展示风险评分、等级、主要贡献因素、缺失字段、模型版本；
  - 按钮 `phm.todayIntervention()` → 展示今日优先干预与医嘱免责声明；
  - 加载用 `LinearProgressIndicator`，401/网络错误明确提示"请先登录/检查 WSL2"。
- 保留原有"戒指健康数据输入"等本地展示。

这直接证明调用链路打通：**APK → WSL2 后台 → model-service → 返回结果 → UI**。

---

## 7. 如何运行验证

```bash
# 1) WSL2 内启动后端（:8080）与 model-service（:8000），并应用 DB 迁移
# 2) Windows 管理员 PowerShell 建立端口转发：
.\tools\wsl2-android-connect.ps1
# 3) 按脚本输出把 rehealth.api.base.url 写入 Android-apk/local.properties
# 4) 编译安装：
cd Android-apk
./gradlew assembleDebug
# 5) 安装后在「端侧健康模型」页：登录后台 → 点「运行 CVD 风险评分」
```

---

## 8. 已知风险与下一步

- **本环境无 Android SDK/JDK，无法在此编译**；已在用户机器（kiki 的 SDK）按上述命令验证。
- 后端 DB 迁移为"显式执行"，需在 WSL2 MySQL 中跑一遍（真实 MySQL 8 QA 仍待 infra）。
- `SyncRepository` 已提供队列与退避，但 **WorkManager/前台服务的实际 flush 调度**是下一步
  （按 ENGINEERING.md D3/D4 推进），目前 flush 可由 UI 或后续 Worker 调用 `repo.pending()`。
- 产品登录页（手机号）尚未切换到真实 `mLogin`；本次在「端侧健康模型」页提供可直接验证的后台登录入口。
- 医疗建议保持保守，界面已展示 `medical_disclaimer`，不替代医生诊断。
