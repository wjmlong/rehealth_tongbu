"""
Premium Calculator for Chinese Health Insurance Market

Dynamic premium calculation based on risk improvement,
actuarial tables, and wellness incentives.
"""

from typing import Dict, List, Optional, Any
from datetime import date, datetime
import numpy as np

from .models import PolicyType, RiskStratification


class PremiumCalculator:
    """
    Premium calculator for Chinese health insurance.

    Supports dynamic premium adjustment based on risk improvement
    and wellness program participation.
    """

    # Chinese CVD baseline rates by age group
    BASELINE_CVD_RATES = {
        "35-44": 0.05,
        "45-54": 0.10,
        "55-64": 0.20,
        "65-74": 0.35,
        "75+": 0.45,
    }

    # Gender adjustment factors (Chinese population)
    GENDER_FACTORS = {
        0: 1.1,  # Female: slightly higher CVD in Chinese data
        1: 1.0,  # Male: baseline
    }

    # Regional adjustment factors
    REGION_FACTORS = {
        "urban": 1.0,
        "rural": 0.9,  # Lower healthcare access
    }

    # Policy type base rates (annual premium per 10,000 coverage)
    POLICY_BASE_RATES = {
        PolicyType.CRITICAL_ILLNESS: 300,  # 重疾险: 300元/万保额
        PolicyType.MILLION_MEDICAL: 100,  # 百万医疗: 100元/万保额
        PolicyType.TAX_ADVANTAGED: 150,  # 税优健康险: 150元/万保额
        PolicyType.HUIMIN_BAO: 50,  # 惠民保: 50元/万保额
        PolicyType.SPECIFIC_DISEASE: 200,  # 特定疾病: 200元/万保额
        PolicyType.TERM_LIFE: 80,  # 定期寿险: 80元/万保额
    }

    def __init__(self, config: Dict[str, Any] = None):
        config = config or {}
        self.no_claim_discount_rate = config.get("no_claim_discount_rate", 0.1)
        self.max_no_claim_discount = config.get("max_no_claim_discount", 0.3)
        self.wellness_discount_rate = config.get("wellness_discount_rate", 0.05)
        self.max_wellness_discount = config.get("max_wellness_discount", 0.15)

    def calculate_base_premium(
        self,
        policy_type: PolicyType,
        coverage_amount: float,
        risk_stratification: RiskStratification,
    ) -> float:
        """
        Calculate base premium based on policy type and risk.

        Parameters
        ----------
        policy_type : PolicyType
            Insurance policy type
        coverage_amount : float
            Coverage amount in CNY
        risk_stratification : RiskStratification
            Risk stratification result

        Returns
        -------
        float
            Annual premium in CNY
        """
        # Base rate per 10,000 coverage
        base_rate = self.POLICY_BASE_RATES.get(policy_type, 100)

        # Coverage in units of 10,000
        coverage_units = coverage_amount / 10000

        # Age factor
        age_factor = self._age_factor(risk_stratification.age_bracket)

        # Gender factor
        gender_factor = self.GENDER_FACTORS.get(risk_stratification.gender, 1.0)

        # Region factor
        region_factor = self.REGION_FACTORS.get(risk_stratification.region or "urban", 1.0)

        # Risk score factor (normalized to 0.5-2.0 range)
        risk_factor = self._risk_score_factor(risk_stratification.risk_score)

        # Comorbidity factor
        comorbidity_factor = 1 + risk_stratification.comorbidity_count * 0.1

        # Calculate premium
        premium = (
            base_rate
            * coverage_units
            * age_factor
            * gender_factor
            * region_factor
            * risk_factor
            * comorbidity_factor
        )

        return round(premium, 2)

    def calculate_adjusted_premium(
        self,
        base_premium: float,
        risk_improvement: float,
        no_claim_years: int = 0,
        wellness_participation: bool = False,
    ) -> Dict[str, Any]:
        """
        Calculate adjusted premium with discounts.

        Parameters
        ----------
        base_premium : float
            Base annual premium
        risk_improvement : float
            Risk score improvement (positive = better)
        no_claim_years : int
            Number of claim-free years
        wellness_participation : bool
            Whether participating in wellness program

        Returns
        -------
        dict with adjusted_premium and discount_details
        """
        # No-claim discount
        no_claim_discount = min(
            no_claim_years * self.no_claim_discount_rate,
            self.max_no_claim_discount
        )

        # Wellness discount
        wellness_discount = (
            self.wellness_discount_rate if wellness_participation else 0
        )

        # Risk improvement bonus
        risk_improvement_bonus = min(risk_improvement * 0.5, 0.2)  # Max 20%

        # Total discount
        total_discount = no_claim_discount + wellness_discount + risk_improvement_bonus
        total_discount = min(total_discount, 0.5)  # Max 50% discount

        # Adjusted premium
        adjusted_premium = base_premium * (1 - total_discount)

        return {
            "base_premium": base_premium,
            "adjusted_premium": round(adjusted_premium, 2),
            "discount_amount": round(base_premium - adjusted_premium, 2),
            "discount_breakdown": {
                "no_claim_discount": round(no_claim_discount, 4),
                "wellness_discount": round(wellness_discount, 4),
                "risk_improvement_bonus": round(risk_improvement_bonus, 4),
                "total_discount": round(total_discount, 4),
            },
        }

    def calculate_wellness_incentive(
        self,
        risk_reduction: float,
        adherence_rate: float,
        base_incentive: float = 1000,
    ) -> Dict[str, Any]:
        """
        Calculate wellness incentive reward.

        Parameters
        ----------
        risk_reduction : float
            Risk score reduction achieved
        adherence_rate : float
            Intervention adherence rate (0-1)
        base_incentive : float
            Base incentive amount in CNY

        Returns
        -------
        dict with incentive details
        """
        # Risk reduction multiplier
        risk_multiplier = min(risk_reduction * 10, 2.0)  # Max 2x

        # Adherence multiplier
        adherence_multiplier = adherence_rate

        # Calculate incentive
        incentive = base_incentive * risk_multiplier * adherence_multiplier

        # Cap at reasonable amount
        incentive = min(incentive, 5000)  # Max 5000 CNY

        return {
            "base_incentive": base_incentive,
            "risk_reduction": risk_reduction,
            "adherence_rate": adherence_rate,
            "risk_multiplier": round(risk_multiplier, 2),
            "adherence_multiplier": round(adherence_multiplier, 2),
            "final_incentive": round(incentive, 2),
        }

    def _age_factor(self, age_bracket: str) -> float:
        """Calculate age adjustment factor."""
        factors = {
            "35-44": 0.8,
            "45-54": 1.0,
            "55-64": 1.3,
            "65-74": 1.8,
            "75+": 2.5,
        }
        return factors.get(age_bracket, 1.0)

    def _risk_score_factor(self, risk_score: float) -> float:
        """Calculate risk score adjustment factor."""
        # Normalize risk score to factor range 0.5-2.0
        if risk_score < 0.2:
            return 0.5
        elif risk_score < 0.4:
            return 0.8
        elif risk_score < 0.6:
            return 1.0
        elif risk_score < 0.8:
            return 1.5
        else:
            return 2.0

    def generate_premium_schedule(
        self,
        base_premium: float,
        years: int = 10,
        expected_risk_improvement: float = 0.05,
    ) -> List[Dict[str, Any]]:
        """
        Generate premium schedule for multiple years.

        Parameters
        ----------
        base_premium : float
            Initial base premium
        years : int
            Number of years
        expected_risk_improvement : float
            Expected annual risk improvement

        Returns
        -------
        list of dict with yearly premium schedule
        """
        schedule = []
        current_premium = base_premium

        for year in range(1, years + 1):
            # Calculate adjusted premium
            adjustment = self.calculate_adjusted_premium(
                base_premium=base_premium,
                risk_improvement=expected_risk_improvement * year,
                no_claim_years=year,
                wellness_participation=True,
            )

            schedule.append({
                "year": year,
                "base_premium": base_premium,
                "adjusted_premium": adjustment["adjusted_premium"],
                "discount": adjustment["discount_amount"],
                "cumulative_savings": round(
                    base_premium - adjustment["adjusted_premium"], 2
                ),
            })

        return schedule
