# Demo UI Live API Integration Report

Generated: 2026-07-21

## 1. 最终结论

已在 `codex/real-device` 上完成当前仓库可支持的真实能力整合，并保留现有 Demo Compose UI、归因页布局、第二页编排及导航结构。生产默认关闭 fake ring 和 fake health-data seed；硬编码调试 JWT、生成式归因历史和风险评估失败时的 mock 分数展示均已移除。

已接入的真实链路包括 MRD BLE、Room、前台采集、认证、JeecgBoot typed API、离线上传队列、WorkManager、CVD 16 特征、真实风险评分、干预反馈和 PIAS 归因。成功的非 mock 云端风险结果现在按用户/日期持久化，归因页读取该真实历史，并以本地完成/部分完成反馈标记干预日。

Debug APK 构建成功：`Android-apk/app/build/outputs/apk/debug/app-debug.apk`，21,693,342 bytes，SHA-256 `1443ED0E90942C703E8A1EFE5396A89A0F138C2CFB591DEBDE956DABC3D2E9E0`。

本轮没有连接 Android 设备或模拟器，因此不声称已完成安装、页面点击、真实 MRD、权限拒绝、真实账号/API 返回或 PIAS 多日报告的运行时验证。

## 2. 分支信息

| 项目 | 结果 |
| --- | --- |
| 用户指定基础/工作分支 | `codex/real-device` |
| 实际开始分支 | `main`（仅有的本地/远端分支，领先 `origin/main` 3 个提交） |
| 最终工作分支 | `codex/real-device`（从实际整合 HEAD 恢复该本地分支名；未创建集成分支） |
| 真实能力来源 | 当前工作树中此前已整合的 `work/D3_android_auth_typed_feedback` 能力，依据三份 comparison/diff 材料复核 |
| PIAS 首轮提交 | `476a4c679fe08ab2719c5515ebabdc4b44db22d9` |
| 真实风险历史提交 | `97854363f63790e2d46df799181873ec37bcbc08` |
| 报告提交 | 见本文件最终 Git 历史 |

报告中的历史分支描述与实际仓库不一致：comparison 文件记录了 `work/D3_android_auth_typed_feedback`，但当前 clone 只保留 `main`；`codex/real-device` 在任务开始时也不在本地或 origin。实际代码、提交历史与构建结果被作为最终依据。

## 3. 修改文件清单

### 首轮真实 PIAS 整合（`476a4c6`）

| 文件 | 目的 |
| --- | --- |
| `Android-apk/app/build.gradle.kts` | 本地/环境注入 API 与 PIAS URL，移除签名 secret 默认值，关闭 debug fake seed。 |
| `Android-apk/app/src/main/java/com/rehealth/genie/ReHealthApplication.kt` | 装配 PIAS client，删除硬编码调试 JWT。 |
| `Android-apk/app/src/main/java/com/rehealth/genie/network/PiasApiClient.kt` | 调用真实 `/api/pias/v2/attribute/individual`。 |
| `Android-apk/app/src/main/java/com/rehealth/genie/phm/RemotePhmService.kt` | PIAS DTO 到既有归因 UI model 的映射。 |
| `Android-apk/app/src/main/java/com/rehealth/genie/ui/AttributionReportScreen.kt` | 删除生成式历史，保留加载/空/错误/重试与原布局。 |

### 本轮收尾（`9785436`）

| 文件 | 目的 |
| --- | --- |
| `ReHealthApplication.kt` | 装配风险历史 repository。 |
| `data/AppDatabase.kt` | Room 3 -> 4 migration，新增真实风险历史表。 |
| `data/RiskHistoryEntity.kt` | 每用户/每日真实风险记录 schema。 |
| `data/RiskHistoryDao.kt` | 风险历史写入和最近 30 天查询。 |
| `data/RiskHistoryRepository.kt` | 仅持久化后端明确 `is_mock=false` 的结果；合并真实反馈日。 |
| `data/sync/InterventionFeedbackDao.kt` | 查询归因窗口内的本地反馈。 |
| `ui/ReHealthApp.kt` | 成功评分写入历史；失败时不再展示 mock 风险分。 |
| `ui/AttributionReportScreen.kt` | 从真实 Room 风险历史加载 PIAS 输入。 |
| `test/.../RiskHistoryRepositoryTest.kt` | 验证 mock 隔离、真实持久化与反馈标记。 |

未删除文件。未提交 `.omo`、`.workbuddy`、截图、XML dump、日志、PID、自动化脚本、`local.properties.backup` 或其他工具产物。

## 4. 能力迁移清单

