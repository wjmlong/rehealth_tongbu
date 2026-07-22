"""
Intervention Agent

Detects compliance risk and triggers intervention nudges.
"""

from dataclasses import dataclass
from typing import Optional, List
import random

from healthagent.models.patient_profile import PatientProfile
from healthagent.models.intervention import InterventionPlan, InterventionType


# Intervention message templates by type
INTERVENTION_MESSAGES = {
    InterventionType.SLEEP_OPTIMIZATION: {
        "encouragement": [
            "Great progress! Your sleep pattern is improving.",
            "You're building healthy sleep habits - keep it up!",
            "Consistency is key. Your body is adapting well.",
        ],
        "reminder": [
            "Don't forget your wind-down routine tonight.",
            "Remember: screens off 1 hour before bed.",
            "Tonight's goal: lights out by your target time.",
        ],
        "warning": [
            "I notice your sleep quality has declined. Let's refocus.",
            "Stress is affecting your sleep. Try some relaxation techniques.",
            "You've missed several nights. Let's get back on track.",
        ],
        "intervention": [
            "Time for a check-in. How can we help you sleep better?",
            "Consider adjusting your bedtime routine for better results.",
            "Let's review your sleep goals and make adjustments.",
        ],
    },
    InterventionType.CARDIO_FITNESS: {
        "encouragement": [
            "Your resting heart rate is improving!",
            "Great job staying active. Your cardiovascular health is benefiting.",
            "You're building fitness momentum!",
        ],
        "reminder": [
            "Time for your daily walk or exercise session.",
            "Don't forget your active breaks today.",
            "Your heart will thank you for today's workout.",
        ],
        "warning": [
            "Activity levels have dropped. Let's get moving again.",
            "Your resting HR is elevated - exercise can help.",
            "Missing workouts affects progress. Let's reset.",
        ],
        "intervention": [
            "Let's modify your exercise plan to fit your current situation.",
            "Consider shorter, more frequent activity sessions.",
            "How can we make your fitness routine more sustainable?",
        ],
    },
    InterventionType.STRESS_MANAGEMENT: {
        "encouragement": [
            "Your stress levels are trending down. Keep practicing!",
            "Mindfulness is working. You're handling challenges better.",
            "Great consistency with your stress management practices.",
        ],
        "reminder": [
            "Time for your daily mindfulness session.",
            "Remember: 3 deep breaths can shift your state.",
            "Schedule your nature break today.",
        ],
        "warning": [
            "Stress levels are rising. Prioritize self-care today.",
            "Warning signs detected. Let's focus on stress relief.",
            "Don't skip your stress management practices today.",
        ],
        "intervention": [
            "Let's add extra stress relief techniques this week.",
            "Consider scheduling a longer relaxation session.",
            "Time for a stress management check-in.",
        ],
    },
    InterventionType.HYBRID: {
        "encouragement": [
            "Your holistic approach is paying off!",
            "Multiple health markers are improving - great work!",
            "You're building sustainable healthy habits.",
        ],
        "reminder": [
            "Stay consistent with your wellness routine today.",
            "Remember your daily health commitments.",
            "Small daily actions lead to big changes.",
        ],
        "warning": [
            "Several health indicators need attention.",
            "Your routine has slipped. Let's get back on track.",
            "Multiple factors suggest you need extra support.",
        ],
        "intervention": [
            "Let's reassess your wellness priorities.",
            "Time for a comprehensive health check-in.",
            "Consider which area needs the most focus right now.",
        ],
    },
}


@dataclass
class InterventionDecision:
    """Represents an intervention decision."""
    triggered: bool
    message: Optional[str]
    intervention_type: str
    priority: str  # "low", "medium", "high"
    reason: str
    
    def to_dict(self) -> dict:
        return {
            "triggered": self.triggered,
            "message": self.message,
            "intervention_type": self.intervention_type,
            "priority": self.priority,
        }


