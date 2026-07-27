"""
Physiology Agent

Translates behavior and emotional state into physiological markers.
"""

from dataclasses import dataclass
from typing import Optional, Tuple
import random
import math

from healthagent.models.patient_profile import PatientProfile
from healthagent.models.intervention import InterventionPlan, InterventionType


@dataclass
class PhysiologyState:
    """Represents physiological state for a given day."""
    hrv: float  # Heart Rate Variability (ms)
    resting_hr: int  # Resting heart rate (bpm)
    sleep_hours: float  # Hours of sleep
    sleep_quality: float  # Sleep quality score (0-1)
    
    def to_dict(self) -> dict:
        return {
            "hrv": round(self.hrv, 2),
            "resting_hr": self.resting_hr,
            "sleep_hours": round(self.sleep_hours, 2),
            "sleep_quality": round(self.sleep_quality, 3),
        }


class PhysiologyAgent:
    """
    Agent that models physiological responses to behavior and emotional states.
    
    Models the following biomarkers:
    - HRV (Heart Rate Variability): Higher is better, indicates recovery
    - Resting Heart Rate: Lower is better, indicates cardiovascular fitness
    - Sleep Hours: Actual sleep duration
    - Sleep Quality: Composite score of sleep effectiveness
    
    These markers are influenced by:
    - Compliance with intervention
    - Stress level
    - Previous day's physiology
    - Intervention type and effectiveness
    """
    
    # HRV baseline by age group (approximate)
    HRV_BASELINE = {
        "young_adult": 60,  # 18-29
        "middle_aged": 45,  # 30-49
        "older_adult": 35,  # 50+
    }
    
    # Resting HR baseline by fitness level
    RHR_BASELINE = {
        "fit": 55,
        "average": 70,
        "unfit": 80,
    }
    
    def __init__(self, patient: PatientProfile, intervention: InterventionPlan,
                 seed: Optional[int] = None):
        """
        Initialize PhysiologyAgent.
        
        Args:
            patient: Patient profile
            intervention: Intervention plan
            seed: Random seed for reproducibility
        """
        self.patient = patient
        self.intervention = intervention
        self.rng = random.Random(seed)
        
        # Initialize physiological state
        self._hrv = patient.hrv_baseline or self._estimate_hrv_baseline()
        self._resting_hr = patient.resting_hr
        self._sleep_hours = patient.sleep_avg_hours
        self._sleep_quality = 0.6  # Baseline quality
        
        # Track history for trends
        self.hrv_history = [self._hrv]
        self.rhr_history = [self._resting_hr]
        self.sleep_history = [self._sleep_hours]
    
    def _estimate_hrv_baseline(self) -> float:
        """Estimate HRV baseline based on patient characteristics."""
        base = self.HRV_BASELINE.get(self.patient.age_group, 50)
        
        # Adjust for BMI
        bmi_factor = 1 - max(0, (self.patient.bmi - 25) * 0.02)
        
        # Adjust for stress
        stress_factor = {
            "low": 1.1,
            "medium": 1.0,
            "high": 0.85,
        }.get(self.patient.stress_level.value, 1.0)
        
        return base * bmi_factor * stress_factor
    
    def simulate(self, stress_level: float, compliance: bool, 
                 compliance_score: float, day: int) -> PhysiologyState:
        """
        Simulate physiological state for a given day.
        
        Args:
            stress_level: Current stress level (0-1)
            compliance: Whether patient complied with intervention
            compliance_score: Degree of compliance (0-1)
            day: Current day in simulation
            
        Returns:
            PhysiologyState with updated biomarkers
        """
        # Update sleep
        sleep_hours, sleep_quality = self._simulate_sleep(
            stress_level, compliance, compliance_score
        )
        
        # Update HRV
        hrv = self._simulate_hrv(
            stress_level, compliance, compliance_score, sleep_quality
        )
        
        # Update resting HR
        resting_hr = self._simulate_resting_hr(
            compliance, compliance_score, stress_level
        )
        
        # Store state
        self._hrv = hrv
        self._resting_hr = resting_hr
        self._sleep_hours = sleep_hours
        self._sleep_quality = sleep_quality
        
        # Update history
        self.hrv_history.append(hrv)
        self.rhr_history.append(resting_hr)
        self.sleep_history.append(sleep_hours)
        
        return PhysiologyState(
            hrv=hrv,
            resting_hr=resting_hr,
            sleep_hours=sleep_hours,
            sleep_quality=sleep_quality,
        )
    
    def _simulate_sleep(self, stress_level: float, compliance: bool,
                        compliance_score: float) -> Tuple[float, float]:
        """
        Simulate sleep hours and quality.
        
        Sleep is affected by:
        - Stress (negative)
        - Compliance with sleep interventions
        - Previous sleep patterns
        - Random factors
        """
        # Base sleep from patient average
        base_sleep = self.patient.sleep_avg_hours
        
        # Intervention effect (if sleep-related)
        if self._is_sleep_intervention():
            intervention_effect = 0.3 if compliance else -0.2
        else:
            intervention_effect = 0.1 if compliance else -0.1
        
        # Stress effect (high stress = poor sleep)
        stress_effect = -stress_level * 0.8
        
        # Momentum (tendency to stay near recent average)
        recent_avg = sum(self.sleep_history[-7:]) / min(7, len(self.sleep_history))
        momentum = (recent_avg - base_sleep) * 0.3
        
        # Random variation
        noise = self.rng.gauss(0, 0.3)
        
        # Calculate sleep hours
        sleep_hours = (
            base_sleep +
            intervention_effect +
            stress_effect +
            momentum +
            noise
        )
        
        # Clamp to realistic range
        sleep_hours = max(3.5, min(10.0, sleep_hours))
        
        # Calculate sleep quality
        # Quality is based on hours, stress, and intervention compliance
        hours_quality = 1 - abs(sleep_hours - 8) / 4  # Optimal around 8 hours
        stress_quality_factor = 1 - stress_level * 0.3
        compliance_factor = 1 + (compliance_score - 0.5) * 0.2
        
        sleep_quality = hours_quality * stress_quality_factor * compliance_factor
        sleep_quality = max(0.1, min(1.0, sleep_quality + self.rng.uniform(-0.1, 0.1)))
        
        return sleep_hours, sleep_quality
    
    def _simulate_hrv(self, stress_level: float, compliance: bool,
                      compliance_score: float, sleep_quality: float) -> float:
        """
        Simulate Heart Rate Variability.
        
        HRV increases with:
        - Good sleep
        - Low stress
        - Exercise compliance
        - Recovery
        
        HRV decreases with:
        - High stress
        - Poor sleep
        - Illness/fatigue
        """
        # Previous HRV with slight decay toward baseline
        baseline = self._estimate_hrv_baseline()
        current = self._hrv * 0.7 + baseline * 0.3
        
        # Intervention effect
        if compliance:
            # Compliance boosts HRV
            intervention_boost = self.intervention.average_effectiveness * 3
            current += intervention_boost
        
        # Stress impact (high stress = lower HRV)
        stress_impact = -stress_level * 8
        current += stress_impact
        
        # Sleep quality impact
        sleep_impact = (sleep_quality - 0.5) * 6
        current += sleep_impact
        
        # Trend effect (improvements compound slowly)
        if len(self.hrv_history) >= 7:
            recent_trend = sum(self.hrv_history[-7:]) / 7 - baseline
            if recent_trend > 0:
                current += recent_trend * 0.1
        
        # Random variation
        current += self.rng.gauss(0, 2)
        
        # Clamp to realistic range
        return max(15, min(100, current))
    
    def _simulate_resting_hr(self, compliance: bool, compliance_score: float,
                             stress_level: float) -> int:
        """
        Simulate resting heart rate.
        
        Resting HR decreases with:
        - Cardiovascular exercise
        - Better fitness
        - Low stress
        
        Resting HR increases with:
        - High stress
        - Poor sleep
        - Deconditioning
        """
        # Current state with slight drift toward baseline
        baseline = self.patient.resting_hr
        current = int(self._resting_hr * 0.8 + baseline * 0.2)
        
        # Intervention effect (especially cardio)
        if compliance and self._is_cardio_intervention():
            # Cardio training lowers resting HR
            improvement = int(self.intervention.average_effectiveness * 2)
            current -= improvement
        elif compliance:
            # General compliance has smaller effect
            current -= 1
        
        # Stress effect (acute stress elevates HR)
        stress_effect = int(stress_level * 4)
        current += stress_effect
        
        # Random variation
        current += int(self.rng.gauss(0, 1))
        
        # Clamp to realistic range
        return max(45, min(100, current))
    
    def _is_sleep_intervention(self) -> bool:
        """Check if intervention targets sleep."""
        return self.intervention.intervention_type in [
            InterventionType.SLEEP_OPTIMIZATION,
            InterventionType.HYBRID,
        ]
    
    def _is_cardio_intervention(self) -> bool:
        """Check if intervention targets cardio fitness."""
        return self.intervention.intervention_type in [
            InterventionType.CARDIO_FITNESS,
            InterventionType.HYBRID,
        ]
    
    def get_trends(self) -> dict:
        """Get physiological trends over the simulation."""
        if len(self.hrv_history) < 2:
            return {}
        
        hrv_trend = self.hrv_history[-1] - self.hrv_history[0]
        rhr_trend = self.rhr_history[-1] - self.rhr_history[0]
        sleep_trend = self.sleep_history[-1] - self.sleep_history[0]
        
        return {
            "hrv_change": round(hrv_trend, 2),
            "resting_hr_change": rhr_trend,
            "sleep_hours_change": round(sleep_trend, 2),
        }
