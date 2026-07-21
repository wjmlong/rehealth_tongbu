"""
Async repository layer -- thin CRUD wrappers over SQLAlchemy async sessions.
"""

from __future__ import annotations

import json
from datetime import date, datetime, timezone

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession

from bodyup_cloud.db.models import (
    DesensitizedUpload,
    DoctorPatient,
    HealthDataPoint,
    LLMConfig,
    ModelConfig,
    Notification,
    RiskHistory,
    User,
)
from bodyup_cloud.db.schemas import HealthDataIn, LLMConfigUpdate


# ---------------------------------------------------------------------------
# Users
# ---------------------------------------------------------------------------

class UserRepository:
    def __init__(self, session: AsyncSession) -> None:
        self.session = session

    async def create(self, user_create, hashed_pw: str) -> User:
        """Persist a new user and return the ORM instance."""
        user = User(
            email=user_create.email,
            hashed_password=hashed_pw,
            full_name=user_create.full_name,
            role=user_create.role,
        )
        self.session.add(user)
        await self.session.flush()
        await self.session.refresh(user)
        return user

    async def get_by_email(self, email: str) -> User | None:
        stmt = select(User).where(User.email == email)
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def get_by_id(self, user_id: int) -> User | None:
        stmt = select(User).where(User.id == user_id)
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def get_patients_for_doctor(self, doctor_id: int) -> list[User]:
        stmt = (
            select(User)
            .join(DoctorPatient, DoctorPatient.patient_id == User.id)
            .where(DoctorPatient.doctor_id == doctor_id)
        )
        result = await self.session.execute(stmt)
        return list(result.scalars().all())

    async def assign_patient_to_doctor(self, doctor_id: int, patient_id: int) -> DoctorPatient:
        link = DoctorPatient(doctor_id=doctor_id, patient_id=patient_id)
        self.session.add(link)
        await self.session.flush()
        return link

    async def list_all(self) -> list[User]:
        result = await self.session.execute(select(User))
        return list(result.scalars().all())

    async def count_by_role(self, role: str) -> int:
        stmt = select(func.count()).select_from(User).where(User.role == role)
        result = await self.session.execute(stmt)
        return result.scalar_one()

    async def get_doctors_for_patient(self, patient_id: int) -> list[User]:
        stmt = (
            select(User)
            .join(DoctorPatient, DoctorPatient.doctor_id == User.id)
            .where(DoctorPatient.patient_id == patient_id)
        )
        result = await self.session.execute(stmt)
        return list(result.scalars().all())


# ---------------------------------------------------------------------------
# Health data & risk
# ---------------------------------------------------------------------------

class HealthRepository:
    def __init__(self, session: AsyncSession) -> None:
        self.session = session

    async def save_health_data(self, patient_id: int, data: HealthDataIn) -> HealthDataPoint:
        point = HealthDataPoint(
            patient_id=patient_id,
            device_id=data.device_id,
            data_type=data.data_type,
            value=data.value,
            recorded_at=data.recorded_at or datetime.now(timezone.utc),
        )
        self.session.add(point)
        await self.session.flush()
        await self.session.refresh(point)
        return point

    async def get_realtime_data(
        self,
        patient_id: int,
        data_type: str | None = None,
        limit: int = 100,
    ) -> list[HealthDataPoint]:
        stmt = (
            select(HealthDataPoint)
            .where(HealthDataPoint.patient_id == patient_id)
            .order_by(HealthDataPoint.recorded_at.desc())
            .limit(limit)
        )
        if data_type is not None:
            stmt = stmt.where(HealthDataPoint.data_type == data_type)
        result = await self.session.execute(stmt)
        return list(result.scalars().all())

    async def save_risk_history(
        self,
        device_id: str,
        patient_id: int | None,
        date_str: str,
        Y: float,
        Z: int,
    ) -> RiskHistory:
        record = RiskHistory(
            device_id=device_id,
            patient_id=patient_id,
            date=date_str,
            risk_score=Y,
            intervention=Z,
        )
        self.session.add(record)
        await self.session.flush()
        await self.session.refresh(record)
        return record

    async def get_risk_history(
        self,
        device_id: str | None = None,
        patient_id: int | None = None,
        limit: int = 365,
    ) -> list[RiskHistory]:
        stmt = select(RiskHistory).order_by(RiskHistory.date.desc()).limit(limit)
        if device_id is not None:
            stmt = stmt.where(RiskHistory.device_id == device_id)
        if patient_id is not None:
            stmt = stmt.where(RiskHistory.patient_id == patient_id)
        result = await self.session.execute(stmt)
        return list(result.scalars().all())

    async def save_desensitized(self, device_id: str, payload: dict) -> DesensitizedUpload:
        upload = DesensitizedUpload(
            device_id=device_id,
            payload=json.dumps(payload, ensure_ascii=False),
        )
        self.session.add(upload)
        await self.session.flush()
        await self.session.refresh(upload)
        return upload

    async def count_scores_today(self) -> int:
        today_str = date.today().isoformat()
        stmt = (
            select(func.count())
            .select_from(RiskHistory)
            .where(RiskHistory.date == today_str)
        )
        result = await self.session.execute(stmt)
        return result.scalar_one()

    async def get_all_desensitized(self, limit: int = 10000) -> list[DesensitizedUpload]:
        stmt = (
            select(DesensitizedUpload)
            .order_by(DesensitizedUpload.created_at.desc())
            .limit(limit)
        )
        result = await self.session.execute(stmt)
        return list(result.scalars().all())


