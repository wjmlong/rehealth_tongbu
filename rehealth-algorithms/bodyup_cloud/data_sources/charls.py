# -*- coding: utf-8 -*-
"""
CHARLS (China Health and Retirement Longitudinal Study) 数据源
数据来源: figshare CHARLS.csv (gbk 编码, mg/dL 单位)

列映射 (CSV → 标准16特征):
  age → age, gender → gender, bmi → bmi,
  systo → sbp, diasto → dbp,
  bl_glu → fasting_glucose (×0.0555),
  bl_cho → total_cholesterol (×0.02586),
  bl_ldl → ldl (×0.02586), bl_hdl → hdl (×0.02586),
  bl_tg → triglycerides (×0.01129),
  smokev → smoking, drinkev → drinking,
  diabe → diabetes_history, hibpe → hypertension_history,
  hearte/stroke → label (CVD event)

注意: CHARLS 缺少 exercise_days 和 family_history，用默认值填充
"""

from pathlib import Path

import numpy as np
import pandas as pd

from .base import (
    CATEGORICAL_FEATURES,
    CONTINUOUS_FEATURES,
    STANDARD_FEATURES,
    DataSourceBase,
)

_GLUCOSE_FACTOR = 0.0555
_CHOLESTEROL_FACTOR = 0.02586
_TRIGLYCERIDES_FACTOR = 0.01129


class CHARLSSource(DataSourceBase):
    """CHARLS data source — Chinese population calibration."""

    name = "charls"
    version = "2018"
    description = (
        "CHARLS 2018: China Health and Retirement Longitudinal Study. "
        "Chinese population calibration for CVD risk model. Age 35+."
    )

    feature_mapping = {col: col for col in STANDARD_FEATURES}

    def load_raw(self, path: str) -> pd.DataFrame:
        data_dir = Path(path)
        csv_path = data_dir / "CHARLS.csv"
        if not csv_path.exists():
            raise FileNotFoundError(
                f"CHARLS.csv not found in {data_dir}. "
                "Download from figshare or register at https://charls.pku.edu.cn/"
            )
        return pd.read_csv(str(csv_path), encoding="gbk")

    def preprocess(self, df: pd.DataFrame) -> pd.DataFrame:
        feat = pd.DataFrame()

        feat["age"] = df["age"]
        feat["gender"] = df["gender"].astype(int)
        feat["bmi"] = df["bmi"]

        feat["sbp"] = df["systo"]
        feat["dbp"] = df["diasto"]

        # mg/dL → mmol/L
        feat["fasting_glucose"] = df["bl_glu"] * _GLUCOSE_FACTOR
        feat["total_cholesterol"] = df["bl_cho"] * _CHOLESTEROL_FACTOR
        feat["ldl"] = df["bl_ldl"] * _CHOLESTEROL_FACTOR
        feat["hdl"] = df["bl_hdl"] * _CHOLESTEROL_FACTOR
        feat["triglycerides"] = df["bl_tg"] * _TRIGLYCERIDES_FACTOR

        feat["smoking"] = df["smokev"].astype(int)
        feat["drinking"] = df["drinkev"].astype(int)

        # CHARLS 没有 exercise_days 直接字段; totmet 是代谢当量
        # 从 totmet 粗略推算: >600 MET-min/week ≈ 3天, >1200 ≈ 5天
        if "totmet" in df.columns:
            met = pd.to_numeric(df["totmet"], errors="coerce").fillna(0)
            feat["exercise_days"] = np.where(met >= 1200, 5, np.where(met >= 600, 3, np.where(met > 0, 1, 0)))
        else:
            feat["exercise_days"] = 0

        feat["diabetes_history"] = df["diabe"].astype(int)
        feat["hypertension_history"] = df["hibpe"].astype(int)

        # CHARLS 无 family_history
        feat["family_history"] = 0

        # Label: hearte OR stroke
        feat["label"] = ((df["hearte"] == 1) | (df["stroke"] == 1)).astype(int)

        # Age filter
        feat = feat[(feat["age"] >= 18) & (feat["age"] <= 100)].copy()

        # Imputation
        for col in CONTINUOUS_FEATURES:
            if col in feat.columns:
                feat[col] = feat[col].fillna(feat[col].median())
        for col in CATEGORICAL_FEATURES:
            if col in feat.columns:
                mode_val = feat[col].mode()
                if len(mode_val) > 0:
                    feat[col] = feat[col].fillna(mode_val.iloc[0]).astype(int)

        feat = feat.dropna(subset=["label"])
        return feat
