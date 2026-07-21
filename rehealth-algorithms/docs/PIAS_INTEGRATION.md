# PIAS Engine Integration Guide

> Production status (2026-07-13): this document describes the research PIAS implementation in `rehealth-algorithms`. It does not mean the complete PIAS chain is available to the Android app in production. The approved production path is `Android App -> backend -> model-service`; Python PIAS code and model artifacts must not be embedded in the APK. See `docs/LOWDIM_WEARABLE_MODEL.md` for the current low-dimensional model requirement and deployment boundary.

## Overview

PIAS (Predict, Intervene, Attribute, Settle) engine has been integrated into HealthAgent to provide CVD risk prediction and intervention attribution capabilities.

## Architecture

```
HealthAgent
├── healthagent/
│   ├── pias/                    # PIAS Engine
│   │   ├── __init__.py
│   │   ├── risk_scorer.py       # P - CatBoost risk prediction
│   │   ├── individual_prediction.py  # A Level 1 - Individual trajectory
│   │   ├── group_attribution.py      # A Level 2 - Group attribution (PSM+DRE)
│   │   └── prescription_generator.py # I - Intervention prescriptions
│   ├── agents/                  # Multi-agent simulation
│   ├── models/
│   └── engine/
│
└── api/
    ├── main.py
    └── routers/
        ├── pias.py              # PIAS API endpoints
        ├── patients.py
        ├── simulation.py
        └── records.py
```

## API Endpoints

### P — Predict

```http
POST /api/pias/predict
```

Predict CVD risk using 16 clinical features.

**Request Body:**
```json
{
  "age": 52,
  "gender": 1,
  "bmi": 27.5,
  "sbp": 145,
  "dbp": 90,
  "fasting_glucose": 6.2,
  "total_cholesterol": 5.8,
  "ldl": 3.9,
  "hdl": 1.1,
  "triglycerides": 2.1,
  "exercise_days": 2,
  "smoking": 1,
  "drinking": 0,
  "diabetes_history": 0,
  "hypertension_history": 1,
  "family_history": 1
}
```

**Response:**
```json
{
  "risk_score": 0.723,
  "risk_level": "high",
  "feature_contributions": {
    "age": 0.15,
    "sbp": 0.12,
    "smoking": 0.08,
    ...
  },
  "model_version": "2.0",
  "model_auc": 0.767
}
```

### I — Intervene

```http
POST /api/pias/intervene
```

Generate personalized intervention prescription.

**Request Body:**
```json
{
  "risk_result": { ... },  // Output from /predict
  "patient_context": {
    "age": 52,
    "gender": "male"
  }
}
```

### A — Attribute (Level 1: Individual)

```http
POST /api/pias/attribute/individual
```

Predict individual risk trajectory with confidence intervals.

**Request Body:**
```json
{
  "risk_history": [
    {"date": "2026-05-01", "Y": 0.52, "Z": 1},
    {"date": "2026-05-02", "Y": 0.50, "Z": 1},
    ...
  ],
  "forecast_days": 30,
  "decay_factor": 0.95
}
```

**Response includes:**
- `forecast_status_quo`: Risk trajectory without intervention
- `forecast_with_plan`: Risk trajectory with continued intervention
- `forecast_ci_upper/lower`: 95% confidence intervals
- `retrospective_att`: Average Treatment Effect on Treated
- `report_text`: Human-readable report

### A — Attribute (Level 2: Group)

```http
POST /api/pias/attribute/group
```

Group attribution using PSM + DRE + Bootstrap.

**Request Body:**
```json
{
  "user_records": [
    {
      "device_id": "user_001",
      "Z": 1,
      "delta_Y": -0.05,
      "features": {
        "age_bracket": 2,
        "bmi_level_encoded": 1,
        "bp_baseline_grade_encoded": 2,
        "activity_level_encoded": 1,
        "gender_encoded": 1,
        "season_sin": 0.5,
        "season_cos": 0.866
      }
    },
    ...
  ],
  "n_bootstrap": 200,
  "confidence_level": 0.95
}
```

**Response includes:**
- `att`: Average Treatment Effect
- `ci_lower/ci_upper`: Confidence interval
- `is_significant`: Statistical significance
- `matching_balance`: SMD balance diagnostics
- `gamma_sensitivity`: Rosenbaum sensitivity analysis
- `settlement_report`: Formatted report for insurance

