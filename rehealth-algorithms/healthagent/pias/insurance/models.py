"""
Insurance Product Data Models for Chinese Health Insurance Market

Supports: Critical Illness (重疾险), Million Medical (百万医疗),
Tax-Advantaged (税优健康险), Huimin Bao (惠民保)
"""

from datetime import datetime, date
from enum import Enum
from typing import Optional, List, Dict
from pydantic import BaseModel, Field


class PolicyType(str, Enum):
    """Chinese health insurance policy types."""
    CRITICAL_ILLNESS = "critical_illness"  # 重大疾病保险
    MILLION_MEDICAL = "million_medical"  # 百万医疗险
    TAX_ADVANTAGED = "tax_advantaged"  # 税优健康险
    HUIMIN_BAO = "huimin_bao"  # 惠民保
    SPECIFIC_DISEASE = "specific_disease"  # 特定疾病保险
    TERM_LIFE = "term_life"  # 定期寿险


class ClaimStatus(str, Enum):
    """Settlement claim status."""
    PENDING = "pending"  # 待审核
    APPROVED = "approved"  # 已批准
    REJECTED = "rejected"  # 已拒绝
    SETTLED = "settled"  # 已结算
    DISPUTED = "disputed"  # 争议中


class CoverageType(str, Enum):
    """Coverage types for CVD conditions."""
    ACUTE_MI = "acute_myocardial_infarction"  # 急性心肌梗塞
    STROKE = "stroke"  # 脑中风
    CABG = "coronary_artery_bypass"  # 冠状动脉搭桥术
    HEART_FAILURE = "heart_failure"  # 心力衰竭
    ARRHYTHMIA = "arrhythmia"  # 心律失常
    PCI = "percutaneous_coronary_intervention"  # 经皮冠状动脉介入术


class InsurancePolicy(BaseModel):
    """Insurance policy model for Chinese health insurance."""
    policy_id: str = Field(..., description="保单号")
    insurer_id: str = Field(..., description="保险公司ID")
    policy_type: PolicyType = Field(..., description="保单类型")
    policyholder_id: str = Field(..., description="投保人ID")
    insured_id: str = Field(..., description="被保险人ID")

    # Coverage details
    coverage_amount: float = Field(..., description="保额(元)")
    premium: float = Field(..., description="保费(元/年)")
    deductible: float = Field(default=0, description="免赔额(元)")
    waiting_period_days: int = Field(default=90, description="等待期(天)")

    # Time range
    start_date: date = Field(..., description="生效日期")
    end_date: date = Field(..., description="到期日期")
    waiting_period_end: Optional[date] = Field(None, description="等待期结束日期")

    # Coverage conditions
    covered_conditions: List[CoverageType] = Field(
        default_factory=lambda: [CoverageType.ACUTE_MI, CoverageType.STROKE],
        description="承保的心血管疾病类型"
    )
    exclusion_clauses: List[str] = Field(
        default_factory=list,
        description="免责条款"
    )
    pre_existing_conditions: bool = Field(
        default=False,
        description="是否承保既往症"
    )

    # Status
    is_active: bool = Field(default=True, description="保单是否有效")
    created_at: datetime = Field(default_factory=datetime.now)
    updated_at: datetime = Field(default_factory=datetime.now)

    class Config:
        use_enum_values = True


