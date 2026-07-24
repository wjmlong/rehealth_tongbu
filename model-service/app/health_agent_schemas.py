from __future__ import annotations

from typing import Literal

from pydantic import BaseModel, ConfigDict, Field


class HealthAgentContext(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    age_band: str | None = Field(default=None, max_length=16)
    risk_level: str | None = Field(default=None, max_length=32)
    risk_score_percent: float | None = Field(default=None, ge=0, le=100)
    recommended_action: str | None = Field(default=None, max_length=240)
    trend: Literal["improving", "stable", "worsening", "unknown"] = "unknown"


class HealthAgentRequest(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    request_id: str = Field(pattern=r"^[A-Za-z0-9._:-]{1,128}$")
    message: str = Field(min_length=1, max_length=1200)
    locale: str = Field(default="zh-CN", pattern=r"^[A-Za-z]{2,3}(?:-[A-Za-z0-9]{2,8})?$")
    context: HealthAgentContext


class HealthAgentResponse(BaseModel):
    model_config = ConfigDict(extra="forbid", frozen=True)

    request_id: str
    status: Literal["ok", "safety_refusal", "provider_unavailable", "demo"]
    answer: str
    medical_disclaimer: str
    provider: Literal["configured", "demo_disabled"]
    model_version: str
    is_demo: bool
    retryable: bool = False
