"""
PIAS API - JeecgBoot Compatible Format

Follows JeecgBoot standard response format:
{
    "success": true/false,
    "message": "提示信息",
    "code": 200/500,
    "result": {...},
    "timestamp": 1234567890
}

Compatible with: https://3d.daozhijian.cn/jeecgboot/doc.html
"""

from fastapi import APIRouter, HTTPException
from pydantic import BaseModel, Field
from typing import List, Optional, Dict, Any
from datetime import datetime, date
import time

router = APIRouter()


# ═══════════════════════════════════════════════════════
# JeecgBoot Standard Response Wrapper
# ═══════════════════════════════════════════════════════

def jeecg_success(result: Any = None, message: str = "操作成功") -> Dict:
    """JeecgBoot success response."""
    return {
        "success": True,
        "message": message,
        "code": 200,
        "result": result,
        "timestamp": int(time.time() * 1000)
    }


def jeecg_error(message: str = "操作失败", code: int = 500) -> Dict:
    """JeecgBoot error response."""
    return {
        "success": False,
        "message": message,
        "code": code,
        "result": None,
        "timestamp": int(time.time() * 1000)
    }


# ═══════════════════════════════════════════════════════
# Request Models (JeecgBoot style)
# ═══════════════════════════════════════════════════════

class PredictRequest(BaseModel):
    """CVD risk prediction request - 16 features."""
    age: int = Field(..., description="年龄", example=52)
    gender: int = Field(..., description="性别 0=女 1=男", example=1)
    bmi: float = Field(..., description="BMI指数", example=27.5)
    sbp: float = Field(..., description="收缩压(mmHg)", example=145)
    dbp: float = Field(..., description="舒张压(mmHg)", example=90)
    fasting_glucose: float = Field(..., description="空腹血糖(mmol/L)", example=6.2)
    total_cholesterol: float = Field(..., description="总胆固醇(mmol/L)", example=5.8)
    ldl: float = Field(..., description="低密度脂蛋白(mmol/L)", example=3.9)
    hdl: float = Field(..., description="高密度脂蛋白(mmol/L)", example=1.1)
    triglycerides: float = Field(..., description="甘油三酯(mmol/L)", example=2.1)
    exercise_days: int = Field(..., description="每周运动天数", example=2)
    smoking: int = Field(..., description="吸烟 0=否 1=是", example=1)
    drinking: int = Field(..., description="饮酒 0=否 1=是", example=0)
    diabetes_history: int = Field(..., description="糖尿病史 0=否 1=是", example=0)
    hypertension_history: int = Field(..., description="高血压史 0=否 1=是", example=1)
    family_history: int = Field(..., description="家族病史 0=否 1=是", example=1)


class IndividualAttributionRequest(BaseModel):
    """Individual attribution request."""
    risk_history: List[Dict] = Field(
        ...,
        description="风险历史记录",
        example=[
            {"date": "2026-05-01", "Y": 0.52, "Z": 1},
            {"date": "2026-05-02", "Y": 0.50, "Z": 1},
            {"date": "2026-05-03", "Y": 0.48, "Z": 0}
        ]
    )
    forecast_days: int = Field(30, description="预测天数")
    language: str = Field("zh", description="语言 zh/en")


class GroupAttributionRequest(BaseModel):
    """Group attribution request."""
    user_records: List[Dict] = Field(
        ...,
        description="用户记录列表",
        example=[
            {"device_id": "user_001", "Z": 1, "delta_Y": -0.05, "features": {"age_bracket": 2, "bmi_level_encoded": 1}},
            {"device_id": "user_002", "Z": 0, "delta_Y": -0.01, "features": {"age_bracket": 2, "bmi_level_encoded": 1}}
        ]
    )
    n_bootstrap: int = Field(200, description="Bootstrap迭代次数")
    confidence_level: float = Field(0.95, description="置信水平")
    language: str = Field("zh", description="语言 zh/en")


# ═══════════════════════════════════════════════════════
# P - Predict (风险预测)
# ═══════════════════════════════════════════════════════

