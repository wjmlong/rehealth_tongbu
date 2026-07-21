# encoding: utf-8
"""
HealthAgent Skill for DeerFlow 2.0
Multi-agent health intervention simulator
Integrates: ComplianceAgent, EmotionAgent, PhysiologyAgent, InterventionAgent
Author: ReHealth AI (https://github.com/csong8904-spec/ReHealthAI-HealthAgent)
"""

import sys
import os
import json

# Force load HealthAgent .env
import dotenv as _dotenv
_dotenv.load_dotenv(r"C:\Users\Administrator\Desktop\ReHealth AI HealthAgent\.env", override=True)
# Add HealthAgent project to path
HEALTH_AGENT_PATH = r"C:\Users\Administrator\Desktop\ReHealth AI HealthAgent"
if HEALTH_AGENT_PATH not in sys.path:
    sys.path.insert(0, HEALTH_AGENT_PATH)

from healthagent.models.patient_profile import PatientProfile, StressLevel
from healthagent.models.intervention import InterventionPlan, InterventionType
from healthagent.agents.orchestrator import OrchestratorAgent


def run_health_simulation(
    age: int = 52,
    stress: str = "high",
    sleep_hours: float = 6.2,
    bmi: float = 27.5,
    days: int = 7,
    primary_issue: str = "Pre-hypertension + Insufficient sleep"
) -> dict:
    """
    Run a multi-agent health intervention simulation.

    Args:
        age: Patient age in years
        stress: Stress level (low/medium/high/extreme)
        sleep_hours: Average daily sleep duration in hours
        bmi: Body Mass Index
        days: Simulation duration in days (recommended: 7-90)
        primary_issue: Primary health concern description

    Returns:
        Dictionary containing simulation results and health assessment
    """
    stress_map = {
        "low": StressLevel.LOW,
        "medium": StressLevel.MEDIUM,
        "high": StressLevel.HIGH,
        "extreme": StressLevel.HIGH,
    }
    stress_level = stress_map.get(stress.lower(), StressLevel.HIGH)

    patient = PatientProfile(
        age=age,
        age_group="older_adult" if age >= 50 else "adult",
        bmi=bmi,
        stress_level=stress_level,
        sleep_avg_hours=sleep_hours,
        resting_hr=78,
        hrv_baseline=38.0,
        motivation_score=0.6,
        primary_issue=primary_issue,
    )

    intervention = InterventionPlan(
        name="Comprehensive Health Intervention",
        intervention_type=InterventionType.HYBRID,
        duration_days=days,
        difficulty=0.5,
        average_effectiveness=0.65,
        nudge_frequency=7,
        rules=[
            "30-minute brisk walk daily",
            "Bedtime before 10:30 PM",
            "10-minute daily mindfulness meditation",
            "Low-salt, low-sugar diet",
        ],
    )

    orchestrator = OrchestratorAgent(patient=patient, intervention=intervention, seed=42)
    result = orchestrator.run(days=days)

    return {
        "patient_info": {
            "age": age,
            "stress_level": stress,
            "sleep_hours": sleep_hours,
            "bmi": bmi,
            "primary_issue": primary_issue,
        },
        "simulation_days": result.simulation_days,
        "summary": {
            "compliance_rate": round(result.compliance_rate, 3),
            "compliance_percentage": f"{result.compliance_rate:.1%}",
            "average_sleep_hours": round(result.average_sleep_hours, 2),
            "sleep_improvement": round(result.sleep_improvement, 2),
            "hrv_change": round(result.hrv_change, 2),
            "resting_hr_change": result.resting_hr_change,
            "intervention_count": result.intervention_count,
        },
        "daily_highlights": [
            {
                "day": d.day,
                "compliant": d.compliance,
                "emotion": d.emotion_state,
                "stress": round(d.stress_level, 2),
                "sleep": round(d.sleep_hours, 1),
                "hrv": round(d.hrv, 1),
                "heart_rate": d.resting_hr,
                "intervention": d.intervention_message if d.intervention_triggered else None,
            }
            for d in result.trajectory
            if d.day % (max(1, days // 7)) == 0 or d.day == 1
        ],
        "health_assessment": _generate_assessment(result),
    }


def _generate_assessment(result) -> str:
    """生成健康干预评估报告"""
    compliance = result.compliance_rate
    sleep_imp = result.sleep_improvement
    hrv = result.hrv_change
    hr = result.resting_hr_change
    assessment = []
    if compliance >= 0.8:
        assessment.append(f"依从性优秀（{compliance:.0%}），患者很好地执行了干预计划。")
    elif compliance >= 0.6:
        assessment.append(f"依从性良好（{compliance:.0%}），但仍有提升空间。")
    elif compliance >= 0.3:
        assessment.append(f"依从性较低（{compliance:.0%}），建议加强激励和支持策略。")
    else:
        assessment.append(f"依从性很差（{compliance:.0%}），需要重新评估干预方案的可行性。")
    if sleep_imp > 0.5:
        assessment.append(f"睡眠质量显著改善，平均增加{sleep_imp:.1f}小时。")
    elif sleep_imp > 0:
        assessment.append(f"睡眠略有改善，增加{sleep_imp:.1f}小时。")
    elif sleep_imp > -0.3:
        assessment.append("睡眠无明显变化，建议调整睡眠干预策略。")
    else:
        assessment.append(f"睡眠质量下降{abs(sleep_imp):.1f}小时，压力管理需优先处理。")
    if hrv > 2:
        assessment.append(f"心率变异性提升{hrv:.1f}ms，心血管健康状况改善。")
    elif hrv < -5:
        assessment.append(f"心率变异性下降{abs(hrv):.1f}ms，建议关注压力和恢复。")
    if hr < -2:
        assessment.append(f"静息心率下降{abs(hr)}次/分，心脏效率提升。")
    elif hr > 3:
        assessment.append(f"静息心率上升{hr}次/分，需加强有氧运动。")
    return "".join(assessment)


def get_tool_definition() -> dict:
    """Return the DeerFlow tool definition for this skill."""
    return {
        "name": "health_agent_simulation",
        "description": (
            "Run a multi-agent health intervention simulation using ReHealth AI HealthAgent. "
            "Analyzes patient compliance, emotional state, and physiological metrics over time, "
            "then delivers personalized intervention recommendations. "
            "Suitable for chronic disease management, lifestyle improvement, and preventive healthcare."
        ),
        "parameters": {
            "type": "object",
            "properties": {
                "age": {"type": "integer", "description": "Patient age in years"},
                "stress": {"type": "string", "description": "Stress level: low / medium / high / extreme"},
                "sleep_hours": {"type": "number", "description": "Average daily sleep duration in hours"},
                "bmi": {"type": "number", "description": "Body Mass Index (BMI)"},
                "days": {"type": "integer", "description": "Simulation duration in days (recommended: 7-30)"},
                "primary_issue": {"type": "string", "description": "Primary health concern, e.g. 'Pre-hypertension + Insufficient sleep'"},
            },
            "required": ["age", "stress", "days"],
        },
    }


if __name__ == "__main__":
    print("Testing HealthAgent Skill...")
    result = run_health_simulation(age=52, stress="high", sleep_hours=6.2, days=7)
    print(json.dumps(result, ensure_ascii=False, indent=2))


