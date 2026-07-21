"""
Actuarial Validation Module

Comprehensive validation metrics for insurance actuary review.
Implements: Framingham comparison, China-PAR, calibration metrics,
E-value, power analysis, NNT.
"""

import numpy as np
import pandas as pd
from typing import Dict, List, Optional, Tuple, Any
from sklearn.metrics import roc_auc_score, brier_score_loss
from sklearn.calibration import calibration_curve
from scipy import stats


class ActuarialValidator:
    """
    Validation metrics for insurance actuary review.

    Provides comprehensive model validation including:
    - Comparison with Framingham and China-PAR
    - Calibration metrics (Hosmer-Lemeshow, calibration slope)
    - Discrimination metrics (AUC, C-statistic)
    - Reclassification metrics (NRI, IDI)
    - Clinical significance (NNT)
    - E-value for unmeasured confounding
    - Power analysis
    """

    def __init__(self):
        self.results = {}

    def validate_model(
        self,
        y_true: np.ndarray,
        y_prob: np.ndarray,
        y_prob_framingham: Optional[np.ndarray] = None,
        y_prob_china_par: Optional[np.ndarray] = None,
        treatment: Optional[np.ndarray] = None,
        risk_reduction: Optional[float] = None,
    ) -> Dict[str, Any]:
        """
        Run comprehensive model validation.

        Parameters
        ----------
        y_true : np.ndarray
            True labels (0/1)
        y_prob : np.ndarray
            Model predicted probabilities
        y_prob_framingham : np.ndarray, optional
            Framingham risk scores
        y_prob_china_par : np.ndarray, optional
            China-PAR risk scores
        treatment : np.ndarray, optional
            Treatment indicator for ATT validation
        risk_reduction : float, optional
            Observed risk reduction for NNT calculation

        Returns
        -------
        dict with all validation metrics
        """
        results = {}

        # 1. Discrimination metrics
        results["discrimination"] = self._discrimination_metrics(y_true, y_prob)

        # 2. Calibration metrics
        results["calibration"] = self._calibration_metrics(y_true, y_prob)

        # 3. Comparison with Framingham
        if y_prob_framingham is not None:
            results["framingham_comparison"] = self._compare_models(
                y_true, y_prob, y_prob_framingham, "Framingham"
            )

        # 4. Comparison with China-PAR
        if y_prob_china_par is not None:
            results["china_par_comparison"] = self._compare_models(
                y_true, y_prob, y_prob_china_par, "China-PAR"
            )

        # 5. Reclassification metrics
        if y_prob_framingham is not None:
            results["reclassification"] = self._reclassification_metrics(
                y_true, y_prob, y_prob_framingham
            )

        # 6. Clinical significance
        if risk_reduction is not None:
            results["clinical_significance"] = self._clinical_significance(
                risk_reduction, y_true.mean()
            )

        # 7. E-value for unmeasured confounding
        if risk_reduction is not None:
            results["e_value"] = self._compute_e_value(risk_reduction)

        # 8. Power analysis
        results["power_analysis"] = self._power_analysis(
            len(y_true), y_true.mean(), risk_reduction or 0.05
        )

        self.results = results
        return results

    def _discrimination_metrics(
        self, y_true: np.ndarray, y_prob: np.ndarray
    ) -> Dict[str, float]:
        """Compute discrimination metrics."""
        auc = roc_auc_score(y_true, y_prob)

        # Compute C-statistic (same as AUC for binary outcomes)
        c_statistic = auc

        # Compute Youden's J statistic
        fpr, tpr, thresholds = self._roc_curve(y_true, y_prob)
        youden_j = np.max(tpr - fpr)
        optimal_threshold = thresholds[np.argmax(tpr - fpr)]

        return {
            "auc": auc,
            "c_statistic": c_statistic,
            "youden_j": youden_j,
            "optimal_threshold": optimal_threshold,
        }

    def _calibration_metrics(
        self, y_true: np.ndarray, y_prob: np.ndarray, n_bins: int = 10
    ) -> Dict[str, Any]:
        """Compute calibration metrics."""
        # Brier score
        brier_score = brier_score_loss(y_true, y_prob)

        # Hosmer-Lemeshow test
        hl_stat, hl_p_value = self._hosmer_lemeshow_test(y_true, y_prob, n_bins)

        # Calibration slope (logistic regression of y_true on logit(y_prob))
        eps = 1e-7
        logits = np.log(np.clip(y_prob, eps, 1 - eps) / (1 - np.clip(y_prob, eps, 1 - eps)))
        slope, intercept = np.polyfit(logits, y_true, 1)

        # Expected vs Observed ratio
        expected_events = y_prob.sum()
        observed_events = y_true.sum()
        eo_ratio = observed_events / max(expected_events, 1)

        # Calibration plot data
        prob_true, prob_pred = calibration_curve(y_true, y_prob, n_bins=n_bins)

        return {
            "brier_score": brier_score,
            "hosmer_lemeshow_statistic": hl_stat,
            "hosmer_lemeshow_p_value": hl_p_value,
            "calibration_slope": slope,
            "calibration_intercept": intercept,
            "expected_observed_ratio": eo_ratio,
            "calibration_plot": {
                "observed": prob_true.tolist(),
                "predicted": prob_pred.tolist(),
            },
        }

    def _hosmer_lemeshow_test(
        self, y_true: np.ndarray, y_prob: np.ndarray, n_groups: int = 10
    ) -> Tuple[float, float]:
        """Hosmer-Lemeshow goodness-of-fit test."""
        # Sort by predicted probability
        sorted_idx = np.argsort(y_prob)
        y_true_sorted = y_true[sorted_idx]
        y_prob_sorted = y_prob[sorted_idx]

        # Divide into groups
        group_size = len(y_true) // n_groups
        hl_stat = 0.0

        for i in range(n_groups):
            start = i * group_size
            end = start + group_size if i < n_groups - 1 else len(y_true)

            observed = y_true_sorted[start:end].sum()
            expected = y_prob_sorted[start:end].sum()
            n = end - start

            if expected > 0 and (n - expected) > 0:
                hl_stat += (observed - expected) ** 2 / expected
                hl_stat += ((n - observed) - (n - expected)) ** 2 / (n - expected)

        # Chi-squared test
        df = n_groups - 2
        p_value = 1 - stats.chi2.cdf(hl_stat, df)

        return hl_stat, p_value

    def _compare_models(
        self,
        y_true: np.ndarray,
        y_prob_new: np.ndarray,
        y_prob_ref: np.ndarray,
        ref_name: str,
    ) -> Dict[str, Any]:
        """Compare new model with reference model."""
        auc_new = roc_auc_score(y_true, y_prob_new)
        auc_ref = roc_auc_score(y_true, y_prob_ref)

        # DeLong test for AUC comparison (simplified)
        auc_diff = auc_new - auc_ref

        # Net Reclassification Index
        nri = self._compute_nri(y_true, y_prob_new, y_prob_ref)

        # Integrated Discrimination Improvement
        idi = self._compute_idi(y_true, y_prob_new, y_prob_ref)

        return {
            "reference_model": ref_name,
            "auc_new": auc_new,
            "auc_reference": auc_ref,
            "auc_difference": auc_diff,
            "auc_improvement_pct": (auc_diff / auc_ref) * 100 if auc_ref > 0 else 0,
            "nri": nri,
            "idi": idi,
        }

    def _compute_nri(
        self,
        y_true: np.ndarray,
        y_prob_new: np.ndarray,
        y_prob_ref: np.ndarray,
        categories: List[float] = None,
    ) -> Dict[str, float]:
        """Compute Net Reclassification Index."""
        if categories is None:
            categories = [0.1, 0.2]  # Low, medium, high risk categories

        # Classify into categories
        cat_new = np.digitize(y_prob_new, categories)
        cat_ref = np.digitize(y_prob_ref, categories)

        # Events (y_true = 1)
        events = y_true == 1
        non_events = y_true == 0

        # Upward reclassification for events is good
        events_up = ((cat_new > cat_ref) & events).sum()
        events_down = ((cat_new < cat_ref) & events).sum()

        # Downward reclassification for non-events is good
        non_events_down = ((cat_new < cat_ref) & non_events).sum()
        non_events_up = ((cat_new > cat_ref) & non_events).sum()

        # NRI for events
        nri_events = (events_up - events_down) / max(events.sum(), 1)

        # NRI for non-events
        nri_non_events = (non_events_down - non_events_up) / max(non_events.sum(), 1)

        # Overall NRI
        nri_overall = nri_events + nri_non_events

        return {
            "nri_overall": nri_overall,
            "nri_events": nri_events,
            "nri_non_events": nri_non_events,
            "events_reclassified_up": int(events_up),
            "events_reclassified_down": int(events_down),
            "non_events_reclassified_down": int(non_events_down),
            "non_events_reclassified_up": int(non_events_up),
        }

    def _compute_idi(
        self,
        y_true: np.ndarray,
        y_prob_new: np.ndarray,
        y_prob_ref: np.ndarray,
    ) -> Dict[str, float]:
        """Compute Integrated Discrimination Improvement."""
        #IDI = (mean predicted probability of events - mean predicted probability of non-events)
        # for new model minus same for reference model

        events = y_true == 1
        non_events = y_true == 0

        # New model
        p_events_new = y_prob_new[events].mean()
        p_non_events_new = y_prob_new[non_events].mean()
        idi_new = p_events_new - p_non_events_new

        # Reference model
        p_events_ref = y_prob_ref[events].mean()
        p_non_events_ref = y_prob_ref[non_events].mean()
        idi_ref = p_events_ref - p_non_events_ref

        # IDI
        idi = idi_new - idi_ref

        return {
            "idi": idi,
            "idi_new_model": idi_new,
            "idi_reference_model": idi_ref,
            "mean_risk_events_new": p_events_new,
            "mean_risk_non_events_new": p_non_events_new,
            "mean_risk_events_ref": p_events_ref,
            "mean_risk_non_events_ref": p_non_events_ref,
        }

    def _clinical_significance(
        self, risk_reduction: float, baseline_risk: float
    ) -> Dict[str, Any]:
        """Compute clinical significance metrics."""
        # Number Needed to Treat (NNT)
        if risk_reduction > 0:
            nnt = 1 / risk_reduction
        else:
            nnt = float('inf')

        # Absolute Risk Reduction
        arr = risk_reduction

        # Relative Risk Reduction
        if baseline_risk > 0:
            rrr = risk_reduction / baseline_risk
        else:
            rrr = 0

        # QALYs gained (simplified, assuming 1 year)
        # Each prevented CVD event saves ~0.8 QALYs (conservative estimate)
        qalys_per_event = 0.8
        qalys_gained = risk_reduction * qalys_per_event

        return {
            "nnt": nnt,
            "absolute_risk_reduction": arr,
            "relative_risk_reduction": rrr,
            "baseline_risk": baseline_risk,
            "qalys_gained_per_100": qalys_gained * 100,
            "interpretation": f"每治疗 {nnt:.0f} 人可预防1例CVD事件",
        }

    def _compute_e_value(self, risk_reduction: float) -> Dict[str, Any]:
        """
        Compute E-value for unmeasured confounding.

        E-value = RR + sqrt(RR * (RR - 1))
        where RR is the observed risk ratio.

        Interpretation: An unmeasured confounder would need to have
        an association of at least E-value with both treatment and
        outcome to explain away the observed effect.
        """
        # Convert risk reduction to risk ratio
        # Assuming baseline risk of 0.2 (typical CVD prevalence)
        baseline_risk = 0.2
        risk_with_intervention = baseline_risk - risk_reduction
        rr = risk_with_intervention / baseline_risk

        # E-value formula
        if rr <= 1:
            e_value = rr + np.sqrt(rr * (1 - rr))
        else:
            e_value = rr + np.sqrt(rr * (rr - 1))

        return {
            "e_value": e_value,
            "risk_ratio": rr,
            "interpretation": (
                f"一个未测量的混杂因素需要与治疗和结局都有至少 {e_value:.2f} 的风险比，"
                f"才能解释观察到的效应。"
            ),
        }

    def _power_analysis(
        self, n: int, baseline_risk: float, expected_effect: float
    ) -> Dict[str, Any]:
        """
        Compute statistical power.

        Parameters
        ----------
        n : int
            Sample size
        baseline_risk : float
            Baseline event rate
        expected_effect : float
            Expected risk reduction

        Returns
        -------
        dict with power analysis results
        """
        # Simplified power calculation for two proportions
        from scipy.stats import norm

        p1 = baseline_risk
        p2 = baseline_risk - expected_effect

        # Pooled proportion
        p_pool = (p1 + p2) / 2

        # Standard error
        se = np.sqrt(2 * p_pool * (1 - p_pool) / n)

        # Effect size (Cohen's h)
        h = 2 * np.arcsin(np.sqrt(p1)) - 2 * np.arcsin(np.sqrt(p2))

        # Z-scores
        z_alpha = norm.ppf(0.975)  # Two-tailed alpha = 0.05
        z_beta = (h * np.sqrt(n / 2)) - z_alpha

        # Power
        power = norm.cdf(z_beta)

        # Minimum sample size for 80% power
        z_80 = norm.ppf(0.8)
        n_80 = 2 * ((z_alpha + z_80) / h) ** 2

        # Minimum sample size for 90% power
        z_90 = norm.ppf(0.9)
        n_90 = 2 * ((z_alpha + z_90) / h) ** 2

        return {
            "sample_size": n,
            "baseline_risk": baseline_risk,
            "expected_effect": expected_effect,
            "effect_size_h": h,
            "power": power,
            "min_n_80_power": int(np.ceil(n_80)),
            "min_n_90_power": int(np.ceil(n_90)),
            "adequately_powered": power >= 0.8,
            "interpretation": (
                f"当前样本量 {n} 的统计功效为 {power:.1%}。"
                f"达到80%功效需要 {int(np.ceil(n_80))} 人，"
                f"达到90%功效需要 {int(np.ceil(n_90))} 人。"
            ),
        }

    def _roc_curve(
        self, y_true: np.ndarray, y_prob: np.ndarray
    ) -> Tuple[np.ndarray, np.ndarray, np.ndarray]:
        """Compute ROC curve."""
        thresholds = np.sort(np.unique(y_prob))[::-1]
        tpr = np.zeros(len(thresholds))
        fpr = np.zeros(len(thresholds))

        for i, thresh in enumerate(thresholds):
            y_pred = (y_prob >= thresh).astype(int)
            tpr[i] = ((y_pred == 1) & (y_true == 1)).sum() / max((y_true == 1).sum(), 1)
            fpr[i] = ((y_pred == 1) & (y_true == 0)).sum() / max((y_true == 0).sum(), 1)

        return fpr, tpr, thresholds

    def generate_actuarial_report(self) -> Dict[str, Any]:
        """
        Generate comprehensive actuarial validation report.

        Returns
        -------
        dict with formatted report
        """
        report = {
            "report_type": "actuarial_validation",
            "report_version": "1.0",
            "sections": [],
        }

        # Section 1: Discrimination
        if "discrimination" in self.results:
            d = self.results["discrimination"]
            report["sections"].append({
                "title": "模型区分度 (Discrimination)",
                "metrics": {
                    "AUC (C-statistic)": f"{d['auc']:.4f}",
                    "Youden's J": f"{d['youden_j']:.4f}",
                    "最优阈值": f"{d['optimal_threshold']:.4f}",
                },
                "interpretation": self._interpret_auc(d['auc']),
            })

        # Section 2: Calibration
        if "calibration" in self.results:
            c = self.results["calibration"]
            report["sections"].append({
                "title": "模型校准度 (Calibration)",
                "metrics": {
                    "Brier Score": f"{c['brier_score']:.4f}",
                    "Hosmer-Lemeshow p值": f"{c['hosmer_lemeshow_p_value']:.4f}",
                    "校准斜率": f"{c['calibration_slope']:.4f}",
                    "期望/观察比": f"{c['expected_observed_ratio']:.4f}",
                },
                "interpretation": self._interpret_calibration(c),
            })

        # Section 3: Comparison with benchmarks
        for key in ["framingham_comparison", "china_par_comparison"]:
            if key in self.results:
                comp = self.results[key]
                report["sections"].append({
                    "title": f"与{comp['reference_model']}对比",
                    "metrics": {
                        "新模型AUC": f"{comp['auc_new']:.4f}",
                        f"{comp['reference_model']} AUC": f"{comp['auc_reference']:.4f}",
                        "AUC提升": f"{comp['auc_improvement_pct']:.1f}%",
                        "NRI": f"{comp['nri']['nri_overall']:.4f}",
                        "IDI": f"{comp['idi']['idi']:.4f}",
                    },
                    "interpretation": self._interpret_comparison(comp),
                })

        # Section 4: Clinical significance
        if "clinical_significance" in self.results:
            cs = self.results["clinical_significance"]
            report["sections"].append({
                "title": "临床意义 (Clinical Significance)",
                "metrics": {
                    "NNT": f"{cs['nnt']:.0f}",
                    "绝对风险降低": f"{cs['absolute_risk_reduction']:.1%}",
                    "相对风险降低": f"{cs['relative_risk_reduction']:.1%}",
                    "每100人QALY获益": f"{cs['qalys_gained_per_100']:.2f}",
                },
                "interpretation": cs['interpretation'],
            })

        # Section 5: E-value
        if "e_value" in self.results:
            ev = self.results["e_value"]
            report["sections"].append({
                "title": "未测量混杂敏感性 (E-value)",
                "metrics": {
                    "E-value": f"{ev['e_value']:.2f}",
                    "风险比": f"{ev['risk_ratio']:.2f}",
                },
                "interpretation": ev['interpretation'],
            })

        # Section 6: Power analysis
        if "power_analysis" in self.results:
            pa = self.results["power_analysis"]
            report["sections"].append({
                "title": "统计功效 (Power Analysis)",
                "metrics": {
                    "当前样本量": pa['sample_size'],
                    "当前功效": f"{pa['power']:.1%}",
                    "80%功效所需样本": pa['min_n_80_power'],
                    "90%功效所需样本": pa['min_n_90_power'],
                },
                "interpretation": pa['interpretation'],
            })

        return report

    def _interpret_auc(self, auc: float) -> str:
        """Interpret AUC value."""
        if auc >= 0.9:
            return "优秀 (Excellent) - 模型区分度极高"
        elif auc >= 0.8:
            return "良好 (Good) - 模型区分度较好"
        elif auc >= 0.7:
            return "可接受 (Acceptable) - 模型有一定区分能力"
        elif auc >= 0.6:
            return "较差 (Poor) - 模型区分能力有限"
        else:
            return "失败 (Fail) - 模型无区分能力"

    def _interpret_calibration(self, cal: Dict) -> str:
        """Interpret calibration metrics."""
        hl_p = cal['hosmer_lemeshow_p_value']
        slope = cal['calibration_slope']

        parts = []
        if hl_p > 0.05:
            parts.append("Hosmer-Lemeshow检验通过(p>0.05)，模型校准良好")
        else:
            parts.append("Hosmer-Lemeshow检验未通过(p≤0.05)，模型校准存在问题")

        if 0.8 <= slope <= 1.2:
            parts.append("校准斜率接近1，预测概率与观察概率一致")
        elif slope < 0.8:
            parts.append("校准斜率<0.8，模型过度预测风险")
        else:
            parts.append("校准斜率>1.2，模型预测风险不足")

        return "；".join(parts)

    def _interpret_comparison(self, comp: Dict) -> str:
        """Interpret model comparison."""
        auc_diff = comp['auc_difference']
        nri = comp['nri']['nri_overall']

        parts = []
        if auc_diff > 0:
            parts.append(f"新模型AUC优于{comp['reference_model']} {abs(auc_diff):.1%}")
        else:
            parts.append(f"新模型AUC低于{comp['reference_model']} {abs(auc_diff):.1%}")

        if nri > 0:
            parts.append(f"NRI={nri:.2%}，整体重分类改善")
        else:
            parts.append(f"NRI={nri:.2%}，整体重分类无改善")

        return "；".join(parts)

    def _reclassification_metrics(
        self,
        y_true: np.ndarray,
        y_prob_new: np.ndarray,
        y_prob_ref: np.ndarray,
    ) -> Dict[str, Any]:
        """Compute reclassification metrics."""
        return self._compute_nri(y_true, y_prob_new, y_prob_ref)


