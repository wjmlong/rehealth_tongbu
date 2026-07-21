"""
Pydantic v2 schemas for API request / response serialisation.
"""

from datetime import datetime
from typing import Optional

from pydantic import BaseModel, ConfigDict, EmailStr, Field


# ---------------------------------------------------------------------------
# Auth
# ---------------------------------------------------------------------------

class UserCreate(BaseModel):
    email: EmailStr
    password: str = Field(..., min_length=6)
    full_name: str = Field(..., min_length=1)
    role: str = "patient"


class UserLogin(BaseModel):
    email: EmailStr
    password: str


class UserOut(BaseModel):
    id: int
    email: str
    full_name: str
    role: str
    is_active: bool
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)


class Token(BaseModel):
    access_token: str
    token_type: str = "bearer"


# ---------------------------------------------------------------------------
# Risk scoring
# ---------------------------------------------------------------------------

class RiskScoreRequest(BaseModel):
    age: int = Field(..., ge=0, le=150)
    gender: int = Field(..., ge=0, le=1)
    bmi: float = Field(..., gt=0)
    sbp: float = Field(..., gt=0)
    dbp: float = Field(..., gt=0)
    fasting_glucose: float = Field(..., gt=0)
    total_cholesterol: float = Field(..., gt=0)
    ldl: float = Field(..., ge=0)
    hdl: float = Field(..., gt=0)
    triglycerides: float = Field(..., ge=0)
    exercise_days: int = Field(..., ge=0, le=7)
    smoking: int = Field(..., ge=0, le=1)
    drinking: int = Field(..., ge=0, le=1)
    diabetes_history: int = Field(..., ge=0, le=1)
    hypertension_history: int = Field(..., ge=0, le=1)
    family_history: int = Field(..., ge=0, le=1)


class RiskScoreResponse(BaseModel):
    risk_score: float
    risk_level: str
    feature_contributions: dict
    model_version: str


# ---------------------------------------------------------------------------
# Prescription
# ---------------------------------------------------------------------------

class PrescriptionRequest(BaseModel):
    risk_result: dict
    memory_snapshot: dict


class PrescriptionResponse(BaseModel):
    prescription: str


# ---------------------------------------------------------------------------
# Intervention Attribution
# ---------------------------------------------------------------------------

class InterventionRequest(BaseModel):
    t0_features: RiskScoreRequest
    t1_features: RiskScoreRequest


# ---------------------------------------------------------------------------
# Patient
# ---------------------------------------------------------------------------

class HealthDataIn(BaseModel):
    data_type: str
    value: float
    device_id: Optional[str] = None
    recorded_at: Optional[datetime] = None


class AlertRequest(BaseModel):
    message: str = ""
    include_risk_score: bool = True


class NotificationOut(BaseModel):
    id: int
    type: str
    title: str
    content: str
    is_read: bool
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)


# ---------------------------------------------------------------------------
# Doctor
# ---------------------------------------------------------------------------

class PatientSummary(BaseModel):
    id: int
    full_name: str
    latest_risk_score: Optional[float] = None
    alert_count: int


class PushContentRequest(BaseModel):
    title: str
    content: str
    category: str = "tutorial"


# ---------------------------------------------------------------------------
# Admin
# ---------------------------------------------------------------------------

class LLMConfigUpdate(BaseModel):
    provider: str
    model: str
    api_key: str


class LLMConfigOut(BaseModel):
    id: int
    provider: str
    model: str
    is_active: bool

    model_config = ConfigDict(from_attributes=True)


class ModelConfigOut(BaseModel):
    id: int
    name: str
    path: str
    version: str
    auc: Optional[float] = None
    data_source: str
    is_default: bool

    model_config = ConfigDict(from_attributes=True)


class SystemStats(BaseModel):
    total_users: int
    total_patients: int
    total_doctors: int
    total_scores_today: int
    active_devices: int