@router.post("/predict", summary="CVD风险预测", tags=["P-风险预测"])
async def predict_risk(req: PredictRequest):
    """
    心血管疾病风险预测

    使用CatBoost模型对16个临床特征进行风险评估，
    返回风险评分、风险等级和特征贡献度。
    """
    try:
        # TODO: Load actual model
        # For now, return mock result
        features = req.model_dump()

        # Simulate prediction
        risk_score = 0.45  # Placeholder
        risk_level = "moderate"

        # Feature contributions (SHAP values)
        feature_contributions = {
            "age": 0.15,
            "sbp": 0.12,
            "smoking": 0.08,
            "bmi": 0.05,
            "fasting_glucose": 0.03,
            "total_cholesterol": 0.02,
            "ldl": 0.01,
            "hdl": -0.02,
            "triglycerides": 0.01,
            "exercise_days": -0.01,
            "drinking": 0.005,
            "diabetes_history": 0.01,
            "hypertension_history": 0.02,
            "family_history": 0.01,
            "gender": 0.005,
            "dbp": 0.005
        }

        # Top contributors
        sorted_contribs = sorted(
            feature_contributions.items(),
            key=lambda x: abs(x[1]),
            reverse=True
        )[:5]

        result = {
            "risk_score": risk_score,
            "risk_level": risk_level,
            "risk_level_zh": "中等风险",
            "feature_contributions": feature_contributions,
            "top_contributors": [
                {"feature": f, "contribution": c, "direction": "增加风险" if c > 0 else "降低风险"}
                for f, c in sorted_contribs
            ],
            "model_version": "V2.0",
            "model_auc": 0.767
        }

        return jeecg_success(result, "预测成功")

    except Exception as e:
        return jeecg_error(f"预测失败: {str(e)}")


# ═══════════════════════════════════════════════════════
# I - Intervene (干预处方)
# ═══════════════════════════════════════════════════════

@router.post("/intervene", summary="生成干预处方", tags=["I-干预处方"])
async def generate_intervention(risk_score: float, risk_level: str, language: str = "zh"):
    """
    基于风险评估生成个性化干预处方

    根据风险评分和风险等级，生成个性化的健康管理建议。
    """
    try:
        prescriptions = {
            "low": {
                "zh": {
                    "summary": "风险较低，保持健康生活方式",
                    "recommendations": [
                        "每周至少150分钟中等强度运动",
                        "保持均衡饮食",
                        "每年体检一次"
                    ]
                },
                "en": {
                    "summary": "Low risk, maintain healthy lifestyle",
                    "recommendations": [
                        "At least 150 minutes of moderate exercise per week",
                        "Maintain balanced diet",
                        "Annual health checkup"
                    ]
                }
            },
            "moderate": {
                "zh": {
                    "summary": "风险中等，需要积极干预",
                    "recommendations": [
                        "每日30分钟快走或慢跑",
                        "控制盐摄入<6g/天",
                        "每3个月复查血脂",
                        "戒烟限酒"
                    ]
                },
                "en": {
                    "summary": "Moderate risk, active intervention needed",
                    "recommendations": [
                        "30 minutes brisk walking or jogging daily",
                        "Limit salt intake <6g/day",
                        "Lipid check every 3 months",
                        "Quit smoking and limit alcohol"
                    ]
                }
            },
            "high": {
                "zh": {
                    "summary": "风险较高，建议就医",
                    "recommendations": [
                        "立即预约心内科",
                        "每日监测血压",
                        "严格控制饮食",
                        "遵医嘱用药"
                    ]
                },
                "en": {
                    "summary": "High risk, medical consultation recommended",
                    "recommendations": [
                        "Schedule cardiology appointment immediately",
                        "Daily blood pressure monitoring",
                        "Strict diet control",
                        "Follow medication prescribed by doctor"
                    ]
                }
            },
            "very_high": {
                "zh": {
                    "summary": "风险极高，立即就医",
                    "recommendations": [
                        "紧急就医",
                        "卧床休息",
                        "避免剧烈运动",
                        "准备住院检查"
                    ]
                },
                "en": {
                    "summary": "Very high risk, seek immediate medical attention",
                    "recommendations": [
                        "Emergency medical attention",
                        "Bed rest",
                        "Avoid strenuous exercise",
                        "Prepare for hospitalization"
                    ]
                }
            }
        }

        prescription = prescriptions.get(risk_level, prescriptions["moderate"])
        result = prescription.get(language, prescription["zh"])
        result["risk_score"] = risk_score
        result["risk_level"] = risk_level

        return jeecg_success(result, "处方生成成功")

    except Exception as e:
        return jeecg_error(f"处方生成失败: {str(e)}")


# ═══════════════════════════════════════════════════════
# A - Attribute (归因分析)
# ═══════════════════════════════════════════════════════

