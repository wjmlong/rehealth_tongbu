# 睿禾健康 PIAS 项目总结文档

## 一、项目概述

**项目名称**: PIAS (Predict, Intervene, Attribute, Settle)  
**定位**: 心血管疾病风险预测与干预归因平台  
**技术栈**: FastAPI + CatBoost + LLM + Ed25519签名  
**GitHub**: https://github.com/RehealthAI/Rehealth_AI  

---

## 二、已完成模块

### 2.1 PIAS核心引擎

| 模块 | 文件 | 功能 | 状态 |
|------|------|------|------|
| 风险预测 | `risk_scorer.py` | CatBoost 16特征CVD预测 | ✅ |
| 特征工程 | `feature_engineering.py` | 16→40+派生特征 | ✅ |
| 增强预测 | `enhanced_scorer.py` | 特征工程+模型融合 | ✅ |
| 个人归因 | `attribution/individual.py` | 指数衰减+配对ATT | ✅ |
| 群体归因 | `attribution/group.py` | PSM+DRE+Bootstrap | ✅ |
| 归因报告 | `attribution/report.py` | 分层报告生成 | ✅ |
| 中国校准 | `china_calibration.py` | 中国人群阈值校准 | ✅ |
| 知识蒸馏 | `knowledge_distillation.py` | V8→V2模型蒸馏 | ✅ |

### 2.2 保险业务模块

| 模块 | 文件 | 功能 | 状态 |
|------|------|------|------|
| 保单管理 | `insurance/models.py` | 保单、理赔、保险公司模型 | ✅ |
| 结算引擎 | `insurance/settlement_engine.py` | 结算报告生成 | ✅ |
| 保费计算 | `insurance/premium_calculator.py` | 动态保费+健康奖励 | ✅ |
| 报告模板 | `insurance/report_schema.py` | 结构化报告模板 | ✅ |

### 2.3 合规模块

| 模块 | 文件 | 功能 | 状态 |
|------|------|------|------|
| 同意管理 | `compliance/consent_manager.py` | PIPL知情同意 | ✅ |
| 数据删除 | `compliance/data_deletion.py` | 删除权实现 | ✅ |
| 审计日志 | `compliance/audit_trail.py` | 不可篡改审计 | ✅ |

### 2.4 精算验证模块

| 模块 | 文件 | 功能 | 状态 |
|------|------|------|------|
| 精算验证 | `actuarial_validation.py` | Framingham/China-PAR对比 | ✅ |
| 增强归因 | `enhanced_attribution.py` | p值+E-value+功效分析 | ✅ |

---

## 三、API接口

### 3.1 JeecgBoot兼容格式

```json
{
    "success": true,
    "message": "操作成功",
    "code": 200,
    "result": {...},
    "timestamp": 1780559733777
}
```

### 3.2 接口列表

| 端点 | 方法 | 说明 |
|------|------|------|
| `/api/pias/v2/predict` | POST | CVD风险预测(16特征) |
| `/api/pias/v2/intervene` | POST | 干预处方生成 |
| `/api/pias/v2/attribute/individual` | POST | 个人归因分析 |
| `/api/pias/v2/attribute/group` | POST | 群体归因分析 |
| `/api/pias/v2/settle` | POST | 保险结算报告 |
| `/api/pias/v2/test/individual` | GET | 测试数据(个人) |
| `/api/pias/v2/test/group` | GET | 测试数据(群体) |
| `/api/pias/v2/weijiankang/heatmap` | GET | 区县风险热力图 |
| `/api/pias/v2/weijiankang/eval` | GET | 管理效果评估 |
| `/api/pias/v2/weijiankang/high-risk` | GET | 极高危人群追踪 |
| `/api/pias/v2/weijiankang/ranking` | GET | 中心绩效排名 |
| `/api/pias/v2/weijiankang/2030` | GET | 健康中国2030考核 |

### 3.3 响应特性

- **ECharts图表**: 风险趋势、特征贡献、干预效果对比、倾向得分分布、Bootstrap分布
- **动画配置**: 风险下降动画、PSM匹配动画、Bootstrap采样动画
- **分层报告**: user(用户)、manager(管理层)、actuary(精算师)、regulator(监管)
- **双语支持**: 中文/英文

---

## 四、数据资产

### 4.1 训练数据

