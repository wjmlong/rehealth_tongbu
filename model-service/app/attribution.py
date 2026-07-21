from __future__ import annotations

from statistics import mean

from app.risk_scorer import MOCK_MODEL_VERSION
from app.schemas import IndividualAttributionRequest, IndividualAttributionResponse


class IndividualAttributor:
    def evaluate(self, request: IndividualAttributionRequest) -> IndividualAttributionResponse:
        if not request.events:
            return IndividualAttributionResponse(
                model_version=MOCK_MODEL_VERSION,
                trend_delta=None,
                adherence_average=None,
                interpretation="No events were provided, so no individual trend can be estimated.",
            )

        ordered = sorted(request.events, key=lambda event: event.date)
        baseline = request.baseline_risk_score
        if baseline is None:
            baseline = ordered[0].risk_score
        trend_delta = ordered[-1].risk_score - baseline
        adherence_values = [event.adherence for event in ordered if event.adherence is not None]
        adherence_average = mean(adherence_values) if adherence_values else None
        interpretation = (
            "Risk trend decreased during the observed window."
            if trend_delta < 0
            else "Risk trend did not decrease during the observed window."
        )
        return IndividualAttributionResponse(
            model_version=MOCK_MODEL_VERSION,
            trend_delta=round(trend_delta, 4),
            adherence_average=round(adherence_average, 4) if adherence_average is not None else None,
            interpretation=interpretation,
        )
