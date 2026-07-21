# 睿禾健康 Android App 工程实施总纲

> 放置位置：仓库根目录 `ENGINEERING.md`  
> 适用仓库：`RehealthAI/Android-apk` 为主，`RehealthAI/backend` 与 `model-service` 配合  
> 当前目标：从 Demo App 走向“真实采集数据 + 本地特征 + 云端评分 + 干预建议 + 反馈闭环”的可用 MVP

> **Current implementation update (2026-07-13):** Android D2 telemetry queue,
> backend E2.1 hardware persistence, backend E1.1 authenticated software
> persistence, and the reviewed real Core16 model-service path are implemented
> and covered by automated tests recorded in their status files. Remaining
> release blockers are Android auth/typed feedback integration, cross-service
> evidence, real MySQL 8 QA, physical MRD QA, and release privacy/log review.
> Use `backend/docs/qa/PRODUCT_ARCHITECTURE_ACCEPTANCE_2026-07-13.md` for current
> planning; older milestone/gap lists below are retained as architecture history.

---

## 0. 当前判断

当前最稳的工程路线：

```text
RehealthAI/Android-apk        真正 Android App 主仓库，负责真机采集、Room 本地缓存、用户交互
RehealthAI/backend            JeecgBoot 后台，负责账号、权限、后台管理、医生/运营端、配置管理
RehealthAI/model-service      建议新建，Python FastAPI，负责 CatBoost/SHAP/LLM/归因
RehealthAI/rehealth-algorithms   模型训练、HealthAgent/PIAS 干预模拟与算法研究仓库
```

不要把所有能力塞进 Android，也不要把 CatBoost / SHAP / LLM 强行塞进 Java 后端。  
正确做法是：Android 采集和展示；Java 管用户、设备、后台；Python 跑模型和干预生成。

---

## 1. MVP 一句话目标

用户安装 App 后，可以完成：

```text
登录/采访 → 绑定 MRD 戒指 → 前后台采集真实生命体征 → 本地 Room 保存
→ 生成 CVD 16 维特征 → 上传后端 → 模型服务评分 → 返回风险等级和今日干预建议
→ 用户反馈执行情况 → 连续形成风险趋势和依从性记录
```

---

## 2. 必须坚持的工程原则

### 2.1 先真实可用，再追求完整愿景

本阶段不要做：

- 端侧大模型
- 端侧 LoRA
- 联邦学习
- 保险结算签名
- 复杂群体归因
- 医生端完整工作台

本阶段必须做：

- 真机稳定采集
- 后台采集不掉线
- 本地数据质量可控
- 16 维特征生成
- 后端上传和模型服务评分
- Mock PHM 替换为真实服务

### 2.2 所有健康数据先落本地，再异步上传

不要在 BLE 采集链路里直接调用网络接口。

正确顺序：

```text
BLE/MRD SDK → Repository → Room → FeatureExtractor → UploadQueue → Worker → Backend/API
```

### 2.3 Android 端只做轻量计算

Android 可做：

- 最近 7/14/30 天统计
- mean / std / slope
- 数据质量标记
- 缺失字段标记
- 本地健康记忆文件
- 离线队列

Android 暂不做：

- CatBoost 推理
- SHAP
- LLM
- 归因模型
- 大规模模型训练

### 2.4 后端 Java 和模型 Python 解耦

JeecgBoot 后端只做业务编排和数据管理：

```text
Android → backend/mobile API → model-service
```

Python `model-service` 独立部署，提供：

```text
POST /v1/cvd/risk/evaluate
POST /v1/cvd/intervention/generate
POST /v1/cvd/attribution/individual
GET  /health
```

---

## 3. Android-apk 当前模块定位

### 3.1 现有能力

已有：

- Compose UI 主流程
- 登录、健康采访、设备绑定、首页、数据页
- MRD 智能戒指 SDK 接入
- 真机验证心率、血氧、血压手动测量
- 体温定时采集
- 低频自动采集
- Room 本地保存

### 3.2 当前缺口

必须补齐：

1. Foreground Service：退后台/锁屏后仍能低频采集
2. WorkManager：后台任务兜底
3. FeatureExtractor：Room 数据转 CVD 16 维
4. UploadQueue：离线上传队列
5. Retrofit/OkHttp：正式后端接入
6. RemotePhmService：替换 MockPhmService
7. 真机 QA 和 Release Checklist

---

## 4. 推荐目录结构

### 4.1 Android 新增目录