# ---------------------------------------------------------------------------
# Notifications
# ---------------------------------------------------------------------------

class NotificationRepository:
    def __init__(self, session: AsyncSession) -> None:
        self.session = session

    async def create(
        self,
        from_id: int | None,
        to_id: int,
        type: str,
        title: str,
        content: str,
    ) -> Notification:
        notif = Notification(
            from_user_id=from_id,
            to_user_id=to_id,
            type=type,
            title=title,
            content=content,
        )
        self.session.add(notif)
        await self.session.flush()
        await self.session.refresh(notif)
        return notif

    async def get_unread(self, user_id: int) -> list[Notification]:
        stmt = (
            select(Notification)
            .where(Notification.to_user_id == user_id, Notification.is_read == False)  # noqa: E712
            .order_by(Notification.created_at.desc())
        )
        result = await self.session.execute(stmt)
        return list(result.scalars().all())

    async def get_for_user(self, user_id: int, limit: int = 50) -> list[Notification]:
        stmt = (
            select(Notification)
            .where(Notification.to_user_id == user_id)
            .order_by(Notification.created_at.desc())
            .limit(limit)
        )
        result = await self.session.execute(stmt)
        return list(result.scalars().all())

    async def mark_read(self, notification_id: int, user_id: int) -> bool:
        stmt = select(Notification).where(
            Notification.id == notification_id,
            Notification.to_user_id == user_id,
        )
        result = await self.session.execute(stmt)
        notif = result.scalar_one_or_none()
        if notif is None:
            return False
        notif.is_read = True
        await self.session.flush()
        return True

    async def count_unread(self, user_id: int) -> int:
        stmt = (
            select(func.count())
            .select_from(Notification)
            .where(Notification.to_user_id == user_id, Notification.is_read == False)  # noqa: E712
        )
        result = await self.session.execute(stmt)
        return result.scalar_one()


# ---------------------------------------------------------------------------
# Platform config (LLM & model)
# ---------------------------------------------------------------------------

class ConfigRepository:
    def __init__(self, session: AsyncSession) -> None:
        self.session = session

    async def get_active_llm_config(self) -> LLMConfig | None:
        stmt = select(LLMConfig).where(LLMConfig.is_active == True).limit(1)  # noqa: E712
        result = await self.session.execute(stmt)
        return result.scalar_one_or_none()

    async def update_llm_config(self, update: LLMConfigUpdate) -> LLMConfig:
        """Deactivate all existing configs, then insert a new active one."""
        from bodyup_cloud.app.crypto import encrypt_api_key
        from bodyup_cloud.config.settings import settings

        all_stmt = select(LLMConfig).where(LLMConfig.is_active == True)  # noqa: E712
        result = await self.session.execute(all_stmt)
        for cfg in result.scalars().all():
            cfg.is_active = False

        encrypted_key = encrypt_api_key(update.api_key, settings.secret_key) if update.api_key else ""

        new_cfg = LLMConfig(
            provider=update.provider,
            model=update.model,
            api_key_encrypted=encrypted_key,
            is_active=True,
        )
        self.session.add(new_cfg)
        await self.session.flush()
        await self.session.refresh(new_cfg)
        return new_cfg

    async def get_model_configs(self) -> list[ModelConfig]:
        result = await self.session.execute(select(ModelConfig))
        return list(result.scalars().all())

    async def create_model_config(self, **kwargs) -> ModelConfig:
        cfg = ModelConfig(**kwargs)
        self.session.add(cfg)
        await self.session.flush()
        await self.session.refresh(cfg)
        return cfg
