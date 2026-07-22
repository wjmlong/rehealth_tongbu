#!/usr/bin/env python3
"""
Cardio Fitness Simulation Example

This example demonstrates a simulation focused on cardiovascular fitness intervention.
It shows how to track cardiovascular metrics and exercise adherence.
"""

import sys
import os

# Add parent directory to path for imports
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from datetime import datetime
from healthagent import SimulationEngine, SimulationResult
from healthagent.models.patient_profile import PatientProfile, StressLevel
from healthagent.models.intervention import InterventionPlan, InterventionType


def run_cardio_simulation():
    """Run a cardio-focused health intervention simulation."""
    
    print("=" * 60)
    print("      CARDIO FITNESS SIMULATION EXAMPLE")
    print("=" * 60)
    
    # Create simulation engine
    engine = SimulationEngine(seed=123)
    
    # Define patient profile - sedentary office worker
    patient_profile = {
        "age": 42,
        "gender": "male",
        "bmi": 27.5,  # Overweight
        "sleep_avg_hours": 6.5,
        "stress_level": "medium",
        "resting_hr": 78,  # Elevated
        "occupation": "office_worker",
        "wearable": True,
        "motivation_score": 0.5,
        "habit_strength": 0.25,  # Lower habit strength
    }
    
    print("\n[PATIENT PROFILE]")
    print(f"  Age: {patient_profile['age']} years")
    print(f"  Gender: {patient_profile['gender']}")
    print(f"  BMI: {patient_profile['bmi']} (Overweight)")
    print(f"  Resting HR: {patient_profile['resting_hr']} bpm (Elevated)")
    print(f"  Average Sleep: {patient_profile['sleep_avg_hours']} hours")
    print(f"  Stress Level: {patient_profile['stress_level']}")
    print(f"  Habit Strength: {patient_profile['habit_strength']}")
    
    # Run 120-day simulation
    print("\n[RUNNING 120-DAY CARDIO FITNESS SIMULATION...]")
    
    result = engine.run_simulation(
        patient_profile=patient_profile,
        intervention_plan="cardio_fitness",
        simulation_days=120,
    )
    
    # Display results
    print("\n" + "=" * 60)
    print("             SIMULATION RESULTS")
    print("=" * 60)
    
    print("\n[COMPLIANCE METRICS]")
    print(f"  Overall Compliance Rate: {result.compliance_rate:.1%}")
    print(f"  Total Interventions: {result.intervention_count}")
    
    print("\n[CARDIOVASCULAR OUTCOMES]")
    print(f"  Baseline Resting HR: {patient_profile['resting_hr']} bpm")
    print(f"  Final Resting HR: {result.trajectory[-1].resting_hr} bpm")
    print(f"  Resting HR Change: {result.resting_hr_change:+.1f} bpm")
    
    print("\n[HRV OUTCOMES]")
    print(f"  HRV Change: {result.hrv_change:+.2f} ms")
    
    print("\n[SLEEP OUTCOMES]")
    print(f"  Baseline Sleep: {patient_profile['sleep_avg_hours']} hours")
    print(f"  Average Sleep: {result.average_sleep_hours:.2f} hours")
    print(f"  Sleep Improvement: {result.sleep_improvement:+.2f} hours")
    
    print("\n[OVERALL OUTCOME]")
    print(f"  Success Score: {result.success_score:.2f}/1.00")
    print(f"  Health Impact: {result.estimated_health_impact.replace('_', ' ').title()}")
    
    # Analyze HRV trajectory
    print("\n[HRV TRAJECTORY ANALYSIS]")
    hrv_values = [d.hrv for d in result.trajectory]
    print(f"  Starting HRV: {hrv_values[0]:.1f} ms")
    print(f"  Final HRV: {hrv_values[-1]:.1f} ms")
    print(f"  Minimum HRV: {min(hrv_values):.1f} ms")
    print(f"  Maximum HRV: {max(hrv_values):.1f} ms")
    print(f"  First Week Avg: {sum(hrv_values[:7])/7:.1f} ms")
    print(f"  Last Week Avg: {sum(hrv_values[-7:])/7:.1f} ms")
    
    # Analyze resting HR trajectory
    print("\n[RESTING HR TRAJECTORY ANALYSIS]")
    rhr_values = [d.resting_hr for d in result.trajectory]
    print(f"  Starting RHR: {rhr_values[0]} bpm")
    print(f"  Final RHR: {rhr_values[-1]} bpm")
    print(f"  Minimum RHR: {min(rhr_values)} bpm")
    print(f"  Maximum RHR: {max(rhr_values)} bpm")
    
    # Compliance by day of week
    print("\n[COMPLIANCE BY DAY OF WEEK]")
    dow_compliance = {i: [0, 0] for i in range(1, 8)}  # 1=Mon, 7=Sun
    dow_names = ["", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
    
    for d in result.trajectory:
        dow_compliance[d.day_of_week][0] += 1
        if d.compliance:
            dow_compliance[d.day_of_week][1] += 1
    
    for dow in range(1, 8):
        total, compliant = dow_compliance[dow]
        rate = compliant / total if total > 0 else 0
        print(f"  {dow_names[dow]:12s}: {rate:.1%} ({compliant}/{total} days)")
    
    # Stress correlation analysis
    print("\n[STRESS-COMPLIANCE CORRELATION]")
    high_stress_days = [d for d in result.trajectory if d.stress_level > 0.6]
    low_stress_days = [d for d in result.trajectory if d.stress_level < 0.4]
    
    if high_stress_days:
        high_stress_compliance = sum(1 for d in high_stress_days if d.compliance) / len(high_stress_days)
        print(f"  High Stress Days Compliance: {high_stress_compliance:.1%} ({len(high_stress_days)} days)")
    
    if low_stress_days:
        low_stress_compliance = sum(1 for d in low_stress_days if d.compliance) / len(low_stress_days)
        print(f"  Low Stress Days Compliance: {low_stress_compliance:.1%} ({len(low_stress_days)} days)")
    
    print("\n" + "=" * 60)
    print("       Research Disclaimer: Simulation results are")
    print("       for research purposes only and should not be")
    print("       used for clinical decision-making.")
    print("=" * 60 + "\n")
    
    return result


def analyze_long_term_cardio():
    """Analyze long-term cardiovascular adaptation."""
    
    print("\n" + "=" * 60)
    print("     LONG-TERM CARDIO ADAPTATION ANALYSIS")
    print("=" * 60)
    
    engine = SimulationEngine(seed=456)
    
    patient = {
        "age": 35,
        "bmi": 26,
        "sleep_avg_hours": 6.0,
        "stress_level": "medium",
        "resting_hr": 75,
    }
    
    # Run 180-day simulation
    result = engine.run_simulation(
        patient_profile=patient,
        intervention_plan="cardio_fitness",
        simulation_days=180,
    )
    
    # Analyze monthly progression
    print("\n[MONTHLY PROGRESSION]")
    print(f"{'Month':8s} | {'Avg RHR':>8s} | {'Avg HRV':>8s} | {'Compliance':>10s}")
    print("-" * 45)
    
    for month in range(6):
        start = month * 30
        end = start + 30
        month_data = result.trajectory[start:end]
        
        if month_data:
            avg_rhr = sum(d.resting_hr for d in month_data) / len(month_data)
            avg_hrv = sum(d.hrv for d in month_data) / len(month_data)
            compliance = sum(1 for d in month_data if d.compliance) / len(month_data)
            
            print(f"Month {month+1}: | {avg_rhr:>8.1f} | {avg_hrv:>8.1f} | {compliance:>10.1%}")
    
    print("\n" + "=" * 60 + "\n")
    
    return result


if __name__ == "__main__":
    # Run main simulation
    result = run_cardio_simulation()
    
    # Optionally analyze long-term trends
    # analyze_long_term_cardio()
