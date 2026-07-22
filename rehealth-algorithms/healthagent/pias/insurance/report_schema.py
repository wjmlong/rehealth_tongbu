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
