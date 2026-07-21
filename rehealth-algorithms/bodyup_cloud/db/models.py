"""
SQLAlchemy ORM models for the BodyUP Cloud platform.
"""

from datetime import datetime
from typing import Optional

from sqlalchemy import Boolean, Float, ForeignKey, Index, Integer, String, Text, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from bodyup_cloud.db.database import Base


# ---------------------------------------------------------------------------
# Users & relationships
# ---------------------------------------------------------------------------

class User(Base):
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    email: Mapped[str] = mapped_column(String(255), unique=True, index=True, nullable=False)
    hashed_password: Mapped[str] = mapped_column(String(255), nullable=False)
    full_name: Mapped[str] = mapped_column(String(255), nullable=False)
    role: Mapped[str] = mapped_column(String(32), nullable=False)  # "doctor", "patient", "admin"
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        default=None, server_default=func.now(), nullable=False,
    )

    # Relationships (doctor side)
    doctor_links: Mapped[list["DoctorPatient"]] = relationship(
        "DoctorPatient",
        foreign_keys="DoctorPatient.doctor_id",
        back_populates="doctor",
        lazy="selectin",
    )
    # Relationships (patient side)
    patient_links: Mapped[list["DoctorPatient"]] = relationship(
        "DoctorPatient",
        foreign_keys="DoctorPatient.patient_id",
        back_populates="patient",
        lazy="selectin",
    )

    medical_records: Mapped[list["MedicalRecord"]] = relationship(
        back_populates="patient", lazy="selectin",
    )
    health_data_points: Mapped[list["HealthDataPoint"]] = relationship(
        back_populates="patient", lazy="selectin",
    )
    notifications_received: Mapped[list["Notification"]] = relationship(
        "Notification",
        foreign_keys="Notification.to_user_id",
        back_populates="to_user",
        lazy="selectin",
    )

    def __repr__(self) -> str:
        return f"<User id={self.id} email={self.email!r} role={self.role!r}>"


class DoctorPatient(Base):
    __tablename__ = "doctor_patients"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    doctor_id: Mapped[int] = mapped_column(ForeignKey("users.id"), nullable=False)
    patient_id: Mapped[int] = mapped_column(ForeignKey("users.id"), nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        default=None, server_default=func.now(), nullable=False,
    )

    doctor: Mapped["User"] = relationship(
        "User", foreign_keys=[doctor_id], back_populates="doctor_links",
    )
    patient: Mapped["User"] = relationship(
        "User", foreign_keys=[patient_id], back_populates="patient_links",
    )


# ---------------------------------------------------------------------------
# Clinical / health data
# ---------------------------------------------------------------------------

class MedicalRecord(Base):
    __tablename__ = "medical_records"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    patient_id: Mapped[int] = mapped_column(ForeignKey("users.id"), index=True, nullable=False)
    record_type: Mapped[str] = mapped_column(String(64), nullable=False)   # "checkup", "prescription", "diagnosis"
    content: Mapped[str] = mapped_column(Text, nullable=False)             # JSON string
    source: Mapped[str] = mapped_column(String(64), nullable=False)        # "manual", "hospital", "device"
    created_at: Mapped[datetime] = mapped_column(
        default=None, server_default=func.now(), nullable=False,
    )

    patient: Mapped["User"] = relationship(back_populates="medical_records")


class HealthDataPoint(Base):
    __tablename__ = "health_data_points"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    patient_id: Mapped[int] = mapped_column(ForeignKey("users.id"), index=True, nullable=False)
    device_id: Mapped[Optional[str]] = mapped_column(String(128), nullable=True)
    data_type: Mapped[str] = mapped_column(String(64), nullable=False)     # "sbp", "dbp", "heart_rate", ...
    value: Mapped[float] = mapped_column(Float, nullable=False)
    recorded_at: Mapped[datetime] = mapped_column(index=True, nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        default=None, server_default=func.now(), nullable=False,
    )

    patient: Mapped["User"] = relationship(back_populates="health_data_points")


