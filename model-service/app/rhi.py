from __future__ import annotations

from dataclasses import dataclass
from datetime import date
from enum import Enum
from math import isfinite, tanh
from typing import Any

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator

from app.schemas import FeatureQuality, FeatureQualityStatus, FeatureSource, RiskLevel


RHI_SCHEMA_VERSION = "rhi-core32-v2-preview"
RHI_ALGORITHM_VERSION = "rhi-deterministic-preview-2.0.0"
RHI_ALGORITHM_STATUS = "research_preview_not_clinically_validated"


class ProductTier(str, Enum):
    LITE = "lite"
    STANDARD = "standard"
    CLINICAL = "clinical"


class GlycemiaMetric(str, Enum):
    HBA1C_PERCENT = "hba1c_percent"
    FASTING_GLUCOSE_MMOL_L = "fasting_glucose_mmol_l"


class BiologicalSex(str, Enum):
    FEMALE = "female"
    MALE = "male"
    UNSPECIFIED = "unspecified"


class RhiFeatureVector(BaseModel):
    age: int | None = None
    biological_sex: BiologicalSex | None = None
    waist_circumference_cm: float | None = None
    bmi: float | None = None
    sbp_7d_mean: float | None = None
    total_cholesterol: float | None = None
    hdl_c: float | None = None
    ldl_c: float | None = None
    triglycerides: float | None = None
    glycemia_value: float | None = None
    egfr: float | None = None
    nicotine_exposure: int | None = None
    diabetes_status: int | None = None
    antihypertensive_medication: int | None = None
    lipid_lowering_medication: int | None = None
    premature_cvd_family_history: int | None = None
    dbp_7d_mean: float | None = None
    resting_hr_14d_median: float | None = None
    resting_hr_change_28d_pct: float | None = None
    nocturnal_hrv_14d_median: float | None = None
    hrv_change_28d_pct: float | None = None
    cardiorespiratory_fitness_score: float | None = None
    sleep_duration_7d_mean_hours: float | None = None
    sleep_regularity_14d_pct: float | None = None
    sleep_efficiency_14d_pct: float | None = None
    nocturnal_spo2_drop_burden_14d_pct: float | None = None
    steps_7d_mean: float | None = None
    mvpa_minutes_7d: float | None = None
    sedentary_hours_7d_mean: float | None = None
    active_day_regularity_14d_pct: float | None = None
    weight_change_28d_pct: float | None = None
    adherence_composite_28d_pct: float | None = None

    @field_validator("*")
    @classmethod
    def finite_numeric_values(cls, value: Any) -> Any:
        if isinstance(value, float) and not isfinite(value):
            raise ValueError("RHI feature values must be finite")
        return value

    @field_validator("age")
    @classmethod
    def adult_age(cls, value: int | None) -> int | None:
        if value is not None and not 18 <= value <= 120:
            raise ValueError("age must be between 18 and 120")
        return value

    @field_validator("waist_circumference_cm")
    @classmethod
    def plausible_waist(cls, value: float | None) -> float | None:
        if value is not None and not 40 <= value <= 200:
            raise ValueError("waist_circumference_cm must be between 40 and 200")
        return value

    @field_validator("bmi")
    @classmethod
    def plausible_bmi(cls, value: float | None) -> float | None:
        if value is not None and not 10 <= value <= 80:
            raise ValueError("bmi must be between 10 and 80")
        return value

    @model_validator(mode="after")
    def plausible_blood_pressure(self) -> "RhiFeatureVector":
        if self.sbp_7d_mean is not None and not 70 <= self.sbp_7d_mean <= 250:
            raise ValueError("sbp_7d_mean must be between 70 and 250")
        if self.dbp_7d_mean is not None and not 40 <= self.dbp_7d_mean <= 150:
            raise ValueError("dbp_7d_mean must be between 40 and 150")
        if (
            self.sbp_7d_mean is not None
            and self.dbp_7d_mean is not None
            and self.sbp_7d_mean <= self.dbp_7d_mean
        ):
            raise ValueError("sbp_7d_mean must be greater than dbp_7d_mean")
        return self

    @field_validator(
        "total_cholesterol",
        "hdl_c",
        "ldl_c",
        "triglycerides",
        "glycemia_value",
    )
    @classmethod
    def positive_labs(cls, value: float | None) -> float | None:
        if value is not None and not 0 < value <= 1000:
            raise ValueError("laboratory values must be positive and no greater than 1000")
        return value

    @field_validator("egfr")
    @classmethod
    def plausible_egfr(cls, value: float | None) -> float | None:
        if value is not None and not 0 <= value <= 250:
            raise ValueError("egfr must be between 0 and 250")
        return value

    @field_validator("resting_hr_14d_median")
    @classmethod
    def plausible_resting_hr(cls, value: float | None) -> float | None:
        if value is not None and not 25 <= value <= 220:
            raise ValueError("resting_hr_14d_median must be between 25 and 220")
        return value

    @field_validator("nocturnal_hrv_14d_median")
    @classmethod
    def plausible_hrv(cls, value: float | None) -> float | None:
        if value is not None and not 0 <= value <= 1000:
            raise ValueError("nocturnal_hrv_14d_median must be between 0 and 1000")
        return value

    @field_validator("sleep_duration_7d_mean_hours", "sedentary_hours_7d_mean")
    @classmethod
    def hours_per_day(cls, value: float | None) -> float | None:
        if value is not None and not 0 <= value <= 24:
            raise ValueError("daily hour fields must be between 0 and 24")
        return value

    @field_validator("steps_7d_mean")
    @classmethod
    def plausible_steps(cls, value: float | None) -> float | None:
        if value is not None and not 0 <= value <= 100_000:
            raise ValueError("steps_7d_mean must be between 0 and 100000")
        return value

    @field_validator("mvpa_minutes_7d")
    @classmethod
    def plausible_mvpa(cls, value: float | None) -> float | None:
        if value is not None and not 0 <= value <= 10_080:
            raise ValueError("mvpa_minutes_7d must be between 0 and 10080")
        return value

    @field_validator("weight_change_28d_pct")
    @classmethod
    def plausible_weight_change(cls, value: float | None) -> float | None:
        if value is not None and not -50 <= value <= 100:
            raise ValueError("weight_change_28d_pct must be between -50 and 100")
        return value

    @field_validator(
        "nicotine_exposure",
        "diabetes_status",
        "antihypertensive_medication",
        "lipid_lowering_medication",
        "premature_cvd_family_history",
    )
    @classmethod
    def binary_values(cls, value: int | None) -> int | None:
        if value is not None and value not in {0, 1}:
            raise ValueError("binary RHI fields must be 0 or 1")
        return value

    @field_validator(
        "sleep_regularity_14d_pct",
        "sleep_efficiency_14d_pct",
        "nocturnal_spo2_drop_burden_14d_pct",
        "active_day_regularity_14d_pct",
        "adherence_composite_28d_pct",
        "cardiorespiratory_fitness_score",
    )
    @classmethod
    def percentage_values(cls, value: float | None) -> float | None:
        if value is not None and not 0 <= value <= 100:
            raise ValueError("percentage and normalized score fields must be between 0 and 100")
        return value


