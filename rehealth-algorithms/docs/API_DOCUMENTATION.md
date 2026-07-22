# PIAS API 接口文档 (JeecgBoot兼容)

## 概述

PIAS (Predict, Intervene, Attribute, Settle) 心血管疾病风险预测与干预归因平台API。

- **基础路径**: `/api/pias/v2`
- **响应格式**: JeecgBoot标准格式
- **认证**: Token (Header: `X-Access-Token`)

## 标准响应格式

```json
{
    "success": true,
    "message": "操作成功",
    "code": 200,
    "result": {...},
    "timestamp": 1780559733777
}
```

---

## 1. 风险预测

### `POST /api/pias/v2/predict`

CVD风险预测，使用CatBoost模型对16个临床特征进行评估。

**请求参数:**

| 字段 | 类型 | 必填 | 说明 | 示例 |
|------|------|------|------|------|
| age | int | ✅ | 年龄 | 52 |
| gender | int | ✅ | 性别 0=女 1=男 | 1 |
| bmi | float | ✅ | BMI指数 | 27.5 |
| sbp | float | ✅ | 收缩压(mmHg) | 145 |
| dbp | float | ✅ | 舒张压(mmHg) | 90 |
| fasting_glucose | float | ✅ | 空腹血糖(mmol/L) | 6.2 |
| total_cholesterol | float | ✅ | 总胆固醇(mmol/L) | 5.8 |
| ldl | float | ✅ | 低密度脂蛋白(mmol/L) | 3.9 |
| hdl | float | ✅ | 高密度脂蛋白(mmol/L) | 1.1 |
| triglycerides | float | ✅ | 甘油三酯(mmol/L) | 2.1 |
| exercise_days | int | ✅ | 每周运动天数 | 2 |
| smoking | int | ✅ | 吸烟 0=否 1=是 | 1 |
| drinking | int | ✅ | 饮酒 0=否 1=是 | 0 |
| diabetes_history | int | ✅ | 糖尿病史 0=否 1=是 | 0 |
| hypertension_history | int | ✅ | 高血压史 0=否 1=是 | 1 |
| family_history | int | ✅ | 家族病史 0=否 1=是 | 1 |

**响应示例:**

```json
{
    "success": true,
    "message": "预测成功",
    "code": 200,
    "result": {
        "risk_score": 0.45,
        "risk_level": "moderate",
        "risk_level_zh": "中等风险",
        "feature_contributions": {
            "age": 0.15,
            "sbp": 0.12,
            "smoking": 0.08
        },
        "top_contributors": [
            {"feature": "age", "contribution": 0.15, "direction": "增加风险"}
        ],
        "model_version": "V2.0",
        "model_auc": 0.767
    },
    "timestamp": 1780559733777
}
```

---

## 2. 干预处方

### `POST /api/pias/v2/intervene`

基于风险评估生成个性化干预处方。

**请求参数:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| risk_score | float | ✅ | 风险评分(0-1) |
| risk_level | string | ✅ | 风险等级(low/moderate/high/very_high) |
| language | string | ❌ | 语言(zh/en)，默认zh |

**响应示例:**

```json
{
    "success": true,
    "message": "处方生成成功",
    "code": 200,
    "result": {
        "summary": "风险中等，需要积极干预",
        "recommendations": [
            "每日30分钟快走或慢跑",
            "控制盐摄入<6g/天",
            "每3个月复查血脂",
            "戒烟限酒"
        ],
        "risk_score": 0.45,
        "risk_level": "moderate"
    },
    "timestamp": 1780559737116
}
```

---

## 3. 个人归因

### `POST /api/pias/v2/attribute/individual`

Level 1: 个人风险轨迹预测与干预效果评估。

**请求参数:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| risk_history | array | ✅ | 风险历史记录 |
| forecast_days | int | ❌ | 预测天数，默认30 |
| language | string | ❌ | 语言(zh/en) |

**risk_history元素格式:**

```json
{"date": "2026-05-01", "Y": 0.52, "Z": 1}
```

- `date`: 日期
- `Y`: 风险评分(0-1)
- `Z`: 是否干预(0=否, 1=是)

**响应结构:**

```json
{
    "success": true,
    "result": {
        "report_id": "IND-20260604-8000",
        "status": "ready",
        "current_state": {
            "risk_score": 0.45,
            "risk_level": "moderate",
            "trend": "improving"
        },
        "forecast": {
            "raw": {
                "dates": ["Day 1", "Day 2", ...],
                "no_action": [0.45, 0.46, ...],
                "with_plan": [0.45, 0.44, ...],
                "ci_upper": [0.48, ...],
                "ci_lower": [0.42, ...]
            },
            "summary": {
                "30d_no_action": 0.52,
                "30d_with_plan": 0.42,
                "risk_reduction": 0.10
            }
        },
        "intervention_effect": {
            "individual_att": -0.05,
            "att_ci_lower": -0.08,
            "att_ci_upper": -0.02,
            "att_p_value": 0.003,
            "att_significant": true
        },
        "charts": {
            "risk_trend": { /* ECharts配置 */ },
            "feature_contributions": { /* ECharts配置 */ }
        },
        "animations": {
            "risk_decrease": { /* 动画配置 */ }
        },
        "reports": {
            "user": { "headline": "...", "body": "...", "advice": "..." },
            "manager": { "summary": "...", "metrics": {...} },
            "actuary": { "methodology": "...", "metrics": {...} },
            "regulator": { "compliance": "...", "audit_trail": {...} }
        }
    }
}
```

