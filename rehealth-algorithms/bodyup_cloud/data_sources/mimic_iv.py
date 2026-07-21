# -*- coding: utf-8 -*-
"""
MIMIC-IV (Medical Information Mart for Intensive Care) 数据源
支持 MIMIC-IV demo (100 patients) 和完整版

从 patients + labevents + diagnoses_icd + chartevents 提取:
  - 人口学: age, gender
  - 实验室: glucose, cholesterol, HDL, LDL, triglycerides (mg/dL → mmol/L)
  - 生命体征: SBP, DBP (从 chartevents)
  - 合并症: diabetes, hypertension (从 ICD 诊断码)
  - BMI: 从 omr 表获取 (如果可用)
  - 标签: CVD (ICD 心脏病/中风诊断码)

不可用字段: smoking, drinking, exercise_days, family_history → 默认值
"""

from pathlib import Path
from typing import Any

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

# Lab itemids
_LAB_ITEMS = {
    50809: ("glucose", _GLUCOSE_FACTOR),
    50931: ("glucose", _GLUCOSE_FACTOR),
    50907: ("total_cholesterol", _CHOLESTEROL_FACTOR),
    50904: ("hdl", _CHOLESTEROL_FACTOR),
    50905: ("ldl", _CHOLESTEROL_FACTOR),
    51000: ("triglycerides", _TRIGLYCERIDES_FACTOR),
}

# Chartevents itemids for vital signs
_BP_ITEMS = {
    220179: "sbp",   # non-invasive systolic
    220050: "sbp",   # arterial systolic
    220180: "dbp",   # non-invasive diastolic
    220051: "dbp",   # arterial diastolic
}

# ICD codes for comorbidities
_DIABETES_ICD9 = ["250"]
_DIABETES_ICD10 = ["E10", "E11", "E12", "E13", "E14"]
_HYPERTENSION_ICD9 = ["401", "402", "403", "404", "405"]
_HYPERTENSION_ICD10 = ["I10", "I11", "I12", "I13", "I15"]

# CVD label ICD codes
_CVD_ICD9 = ["410", "411", "412", "413", "414", "428", "430", "431", "432", "433", "434", "436"]
_CVD_ICD10 = ["I20", "I21", "I22", "I23", "I24", "I25", "I50", "I60", "I61", "I62", "I63", "I64"]


def _icd_startswith(code: str, prefixes: list[str]) -> bool:
    return any(code.startswith(p) for p in prefixes)


