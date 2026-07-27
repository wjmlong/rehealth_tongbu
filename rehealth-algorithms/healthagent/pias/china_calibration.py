"""
Chinese Population Calibration Layer

Calibrate NHANES-trained models for Chinese population using CHARLS data.
Implements Platt scaling and isotonic regression for probability calibration.
"""

import numpy as np
import pandas as pd
from typing import Dict, List, Optional, Tuple, Any
from sklearn.calibration import CalibratedClassifierCV
from sklearn.isotonic import IsotonicRegression
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import train_test_split
from sklearn.metrics import roc_auc_score, brier_score_loss
import joblib


class ChinesePopulationCalibrator:
    """
    Calibrate model predictions for Chinese population.

    Adjusts NHANES-trained model probabilities to match
    Chinese CVD prevalence rates.
    """

    # Chinese population CVD prevalence by age group (from CHARLS)
    CHINESE_CVD_PREVALENCE = {
        "35-44": 0.12,
        "45-54": 0.18,
        "55-64": 0.25,
        "65-74": 0.35,
        "75+": 0.40,
    }

    # NHANES CVD prevalence by age group (for comparison)
    NHANES_CVD_PREVALENCE = {
        "35-44": 0.05,
        "45-54": 0.10,
        "55-64": 0.15,
        "65-74": 0.25,
        "75+": 0.35,
    }

    def __init__(self, method: str = "platt"):
        """
        Initialize calibrator.

        Parameters
        ----------
        method : str
            Calibration method: "platt", "isotonic", or "both"
        """
        self.method = method
        self.calibrator = None
        self.isotonic_calibrator = None
        self.calibration_params = {}

    def fit(
        self,
        y_prob: np.ndarray,
        y_true: np.ndarray,
        age_groups: Optional[np.ndarray] = None,
    ) -> "ChinesePopulationCalibrator":
        """
        Fit calibrator on Chinese validation data.

        Parameters
        ----------
        y_prob : np.ndarray
            Model predicted probabilities
        y_true : np.ndarray
            True labels (0/1)
        age_groups : np.ndarray, optional
            Age group labels for group-specific calibration

        Returns
        -------
        self
        """
        if self.method == "platt":
            self._fit_platt(y_prob, y_true)
        elif self.method == "isotonic":
            self._fit_isotonic(y_prob, y_true)
        elif self.method == "both":
            self._fit_platt(y_prob, y_true)
            self._fit_isotonic(y_prob, y_true)

        # Calculate calibration metrics
        self.calibration_params["brier_score_before"] = brier_score_loss(y_true, y_prob)
        calibrated = self.calibrate(y_prob)
        self.calibration_params["brier_score_after"] = brier_score_loss(y_true, calibrated)

        return self

    def calibrate(self, y_prob: np.ndarray) -> np.ndarray:
        """
        Calibrate predicted probabilities.

        Parameters
        ----------
        y_prob : np.ndarray
            Raw model probabilities

        Returns
        -------
        np.ndarray
            Calibrated probabilities
        """
        if self.method == "platt":
            return self._calibrate_platt(y_prob)
        elif self.method == "isotonic":
            return self._calibrate_isotonic(y_prob)
        elif self.method == "both":
            # Use isotonic if available, otherwise Platt
            if self.isotonic_calibrator is not None:
                return self._calibrate_isotonic(y_prob)
            return self._calibrate_platt(y_prob)
        else:
            return y_prob

    def _fit_platt(self, y_prob: np.ndarray, y_true: np.ndarray):
        """Fit Platt scaling (logistic regression on logits)."""
        # Convert probabilities to logits
        eps = 1e-7
        logits = np.log(np.clip(y_prob, eps, 1 - eps) / (1 - np.clip(y_prob, eps, 1 - eps)))

        # Fit logistic regression
        self.calibrator = LogisticRegression(C=1.0, solver='lbfgs')
        self.calibrator.fit(logits.reshape(-1, 1), y_true)

        # Store parameters
        self.calibration_params["platt_coef"] = float(self.calibrator.coef_[0, 0])
        self.calibration_params["platt_intercept"] = float(self.calibrator.intercept_[0])

    def _fit_isotonic(self, y_prob: np.ndarray, y_true: np.ndarray):
        """Fit isotonic regression."""
        self.isotonic_calibrator = IsotonicRegression(out_of_bounds='clip')
        self.isotonic_calibrator.fit(y_prob, y_true)

    def _calibrate_platt(self, y_prob: np.ndarray) -> np.ndarray:
        """Apply Platt scaling."""
        if self.calibrator is None:
            return y_prob

        eps = 1e-7
        logits = np.log(np.clip(y_prob, eps, 1 - eps) / (1 - np.clip(y_prob, eps, 1 - eps)))
        return self.calibrator.predict_proba(logits.reshape(-1, 1))[:, 1]

    def _calibrate_isotonic(self, y_prob: np.ndarray) -> np.ndarray:
        """Apply isotonic regression."""
        if self.isotonic_calibrator is None:
            return y_prob
        return self.isotonic_calibrator.predict(y_prob)

    def get_calibration_report(self) -> Dict[str, Any]:
        """Get calibration report."""
        return {
            "method": self.method,
            "parameters": self.calibration_params,
            "improvement": (
                self.calibration_params.get("brier_score_before", 0)
                - self.calibration_params.get("brier_score_after", 0)
            ),
        }

    def save(self, path: str):
        """Save calibrator to file."""
        joblib.dump({
            "method": self.method,
            "calibrator": self.calibrator,
            "isotonic_calibrator": self.isotonic_calibrator,
            "calibration_params": self.calibration_params,
        }, path)

    @classmethod
    def load(cls, path: str) -> "ChinesePopulationCalibrator":
        """Load calibrator from file."""
        data = joblib.load(path)
        calibrator = cls(method=data["method"])
        calibrator.calibrator = data["calibrator"]
        calibrator.isotonic_calibrator = data["isotonic_calibrator"]
        calibrator.calibration_params = data["calibration_params"]
        return calibrator


