# 睿禾健康 — CVD 预测模型工程日记

> 维护人：吴嘉铭 (CTO)  
> 起始日期：2026-05-25  
> 最后更新：2026-05-27

---

## 一、项目目标

为 PIAS 引擎（Predict → Intervene → Attribute → Settle）构建 CVD 风险预测模型，目标 **AUC ≥ 0.88**。

---

## 二、运行环境

| 项 | 值 |
|----|-----|
| 机器 | Laptop, RTX 3060 6GB VRAM, CUDA 12.2 |
| Python | 3.10 (miniconda, env=ruihe) |
| 关键库 | CatBoost 1.2+ (GPU), XGBoost (CUDA hist), LightGBM, Optuna, scikit-learn |
| 项目路径 | `/mnt/e/DISK_D/Project/Rehealth_AI/Project/` |
| 训练脚本 | `train/train_v8_push.py`（v8 可中断管线） |

---

## 三、模型版本演进

| 版本 | 日期 | Test AUC | CV AUC | 方法 | 关键改动 |
|------|------|----------|--------|------|----------|
| V2 | 5/25 | ~0.767 | — | CatBoost 单模型 | 基线 |
| V3 | 5/26 | ~0.80 | — | 统一特征集 | 特征标准化 |
| V4 | 5/26 | ~0.82 | — | +生化指标 | 加 CBC、肝肾功、炎症 |
| V5 | 5/26 | **0.8641** | — | 3 模型 ensemble | CatBoost+LGB+XGB stacking |
| V7 | 5/27 | 0.8609 | 0.8666 | +可穿戴+CRP 插补 | 加速度计 17 特征, LGB 插补 CRP |
| V8 | 5/27 | **0.8615** | **0.8671** | 6 模型 blend3_gbdt | +ExtraTrees+RF+GBM, GPU HPO 120 trials |

### V8 各集成方法 AUC

| 方法 | AUC |
|------|-----|
| blend3_gbdt (CB+LGB+XGB 均值) | **0.8615** |
| stacking (LogisticCV meta) | 0.8608 |
| catboost 单模型 | 0.8607 |
| stack_fold_blend3 | 0.8607 |
| xgboost | 0.8604 |
| rank_avg3 | 0.8604 |
| blend6 / wblend | 0.8588 |
| gbm | 0.8563 |
| lightgbm | 0.8538 |
| extratrees | 0.8477 |
| randomforest | 0.8467 |

### V8 Top 20 特征重要性

| 排名 | 特征 | 重要性 | 类型 |
|------|------|--------|------|
| 1 | age | 6.96 | 原始 |
| 2 | hypertension_history | 5.46 | 原始 |
| 3 | smoking | 5.44 | 原始 |
| 4 | age_sbp | 4.92 | 工程 |
| 5 | age_hba1c | 4.73 | 工程 |
| 6 | age_activity | 4.64 | 可穿戴交叉 |
| 7 | age_sq | 4.63 | 工程 |
| 8 | exercise_days | 4.02 | 原始 |
| 9 | med_count | 3.27 | 工程 |
| 10 | age_hyp | 2.88 | 工程 |
| 11 | total_cholesterol | 2.85 | 原始 |
| 12 | fib4 | 2.71 | 工程 |
| 13 | egfr | 2.50 | 原始 |
| 14 | risk_factor_count | 2.10 | 工程 |
| 15 | gender | 2.04 | 原始 |
| 16 | on_chol_meds | 2.04 | 原始 |
| 17 | diabetes_history | 2.00 | 原始 |
| 18 | mhr | 1.99 | 工程 |
| 19 | rdw | 1.73 | 原始 |
| 20 | mets_score | 1.57 | 工程 |

---

## 四、训练数据

### 4.1 主数据：NHANES（美国国家健康与营养调查）

| 周期 | 原始行数 | 有加速度计 | 有 HSCRP |
|------|---------|-----------|---------|
| 2011-2012 | 9,756 | Yes | No (插补) |
| 2013-2014 | 10,175 | Yes | No (插补) |
| 2015-2016 | 9,971 | No | Yes |
| 2017-2018 | 9,254 | No | Yes |
| 2021-2023 | 11,933 | No | Yes |
| **合计** | **51,089** | | |
| **成人 (18-100)** | **31,978** | | |

**每周期加载的 XPT 文件：**

