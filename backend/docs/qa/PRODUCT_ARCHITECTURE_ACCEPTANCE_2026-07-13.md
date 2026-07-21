# ReHealth 产品架构与模型演进建议验收（2026-07-13）

> 范围：审核“当前工程已进入产品化架构收敛阶段、应优先建设 Model SDK + Model Registry、暂缓 E1.1”的判断。
>
> 结论口径：以当前代码、状态记录和可复现验证为准。研究代码存在不等于产品链路可用；接口边界存在不等于数据已经持久化或真机验收通过。

## 1. 总体结论

原判断的架构方向大体正确：Android 不应嵌入 CatBoost、SHAP、LLM 或 PIAS；模型应在云端独立迭代；feature schema 和 model version 必须显式治理。

但优先级判断不正确。当前最大的产品阻塞不是“缺少 Model Registry”，而是尚未完成可恢复、可追踪的业务闭环：

1. Android D2 上传队列已有代码，但 `testDebugUnitTest`、`assembleDebug` 和 Android 到 backend 的联调证据仍缺失。
2. backend E2.1 durable telemetry 已实现并通过 13 项自动化测试，但尚未在真实 MySQL 上完成迁移、重启和重复批次 QA。
3. backend E1.1 仍是明确的空实现；风险结果、今日干预、反馈和 attribution event 不持久化。
4. Android 用户打卡仍走 legacy `ReHealthBackendClient` 或本地回退，没有接入已经存在的 typed intervention feedback API。
5. model-service 已有真实 Core16 scorer、artifact loader、mock/real 安全切换和 M1 registry/trace skeleton。因此“Model Registry 是当前最大缺失”与代码现状不符。
6. F3b 的八字段候选是 `research_only_not_deployable_to_cvd_16_api`。它没有使用真实手环 HR、HRV、SpO2、sleep 训练数据，也没有替换生产 `cvd-16-v1`。

当前阶段可描述为“产品化架构收敛中”，但不能描述为“主要只剩模型迭代架构”。MVP 仍处于最终闭环和发布证据补齐阶段。

## 2. 逐项审核

### 2.1 当前模块状态

| 原判断 | 判定 | 当前证据与修正 |
| --- | --- | --- |
| Android 已有 MRD 真实采集 | 部分正确 | `MrdBleRingRepository` 和协议适配代码存在，且本地先写 Room；但仍缺 Android 13+ 真机、锁屏、杀进程、蓝牙恢复等发布证据，不能把“代码存在”写成“真机发布验收完成”。 |
| Android 已有 Room 缓存 | 正确 | ring measurement、sleep、activity、signal metadata 及 D2 `upload_queue` 均使用 Room；D2 有 2->3 add-only migration。 |
| Android 已有 Background Service | 部分正确 | Foreground Service、恢复 Worker 和 boot receiver 已实现；缺真机长时间稳定性证据。 |
| Android 已有 FeatureExtractor | 正确 | `HealthFeatureExtractor` 生成稳定 CVD16 和逐字段质量信息。当前是 app 内 module/contract，不是独立发布的 Android SDK。 |
| Android 已有 Canonical Risk UI Path | 正确 | UI 通过 `RemotePhmService` 和 `/features/evaluate` 获取风险；legacy 路径不再是主风险路径。网络失败时仍有显式 mock fallback，UI 必须继续明确标记。 |
| Android 已有 RemotePhmService | 正确 | 风险、latest risk、today intervention 和 typed feedback 方法存在；但 `RingViewModel.submitCheckIn` 尚未使用 typed feedback 方法。 |
| backend 已有独立 rehealth module | 正确 | JeecgBoot `jeecg-module-rehealth` 已形成 mobile API、model client、ingest 和 repository 边界。 |
| backend 已有 E2 ingest boundary | 正确但已过时 | E2 boundary 已进一步升级为 E2.1 direct transactional hardware_db writer；真实 MySQL manual QA 仍待完成。 |
| backend 已有 ModelServiceClient | 正确 | 风险、干预和 attribution 调用封装在 client abstraction 后。 |
| backend API 契约完成 | 部分正确 | DTO 和端点已定义，但 E1.1 repository 仍返回 empty/null 或 `persisted=false`；契约可调用不等于业务闭环完成。 |
| model-service 已有真模型接口边界 | 正确 | FastAPI typed API 和真实 Core16 scorer 已完成。 |
| model-service 已有 artifact loader | 正确 | loader 校验本地 artifact、feature order、有限预测结果和 fallback 状态。 |
| model-service 已有 mock/real 切换 | 正确 | `/health` 和 risk response 区分 `real_available`、`real_unavailable`、`mock`，只有真实预测成功才允许 `is_mock=false`。 |
| rehealth-algorithms 已有训练、wearable、PIAS、HealthAgent | 部分正确 | 训练、PIAS/HealthAgent 研究实现和 F3b Pareto 搜索存在；“wearable model”目前主要是低负担问卷/基础字段候选，不是基于真实手环 HR/HRV/SpO2/sleep 对齐数据的生产模型。PIAS 也尚未形成 App 可调用的完整 typed production chain。 |

