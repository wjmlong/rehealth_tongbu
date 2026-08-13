# 标准化 PSM + RWE 结算报告 Draft

## 目标

本版本把保险健康管理的研究链路固定为：

```text
冻结队列 → 基线协变量校验 → 倾向评分 → 共同支持 → 1:1 匹配
→ 匹配前后 SMD → ATT/CI/p 值 → RWE 结局解释 → 合同化财务计算
→ Draft 报告 → 人工审批
```

它用于内部研究、产品演示和合同结算前复核。它不是监管认证、精算定价或医疗理赔自动决策模块。

## 入口

```python
from healthagent.pias import PSMEngine
from healthagent.pias.insurance import SettlementEngine

psm_result = PSMEngine({
    "matching_features": [
        "age", "gender", "bmi", "sbp", "dbp", "fasting_glucose",
        "total_cholesterol", "ldl", "hdl", "triglycerides",
        "exercise_days", "smoking", "drinking", "diabetes_history",
        "hypertension_history", "family_history",
    ],
    "outcome_field": "claims_pmpm",
    "outcome_type": "continuous",
    "outcome_direction": "lower_is_better",
    "n_bootstrap": 1000,
    "min_matched_pairs": 30,
}).run(records)

report = SettlementEngine().generate_settlement_report(
    psm_result,
    insurer_info={"insurer_name": "示例保险机构", "policy_group_id": "PLAN-001"},
    reporting_period={"start": start_date, "end": end_date},
    financial_terms={
        "outcome_unit": "rmb_per_member_month",
        "effective_treated_units": 6400,
        "unit_value": 3286,
        "service_cost": 500000,
        "sharing_ratio": 0.30,
        "source_batch_id": "claims-2026Q1-v3",
        "model_version": "cvd-v2.0",
    },
)

json_payload = report.to_dict()
markdown = report.to_markdown()
```

每行记录至少包含：

```json
{
  "member_id": "member-001",
  "Z": 1,
  "claims_pmpm": 3286.0,
  "features": {
    "age": 52,
    "gender": 1,
    "bmi": 27.5,
    "sbp": 145,
    "dbp": 90,
    "fasting_glucose": 6.2,
    "total_cholesterol": 5.8,
    "ldl": 3.9,
    "hdl": 1.1,
    "triglycerides": 2.1,
    "exercise_days": 2,
    "smoking": 1,
    "drinking": 0,
    "diabetes_history": 0,
    "hypertension_history": 1,
    "family_history": 1
  }
}
```

`features` 只允许使用干预发生前或索引日已知的变量。T1 健康指标、干预依从性和随访后的理赔结果不能进入倾向评分协变量。

## 统计口径

- 主要 estimand：ATT。
- 倾向评分：标准化 Logistic 回归。
- 匹配：1:1、无放回、logit 倾向评分距离。
- Caliper：默认 `0.2 × SD(logit propensity)`。
- 主要 ATT：匹配对结局差值的均值。
- CI：匹配对差值的 percentile bootstrap。
- p 值：连续结局使用匹配对差值单样本 t 检验；二分类结局使用配对 sign test。
- AIPW/DR：分开拟合两个 outcome model，仅作为辅助诊断，不与主要 ATT 混用。
- 平衡：逐项披露匹配前 SMD、匹配后 SMD；默认匹配后 `SMD < 0.1`。

## 结算公式

若结局是每人金额或 PMPM，必须先把 ATT 转成合同定义的同单位金额。

```text
有益方向 ATT =
    -ATT, 当 outcome_direction = lower_is_better
    +ATT, 当 outcome_direction = higher_is_better

毛节省 = 有益方向 ATT × 有效干预单位数 × 单位价值
净节省 = 毛节省 - 健康管理服务成本
结算金额 = max(净节省, 0) × 合同共享比例
```

引擎不会再使用固定的“每人 50,000 元赔款”或“每人 5,000 元保费”占位公式。缺少单位价值、有效单位数或结局单位时，报告仍能生成，但金额为 0，且 `settlement_ready=false`。

## 报告中的关键追溯字段

- `snapshot_hash`：冻结输入队列和分析配置的 SHA-256。
- `source_batch_id`：保单/理赔/健康数据导入批次。
- `model_version`：风险模型或特征处理版本。
- `engine_version`：PSM/RWE 引擎版本。
- `matching_features`：实际进入 PSM 的基线协变量清单。
- `quality_gates`：样本、匹配数、共同支持、平衡性和财务口径门槛。

## 生产化前仍需补齐

1. 保险机构真实保单、已赚保费、已发生赔款和理赔冲正事实表。
2. 队列冻结、审批、退回、批准、支付核销和审计日志服务。
3. 分险种/计划/地区/依从性亚组及多重比较控制。
4. 经过统计审核的生存分析、计数结局和非正态金额结局模型。
5. 经过验证的 Rosenbaum bounds、E-value 和敏感性分析实现。
6. 合同规则、风险走廊、异常高额理赔、服务费和税务口径。
7. 固定 PDF/Word 版式、签名密钥管理和不可变报告存档。
