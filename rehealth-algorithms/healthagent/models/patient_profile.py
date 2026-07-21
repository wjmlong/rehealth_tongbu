from dataclasses import dataclass
from enum import Enum


class StressLevel(Enum):
    LOW = "low"
    MEDIUM = "medium"
    HIGH = "high"


class AgeGroup(Enum):
    YOUNG = "young"
    ADULT = "adult"
    MIDDLE_AGED = "middle_aged"
    SENIOR = "senior"


@dataclass
class PatientProfile:

    # Basic information
    name: str = "Patient"
    age: int = 0
    age_group: str = "adult"  # young, adult, middle_aged, older_adult

    # Body metrics
    height: float = 0.0
    weight: float = 0.0
    bmi: float = 0.0
    resting_hr: int = 70
    hrv_baseline: float = 45.0  # Heart rate variability baseline

    # Lifestyle
    sleep_avg_hours: float = 7.0
    exercise_per_week: int = 0

    # Psychological state
    stress_level: StressLevel = StressLevel.MEDIUM
    motivation_score: float = 0.5  # Motivation score (0-1)

    # Health issues
    primary_issue: str = ""
