"""
Prescription Generator — Rule-based intervention recommendations.

Part of PIAS engine - Intervene module.
Simplified version for HealthAgent integration.
"""

from typing import Dict, Optional


class PrescriptionGenerator:
    """Generate personalized intervention prescriptions based on risk assessment."""

    def generate(self, risk_result: Dict, patient_context: Optional[Dict] = None) -> Dict:
        """
        Generate intervention prescription.

        Parameters
        ----------
        risk_result : dict
            Output from CVDRiskScorer.predict()
        patient_context : dict, optional
            Additional patient context (age, gender, etc.)

        Returns
        -------
        dict with prescription details
        """
        risk_level = risk_result.get("risk_level", "moderate")
        risk_score = risk_result.get("risk_score", 0.5)
        feature_contributions = risk_result.get("feature_contributions", {})

        # Identify top risk factors
        top_factors = self._identify_risk_factors(feature_contributions)

        # Generate base prescription
        prescription = self._base_prescription(risk_level)

        # Add personalized recommendations based on top risk factors
        personalized = self._personalize(top_factors, patient_context or {})

        prescription["personalized_recommendations"] = personalized
        prescription["risk_factors"] = top_factors
        prescription["follow_up"] = self._follow_up_schedule(risk_level)

        return prescription

    def _identify_risk_factors(self, contributions: Dict) -> list:
        """Identify top contributing risk factors."""
        if not contributions:
            return []

        # Sort by absolute contribution
        sorted_factors = sorted(
            contributions.items(),
            key=lambda x: abs(x[1]),
            reverse=True
        )

        # Return top 3 negative contributors (increase risk)
        top_factors = []
        for factor, value in sorted_factors[:5]:
            if value > 0:  # Positive SHAP = increases risk
                top_factors.append({
                    "factor": factor,
                    "contribution": value,
                    "description": self._factor_description(factor)
                })

        return top_factors[:3]

    def _factor_description(self, factor: str) -> str:
        """Get human-readable description of risk factor."""
        descriptions = {
            "age": "年龄",
            "gender": "性别",
            "bmi": "BMI指数",
            "sbp": "收缩压",
            "dbp": "舒张压",
            "fasting_glucose": "空腹血糖",
            "total_cholesterol": "总胆固醇",
            "ldl": "低密度脂蛋白",
            "hdl": "高密度脂蛋白",
            "triglycerides": "甘油三酯",
            "exercise_days": "运动频率",
            "smoking": "吸烟",
            "drinking": "饮酒",
            "diabetes_history": "糖尿病史",
            "hypertension_history": "高血压史",
            "family_history": "家族病史",
        }
        return descriptions.get(factor, factor)

    def _base_prescription(self, risk_level: str) -> Dict:
        """Generate base prescription based on risk level."""
        prescriptions = {
            "low": {
                "summary": "风险较低，保持健康生活方式",
                "lifestyle": [
                    "每周至少150分钟中等强度运动",
                    "保持均衡饮食，多吃蔬菜水果",
                    "保持健康体重",
                    "每年体检一次"
                ],
                "medication": "无需药物干预",
                "urgency": "routine"
            },
            "moderate": {
                "summary": "风险中等，需要积极干预",
                "lifestyle": [
                    "每日30分钟快走或慢跑",
                    "控制盐摄入<6g/天",
                    "减少饱和脂肪摄入",
                    "戒烟限酒",
                    "保证7-8小时睡眠"
                ],
                "medication": "建议咨询医生是否需要药物",
                "urgency": "moderate"
            },
            "high": {
                "summary": "风险较高，建议就医",
                "lifestyle": [
                    "立即预约心内科",
                    "每日监测血压",
                    "严格控制饮食",
                    "避免剧烈运动",
                    "保持情绪稳定"
                ],
                "medication": "可能需要降压/降脂药物",
                "urgency": "high"
            },
            "very_high": {
                "summary": "风险极高，立即就医",
                "lifestyle": [
                    "紧急就医",
                    "卧床休息",
                    "避免剧烈运动",
                    "准备住院检查"
                ],
                "medication": "需要药物治疗",
                "urgency": "emergency"
            }
        }
        return prescriptions.get(risk_level, prescriptions["moderate"])

    def _personalize(self, top_factors: list, context: Dict) -> list:
        """Generate personalized recommendations based on risk factors."""
        recommendations = []

        for factor_info in top_factors:
            factor = factor_info["factor"]

            if factor == "sbp" or factor == "dbp":
                recommendations.append({
                    "target": "血压管理",
                    "actions": [
                        "每日定时测量血压",
                        "减少钠盐摄入",
                        "增加钾摄入（香蕉、菠菜）",
                        "规律有氧运动"
                    ]
                })
            elif factor == "fasting_glucose":
                recommendations.append({
                    "target": "血糖控制",
                    "actions": [
                        "控制碳水化合物摄入",
                        "增加膳食纤维",
                        "餐后散步15分钟",
                        "定期监测血糖"
                    ]
                })
            elif factor in ["total_cholesterol", "ldl", "triglycerides"]:
                recommendations.append({
                    "target": "血脂管理",
                    "actions": [
                        "减少饱和脂肪摄入",
                        "增加Omega-3脂肪酸",
                        "多吃鱼类和坚果",
                        "定期复查血脂"
                    ]
                })
            elif factor == "bmi":
                recommendations.append({
                    "target": "体重管理",
                    "actions": [
                        "控制每日热量摄入",
                        "增加运动量",
                        "记录饮食日记",
                        "设定合理减重目标"
                    ]
                })
            elif factor == "smoking":
                recommendations.append({
                    "target": "戒烟",
                    "actions": [
                        "制定戒烟计划",
                        "寻求戒烟支持",
                        "使用尼古丁替代疗法",
                        "避免吸烟环境"
                    ]
                })
            elif factor == "drinking":
                recommendations.append({
                    "target": "限酒",
                    "actions": [
                        "男性每日≤2杯，女性≤1杯",
                        "避免酗酒",
                        "选择低度酒",
                        "记录饮酒量"
                    ]
                })

        return recommendations

    def _follow_up_schedule(self, risk_level: str) -> Dict:
        """Generate follow-up schedule based on risk level."""
        schedules = {
            "low": {
                "next_checkup": "12个月",
                "monitoring": ["年度体检"],
                "specialist": "无需"
            },
            "moderate": {
                "next_checkup": "3-6个月",
                "monitoring": ["血压：每日", "血脂：3个月", "血糖：6个月"],
                "specialist": "建议心内科咨询"
            },
            "high": {
                "next_checkup": "1-3个月",
                "monitoring": ["血压：每日", "血脂：1个月", "血糖：3个月", "心电图：3个月"],
                "specialist": "心内科就诊"
            },
            "very_high": {
                "next_checkup": "立即",
                "monitoring": ["血压：每日", "心电图：立即"],
                "specialist": "紧急心内科就诊"
            }
        }
        return schedules.get(risk_level, schedules["moderate"])
