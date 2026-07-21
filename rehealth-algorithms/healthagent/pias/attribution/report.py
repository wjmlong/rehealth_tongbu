"""
Attribution Report Generator

Generates structured reports for insurance actuaries,
including all required statistical metrics and interpretations.
"""

from datetime import datetime
from typing import Dict, List, Optional, Any
from pydantic import BaseModel, Field


class AttributionReport(BaseModel):
    """Attribution report for insurance actuary review."""
    report_id: str = Field(..., description="报告ID")
    report_type: str = Field(..., description="报告类型: individual/group")
    generated_at: datetime = Field(default_factory=datetime.now)

    # Summary
    conclusion: str = Field(..., description="结论")
    recommendation: str = Field(..., description="建议")

    # Core metrics
    metrics: Dict[str, Any] = Field(default_factory=dict, description="核心指标")

    # Detailed sections
    sections: List[Dict[str, Any]] = Field(default_factory=list, description="报告章节")

    # Interpretation
    executive_summary: str = Field(default="", description="执行摘要")
    methodology: str = Field(default="", description="方法论说明")
    limitations: str = Field(default="", description="局限性说明")

    @classmethod
    def from_individual_result(cls, result: Dict) -> "AttributionReport":
        """Create report from individual attribution result."""
        import uuid

        report_id = f"IND-{datetime.now().strftime('%Y%m%d')}-{uuid.uuid4().hex[:8]}"

        report_text = result.get("report_text", {})

        sections = [
            {
                "title": "当前风险状态",
                "content": {
                    "当前风险评分": f"{result['current_risk_score']:.1%}",
                    "风险等级": result["risk_level"],
                    "趋势方向": result["trend_direction"],
                },
            },
            {
                "title": "预测分析",
                "content": {
                    "30天无干预风险": f"{result['projected_risk_30d_no_action']:.1%}",
                    "30天干预后风险": f"{result['projected_risk_30d_with_plan']:.1%}",
                    "干预获益": f"{result['risk_reduction_30d']:.1%}",
                },
            },
            {
                "title": "干预效果",
                "content": {
                    "干预天数": result["intervention_days"],
                    "干预依从性": f"{result['intervention_adherence']:.1%}",
                    "数据充分性": "充分" if result["intervention_data_sufficient"] else "不足",
                },
            },
        ]

        if result.get("individual_att") is not None:
            sections.append({
                "title": "个体干预效果 (ATT)",
                "content": {
                    "个体ATT": f"{result['individual_att']:.1%}",
                    "置信区间": f"[{result.get('att_ci_lower', 'N/A')}, {result.get('att_ci_upper', 'N/A')}]",
                    "p值": result.get("att_p_value", "N/A"),
                    "显著性": "显著" if result.get("att_significant") else "不显著",
                },
            })

        return cls(
            report_id=report_id,
            report_type="individual",
            conclusion=report_text.get("headline", ""),
            recommendation=report_text.get("advice", ""),
            metrics=report_text.get("metrics", {}),
            sections=sections,
            executive_summary=report_text.get("body", ""),
            methodology="指数衰减模型 + 配对ATT",
            limitations="个体层面因果推断存在局限，建议结合群体归因结果",
        )

    @classmethod
    def from_group_result(cls, result: Dict) -> "AttributionReport":
        """Create report from group attribution result."""
        import uuid

        report_id = f"GRP-{datetime.now().strftime('%Y%m%d')}-{uuid.uuid4().hex[:8]}"

        report = result.get("settlement_report", {})

        sections = [
            {
                "title": "核心结果",
                "content": {
                    "ATT": f"{result['att']:+.4f}",
                    "95%置信区间": f"[{result['ci_lower']:+.4f}, {result['ci_upper']:+.4f}]",
                    "p值": f"{result['p_value']:.4f}",
                    "显著性": "显著" if result["is_significant"] else "不显著",
                },
            },
            {
                "title": "样本信息",
                "content": {
                    "总样本": result["n_total"],
                    "干预组": result["n_treated"],
                    "对照组": result["n_control"],
                    "匹配对": result["n_matched_pairs"],
                },
            },
            {
                "title": "效应量",
                "content": result.get("effect_sizes", {}),
            },
            {
                "title": "敏感性分析",
                "content": {
                    "Rosenbaum Γ": result.get("gamma_sensitivity", "N/A"),
                    "E-value": result.get("e_value", {}).get("e_value", "N/A"),
                    "解释": result.get("e_value", {}).get("interpretation", ""),
                },
            },
            {
                "title": "统计功效",
                "content": result.get("power_analysis", {}),
            },
            {
                "title": "平衡性诊断",
                "content": {
                    "平衡特征数": f"{sum(1 for v in result.get('balance', {}).values() if v.get('balanced'))}/{len(result.get('balance', {}))}",
                    "重叠质量": result.get("overlap_diagnostics", {}).get("overlap_quality", "N/A"),
                },
            },
            {
                "title": "模型稳健性",
                "content": {
                    "ATT范围": result.get("propensity_robustness", {}).get("att_range", "N/A"),
                    "一致性": "一致" if result.get("propensity_robustness", {}).get("consistent") else "不一致",
                },
            },
        ]

        # Limitations
        limitations = """
1. 观察性研究设计，无法完全排除未观测混杂
2. 倾向得分匹配可能引入选择偏差
3. 干预效果可能受依从性影响
4. 结果的外推性需谨慎评估
5. 建议结合随机对照试验验证
"""

        return cls(
            report_id=report_id,
            report_type="group",
            conclusion=report.get("conclusion", ""),
            recommendation=report.get("recommendation", ""),
            metrics=report.get("metrics", {}),
            sections=sections,
            executive_summary=report.get("detail", ""),
            methodology=report.get("method", ""),
            limitations=limitations,
        )

    def to_dict(self) -> Dict:
        """Convert to dictionary."""
        return {
            "report_id": self.report_id,
            "report_type": self.report_type,
            "generated_at": self.generated_at.isoformat(),
            "conclusion": self.conclusion,
            "recommendation": self.recommendation,
            "metrics": self.metrics,
            "sections": self.sections,
            "executive_summary": self.executive_summary,
            "methodology": self.methodology,
            "limitations": self.limitations,
        }

    def to_markdown(self) -> str:
        """Convert to markdown format."""
        md = f"# 归因分析报告\n\n"
        md += f"**报告ID**: {self.report_id}\n"
        md += f"**报告类型**: {self.report_type}\n"
        md += f"**生成时间**: {self.generated_at.strftime('%Y-%m-%d %H:%M:%S')}\n\n"

        md += f"## 结论\n\n{self.conclusion}\n\n"
        md += f"## 建议\n\n{self.recommendation}\n\n"

        md += f"## 执行摘要\n\n{self.executive_summary}\n\n"

        md += f"## 核心指标\n\n"
        for k, v in self.metrics.items():
            md += f"- **{k}**: {v}\n"
        md += "\n"

        md += f"## 详细分析\n\n"
        for section in self.sections:
            md += f"### {section['title']}\n\n"
            if isinstance(section['content'], dict):
                for k, v in section['content'].items():
                    md += f"- **{k}**: {v}\n"
            else:
                md += f"{section['content']}\n"
            md += "\n"

        md += f"## 方法论\n\n{self.methodology}\n\n"
        md += f"## 局限性\n\n{self.limitations}\n"

        return md
