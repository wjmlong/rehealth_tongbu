from __future__ import annotations

from datetime import datetime
from enum import Enum
from math import isfinite
from typing import Any

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator


class FeatureSource(str, Enum):
    REAL_DEVICE = "REAL_DEVICE"
    USER_REPORTED = "USER_REPORTED"
    CLINICAL_REPORT = "CLINICAL_REPORT"
    DERIVED = "DERIVED"
    UNKNOWN = "UNKNOWN"


class FeatureQualityStatus(str, Enum):
    VALID = "VALID"
    MISSING = "MISSING"
    STALE = "STALE"
    LOW_CONFIDENCE = "LOW_CONFIDENCE"


class RiskLevel(str, Enum):
    LOW = "low"
    MODERATE = "moderate"
    HIGH = "high"
    VERY_HIGH = "very_high"


class FeatureQuality(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    status: FeatureQualityStatus
    source: FeatureSource
    observed_at: int | None = Field(default=None, alias="observedAt")
    reason: str

    @model_validator(mode="before")
    @classmethod
    def normalize_observed_at(cls, data: Any) -> Any:
        if isinstance(data, dict) and "observedAt" in data and "observed_at" not in data:
            data = dict(data)
            data["observed_at"] = data.pop("observedAt")
        return data


class CvdFeatureVector(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    age: int | None = None
    gender: int | None = None
    bmi: float | None = None
    sbp: float | None = None
    dbp: float | None = None
    fasting_glucose: float | None = None
    total_cholesterol: float | None = None
    ldl: float | None = None
    hdl: float | None = None
    triglycerides: float | None = None
    exercise_days: int | None = None
    smoking: int | None = None
    drinking: int | None = None
    diabetes_history: int | None = None
    hypertension_history: int | None = None
    family_history: int | None = None
    feature_quality: dict[str, FeatureQuality] = Field(default_factory=dict, alias="featureQuality")

    @model_validator(mode="before")
    @classmethod
    def normalize_android_names(cls, data: Any) -> Any:
        if not isinstance(data, dict):
            return data
        normalized = dict(data)
        aliases = {
            "fastingGlucose": "fasting_glucose",
            "totalCholesterol": "total_cholesterol",
            "exerciseDays": "exercise_days",
            "diabetesHistory": "diabetes_history",
            "hypertensionHistory": "hypertension_history",
            "familyHistory": "family_history",
            "featureQuality": "feature_quality",
        }
        for src, dst in aliases.items():
            if src in normalized and dst not in normalized:
                normalized[dst] = normalized.pop(src)
        return normalized

    @model_validator(mode="after")
    def require_quality_entries(self) -> "CvdFeatureVector":
        missing_quality = [field for field in FEATURE_FIELDS if field not in self.feature_quality]
        if missing_quality:
            raise ValueError(f"feature_quality missing entries for: {', '.join(missing_quality)}")
        return self

    @field_validator("age")
    @classmethod
    def validate_age(cls, value: int | None) -> int | None:
        if value is not None and not 18 <= value <= 120:
            raise ValueError("age must be between 18 and 120")
        return value

    @field_validator("bmi")
    @classmethod
    def validate_bmi(cls, value: float | None) -> float | None:
        if value is not None and not 10 <= value <= 80:
            raise ValueError("bmi must be between 10 and 80")
        return value

    @model_validator(mode="after")
    def validate_blood_pressure_pair(self) -> "CvdFeatureVector":
        if self.sbp is not None and not 70 <= self.sbp <= 250:
            raise ValueError("sbp must be between 70 and 250")
        if self.dbp is not None and not 40 <= self.dbp <= 150:
            raise ValueError("dbp must be between 40 and 150")
        if self.sbp is not None and self.dbp is not None and self.sbp <= self.dbp:
            raise ValueError("sbp must be greater than dbp")
        return self

    @field_validator(
        "fasting_glucose",
        "total_cholesterol",
        "ldl",
        "hdl",
        "triglycerides",
    )
    @classmethod
    def validate_lab_value(cls, value: float | None) -> float | None:
        if value is not None and (not isfinite(value) or not 0 < value <= 1000):
            raise ValueError("lab values must be finite positive numbers up to 1000")
        return value

    @field_validator("exercise_days")
    @classmethod
    def validate_exercise_days(cls, value: int | None) -> int | None:
        if value is not None and not 0 <= value <= 7:
            raise ValueError("exercise_days must be between 0 and 7")
        return value

    @field_validator("gender", "smoking", "drinking", "diabetes_history", "hypertension_history", "family_history")
    @classmethod
    def validate_binary_fields(cls, value: int | None) -> int | None:
        if value is not None and value not in {0, 1}:
            raise ValueError("binary fields must be 0 or 1")
        return value

    @field_validator(
        "age",
        "gender",
        "exercise_days",
        "smoking",
        "drinking",
        "diabetes_history",
        "hypertension_history",
        "family_history",
    )
    @classmethod
    def validate_int_fields(cls, value: int | None) -> int | None:
        if value is None:
            return None
        return int(value)

    def missing_fields(self) -> list[str]:
        return [field for field in FEATURE_FIELDS if getattr(self, field) is None]


class RiskEvaluateRequest(BaseModel):
    feature_vector: CvdFeatureVector = Field(alias="featureVector")
    request_id: str | None = Field(default=None, alias="requestId")

    model_config = ConfigDict(populate_by_name=True)

    @model_validator(mode="before")
    @classmethod
    def allow_flat_feature_vector(cls, data: Any) -> Any:
        if not isinstance(data, dict):
            return data
        if "featureVector" in data or "feature_vector" in data:
            return data
        return {"featureVector": data}


class ModelTrace(BaseModel):
    feature_schema_version: str
    model_version: str
    artifact_name: str | None = None
    scorer_mode: str
    fallback_reason: str | None = None
    request_id: str | None = None


class RiskEvaluateResponse(BaseModel):
    risk_score: float = Field(ge=0.0, le=1.0)
    risk_level: RiskLevel
    feature_contributions: dict[str, float]
    model_version: str
    is_mock: bool
    missing_fields: list[str]
    quality_warnings: list[str]
    summary: str
    request_id: str | None = None
    contribution_method: str | None = None
    base_value: float | None = None
    model_trace: ModelTrace | None = None


class InterventionGenerateRequest(BaseModel):
    risk_result: RiskEvaluateResponse = Field(alias="riskResult")
    feature_vector: CvdFeatureVector | None = Field(default=None, alias="featureVector")
    patient_context: dict[str, Any] = Field(default_factory=dict, alias="patientContext")

    model_config = ConfigDict(populate_by_name=True)


class InterventionGenerateResponse(BaseModel):
    plan_id: str
    generated_at: datetime
    priority_intervention: str
    rationale: str
    expected_impact: str
    contraindications: list[str]
    confidence: float = Field(ge=0.0, le=1.0)
    model_version: str
    is_mock: bool
    medical_disclaimer: str


class AttributionEvent(BaseModel):
    date: str
    risk_score: float
    intervention_id: str | None = None
    adherence: float | None = None


class IndividualAttributionRequest(BaseModel):
    events: list[AttributionEvent]
    baseline_risk_score: float | None = Field(default=None, alias="baselineRiskScore")

    model_config = ConfigDict(populate_by_name=True)


class IndividualAttributionResponse(BaseModel):
    model_version: str
    trend_delta: float | None
    adherence_average: float | None
    interpretation: str


class HealthResponse(BaseModel):
    status: str
    service: str
    model_version: str
    model_registry_version: str
    feature_schema_version: str
    scorer_mode: str
    model_available: bool
    model_unavailable_reason: str | None = None
    expected_model_artifacts: list[str] = Field(default_factory=list)
    supported_model_artifact_aliases: list[str] = Field(default_factory=list)
    expected_feature_order_artifacts: list[str] = Field(default_factory=list)
    loaded_artifact_name: str | None = None


FEATURE_FIELDS = [
    "age",
    "gender",
    "bmi",
    "sbp",
    "dbp",
    "fasting_glucose",
    "total_cholesterol",
    "ldl",
    "hdl",
    "triglycerides",
    "exercise_days",
    "smoking",
    "drinking",
    "diabetes_history",
    "hypertension_history",
    "family_history",
]
