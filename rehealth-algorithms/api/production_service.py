from __future__ import annotations

import hashlib
from collections import OrderedDict
from threading import Lock

from healthagent.pias.attribution import IndividualAttributor

from api.production_auth import PiasApiError
from api.production_schemas import (
    CurrentState,
    Forecast,
    ForecastRaw,
    ForecastSummary,
    IndividualAttributionRequest,
    IndividualAttributionResult,
    InterventionEffect,
    Reports,
    SuccessEnvelope,
    UserReport,
)


class IndividualAttributionService:
    def __init__(self, engine_version: str, idempotency_capacity: int) -> None:
        self._engine_version = engine_version
        self._capacity = idempotency_capacity
        self._cache: OrderedDict[str, tuple[str, SuccessEnvelope]] = OrderedDict()
        self._lock = Lock()

    def attribute(
        self,
        request: IndividualAttributionRequest,
        request_id: str,
        idempotency_key: str,
    ) -> SuccessEnvelope:
        fingerprint = hashlib.sha256(request.model_dump_json(by_alias=True).encode()).hexdigest()
        with self._lock:
            cached = self._cache.get(idempotency_key)
            if cached is not None:
                cached_fingerprint, envelope = cached
                if cached_fingerprint != fingerprint:
                    raise PiasApiError(
                        409,
                        "PIAS_IDEMPOTENCY_CONFLICT",
                        "idempotency key was already used for a different request",
                        request_id,
                    )
                return envelope

        engine = IndividualAttributor({"forecast_days": request.forecast_days})
        engine_history = [
            {
                "date": point.date.isoformat(),
                "Y": point.risk_score,
                "Z": point.intervention,
            }
            for point in request.risk_history
        ]
        result = engine.attribute(engine_history)
        raw = ForecastRaw(
            dates=tuple(f"Day {index + 1}" for index in range(len(result.forecast_status_quo))),
            no_action=tuple(result.forecast_status_quo),
            with_plan=tuple(result.forecast_with_plan),
            ci_upper=tuple(result.forecast_ci_upper),
            ci_lower=tuple(result.forecast_ci_lower),
        )
        report = result.report_text
        user_report = UserReport(
            headline=str(report.get("headline", "")),
            body=str(report.get("body", "")),
            advice=str(report.get("advice", "")),
        )
        att_available = result.individual_att is not None
        envelope = SuccessEnvelope(
            request_id=request_id,
            result=IndividualAttributionResult(
                status=result.status,
                history_days=result.history_days,
                min_history_days=result.min_history_days,
                intervention_days=result.intervention_days,
                intervention_data_sufficient=result.intervention_data_sufficient,
                current_state=CurrentState(
                    risk_score=result.current_risk_score,
                    risk_level=result.risk_level,
                    trend=result.trend_direction,
                ),
                forecast=Forecast(
                    raw=raw,
                    summary=ForecastSummary(
                        **{
                            "30d_no_action": result.projected_risk_30d_no_action,
                            "30d_with_plan": result.projected_risk_30d_with_plan,
                            "risk_reduction": result.risk_reduction_30d,
                        }
                    ),
                ),
                intervention_effect=InterventionEffect(
                    individual_att=result.individual_att,
                    att_ci_lower=result.att_ci_lower,
                    att_ci_upper=result.att_ci_upper,
                    att_p_value=result.att_p_value,
                    att_significant=result.att_significant,
                    att_available=att_available,
                    att_unavailable_reason=None if att_available else "intervention_data_insufficient",
                    intervention_days=result.intervention_days,
                    intervention_data_sufficient=result.intervention_data_sufficient,
                ),
                reports=Reports(user=user_report),
                model_version=self._engine_version,
            ),
        )
        with self._lock:
            self._cache[idempotency_key] = (fingerprint, envelope)
            self._cache.move_to_end(idempotency_key)
            while len(self._cache) > self._capacity:
                self._cache.popitem(last=False)
        return envelope