```text
app/src/main/java/com/rehealth/genie/
  service/
    RingForegroundService.kt

  features/
    CvdFeatureVector.kt
    FeatureQuality.kt
    HealthFeatureExtractor.kt
    HealthMemorySnapshot.kt

  network/
    ApiClient.kt
    ReHealthApi.kt
    AuthInterceptor.kt
    dto/

  sync/
    UploadQueueEntity.kt
    UploadQueueDao.kt
    MeasurementSyncWorker.kt
    SyncRepository.kt

  phm/
    PhmService.kt
    LocalPhmService.kt
    RemotePhmService.kt
```

### 4.2 Android 依赖建议

```kotlin
implementation("com.squareup.retrofit2:retrofit:2.11.0")
implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
implementation("androidx.work:work-runtime-ktx:2.10.0")
implementation("androidx.security:security-crypto:1.1.0-alpha06")
```

新增生产依赖前，必须在 PR 说明中写清楚：

- 为什么需要
- 替代方案是什么
- 是否影响 APK 体积
- 是否涉及隐私/权限变化

---

## 5. CVD 16 维特征

### 5.1 标准输入

```kotlin
data class CvdFeatureVector(
    val age: Int,
    val gender: Int,
    val bmi: Double,
    val sbp: Double,
    val dbp: Double,
    val fastingGlucose: Double?,
    val totalCholesterol: Double?,
    val ldl: Double?,
    val hdl: Double?,
    val triglycerides: Double?,
    val exerciseDays: Int,
    val smoking: Int,
    val drinking: Int,
    val diabetesHistory: Int,
    val hypertensionHistory: Int,
    val familyHistory: Int,
    val featureQuality: Map<String, FeatureQuality>
)
```

### 5.2 字段来源

```text
age/gender/smoking/drinking/history   健康采访
bmi                                   健康采访/体检录入
sbp/dbp                               MRD 戒指血压 + 用户手动录入兜底
exerciseDays                          戒指步数/运动记录 + 用户确认
fastingGlucose/lipids                 体检录入/报告识别，MVP 可为空但必须标记缺失
```

### 5.3 数据质量

每个字段必须带质量状态：

```kotlin
enum class FeatureQuality {
    REAL_DEVICE,
    USER_REPORTED,
    CLINICAL_REPORT,
    DERIVED,
    MISSING,
    STALE,
    LOW_CONFIDENCE
}
```

模型接口可以接受缺失，但 UI 必须告诉用户哪些字段需要补充。

---

## 6. 后端 API 最小合同

移动端最小 API：

```text
POST /jeecg-boot/rehealth/mobile/auth/login
POST /jeecg-boot/rehealth/mobile/devices/bind
POST /jeecg-boot/rehealth/mobile/measurements/batch
POST /jeecg-boot/rehealth/mobile/features/evaluate
GET  /jeecg-boot/rehealth/mobile/risk/latest
GET  /jeecg-boot/rehealth/mobile/interventions/today
POST /jeecg-boot/rehealth/mobile/interventions/{id}/feedback
POST /jeecg-boot/rehealth/mobile/attribution/events
GET  /jeecg-boot/rehealth/mobile/config
```

### 6.1 上传策略

- 采集数据：批量上传
- 特征向量：每日或用户主动评估时上传
- 干预反馈：实时写本地，异步上传
- 网络失败：进入 UploadQueue，指数退避重试
- token 过期：暂停队列，刷新登录后继续

---

## 7. model-service 最小合同

建议新仓库：

```text
RehealthAI/model-service
  app/
    main.py
    schemas.py
    risk_scorer.py
    prescription_generator.py
    attribution.py
  models/
    rehealth_v2_final.pkl
    feature_cols.pkl
  tests/
  Dockerfile
  README.md
```

接口：

```text
POST /v1/cvd/risk/evaluate
POST /v1/cvd/intervention/generate
POST /v1/cvd/attribution/individual
GET  /health
```

风险评分返回：

```json
{
  "risk_score": 0.42,
  "risk_level": "moderate",
  "feature_contributions": {
    "age": 0.11,
    "sbp": 0.09,
    "exercise_days": -0.04
  },
  "model_version": "cvd-catboost-v2"
}
```

---

## 8. 4 周里程碑

### Week 1：能跑、能采、能存

验收：

- Android Studio 可编译
- Debug APK 可安装
- 可完成 onboarding
- 可绑定 MRD 戒指
- 可采集心率/血氧/血压/体温/睡眠/步数
- 退后台仍能低频采集
- Room 可查到真实记录

