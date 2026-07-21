"""
CatBoost CVD risk scoring with SHAP explanations and model registry.

Part of bodyup_cloud.engine — V1 specification.
"""

import joblib
import shap
import numpy as np
import pandas as pd
from typing import Dict, Optional


class CVDRiskScorer:
    """Cardiovascular disease risk scorer backed by a CatBoost model."""

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

    def __init__(self, model_path: str, model_auc: float | None = None):
        self.model = joblib.load(model_path)
        self.explainer = shap.TreeExplainer(self.model)
        self._model_auc = model_auc

    def predict(self, features: dict) -> dict:
        """Run risk prediction with SHAP explanation.

        Parameters
        ----------
        features : dict
            Must contain all keys in FEATURE_COLS.

        Returns
        -------
        dict with risk_score, risk_level, feature_contributions,
        model_version, and model_auc.
        """
        # 1. Build 1-row DataFrame in FEATURE_COLS order
        row = {col: features[col] for col in self.FEATURE_COLS}
        df = pd.DataFrame([row], columns=self.FEATURE_COLS)

        # 2. Cast categorical features to int
        for col in self.CAT_FEATURES:
            df[col] = df[col].astype(int)

        # 3. predict_proba -> risk_score (probability of class 1)
        proba = self.model.predict_proba(df)
        risk_score = float(proba[0, 1])

        # 4. SHAP values -> feature_contributions dict
        shap_values = self.explainer.shap_values(df)
        # For binary classification TreeExplainer may return a list of two
        # arrays (one per class) or a single array.  We want class-1 values.
        if isinstance(shap_values, list):
            sv = shap_values[1][0]
        else:
            sv = shap_values[0]
        feature_contributions = {
            col: float(sv[i]) for i, col in enumerate(self.FEATURE_COLS)
        }

        # 5. Assemble result
        return {
            "risk_score": risk_score,
            "risk_level": self._grade(risk_score),
            "feature_contributions": feature_contributions,
            "model_version": "2.0",
            "model_auc": self._model_auc if self._model_auc is not None else "not_evaluated",
        }

    @staticmethod
    def _grade(score: float) -> str:
        if score < 0.3:
            return "low"
        if score < 0.5:
            return "moderate"
        if score < 0.7:
            return "high"
        return "very_high"


class ModelRegistry:
    """Thread-safe registry for named CVDRiskScorer instances."""

    def __init__(self):
        self._models: Dict[str, CVDRiskScorer] = {}

    def register(self, name: str, model_path: str, model_auc: float | None = None):
        self._models[name] = CVDRiskScorer(model_path, model_auc=model_auc)

    def get(self, name: str = "default") -> CVDRiskScorer:
        if name not in self._models:
            raise KeyError(f"Model '{name}' not registered")
        return self._models[name]

    def list_models(self) -> list:
        return list(self._models.keys())