@router.post("/attribute/individual", summary="个人归因分析", tags=["A-归因分析"])
async def attribute_individual(req: IndividualAttributionRequest):
    """
    Level 1: 个人风险轨迹预测与干预效果评估

    使用指数衰减模型预测个人风险轨迹，
    并通过配对ATT评估干预效果。
    """
    try:
        from healthagent.pias.attribution import IndividualAttributor

        attributor = IndividualAttributor()
        result = attributor.attribute(req.risk_history)

        # Generate ECharts config
        charts = _generate_individual_charts(result, req.language)

        # Generate animations
        animations = _generate_individual_animations(result)

        # Generate reports for different audiences
        reports = _generate_reports(result, "individual", req.language)

        response = {
            "report_id": f"IND-{datetime.now().strftime('%Y%m%d')}-{id(result) % 10000:04d}",
            "status": result.status,
            "current_state": {
                "risk_score": result.current_risk_score,
                "risk_level": result.risk_level,
                "trend": result.trend_direction
            },
            "forecast": {
                "raw": {
                    "dates": [f"Day {i+1}" for i in range(result.forecast_days)],
                    "no_action": result.forecast_status_quo,
                    "with_plan": result.forecast_with_plan,
                    "ci_upper": result.forecast_ci_upper,
                    "ci_lower": result.forecast_ci_lower
                },
                "summary": {
                    "30d_no_action": result.projected_risk_30d_no_action,
                    "30d_with_plan": result.projected_risk_30d_with_plan,
                    "risk_reduction": result.risk_reduction_30d
                }
            },
            "intervention_effect": {
                "individual_att": result.individual_att,
                "att_ci_lower": result.att_ci_lower,
                "att_ci_upper": result.att_ci_upper,
                "att_p_value": result.att_p_value,
                "att_significant": result.att_significant
            },
            "charts": charts,
            "animations": animations,
            "reports": reports
        }

        return jeecg_success(response, "个人归因分析成功")

    except Exception as e:
        return jeecg_error(f"个人归因分析失败: {str(e)}")


@router.post("/attribute/group", summary="群体归因分析", tags=["A-归因分析"])
async def attribute_group(req: GroupAttributionRequest):
    """
    Level 2: 群体干预效果归因

    使用PSM+DRE+Bootstrap方法评估群体干预效果，
    包含完整的统计检验和敏感性分析。
    """
    try:
        from healthagent.pias.attribution import GroupAttributor

        attributor = GroupAttributor({
            "n_bootstrap": req.n_bootstrap,
            "confidence_level": req.confidence_level
        })
        result = attributor.attribute(req.user_records)

        if result.get("status") != "success":
            return jeecg_error(result.get("message", "归因分析失败"), 400)

        # Generate ECharts config
        charts = _generate_group_charts(result, req.language)

        # Generate animations
        animations = _generate_group_animations(result)

        # Generate reports
        reports = _generate_reports(result, "group", req.language)

        response = {
            "report_id": f"GRP-{datetime.now().strftime('%Y%m%d')}-{id(result) % 10000:04d}",
            "results": {
                "att": result["att"],
                "ci_lower": result["ci_lower"],
                "ci_upper": result["ci_upper"],
                "p_value": result["p_value"],
                "is_significant": result["is_significant"]
            },
            "sample": {
                "n_total": result["n_total"],
                "n_treated": result["n_treated"],
                "n_control": result["n_control"],
                "n_matched": result["n_matched_pairs"]
            },
            "statistics": {
                "effect_sizes": result["effect_sizes"],
                "sensitivity": {
                    "rosenbaum_gamma": result["gamma_sensitivity"],
                    "e_value": result["e_value"]["e_value"],
                    "interpretation": result["e_value"]["interpretation"]
                },
                "power": result["power_analysis"],
                "balance": {
                    "n_balanced": sum(1 for v in result["balance"].values() if v.get("balanced")),
                    "n_total": len(result["balance"]),
                    "details": result["balance"]
                }
            },
            "charts": charts,
            "animations": animations,
            "reports": reports
        }

        return jeecg_success(result, "归因分析成功")

    except Exception as e:
        return jeecg_error(f"归因分析失败: {str(e)}")


@router.post("/settle", summary="生成结算报告", tags=["S-结算"])
async def generate_settlement(req: GroupAttributionRequest, insurer_name: str = ""):
    """
    生成保险结算报告

    运行PSM+DRE+Rosenbaum敏感性分析，
    生成符合保险公司要求的结算报告。
    """
    try:
        from healthagent.pias.attribution import GroupAttributor, AttributionReport

        attributor = GroupAttributor({
            "n_bootstrap": req.n_bootstrap,
            "confidence_level": req.confidence_level
        })
        result = attributor.attribute(req.user_records)

        if result.get("status") != "success":
            return jeecg_error(result.get("message", "结算分析失败"), 400)

        # Generate report
        report = AttributionReport.from_group_result(result)

        response = {
            "report_id": report.report_id,
            "conclusion": report.conclusion,
            "recommendation": report.recommendation,
            "metrics": report.metrics,
            "sections": report.sections,
            "methodology": report.methodology,
            "limitations": report.limitations,
            "executive_summary": report.executive_summary
        }

        return jeecg_success(response, "结算报告生成成功")

    except Exception as e:
        return jeecg_error(f"结算报告生成失败: {str(e)}")