class PersonalBaseline(BaseModel):
    median: float
    mad: float = Field(ge=0)
    sample_count: int = Field(ge=1)
    window_days: int = Field(default=28, ge=7, le=90)
    device_fingerprint: str | None = None


class RhiHistoryContext(BaseModel):
    available_days: int = Field(default=1, ge=0)
    previous_display_score: float | None = Field(default=None, ge=0, le=100)
    display_score_7d_ago: float | None = Field(default=None, ge=0, le=100)
    display_score_28d_ago: float | None = Field(default=None, ge=0, le=100)


class ClinicalRiskAnchor(BaseModel):
    model: str
    risk_10y: float | None = Field(default=None, ge=0, le=1)
    risk_level: RiskLevel | None = None
    applicable: bool
    last_updated_at: date | None = None
    model_version: str
    reason: str | None = None

    @model_validator(mode="after")
    def applicable_anchor_requires_probability(self) -> "ClinicalRiskAnchor":
        if self.applicable and (self.risk_10y is None or self.risk_level is None):
            raise ValueError("applicable clinical risk requires risk_10y and risk_level")
        return self


class DeviceContext(BaseModel):
    brand: str | None = None
    model: str | None = None
    firmware_version: str | None = None
    algorithm_version: str | None = None
    measurement_method: str | None = None
    signal_quality: float | None = Field(default=None, ge=0, le=1)
    wear_time_hours: float | None = Field(default=None, ge=0, le=24)
    device_change_detected: bool = False