| 能力 | 当前实现 |
| --- | --- |
| 真实 API / 网络层 | `ReHealthMobileApi`, `AuthenticatedApiClient`, `ReHealthBackendClient`, Retrofit/Moshi/OkHttp。 |
| 数据模型 | typed auth/mobile/feature/attribution DTO；现有 UI model 由 mapper/service 适配。 |
| Repository / Service | `RemotePhmService`, `SyncRepository`, `RiskHistoryRepository`, feedback repository。 |
| 设备能力 | `MrdBleRingRepository` + vendor SDK；采集先落 Room，再异步上传。 |
| 鉴权 | encrypted `SessionStore`、登录/注册 ViewModel、401 queue pause；无硬编码 token。 |
| 状态管理 | Compose state + RingViewModel；加载、成功、空、错误、重试和刷新已保留。 |
| 错误处理 | typed remote errors；UI 显示用户可理解提示，不展示堆栈或原始健康数据。 |
| Mock 替换 | fake ring/seed 默认关闭；归因无 mock；风险 API 失败显示不可用，不再展示 mock 分。Mock 类仅保留为显式测试/开发代码。 |
| 配置安全 | base URL 从 `local.properties`/环境注入；token 存加密偏好；未发现新增源码密钥。 |

## 5. 页面映射

```text
归因页 AttributionReportScreen
  -> RiskHistoryRepository.attributionHistory
  -> Room cvd_risk_history + intervention_feedback_queue
  -> RemotePhmService.attributeIndividual
  -> PiasApiClient -> PIAS /api/pias/v2/attribute/individual
  -> IndividualAttributionResponseDto -> IndividualAttributionResult
  -> 既有归因卡片、趋势图、报告、空/错/重试 UI

第二页 / 数据页 DataScreen
  -> RingViewModel + refreshRemoteFeatureEvaluateStatus
  -> RingRepository / HealthFeatureExtractor / RemotePhmService
  -> MRD BLE -> Room；JeecgBoot /features/evaluate
  -> RiskResultDto -> RemoteFeatureEvaluateStatus / RingUiState
  -> 既有数据卡片、图表和交互编排

设备页 DeviceBindingScreen
  -> RingViewModel
  -> MrdBleRingRepository -> MRD SDK -> Room
  -> SyncRepository / MeasurementSyncWorker -> JeecgBoot
  -> RingUiState -> 既有设备 UI
```

## 6. 验证结果

| 命令 | 结果 |
| --- | --- |
| `gradlew.bat testDebugUnitTest --no-daemon --max-workers=1` | 成功；5 suites / 29 tests / 0 failures。 |
| `gradlew.bat assembleDebug --no-daemon --max-workers=1` | 成功；`BUILD SUCCESSFUL in 2m 25s`。 |
| `gradlew.bat lintDebug --no-daemon --max-workers=1` | 失败；8 errors / 9 warnings。8 个 error 均为既有 BLE 文件的 `MissingPermission`：`BleTool.java` 1、`MrdBleRingRepository.kt` 5、`VendorRingRepository.kt` 2；新增风险历史代码无 lint finding。 |
| `git diff --check` / staged secret scan | 成功；无 whitespace error，未发现新增硬编码 token/key/password。 |
| `adb devices -l` | 成功执行；设备列表为空。 |

本地 `local.properties` 的 `sdk.dir` 指向当前不存在的路径；验证通过显式 `JAVA_HOME=D:\Android_Studio\jbr` 与 `ANDROID_HOME=D:\Android_SDK` 执行。该本地配置未提交。

## 7. 遗留问题与风险

- 归因算法达到统计意义需至少 14 天历史，并要求干预/对照各至少 7 天；UI 的 4 天门槛只允许 PIAS 返回 accumulating 状态，不能保证 ATT 已就绪。
- 当前风险历史从本次版本安装后逐日积累；旧版本没有保存每日 non-mock 评分，无法无损回填。
- 真实账号登录、JeecgBoot、model-service/PIAS 配置和物理设备 LAN 地址仍需目标环境参数；不需要也不应把密钥写入仓库。
- 全量 lint 的 8 个既有 BLE 运行时权限错误仍是 release blocker，应在独立 BLE 安全任务中修复并真机回归。
- 未执行真机/模拟器页面验证：归因页、第二页、权限拒绝、设备断连、网络断开、刷新与生命周期取消仍需 instrumentation/manual QA。
- 共享链接 `https://opncd.ai/share/uAqauM8c` 两次尝试均无法附着页面，未能可靠读取网页正文；三份本地比对文件、Claude JSONL、Git 历史、代码和构建结果已完整用于本报告。

## 8. 下一步

连接测试设备或启动模拟器，配置真实 JeecgBoot/PIAS 地址，完成登录、第二页、归因页、MRD 权限/断连和多日风险积累的端到端 QA；随后单独修复 BLE `MissingPermission` lint blocker。