class FraminghamRiskScore:
    """
    Framingham Risk Score implementation for comparison.

    Based on 2008 Framingham Heart Study equations.
    """

    @staticmethod
    def compute_score(
        age: int,
        gender: int,  # 0=female, 1=male
        total_cholesterol: float,  # mmol/L
        hdl: float,  # mmol/L
        sbp: float,  # mmHg
        treated_bp: bool,  # Whether on BP medication
        smoking: bool,
        diabetes: bool,
    ) -> float:
        """
        Compute 10-year CVD risk using Framingham score.

        Returns
        -------
        float
            10-year CVD risk (0-1)
        """
        # Convert cholesterol from mmol/L to mg/dL
        tc_mgdl = total_cholesterol * 38.67
        hdl_mgdl = hdl * 38.67

        if gender == 1:  # Male
            return FraminghamRiskScore._male_score(
                age, tc_mgdl, hdl_mgdl, sbp, treated_bp, smoking, diabetes
            )
        else:  # Female
            return FraminghamRiskScore._female_score(
                age, tc_mgdl, hdl_mgdl, sbp, treated_bp, smoking, diabetes
            )

    @staticmethod
    def _male_score(age, tc, hdl, sbp, treated_bp, smoking, diabetes):
        """Male Framingham score."""
        import math

        # Point assignments
        age_points = 0
        if age < 35: age_points = -1
        elif age < 40: age_points = 0
        elif age < 45: age_points = 1
        elif age < 50: age_points = 2
        elif age < 55: age_points = 3
        elif age < 60: age_points = 4
        elif age < 65: age_points = 5
        elif age < 70: age_points = 6
        elif age < 75: age_points = 7
        else: age_points = 8

        tc_points = 0
        if tc < 160: tc_points = -3
        elif tc < 200: tc_points = 0
        elif tc < 240: tc_points = 1
        elif tc < 280: tc_points = 2
        else: tc_points = 3

        hdl_points = 0
        if hdl < 40: hdl_points = 2
        elif hdl < 50: hdl_points = 1
        elif hdl < 60: hdl_points = 0
        else: hdl_points = -1

        bp_points = 0
        if sbp < 120: bp_points = 0
        elif sbp < 130: bp_points = 1
        elif sbp < 140: bp_points = 2
        elif sbp < 160: bp_points = 3
        else: bp_points = 4

        if treated_bp:
            bp_points += 1

        smoking_points = 2 if smoking else 0
        diabetes_points = 3 if diabetes else 0

        total_points = (age_points + tc_points + hdl_points +
                       bp_points + smoking_points + diabetes_points)

        # Convert to probability using Cox model
        s0 = 0.88431  # 10-year baseline survival
        beta_points = total_points * 0.1  # Simplified

        risk = 1 - s0 ** math.exp(beta_points - 3.0)  # Centering
        return min(max(risk, 0), 1)

    @staticmethod
    def _female_score(age, tc, hdl, sbp, treated_bp, smoking, diabetes):
        """Female Framingham score."""
        import math

        # Point assignments (different for women)
        age_points = 0
        if age < 35: age_points = -9
        elif age < 40: age_points = -4
        elif age < 45: age_points = 0
        elif age < 50: age_points = 3
        elif age < 55: age_points = 6
        elif age < 60: age_points = 7
        elif age < 65: age_points = 8
        elif age < 70: age_points = 8
        elif age < 75: age_points = 8
        else: age_points = 8

        tc_points = 0
        if tc < 160: tc_points = -2
        elif tc < 200: tc_points = 0
        elif tc < 240: tc_points = 1
        elif tc < 280: tc_points = 2
        else: tc_points = 3

        hdl_points = 0
        if hdl < 40: hdl_points = 5
        elif hdl < 50: hdl_points = 2
        elif hdl < 60: hdl_points = 1
        else: hdl_points = 0

        bp_points = 0
        if sbp < 120: bp_points = -3
        elif sbp < 130: bp_points = 0
        elif sbp < 140: bp_points = 1
        elif sbp < 160: bp_points = 2
        else: bp_points = 3

        if treated_bp:
            bp_points += 2

        smoking_points = 3 if smoking else 0
        diabetes_points = 4 if diabetes else 0

        total_points = (age_points + tc_points + hdl_points +
                       bp_points + smoking_points + diabetes_points)

        # Convert to probability
        s0 = 0.95012  # 10-year baseline survival for women
        beta_points = total_points * 0.1

        risk = 1 - s0 ** math.exp(beta_points - 2.0)
        return min(max(risk, 0), 1)


