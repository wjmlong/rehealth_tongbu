# 特征工程 + 知识蒸馏方案

## 问题背景

| 模型 | 特征数 | AUC | 用户可用性 |
|------|--------|-----|-----------|
| V2 | 16 | 0.767 | ✅ 所有用户 |
| V8 | 97 | 0.8615 | ❌ 需要血常规、肝功等 |

**目标**：在不增加用户数据输入的前提下，将V2的AUC从0.767提升到0.80-0.82。

---

## 解决方案

### 1. 特征工程 (Feature Engineering)

从16个基础特征中派生出24个新特征，总共40个特征。

#### 新增特征类型

| 类别 | 特征 | 说明 |
|------|------|------|
| **血压** | pulse_pressure | 脉压 = 收缩压 - 舒张压 |
| | mean_arterial_pressure | 平均动脉压 |
| | bp_ratio | 血压比 = 收缩压 / 舒张压 |
| | bp_category | 血压分级 (0-3) |
| **血脂** | non_hdl_cholesterol | 非HDL胆固醇 |
| | tc_hdl_ratio | 总胆固醇/HDL比值 |
| | ldl_hdl_ratio | LDL/HDL比值 |
| | trig_hdl_ratio | 甘油三酯/HDL比值 |
| | atherogenic_index | 致动脉粥样硬化指数 |
| **代谢** | metabolic_score | 代谢综合征评分 (0-5) |
| | glucose_category | 血糖分级 |
| **BMI** | bmi_category | BMI分级 |
| | bmi_age_interaction | BMI × 年龄 |
| **交互** | smoking_age_interaction | 吸烟 × 年龄 |
| | smoking_bp_interaction | 吸烟 × 收缩压 |
| | diabetes_glucose_interaction | 糖尿病 × 血糖 |
| **复合** | framingham_risk_factors | Framingham风险因子 |
| | ascvd_risk_factors | ASCVD风险因子 |
| | risk_factor_count | 风险因子计数 |
| | lifestyle_score | 生活方式评分 |
| **年龄** | age_squared | 年龄² |
| | age_risk_interaction | 年龄 × 风险因子 |
| **性别** | gender_age_interaction | 性别 × 年龄 |
| | gender_bp_interaction | 性别 × 血压 |

**预期提升**：AUC +0.01-0.02

---

### 2. 知识蒸馏 (Knowledge Distillation)

用V8模型(97特征, AUC 0.86)的知识来训练V2模型(40特征)。

#### 蒸馏流程

```
┌─────────────────────────────────────────────────────────────┐
│                    Knowledge Distillation                    │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────────┐
        │         V8 Teacher Model              │
        │    (97 features, AUC 0.86)            │
        └───────────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────────┐
        │         Generate Soft Labels          │
        │    (probability predictions)          │
        └───────────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────────┐
        │      Feature Engineering              │
        │    (16 → 40 features)                 │
        └───────────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────────┐
        │      Train Student Model              │
        │    (40 features, soft labels)         │
        └───────────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────────┐
        │      Distilled V2 Model               │
        │    (40 features, AUC 0.80-0.82)       │
        └───────────────────────────────────────┘
```

#### 关键参数

| 参数 | 值 | 说明 |
|------|-----|------|
| temperature | 3.0 | 软化概率分布 |
| alpha | 0.7 | 软标签权重 vs 硬标签 |
| student_type | catboost | 学生模型类型 |

**预期提升**：AUC +0.03-0.05

---

## 使用方法

### 方法1: 使用增强版预测器

