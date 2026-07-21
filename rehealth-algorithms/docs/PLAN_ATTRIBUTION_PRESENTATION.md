# 归因报告呈现方案设计

## 一、需求总结

### 受众（4个独立模板）

| 受众 | 关注点 | 语言风格 |
|------|--------|----------|
| 保险精算师 | p值、CI、敏感性分析、效应量 | 专业术语 |
| 管理层 | ROI、业务建议、结论摘要 | 通俗+图表 |
| 投保用户 | 健康建议、风险等级、趋势 | 通俗易懂 |
| 监管机构 | 合规格式、方法论、审计追踪 | 专业+规范 |

### 输出格式

- **主格式**: API JSON（结构化数据）
- **图表**: ECharts配置（客户端渲染）
- **PDF**: 后续再考虑
- **语言**: 双语支持（中/英）

### 可视化图表（4个）

1. **风险趋势折线图** - 风险评分随时间变化，带置信区间
2. **干预效果对比图** - 干预组vs对照组的风险变化
3. **倾向得分分布图** - 匹配前后的分布对比
4. **特征贡献瀑布图** - 各风险因素的贡献

### 动画效果（3个）

1. **风险下降动画** - 风险评分从高到低的过渡
2. **PSM匹配动画** - 匹配过程的可视化
3. **Bootstrap动画** - 置信区间的采样过程

---

## 二、API设计

### 个人归因报告

```json
POST /api/pias/attribution/individual
```

**响应结构**:
```json
{
  "report_id": "IND-20260529-abc123",
  "generated_at": "2026-05-29T10:00:00Z",

  // 核心结论
  "summary": {
    "headline": "健康计划有效，继续保持",
    "headline_en": "Health plan effective, keep going",
    "conclusion": "干预显著降低心血管风险",
    "recommendation": "建议维持当前方案"
  },

  // 当前状态
  "current_state": {
    "risk_score": 0.45,
    "risk_level": "moderate",
    "trend": "improving",
    "trend_slope": -0.002
  },

  // 预测数据（原始+聚合）
  "forecast": {
    "raw": {
      "dates": ["2026-05-30", "2026-05-31", ...],
      "no_action": [0.45, 0.452, ...],
      "with_plan": [0.45, 0.448, ...],
      "ci_upper": [0.48, 0.482, ...],
      "ci_lower": [0.42, 0.418, ...]
    },
    "weekly": {
      "weeks": ["Week 1", "Week 2", ...],
      "no_action": [0.45, 0.46, ...],
      "with_plan": [0.45, 0.44, ...]
    },
    "summary": {
      "30d_no_action": 0.52,
      "30d_with_plan": 0.42,
      "risk_reduction": 0.10
    }
  },

  // 干预效果
  "intervention_effect": {
    "individual_att": -0.05,
    "ci_lower": -0.08,
    "ci_upper": -0.02,
    "p_value": 0.003,
    "significant": true,
    "effect_size_d": 0.45,
    "interpretation": "中等效应"
  },

  // ECharts图表配置
  "charts": {
    "risk_trend": {
      "type": "line",
      "title": {"text": "风险趋势预测", "text_en": "Risk Trend Forecast"},
      "xAxis": {"data": [...]},
      "series": [...],
      "animation": {"duration": 2000, "easing": "cubicOut"}
    },
    "feature_contributions": {
      "type": "waterfall",
      "title": {"text": "风险因素贡献", "text_en": "Risk Factor Contributions"},
      "data": [...]
    }
  },

  // 动画配置
  "animations": {
    "risk_decrease": {
      "from": 0.55,
      "to": 0.45,
      "duration": 2000,
      "easing": "cubicOut"
    }
  },

  // 分层报告
  "reports": {
    "user": {
      "headline": "您的心血管风险正在改善",
      "body": "坚持健康计划，30天后风险可降低10%",
      "advice": "建议继续当前饮食和运动方案"
    },
    "manager": {
      "summary": "干预有效，ROI正向",
      "metrics": {"risk_reduction": "10%", "nnt": 10}
    },
    "actuary": {
      "methodology": "指数衰减模型 + 配对ATT",
      "metrics": {"att": -0.05, "p_value": 0.003, "ci": [-0.08, -0.02]},
      "sensitivity": {"e_value": 2.5, "gamma": 2.0}
    },
    "regulator": {
      "compliance": "符合《健康保险管理办法》要求",
      "audit_trail": {"report_id": "...", "signed_at": "..."}
    }
  }
}
```

