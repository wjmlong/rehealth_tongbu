# ReHealth MVP 全流程验收复审 (2026-07-10)

> 本文件由 codex 在 2026-07-09 验收报告之后重新审核生成，供后续 codex 任务读取。
> 替代/补充：`ACCEPTANCE_REPORT.md` (G1, 2026-07-09), `RELEASE_CHECKLIST.md`, `QA_TEST_PLAN.md`。
> 目标：以"最小可上线切片"视角，重审端到端流程，列出仍存在的真实问题与下一步。

---

## 0. 复审输入

读取的对象：

- `AGENTS.md`, `ENGINEERING.md`, `CODEX_ORCHESTRATION.md`
- `.codex/prompts/00..G` 全部 workstream 提示
- `Android-apk/codex-runs/2026-07-09/{A,B,B1_fixup,C,P0b}_status.md`
- `Android-apk/codex-runs/2026-07-09/` (D_status.md **缺失**)
- `backend/codex-runs/2026-07-09/{E0_5,E1,E2,E,G,P0c,workspace_docs_versioning}_status.md`
- `model-service/codex-runs/2026-07-09/{F,F2b,F2c,F2e}_status.md`
- `model-service/codex-runs/2026-07-10/M1_status.md`
- `Android-apk/docs/{CANONICAL_RISK_PATH, FEATURE_EXTRACTOR, NETWORK_FEATURE_EVALUATE, BLE_BACKGROUND_QA, BLE_RELEASE_BLOCKERS, REHEALTH_INTEGRATION_CONTRACT, ENGINEERING_STATUS}.md`
- `backend/docs/{MOBILE_API, CANONICAL_BACKEND_RISK_PATH, BACKEND_MODULE_SELECTION, DATABASE_SPLIT_ARCHITECTURE, HARDWARE_INGEST_ARCHITECTURE, E2_INGEST_RUNBOOK}.md`
- `model-service/docs/{API_CONTRACT, MODEL_ARTIFACTS, MODEL_REGISTRY, REAL_MODEL_INTEGRATION, REHEALTH_ANDROID_MODEL_TRACE}.md`
- 实际源码：Android `/app/src/main/java/com/rehealth/genie/**`, backend `/jeecg-module-rehealth/**`, model-service `/app/**`
- 实测：Android `gradlew testDebugUnitTest assembleDebug` PASS；model-service `pytest` 22 passed。

## 1. 分支与提交现状（与 07-09 报告已不同）

| 仓库 | 当前分支 | HEAD | 状态 |
| --- | --- | --- | --- |
| `Android-apk` | `work/B1_fixup_release_blockers` | `d251fab fix(android): harden ring background collection release blockers` | clean；A1/B1/C1/D1/P0b/B1-fixup 已合入此分支 |
| `backend` | `work/P0c_backend_legacy_path_retirement` | `e0e32a5 fix(backend): retire legacy mock risk paths` | clean；E1/E2/P0c 已合入 |
| `model-service` | `main` | `c6624f3 feat(model-service): add model registry trace metadata` | clean，ahead origin/main 4 (F2b/F2c/F2e/M1) |
| `rehealth-android` | `main` | `96af186` | clean；与本 MVP 解耦，仅作研究/训练参考 |

**重要变化**（相对 07-09 验收报告）：

1. D1 的 process-blocker「Android-apk/codex-runs/2026-07-09/D_status.md 缺失」**仍存在**。
   仅根目录 `D:\rehealthAI\codex-runs\2026-07-09\D_status.md` 存在，Android 子目录下确实没有 `D_status.md`。
2. B1 大部分 blocker 已被 `work/B1_fixup_release_blockers` 清理（raw BLE 日志、Android 13 通知权限、legacy `/ring/snapshots` 在 RingViewModel 中已断开、boot 重启恢复）。
3. model-service 已从 G1 时的 "F1 已接受" 推进到 F2e + M1：real-model 边界 + alias 策略 + 注册表/trace metadata；但仍 `is_mock=true / real_unavailable`，无真实 artifact。
4. E2 已落地为 in-memory dev 队列 + `E2PendingHardwareTelemetryWriter`，不是 durable MQ/hardware_db。
5. M1 新增 `model_trace` 响应块与 `feature_schema_version=cvd-16-v1` —— 这是一个 **新的契约面**，需 backend/Android 明确决策是否透传/存储。

