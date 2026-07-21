from healthagent.models.patient_profile import PatientProfile, StressLevel
from healthagent.models.intervention import InterventionPlan, InterventionType
from healthagent.agents.compliance_agent import ComplianceAgent

patient = PatientProfile(
    age=34,
    stress_level=StressLevel.MEDIUM,
    primary_issue='Chronic sleep deprivation',
    sleep_avg_hours=5.8
)

intervention = InterventionPlan(
    name='Sleep Optimization Phase 1',
    intervention_type=InterventionType.SLEEP_OPTIMIZATION,
    duration_days=90,
    difficulty=0.5,
    rules=['Sleep before 11pm', 'Wake up at 7am', 'No screens 30min before bed']
)

agent = ComplianceAgent(patient, intervention)
result = agent.decide(1, [], 'motivated', 0.3, 0.2)
print(result)