### 群体归因报告

```json
POST /api/pias/attribution/group
```

**响应结构**:
```json
{
  "report_id": "GRP-20260529-xyz789",

  // 核心结果
  "results": {
    "att": -0.05,
    "ci_lower": -0.08,
    "ci_upper": -0.02,
    "p_value": 0.003,
    "is_significant": true
  },

  // 样本信息
  "sample": {
    "n_total": 200,
    "n_treated": 100,
    "n_control": 100,
    "n_matched": 85
  },

  // 统计指标
  "statistics": {
    "effect_sizes": {
      "cohens_d": -0.45,
      "hedges_g": -0.44,
      "interpretation": "中等效应"
    },
    "sensitivity": {
      "rosenbaum_gamma": 2.5,
      "e_value": 3.0,
      "interpretation": "结论较稳健"
    },
    "power": {
      "power": 0.85,
      "min_n_80": 150,
      "adequately_powered": true
    },
    "balance": {
      "n_balanced": 6,
      "n_total": 7,
      "smd_details": {...}
    }
  },

  // ECharts图表配置
  "charts": {
    "psm_comparison": {
      "type": "bar",
      "title": {"text": "干预效果对比", "text_en": "Intervention Effect Comparison"},
      "animation": {...}
    },
    "propensity_distribution": {
      "type": "histogram",
      "title": {"text": "倾向得分分布", "text_en": "Propensity Score Distribution"},
      "before_matching": [...],
      "after_matching": [...]
    },
    "bootstrap_distribution": {
      "type": "histogram",
      "title": {"text": "Bootstrap分布", "text_en": "Bootstrap Distribution"},
      "data": [...],
      "ci_lines": [...]
    }
  },

  // 动画配置
  "animations": {
    "psm_matching": {
      "steps": [
        {"treated": [...], "control": [...]},
        {"matches": [[0, 3], [1, 5], ...]}
      ],
      "duration": 3000
    },
    "bootstrap_sampling": {
      "iterations": 50,
      "duration": 5000,
      "data": [...]
    }
  },

  // 分层报告
  "reports": {
    "user": {...},
    "manager": {...},
    "actuary": {...},
    "regulator": {...}
  }
}
```

---

## 三、ECharts图表配置

### 1. 风险趋势折线图

```json
{
  "type": "line",
  "title": {
    "text": "心血管风险趋势预测",
    "text_en": "CVD Risk Trend Forecast"
  },
  "tooltip": {"trigger": "axis"},
  "legend": {
    "data": ["无干预", "干预后", "置信区间"]
  },
  "xAxis": {
    "type": "category",
    "data": ["Day 1", "Day 2", ...]
  },
  "yAxis": {
    "type": "value",
    "name": "风险评分",
    "min": 0,
    "max": 1
  },
  "series": [
    {
      "name": "无干预",
      "type": "line",
      "data": [0.45, 0.46, ...],
      "lineStyle": {"color": "#ff6b6b"}
    },
    {
      "name": "干预后",
      "type": "line",
      "data": [0.45, 0.44, ...],
      "lineStyle": {"color": "#51cf66"}
    },
    {
      "name": "置信区间",
      "type": "line",
      "data": [...],
      "areaStyle": {"opacity": 0.3}
    }
  ],
  "animation": {
    "duration": 2000,
    "easing": "cubicOut"
  }
}
```

### 2. 干预效果对比图

```json
{
  "type": "bar",
  "title": {
    "text": "干预效果对比",
    "text_en": "Intervention Effect Comparison"
  },
  "xAxis": {
    "data": ["干预组", "对照组"]
  },
  "series": [
    {
      "name": "风险变化",
      "type": "bar",
      "data": [
        {"value": -0.05, "itemStyle": {"color": "#51cf66"}},
        {"value": -0.01, "itemStyle": {"color": "#868e96"}}
      ]
    }
  ]
}
```

### 3. 倾向得分分布图

