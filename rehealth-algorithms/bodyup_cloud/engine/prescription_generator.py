"""
LLM-powered personalised intervention prescription generator.

Part of bodyup_cloud.engine — V1 specification.
"""

from bodyup_cloud.engine.llm_provider import LLMProvider


class PrescriptionGenerator:
    """Generate a JSON intervention prescription via LLM."""

    SYSTEM_PROMPT = (
        "你是睿禾健康的专业心脑血管健康顾问。\n"
        "根据用户的风险评分、特征贡献和健康记忆，生成个性化干预处方。\n"
        "要求：具体、可执行、考虑用户的历史依从性。\n"
        '输出格式：JSON {"diet": "...", "exercise": "...", "sleep": "...", '
        '"medication_reminder": "...", "expected_risk_reduction": 0.0}'
    )

    def __init__(self, llm: LLMProvider):
        self.llm = llm

    def generate(self, risk_result: dict, memory_snapshot: dict) -> str:
        """Build a user-context prompt and call the LLM.

        Parameters
        ----------
        risk_result : dict
            Output of CVDRiskScorer.predict().
        memory_snapshot : dict
            User health profile keys such as age_bracket, bp_variability,
            night_bp_pattern, activity_level, sleep_quality_index,
            intervention_compliance.

        Returns
        -------
        str  — raw LLM response (expected to be JSON).
        """
        prompt = f"""
用户风险评估结果：
- 风险分: {risk_result['risk_score']:.3f} ({risk_result['risk_level']})
- 主要风险因子: {self._top_factors(risk_result.get('feature_contributions', {}))}

用户健康画像：
- 年龄段: {memory_snapshot.get('age_bracket', '未知')}
- 血压波动性: {memory_snapshot.get('bp_variability', '未知')}
- 夜间血压模式: {memory_snapshot.get('night_bp_pattern', '未知')}
- 活动水平: {memory_snapshot.get('activity_level', '未知')}
- 睡眠质量: {memory_snapshot.get('sleep_quality_index', '未知')}
- 干预依从率: {memory_snapshot.get('intervention_compliance', '未知')}

请生成个性化干预处方。"""
        return self.llm.generate(self.SYSTEM_PROMPT, prompt)

    @staticmethod
    def _top_factors(contributions: dict, n: int = 5) -> str:
        """Return the top-*n* absolute SHAP contributors as a compact string."""
        sorted_factors = sorted(
            contributions.items(), key=lambda x: abs(x[1]), reverse=True
        )[:n]
        return ", ".join(f"{k}({v:+.3f})" for k, v in sorted_factors)
