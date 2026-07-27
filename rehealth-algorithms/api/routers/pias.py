"""
PIAS Engine API Routes

Predict, Intervene, Attribute, Settle — CVD risk prediction and intervention attribution.
"""

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import List, Optional, Dict
from datetime import date

router = APIRouter()


# ─────────────────────────────────────────────
# Request/Response Models
# ─────────────────────────────────────────────

class PredictRequest(BaseModel):
    """CVD risk prediction request — 16 features."""
    age: int
    gender: int  # 0=female, 1=male
    bmi: float
    sbp: float  # systolic blood pressure
    dbp: float  # diastolic blood pressure
    fasting_glucose: float
    total_cholesterol: float
    ldl: float
    hdl: float
    triglycerides: float
    exercise_days: int  # days per week
    smoking: int  # 0=no, 1=yes
    drinking: int  # 0=no, 1=yes
    diabetes_history: int  # 0=no, 1=yes
    hypertension_history: int  # 0=no, 1=yes
    family_history: int  # 0=no, 1=yes


class PredictResponse(BaseModel):
    risk_score: float
    risk_level: str
    feature_contributions: Dict[str, float]
    model_version: str
    model_auc: float | str


class IndividualPredictRequest(BaseModel):
    """Individual risk trajectory prediction request."""
    risk_history: List[Dict]  # [{"date": "2026-05-01", "Y": 0.52, "Z": 1}, ...]
    forecast_days: int = 30
    decay_factor: float = 0.95


class GroupAttributionRequest(BaseModel):
    """Group attribution request — PSM + DRE."""
    user_records: List[Dict]  # [{"device_id": "xxx", "Z": 1, "delta_Y": -0.05, "features": {...}}, ...]
    n_bootstrap: int = 200
    confidence_level: float = 0.95


# ─────────────────────────────────────────────
# Global model registry (lazy init)
# ─────────────────────────────────────────────

_model_registry = None


def _get_registry():
    global _model_registry
    if _model_registry is None:
        from healthagent.pias import ModelRegistry
        _model_registry = ModelRegistry()
    return _model_registry


def _get_scorer(model_name: str = "default"):
    """Get or load the CVD risk scorer."""
    registry = _get_registry()
    try:
        return registry.get(model_name)
    except KeyError:
        # Try to load default model
        import os
        model_path = os.environ.get(
            "PIAS_MODEL_PATH",
            "models/rehealth_v2_final.pkl"
        )
        model_auc = float(os.environ.get("PIAS_MODEL_AUC", "0.767"))
        if os.path.exists(model_path):
            registry.register("default", model_path, model_auc)
            return registry.get("default")
        raise HTTPException(
            status_code=503,
            detail=f"Model not found. Set PIAS_MODEL_PATH env var or place model at {model_path}"
        )


# ─────────────────────────────────────────────
# P — Predict
# ─────────────────────────────────────────────

@router.post("/predict", response_model=PredictResponse, tags=["P - Predict"])
def predict_risk(req: PredictRequest):
    """
    Predict CVD risk score using CatBoost model with SHAP explanations.

    Returns risk_score (0-1), risk_level, and feature_contributions.
    """
    scorer = _get_scorer()
    features = req.model_dump()
    result = scorer.predict(features)
    return result


@router.post("/predict/batch", tags=["P - Predict"])
def predict_risk_batch(reqs: List[PredictRequest]):
    """Batch prediction for multiple patients."""
    scorer = _get_scorer()
    results = []
    for req in reqs:
        features = req.model_dump()
        results.append(scorer.predict(features))
    return {"predictions": results, "count": len(results)}


# ─────────────────────────────────────────────
# I — Intervene (LLM prescription)
# ─────────────────────────────────────────────

class InterventionRequest(BaseModel):
    """Intervention prescription request."""
    risk_result: Dict  # Output from /predict
    patient_context: Optional[Dict] = None  # Additional context for LLM