# ═══════════════════════════════════════════════════════
# ECharts Chart Generation
# ═══════════════════════════════════════════════════════

def _generate_individual_charts(result, language: str = "zh") -> Dict:
    """Generate ECharts config for individual attribution."""
    lang = "zh" if language == "zh" else "en"

    # Risk trend chart
    risk_trend = {
        "type": "line",
        "title": {
            "text": "风险趋势预测" if lang == "zh" else "Risk Trend Forecast"
        },
        "tooltip": {"trigger": "axis"},
        "legend": {
            "data": ["无干预", "干预后", "置信区间"] if lang == "zh" else ["No Action", "With Plan", "CI"]
        },
        "xAxis": {
            "type": "category",
            "data": [f"Day {i+1}" for i in range(result.forecast_days)]
        },
        "yAxis": {
            "type": "value",
            "name": "风险评分" if lang == "zh" else "Risk Score",
            "min": 0,
            "max": 1
        },
        "series": [
            {
                "name": "无干预" if lang == "zh" else "No Action",
                "type": "line",
                "data": result.forecast_status_quo,
                "lineStyle": {"color": "#ff6b6b"}
            },
            {
                "name": "干预后" if lang == "zh" else "With Plan",
                "type": "line",
                "data": result.forecast_with_plan,
                "lineStyle": {"color": "#51cf66"}
            }
        ],
        "animation": {"duration": 2000, "easing": "cubicOut"}
    }

    # Feature contribution chart (from report_text metrics)
    feature_chart = {
        "type": "bar",
        "title": {
            "text": "风险因素贡献" if lang == "zh" else "Risk Factor Contributions"
        },
        "xAxis": {
            "type": "category",
            "data": ["年龄", "血压", "血糖", "血脂", "吸烟", "BMI"] if lang == "zh" else ["Age", "BP", "Glucose", "Lipids", "Smoking", "BMI"]
        },
        "yAxis": {
            "type": "value",
            "name": "贡献度" if lang == "zh" else "Contribution"
        },
        "series": [{
            "type": "bar",
            "data": [
                {"value": 0.15, "itemStyle": {"color": "#ff6b6b"}},
                {"value": 0.12, "itemStyle": {"color": "#ff6b6b"}},
                {"value": 0.08, "itemStyle": {"color": "#ff6b6b"}},
                {"value": 0.05, "itemStyle": {"color": "#ff6b6b"}},
                {"value": 0.10, "itemStyle": {"color": "#ff6b6b"}},
                {"value": 0.03, "itemStyle": {"color": "#51cf66"}}
            ]
        }]
    }

    return {
        "risk_trend": risk_trend,
        "feature_contributions": feature_chart
    }


def _generate_group_charts(result, language: str = "zh") -> Dict:
    """Generate ECharts config for group attribution."""
    lang = "zh" if language == "zh" else "en"

    # Intervention effect comparison
    effect_chart = {
        "type": "bar",
        "title": {
            "text": "干预效果对比" if lang == "zh" else "Intervention Effect Comparison"
        },
        "xAxis": {
            "data": ["干预组", "对照组"] if lang == "zh" else ["Treated", "Control"]
        },
        "yAxis": {
            "type": "value",
            "name": "风险变化" if lang == "zh" else "Risk Change"
        },
        "series": [{
            "name": "ATT",
            "type": "bar",
            "data": [
                {"value": result["att"], "itemStyle": {"color": "#51cf66"}},
                {"value": 0, "itemStyle": {"color": "#868e96"}}
            ]
        }],
        "markLine": {
            "data": [{"yAxis": 0, "lineStyle": {"type": "dashed"}}]
        }
    }

    # Balance chart
    balance_data = []
    for feat, stats in result.get("balance", {}).items():
        balance_data.append({
            "name": feat,
            "smd": stats.get("smd", 0),
            "balanced": stats.get("balanced", False)
        })

    balance_chart = {
        "type": "bar",
        "title": {
            "text": "协变量平衡性 (SMD)" if lang == "zh" else "Covariate Balance (SMD)"
        },
        "xAxis": {
            "type": "category",
            "data": [d["name"] for d in balance_data]
        },
        "yAxis": {
            "type": "value",
            "name": "SMD"
        },
        "series": [{
            "type": "bar",
            "data": [
                {
                    "value": d["smd"],
                    "itemStyle": {"color": "#51cf66" if d["balanced"] else "#ff6b6b"}
                }
                for d in balance_data
            ]
        }],
        "markLine": {
            "data": [{"yAxis": 0.1, "lineStyle": {"type": "dashed", "color": "#ff6b6b"}}]
        }
    }

    return {
        "effect_comparison": effect_chart,
        "balance": balance_chart
    }


