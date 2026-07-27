"""
Settlement Engine for Chinese Insurance Market

Generates settlement reports, handles claims, and integrates with PIAS attribution.
"""

import uuid
from datetime import datetime, date, timedelta
from typing import Dict, List, Optional, Any
import numpy as np

from .models import (
    InsurancePolicy,
    SettlementClaim,
    ClaimStatus,
    PolicyType,
)
from .report_schema import (
    SettlementReport,
    ReportHeader,
    InsurerInfo,
    StatisticalMethodology,
    CohortComposition,
    BalanceDiagnostics,
    ATTResult,
    FinancialImpact,
    ComplianceSection,
    DigitalSignature,
    QuarterlyReport,
    IndividualClaimReport,
)


class SettlementEngine:
    """
    Settlement engine for Chinese insurance market.

    Integrates with PIAS attribution to generate settlement reports
    and handle claims processing.
    """

    def __init__(self, config: Dict[str, Any] = None):
        config = config or {}
        self.default_confidence_level = config.get("confidence_level", 0.95)
        self.default_bootstrap_iterations = config.get("bootstrap_iterations", 200)
        self.report_version = config.get("report_version", "1.0")

    def generate_settlement_report(
        self,
        attribution_result: Dict,
        insurer_info: Dict,
        reporting_period: Optional[Dict] = None,
    ) -> SettlementReport:
        """
        Generate settlement report from attribution result.

        Parameters
        ----------
        attribution_result : dict
            Output from GroupAttributor.estimate()
        insurer_info : dict
            Insurance company information
        reporting_period : dict, optional
            {"start": date, "end": date}

        Returns
        -------
        SettlementReport
        """
        # Generate report ID
        report_id = f"RPT-{datetime.now().strftime('%Y%m%d')}-{uuid.uuid4().hex[:8]}"

        # Header
        header = ReportHeader(
            report_id=report_id,
            report_type="settlement",
            report_version=self.report_version,
            reporting_period_start=reporting_period.get("start") if reporting_period else None,
            reporting_period_end=reporting_period.get("end") if reporting_period else None,
        )

        # Insurer info
        insurer = InsurerInfo(**insurer_info)

        # Statistical methodology
        methodology = StatisticalMethodology(
            psm_caliper=str(attribution_result.get("caliper", "auto")),
            rosenbaum_gamma=attribution_result.get("gamma_sensitivity", 1.0),
            sensitivity_interpretation=attribution_result.get(
                "sensitivity_interpretation", ""
            ),
        )

        # Cohort composition
        cohort = CohortComposition(
            n_total=attribution_result.get("n_total_users", 0),
            n_treated=attribution_result.get("n_treated", 0),
            n_control=attribution_result.get("n_control", 0),
            n_matched_pairs=attribution_result.get("n_matched_pairs", 0),
            matching_rate=(
                attribution_result.get("n_matched_pairs", 0) * 2
                / max(attribution_result.get("n_total_users", 1), 1)
            ),
        )

        # Balance diagnostics
        balance_list = []
        balance_data = attribution_result.get("matching_balance", {})
        for feature, stats in balance_data.items():
            balance_list.append(
                BalanceDiagnostics(
                    feature=feature,
                    smd_before=stats.get("smd_before", stats.get("smd", 0)),
                    smd_after=stats.get("smd", 0),
                    balanced=stats.get("balanced", True),
                )
            )

        # ATT result
        att = attribution_result.get("att", 0)
        ci_lower = attribution_result.get("ci_lower", 0)
        ci_upper = attribution_result.get("ci_upper", 0)
        is_significant = attribution_result.get("is_significant", False)

        att_result = ATTResult(
            att_estimate=att,
            ci_lower=ci_lower,
            ci_upper=ci_upper,
            is_significant=is_significant,
            interpretation=self._interpret_att(att, ci_lower, ci_upper, is_significant),
        )

        # Financial impact
        financial = FinancialImpact(
            estimated_claims_avoided=abs(att) * 50000,  # 假设平均理赔5万
            premium_savings=abs(att) * 5000,  # 假设平均保费5千
            per_user_value=abs(att) * 50000 / max(cohort.n_total, 1),
            roi=abs(att) * 50000 / max(abs(att) * 5000, 1) if att != 0 else 0,
        )

        # Conclusion and recommendation
        if is_significant:
            conclusion = "统计显著"
            recommendation = "建议按约定费率结算"
            detail = (
                f"经倾向得分匹配(PSM)与双重稳健估计(DRE)分析，"
                f"干预组相较对照组心血管风险平均额外下降 {abs(att):.1%}，"
                f"95%置信区间为 [{abs(ci_upper):.1%}, {abs(ci_lower):.1%}]，"
                f"效果具有统计显著性(p<0.05)。"
            )
        else:
            conclusion = "未达统计显著"
            recommendation = "建议延长观察周期或扩大样本量"
            detail = (
                f"经PSM+DRE分析，干预组风险变化为 {att:+.1%}，"
                f"95%置信区间 [{ci_lower:+.1%}, {ci_upper:+.1%}] 跨越零值，"
                f"尚未达到统计显著性。"
            )

        method_summary = (
            f"方法：倾向得分匹配(自适应Caliper + KD-tree最近邻, 无放回) + "
            f"双重稳健估计(DRE) + {methodology.bootstrap_iterations}次Bootstrap置信区间 + "
            f"Rosenbaum敏感性分析(Γ={methodology.rosenbaum_gamma:.2f})。"
            f"共纳入 {cohort.n_total} 名用户"
            f"(干预组 {cohort.n_treated} 人, 对照组 {cohort.n_control} 人)，"
            f"成功匹配 {cohort.n_matched_pairs} 对。"
        )

        return SettlementReport(
            header=header,
            insurer_info=insurer,
            methodology=methodology,
            cohort=cohort,
            balance_diagnostics=balance_list,
            att_result=att_result,
            financial_impact=financial,
            conclusion=conclusion,
            recommendation=recommendation,
            detail=detail,
            method_summary=method_summary,
        )

    def generate_quarterly_report(
        self,
        claims: List[Dict],
        insurer_info: Dict,
        quarter: str,
    ) -> QuarterlyReport:
        """
        Generate quarterly settlement report.

        Parameters
        ----------
        claims : list of dict
            List of claim records
        insurer_info : dict
            Insurance company information
        quarter : str
            Quarter string (e.g., "2026Q1")

        Returns
        -------
        QuarterlyReport
        """
        report_id = f"QRPT-{quarter}-{uuid.uuid4().hex[:8]}"

        # Calculate summary statistics
        total_claims = len(claims)
        total_amount = sum(c.get("claim_amount", 0) for c in claims)
        approved_claims = sum(1 for c in claims if c.get("status") == "approved")
        rejected_claims = sum(1 for c in claims if c.get("status") == "rejected")

        risk_before = [c.get("risk_score_before", 0) for c in claims]
        risk_after = [c.get("risk_score_after", 0) for c in claims]

        avg_risk_before = np.mean(risk_before) if risk_before else 0
        avg_risk_after = np.mean(risk_after) if risk_after else 0
        avg_risk_reduction = avg_risk_before - avg_risk_after

        header = ReportHeader(
            report_id=report_id,
            report_type="quarterly",
            report_version=self.report_version,
        )

        return QuarterlyReport(
            header=header,
            insurer_info=InsurerInfo(**insurer_info),
            quarter=quarter,
            total_claims=total_claims,
            total_amount=total_amount,
            approved_claims=approved_claims,
            rejected_claims=rejected_claims,
            avg_risk_score_before=avg_risk_before,
            avg_risk_score_after=avg_risk_after,
            avg_risk_reduction=avg_risk_reduction,
            claims=claims,
        )

    def generate_individual_claim_report(
        self,
        claim_data: Dict,
        attribution_result: Dict,
    ) -> IndividualClaimReport:
        """
        Generate individual claim evidence report.

        Parameters
        ----------
        claim_data : dict
            Claim information
        attribution_result : dict
            Individual attribution result

        Returns
        -------
        IndividualClaimReport
        """
        report_id = f"ICR-{claim_data.get('claim_id', '')}-{uuid.uuid4().hex[:8]}"

        header = ReportHeader(
            report_id=report_id,
            report_type="individual_claim",
            report_version=self.report_version,
        )

        # Calculate individual ATT
        risk_before = claim_data.get("risk_score_before", 0)
        risk_after = claim_data.get("risk_score_after", 0)
        risk_reduction = risk_before - risk_after

        # Determine claim recommendation
        if risk_reduction > 0.1:
            recommendation = "建议全额赔付"
            amount = claim_data.get("claim_amount", 0)
        elif risk_reduction > 0.05:
            recommendation = "建议部分赔付"
            amount = claim_data.get("claim_amount", 0) * 0.7
        else:
            recommendation = "建议进一步审核"
            amount = claim_data.get("claim_amount", 0) * 0.5

        return IndividualClaimReport(
            header=header,
            claim_id=claim_data.get("claim_id", ""),
            policy_id=claim_data.get("policy_id", ""),
            insured_id=claim_data.get("insured_id", ""),
            risk_score_before=risk_before,
            risk_score_after=risk_after,
            risk_reduction=risk_reduction,
            intervention_type=claim_data.get("intervention_type", "lifestyle"),
            intervention_start=claim_data.get("intervention_start", date.today()),
            intervention_end=claim_data.get("intervention_end", date.today()),
            intervention_adherence=claim_data.get("intervention_adherence", 0.8),
            individual_att=attribution_result.get("att", risk_reduction),
            confidence_interval={
                "lower": attribution_result.get("ci_lower", risk_reduction - 0.05),
                "upper": attribution_result.get("ci_upper", risk_reduction + 0.05),
            },
            clinical_markers=claim_data.get("clinical_markers", {}),
            claim_recommendation=recommendation,
            recommended_amount=amount,
        )

    def process_claim(
        self,
        policy: InsurancePolicy,
        claim_data: Dict,
        attribution_result: Dict,
    ) -> SettlementClaim:
        """
        Process a settlement claim.

        Parameters
        ----------
        policy : InsurancePolicy
            Insurance policy
        claim_data : dict
            Claim information
        attribution_result : dict
            Attribution result

        Returns
        -------
        SettlementClaim
        """
        claim_id = f"CLM-{datetime.now().strftime('%Y%m%d')}-{uuid.uuid4().hex[:8]}"

        risk_before = claim_data.get("risk_score_before", 0)
        risk_after = claim_data.get("risk_score_after", 0)
        risk_reduction = risk_before - risk_after

        # Calculate intervention value
        intervention_value = risk_reduction * policy.coverage_amount

        # Determine claim amount based on policy type
        if policy.policy_type == PolicyType.CRITICAL_ILLNESS:
            # 重疾险：确诊即赔
            claim_amount = policy.coverage_amount
        elif policy.policy_type == PolicyType.MILLION_MEDICAL:
            # 百万医疗：按实际费用报销
            claim_amount = min(
                claim_data.get("actual_cost", 0) - policy.deductible,
                policy.coverage_amount
            )
        else:
            # 其他：基于风险降低
            claim_amount = intervention_value

        return SettlementClaim(
            claim_id=claim_id,
            policy_id=policy.policy_id,
            claim_type=claim_data.get("claim_type", "cvd_intervention"),
            claim_amount=claim_amount,
            risk_score_before=risk_before,
            risk_score_after=risk_after,
            risk_reduction=risk_reduction,
            intervention_value=intervention_value,
            attribution_report_id=claim_data.get("attribution_report_id"),
            psm_att=attribution_result.get("att"),
            psm_ci_lower=attribution_result.get("ci_lower"),
            psm_ci_upper=attribution_result.get("ci_upper"),
            rosenbaum_gamma=attribution_result.get("gamma_sensitivity"),
        )

    def _interpret_att(
        self, att: float, ci_lower: float, ci_upper: float, is_significant: bool
    ) -> str:
        """Interpret ATT result."""
        if is_significant:
            if att < 0:
                return f"干预显著降低心血管风险 {abs(att):.1%}，置信区间 [{ci_lower:.1%}, {ci_upper:.1%}]"
            else:
                return f"干预显著增加心血管风险 {att:.1%}，需进一步调查原因"
        else:
            return f"干预效果未达统计显著性，ATT={att:+.1%}，置信区间 [{ci_lower:+.1%}, {ci_upper:+.1%}]"