### Week 2：能提特征、能上传

验收：

- 生成 CVD 16 维
- 每个字段有来源和质量标记
- 离线时进入上传队列
- 联网后自动上传
- 后台能看到用户、设备、采集记录

### Week 3：能评分、能返回建议

验收：

- App 点击“今日评估”
- 后端收到 feature vector
- model-service 返回 risk_score
- App 展示风险等级、主要贡献因素、今日建议
- MockPhmService 不再驱动主 UI

### Week 4：能闭环、能试点

验收：

- 用户可完成/拒绝干预
- 反馈写入本地并上传
- 连续 7 天形成趋势
- 后台能看依从性
- 可导出单用户健康报告

---

## 9. Definition of Done

每个 Codex 任务完成时，必须满足：

1. 代码已提交到当前分支
2. `git status` 干净，或明确列出未提交原因
3. 能运行的测试已运行
4. 如果测试无法运行，说明原因和阻塞
5. 修改了数据结构时，补充 migration
6. 修改了接口时，补充 DTO / OpenAPI / README
7. 修改了用户可见行为时，补充 QA checklist
8. 不能用 mock 冒充真实功能，除非文件名/变量名明确标注 mock
9. 不吞异常，不只写 `catch {}` 或空 `onFailure`
10. 医疗建议必须保守，不能替代医生诊断

---

## 10. Codex 并行任务总表

| 线 | 名称 | 仓库 | 主要交付 |
|---|---|---|---|
| A | Android 构建健康线 | Android-apk | BUILD_NOTES.md，assembleDebug 通过 |
| B | BLE/后台采集线 | Android-apk | RingForegroundService，WorkManager |
| C | 特征工程线 | Android-apk | CvdFeatureVector，FeatureExtractor |
| D | 网络同步线 | Android-apk | Retrofit，UploadQueue，Worker |
| E | 后端移动 API 线 | backend | mobile API，基础表，Swagger |
| F | Python 模型服务线 | model-service | FastAPI 风险评分接口 |
| G | QA/验收线 | 全仓库 | QA_TEST_PLAN.md，RELEASE_CHECKLIST.md |

---

## 11. Codex 执行纪律

每次任务开头必须先做：

```text
1. 阅读 AGENTS.md
2. 阅读 ENGINEERING.md
3. 阅读与当前任务相关的 README / build.gradle / manifest / service 文件
4. 先输出 5-10 行计划
5. 再改代码
```

每次任务结束必须输出：

```text
Changed files
Implementation summary
Tests run
Manual QA steps
Known risks
Next task recommendation
```

---

## 12. 运行时部署架构（WSL2 + Docker）

> 新增记录（2026-07-15）：PIAS 归因算法已作为容器化服务跑在这台 PC 的 WSL2 上，
> 经 Android 归因界面端到端验证。本段沉淀运行时事实，避免后续被当成“本地脚本验证”。

### 12.1 PIAS 主力运行时 = `rehealth-algorithms/api` 容器（非 model-service stub）

仓库里存在**两套**名称都叫“PIAS / attribution”的实现，必须区分清楚：

| 服务 | 位置 | 端点 | 真实度 |
|---|---|---|---|
| **真实 PIAS** | `rehealth-algorithms/api`（FastAPI，调 `healthagent/pias/attribution/IndividualAttributor`） | `:8000/api/pias/v2/attribute/individual` | ✅ 真因果归因（Wilcoxon + Bootstrap ATT），需 ≥14 天历史、干预/对照各 ≥7 天 |
| **占位 stub** | `model-service/app/attribution.py` | `:8000/v1/cvd/attribution/individual` | ⚠️ 仅 `trend_delta = 末值 - 基线`，非真归因 |

- Android 归因屏 `PiasApi` 默认连 `/api/pias/v2/attribute/individual`（见 `local.properties` 的 `rehealth.model.service.base.url`）。**这条是对的，接的是真实 PIAS。**
- `model-service` 的 attribution 目前只是占位，不能用于生产归因结论。

### 12.2 容器化部署（Windows Docker Desktop + WSL2 后端）

- Docker 用 **Windows 已装好的 Docker Desktop**（WSL2 backend），**不要**在 WSL2 里 `apt install docker.io`。
  - 可执行：`C:\Program Files\Docker\Docker\resources\bin\docker`（v29.6.1）。
  - 首次 daemon 未起时，用 **PowerShell**（非 Bash，Bash 调 cmd.exe 被安全策略拦截）`Start-Process "Docker Desktop.exe"` 拉起。