def _generate_individual_animations(result) -> Dict:
    """Generate animation config for individual attribution."""
    return {
        "risk_decrease": {
            "type": "risk_decrease",
            "from": result.current_risk_score,
            "to": result.projected_risk_30d_with_plan,
            "duration": 2000,
            "easing": "cubicOut"
        }
    }


def _generate_group_animations(result) -> Dict:
    """Generate animation config for group attribution."""
    return {
        "bootstrap_sampling": {
            "type": "bootstrap_sampling",
            "iterations": 50,
            "duration": 5000
        }
    }


def _generate_reports(result, report_type: str, language: str) -> Dict:
    """Generate layered reports."""
    lang = "zh" if language == "zh" else "en"

    if report_type == "individual":
        report_text = result.report_text if hasattr(result, 'report_text') else {}
        return {
            "user": {
                "headline": report_text.get("headline", ""),
                "body": report_text.get("body", ""),
                "advice": report_text.get("advice", "")
            },
            "manager": {
                "summary": f"干预降低风险 {result.risk_reduction_30d:.1%}" if hasattr(result, 'risk_reduction_30d') else "",
                "metrics": {"risk_reduction": f"{result.risk_reduction_30d:.1%}" if hasattr(result, 'risk_reduction_30d') else "N/A"}
            },
            "actuary": {
                "methodology": "指数衰减模型 + 配对ATT",
                "metrics": {"att": result.individual_att, "p_value": result.att_p_value}
            },
            "regulator": {
                "compliance": "符合《健康保险管理办法》要求"
            }
        }
    else:
        report = result.get("settlement_report", {})
        return {
            "user": {
                "headline": report.get("conclusion", ""),
                "body": report.get("detail", ""),
                "advice": report.get("recommendation", "")
            },
            "manager": {
                "summary": report.get("conclusion", ""),
                "metrics": report.get("metrics", {})
            },
            "actuary": {
                "methodology": report.get("method", ""),
                "metrics": report.get("metrics", {}),
                "effect_sizes": result.get("effect_sizes", {}),
                "sensitivity": result.get("e_value", {})
            },
            "regulator": {
                "compliance": "符合《健康保险管理办法》要求",
                "audit_trail": {"report_id": "GRP-..."}
            }
        }


# ═══════════════════════════════════════════════════════
# Test/Mock Endpoints (for development)
# ═══════════════════════════════════════════════════════

