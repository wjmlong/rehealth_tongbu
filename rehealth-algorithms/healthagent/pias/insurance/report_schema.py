"""
Settlement Report Schema for Chinese Insurance Market

Structured reports compliant with Chinese insurance company requirements.
"""

from datetime import datetime, date
from typing import Optional, List, Dict, Any
from pydantic import BaseModel, Field


class ReportHeader(BaseModel):
    """Standard report header."""
    report_id: str = Field(..., description="报告ID")
    report_type: str = Field(..., description="报告类型")
    report_version: str = Field(default="1.0", description="报告版本")
    generation_date: datetime = Field(default_factory=datetime.now, description="生成日期")
    reporting_period_start: Optional[date] = Field(None, description="报告期开始")
    reporting_period_end: Optional[date] = Field(None, description="报告期结束")


class InsurerInfo(BaseModel):
    """Insurance company information."""
    insurer_name: str = Field(..., description="保险公司名称")
    policy_group_id: Optional[str] = Field(None, description="保单组ID")
    underwriter_name: Optional[str] = Field(None, description="核保人姓名")


class StatisticalMethodology(BaseModel):
    """Statistical methodology details."""
    method: str = Field(default="PSM + DRE", description="统计方法")
    psm_caliper: str = Field(default="auto", description="PSM卡尺值")
    psm_matching: str = Field(default="KD-tree nearest neighbor", description="PSM匹配方法")
    dre_estimator: str = Field(default="Doubly Robust", description="DRE估计量")
    bootstrap_iterations: int = Field(default=200, description="Bootstrap迭代次数")
    confidence_level: float = Field(default=0.95, description="置信水平")
    rosenbaum_gamma: float = Field(..., description="Rosenbaum Γ值")
    sensitivity_interpretation: str = Field(..., description="敏感性分析解读")


class CohortComposition(BaseModel):
    """Cohort composition table."""
    n_total: int = Field(..., description="总样本量")
    n_treated: int = Field(..., description="干预组人数")
    n_control: int = Field(..., description="对照组人数")
    n_matched_pairs: int = Field(..., description="匹配对数")
    matching_rate: float = Field(..., description="匹配率")


class BalanceDiagnostics(BaseModel):
    """Balance diagnostics for matching."""
    feature: str = Field(..., description="特征名称")
    smd_before: float = Field(..., description="匹配前SMD")
    smd_after: float = Field(..., description="匹配后SMD")
    balanced: bool = Field(..., description="是否平衡(SMD<0.1)")


class ATTResult(BaseModel):
    """ATT estimation result."""
    att_estimate: float = Field(..., description="ATT估计值")
    ci_lower: float = Field(..., description="置信区间下限")
    ci_upper: float = Field(..., description="置信区间上限")
    p_value: Optional[float] = Field(None, description="p值")
    is_significant: bool = Field(..., description="是否显著")
    interpretation: str = Field(..., description="结果解读")


class FinancialImpact(BaseModel):
    """Financial impact section."""
    estimated_claims_avoided: float = Field(..., description="预计避免理赔金额(元)")
    premium_savings: float = Field(..., description="保费节省(元)")
    per_user_value: float = Field(..., description="每用户价值(元)")
    roi: Optional[float] = Field(None, description="投资回报率")


    outcome_unit: str = Field(default="unspecified", description="Outcome unit")
    effective_treated_units: int = Field(default=0, description="Effective treated units")
    att_per_unit: Optional[float] = Field(None, description="ATT per unit")
    gross_savings: float = Field(default=0, description="Gross savings")
    service_cost: float = Field(default=0, description="Service cost")
    net_savings: float = Field(default=0, description="Net savings")
    sharing_ratio: float = Field(default=0, description="Contract sharing ratio")
    settlement_amount: float = Field(default=0, description="Settlement amount")
    formula: str = Field(default="", description="Reproducible financial formula")