| 类别 | 文件 | 提取变量 |
|------|------|---------|
| 人口学 | DEMO | age, gender |
| 体测 | BMX | bmi, waist |
| 血压 | BPX / BPXO | sbp, dbp |
| 血糖 | GLU | fasting_glucose |
| 血脂 | TCHOL, HDL, TRIGLY | total_cholesterol, hdl, ldl, triglycerides |
| 糖化血红蛋白 | GHB | hba1c |
| 炎症 | HSCRP | crp (2011-2014 用 LGBMRegressor 插补) |
| 生化全套 | BIOPRO | uric_acid, bun, egfr, alt, ast, ggt, albumin, alk_phos |
| 血常规 | CBC | wbc, hemoglobin, hematocrit, platelet, rdw, 中性粒/淋巴/单核 |
| 问卷 | BPQ, DIQ, MCQ, SMQ, ALQ, PAQ | 高血压/糖尿病/CVD 病史, 用药, 吸烟, 饮酒, 运动 |
| 加速度计 | PAXDAY (仅 2011-2014) | 17 个可穿戴特征 |

**标签定义：** MCQ160B(冠心病) / C(心绞痛) / D(心梗) / E(中风) / F(中风)，任一自报=1  
**阳性率：** 10.89%（3,478 / 31,978）  
**分割：** Train 25,582 / Test 6,396 (80/20 stratified, random_state=42)

**CRP 插补方法：** 对 2011-2014 无 HSCRP 的周期，用 LGBMRegressor 在 log1p(CRP) 上训练，特征为 age, gender, bmi, bp, glucose, lipids, hba1c, wbc, smoking, 病史, 用药。插补后 15,641 行 (mean=2.54, 原始观测 mean=4.07)。

### 4.2 97 个特征构成

| 类别 | 数量 | 示例 |
|------|------|------|
| 原始临床 | 42 | age, bmi, sbp, hdl, hba1c, crp, wbc, rdw... |
| 原始可穿戴 | 17 | accel_daily_mims, accel_sleep_min, accel_intensity... |
| 工程-临床衍生 | 33 | pulse_pressure, tyg_index, nlr, plr, mhr, fib4, framingham_proxy, mets_score... |
| 工程-可穿戴×临床交叉 | 5 | age_activity, bmi_activity, sbp_activity, sleep_activity_ratio, activity_risk |
| **总计** | **97** | |

**9 个类别特征（CatBoost 原生处理）：** gender, smoking, drinking, diabetes_history, hypertension_history, family_history, on_bp_meds, on_chol_meds, on_diabetes_meds

### 4.3 候选外部数据

| 数据集 | 样本量 | CVD 阳性率 | 与 NHANES 共同特征 | 状态 |
|--------|--------|-----------|-------------------|------|
| **CHARLS** (中国健康养老) | 12,966 | 27.7% (hearte\|stroke) | **24 个直接映射** | 已有 CSV，零缺失 |
| **Kaggle CVD 70K** | ~70,000 | ~50% | ~10 个 (age,gender,bp,chol,glucose,smoke) | 需登录下载 |
| MIMIC-IV (ICU) | — | — | 少（ICU 场景不同） | 已有 demo，不适合 |

### 4.4 CHARLS → NHANES 特征映射（24 个）

| CHARLS 字段 | NHANES 字段 | 含义 |
|-------------|-------------|------|
| age | age | 年龄 |
| gender | gender | 性别 |
| bmi | bmi | BMI |
| mwaist | waist | 腰围 |
| systo | sbp | 收缩压 |
| diasto | dbp | 舒张压 |
| bl_glu | fasting_glucose | 空腹血糖 |
| bl_cho | total_cholesterol | 总胆固醇 |
| bl_hdl | hdl | HDL |
| bl_ldl | ldl | LDL |
| bl_tg | triglycerides | 甘油三酯 |
| smokev | smoking | 吸烟 |
| drinkev | drinking | 饮酒 |
| diabe | diabetes_history | 糖尿病史 |
| hibpe | hypertension_history | 高血压史 |
| bl_hbalc | hba1c | 糖化血红蛋白 |
| bl_crp | crp | C-反应蛋白 |
| bl_ua | uric_acid | 尿酸 |
| bl_bun | bun | 血尿素氮 |
| bl_crea | → 计算 egfr | 肌酐 (需 CKD-EPI 公式) |
| bl_hgb | hemoglobin | 血红蛋白 |
| bl_hct | hematocrit | 红细胞压积 |
| bl_wbc | wbc | 白细胞 |
| bl_plt | platelet | 血小板 |