class InterventionAgent:
    """
    Agent that detects compliance risk and triggers intervention nudges.
    
    Triggers interventions based on:
    - Low compliance probability
    - Declining trends in health markers
    - Streaks of non-compliance
    - High stress indicators
    - Scheduled check-in points
    """
    
    def __init__(self, patient: PatientProfile, intervention: InterventionPlan,
                 seed: Optional[int] = None):
        """
        Initialize InterventionAgent.
        
        Args:
            patient: Patient profile
            intervention: Intervention plan
            seed: Random seed for reproducibility
        """
        self.patient = patient
        self.intervention = intervention
        self.rng = random.Random(seed)
        
        # Track intervention history
        self.last_intervention_day = -100  # Days since last intervention
        self.intervention_count = 0
        
        # Track compliance streaks for intervention logic
        self.non_compliance_streak = 0
        self.compliance_streak = 0
    
    def evaluate(self, day: int, compliance_probability: float,
                 stress_level: float, recent_compliance: bool,
                 hrv_trend: float, sleep_trend: float) -> InterventionDecision:
        """
        Evaluate whether to trigger an intervention.
        
        Args:
            day: Current day in simulation
            compliance_probability: Predicted compliance probability
            stress_level: Current stress level
            recent_compliance: Whether patient complied yesterday
            hrv_trend: Recent HRV change
            sleep_trend: Recent sleep change
            
        Returns:
            InterventionDecision with whether to trigger and message
        """
        # Update streak tracking
        if recent_compliance:
            self.compliance_streak += 1
            self.non_compliance_streak = 0
        else:
            self.non_compliance_streak += 1
            self.compliance_streak = 0
        
        # Check intervention conditions
        should_intervene, priority, reason = self._check_intervention_conditions(
            day, compliance_probability, stress_level,
            hrv_trend, sleep_trend
        )
        
        # Select and format message
        if should_intervene:
            message = self._select_message(priority, reason)
            self.last_intervention_day = day
            self.intervention_count += 1
        else:
            message = None
        
        return InterventionDecision(
            triggered=should_intervene,
            message=message,
            intervention_type=self.intervention.intervention_type.value,
            priority=priority,
            reason=reason,
        )
    
    def _check_intervention_conditions(self, day: int, compliance_prob: float,
                                        stress_level: float, hrv_trend: float,
                                        sleep_trend: float) -> tuple:
        """
        Check if intervention should be triggered.
        
        Returns:
            Tuple of (should_intervene, priority, reason)
        """
        reasons = []
        priority = "low"
        
        # Check compliance probability
        if compliance_prob < 0.3:
            reasons.append("very_low_compliance_probability")
            priority = "high"
        elif compliance_prob < 0.5:
            reasons.append("low_compliance_probability")
            priority = max(priority, "medium")
        
        # Check non-compliance streak
        if self.non_compliance_streak >= 3:
            reasons.append("non_compliance_streak")
            priority = "high"
        elif self.non_compliance_streak >= 2:
            reasons.append("consecutive_misses")
            priority = max(priority, "medium")
        
        # Check stress level
        if stress_level > 0.8:
            reasons.append("high_stress")
            priority = max(priority, "high")
        elif stress_level > 0.6:
            reasons.append("elevated_stress")
            priority = max(priority, "medium")
        
        # Check declining health markers
        if hrv_trend < -5:
            reasons.append("hrv_declining")
            priority = max(priority, "medium")
        
        if sleep_trend < -1:
            reasons.append("sleep_declining")
            priority = max(priority, "medium")
        
        # Scheduled check-in
        nudge_frequency = self.intervention.nudge_frequency
        days_since_last = day - self.last_intervention_day
        if days_since_last >= nudge_frequency and day > 3:
            if not reasons:  # Only add if no other reasons
                reasons.append("scheduled_checkin")
                priority = max(priority, "low")
        
        # Cooldown: Don't intervene too frequently
        if days_since_last < 2 and priority != "high":
            return False, "low", "cooldown"
        
        # Determine final decision
        should_intervene = len(reasons) > 0
        
        if not should_intervene:
            reason = "no_intervention_needed"
        else:
            reason = reasons[0]  # Primary reason
        
        return should_intervene, priority, reason
    
    def _select_message(self, priority: str, reason: str) -> str:
        """
        Select an appropriate intervention message.
        
        Args:
            priority: Intervention priority
            reason: Reason for intervention
            
        Returns:
            Selected message string
        """
        # Determine message category based on reason
        if "low_compliance" in reason or "streak" in reason or "misses" in reason:
            category = "warning" if priority == "high" else "reminder"
        elif "stress" in reason:
            category = "warning"
        elif "declining" in reason:
            category = "warning"
        elif "checkin" in reason:
            category = "encouragement" if self.compliance_streak > 3 else "reminder"
        else:
            category = "reminder"
        
        # Get message templates for this intervention type
        messages_by_type = INTERVENTION_MESSAGES.get(
            self.intervention.intervention_type,
            INTERVENTION_MESSAGES[InterventionType.HYBRID]
        )
        
        # Select random message from category
        messages = messages_by_type.get(category, messages_by_type["reminder"])
        return self.rng.choice(messages)
    
    def get_intervention_count(self) -> int:
        """Get total number of interventions triggered."""
        return self.intervention_count