### 2.2 “Model SDK + Model Service”建议

| 建议 | 判定 | 修正 |
| --- | --- | --- |
| 不把模型放进 APK | 正确 | 保持现有硬边界。Android 不承载 Python 模型、SHAP、LLM 或 PIAS 推理。 |
| Android 只负责传感器、采集和特征准备 | 正确 | 还必须负责本地持久化、质量/来源标记、离线队列、用户授权和结果展示。 |
| 建设 Android Feature SDK | 方向正确但当前不应优先拆 SDK | MVP 先稳定 `feature_schema_version`、DTO、质量语义和 contract tests。只有第二个消费者出现或需要独立发布节奏时，再从 app 内 module 提取 SDK，避免为尚未稳定的 wearable-v2 schema制造二次迁移。 |
| 模型更新不改 App | 有条件正确 | 同一 feature schema 下可只更新 model-service artifact。若输入从 `cvd-16-v1` 改为 HR/HRV/sleep/SpO2 等 wearable schema，Android 仍必须更新采集、聚合、授权和 DTO；不能承诺所有模型升级都无需更新 App。 |
| 请求带 `model_version: latest` | 不建议 | Android/普通客户端不应决定生产模型。客户端发送稳定 `feature_schema_version`；backend/model-service 由受控配置、cohort 和 rollback policy 选择模型，并在 `model_trace` 返回实际 model version。 |
| 响应返回 model/feature version 和 explanation | 正确 | 当前已有 `model_version`、`model_trace.feature_schema_version`、artifact/scorer mode 和 contributions 字段；真实 contribution 目前仍是 deterministic zero fallback，不能称为 SHAP explanation。 |

### 2.3 “Model Registry 是最大缺失”判断

判定：错误，但存在后续增强空间。

当前已有：

- `app/model_registry.py` 的 `model-registry-v1` governance skeleton。
- 稳定 `cvd-16-v1` schema 标识。
- active scorer、artifact name、fallback reason 和 request trace。
- real/mock 安全状态与 Android/backend nullable pass-through contract。

当前尚未具备：

- 多模型、多 schema 的运行时路由。
- signed manifest、artifact digest 强制校验和不可变 artifact store。
- candidate -> staging -> production 审批流、灰度、回滚和环境隔离。
- registry 持久化、管理 API 和审计记录。

因此它应命名为后续 `M2_model_release_registry`，而不是抢占 MVP 闭环任务。建议的版本目录结构可以采用，但 `approved: true` 不能由训练脚本自行写入后就获得生产资格；审批必须与 artifact hash、验证报告、批准人/时间、部署环境和回滚版本绑定。

## 3. 对训练 prompts 的审核

### Step 1 算法审计

判定：方向正确，但前提错误。

“必须基于 HR、HRV、sleep、SpO2、BP”只有在存在标签对齐的训练数据时才成立。F3b 已证明当前 NHANES/CVD16 数据不能完成纯 commercial-band sensor model；`exercise_days` 也主要是问卷变量。审计任务必须先做 field-to-source 和 label alignment inventory，不能先假设可训练字段已经存在。

还应增加：标签时间关系、数据泄漏检查、缺失机制、设备测量误差、目标人群迁移、外部验证和医疗用途边界。

### Step 2 多候选训练

判定：可以作为研究任务，但原 prompt 不足以产生可部署模型。

必须补充：

- patient-level/temporal split，所有 imputation、calibration 和 feature selection 只在允许的数据分区拟合。
- sealed lockbox 只使用一次；记录数据版本、代码 commit、随机种子和环境。
- AUC 之外报告 AUPRC、Brier、校准 slope/intercept、预设阈值下 sensitivity/specificity、亚组表现和缺失鲁棒性。
- 输出到不可变时间戳目录，并记录 SHA-256；不要覆盖简单的 `rehealth_v2_*.pkl` 文件。
- 研究 candidate 不能直接复制到 model-service；必须先通过独立 deploy-candidate validation。

