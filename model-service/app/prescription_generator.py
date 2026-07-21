from __future__ import annotations

from datetime import UTC, datetime
from uuid import NAMESPACE_URL, uuid5

from app.schemas import InterventionGenerateRequest, InterventionGenerateResponse


MEDICAL_DISCLAIMER = (
    "This guidance is conservative wellness support and does not diagnose disease or replace a clinician."
)


class ConservativePrescriptionGenerator:
    def generate(self, request: InterventionGenerateRequest) -> InterventionGenerateResponse:
        level = request.risk_result.risk_level
        contraindications = [
            "chest pain",
            "dizziness",
            "unusual shortness of breath",
            "clinician-advised activity restriction",
        ]

        if level in {"high", "very_high"}:
            priority = "Arrange clinician review of blood pressure, labs, medications, and symptoms."
            rationale = "The risk estimate is elevated, so conservative next action is clinical review with complete context."
            expected_impact = "May improve safety and care planning; no outcome change is guaranteed."
            confidence = 0.62
        elif request.risk_result.feature_contributions.get("sbp", 0.0) > 0:
            priority = "Record blood pressure at consistent morning and evening times for 3 days."
            rationale = "Blood pressure contributed to the estimate, and repeated measurements can reduce uncertainty."
            expected_impact = "May clarify whether readings are persistent or one-off variation."
            confidence = 0.58
        else:
            priority = "Add 15 to 20 minutes of easy walking after one meal if there are no contraindications."
            rationale = "Light activity is a conservative wellness step when the current risk estimate is not high."
            expected_impact = "May support cardiometabolic health over time; it is not a treatment claim."
            confidence = 0.52

        plan_seed = f"{request.risk_result.model_version}:{level}:{priority}"
        return InterventionGenerateResponse(
            plan_id=f"plan-{uuid5(NAMESPACE_URL, plan_seed)}",
            generated_at=datetime.now(UTC),
            priority_intervention=priority,
            rationale=rationale,
            expected_impact=expected_impact,
            contraindications=contraindications,
            confidence=confidence,
            model_version=request.risk_result.model_version,
            is_mock=request.risk_result.is_mock,
            medical_disclaimer=MEDICAL_DISCLAIMER,
        )