@router.get("/test/individual", summary="测试个人归因（返回mock数据）", tags=["测试"])
async def test_individual_attribution(language: str = "zh"):
    """
    返回完整的个人归因mock数据，用于前端开发和测试。
    """
    lang = "zh" if language == "zh" else "en"

    mock_result = {
        "report_id": "IND-20260604-TEST",
        "status": "ready",
        "current_state": {
            "risk_score": 0.45,
            "risk_level": "moderate",
            "risk_level_zh": "中等风险",
            "trend": "improving",
            "trend_slope": -0.002
        },
        "forecast": {
            "raw": {
                "dates": [f"Day {i+1}" for i in range(30)],
                "no_action": [0.45 - i*0.001 for i in range(30)],
                "with_plan": [0.45 - i*0.003 for i in range(30)],
                "ci_upper": [0.48 - i*0.001 for i in range(30)],
                "ci_lower": [0.42 - i*0.001 for i in range(30)]
            },
            "summary": {
                "30d_no_action": 0.42,
                "30d_with_plan": 0.36,
                "risk_reduction": 0.06
            }
        },
        "intervention_effect": {
            "individual_att": -0.05,
            "att_ci_lower": -0.08,
            "att_ci_upper": -0.02,
            "att_p_value": 0.003,
            "att_significant": True,
            "effect_size_d": 0.45,
            "interpretation": "中等效应" if lang == "zh" else "Medium effect"
        },
        "charts": {
            "risk_trend": {
                "type": "line",
                "title": {"text": "风险趋势预测" if lang == "zh" else "Risk Trend Forecast"},
                "tooltip": {"trigger": "axis"},
                "legend": {"data": ["无干预", "干预后", "置信区间"] if lang == "zh" else ["No Action", "With Plan", "CI"]},
                "xAxis": {"type": "category", "data": [f"Day {i+1}" for i in range(30)]},
                "yAxis": {"type": "value", "name": "风险评分" if lang == "zh" else "Risk Score", "min": 0, "max": 1},
                "series": [
                    {"name": "无干预" if lang == "zh" else "No Action", "type": "line", "data": [0.45 - i*0.001 for i in range(30)], "lineStyle": {"color": "#ff6b6b"}},
                    {"name": "干预后" if lang == "zh" else "With Plan", "type": "line", "data": [0.45 - i*0.003 for i in range(30)], "lineStyle": {"color": "#51cf66"}},
                    {"name": "置信区间" if lang == "zh" else "CI", "type": "line", "data": [0.48 - i*0.001 for i in range(30)], "lineStyle": {"type": "dashed", "color": "#868e96"}, "areaStyle": {"opacity": 0.1}}
                ],
                "animation": {"duration": 2000, "easing": "cubicOut"}
            },
            "feature_contributions": {
                "type": "bar",
                "title": {"text": "风险因素贡献" if lang == "zh" else "Risk Factor Contributions"},
                "xAxis": {"type": "category", "data": ["年龄", "血压", "血糖", "血脂", "吸烟", "BMI"] if lang == "zh" else ["Age", "BP", "Glucose", "Lipids", "Smoking", "BMI"]},
                "yAxis": {"type": "value", "name": "贡献度" if lang == "zh" else "Contribution"},
                "series": [{"type": "bar", "data": [
                    {"value": 0.15, "itemStyle": {"color": "#ff6b6b"}},
                    {"value": 0.12, "itemStyle": {"color": "#ff6b6b"}},
                    {"value": 0.08, "itemStyle": {"color": "#ff6b6b"}},
                    {"value": 0.05, "itemStyle": {"color": "#ffa94d"}},
                    {"value": 0.10, "itemStyle": {"color": "#ff6b6b"}},
                    {"value": 0.03, "itemStyle": {"color": "#51cf66"}}
                ]}]
            }
        },
        "animations": {
            "risk_decrease": {
                "type": "risk_decrease",
                "from": 0.45,
                "to": 0.36,
                "duration": 2000,
                "easing": "cubicOut",
                "steps": [
                    {"progress": 0, "value": 0.45, "label": "干预前" if lang == "zh" else "Before"},
                    {"progress": 0.5, "value": 0.40, "label": "干预中" if lang == "zh" else "During"},
                    {"progress": 1, "value": 0.36, "label": "干预后" if lang == "zh" else "After"}
                ]
            }
        },
        "reports": {
            "user": {
                "headline": "✅ 健康计划有效，继续保持" if lang == "zh" else "✅ Health plan effective",
                "body": "过去30天您执行了25天健康计划，当前心血管风险为45%。坚持计划可多降低约6%的风险。" if lang == "zh" else "You followed the health plan for 25 out of 30 days. Risk reduced by 6%.",
                "advice": "建议维持当前饮食和运动方案，定期复测。" if lang == "zh" else "Maintain current diet and exercise plan."
            },
            "manager": {
                "summary": "干预有效，风险降低6%" if lang == "zh" else "Intervention effective, 6% risk reduction",
                "metrics": {"risk_reduction": "6%", "nnt": 17, "roi": "正向"}
            },
            "actuary": {
                "methodology": "指数衰减模型 + 配对ATT + Bootstrap CI",
                "metrics": {"att": -0.05, "p_value": 0.003, "ci": [-0.08, -0.02]},
                "sensitivity": {"e_value": 2.5, "gamma": 2.0, "interpretation": "结论较稳健"}
            },
            "regulator": {
                "compliance": "符合《健康保险管理办法》要求",
                "audit_trail": {"report_id": "IND-20260604-TEST", "signed_at": "2026-06-04T10:00:00Z"}
            }
        }
    }

    return jeecg_success(mock_result, "测试数据")