```json
{
  "type": "histogram",
  "title": {
    "text": "倾向得分分布",
    "text_en": "Propensity Score Distribution"
  },
  "series": [
    {
      "name": "匹配前-干预组",
      "type": "bar",
      "data": [...]
    },
    {
      "name": "匹配前-对照组",
      "type": "bar",
      "data": [...]
    },
    {
      "name": "匹配后-干预组",
      "type": "bar",
      "data": [...]
    },
    {
      "name": "匹配后-对照组",
      "type": "bar",
      "data": [...]
    }
  ]
}
```

### 4. 特征贡献瀑布图

```json
{
  "type": "waterfall",
  "title": {
    "text": "风险因素贡献",
    "text_en": "Risk Factor Contributions"
  },
  "data": [
    {"name": "年龄", "value": 0.15},
    {"name": "血压", "value": 0.12},
    {"name": "血糖", "value": 0.08},
    {"name": "血脂", "value": 0.05},
    {"name": "吸烟", "value": 0.10},
    {"name": "BMI", "value": 0.03}
  ]
}
```

---

## 四、动画配置

### 1. 风险下降动画

```json
{
  "type": "risk_decrease",
  "from": 0.55,
  "to": 0.45,
  "duration": 2000,
  "easing": "cubicOut",
  "steps": [
    {"progress": 0, "value": 0.55, "label": "干预前"},
    {"progress": 0.5, "value": 0.50, "label": "干预中"},
    {"progress": 1, "value": 0.45, "label": "干预后"}
  ]
}
```

### 2. PSM匹配动画

```json
{
  "type": "psm_matching",
  "duration": 3000,
  "steps": [
    {
      "step": 1,
      "title": "计算倾向得分",
      "treated": [{"id": 1, "ps": 0.7}, ...],
      "control": [{"id": 101, "ps": 0.3}, ...]
    },
    {
      "step": 2,
      "title": "匹配最近邻",
      "matches": [
        {"treated": 1, "control": 105, "distance": 0.02},
        ...
      ]
    },
    {
      "step": 3,
      "title": "移除不匹配样本",
      "matched_pairs": [...]
    }
  ]
}
```

### 3. Bootstrap动画

```json
{
  "type": "bootstrap_sampling",
  "iterations": 50,
  "duration": 5000,
  "data": {
    "original": [...],
    "samples": [
      {"iteration": 1, "indices": [...], "att": -0.048},
      {"iteration": 2, "indices": [...], "att": -0.052},
      ...
    ],
    "ci_lines": [
      {"type": "lower", "value": -0.08},
      {"type": "upper", "value": -0.02}
    ]
  }
}
```

---

## 五、实现计划

### Phase 1: 核心API（1-2天）

1. 更新 `/api/pias/attribution/individual` 端点
2. 更新 `/api/pias/attribution/group` 端点
3. 返回ECharts配置和动画数据
4. 支持中英文双语

### Phase 2: 报告模板（2-3天）

1. 创建4个报告模板（user/manager/actuary/regulator）
2. 实现分层呈现逻辑
3. 添加统计指标和解释

### Phase 3: 图表和动画（3-4天）

1. 实现风险趋势折线图配置
2. 实现干预效果对比图配置
3. 实现倾向得分分布图配置
4. 实现特征贡献瀑布图配置
5. 实现3个动画效果

### Phase 4: 测试和优化（1-2天）

1. 单元测试
2. 集成测试
3. 性能优化

---

## 六、文件结构

```
healthagent/pias/
├── attribution/
│   ├── __init__.py
│   ├── individual.py      # 个人归因（已实现）
│   ├── group.py           # 群体归因（已实现）
│   └── report.py          # 报告生成（已实现）
│
├── presentation/
│   ├── __init__.py
│   ├── echarts.py          # ECharts图表配置
│   ├── animations.py       # 动画配置
│   ├── templates/
│   │   ├── user.json       # 用户报告模板
│   │   ├── manager.json    # 管理层报告模板
│   │   ├── actuary.json    # 精算师报告模板
│   │   └── regulator.json  # 监管报告模板
│   └── i18n.py             # 国际化
│
└── api/
    └── routers/
        └── attribution.py  # 归因API端点
```
