"""
Individual Attribution (Level 1)

Predicts individual CVD risk trajectory and quantifies
the effect of health interventions for a single patient.

Key Features:
- Exponential decay risk prediction
- Confidence intervals
- Paired ATT for individual
- Trend analysis
- Personalized recommendations
"""

import numpy as np
from typing import Dict, List, Optional, Tuple
from datetime import datetime, date
from pydantic import BaseModel, Field


class IndividualResult(BaseModel):
    """Individual attribution result."""
    status: str = Field(..., description="状态: ready/accumulating/error")

    # Current state
    current_risk_score: float = Field(..., description="当前风险评分")
    risk_level: str = Field(..., description="风险等级")

    # Trend analysis
    trend_slope_overall: float = Field(..., description="整体趋势斜率")
    trend_slope_intervention: float = Field(..., description="干预趋势斜率")
    trend_direction: str = Field(..., description="趋势方向: improving/stable/worsening")

    # Forecasts
    forecast_days: int = Field(..., description="预测天数")
    forecast_status_quo: List[float] = Field(default_factory=list, description="不干预预测")
    forecast_with_plan: List[float] = Field(default_factory=list, description="干预预测")
    forecast_ci_upper: List[float] = Field(default_factory=list, description="置信区间上限")
    forecast_ci_lower: List[float] = Field(default_factory=list, description="置信区间下限")

    # Projections
    projected_risk_30d_no_action: float = Field(..., description="30天无干预风险")
    projected_risk_30d_with_plan: float = Field(..., description="30天干预风险")
    risk_reduction_30d: float = Field(..., description="30天风险降低")

    # Individual ATT
    individual_att: Optional[float] = Field(None, description="个体干预效果")
    att_ci_lower: Optional[float] = Field(None, description="ATT置信区间下限")
    att_ci_upper: Optional[float] = Field(None, description="ATT置信区间上�")
    att_p_value: Optional[float] = Field(None, description="ATT p值")
    att_significant: Optional[bool] = Field(None, description="ATT是否显著")

    # Intervention metrics
    intervention_days: int = Field(default=0, description="干预天数")
    intervention_adherence: float = Field(default=0, description="干预依从性")
    intervention_data_sufficient: bool = Field(default=False, description="干预数据是否充分")

    # Report
    report_text: Dict = Field(default_factory=dict, description="报告文本")

    # Metadata
    history_days: int = Field(..., description="历史数据天数")
    min_history_days: int = Field(default=14, description="最少历史天数")
    computed_at: datetime = Field(default_factory=datetime.now)


