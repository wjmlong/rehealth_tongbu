"""
Orchestrator Agent

Coordinates the simulation loop and orchestrates all other agents.
"""

from dataclasses import dataclass, field
from typing import Optional, List, Tuple
from datetime import datetime, timedelta
import random

from healthagent.models.patient_profile import PatientProfile
from healthagent.models.intervention import InterventionPlan
from healthagent.models.trajectory import DailyState, SimulationResult

from healthagent.agents.emotion_agent import EmotionAgent, EmotionState
from healthagent.agents.compliance_agent import ComplianceAgent, ComplianceDecision
from healthagent.agents.physiology_agent import PhysiologyAgent, PhysiologyState
from healthagent.agents.intervention_agent import InterventionAgent, InterventionDecision


@dataclass
class SimulationContext:
    """Context object passed between agents during simulation."""
    day: int
    day_of_week: int
    date: datetime
    
    # Previous day's values
    prev_compliance: bool = True
    prev_sleep_hours: float = 7.0
    prev_sleep_quality: float = 0.6
    
    # Current day's computed values
    emotion_state: Optional[EmotionState] = None
    compliance_decision: Optional[ComplianceDecision] = None
    physiology_state: Optional[PhysiologyState] = None
    intervention_decision: Optional[InterventionDecision] = None