@router.post("/intervene", tags=["I - Intervene"])
def generate_intervention(req: InterventionRequest):
    """
    Generate personalized intervention prescription using LLM.

    Requires DEEPSEEK_API_KEY or compatible LLM endpoint.
    """
    try:
        from healthagent.pias.prescription_generator import PrescriptionGenerator
        generator = PrescriptionGenerator()
        prescription = generator.generate(
            risk_result=req.risk_result,
            patient_context=req.patient_context or {}
        )
        return {"prescription": prescription}
    except ImportError:
        # Fallback: rule-based prescription
        risk_level = req.risk_result.get("risk_level", "moderate")
        prescriptions = {
            "low": {
                "summary": "风险较低，保持健康生活方式",
                "recommendations": [
                    "每周至少150分钟中等强度运动",
                    "保持均衡饮食",
                    "每年体检一次"
                ]
            },
            "moderate": {
                "summary": "风险中等，需要积极干预",
                "recommendations": [
                    "每日30分钟快走或慢跑",
                    "控制盐摄入<6g/天",
                    "每3个月复查血脂",
                    "戒烟限酒"
                ]
            },
            "high": {
                "summary": "风险较高，建议就医",
                "recommendations": [
                    "立即预约心内科",
                    "每日监测血压",
                    "严格控制饮食",
                    "遵医嘱用药"
                ]
            },
            "very_high": {
                "summary": "风险极高，立即就医",
                "recommendations": [
                    "紧急就医",
                    "卧床休息",
                    "避免剧烈运动",
                    "准备住院检查"
                ]
            }
        }
        return {"prescription": prescriptions.get(risk_level, prescriptions["moderate"])}


# ─────────────────────────────────────────────
# A — Attribute (Level 1 + Level 2)
# ─────────────────────────────────────────────

@router.post("/attribute/individual", tags=["A - Attribute"])
def attribute_individual(req: IndividualPredictRequest):
    """
    Level 1: Individual risk trajectory prediction with exponential decay model.

    Returns forecast with confidence intervals and retrospective ATT.
    """
    from healthagent.pias import IndividualPredictor
    predictor = IndividualPredictor({
        "forecast_days": req.forecast_days,
        "decay_factor": req.decay_factor,
    })
    result = predictor.predict(req.risk_history)
    return result


@router.post("/attribute/group", tags=["A - Attribute"])
def attribute_group(req: GroupAttributionRequest):
    """
    Level 2: Group attribution using PSM + DRE + Bootstrap.

    Returns ATT with confidence interval and Rosenbaum sensitivity analysis.
    """
    from healthagent.pias import GroupAttributor
    attributor = GroupAttributor({
        "n_bootstrap": req.n_bootstrap,
        "confidence_level": req.confidence_level,
    })
    result = attributor.estimate(req.user_records)
    return result


# ─────────────────────────────────────────────
# S — Settle (Settlement report)
# ─────────────────────────────────────────────

@router.post("/settle", tags=["S - Settle"])
def generate_settlement(req: GroupAttributionRequest):
    """
    Generate settlement report for insurance company.

    Runs PSM + DRE + Rosenbaum sensitivity analysis and returns
    a formatted settlement report.
    """
    from healthagent.pias import GroupAttributor
    attributor = GroupAttributor({
        "n_bootstrap": req.n_bootstrap,
        "confidence_level": req.confidence_level,
    })
    result = attributor.estimate(req.user_records)

    if result.get("status") != "success":
        return result

    # Extract settlement report
    report = result.get("settlement_report", {})
    return {
        "status": "success",
        "settlement_report": report,
        "att": result["att"],
        "ci_lower": result["ci_lower"],
        "ci_upper": result["ci_upper"],
        "is_significant": result["is_significant"],
        "gamma_sensitivity": result["gamma_sensitivity"],
        "sensitivity_interpretation": result["sensitivity_interpretation"],
    }


# ─────────────────────────────────────────────
# Model management
# ─────────────────────────────────────────────

@router.get("/models", tags=["Model Management"])
def list_models():
    """List registered models."""
    registry = _get_registry()
    return {"models": registry.list_models()}


@router.post("/models/register", tags=["Model Management"])
def register_model(name: str, model_path: str, model_auc: Optional[float] = None):
    """Register a new model."""
    registry = _get_registry()
    registry.register(name, model_path, model_auc)
    return {"status": "registered", "name": name}
