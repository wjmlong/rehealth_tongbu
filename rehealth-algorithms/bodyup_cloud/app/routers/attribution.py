from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession

from bodyup_cloud.app.auth import get_current_user, require_role
from bodyup_cloud.db.database import get_db
from bodyup_cloud.db.models import User
from bodyup_cloud.db.repository import HealthRepository
from bodyup_cloud.engine.individual_prediction import IndividualRiskPredictor
from bodyup_cloud.engine.group_attribution import GroupAttributionEngine
from bodyup_cloud.engine.report_signer import ReportSigner
from bodyup_cloud.app.dependencies import get_signer

router = APIRouter(prefix="/attribution", tags=["attribution"])

_predictor = IndividualRiskPredictor()
_group_engine = GroupAttributionEngine()

BMI_MAP = {"underweight": 0, "normal": 1, "overweight": 2, "obese": 3}
BP_MAP = {"normal": 0, "elevated": 1, "stage1": 2, "stage2": 3, "crisis": 4}
ACTIVITY_MAP = {"sedentary": 0, "light": 1, "moderate": 2, "vigorous": 3}
GENDER_MAP = {"male": 0, "female": 1}


@router.post("/predict/{device_id}")
async def predict_individual(
    device_id: str,
    db: AsyncSession = Depends(get_db),
    _user: User = Depends(require_role("patient", "doctor", "admin")),
):
    repo = HealthRepository(db)
    rows = await repo.get_risk_history(device_id=device_id)
    history = [
        {"date": r.date, "Y": r.risk_score, "Z": r.intervention}
        for r in rows
    ]
    result = _predictor.predict(history)
    return result


@router.post("/group")
async def run_group_attribution(
    disease_type: str = "CVD",
    min_observation_days: int = 30,
    db: AsyncSession = Depends(get_db),
    signer: ReportSigner = Depends(get_signer),
    _user: User = Depends(require_role("admin")),
):
    repo = HealthRepository(db)
    uploads = await repo.get_all_desensitized()
    if not uploads:
        raise HTTPException(400, "No desensitized data available")

    user_records = []
    for upload in uploads:
        import json
        payload = json.loads(upload.payload) if isinstance(upload.payload, str) else upload.payload
        ts = payload.get("attribution_timeseries", [])
        snap = payload.get("memory_snapshot", {})
        if len(ts) < min_observation_days:
            continue
        Y_start = ts[0]["Y"]
        Y_end = ts[-1]["Y"]
        Z = 1 if any(p["Z"] == 1 for p in ts) else 0
        user_records.append({
            "device_id": upload.device_id,
            "Z": Z,
            "delta_Y": Y_end - Y_start,
            "features": {
                "age_bracket": snap.get("age_bracket", 0),
                "bmi_level_encoded": BMI_MAP.get(snap.get("bmi_level", ""), 0),
                "bp_baseline_grade_encoded": BP_MAP.get(snap.get("bp_baseline_grade", ""), 0),
                "activity_level_encoded": ACTIVITY_MAP.get(snap.get("activity_level", ""), 0),
                "gender_encoded": GENDER_MAP.get(snap.get("gender", ""), 0),
                "season_sin": 0.0,
                "season_cos": 1.0,
            },
        })

    result = _group_engine.estimate(user_records)
    if result.get("status") == "success" and signer:
        signed = signer.sign_report(result, device_id="group", disease_type=disease_type)
        return signed
    return result


@router.get("/group/readiness")
async def check_readiness(
    disease_type: str = "CVD",
    db: AsyncSession = Depends(get_db),
    _user: User = Depends(require_role("admin", "doctor")),
):
    repo = HealthRepository(db)
    uploads = await repo.get_all_desensitized()
    n_total = len(uploads)
    return {
        "total_uploads": n_total,
        "min_required": 30,
        "ideal": 100,
        "ready": n_total >= 30,
        "disease_type": disease_type,
    }
