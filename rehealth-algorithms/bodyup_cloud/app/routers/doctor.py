from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession

from bodyup_cloud.app.auth import get_current_user, require_role
from bodyup_cloud.db.database import get_db
from bodyup_cloud.db.models import User
from bodyup_cloud.db.repository import UserRepository, HealthRepository, NotificationRepository
from bodyup_cloud.db.schemas import PatientSummary, PushContentRequest

router = APIRouter(prefix="/doctor", tags=["doctor"])


@router.get("/patients")
async def list_patients(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(require_role("doctor")),
):
    user_repo = UserRepository(db)
    health_repo = HealthRepository(db)
    notif_repo = NotificationRepository(db)
    patients = await user_repo.get_patients_for_doctor(current_user.id)

    result = []
    for p in patients:
        history = await health_repo.get_risk_history(patient_id=p.id, limit=1)
        latest_score = history[0].risk_score if history else None
        alert_count = await notif_repo.count_unread(p.id)
        result.append({
            "id": p.id,
            "full_name": p.full_name,
            "email": p.email,
            "latest_risk_score": latest_score,
            "alert_count": alert_count,
        })
    return result


@router.get("/patients/{patient_id}/dashboard")
async def patient_dashboard(
    patient_id: int,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(require_role("doctor")),
):
    user_repo = UserRepository(db)
    health_repo = HealthRepository(db)

    patient = await user_repo.get_by_id(patient_id)
    if not patient:
        raise HTTPException(404, "Patient not found")

    risk_history = await health_repo.get_risk_history(patient_id=patient_id, limit=90)
    recent_data = await health_repo.get_realtime_data(patient_id=patient_id, limit=50)

    return {
        "patient": {"id": patient.id, "full_name": patient.full_name, "email": patient.email},
        "risk_history": [
            {"date": r.date, "risk_score": r.risk_score, "intervention": r.intervention}
            for r in risk_history
        ],
        "recent_health_data": [
            {"data_type": d.data_type, "value": d.value, "recorded_at": str(d.recorded_at)}
            for d in recent_data
        ],
    }


@router.get("/patients/{patient_id}/alerts")
async def patient_alerts(
    patient_id: int,
    db: AsyncSession = Depends(get_db),
    _user: User = Depends(require_role("doctor")),
):
    notif_repo = NotificationRepository(db)
    alerts = await notif_repo.get_for_user(patient_id, limit=20)
    return [
        {"id": a.id, "type": a.type, "title": a.title,
         "content": a.content, "is_read": a.is_read, "created_at": str(a.created_at)}
        for a in alerts if a.type == "alert"
    ]


@router.post("/patients/{patient_id}/push")
async def push_content(
    patient_id: int,
    req: PushContentRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(require_role("doctor")),
):
    notif_repo = NotificationRepository(db)
    notif = await notif_repo.create(
        from_id=current_user.id,
        to_id=patient_id,
        type="push",
        title=req.title,
        content=req.content,
    )
    await db.commit()
    return {"id": notif.id, "status": "sent"}


@router.get("/patients/{patient_id}/risk-history")
async def patient_risk_history(
    patient_id: int,
    limit: int = 365,
    db: AsyncSession = Depends(get_db),
    _user: User = Depends(require_role("doctor")),
):
    health_repo = HealthRepository(db)
    history = await health_repo.get_risk_history(patient_id=patient_id, limit=limit)
    return [
        {"date": r.date, "risk_score": r.risk_score, "intervention": r.intervention}
        for r in history
    ]