---

## 4. 群体归因

### `POST /api/pias/v2/attribute/group`

Level 2: 群体干预效果归因，PSM+DRE+Bootstrap。

**请求参数:**

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| user_records | array | ✅ | 用户记录列表 |
| n_bootstrap | int | ❌ | Bootstrap迭代次数，默认200 |
| confidence_level | float | ❌ | 置信水平，默认0.95 |
| language | string | ❌ | 语言(zh/en) |

**user_records元素格式:**

```json
{
    "device_id": "user_001",
    "Z": 1,
    "delta_Y": -0.05,
    "features": {
        "age_bracket": 2,
        "bmi_level_encoded": 1,
        "bp_baseline_grade_encoded": 2,
        "activity_level_encoded": 1,
        "gender_encoded": 1,
        "season_sin": 0.5,
        "season_cos": 0.866
    }
}
```

**响应结构:**

```json
{
    "success": true,
    "result": {
        "report_id": "GRP-20260604-8000",
        "results": {
            "att": -0.05,
            "ci_lower": -0.08,
            "ci_upper": -0.02,
            "p_value": 0.003,
            "is_significant": true
        },
        "sample": {
            "n_total": 200,
            "n_treated": 100,
            "n_control": 100,
            "n_matched": 85
        },
        "statistics": {
            "effect_sizes": { "cohens_d": -0.45, "hedges_g": -0.44 },
            "sensitivity": { "rosenbaum_gamma": 2.5, "e_value": 3.0 },
            "power": { "power": 0.85, "adequately_powered": true },
            "balance": { "n_balanced": 6, "n_total": 7 }
        },
        "charts": {
            "effect_comparison": { /* ECharts配置 */ },
            "balance": { /* ECharts配置 */ },
            "propensity_distribution": { /* ECharts配置 */ },
            "bootstrap_distribution": { /* ECharts配置 */ }
        },
        "animations": {
            "psm_matching": { /* 动画配置 */ },
            "bootstrap_sampling": { /* 动画配置 */ }
        },
        "reports": {
            "user": {...},
            "manager": {...},
            "actuary": {...},
            "regulator": {...}
        }
    }
}
```

---

## 5. 结算报告

### `POST /api/pias/v2/settle`

生成保险结算报告。

**请求参数:** 同群体归因，额外参数:

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| insurer_name | string | ❌ | 保险公司名称 |

**响应结构:**

```json
{
    "success": true,
    "result": {
        "report_id": "RPT-20260604-8000",
        "conclusion": "统计显著",
        "recommendation": "建议按约定费率结算",
        "metrics": { "att": "+0.0500", "ci_95": "[...]", "p_value": "0.0030" },
        "sections": [...],
        "methodology": "PSM + DRE + Bootstrap...",
        "limitations": "..."
    }
}
```

---

## 6. 测试端点

### `GET /api/pias/v2/test/individual?language=zh`

返回完整的个人归因mock数据，用于前端开发。

### `GET /api/pias/v2/test/group?language=zh`

返回完整的群体归因mock数据，用于前端开发。

---

## ECharts图表配置说明

所有图表配置均为标准ECharts option，前端直接使用 `chart.setOption(option)` 渲染。

### 图表类型

| 图表 | 类型 | 用途 |
|------|------|------|
| risk_trend | line | 风险趋势预测折线图 |
| feature_contributions | bar | 风险因素贡献柱状图 |
| effect_comparison | bar | 干预效果对比 |
| balance | bar | 协变量平衡性(SMD) |
| propensity_distribution | histogram | 倾向得分分布 |
| bootstrap_distribution | histogram | Bootstrap ATT分布 |

### 动画配置

| 动画 | 类型 | 说明 |
|------|------|------|
| risk_decrease | 数值过渡 | 风险评分从高到低 |
| psm_matching | 步骤动画 | PSM匹配过程 |
| bootstrap_sampling | 采样动画 | Bootstrap重采样 |

---

## 分层报告说明

每个归因结果包含4个层级的报告:

| 层级 | 受众 | 内容 |
|------|------|------|
| user | 投保用户 | 通俗易懂的健康建议 |
| manager | 管理层 | ROI、业务建议、结论摘要 |
| actuary | 精算师 | p值、CI、效应量、敏感性分析 |
| regulator | 监管机构 | 合规声明、审计追踪 |