### Step 3 production candidate 选择

判定：原 prompt 的自动生产切换方式不安全。

不能只按 AUC、feature 数、解释性和可获得性自动生成生效的 `production_model.json`。模型选择还需要目标用途、校准、阈值临床含义、亚组风险、数据漂移、外部验证、artifact 完整性和人工批准。Codex 可以生成“推荐候选与证据包”，但不能让同一个训练任务自行把候选标记为 production。

## 4. 修正后的目标架构

```text
MRD ring
  -> Android BLE repository
  -> Room (local source of truth)
  -> versioned feature contract + quality/source metadata
  -> Android upload queue
  -> backend authenticated mobile API
       -> hardware_db telemetry persistence
       -> software_db profile/risk/intervention/feedback persistence
       -> ModelServiceClient
            -> model-service schema router
            -> governed artifact loader/registry
            -> risk/intervention/individual attribution
  -> Android risk/intervention display and feedback sync

rehealth-algorithms
  -> training and research outputs
  -> reviewed candidate evidence package
  -> controlled artifact handoff
  -> model-service (never directly to Android)
```

关键版本规则：

- 模型内部升级且输入不变：保持 `cvd-16-v1`，只更新受控 model version。
- 输入字段或语义变化：新增 schema，例如 `cvd-wearable-v1`；不原地修改 `cvd-16-v1`。
- Android 发送 schema 和 feature quality，不发送 `model_version=latest` 控制生产路由。
- model-service 返回实际 model/schema/artifact trace；backend 在 E1.1 后持久化必要的风险审计信息。

## 5. 更新后的实施顺序

| 顺序 | 任务 | 原因与完成门槛 |
| --- | --- | --- |
| 0 | E2.1 + D2 integration acceptance | 在真实 MySQL 应用 V1 migration，Android build/test 通过；同一 `batchId` 上传两次仅一份数据；backend 重启后数据仍在；raw signal 仍拒绝。E2.1 代码已完成，本项主要补证据。 |
| 1 | E1.1 software_db persistence | 持久化 device binding、feature request/risk result、intervention plan、feedback、attribution event，并按 authenticated user 隔离；让 latest/today/feedback 不再返回 null 或 `persisted=false`。这是下一项代码任务。 |
| 2 | D3 authenticated feedback and queue resume | Android 去除 build-time token 作为产品路径；接入登录/token refresh；401 暂停后可恢复；`submitCheckIn` 切到 typed feedback API，并 local-first 入队。 |
| 3 | G3/G4 physical and release acceptance | MRD 真机后台采集、锁屏/重启、D2/E2.1、真实 Core16、干预反馈、隐私日志和 release build 全链路证据。 |
| 4 | PIAS production slice | 先做 Predict -> Intervene -> individual Attribute；settlement 保持 admin-only evidence，不作为 patient App API。依赖 E1.1 的历史数据。 |
| 5 | M2 model release registry | signed manifest、hash、环境别名、审批、回滚和灰度；不阻塞单模型 MVP。 |
| 6 | Wearable-v1 data/model program | 先取得 HR/HRV/sleep/SpO2/BP 与目标标签对齐的数据，再训练和外部验证；通过新 schema 灰度，不替换 `cvd-16-v1`。 |

原建议中的 `D2 -> F2 -> registry -> E1.1 -> PIAS` 已与现状不符：D2、F2-real 和 M1 skeleton 均已有实现，而 E1.1 正在阻断风险历史和反馈闭环。

## 6. 下一步实施 Prompt：E1.1