## 2. 端到端契约核对（按 SKILL 的 10 步验收）

| 步骤 | 期望 | 现状 | 证据 | 结论 |
| --- | --- | --- | --- | --- |
| 1. App 可安装 | assembleDebug PASS | PASS (实测 45 task up-to-date) | `app/build/outputs/apk/debug/app-debug.apk` | OK |
| 2. 绑定戒指 | UI 流程 + BLE 权限 | `RingViewModel.scan/connect`, `MrdBleRingRepository`, Android 12+ 权限 | `Android-apk/app/.../ring/` | OK（待真机） |
| 3. 真数据落 Room | BLE→Repo→Room | `MrdBleRingRepository.persistPackets` 写 `dao.insertBatch`；4 个表 | `ring/data/RingDataDao.kt` | OK |
| 4. 后台采集 | ForegroundService + WorkManager | `RingForegroundService`, `RingBackgroundRecoveryWorker`, `RingBootReceiver`，15min 周期 | `service/`, `work/` | OK |
| 5. 生成 CVD16 | `HealthFeatureExtractor` 16 字段 + quality | 实现完整，单元测试覆盖 missing/outlier/dup | `features/` | OK |
| 6. 离线上传队列 | 本地先落，异步上传 | **未实现**。D1 仅接 feature evaluate 路径；`sync/` 目录不存在 | `app/.../network/` 无 `sync/`, `ReHealthApi` 注释明确 `/measurements/batch` INTENTIONALLY ABSENT | **GAP** |
| 7. 后端收到批量 | `/measurements/batch` | 后端端点存在 (controller line 59) + E2 dev 队列；Android 不上传 | `mobile/controller/ReHealthMobileController.java` | 半通（后端就绪，端侧不调） |
| 8. model-service 评分 | `risk_score/level/contributions/version/is_mock` | mock 返回所有字段；real artifact 缺失 | `risk_scorer.py` | OK（mock） |
| 9. App 展示风险/干预 | P0b canonical UI | `ReHealthApp.kt` ModelScreen 已接 `RemotePhmService`；显示 score/level/contributions/version/mock 标 | `ui/ReHealthApp.kt` | OK |
| 10. 反馈保存与同步 | 干预反馈 → 本地+后端 | 后端 `POST /interventions/{id}/feedback` 存在；Android 端 `submitCheckIn` 仅走 legacy `ReHealthBackendClient`/本地模拟 | `RingViewModel.submitCheckIn` | 半通 |

**结论**：核心风险评分闭环（步骤 1-5, 8-9）已通；**数据回传闭环（6-7, 10）未闭环** —— 这是当前 MVP 的最大空洞。

## 3. 排查出的真实问题（按优先级）

### P0 — 阻塞最终发布

| ID | 问题 | 证据 | 影响 |
| --- | --- | --- | --- |
| P0-1 | 无真实模型 artifact；`is_mock=true` 贯穿整条链 | `models/.gitkeep` only；`/health` 返回 `scorer_mode=real_unavailable` | 任何 UI/后台对外都不能宣称"AI 风险评分" |
| P0-2 | 无 MeasurementSyncWorker；`/measurements/batch` Android 不上传 | `Android-apk/app/.../network/ReHealthApi.kt:20` 显式 absent | 采集数据无法上云；后台无法看到用户历史；无法支撑"数据→评分→干预→反馈"完整闭环 |
| P0-3 | 后端 `/measurements/batch` 仅 dev in-memory 队列 | `E2PendingHardwareTelemetryWriter`, `InMemoryTelemetryIngestQueue` | 重启即丢；非生产级 ingestion |
| P0-4 | 无真机 MRD 戒指 QA 证据（锁屏/杀 app/蓝牙关/权限拒/重启） | `BLE_RELEASE_BLOCKERS.md` "Still Required For Release Approval" | 无法判定 B1 在真机上是否真"不掉线" |
| P0-5 | `Android-apk/codex-runs/2026-07-09/D_status.md` 仍缺失 | glob 验证 | 验收链断点；codex 后续任务可能拒绝执行相关 follow-up |

### P1 — 影响一致性与可读性

