# -*- coding: utf-8 -*-
"""
Intervention effect attribution engine.

Uses counterfactual decomposition to attribute risk change between T0 and T1:
  1. T0 risk — actual baseline
  2. T1 risk — actual follow-up
  3. Counterfactual risk — what T1 would be if modifiable features stayed at T0
  4. Intervention effect = counterfactual - T1 risk (risk avoided by improvements)
  5. Natural change = counterfactual - T0 risk (change from non-modifiable factors)

Also provides per-feature marginal contributions via single-feature counterfactuals.

This is the commercial core for insurance settlement: quantified proof that
following the platform's health advice reduced CVD risk → fewer claims.
"""

from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any

import numpy as np
import pandas as pd

MODIFIABLE_FEATURES = [
    "bmi", "sbp", "dbp", "fasting_glucose",
    "total_cholesterol", "ldl", "hdl", "triglycerides",
    "exercise_days", "smoking", "drinking",
]

NON_MODIFIABLE_FEATURES = [
    "age", "gender", "family_history",
    "diabetes_history", "hypertension_history",
]


@dataclass
class FeatureAttribution:
    feature: str
    t0_value: float
    t1_value: float
    value_changed: bool
    is_modifiable: bool
    marginal_effect: float  # risk reduction from this feature alone (negative = good)
    direction: str  # "improved", "worsened", "unchanged"


@dataclass
class InterventionReport:
    risk_before: float
    risk_after: float
    risk_change: float
    risk_level_before: str
    risk_level_after: str
    counterfactual_risk: float
    intervention_effect: float
    natural_change: float
    attribution_ratio: float
    feature_details: list[FeatureAttribution] = field(default_factory=list)

    def to_dict(self) -> dict[str, Any]:
        return {
            "risk_before": round(self.risk_before, 4),
            "risk_after": round(self.risk_after, 4),
            "risk_change": round(self.risk_change, 4),
            "risk_level_before": self.risk_level_before,
            "risk_level_after": self.risk_level_after,
            "counterfactual_risk": round(self.counterfactual_risk, 4),
            "intervention_effect": round(self.intervention_effect, 4),
            "natural_change": round(self.natural_change, 4),
            "attribution_ratio": round(self.attribution_ratio, 4),
            "feature_details": [
                {
                    "feature": f.feature,
                    "t0_value": round(f.t0_value, 4),
                    "t1_value": round(f.t1_value, 4),
                    "value_changed": f.value_changed,
                    "is_modifiable": f.is_modifiable,
                    "marginal_effect": round(f.marginal_effect, 4),
                    "direction": f.direction,
                }
                for f in self.feature_details
            ],
            "summary": self._summary(),
        }

    def _summary(self) -> str:
        if self.intervention_effect <= 0:
            return (
                f"干预无效或风险上升。"
                f"风险 {self.risk_before:.3f}→{self.risk_after:.3f}"
            )
        pct = self.intervention_effect / self.risk_before * 100 if self.risk_before > 0 else 0
        return (
            f"干预有效，避免了 {self.intervention_effect:.3f} 的风险增长"
            f"（占基线风险的 {pct:.1f}%）。"
            f"风险 {self.risk_before:.3f}→{self.risk_after:.3f}，"
            f"其中 {self.attribution_ratio:.0%} 归因于干预效果"
        )


class InterventionAttributor:
    """Compute intervention effect attribution between two time points."""

    FEATURE_COLS = [
        "age", "gender", "bmi", "sbp", "dbp",
        "fasting_glucose", "total_cholesterol", "ldl", "hdl", "triglycerides",
        "exercise_days", "smoking", "drinking",
        "diabetes_history", "hypertension_history", "family_history",
    ]
    CAT_FEATURES = [
        "gender", "smoking", "drinking",
        "diabetes_history", "hypertension_history", "family_history",
    ]

    def __init__(self, model):
        self.model = model

    def _prepare_row(self, features: dict) -> pd.DataFrame:
        row = {col: features[col] for col in self.FEATURE_COLS}
        df = pd.DataFrame([row], columns=self.FEATURE_COLS)
        for col in self.CAT_FEATURES:
            df[col] = df[col].astype(int)
        return df

    def _score(self, features: dict) -> float:
        df = self._prepare_row(features)
        return float(self.model.predict_proba(df)[0, 1])

    @staticmethod
    def _grade(score: float) -> str:
        if score < 0.3:
            return "low"
        if score < 0.5:
            return "moderate"
        if score < 0.7:
            return "high"
        return "very_high"

    def attribute(self, t0_features: dict, t1_features: dict) -> InterventionReport:
        """Compare T0 and T1 patient snapshots and attribute risk change.

        Counterfactual decomposition:
          - counterfactual = T1 with modifiable features rolled back to T0 values
            (i.e., "what if the patient didn't improve modifiable factors?")
          - intervention_effect = counterfactual_risk - t1_risk
            (positive means the intervention reduced risk)
          - natural_change = counterfactual_risk - t0_risk
            (risk change from non-modifiable factors like aging)

        Also computes per-feature marginal effects via single-feature swaps.
        """
        risk_t0 = self._score(t0_features)
        risk_t1 = self._score(t1_features)

        # Counterfactual: T1 non-modifiable features + T0 modifiable features
        cf_features = dict(t1_features)
        for feat in MODIFIABLE_FEATURES:
            cf_features[feat] = t0_features[feat]
        risk_cf = self._score(cf_features)

        # Decomposition
        intervention_effect = risk_cf - risk_t1  # positive = intervention helped
        natural_change = risk_cf - risk_t0       # positive = natural worsening

        risk_change = risk_t1 - risk_t0
        total_change = abs(risk_change) if abs(risk_change) > 1e-6 else 1e-6
        attribution_ratio = max(0.0, min(1.0, intervention_effect / total_change)) if risk_change < 0 else (
            intervention_effect / (intervention_effect + abs(natural_change))
            if intervention_effect > 0 and (intervention_effect + abs(natural_change)) > 1e-6
            else 0.0
        )

        # Per-feature marginal effects
        details = []
        for col in self.FEATURE_COLS:
            t0_val = float(t0_features[col])
            t1_val = float(t1_features[col])
            is_mod = col in MODIFIABLE_FEATURES
            changed = abs(t0_val - t1_val) > 1e-6

            if changed:
                # Marginal: swap just this one feature back to T0 in T1 snapshot
                swap = dict(t1_features)
                swap[col] = t0_features[col]
                risk_swap = self._score(swap)
                marginal = risk_swap - risk_t1  # positive = this feature's T1 value helped
            else:
                marginal = 0.0

            if not changed:
                direction = "unchanged"
            elif marginal > 1e-4:
                direction = "improved"
            elif marginal < -1e-4:
                direction = "worsened"
            else:
                direction = "negligible"

            details.append(FeatureAttribution(
                feature=col,
                t0_value=t0_val,
                t1_value=t1_val,
                value_changed=changed,
                is_modifiable=is_mod,
                marginal_effect=marginal,
                direction=direction,
            ))

        details.sort(key=lambda f: abs(f.marginal_effect), reverse=True)

        return InterventionReport(
            risk_before=risk_t0,
            risk_after=risk_t1,
            risk_change=risk_change,
            risk_level_before=self._grade(risk_t0),
            risk_level_after=self._grade(risk_t1),
            counterfactual_risk=risk_cf,
            intervention_effect=intervention_effect,
            natural_change=natural_change,
            attribution_ratio=attribution_ratio,
            feature_details=details,
        )