```text
Read completely before coding:
- D:\rehealthAI\AGENTS.md
- D:\rehealthAI\ENGINEERING.md
- D:\rehealthAI\CODEX_ORCHESTRATION.md
- D:\rehealthAI\ACCEPTANCE_REVIEW_2026-07-10.md
- backend/docs/DATABASE_SPLIT_ARCHITECTURE.md
- backend/docs/MOBILE_API.md
- backend/docs/HARDWARE_INGEST_ARCHITECTURE.md
- backend/codex-runs/2026-07-13/E2_1_status.md
- backend/docs/qa/PRODUCT_ARCHITECTURE_ACCEPTANCE_2026-07-13.md

Workstream: E1.1_backend_software_db_persistence.

Goal:
Replace E1PendingSoftwareDbReHealthBusinessRepository with durable,
authenticated-user-scoped software_db persistence for the MVP business loop.
Keep model inference in model-service and hardware telemetry in hardware_db.

Before editing:
1. Inspect the current controller/service/repository/DTO/auth/datasource patterns.
2. Summarize the existing implementation and exact persistence gaps.
3. Propose the smallest schema and migration plan.
4. List target files, compatibility risks, and test/build commands.
5. Then implement.

Required scope:
- Add add-only versioned software_db migrations and entities/mappers for:
  device binding, CVD feature evaluation request metadata/risk result,
  intervention plan, intervention feedback, and attribution event.
- Bind every write/read to the authenticated Jeecg LoginUser.id. Do not trust a
  request-body userId as authority.
- Persist enough model governance fields for audit: request_id,
  feature_schema_version, model_version, scorer_mode/is_mock, artifact_name when
  present, score/level, generated timestamps, and minimized contribution data.
- Make GET /risk/latest and GET /interventions/today user-scoped and durable.
- Make POST /interventions/{id}/feedback idempotent and return persisted=true
  only after commit.
- Verify the intervention belongs to the authenticated user before feedback.
- Preserve existing mobile DTO and JSON compatibility unless a documented,
  backward-compatible optional field is necessary.
- Keep raw telemetry out of software_db. Do not modify E2.1 hardware_db tables.
- Do not call CatBoost/SHAP/PIAS directly from Java.
- Do not log raw health payloads, tokens, phone numbers, or identifiers.
- Do not implement Android changes, token refresh, full PIAS orchestration,
  model registry M2, or admin settlement in this task.

Tests:
- Repository/service integration tests for user isolation.
- Save then retrieve latest risk and today's intervention.
- Duplicate feedback is idempotent.
- Cross-user feedback is rejected.
- Transaction rollback does not leave partial feature/risk/intervention data.
- Existing mobile API and E2.1 ingest tests remain green.

Validation:
- Run focused Maven tests for E1.1 and existing E2.1 tests.
- Run rehealth module package and jeecg-system-start package.
- Run git diff --check and git status.
- If real MySQL is unavailable, run migration-aligned H2 tests and document the
  exact manual MySQL QA still required; do not claim production validation.

Documentation and delivery:
- Update MOBILE_API.md and DATABASE_SPLIT_ARCHITECTURE.md with actual table and
  transaction behavior.
- Add backend/codex-runs/2026-07-13/E1_1_status.md.
- Commit on a dedicated work/E1.1_backend_software_db_persistence branch.
- Report changed files, migration strategy, tests/builds, manual QA, risks,
  git status, commit, and next recommended task.
```

## 7. 后续模型任务 Prompt 修正原则

下一次算法任务应先命名为 `F4_wearable_dataset_readiness_and_schema_design`，只做数据与契约审计，不直接训练或切 production：

```text
Audit whether ReHealth currently has label-aligned, legally usable training data
for resting HR, HRV, sleep, SpO2, steps/activity, BP, age, sex, and BMI.

Do not assume these fields exist because Android can collect them. Produce:
- field -> device/source -> sampling/aggregation -> label-time alignment matrix;
- cohort, endpoint, missingness, device-quality and consent assessment;
- leakage and temporal split risks;
- proposed cvd-wearable-v1 schema with quality/source semantics;
- gap list and data collection plan;
- go/no-go decision for model training.

Do not modify cvd-16-v1, do not copy artifacts to model-service, and do not mark
any candidate production-ready. Save a dated audit report and validation plan.
```

只有该审计得到 GO，才进入多候选训练、独立验证、受控 artifact handoff 和 M2 registry 发布流程。

## 8. 验收结论

架构分层建议：通过，但需按本文件修正客户端模型选择、SDK时机和 schema 演进规则。

“Model Registry 当前最大缺失”：不通过；M1 skeleton 已存在，M2 是后续治理增强。

“暂缓 E1.1，优先 registry”：不通过；E1.1 是当前风险、干预、反馈与 PIAS 历史数据的直接依赖。

“F3b 已形成可部署 wearable 模型”：不通过；当前仅是 NHANES 八字段研究候选，缺真实手环传感器训练对齐和外部临床验证。

产品发布状态：仍为 BLOCKED。下一项代码任务为 E1.1；执行前先补 E2.1 + D2 的真实 MySQL/Android integration acceptance 证据。