### S — Settle

```http
POST /api/pias/settle
```

Generate settlement report for insurance company.

Same request as `/attribute/group`, returns formatted settlement report.

## Setup

### 1. Install Dependencies

```bash
pip install -r requirements.txt
```

### 2. Set Environment Variables

```bash
# Required for model loading
export PIAS_MODEL_PATH="models/rehealth_v2_final.pkl"
export PIAS_MODEL_AUC=0.767

# Optional: For LLM-based prescriptions
export DEEPSEEK_API_KEY="your_key_here"
```

### 3. Place Model File

Copy the CatBoost model to the models directory:

```bash
mkdir -p models
cp /path/to/rehealth_v2_final.pkl models/
```

### 4. Run the Server

```bash
cd api
uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

## Testing

```bash
# Test prediction
curl -X POST http://localhost:8000/api/pias/predict \
  -H "Content-Type: application/json" \
  -d '{
    "age": 52,
    "gender": 1,
    "bmi": 27.5,
    "sbp": 145,
    "dbp": 90,
    "fasting_glucose": 6.2,
    "total_cholesterol": 5.8,
    "ldl": 3.9,
    "hdl": 1.1,
    "triglycerides": 2.1,
    "exercise_days": 2,
    "smoking": 1,
    "drinking": 0,
    "diabetes_history": 0,
    "hypertension_history": 1,
    "family_history": 1
  }'

# Test individual attribution
curl -X POST http://localhost:8000/api/pias/attribute/individual \
  -H "Content-Type: application/json" \
  -d '{
    "risk_history": [
      {"date": "2026-05-01", "Y": 0.52, "Z": 1},
      {"date": "2026-05-02", "Y": 0.50, "Z": 1},
      {"date": "2026-05-03", "Y": 0.48, "Z": 0},
      ...
    ],
    "forecast_days": 30
  }'
```

## Integration with HealthAgent Simulation

The PIAS engine can be combined with HealthAgent's multi-agent simulation:

```python
from healthagent.pias import CVDRiskScorer, IndividualPredictor
from healthagent import SimulationEngine

# 1. Predict initial risk
scorer = CVDRiskScorer("models/rehealth_v2_final.pkl", model_auc=0.767)
risk_result = scorer.predict(patient_features)

# 2. Run simulation
engine = SimulationEngine()
simulation_result = engine.run_simulation(patient, intervention)

# 3. Track risk over time
risk_history = []
for day in simulation_result.trajectory:
    risk_history.append({
        "date": day.date,
        "Y": day.risk_score,
        "Z": 1 if day.compliant else 0
    })

# 4. Predict trajectory
predictor = IndividualPredictor()
trajectory = predictor.predict(risk_history)
```

## Key Features

### Predict (P)
- CatBoost model with 16 clinical features
- SHAP explanations for feature importance
- Risk level classification (low/moderate/high/very_high)

### Intervene (I)
- Rule-based prescriptions (LLM optional)
- Personalized recommendations based on risk factors
- Follow-up scheduling

### Attribute (A)
- **Level 1**: Individual trajectory prediction
  - Exponential decay model
  - Confidence intervals
  - Paired ATT (Average Treatment Effect on Treated)

- **Level 2**: Group attribution
  - Propensity Score Matching (PSM) with KD-tree
  - Doubly Robust Estimation (DRE)
  - Bootstrap confidence intervals
  - Rosenbaum sensitivity analysis

### Settle (S)
- Insurance settlement reports
- Statistical significance testing
- Balance diagnostics
- Sensitivity analysis (Γ value)

## Model Versions

| Version | Features | AUC | Usage |
|---------|----------|-----|-------|
| V2 | 16 | 0.767 | **Production API** |
| V5 | 97 | 0.8641 | Research |
| V8 | 97 | 0.8615 | Research baseline |

**Decision**: Production uses V2 (16 features) because real users don't have CBC/liver function/accelerometer data.

## Related Documentation

- [PIAS归因层全面改进](../docs/gemini_conversation/20260527_PIAS归因层全面改进.md)
- [项目状态与记忆索引](../docs/gemini_conversation/20260528_项目状态与记忆索引.md)