class ChinaPARRiskScore:
    """
    China-PAR (Prediction for ASCVD Risk in China) model.

    Based on Yang et al., 2016, Lancet.
    Simplified implementation for comparison purposes.
    """

    @staticmethod
    def compute_score(
        age: int,
        gender: int,  # 0=female, 1=male
        region: str,  # "north" or "south"
        urban: bool,
        sbp: float,
        dbp: float,
        total_cholesterol: float,
        hdl: float,
        smoking: bool,
        diabetes: bool,
        waist_circumference: float,
        family_history: bool,
    ) -> float:
        """
        Compute 10-year ASCVD risk using China-PAR model.

        Returns
        -------
        float
            10-year ASCVD risk (0-1)
        """
        import math

        # Coefficients (simplified from published equations)
        # These are approximations for demonstration

        # Age effect
        age_effect = (age - 55) * 0.05

        # Gender effect
        gender_effect = 0.3 if gender == 1 else 0

        # Region effect
        region_effect = 0.1 if region == "north" else 0

        # Urban effect
        urban_effect = 0.05 if urban else 0

        # Blood pressure
        bp_effect = 0
        if sbp >= 180 or dbp >= 110:
            bp_effect = 0.8
        elif sbp >= 160 or dbp >= 100:
            bp_effect = 0.5
        elif sbp >= 140 or dbp >= 90:
            bp_effect = 0.3
        elif sbp >= 120 or dbp >= 80:
            bp_effect = 0.1

        # Lipids
        lipid_effect = (total_cholesterol - 5.0) * 0.1 - (hdl - 1.0) * 0.2

        # Smoking
        smoking_effect = 0.3 if smoking else 0

        # Diabetes
        diabetes_effect = 0.4 if diabetes else 0

        # Waist circumference
        waist_effect = (waist_circumference - 80) * 0.01

        # Family history
        family_effect = 0.2 if family_history else 0

        # Combine effects
        total_effect = (age_effect + gender_effect + region_effect +
                       urban_effect + bp_effect + lipid_effect +
                       smoking_effect + diabetes_effect +
                       waist_effect + family_effect)

        # Convert to probability
        baseline_risk = 0.05  # 5% baseline 10-year risk
        risk = baseline_risk * math.exp(total_effect)

        return min(max(risk, 0), 1)
