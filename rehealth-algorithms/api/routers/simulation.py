import sys, json
sys.path.insert(0, r'C:\Users\Administrator\Desktop\ReHealth AI HealthAgent')
from fastapi import APIRouter
from pydantic import BaseModel
from typing import Optional

router = APIRouter()

class SimulationRequest(BaseModel):
    age: int
    stress: str = "medium"
    sleep_hours: float = 7.0
    bmi: float = 22.0
    days: int = 7
    primary_issue: str = ""

class PatientSimRequest(BaseModel):
    patient_id: int
    days: int = 7

@router.post("/run")
def run_simulation(data: SimulationRequest):
    from healthagent.run import run_health_simulation
    return run_health_simulation(**data.model_dump())

@router.post("/patient")
def simulate_patient(data: PatientSimRequest):
    from healthagent.patient_db.db import get_patient, save_simulation
    from healthagent.run import run_health_simulation
    patient = get_patient(data.patient_id)
    if "error" in patient:
        return patient
    params = {
        "age": patient["age"],
        "stress": patient["stress_level"],
        "sleep_hours": patient["sleep_hours"],
        "bmi": patient["bmi"],
        "days": data.days,
        "primary_issue": patient["primary_issue"]
    }
    result = run_health_simulation(**params)
    save_simulation(data.patient_id, data.days, params, result)
    return result
