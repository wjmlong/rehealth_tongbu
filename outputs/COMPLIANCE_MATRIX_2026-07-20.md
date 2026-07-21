# ReHealth AI — 联调审计 + A/B/C/D 逐项合规矩阵

> 审计对象：WorkBuddy 上一次「比对 GitHub 分支 `codex/real-device` 与本地工程」的工作
> 结论：**只做了一半** —— 文件/外形对比已完成，但**联调（数据真接入 + 计算型兜底）缺失**。本表为你要求的「确保每一项」的落地核对。

## 一、四条硬性规则（你的原话）

| 规则 | 含义 |
|---|---|
| **A 外形** | App 外观、文案与 `codex/real-device` 一致 |
| **B 数据/端口** | 用**真实数据 + 真实端口**接入（JeecgBoot 后端 `10.0.2.2:8080/jeecg-boot`、model-service `:8000`） |
| **C 兜底** | 无真实数据的项：**保留 API 接口**，但发送「**计算得到的**」数据（后端/前端算出，非随机、非写死） |
| **D 每一项** | A/B/C 对**每一项**都成立 |

## 二、逐项合规矩阵

| # | 项 | 规则 | 之前（联调前） | 本次修复 | 位置 | 状态 |
|---|---|---|---|---|---|---|
| 1 | 首页问候 / 用户名 | A·B | 写死「你好」 | 时段问候 + `session.username` | `ReHealthApp.kt:470,528` | ✅ |
| 2 | 我的页「已陪伴 X 天」 | A | 写死「28 天」 | `SessionStore.firstUseDays()`（首启锚点，仅记一次） | `SessionStore.kt:43-52`、`ReHealthApp.kt:1836` | ✅ |
| 3 | 首页指标卡（睡眠/步数/体重） | A·B | 写死 `7h30m`/`8,000`/`54.0` | 绑定 `state.sleep` / `measurements[STEPS]` / `profile.weightKg` | `ReHealthApp.kt:1850-1851` + `formatSleepMinutes/formatSteps:2184-2192` | ✅ |
| 4 | 戒指实时体征流 | B | 真实 RingRepository + 计算型模拟仓 | 沿用；基线由年龄/BMI/历史趋势算出 | `ring/*`、`MockRingRepository.kt` | ✅ |
| 5 | CVD 风险评分 | B·C | 真实 model-service；兜底写死常量 | 兜底改 `CvdRiskHeuristic` **确定性逻辑回归**计算 | `MockPhmService.kt`、`CvdRiskHeuristic.kt` | ✅ |
| 6 | 归因报告 30 天历史 | C | `randomAttributionHistory()` 随机 | `computedAttributionHistory()` 由 `latestRisk()` 推算趋势 | `AttributionReportScreen.kt` | ✅ |
| 7 | AI 健康问答「根据我的数据」 | A·C | 写死 `72 bpm / 98% / 7h30m / 8000步` | **本次新增修复**：绑定真实 `ringViewModel.uiState`；缺读数时回退本地 7 天聚合（Room 历史计算） | `HealthChatScreen.kt:30,125-135` | ✅ |
| 8 | 后端 16 字段契约 | B | 运行容器落后，丢 9 个 snake_case 字段 | 源码已补 `@JSONField` 别名（fastjson 不自动映射） | 后端 `CvdFeatureVectorDto.java` | ⚠️ 待你本机重建容器 |
| 9 | 全局无 `Random()` | C | — | 已核实激活源码 **0 处**（仅 `.kt.disabled` 与文档含字面量） | 全仓 grep | ✅ |

## 三、构建验证（已实跑）

- `./gradlew assembleDebug --no-daemon` → **BUILD SUCCESSFUL in 17s**
- 产物：`outputs/rehealth_liandong_debug.apk`（21.6 MB，2026-07-21 02:24）
- 仅 2 条 benign 告警（`Icons.Outlined.ArrowBack/Send` AutoMirrored 弃用，与本次改动无关）
- 激活源码硬编码 demo 字符串：**0 处**；`Random(`：**0 处**

## 四、你这边还需做的一步（规则 B 的终验）

上一项（#8）源码已正确，但**运行中的 Docker 容器仍是旧镜像**，所以需要你在本机把后端重建一次（Docker 在此无头环境起不来）：

```bash
# 1) 启动本机 Docker Desktop
# 2) 重建并起后端
cd ~/rehealthAI/backend/deploy/staging
docker compose up -d --build backend
# 3) 重跑探针，确认 16 字段全到、is_mock:false、missingFields 收敛到真正为 null 的化验项
python outputs/probe_mobile_fixed.py
```

## 五、两点说明

1. **`HealthChatScreen` 当前未挂到导航**（orphaned，已知于 `TEST_REPORT.md`），但它仍会编译进包且带着写死 demo 数据——本次已将其数据绑定真实化，未来一旦接入即用，不再有假数据。
2. 所有「模拟」均为**确定性计算**（基于年龄/BMI/历史趋势/逻辑回归），**没有任何 `Random` 随机数**，符合规则 C「计算后发模拟数据」。

---
**一句话总结**：规则 A/B/C 在 App 侧已全部落实并构建通过；唯一阻塞项是后端容器需你本机 `docker compose up -d --build backend` 重建（源码已就绪），随后重跑探针即可闭环规则 B。
