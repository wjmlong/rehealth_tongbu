# -*- coding: utf-8 -*-
"""
数据源基类 — 多源医学数据统一接口
定义标准16特征格式、有效范围、以及所有数据源必须实现的抽象方法。

Standard 16 features (对齐官网表单):
  age, gender, bmi, sbp, dbp,
  fasting_glucose, total_cholesterol, ldl, hdl, triglycerides,
  exercise_days, smoking, drinking,
  diabetes_history, hypertension_history, family_history

Units: glucose/lipids in mmol/L, BP in mmHg, BMI in kg/m²
gender: 1=male, 0=female
binary fields (smoking, drinking, diabetes_history, etc.): 1=yes, 0=no
"""

from abc import ABC, abstractmethod
from typing import Any

import numpy as np
import pandas as pd

# ═══════════════════════════════════════════════════════════════
# Standard feature definitions
# ═══════════════════════════════════════════════════════════════

STANDARD_FEATURES = [
    "age", "gender", "bmi", "sbp", "dbp",
    "fasting_glucose", "total_cholesterol", "ldl", "hdl", "triglycerides",
    "exercise_days", "smoking", "drinking",
    "diabetes_history", "hypertension_history", "family_history",
]

CONTINUOUS_FEATURES = [
    "age", "bmi", "sbp", "dbp",
    "fasting_glucose", "total_cholesterol", "ldl", "hdl", "triglycerides",
    "exercise_days",
]

CATEGORICAL_FEATURES = [
    "gender", "smoking", "drinking",
    "diabetes_history", "hypertension_history", "family_history",
]

# Valid physiological ranges — values outside are treated as missing
VALID_RANGES = {
    "age": (18, 100),
    "bmi": (15, 60),
    "sbp": (60, 250),
    "dbp": (40, 150),
    "fasting_glucose": (2.0, 35.0),
    "total_cholesterol": (2.0, 20.0),
    "ldl": (0.5, 10.0),
    "hdl": (0.3, 5.0),
    "triglycerides": (0.2, 20.0),
    "exercise_days": (0, 7),
}


# ═══════════════════════════════════════════════════════════════
# Abstract base class
# ═══════════════════════════════════════════════════════════════

class DataSourceBase(ABC):
    """Abstract base for all medical data sources.

    Each concrete subclass (NHANES, CHARLS, MIMIC-IV, ...) must set the
    class-level metadata and implement ``load_raw`` + ``preprocess``.
    The shared ``to_standard_features`` and ``validate`` methods handle
    column mapping, range filtering, and quality reporting.
    """

    name: str           # e.g. "nhanes", "charls", "mimic_iv"
    version: str        # e.g. "2015-2016"
    description: str
    feature_mapping: dict[str, str]  # source column → standard feature name

    # ── abstract interface ─────────────────────────────────

    @abstractmethod
    def load_raw(self, path: str) -> pd.DataFrame:
        """Load raw data from the source format (CSV, XPT, Parquet, etc.)."""
        ...

    @abstractmethod
    def preprocess(self, df: pd.DataFrame) -> pd.DataFrame:
        """Source-specific cleaning: unit conversion, encoding, filtering.

        The returned DataFrame should use source-native column names.
        ``to_standard_features`` will handle renaming afterwards.
        """
        ...

    # ── shared implementation ──────────────────────────────

    def to_standard_features(self, df: pd.DataFrame) -> pd.DataFrame:
        """Map source columns to the standard 16-feature format.

        Steps:
        1. Rename columns via ``feature_mapping``
        2. Validate that all 16 standard features are present
        3. Apply range filtering (out-of-range → NaN)
        4. Return DataFrame containing only STANDARD_FEATURES columns
        """
        # 1. Rename
        rename_map = {
            src: std
            for src, std in self.feature_mapping.items()
            if src in df.columns
        }
        out = df.rename(columns=rename_map)

        # 2. Check completeness
        missing = [f for f in STANDARD_FEATURES if f not in out.columns]
        if missing:
            raise ValueError(
                f"[{self.name}] Missing standard features after mapping: {missing}"
            )

        # Keep only standard columns (+ optional extras like 'label', 'SEQN')
        extra_cols = [c for c in ("label", "SEQN", "subject_id", "stay_id") if c in out.columns]
        out = out[STANDARD_FEATURES + extra_cols].copy()

        # 3. Range filtering — set out-of-range values to NaN
        for col, (lo, hi) in VALID_RANGES.items():
            if col in out.columns:
                out[col] = out[col].where(out[col].between(lo, hi))

        return out

    def validate(self, df: pd.DataFrame) -> dict[str, Any]:
        """Return a data quality report for a standardised DataFrame.

        Returns dict with:
        - row_count: total rows
        - missing_pct: {feature: pct_missing}
        - range_violations: {feature: count_outside_range} (before filtering)
        - dtype_check: {feature: dtype}
        """
        report: dict[str, Any] = {
            "source": self.name,
            "version": self.version,
            "row_count": len(df),
            "missing_pct": {},
            "range_violations": {},
            "dtype_check": {},
        }

        for feat in STANDARD_FEATURES:
            if feat not in df.columns:
                report["missing_pct"][feat] = 100.0
                continue

            col = df[feat]
            n_missing = col.isna().sum()
            report["missing_pct"][feat] = round(100.0 * n_missing / len(df), 2) if len(df) > 0 else 0.0
            report["dtype_check"][feat] = str(col.dtype)

            if feat in VALID_RANGES:
                lo, hi = VALID_RANGES[feat]
                valid = col.dropna()
                violations = ((valid < lo) | (valid > hi)).sum()
                report["range_violations"][feat] = int(violations)

        return report
