from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession

from bodyup_cloud.app.auth import require_role
from bodyup_cloud.db.database import get_db
from bodyup_cloud.db.models import User
from bodyup_cloud.db.schemas import (
    InterventionRequest,
    PrescriptionRequest,
    RiskScoreRequest,
    RiskScoreResponse,
)
from bodyup_cloud.engine.intervention_attributor import InterventionAttributor
from bodyup_cloud.app.dependencies import get_model_registry, get_prescription_generator

router = APIRouter(prefix="/inference", tags=["inference"])


@router.post("/score", response_model=RiskScoreResponse)
async def score_risk(
    req: RiskScoreRequest,
    registry=Depends(get_model_registry),
    _user: User = Depends(require_role("patient", "doctor", "admin")),
):
    if not registry.list_models():
        raise HTTPException(503, "Risk scoring model not loaded. Contact administrator.")
    scorer = registry.get("default")
    result = scorer.predict(req.model_dump())
    return result


@router.post("/prescription")
async def generate_prescription(
    req: PrescriptionRequest,
    registry=Depends(get_model_registry),
    gen=Depends(get_prescription_generator),
    _user: User = Depends(require_role("patient", "doctor", "admin")),
):
    if gen is None:
        raise HTTPException(503, "Prescription generator unavailable. LLM API key may be missing.")
    if not req.risk_result:
        if not registry.list_models():
            raise HTTPException(503, "Risk scoring model not loaded. Contact administrator.")
        scorer = registry.get("default")
        snap = req.memory_snapshot or {}
        missing = [c for c in scorer.FEATURE_COLS if c not in snap]
        if missing:
            raise HTTPException(400, f"memory_snapshot missing required features: {missing}")
        features = {c: snap[c] for c in scorer.FEATURE_COLS}
        risk_result = scorer.predict(features)
    else:
        risk_result = req.risk_result
    prescription = gen.generate(risk_result, req.memory_snapshot)
    return {"prescription": prescription}


@router.post("/attribution")
async def intervention_attribution(
    req: InterventionRequest,
    registry=Depends(get_model_registry),
    _user: User = Depends(require_role("patient", "doctor", "admin")),
):
    if not registry.list_models():
        raise HTTPException(503, "Risk scoring model not loaded. Contact administrator.")
    scorer = registry.get("default")
    attributor = InterventionAttributor(scorer.model)
    report = attributor.attribute(req.t0_features.model_dump(), req.t1_features.model_dump())
    return report.to_dict()
