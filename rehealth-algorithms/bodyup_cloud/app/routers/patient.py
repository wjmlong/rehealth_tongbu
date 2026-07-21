import json
from datetime import datetime
from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy.ext.asyncio import AsyncSession

from bodyup_cloud.app.auth import get_current_user, require_role
from bodyup_cloud.db.database import get_db
from bodyup_cloud.db.models import User
from bodyup_cloud.db.repository import (
    HealthRepository, NotificationRepository, UserRepository,
)
from bodyup_cloud.db.schemas import HealthDataIn, AlertRequest

router = APIRouter(prefix="/patient", tags=["patient"])


@router.get("/me/records")
async def my_records(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(require_role("patient")),
):
    from bodyup_cloud.db.models import MedicalRecord
    from sqlalchemy import select
    stmt = select(MedicalRecord).where(
        MedicalRecord.patient_id == current_user.id
    ).order_by(MedicalRecord.created_at.desc()).limit(50)
    result = await db.execute(stmt)
    records = result.scalars().all()
    return [
        {"id": r.id, "record_type": r.record_type,
         "content": r.content, "source": r.source,
         "created_at": str(r.created_at)}
        for r in records
    ]


@router.get("/me/realtime")
async def my_realtime_data(
    data_type: str | None = None,
    limit: int = 100,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(require_role("patient")),
):
    repo = HealthRepository(db)
    data = await repo.get_realtime_data(current_user.id, data_type=data_type, limit=limit)
    return [
        {"data_type": d.data_type, "value": d.value,
         "device_id": d.device_id, "recorded_at": str(d.recorded_at)}
        for d in data
    ]


@router.post("/me/upload")
async def upload_desensitized(
    payload: dict,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(require_role("patient")),
):
    repo = HealthRepository(db)
    device_id = payload.get("device_id", f"user-{current_user.id}")
    upload = await repo.save_desensitized(device_id, payload)

    ts = payload.get("attribution_timeseries", [])
    for entry in ts:
        await repo.save_risk_history(
            device_id=device_id,
            patient_id=current_user.id,
            date_str=entry["date"],
            Y=entry["Y"],
            Z=entry["Z"],
        )

    await db.commit()
    return {"status": "uploaded", "device_id": device_id, "timeseries_points": len(ts)}


@router.post("/me/alert")
async def send_alert(
    req: AlertRequest,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(require_role("patient")),
):
    user_repo = UserRepository(db)
    notif_repo = NotificationRepository(db)

    doctors = await user_repo.get_doctors_for_patient(current_user.id)
    if not doctors:
        raise HTTPException(400, "No assigned doctor found")

    risk_info = ""
    if req.include_risk_score:
        health_repo = HealthRepository(db)
        history = await health_repo.get_risk_history(patient_id=current_user.id, limit=1)
        if history:
            risk_info = f" [当前风险分: {history[0].risk_score:.3f}]"

    title = "患者紧急报警"
    content = f"{current_user.full_name} 发送了紧急报警。{req.message}{risk_info}"

    for doc in doctors:
        await notif_repo.create(
            from_id=current_user.id,
            to_id=doc.id,
            type="alert",
            title=title,
            content=content,
        )
    await db.commit()
    return {"status": "alert_sent", "notified_doctors": len(doctors)}


@router.get("/me/prescription")
async def my_prescription(
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(require_role("patient")),
):
    from bodyup_cloud.db.models import Notification
    from sqlalchemy import select
    stmt = select(Notification).where(
        Notification.to_user_id == current_user.id,
        Notification.type == "push",
    ).order_by(Notification.created_at.desc()).limit(5)
    result = await db.execute(stmt)
    notifs = result.scalars().all()
    return [
        {"id": n.id, "title": n.title, "content": n.content, "created_at": str(n.created_at)}
        for n in notifs
    ]


@router.get("/me/notifications")
async def my_notifications(
    limit: int = 50,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(require_role("patient")),
):
    repo = NotificationRepository(db)
    notifs = await repo.get_for_user(current_user.id, limit=limit)
    return [
        {"id": n.id, "type": n.type, "title": n.title,
         "content": n.content, "is_read": n.is_read,
         "created_at": str(n.created_at)}
        for n in notifs
    ]


@router.post("/me/health-data")
async def report_health_data(
    data: HealthDataIn,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(require_role("patient")),
):
    repo = HealthRepository(db)
    point = await repo.save_health_data(current_user.id, data)
    await db.commit()
    return {"id": point.id, "status": "recorded"}
