"""
simulate.py - ReHealth AI HealthAgent Main Entry Point
Usage: python simulate.py
"""
import sys
import json
import os
from datetime import datetime

# Add project root to path
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from healthagent.models.patient_profile import PatientProfile, StressLevel
from healthagent.models.intervention import InterventionPlan, InterventionType
from healthagent.agents.orchestrator import OrchestratorAgent

# Simulation days (3 for debugging, 90 for production)
SIMULATION_DAYS = 90


def create_patient() -> PatientProfile:
    """Create a sample patient profile."""
    return PatientProfile(
        age=52,
        age_group="older_adult",
        bmi=27.5,
        stress_level=StressLevel.HIGH,
        sleep_avg_hours=6.2,
        resting_hr=78,
        hrv_baseline=38.0,
        motivation_score=0.6,
        primary_issue="Pre-hypertension + Insufficient sleep",
    )


def create_intervention() -> InterventionPlan:
    """Create an intervention plan."""
    return InterventionPlan(
        name="Comprehensive Health Intervention",
        intervention_type=InterventionType.HYBRID,
        duration_days=SIMULATION_DAYS,
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


def print_day(day_state):
    """Print a daily summary."""
    compliance_icon = "✅" if day_state.compliance else "❌"
    intervention_icon = "💬" if day_state.intervention_triggered else "  "
    print(
        f"  Day {day_state.day:>3} | "
        f"{compliance_icon} Compliance:{day_state.compliance_score:.2f} | "
        f"Emotion:{day_state.emotion_state:<10} | "
        f"Stress:{day_state.stress_level:.2f} | "
        f"Sleep:{day_state.sleep_hours:.1f}h | "
        f"HRV:{day_state.hrv:.1f} | "
        f"HR:{day_state.resting_hr}bpm "
        f"{intervention_icon}"
    )
    if day_state.intervention_triggered and day_state.intervention_message:
        print(f"         💬 {day_state.intervention_message}")


def save_results(result, output_dir="outputs"):
    """Save results to files."""
    os.makedirs(output_dir, exist_ok=True)
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")

    # Save JSON
    json_path = os.path.join(output_dir, f"trajectory_{timestamp}.json")
    data = {
        "summary": {
            "simulation_days": result.simulation_days,
            "compliance_rate": round(result.compliance_rate, 3),
            "average_sleep_hours": round(result.average_sleep_hours, 2),
            "sleep_improvement": round(result.sleep_improvement, 2),
            "hrv_change": round(result.hrv_change, 2),
            "resting_hr_change": result.resting_hr_change,
            "intervention_count": result.intervention_count,
        },
        "trajectory": [d.__dict__ for d in result.trajectory],
    }
    with open(json_path, "w", encoding="utf-8") as f:
        json.dump(data, f, ensure_ascii=False, indent=2, default=str)

    # Save text report
    txt_path = os.path.join(output_dir, f"report_{timestamp}.txt")
    lines = [
        "=" * 60,
        "  ReHealth AI HealthAgent Simulation Report",
        "=" * 60,
        f"Simulation Days:     {result.simulation_days}",
        f"Compliance Rate:     {result.compliance_rate:.1%}",
        f"Average Sleep:       {result.average_sleep_hours:.1f} hours",
        f"Sleep Improvement:   {result.sleep_improvement:+.1f} hours",
        f"HRV Change:          {result.hrv_change:+.1f} ms",
        f"Heart Rate Change:   {result.resting_hr_change:+d} bpm",
        f"Interventions:       {result.intervention_count}",
        "=" * 60,
    ]
    with open(txt_path, "w", encoding="utf-8") as f:
        f.write("\n".join(lines))

    print(f"\n📁 Results saved:")
    print(f"   JSON: {json_path}")
    print(f"   Report: {txt_path}")


def main():
    print("\n" + "=" * 60)
    print("  🏥 ReHealth AI HealthAgent Started")
    print("=" * 60)

    # Initialize
    patient = create_patient()
    intervention = create_intervention()

    print(f"\n👤 Patient: {patient.age}y/o, Stress:{patient.stress_level.value}, Sleep:{patient.sleep_avg_hours}h")
    print(f"📋 Plan: {intervention.name} ({SIMULATION_DAYS} days)")
    print(f"\n{'─'*60}")

    # Run simulation
    orchestrator = OrchestratorAgent(patient=patient, intervention=intervention, seed=42)

    print(f"▶ Starting {SIMULATION_DAYS}-day simulation...\n")
    result = orchestrator.run(days=SIMULATION_DAYS)

    # Print daily summary (every 10 days)
    print("📅 Daily Trajectory (10-day summary):")
    for day_state in result.trajectory:
        if day_state.day % 10 == 0 or day_state.day == 1:
            print_day(day_state)

    # Print final statistics
    print(f"\n{'='*60}")
    print("🎉 Simulation Complete! Final Results:")
    print(f"   Compliance Rate:  {result.compliance_rate:.1%}")
    print(f"   Average Sleep:    {result.average_sleep_hours:.1f} hours")
    print(f"   Sleep Improvement: {result.sleep_improvement:+.1f} hours")
    print(f"   HRV Change:       {result.hrv_change:+.1f} ms")
    print(f"   Heart Rate Change: {result.resting_hr_change:+d} bpm")
    print(f"   Interventions:    {result.intervention_count}")
    print(f"{'='*60}\n")

    # Save results
    save_results(result)


if __name__ == "__main__":
    main()