class RiskHistory(Base):
    __tablename__ = "risk_history"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    device_id: Mapped[str] = mapped_column(String(128), index=True, nullable=False)
    patient_id: Mapped[Optional[int]] = mapped_column(
        ForeignKey("users.id"), index=True, nullable=True,
    )
    date: Mapped[str] = mapped_column(String(16), nullable=False)          # "2026-05-25"
    risk_score: Mapped[float] = mapped_column(Float, nullable=False)       # Y value
    intervention: Mapped[int] = mapped_column(Integer, nullable=False)     # Z: 1=intervention, 0=not
    created_at: Mapped[datetime] = mapped_column(
        default=None, server_default=func.now(), nullable=False,
    )

    __table_args__ = (
        Index("ix_risk_history_device_date", "device_id", "date"),
    )


class DesensitizedUpload(Base):
    __tablename__ = "desensitized_uploads"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    device_id: Mapped[str] = mapped_column(String(128), index=True, nullable=False)
    payload: Mapped[str] = mapped_column(Text, nullable=False)             # JSON
    created_at: Mapped[datetime] = mapped_column(
        default=None, server_default=func.now(), nullable=False,
    )


# ---------------------------------------------------------------------------
# Notifications & content
# ---------------------------------------------------------------------------

class Notification(Base):
    __tablename__ = "notifications"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    from_user_id: Mapped[Optional[int]] = mapped_column(
        ForeignKey("users.id"), nullable=True,
    )
    to_user_id: Mapped[int] = mapped_column(
        ForeignKey("users.id"), index=True, nullable=False,
    )
    type: Mapped[str] = mapped_column(String(32), nullable=False)          # "alert", "push", "system"
    title: Mapped[str] = mapped_column(String(512), nullable=False)
    content: Mapped[str] = mapped_column(Text, nullable=False)
    is_read: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        default=None, server_default=func.now(), nullable=False,
    )

    to_user: Mapped["User"] = relationship(
        "User", foreign_keys=[to_user_id], back_populates="notifications_received",
    )


class HealthContent(Base):
    __tablename__ = "health_contents"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    title: Mapped[str] = mapped_column(String(512), nullable=False)
    category: Mapped[str] = mapped_column(String(64), nullable=False)      # "product", "tutorial"
    content: Mapped[str] = mapped_column(Text, nullable=False)
    target_conditions: Mapped[str] = mapped_column(Text, nullable=False)   # JSON array
    created_at: Mapped[datetime] = mapped_column(
        default=None, server_default=func.now(), nullable=False,
    )


# ---------------------------------------------------------------------------
# Platform configuration
# ---------------------------------------------------------------------------

class LLMConfig(Base):
    __tablename__ = "llm_configs"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    provider: Mapped[str] = mapped_column(String(64), nullable=False)      # "claude", "openai", "openrouter"
    model: Mapped[str] = mapped_column(String(128), nullable=False)
    api_key_encrypted: Mapped[str] = mapped_column(Text, nullable=False)
    is_active: Mapped[bool] = mapped_column(Boolean, default=True, nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        default=None, server_default=func.now(), nullable=False,
    )


class ModelConfig(Base):
    __tablename__ = "model_configs"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, autoincrement=True)
    name: Mapped[str] = mapped_column(String(128), unique=True, nullable=False)
    path: Mapped[str] = mapped_column(String(512), nullable=False)
    version: Mapped[str] = mapped_column(String(64), nullable=False)
    auc: Mapped[Optional[float]] = mapped_column(Float, nullable=True)
    data_source: Mapped[str] = mapped_column(String(128), nullable=False)
    is_default: Mapped[bool] = mapped_column(Boolean, default=False, nullable=False)
    created_at: Mapped[datetime] = mapped_column(
        default=None, server_default=func.now(), nullable=False,
    )