class ComplianceSection(BaseModel):
    """Compliance section."""
    regulatory_references: List[str] = Field(
        default_factory=list,
        description="监管参考文件"
    )
    data_anonymization_method: str = Field(
        default="k-anonymity + l-diversity",
        description="数据匿名化方法"
    )
    data_residency: str = Field(default="China", description="数据驻留地")
    encryption_standard: str = Field(default="SM2/SM3/SM4", description="加密标准")


class DigitalSignature(BaseModel):
    """Digital signature section."""
    signature_algorithm: str = Field(default="Ed25519", description="签名算法")
    signature: str = Field(..., description="数字签名")
    signed_at: datetime = Field(default_factory=datetime.now, description="签名时间")
    signed_by: str = Field(..., description="签名者")
    blockchain_anchor: Optional[str] = Field(None, description="区块链锚点")


class SettlementReport(BaseModel):
    """Complete settlement report for insurance companies."""
    header: ReportHeader = Field(..., description="报告头")
    insurer_info: InsurerInfo = Field(..., description="保险公司信息")
    methodology: StatisticalMethodology = Field(..., description="统计方法")
    cohort: CohortComposition = Field(..., description="队列组成")
    balance_diagnostics: List[BalanceDiagnostics] = Field(
        default_factory=list,
        description="平衡性诊断"
    )
    att_result: ATTResult = Field(..., description="ATT结果")
    financial_impact: FinancialImpact = Field(..., description="财务影响")
    compliance: ComplianceSection = Field(default_factory=ComplianceSection)
    signature: Optional[DigitalSignature] = Field(None, description="数字签名")

    # Summary text
    conclusion: str = Field(..., description="结论")
    recommendation: str = Field(..., description="建议")
    detail: str = Field(..., description="详细说明")
    method_summary: str = Field(..., description="方法摘要")


    data_provenance: Dict[str, Any] = Field(default_factory=dict, description="Data provenance")
    quality_gates: Dict[str, Any] = Field(default_factory=dict, description="Quality gates")
    report_status: str = Field(default="draft", description="Report status")

    def to_dict(self) -> Dict[str, Any]:
        """Return a JSON-compatible report payload across Pydantic versions."""
        if hasattr(self, "model_dump"):
            return self.model_dump(mode="json")
        return self.dict()

    def to_markdown(self) -> str:
        """Render the standardized Draft report for internal review."""
        data = self.to_dict()
        header = data["header"]
        cohort = data["cohort"]
        att = data["att_result"]
        financial = data["financial_impact"]
        gates = data.get("quality_gates", {})
        lines = [
            "# ReHealth PSM + RWE 结算报告（Draft）",
            "",
            f"- 报告 ID：{header['report_id']}",
            f"- 报告状态：{data.get('report_status', 'draft')}",
            f"- 生成时间：{header['generation_date']}",
            "",
            "## 结论",
            "",
            data["conclusion"],
            "",
            data["detail"],
            "",
            "## 队列与 PSM",
            "",
            f"- 总人数：{cohort['n_total']}",
            f"- 干预组：{cohort['n_treated']}",
            f"- 对照组：{cohort['n_control']}",
            f"- 匹配对数：{cohort['n_matched_pairs']}",
            f"- 干预组匹配率：{cohort['matching_rate']:.1%}",
            f"- ATT：{att['att_estimate']:+.6g}",
            f"- 置信区间：[{att['ci_lower']:+.6g}, {att['ci_upper']:+.6g}]",
            f"- p 值：{att.get('p_value', 'N/A')}",
            "",
            "## 财务影响",
            "",
            f"- 结局单位：{financial['outcome_unit']}",
            f"- 有效干预单位：{financial['effective_treated_units']}",
            f"- 毛节省：¥{financial['gross_savings']:,.2f}",
            f"- 服务成本：¥{financial['service_cost']:,.2f}",
            f"- 净节省：¥{financial['net_savings']:,.2f}",
            f"- 共享比例：{financial['sharing_ratio']:.1%}",
            f"- 结算金额：¥{financial['settlement_amount']:,.2f}",
            f"- 公式：`{financial['formula']}`",
            "",
            "## 质量门槛",
            "",
        ]
        lines.extend(f"- {key}: {value}" for key, value in gates.items())
        lines.extend([
            "",
            "## 数据血缘",
            "",
            f"- 快照哈希：`{data.get('data_provenance', {}).get('snapshot_hash')}`",
            f"- 引擎版本：`{data.get('data_provenance', {}).get('engine_version')}`",
            f"- 模型版本：`{data.get('data_provenance', {}).get('model_version')}`",
            "",
            "## 方法与限制",
            "",
            data["method_summary"],
            "",
            "本报告为内部 Draft。观察性研究不能排除未测量混杂；未通过质量门槛或缺少合同财务口径时，不得直接用于正式结算、定价或监管提交。",
        ])
        return "\n".join(lines) + "\n"