class IndividualAttributor:
    """
    Individual-level CVD risk prediction and intervention attribution.

    Uses exponential decay model to predict risk trajectory and
    quantifies individual intervention effect using paired comparisons.
    """

    def __init__(self, config: Dict = None):
        config = config or {}
        self.min_history_days = config.get("min_history_days", 14)
        self.forecast_days = config.get("forecast_days", 30)
        self.decay_factor = config.get("decay_factor", 0.95)
        self.min_intervention_days = config.get("min_intervention_days", 7)

    def attribute(
        self,
        risk_history: List[Dict],
        intervention_start_date: Optional[str] = None,
    ) -> IndividualResult:
        """
        Run individual-level attribution.

        Parameters
        ----------
        risk_history : list of dict
            Each item: {"date": "2026-05-01", "Y": 0.52, "Z": 1}
            Y = risk score, Z = 1 if intervention day else 0.
        intervention_start_date : str, optional
            Intervention start date for analysis

        Returns
        -------
        IndividualResult
        """
        N = len(risk_history)

        # Check if enough data
        if N < self.min_history_days:
            return IndividualResult(
                status="accumulating",
                current_risk_score=risk_history[-1]["Y"] if risk_history else 0,
                risk_level="unknown",
                trend_slope_overall=0,
                trend_slope_intervention=0,
                trend_direction="unknown",
                forecast_days=self.forecast_days,
                projected_risk_30d_no_action=0,
                projected_risk_30d_with_plan=0,
                risk_reduction_30d=0,
                history_days=N,
                min_history_days=self.min_history_days,
            )

        # Extract data
        Y = np.array([r["Y"] for r in risk_history])
        Z = np.array([r["Z"] for r in risk_history])
        t = np.arange(N, dtype=float)

        # Weights (exponential decay)
        weights = self.decay_factor ** np.arange(N - 1, -1, -1)

        # Log-transform for exponential model
        Y_safe = np.clip(Y, 0.01, 0.99)
        log_Y = np.log(Y_safe)

        # Fit overall trend
        rate_all, log_intercept_all = self._weighted_fit(t, log_Y, weights)

        # Residuals for uncertainty
        residuals = log_Y - (log_intercept_all + rate_all * t)
        w_sum = weights.sum()
        sigma = np.sqrt(np.sum(weights * residuals**2) / w_sum)

        # Intervention trend
        intervention_mask = Z == 1
        n_intervention = int(intervention_mask.sum())
        intervention_data_sufficient = n_intervention >= self.min_intervention_days

        if intervention_data_sufficient:
            t_int = t[intervention_mask]
            log_Y_int = log_Y[intervention_mask]
            w_int = weights[intervention_mask]
            rate_plan, log_intercept_plan = self._weighted_fit(t_int, log_Y_int, w_int)
        else:
            rate_plan = rate_all
            log_intercept_plan = log_intercept_all

        # Current risk
        last_Y = float(Y[-1])

        # Forecasts
        future_t = np.arange(1, self.forecast_days + 1, dtype=float)

        forecast_no_action = np.clip(last_Y * np.exp(rate_all * future_t), 0, 1)
        forecast_with_plan = np.clip(last_Y * np.exp(rate_plan * future_t), 0, 1)

        # Confidence intervals
        prediction_se = sigma * np.sqrt(1 + 1.0 / N)
        ci_multiplier = 1.96 * prediction_se

        forecast_ci_upper = np.clip(
            last_Y * np.exp(rate_all * future_t + ci_multiplier), 0, 1
        )
        forecast_ci_lower = np.clip(
            last_Y * np.exp(rate_all * future_t - ci_multiplier), 0, 1
        )

        # Individual ATT
        att_result = self._compute_individual_att(Y, Z)

        # Projections
        proj_no = float(forecast_no_action[-1])
        proj_plan = float(forecast_with_plan[-1])
        risk_reduction = proj_no - proj_plan

        # Risk level
        risk_level = self._get_risk_level(last_Y)

        # Trend direction
        if rate_all < -1e-5:
            trend_direction = "improving"
        elif rate_all > 1e-5:
            trend_direction = "worsening"
        else:
            trend_direction = "stable"

        # Intervention adherence
        adherence = Z.mean() if N > 0 else 0

        # Generate report
        report = self._generate_report(
            last_Y, proj_no, proj_plan, risk_reduction,
            rate_all, rate_plan, N, n_intervention,
            intervention_data_sufficient, att_result,
            float(forecast_ci_upper[-1] - forecast_ci_lower[-1]),
        )

        return IndividualResult(
            status="ready",
            current_risk_score=last_Y,
            risk_level=risk_level,
            trend_slope_overall=float(rate_all),
            trend_slope_intervention=float(rate_plan),
            trend_direction=trend_direction,
            forecast_days=self.forecast_days,
            forecast_status_quo=forecast_no_action.tolist(),
            forecast_with_plan=forecast_with_plan.tolist(),
            forecast_ci_upper=forecast_ci_upper.tolist(),
            forecast_ci_lower=forecast_ci_lower.tolist(),
            projected_risk_30d_no_action=proj_no,
            projected_risk_30d_with_plan=proj_plan,
            risk_reduction_30d=risk_reduction,
            individual_att=att_result.get("att"),
            att_ci_lower=att_result.get("ci_lower"),
            att_ci_upper=att_result.get("ci_upper"),
            att_p_value=att_result.get("p_value"),
            att_significant=att_result.get("significant"),
            intervention_days=n_intervention,
            intervention_adherence=adherence,
            intervention_data_sufficient=intervention_data_sufficient,
            report_text=report,
            history_days=N,
        )

    def _compute_individual_att(
        self, Y: np.ndarray, Z: np.ndarray
    ) -> Dict:
        """
        Compute individual-level Average Treatment Effect on Treated.

        Uses paired comparison of intervention vs non-intervention days.
        """
        n_intervention = int((Z == 1).sum())
        n_control = int((Z == 0).sum())

        if n_intervention < 7 or n_control < 7:
            return {
                "att": None,
                "ci_lower": None,
                "ci_upper": None,
                "p_value": None,
                "significant": None,
                "message": "干预数据不足7天，无法计算个体ATT",
            }

        # Paired ATT by week
        N = len(Y)
        days_per_week = 7
        n_weeks = N // days_per_week
        paired_diffs = []

        for w in range(n_weeks):
            start = w * days_per_week
            end = start + days_per_week
            week_Y = Y[start:end]
            week_Z = Z[start:end]
            int_mask = week_Z == 1
            ctrl_mask = week_Z == 0
            if int_mask.sum() > 0 and ctrl_mask.sum() > 0:
                paired_diffs.append(
                    float(week_Y[int_mask].mean() - week_Y[ctrl_mask].mean())
                )

        if len(paired_diffs) < 2:
            # Fallback: simple comparison
            att = float(Y[Z == 1].mean() - Y[Z == 0].mean())
            return {
                "att": att,
                "ci_lower": None,
                "ci_upper": None,
                "p_value": None,
                "significant": None,
                "message": "配对数据不足，使用简单比较",
            }

        diffs = np.array(paired_diffs)
        att = float(diffs.mean())

        # Bootstrap CI
        rng = np.random.default_rng(42)
        boot_atts = []
        for _ in range(200):
            idx = rng.choice(len(diffs), size=len(diffs), replace=True)
            boot_atts.append(float(diffs[idx].mean()))

        boot_arr = np.array(boot_atts)
        ci_lower = float(np.percentile(boot_arr, 2.5))
        ci_upper = float(np.percentile(boot_arr, 97.5))

        # P-value (Wilcoxon signed-rank test)
        from scipy.stats import wilcoxon
        try:
            _, p_value = wilcoxon(diffs, alternative="two-sided")
            p_value = float(p_value)
        except:
            p_value = 1.0

        significant = p_value < 0.05 and ci_upper < 0

        return {
            "att": att,
            "ci_lower": ci_lower,
            "ci_upper": ci_upper,
            "p_value": p_value,
            "significant": significant,
            "n_weeks": len(paired_diffs),
            "message": f"基于{len(paired_diffs)}周配对数据",
        }

    def _weighted_fit(
        self, x: np.ndarray, y: np.ndarray, w: np.ndarray
    ) -> Tuple[float, float]:
        """Weighted least squares: beta = (X^T W X)^{-1} X^T W y."""
        X = np.column_stack([x, np.ones_like(x)])
        W = np.diag(w)
        XtW = X.T @ W
        beta = np.linalg.solve(XtW @ X, XtW @ y)
        return float(beta[0]), float(beta[1])

    def _get_risk_level(self, score: float) -> str:
        """Get risk level from score."""
        if score < 0.3:
            return "low"
        elif score < 0.5:
            return "moderate"
        elif score < 0.7:
            return "high"
        else:
            return "very_high"

    def _generate_report(
        self,
        current: float,
        proj_no: float,
        proj_plan: float,
        risk_reduction: float,
        rate_all: float,
        rate_plan: float,
        history_days: int,
        intervention_days: int,
        intervention_sufficient: bool,
        att_result: Dict,
        ci_width: float,
    ) -> Dict:
        """Generate individual attribution report."""
        # Trend description
        if rate_all < -1e-5:
            trend_desc = "下降"
            trend_emoji = "📉"
        elif rate_all > 1e-5:
            trend_desc = "上升"
            trend_emoji = "📈"
        else:
            trend_desc = "平稳"
            trend_emoji = "➡️"

        # Main headline
        if not intervention_sufficient:
            headline = "⏳ 干预数据积累中"
            body = (
                f"过去 {history_days} 天中执行了 {intervention_days} 天健康计划（不足{self.min_intervention_days}天），"
                f"当前心血管风险为 {current:.1%}，整体趋势{trend_desc}。\n"
                f"干预数据不足以独立评估计划效果，建议继续坚持计划。"
            )
            advice = "建议持续执行健康计划，积累足够数据后可获得更精准的效果评估。"
        elif risk_reduction > 0.01:
            headline = "✅ 健康计划有效"
            body = (
                f"过去 {history_days} 天中执行了 {intervention_days} 天健康计划，"
                f"当前心血管风险为 {current:.1%}。\n"
                f"若继续坚持计划，预计 {self.forecast_days} 天后风险可降至 {proj_plan:.1%}；"
                f"若停止干预，风险预计为 {proj_no:.1%}。\n"
                f"坚持计划可多降低约 {risk_reduction:.1%} 的风险。"
            )
            advice = "建议维持当前饮食和运动方案，定期复测。"
        elif risk_reduction > -0.01:
            headline = "➡️ 风险趋势平稳"
            body = (
                f"过去 {history_days} 天您的风险整体{trend_desc}，"
                f"当前为 {current:.1%}。\n"
                f"计划执行与否的 {self.forecast_days} 天差异较小（{abs(risk_reduction):.1%}），"
                f"建议与医生沟通调整方案。"
            )
            advice = "当前方案效果不明显，可考虑加强运动强度或调整饮食。"
        else:
            headline = "⚠️ 需要关注"
            body = (
                f"过去 {history_days} 天中执行了 {intervention_days} 天计划，"
                f"但风险呈{trend_desc}趋势，当前为 {current:.1%}。\n"
                f"建议尽快就医复查相关指标。"
            )
            advice = "建议预约医生复诊，排查可能的基础疾病变化。"

        # ATT summary
        att_summary = None
        if att_result.get("att") is not None:
            att = att_result["att"]
            if att < 0:
                att_summary = f"干预日平均风险比非干预日低 {abs(att):.1%}"
            else:
                att_summary = f"干预日平均风险比非干预日高 {abs(att):.1%}"

            if att_result.get("p_value") is not None:
                att_summary += f" (p={att_result['p_value']:.3f})"

        # Metrics
        metrics = {
            "current_risk": f"{current:.1%}",
            "projected_no_action": f"{proj_no:.1%}",
            "projected_with_plan": f"{proj_plan:.1%}",
            "plan_benefit": f"{risk_reduction:.1%}",
            "confidence_band_width": f"{ci_width:.1%}",
            "trend_slope": f"{rate_all:.4f}",
            "intervention_days": intervention_days,
            "intervention_adherence": f"{intervention_days/history_days:.1%}",
        }

        if att_result.get("att") is not None:
            metrics["individual_att"] = f"{att_result['att']:.1%}"
            metrics["att_p_value"] = f"{att_result.get('p_value', 'N/A')}"

        return {
            "headline": headline,
            "trend_emoji": trend_emoji,
            "body": body,
            "advice": advice,
            "att_summary": att_summary,
            "metrics": metrics,
        }