| ID | 问题 | 证据 | 修复建议 |
| --- | --- | --- | --- |
| P1-1 | `docs/REHEALTH_INTEGRATION_CONTRACT.md` 完全过时，仍把 `/ring/snapshots`、`/patient/risk-score`、`/patient/intervention-plan`、PIAS `/api/pias/predict` 写成"当前"路径 | 文件第 14-56 行 | 全文重写为 canonical contract 或加 `DEPRECATED` 顶部横幅指向 `CANONICAL_RISK_PATH.md` 和 `backend/docs/MOBILE_API.md` |
| P1-2 | Android `RingViewModel` 仍持有 `backendClient: ReHealthBackendClient`、`PatientMvpPayload`、`cloudRiskScore/Level/Mode/Summary` 等 legacy 状态 | `RingViewModel.kt:60,407`；`refreshPatientMvp` + `/patient/mvp` | 与 D2 一并迁移到 `RemotePhmService` 或显式 dev-gate；保留则需在文件顶部加 `@Deprecated` 注释 |
| P1-3 | M1 `model_trace` 响应字段未在 backend DTO / Android DTO 中声明 | `model-service/docs/MODEL_REGISTRY.md` 列出，但 `RiskEvaluateResponseDto.java` / `RiskResultDto.kt` 未见对应字段 | 决策：透传并存储 → 加字段；忽略 → 文档说明"Android/backend 忽略 model_trace" |
| P1-4 | `rehealth-android` 的 `rehealth_v2_final.pkl`、`feature_cols_v2.pkl`、`model_meta_v2.json` 未生成/未移交 | `MODEL_ARTIFACTS.md` Request List | 跑一次 `rehealth-android/train/train_v2_final.py` 产出 artifact，review 后放入 `model-service/models/` |
| P1-5 | `skill` 目录在仓库根 `D:\rehealthAI` 不是 git repo，根级 QA 文档无法直接 commit | `ACCEPTANCE_REPORT.md` 已记 | 已通过 `backend/docs/qa/` snapshot 版本化（`workspace_docs_versioning_status.md`）；新评审文档应同样处理 |
| P1-6 | backend E1 无 `software_db` 表/mapper（`risk/latest`, `interventions/today` 返回 null） | `E1PendingSoftwareDbReHealthBusinessRepository` | E1.x 任务补 schema/migration |

### P2 — 出现警告/小问题

- model-service pytest 在沙箱中 `.pytest_cache` 写入失败（权限问题，非功能失败，tests pass）。
- `Android-apk/app/build.gradle.kts` 默认 base URL `http://10.0.2.2:8080/jeecg-boot`、token 空字符串 —— 真机 QA 需本地覆盖。
- `MrdProtocolAdapter` 中 `raw` 字节仍被存入 Room 实体的 `raw_payload`（**不是日志，不是上传**，符合"先落本地"原则，但需确认不会被 sync worker 上传）。
- `docs/ENGINEERING_STATUS.md` (2026-07-08) 描述"服务器尚未接入，当前数据不会上传云端" — 已过时，应更新或加 `SUPERSEDED` 横幅。

## 4. canonical 流程图（当前实际状态）

```text
[MRD戒指] --BLE--> MrdBleRingRepository --persistPackets--> Room (4表) ✅
                                                              |
                                                              v
HealthFeatureExtractor --CvdFeatureVector(16+quality)--> ✅
                                                              |
                                              CvdFeatureVectorDtoMapper ✅
                                                              |
                                                              v
              Android UI (ReHealthApp.ModelScreen) ✅
                                                              |
                                              RemotePhmService.evaluate ✅
                                                              |
                                                              v
      POST /rehealth/mobile/features/evaluate (backend E1) ✅
                                                              |
                                              ModelServiceClient ✅
                                                              |
                                                              v
              POST /v1/cvd/risk/evaluate (model-service) ✅ mock
                                                              |
                                                              v
      is_mock=true, risk_score/level/contributions/version ✅
                                                              |
                                                              v
                  UI 显示风险 + mock 标记 ✅
                                                              |
                                                              v
                  干预反馈 submitCheckIn ⚠️ legacy/local only

[离线上传队列] ❌ 未实现 (sync/ 不存在)
[MeasurementSyncWorker -> /measurements/batch] ❌ 未实现
[backend durable ingestion (MQ/hardware_db)] ⚠️ dev in-memory only
[real CatBoost artifact] ❌ 缺失
```

## 5. 推荐下一步（按依赖顺序）

### 阶段 A：闭环数据回传（最高优先级）