```python
from healthagent.pias import EnhancedCVDRiskScorer

# 加载蒸馏后的模型
scorer = EnhancedCVDRiskScorer(
    model_path="models/distilled/rehealth_v2_distilled_catboost.pkl",
    model_auc=0.81,
)

# 使用相同的16个特征
patient = {
    "age": 52, "gender": 1, "bmi": 27.5,
    "sbp": 145, "dbp": 90,
    "fasting_glucose": 6.2, "total_cholesterol": 5.8,
    "ldl": 3.9, "hdl": 1.1, "triglycerides": 2.1,
    "exercise_days": 2, "smoking": 1, "drinking": 0,
    "diabetes_history": 0, "hypertension_history": 1,
    "family_history": 1,
}

result = scorer.predict(patient)

print(f"Risk Score: {result['risk_score']:.1%}")
print(f"Risk Level: {result['risk_level']}")
print(f"Model: {result['model_version']}")
print(f"Features: {result['feature_engineering']['engineered_features']}")

print("\nTop Risk Factors:")
for factor in result['top_contributors']:
    print(f"  - {factor['description']}: {factor['contribution']:+.3f}")
```

### 方法2: 训练蒸馏模型

```bash
# 准备训练数据CSV，包含：
# - 16个基础特征列
# - label列 (0/1)

# 运行蒸馏训练
python -m healthagent.pias.knowledge_distillation \
    --teacher models/rehealth_v8_final.pkl \
    --data data/training_data.csv \
    --output models/distilled \
    --student-type catboost
```

---

## 效果对比

| 指标 | 原始V2 | 增强V2 | 提升 |
|------|--------|--------|------|
| 特征数 | 16 | 40 | +24 |
| AUC | 0.767 | 0.80-0.82 | +0.03-0.05 |
| 用户输入 | 16项 | 16项 | 不变 |
| 额外数据 | 无 | 无 | 不变 |
| 推理速度 | 快 | 快 | 不变 |

---

## 技术细节

### 特征工程原理

1. **血压特征**
   - 脉压反映动脉硬化程度
   - 平均动脉压更准确反映组织灌注

2. **血脂比率**
   - TC/HDL比值比单独指标更能预测风险
   - 甘油三酯/HDL比值反映胰岛素抵抗

3. **交互特征**
   - 吸烟+高血压的协同风险
   - 年龄放大其他风险因素的影响

4. **复合评分**
   - Framingham和ASCVD是经典的心血管风险评估工具
   - 综合多个风险因子的加权评分

### 知识蒸馏原理

1. **软标签**
   - V8模型的预测概率比硬标签(0/1)包含更多信息
   - 软标签包含"不确定性"信息

2. **温度缩放**
   - 温度T=3使概率分布更平滑
   - 帮助学生模型学习更细粒度的决策边界

3. **混合训练**
   - y_combined = α × soft_label + (1-α) × hard_label
   - 平衡教师知识和真实标签

---

## 文件结构

```
healthagent/pias/
├── __init__.py                    # 模块导出
├── risk_scorer.py                 # 原始V2评分器
├── enhanced_scorer.py             # 增强版评分器
├── feature_engineering.py         # 特征工程
├── knowledge_distillation.py      # 知识蒸馏
├── individual_prediction.py       # Level 1 归因
├── group_attribution.py           # Level 2 归因
└── prescription_generator.py      # 干预处方

examples/
└── enhanced_prediction_example.py # 使用示例

models/
└── distilled/                     # 蒸馏后的模型
    ├── rehealth_v2_distilled_catboost.pkl
    └── distilled_features.txt
```

---

## 后续优化

1. **特征选择**
   - 使用互信息选择最重要的25个特征
   - 减少模型复杂度，提高泛化能力

2. **模型融合**
   - 多个蒸馏模型的集成
   - 进一步提升稳定性

3. **在线学习**
   - 随着用户数据积累，持续优化模型
   - 个性化微调

4. **主动特征推荐**
   - 推荐用户补充最有价值的特征
   - 逐步提升模型精度

---

## 参考资料

- [Hinton et al., 2015 - Distilling Knowledge in a Neural Network](https://arxiv.org/abs/1503.02531)
- [Framingham Risk Score](https://www.framinghamheartstudy.org/fhs-risk-functions/)
- [ASCVD Risk Calculator](https://tools.acc.org/ASCVD-Risk-Estimator/)
