"""
Insurance API Routes for Chinese Market

Endpoints for policy management, claims processing,
settlement reports, and premium calculation.
"""

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List, Optional, Dict, Any
from datetime import date, datetime

router = APIRouter()


# ─────────────────────────────────────────────
# Request/Response Models
# ─────────────────────────────────────────────

class PolicyCreateRequest(BaseModel):
    """Create insurance policy request."""
    policyholder_id: str
    insured_id: str
    policy_type: str  # critical_illness, million_medical, etc.
    coverage_amount: float
    premium: float
    start_date: date
    end_date: date
    deductible: float = 0
    waiting_period_days: int = 90


class ClaimSubmitRequest(BaseModel):
    """Submit claim request."""
    policy_id: str
    claim_type: str
    risk_score_before: float
    risk_score_after: float
    actual_cost: Optional[float] = None
    intervention_type: str = "lifestyle"
    intervention_start: date
    intervention_end: date
    intervention_adherence: float = 0.8


class PremiumCalculationRequest(BaseModel):
    """Premium calculation request."""
    policy_type: str
    coverage_amount: float
    age: int
    gender: int
    bmi: float
    sbp: float
    dbp: float
    fasting_glucose: float
    total_cholesterol: float
    ldl: float
    hdl: float
    triglycerides: float
    smoking: int = 0
    drinking: int = 0
    region: str = "urban"


class SettlementReportRequest(BaseModel):
    """Settlement report request."""
    insurer_name: str
    insurer_id: str
    reporting_period_start: date
    reporting_period_end: date
    user_records: List[Dict[str, Any]]


# ─────────────────────────────────────────────
# Global instances (lazy init)
# ─────────────────────────────────────────────

_settlement_engine = None
_premium_calculator = None
_policies = {}  # In-memory storage (replace with database in production)
_claims = {}


def _get_settlement_engine():
    global _settlement_engine
    if _settlement_engine is None:
        from healthagent.pias import SettlementEngine
        _settlement_engine = SettlementEngine()
    return _settlement_engine


def _get_premium_calculator():
    global _premium_calculator
    if _premium_calculator is None:
        from healthagent.pias import PremiumCalculator
        _premium_calculator = PremiumCalculator()
    return _premium_calculator


# ─────────────────────────────────────────────
# Policy Management
# ─────────────────────────────────────────────

@router.post("/policies", tags=["Policy Management"])
def create_policy(req: PolicyCreateRequest):
    """Create insurance policy."""
    import uuid

    policy_id = f"POL-{datetime.now().strftime('%Y%m%d')}-{uuid.uuid4().hex[:8]}"

    policy = {
        "policy_id": policy_id,
        "policyholder_id": req.policyholder_id,
        "insured_id": req.insured_id,
        "policy_type": req.policy_type,
        "coverage_amount": req.coverage_amount,
        "premium": req.premium,
        "start_date": req.start_date.isoformat(),
        "end_date": req.end_date.isoformat(),
        "deductible": req.deductible,
        "waiting_period_days": req.waiting_period_days,
        "is_active": True,
        "created_at": datetime.now().isoformat(),
    }

    _policies[policy_id] = policy

    return {"status": "created", "policy": policy}


@router.get("/policies/{policy_id}", tags=["Policy Management"])
def get_policy(policy_id: str):
    """Get policy details."""
    if policy_id not in _policies:
        raise HTTPException(status_code=404, detail="Policy not found")
    return _policies[policy_id]


@router.get("/policies", tags=["Policy Management"])
def list_policies(insured_id: str = None, policy_type: str = None):
    """List policies."""
    policies = list(_policies.values())

    if insured_id:
        policies = [p for p in policies if p["insured_id"] == insured_id]
    if policy_type:
        policies = [p for p in policies if p["policy_type"] == policy_type]

    return {"policies": policies, "count": len(policies)}


# ─────────────────────────────────────────────
# Claims Processing
# ─────────────────────────────────────────────