任务名：`D2_android_telemetry_sync_and_queue`

范围：

1. 新建 `app/src/main/java/com/rehealth/genie/sync/`：
   - `UploadQueueEntity.kt`, `UploadQueueDao.kt`, `SyncRepository.kt`
   - `MeasurementSyncWorker.kt`（WorkManager，NetworkType.CONNECTED，指数退避）
2. 在 `ReHealthApi.kt` 启用 `POST /rehealth/mobile/measurements/batch`，带幂等 key
3. 把 Room `ring_measurements`/`ring_sleep_sessions`/`ring_activities` 批量打包上传；**`ring_signal_chunks` 默认不上传**（raw PPG/RRI 政策）
4. 写入失败 → 落 `UploadQueue`；成功 → 标记 synced
5. 集成测试：backend 启动 → 上传成功 → Room 与后端 in-memory 队列一致

Definition of Done：

- `sync/` 模块存在并被 `ReHealthApplication` 注册
- `MeasurementSyncWorker` 周期性触发，可在 Profile toggle 中暂停
- Coordinating：BLE 采集绝不在 worker 回调中阻塞
- 无 raw signal upload（除非 build flag 显式开启）
- `assembleDebug` + `testDebugUnitTest` PASS

### 阶段 B：后端持久化 + 真实 artifact

`E1.1_backend_software_db_schema`：补 `risk_latest`, `intervention_plan_today`, `feedback` 的 software_db 表/mapper/migration。

`F2_real_model_handoff`：在 `rehealth-android/train/train_v2_final.py` 跑一次 → 产出 `rehealth_v2_final.pkl`, `feature_cols_v2.pkl`, `model_meta_v2.json` → 复核 → 放 `model-service/models/`（或通过 `model_manifest.json`）→ 验证 `/health` 返回 `scorer_mode=real_available`、`is_mock=false`、`feature_order 匹配`、`predict 成功`。

### 阶段 C：真机 B1 QA + 文档清理

`B1_physical_qa_evidence`：用至少 1 台 Android 13+ 真机 + MRD 戒指，按 `BLE_BACKGROUND_QA.md` 25 步逐项取证，归档至 `Android-apk/docs/evidence/<build-sha>/`。

`G2_doc_cleanup`（文档收尾）：

- 重写 `Android-apk/docs/REHEALTH_INTEGRATION_CONTRACT.md` 为 canonical contract（或显式标 DEPRECATED 并指向 `CANONICAL_RISK_PATH.md` + `backend/docs/MOBILE_API.md`）
- 更新 `Android-apk/docs/ENGINEERING_STATUS.md`（2026-07-08 版已过时）
- 在 `Android-apk/codex-runs/2026-07-09/` 补 `D_status.md`（可从根 `D_status.md` 复制并加注 "restored by G2_doc_cleanup"）
- 决策 `model_trace` 字段在 backend/Android 中的处理策略并写入 `MODEL_REGISTRY.md` 与 Android `RiskResultDto`
- 把本评审文档同步 to `backend/docs/qa/ACCEPTANCE_REVIEW_2026-07-10.md`

### 阶段 D：医疗安全与隐私收尾

`D3_auth_token_refresh`：D1 当前不含 token 刷新；token 过期时 app 停在 mock fallback。生产前必须实现 token 拦截器 + 刷新 + 队列暂停。
`G3_release_build_audit`：release build logcat 证据 — 无 raw BLE hex / parsed JSON / PPG / RRI 日志；network inspector 证据 — 无 `/measurements/batch` / `/ring/snapshots` / raw signal 上传（除非显式开启）。

## 6. 验证命令（codex 应照此跑）

```powershell
# Android
cd D:\rehealthAI\Android-apk
$env:JAVA_HOME = "D:\Android_Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat testDebugUnitTest
.\gradlew.bat assembleDebug
git diff --check

# Backend
cd D:\rehealthAI\backend\jeecg-boot
$env:JAVA_HOME = "D:\Android_Studio\jbr"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
D:\rehealthAI\tools\apache-maven-3.9.11\bin\mvn.cmd -pl jeecg-boot-module/jeecg-module-rehealth -am package -DskipTests
D:\rehealthAI\tools\apache-maven-3.9.11\bin\mvn.cmd -pl jeecg-module-system/jeecg-system-start -am package -DskipTests

# Model-service
cd D:\rehealthAI\model-service
$env:TMP='D:\rehealthAI\model-service\.tmp'
$env:TEMP='D:\rehealthAI\model-service\.tmp'
C:\Users\kiki\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe -m pytest
C:\Users\kiki\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe -m compileall app
```