- 部署套件位于 `rehealth-algorithms/docker/`：
  - `Dockerfile`：`python:3.12-slim`，COPY `api` + `healthagent`，`HEALTHCHECK /health`，`CMD uvicorn`。
  - `docker-compose.yml`：base 服务 `pias`，端口 8000。
  - `docker-compose.dev.yml`：源码挂载 `../api` + `../healthagent`，`--reload`，`restart: "no"`（开发免重建）。
  - `docker-compose.prod.yml`：不可变镜像，`restart: always`，healthcheck，`cpus: "2.0"`、`memory: 2G`（生产隔离）。
  - `docker/requirements.txt`：**已补全** numpy/scipy/scikit-learn/pandas/joblib（原 `api/requirements.txt` 只有 5 个 web 包，跑不起 PIAS）。
  - `.dockerignore`、`.env.example`、`README.md`（README 已更正为 Win Docker 流程）。
- 验证全过：`docker compose -f docker-compose.yml -f docker-compose.dev.yml build` 成功（镜像 `rehealth-pias:dev`）；容器 `docker-pias-1` 发布 `0.0.0.0:8000`；Win 侧与 WSL2 侧 `curl localhost:8000/health` 均 `{"status":"healthy"}`；POST `/api/pias/v2/attribute/individual` 返回真实结构（3 天 → `accumulating`；30 天 → `ready`）。
- 关键约束：**开发环境与生产环境通过 compose override 隔离**，生产用不可变镜像 + 资源上限，开发用源码挂载热重载，互不污染。

### 12.3 交互式归因报告（H5，二层下钻）

- 生成脚本：`tools/generate_live_h5.py` —— 从**活容器**取数，产出 `outputs/pias_attribution_report.html`。
- 页面含 ECharts 双线趋势（不干预 vs 干预 + CI 带）、指标卡、分层报告 tab，以及浏览器“打印/导出 PDF”按钮（PDF 走浏览器打印，不依赖 reportlab 离线生成）。
- **二层下钻（用户要求的可点进去界面）**：归因主页面有按钮「查看归因逐日明细 →」（风险轨迹面板）与「逐日明细 →」（ATT 面板），点击进入全屏 overlay，含三个 tab：
  - `ov-data`：历史（日期/类型/风险分 Y/干预日 Z）+ 预测（不干预/干预/CI 上下）合并表；
  - `ov-method`：ATT / Wilcoxon / Bootstrap 方法说明；
  - `ov-raw`：提交的 `risk_history` 原始 JSON。
  - 由 JS `openDetail / closeDetail / showOv` 控制。
- Android 归因屏 `ui/AttributionReportScreen.kt` 调用真实 `phmService.attributeIndividual`；无后端时显示错误态 + 重试，H5 是给用户在归因界面里查看/分享的可交互产物。

### 12.4 已知阻塞：assembleDebug 卡在预存损坏文件

- 编译进入 Kotlin 阶段后报 `app/src/main/java/com/rehealth/genie/ui/DeviceSettingsScreen.kt` 错误。
- 已修一处真错：45-46 行 `as Type\n.let{...}` 非法链式调用 → 改为 `MrdProtocolAdapter(application)`。
- 文件**仍缺 2 个 `}`**（圆括号/方括号均平衡，`tools/brace_check.py` 扫描确认 net open braces = 2）。更可疑：文件里**存在两个 `SystemInfoContent`**（433 带参 / 757 不带参），疑似被重复拼接/损坏。
- 该文件是既有戒指 BLE 设置 UI（907 行），与归因功能无关；Jul 14 已有可用 apk，说明属**预存损坏**，非本次工作引入。盲目补 `}` 可能引入回归。
- 待用户拍板（A/B/C）：
  - **A**：我谨慎定位缺 `}` 处修复（需更多读取，风险中）；
  - **B**：用户提供已知好版本覆盖；
  - **C**：确认该屏可暂时排除出编译（如 `exclude` 或移出 source set）以先出 apk，后续再修。
- 沙箱（本会话）无 JDK/Android SDK，**无法在此真正 assembleDebug**；Win 主机（wjmlong）工具链：Android Studio `D:\Android_Studio`、SDK `D:\Android_SDK`（android-36 / build-tools 36.0.0）、JDK `D:\Android_Studio\jbr`。真机编译须在 Win 主机 `cd Android-apk && ./gradlew assembleDebug`。
