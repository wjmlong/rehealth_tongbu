"""Standardized PSM/RWE analysis for insurance outcome studies.

This module is intentionally independent from the legacy demo attribution
classes.  It accepts a frozen, one-row-per-member cohort and returns a
reproducible analysis object that can be consumed by the settlement report
generator.

The engine estimates a one-to-one ATT on the matched pairs.  It also reports
an AIPW/DR ATT as a secondary diagnostic; the matched-pair ATT is the primary
settlement estimand because its confidence interval and p-value are computed
from the same matched-pair differences.
"""

from __future__ import annotations

from dataclasses import dataclass, field, replace
from datetime import datetime, timezone
import hashlib
import json
from typing import Any, Dict, Iterable, List, Mapping, Optional, Sequence, Tuple

import numpy as np
from scipy import stats
from scipy.spatial import cKDTree
from sklearn.linear_model import LogisticRegression, Ridge
from sklearn.pipeline import make_pipeline
from sklearn.preprocessing import StandardScaler


CVD_BASELINE_FEATURES: Tuple[str, ...] = (
    "age",
    "gender",
    "bmi",
    "sbp",
    "dbp",
    "fasting_glucose",
    "total_cholesterol",
    "ldl",
    "hdl",
    "triglycerides",
    "exercise_days",
    "smoking",
    "drinking",
    "diabetes_history",
    "hypertension_history",
    "family_history",
)

LEGACY_MATCHING_FEATURES: Tuple[str, ...] = (
    "age_bracket",
    "bmi_level_encoded",
    "bp_baseline_grade_encoded",
    "activity_level_encoded",
    "gender_encoded",
    "season_sin",
    "season_cos",
)


@dataclass(frozen=True)
class PSMConfig:
    """Analysis protocol for one PSM study."""

    matching_features: Tuple[str, ...] = CVD_BASELINE_FEATURES
    outcome_field: str = "outcome"
    outcome_type: str = "continuous"  # continuous or binary
    outcome_direction: str = "lower_is_better"
    caliper: str | float = "auto"
    n_bootstrap: int = 1000
    confidence_level: float = 0.95
    min_group_size: int = 30
    min_matched_pairs: int = 30
    balance_threshold: float = 0.80
    seed: int = 42
    model_version: str = "unspecified"
    strata_field: Optional[str] = None

    @classmethod
    def from_mapping(cls, values: Optional[Mapping[str, Any]] = None) -> "PSMConfig":
        values = dict(values or {})
        if "matching_features" in values:
            values["matching_features"] = tuple(values["matching_features"])
        allowed = {field.name for field in cls.__dataclass_fields__.values()}
        return cls(**{key: value for key, value in values.items() if key in allowed})


