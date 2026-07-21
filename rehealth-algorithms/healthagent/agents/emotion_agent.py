"""
Emotion Agent

Simulates daily emotional state including stress, motivation, and fatigue.
"""

from dataclasses import dataclass
from typing import Optional, Tuple
import random
import math

from healthagent.models.patient_profile import PatientProfile, StressLevel


@dataclass
class EmotionState:
    """Represents the emotional state for a given day."""
    emotion_state: str
    stress_level: float  # 0.0 to 1.0
    motivation: float  # 0.0 to 1.0
    fatigue: float  # 0.0 to 1.0
    
    def to_dict(self) -> dict:
        return {
            "emotion_state": self.emotion_state,
            "stress_level": round(self.stress_level, 3),
            "motivation": round(self.motivation, 3),
            "fatigue": round(self.fatigue, 3),
        }


class EmotionAgent:
    """
    Agent that simulates emotional state dynamics.
    
    Emotional states are influenced by:
    - Baseline stress level
    - Day of week (weekends are less stressful)
    - Previous compliance (success breeds motivation)
    - Sleep quality
    - Random life events
    """
    
    # Emotion state categories
    EMOTION_STATES = [
        "calm", "stressed", "motivated", "fatigued", 
        "anxious", "content", "frustrated", "hopeful"
    ]
    
    # Day-of-week stress modifiers (Monday=0, Sunday=6)
    DOW_STRESS_MODIFIERS = {
        0: 0.15,  # Monday - higher stress
        1: 0.05,  # Tuesday
        2: 0.0,   # Wednesday - baseline
        3: -0.05, # Thursday
        4: -0.10, # Friday - lower stress
        5: -0.20, # Saturday - lowest stress
        6: -0.15, # Sunday
    }
    
    def __init__(self, patient: PatientProfile, seed: Optional[int] = None):
        """
        Initialize EmotionAgent.
        
        Args:
            patient: Patient profile with baseline characteristics
            seed: Random seed for reproducibility
        """
        self.patient = patient
        self.rng = random.Random(seed)
        
        # Initialize state
        self._stress_baseline = self._get_baseline_stress()
        self._motivation_baseline = patient.motivation_score
        self._previous_stress = self._stress_baseline
        self._previous_compliance = True
    
    def _get_baseline_stress(self) -> float:
        """Convert stress level enum to numeric baseline."""
        stress_map = {
            StressLevel.LOW: 0.25,
            StressLevel.MEDIUM: 0.50,
            StressLevel.HIGH: 0.75,
        }
        return stress_map.get(self.patient.stress_level, 0.50)
    
    def simulate(self, day: int, day_of_week: int, previous_compliance: bool,
                 sleep_hours: float, sleep_quality: float) -> EmotionState:
        """
        Simulate emotional state for a given day.
        
        Args:
            day: Current day in simulation
            day_of_week: Day of week (0=Monday, 6=Sunday)
            previous_compliance: Whether patient complied yesterday
            sleep_hours: Hours of sleep last night
            sleep_quality: Quality of sleep (0-1)
            
        Returns:
            EmotionState for the current day
        """
        # Calculate stress level
        stress = self._calculate_stress(day_of_week, sleep_hours, sleep_quality)
        
        # Calculate motivation
        motivation = self._calculate_motivation(previous_compliance, day)
        
        # Calculate fatigue
        fatigue = self._calculate_fatigue(sleep_hours, stress)
        
        # Determine dominant emotion state
        emotion_state = self._determine_emotion_state(stress, motivation, fatigue)
        
        # Update internal state
        self._previous_stress = stress
        self._previous_compliance = previous_compliance
        
        return EmotionState(
            emotion_state=emotion_state,
            stress_level=stress,
            motivation=motivation,
            fatigue=fatigue,
        )
    
    def _calculate_stress(self, day_of_week: int, sleep_hours: float,
                          sleep_quality: float) -> float:
        """
        Calculate stress level for the day.
        
        Factors:
        - Baseline stress
        - Day of week
        - Sleep quality
        - Random events (10% chance of stressor)
        """
        # Start with baseline
        stress = self._stress_baseline
        
        # Apply day-of-week modifier
        dow_modifier = self.DOW_STRESS_MODIFIERS.get(day_of_week, 0)
        stress += dow_modifier
        
        # Sleep quality influence (poor sleep increases stress)
        sleep_stress_factor = (1 - sleep_quality) * 0.15
        stress += sleep_stress_factor
        
        # Sleep duration influence (less than 6 hours increases stress)
        if sleep_hours < 6:
            stress += (6 - sleep_hours) * 0.05
        
        # Random stressor event (10% chance)
        if self.rng.random() < 0.10:
            stress += self.rng.uniform(0.1, 0.3)
        
        # Stress recovery (tends to return toward baseline)
        stress = 0.7 * stress + 0.3 * self._previous_stress
        
        # Clamp to valid range
        return max(0.0, min(1.0, stress))
    
    def _calculate_motivation(self, previous_compliance: bool, day: int) -> float:
        """
        Calculate motivation level for the day.
        
        Motivation is influenced by:
        - Baseline motivation
        - Previous compliance (success breeds success)
        - Progress through intervention (early days = higher motivation)
        - Recent compliance streak
        """
        # Start with baseline
        motivation = self._motivation_baseline
        
        # Compliance influence
        if previous_compliance:
            motivation += 0.1  # Boost from yesterday's success
        else:
            motivation -= 0.15  # Demotivation from failure
        
        # Time-based motivation (early enthusiasm, mid-slump, late commitment)
        if day <= 7:
            motivation += 0.15  # New year effect
        elif day <= 21:
            motivation -= 0.05  # Reality sets in
        elif day <= 45:
            motivation -= 0.10  # Mid-intervention slump
        else:
            motivation += 0.05  # End in sight
        
        # Random fluctuation
        motivation += self.rng.uniform(-0.1, 0.1)
        
        # Gradual return toward baseline
        motivation = 0.8 * motivation + 0.2 * self._motivation_baseline
        
        return max(0.0, min(1.0, motivation))
    
    def _calculate_fatigue(self, sleep_hours: float, stress: float) -> float:
        """
        Calculate fatigue level for the day.
        
        Fatigue is influenced by:
        - Sleep duration
        - Stress level
        - Cumulative effect
        """
        # Base fatigue from sleep
        if sleep_hours >= 7.5:
            sleep_fatigue = 0.2
        elif sleep_hours >= 6.5:
            sleep_fatigue = 0.35
        else:
            sleep_fatigue = 0.5 + (6.5 - sleep_hours) * 0.15
        
        # Stress adds to fatigue
        stress_fatigue = stress * 0.3
        
        fatigue = sleep_fatigue + stress_fatigue
        
        # Add randomness
        fatigue += self.rng.uniform(-0.1, 0.15)
        
        return max(0.0, min(1.0, fatigue))
    
    def _determine_emotion_state(self, stress: float, motivation: float,
                                  fatigue: float) -> str:
        """
        Determine the dominant emotion state label.
        
        Uses a rule-based approach considering stress, motivation, and fatigue.
        """
        # Decision tree for emotion state
        if stress > 0.7:
            if motivation > 0.6:
                return "anxious"
            else:
                return "stressed"
        
        if fatigue > 0.6:
            if stress > 0.4:
                return "frustrated"
            else:
                return "fatigued"
        
        if motivation > 0.7:
            if stress < 0.3:
                return "motivated"
            else:
                return "hopeful"
        
        if stress < 0.35 and motivation > 0.5:
            return "content"
        
        if stress < 0.4 and fatigue < 0.4:
            return "calm"
        
        # Default state based on dominant factor
        if stress >= fatigue and stress >= motivation:
            return "stressed"
        elif fatigue >= motivation:
            return "fatigued"
        else:
            return "calm"
