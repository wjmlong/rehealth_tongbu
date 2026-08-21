# ReHealth 动态心健康指数 RHI-100 v2.0 规划

状态：`research preview`，2026-07-31。本文是 v2 算法研究与验证的
规范入口，不替代 `cvd-16-v1` 生产模型，也不构成医疗器械或临床风险模型
有效性声明。

## 1. 决策

v2 采用“双轨模型、四个输出、32 个核心健康维度”：

1. 长期心血管风险：独立的、经过人群适用性和校准审查的临床模型；
2. 动态心健康指数 RHI：0–100，每日原始计算、首页展示 7 日平滑值；
3. 28 日改善动量：RHI 展示值相对 28 日前的变化；
4. 数据可信度：独立反映缺失、过期、来源、信号质量和设备变化。

长期风险与 RHI 不相互伪装。现有 CVD-16 CatBoost 继续由
`model-service /v1/cvd/risk/evaluate` 提供兼容服务；在中国人群临床锚点
完成授权、公式复核、外部验证与本地校准前，v2 不生成 China-PAR 概率。

该决策与现有证据一致：

- China-PAR 是中国前瞻性队列开发和验证的 10 年 ASCVD 风险工具：
  <https://pubmed.ncbi.nlm.nih.gov/27682885/>
- PREVENT 来自美国当代队列，适合作为研究挑战者，不应未经中国人群
  本地验证直接作为正式概率：
  <https://pubmed.ncbi.nlm.nih.gov/37947085/>
- AHA Life's Essential 8 将饮食、活动、尼古丁、睡眠、体重、血脂、
  血糖和血压作为心血管健康的可管理领域：
  <https://www.heart.org/en/healthy-living/healthy-lifestyle/lifes-essential-8>
- 消费级设备间夜间静息心率和 HRV 一致性存在差异，HRV 优先用于
  同用户、同设备、同算法版本的个人趋势：
  <https://pubmed.ncbi.nlm.nih.gov/40834291/>

## 2. 32 维核心协议

| # | v2 字段 | 分组 | 主要用途 |
| ---: | --- | --- | --- |
| 1 | `age` | 临床慢变量 | 长期风险 |
| 2 | `biological_sex` | 临床慢变量 | 长期风险 |
| 3 | `waist_circumference_cm` | 临床慢变量 | 中心性肥胖/代谢域 |
| 4 | `bmi` | 临床慢变量 | 代谢域 |
| 5 | `sbp_7d_mean` | 临床慢变量 | 长期风险/血流动力域 |
| 6 | `total_cholesterol` | 临床慢变量 | 长期风险 |
| 7 | `hdl_c` | 临床慢变量 | 长期风险/代谢域 |
| 8 | `ldl_c` | 临床慢变量 | 代谢管理 |
| 9 | `triglycerides` | 临床慢变量 | 代谢管理 |
| 10 | `glycemia_value` + `glycemia_metric` | 临床慢变量 | HbA1c 优先，空腹血糖兜底 |
| 11 | `egfr` | 临床慢变量 | 心肾代谢风险 |
| 12 | `nicotine_exposure` | 临床慢变量 | 长期风险/行为域 |
| 13 | `diabetes_status` | 临床慢变量 | 长期风险 |
| 14 | `antihypertensive_medication` | 临床慢变量 | 治疗状态 |
| 15 | `lipid_lowering_medication` | 临床慢变量 | 治疗状态 |
| 16 | `premature_cvd_family_history` | 临床慢变量 | 长期风险增强因素 |
| 17 | `dbp_7d_mean` | 动态生理 | 血流动力域 |
| 18 | `resting_hr_14d_median` | 动态生理 | 血流动力域 |
| 19 | `resting_hr_change_28d_pct` | 动态生理 | 个人变化 |
| 20 | `nocturnal_hrv_14d_median` | 动态生理 | 仅同设备个人基线 |
| 21 | `hrv_change_28d_pct` | 动态生理 | 个人变化 |
| 22 | `cardiorespiratory_fitness_score` | 动态生理 | 设备/算法归一化 0–100 |
| 23 | `sleep_duration_7d_mean_hours` | 动态生理 | 睡眠域 |
| 24 | `sleep_regularity_14d_pct` | 动态生理 | 睡眠域 |
| 25 | `sleep_efficiency_14d_pct` | 动态生理 | 睡眠域 |
| 26 | `nocturnal_spo2_drop_burden_14d_pct` | 动态生理 | 支持设备才启用 |
| 27 | `steps_7d_mean` | 行为执行 | 活动域 |
| 28 | `mvpa_minutes_7d` | 行为执行 | 活动域 |
| 29 | `sedentary_hours_7d_mean` | 行为执行 | 活动域 |
| 30 | `active_day_regularity_14d_pct` | 行为执行 | 活动规律 |
| 31 | `weight_change_28d_pct` | 行为执行 | 代谢响应 |
| 32 | `adherence_composite_28d_pct` | 行为执行 | 行为与依从性域 |