class QuarterlyReport(BaseModel):
    """Quarterly settlement report."""
    header: ReportHeader = Field(..., description="报告头")
    insurer_info: InsurerInfo = Field(..., description="保险公司信息")
    quarter: str = Field(..., description="季度(如2026Q1)")

    # Summary statistics
    total_claims: int = Field(..., description="总理赔数")
    total_amount: float = Field(..., description="总理赔金额(元)")
    approved_claims: int = Field(..., description="批准理赔数")
    rejected_claims: int = Field(..., description="拒绝理赔数")

    # Risk improvement
    avg_risk_score_before: float = Field(..., description="平均干预前风险评分")
    avg_risk_score_after: float = Field(..., description="平均干预后风险评分")
    avg_risk_reduction: float = Field(..., description="平均风险降低幅度")

    # Individual claims
    claims: List[Dict[str, Any]] = Field(default_factory=list, description="理赔明细")

    # Signature
    signature: Optional[DigitalSignature] = Field(None)


class AnnualReport(BaseModel):
    """Annual actuarial report."""
    header: ReportHeader = Field(..., description="报告头")
    insurer_info: InsurerInfo = Field(..., description="保险公司信息")
    year: int = Field(..., description="年份")

    # Annual statistics
    total_policyholders: int = Field(..., description="总投保人数")
    total_premium: float = Field(..., description="总保费(元)")
    total_claims: float = Field(..., description="总理赔金额(元)")
    loss_ratio: float = Field(..., description="赔付率")

    # Risk trends
    risk_trend: Dict[str, Any] = Field(default_factory=dict, description="风险趋势")
    cohort_analysis: Dict[str, Any] = Field(default_factory=dict, description="队列分析")

    # Actuarial tables
    mortality_table: Optional[Dict[str, Any]] = Field(None, description="死亡率表")
    morbidity_table: Optional[Dict[str, Any]] = Field(None, description="发病率表")

    # Signature
    signature: Optional[DigitalSignature] = Field(None)


class IndividualClaimReport(BaseModel):
    """Individual claim evidence report."""
    header: ReportHeader = Field(..., description="报告头")
    claim_id: str = Field(..., description="理赔号")
    policy_id: str = Field(..., description="保单号")
    insured_id: str = Field(..., description="被保险人ID")

    # Risk assessment
    risk_score_before: float = Field(..., description="干预前风险评分")
    risk_score_after: float = Field(..., description="干预后风险评分")
    risk_reduction: float = Field(..., description="风险降低幅度")

    # Intervention details
    intervention_type: str = Field(..., description="干预类型")
    intervention_start: date = Field(..., description="干预开始日期")
    intervention_end: date = Field(..., description="干预结束日期")
    intervention_adherence: float = Field(..., description="干预依从性")

    # Attribution
    individual_att: float = Field(..., description="个体ATT")
    confidence_interval: Dict[str, float] = Field(
        default_factory=dict,
        description="置信区间"
    )

    # Clinical evidence
    clinical_markers: Dict[str, Any] = Field(
        default_factory=dict,
        description="临床指标变化"
    )

    # Recommendation
    claim_recommendation: str = Field(..., description="理赔建议")
    recommended_amount: float = Field(..., description="建议理赔金额(元)")

    # Signature
    signature: Optional[DigitalSignature] = Field(None)