class SettlementClaim(BaseModel):
    """Settlement claim model."""
    claim_id: str = Field(..., description="理赔号")
    policy_id: str = Field(..., description="保单号")
    claim_type: str = Field(..., description="理赔类型")
    claim_amount: float = Field(..., description="理赔金额(元)")

    # Evidence
    risk_score_before: float = Field(..., description="干预前风险评分")
    risk_score_after: float = Field(..., description="干预后风险评分")
    risk_reduction: float = Field(..., description="风险降低幅度")
    intervention_value: float = Field(..., description="干预价值(元)")

    # Attribution evidence
    attribution_report_id: Optional[str] = Field(None, description="归因报告ID")
    psm_att: Optional[float] = Field(None, description="PSM ATT值")
    psm_ci_lower: Optional[float] = Field(None, description="PSM置信区间下限")
    psm_ci_upper: Optional[float] = Field(None, description="PSM置信区间上限")
    rosenbaum_gamma: Optional[float] = Field(None, description="Rosenbaum Γ值")

    # Status
    status: ClaimStatus = Field(default=ClaimStatus.PENDING, description="理赔状态")
    reviewer_id: Optional[str] = Field(None, description="审核人ID")
    review_notes: Optional[str] = Field(None, description="审核备注")

    # Timestamps
    submitted_at: datetime = Field(default_factory=datetime.now)
    reviewed_at: Optional[datetime] = Field(None)
    settled_at: Optional[datetime] = Field(None)

    class Config:
        use_enum_values = True


class InsurerPartner(BaseModel):
    """Insurance company partner model."""
    insurer_id: str = Field(..., description="保险公司ID")
    insurer_name: str = Field(..., description="保险公司名称")
    insurer_name_en: Optional[str] = Field(None, description="保险公司英文名")

    # Contact info
    contact_person: str = Field(..., description="联系人")
    contact_phone: str = Field(..., description="联系电话")
    contact_email: str = Field(..., description="联系邮箱")

    # API integration
    settlement_api_endpoint: Optional[str] = Field(None, description="结算API端点")
    public_key_path: Optional[str] = Field(None, description="公钥路径")
    api_key: Optional[str] = Field(None, description="API密钥")

    # Supported products
    supported_policy_types: List[PolicyType] = Field(
        default_factory=list,
        description="支持的保单类型"
    )

    # Compliance
    nfra_license: Optional[str] = Field(None, description="NFRA许可证号")
    data_residency: str = Field(default="china", description="数据驻留地")

    # Status
    is_active: bool = Field(default=True)
    created_at: datetime = Field(default_factory=datetime.now)

    class Config:
        use_enum_values = True


class PremiumAdjustment(BaseModel):
    """Premium adjustment record."""
    adjustment_id: str = Field(..., description="调整ID")
    policy_id: str = Field(..., description="保单号")
    adjustment_date: date = Field(..., description="调整日期")

    # Premium change
    old_premium: float = Field(..., description="原保费(元)")
    new_premium: float = Field(..., description="新保费(元)")
    adjustment_amount: float = Field(..., description="调整金额(元)")
    adjustment_ratio: float = Field(..., description="调整比例")

    # Reason
    reason: str = Field(..., description="调整原因")
    risk_score_change: Optional[float] = Field(None, description="风险评分变化")
    evidence_report_id: Optional[str] = Field(None, description="证据报告ID")

    # Approval
    approved_by: Optional[str] = Field(None, description="审批人")
    approved_at: Optional[datetime] = Field(None)

    created_at: datetime = Field(default_factory=datetime.now)

    class Config:
        use_enum_values = True


class RiskStratification(BaseModel):
    """Chinese population risk stratification."""
    age_bracket: str = Field(..., description="年龄段")
    gender: int = Field(..., description="性别(0=女, 1=男)")
    region: Optional[str] = Field(None, description="地区(城市/农村)")
    province: Optional[str] = Field(None, description="省份")

    # Risk factors
    bmi_category: str = Field(..., description="BMI分级(中国标准)")
    bp_category: str = Field(..., description="血压分级")
    glucose_category: str = Field(..., description="血糖分级")
    lipid_category: str = Field(..., description="血脂分级")
    smoking_status: int = Field(..., description="吸烟状态")
    drinking_status: int = Field(..., description="饮酒状态")

    # Comorbidities
    hypertension: bool = Field(default=False, description="高血压")
    diabetes: bool = Field(default=False, description="糖尿病")
    dyslipidemia: bool = Field(default=False, description="血脂异常")
    comorbidity_count: int = Field(default=0, description="合并症数量")

    # Risk score
    risk_score: float = Field(..., description="风险评分")
    risk_level: str = Field(..., description="风险等级")

    class Config:
        use_enum_values = True