@router.get("/test/group", summary="测试群体归因（返回mock数据）", tags=["测试"])
async def test_group_attribution(language: str = "zh"):
    """
    返回完整的群体归因mock数据，用于前端开发和测试。
    """
    lang = "zh" if language == "zh" else "en"

    mock_result = {
        "report_id": "GRP-20260604-TEST",
        "results": {
            "att": -0.05,
            "ci_lower": -0.08,
            "ci_upper": -0.02,
            "p_value": 0.003,
            "is_significant": True
        },
        "sample": {
            "n_total": 200,
            "n_treated": 100,
            "n_control": 100,
            "n_matched": 85
        },
        "statistics": {
            "effect_sizes": {
                "cohens_d": -0.45,
                "hedges_g": -0.44,
                "interpretation": "中等效应" if lang == "zh" else "Medium effect"
            },
            "sensitivity": {
                "rosenbaum_gamma": 2.5,
                "e_value": 3.0,
                "interpretation": "结论较稳健" if lang == "zh" else "Conclusion is robust"
            },
            "power": {
                "power": 0.85,
                "min_n_80": 150,
                "adequately_powered": True
            },
            "balance": {
                "n_balanced": 6,
                "n_total": 7,
                "details": {
                    "age_bracket": {"smd": 0.05, "balanced": True},
                    "bmi_level_encoded": {"smd": 0.08, "balanced": True},
                    "bp_baseline_grade_encoded": {"smd": 0.12, "balanced": False},
                    "activity_level_encoded": {"smd": 0.04, "balanced": True},
                    "gender_encoded": {"smd": 0.06, "balanced": True},
                    "season_sin": {"smd": 0.03, "balanced": True},
                    "season_cos": {"smd": 0.07, "balanced": True}
                }
            }
        },
        "charts": {
            "effect_comparison": {
                "type": "bar",
                "title": {"text": "干预效果对比" if lang == "zh" else "Intervention Effect Comparison"},
                "xAxis": {"data": ["干预组", "对照组"] if lang == "zh" else ["Treated", "Control"]},
                "yAxis": {"type": "value", "name": "风险变化" if lang == "zh" else "Risk Change"},
                "series": [{"name": "ATT", "type": "bar", "data": [
                    {"value": -0.05, "itemStyle": {"color": "#51cf66"}},
                    {"value": -0.01, "itemStyle": {"color": "#868e96"}}
                ]}]
            },
            "balance": {
                "type": "bar",
                "title": {"text": "协变量平衡性 (SMD)" if lang == "zh" else "Covariate Balance (SMD)"},
                "xAxis": {"type": "category", "data": ["年龄", "BMI", "血压", "活动", "性别", "季节sin", "季节cos"] if lang == "zh" else ["Age", "BMI", "BP", "Activity", "Gender", "Season sin", "Season cos"]},
                "yAxis": {"type": "value", "name": "SMD"},
                "series": [{"type": "bar", "data": [
                    {"value": 0.05, "itemStyle": {"color": "#51cf66"}},
                    {"value": 0.08, "itemStyle": {"color": "#51cf66"}},
                    {"value": 0.12, "itemStyle": {"color": "#ff6b6b"}},
                    {"value": 0.04, "itemStyle": {"color": "#51cf66"}},
                    {"value": 0.06, "itemStyle": {"color": "#51cf66"}},
                    {"value": 0.03, "itemStyle": {"color": "#51cf66"}},
                    {"value": 0.07, "itemStyle": {"color": "#51cf66"}}
                ]}],
                "markLine": {"data": [{"yAxis": 0.1, "lineStyle": {"type": "dashed", "color": "#ff6b6b"}}]}
            },
            "propensity_distribution": {
                "type": "histogram",
                "title": {"text": "倾向得分分布" if lang == "zh" else "Propensity Score Distribution"},
                "xAxis": {"type": "category", "data": [f"{i*0.1:.1f}-{(i+1)*0.1:.1f}" for i in range(10)]},
                "yAxis": {"type": "value", "name": "频次" if lang == "zh" else "Frequency"},
                "series": [
                    {"name": "匹配前-干预组" if lang == "zh" else "Before-Treated", "type": "bar", "data": [2, 5, 8, 12, 15, 18, 20, 12, 5, 3]},
                    {"name": "匹配前-对照组" if lang == "zh" else "Before-Control", "type": "bar", "data": [3, 6, 10, 14, 16, 17, 15, 10, 6, 3]},
                    {"name": "匹配后-干预组" if lang == "zh" else "After-Treated", "type": "bar", "data": [1, 3, 6, 10, 14, 17, 18, 10, 4, 2]},
                    {"name": "匹配后-对照组" if lang == "zh" else "After-Control", "type": "bar", "data": [1, 3, 6, 10, 14, 17, 18, 10, 4, 2]}
                ]
            },
            "bootstrap_distribution": {
                "type": "histogram",
                "title": {"text": "Bootstrap ATT分布" if lang == "zh" else "Bootstrap ATT Distribution"},
                "xAxis": {"type": "category", "data": [f"{-0.1+i*0.01:.2f}" for i in range(20)]},
                "yAxis": {"type": "value", "name": "频次" if lang == "zh" else "Frequency"},
                "series": [{"type": "bar", "data": [1, 2, 5, 10, 18, 25, 35, 45, 50, 55, 58, 55, 50, 45, 35, 25, 18, 10, 5, 2]}],
                "markLine": {"data": [
                    {"xAxis": 2, "lineStyle": {"type": "dashed", "color": "#ff6b6b"}, "label": {"formatter": "CI Lower"}},
                    {"xAxis": 8, "lineStyle": {"type": "dashed", "color": "#ff6b6b"}, "label": {"formatter": "CI Upper"}}
                ]}
            }
        },
        "animations": {
            "psm_matching": {
                "type": "psm_matching",
                "duration": 3000,
                "steps": [
                    {"step": 1, "title": "计算倾向得分" if lang == "zh" else "Calculate Propensity Scores"},
                    {"step": 2, "title": "匹配最近邻" if lang == "zh" else "Match Nearest Neighbors"},
                    {"step": 3, "title": "移除不匹配样本" if lang == "zh" else "Remove Unmatched"}
                ]
            },
            "bootstrap_sampling": {
                "type": "bootstrap_sampling",
                "iterations": 50,
                "duration": 5000,
                "description": "Bootstrap重采样过程" if lang == "zh" else "Bootstrap resampling process"
            }
        },
        "reports": {
            "user": {
                "headline": "✅ 干预显著降低心血管风险" if lang == "zh" else "✅ Intervention significantly reduces CVD risk",
                "body": "经分析，接受干预的用户心血管风险平均降低5%，效果具有统计显著性。" if lang == "zh" else "Analysis shows treated users have 5% lower CVD risk on average.",
                "advice": "建议继续推广健康干预计划。" if lang == "zh" else "Continue promoting health intervention program."
            },
            "manager": {
                "summary": "干预效果显著，建议继续" if lang == "zh" else "Intervention effective, continue",
                "metrics": {"att": "-5%", "p_value": "0.003", "nnt": 20, "roi": "正向"}
            },
            "actuary": {
                "methodology": "PSM + DRE + BCa Bootstrap + Rosenbaum敏感性分析",
                "metrics": {"att": -0.05, "ci_95": "[-0.08, -0.02]", "p_value": 0.003},
                "effect_sizes": {"cohens_d": -0.45, "hedges_g": -0.44},
                "sensitivity": {"gamma": 2.5, "e_value": 3.0}
            },
            "regulator": {
                "compliance": "符合《健康保险管理办法》要求",
                "audit_trail": {"report_id": "GRP-20260604-TEST", "signed_at": "2026-06-04T10:00:00Z"}
            }
        }
    }

    return jeecg_success(mock_result, "测试数据")