**CHARLS 缺少的 NHANES 特征（需填 0 或缺失标记）：** exercise_days, family_history, on_bp_meds, on_chol_meds, on_diabetes_meds, rdw, neutrophil/lymphocyte/monocyte 分类, alt, ast, ggt, albumin, alk_phos, 全部 17 个可穿戴特征

**CHARLS 独有但 NHANES 未用的：** rural（城乡）, edu（教育）, marry（婚姻）, bl_mcv, bl_cysc（胱抑素C）, totmet（代谢当量）, cesd10（抑郁量表）, province

---

## 五、关键技术决策与踩坑记录

### 5.1 CatBoost GPU 兼容性问题 (5/27)

| 问题 | 报错 | 修复 |
|------|------|------|
| rsm on GPU | `rsm on GPU is supported for pairwise modes only` | 删除 `colsample_bylevel` |
| Lossguide on GPU | GPU 不支持 Lossguide grow_policy | 限制为 `["SymmetricTree", "Depthwise"]` |
| subsample + bagging_temperature | GPU 上不兼容 | 删除 `bagging_temperature`，显式设 `bootstrap_type="Bernoulli"` |
| final model 缺 bootstrap_type | `study.best_params` 不含硬编码参数 | 在 `best_cb.update()` 中手动加 `bootstrap_type: "Bernoulli"` |

### 5.2 Stacking 无提升的原因分析

V8 的 6 模型 stacking (0.8608) 仅比 CatBoost 单模型 (0.8607) 高 0.0001。原因：
1. **标签噪声上限** — 自报 CVD (MCQ160B-F) 存在漏报和误报
2. **特征同质化** — 6 个模型使用相同 97 特征，学到的信号高度重叠
3. **数据量瓶颈** — 31,978 样本对 6 模型 stacking 偏少

### 5.3 脚本可中断设计 (5/27)

`train_v8_push.py` 重构为 7 阶段管线：
- Stage 1: 数据加载+特征工程 → `checkpoints/v8/stage1_features.pkl`
- Stage 2: 数据分割 → `stage2_split.pkl`
- Stage 3a/3b/3c: Optuna HPO → SQLite `.db` 文件（`load_if_exists=True` 断点续跑）
- Stage 4: 训练 6 模型 → `stage4_base_models.pkl`
- Stage 5: Stacking+集成 → `stage5_stacking.pkl`
- Stage 6: CV+保存 → `stage6_results.json`

CLI: `--from-stage`, `--force`, `--only-stage`

---

## 六、下一步计划

### 距 0.88 的 gap = 0.0185

靠调参和模型堆叠已到天花板。可行路径：

| 方案 | 预期收益 | 复杂度 | 说明 |
|------|---------|--------|------|
| **引入 CHARLS 做 transfer** | +0.01~0.02 | 中 | 24 个共同特征，12,966 样本，中国人群 |
| **引入 Kaggle CVD 70K** | +0.005~0.01 | 中 | 需登录下载，共同特征仅 ~10 个 |
| **换标签定义** | 可能 +0.02 | 低 | Framingham 10 年风险 ≥20% 二值化，信噪比更高 |
| **增加 NHANES 未用指标** | +0.005 | 低 | 如 BNP、troponin（如果有） |

**CHARLS / Kaggle transfer 列入 V2 阶段开发需求，V1 先用 16 特征模型跑通全链路。**

---

## 六B、PIAS 全链路审计 (2026-05-27)

### 端到端测试结果

```
P(Predict) ──✅──> I(Intervene) ──✅──> A(Attribute) ──✅──> S(Settle) ✅
```

全链路已跑通。用 `rehealth_v2_final.pkl` (16 特征, AUC ~0.767) 作为 V1 生产模型。

### P → I 链路

| 步骤 | 端点 | 输入 | 输出 | 状态 |
|------|------|------|------|------|
| P | `POST /inference/score` | 16 维脱敏特征 | risk_score + risk_level + SHAP 贡献度 | ✅ |
| I | `POST /inference/prescription` | risk_result + memory_snapshot | LLM 干预处方 JSON | ✅ |