class RhiEvaluateRequest(BaseModel):
    model_config = ConfigDict(populate_by_name=True)

    feature_vector: RhiFeatureVector = Field(alias="featureVector")
    feature_quality: dict[str, FeatureQuality] = Field(alias="featureQuality")
    product_tier: ProductTier = Field(default=ProductTier.LITE, alias="productTier")
    glycemia_metric: GlycemiaMetric | None = Field(default=None, alias="glycemiaMetric")
    personal_baselines: dict[str, PersonalBaseline] = Field(
        default_factory=dict,
        alias="personalBaselines",
    )
    history: RhiHistoryContext = Field(default_factory=RhiHistoryContext)
    clinical_risk: ClinicalRiskAnchor | None = Field(default=None, alias="clinicalRisk")
    device_context: DeviceContext = Field(default_factory=DeviceContext, alias="deviceContext")
    safety_flags: list[str] = Field(default_factory=list, alias="safetyFlags")
    request_id: str | None = Field(default=None, alias="requestId")

    @model_validator(mode="after")
    def quality_covers_all_features(self) -> "RhiEvaluateRequest":
        missing = [name for name in RHI_FEATURE_FIELDS if name not in self.feature_quality]
        if missing:
            raise ValueError(f"feature_quality missing entries for: {', '.join(missing)}")
        if self.feature_vector.glycemia_value is not None and self.glycemia_metric is None:
            raise ValueError("glycemia_metric is required when glycemia_value is present")
        unknown_baselines = set(self.personal_baselines) - set(RHI_FEATURE_FIELDS)
        if unknown_baselines:
            raise ValueError(f"unknown personal baseline fields: {', '.join(sorted(unknown_baselines))}")
        return self


class DynamicHealthIndex(BaseModel):
    score: float = Field(ge=0, le=100)
    raw_score: float = Field(ge=0, le=100)
    delta_7d: float | None = None
    delta_28d: float | None = None
    status: str
    smoothing_alpha: float = Field(ge=0, le=1)


class DomainScores(BaseModel):
    hemodynamic: float | None = Field(default=None, ge=0, le=100)
    activity_fitness: float | None = Field(default=None, ge=0, le=100)
    sleep_recovery: float | None = Field(default=None, ge=0, le=100)
    metabolic_control: float | None = Field(default=None, ge=0, le=100)
    behavior_adherence: float | None = Field(default=None, ge=0, le=100)


class DataConfidence(BaseModel):
    score: float = Field(ge=0, le=1)
    grade: str
    missing_fields: list[str]
    stale_fields: list[str]
    low_confidence_fields: list[str]
    device_change_detected: bool


class RhiDriver(BaseModel):
    feature: str
    effect: float
    direction: str


class RhiEvaluateResponse(BaseModel):
    schema_version: str
    algorithm_version: str
    algorithm_status: str
    product_tier: ProductTier
    clinical_risk: ClinicalRiskAnchor
    dynamic_health_index: DynamicHealthIndex
    domains: DomainScores
    data_confidence: DataConfidence
    top_drivers: list[RhiDriver]
    safety_flags: list[str]
    request_id: str | None = None


@dataclass(frozen=True)
class Indicator:
    domain: str
    points: tuple[tuple[float, float], ...] | None
    lambda_absolute: float
    improvement_direction: int
    minimum_tier: ProductTier


TIER_RANK = {
    ProductTier.LITE: 0,
    ProductTier.STANDARD: 1,
    ProductTier.CLINICAL: 2,
}

DOMAIN_WEIGHTS = {
    "hemodynamic": 0.25,
    "activity_fitness": 0.25,
    "sleep_recovery": 0.20,
    "metabolic_control": 0.20,
    "behavior_adherence": 0.10,
}