@dataclass
class PSMEngine:
    """Run a deterministic one-to-one propensity-score matching analysis."""

    config: PSMConfig = field(default_factory=PSMConfig)

    def __init__(self, config: Optional[Mapping[str, Any] | PSMConfig] = None):
        self.config = config if isinstance(config, PSMConfig) else PSMConfig.from_mapping(config)

    def run(
        self,
        records: Sequence[Mapping[str, Any]],
        config: Optional[Mapping[str, Any] | PSMConfig] = None,
    ) -> Dict[str, Any]:
        """Analyze a frozen cohort.

        Each record must contain ``Z`` (0/1), ``features`` with all configured
        baseline covariates, and either the configured outcome field or the
        legacy ``delta_Y`` field.  Post-treatment variables must not be put in
        ``features``.
        """

        cfg = config if isinstance(config, PSMConfig) else (
            PSMConfig.from_mapping(config) if config is not None else self.config
        )
        records = list(records)
        if cfg.strata_field:
            return self._run_with_strata(records, cfg)
        return self._run_core(records, cfg)

    # ``estimate`` is the spelling used by the older API router.
    estimate = run

    def _run_with_strata(
        self, records: List[Mapping[str, Any]], cfg: PSMConfig
    ) -> Dict[str, Any]:
        overall_cfg = replace(cfg, strata_field=None)
        overall = self._run_core(records, overall_cfg)
        strata: Dict[str, Any] = {}
        groups: Dict[str, List[Mapping[str, Any]]] = {}
        for record in records:
            key = str(record.get(cfg.strata_field, "__missing__"))
            groups.setdefault(key, []).append(record)
        for key, group in sorted(groups.items()):
            strata[key] = self._run_core(group, overall_cfg)
        overall["stratified_results"] = strata
        overall["stratification"] = {
            "field": cfg.strata_field,
            "n_strata": len(strata),
        }
        return overall

    def _run_core(
        self, records: List[Mapping[str, Any]], cfg: PSMConfig
    ) -> Dict[str, Any]:
        validation = self._validate_records(records, cfg)
        if validation:
            return {
                "status": "invalid_input",
                "analysis_status": "draft",
                "engine_version": "psm-rwe-draft-1.0",
                "errors": validation,
                "n_total": len(records),
            }

        X = np.asarray(
            [[float(record["features"][feature]) for feature in cfg.matching_features]
             for record in records],
            dtype=float,
        )
        Z = np.asarray([int(record["Z"]) for record in records], dtype=int)
        Y = np.asarray([self._get_outcome(record, cfg) for record in records], dtype=float)
        n_total = len(records)
        n_treated = int(Z.sum())
        n_control = n_total - n_treated

        propensity_model = make_pipeline(
            StandardScaler(),
            LogisticRegression(C=1.0, solver="lbfgs", max_iter=1000, random_state=cfg.seed),
        )
        propensity_model.fit(X, Z)
        propensity = np.clip(propensity_model.predict_proba(X)[:, 1], 1e-4, 1 - 1e-4)
        logit_propensity = np.log(propensity / (1 - propensity))

        support_lower = max(float(propensity[Z == 1].min()), float(propensity[Z == 0].min()))
        support_upper = min(float(propensity[Z == 1].max()), float(propensity[Z == 0].max()))
        in_support = (propensity >= support_lower) & (propensity <= support_upper)
        overlap = self._overlap_diagnostics(propensity, Z, support_lower, support_upper)
        if support_lower > support_upper:
            return self._failure_result(
                records, Z, "insufficient_overlap", "No common propensity-score support."
            ) | {"overlap": overlap}

        caliper = self._resolve_caliper(logit_propensity, cfg.caliper)
        pairs = self._match(
            logit_propensity,
            Z,
            np.flatnonzero(in_support),
            caliper,
        )
        if len(pairs) < cfg.min_matched_pairs:
            return self._failure_result(
                records,
                Z,
                "insufficient_matches",
                f"Only {len(pairs)} pairs matched; {cfg.min_matched_pairs} required.",
            ) | {
                "caliper": caliper,
                "overlap": overlap,
            }

        idx_t = np.asarray([pair[0] for pair in pairs], dtype=int)
        idx_c = np.asarray([pair[1] for pair in pairs], dtype=int)
        pair_differences = Y[idx_t] - Y[idx_c]
        att = float(pair_differences.mean())
        ci_lower, ci_upper = self._bootstrap_ci(pair_differences, cfg)
        p_value, p_value_method = self._p_value(pair_differences, cfg.outcome_type)
        balance = self._balance_diagnostics(X, Z, idx_t, idx_c, cfg.matching_features)
        n_balanced = sum(item["balanced"] for item in balance.values())
        balance_rate = n_balanced / max(len(balance), 1)
        dre_att = self._aipw_att(X, Z, Y, propensity, cfg.outcome_type)

        quality_gates = {
            "minimum_sample": n_total >= cfg.min_group_size,
            "minimum_matched_pairs": len(pairs) >= cfg.min_matched_pairs,
            "common_support": support_lower < support_upper,
            "balance": balance_rate >= cfg.balance_threshold,
            "analysis_pass": (
                n_total >= cfg.min_group_size
                and len(pairs) >= cfg.min_matched_pairs
                and support_lower <= support_upper
                and balance_rate >= cfg.balance_threshold
            ),
        }
        snapshot_hash = self.snapshot_hash(records, cfg)
        return {
            "status": "success",
            "analysis_status": "draft",
            "estimand": "ATT",
            "n_total": n_total,
            "n_treated": n_treated,
            "n_control": n_control,
            "n_matched_pairs": len(pairs),
            "matching_rate_treated": len(pairs) / max(n_treated, 1),
            "matching_rate_members": 2 * len(pairs) / max(n_total, 1),
            "matching_features": list(cfg.matching_features),
            "outcome_field": cfg.outcome_field,
            "outcome_type": cfg.outcome_type,
            "outcome_direction": cfg.outcome_direction,
            "confidence_level": cfg.confidence_level,
            "n_bootstrap": cfg.n_bootstrap,
            "seed": cfg.seed,
            "propensity_model": "standardized logistic regression",
            "propensity_score": {
                "min": float(propensity.min()),
                "max": float(propensity.max()),
                "treated_mean": float(propensity[Z == 1].mean()),
                "control_mean": float(propensity[Z == 0].mean()),
            },
            "caliper": caliper,
            "caliper_scale": "logit_propensity",
            "overlap": overlap,
            "pairs": [(int(t), int(c)) for t, c in pairs],
            "att": att,
            "ci_lower": ci_lower,
            "ci_upper": ci_upper,
            "ci_method": "percentile bootstrap of matched-pair differences",
            "p_value": p_value,
            "p_value_method": p_value_method,
            "is_significant": bool(p_value < (1 - cfg.confidence_level)),
            "matched_outcome": {
                "treated_mean": float(Y[idx_t].mean()),
                "control_mean": float(Y[idx_c].mean()),
                "difference": att,
            },
            "dre_att": dre_att,
            "balance": balance,
            "balance_rate": balance_rate,
            "quality_gates": quality_gates,
            "snapshot_hash": snapshot_hash,
            "engine_version": "psm-rwe-draft-1.0",
            "limitations": [
                "观察性研究不能排除未测量混杂。",
                "主要 ATT 的置信区间和 p 值基于匹配对差值；DRE 仅作为辅助估计。",
                "当前实现未提供经过验证的 Rosenbaum bounds；不得将 Γ 当作正式敏感性结论。",
                "结算金额只能在合同财务口径和有效人数核验后使用。",
            ],
        }

    @staticmethod
    def snapshot_hash(
        records: Sequence[Mapping[str, Any]], cfg: Optional[PSMConfig] = None
    ) -> str:
        payload = {
            "records": records,
            "config": (cfg.__dict__ if cfg else {}),
        }
        canonical = json.dumps(
            payload, sort_keys=True, separators=(",", ":"), ensure_ascii=False, default=str
        ).encode("utf-8")
        return hashlib.sha256(canonical).hexdigest()

    @staticmethod
    def _get_outcome(record: Mapping[str, Any], cfg: PSMConfig) -> float:
        if cfg.outcome_field in record:
            return float(record[cfg.outcome_field])
        if cfg.outcome_field == "outcome" and "delta_Y" in record:
            return float(record["delta_Y"])
        raise ValueError(f"Missing outcome field: {cfg.outcome_field}")

    @staticmethod
    def _validate_records(
        records: Sequence[Mapping[str, Any]], cfg: PSMConfig
    ) -> List[str]:
        errors: List[str] = []
        if len(records) < cfg.min_group_size:
            errors.append(f"At least {cfg.min_group_size} records are required.")
        if cfg.outcome_type not in {"continuous", "binary"}:
            errors.append("outcome_type must be 'continuous' or 'binary'.")
        if cfg.outcome_direction not in {"lower_is_better", "higher_is_better"}:
            errors.append("outcome_direction must be lower_is_better or higher_is_better.")
        if not 0 < cfg.confidence_level < 1:
            errors.append("confidence_level must be between 0 and 1.")
        if cfg.n_bootstrap < 100:
            errors.append("n_bootstrap must be at least 100 for a draft confidence interval.")
        if not 0 < cfg.balance_threshold <= 1:
            errors.append("balance_threshold must be in (0, 1].")

        ids = set()
        treatments = []
        outcomes = []
        for index, record in enumerate(records):
            if "Z" not in record or int(record.get("Z", -1)) not in {0, 1}:
                errors.append(f"Row {index} has invalid Z; expected 0 or 1.")
            treatments.append(int(record.get("Z", 0)))
            features = record.get("features")
            if not isinstance(features, Mapping):
                errors.append(f"Row {index} is missing features mapping.")
                continue
            missing = [name for name in cfg.matching_features if name not in features]
            if missing:
                errors.append(f"Row {index} missing baseline features: {', '.join(missing)}.")
            for name in cfg.matching_features:
                if name in features:
                    try:
                        if not np.isfinite(float(features[name])):
                            errors.append(f"Row {index} feature {name} is not finite.")
                    except (TypeError, ValueError):
                        errors.append(f"Row {index} feature {name} is not numeric.")
            try:
                outcome = PSMEngine._get_outcome(record, cfg)
                if not np.isfinite(outcome):
                    errors.append(f"Row {index} outcome is not finite.")
                outcomes.append(outcome)
            except (TypeError, ValueError) as exc:
                errors.append(f"Row {index}: {exc}")
            record_id = record.get("device_id", record.get("member_id", index))
            if record_id in ids:
                errors.append(f"Duplicate member identifier: {record_id}")
            ids.add(record_id)

        if treatments and (sum(treatments) < 10 or len(treatments) - sum(treatments) < 10):
            errors.append("At least 10 treated and 10 control records are required.")
        if cfg.outcome_type == "binary" and outcomes and any(value not in {0.0, 1.0} for value in outcomes):
            errors.append("Binary outcomes must be coded as 0 or 1.")
        return sorted(set(errors))

    @staticmethod
    def _resolve_caliper(logit_propensity: np.ndarray, caliper: str | float) -> float:
        if caliper != "auto":
            value = float(caliper)
            if value <= 0:
                raise ValueError("caliper must be positive")
            return value
        value = 0.2 * float(np.std(logit_propensity, ddof=1))
        return max(value, 1e-6)

    @staticmethod
    def _match(
        logit_propensity: np.ndarray,
        Z: np.ndarray,
        eligible_idx: np.ndarray,
        caliper: float,
    ) -> List[Tuple[int, int]]:
        treated = [int(index) for index in eligible_idx if Z[index] == 1]
        control = [int(index) for index in eligible_idx if Z[index] == 0]
        if not treated or not control:
            return []

        tree = cKDTree(logit_propensity[np.asarray(control)].reshape(-1, 1))
        candidates: List[Tuple[float, int, int]] = []
        for treated_index in treated:
            local_controls = tree.query_ball_point(
                [logit_propensity[treated_index]], caliper
            )
            for local_control in local_controls:
                control_index = control[int(local_control)]
                distance = abs(logit_propensity[treated_index] - logit_propensity[control_index])
                candidates.append((float(distance), treated_index, control_index))

        candidates.sort(key=lambda item: (item[0], item[1], item[2]))
        used_treated: set[int] = set()
        used_control: set[int] = set()
        pairs: List[Tuple[int, int]] = []
        for distance, treated_index, control_index in candidates:
            if distance > caliper:
                break
            if treated_index in used_treated or control_index in used_control:
                continue
            used_treated.add(treated_index)
            used_control.add(control_index)
            pairs.append((treated_index, control_index))
        return pairs

    @staticmethod
    def _overlap_diagnostics(
        propensity: np.ndarray,
        Z: np.ndarray,
        lower: float,
        upper: float,
    ) -> Dict[str, Any]:
        treated = propensity[Z == 1]
        control = propensity[Z == 0]
        return {
            "common_support_lower": lower,
            "common_support_upper": upper,
            "support_width": upper - lower,
            "outside_treated": int(np.sum((treated < lower) | (treated > upper))),
            "outside_control": int(np.sum((control < lower) | (control > upper))),
            "quality": (
                "good" if upper - lower >= 0.5
                else "fair" if upper - lower >= 0.2
                else "exact" if np.isclose(upper, lower)
                else "poor"
            ),
        }

    @staticmethod
    def _balance_diagnostics(
        X: np.ndarray,
        Z: np.ndarray,
        idx_t: np.ndarray,
        idx_c: np.ndarray,
        features: Sequence[str],
    ) -> Dict[str, Dict[str, Any]]:
        result: Dict[str, Dict[str, Any]] = {}
        for column, feature in enumerate(features):
            before_t = X[Z == 1, column]
            before_c = X[Z == 0, column]
            after_t = X[idx_t, column]
            after_c = X[idx_c, column]
            smd_before = PSMEngine._smd(before_t, before_c)
            smd_after = PSMEngine._smd(after_t, after_c)
            result[feature] = {
                "mean_treated_before": float(before_t.mean()),
                "mean_control_before": float(before_c.mean()),
                "smd_before": smd_before,
                "mean_treated_after": float(after_t.mean()),
                "mean_control_after": float(after_c.mean()),
                "smd_after": smd_after,
                "smd": smd_after,
                "balanced": bool(smd_after < 0.1),
            }
        return result

    @staticmethod
    def _smd(treated: np.ndarray, control: np.ndarray) -> float:
        pooled = np.sqrt((np.var(treated, ddof=1) + np.var(control, ddof=1)) / 2)
        if pooled == 0:
            return 0.0 if np.isclose(np.mean(treated), np.mean(control)) else float("inf")
        return float(abs(np.mean(treated) - np.mean(control)) / pooled)

    @staticmethod
    def _bootstrap_ci(differences: np.ndarray, cfg: PSMConfig) -> Tuple[float, float]:
        rng = np.random.default_rng(cfg.seed)
        samples = rng.choice(
            differences, size=(cfg.n_bootstrap, len(differences)), replace=True
        )
        estimates = np.mean(samples, axis=1)
        alpha = 1 - cfg.confidence_level
        return (
            float(np.quantile(estimates, alpha / 2)),
            float(np.quantile(estimates, 1 - alpha / 2)),
        )

    @staticmethod
    def _p_value(differences: np.ndarray, outcome_type: str) -> Tuple[float, str]:
        if len(differences) < 2 or np.allclose(differences, 0):
            return 1.0, "degenerate matched differences"
        if outcome_type == "binary":
            non_zero = differences[differences != 0]
            if len(non_zero) == 0:
                return 1.0, "paired sign test"
            positive = int(np.sum(non_zero > 0))
            return float(stats.binomtest(positive, len(non_zero), 0.5).pvalue), "paired sign test"
        return float(stats.ttest_1samp(differences, 0).pvalue), "paired t-test on matched differences"

    @staticmethod
    def _aipw_att(
        X: np.ndarray,
        Z: np.ndarray,
        Y: np.ndarray,
        propensity: np.ndarray,
        outcome_type: str,
    ) -> Dict[str, Any]:
        if outcome_type == "binary":
            model_0 = make_pipeline(StandardScaler(), LogisticRegression(max_iter=1000, random_state=42))
            model_1 = make_pipeline(StandardScaler(), LogisticRegression(max_iter=1000, random_state=42))
        else:
            model_0 = make_pipeline(StandardScaler(), Ridge(alpha=1.0))
            model_1 = make_pipeline(StandardScaler(), Ridge(alpha=1.0))
        model_0.fit(X[Z == 0], Y[Z == 0])
        model_1.fit(X[Z == 1], Y[Z == 1])
        mu_0 = model_0.predict(X)
        mu_1 = model_1.predict(X)
        treated = Z == 1
        n_treated = int(treated.sum())
        residual_treated = float(np.mean(Y[treated] - mu_1[treated]))
        weighted_control = float(
            np.sum((propensity[~treated] / (1 - propensity[~treated])) * (Y[~treated] - mu_0[~treated]))
            / n_treated
        )
        att = float(np.mean((mu_1 - mu_0)[treated]) + residual_treated - weighted_control)
        return {
            "att": att,
            "method": "AIPW ATT with separate outcome models",
            "cross_fitting": False,
            "role": "secondary diagnostic; not the primary settlement estimand",
        }

    @staticmethod
    def _failure_result(
        records: Sequence[Mapping[str, Any]],
        Z: np.ndarray,
        status: str,
        message: str,
    ) -> Dict[str, Any]:
        return {
            "status": status,
            "analysis_status": "draft",
            "engine_version": "psm-rwe-draft-1.0",
            "message": message,
            "n_total": len(records),
            "n_treated": int(Z.sum()) if len(Z) else 0,
            "n_control": int(len(Z) - Z.sum()) if len(Z) else 0,
            "n_matched_pairs": 0,
            "quality_gates": {"analysis_pass": False},
        }
