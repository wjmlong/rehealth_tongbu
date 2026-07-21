"""
Simulation Engine

High-level interface for running health intervention simulations.
"""

from dataclasses import dataclass
from typing import Optional, Dict, Any
from datetime import datetime
import json

from healthagent.models.patient_profile import PatientProfile, StressLevel
from healthagent.models.intervention import InterventionPlan, InterventionType, get_intervention_plan
from healthagent.models.trajectory import SimulationResult
from healthagent.agents.orchestrator import OrchestratorAgent


class SimulationEngine:
    """
    High-level simulation engine for health intervention modeling.
    
    Provides a simple interface to run simulations with various configurations.
    
    Example:
        ```python
        engine = SimulationEngine()
        
        result = engine.run_simulation(
            patient_profile={
                "age": 34,
                "bmi": 25,
                "sleep_avg_hours": 5.8,
                "stress_level": "high",
            },
            intervention_plan="sleep_optimization",
            simulation_days=90
        )
        
        print(result)
        ```
    """
    
    def __init__(self, seed: Optional[int] = None):
        """
        Initialize SimulationEngine.
        
        Args:
            seed: Random seed for reproducibility
        """
        self.seed = seed
        self._last_result: Optional[SimulationResult] = None
    
    def run_simulation(
        self,
        patient_profile: Dict[str, Any],
        intervention_plan: Optional[str] = None,
        simulation_days: Optional[int] = None,
        custom_intervention: Optional[InterventionPlan] = None,
        start_date: Optional[datetime] = None,
    ) -> SimulationResult:
        """
        Run a health intervention simulation.
        
        Args:
            patient_profile: Dictionary with patient parameters
                Required: age, bmi, sleep_avg_hours, stress_level
                Optional: gender, resting_hr, occupation, wearable,
                         motivation_score, habit_strength
            intervention_plan: Name of predefined intervention plan
                Options: "sleep_optimization", "cardio_fitness", 
                        "stress_management", "holistic_wellness"
            simulation_days: Number of days to simulate (default: intervention duration)
            custom_intervention: Custom InterventionPlan object
            start_date: Starting date for simulation
            
        Returns:
            SimulationResult with trajectory and summary statistics
        """
        # Create patient profile
        patient = self._create_patient_profile(patient_profile)
        
        # Get intervention plan
        if custom_intervention:
            intervention = custom_intervention
        elif intervention_plan:
            intervention = get_intervention_plan(intervention_plan)
            if intervention is None:
                raise ValueError(f"Unknown intervention plan: {intervention_plan}")
        else:
            # Default intervention
            intervention = get_intervention_plan("sleep_optimization")
        
        # Override duration if specified
        if simulation_days:
            intervention.duration_days = simulation_days
        
        # Create orchestrator and run
        orchestrator = OrchestratorAgent(
            patient=patient,
            intervention=intervention,
            seed=self.seed,
        )
        
        result = orchestrator.run(days=simulation_days, start_date=start_date)
        
        # Store result
        self._last_result = result
        
        return result
    
    def _create_patient_profile(self, params: Dict[str, Any]) -> PatientProfile:
        """
        Create a PatientProfile from a dictionary of parameters.
        
        Args:
            params: Dictionary with patient parameters
            
        Returns:
            PatientProfile instance
        """
        # Extract required parameters
        age = params.get("age")
        bmi = params.get("bmi")
        sleep_avg_hours = params.get("sleep_avg_hours")
        stress_level = params.get("stress_level")
        
        # Validate required parameters
        if age is None:
            raise ValueError("Patient profile must include 'age'")
        if bmi is None:
            raise ValueError("Patient profile must include 'bmi'")
        if sleep_avg_hours is None:
            raise ValueError("Patient profile must include 'sleep_avg_hours'")
        if stress_level is None:
            raise ValueError("Patient profile must include 'stress_level'")
        
        # Convert stress level to enum
        if isinstance(stress_level, str):
            stress_level = StressLevel(stress_level.lower())
        
        # Create profile with optional parameters
        return PatientProfile(
            age=age,
            bmi=bmi,
            sleep_avg_hours=sleep_avg_hours,
            stress_level=stress_level,
            gender=params.get("gender", "other"),
            resting_hr=params.get("resting_hr", 70),
            occupation=params.get("occupation", "office_worker"),
            wearable=params.get("wearable", False),
            motivation_score=params.get("motivation_score", 0.5),
            habit_strength=params.get("habit_strength", 0.3),
            social_support=params.get("social_support", 0.5),
        )
    
    def get_last_result(self) -> Optional[SimulationResult]:
        """Get the most recent simulation result."""
        return self._last_result
    
    def quick_simulation(
        self,
        age: int,
        bmi: float,
        sleep_hours: float,
        stress: str,
        days: int = 90,
        intervention: str = "sleep_optimization",
    ) -> SimulationResult:
        """
        Quick simulation with minimal parameters.
        
        Args:
            age: Patient age
            bmi: Body Mass Index
            sleep_hours: Average sleep hours
            stress: Stress level ("low", "medium", "high")
            days: Simulation duration
            intervention: Intervention plan name
            
        Returns:
            SimulationResult
        """
        return self.run_simulation(
            patient_profile={
                "age": age,
                "bmi": bmi,
                "sleep_avg_hours": sleep_hours,
                "stress_level": stress,
            },
            intervention_plan=intervention,
            simulation_days=days,
        )


def run_simulation(
    patient_profile: PatientProfile,
    intervention_plan: InterventionPlan,
    simulation_days: Optional[int] = None,
    seed: Optional[int] = None,
) -> SimulationResult:
    """
    Convenience function to run a simulation.
    
    Args:
        patient_profile: Patient profile instance
        intervention_plan: Intervention plan instance
        simulation_days: Number of days to simulate
        seed: Random seed
        
    Returns:
        SimulationResult
    """
    orchestrator = OrchestratorAgent(
        patient=patient_profile,
        intervention=intervention_plan,
        seed=seed,
    )
    return orchestrator.run(days=simulation_days)