@router.post("/claims", tags=["Claims Processing"])
def submit_claim(req: ClaimSubmitRequest):
    """Submit insurance claim."""
    import uuid

    # Get policy
    if req.policy_id not in _policies:
        raise HTTPException(status_code=404, detail="Policy not found")

    policy = _policies[req.policy_id]

    # Calculate risk reduction
    risk_reduction = req.risk_score_before - req.risk_score_after

    # Determine claim amount based on policy type
    if policy["policy_type"] == "critical_illness":
        claim_amount = policy["coverage_amount"]
    elif policy["policy_type"] == "million_medical":
        claim_amount = min(
            (req.actual_cost or 0) - policy["deductible"],
            policy["coverage_amount"]
        )
    else:
        claim_amount = risk_reduction * policy["coverage_amount"]

    claim_id = f"CLM-{datetime.now().strftime('%Y%m%d')}-{uuid.uuid4().hex[:8]}"

    claim = {
        "claim_id": claim_id,
        "policy_id": req.policy_id,
        "claim_type": req.claim_type,
        "claim_amount": claim_amount,
        "risk_score_before": req.risk_score_before,
        "risk_score_after": req.risk_score_after,
        "risk_reduction": risk_reduction,
        "intervention_type": req.intervention_type,
        "intervention_start": req.intervention_start.isoformat(),
        "intervention_end": req.intervention_end.isoformat(),
        "intervention_adherence": req.intervention_adherence,
        "status": "pending",
        "submitted_at": datetime.now().isoformat(),
    }

    _claims[claim_id] = claim

    return {"status": "submitted", "claim": claim}


@router.get("/claims/{claim_id}", tags=["Claims Processing"])
def get_claim(claim_id: str):
    """Get claim details."""
    if claim_id not in _claims:
        raise HTTPException(status_code=404, detail="Claim not found")
    return _claims[claim_id]


@router.post("/claims/{claim_id}/approve", tags=["Claims Processing"])
def approve_claim(claim_id: str, reviewer_id: str, notes: str = ""):
    """Approve claim."""
    if claim_id not in _claims:
        raise HTTPException(status_code=404, detail="Claim not found")

    claim = _claims[claim_id]
    claim["status"] = "approved"
    claim["reviewer_id"] = reviewer_id
    claim["review_notes"] = notes
    claim["reviewed_at"] = datetime.now().isoformat()

    return {"status": "approved", "claim": claim}


# ─────────────────────────────────────────────
# Premium Calculation
# ─────────────────────────────────────────────

@router.post("/premium/calculate", tags=["Premium Calculation"])
def calculate_premium(req: PremiumCalculationRequest):
    """Calculate insurance premium."""
    from healthagent.pias import PremiumCalculator, RiskStratification

    calculator = _get_premium_calculator()

    # Create risk stratification
    risk = RiskStratification(
        age_bracket=_get_age_bracket(req.age),
        gender=req.gender,
        region=req.region,
        bmi_category=_get_bmi_category(req.bmi),
        bp_category=_get_bp_category(req.sbp, req.dbp),
        glucose_category=_get_glucose_category(req.fasting_glucose),
        lipid_category=_get_lipid_category(req.total_cholesterol, req.ldl, req.hdl, req.triglycerides),
        smoking_status=req.smoking,
        drinking_status=req.drinking,
        risk_score=0.5,  # Placeholder
        risk_level="moderate",
    )

    # Calculate base premium
    base_premium = calculator.calculate_base_premium(
        policy_type=req.policy_type,
        coverage_amount=req.coverage_amount,
        risk_stratification=risk,
    )

    # Calculate adjusted premium
    adjusted = calculator.calculate_adjusted_premium(
        base_premium=base_premium,
        risk_improvement=0,
        no_claim_years=0,
        wellness_participation=False,
    )

    return {
        "base_premium": base_premium,
        "adjusted_premium": adjusted["adjusted_premium"],
        "discount": adjusted["discount_amount"],
        "risk_stratification": risk.dict(),
    }