| 数据集 | 来源 | 样本量 | 特征数 | 用途 |
|--------|------|--------|--------|------|
| NHANES 2015-2016 | 美国CDC | 5,992 | 16 | V2模型训练 |
| CHARLS | 中国健康与养老 | 12,966 | 51 | 中国人群校准 |
| MIMIC-IV | 重症监护 | - | - | 重症预测 |

### 4.2 真实业务数据(杭州)

| 数据集 | 样本量 | 内容 | 来源 |
|--------|--------|------|------|
| 高血脂一览 | 31,016人 | TC/LDL/HDL/TG+血压+BMI+10年危险性 | 杭州市卫健委 |
| 2015vs2014对比 | 25,583人 | 血压/血脂/血糖/BMI纵向变化 | 杭州市卫健委 |

### 4.3 模型版本

| 版本 | 特征数 | AUC | 用途 |
|------|--------|-----|------|
| V2 | 16 | 0.767 | **生产API** |
| V4 | 37 | - | 研究 |
| V5 | 74 | 0.8641 | 研究 |
| V8 | 97 | 0.8615 | 研究基线 |

---

## 五、杭州卫健委服务方案

### 5.1 区县CVD风险热力图

- **数据**: 31,016人 × 8个区县
- **发现**: 下城区风险最高(33.8%), 江干区最低(26.5%)
- **政绩**: 区域差异化资源配置依据

### 5.2 高血脂分级管理效果评估

- **数据**: 25,583人纵向对比
- **发现**: SBP -1.135mmHg***, TC -0.306***
- **政绩**: 慢病管理示范区申报数据支撑

### 5.3 极高危人群靶向干预追踪

- **数据**: 1,181人极高危子集
- **发现**: SBP -14.07mmHg***, TC -1.124***
- **政绩**: 精准健康管理典型案例

### 5.4 社区卫生服务中心绩效排名

- **数据**: 69个社区中心
- **发现**: 拱墅区/半山街道最优(+6.83mmHg)
- **政绩**: 基层机构客观考核

### 5.5 健康中国2030考核达标

- **数据**: 全量25,583人
- **发现**: 高血压率 -4.4pp, 高TC率 -7.8pp
- **政绩**: 考核指标自动化生成

---

## 六、技术架构

```
Rehealth_AI/
├── api/
│   ├── main.py                    # FastAPI应用
│   └── routers/
│       ├── pias.py                # PIAS原始API
│       ├── pias_jeecg.py          # JeecgBoot兼容API (12端点)
│       ├── insurance.py           # 保险业务API
│       └── ...
├── healthagent/
│   └── pias/
│       ├── attribution/           # 归因模块
│       │   ├── individual.py      # 个人归因
│       │   ├── group.py           # 群体归因
│       │   └── report.py          # 报告生成
│       ├── insurance/             # 保险模块
│       ├── compliance/            # 合规模块
│       ├── feature_engineering.py # 特征工程
│       ├── china_calibration.py   # 中国校准
│       └── ...
├── data/
│   ├── solution_1_heatmap.json    # 热力图数据
│   ├── solution_2_eval.json       # 评估数据
│   ├── solution_3_high_risk.json  # 极高危数据
│   ├── solution_4_ranking.json    # 排名数据
│   └── solution_5_2030.json       # 考核数据
├── docs/
│   ├── API_DOCUMENTATION.md       # 接口文档
│   ├── HANGZHOU_WEIJIANKANG_PROPOSAL.md
│   └── PROJECT_SUMMARY.md         # 本文档
└── train/
    ├── rehealth_v2_final.pkl      # V2生产模型
    ├── rehealth_v8_push.pkl       # V8研究模型
    └── ...
```

---

## 七、下一步计划

### 7.1 短期(1-2周)

- [ ] 用杭州真实数据训练本地化模型
- [ ] 完善ECharts图表渲染
- [ ] 补充单元测试

### 7.2 中期(1-2月)

- [ ] 接入可穿戴设备数据
- [ ] 完成等保三级认证文档
- [ ] 对接保险公司API

### 7.3 长期(3-6月)

- [ ] 部署到阿里云/腾讯云
- [ ] 获取NMPA二类医疗器械注册
- [ ] 推广到更多区县

---

## 八、联系方式

- **CTO**: 吴嘉铭
- **公司**: 睿禾健康 (ReHealth AI)
- **GitHub**: https://github.com/RehealthAI