**P→I 数据流：** risk_score (0.2272) + SHAP top 5 (exercise_days, hdl, total_cholesterol, age, drinking) + 记忆快照 (age_bracket, bp_variability, night_bp_pattern, activity_level, sleep_quality_index, intervention_compliance) → MiMo LLM → JSON {diet, exercise, sleep, medication_reminder, expected_risk_reduction}

### A 层双轨归因

#### Level 1: 个人预测性归因 (面向用户)

| V1 文档要求 | 实现文件 | 状态 |
|-------------|---------|------|
| 单用户脱敏时序 | `individual_prediction.py` | ✅ |
| 历史趋势 (14天 Y 序列) | 指数衰减加权回归, min_history_days=14 | ✅ |
| 时序外推: "维持现状" | `forecast_status_quo` — 整体斜率外推 30 天 | ✅ |
| 时序外推: "执行计划" | `forecast_with_plan` — 干预日斜率外推 30 天 | ✅ |
| 输出: 风险走势报告 → App | 数值完整，文案模板已补 | ✅ |

测试输出 (21 天数据, 14 天干预):
```
当前风险: 43.97%
30天维持现状: 36.94%  |  执行计划: 34.82%
干预多降: 2.1%
```

**注意**: Level 1 不重新运行 CatBoost，它消费的是 P 步骤每天产出的 Y 值序列。设计上是正确的——前提是端侧每天调 P 步骤并记录 Y。

#### Level 2: 群体因果归因 (面向保司)

| V1 文档要求 | 实现文件 | 状态 |
|-------------|---------|------|
| 100+ 用户脱敏向量 | `group_attribution.py`, min=30 | ✅ |
| PSM 按基线特征匹配 | Caliper 最近邻, 7 维特征 | ✅ |
| DRE 双重稳健估计 → ATT | L2 逻辑回归 + Ridge outcome | ✅ |
| 200x Bootstrap → 95% CI | 百分位法 CI | ✅ |
| Ed25519 签名 | `attribution.py:76` → `report_signer.py` | ✅ |

测试输出 (120 用户, 60 干预/60 对照):
```
ATT: -4.5% (干预组比对照组多降 4.5% 风险)
95% CI: [-6.3%, -2.6%] — 统计显著
60 对匹配, 200 次 Bootstrap
```

### 已知问题

| # | 问题 | 严重度 | 说明 | 状态 |
|---|------|--------|------|------|
| 1 | `risk_scorer.py:75` 硬编码 `model_auc: 0.847` | 🟡 | 应从模型元数据读 | ✅ 已修复(2026-05-27): 改为 __init__ 参数传入 |
| 2 | V2 模型 AUC 偏低 (0.767) | 🟡 | 归因效果方向可能反直觉，需 16 特征重训 | ⏳ 数据/标签瓶颈，短期靠调参无法解决 |
| 3 | `/prescription` 不传 risk_result 时把 memory_snapshot 当特征 | 🟡 | `inference.py:45` 逻辑错误 | ✅ 已修复(2026-05-27): 提取 FEATURE_COLS + 缺字段 400 |
| 4 | `/attribution/group` 用 `hash() % N` 编码特征 | 🟡 | 不稳定，应用明确映射 | ✅ 已修复(2026-05-27): 显式字典映射 + gender 修复 |
| 5 | PSM 匹配平衡性 4/7 SMD>0.1 | 🟢 | 有诊断报告，数据量上去后会改善 | ⏳ 需更大样本量 |

---

## 七、文件清单

| 文件 | 说明 |
|------|------|
| `train/train_v8_push.py` | V8 可中断训练管线（当前主脚本） |
| `train/train_v7_wearable.py` | V7 数据加载/特征工程函数（被 V8 import） |
| `train/rehealth_v8_push.pkl` | V8 CatBoost 主模型 (1.2MB) |
| `train/rehealth_v8_push_ensemble.pkl` | V8 6 模型集成 (1.1GB) |
| `train/rehealth_v8_push.json` | V8 元数据 (AUC, 特征列表) |
| `train/rehealth_v7_wearable.pkl` | V7 模型 (306KB) |
| `train/rehealth_v5_deep.pkl` | V5 模型 (342KB) |
| `data/nhanes/` | 5 周期 NHANES XPT 文件 |
| `data/charls/CHARLS.csv` | CHARLS 数据 (12,966×51, GBK 编码) |
| `data/kaggle/` | Kaggle CVD 70K（待下载） |