class ChineseFeatureThresholds:
    """
    Chinese-specific feature thresholds.

    Based on Chinese medical guidelines:
    - Chinese Guidelines for Management of Hypertension (2023)
    - Chinese Diabetes Society Guidelines
    - China CDC BMI cutoffs
    """

    # BMI categories (Chinese standards)
    BMI_CATEGORIES = {
        "underweight": (0, 18.5),
        "normal": (18.5, 24),
        "overweight": (24, 28),
        "obese": (28, float('inf')),
    }

    # Blood pressure categories (Chinese 2023 guidelines)
    BP_CATEGORIES = {
        "normal": {"sbp": (0, 120), "dbp": (0, 80)},
        "elevated": {"sbp": (120, 140), "dbp": (80, 90)},
        "stage1_hypertension": {"sbp": (140, 160), "dbp": (90, 100)},
        "stage2_hypertension": {"sbp": (160, 180), "dbp": (100, 110)},
        "stage3_hypertension": {"sbp": (180, float('inf')), "dbp": (110, float('inf'))},
    }

    # Fasting glucose categories (Chinese Diabetes Society)
    GLUCOSE_CATEGORIES = {
        "normal": (0, 6.1),
        "impaired_fasting_glucose": (6.1, 7.0),
        "diabetes": (7.0, float('inf')),
    }

    # Lipid targets (Chinese guidelines)
    LIPID_TARGETS = {
        "total_cholesterol": {"desirable": 5.2, "borderline": 6.2, "high": 6.2},
        "ldl": {"desirable": 3.4, "borderline_high": 4.1, "high": 4.1},
        "hdl": {"low": 1.0, "desirable": 1.0, "high": 1.6},
        "triglycerides": {"desirable": 1.7, "borderline_high": 2.3, "high": 2.3},
    }

    # Waist circumference (Chinese standards)
    WAIST_CIRCUMFERENCE = {
        "male": {"normal": 90, "central_obesity": 90},
        "female": {"normal": 85, "central_obesity": 85},
    }

    @classmethod
    def get_bmi_category(cls, bmi: float) -> str:
        """Get BMI category using Chinese standards."""
        if bmi < 18.5:
            return "underweight"
        elif bmi < 24:
            return "normal"
        elif bmi < 28:
            return "overweight"
        else:
            return "obese"

    @classmethod
    def get_bp_category(cls, sbp: float, dbp: float) -> str:
        """Get blood pressure category using Chinese 2023 guidelines."""
        if sbp >= 180 or dbp >= 110:
            return "stage3_hypertension"
        elif sbp >= 160 or dbp >= 100:
            return "stage2_hypertension"
        elif sbp >= 140 or dbp >= 90:
            return "stage1_hypertension"
        elif sbp >= 120 or dbp >= 80:
            return "elevated"
        else:
            return "normal"

    @classmethod
    def get_glucose_category(cls, glucose_mmol: float) -> str:
        """Get glucose category using Chinese Diabetes Society guidelines."""
        if glucose_mmol >= 7.0:
            return "diabetes"
        elif glucose_mmol >= 6.1:
            return "impaired_fasting_glucose"
        else:
            return "normal"

    @classmethod
    def get_lipid_category(cls, total_cholesterol: float, ldl: float, hdl: float, tg: float) -> str:
        """Get lipid category."""
        risk_factors = 0

        if total_cholesterol > 5.2:
            risk_factors += 1
        if ldl > 3.4:
            risk_factors += 1
        if hdl < 1.0:
            risk_factors += 1
        if tg > 1.7:
            risk_factors += 1

        if risk_factors >= 3:
            return "high_risk"
        elif risk_factors >= 2:
            return "moderate_risk"
        elif risk_factors >= 1:
            return "low_risk"
        else:
            return "optimal"

    @classmethod
    def get_metabolic_score(cls, features: Dict[str, float]) -> int:
        """
        Calculate metabolic syndrome score using Chinese criteria.

        Parameters
        ----------
        features : dict
            Feature values

        Returns
        -------
        int
            Metabolic syndrome score (0-5)
        """
        score = 0

        # Waist circumference
        gender = features.get("gender", 1)
        waist = features.get("waist_circumference", 0)
        threshold = cls.WAIST_CIRCUMFERENCE["male" if gender == 1 else "female"]["central_obesity"]
        if waist >= threshold:
            score += 1

        # Triglycerides
        if features.get("triglycerides", 0) >= 1.7:
            score += 1

        # HDL
        if features.get("hdl", 0) < 1.0:
            score += 1

        # Blood pressure
        if features.get("sbp", 0) >= 130 or features.get("dbp", 0) >= 85:
            score += 1

        # Fasting glucose
        if features.get("fasting_glucose", 0) >= 5.6:
            score += 1

        return score

    @classmethod
    def get_risk_factors_count(cls, features: Dict[str, float]) -> int:
        """
        Count risk factors using Chinese guidelines.

        Parameters
        ----------
        features : dict
            Feature values

        Returns
        -------
        int
            Number of risk factors
        """
        count = 0

        # Age
        if features.get("age", 0) >= 45:
            count += 1

        # Smoking
        if features.get("smoking", 0) == 1:
            count += 1

        # BMI
        if features.get("bmi", 0) >= 24:
            count += 1

        # Blood pressure
        if features.get("sbp", 0) >= 140 or features.get("dbp", 0) >= 90:
            count += 1

        # Glucose
        if features.get("fasting_glucose", 0) >= 6.1:
            count += 1

        # Lipids
        if features.get("total_cholesterol", 0) >= 5.2:
            count += 1
        if features.get("hdl", 0) < 1.0:
            count += 1

        # Family history
        if features.get("family_history", 0) == 1:
            count += 1

        return count