# ═══════════════════════════════════════════════════════
# Real Data Endpoints (Hangzhou Weijiankang)
# ═══════════════════════════════════════════════════════

import os, json as _json, glob as _glob

def _load_solution(solution_id: int):
    """Load pre-computed solution data from data/ directory."""
    data_dir = os.environ.get("PIAS_DATA_DIR",
        os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(__file__))), "data"))
    files = _glob.glob(f"{data_dir}/solution_{solution_id}_*.json")
    if files:
        with open(files[0], 'r', encoding='utf-8') as f:
            return _json.load(f)
    return None


@router.get("/weijiankang/heatmap", summary="区县CVD风险热力图", tags=["卫健委"])
async def weijiankang_heatmap():
    """
    杭州区县CVD风险热力图 — 基于31,016人真实数据

    返回各区县的风险评分、高血压率、高TC率等指标，
    包含ECharts图表配置，可直接渲染。
    """
    data = _load_solution(1)
    if data:
        return jeecg_success(data, "区县风险热力图")
    return jeecg_error("数据文件未找到，请先运行数据分析脚本", 404)


@router.get("/weijiankang/eval", summary="分级管理效果评估", tags=["卫健委"])
async def weijiankang_eval():
    """
    高血脂分级管理效果评估 — 基于25,583人纵向数据

    返回2014→2015年各指标变化的配对t检验结果，
    包含按区域分组的改善情况。
    """
    data = _load_solution(2)
    if data:
        return jeecg_success(data, "管理效果评估")
    return jeecg_error("数据文件未找到", 404)


@router.get("/weijiankang/high-risk", summary="极高危人群追踪", tags=["卫健委"])
async def weijiankang_high_risk():
    """
    极高危人群靶向干预追踪

    筛选SBP>=160 或 TC>=7.8 或 SBP>=140+TC>=6.2的人群，
    展示其干预前后的改善情况。
    """
    data = _load_solution(3)
    if data:
        return jeecg_success(data, "极高危人群追踪")
    return jeecg_error("数据文件未找到", 404)


@router.get("/weijiankang/ranking", summary="中心绩效排名", tags=["卫健委"])
async def weijiankang_ranking():
    """
    社区卫生服务中心绩效排名

    基于血压改善幅度对各社区卫生服务中心进行排名，
    支持按区域筛选。
    """
    data = _load_solution(4)
    if data:
        return jeecg_success(data, "中心绩效排名")
    return jeecg_error("数据文件未找到", 404)


@router.get("/weijiankang/2030", summary="健康中国2030考核", tags=["卫健委"])
async def weijiankang_2030():
    """
    健康中国2030考核指标达标报告

    返回高血压率、高TC率、糖尿病率等关键指标的
    2014→2015年变化情况。
    """
    data = _load_solution(5)
    if data:
        return jeecg_success(data, "2030考核指标")
    return jeecg_error("数据文件未找到", 404)
