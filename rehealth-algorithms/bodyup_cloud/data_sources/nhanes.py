# -*- coding: utf-8 -*-
"""
NHANES (National Health and Nutrition Examination Survey) 数据源
支持多周期: 2015-2016, 2017-2018, 2021-2023

单位换算 (mg/dL → mmol/L):
  glucose:     ×0.0555
  cholesterol: ×0.02586 (TC, HDL, LDL)
  triglycerides: ×0.01129
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

_HEART_COLS = ["MCQ160B", "MCQ160C", "MCQ160D", "MCQ160E"]
_STROKE_COL = "MCQ160F"

# Per-cycle file config: maps standard table name → (filename, {column overrides})
CYCLE_CONFIG = {
    "2015-2016": {
        "suffix": "I",
        "files": [
            "DEMO_I.xpt", "BMX_I.xpt", "BPX_I.xpt", "BPQ_I.xpt",
            "GLU_I.xpt", "TCHOL_I.xpt", "HDL_I.xpt", "TRIGLY_I.xpt",
            "SMQ_I.xpt", "ALQ_I.xpt", "DIQ_I.xpt", "PAQ_I.xpt", "MCQ_I.xpt",
        ],
        "bp_sbp": "BPXSY1",
        "bp_dbp": "BPXDI1",
        "has_exercise_days": True,
        "has_family_history": True,
    },
    "2017-2018": {
        "suffix": "J",
        "files": [
            "DEMO_J.xpt", "BMX_J.xpt", "BPX_J.xpt", "BPQ_J.xpt",
            "GLU_J.xpt", "TCHOL_J.xpt", "HDL_J.xpt", "TRIGLY_J.xpt",
            "SMQ_J.xpt", "ALQ_J.xpt", "DIQ_J.xpt", "PAQ_J.xpt", "MCQ_J.xpt",
        ],
        "bp_sbp": "BPXSY1",
        "bp_dbp": "BPXDI1",
        "has_exercise_days": True,
        "has_family_history": True,
    },
    "2021-2023": {
        "suffix": "L",
        "files": [
            "DEMO_L.xpt", "BMX_L.xpt", "BPXO_L.xpt", "BPQ_L.xpt",
            "GLU_L.xpt", "TCHOL_L.xpt", "HDL_L.xpt", "TRIGLY_L.xpt",
            "SMQ_L.xpt", "ALQ_L.xpt", "DIQ_L.xpt", "PAQ_L.xpt", "MCQ_L.xpt",
        ],
        "bp_sbp": "BPXOSY1",
        "bp_dbp": "BPXODI1",
        "has_exercise_days": False,
        "has_family_history": False,
    },
}


class NHANESSource(DataSourceBase):
    """NHANES data source supporting multiple survey cycles."""

    name = "nhanes"
    version = "multi-cycle"
    description = "NHANES multi-cycle: 2015-2016, 2017-2018, 2021-2023"

    feature_mapping = {col: col for col in STANDARD_FEATURES}

    def __init__(self, cycles: list[str] | None = None):
        self.cycles = cycles or list(CYCLE_CONFIG.keys())

    def load_raw(self, path: str) -> pd.DataFrame:
        """Load and merge NHANES XPT files from *path*.

        Supports two directory layouts:
          1. Multi-cycle: path/{cycle}/DEMO_{X}.xpt (e.g. data/nhanes/2017-2018/)
          2. Flat: path/DEMO_I.xpt (single cycle, auto-detected)
        """
        data_dir = Path(path)
        if not data_dir.is_dir():
            raise FileNotFoundError(f"NHANES data directory not found: {data_dir}")

        all_frames = []

        for cycle_name in self.cycles:
            cfg = CYCLE_CONFIG[cycle_name]

            # Try multi-cycle layout first, then flat
            cycle_dir = data_dir / cycle_name
            if not cycle_dir.is_dir():
                # Check if files are directly in data_dir (flat layout)
                test_file = data_dir / cfg["files"][0]
                if test_file.exists():
                    cycle_dir = data_dir
                else:
                    print(f"  [skip] {cycle_name}: directory not found")
                    continue

            # Check all files exist
            missing = [f for f in cfg["files"] if not (cycle_dir / f).exists()]
            if missing:
                # Also try uppercase .XPT extension
                missing2 = [f for f in missing if not (cycle_dir / f.replace(".xpt", ".XPT")).exists()]
                if missing2:
                    print(f"  [skip] {cycle_name}: missing {missing2}")
                    continue

            print(f"  Loading {cycle_name} from {cycle_dir}...")
            tables = {}
            for fname in cfg["files"]:
                fpath = cycle_dir / fname
                if not fpath.exists():
                    fpath = cycle_dir / fname.replace(".xpt", ".XPT")
                tables[fname.split("_")[0]] = pd.read_sas(
                    str(fpath), format="xport", encoding="latin-1"
                )

            # Merge on SEQN
            demo_key = f"DEMO"
            demo = tables[demo_key]
            df = demo[["SEQN"]].copy()
            for key, tbl in tables.items():
                if key == demo_key:
                    df = pd.merge(df, demo, on="SEQN", how="left")
                else:
                    df = pd.merge(df, tbl, on="SEQN", how="left")

            df["_cycle"] = cycle_name
            df["_bp_sbp_col"] = cfg["bp_sbp"]
            df["_bp_dbp_col"] = cfg["bp_dbp"]
            df["_has_exercise_days"] = cfg["has_exercise_days"]
            df["_has_family_history"] = cfg["has_family_history"]
            all_frames.append(df)

        if not all_frames:
            raise FileNotFoundError("No NHANES cycle data found")

        combined = pd.concat(all_frames, ignore_index=True)
        print(f"  Total raw rows: {len(combined)} from {len(all_frames)} cycle(s)")
        return combined

    def preprocess(self, df: pd.DataFrame) -> pd.DataFrame:
        """Multi-cycle preprocessing with per-cycle column handling."""
        feat = pd.DataFrame()
        feat["SEQN"] = df["SEQN"]

        # Demographics
        feat["age"] = df["RIDAGEYR"]
        feat["gender"] = (df["RIAGENDR"] == 1).astype(int)

        # BMI
        feat["bmi"] = df["BMXBMI"]

        # Blood pressure — per-cycle column names
        sbp_vals = pd.Series(np.nan, index=df.index)
        dbp_vals = pd.Series(np.nan, index=df.index)
        for col in ["BPXSY1", "BPXOSY1"]:
            if col in df.columns:
                mask = sbp_vals.isna() & df[col].notna()
                sbp_vals[mask] = df.loc[mask, col]
        for col in ["BPXDI1", "BPXODI1"]:
            if col in df.columns:
                mask = dbp_vals.isna() & df[col].notna()
                dbp_vals[mask] = df.loc[mask, col]
        feat["sbp"] = sbp_vals
        feat["dbp"] = dbp_vals

        # Lab results (unit conversion mg/dL → mmol/L)
        feat["fasting_glucose"] = df["LBXGLU"] * _GLUCOSE_FACTOR if "LBXGLU" in df.columns else np.nan
        feat["total_cholesterol"] = df["LBXTC"] * _CHOLESTEROL_FACTOR if "LBXTC" in df.columns else np.nan
        feat["hdl"] = df["LBDHDD"] * _CHOLESTEROL_FACTOR if "LBDHDD" in df.columns else np.nan
        feat["ldl"] = df["LBDLDL"] * _CHOLESTEROL_FACTOR if "LBDLDL" in df.columns else np.nan
        feat["triglycerides"] = df["LBXTR"] * _TRIGLYCERIDES_FACTOR if "LBXTR" in df.columns else np.nan

        # Smoking
        feat["smoking"] = 0
        if "SMQ040" in df.columns:
            feat["smoking"] = df["SMQ040"].isin([1, 2]).astype(int)

        # Drinking
        feat["drinking"] = 0
        if "ALQ130" in df.columns:
            feat["drinking"] = (df["ALQ130"] >= 1).astype(int)

        # Exercise days — different PAQ structure across cycles
        feat["exercise_days"] = 0
        if "PAD660" in df.columns and "PAD675" in df.columns:
            vig = pd.to_numeric(df["PAD660"], errors="coerce").clip(0, 7).fillna(0)
            mod = pd.to_numeric(df["PAD675"], errors="coerce").clip(0, 7).fillna(0)
            has_old_paq = df["PAD660"].notna() | df["PAD675"].notna()
            feat.loc[has_old_paq, "exercise_days"] = vig[has_old_paq].combine(mod[has_old_paq], max).astype(int)
        if "PAD680" in df.columns:
            # 2021-2023: PAD680 = sedentary minutes/day. Invert: <300min → ~3 days active
            sed = pd.to_numeric(df["PAD680"], errors="coerce")
            has_new_paq = sed.notna() & (feat["exercise_days"] == 0)
            feat.loc[has_new_paq, "exercise_days"] = np.where(
                sed[has_new_paq] < 180, 5,
                np.where(sed[has_new_paq] < 360, 3,
                np.where(sed[has_new_paq] < 540, 1, 0))
            )

        # Diabetes history
        feat["diabetes_history"] = 0
        if "DIQ010" in df.columns:
            feat["diabetes_history"] = (df["DIQ010"] == 1).astype(int)

        # Hypertension history
        feat["hypertension_history"] = 0
        if "BPQ020" in df.columns:
            feat["hypertension_history"] = (df["BPQ020"] == 1).astype(int)

        # Family history (MCQ300B/C — not available in 2021-2023)
        feat["family_history"] = 0
        for col in ("MCQ300B", "MCQ300C"):
            if col in df.columns:
                feat["family_history"] = feat["family_history"] | (df[col] == 1).astype(int)

        # Label: CVD event (heart disease OR stroke)
        label = pd.Series(0, index=df.index)
        for col in _HEART_COLS:
            if col in df.columns:
                label = label | (df[col] == 1).astype(int)
        if _STROKE_COL in df.columns:
            label = label | (df[_STROKE_COL] == 1).astype(int)
        feat["label"] = label.values

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
