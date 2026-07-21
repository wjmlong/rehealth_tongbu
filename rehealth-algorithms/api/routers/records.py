import sys
sys.path.insert(0, r'C:\Users\Administrator\Desktop\ReHealth AI HealthAgent')
from fastapi import APIRouter
from pydantic import BaseModel
from typing import Optional
from healthagent.patient_db.db import add_daily_record, get_daily_records

router = APIRouter()

class DailyRecord(BaseModel):
    patient_id: int
    record_date: Optional[str] = None
    systolic_bp: Optional[int] = None
    diastolic_bp: Optional[int] = None
    fasting_glucose: Optional[float] = None
    weight: Optional[float] = None
    steps: Optional[int] = None
    exercise_minutes: Optional[int] = None
    mood_score: Optional[int] = None
    sleep_hours: Optional[float] = None
    notes: str = ""

@router.post("/")
def create_record(data: DailyRecord):
    return add_daily_record(**data.model_dump())

@router.get("/{patient_id}")
def get_records(patient_id: int, days: int = 30):
    return get_daily_records(patient_id, days)
