import sys, json
sys.path.insert(0, r'C:\Users\Administrator\Desktop\ReHealth AI HealthAgent')
from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from typing import Optional
from healthagent.patient_db.db import (
    add_patient, list_patients, get_patient,
    add_daily_record, get_health_trend, get_patient_history
)

router = APIRouter()

class PatientCreate(BaseModel):
    name: str
    age: int
    gender: str = "未知"
    bmi: float = 22.0
    stress_level: str = "medium"
    sleep_hours: float = 7.0
    primary_issue: str = ""
    medical_history: str = ""
    systolic_bp: int = 120
    diastolic_bp: int = 80
    fasting_glucose: float = 5.0

@router.post("/")
def create_patient(data: PatientCreate):
    return add_patient(**data.model_dump())

@router.get("/")
def get_all_patients():
    return list_patients()

@router.get("/{patient_id}")
def get_one_patient(patient_id: int):
    result = get_patient(patient_id)
    if "error" in result:
        raise HTTPException(status_code=404, detail=result["error"])
    return result

@router.get("/{patient_id}/trend")
def get_trend(patient_id: int):
    return get_health_trend(patient_id)

@router.get("/{patient_id}/history")
def get_history(patient_id: int):
    return get_patient_history(patient_id)
