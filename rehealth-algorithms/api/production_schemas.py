from __future__ import annotations

from datetime import date
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field


class RiskHistoryPoint(BaseModel):
    model_config = ConfigDict(populate_by_name=True, frozen=True)

    date: date
    risk_score: float = Field(alias="Y", ge=0.0, le=1.0)
    intervention: int = Field(alias="Z", ge=0, le=1)


class IndividualAttributionRequest(BaseModel):
    model_config = ConfigDict(frozen=True)

    risk_history: tuple[RiskHistoryPoint, ...] = ()
    forecast_days: int = Field(default=30, ge=1, le=90)
    language: Literal["zh", "en"] = "zh"


class CurrentState(BaseModel):
    model_config = ConfigDict(frozen=True)

    risk_score: float
    risk_level: str
    trend: str


class ForecastRaw(BaseModel):
    model_config = ConfigDict(frozen=True)

    dates: tuple[str, ...] = ()
    no_action: tuple[float, ...] = ()
    with_plan: tuple[float, ...] = ()
    ci_upper: tuple[float, ...] = ()
    ci_lower: tuple[float, ...] = ()


class ForecastSummary(BaseModel):
    model_config = ConfigDict(frozen=True)

    d30_no_action: float = Field(alias="30d_no_action")
    d30_with_plan: float = Field(alias="30d_with_plan")
    risk_reduction: float


class Forecast(BaseModel):
    model_config = ConfigDict(frozen=True)

    raw: ForecastRaw
    summary: ForecastSummary


class InterventionEffect(BaseModel):
    model_config = ConfigDict(frozen=True)

    individual_att: float | None
    att_ci_lower: float | None
    att_ci_upper: float | None
    att_p_value: float | None
    att_significant: bool | None
    att_available: bool
    att_unavailable_reason: str | None
    intervention_days: int
    intervention_data_sufficient: bool


class UserReport(BaseModel):
    model_config = ConfigDict(frozen=True)

    headline: str
    body: str
    advice: str


class Reports(BaseModel):
    model_config = ConfigDict(frozen=True)

    user: UserReport


class IndividualAttributionResult(BaseModel):
    model_config = ConfigDict(frozen=True, protected_namespaces=())

    status: Literal["accumulating", "ready"]
    history_days: int
    min_history_days: int
    intervention_days: int
    intervention_data_sufficient: bool
    current_state: CurrentState
    forecast: Forecast
    intervention_effect: InterventionEffect
    reports: Reports
    attribution_mode: Literal["pias"] = "pias"
    is_mock: Literal[False] = False
    provider: Literal["pias"] = "pias"
    model_version: str


class SuccessEnvelope(BaseModel):
    model_config = ConfigDict(frozen=True)

    success: Literal[True] = True
    code: Literal[200] = 200
    message: str = "ok"
    request_id: str
    result: IndividualAttributionResult


class ErrorDetail(BaseModel):
    model_config = ConfigDict(frozen=True)

    code: str
    message: str
    retryable: bool


class ErrorEnvelope(BaseModel):
    model_config = ConfigDict(frozen=True)

    success: Literal[False] = False
    code: int
    request_id: str | None
    error: ErrorDetail


class HealthStatus(BaseModel):
    model_config = ConfigDict(frozen=True)

    status: Literal["ok", "ready"]
    service: Literal["pias"]
    engine_version: str