`adherence_composite_28d_pct` 只是评分聚合值；用药、运动、饮食、血压测量、
复诊/检验完成率必须在业务数据库中分列保存，不能只保存聚合值。

## 3. 旧字段到新字段迁移

| CVD-16 字段 | v2 处理 |
| --- | --- |
| `age` | 映射到 `age`，只进入长期风险，不直接压低每日 RHI |
| `gender` | 0/1 明确映射为 `biological_sex` |
| `bmi` | 映射到 `bmi`，后续增加腰围和体重趋势 |
| `sbp` / `dbp` | 不自动映射；单次值不能伪装成 7 日家庭均值 |
| `fasting_glucose` | 映射到 `glycemia_value`，标记 `fasting_glucose_mmol_l` |
| `total_cholesterol` | 映射到 `total_cholesterol` |
| `ldl` / `hdl` / `triglycerides` | 映射到 `ldl_c` / `hdl_c` / `triglycerides` |
| `exercise_days` | 不映射；由步数、MVPA、久坐、规律性和心肺适能替代 |
| `smoking` | 映射到 `nicotine_exposure`；后续扩展电子烟和二手烟 |
| `drinking` | 不进入 32 维核心，保留为干预背景 |
| `diabetes_history` | 映射到 `diabetes_status` |
| `hypertension_history` | 不映射为用药或血压均值 |
| `family_history` | 不自动映射到早发家族史；正式接入前需补充“早发”定义和采集字段 |

Android 的 `RhiV2DraftMapper` 已按上述保守规则实施。所有新字段均有显式
`FeatureQuality`；未获得的数据保持 `null/MISSING`，不得补 0 或正常值。

## 4. RHI 计算

五域默认权重：

| 健康域 | 权重 |
| --- | ---: |
| 血压与心血管负荷 `hemodynamic` | 25% |
| 活动与心肺适能 `activity_fitness` | 25% |
| 睡眠与恢复 `sleep_recovery` | 20% |
| 代谢与身体控制 `metabolic_control` | 20% |
| 行为与依从性 `behavior_adherence` | 10% |

每个指标同时计算透明的绝对分 `A` 和个人改善分 `P`：

```text
z = improvement_direction
    × (current - baseline_median)
    / (1.4826 × baseline_MAD + epsilon)

P = 50 + 50 × tanh(z / 2)
indicator = lambda × A + (1-lambda) × P
```

临床指标 `lambda=0.8`，动态指标 `lambda=0.5`，依从性以完成率为主。
HRV 绝对值不跨品牌评分：没有同设备个人基线时，个人改善分保持中性 50。

可信度按状态和来源计算，并执行中性收缩：

```text
adjusted_indicator = 50 + q × (indicator - 50)
```

缺失字段 `q=0`，因此只回到中性分，绝不会因为补“正常值”而得到高分。
产品等级决定纳入的指标：

- Lite：可穿戴动态生理、活动、睡眠、尼古丁和依从性；
- Standard：Lite + 验证血压、BMI/腰围和体重趋势；
- Clinical：Standard + 化验、用药和临床风险输入。

首页显示值采用：

```text
display(t) = 0.25 × raw(t) + 0.75 × display(t-1)
```

安全事件不平滑、不折算成扣分；由上游检测/复核链路放入 `safety_flags`
并触发独立提醒。

