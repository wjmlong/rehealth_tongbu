"""
Level 1 individual risk prediction — exponential decay model with
confidence intervals and paired ATT.

Part of bodyup_cloud.engine — V1 specification.
"""

import numpy as np
from typing import List, Dict


class IndividualRiskPredictor:
    """Forecast an individual's CVD risk trajectory using time-series
    weighted regression on daily risk scores."""

    def __init__(self, config: dict | None = None):
        config = config or {}
        self.min_history_days = config.get("min_history_days", 14)
        self.forecast_days = config.get("forecast_days", 30)
        self.decay_factor = config.get("decay_factor", 0.95)

    def predict(self, risk_history: List[Dict]) -> dict:
        """Run the individual-level prediction.

        Parameters
        ----------
        risk_history : list of dict
            Each item: {"date": "2026-05-01", "Y": 0.52, "Z": 1}
            Y = risk score, Z = 1 if intervention day else 0.

        Returns
        -------
        dict with forecast arrays, trend slopes, and retrospective ATT.
        """
        N = len(risk_history)
        if N < self.min_history_days:
            return {
                "status": "accumulating",
                "days_available": N,
                "days_needed": self.min_history_days,
            }

        Y = np.array([r["Y"] for r in risk_history])
        Z = np.array([r["Z"] for r in risk_history])
        t = np.arange(N, dtype=float)

        weights = self.decay_factor ** np.arange(N - 1, -1, -1)

        Y_safe = np.clip(Y, 0.01, 0.99)
        log_Y = np.log(Y_safe)

        rate_all, log_intercept_all = self._weighted_fit(t, log_Y, weights)

        residuals = log_Y - (log_intercept_all + rate_all * t)
        w_sum = weights.sum()
        sigma = np.sqrt(np.sum(weights * residuals**2) / w_sum)

        intervention_mask = Z == 1
        n_intervention = int(intervention_mask.sum())
        intervention_data_sufficient = n_intervention >= 7

        if intervention_data_sufficient:
            t_int = t[intervention_mask]
            log_Y_int = log_Y[intervention_mask]
            w_int = weights[intervention_mask]
            rate_plan, log_intercept_plan = self._weighted_fit(t_int, log_Y_int, w_int)
        else:
            rate_plan = rate_all
            log_intercept_plan = log_intercept_all

        last_Y = float(Y[-1])
        future_t = np.arange(1, self.forecast_days + 1, dtype=float)

        forecast_no_action = np.clip(last_Y * np.exp(rate_all * future_t), 0, 1)
        forecast_with_plan = np.clip(last_Y * np.exp(rate_plan * future_t), 0, 1)

        prediction_se = sigma * np.sqrt(1 + 1.0 / N)
        ci_multiplier = 1.96 * prediction_se

        forecast_ci_upper = np.clip(
            last_Y * np.exp(rate_all * future_t + ci_multiplier), 0, 1
        )
        forecast_ci_lower = np.clip(
            last_Y * np.exp(rate_all * future_t - ci_multiplier), 0, 1
        )

        att = self._paired_att(Y, Z)

        proj_no = float(forecast_no_action[-1])
        proj_plan = float(forecast_with_plan[-1])

        return {
            "status": "ready",
            "current_risk_score": last_Y,
            "trend_slope_overall": float(rate_all),
            "trend_slope_intervention": float(rate_plan),
            "forecast_status_quo": forecast_no_action.tolist(),
            "forecast_with_plan": forecast_with_plan.tolist(),
            "forecast_ci_upper": forecast_ci_upper.tolist(),
            "forecast_ci_lower": forecast_ci_lower.tolist(),
            "projected_risk_30d_no_action": proj_no,
            "projected_risk_30d_with_plan": proj_plan,
            "retrospective_att": att,
            "intervention_data_sufficient": intervention_data_sufficient,
            "history_days": N,
            "intervention_days": n_intervention,
            "report_text": self._render_report(
                last_Y, proj_no, proj_plan, att,
                float(rate_all), float(rate_plan), N, n_intervention,
                intervention_data_sufficient,
                float(forecast_ci_upper[-1] - forecast_ci_lower[-1]),
            ),
        }

    @staticmethod
    def _paired_att(Y: np.ndarray, Z: np.ndarray) -> float | None:
        n_intervention = int((Z == 1).sum())
        n_control = int((Z == 0).sum())
        if n_intervention < 7 or n_control < 7:
            return None

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

        if len(paired_diffs) >= 2:
            return float(np.mean(paired_diffs))
        return float(Y[Z == 1].mean() - Y[Z == 0].mean())

    @staticmethod
    def _render_report(
        current: float, proj_no: float, proj_plan: float, att,
        rate_all: float, rate_plan: float,
        history_days: int, intervention_days: int,
        intervention_sufficient: bool,
        ci_width: float,
    ) -> dict:
        delta = proj_no - proj_plan
        trend_word = "下降" if rate_all < -1e-5 else ("上升" if rate_all > 1e-5 else "平稳")

        if not intervention_sufficient:
            headline = "干预数据不足，暂以整体趋势预测"
            body = (
                f"过去 {history_days} 天中仅执行了 {intervention_days} 天健康计划（不足7天），"
                f"当前心血管风险为 {current:.1%}，整体趋势{trend_word}。\n"
                f"干预数据不足以独立评估计划效果，建议继续坚持计划至少7天后再评估。"
            )
            advice = "建议持续执行健康计划，积累足够数据后可获得更精准的效果评估。"
        elif delta > 0.005:
            headline = "健康计划有效，继续保持"
            body = (
                f"过去 {history_days} 天中您执行了 {intervention_days} 天健康计划，"
                f"当前心血管风险为 {current:.1%}。\n"
                f"若继续坚持计划，预计 30 天后风险可降至 {proj_plan:.1%}；"
                f"若停止干预，风险预计为 {proj_no:.1%}。"
                f"坚持计划可多降低约 {delta:.1%} 的风险。"
            )
            advice = "建议维持当前饮食和运动方案，定期复测。"
        elif delta > -0.005:
            headline = "风险趋势平稳"
            body = (
                f"过去 {history_days} 天您的风险整体{trend_word}，"
                f"当前为 {current:.1%}。"
                f"计划执行与否的 30 天差异较小（{abs(delta):.1%}），"
                f"建议与医生沟通调整方案。"
            )
            advice = "当前方案效果不明显，可考虑加强运动强度或调整饮食。"
        else:
            headline = "需要关注：风险未如预期改善"
            body = (
                f"过去 {history_days} 天中虽执行了 {intervention_days} 天计划，"
                f"但风险呈{trend_word}趋势，当前为 {current:.1%}。"
                f"建议尽快就医复查相关指标。"
            )
            advice = "建议预约医生复诊，排查可能的基础疾病变化。"

        att_text = None
        if att is not None:
            direction = "低" if att < 0 else "高"
            att_text = f"干预日平均风险比非干预日{direction} {abs(att):.1%}"

        return {
            "headline": headline,
            "body": body,
            "advice": advice,
            "att_summary": att_text,
            "metrics": {
                "current_risk": f"{current:.1%}",
                "projected_no_action": f"{proj_no:.1%}",
                "projected_with_plan": f"{proj_plan:.1%}",
                "plan_benefit": f"{delta:.1%}",
                "confidence_band_width": f"{ci_width:.1%}",
            },
        }

    @staticmethod
    def _weighted_fit(x: np.ndarray, y: np.ndarray, w: np.ndarray):
        """Weighted least squares: beta = (X^T W X)^{-1} X^T W y.

        Returns (slope, intercept).
        """
        X = np.column_stack([x, np.ones_like(x)])
        W = np.diag(w)
        XtW = X.T @ W
        beta = np.linalg.solve(XtW @ X, XtW @ y)
        return float(beta[0]), float(beta[1])