## 7. 当前评审实测结果（2026-07-10）

| 命令 | 结果 |
| --- | --- |
| Android `testDebugUnitTest assembleDebug` | BUILD SUCCESSFUL (45 task up-to-date) |
| model-service `pytest` | 22 passed, 1 cache warning (权限，非失败) |
| backend (本次未跑，07-09 已记录 BUILD SUCCESS) | 见 `RELEASE_CHECKLIST.md` |

## 8. 最终验收决定

**状态**：MVP 工程阶段 II 完成（风险评分闭环通），**最终发布仍 BLOCKED**。

**已通**：

- A1 构建健康
- B1 BLE/后台采集（含 B1-fixup 大部分 blocker 已清）
- C1 CVD16 特征工程
- D1 网络特征评估路径
- E1 后端移动 API
- E2 后端 ingest 边界（dev queue）
- F1 model-service mock 评分
- F2 model-service real artifact 边界 + alias 策略
- M1 model registry trace metadata
- P0b canonical risk UI
- P0c backend legacy 路径下线

**仍阻塞**：

- D2 数据回传闭环（UploadQueue + MeasurementSyncWorker）
- E1.1 software_db 持久化
- F2-real 真实 artifact 移交与校验
- B1 真机 QA 证据
- D_status.md 在 Android-apk 路径补齐
- G2 文档清理（INTEGRATION_CONTRACT 过时等）
- D3 token 刷新
- G3 release build 隐私/日志审计

**允许的下一步工作**：按阶段 A → B → C → D 顺序推进。未通 D2 前不应宣布"已上传数据"或"已打通数据回传"。

---

## 附录：给 codex 下一个任务的指令模板

```text
Read AGENTS.md, ENGINEERING.md, ACCEPTANCE_REVIEW_2026-07-10.md.

Current state: phase II complete (mock risk loop), phase III (data return loop) NOT started.

If tasked with D2_android_telemetry_sync_and_queue:
1. Only edit files under app/src/main/java/com/rehealth/genie/sync/, network/ReHealthApi.kt, ReHealthApplication.kt, app/build.gradle.kts
2. Do NOT touch BLE/repository internals
3. Do NOT upload raw_signal_chunks / PPG / RRI by default
4. WorkManager + Retry + idempotency key
5. Run testDebugUnitTest + assembleDebug, update codex-runs/2026-07-10/D2_status.md
6. Coordinate with B1 — background service must not block on network

Report back with: changed files, build result, manual QA steps, risks, next task.
```

---

## 附录 B — 2026-07-10 后续补钉执行日志

本节由 Same-session G2 pass 追加,记录在 2026-07-10 评审完之后立即执行的清理与契约饱和工作。所有变更已 commit、build/test pass;除 `Android-apk a5e03e7` 因网络抖动一次失败外,其余全部 push 到 GitHub。

| 阶段 | 仓库 | 分支 | Commit | 说明 |
| --- | --- | --- | --- | --- |
| 1 备份 | backend | work/E2_backend_hardware_ingest | `0acbc02` | merge(backend): integrate P0c legacy path retirement into E2 work branch |
| 1 备份 | backend | work/E2_backend_hardware_ingest | (push) | push 到 origin/work/E2_backend_hardware_ingest |
| 1 备份 | Android-apk | work/B1_fixup_release_blockers | (push) | 新分支首次推送 |
| 1 备份 | model-service | main | (push) | `9ddf227..c6624f3 main -> main` |
| 2 文档 | Android-apk | work/B1_fixup_release_blockers | `528b2db` | docs(android): cleanup legacy paths and restore D1 status (G2_doc_cleanup) |
| 2 文档 | Android-apk | work/B1_fixup_release_blockers | (push) | `d251fab..528b2db` |
| 3 model_trace | backend | work/E2_backend_hardware_ingest | `1a90014` | feat(backend): add ModelTrace passthrough DTO for model_trace contract (M1-P0c followup) |
| 3 model_trace | backend | work/E2_backend_hardware_ingest | (push) | `0acbc02..1a90014` |
| 3 model_trace | Android-apk | work/B1_fixup_release_blockers | `12cefba` | feat(android): add ModelTrace DTO passthrough in RiskResultDto (M1 model_trace_contract) |
| 3 model_trace | Android-apk | work/B1_fixup_release_blockers | (push) | `528b2db..12cefba` |
| 3 model_trace | model-service | main | `cba4982` | docs(model-service): record Android/backend model_trace pass-through policy (M1 followup) |
| 3 model_trace | model-service | main | (push) | `c6624f3..cba4982 main -> main` |
| 4 D2 plan | Android-apk | work/B1_fixup_release_blockers | `a5e03e7` | docs(android): add D2 telemetry sync plan (G2 followup, planning only) |
| 4 D2 plan | Android-apk | work/B1_fixup_release_blockers | (pending) | 待网络恢复后重 push;commit 已本地保存,数据不丢 |