当前阈值曲线位于 `model-service/app/rhi.py`，状态固定为
`research_preview_not_clinically_validated`。它们只能用于内部验证和接口联调，
不能作为正式医疗阈值或生产发布依据。
机器可读的 32 维注册表、域权重、lambda、改善方向、质量因子和基线策略位于
`config/rhi_v2_preview.json`；自动化测试约束字段唯一性和域权重归一化。

## 5. 服务与 APK 边界

新增模型服务预览接口：

```text
POST /v2/rhi/evaluate
```

该接口返回 `clinical_risk`、`dynamic_health_index`、`domains`、
`data_confidence`、`top_drivers` 和 `safety_flags`。它不会自行生成
China-PAR；临床概率只能由审核后的独立模型作为 `clinicalRisk` 锚点传入。
当前接口只在 development/demo 运行模式开放，production/staging 失败关闭。

Android 网络层当前只包含 DTO 和 v1→v2 草稿映射器，不声明 Retrofit 路由，
也不把研究预览当成经验证临床结果。归因页“健康改善得分”和数据页“健康指数”
可在端侧运行
`rhi-deterministic-preview-2.2.0-android-lite`：使用 Room 中可安全推导的
可穿戴字段、“我的 > 健康档案”经校验的手填指标和可信个人资料，复用本节
阈值曲线、域权重、可信度收缩与平滑。2.2.0 相对本计划引用的 2.1.0 修正了
四处计算缺陷：按 LITE/STANDARD/CLINICAL 分级判定可信度分母（由实际提取到的
证据决定，不区分手填或设备同步）、消除 `total_cholesterol` 重复计数、
MVPA 个人基线改用 7 日滚动总量、`steps_7d_mean` 恒除以 7 且未佩戴日按零
暴露计入，并新增四类仅解释可信度的质量提醒。日均久坐、腰围、正式 VO₂max、
HbA1c 和 eGFR 保存为 nullable 字段；经确认上臂袖带 7 日血压和带报告日期的
医院血检单独保存。戒指无袖带血压不进入 RHI，空白字段不补正常值。
数据页今日/7 日及归因页 7 日显示当前 RHI，30/90 日显示
有效日 RHI 稳健中位数；同页风险分以 RDI-16 名义复用既有 CVD-16 评估接口，
不在本次改动中重写 16 特征提取规则。
云端正式接入顺序：

1. 冻结共享 OpenAPI 和 `rhi-core32-v2` 字段单位；
2. Device Service/Feature Pipeline 生成版本化日特征快照；
3. JeecgBoot 增加持久化和幂等编排；
4. Android 增加认证 API、Room 缓存迁移和渐进 UI；
5. 灰度双跑 v1/v2，完成 90 天验证后才允许生产展示。

建议数据库拆分为 `clinical_risk_assessment`、`daily_health_index`、
`daily_domain_score`、`daily_feature_snapshot`、`data_quality_snapshot`、
`safety_event` 和 `intervention_execution`。

## 6. 验证门禁

临床风险必须独立报告 AUC/C-index、Brier、校准曲线、校准截距/斜率、
决策曲线和关键亚组；不得沿用横断面自报疾病分类 AUC 作为 10 年概率证据。

`rhi/validation.py` 提供首批动态 RHI 验证计算：

- 稳定性：标准差与最大日变化；
- 响应性：干预前后中位变化是否方向正确；
- 单调性：有序压力测试中的违反率；
- 设备公平性：不同设备组平均分最大差。

还必须验证缺失安全、逐分可解释、90 天短期效标、设备/固件/算法切换
重建基线，以及红旗事件绕过平滑。首轮建议 200–300 人、90 天产品验证；
目标是证明可靠、响应、可理解和设备公平，不宣称 MACE 降低。

## 7. 阶段路线

1. **Preview（当前）**：保留 v1；冻结 32 维草案；确定性 RHI、可信度和冷启动；
2. **Shadow**：云端日特征管道、不可变快照、v1/v2 双跑、不对用户展示；
3. **Pilot**：200–300 人、90 天验证，医生审核阈值和安全文案；
4. **Learned weights**：研究单调 GAM、单调 CatBoost/LightGBM、混合效应和状态空间模型；
5. **Validated production**：校准、亚组、公平、隐私和发布门禁全部通过后，
   才允许把 `algorithm_status` 改为 `validated_production`。
