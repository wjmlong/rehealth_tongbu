from dataclasses import dataclass
from enum import Enum


class InterventionType(Enum):
    SLEEP_OPTIMIZATION = "sleep_optimization"
    CARDIO_FITNESS = "cardio_fitness"
    CARDIO_TRAINING = "cardio_training"
    STRESS_REDUCTION = "stress_reduction"
    STRESS_MANAGEMENT = "stress_management"
    NUTRITION_PLAN = "nutrition_plan"
    HYBRID = "hybrid"


@dataclass
class InterventionPlan:
    name: str
    intervention_type: InterventionType
    duration_days: int
    intensity: float = 0.5
    difficulty: float = 0.5
    average_effectiveness: float = 0.6
    nudge_frequency: int = 7
    rules: list = None
    
    def __post_init__(self):
        if self.rules is None:
            self.rules = []