from __future__ import annotations

from datetime import date
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field


class DemoRiskPoint(BaseModel):
    model_config = ConfigDict(populate_by_name=True, frozen=True)

    date: date
    risk_score: float = Field(alias="Y", ge=0.0, le=1.0)
    intervention: int = Field(alias="Z", ge=0, le=1)


class DemoAttributionRequest(BaseModel):
    model_config = ConfigDict(frozen=True)

    risk_history: tuple[DemoRiskPoint, ...] = ()
    forecast_days: int = Field(default=30, ge=1, le=90)
    language: Literal["zh", "en"] = "zh"


class DemoCurrentState(BaseModel):
    risk_score: float
    risk_level: str
    trend: str


class DemoForecastRaw(BaseModel):
    dates: tuple[str, ...]
    no_action: tuple[float, ...]
    with_plan: tuple[float, ...]
    ci_upper: tuple[float, ...]
    ci_lower: tuple[float, ...]


class DemoForecastSummary(BaseModel):
    d30_no_action: float = Field(alias="30d_no_action")
    d30_with_plan: float = Field(alias="30d_with_plan")
    risk_reduction: float


class DemoForecast(BaseModel):
    raw: DemoForecastRaw
    summary: DemoForecastSummary


class DemoInterventionEffect(BaseModel):
    individual_att: float | None
    att_ci_lower: float | None = None
    att_ci_upper: float | None = None
    att_p_value: float | None = None
    att_significant: bool | None = None
    att_available: bool
    att_unavailable_reason: str | None
    intervention_days: int
    intervention_data_sufficient: bool


class DemoUserReport(BaseModel):
    headline: str
    body: str
    advice: str


class DemoReports(BaseModel):
    user: DemoUserReport


class DemoAttributionResponse(BaseModel):
    status: Literal["accumulating", "ready"]
    history_days: int
    min_history_days: int
    intervention_days: int
    intervention_data_sufficient: bool
    current_state: DemoCurrentState
    forecast: DemoForecast
    intervention_effect: DemoInterventionEffect
    reports: DemoReports
    attribution_mode: Literal["demo_mock"] = "demo_mock"
    is_mock: Literal[True] = True
    provider: Literal["model-service"] = "model-service"
    model_version: str
    trend_delta: float | None
    adherence_average: float | None
    interpretation: str
