from dataclasses import dataclass, field
from typing import List, Optional
from datetime import datetime


@dataclass
class DailyState:
    day: int
    emotion_state: str
    stress_level: float
    motivation: float
    compliance: bool
    compliance_score: float
    sleep_hours: float
    sleep_quality: float
    hrv: float
    resting_hr: int
    intervention_triggered: bool
    intervention_message: Optional[str] = None
    narrative: str = ""
    day_of_week: int = 1


@dataclass
class SimulationResult:
    trajectory: List[DailyState] = field(default_factory=list)
    compliance_rate: float = 0.0
    average_sleep_hours: float = 0.0
    sleep_improvement: float = 0.0
    hrv_change: float = 0.0
    resting_hr_change: int = 0
    intervention_count: int = 0
    simulation_days: int = 0
    start_date: Optional[datetime] = None
    end_date: Optional[datetime] = None