class OrchestratorAgent:
    """
    Main orchestrator that coordinates the multi-agent simulation.
    
    The orchestrator:
    1. Initializes all agents with patient profile and intervention plan
    2. Runs the day-by-day simulation loop
    3. Coordinates information flow between agents
    4. Collects and aggregates results
    """
    
    def __init__(self, patient: PatientProfile, intervention: InterventionPlan,
                 seed: Optional[int] = None):
        """
        Initialize OrchestratorAgent.
        
        Args:
            patient: Patient profile
            intervention: Intervention plan
            seed: Random seed for reproducibility
        """
        self.patient = patient
        self.intervention = intervention
        self.seed = seed
        
        # Initialize random number generator
        self.rng = random.Random(seed)
        
        # Initialize all agents with derived seeds for reproducibility
        self.emotion_agent = EmotionAgent(patient, seed=self._derive_seed("emotion"))
        self.compliance_agent = ComplianceAgent(patient, intervention, seed=self._derive_seed("compliance"))
        self.physiology_agent = PhysiologyAgent(patient, intervention, seed=self._derive_seed("physiology"))
        self.intervention_agent = InterventionAgent(patient, intervention, seed=self._derive_seed("intervention"))
        
        # Simulation state
        self.trajectory: List[DailyState] = []
        self.start_date: Optional[datetime] = None
    
    def _derive_seed(self, agent_name: str) -> int:
        """Derive a seed for an agent from the main seed."""
        if self.seed is None:
            return None
        agent_seeds = {
            "emotion": 1000,
            "compliance": 2000,
            "physiology": 3000,
            "intervention": 4000,
        }
        return self.seed + agent_seeds.get(agent_name, 0)
    
    def run(self, days: Optional[int] = None, start_date: Optional[datetime] = None) -> SimulationResult:
        """
        Run the full simulation.
        
        Args:
            days: Number of days to simulate (defaults to intervention duration)
            start_date: Starting date (defaults to today)
            
        Returns:
            SimulationResult with trajectory and summary statistics
        """
        # Set simulation parameters
        simulation_days = days or self.intervention.duration_days
        self.start_date = start_date or datetime.now()
        
        # Reset trajectory
        self.trajectory = []
        
        # Initialize context
        context = SimulationContext(
            day=0,
            day_of_week=self.start_date.weekday(),
            date=self.start_date,
            prev_sleep_hours=self.patient.sleep_avg_hours,
            prev_sleep_quality=0.6,
        )
        
        # Run day-by-day simulation
        for day in range(1, simulation_days + 1):
            # Update context for new day
            context.day = day
            context.date = self.start_date + timedelta(days=day - 1)
            context.day_of_week = context.date.weekday()
            
            # Run agents in sequence
            context = self._simulate_day(context)
            
            # Create daily state record
            daily_state = self._create_daily_state(context)
            self.trajectory.append(daily_state)
            
            # Update context for next iteration
            context.prev_compliance = context.compliance_decision.compliant
            context.prev_sleep_hours = context.physiology_state.sleep_hours
            context.prev_sleep_quality = context.physiology_state.sleep_quality
        
        # Create and return result
        return self._create_simulation_result(simulation_days)
    
    def _simulate_day(self, context: SimulationContext) -> SimulationContext:
        """
        Simulate a single day by calling all agents.
        
        Agent execution order:
        1. EmotionAgent - Calculate emotional state
        2. ComplianceAgent - Decide if patient complies
        3. PhysiologyAgent - Update physiological markers
        4. InterventionAgent - Check if intervention needed
        """
        # 1. Emotion Agent
        context.emotion_state = self.emotion_agent.simulate(
            day=context.day,
            day_of_week=context.day_of_week,
            previous_compliance=context.prev_compliance,
            sleep_hours=context.prev_sleep_hours,
            sleep_quality=context.prev_sleep_quality,
        )
        
        # 2. Compliance Agent
        context.compliance_decision = self.compliance_agent.decide(
            stress_level=context.emotion_state.stress_level,
            motivation=context.emotion_state.motivation,
            day_of_week=context.day_of_week,
            sleep_quality=context.prev_sleep_quality,
            fatigue=context.emotion_state.fatigue,
        )
        
        # 3. Physiology Agent
        context.physiology_state = self.physiology_agent.simulate(
            stress_level=context.emotion_state.stress_level,
            compliance=context.compliance_decision.compliant,
            compliance_score=context.compliance_decision.compliance_score,
            day=context.day,
        )
        
        # 4. Intervention Agent
        # Get trends for intervention decision
        trends = self.physiology_agent.get_trends()
        hrv_trend = trends.get("hrv_change", 0)
        sleep_trend = trends.get("sleep_hours_change", 0)
        
        context.intervention_decision = self.intervention_agent.evaluate(
            day=context.day,
            compliance_probability=context.compliance_decision.probability,
            stress_level=context.emotion_state.stress_level,
            recent_compliance=context.prev_compliance,
            hrv_trend=hrv_trend,
            sleep_trend=sleep_trend,
        )
        
        return context
    
    def _create_daily_state(self, context: SimulationContext) -> DailyState:
        """Create a DailyState record from simulation context."""
        # Generate narrative
        narrative = self._generate_narrative(context)
        
        return DailyState(
            day=context.day,
            emotion_state=context.emotion_state.emotion_state,
            stress_level=context.emotion_state.stress_level,
            motivation=context.emotion_state.motivation,
            compliance=context.compliance_decision.compliant,
            compliance_score=context.compliance_decision.compliance_score,
            sleep_hours=context.physiology_state.sleep_hours,
            sleep_quality=context.physiology_state.sleep_quality,
            hrv=context.physiology_state.hrv,
            resting_hr=context.physiology_state.resting_hr,
            intervention_triggered=context.intervention_decision.triggered,
            intervention_message=context.intervention_decision.message,
            narrative=narrative,
            day_of_week=context.day_of_week + 1,  # Convert to 1-indexed
        )
    
    def _generate_narrative(self, context: SimulationContext) -> str:
        """Generate a human-readable narrative for the day."""
        parts = []
        
        # Day and emotion
        dow_names = ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"]
        dow = dow_names[context.day_of_week]
        parts.append(f"Day {context.day} ({dow}):")
        
        # Emotional state
        emotion = context.emotion_state
        parts.append(f"Feeling {emotion.emotion_state} with stress at {emotion.stress_level:.0%}.")
        
        # Compliance
        if context.compliance_decision.compliant:
            parts.append("Successfully followed the intervention plan.")
        else:
            parts.append("Did not complete today's intervention tasks.")
        
        # Physiology
        phys = context.physiology_state
        parts.append(f"Slept {phys.sleep_hours:.1f} hours (quality: {phys.sleep_quality:.0%}).")
        
        # Intervention
        if context.intervention_decision.triggered and context.intervention_decision.message:
            parts.append(f"[Nudge] {context.intervention_decision.message}")
        
        return " ".join(parts)
    
    def _create_simulation_result(self, simulation_days: int) -> SimulationResult:
        """Create the final simulation result with summary statistics."""
        if not self.trajectory:
            return SimulationResult()
        
        # Calculate changes from baseline
        first_day = self.trajectory[0]
        last_day = self.trajectory[-1]
        
        sleep_improvement = last_day.sleep_hours - self.patient.sleep_avg_hours
        hrv_change = last_day.hrv - (self.patient.hrv_baseline or 50)
        resting_hr_change = last_day.resting_hr - self.patient.resting_hr
        
        # Calculate compliance rate
        compliant_days = sum(1 for d in self.trajectory if d.compliance)
        compliance_rate = compliant_days / len(self.trajectory)
        
        return SimulationResult(
            trajectory=self.trajectory,
            compliance_rate=compliance_rate,
            average_sleep_hours=sum(d.sleep_hours for d in self.trajectory) / len(self.trajectory),
            sleep_improvement=sleep_improvement,
            hrv_change=hrv_change,
            resting_hr_change=resting_hr_change,
            intervention_count=sum(1 for d in self.trajectory if d.intervention_triggered),
            simulation_days=simulation_days,
            start_date=self.start_date,
            end_date=self.start_date + timedelta(days=simulation_days - 1),
        )
