import json
import os
import random
from openai import OpenAI
from dataclasses import dataclass
from typing import List
from dotenv import load_dotenv

from healthagent.models.patient_profile import PatientProfile
from healthagent.models.intervention import InterventionPlan

load_dotenv()


@dataclass
class ComplianceDecision:
    compliant: bool
    probability: float
    compliance_score: float


class ComplianceAgent:
    def __init__(self, patient: PatientProfile, intervention: InterventionPlan, seed: int = None):
        self.patient = patient
        self.intervention = intervention
        self.rng = random.Random(seed)
        self.client = OpenAI(
            api_key=os.environ.get("DEEPSEEK_API_KEY"),
            base_url="https://api.deepseek.com"
        )
        self.compliance_history: List[bool] = []

    def decide(self, stress_level: float, motivation: float, day_of_week: int, 
               sleep_quality: float, fatigue: float) -> ComplianceDecision:
        """Decide whether the patient complies with the intervention today."""
        
        # Calculate consecutive compliance days
        consecutive = 0
        for r in reversed(self.compliance_history):
            if r:
                consecutive += 1
            else:
                break

        # Build prompt
        prompt = f"""You are a behavioral science expert. Determine if the patient will follow the health intervention plan today.

Patient Information:
- Age: {self.patient.age} years old
- Stress Level: {self.patient.stress_level.value}
- Primary Issue: {self.patient.primary_issue}
- Motivation Score: {self.patient.motivation_score}

Intervention Plan: {self.intervention.name} (Difficulty: {self.intervention.difficulty})
Rules: {', '.join(self.intervention.rules)}

Today's Status:
- Day of Week: {day_of_week} (0=Monday, 6=Sunday)
- Stress Level: {stress_level:.2f}
- Sleep Quality: {sleep_quality:.2f}
- Fatigue Level: {fatigue:.2f}
- Consecutive Compliance Days: {consecutive}
- Recent 7-Day Record: {self.compliance_history[-7:] if self.compliance_history else 'No record'}

Please output strictly in the following JSON format with no other content:
{{
  "complied": true or false,
  "compliance_quality": "full" or "partial" or "none",
  "compliance_probability": a number between 0 and 1,
  "key_factor": "the most critical factor affecting today's compliance",
  "narrative": "what happened today (within 100 words, third person)"
}}"""

        # Call DeepSeek API
        response = self.client.chat.completions.create(
            model="deepseek-chat",
            messages=[{"role": "user", "content": prompt}],
            max_tokens=500
        )
        
        raw = response.choices[0].message.content
        raw = raw.strip().removeprefix("```json").removeprefix("```").removesuffix("```").strip()
        result = json.loads(raw)
        
        # Update history
        self.compliance_history.append(result.get("complied", False))
        
        # Return decision object
        return ComplianceDecision(
            compliant=result.get("complied", False),
            probability=result.get("compliance_probability", 0.5),
            compliance_score=1.0 if result.get("compliance_quality") == "full" else (0.5 if result.get("compliance_quality") == "partial" else 0.0)
        )
