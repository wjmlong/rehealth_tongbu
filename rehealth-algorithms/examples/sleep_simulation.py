#!/usr/bin/env python3
"""
Sleep Optimization Simulation Example

This example demonstrates a simulation focused on sleep improvement intervention.
It shows how to configure patient profiles and analyze sleep-related outcomes.
"""

import sys
import os

# Add parent directory to path for imports
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from datetime import datetime
from healthagent import SimulationEngine, SimulationResult
from healthagent.models.patient_profile import PatientProfile, StressLevel
from healthagent.models.intervention import InterventionPlan, InterventionType


def run_sleep_simulation():
    """Run a sleep-focused health intervention simulation."""
    
    print("=" * 60)
    print("     SLEEP OPTIMIZATION SIMULATION EXAMPLE")
    print("=" * 60)
    
    # Create simulation engine
    engine = SimulationEngine(seed=42)  # Seed for reproducibility
    
    # Define patient profile with sleep issues
    patient_profile = {
        "age": 34,
        "gender": "female",
        "bmi": 24.5,
        "sleep_avg_hours": 5.8,  # Below recommended
        "stress_level": "high",
        "resting_hr": 72,
        "occupation": "office_worker",
        "wearable": True,
        "motivation_score": 0.6,  # Moderate motivation
        "habit_strength": 0.35,
    }
    
    print("\n[PATIENT PROFILE]")
    print(f"  Age: {patient_profile['age']} years")
    print(f"  Gender: {patient_profile['gender']}")
    print(f"  BMI: {patient_profile['bmi']}")
    print(f"  Average Sleep: {patient_profile['sleep_avg_hours']} hours")
    print(f"  Stress Level: {patient_profile['stress_level']}")
    print(f"  Motivation: {patient_profile['motivation_score']}")
    
    # Run 90-day simulation
    print("\n[RUNNING 90-DAY SIMULATION...]")
    
    result = engine.run_simulation(
        patient_profile=patient_profile,
        intervention_plan="sleep_optimization",
        simulation_days=90,
    )
    
    # Display results
    print("\n" + "=" * 60)
    print("             SIMULATION RESULTS")
    print("=" * 60)
    
    print("\n[COMPLIANCE METRICS]")
    print(f"  Overall Compliance Rate: {result.compliance_rate:.1%}")
    print(f"  Total Interventions: {result.intervention_count}")
    
    print("\n[SLEEP OUTCOMES]")
    print(f"  Baseline Sleep: {patient_profile['sleep_avg_hours']} hours")
    print(f"  Average Sleep: {result.average_sleep_hours:.2f} hours")
    print(f"  Sleep Improvement: {result.sleep_improvement:+.2f} hours")
    
    print("\n[PHYSIOLOGICAL CHANGES]")
    print(f"  HRV Change: {result.hrv_change:+.2f} ms")
    print(f"  Resting HR Change: {result.resting_hr_change:+.1f} bpm")
    
    print("\n[OVERALL OUTCOME]")
    print(f"  Success Score: {result.success_score:.2f}/1.00")
    print(f"  Health Impact: {result.estimated_health_impact.replace('_', ' ').title()}")
    
    # Analyze sleep trajectory
    print("\n[SLEEP TRAJECTORY ANALYSIS]")
    sleep_values = [d.sleep_hours for d in result.trajectory]
    print(f"  Minimum Sleep: {min(sleep_values):.2f} hours")
    print(f"  Maximum Sleep: {max(sleep_values):.2f} hours")
    print(f"  First Week Avg: {sum(sleep_values[:7])/7:.2f} hours")
    print(f"  Last Week Avg: {sum(sleep_values[-7:])/7:.2f} hours")
    
    # Analyze emotional patterns
    print("\n[EMOTIONAL STATE DISTRIBUTION]")
    emotion_counts = {}
    for d in result.trajectory:
        emotion_counts[d.emotion_state] = emotion_counts.get(d.emotion_state, 0) + 1
    
    for emotion, count in sorted(emotion_counts.items(), key=lambda x: -x[1]):
        pct = count / len(result.trajectory) * 100
        print(f"  {emotion:15s}: {count:3d} days ({pct:.1f}%)")
    
    # Show sample days
    print("\n[SAMPLE DAILY LOGS]")
    sample_days = [1, 15, 30, 60, 90]
    for day_num in sample_days:
        if day_num <= len(result.trajectory):
            state = result.trajectory[day_num - 1]
            print(f"\n  Day {state.day}:")
            print(f"    Emotional State: {state.emotion_state}")
            print(f"    Stress Level: {state.stress_level:.2f}")
            print(f"    Sleep Hours: {state.sleep_hours:.2f}")
            print(f"    Compliance: {'Yes' if state.compliance else 'No'}")
            if state.intervention_triggered:
                print(f"    [INTERVENTION] {state.intervention_message}")
    
    print("\n" + "=" * 60)
    print("       Research Disclaimer: Simulation results are")
    print("       for research purposes only and should not be")
    print("       used for clinical decision-making.")
    print("=" * 60 + "\n")
    
    return result


def compare_interventions():
    """Compare different intervention approaches for sleep."""
    
    print("\n" + "=" * 60)
    print("     COMPARING INTERVENTION APPROACHES")
    print("=" * 60)
    
    engine = SimulationEngine(seed=42)
    
    patient = {
        "age": 34,
        "bmi": 25,
        "sleep_avg_hours": 5.5,
        "stress_level": "high",
    }
    
    interventions = ["sleep_optimization", "stress_management", "holistic_wellness"]
    
    results = {}
    for intervention in interventions:
        result = engine.run_simulation(
            patient_profile=patient,
            intervention_plan=intervention,
            simulation_days=90,
        )
        results[intervention] = result
    
    print("\n[COMPARISON RESULTS]")
    print(f"{'Intervention':25s} | {'Compliance':>10s} | {'Sleep Δ':>8s} | {'Success':>8s}")
    print("-" * 60)
    
    for name, result in results.items():
        print(
            f"{name:25s} | {result.compliance_rate:>10.1%} | "
            f"{result.sleep_improvement:>+8.2f}h | {result.success_score:>8.2f}"
        )
    
    print("\n" + "=" * 60 + "\n")
    
    return results


if __name__ == "__main__":
    # Run main simulation
    result = run_sleep_simulation()
    
    # Optionally compare interventions
    # compare_interventions()