# These transparent curves are a research preview. They are deliberately isolated
# from the validated clinical-risk model and must be reviewed against Chinese
# guidance and longitudinal product data before production use.
INDICATORS: dict[str, Indicator] = {
    "sbp_7d_mean": Indicator("hemodynamic", ((90, 65), (110, 100), (120, 95), (130, 75), (140, 50), (160, 20), (180, 0)), 0.8, -1, ProductTier.STANDARD),
    "dbp_7d_mean": Indicator("hemodynamic", ((50, 65), (70, 100), (80, 95), (90, 60), (100, 25), (120, 0)), 0.8, -1, ProductTier.STANDARD),
    "resting_hr_14d_median": Indicator("hemodynamic", ((40, 60), (55, 95), (70, 100), (80, 80), (90, 50), (110, 10)), 0.5, -1, ProductTier.LITE),
    "nocturnal_hrv_14d_median": Indicator("hemodynamic", None, 0.0, 1, ProductTier.LITE),
    "resting_hr_change_28d_pct": Indicator("hemodynamic", ((-15, 100), (-5, 80), (0, 50), (5, 25), (15, 0)), 0.5, -1, ProductTier.LITE),
    "hrv_change_28d_pct": Indicator("hemodynamic", ((-30, 0), (-10, 25), (0, 50), (10, 75), (30, 100)), 0.5, 1, ProductTier.LITE),
    "steps_7d_mean": Indicator("activity_fitness", ((0, 0), (3000, 35), (5000, 55), (7000, 75), (10000, 100)), 0.5, 1, ProductTier.LITE),
    "mvpa_minutes_7d": Indicator("activity_fitness", ((0, 0), (75, 45), (150, 85), (300, 100)), 0.5, 1, ProductTier.LITE),
    "sedentary_hours_7d_mean": Indicator("activity_fitness", ((4, 100), (6, 85), (8, 60), (10, 30), (14, 0)), 0.5, -1, ProductTier.LITE),
    "active_day_regularity_14d_pct": Indicator("activity_fitness", ((0, 0), (40, 40), (70, 75), (90, 100)), 0.5, 1, ProductTier.LITE),
    "cardiorespiratory_fitness_score": Indicator("activity_fitness", ((0, 0), (50, 50), (80, 80), (100, 100)), 0.5, 1, ProductTier.LITE),
    "sleep_duration_7d_mean_hours": Indicator("sleep_recovery", ((3, 0), (5, 35), (6, 70), (7, 100), (9, 100), (10, 70), (12, 20)), 0.5, 0, ProductTier.LITE),
    "sleep_regularity_14d_pct": Indicator("sleep_recovery", ((0, 0), (50, 45), (75, 80), (90, 100)), 0.5, 1, ProductTier.LITE),
    "sleep_efficiency_14d_pct": Indicator("sleep_recovery", ((50, 0), (75, 50), (85, 85), (95, 100)), 0.5, 1, ProductTier.LITE),
    "nocturnal_spo2_drop_burden_14d_pct": Indicator("sleep_recovery", ((0, 100), (2, 90), (5, 65), (10, 30), (20, 0)), 0.5, -1, ProductTier.LITE),
    "bmi": Indicator("metabolic_control", ((15, 20), (18.5, 80), (21, 100), (24, 90), (28, 55), (32, 25), (40, 0)), 0.8, 0, ProductTier.STANDARD),
    "waist_circumference_cm": Indicator("metabolic_control", ((55, 100), (75, 100), (85, 80), (90, 60), (100, 25), (120, 0)), 0.8, -1, ProductTier.STANDARD),
    "weight_change_28d_pct": Indicator("metabolic_control", ((-6, 50), (-3, 90), (-1, 100), (0, 85), (2, 55), (5, 20)), 0.5, 0, ProductTier.STANDARD),
    "glycemia_value": Indicator("metabolic_control", None, 0.8, -1, ProductTier.CLINICAL),
    "ldl_c": Indicator("metabolic_control", ((1, 100), (2.6, 90), (3.4, 65), (4.1, 35), (5, 0)), 0.8, -1, ProductTier.CLINICAL),
    "triglycerides": Indicator("metabolic_control", ((0.5, 100), (1.7, 90), (2.3, 65), (5, 20), (8, 0)), 0.8, -1, ProductTier.CLINICAL),
    "hdl_c": Indicator("metabolic_control", ((0.5, 0), (1, 60), (1.3, 85), (1.6, 100)), 0.8, 1, ProductTier.CLINICAL),
    "nicotine_exposure": Indicator("behavior_adherence", ((0, 100), (1, 0)), 1.0, -1, ProductTier.LITE),
    "adherence_composite_28d_pct": Indicator("behavior_adherence", ((0, 0), (50, 50), (80, 80), (100, 100)), 1.0, 1, ProductTier.LITE),
}

