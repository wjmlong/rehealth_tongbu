from __future__ import annotations

from dataclasses import dataclass
from math import isfinite
from time import time

from app.schemas import CvdFeatureVector, FeatureQuality, FeatureQualityStatus, FeatureSource


FACTOR16_RULE_VERSION = "factor16-rule-v1.0.0"
CLINICAL_80_20_FIELDS = {
    "sbp",
    "dbp",
    "fasting_glucose",
    "total_cholesterol",
    "ldl",
    "hdl",
    "triglycerides",
}


@dataclass(frozen=True)
class Factor16ContributionResult:
    contributions: dict[str, float]
    measured_components: dict[str, float]
    control_support_components: dict[str, float]
    rule_version: str = FACTOR16_RULE_VERSION


class Factor16ContributionEngine:
    """Transparent 16-factor display contribution evaluator.

    This is separate from the CVD probability model and from RDI/RHI. It converts
    the current, quality-gated CVD-16 inputs into governed contribution points for
    the Android contribution card. Blood-pressure and laboratory cards apply the
    governed 80/20 display split. Until a verified longitudinal control-support
    input is supplied, the 20% component stays zero rather than being fabricated.
    """

    def __init__(self, now_millis: int | None = None) -> None:
        self._now_millis = now_millis if now_millis is not None else int(time() * 1000)

    def evaluate(self, vector: CvdFeatureVector) -> Factor16ContributionResult:
        raw: dict[str, float] = {}
        values = vector.model_dump()

        self._put(raw, vector, "age", self._clamp((self._number(values["age"]) - 40.0) / 5.0 * 0.8, -2.4, 8.0))
        gender = values["gender"]
        self._put(raw, vector, "gender", 1.5 if gender == 1 else 0.0)
        self._put(raw, vector, "bmi", self._bmi(values["bmi"]))
        self._put(raw, vector, "sbp", self._bp(values["sbp"], 120.0, 5.0, 0.6, -1.2, 6.0), require_cuff=True)
        self._put(raw, vector, "dbp", self._bp(values["dbp"], 80.0, 5.0, 0.5, -1.0, 4.0), require_cuff=True)

        diabetes = values["diabetes_history"] == 1
        glucose_target = 7.0 if diabetes else 6.1
        self._put(
            raw,
            vector,
            "fasting_glucose",
            self._fasting_glucose(values["fasting_glucose"], glucose_target),
            lab=True,
        )
        tc_multiplier = 0.35 if self._usable(vector, "ldl", lab=True) else 1.0
        self._put(
            raw,
            vector,
            "total_cholesterol",
            self._linear(values["total_cholesterol"], 5.2, 0.5, 0.5, -1.0, 2.0) * tc_multiplier,
            lab=True,
        )
        self._put(raw, vector, "ldl", self._linear(values["ldl"], 3.4, 0.5, 1.2, -2.4, 4.0), lab=True)
        self._put(
            raw,
            vector,
            "hdl",
            self._reverse_linear(values["hdl"], 1.0, 0.2, 0.4, -1.2, 1.2),
            lab=True,
        )
        self._put(
            raw,
            vector,
            "triglycerides",
            self._linear(values["triglycerides"], 1.7, 0.5, 0.6, -1.2, 2.4),
            lab=True,
        )
        self._put(
            raw,
            vector,
            "exercise_days",
            self._linear(5.0 - self._number(values["exercise_days"]), 0.0, 1.0, 0.6, -1.2, 3.0),
        )
        self._put(raw, vector, "smoking", 5.0 if values["smoking"] == 1 else 0.0)
        self._put(raw, vector, "drinking", 0.8 if values["drinking"] == 1 else 0.0)
        self._put(raw, vector, "diabetes_history", 3.0 if diabetes else 0.0)
        self._put(raw, vector, "hypertension_history", 2.5 if values["hypertension_history"] == 1 else 0.0)
        self._put(raw, vector, "family_history", 2.0 if values["family_history"] == 1 else 0.0)

        self._apply_domain_cap(raw, ("sbp", "dbp", "hypertension_history"), -2.0, 9.0)
        self._apply_domain_cap(raw, ("fasting_glucose", "diabetes_history"), -1.2, 6.0)
        self._apply_domain_cap(
            raw,
            ("total_cholesterol", "ldl", "hdl", "triglycerides"),
            -4.0,
            8.0,
        )

        measured = {
            field: round(points * 0.8, 4)
            for field, points in raw.items()
            if field in CLINICAL_80_20_FIELDS
        }
        support = {field: 0.0 for field in measured}
        displayed = {
            field: round(measured.get(field, points) + support.get(field, 0.0), 4)
            for field, points in raw.items()
        }
        return Factor16ContributionResult(
            contributions=displayed,
            measured_components=measured,
            control_support_components=support,
        )

    def _put(
        self,
        output: dict[str, float],
        vector: CvdFeatureVector,
        field: str,
        points: float,
        *,
        require_cuff: bool = False,
        lab: bool = False,
    ) -> None:
        if not self._usable(vector, field, require_cuff=require_cuff, lab=lab):
            return
        confidence = self._confidence(vector.feature_quality[field], lab=lab)
        if confidence <= 0.0:
            return
        output[field] = points * confidence

    def _usable(
        self,
        vector: CvdFeatureVector,
        field: str,
        *,
        require_cuff: bool = False,
        lab: bool = False,
    ) -> bool:
        value = getattr(vector, field)
        quality = vector.feature_quality.get(field)
        if value is None or quality is None or quality.status != FeatureQualityStatus.VALID:
            return False
        if require_cuff:
            reason = quality.reason.lower()
            if quality.source != FeatureSource.CLINICAL_REPORT or "upper-arm cuff" not in reason:
                return False
        if lab and quality.source != FeatureSource.CLINICAL_REPORT:
            return False
        return True

    def _confidence(self, quality: FeatureQuality, *, lab: bool) -> float:
        source_confidence = {
            FeatureSource.CLINICAL_REPORT: 1.0,
            FeatureSource.REAL_DEVICE: 0.80,
            FeatureSource.DERIVED: 0.80,
            FeatureSource.USER_REPORTED: 0.60,
            FeatureSource.UNKNOWN: 0.0,
        }[quality.source]
        if not lab or quality.observed_at is None:
            return source_confidence
        age_days = max(self._now_millis - quality.observed_at, 0) / 86_400_000
        freshness = 1.0 if age_days <= 90 else 0.8 if age_days <= 180 else 0.5 if age_days <= 365 else 0.2
        return source_confidence * freshness

    @staticmethod
    def _number(value: float | int | None) -> float:
        return float(value) if value is not None and isfinite(float(value)) else 0.0

    def _bmi(self, value: float | None) -> float:
        bmi = self._number(value)
        if bmi < 18.5:
            return self._clamp((18.5 - bmi) * 0.5, 0.0, 2.0)
        if bmi < 24.0:
            return 0.0
        if bmi < 28.0:
            return self._clamp((bmi - 24.0) * 0.5, 0.0, 2.0)
        return self._clamp(2.0 + (bmi - 28.0) * 0.75, 2.0, 5.0)

    def _bp(
        self,
        value: float | None,
        reference: float,
        step: float,
        points_per_step: float,
        minimum: float,
        maximum: float,
    ) -> float:
        measured = self._number(value)
        if (reference == 120.0 and measured < 90.0) or (reference == 80.0 and measured < 60.0):
            return 0.0
        return self._clamp((measured - reference) / step * points_per_step, minimum, maximum)

    def _fasting_glucose(self, value: float | None, target: float) -> float:
        measured = self._number(value)
        if measured < 3.9:
            return 0.0
        return self._linear(measured, target, 0.5, 0.6, -1.2, 4.0)

    def _linear(
        self,
        value: float | int | None,
        reference: float,
        step: float,
        points_per_step: float,
        minimum: float,
        maximum: float,
    ) -> float:
        return self._clamp((self._number(value) - reference) / step * points_per_step, minimum, maximum)

    def _reverse_linear(
        self,
        value: float | None,
        reference: float,
        step: float,
        points_per_step: float,
        minimum: float,
        maximum: float,
    ) -> float:
        return self._clamp((reference - self._number(value)) / step * points_per_step, minimum, maximum)

    @staticmethod
    def _clamp(value: float, minimum: float, maximum: float) -> float:
        return min(max(value, minimum), maximum)

    @staticmethod
    def _apply_domain_cap(
        contributions: dict[str, float],
        fields: tuple[str, ...],
        minimum: float,
        maximum: float,
    ) -> None:
        present = [field for field in fields if field in contributions]
        total = sum(contributions[field] for field in present)
        capped = min(max(total, minimum), maximum)
        if present and total != 0.0 and capped != total:
            scale = capped / total
            for field in present:
                contributions[field] *= scale