@router.post("/premium/schedule", tags=["Premium Calculation"])
def generate_premium_schedule(
    base_premium: float,
    years: int = 10,
    expected_risk_improvement: float = 0.05,
):
    """Generate premium schedule for multiple years."""
    calculator = _get_premium_calculator()

    schedule = calculator.generate_premium_schedule(
        base_premium=base_premium,
        years=years,
        expected_risk_improvement=expected_risk_improvement,
    )

    return {"schedule": schedule}


# ─────────────────────────────────────────────
# Settlement Reports
# ─────────────────────────────────────────────

@router.post("/settlement/report", tags=["Settlement Reports"])
def generate_settlement_report(req: SettlementReportRequest):
    """Generate settlement report for insurance company."""
    from healthagent.pias import GroupAttributor, SettlementEngine

    # Run attribution
    attributor = GroupAttributor()
    attribution_result = attributor.estimate(req.user_records)

    if attribution_result.get("status") != "success":
        return attribution_result

    # Generate report
    engine = _get_settlement_engine()
    report = engine.generate_settlement_report(
        attribution_result=attribution_result,
        insurer_info={
            "insurer_name": req.insurer_name,
            "insurer_id": req.insurer_id,
        },
        reporting_period={
            "start": req.reporting_period_start,
            "end": req.reporting_period_end,
        },
    )

    return report.dict()


@router.post("/settlement/quarterly", tags=["Settlement Reports"])
def generate_quarterly_report(
    insurer_name: str,
    quarter: str,
    claims: List[Dict[str, Any]],
):
    """Generate quarterly settlement report."""
    engine = _get_settlement_engine()

    report = engine.generate_quarterly_report(
        claims=claims,
        insurer_info={"insurer_name": insurer_name},
        quarter=quarter,
    )

    return report.dict()


# ─────────────────────────────────────────────
# Helper functions
# ─────────────────────────────────────────────

def _get_age_bracket(age: int) -> str:
    """Get age bracket."""
    if age < 45:
        return "35-44"
    elif age < 55:
        return "45-54"
    elif age < 65:
        return "55-64"
    elif age < 75:
        return "65-74"
    else:
        return "75+"


def _get_bmi_category(bmi: float) -> str:
    """Get BMI category (Chinese standards)."""
    if bmi < 18.5:
        return "underweight"
    elif bmi < 24:
        return "normal"
    elif bmi < 28:
        return "overweight"
    else:
        return "obese"


def _get_bp_category(sbp: float, dbp: float) -> str:
    """Get BP category (Chinese 2023 guidelines)."""
    if sbp >= 180 or dbp >= 110:
        return "stage3_hypertension"
    elif sbp >= 160 or dbp >= 100:
        return "stage2_hypertension"
    elif sbp >= 140 or dbp >= 90:
        return "stage1_hypertension"
    elif sbp >= 120 or dbp >= 80:
        return "elevated"
    else:
        return "normal"


def _get_glucose_category(glucose: float) -> str:
    """Get glucose category (Chinese Diabetes Society)."""
    if glucose >= 7.0:
        return "diabetes"
    elif glucose >= 6.1:
        return "impaired_fasting_glucose"
    else:
        return "normal"


def _get_lipid_category(tc: float, ldl: float, hdl: float, tg: float) -> str:
    """Get lipid category."""
    risk_factors = 0
    if tc > 5.2:
        risk_factors += 1
    if ldl > 3.4:
        risk_factors += 1
    if hdl < 1.0:
        risk_factors += 1
    if tg > 1.7:
        risk_factors += 1

    if risk_factors >= 3:
        return "high_risk"
    elif risk_factors >= 2:
        return "moderate_risk"
    elif risk_factors >= 1:
        return "low_risk"
    else:
        return "optimal"