RHI_FEATURE_FIELDS = [
    "age",
    "biological_sex",
    "waist_circumference_cm",
    "bmi",
    "sbp_7d_mean",
    "total_cholesterol",
    "hdl_c",
    "ldl_c",
    "triglycerides",
    "glycemia_value",
    "egfr",
    "nicotine_exposure",
    "diabetes_status",
    "antihypertensive_medication",
    "lipid_lowering_medication",
    "premature_cvd_family_history",
    "dbp_7d_mean",
    "resting_hr_14d_median",
    "resting_hr_change_28d_pct",
    "nocturnal_hrv_14d_median",
    "hrv_change_28d_pct",
    "cardiorespiratory_fitness_score",
    "sleep_duration_7d_mean_hours",
    "sleep_regularity_14d_pct",
    "sleep_efficiency_14d_pct",
    "nocturnal_spo2_drop_burden_14d_pct",
    "steps_7d_mean",
    "mvpa_minutes_7d",
    "sedentary_hours_7d_mean",
    "active_day_regularity_14d_pct",
    "weight_change_28d_pct",
    "adherence_composite_28d_pct",
]


class RhiPreviewEngine:
    smoothing_alpha = 0.25

    def evaluate(self, request: RhiEvaluateRequest) -> RhiEvaluateResponse:
        feature_scores: dict[str, float] = {}
        feature_effects: dict[str, float] = {}
        domain_values: dict[str, list[float]] = {name: [] for name in DOMAIN_WEIGHTS}
        eligible = {
            name: spec
            for name, spec in INDICATORS.items()
            if TIER_RANK[request.product_tier] >= TIER_RANK[spec.minimum_tier]
        }

        for name, spec in eligible.items():
            value = getattr(request.feature_vector, name)
            quality = request.feature_quality[name]
            absolute = self._absolute_score(name, value, request)
            personal = self._personal_score(name, value, spec, request)
            raw_indicator = (
                spec.lambda_absolute * absolute
                + (1.0 - spec.lambda_absolute) * personal
            )
            confidence = self._quality_confidence(quality, request.device_context)
            adjusted = 50.0 + confidence * (raw_indicator - 50.0)
            adjusted = self._bounded(adjusted)
            feature_scores[name] = adjusted
            domain_values[spec.domain].append(adjusted)
            feature_effects[name] = adjusted - 50.0

        domains = {
            name: (round(sum(values) / len(values), 1) if values else None)
            for name, values in domain_values.items()
        }
        applicable_domain_weights = {
            name: weight
            for name, weight in DOMAIN_WEIGHTS.items()
            if domains[name] is not None
        }
        weight_total = sum(applicable_domain_weights.values())
        raw_rhi = sum(
            domains[name] * weight for name, weight in applicable_domain_weights.items()
        ) / weight_total
        confidence = self._data_confidence(request)
        previous = request.history.previous_display_score
        display_rhi = (
            raw_rhi
            if previous is None
            else self.smoothing_alpha * raw_rhi + (1 - self.smoothing_alpha) * previous
        )
        if previous is not None and confidence.missing_fields:
            display_rhi = min(display_rhi, previous)

        drivers = sorted(
            (
                RhiDriver(
                    feature=name,
                    effect=round(effect, 1),
                    direction="improved" if effect > 0 else "needs_attention",
                )
                for name, effect in feature_effects.items()
                if abs(effect) >= 0.05
            ),
            key=lambda item: abs(item.effect),
            reverse=True,
        )[:5]
        clinical_risk = request.clinical_risk or ClinicalRiskAnchor(
            model="not_available",
            applicable=False,
            model_version="none",
            reason="No reviewed clinical risk anchor was supplied.",
        )
        return RhiEvaluateResponse(
            schema_version=RHI_SCHEMA_VERSION,
            algorithm_version=RHI_ALGORITHM_VERSION,
            algorithm_status=RHI_ALGORITHM_STATUS,
            product_tier=request.product_tier,
            clinical_risk=clinical_risk,
            dynamic_health_index=DynamicHealthIndex(
                score=round(self._bounded(display_rhi), 1),
                raw_score=round(self._bounded(raw_rhi), 1),
                delta_7d=self._delta(display_rhi, request.history.display_score_7d_ago),
                delta_28d=self._delta(display_rhi, request.history.display_score_28d_ago),
                status=self._cold_start_status(request.history.available_days),
                smoothing_alpha=self.smoothing_alpha,
            ),
            domains=DomainScores(**domains),
            data_confidence=confidence,
            top_drivers=drivers,
            safety_flags=request.safety_flags,
            request_id=request.request_id,
        )

    def _absolute_score(
        self,
        name: str,
        value: float | int | None,
        request: RhiEvaluateRequest,
    ) -> float:
        if value is None:
            return 50.0
        if name == "glycemia_value":
            if request.glycemia_metric == GlycemiaMetric.HBA1C_PERCENT:
                return self._interpolate(float(value), ((4, 100), (5.6, 95), (6.4, 65), (8, 25), (12, 0)))
            return self._interpolate(float(value), ((3.5, 90), (5.5, 100), (6.1, 75), (7, 45), (11, 0)))
        points = INDICATORS[name].points
        if points is None:
            return 50.0
        return self._interpolate(float(value), points)

    def _personal_score(
        self,
        name: str,
        value: float | int | None,
        spec: Indicator,
        request: RhiEvaluateRequest,
    ) -> float:
        if value is None or spec.improvement_direction == 0:
            return 50.0
        if request.device_context.device_change_detected:
            return 50.0
        baseline = request.personal_baselines.get(name)
        if baseline is None or baseline.sample_count < 7:
            return 50.0
        scale = 1.4826 * baseline.mad + 1e-6
        z = spec.improvement_direction * (float(value) - baseline.median) / scale
        return self._bounded(50.0 + 50.0 * tanh(z / 2.0))

    def _data_confidence(
        self,
        request: RhiEvaluateRequest,
    ) -> DataConfidence:
        expected_fields = self._expected_confidence_fields(request.product_tier)
        confidences = [
            self._quality_confidence(request.feature_quality[name], request.device_context)
            for name in expected_fields
        ]
        score = sum(confidences) / len(confidences) if confidences else 0.0
        missing = [
            name
            for name in expected_fields
            if request.feature_quality[name].status == FeatureQualityStatus.MISSING
        ]
        stale = [
            name
            for name in expected_fields
            if request.feature_quality[name].status == FeatureQualityStatus.STALE
        ]
        low = [
            name
            for name in expected_fields
            if request.feature_quality[name].status == FeatureQualityStatus.LOW_CONFIDENCE
        ]
        return DataConfidence(
            score=round(score, 2),
            grade=self._confidence_grade(score),
            missing_fields=missing,
            stale_fields=stale,
            low_confidence_fields=low,
            device_change_detected=request.device_context.device_change_detected,
        )

    @staticmethod
    def _expected_confidence_fields(tier: ProductTier) -> list[str]:
        if tier == ProductTier.CLINICAL:
            return RHI_FEATURE_FIELDS
        return [
            name
            for name, spec in INDICATORS.items()
            if TIER_RANK[tier] >= TIER_RANK[spec.minimum_tier]
        ]

    @staticmethod
    def _quality_confidence(quality: FeatureQuality, device: DeviceContext) -> float:
        status_factor = {
            FeatureQualityStatus.VALID: 1.0,
            FeatureQualityStatus.LOW_CONFIDENCE: 0.4,
            FeatureQualityStatus.STALE: 0.2,
            FeatureQualityStatus.MISSING: 0.0,
        }[quality.status]
        source_factor = {
            FeatureSource.REAL_DEVICE: 0.95,
            FeatureSource.CLINICAL_REPORT: 0.95,
            FeatureSource.DERIVED: 0.90,
            FeatureSource.USER_REPORTED: 0.80,
            FeatureSource.UNKNOWN: 0.50,
        }[quality.source]
        device_factor = 0.6 if device.device_change_detected and quality.source in {
            FeatureSource.REAL_DEVICE,
            FeatureSource.DERIVED,
        } else 1.0
        signal_factor = device.signal_quality if (
            device.signal_quality is not None
            and quality.source == FeatureSource.REAL_DEVICE
        ) else 1.0
        return status_factor * source_factor * device_factor * signal_factor

    @staticmethod
    def _interpolate(value: float, points: tuple[tuple[float, float], ...]) -> float:
        if value <= points[0][0]:
            return points[0][1]
        if value >= points[-1][0]:
            return points[-1][1]
        for left, right in zip(points, points[1:]):
            if left[0] <= value <= right[0]:
                fraction = (value - left[0]) / (right[0] - left[0])
                return left[1] + fraction * (right[1] - left[1])
        return 50.0

    @staticmethod
    def _bounded(value: float) -> float:
        return max(0.0, min(100.0, value))

    @staticmethod
    def _delta(current: float, previous: float | None) -> float | None:
        return None if previous is None else round(current - previous, 1)

    @staticmethod
    def _cold_start_status(days: int) -> str:
        if days < 7:
            return "provisional"
        if days < 14:
            return "initial"
        if days < 28:
            return "baseline_confirmed"
        return "confirmed"

    @staticmethod
    def _confidence_grade(score: float) -> str:
        if score >= 0.85:
            return "A"
        if score >= 0.70:
            return "B"
        if score >= 0.50:
            return "C"
        return "D"
