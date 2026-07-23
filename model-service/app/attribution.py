from __future__ import annotations

from statistics import mean

from app.attribution_schemas import (
    DemoAttributionRequest,
    DemoAttributionResponse,
    DemoCurrentState,
    DemoForecast,
    DemoForecastRaw,
    DemoForecastSummary,
    DemoInterventionEffect,
    DemoReports,
    DemoUserReport,
)
from app.risk_scorer import MOCK_MODEL_VERSION


class IndividualAttributor:
    def evaluate(self, request: DemoAttributionRequest) -> DemoAttributionResponse:
        ordered = sorted(request.risk_history, key=lambda point: point.date)
        history_days = len(ordered)
        current = ordered[-1].risk_score if ordered else 0.0
        baseline = ordered[0].risk_score if ordered else None
        trend_delta = None if baseline is None else current - baseline
        intervention_days = sum(point.intervention for point in ordered)
        adherence_average = mean(point.intervention for point in ordered) if ordered else None
        ready = history_days > 0
        interpretation = (
            "Risk trend decreased during the observed window."
            if trend_delta is not None and trend_delta < 0
            else "Risk trend did not decrease during the observed window."
        )
        projected = tuple(round(max(0.0, current - index * 0.001), 4) for index in range(request.forecast_days))
        with_plan = tuple(round(max(0.0, value - 0.005), 4) for value in projected)
        individual_att = (
            mean(point.risk_score for point in ordered if point.intervention == 1)
            - mean(point.risk_score for point in ordered if point.intervention == 0)
            if {point.intervention for point in ordered} == {0, 1}
            else None
        )
        return DemoAttributionResponse(
            status="ready" if ready else "accumulating",
            history_days=history_days,
            min_history_days=1,
            intervention_days=intervention_days,
            intervention_data_sufficient=ready,
            current_state=DemoCurrentState(
                risk_score=current,
                risk_level="demo",
                trend="improving" if trend_delta is not None and trend_delta < 0 else "stable",
            ),
            forecast=DemoForecast(
                raw=DemoForecastRaw(
                    dates=tuple(f"Day {index + 1}" for index in range(request.forecast_days)) if ready else (),
                    no_action=projected if ready else (),
                    with_plan=with_plan if ready else (),
                    ci_upper=tuple(min(1.0, value + 0.02) for value in projected) if ready else (),
                    ci_lower=tuple(max(0.0, value - 0.02) for value in projected) if ready else (),
                ),
                summary=DemoForecastSummary(
                    **{
                        "30d_no_action": projected[-1] if ready else 0.0,
                        "30d_with_plan": with_plan[-1] if ready else 0.0,
                        "risk_reduction": 0.005 if ready else 0.0,
                    }
                ),
            ),
            intervention_effect=DemoInterventionEffect(
                individual_att=individual_att,
                att_available=individual_att is not None,
                att_unavailable_reason=None if individual_att is not None else "demo_history_insufficient",
                intervention_days=intervention_days,
                intervention_data_sufficient=ready,
            ),
            reports=DemoReports(
                user=DemoUserReport(
                    headline="Demo Mock attribution",
                    body="Synthetic trend preview for demonstration only.",
                    advice="Continue collecting real data; this preview is not medical advice.",
                )
            ),
            model_version=MOCK_MODEL_VERSION,
            trend_delta=round(trend_delta, 4) if trend_delta is not None else None,
            adherence_average=round(adherence_average, 4) if adherence_average is not None else None,
            interpretation=interpretation,
        )