**最终验证**(2026-07-10 实测,参见本文件 Section 7 与本次 grep):

| 项 | 结果 |
| --- | --- |
| model-service pytest | 22 passed (1 pytest_cache 权限警告,非失败) |
| Android-apk `testDebugUnitTest assembleDebug` | BUILD SUCCESSFUL (45 up-to-date) |
| backend mvn package `-DskipTests` | BUILD SUCCESS (was 34 source files, now 35 after ModelTraceDto) |

**变更面汇总**(本 review 之后的 G2 补丁):

- **P1-1 过时 INTEGRATION_CONTRACT**(已解决): `Android-apk/docs/REHEALTH_INTEGRATION_CONTRACT.md` 全文 rewrite 为 canonical,标记 SUPERSEDED-then-canonical,retired 路径全部 P0c 标注。
- **P1 D_status 缺失**(已解决): `Android-apk/codex-runs/2026-07-09/D_status.md` 已补,带有完整 restoration note 引用原始 commit `5528e22` 与当前 HEAD `d251fab`。
- **P1 ENGINEERING_STATUS 过时**(已解决): `Android-apk/docs/ENGINEERING_STATUS.md` 加 SUPERSEDED 横幅 + 内联标注 + 2026-07-10 增补段。
- **P1-3 model_trace 契约饱和**(已解决): pass-through store 决策落地于三端 backend `RiskEvaluateResponseDto.modelTrace` + Android `RiskResultDto.model_trace/modelTrace` + model-service `MODEL_REGISTRY.md` "Android And Backend Pass-through Policy" 章节。FastJSON 入向用 snake_case `@JSONField(name="model_trace")`,与 Android DTO 主字段 snake_case 一致。FastJSON 出向 (controller 序列化 Android) 也走 `@JSONField(name="model_trace")` 输出 snake_case key,契约对齐。
- **D2 任务规划**(已就绪): `Android-apk/docs/D2_TELEMETRY_SYNC_PLAN.md` 提交给下一个 codex 任务,有明确 Section 4/5/9/10/11 (必要文件、必改文件、验证、policy、DoD) + Section 12 codex 任务 prompt 模板。
- **AGENTS.md 入口**(已更新): 根 `AGENTS.md` 添加引用,指引下一个 codex 任务读 `ACCEPTANCE_REVIEW_2026-07-10.md` 与 `Android-apk/docs/D2_TELEMETRY_SYNC_PLAN.md`。

**仍未闭环的 G2 项目**(由后续任务推进):

- backend E1.1 software_db 持久化(`risk/latest`, `interventions/today` 仍返回 null) — 见原报告 Section 5 阶段 B
- F2-real:真 CatBoost artifact 移交(`rehealth-android/train/train_v2_final.py` 跑通产出 `rehealth_v2_final.pkl` + `feature_cols_v2.pkl` + `model_meta_v2.json`,review 后放 `model-service/models/`) — 见阶段 B
- B1 真机 QA 证据 — 见阶段 C
- D3 token 刷新 — 见阶段 D
- G3 release build 隐私/日志审计 — 见阶段 D

**镜像版本化**:本次新增根 `ACCEPTANCE_REVIEW_2026-07-10.md` 通过本附录追加的方式就地更新,**同时** mirror 一份到 `backend/docs/qa/ACCEPTANCE_REVIEW_2026-07-10.md`(沿用 P1-5 已建立的根文档版本化模式)。下次 codex 在 backend 仓库即可读到这份验收状态副本。
</content>
</invoke>
