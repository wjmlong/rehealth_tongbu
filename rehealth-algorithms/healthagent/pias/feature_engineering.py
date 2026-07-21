"""
Feature Engineering for PIAS Engine

Expand 16 base features to 40+ derived features using domain knowledge.
Goal: Improve V2 model AUC from 0.767 to 0.78+ without requiring extra user data.
"""

import numpy as np
import pandas as pd
from typing import Dict, List


class FeatureEngineer:
    """
    Expand 16 base CVD features into 40+ engineered features.

    All derived features are computed from the original 16 features,
    so users don't need to provide any additional data.
    """

    # Original 16 features
    BASE_FEATURES = [
        "age", "gender", "bmi", "sbp", "dbp",
        "fasting_glucose", "total_cholesterol", "ldl", "hdl", "triglycerides",
        "exercise_days", "smoking", "drinking",
        "diabetes_history", "hypertension_history", "family_history",
    ]

    # Derived feature names
    DERIVED_FEATURES = [
        # Blood pressure features
        "pulse_pressure",
        "mean_arterial_pressure",
        "bp_ratio",
        "bp_category",

        # Lipid features
        "non_hdl_cholesterol",
        "tc_hdl_ratio",
        "ldl_hdl_ratio",
        "trig_hdl_ratio",
        "atherogenic_index",

        # Metabolic features
        "metabolic_score",
        "glucose_category",

        # BMI features
        "bmi_category",
        "bmi_age_interaction",

        # Risk interactions
        "smoking_age_interaction",
        "smoking_bp_interaction",
        "diabetes_glucose_interaction",

        # Composite risk scores
        "framingham_risk_factors",
        "ascvd_risk_factors",
        "risk_factor_count",

        # Lifestyle score
        "lifestyle_score",

        # Age-related
        "age_squared",
        "age_risk_interaction",

        # Gender-specific adjustments
        "gender_age_interaction",
        "gender_bp_interaction",
    ]

    def __init__(self):
        self.all_features = self.BASE_FEATURES + self.DERIVED_FEATURES

    def transform(self, df: pd.DataFrame) -> pd.DataFrame:
        """
        Transform base features into expanded feature set.

        Parameters
        ----------
        df : pd.DataFrame
            Must contain all 16 BASE_FEATURES columns.

        Returns
        -------
        pd.DataFrame with original + derived features.
        """
        result = df.copy()

        # Blood pressure features
        result["pulse_pressure"] = result["sbp"] - result["dbp"]
        result["mean_arterial_pressure"] = result["dbp"] + (result["sbp"] - result["dbp"]) / 3
        result["bp_ratio"] = result["sbp"] / result["dbp"].clip(lower=1)
        result["bp_category"] = result["sbp"].apply(self._bp_category)

        # Lipid features
        result["non_hdl_cholesterol"] = result["total_cholesterol"] - result["hdl"]
        result["tc_hdl_ratio"] = result["total_cholesterol"] / result["hdl"].clip(lower=0.1)
        result["ldl_hdl_ratio"] = result["ldl"] / result["hdl"].clip(lower=0.1)
        result["trig_hdl_ratio"] = result["triglycerides"] / result["hdl"].clip(lower=0.1)
        result["atherogenic_index"] = np.log10(result["triglycerides"].clip(lower=0.1) / result["hdl"].clip(lower=0.1))

        # Metabolic features
        result["metabolic_score"] = (
            (result["fasting_glucose"] > 5.6).astype(int) +
            (result["triglycerides"] > 1.7).astype(int) +
            (result["hdl"] < 1.0).astype(int) +
            (result["sbp"] > 130).astype(int) +
            (result["bmi"] > 25).astype(int)
        )
        result["glucose_category"] = result["fasting_glucose"].apply(self._glucose_category)

        # BMI features
        result["bmi_category"] = result["bmi"].apply(self._bmi_category)
        result["bmi_age_interaction"] = result["bmi"] * result["age"]

        # Risk interactions
        result["smoking_age_interaction"] = result["smoking"] * result["age"]
        result["smoking_bp_interaction"] = result["smoking"] * result["sbp"]
        result["diabetes_glucose_interaction"] = result["diabetes_history"] * result["fasting_glucose"]

        # Composite risk scores
        result["framingham_risk_factors"] = (
            self._age_points(result["age"]) +
            self._tc_points(result["total_cholesterol"], result["age"]) +
            self._hdl_points(result["hdl"]) +
            self._bp_points(result["sbp"], result["bp_category"]) +
            result["smoking"] * 4 +
            result["diabetes_history"] * 3
        )

        result["ascvd_risk_factors"] = (
            result["age"] * 0.1 +
            result["sbp"] * 0.02 +
            result["total_cholesterol"] * 0.05 +
            result["hdl"] * -0.08 +
            result["fasting_glucose"] * 0.03 +
            result["smoking"] * 5 +
            result["diabetes_history"] * 4
        )

        result["risk_factor_count"] = (
            (result["smoking"] > 0).astype(int) +
            (result["bmi"] > 25).astype(int) +
            (result["sbp"] > 140).astype(int) +
            (result["fasting_glucose"] > 7.0).astype(int) +
            (result["total_cholesterol"] > 6.2).astype(int) +
            (result["exercise_days"] < 3).astype(int) +
            result["family_history"]
        )

        # Lifestyle score (0-100, higher is better)
        result["lifestyle_score"] = (
            result["exercise_days"] * 8 +
            (1 - result["smoking"]) * 30 +
            (1 - result["drinking"]) * 15 +
            np.clip(result["bmi"], 18, 30).apply(lambda x: max(0, 30 - abs(x - 22)) * 1.5)
        )

        # Age-related
        result["age_squared"] = result["age"] ** 2
        result["age_risk_interaction"] = result["age"] * result["risk_factor_count"]

        # Gender-specific
        result["gender_age_interaction"] = result["gender"] * result["age"]
        result["gender_bp_interaction"] = result["gender"] * result["sbp"]

        return result

    def get_feature_names(self) -> List[str]:
        """Return all feature names (base + derived)."""
        return self.all_features

    # ─────────────────────────────────────────────
    # Helper functions for category encoding
    # ─────────────────────────────────────────────

    @staticmethod
    def _bp_category(sbp: float) -> int:
        """Blood pressure category (0=normal, 1=elevated, 2=high)."""
        if sbp < 120:
            return 0
        elif sbp < 130:
            return 1
        elif sbp < 140:
            return 2
        else:
            return 3

    @staticmethod
    def _glucose_category(glucose: float) -> int:
        """Glucose category (0=normal, 1=prediabetic, 2=diabetic)."""
        if glucose < 5.6:
            return 0
        elif glucose < 7.0:
            return 1
        else:
            return 2

    @staticmethod
    def _bmi_category(bmi: float) -> int:
        """BMI category (0=underweight, 1=normal, 2=overweight, 3=obese)."""
        if bmi < 18.5:
            return 0
        elif bmi < 25:
            return 1
        elif bmi < 30:
            return 2
        else:
            return 3

    @staticmethod
    def _age_points(age: pd.Series) -> pd.Series:
        """Framingham age points."""
        return age.apply(lambda a:
            0 if a < 35 else
            1 if a < 40 else
            2 if a < 45 else
            3 if a < 50 else
            4 if a < 55 else
            5 if a < 60 else
            6 if a < 65 else
            7 if a < 70 else
            8
        )

    @staticmethod
    def _tc_points(tc: pd.Series, age: pd.Series) -> pd.Series:
        """Framingham total cholesterol points."""
        def _points(tc_val, age_val):
            if age_val < 40:
                base = 0 if tc_val < 4.1 else 1 if tc_val < 5.2 else 2 if tc_val < 6.2 else 3
            elif age_val < 50:
                base = 0 if tc_val < 4.1 else 1 if tc_val < 5.2 else 2 if tc_val < 6.2 else 3
            else:
                base = 0 if tc_val < 4.1 else 1 if tc_val < 5.2 else 2 if tc_val < 6.2 else 3
            return base
        return pd.Series([_points(t, a) for t, a in zip(tc, age)], index=tc.index)

    @staticmethod
    def _hdl_points(hdl: pd.Series) -> pd.Series:
        """Framingham HDL points."""
        return hdl.apply(lambda h:
            2 if h < 1.0 else
            1 if h < 1.3 else
            0 if h < 1.6 else
            -1
        )

    @staticmethod
    def _bp_points(sbp: pd.Series, bp_cat: pd.Series) -> pd.Series:
        """Framingham BP points."""
        return pd.Series([
            0 if sbp_val < 120 else 1 if sbp_val < 130 else 2 if sbp_val < 140 else 3
            for sbp_val in sbp
        ], index=sbp.index)


class FeatureSelector:
    """
    Select top-K most important features for the distilled model.
    """

    def __init__(self, k: int = 25):
        self.k = k
        self.selected_features = None

    def fit(self, X: pd.DataFrame, y: pd.Series, feature_names: List[str]):
        """
        Fit feature selector using mutual information or correlation.
        """
        from sklearn.feature_selection import mutual_info_classif

        mi_scores = mutual_info_classif(X, y, random_state=42)
        mi_df = pd.DataFrame({
            "feature": feature_names,
            "mi_score": mi_scores
        }).sort_values("mi_score", ascending=False)

        self.selected_features = mi_df.head(self.k)["feature"].tolist()
        return self

    def transform(self, X: pd.DataFrame) -> pd.DataFrame:
        """Select top-K features."""
        if self.selected_features is None:
            raise ValueError("FeatureSelector not fitted yet")
        return X[self.selected_features]

    def get_selected_features(self) -> List[str]:
        """Return selected feature names."""
        return self.selected_features or []