class MIMICIVSource(DataSourceBase):
    """MIMIC-IV data source (supports demo and full datasets)."""

    name = "mimic_iv"
    version = "2.2"
    description = "MIMIC-IV 2.2: ICU data for CVD risk model supplementation."

    feature_mapping = {col: col for col in STANDARD_FEATURES}

    def load_raw(self, path: str) -> pd.DataFrame:
        data_dir = Path(path)

        # Auto-detect nested directory (demo zip extracts to a subdirectory)
        subdirs = [d for d in data_dir.iterdir() if d.is_dir() and "mimic" in d.name.lower()]
        if subdirs:
            data_dir = subdirs[0]

        hosp = data_dir / "hosp"
        icu = data_dir / "icu"

        if not hosp.exists():
            raise FileNotFoundError(f"MIMIC-IV hosp/ directory not found in {data_dir}")

        patients = pd.read_csv(hosp / "patients.csv.gz")
        admissions = pd.read_csv(hosp / "admissions.csv.gz")
        labevents = pd.read_csv(hosp / "labevents.csv.gz")
        diagnoses = pd.read_csv(hosp / "diagnoses_icd.csv.gz")

        chartevents = None
        if (icu / "chartevents.csv.gz").exists():
            chartevents = pd.read_csv(icu / "chartevents.csv.gz")

        omr = None
        if (hosp / "omr.csv.gz").exists():
            omr = pd.read_csv(hosp / "omr.csv.gz")

        return {
            "_patients": patients,
            "_admissions": admissions,
            "_labevents": labevents,
            "_diagnoses": diagnoses,
            "_chartevents": chartevents,
            "_omr": omr,
        }

    def preprocess(self, tables) -> pd.DataFrame:
        patients = tables["_patients"]
        admissions = tables["_admissions"]
        labevents = tables["_labevents"]
        diagnoses = tables["_diagnoses"]
        chartevents = tables.get("_chartevents")
        omr = tables.get("_omr")

        # One row per subject
        subjects = patients[["subject_id", "gender", "anchor_age"]].copy()
        subjects["age"] = subjects["anchor_age"]
        subjects["gender"] = (subjects["gender"] == "M").astype(int)

        # Labs: aggregate per subject (median of all values)
        lab_filtered = labevents[labevents["itemid"].isin(_LAB_ITEMS.keys())].copy()
        lab_filtered["feature"] = lab_filtered["itemid"].map(lambda x: _LAB_ITEMS[x][0])
        lab_filtered["factor"] = lab_filtered["itemid"].map(lambda x: _LAB_ITEMS[x][1])
        lab_filtered["value_converted"] = pd.to_numeric(lab_filtered["valuenum"], errors="coerce") * lab_filtered["factor"]

        lab_pivot = (
            lab_filtered.groupby(["subject_id", "feature"])["value_converted"]
            .median()
            .unstack(fill_value=np.nan)
        )

        subjects = subjects.merge(lab_pivot, on="subject_id", how="left")

        # Vital signs from chartevents
        if chartevents is not None and not chartevents.empty:
            # Map to ICU stays → subjects
            bp_items = chartevents[chartevents["itemid"].isin(_BP_ITEMS.keys())].copy()
            bp_items["signal"] = bp_items["itemid"].map(_BP_ITEMS)
            bp_items["valuenum"] = pd.to_numeric(bp_items["valuenum"], errors="coerce")

            bp_agg = bp_items.groupby(["subject_id", "signal"])["valuenum"].median().unstack(fill_value=np.nan)
            subjects = subjects.merge(bp_agg, on="subject_id", how="left")

        # BMI from omr
        if omr is not None and not omr.empty:
            bmi_rows = omr[omr["result_name"].str.lower().str.contains("bmi", na=False)]
            if not bmi_rows.empty:
                bmi_rows = bmi_rows.copy()
                bmi_rows["result_value"] = pd.to_numeric(bmi_rows["result_value"], errors="coerce")
                bmi_per_subject = bmi_rows.groupby("subject_id")["result_value"].median()
                bmi_per_subject.name = "bmi"
                subjects = subjects.merge(bmi_per_subject, on="subject_id", how="left")

        # ICD diagnoses → comorbidities + label
        diag = diagnoses[["subject_id", "icd_code", "icd_version"]].copy()

        def check_icd(row, icd9_prefixes, icd10_prefixes):
            code = str(row["icd_code"])
            ver = int(row["icd_version"])
            if ver == 9:
                return _icd_startswith(code, icd9_prefixes)
            return _icd_startswith(code, icd10_prefixes)

        diag["is_diabetes"] = diag.apply(lambda r: check_icd(r, _DIABETES_ICD9, _DIABETES_ICD10), axis=1)
        diag["is_hypertension"] = diag.apply(lambda r: check_icd(r, _HYPERTENSION_ICD9, _HYPERTENSION_ICD10), axis=1)
        diag["is_cvd"] = diag.apply(lambda r: check_icd(r, _CVD_ICD9, _CVD_ICD10), axis=1)

        comorbid = diag.groupby("subject_id").agg(
            diabetes_history=("is_diabetes", "max"),
            hypertension_history=("is_hypertension", "max"),
            label=("is_cvd", "max"),
        ).astype(int)

        subjects = subjects.merge(comorbid, on="subject_id", how="left")

        # Fill unavailable lifestyle fields
        for col in ["smoking", "drinking", "exercise_days", "family_history"]:
            if col not in subjects.columns:
                subjects[col] = 0
        if "bmi" not in subjects.columns:
            subjects["bmi"] = np.nan
        if "sbp" not in subjects.columns:
            subjects["sbp"] = np.nan
        if "dbp" not in subjects.columns:
            subjects["dbp"] = np.nan

        # Build standard features
        feat = pd.DataFrame()
        for col in STANDARD_FEATURES:
            feat[col] = subjects.get(col, 0)
        feat["label"] = subjects.get("label", 0).fillna(0).astype(int)
        feat["subject_id"] = subjects["subject_id"]

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
