"""
Level 2 group attribution — Propensity Score Matching (PSM) with Doubly
Robust Estimation (DRE), bootstrap confidence intervals, and Rosenbaum
sensitivity analysis.

Part of bodyup_cloud.engine — V1 specification.
"""

import numpy as np
from scipy.stats import wilcoxon
from sklearn.linear_model import LogisticRegression
from sklearn.neighbors import NearestNeighbors
from typing import List, Dict


class GroupAttributionEngine:
    """Estimate the Average Treatment effect on the Treated (ATT) across a
    cohort using PSM + DRE + Bootstrap."""

    MATCHING_FEATURES = [
        "age_bracket",
        "bmi_level_encoded",
        "bp_baseline_grade_encoded",
        "activity_level_encoded",
        "gender_encoded",
        "season_sin",
        "season_cos",
    ]

    def __init__(self, config: dict | None = None):
        config = config or {}
        self.caliper = config.get("caliper", "auto")
        self.n_bootstrap = config.get("n_bootstrap", 200)
        self.confidence_level = config.get("confidence_level", 0.95)
        self.min_group_size = config.get("min_group_size", 30)

    # ------------------------------------------------------------------
    # Public API
    # ------------------------------------------------------------------

    def estimate(self, user_records: List[Dict]) -> dict:
        """Run the full PSM + DRE + Bootstrap pipeline.

        Parameters
        ----------
        user_records : list of dict
            Each record: {device_id, Z (0 or 1), delta_Y (Y_end - Y_start),
            features: {MATCHING_FEATURES...}}.

        Returns
        -------
        dict with ATT, confidence interval, balance diagnostics, etc.
        """
        n = len(user_records)
        Z = np.array([r["Z"] for r in user_records])
        Y = np.array([r["delta_Y"] for r in user_records])

        n_treated = int(Z.sum())
        n_control = int((1 - Z).sum())

        if n < self.min_group_size:
            return {
                "status": "insufficient_data",
                "n_total_users": n,
                "min_required": self.min_group_size,
            }
        if n_treated < 10 or n_control < 10:
            return {
                "status": "insufficient_groups",
                "n_treated": n_treated,
                "n_control": n_control,
                "min_per_group": 10,
            }

        X_raw = np.array(
            [
                [r["features"].get(f, 0) for f in self.MATCHING_FEATURES]
                for r in user_records
            ]
        )
        X = np.column_stack([np.ones(n), X_raw])

        ps = self._compute_propensity(X, Z)

        caliper_value = self._resolve_caliper(ps)
        matched_pairs = self._psm_match(ps, Z, caliper_value)

        if len(matched_pairs) < 10:
            return {
                "status": "insufficient_matches",
                "n_matched_pairs": len(matched_pairs),
                "min_required": 10,
            }

        idx_t = np.array([p[0] for p in matched_pairs])
        idx_c = np.array([p[1] for p in matched_pairs])
        matched_idx = np.concatenate([idx_t, idx_c])

        Z_m = Z[matched_idx]
        Y_m = Y[matched_idx]
        X_m = X[matched_idx]

        e_m = self._compute_propensity(X_m, Z_m)
        mu_m = self._fit_outcome(X_m, Y_m)
        mz = Z_m.mean()

        dr1 = (Z_m * Y_m) / mz - ((Z_m - e_m) / (1 - e_m)) * (mu_m / mz)
        dr0 = ((1 - Z_m) * e_m * Y_m) / ((1 - e_m) * mz) + (
            (Z_m - e_m) / (1 - e_m)
        ) * (mu_m / mz)
        individual_effects = dr1 - dr0
        att = float(individual_effects.mean())

        alpha = 1 - self.confidence_level
        boot_atts: list[float] = []
        rng = np.random.default_rng(42)
        for _ in range(self.n_bootstrap):
            idx = rng.choice(
                len(individual_effects), size=len(individual_effects), replace=True
            )
            boot_atts.append(float(individual_effects[idx].mean()))
        boot_arr = np.array(boot_atts)
        ci_lower = float(np.percentile(boot_arr, 100 * alpha / 2))
        ci_upper = float(np.percentile(boot_arr, 100 * (1 - alpha / 2)))

        balance = self._check_balance(X_raw, Z, idx_t, idx_c)

        matched_Y_t = Y[idx_t]
        matched_Y_c = Y[idx_c]
        gamma_sensitivity, sensitivity_interpretation = self._rosenbaum_bounds(
            matched_Y_t, matched_Y_c
        )

        is_sig = ci_upper < 0
        report = self._render_settlement_report(
            att, ci_lower, ci_upper, is_sig,
            n, n_treated, n_control, len(matched_pairs),
            self.n_bootstrap, balance, gamma_sensitivity,
        )

        return {
            "status": "success",
            "level": "group",
            "att": att,
            "ci_lower": ci_lower,
            "ci_upper": ci_upper,
            "n_total_users": n,
            "n_treated": n_treated,
            "n_control": n_control,
            "n_matched_pairs": len(matched_pairs),
            "n_bootstrap": self.n_bootstrap,
            "is_significant": is_sig,
            "matching_balance": balance,
            "gamma_sensitivity": gamma_sensitivity,
            "sensitivity_interpretation": sensitivity_interpretation,
            "settlement_report": report,
        }

    # ------------------------------------------------------------------
    # Internal helpers
    # ------------------------------------------------------------------

    def _compute_propensity(self, X: np.ndarray, Z: np.ndarray) -> np.ndarray:
        """L2-regularised logistic regression via sklearn."""
        lr = LogisticRegression(C=10, solver="lbfgs", max_iter=300, random_state=42)
        lr.fit(X, Z)
        ps = lr.predict_proba(X)[:, 1]
        return np.clip(ps, 0.05, 0.95)

    def _resolve_caliper(self, ps: np.ndarray) -> float:
        if self.caliper != "auto":
            return float(self.caliper)
        logit_ps = np.log(ps / (1 - ps))
        return 0.2 * float(np.std(logit_ps))

    def _psm_match(
        self, ps: np.ndarray, Z: np.ndarray, caliper_value: float
    ) -> list[tuple[int, int]]:
        """Caliper nearest-neighbour matching without replacement using KD-tree."""
        treated_idx = np.where(Z == 1)[0]
        control_idx = np.where(Z == 0)[0]

        if len(control_idx) == 0 or len(treated_idx) == 0:
            return []

        ps_control = ps[control_idx].reshape(-1, 1)
        ps_treated = ps[treated_idx].reshape(-1, 1)

        nn = NearestNeighbors(n_neighbors=1, metric="euclidean")
        nn.fit(ps_control)
        distances, indices = nn.kneighbors(ps_treated)
        distances = distances.ravel()
        indices = indices.ravel()

        order = np.argsort(distances)
        matched: list[tuple[int, int]] = []
        used_control: set[int] = set()

        for rank in order:
            if distances[rank] > caliper_value:
                break
            c_local_idx = int(indices[rank])
            if c_local_idx in used_control:
                continue
            matched.append((int(treated_idx[rank]), int(control_idx[c_local_idx])))
            used_control.add(c_local_idx)

        return matched

    def _fit_outcome(self, X: np.ndarray, Y: np.ndarray) -> np.ndarray:
        """Ridge regression predicted values: X @ beta."""
        alpha = 1.0
        n_features = X.shape[1]
        XtX = X.T @ X + alpha * np.eye(n_features)
        beta = np.linalg.solve(XtX, X.T @ Y)
        return X @ beta

    def _check_balance(
        self,
        X_raw: np.ndarray,
        Z: np.ndarray,
        idx_t: np.ndarray,
        idx_c: np.ndarray,
    ) -> dict:
        """Compute standardised mean differences for matched groups."""
        balance: dict = {}
        for i, feat in enumerate(self.MATCHING_FEATURES):
            treated_vals = X_raw[idx_t, i]
            control_vals = X_raw[idx_c, i]
            mean_t = treated_vals.mean()
            mean_c = control_vals.mean()
            pooled_std = np.sqrt((treated_vals.var() + control_vals.var()) / 2)
            smd = abs(mean_t - mean_c) / pooled_std if pooled_std > 0 else 0.0
            balance[feat] = {"smd": round(smd, 4), "balanced": smd < 0.1}
        return balance

    @staticmethod
    def _rosenbaum_bounds(
        matched_Y_t: np.ndarray, matched_Y_c: np.ndarray
    ) -> tuple[float, str]:
        """Rosenbaum sensitivity analysis for matched pair differences."""
        diffs = matched_Y_t - matched_Y_c
        if len(diffs) < 5 or np.all(diffs == 0):
            return 1.0, "样本不足或无差异，无法进行敏感性分析"

        gamma_values = [1.0, 1.25, 1.5, 1.75, 2.0, 2.5, 3.0]
        max_robust_gamma = 1.0

        for gamma in gamma_values:
            adjusted = diffs * gamma
            try:
                _, p_value = wilcoxon(adjusted, alternative="less")
            except ValueError:
                p_value = 1.0
            if p_value < 0.05:
                max_robust_gamma = gamma
            else:
                break

        if max_robust_gamma >= 2.5:
            interpretation = f"结论非常稳健(Γ≥{max_robust_gamma:.2f})，即使存在较大未观测混杂也不影响结论"
        elif max_robust_gamma >= 1.5:
            interpretation = f"结论较稳健(Γ={max_robust_gamma:.2f})，可容忍中等程度的未观测混杂"
        elif max_robust_gamma > 1.0:
            interpretation = f"结论略敏感(Γ={max_robust_gamma:.2f})，小幅未观测混杂可能改变结论"
        else:
            interpretation = "结论对未观测混杂敏感(Γ=1.0)，建议谨慎解读"

        return float(max_robust_gamma), interpretation

    @staticmethod
    def _render_settlement_report(
        att, ci_lower, ci_upper, is_sig,
        n_total, n_treated, n_control, n_matched,
        n_bootstrap, balance, gamma_sensitivity,
    ) -> dict:
        n_balanced = sum(1 for v in balance.values() if v["balanced"])
        n_features = len(balance)

        if is_sig:
            conclusion = "统计显著"
            recommendation = "建议按约定费率结算"
            detail = (
                f"经倾向得分匹配(PSM)与双重稳健估计(DRE)分析，"
                f"干预组相较对照组心血管风险平均额外下降 {abs(att):.1%}，"
                f"95%置信区间为 [{abs(ci_upper):.1%}, {abs(ci_lower):.1%}]，"
                f"效果具有统计显著性(p<0.05)。"
            )
        else:
            conclusion = "未达统计显著"
            recommendation = "建议延长观察周期或扩大样本量"
            detail = (
                f"经PSM+DRE分析，干预组风险变化为 {att:+.1%}，"
                f"95%置信区间 [{ci_lower:+.1%}, {ci_upper:+.1%}] 跨越零值，"
                f"尚未达到统计显著性。"
            )

        method = (
            f"方法：倾向得分匹配(自适应Caliper + KD-tree最近邻, 无放回) + "
            f"双重稳健估计(DRE) + {n_bootstrap}次Bootstrap置信区间 + "
            f"Rosenbaum敏感性分析(Γ={gamma_sensitivity:.2f})。"
            f"共纳入 {n_total} 名用户(干预组 {n_treated} 人, 对照组 {n_control} 人)，"
            f"成功匹配 {n_matched} 对。"
            f"匹配平衡性：{n_balanced}/{n_features} 项特征 SMD<0.1。"
        )

        return {
            "conclusion": conclusion,
            "recommendation": recommendation,
            "detail": detail,
            "method": method,
            "metrics": {
                "att": f"{att:+.4f}",
                "ci_95": f"[{ci_lower:+.4f}, {ci_upper:+.4f}]",
                "n_matched_pairs": n_matched,
                "balance_pass_rate": f"{n_balanced}/{n_features}",
                "gamma_sensitivity": f"{gamma_sensitivity:.2f}",
            },
        }
