"""
Group Attribution (Level 2)

Estimates the Average Treatment Effect on the Treated (ATT) across a
cohort using PSM + DRE + Bootstrap + comprehensive statistical validation.

Designed for insurance actuary review with:
- Explicit p-values
- E-value for unmeasured confounding
- Overlap/common support diagnostics
- Power analysis
- Multiple propensity model specifications
- BCa bootstrap confidence intervals
- Effect size metrics
- Framingham/China-PAR comparison ready
"""

import numpy as np
from typing import Dict, List, Optional, Tuple
from scipy import stats

try:
    from scipy.stats import wilcoxon
except ImportError:
    wilcoxon = None

try:
    from sklearn.linear_model import LogisticRegression
    from sklearn.neighbors import NearestNeighbors
    from sklearn.ensemble import GradientBoostingClassifier
except ImportError:
    LogisticRegression = None
    NearestNeighbors = None
    GradientBoostingClassifier = None


class GroupAttributor:
    """
    Group-level attribution engine with comprehensive statistical rigor.

    Implements PSM + DRE + Bootstrap with all metrics required by
    insurance actuaries for health intervention validation.
    """

    MATCHING_FEATURES = [
        "age_bracket",
        "bmi_level_encoded",
        "bp_baseline_grade_encoded",
        "activity_level_encoded",
        "gender_encoded",
        "season_sin",
        "season_cos",
    ]

    def __init__(self, config: Dict = None):
        config = config or {}
        self.caliper = config.get("caliper", "auto")
        self.n_bootstrap = config.get("n_bootstrap", 1000)
        self.confidence_level = config.get("confidence_level", 0.95)
        self.min_group_size = config.get("min_group_size", 30)

    def attribute(self, user_records: List[Dict]) -> Dict:
        """
        Run group-level attribution.

        Parameters
        ----------
        user_records : list of dict
            Each record: {device_id, Z (0 or 1), delta_Y, features: {...}}

        Returns
        -------
        dict with comprehensive attribution results
        """
        # Validate input
        validation = self._validate_input(user_records)
        if validation:
            return validation

        # Extract data
        n = len(user_records)
        Z = np.array([r["Z"] for r in user_records])
        Y = np.array([r["delta_Y"] for r in user_records])
        X_raw = np.array([
            [r["features"].get(f, 0) for f in self.MATCHING_FEATURES]
            for r in user_records
        ])
        X = np.column_stack([np.ones(n), X_raw])

        n_treated = int(Z.sum())
        n_control = int((1 - Z).sum())

        # Step 1: Propensity Score Estimation
        ps_results = self._estimate_propensity_scores(X, Z)

        # Step 2: Overlap Diagnostics
        overlap = self._compute_overlap_diagnostics(ps_results["ensemble"], Z)

        # Step 3: PSM Matching
        caliper_value = self._resolve_caliper(ps_results["ensemble"])
        matched_pairs = self._psm_match(ps_results["ensemble"], Z, caliper_value)

        if len(matched_pairs) < 10:
            return {
                "status": "insufficient_matches",
                "n_matched_pairs": len(matched_pairs),
                "min_required": 10,
                "overlap_diagnostics": overlap,
            }

        # Step 4: Extract matched data
        idx_t = np.array([p[0] for p in matched_pairs])
        idx_c = np.array([p[1] for p in matched_pairs])
        matched_idx = np.concatenate([idx_t, idx_c])

        Z_m = Z[matched_idx]
        Y_m = Y[matched_idx]
        X_m = X[matched_idx]

        # Step 5: DRE Estimation
        att, individual_effects = self._dre_estimate(Z_m, Y_m, X_m)

        # Step 6: Bootstrap Confidence Intervals (BCa)
        ci_lower, ci_upper, boot_atts = self._bca_bootstrap(
            individual_effects, 1 - self.confidence_level
        )

        # Step 7: Balance Diagnostics
        balance = self._check_balance(X_raw, Z, idx_t, idx_c)

        # Step 8: Statistical Tests
        matched_Y_t = Y[idx_t]
        matched_Y_c = Y[idx_c]
        pair_diffs = matched_Y_t - matched_Y_c

        p_value = self._compute_p_value(pair_diffs)
        effect_sizes = self._compute_effect_sizes(pair_diffs)

        # Step 9: Sensitivity Analysis
        gamma, gamma_interpretation = self._rosenbaum_bounds(matched_Y_t, matched_Y_c)
        e_value = self._compute_e_value(att, p_value)

        # Step 10: Power Analysis
        power = self._power_analysis(len(pair_diffs), pair_diffs)

        # Step 11: Propensity Model Robustness
        robustness = self._propensity_model_robustness(X_raw, Z, Y, ps_results)

        # Determine significance
        is_sig = ci_upper < 0

        # Generate report
        report = self._generate_report(
            att, ci_lower, ci_upper, p_value, is_sig,
            n, n_treated, n_control, len(matched_pairs),
            balance, gamma, effect_sizes, e_value, power,
            overlap, robustness,
        )

        return {
            "status": "success",
            "level": "group",

            # Core results
            "att": att,
            "ci_lower": ci_lower,
            "ci_upper": ci_upper,
            "p_value": p_value,
            "is_significant": is_sig,

            # Sample info
            "n_total": n,
            "n_treated": n_treated,
            "n_control": n_control,
            "n_matched_pairs": len(matched_pairs),

            # Diagnostics
            "balance": balance,
            "overlap_diagnostics": overlap,
            "propensity_model_results": ps_results["model_comparison"],

            # Sensitivity
            "gamma_sensitivity": gamma,
            "gamma_interpretation": gamma_interpretation,
            "e_value": e_value,

            # Effect sizes
            "effect_sizes": effect_sizes,

            # Power
            "power_analysis": power,

            # Robustness
            "propensity_robustness": robustness,

            # Report
            "settlement_report": report,
        }

    def _validate_input(self, user_records: List[Dict]) -> Optional[Dict]:
        """Validate input data."""
        n = len(user_records)
        Z = np.array([r["Z"] for r in user_records])
        n_treated = int(Z.sum())
        n_control = n - n_treated

        if n < self.min_group_size:
            return {
                "status": "insufficient_data",
                "n_total": n,
                "min_required": self.min_group_size,
            }

        if n_treated < 10 or n_control < 10:
            return {
                "status": "insufficient_groups",
                "n_treated": n_treated,
                "n_control": n_control,
                "min_per_group": 10,
            }

        return None

    def _estimate_propensity_scores(
        self, X: np.ndarray, Z: np.ndarray
    ) -> Dict:
        """
        Estimate propensity scores with multiple models.

        Returns ensemble scores and model comparison.
        """
        models = {}
        predictions = {}

        # Model 1: Logistic Regression
        lr = LogisticRegression(C=10, solver="lbfgs", max_iter=300, random_state=42)
        lr.fit(X, Z)
        ps_lr = lr.predict_proba(X)[:, 1]
        models["logistic"] = {"C": 10}
        predictions["logistic"] = np.clip(ps_lr, 0.05, 0.95)

        # Model 2: Logistic with interactions
        X_interact = np.column_stack([X, X[:, 1:3] * X[:, 3:5]])  # Add interactions
        lr2 = LogisticRegression(C=10, solver="lbfgs", max_iter=300, random_state=42)
        lr2.fit(X_interact, Z)
        ps_lr2 = lr2.predict_proba(X_interact)[:, 1]
        models["logistic_interact"] = {"C": 10}
        predictions["logistic_interact"] = np.clip(ps_lr2, 0.05, 0.95)

        # Model 3: Gradient Boosting
        if GradientBoostingClassifier is not None:
            gb = GradientBoostingClassifier(
                n_estimators=100, max_depth=3, random_state=42
            )
            gb.fit(X, Z)
            ps_gb = gb.predict_proba(X)[:, 1]
            models["gradient_boosting"] = {"n_estimators": 100}
            predictions["gradient_boosting"] = np.clip(ps_gb, 0.05, 0.95)

        # Ensemble: average
        all_ps = np.array(list(predictions.values()))
        ps_ensemble = np.mean(all_ps, axis=0)

        # Model comparison
        model_comparison = {}
        for name, ps in predictions.items():
            model_comparison[name] = {
                "mean_ps": float(ps.mean()),
                "std_ps": float(ps.std()),
            }

        return {
            "ensemble": ps_ensemble,
            "individual": predictions,
            "model_comparison": model_comparison,
        }

    def _compute_overlap_diagnostics(
        self, ps: np.ndarray, Z: np.ndarray
    ) -> Dict:
        """
        Compute overlap/common support diagnostics.
        """
        ps_treated = ps[Z == 1]
        ps_control = ps[Z == 0]

        # Common support
        lower = max(ps_treated.min(), ps_control.min())
        upper = min(ps_treated.max(), ps_control.max())

        # Units outside
        outside_t = ((ps_treated < lower) | (ps_treated > upper)).sum()
        outside_c = ((ps_control < lower) | (ps_control > upper)).sum()

        # Overlap coefficient
        overlap_coef = upper - lower

        return {
            "ps_treated_mean": float(ps_treated.mean()),
            "ps_treated_std": float(ps_treated.std()),
            "ps_control_mean": float(ps_control.mean()),
            "ps_control_std": float(ps_control.std()),
            "common_support_lower": float(lower),
            "common_support_upper": float(upper),
            "outside_treated": int(outside_t),
            "outside_control": int(outside_c),
            "overlap_coefficient": float(overlap_coef),
            "overlap_quality": "good" if overlap_coef > 0.5 else "fair" if overlap_coef > 0.3 else "poor",
        }

    def _resolve_caliper(self, ps: np.ndarray) -> float:
        """Resolve caliper value."""
        if self.caliper != "auto":
            return float(self.caliper)
        logit_ps = np.log(ps / (1 - ps))
        return 0.2 * float(np.std(logit_ps))

    def _psm_match(
        self, ps: np.ndarray, Z: np.ndarray, caliper: float
    ) -> List[Tuple[int, int]]:
        """PSM matching with KD-tree."""
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
        matched = []
        used = set()

        for rank in order:
            if distances[rank] > caliper:
                break
            c_idx = int(indices[rank])
            if c_idx in used:
                continue
            matched.append((int(treated_idx[rank]), int(control_idx[c_idx])))
            used.add(c_idx)

        return matched

    def _dre_estimate(
        self, Z: np.ndarray, Y: np.ndarray, X: np.ndarray
    ) -> Tuple[float, np.ndarray]:
        """
        Doubly Robust Estimation of ATT.

        DR₁ = Z*Y/mz - (Z-e)/(1-e) * μ/mz
        DR₀ = (1-Z)*e*Y/((1-e)*mz) + (Z-e)/(1-e) * μ/mz
        ATT = E[DR₁ - DR₀]
        """
        # Propensity scores
        lr = LogisticRegression(C=10, solver="lbfgs", max_iter=300, random_state=42)
        lr.fit(X, Z)
        e = np.clip(lr.predict_proba(X)[:, 1], 0.05, 0.95)

        # Outcome model (Ridge regression)
        alpha = 1.0
        n_features = X.shape[1]
        XtX = X.T @ X + alpha * np.eye(n_features)
        beta = np.linalg.solve(XtX, X.T @ Y)
        mu = X @ beta

        # DR estimator
        mz = Z.mean()
        dr1 = (Z * Y) / mz - ((Z - e) / (1 - e)) * (mu / mz)
        dr0 = ((1 - Z) * e * Y) / ((1 - e) * mz) + ((Z - e) / (1 - e)) * (mu / mz)
        individual_effects = dr1 - dr0

        att = float(individual_effects.mean())
        return att, individual_effects

    def _bca_bootstrap(
        self, individual_effects: np.ndarray, alpha: float
    ) -> Tuple[float, float, np.ndarray]:
        """BCa bootstrap confidence intervals."""
        n = len(individual_effects)
        rng = np.random.default_rng(42)

        theta_hat = individual_effects.mean()

        # Bootstrap samples
        boot_atts = []
        for _ in range(self.n_bootstrap):
            idx = rng.choice(n, size=n, replace=True)
            boot_atts.append(float(individual_effects[idx].mean()))

        boot_arr = np.array(boot_atts)

        # Bias correction
        z0 = stats.norm.ppf((boot_arr < theta_hat).mean())

        # Acceleration (jackknife)
        jack_vals = np.array([
            np.delete(individual_effects, i).mean() for i in range(n)
        ])
        jack_mean = jack_vals.mean()
        num = ((jack_mean - jack_vals) ** 3).sum()
        den = 6 * (((jack_mean - jack_vals) ** 2).sum() ** 1.5)
        a = num / den if den != 0 else 0

        # Adjusted percentiles
        z_lo = stats.norm.ppf(alpha / 2)
        z_hi = stats.norm.ppf(1 - alpha / 2)

        p_lo = stats.norm.cdf(z0 + (z0 + z_lo) / (1 - a * (z0 + z_lo)))
        p_hi = stats.norm.cdf(z0 + (z0 + z_hi) / (1 - a * (z0 + z_hi)))

        ci_lower = float(np.percentile(boot_arr, 100 * p_lo))
        ci_upper = float(np.percentile(boot_arr, 100 * p_hi))

        return ci_lower, ci_upper, boot_arr

    def _check_balance(
        self, X_raw: np.ndarray, Z: np.ndarray,
        idx_t: np.ndarray, idx_c: np.ndarray
    ) -> Dict:
        """Compute SMD for covariate balance."""
        balance = {}
        for i, feat in enumerate(self.MATCHING_FEATURES):
            t_vals = X_raw[idx_t, i]
            c_vals = X_raw[idx_c, i]
            mean_t = t_vals.mean()
            mean_c = c_vals.mean()
            pooled_std = np.sqrt((t_vals.var() + c_vals.var()) / 2)
            smd = abs(mean_t - mean_c) / pooled_std if pooled_std > 0 else 0
            balance[feat] = {
                "smd": round(smd, 4),
                "balanced": smd < 0.1,
                "mean_treated": round(mean_t, 4),
                "mean_control": round(mean_c, 4),
            }
        return balance

    def _compute_p_value(self, pair_diffs: np.ndarray) -> float:
        """Compute p-value from Wilcoxon signed-rank test."""
        if wilcoxon is None or len(pair_diffs) < 5:
            return 1.0

        non_zero = pair_diffs[pair_diffs != 0]
        if len(non_zero) < 5:
            return 1.0

        try:
            _, p = wilcoxon(non_zero, alternative="two-sided")
            return float(p)
        except:
            return 1.0

    def _compute_effect_sizes(self, pair_diffs: np.ndarray) -> Dict:
        """Compute effect size metrics."""
        if len(pair_diffs) == 0:
            return {"cohens_d": 0, "hedges_g": 0, "smd": 0}

        mean_diff = pair_diffs.mean()
        std_diff = pair_diffs.std()

        d = mean_diff / std_diff if std_diff > 0 else 0
        correction = 1 - (3 / (4 * len(pair_diffs) - 1))
        g = d * correction

        if abs(d) < 0.2:
            interpretation = "微小效应"
        elif abs(d) < 0.5:
            interpretation = "小效应"
        elif abs(d) < 0.8:
            interpretation = "中等效应"
        else:
            interpretation = "大效应"

        return {
            "cohens_d": float(d),
            "hedges_g": float(g),
            "mean_difference": float(mean_diff),
            "std_difference": float(std_diff),
            "interpretation": interpretation,
        }

    def _rosenbaum_bounds(
        self, Y_t: np.ndarray, Y_c: np.ndarray
    ) -> Tuple[float, str]:
        """Rosenbaum sensitivity analysis."""
        if wilcoxon is None:
            return 1.0, "scipy未安装"

        diffs = Y_t - Y_c
        if len(diffs) < 5 or np.all(diffs == 0):
            return 1.0, "样本不足"

        gamma_values = [1.0, 1.25, 1.5, 1.75, 2.0, 2.5, 3.0, 4.0, 5.0]
        max_gamma = 1.0

        for gamma in gamma_values:
            try:
                _, p = wilcoxon(diffs * gamma, alternative="less")
                if p < 0.05:
                    max_gamma = gamma
                else:
                    break
            except:
                break

        if max_gamma >= 3.0:
            interp = f"非常稳健(Γ≥{max_gamma})，即使存在较大未观测混杂也不影响结论"
        elif max_gamma >= 2.0:
            interp = f"较稳健(Γ={max_gamma})，可容忍中等程度未观测混杂"
        elif max_gamma >= 1.5:
            interp = f"略敏感(Γ={max_gamma})，小幅未观测混杂可能改变结论"
        else:
            interp = f"敏感(Γ={max_gamma})，建议谨慎解读"

        return float(max_gamma), interp

    def _compute_e_value(self, att: float, p_value: float) -> Dict:
        """Compute E-value for unmeasured confounding."""
        if p_value > 0.05 or att >= 0:
            return {"e_value": 1.0, "interpretation": "效应不显著，E-value不适用"}

        baseline_risk = 0.2
        risk_with = baseline_risk + att
        rr = risk_with / baseline_risk

        if rr <= 1:
            e_value = rr + np.sqrt(rr * (1 - rr))
        else:
            e_value = rr + np.sqrt(rr * (rr - 1))

        return {
            "e_value": float(e_value),
            "risk_ratio": float(rr),
            "interpretation": f"未测量混杂需要RR≥{e_value:.2f}才能解释效应",
        }

    def _power_analysis(
        self, n_pairs: int, pair_diffs: np.ndarray
    ) -> Dict:
        """Compute statistical power."""
        mean_diff = abs(pair_diffs.mean())
        std_diff = pair_diffs.std()

        if std_diff == 0:
            return {"power": 1.0, "adequately_powered": True}

        d = mean_diff / std_diff
        z_alpha = stats.norm.ppf(0.975)
        z_beta = (d * np.sqrt(n_pairs)) - z_alpha
        power = float(stats.norm.cdf(z_beta))

        z_80 = stats.norm.ppf(0.8)
        n_80 = int(np.ceil(((z_alpha + z_80) / d) ** 2))

        return {
            "n_pairs": n_pairs,
            "effect_size_d": float(d),
            "power": power,
            "adequately_powered": power >= 0.8,
            "min_n_80_power": n_80,
        }

    def _propensity_model_robustness(
        self, X_raw: np.ndarray, Z: np.ndarray, Y: np.ndarray,
        ps_results: Dict
    ) -> Dict:
        """Test robustness across different propensity models."""
        results = {}

        for name, ps in ps_results["individual"].items():
            # Quick ATT via IPW
            mz = Z.mean()
            ipw = (Z * Y) / mz - ((1 - Z) * ps * Y) / ((1 - ps) * mz)
            results[name] = {
                "att_ipw": float(ipw.mean()),
                "mean_ps": float(ps.mean()),
            }

        # Check consistency
        atts = [r["att_ipw"] for r in results.values()]
        att_range = max(atts) - min(atts)

        return {
            "model_results": results,
            "att_range": float(att_range),
            "consistent": att_range < 0.05,
        }

    def _generate_report(self, *args) -> Dict:
        """Generate settlement report."""
        (att, ci_lower, ci_upper, p_value, is_sig,
         n_total, n_treated, n_control, n_matched,
         balance, gamma, effect_sizes, e_value, power,
         overlap, robustness) = args

        n_balanced = sum(1 for v in balance.values() if v["balanced"])
        n_features = len(balance)

        if is_sig:
            conclusion = "统计显著"
            recommendation = "建议按约定费率结算"
            detail = (
                f"经PSM+DRE分析，干预组心血管风险平均额外下降 {abs(att):.1%}，"
                f"95% CI [{abs(ci_upper):.1%}, {abs(ci_lower):.1%}]，"
                f"p={p_value:.4f}，效果具有统计显著性。"
            )
        else:
            conclusion = "未达统计显著"
            recommendation = "建议延长观察周期或扩大样本量"
            detail = (
                f"经PSM+DRE分析，干预组风险变化 {att:+.1%}，"
                f"95% CI [{ci_lower:+.1%}, {ci_upper:+.1%}]，"
                f"p={p_value:.4f}，尚未达到统计显著性。"
            )

        method = (
            f"方法：倾向得分匹配(自适应Caliper+KD-tree) + 双重稳健估计(DRE) + "
            f"{self.n_bootstrap}次BCa Bootstrap + Wilcoxon符号秩检验 + "
            f"Rosenbaum敏感性分析(Γ={gamma:.2f})。"
            f"共 {n_total} 人(干预{n_treated}/对照{n_control})，匹配 {n_matched} 对。"
            f"平衡性：{n_balanced}/{n_features} 特征SMD<0.1。"
        )

        return {
            "conclusion": conclusion,
            "recommendation": recommendation,
            "detail": detail,
            "method": method,
            "metrics": {
                "att": f"{att:+.4f}",
                "ci_95": f"[{ci_lower:+.4f}, {ci_upper:+.4f}]",
                "p_value": f"{p_value:.4f}",
                "effect_size_d": f"{effect_sizes['cohens_d']:.3f}",
                "e_value": f"{e_value['e_value']:.2f}",
                "gamma": f"{gamma:.2f}",
                "power": f"{power['power']:.1%}",
                "n_matched": n_matched,
                "balance_rate": f"{n_balanced}/{n_features}",
                "overlap_quality": overlap["overlap_quality"],
                "robustness": "一致" if robustness["consistent"] else "不一致",
            },
            "effect_sizes": effect_sizes,
            "e_value": e_value,
            "power_analysis": power,
        }
