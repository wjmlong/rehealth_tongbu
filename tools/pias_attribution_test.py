"""
PIAS 个人归因 - 端到端验证脚本（可复现）

模拟 Android App 的 AttributionDetailScreen 发送的随机 risk_history，
直接调用 rehealth-algorithms 里的真实 PIAS 算法（IndividualAttributor），
验证"app 随机输入 -> 走通 PIAS -> 产出真实归因结果"这条链路。

运行（隔离 venv，已被本脚本自动用 PYTHONPATH 指向 rehealth-algorithms）：
  python tools/pias_attribution_test.py

产物：
  outputs/pias_attribution_result.json  真实算法输出
  outputs/pias_attribution_report.html  可交互页面（含真实 ECharts 图表）
  outputs/pias_attribution_report.pdf   同一份报告的 PDF 版
"""

import os
import sys
import json
import random
from datetime import date, timedelta

# 让 healthagent 包可被导入（rehealth-algorithms 是仓库根）
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RA = os.path.join(ROOT, "rehealth-algorithms")
if RA not in sys.path:
    sys.path.insert(0, RA)

from healthagent.pias.attribution import IndividualAttributor  # noqa: E402


def make_history(n_days, base_risk, intervention_effect=0.0, seed=None,
                intervention_prob=0.6):
    """构造 app 会发送的 risk_history。

    Y = 风险分(0~1)；Z = 是否干预日(1/0)。
    intervention_effect>0 表示干预日风险更低（计划有效）。
    """
    rng = random.Random(seed)
    start = date(2026, 6, 15)
    hist = []
    for i in range(n_days):
        d = start + timedelta(days=i)
        is_int = 1 if rng.random() < intervention_prob else 0
        # 基线 + 缓慢下降趋势 + 噪声
        trend = -0.002 * i
        noise = rng.uniform(-0.04, 0.04)
        effect = -intervention_effect if is_int else 0.0
        y = base_risk + trend + noise + effect
        y = max(0.05, min(0.95, round(y, 4)))
        hist.append({"date": d.isoformat(), "Y": y, "Z": is_int})
    return hist


def run_one(name, hist, forecast_days=30, lang="zh"):
    attr = IndividualAttributor({"forecast_days": forecast_days})
    res = attr.attribute(hist)
    d = res.model_dump()
    d["scenario"] = name
    d["input_history"] = hist  # 用户填入的随机信息（用于报告中展示）
    d["n_days"] = len(hist)
    n_int = sum(1 for r in hist if r["Z"] == 1)
    d["n_intervention_days"] = n_int
    d["n_control_days"] = len(hist) - n_int
    return d


def main():
    scenarios = []

    # 1) 真·随机（无种子）：模拟用户在 app 里随便填的信息
    random.seed()  # 系统随机
    hist_random = make_history(30, base_risk=0.45, intervention_effect=0.04,
                              seed=None, intervention_prob=0.55)
    scenarios.append(run_one("random_any_input", hist_random))

    # 2) 演示场景（种子固定，可复现）：干预日风险更低 -> 计划有效
    hist_demo = make_history(30, base_risk=0.52, intervention_effect=0.09,
                             seed=20260715, intervention_prob=0.6)
    scenarios.append(run_one("demo_plan_effective", hist_demo))

    # 3) 数据不足分支：仅 10 天 -> status=accumulating
    hist_short = make_history(10, base_risk=0.5, seed=7, intervention_prob=0.5)
    scenarios.append(run_one("accumulating_short", hist_short))

    out_dir = os.path.join(ROOT, "outputs")
    os.makedirs(out_dir, exist_ok=True)
    out_path = os.path.join(out_dir, "pias_attribution_result.json")
    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(scenarios, f, ensure_ascii=False, indent=2, default=str)

    # 控制台摘要
    print("=" * 60)
    print("PIAS 个人归因 - 真实算法验证")
    print("=" * 60)
    for s in scenarios:
        print(f"\n[{s['scenario']}]  历史天数={s['n_days']} "
              f"(干预{s['n_intervention_days']}/对照{s['n_control_days']})")
        print(f"  status            : {s['status']}")
        print(f"  current_risk      : {s['current_risk_score']:.4f} ({s['risk_level']})")
        print(f"  trend             : {s['trend_direction']} (slope={s['trend_slope_overall']:.5f})")
        if s.get("individual_att") is not None:
            print(f"  个体ATT           : {s['individual_att']:+.4f}  "
                  f"95%CI[{s['att_ci_lower']:+.4f}, {s['att_ci_upper']:+.4f}]  "
                  f"p={s['att_p_value']:.4f}  significant={s['att_significant']}")
        else:
            print(f"  个体ATT           : 未计算（干预/对照数据不足7天）")
        print(f"  30d无干预/有干预  : {s['projected_risk_30d_no_action']:.4f} / "
              f"{s['projected_risk_30d_with_plan']:.4f}  "
              f"(降低 {s['risk_reduction_30d']:+.4f})")
        if s.get("report_text"):
            print(f"  报告标题          : {s['report_text'].get('headline')}")
    print(f"\nJSON 已写出: {out_path}")
    return out_path


if __name__ == "__main__":
    main()
